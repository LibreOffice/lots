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
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;

/**
 * Die Factory enthält Hilfsfunktionen zum einfacheren Erzeugen von
 * UNO-Steuerelementen.
 *
 */
public class GuiFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(GuiFactory.class);

  private GuiFactory()
  {
  }

  /**
   * Erzeugt ein Fenster ohne Dekorationen. Das Fenster kann als Inhalt eines
   * Sidebar-Panels verwendet werden.
   *
   * @param toolkit
   * @param parentWindow
   * @return
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
   * Erzeugt einen Button mit Label und ActionListener.
   *
   * @param xMCF
   * @param context
   * @param toolkit
   * @param windowPeer
   * @param label
   * @param listener
   * @param size
   * @param props
   * @return
   */
  @SuppressWarnings("squid:S00107")
  public static XControl createButton(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String label, XActionListener listener, Rectangle size, SortedMap<String, Object> props)
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
   * Erzeugt ein Texteingabefeld.
   *
   * @param xMCF
   * @param context
   * @param toolkit
   * @param windowPeer
   * @param text
   * @param size
   * @param props
   * @return
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
   * Erzeugt ein Label.
   *
   * @param xMCF
   * @param context
   * @param toolkit
   * @param windowPeer
   * @param text
   * @param size
   * @param props
   * @return
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
   * Erzeugt ein Datenmodell für einen Baum-Steuerelement.
   *
   * @param xMCF
   * @param context
   * @return
   * @throws Exception
   */
  public static XMutableTreeDataModel createTreeModel(XMultiComponentFactory xMCF,
      XComponentContext context) throws Exception
  {
    return UnoRuntime.queryInterface(XMutableTreeDataModel.class,
      xMCF.createInstanceWithContext("com.sun.star.awt.tree.MutableTreeDataModel",
        context));
  }

  /**
   * Erzeugt ein Baum-Steuerelement mit einem vorgegebenen Datenmodell.
   * Das Datenmodel kann mit {@link #createTreeModel(XMultiComponentFactory, XComponentContext)}
   * erzeugt werden.
   *
   * @param xMCF
   * @param context
   * @param toolkit
   * @param windowPeer
   * @param dataModel
   * @return
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

  public static XControl createCombobox(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text,
      Rectangle size, SortedMap<String, Object> props)
  {
    XControl ctrl =
        createControl(xMCF, context, toolkit, windowPeer,
            "com.sun.star.awt.UnoControlComboBox", props, size);
    XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, ctrl);
    tf.setText(text);
    XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, ctrl);
    cmb.setDropDownLineCount((short) 10);

    return ctrl;
  }

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

  @SuppressWarnings("squid:S00107")
  public static XControl createSpinField(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, int value, XTextListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    if (props == null)
    {
      props = new TreeMap<>();
    }
    props.put("Spin", Boolean.TRUE);

    return createNumericField(xMCF, context, toolkit, windowPeer, value, listener, size, props);
  }

  public static XControl createHLine(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, Rectangle size, SortedMap<String, Object> props)
  {
    return createControl(xMCF, context, toolkit, windowPeer, "com.sun.star.awt.UnoControlFixedLine",
        props, size);
  }

  public static XControl createListBox(XMultiComponentFactory xMCF, XComponentContext context,
      XToolkit toolkit, XWindowPeer windowPeer, XItemListener listener, Rectangle size,
      SortedMap<String, Object> props)
  {
    XControl ctrl = createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlListBox",
        props, size);
    UNO.XListBox(ctrl).addItemListener(listener);

    return ctrl;
  }

  /**
   * Eine allgemeine Hilfsfunktion, mit der UNO-Steuerelemente erzeugt werden.
   *
   * @param xMCF
   * @param xContext
   * @param toolkit
   * @param windowPeer
   * @param type
   *          Klasse des Steuerelements, das erzeugt werden soll.
   * @param props
   * @param rectangle
   * @return
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
   * Ändert die Größe und Position eines Fensters.
   *
   * @param window
   * @param posSize
   */
  public static void setWindowPosSize(XWindow window, Rectangle posSize)
  {
    setWindowPosSize(window, posSize, 0, 0);
  }

  private static void setWindowPosSize(XWindow window, Rectangle posSize,
      int horizontalOffset, int verticalOffset)
  {
    window.setPosSize(posSize.X - horizontalOffset, posSize.Y - verticalOffset,
      posSize.Width, posSize.Height, PosSize.POSSIZE);
  }

}
