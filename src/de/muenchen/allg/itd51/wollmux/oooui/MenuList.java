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
  protected static final int TOOLBAR = 1;

  protected static final int MENU = 2;

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

  // private static String S_TOOLBAR = "private:resource/toolbar/UITest";
  private static String S_TOOLBAR_PREFIX = "private:resource/toolbar/user_";

  // info about the type of the currently generated elment (e.g. toolbar, menu)
  private int elementType = 0;

  private static String PREFIX = "wollmux:";

  private static String UNDEFINED = "undefined";

  private static String SEPARATOR = "-----------------";

  private XIndexContainer oMenuBarSettings;

  // TODO: delete fileMenus as soon as the problem with empty entries in the
  // submenus is solved
  private PropertyValue[] fileMenus;

  // TODO: the string is intended to be used for the creation of the separators.
  // TODO: Contains the last inserted MenuItem. Doesn't work for unknown
  // reasons.
  // private String lastCommandUrl = "wollmux:myaction";

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
    {
      elementType = MENU;
      _generateMenues(root, xContext, xFrame, targetUIStr);
    }
    else if (targetUIStr.equals(S_TOOLBAR_PREFIX))
    {
      elementType = TOOLBAR;
      _generateToolbar(root, xContext, xFrame, targetUIStr);
    }

  }

  private void _generateToolbar(ConfigThingy root, XComponentContext xContext,
      XFrame xFrame, String targetUIStr) throws Exception
  {
    this.xFrame = xFrame;
    this.mxRemoteContext = xContext;
    initConnection();

    htIdToMenu = readMenues(root);

    // 0. read in the names of all top-level menues.
    ConfigThingy elements = root.query("Symbolleisten").getLastChild();
    Iterator iterOverElements = elements.iterator();
    while (iterOverElements.hasNext())
    {
      ConfigThingy uiElement = (ConfigThingy) iterOverElements.next();
      String tbResource = targetUIStr + uiElement.getName();
      Iterator iterOverItems = uiElement.queryByChild("TYPE").iterator();
      topLevelMenues = new ArrayList();
      while (iterOverItems.hasNext())
      {
        ConfigThingy uiItem = (ConfigThingy) iterOverItems.next();
        topLevelMenues.add(uiItem);
      }

      // 2. init objects required for the creation of the menu elements
      XUIElementSettings xoMenuBarSettings = getUIElementSettings(tbResource);

      removeMenues(PREFIX);

      int mCounter = 0;
      for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
      {
        ConfigThingy element = (ConfigThingy) iter.next();
        PropertyValue[] topMenu = createUIItemeTree(element);
        oMenuBarSettings.insertByIndex(mCounter, topMenu);
        mCounter++;
      }
      xoMenuBarSettings.setSettings(oMenuBarSettings);
      XUIConfigurationPersistence xUIConfigurationPersistence = (XUIConfigurationPersistence) UnoRuntime
          .queryInterface(XUIConfigurationPersistence.class, uiConfigManager);
      xUIConfigurationPersistence.store();
      xUIConfigurationPersistence.isReadOnly();
    }

  }

  /**
   * 
   * set private variables required in other methods and return element settings
   * back.
   * 
   * @param tbResource
   * @return
   * @throws WrappedTargetException
   * @throws IllegalArgumentException
   * @throws PropertyVetoException
   * @throws UnknownPropertyException
   */
  private XUIElementSettings getUIElementSettings(String tbResource)
      throws UnknownPropertyException, PropertyVetoException,
      IllegalArgumentException, WrappedTargetException

  {
    XUIElement oMenuBar = xLayoutManager.getElement(tbResource);
    if (oMenuBar == null)
    {
      xLayoutManager.destroyElement(tbResource);
      xLayoutManager.createElement(tbResource);
      oMenuBar = xLayoutManager.getElement(tbResource);
    }
    xLayoutManager.showElement(tbResource);

    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        oMenuBar);
    props.setPropertyValue("Persistent", new Boolean(false));
    XUIElementSettings xoMenuBarSettings = (XUIElementSettings) UnoRuntime
        .queryInterface(XUIElementSettings.class, oMenuBar);
    oMenuBarSettings = (XIndexContainer) UnoRuntime.queryInterface(
        XIndexContainer.class,
        xoMenuBarSettings.getSettings(true));
    return xoMenuBarSettings;
  }

  private Hashtable readMenues(ConfigThingy root) throws NodeNotFoundException
  {
    Hashtable hash = new Hashtable();
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
        menues = getMenuItems(lhmVorlage);
        // 1.3 put something like "LHMVorlagen"=> <array of Menu> into hash for
        // sake of uniquness
        hash.put(lhmVorlage.getName(), menues);
      }
    }
    return hash;
  }

  private void _generateMenues(ConfigThingy root, XComponentContext xContext,
      XFrame xFrame, String targetUIStr) throws Exception,
      NodeNotFoundException, UnknownPropertyException, PropertyVetoException,
      IllegalArgumentException, WrappedTargetException,
      IndexOutOfBoundsException, com.sun.star.uno.Exception
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

    htIdToMenu = readMenues(root);

    // 2. init objects required for the creation of the menu elements
    XUIElement oMenuBar = xLayoutManager.getElement(targetUIStr);
    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        oMenuBar);
    props.setPropertyValue("Persistent", new Boolean(false));
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

      PropertyValue[] topMenu = createUIItemeTree(element);
      // TODO: check if insertion at POSITION is possible
      // UNDO 2
      oMenuBarSettings.insertByIndex(Integer.parseInt(LimuxHelper.getProperty(
          element,
          "POSITION",
          "0")), topMenu);

      xoMenuBarSettings.setSettings(oMenuBarSettings);
    }

    XUIConfigurationPersistence xUIConfigurationPersistence = (XUIConfigurationPersistence) UnoRuntime
        .queryInterface(XUIConfigurationPersistence.class, uiConfigManager);
    xUIConfigurationPersistence.store();
  }

  /**
   * initializes connection to the remote Service Manager. Get different
   * suppliers and managers, required to work with the UI.
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
   * Entry point for the generation of the top level menu (e.g. menu entry at
   * the level of the "File" menu). Returned PropertyValue[] defines the
   * top-level menu. The methods is called recursively.
   * 
   * @param ct
   * @throws WrappedTargetException
   * @throws IndexOutOfBoundsException
   * @throws IllegalArgumentException
   * 
   */
  protected PropertyValue[] createUIItemeTree(ConfigThingy ct)
      throws IllegalArgumentException, IndexOutOfBoundsException,
      WrappedTargetException
  {

    if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("menu"))
    {
      // lastCommandUrl = PREFIX+getProperty(ct,"ACTION")+"menuAction";
      PropertyValue[] menu = createMenu(ct);
      List subCts = (List) htIdToMenu.get(LimuxHelper.getProperty(
          ct,
          "MENU",
          UNDEFINED));
      PropertyValue[] menuItem = null;
      XIndexContainer container = uiConfigManager.createSettings();
      // container.insertByIndex(0,fileMenus);
      int counter = 0;
      // the menu was referenced, but not defined. Create subfolder and skip.
      // Example: "MenuA"=>"A_set", but the entries for the "A_set" are not
      // defined anywhere.
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
        menuItem = createUIItemeTree(subCt);
        // TODO: re-implement as soon as the following problem is solved:
        // the submenues are not shown, if first entry in the menuu doesn't
        // confirm
        // to some _unknown_ criterias. A workarround is to use the "File" (or
        // any other
        // standard menu entry) for creation of the first entry in the custom
        // menu.
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
              "ItemDescriptorContainer",
              null);
          fileMenus = LimuxHelper.setProperty(
              fileMenus,
              "ItemDescriptorContainer",
              null);
          if (subM != null)
          {
            fileMenus = LimuxHelper.setProperty(
                fileMenus,
                "ItemDescriptorContainer",
                LimuxHelper.getProperty(
                    menuItem,
                    "ItemDescriptorContainer",
                    null));
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
      PropertyValue[] menuItem = createButton(ct);
      return menuItem;
    }
    else if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("separator"))
    {
      PropertyValue[] menuItem = createSeparator(ct);
      return menuItem;
    }
    else if (LimuxHelper.getProperty(ct, "TYPE", UNDEFINED).equals("senderbox"))
    {
      PropertyValue[] menuItem = createSenderBox(ct);
      return menuItem;
    }
    else
    {
      // should not occure
      Logger.error("Unknown type of the UI item: "
                   + LimuxHelper.getProperty(ct, "TYPE", UNDEFINED));
      return new PropertyValue[] {};
    }

  }

  /**
   * 
   * a senderBox is an item (usually belongs to a toolbar) with drop-down list
   * of entries, which can be selected.
   * 
   * @param ct
   * @return
   */
  private PropertyValue[] createSenderBox(ConfigThingy ct)
  {
    PropertyValue[] loadProps = null;

    // LABEL
    String value = LimuxHelper.getProperty(ct, "LABEL", UNDEFINED);
    loadProps = LimuxHelper.setProperty(loadProps, "Label", value);

    // TYPE
    loadProps = LimuxHelper.setProperty(loadProps, "Type", new Short(
        ItemType.DEFAULT));

    loadProps = LimuxHelper.setProperty(loadProps, "CommandURL", PREFIX
                                                                 + "senderBox");

    return loadProps;
  }

  /**
   * return UI element items as a list of ConfigThingy
   * 
   * @param lhmVorlage
   * @return
   * @throws NodeNotFoundException
   */
  private List getMenuItems(ConfigThingy lhmVorlage)
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
      new MenuList(configThingy, xContext, xFrame, S_TOOLBAR_PREFIX);
    }
    catch (Exception e)
    {
      Logger.error("Generation of the toolbar entries failed", e);
    }
  }

  /**
   * generate a menu entry which corresponds to the type "menu" in the
   * "LHMVorlagenMenue.conf" and as a result contains the submenues.
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
        PREFIX + "Menu#" + LimuxHelper.getProperty(ct, "MENU", UNDEFINED));
    loadProps = LimuxHelper.setProperty(loadProps, "Label", setHotkey(
        LimuxHelper.getProperty(ct, "LABEL", UNDEFINED),
        LimuxHelper.getProperty(ct, "HOTKEY", UNDEFINED)));
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
      String url = (String) LimuxHelper.getProperty(
          menu,
          "CommandURL",
          UNDEFINED);
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

  private PropertyValue[] createSeparator(ConfigThingy ct)
  {
    PropertyValue[] loadProps = null;

    switch (elementType)
    {
      case TOOLBAR:
        loadProps = LimuxHelper.setProperty(loadProps, "Label", SEPARATOR);
        break;
      case MENU:
        loadProps = LimuxHelper.setProperty(loadProps, "Label", SEPARATOR);
        break;
      default:
        break;
    }

    loadProps = LimuxHelper.setProperty(loadProps, "Type", new Short(
        ItemType.SEPARATOR_LINE));

    return loadProps;
  }

  /**
   * generate a an item for either toolbar or menu element. Corrsponds to the
   * type "button" in the "LHMVorlagenMenue.conf".
   * 
   * @param ct
   * @return
   */
  private PropertyValue[] createButton(ConfigThingy ct)
  {
    PropertyValue[] loadProps = null;

    // ACTION + FRAG_ID
    // (a commandURL like ".WollMux:myAction#myArgument")
    String action = LimuxHelper.getProperty(ct, "ACTION", UNDEFINED);
    if (action.equalsIgnoreCase("openTemplate"))
    {
      // FRAG_ID hängt vom Typ ab! Nur bei Type Action "openTemplate" soll
      // FRAG_ID als argument verwendet werden.
      String fragid = LimuxHelper.getProperty(ct, "FRAG_ID", UNDEFINED);
      if (fragid.equalsIgnoreCase(UNDEFINED))
      {
        Logger.error("Keine FRAG_ID definiert in menu..."); // ausführliche
        // Meldung.
      }
      else
      {
        action = action + "#" + fragid;
      }
    }
    loadProps = LimuxHelper.setProperty(loadProps, "CommandURL", PREFIX
                                                                 + action);

    // LABEL + HOTKEY
    String label = LimuxHelper.getProperty(ct, "LABEL", UNDEFINED);
    label = setHotkey(label, LimuxHelper.getProperty(ct, "HOTKEY", UNDEFINED));
    loadProps = LimuxHelper.setProperty(loadProps, "Label", label);

    // TYPE
    loadProps = LimuxHelper.setProperty(loadProps, "Type", new Short(
        ItemType.DEFAULT));

    // HelpURL
    loadProps = LimuxHelper.setProperty(loadProps, "HelpURL", "");

    switch (elementType)
    {
      case TOOLBAR:
        // Type [redefine]
        loadProps = LimuxHelper.setProperty(
            loadProps,
            "Type",
            FormButtonType.PUSH);
        // Style
        // (other possible types would be "ItemStyle.AUTO_SIZE" or
        // "ItemStyle.DRAW_FLAT")
        // loadProps = LimuxHelper.setProperty(loadProps,"Style",new
        // Short(ItemStyle.ICON));
        // GraphicURL
        // loadProps =
        // LimuxHelper.setProperty(loadProps,"GraphicURL","file:///C:/test.bmp");
        break;
      case MENU:
        // menu-specific
        break;
      default:
        break;
    }

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

      // MenuList.generateMenues(conf, testmxRemoteContext, xFrame);

      MenuList.generateToolbarEntries(conf, testmxRemoteContext, xFrame);

    }
    catch (Exception e)
    {
      Logger.error("Error in main()", e);
    }
    System.exit(0);
  }
}
