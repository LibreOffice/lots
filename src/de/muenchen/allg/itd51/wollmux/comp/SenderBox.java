/*
 * Dateiname: ToolbarComboBox.java
 * Projekt  : WollMux
 * Funktion : Controller für eine in der Toolbar eingebettete 
 *            ComboBox zur Auswahl des aktuellen Absenders.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 31.10.2005 | LUT | Erstellung
 * -------------------------------------------------------------------

 *
 * @author D-HAIII 5.1 Christoph Lutz
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.comp;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.VclWindowPeerAttribute;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.frame.XToolbarController;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.Exception;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XUpdatable;

import de.muenchen.allg.afid.MsgBox;
import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.XSenderBox;

/**
 * @author lut
 * 
 * Diese Klasse implementiert einen com.sun.star.frame.ToolbarController, mit
 * dem eine ComboBox in eine Toolbar eingebettet werden kann.
 */
public class SenderBox extends ComponentBase implements XServiceInfo,
    XSenderBox
{

  protected static final String __serviceName = "de.muenchen.allg.itd51.wollmux.SenderBox";

  private XFrame frame;

  private String commandURL;

  private UnoService serviceManager;

  /**
   * Das Feld cBox enthält den ComboBox-Service für den weiteren Zugriff auf die
   * Box.
   */
  private static UnoService cBox;

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getImplementationName()
   */
  public String getImplementationName()
  {
    return getClass().getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
  public boolean supportsService(String arg0)
  {
    return arg0.equals(__serviceName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  public String[] getSupportedServiceNames()
  {
    return new String[] { __serviceName };
  }

  public static XSingleServiceFactory __getServiceFactory(String implName,
      XMultiServiceFactory multiFactory, XRegistryKey regKey)
  {
    XSingleServiceFactory xSingleServiceFactory = null;
    if (implName.equals(SenderBox.class.getName()))
    {
      xSingleServiceFactory = FactoryHelper.getServiceFactory(
          SenderBox.class,
          SenderBox.__serviceName,
          multiFactory,
          regKey);
    }
    return xSingleServiceFactory;
  }

  public static boolean __writeRegistryServiceInfo(XRegistryKey regKey)
  {
    return FactoryHelper.writeRegistryServiceInfo(
        SenderBox.class.getName(),
        __serviceName,
        regKey);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XTypeProvider#getTypes()
   */
  public Type[] getTypes()
  {
    return new Type[] {
                       new Type(XServiceInfo.class),
                       new Type(XTypeProvider.class),
                       new Type(XServiceInfo.class),
                       new Type(XStatusListener.class),
                       new Type(XInitialization.class),
                       new Type(XUpdatable.class),
                       new Type(XToolbarController.class) };
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#createItemWindow(com.sun.star.awt.XWindow)
   */
  public XWindow createItemWindow2(XWindow xWindow)
  {
    XMultiServiceFactory xMSF = serviceManager.xMultiServiceFactory();
    try
    {
      // get XWindowPeer
      XWindowPeer xWinPeer = (XWindowPeer) UnoRuntime.queryInterface(
          XWindowPeer.class,
          xWindow);

      // create Toolkit-Service
      Object o = xMSF.createInstance("com.sun.star.awt.Toolkit");
      XToolkit xToolkit = (XToolkit) UnoRuntime.queryInterface(
          XToolkit.class,
          o);

      // create WindowDescriptor
      WindowDescriptor wd = new WindowDescriptor();
      wd.Type = WindowClass.SIMPLE;
      wd.Parent = xWinPeer;
      wd.Bounds = new Rectangle(0, 0, 100, 100);
      wd.ParentIndex = -1;
      wd.WindowAttributes = WindowAttribute.SHOW;
      wd.WindowServiceName = "combobox";

      // create ComboBox
      XWindowPeer cBox_xWinPeer = xToolkit.createWindow(wd);
      XComboBox cBox_xComboBox = (XComboBox) UnoRuntime.queryInterface(
          XComboBox.class,
          cBox_xWinPeer);
      XWindow cBox_xWindow = (XWindow) UnoRuntime.queryInterface(
          XWindow.class,
          cBox_xWinPeer);

      // add some elements
      cBox_xComboBox.addItems(new String[] {
                                            "test",
                                            "foo",
                                            "bar",
                                            "test2",
                                            "foo2",
                                            "bar2" }, (short) 0);

      return cBox_xWindow;
    }
    catch (com.sun.star.uno.Exception e)
    {
      return null;
    }
  }

  public XWindow createItemWindow(XWindow xwin)
  {
    if (cBox == null)
    {
      try
      {
        UnoService toolkit = serviceManager.create("com.sun.star.awt.Toolkit");

        WindowDescriptor wd = new WindowDescriptor();
        wd.Type = WindowClass.SIMPLE;
        wd.Parent = UNO.XWindowPeer(xwin);
        wd.Bounds = new Rectangle(0, 0, 200, 30);
        wd.ParentIndex = -1;
        wd.WindowAttributes = WindowAttribute.SHOW
                              | VclWindowPeerAttribute.DROPDOWN;
        wd.WindowServiceName = "ComboBox";
        cBox = new UnoService(toolkit.xToolkit().createWindow(wd));
      }
      catch (Exception e)
      {
        MsgBox.simple("Exception", "in ToolbarComboBox.createItemWindow()");
      }
    }
    return cBox.xWindow();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.frame.XToolbarComboBox#setText(java.lang.String)
   */
  public void setText(String text)
  {
    if (cBox != null && cBox.xTextComponent() != null)
    {
      cBox.xTextComponent().setText(text);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.frame.XToolbarComboBox#setDropDownLineCount(short)
   */
  public void setDropDownLineCount(short i)
  {
    if (cBox != null && cBox.xComboBox() != null)
    {
      cBox.xComboBox().setDropDownLineCount(i);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.frame.XToolbarComboBox#addItems(java.lang.String[], short)
   */
  public void addItems(String[] items, short pos)
  {
    if (cBox != null && cBox.xComboBox() != null)
    {
      cBox.xComboBox().addItems(items, pos);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.util.XUpdatable#update()
   */
  public void update()
  {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#click()
   */
  public void click()
  {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#createPopupWindow()
   */
  public XWindow createPopupWindow()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#doubleClick()
   */
  public void doubleClick()
  {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#execute(short)
   */
  public void execute(short arg0)
  {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XInitialization#initialize(java.lang.Object[])
   */
  public void initialize(Object[] args) throws Exception
  {
    frame = null;
    commandURL = null;
    serviceManager = null;
    for (int i = 0; i < args.length; i++)
    {
      if (args[i] instanceof PropertyValue)
      {
        String arg = ((PropertyValue) args[i]).Name;
        Object val = ((PropertyValue) args[i]).Value;

        if (arg.equals("Frame"))
        {
          frame = (XFrame) UnoRuntime.queryInterface(XFrame.class, val);
        }
        if (arg.equals("CommandURL"))
        {
          commandURL = (String) val;
        }
        if (arg.equals("ServiceManager"))
        {
          serviceManager = new UnoService(val);
        }
      }
    }
    // check if arguments where correct and complete
    if (frame == null || commandURL == null || serviceManager == null)
    {
      String str = "";
      for (int i = 0; i < args.length; i++)
      {
        str += "     " + args[i].toString() + ",\n";
      }
      MsgBox.simple("ToolbarComboBox: Error", "InvalidArguments: initialize(\n"
                                              + str
                                              + ")\n");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XStatusListener#statusChanged(com.sun.star.frame.FeatureStateEvent)
   */
  public void statusChanged(FeatureStateEvent arg0)
  {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
   */
  public void disposing(EventObject arg0)
  {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) throws java.lang.Exception
  {
    XComponentContext ctx = com.sun.star.comp.helper.Bootstrap.bootstrap();

    UnoService desktop = UnoService.createWithContext(
        "com.sun.star.frame.Desktop",
        ctx);

    String sToolBar = "private:resource/toolbar/UITest";
    String sMyCommand = "macro://./Standard.UITest.DestroyToolbar()";

    UnoService doc = new UnoService(desktop.xDesktop().getCurrentComponent());
    UnoService frame = new UnoService(doc.xModel().getCurrentController()
        .getFrame());
    UnoService layoutmgr = frame.getPropertyValue("LayoutManager");

    // Check if the toolbar still exists and destroy it
    if (layoutmgr.xLayoutManager().getElement(sToolBar) != null)
    {
      layoutmgr.xLayoutManager().destroyElement(sToolBar);
    }
    layoutmgr.xLayoutManager().createElement(sToolBar);

    // Create a new toolbar
    UnoService toolbar = new UnoService(layoutmgr.xLayoutManager().getElement(
        sToolBar));
    toolbar.setPropertyValue("Persistent", Boolean.FALSE);

    // get the module identifier
    UnoService mm = UnoService.createWithContext(
        "com.sun.star.frame.ModuleManager",
        ctx);
    String moduleIdent = mm.xModuleManager().identify(doc.xModel());

    // Create the toolbar settings
    UnoService oModuleCfgMgrSupplier = UnoService.createWithContext(
        "com.sun.star.ui.ModuleUIConfigurationManagerSupplier",
        ctx);
    UnoService oToolBarSettings = new UnoService(oModuleCfgMgrSupplier
        .xModuleUIConfigurationManagerSupplier().getUIConfigurationManager(
            moduleIdent).createSettings());

    // Add the menu item to the settings
    PropertyValue[] oToolbarItem;
    oToolbarItem = createToolbarItem(".uno:NewDoc", "New");
    oToolBarSettings.xIndexContainer().insertByIndex(0, oToolbarItem);
    oToolbarItem = createToolbarItem("wollmux:SenderBox", "ComboBox");
    oToolBarSettings.xIndexContainer().insertByIndex(1, oToolbarItem);

    // set the settings
    toolbar.xUIElementSettings().setSettings(oToolBarSettings.xIndexAccess());

    // default-setting for the combobox:
    UnoService tcb = UnoService.createWithContext(
        "de.muenchen.frame.ToolbarComboBox",
        ctx);
    XSenderBox xTcb = (XSenderBox) UnoRuntime.queryInterface(
        XSenderBox.class,
        tcb.getObject());
    xTcb.addItems(new String[] {
                                "test1",
                                "foo1",
                                "bar1",
                                "test2",
                                "foo2",
                                "bar2",
                                "test3",
                                "foo3",
                                "bar3" }, (short) 0);
    xTcb.setText("Hallo");
    xTcb.setDropDownLineCount((short) 5);

    System.exit(0);
  }

  private static PropertyValue[] createToolbarItem(String command, String label)
  {
    PropertyValue[] aToolbarItem = new PropertyValue[2];
    aToolbarItem[0] = new PropertyValue();
    aToolbarItem[0].Name = "CommandURL";
    aToolbarItem[0].Value = command;
    aToolbarItem[1] = new PropertyValue();
    aToolbarItem[1].Name = "Label";
    aToolbarItem[1].Value = label;
    return aToolbarItem;
  }
}
