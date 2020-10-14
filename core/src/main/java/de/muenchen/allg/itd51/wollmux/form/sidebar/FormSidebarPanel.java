/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.FocusChangeReason;
import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindow2;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.dialog.adapter.AbstractSidebarPanel;
import de.muenchen.allg.dialog.adapter.AbstractTabPageContainerListener;
import de.muenchen.allg.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementType;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.form.config.FormConfig;
import de.muenchen.allg.itd51.wollmux.form.config.TabConfig;
import de.muenchen.allg.itd51.wollmux.form.model.Control;
import de.muenchen.allg.itd51.wollmux.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.ui.HTMLElement;
import de.muenchen.allg.itd51.wollmux.ui.layout.ControlLayout;
import de.muenchen.allg.itd51.wollmux.ui.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.ui.layout.Layout;
import de.muenchen.allg.itd51.wollmux.ui.layout.VerticalLayout;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;

/**
 * form UI in sidebar.
 */
public class FormSidebarPanel extends AbstractSidebarPanel implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FormSidebarPanel.class);
  private static final int BUTTONS_PER_ROW = 2;
  private XWindow parentWindow;
  private XControlContainer controlContainer;
  private Layout vLayout;
  private Layout buttonLayout;
  private XComponentContext context;
  private XMultiComponentFactory xMCF;
  private Map<Short, Layout> tabPageLayouts = new HashMap<>();
  private Map<Control, Short> buttons = new HashMap<>();
  private XTabPageContainer tabControlContainer;
  private FormSidebarController formSidebarController;
  private Map<String, Pair<XControl, XControl>> controls = new HashMap<>();
  private boolean visibilityChanged = false;
  private boolean tabChanged = true;

  /**
   * Creates a new form panel.
   * 
   * @param context
   *          the sidebar context.
   * @param parentWindow
   *          parent sidebar window.
   * @param resourceUrl
   *          resource description.
   * @param formSidebarController
   *          {@link FormSidebarController} the form sidebar controller.
   */
  public FormSidebarPanel(XComponentContext context, XWindow parentWindow, String resourceUrl,
      FormSidebarController formSidebarController)
  {
    super(resourceUrl);
    this.context = context;
    this.panel = this;
    this.xMCF = UNO.XMultiComponentFactory(context.getServiceManager());

    vLayout = new VerticalLayout();
    buttonLayout = new VerticalLayout(20, 5, 0, 0, 5);

    this.parentWindow = parentWindow;
    controlContainer = UNO
        .XControlContainer(GuiFactory.createControlContainer(xMCF, context, new Rectangle(0, 0, 0, 0), null));
    XWindowPeer parentWindowPeer = UNO.XWindowPeer(parentWindow);
    XToolkit parentToolkit = parentWindowPeer.getToolkit();
    UNO.XControl(controlContainer).createPeer(parentToolkit, parentWindowPeer);

    this.formSidebarController = formSidebarController;

    parentWindow.addWindowListener(new AbstractWindowListener()
    {
      @Override
      public void windowResized(WindowEvent e)
      {
        paint();
      }
    });
  }

  /**
   * Paint the tabs and their content.
   */
  private void paint()
  {
    Rectangle rect = parentWindow.getPosSize();
    if (tabControlContainer != null)
    {
      if (visibilityChanged)
      {
        for (Map.Entry<Short, Layout> entry : tabPageLayouts.entrySet())
        {
          UNO.XTabPageModel(UNO.XControl(tabControlContainer.getTabPageByID(entry.getKey())).getModel())
              .setEnabled(entry.getValue().isVisible());
        }
      }
      visibilityChanged = false;
      short activeTab = tabControlContainer.getActiveTabPageID();
      Rectangle tabRect = UNO.XWindow(tabControlContainer.getTabPageByID(activeTab)).getPosSize();
      Rectangle newTabRect = new Rectangle(tabRect.X, rect.Y, rect.Width,
          tabPageLayouts.get(activeTab).getHeightForWidth(rect.Width));
      Pair<Integer, Integer> tabSize = tabPageLayouts.get(activeTab).layout(newTabRect);

      for (Map.Entry<Control, Short> entry : buttons.entrySet())
      {
        XWindow window = UNO.XWindow(controls.get(entry.getKey().getId()).getRight());
        if (window != null)
        {
          window.setVisible(entry.getValue() == activeTab && entry.getKey().isVisible());
        }
      }

      UNO.XWindow(tabControlContainer.getTabPageByID(activeTab)).setPosSize(tabRect.X, tabRect.Y, tabSize.getRight(),
          tabSize.getLeft() + tabRect.Y, PosSize.POSSIZE);
      Rectangle r = UNO.XWindow(tabControlContainer).getPosSize();
      UNO.XWindow(tabControlContainer).setPosSize(r.X, r.Y, tabSize.getRight(),
          tabSize.getLeft() + tabRect.Y,
          PosSize.POSSIZE);
      Rectangle buttonRect = new Rectangle(tabRect.X, tabRect.Y + tabSize.getLeft(), tabSize.getRight(),
          rect.Height - tabSize.getLeft());
      buttonLayout.layout(buttonRect);
    } else
    {
      vLayout.layout(rect);
    }
  }

  /**
   * Creates the tab control with its content controls.
   *
   * @param config
   *          The UI configuration of the form.
   * @param model
   *          {@link FormModel} FormModel from @{link {@link TextDocumentController}.
   */
  public void createTabControl(FormConfig config, FormModel model)
  {
    if (config != null && model != null)
    {
      XControl tabControl = GuiFactory.createTabPageContainer(xMCF, context);
      controlContainer.addControl("tabControl", tabControl);
      tabControlContainer = UNO.XTabPageContainer(tabControl);
      AbstractTabPageContainerListener listener = event -> formSidebarController.requestLayout();
      tabControlContainer.addTabPageContainerListener(listener);

      tabControlContainer = UNO.XTabPageContainer(tabControl);
      vLayout.addControl(tabControl);
      vLayout.addLayout(buttonLayout, 1);

      short tabId = 1;
      List<TabConfig> tabs = config.getTabs();
      for (int i = 0; i < tabs.size(); i++)
      {
        TabConfig tab = tabs.get(i);
        HTMLElement element = new HTMLElement(tab.getTitle());
        GuiFactory.createTab(this.xMCF, this.context, UNO.XTabPageContainerModel(tabControl.getModel()),
            element.getText(), tabId);
        XTabPage xTabPage = tabControlContainer.getTabPageByID(tabId);
        XControlContainer tabPageControlContainer = UNO.XControlContainer(xTabPage);
        Layout controlsVLayout = new VerticalLayout(5, 5, 0, 0, 6);

        if (i > 0)
        {
          addTabSwitcher("backward", tabs.get(i - 1), s -> {
            previousTab();
            s.reduce((f, se) -> se).ifPresent(XWindow::setFocus);
          }, tabPageControlContainer, controlsVLayout);
        }

        setControls(tab, tabPageControlContainer, controlsVLayout);

        if (i < tabs.size() - 1)
        {
          addTabSwitcher("forward", tabs.get(i + 1), s -> {
            nextTab();
            s.findFirst().ifPresent(XWindow::setFocus);
          }, tabPageControlContainer, controlsVLayout);
        }

        addButtonsToLayout(tab, model, controlContainer, buttonLayout, tabId);
        tabPageLayouts.put(tabId, controlsVLayout);

        tabId++;
      }
      tabControlContainer.setActiveTabPageID((short) 1);
    } else
    {
      XControl label = GuiFactory.createLabel(this.xMCF, this.context, "Das Dokument ist kein Formular.",
          new Rectangle(0, 0, 50, 20), null);
      controlContainer.addControl("label", label);
      vLayout.addControl(label);
    }

    formSidebarController.requestLayout();
  }

  /**
   * Add controls from {@link TabConfig} description to {link Layout}.
   * 
   * @param tab
   *          {@link TabConfig} Tab from {@link FormModel}.
   * @param tabPageControlContainer
   *          {@link XControlContainer} ControlContainer in which the controls will be inserted.
   * @param layout
   *          The {@link Layout} in which newly created Layouts by this method will be inserted.
   * @return The generated Layout.
   */
  private void setControls(TabConfig tab, XControlContainer tabPageControlContainer, Layout layout)
  {
    tab.getControls().forEach(control -> {
      Layout controlLayout = createXControlByType(control, tabPageControlContainer);

      if (controlLayout == null)
      {
        LOGGER.trace("layout with control id '{}' is null.", control.getId());
      } else
      {
        layout.addLayout(controlLayout, 1);
      }
    });
  }

  /**
   * Add a control that performs the supplied action if it gets the focus. The action gets a
   * {@link Stream} of the controls which are visible on the given tab.
   *
   * @param id
   *          The ID of the control.
   * @param tab
   *          The tab which has the controls for the action.
   * @param action
   *          The action.
   * @param tabPageControlContainer
   *          {@link XControlContainer} ControlContainer in which the control will be inserted.
   * @param layout
   *          The {@link Layout} in which in which the control will be inserted.
   */
  private void addTabSwitcher(String id, TabConfig tab, Consumer<Stream<XWindow2>> action,
      XControlContainer tabPageControlContainer, Layout layout)
  {
    XControl tabSwithcer = GuiFactory.createTextfield(xMCF, context, id + tab.getId(), new Rectangle(0, 0, 0, 0), null,
        null);
    UNO.XWindow(tabSwithcer).addFocusListener(new AbstractFocusListener()
    {
      @Override
      public void focusGained(FocusEvent event)
      {
        if ((event.FocusFlags & FocusChangeReason.TAB) == 1)
        {
          Stream<XWindow2> controlConfigs = tab.getControls().stream()
              .filter(c -> c.getType() == UIElementType.BUTTON || c.getType() == UIElementType.CHECKBOX
                  || c.getType() == UIElementType.COMBOBOX || c.getType() == UIElementType.LISTBOX
                  || c.getType() == UIElementType.TEXTAREA || c.getType() == UIElementType.TEXTFIELD)
              .map(c -> controls.get(c.getId())).filter(Objects::nonNull).map(p -> UNO.XWindow2(p.getRight()))
              .filter(Objects::nonNull).filter(XWindow2::isVisible);
          action.accept(controlConfigs);
        }
      }
    });
    layout.addLayout(new ControlLayout(UNO.XWindow(tabSwithcer), 0)
    {
      @Override
      public boolean isVisible()
      {
        return false;
      }
    }, 1);
    tabPageControlContainer.addControl(id, tabSwithcer);
  }

  /**
   * Inserts Buttons from form descriptions 'Buttons'-Section to given layout.
   * 
   * @param tab
   *          Tab from {@link FormModel}
   * @param tabPageControlContainer
   *          The ControlContainer where controls will be inserted.
   * @param tabPageButtonVLayout
   *          layout instance.
   * @param tabId
   *          The ID of the tab.
   */
  private void addButtonsToLayout(TabConfig tab, FormModel model, XControlContainer tabPageControlContainer,
      Layout layout, short tabId)
  {
    if (!tab.getButtons().isEmpty())
    {
      Layout tabButtonLayout = null;
      for (int i = 0; i < tab.getButtons().size(); i++)
      {
        if (i % (BUTTONS_PER_ROW + 1) == 0)
        {
          tabButtonLayout = new HorizontalLayout(0, 0, 0, 0, 5);
          layout.addLayout(tabButtonLayout, 1);
        }

        UIElementConfig control = tab.getButtons().get(i);
        Layout controlLayout = createXControlByType(control, tabPageControlContainer);
        if (controlLayout == null)
        {
          LOGGER.trace("layout with control id '{}' is null.", control.getId());
        } else
        {
          buttons.put(model.getControl(control.getId()), tabId);
          tabButtonLayout.addLayout(controlLayout, 1);
        }
      }
    }
  }

  /**
   * Creates LO's UI controls by type of {@link UIElementConfig}. And puts them in a Layout.
   * 
   * @param control
   *          control model.
   * @param page
   *          {@link XTabPage} as XControl
   * @return Layout with one or more Controls.
   */
  private Layout createXControlByType(UIElementConfig control, XControlContainer pageContainer)
  {
    Layout layout = new HorizontalLayout(0, 0, 5, 10, 5);
    XControl xLabel = null;
    XControl xControl = null;
    SortedMap<String, Object> props = new TreeMap<>();

    String controlLabel = control.getLabel();
    HTMLElement htmlElement = new HTMLElement(controlLabel);

    switch (control.getType())
    {
    case TEXTFIELD:
      xLabel = createLabel(control, htmlElement);
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.READ_ONLY, control.isReadonly());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      xControl = GuiFactory.createTextfield(xMCF, context, "", new Rectangle(0, 0, 100, 20), props,
          formSidebarController::textChanged);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case BUTTON:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.LABEL, htmlElement.getText());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.ENABLED, !control.isReadonly());
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      xControl = GuiFactory.createButton(xMCF, context, htmlElement.getText(), formSidebarController::buttonPressed,
          new Rectangle(0, 0, 150, 20), props);
      UNO.XButton(xControl).setActionCommand(control.getAction());
      break;

    case LABEL:
      xLabel = createLabel(control, htmlElement);
      break;

    case COMBOBOX:
      xLabel = createLabel(control, htmlElement);
      if (control.isEditable())
      {
        xControl = createComboBox(control);
      } else
      {
        xControl = createListBox(control);
      }
      break;

    case CHECKBOX:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.LABEL, htmlElement.getText());
      props.put(UnoProperty.STATE, (short) 0);
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.ENABLED, !control.isReadonly());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      xControl = GuiFactory.createCheckBox(xMCF, context, formSidebarController::checkBoxChanged,
          new Rectangle(0, 0, 100, 20), props);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case TEXTAREA:
      layout = new VerticalLayout();
      xLabel = createLabel(control, htmlElement);
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.READ_ONLY, control.isReadonly());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      xControl = GuiFactory.createTextfield(xMCF, context, "",
          new Rectangle(0, 0, 100, control.getLines() * 20), props, formSidebarController::textChanged);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case LISTBOX:
      xLabel = createLabel(control, htmlElement);
      xControl = createListBox(control);
      break;

    case SEPARATOR:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      xControl = GuiFactory.createLine(xMCF, context, new Rectangle(0, 0, 100, 5), props);
      break;

    default:
      return null;
    }

    controls.put(control.getId(), Pair.of(xLabel, xControl));
    if (xLabel != null)
    {
      layout.addControl(xLabel);
      pageContainer.addControl(control.getId() + "Label", xLabel);
    }
    if (xControl != null)
    {
      layout.addControl(xControl);
      pageContainer.addControl(control.getId(), xControl);
    }
    return layout;
  }

  private XControl createLabel(UIElementConfig control, HTMLElement htmlElement)
  {
    if (htmlElement != null)
    {
      SortedMap<String, Object> props = new TreeMap<>();
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      props.put("TabIndex", -1);
      if (!htmlElement.getHref().isEmpty())
      {
        props.put(UnoProperty.URL, htmlElement.getHref());
        return GuiFactory.createHyperLinkLabel(xMCF, context, htmlElement.getText(), new Rectangle(0, 0, 100, 20),
            props);
      } else
      {
        return GuiFactory.createLabel(xMCF, context, htmlElement.getText(), new Rectangle(0, 0, 100, 20), props);
      }
    } else
    {
      LOGGER.trace("HTMLElement is NULL.");
    }

    return null;
  }

  private XControl createListBox(UIElementConfig control)
  {
    SortedMap<String, Object> propsListBox = new TreeMap<>();
    propsListBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsListBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsListBox.put(UnoProperty.DROPDOWN, true);
    propsListBox.put(UnoProperty.HELP_TEXT, control.getTip());
    XControl xControl = GuiFactory.createListBox(xMCF, context, formSidebarController::listBoxChanged,
        new Rectangle(0, 0, 100, 20), propsListBox);
    UNO.XListBox(xControl).setDropDownLineCount((short) 10);
    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XListBox(xControl).addItems(cmbValues, (short) 0);
    }
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());

    return xControl;
  }

  private XControl createComboBox(UIElementConfig control)
  {
    SortedMap<String, Object> propsComboBox = new TreeMap<>();
    propsComboBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsComboBox.put(UnoProperty.DROPDOWN, true);
    propsComboBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsComboBox.put(UnoProperty.BORDER, (short) 2);
    propsComboBox.put(UnoProperty.HELP_TEXT, control.getTip());
    XControl xControl = GuiFactory.createCombobox(xMCF, context, "", formSidebarController::comboBoxChanged,
        new Rectangle(0, 0, 100, 20), propsComboBox);
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XComboBox(xControl).addItems(cmbValues, (short) 0);
    }

    return xControl;
  }

  /**
   * Set Text Property on {@link XControlModel}.
   * 
   * @param id
   *          {@link XControlModel} Model from {@link XControl}
   * @param text
   *          Text Value.
   */
  public void setText(String id, String text)
  {
    XControl control = controls.get(id).getRight();
    if (UNO.XTextComponent(control) != null)
    {
      UNO.XTextComponent(control).setText(text);
    } else if (UNO.XListBox(control) != null)
    {
      UNO.XListBox(control).selectItem(text, true);
    } else if (UNO.XCheckBox(control) != null)
    {
      UNO.XCheckBox(control).setState((short) (Boolean.parseBoolean(text) ? 1 : 0));
    } else
    {
      LOGGER.debug("Unknown control type");
    }
  }

  @Override
  public LayoutSize getHeightForWidth(int width)
  {
    int height = vLayout.getHeightForWidth(width);
    if (tabControlContainer != null)
    {
      short activeId = tabControlContainer.getActiveTabPageID();
      Rectangle r = UNO.XWindow(tabControlContainer.getTabPageByID(activeId)).getPosSize();
      Layout currentLayout = tabPageLayouts.get(activeId);
      height = currentLayout.getHeightForWidth(width);
      height += buttonLayout.getHeightForWidth(width);
      height += r.Y + 5;
    }
    return new LayoutSize(height, height, height);
  }

  @Override
  public int getMinimalWidth()
  {
    int width = 0;
    try
    {
      int maxWidth = (int) UnoConfiguration.getConfiguration("org.openoffice.Office.UI.Sidebar/General",
          "MaximumWidth") - 60;
      for (Map.Entry<Short, Layout> e : tabPageLayouts.entrySet())
      {
        width = Integer.max(width, e.getValue().getMinimalWidth(maxWidth));
      }
      width = Integer.max(width, buttonLayout.getMinimalWidth(maxWidth));
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return width;
    }
    return width;
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    return UNO.XAccessible(getWindow());
  }

  @Override
  public XWindow getWindow()
  {
    return UNO.XWindow(controlContainer);
  }

  @Override
  public void dispose()
  {
    formSidebarController.unregisterListener();
  }

  /**
   * Activate the previous tab.
   */
  public void previousTab()
  {
    if (tabChanged)
    {
      tabChanged = false;
      short currentabPageId = tabControlContainer.getActiveTabPageID();
      short prev = (short) (currentabPageId - 1);

      if (prev > 0)
      {
        tabControlContainer.setActiveTabPageID(prev);
      }
      formSidebarController.requestLayout();
      tabChanged = true;
    }
  }

  /**
   * Activate the next tab.
   */
  public void nextTab()
  {
    if (tabChanged)
    {
      tabChanged = false;
      short currentabPageId = tabControlContainer.getActiveTabPageID();
      short next = (short) (currentabPageId + 1);

      if (next > tabControlContainer.getTabPageCount())
      {
        tabControlContainer.setActiveTabPageID((short) 1);
      } else
      {
        tabControlContainer.setActiveTabPageID(next);
      }
      formSidebarController.requestLayout();
      tabChanged = true;
    }
  }

  /**
   * Hide or show a control and its label.
   *
   * @param id
   *          The ID of the control.
   * @param visible
   *          True if the control should be visible, false otherwise.
   */
  public void setVisible(String id, boolean visible)
  {
    Pair<XControl, XControl> control = controls.get(id);
    if (control != null && control.getLeft() != null)
    {
      UNO.XWindow(control.getLeft()).setVisible(visible);
    }
    if (control != null && control.getRight() != null)
    {
      UNO.XWindow(control.getRight()).setVisible(visible);
    }
    visibilityChanged = true;
  }

  /**
   * Change the background color of a control.
   *
   * @param id
   *          The ID of the control.
   * @param okay
   *          If true the control gets its default background color, otherwise the provided color.
   * @param color
   *          The background color to set if okay is false.
   */
  public void setBackgroundColor(String id, boolean okay, int color)
  {
    XControl control = controls.get(id).getRight();
    try
    {
      if (okay)
      {
        UnoProperty.setPropertyToDefault(control.getModel(), UnoProperty.BACKGROUND_COLOR);
      } else
      {
        UnoProperty.setProperty(control.getModel(), UnoProperty.BACKGROUND_COLOR, color);
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }
}
