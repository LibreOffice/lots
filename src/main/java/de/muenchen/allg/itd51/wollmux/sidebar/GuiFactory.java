package de.muenchen.allg.itd51.wollmux.sidebar;

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
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.XMultiPropertySet;
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
   * @return A new text field.
   */
  public static XControl createTextfield(XMultiComponentFactory xMCF, XComponentContext context, String text,
      Rectangle size, SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put("MultiLine", false);
    props.put("ReadOnly", false);
    props.put("VScroll", false);

    XControl buttonCtrl = createControl(xMCF, context, UnoComponent.CSS_AWT_UNO_CONTROL_EDIT, props, size);

    XTextComponent txt = UNO.XTextComponent(buttonCtrl);
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
    props.put("MultiLine", true);

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
    props.put("Spin", Boolean.TRUE);

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
