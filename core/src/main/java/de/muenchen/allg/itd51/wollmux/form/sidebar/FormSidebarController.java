/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.form.sidebar;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFocusListener;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XModel;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentControllerInitialized;
import de.muenchen.allg.itd51.wollmux.form.config.FormConfig;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;
import de.muenchen.allg.itd51.wollmux.form.model.Control;
import de.muenchen.allg.itd51.wollmux.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.form.model.FormValueChangedListener;
import de.muenchen.allg.itd51.wollmux.form.model.VisibilityChangedListener;
import de.muenchen.allg.itd51.wollmux.form.model.VisibilityGroup;
import de.muenchen.allg.itd51.wollmux.ui.UIElementConfig;
import de.muenchen.allg.util.UnoProperty;

/**
 * The controller of the form sidebar.
 */
public class FormSidebarController implements VisibilityChangedListener, FormValueChangedListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FormSidebarController.class);

  public static final String WM_FORM_GUI = "FormularGuiDeck";

  /**
   * The controlled panel.
   */
  private FormSidebarPanel formSidebarPanel;

  /**
   * The event listener on {@link WollMuxEventHandler} has been unregistered.
   */
  private boolean isUnregistered;

  /**
   * The controller of the document.
   */
  private TextDocumentController documentController;

  /**
   * The controller of the form.
   */
  private FormController formController;

  /**
   * The model of the form.
   */
  private FormModel formModel;

  private FormConfig formConfig;

  /**
   * Are ui elements handled by the controller.
   */
  private boolean processUIElementEvents = false;

  /**
   * Don't process value changes on these IDs.
   */
  private List<String> noProcessValueChangedEvents = new ArrayList<>();

  /**
   * Jumps to book mark in document when a form GUI element gets focused.
   */
  private AbstractFocusListener focusListener = new AbstractFocusListener()
  {
    @Override
    public void focusGained(FocusEvent event)
    {
      if (processUIElementEvents)
      {
        processUIElementEvents = false;
        XControl control = UNO.XControl(event.Source);
        try
        {
          String focusedField = (String) UnoProperty.getProperty(control.getModel(), UnoProperty.DEFAULT_CONTROL);
          documentController.getModel().focusFormField(focusedField);
          XTextCursor cursor = UNO.XTextViewCursorSupplier(documentController.getModel().doc.getCurrentController())
              .getViewCursor();
          cursor.collapseToEnd();
        } catch (UnoHelperException e)
        {
          LOGGER.trace("", e);
        }
      }
      processUIElementEvents = true;
    }
  };

  /**
   * Create a new controller and the gui of the form.
   *
   * @param resourceUrl
   *          The resource description
   * @param context
   *          The context of the sidebar.
   * @param xWindow
   *          The parent window, which contains the sidebar.
   * @param model
   *          The model of the document to which the sidebar belongs.
   */
  public FormSidebarController(String resourceUrl, XComponentContext context, XWindow xWindow, XModel model)
  {
    this.formSidebarPanel = new FormSidebarPanel(context, xWindow, resourceUrl, this);

    XTextDocument doc = UNO.XTextDocument(model);
    if (DocumentManager.hasTextDocumentController(doc))
    {
      isUnregistered = true;
      onTextDocumentControllerInitialized(
          new OnTextDocumentControllerInitialized(DocumentManager.getTextDocumentController(doc)));
    } else
    {
      isUnregistered = false;
      WollMuxEventHandler.getInstance().registerListener(this);
    }
  }

  public FormSidebarPanel getFormSidebarPanel()
  {
    return this.formSidebarPanel;
  }

  public XFocusListener getFocusListener()
  {
    return focusListener;
  }

  /**
   * Sets @{link TextDocumentController} once it is available.
   *
   * @param event
   *          Instance of @{link TextDocumentController}.
   */
  @Subscribe
  public void onTextDocumentControllerInitialized(OnTextDocumentControllerInitialized event)
  {
    documentController = event.getTextDocumentController();

    if (documentController == null)
    {
      LOGGER.trace("{} notify(): documentController is NULL.", this.getClass().getSimpleName());
      return;
    }

    if (documentController.getModel().isFormDocument())
    {
      try
      {
        this.formController = documentController.getFormController();
        formConfig = documentController.getFormConfig();
        formModel = documentController.getFormModel();

        formSidebarPanel.createTabControl(formConfig, formModel);
        formModel.addFormModelChangedListener(this, true);
        formModel.addVisibilityChangedListener(this, true);
        processUIElementEvents = true;
      } catch (FormModelException e)
      {
        LOGGER.trace("", e);
      }
    } else
    {
      formSidebarPanel.createTabControl(null, null);
    }
    unregisterListener();
  }

  /**
   * Unregister the listener on the WollMux Event Bus.
   */
  public void unregisterListener()
  {
    if (!isUnregistered)
    {
      WollMuxEventHandler.getInstance().unregisterListener(this);
      isUnregistered = true;
    }
  }

  /**
   * Handler for check boxes.
   *
   * @param event
   *          The event emitted by the check box.
   */
  public void checkBoxChanged(ItemEvent event)
  {
    try
    {
      XControl checkBox = UNO.XControl(event.Source);
      String id = (String) UnoProperty.getProperty(checkBox.getModel(), UnoProperty.DEFAULT_CONTROL);
      short state = (short) UnoProperty.getProperty(checkBox.getModel(), UnoProperty.STATE);
      String stateToString = state == 0 ? "false" : "true";
      setValue(id, stateToString);
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Handler for list boxes.
   *
   * @param event
   *          The event emitted by the list box.
   */
  public void listBoxChanged(ItemEvent event)
  {
    try
    {
      XControl listBox = UNO.XControl(event.Source);
      String id = (String) UnoProperty.getProperty(listBox.getModel(), UnoProperty.DEFAULT_CONTROL);
      String text = UNO.XListBox(listBox).getSelectedItem();
      setValue(id, text);
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Handler for combo boxes.
   *
   * @param event
   *          The event emitted by the combo box.
   */
  public void comboBoxChanged(ItemEvent event)
  {
    try
    {
      XControl comboBox = UNO.XControl(event.Source);
      String id = (String) UnoProperty.getProperty(comboBox.getModel(), UnoProperty.DEFAULT_CONTROL);
      String text = (String) UnoProperty.getProperty(comboBox.getModel(), UnoProperty.TEXT);
      setValue(id, text);
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Handler for text fields and areas.
   *
   * @param event
   *          The event emitted by the text field/area.
   */
  public void textChanged(TextEvent event)
  {
    try
    {
      XControl txtField = UNO.XControl(event.Source);
      String id = (String) UnoProperty.getProperty(txtField.getModel(), UnoProperty.DEFAULT_CONTROL);
      String text = (String) UnoProperty.getProperty(txtField.getModel(), UnoProperty.TEXT);
      setValue(id, text);
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Update the form model with a new value. Don't handle value changes on the control which
   * triggered this action.
   *
   * @param id
   *          The ID of the field.
   * @param value
   *          THe new value of the field.
   */
  private void setValue(String id, String value)
  {
    LOGGER.info("FormSidebarController:setValue() id {} value {}", id, value);
    LOGGER.info("FormSidebarController:setValue() processUIElementEvents {}", processUIElementEvents);
    if (processUIElementEvents)
    {
      noProcessValueChangedEvents.add(id);
      formController.setValue(id, value, null);
      noProcessValueChangedEvents.remove(id);
    }
  }

  /**
   * Handler for buttons.
   *
   * @param actionEvent
   *          The event emitted by the button.
   */
  public void buttonPressed(ActionEvent actionEvent)
  {
    if (!processUIElementEvents)
    {
      return;
    }
    processUIElementEvents = false;
    String action = actionEvent.ActionCommand;

    if (action == null || action.isEmpty())
    {
      LOGGER.error("{} processActionCommand(): action is NULL or empty.", this.getClass().getSimpleName());
      processUIElementEvents = true;
      return;
    }

    try
    {
      XControl xControl = UNO.XControl(actionEvent.Source);
      String id = (String) UnoProperty.getProperty(xControl.getModel(), UnoProperty.DEFAULT_CONTROL);
      UIElementConfig formControl = formConfig.getControls().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
      if (formControl == null)
      {
        return;
      }

      switch (action)
      {
      case "abort":
        formController.close();
        break;
      case "nextTab":
        formSidebarPanel.nextTab();
        break;
      case "prevTab":
        formSidebarPanel.previousTab();
        break;
      case "funcDialog":
        String dialogName = formControl.getDialog();
        formController.openDialog(dialogName);
        break;
      case "closeAndOpenExt":
        formController.closeAndOpenExt(formControl.getExt());
        break;
      case "saveTempAndOpenExt":
        formController.saveTempAndOpenExt(formControl.getExt());
        break;
      case "printForm":
        formController.print();
        break;
      case "form2PDF":
        formController.pdf();
        break;
      case "save":
        formController.save();
        break;
      case "saveAs":
        formController.saveAs();
        break;
      case "openTemplate":
        formController.openTemplateOrDocument(List.of(formControl.getFragId()), true);
        break;
      case "openDocument":
        formController.openTemplateOrDocument(List.of(formControl.getFragId()), false);
        break;
      case "openExt":
        OpenExt openExInstance = OpenExt.getInstance(formControl.getExt(), formControl.getUrl());
        openExInstance.launch(x -> LOGGER.error("", x));
        break;
      case "form2EMail":
        formController.sendAsEmail();
        break;
      default:
        break;
      }
    } catch (UnoHelperException | MalformedURLException x)
    {
      LOGGER.error("", x);
    } finally
    {
      processUIElementEvents = true;
    }
  }

  /**
   * Sets control's text if value changed. Can be called by a dependency to another control.
   */
  @Override
  public void valueChanged(String id, String value)
  {
    if (!noProcessValueChangedEvents.contains(id))
    {
      processUIElementEvents = false;
      formSidebarPanel.setText(id, value);
      processUIElementEvents = true;
    }
  }

  /**
   * Get notifications if current textfield's value is valid. Colorize if not.
   */
  @Override
  public void statusChanged(String id, boolean okay)
  {
    formSidebarPanel.setBackgroundColor(id, okay, formConfig.getPlausiMarkerColor().getRGB() & ~0xFF000000);
  }

  /**
   * Hide / Show form controls by its new visibility changed status.
   */
  @Override
  public void visibilityChanged(String groupId, boolean visible)
  {
    Collection<Control> controls = formModel.getControlsByGroupId(groupId);

    for (Control control : controls)
    {
      String controlId = control.getId();
      formSidebarPanel.setVisible(controlId, control.getGroups().stream().allMatch(VisibilityGroup::isVisible));
    }
    formSidebarPanel.paint();
  }

}
