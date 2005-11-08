package de.muenchen.allg.itd51.wollmux.oooui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XIndexContainer;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.ui.XUIConfigurationPersistence;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementSettings;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.xml.dom.XDocument;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * TODO: add important checks, where the "null" is possible; e.g. calls to
 * "topMenu.get("MENU")", where the MENU value can be unavailable
 * 
 * @author GOLOVKO
 * 
 */

public class MenuList
{
  protected static int BUTTON = 1;
  protected static int MENU = 1;
  private Hashtable htIdToMenu = new Hashtable();
  private List topLevelMenues = new ArrayList();
  // the variables from the running OO
  private XComponentContext mxRemoteContext = null;
  private static XComponentContext testmxRemoteContext = null;
  private XMultiComponentFactory mxRemoteServiceManager = null;
  private static XMultiComponentFactory testmxRemoteServiceManager = null;
  private static XDesktop xDesktop;
  private static XModel xModel;
  private static XController xController;
  private static XFrame xFrame;
  private XLayoutManager xLayoutManager;
  private XUIConfigurationManager uiConfigManager;
  private XModuleUIConfigurationManagerSupplier xCmSupplier;

  
  String sMenuBar = "private:resource/menubar/menubar";
  // TODO: delete fileMenus as soon as the problem with empty entries in the submenus is solved
  private PropertyValue[] fileMenus;
  private String lastCommandUrl="wollmux:myaction";
  private String PREFIX = "WollMux:";
  private XIndexContainer oMenuBarSettings;
  
  /**
   * Der Konstruktor erzeugt eine neue MenuList aus einer gegebenen
   * Konfiguration.
   * 
   * @param root
   *          Wurzel des Konfigurationsbaumes der Konfigurationsdatei.
   * @throws Exception
   */
  protected MenuList(ConfigThingy root) throws Exception
  {
    initConnection();
    // 0. read in the names of all top-level menues.
    Iterator mlIter = root.query("Menueleiste").getFirstChild().iterator();
    while (mlIter.hasNext())
    {
      ConfigThingy topMenu = (ConfigThingy) mlIter.next();
      topLevelMenues.add(topMenu);
    }

    List menues = new ArrayList();
    // 1. use .query() to get all Menues fragments

    Iterator menuesIter = root.query("Menues").iterator();
    // 1.1 iterate over "Menues"
    while (menuesIter.hasNext())
    {
      // each element is "Menues(...)"
      ConfigThingy menue = (ConfigThingy) menuesIter.next();
      Iterator lhmVorlageIter = menue.iterator();
      // htIdToMenu.put()
      // 1.2 iterate over "vorlagen" inside of the single menu
      while (lhmVorlageIter.hasNext())
      {
        // each element smthng like "LHMVorlagen(...)"
        ConfigThingy lhmVorlage = (ConfigThingy) lhmVorlageIter.next();
        menues = getMenueItems(lhmVorlage);
        // 1.3 put something like "LHMVorlagen"=> <array of Menu> into hash for sake of uniquness
        htIdToMenu.put(lhmVorlage.getName(), menues);
      }
    }

    
    
    
    // 2. init objects required for the creation of the menu elements
    XUIElement oMenuBar = xLayoutManager.getElement(sMenuBar);
    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
            XPropertySet.class, oMenuBar);
    props.setPropertyValue("Persistent", new Boolean(true));
    XUIElementSettings xoMenuBarSettings = (XUIElementSettings) UnoRuntime
            .queryInterface(XUIElementSettings.class, oMenuBar);
    oMenuBarSettings = (XIndexContainer) UnoRuntime
                .queryInterface(XIndexContainer.class, xoMenuBarSettings
                        .getSettings(true));
    fileMenus = (PropertyValue[]) UnoRuntime.queryInterface(PropertyValue[].class,
              oMenuBarSettings.getByIndex(7));
    fileMenus = LimuxHelper.setProperty(fileMenus,"ItemDescriptorContainer",null);   
    
    removeMenues(PREFIX);
    for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();
      
//      int menuIndex = findMenu(PREFIX+LimuxHelper.getProperty(element,"MENU"));
//      if (menuIndex!=-1) oMenuBarSettings.removeByIndex(menuIndex);
      
      PropertyValue[] topMenu = createMenuTree(element);
      //TODO: check if insertion at POSITION is possible 
      oMenuBarSettings.insertByIndex(Integer.parseInt(LimuxHelper.getProperty(element,"POSITION")), topMenu);
      xoMenuBarSettings.setSettings(oMenuBarSettings);
    }

    
    XUIConfigurationPersistence xUIConfigurationPersistence = (XUIConfigurationPersistence) UnoRuntime.queryInterface(XUIConfigurationPersistence.class, uiConfigManager);
    xUIConfigurationPersistence.store();
    
  }

  private void initConnection() throws Exception
  {

    mxRemoteServiceManager = getRemoteServiceManager();
    XPropertySet xps = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        xFrame);
    xLayoutManager = (XLayoutManager) UnoRuntime.queryInterface(
        XLayoutManager.class,
        xps.getPropertyValue("LayoutManager"));
    xCmSupplier = (XModuleUIConfigurationManagerSupplier) UnoRuntime
        .queryInterface(
            XModuleUIConfigurationManagerSupplier.class,
            mxRemoteServiceManager.createInstanceWithContext(
                "com.sun.star.ui.ModuleUIConfigurationManagerSupplier",
                mxRemoteContext));
    uiConfigManager = xCmSupplier
        .getUIConfigurationManager("com.sun.star.text.TextDocument");
  }
 
  /**
   * TODO: return smth like oMenuBarSettings:XIndexContainer
   * @param ct
   * @throws WrappedTargetException 
   * @throws IndexOutOfBoundsException 
   * @throws IllegalArgumentException 
   * @throws NodeNotFoundException 
   */
  protected PropertyValue[] createMenuTree(ConfigThingy ct) throws IllegalArgumentException, IndexOutOfBoundsException, WrappedTargetException, NodeNotFoundException {
    
    if (LimuxHelper.getProperty(ct,"TYPE").equals("menu")){
//      lastCommandUrl = PREFIX+getProperty(ct,"ACTION")+"menuAction";
      PropertyValue[] menu = createMenu(ct);
      List subCts = (List)htIdToMenu.get(LimuxHelper.getProperty(ct,"MENU"));
      PropertyValue[] menuItem = null;
      XIndexContainer container = uiConfigManager.createSettings();
//      container.insertByIndex(0,fileMenus);
      int counter = 0;
      // the menu was referenced, but not defined. Create subfolder and skip.
      // TODO: maybe container=null is required to create an empty submenu. 
      // TODO: special case, should the user be informed about absence of the menu definition? 
      if (subCts==null){
        menu = LimuxHelper.setProperty(menu,"ItemDescriptorContainer",container);
        return menu;
      }
      // iterate over entries in the submenu
      for (Iterator iter = subCts.iterator(); iter.hasNext();)
      {
        ConfigThingy subCt = (ConfigThingy) iter.next();
        menuItem = createMenuTree(subCt);
        
        if (counter==0){
          fileMenus = LimuxHelper.setProperty(fileMenus,"Label",LimuxHelper.getProperty(menuItem,"Label"));
          fileMenus = LimuxHelper.setProperty(fileMenus,"CommandURL",LimuxHelper.getProperty(menuItem,"CommandURL"));
          container.insertByIndex(counter,fileMenus);
        } else {
          container.insertByIndex(counter,menuItem);
        }
        menu = LimuxHelper.setProperty(menu,"ItemDescriptorContainer",container);
        counter++;
      }
      return menu;
    } else if (LimuxHelper.getProperty(ct,"TYPE").equals("button")){
//      lastCommandUrl = PREFIX+getProperty(ct,"ACTION");
      PropertyValue[] menuItem = createMenuItem(ct);
      return menuItem;
    } else if (LimuxHelper.getProperty(ct,"TYPE").equals("separator")){
      PropertyValue[] menuItem = createMenuItem(ct);
      return menuItem;
    } else {
      // should not occure
      Logger.error("The ConfigThingy of the specified file can't be processed: "+LimuxHelper.getProperty(ct,"TYPE"));
      return new PropertyValue[]{};
    }
    
  } 

  private List getMenueItems(ConfigThingy lhmVorlage)
      throws NodeNotFoundException
  {
    ArrayList results = new ArrayList();
    // an element is smth like "LHMVorlagen(...)"
    Iterator iter1 = lhmVorlage.getByChild("TYPE").iterator();
    while (iter1.hasNext())
    {
      // every element is something like "Element(...)" entry
      ConfigThingy child1 = (ConfigThingy) iter1.next();
      // Logger.debug(child1.get("LABEL").toString());
      results.add(child1);
    }
    return results;
  }

  /**
   * Testet die Funktionsweise der MenuList. Eine in url angegebene Konfigdatei
   * wird eingelesen und die dazugehörige MenuList erstellt. Anschliessend wird
   * die ausgegeben.
   * 
   * @param args
   *          url, dabei ist url die URL einer zu lesenden Config-Datei. Das
   *          Programm gibt die Liste der Textfragmente aus.
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException
  {
    try
    {
      if (args.length < 1)
      {
        System.out.println("USAGE: <url>");
        System.exit(0);
      }
      Logger.init(Logger.DEBUG);

      File cwd = new File(".");

      args[0] = args[0].replaceAll("\\\\", "/");
      ConfigThingy conf = new ConfigThingy(args[0], new URL(cwd.toURL(),
          args[0]));


      // the steps are accomplished in an external class
      // 1. standart steps, init variables like XDesktop etc.
      testmxRemoteServiceManager = testgetRemoteServiceManager();
      xDesktop = (XDesktop) UnoRuntime.queryInterface(
          XDesktop.class,
          testmxRemoteServiceManager.createInstanceWithContext(
              "com.sun.star.frame.Desktop",
              testmxRemoteContext));
      xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xDesktop
          .getCurrentComponent());
      xController = xModel.getCurrentController();
      xFrame = xController.getFrame();
      
      
      MenuList.generateMenues(conf, xFrame);
      
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    System.exit(0);
  }

  public static void generateMenues(ConfigThingy configThingy, XFrame xFrame){
    try
    {
      MenuList tfrags = new MenuList(configThingy);
      
    }
    catch (Exception e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private XMultiComponentFactory getRemoteServiceManager() throws BootstrapException
  {
    if (mxRemoteContext == null && mxRemoteServiceManager == null)
    {
      // get the remote office context. If necessary a new office
      // process is started
      mxRemoteContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
      System.out.println("Connected to a running office ...");
      mxRemoteServiceManager = mxRemoteContext.getServiceManager();
    }
    return mxRemoteServiceManager;
  }
  
  private static XMultiComponentFactory testgetRemoteServiceManager() throws BootstrapException
  {
    if (testmxRemoteContext == null && testmxRemoteServiceManager == null)
    {
      // get the remote office context. If necessary a new office
      // process is started
      testmxRemoteContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
      System.out.println("Connected to a running office ...");
      testmxRemoteServiceManager = testmxRemoteContext.getServiceManager();
    }
    return testmxRemoteServiceManager;
  }
  
  /**
   * The method create menu entry with a place for the submenu.
   * 
   * @param ct the element which describes the menu entry
   * @return the PropertyValue[] equivalent of the <b>ct</b>
   */
  private PropertyValue[] createMenu(ConfigThingy ct) {

    PropertyValue[] loadProps = LimuxHelper.setProperty(null,"CommandURL",LimuxHelper.getProperty(ct,"ACTION"));
    loadProps = LimuxHelper.setProperty(loadProps,"Label",setHotkey(
        LimuxHelper.getProperty(ct,"LABEL"),
        LimuxHelper.getProperty(ct,"HOTKEY")
        ));
    //TODO: (note) the following commandURL will not be unique among different submenues. The reason: 
    //      the same submenue can be bound to different parent menues.
    loadProps = LimuxHelper.setProperty(loadProps,"CommandURL",PREFIX+LimuxHelper.getProperty(ct,"MENU"));
    loadProps = LimuxHelper.setProperty(loadProps,"HelpURL","");


    return loadProps;
  }


  /**
   * modify the label to make hotkey underlined in the OO. The case of the Hotkey 
   * letter is ignored. The first occurence of the letter is used as a hotkey.
   * 
   * @param label
   * @param hotkey
   * @return
   */
  private String setHotkey(String label, String hotkey){
    String nLabel = label.replaceFirst(hotkey,"~"+hotkey);
    if (nLabel.equals(label)){
      nLabel = label.replaceFirst(hotkey.toLowerCase(),"~"+hotkey.toLowerCase());
      if (nLabel.equals(label)){
        nLabel = label.replaceFirst(hotkey.toUpperCase(),"~"+hotkey.toUpperCase());
      }
    } 
    
    return nLabel; 
    
  }

  
  private PropertyValue[] createMenuItem(ConfigThingy ct) throws NodeNotFoundException
  {
    Object value = null;
    int type = (LimuxHelper.getProperty(ct,"TYPE").equals("separator")) ? com.sun.star.ui.ItemType.SEPARATOR_LINE : com.sun.star.ui.ItemType.DEFAULT;
    
    value = (type==1) ? lastCommandUrl : PREFIX+LimuxHelper.getProperty(ct,"ACTION");
    PropertyValue[] loadProps = LimuxHelper.setProperty(null,"CommandURL",value);
    
    value = (type==1) ? "" : LimuxHelper.getProperty(ct,"LABEL");
    loadProps = LimuxHelper.setProperty(loadProps,"Label",setHotkey((String)value,LimuxHelper.getProperty(ct,"HOTKEY")));
    
    value = (type==1) ? new Integer(com.sun.star.ui.ItemType.SEPARATOR_LINE) : new Integer(com.sun.star.ui.ItemType.DEFAULT);
    loadProps = LimuxHelper.setProperty(loadProps,"Type",value);
    loadProps = LimuxHelper.setProperty(loadProps,"HelpURL","");


    return loadProps;
  }

  
  /**
   * 
   * @return an index of the dropped menu; it refers to the top-menu entries
   * @throws WrappedTargetException 
   * @throws IndexOutOfBoundsException 
   */
  private void removeMenues(String prefix) throws IndexOutOfBoundsException, WrappedTargetException{
    PropertyValue[] menu;
    ArrayList arr = new ArrayList();
    for (int i = oMenuBarSettings.getCount()-1; i >= 0 ; i--)
    {
      menu = (PropertyValue[]) UnoRuntime.queryInterface(PropertyValue[].class,
          oMenuBarSettings.getByIndex(i));
      String url = (String)LimuxHelper.getProperty(menu,"CommandURL");
      if (url.startsWith(prefix)){
        arr.add(new Integer(i));
//        oMenuBarSettings.removeByIndex(i);
//        System.out.println("found ["+prefix+"] at position ["+i+"]");
//        return i;
      }
    }
    
    for (Iterator iter = arr.iterator(); iter.hasNext();)
    {
      Integer i = (Integer) iter.next();
      oMenuBarSettings.removeByIndex(i.intValue());
      
    }
  }

}
