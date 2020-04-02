package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;

/**
 * Factory for creating UNO control elements.
 */
public class GuiFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(GuiFactory.class);

  private GuiFactory()
  {
    // nothing to initialize.
  }

  /**
   * Create a window without decoration. The window can be used as content of a
   * sidebar panel.
   *
   * @param toolkit
   *          The toolkit to create the window.
   * @param parentWindow
   *          The parent window.
   * @return A new {@link XWindowPeer}.
   */
  public static XWindowPeer createWindow(XToolkit toolkit, XWindowPeer parentWindow)
  {
    WindowDescriptor aWindow = new WindowDescriptor();
    aWindow.Type = WindowClass.CONTAINER;
    aWindow.WindowServiceName = "";
    aWindow.Parent = parentWindow;
    aWindow.ParentIndex = -1;
    aWindow.Bounds = new Rectangle(0, 0, 10, 10);
    aWindow.WindowAttributes =
      WindowAttribute.SIZEABLE | WindowAttribute.MOVEABLE
        | WindowAttribute.NODECORATION;
    return toolkit.createWindow(aWindow);
  }

  /**
   * Create a button with label and action listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
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
  @SuppressWarnings("squid:S00107")
  public static XControl createButton(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String label, AbstractActionListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    XControl buttonCtrl =
      createControl(xMCF, context, toolkit, windowPeer,
            "com.sun.star.awt.UnoControlButton", props, size);
    XButton button = UnoRuntime.queryInterface(XButton.class, buttonCtrl);
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
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param text
   *          The text.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new text field.
   */
  public static XControl createTextfield(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String text, Rectangle size, SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put("MultiLine", false);
    props.put("ReadOnly", false);
    props.put("VScroll", false);

    XControl buttonCtrl =
      createControl(xMCF, context, toolkit, windowPeer,
            "com.sun.star.awt.UnoControlEdit", props, size);

    XTextComponent txt = UnoRuntime.queryInterface(XTextComponent.class, buttonCtrl);
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
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param text
   *          The text of the label.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new label.
   */
  public static XControl createLabel(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, String text, Rectangle size,
      SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put("MultiLine", true);

    XControl buttonCtrl = createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlFixedText", props, size);

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
   * @throws Exception
   *           Model can't be created.
   */
  public static XMutableTreeDataModel createTreeModel(XMultiComponentFactory xMCF,
      XComponentContext context) throws Exception
  {
    return UnoRuntime.queryInterface(XMutableTreeDataModel.class,
      xMCF.createInstanceWithContext("com.sun.star.awt.tree.MutableTreeDataModel",
        context));
  }

  /**
   * Create a tree control with a given data model
   * {@link #createTreeModel(XMultiComponentFactory, XComponentContext)}.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param dataModel
   *          The data model.
   * @return A new tree control.
   */
  public static XControl createTree(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, XMutableTreeDataModel dataModel)
  {
    SortedMap<String, Object> props = new TreeMap<>();
    props.put("DataModel", dataModel);
    return GuiFactory.createControl(xMCF, context, toolkit, windowPeer,
            "com.sun.star.awt.tree.TreeControl", props, new Rectangle(0, 0,
            400, 400));
  }

  /**
   * Create a combo box with options and item listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
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
  @SuppressWarnings("squid:S00107")
  public static XControl createCombobox(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text,
      AbstractItemListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    XControl ctrl =
        createControl(xMCF, context, toolkit, windowPeer,
            "com.sun.star.awt.UnoControlComboBox", props, size);
    XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, ctrl);
    tf.setText(text);
    XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, ctrl);
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
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
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
  @SuppressWarnings("squid:S00107")
  public static XControl createNumericField(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, int value, XTextListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlNumericField", props, size);
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
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
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
  @SuppressWarnings("squid:S00107")
  public static XControl createSpinField(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      int value, AbstractTextListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put("Spin", Boolean.TRUE);

    return createNumericField(xMCF, context, toolkit, windowPeer, value, listener, size, props);
  }

  /**
   * Create a horizontal line.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param size
   *          The size.
   * @param props
   *          Some additional properties.
   * @return A new horizontal line.
   */
  public static XControl createHLine(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, Rectangle size, SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlFixedLine",
        props, size);
  }

  /**
   * Create a list box with an item listener.
   *
   * @param xMCF
   *          The factory.
   * @param context
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param listener
   *          The item listener.
   * @param size
   *          The size of the control.
   * @param props
   *          Some additional properties.
   * @return A new button.
   */
  public static XControl createListBox(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      AbstractItemListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlListBox",
        props, size);
    UNO.XListBox(ctrl).addItemListener(listener);

    return ctrl;
  }

  /**
   * Creates any control.
   *
   * @param xMCF
   *          The factory.
   * @param xContext
   *          The context.
   * @param toolkit
   *          The toolkit.
   * @param windowPeer
   *          The window in which the control is located.
   * @param type
   *          The type of control (FQDN), eg.
   *          com.sun.star.awt.UnoControlFixedText.
   * @param props
   *          Some additional properties of the control.
   * @param rectangle
   *          The size of the control.
   * @return A control or null.
   */
  public static XControl createControl(XMultiComponentFactory xMCF,
      XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer,
      String type, SortedMap<String, Object> props, Rectangle rectangle)
  {
    try
    {
      XControl control =
        UnoRuntime.queryInterface(XControl.class,
          xMCF.createInstanceWithContext(type, xContext));
      XControlModel controlModel =
        UnoRuntime.queryInterface(XControlModel.class,
          xMCF.createInstanceWithContext(type + "Model", xContext));
      control.setModel(controlModel);
      XMultiPropertySet properties =
        UnoRuntime.queryInterface(XMultiPropertySet.class, control.getModel());
      if (props != null && props.size() > 0)
      {
        properties.setPropertyValues(
          props.keySet().toArray(new String[props.size()]),
          props.values().toArray(new Object[props.size()]));
      }
      control.createPeer(toolkit, windowPeer);
      XWindow controlWindow = UnoRuntime.queryInterface(XWindow.class, control);
      setWindowPosSize(controlWindow, rectangle);
      return control;
    }
    catch (com.sun.star.uno.Exception ex)
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
    window.setPosSize(posSize.X, posSize.Y, posSize.Width, posSize.Height,
        PosSize.POSSIZE);
  }

}
