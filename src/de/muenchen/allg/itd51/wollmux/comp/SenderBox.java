/*
 * Dateiname: SenderBox.java
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
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.comp;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.VclWindowPeerAttribute;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XKeyListener;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.frame.XToolbarController;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
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
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.XSenderBox;

/**
 * Diese Klasse implementiert einen com.sun.star.frame.ToolbarController, mit
 * dem eine ComboBox in eine Toolbar eingebettet werden kann, die für die
 * Auswahl des aktuellen Absenders zuständig ist.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
/**
 * @author lut
 * 
 */
public class SenderBox extends ComponentBase implements XServiceInfo,
    XSenderBox, XItemListener, XActionListener, XTextListener, XKeyListener
{

  protected static final String __serviceName = "de.muenchen.allg.itd51.wollmux.SenderBox";

  private XFrame frame;

  private String commandURL;

  private UnoService serviceManager;

  /**
   * Das Feld cBox enthält den ComboBox-Service für den weiteren Zugriff auf die
   * Box.
   */
  private UnoService cBox;

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
        addItems(new String[] {
                               "test1",
                               "foo1",
                               "bar1",
                               "test2",
                               "foo2",
                               "bar2",
                               "test3",
                               "foo3",
                               "bar3" }, (short) 0);
        cBox.xComboBox().addActionListener(this);
        cBox.xComboBox().addItemListener(this);
        cBox.xComponent().addEventListener(this);
        cBox.xTextComponent().addTextListener(this);
        cBox.xWindow().addKeyListener(this);
        cBox.msgboxFeatures();
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
    Logger.debug2("SenderBox::update");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#click()
   */
  public void click()
  {
    Logger.debug2("SenderBox::click");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#createPopupWindow()
   */
  public XWindow createPopupWindow()
  {
    Logger.debug2("SenderBox::createPopupWindow");
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#doubleClick()
   */
  public void doubleClick()
  {
    Logger.debug2("SenderBox::doubleClick");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XToolbarController#execute(short)
   */
  public void execute(short arg0)
  {
    Logger.debug2("SenderBox::execute");
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
    Logger.debug2("SenderBox::statusChanged");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
   */
  public void disposing(EventObject source)
  {
    Logger.debug2("SenderBox::disposing");
    // EventListener deregistrieren.
    XComponent xCompo = UNO.XComponent(source.Source);
    if (xCompo != null) xCompo.removeEventListener(this);
  }

  public static void main(String[] args) throws java.lang.Exception
  {
    XComponentContext ctx = com.sun.star.comp.helper.Bootstrap.bootstrap();

    UnoService desktop = UnoService.createWithContext(
        "com.sun.star.frame.Desktop",
        ctx);

    String sToolBar = "private:resource/toolbar/UITest";

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

  /*
   * Empfängt eine Nachricht, wenn mit der Maus auf ein anderes als das aktive
   * Element geklickt wurde.
   * 
   * (non-Javadoc)
   * 
   * @see com.sun.star.awt.XItemListener#itemStateChanged(com.sun.star.awt.ItemEvent)
   */
  public void itemStateChanged(ItemEvent arg0)
  {
    Logger.debug2("SenderBox::itemStateChanged " + arg0.Selected);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.awt.XActionListener#actionPerformed(com.sun.star.awt.ActionEvent)
   */
  public void actionPerformed(ActionEvent arg0)
  {
    Logger.debug2("SenderBox::actionPerformed " + arg0.ActionCommand);
  }

  /*
   * Empfängt eine Nachricht wenn der Text in der ComboBox geändert wurde.
   * 
   * (non-Javadoc)
   * 
   * @see com.sun.star.awt.XTextListener#textChanged(com.sun.star.awt.TextEvent)
   */
  public void textChanged(TextEvent arg0)
  {
    Logger.debug2("SenderBox::textChanged " + arg0.dummy1);
  }

  public void keyPressed(KeyEvent arg0)
  {
    Logger.debug2("SenderBox::keyPressed" + arg0.KeyCode);
    if (arg0.KeyCode == Key.RETURN) Logger.debug2("Return pressed!");
  }

  public void keyReleased(KeyEvent arg0)
  {
    // do nothing
  }
}
