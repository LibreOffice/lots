/*
 * Dateiname: Event.java
 * Projekt  : WollMux
 * Funktion : The MenuList is responsible for the generation of the menus
 *            and toolbars
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer     | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.10.2005 | GOLOVKO | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Andrej Golovko
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.oooui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexContainer;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.ItemStyle;
import com.sun.star.ui.ItemType;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.ui.XUIConfigurationPersistence;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementSettings;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * The MenuList is responsible for the generation of the menus in the instance
 * of the OO. The generated menues are persistent and will be there after OO
 * restarts.
 * 
 * 
 * 
 * The generated menu entries will be available for all documents of the same
 * type. For example, a user works with a XTextDocument. A call to the
 * generateMenues() results in the new menues. Every new XTextDocument will have
 * these menues. But the menues are not there, if some document of another type
 * as XTextDocument is opened.
 * 
 * 
 * 
 * @author GOLOVKO
 * 
 */

public class MenuList
{
  protected static int BUTTON = 1;

  protected static int MENU = 2;

  private Hashtable htIdToMenu = new Hashtable();

  private List topLevelMenues = new ArrayList();

  // the variables from the running OO
  private XComponentContext mxRemoteContext = null;

  private XMultiComponentFactory mxRemoteServiceManager = null;

  private XLayoutManager xLayoutManager;

  private XUIConfigurationManager uiConfigManager;

  private XModuleUIConfigurationManagerSupplier xCmSupplier;

  private XFrame xFrame;

  private static String S_MENUBAR = "private:resource/menubar/menubar";

//  private static String S_TOOLBAR = "private:resource/toolbar/UITest";
  private static String S_TOOLBAR = "private:resource/toolbar/standardbar";
  

  private static String PREFIX = "wollmux:";
  
  private static String UNDEFINED = "<undefined>";
  private static String SEPARATOR = "-----------------";

  private XIndexContainer oMenuBarSettings;

  // TODO: delete fileMenus as soon as the problem with empty entries in the
  // submenus is solved
  private PropertyValue[] fileMenus;

  // TODO: the string is intended to be used for the creation of the separators.
  // TODO: Contains the last inserted MenuItem. Doesn't work for unknown
  // reasons.
  private String lastCommandUrl = "wollmux:myaction";
  

  /**
   * Der Konstruktor erzeugt eine neue MenuList aus einer gegebenen
   * Konfiguration.
   * 
   * @param root
   *          Wurzel des Konfigurationsbaumes der Konfigurationsdatei.
   * @throws Exception
   */
  protected MenuList(ConfigThingy root, XComponentContext xContext,
      XFrame xFrame, String targetUIStr) throws Exception
  {
    if (targetUIStr.equals(S_MENUBAR))
      _generateMenues(root, xContext, xFrame, targetUIStr);
    else if (targetUIStr.equals(S_TOOLBAR))
      _generateToolbar(root, xContext, xFrame, targetUIStr);

  }
  
  private void _generateToolbar(ConfigThingy root, XComponentContext xContext, XFrame xFrame, String targetUIStr) throws Exception, NodeNotFoundException, UnknownPropertyException, PropertyVetoException, IllegalArgumentException, WrappedTargetException, IndexOutOfBoundsException, com.sun.star.uno.Exception
  {
    this.xFrame = xFrame;
    this.mxRemoteContext = xContext;
    initConnection();
    // 0. read in the names of all top-level menues.
    // DIFF 1
//    Iterator mlIter = root.query("Menueleiste").getFirstChild().iterator();
    //>> getMenuIterator(); getToolbarIterator();
    Iterator mlIter = root.query("Symbolleisten").query("Briefkopfleiste").getFirstChild().iterator();
    // TODO: Briefkopfleiste sollte nicht hardcoded sein 
    // also added to replace similar call in the initConnection()
    uiConfigManager = xCmSupplier.getUIConfigurationManager("com.sun.star.script.BasicIDE");
   
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
        // 1.3 put something like "LHMVorlagen"=> <array of Menu> into hash for
        // sake of uniquness
        htIdToMenu.put(lhmVorlage.getName(), menues);
      }
    }

    // 2. init objects required for the creation of the menu elements
    //    oToolBar = oLayoutManager.getElement( sToolBar )
    // DIFF 2
    XUIElement oMenuBar = xLayoutManager.getElement(targetUIStr);
    if (oMenuBar!=null)
      xLayoutManager.destroyElement(targetUIStr);
    xLayoutManager.createElement(targetUIStr);
    xLayoutManager.showElement(targetUIStr);
    oMenuBar = xLayoutManager.getElement(targetUIStr);
    
    
    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        oMenuBar);
    props.setPropertyValue("Persistent", new Boolean(true));
    XUIElementSettings xoMenuBarSettings = (XUIElementSettings) UnoRuntime
        .queryInterface(XUIElementSettings.class, oMenuBar);
    oMenuBarSettings = (XIndexContainer) UnoRuntime.queryInterface(
        XIndexContainer.class,
        xoMenuBarSettings.getSettings(true));
//    DIFF 3 (the fileMenus is not required?)
    //>> getDummyMenuItem(); getDummyToolbarItem();
//    fileMenus = (PropertyValue[]) UnoRuntime.queryInterface(
//        PropertyValue[].class,
//        oMenuBarSettings.getByIndex(7));
//    fileMenus = LimuxHelper.setProperty(
//        fileMenus,
//        "ItemDescriptorContainer",
//        null);
  fileMenus = (PropertyValue[]) UnoRuntime.queryInterface(PropertyValue[].class,oMenuBarSettings.getByIndex(5));

    
    
    removeMenues(PREFIX);
    //DIFF
    int mCounter = 0;
    
    for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();

      //DIFF 4
      PropertyValue[] topMenu = createMenuTree(element);
//      topMenu = LimuxHelper.setProperty(fileMenus, "Label", LimuxHelper.getProperty(topMenu, "Label", UNDEFINED));
//      fileMenus = LimuxHelper.setProperty(fileMenus, "CommandURL",".uno:NewDoc");
      oMenuBarSettings.insertByIndex(mCounter, topMenu);
      mCounter++;
      
//      // TODO: check if insertion at POSITION is possible
//      // UNDO 2
//      Iterator iter2 = element.iterator();
//      int mCounter = 0;
//      while (iter2.hasNext())
//      {
//        ConfigThingy element2 = (ConfigThingy) iter2.next();
//        PropertyValue[] topMenu = createToolbarItem(element2);
//        fileMenus = LimuxHelper.setProperty(topMenu, "Label", LimuxHelper
//            .getProperty(topMenu, "Label", UNDEFINED));
//        oMenuBarSettings.insertByIndex(mCounter, fileMenus);
////        break;
//      }
      
      
      xoMenuBarSettings.setSettings(oMenuBarSettings);
    }

    // UNDO
    XUIConfigurationPersistence xUIConfigurationPersistence = (XUIConfigurationPersistence) UnoRuntime
        .queryInterface(XUIConfigurationPersistence.class, uiConfigManager);
    xUIConfigurationPersistence.store();
  }

  private void _generateMenues(ConfigThingy root, XComponentContext xContext, XFrame xFrame, String targetUIStr) throws Exception, NodeNotFoundException, UnknownPropertyException, PropertyVetoException, IllegalArgumentException, WrappedTargetException, IndexOutOfBoundsException, com.sun.star.uno.Exception
  {
    this.xFrame = xFrame;
    this.mxRemoteContext = xContext;
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
        // 1.3 put something like "LHMVorlagen"=> <array of Menu> into hash for
        // sake of uniquness
        htIdToMenu.put(lhmVorlage.getName(), menues);
      }
    }

    // 2. init objects required for the creation of the menu elements
    XUIElement oMenuBar = xLayoutManager.getElement(targetUIStr);
    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        oMenuBar);
    props.setPropertyValue("Persistent", new Boolean(true));
    XUIElementSettings xoMenuBarSettings = (XUIElementSettings) UnoRuntime
        .queryInterface(XUIElementSettings.class, oMenuBar);
    oMenuBarSettings = (XIndexContainer) UnoRuntime.queryInterface(
        XIndexContainer.class,
        xoMenuBarSettings.getSettings(true));
    fileMenus = (PropertyValue[]) UnoRuntime.queryInterface(
        PropertyValue[].class,
        oMenuBarSettings.getByIndex(7));
    fileMenus = LimuxHelper.setProperty(
        fileMenus,
        "ItemDescriptorContainer",
        null);

    removeMenues(PREFIX);
    for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();

      PropertyValue[] topMenu = createMenuTree(element);
      // TODO: check if insertion at POSITION is possible
      // UNDO 2
      oMenuBarSettings.insertByIndex(Integer.parseInt(LimuxHelper.getProperty(
          element,
          "POSITION", "0")), topMenu);
      
      
      xoMenuBarSettings.setSettings(oMenuBarSettings);
    }

    XUIConfigurationPersistence xUIConfigurationPersistence = (XUIConfigurationPersistence) UnoRuntime
        .queryInterface(XUIConfigurationPersistence.class, uiConfigManager);
    xUIConfigurationPersistence.store();
  }

  /**
   * initializes connection to the remote Service Manager. Get different suppliers and managers, 
   * required to work with the UI.
   * 
   * @throws Exception
   */
  private void initConnection() throws Exception
  {

    mxRemoteServiceManager = mxRemoteContext.getServiceManager();
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
   * Entry point for the generation of the top level menu (e.g. menu entry at the level of the 
   * "File" menu). Returned PropertyValue[] defines the top-level menu. The methods is called recursively. 
   * 
   * @param ct
   * @throws WrappedTargetException
   * @throws IndexOutOfBoundsException
   * @throws IllegalArgumentException
   * @throws NodeNotFoundException
   */
  protected PropertyValue[] createMenuTree(ConfigThingy ct)
      throws IllegalArgumentException, IndexOutOfBoundsException,
      WrappedTargetException, NodeNotFoundException
  {

    if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("menu"))
    {
      // lastCommandUrl = PREFIX+getProperty(ct,"ACTION")+"menuAction";
      PropertyValue[] menu = createMenu(ct);
      List subCts = (List) htIdToMenu.get(LimuxHelper.getProperty(ct, "MENU", UNDEFINED));
      PropertyValue[] menuItem = null;
      XIndexContainer container = uiConfigManager.createSettings();
      // container.insertByIndex(0,fileMenus);
      int counter = 0;
      // the menu was referenced, but not defined. Create subfolder and skip.
      // Example: "MenuA"=>"A_set", but the entries for the "A_set" are not defined anywhere. 
      // TODO: should the user be informed? 
      if (subCts == null)
      {
        menu = LimuxHelper.setProperty(
            menu,
            "ItemDescriptorContainer",
            container);
        return menu;
      }
      // iterate over entries in the submenu
      for (Iterator iter = subCts.iterator(); iter.hasNext();)
      {
        ConfigThingy subCt = (ConfigThingy) iter.next();
        menuItem = createMenuTree(subCt);
        // TODO: re-implement as soon as the following problem is solved:
        //       the submenues are not shown, if first entry in the menuu doesn't confirm
        //       to some _unknown_ criterias. A workarround is to use the "File" (or any other 
        //       standard menu entry) for creation of the first entry in the custom menu.
        if (counter == 0)
        {
          fileMenus = LimuxHelper.setProperty(fileMenus, "Label", LimuxHelper
              .getProperty(menuItem, "Label", UNDEFINED));
          fileMenus = LimuxHelper.setProperty(
              fileMenus,
              "CommandURL",
              LimuxHelper.getProperty(menuItem, "CommandURL", UNDEFINED));
          Object subM = LimuxHelper.getProperty(
              menuItem,
              "ItemDescriptorContainer", null);
          fileMenus = LimuxHelper.setProperty(fileMenus,"ItemDescriptorContainer",null);
          if (subM != null)
          {
            fileMenus = LimuxHelper.setProperty(
                fileMenus,
                "ItemDescriptorContainer",
                LimuxHelper.getProperty(menuItem, "ItemDescriptorContainer", null));
          }
          container.insertByIndex(counter, fileMenus);
        }
        else
        {
          container.insertByIndex(counter, menuItem);
        }
        menu = LimuxHelper.setProperty(
            menu,
            "ItemDescriptorContainer",
            container);
        counter++;
      }
      return menu;
    }
    else if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("button"))
    {
      // lastCommandUrl = PREFIX+getProperty(ct,"ACTION");
      PropertyValue[] menuItem = createMenuItem(ct);
      return menuItem;
    }
    else if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("separator"))
    {
      PropertyValue[] menuItem = createMenuItem(ct);
      return menuItem;
    }
    else if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("senderbox"))
    {
      PropertyValue[] menuItem = createMenuItem(ct);
      return menuItem;
    } else {
      // should not occure
      Logger
          .error("The ConfigThingy of the specified file can't be processed: "
                 + LimuxHelper.getProperty(ct, "TYPE", UNDEFINED));
      return new PropertyValue[] {};
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
   * generates described with the <b>configThingy</b> menues in the frame of
   * the running OO instance.
   * 
   * @param configThingy
   * @param xFrame
   */
  public static void generateMenues(ConfigThingy configThingy,
      XComponentContext xContext, XFrame xFrame)
  {
    try
    {
      new MenuList(configThingy, xContext, xFrame, S_MENUBAR);

    }
    catch (Exception e)
    {
      Logger.error("Generation of the menu failed", e);
    }
  }

  /**
   * generates described with the <b>configThingy</b> menues in the frame of
   * the running OO instance.
   * 
   * @param configThingy
   * @param xFrame
   */
  public static void generateToolbarEntries(ConfigThingy configThingy,
      XComponentContext xContext, XFrame xFrame)
  {
    try
    {
      new MenuList(configThingy, xContext, xFrame, S_TOOLBAR);

    }
    catch (Exception e)
    {
      Logger.error("Generation of the toolbar entries failed", e);
    }
  }

  /**
   * generate a menu entry which corresponds to the type "menu" in the "LHMVorlagenMenue.conf" and  
   * as a result contains the submenues.
   * 
   * @param ct
   *          the element which describes the menu entry
   * @return the PropertyValue[] equivalent of the <b>ct</b>
   */
  private PropertyValue[] createMenu(ConfigThingy ct)
  {

    PropertyValue[] loadProps = LimuxHelper.setProperty(
        null,
        "CommandURL",
        PREFIX + LimuxHelper.getProperty(ct, "MENU", UNDEFINED));
    loadProps = LimuxHelper.setProperty(loadProps, "Label", setHotkey(
        LimuxHelper.getProperty(ct, "LABEL", UNDEFINED),
        LimuxHelper.getProperty(ct, "HOTKEY", UNDEFINED)));
    // TODO: (note) the following commandURL will not be unique among different
    // submenues. The reason:
    // the same submenue can be bound to different parent menues.
    loadProps = LimuxHelper.setProperty(loadProps, "HelpURL", "");

    return loadProps;
  }

  /**
   * modify the label to make hotkey underlined in the OO. The case of the
   * Hotkey letter is ignored. The first occurence of the letter is used as a
   * hotkey.
   * 
   * @param label
   * @param hotkey
   * @return
   */
  private String setHotkey(String label, String hotkey)
  {
    String nLabel = label.replaceFirst(hotkey, "~" + hotkey);
    if (nLabel.equals(label))
    {
      nLabel = label.replaceFirst(hotkey.toLowerCase(), "~"
                                                        + hotkey.toLowerCase());
      if (nLabel.equals(label))
      {
        nLabel = label.replaceFirst(hotkey.toUpperCase(), "~"
                                                          + hotkey
                                                              .toUpperCase());
      }
    }

    return nLabel;

  }

  
  /**
   * generate a menu item, which corresponds to the type "button" in the "LHMVorlagenMenue.conf"
   * 
   * 
   * @param ct
   * @return
   * @throws NodeNotFoundException
   */
  private PropertyValue[] createMenuItem(ConfigThingy ct)
      throws NodeNotFoundException
  {
    // placeholder for the values set into the PropertyValue
    Object value = null;
    
    // is the entry a Separator or a MenuItem?
    int type = (LimuxHelper.getProperty(ct, "TYPE", "menu").equals("separator"))
                                                                        ? com.sun.star.ui.ItemType.SEPARATOR_LINE
                                                                        : com.sun.star.ui.ItemType.DEFAULT;

    // set CommandURL
    if (type == 1)
    {
      value = lastCommandUrl;
    }
    else
    {
      // a commandURL like ".WollMux:myAction#myArgument"
      String action = PREFIX+LimuxHelper.getProperty(ct, "ACTION", UNDEFINED);
      // TODO: schreibe Warnungen in Logger, wenn die Property nicht gefunden wurde.
      String arg = LimuxHelper.getProperty(ct, "FRAG_ID", UNDEFINED); 
      // TODO: FRAG_ID hängt vom Typ ab! Nur bei Type Action "openTemplate" soll FRAG_ID als argument verwendet werden.
      value = action;
      // TODO: funktioniert nicht richtig...
      //if(action.compareToIgnoreCase("openTemplate") == 0) {
        if (!arg.equals(UNDEFINED)){
          value = ((String)action).concat("#").concat(arg); 
//        } else
//          Logger.error("Keine FRAG_ID definiert in menu..."); //TODO: ausführliche Meldung.
      }
    }
    PropertyValue[] loadProps = LimuxHelper.setProperty(
        null,
        "CommandURL",
        value);

    
    // set Label with appropriate hotkey
    value = (type == 1) ? SEPARATOR : LimuxHelper.getProperty(ct, "LABEL", UNDEFINED);
    loadProps = LimuxHelper.setProperty(loadProps, "Label", setHotkey(
        (String) value,
        LimuxHelper.getProperty(ct, "HOTKEY", UNDEFINED)));

    // usual menuItem or separator?
    value = (type == 1)
                       ? new Integer(com.sun.star.ui.ItemType.SEPARATOR_LINE)
                       : new Integer(com.sun.star.ui.ItemType.DEFAULT);
    loadProps = LimuxHelper.setProperty(loadProps, "Type", value);
    
    // set HelpURL
    loadProps = LimuxHelper.setProperty(loadProps, "HelpURL", "");

    return loadProps;
  }

  /**
   * Remove all top-level menus which start with the PREFIX (e.g. ".WollMux:).  
   * 
   * @return an index of the dropped menu; it refers to the top-menu entries
   * @throws WrappedTargetException
   * @throws IndexOutOfBoundsException
   */
  private void removeMenues(String prefix) throws IndexOutOfBoundsException,
      WrappedTargetException
  {
    PropertyValue[] menu;
    ArrayList arr = new ArrayList();
    for (int i = oMenuBarSettings.getCount() - 1; i >= 0; i--)
    {
      menu = (PropertyValue[]) UnoRuntime.queryInterface(
          PropertyValue[].class,
          oMenuBarSettings.getByIndex(i));
      String url = (String) LimuxHelper.getProperty(menu, "CommandURL", UNDEFINED);
      if (url.startsWith(prefix))
      {
        arr.add(new Integer(i));
      }
    }

    for (Iterator iter = arr.iterator(); iter.hasNext();)
    {
      Integer i = (Integer) iter.next();
      oMenuBarSettings.removeByIndex(i.intValue());

    }
  }
  
  
  /**
   * generate a toolbar item, which corresponds to the type "button" in the "LHMVorlagenMenue.conf"
   * 
   * 
   * @param ct
   * @return
   * @throws NodeNotFoundException
   */
  private PropertyValue[] createToolbarItem(ConfigThingy ct)
      throws NodeNotFoundException
  {
    Object value = null;
    
    // is the entry a Separator or a MenuItem?
    int type = (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("separator"))
                                                                        ? ItemType.SEPARATOR_LINE
                                                                        : ItemType.DEFAULT;

    // set CommandURL
    if (type == ItemType.SEPARATOR_LINE)
    {
      value = lastCommandUrl;
    }
    else
    {
      // a commandURL like ".WollMux:myAction#myArgument"
      String action = PREFIX+LimuxHelper.getProperty(ct, "ACTION", UNDEFINED);
      String arg = LimuxHelper.getProperty(ct, "FRAG_ID", UNDEFINED);
      value = action;
      if (!arg.equals("")){
        value = ((String)action).concat("#").concat(arg); 
      }
    }
    PropertyValue[] loadProps = LimuxHelper.setProperty(
        null,
        "CommandURL",
        value);
    
    // set Label with appropriate hotkey
    value = (type == 1) ? SEPARATOR : LimuxHelper.getProperty(ct, "LABEL", UNDEFINED);
    loadProps = LimuxHelper.setProperty(loadProps, "Label", setHotkey(
        (String) value,
        LimuxHelper.getProperty(ct, "HOTKEY", UNDEFINED)));

    
    // Type
    loadProps = LimuxHelper.setProperty(
        loadProps,
        "Type",
        FormButtonType.PUSH);
    
    
    // Style
    // TODO: other possible types would be "ItemStyle.AUTO_SIZE" or "ItemStyle.DRAW_FLAT"
    loadProps = LimuxHelper.setProperty(
        loadProps,
        "Style",
        new Short(ItemStyle.ICON));

    // GraphicURL
    loadProps = LimuxHelper.setProperty(
        loadProps,
        "GraphicURL",
        "file:///C:/test.bmp");
    
//    // ImageURL
//    loadProps = LimuxHelper.setProperty(
//        loadProps,
//        "ImageURL",
//        "file:///C:/test.bmp");
    
    // usual menuItem or separator?
    value = (type == 1)
                       ? new Integer(com.sun.star.ui.ItemType.SEPARATOR_LINE)
                       : new Integer(com.sun.star.ui.ItemType.DEFAULT);
    loadProps = LimuxHelper.setProperty(loadProps, "Type", value);
    
    // set HelpURL
    loadProps = LimuxHelper.setProperty(loadProps, "HelpURL", "");

    return loadProps;
  }
  
  
  

  // ************************************************************************
  // *************************** the methods for test purposes **************
  // ************************************************************************

  /**
   * Testet die Funktionsweise der MenuList. Eine in url angegebene Konfigdatei
   * wird eingelesen und die dazugehörige MenuList erstellt. Anschliessend wird
   * die ausgegeben.
   * 
   * @param args
   *          url, dabei ist url die URL einer zu lesenden Config-Datei. Das
   *          Programm generiert menues in der laufende Instanz der OO.
   * @author GOLOVKO
   */
  public static void main(String[] args) throws IOException
  {
    XMultiComponentFactory testmxRemoteServiceManager = null;
    XDesktop xDesktop;
    XModel xModel;
    XController xController;
    XFrame xFrame;
    XComponentContext testmxRemoteContext = null;

    try
    {
      if (testmxRemoteContext == null && testmxRemoteServiceManager == null)
      {
        testmxRemoteContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
        System.out.println("Connected to a running office ...");
        testmxRemoteServiceManager = testmxRemoteContext.getServiceManager();
      }

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

      xDesktop = (XDesktop) UnoRuntime.queryInterface(
          XDesktop.class,
          testmxRemoteServiceManager.createInstanceWithContext(
              "com.sun.star.frame.Desktop",
              testmxRemoteContext));
      xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xDesktop
          .getCurrentComponent());
      xController = xModel.getCurrentController();
      xFrame = xController.getFrame();

      MenuList.generateMenues(conf, testmxRemoteContext, xFrame);
      
      MenuList.generateToolbarEntries(conf, testmxRemoteContext, xFrame);
      

    }
    catch (Exception e)
    {
      Logger.error("Error in main()", e);
    }
    System.exit(0);
  }
}
