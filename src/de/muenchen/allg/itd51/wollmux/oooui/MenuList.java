/*
 * Dateiname: MenuList.java
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
 * 13.12.2005 | LUT     | Überarbeitung und Aufräumarbeit
 *                        potentielle Fehlerquellen beseitigt.
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
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XIndexContainer;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.ui.ItemType;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementSettings;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoService;
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
  private Hashtable htIdToMenu = new Hashtable();

  private List topLevelMenues = new ArrayList();

  // the variables from the running OO
  private XComponentContext ctx = null;

  private XLayoutManager xLayoutManager;

  private static String S_MENUBAR = "private:resource/menubar/menubar";

  private static String S_TOOLBAR_PREFIX = "private:resource/toolbar/WollMux-";

  private static String PREFIX = "wollmux:";

  private static String UNDEFINED = "undefined";

  private XIndexContainer oElementSettings;

  // Workaround: a "File" UI item
  private PropertyValue[] fileMenus;

  protected MenuList(XComponentContext ctx, XFrame xFrame)
  {
    this.ctx = ctx;

    XPropertySet xps = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        xFrame);
    try
    {
      xLayoutManager = (XLayoutManager) UnoRuntime.queryInterface(
          XLayoutManager.class,
          xps.getPropertyValue("LayoutManager"));
    }
    catch (java.lang.Exception e)
    {
      // das sollte nicht vorkommen
      Logger.error(e);
    }

  }

  /**
   * @param root
   * @throws Exception
   */
  private void generateToolbar(ConfigThingy root) throws Exception
  {
    htIdToMenu = readMenues(root);

    // 0. read in the names of all top-level menues.
    ConfigThingy bars = root.query("Symbolleisten");
    if (bars.count() > 0)
    {
      Iterator i = null;
      try
      {
        i = bars.getLastChild().iterator();
      }
      catch (NodeNotFoundException e)
      {
        // oben abgefangen
      }
      while (i.hasNext())
      {
        ConfigThingy uiElement = (ConfigThingy) i.next();
        String tbResource = S_TOOLBAR_PREFIX + uiElement.getName();
        Iterator iterOverItems = uiElement.queryByChild("TYPE").iterator();
        topLevelMenues = new ArrayList();
        while (iterOverItems.hasNext())
        {
          ConfigThingy uiItem = (ConfigThingy) iterOverItems.next();
          topLevelMenues.add(uiItem);
        }

        // 2. init objects required for the creation of the menu elements
        XUIElementSettings xoMenuBarSettings = getUIElementSettings(tbResource);

        int mCounter = 0;
        for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
        {
          ConfigThingy element = (ConfigThingy) iter.next();
          PropertyValue[] topMenu = createUIItemeTree(element);
          oElementSettings.insertByIndex(mCounter, topMenu);
          mCounter++;
        }
        xoMenuBarSettings.setSettings(oElementSettings);
        xLayoutManager.showElement(tbResource);
      }
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
    XUIElement uiElement = xLayoutManager.getElement(tbResource);
    if (uiElement == null)
    {
      // xLayoutManager.destroyElement(tbResource);
      xLayoutManager.createElement(tbResource);
      uiElement = xLayoutManager.getElement(tbResource);
    }

    // set persistence to false
    XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class,
        uiElement);
    props.setPropertyValue("Persistent", new Boolean(false));

    XUIElementSettings xoElementSettings = (XUIElementSettings) UnoRuntime
        .queryInterface(XUIElementSettings.class, uiElement);
    oElementSettings = (XIndexContainer) UnoRuntime.queryInterface(
        XIndexContainer.class,
        xoElementSettings.getSettings(true));
    return xoElementSettings;
  }

  private Hashtable readMenues(ConfigThingy root)
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

  private void generateMenues(ConfigThingy root) throws Exception
  {
    // 0. read in the names of all top-level menues.
    ConfigThingy bar = root.query("Menueleiste");
    if (bar.count() > 0)
    {
      Iterator i = null;
      try
      {
        i = bar.getLastChild().iterator();
      }
      catch (NodeNotFoundException e)
      {
        // wird oben abgefangen
      }
      while (i.hasNext())
      {
        ConfigThingy topMenu = (ConfigThingy) i.next();
        topLevelMenues.add(topMenu);
      }

      htIdToMenu = readMenues(root);

      // 2. init objects required for the creation of the menu elements
      XUIElementSettings xoMenuBarSettings = getUIElementSettings(S_MENUBAR);
      fileMenus = (PropertyValue[]) UnoRuntime.queryInterface(
          PropertyValue[].class,
          oElementSettings.getByIndex(7));
      fileMenus = LimuxHelper.setProperty(
          fileMenus,
          "ItemDescriptorContainer",
          null);

      for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
      {
        ConfigThingy element = (ConfigThingy) iter.next();

        PropertyValue[] topMenu = createUIItemeTree(element);
        // TODO: check if insert at POSITION is possible
        oElementSettings.insertByIndex(Integer.parseInt(LimuxHelper
            .getProperty(element, "POSITION", "0")), topMenu);

        xoMenuBarSettings.setSettings(oElementSettings);

      }
    }
  }

  /**
   * Entry point for the generation of the top level menu (e.g. menu entry at
   * the level of the "File" menu). Returned PropertyValue[] defines the
   * top-level menu. The methods is called recursively.
   * 
   * @param ct
   * @throws Exception
   * 
   */
  protected PropertyValue[] createUIItemeTree(ConfigThingy ct) throws Exception
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
      XSingleComponentFactory factory = (XSingleComponentFactory) UnoRuntime
          .queryInterface(XSingleComponentFactory.class, oElementSettings);
      XIndexContainer container = (XIndexContainer) UnoRuntime.queryInterface(
          XIndexContainer.class,
          factory.createInstanceWithContext(ctx));
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
        // TODO: TRICK: a "File" menubar uiItem is used as a template
        // when inserting it as a subitem into a custom top-level item;
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
  {
    ArrayList results = new ArrayList();
    // an element is smth like "LHMVorlagen(...)"
    Iterator iter1 = lhmVorlage.queryByChild("TYPE").iterator();
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
   * @throws NodeNotFoundException
   */
  public static void generateMenues(ConfigThingy configThingy,
      XComponentContext xContext, XFrame xFrame)
  {
    try
    {
      new MenuList(xContext, xFrame).generateMenues(configThingy);
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
      new MenuList(xContext, xFrame).generateToolbar(configThingy);
    }
    catch (Exception e)
    {
      Logger.error("Generation of the toolbar entries failed", e);
    }
  }

  /**
   * make change to the menu/toolbar element persistant
   * 
   * @throws Exception
   * 
   * @throws Exception
   * @throws NodeNotFoundException
   */
  public static void generatePersistentModuleUIElements(ConfigThingy ct,
      XComponentContext ctx) throws Exception
  {
    UnoService moduleCfgMgrSupplier;
    moduleCfgMgrSupplier = UnoService.createWithContext(
        "com.sun.star.ui.ModuleUIConfigurationManagerSupplier",
        ctx);
    UnoService uiConfigMgr = new UnoService(moduleCfgMgrSupplier
        .xModuleUIConfigurationManagerSupplier().getUIConfigurationManager(
            "com.sun.star.text.TextDocument"));

    ConfigThingy elements = ct.query("Symbolleisten");
    if (elements.count() > 0)
    {
      Iterator i = null;
      try
      {
        i = elements.getLastChild().iterator();
      }
      catch (NodeNotFoundException x)
      {
        // oben abgefangen.
      }
      while (i.hasNext())
      {
        ConfigThingy element = (ConfigThingy) i.next();
        String elementURL = S_TOOLBAR_PREFIX + element.getName();
        try
        {
          uiConfigMgr.xUIConfigurationManager().getSettings(elementURL, true);
        }
        catch (NoSuchElementException e)
        {
          UnoService container = new UnoService(uiConfigMgr
              .xUIConfigurationManager().createSettings());
          uiConfigMgr.xUIConfigurationManager().insertSettings(
              elementURL,
              container.xIndexAccess());
        }
      }
    }

    // make the elements persistent.
    uiConfigMgr.xUIConfigurationPersistence().store();
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

  private PropertyValue[] createSeparator(ConfigThingy ct)
  {
    PropertyValue[] loadProps = null;

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
        Logger.error("Keine FRAG_ID definiert in menu...");
        // TODO: ausführliche Meldung.
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
      //
      MenuList.generateMenues(conf, testmxRemoteContext, xFrame);
      MenuList.generateToolbarEntries(conf, testmxRemoteContext, xFrame);
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Error in main()", e);
    }
    System.exit(0);
  }
}
