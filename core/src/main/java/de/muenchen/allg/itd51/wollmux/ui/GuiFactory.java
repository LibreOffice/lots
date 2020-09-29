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
package de.muenchen.allg.itd51.wollmux.ui;

import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainerModel;
import com.sun.star.awt.tab.XTabPageModel;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoProperty;

/**
 * Factory for creating UNO control elements.
 */
public class GuiFactory
{

  private static final Logger LOGGER = LoggerFactory.getLogger(GuiFactory.class);

  private GuiFactory()
  {
    // nothing to initialize.
  }

  /**
   * Create a button with label and action listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param label
   *          The label.
   * @param listener
   *          The action listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new button.
   */
  public static XControl createButton(XMultiComponentFactory xMCF, XComponentContext context, String label,
      AbstractActionListener listener, Rectangle size, SortedMap<String, Object> props)
  {
    XControl buttonCtrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_BUTTON, props, size);
    XButton button = UNO.XButton(buttonCtrl);
    button.setLabel(label);
    button.addActionListener(listener);
    return buttonCtrl;
  }

  /**
   * Create a text field.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param text
   *          The text.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @param textListener
   *          Listener for changes in the text.
   * @return A new text field.
   */
  public static XControl createTextfield(XMultiComponentFactory xMCF, XComponentContext context, String text,
      Rectangle size, SortedMap<String, Object> props, AbstractTextListener textListener)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }

    props.put("VScroll", false);

    XControl buttonCtrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_EDIT, props, size);

    XTextComponent txt = UNO.XTextComponent(buttonCtrl);
    txt.addTextListener(textListener);
    txt.setText(text);
    return buttonCtrl;
  }

  /**
   * Create a label.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param text
   *          The text of the label.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new label.
   */
  public static XControl createLabel(XMultiComponentFactory xMCF, XComponentContext context, String text,
      Rectangle size, SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }

    XControl buttonCtrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_FIXED_TEXT, props, size);

    XFixedText txt = UNO.XFixedText(buttonCtrl);
    txt.setText(text);
    return buttonCtrl;
  }

  /**
   * Create a data model for a {@link XTreeControl}.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @return The model.
   */
  public static XMutableTreeDataModel createTreeModel(XMultiComponentFactory xMCF, XComponentContext context)
  {
    return UnoRuntime.queryInterface(XMutableTreeDataModel.class,
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_TREE_MUTABLE_TREE_DATA_MODEL, xMCF, context));
  }

  /**
   * Create a tree control with a given data model
   * {@link #createTreeModel(XMultiComponentFactory, XComponentContext)}.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param dataModel
   *          The data model.
   * @return A new tree control.
   */
  public static XControl createTree(XMultiComponentFactory xMCF, XComponentContext context,
      XMutableTreeDataModel dataModel)
  {
    SortedMap<String, Object> props = new TreeMap<>();
    props.put(UnoProperty.DATA_MODEL, dataModel);
    return GuiFactory.createControl(xMCF, context, UnoComponent.CSS_AWT_TREE_TREE_CONTROL, props,
        new Rectangle(0, 0, 400, 400));
  }

  /**
   * Create a combo box with options and item listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param text
   *          The selected text.
   * @param listener
   *          The item listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new combo box.
   */
  public static XControl createCombobox(XMultiComponentFactory xMCF, XComponentContext context, String text,
      AbstractItemListener listener, Rectangle size, SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_COMBO_BOX, props, size);
    XTextComponent tf = UNO.XTextComponent(ctrl);
    tf.setText(text);
    XComboBox cmb = UNO.XComboBox(ctrl);
    cmb.addItemListener(listener);
    cmb.setDropDownLineCount((short) 10);

    return ctrl;
  }

  /**
   * Create a numeric field with value and text listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param value
   *          The value.
   * @param listener
   *          The text listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new numeric field.
   */
  public static XControl createNumericField(XMultiComponentFactory xMCF, XComponentContext context, int value,
      XTextListener listener, Rectangle size, SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_NUMERIC_FIELD, props, size);
    UNO.XNumericField(ctrl).setValue(value);
    UNO.XTextComponent(ctrl).addTextListener(listener);

    return ctrl;
  }

  /**
   * Create a spin field with value and text listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param value
   *          The value.
   * @param listener
   *          The text listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new button.
   */
  public static XControl createSpinField(XMultiComponentFactory xMCF, XComponentContext context, int value,
      AbstractTextListener listener, Rectangle size, SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put(UnoProperty.SPIN, Boolean.TRUE);

    return createNumericField(xMCF, context, value, listener, size, props);
  }

  /**
   * Create a horizontal line.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param size
   *          The size.
   * @param props
   *          Some additional properties.
   * @return A new horizontal line.
   */
  public static XControl createLine(XMultiComponentFactory xMCF, XComponentContext context, Rectangle size,
      SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_FIXED_LINE, props, size);
  }

  /**
   * Create a list box with an item listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param listener
   *          The item listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new button.
   */
  public static XControl createListBox(XMultiComponentFactory xMCF, XComponentContext context,
      AbstractItemListener listener, Rectangle size, SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_LIST_BOX, props, size);
    UNO.XListBox(ctrl).addItemListener(listener);

    return ctrl;
  }


  /**
   * Creates a tab at the end of the given model. The tab can be accessed with
   * {@code model.getTabPageById(tabId);}
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param model
   *          The model of the tab page container.
   * @param tabTitle
   *          The title of the tab.
   * @param tabId
   *          The Id of the tab.
   */
  public static void createTab(XMultiComponentFactory xMCF, XComponentContext context, XTabPageContainerModel model,
      String tabTitle, short tabId)
  {
    XTabPageModel tabPageModel = model.createTabPage(tabId); // 0 is not valid
    tabPageModel.setTitle(tabTitle);

    XTabPage xTabPage = null;
    try
    {
      Object tabPageService = UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_TAB_UNO_CONTROL_TAB_PAGE,
          xMCF, context);
      xTabPage = UNO.XTabPage(tabPageService);
      UNO.XControl(xTabPage).setModel(UNO.XControlModel(tabPageModel));
      model.insertByIndex(model.getCount(), tabPageModel);
    } catch (IllegalArgumentException | IndexOutOfBoundsException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Create a tab container.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @return A new tab container.
   */
  public static XControl createTabPageContainer(XMultiComponentFactory xMCF, XComponentContext context)
  {
    Object tabPageContainerModel = UnoComponent
        .createComponentWithContext(UnoComponent.CSS_AWT_TAB_UNO_CONTROL_TAB_PAGE_CONTAINER_MODEL, xMCF, context);

    Object tabControlContainer = UnoComponent
        .createComponentWithContext(UnoComponent.CSS_AWT_TAB_UNO_CONTROL_TAB_PAGE_CONTAINER, xMCF, context);
    XControl tabControl = UNO.XControl(tabControlContainer);

    XControlModel xControlModel = UNO.XControlModel(tabPageContainerModel);
    tabControl.setModel(xControlModel);
    return tabControl;
  }

  /**
   * Create a label with a hyper link.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new label with a hyper link.
   */
  public static XControl createHyperLinkLabel(XMultiComponentFactory xMCF, XComponentContext context, Rectangle size,
      SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_FIXED_HYPER_LINK, props, size);
  }

  /**
   * Create a check box with an item listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param itemListener
   *          The item listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new check box.
   */
  public static XControl createCheckBox(XMultiComponentFactory xMCF, XComponentContext context,
      AbstractItemListener itemListener, Rectangle size, SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_CHECK_BOX, props, size);
    UNO.XCheckBox(ctrl).addItemListener(itemListener);
    return ctrl;
  }

  /**
   * Create a dialog.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new dialog.
   */
  public static XControl createDialog(XMultiComponentFactory xMCF, XComponentContext context, Rectangle size,
      SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_DIALOG, props, size);
  }

  /**
   * Create a control container.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new control container.
   */
  public static XControl createControlContainer(XMultiComponentFactory xMCF, XComponentContext context, Rectangle size,
      SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_CONTAINER, props, size);
  }

  /**
   * Creates any control.
   *
   * @param xMCF
   *          The factory.
   * @param xContext
   *          The context.
   * @param type
   *          The type of control (FQDN), eg. com.sun.star.awt.UnoControlFixedText.
   * @param props
   *          Some additional properties of the control.
   * @param rectangle
   *          The size of the control.
   * @return A control or null.
   */
  public static XControl createControl(XMultiComponentFactory xMCF, XComponentContext xContext, String type,
      SortedMap<String, Object> props, Rectangle rectangle)
  {
    try
    {
      XControl control = UNO.XControl(UnoComponent.createComponentWithContext(type, xMCF, xContext));
      XControlModel controlModel = UnoRuntime.queryInterface(XControlModel.class,
          UnoComponent.createComponentWithContext(type + "Model", xMCF, xContext));
      control.setModel(controlModel);
      XMultiPropertySet properties = UNO.XMultiPropertySet(control.getModel());
      if (props != null && props.size() > 0)
      {
        properties.setPropertyValues(props.keySet().toArray(new String[props.size()]),
            props.values().toArray(new Object[props.size()]));
      }
      XWindow controlWindow = UNO.XWindow(control);
      setWindowPosSize(controlWindow, rectangle);
      return control;
    } catch (com.sun.star.uno.Exception ex)
    {
      LOGGER.debug("", ex);
      return null;
    }
  }

  /**
   * Change size and position of a window/control.
   *
   * @param window
   *          The window/control to change.
   * @param posSize
   *          The new position and size.
   */
  public static void setWindowPosSize(XWindow window, Rectangle posSize)
  {
    window.setPosSize(posSize.X, posSize.Y, posSize.Width, posSize.Height, PosSize.POSSIZE);
  }

}
