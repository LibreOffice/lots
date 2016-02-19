package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.SortedMap;
import java.util.TreeMap;

import com.sun.star.awt.InvalidateStyle;
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
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Die Factory enthält Hilfsfunktionen zum einfacheren Erzeugen von 
 * UNO-Steuerelementen. 
 * 
 */
public class GuiFactory
{
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
   * @return
   * @throws com.sun.star.uno.Exception
   */
  public static XControl createButton(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String label, XActionListener listener, Rectangle size)
      throws com.sun.star.uno.Exception
  {
    XControl buttonCtrl =
      createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlButton", null, null, size);
    XButton button = UnoRuntime.queryInterface(XButton.class, buttonCtrl);
    button.setLabel(label);
    button.addActionListener(listener);
    return buttonCtrl;
  }

  /**
   * Erzeugt die Senderbox für die WollMux-Sidebar.
   * 
   * @param xMCF
   * @param context
   * @param toolkit
   * @param windowPeer
   * @param label
   * @param listener
   * @param size
   * @return
   * @throws com.sun.star.uno.Exception
   */
  public static XControl createSenderbox(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String label, XActionListener listener, Rectangle size)
      throws com.sun.star.uno.Exception
  {
    XControl buttonCtrl =
      createButton(xMCF, context, toolkit, windowPeer, label, listener, size);
    XControlModel model = buttonCtrl.getModel();
    XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, model);
    props.setPropertyValue("FocusOnClick", false);
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
   * @return
   * @throws com.sun.star.uno.Exception
   */
  public static XControl createTextfield(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer,
      String text, Rectangle size) throws com.sun.star.uno.Exception
  {
    XControl buttonCtrl =
      createControl(xMCF, context, toolkit, windowPeer,
        "com.sun.star.awt.UnoControlEdit", null, null, size);
    
    XTextComponent txt = UnoRuntime.queryInterface(XTextComponent.class, buttonCtrl);
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
   * @throws Exception
   */
  public static XControl createTree(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, XMutableTreeDataModel dataModel) throws Exception
  {
    XControl treeCtrl =
        GuiFactory.createControl(xMCF, context, toolkit, windowPeer,
          "com.sun.star.awt.tree.TreeControl", null, null, new Rectangle(0, 0,
            400, 400));

    XPropertySet props =
        UnoRuntime.queryInterface(XPropertySet.class, treeCtrl.getModel());
    props.setPropertyValue("DataModel", dataModel);
    
    return treeCtrl;
  }
  
  public static XControl createCombobox(XMultiComponentFactory xMCF,
      XComponentContext context, XToolkit toolkit, XWindowPeer windowPeer, String text, Rectangle size)
  {
    XControl ctrl =
        createControl(xMCF, context, toolkit, windowPeer,
          "com.sun.star.awt.UnoControlComboBox", null, null, size);
    XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, ctrl);
    tf.setText(text);
    XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, ctrl);
    cmb.setDropDownLineCount((short) 10);
    
    try
    {
      XControlModel model = ctrl.getModel();
//        UnoRuntime.queryInterface(XControlModel.class,
//          xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlComboBoxModel", context));
      
      XPropertySet props =
          UnoRuntime.queryInterface(XPropertySet.class, model);

      props.setPropertyValue("Dropdown", Boolean.TRUE);
      //props.setPropertyValue("ReadOnly", Boolean.TRUE);
      props.setPropertyValue("Autocomplete", Boolean.FALSE);
      props.setPropertyValue("HideInactiveSelection", Boolean.TRUE);
      
      ctrl.setModel(model);
      ctrl.getPeer().invalidate(InvalidateStyle.UPDATE);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    
    return ctrl;
  }

  /**
   * Eine allgemeine Hilfsfunktion, mit der UNO-Steuerelemente erzeugt werden.
   *  
   * @param xMCF
   * @param xContext
   * @param toolkit
   * @param windowPeer
   * @param type Klasse des Steuerelements, das erzeugt werden soll.
   * @param propNames
   * @param propValues
   * @param rectangle
   * @return
   */
  public static XControl createControl(XMultiComponentFactory xMCF,
      XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer,
      String type, String[] propNames, Object[] propValues, Rectangle rectangle)
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
      SortedMap<String, Object> props = new TreeMap<String, Object>();
      if (type.equals("com.sun.star.awt.UnoControlImageControl"))
      {
        props.put("Border", (short) 0);
      }
      else if (type.equals("com.sun.star.awt.UnoControlEdit"))
      {
        props.put("MultiLine", false);
        props.put("ReadOnly", false);
        props.put("VScroll", false);
      }
      if (propNames != null)
      {
        for (int i = 0; i < propNames.length; i++)
        {
          props.put(propNames[i], propValues[i]);
        }
      }
      if (props.size() > 0)
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
      Logger.debug(ex);
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
