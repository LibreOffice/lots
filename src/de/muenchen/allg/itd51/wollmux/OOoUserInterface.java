/*
 * Dateiname: OOoUserInterface.java
 * Projekt  : WollMux
 * Funktion : The OOoUserInterface is responsible for the generation of the menus
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
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

import com.sun.star.awt.Point;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.ui.DockingArea;
import com.sun.star.ui.ItemType;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * The OOoUserInterface is responsible for the generation of the menus in the
 * instance of the OO. The generated menues are persistent and will be there
 * after OO restarts.
 * 
 * The generated menu entries will be available for all documents of the same
 * type. For example, a user works with a XTextDocument. A call to the
 * generateMenues() results in the new menues. Every new XTextDocument will have
 * these menues. But the menues are not there, if some document of another type
 * as XTextDocument is opened.
 * 
 * @author GOLOVKO
 */
public class OOoUserInterface
{
  private XComponentContext ctx;

  private XLayoutManager xLayoutManager;

  private static String S_MENUBAR = "private:resource/menubar/menubar";

  private static String S_TOOLBAR_PREFIX = "private:resource/toolbar/WollMux-";

  private static String PREFIX = "wollmux:";

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
      new OOoUserInterface(xContext, xFrame).generateMenues(configThingy);
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
      new OOoUserInterface(xContext, xFrame).generateToolbar(configThingy);
    }
    catch (Exception e)
    {
      Logger.error("Generation of the toolbar entries failed", e);
    }
  }

  private OOoUserInterface(XComponentContext ctx, XFrame xFrame)
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
    // ConfigurationManager (für persistente Einstellungen) holen.
    UnoService moduleCfgMgrSupplier = UnoService.createWithContext(
        "com.sun.star.ui.ModuleUIConfigurationManagerSupplier",
        ctx);
    UnoService uiConfigMgr = new UnoService(moduleCfgMgrSupplier
        .xModuleUIConfigurationManagerSupplier().getUIConfigurationManager(
            "com.sun.star.text.TextDocument"));

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
        // kann nicht auftreten, da oben abgefangen
        Logger.error(e);
      }

      // Iteration über alle definierten Symbolleisten
      while (i.hasNext())
      {
        ConfigThingy toolbar = (ConfigThingy) i.next();
        String tbResource = S_TOOLBAR_PREFIX + toolbar.getName();

        // Hole settings vom uiCfgMgr oder erzeuge neue settings:
        boolean toolbarIsNew = false;
        UnoService settings;
        try
        {
          settings = new UnoService(uiConfigMgr.xUIConfigurationManager()
              .getSettings(tbResource, true));
        }
        catch (NoSuchElementException e)
        {
          settings = new UnoService(uiConfigMgr.xUIConfigurationManager()
              .createSettings());
          uiConfigMgr.xUIConfigurationManager().insertSettings(
              tbResource,
              settings.xIndexAccess());
          toolbarIsNew = true;
        }

        // Alle bestehenden Einträge löschen:
        while (settings.xIndexContainer().getCount() > 0)
        {
          settings.xIndexContainer().removeByIndex(0);
        }

        // Iteration über alle Elemente der Symbolleiste
        Iterator j = toolbar.queryByChild("TYPE").iterator();
        for (int mCounter = 0; j.hasNext();)
        {
          ConfigThingy element = (ConfigThingy) j.next();
          try
          {
            PropertyValue[] topMenu = createUIItem(
                element,
                root,
                settings.xSingleComponentFactory()).getProps();
            settings.xIndexContainer().insertByIndex(mCounter, topMenu);
            mCounter++;
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }

        // Einstellungen Setzen
        uiConfigMgr.xUIConfigurationManager().replaceSettings(
            tbResource,
            settings.xIndexAccess());

        // Toolbar positionieren und anzeigen.
        xLayoutManager.createElement(tbResource);
        if (toolbarIsNew)
        {
          int topHeight = xLayoutManager.getCurrentDockingArea().Y;
          xLayoutManager.dockWindow(
              tbResource,
              DockingArea.DOCKINGAREA_TOP,
              new Point(0, topHeight));
        }
        xLayoutManager.showElement(tbResource);
      }
    }

    // neue Toolbars persistent machen:
    uiConfigMgr.xUIConfigurationPersistence().store();
  }

  /**
   * @param root
   * @throws Exception
   */
  private void generateMenues(ConfigThingy root) throws Exception
  {
    // 0. read in the names of all top-level menues.
    ConfigThingy bar = root.query("Menueleiste");
    if (bar.count() > 0)
    {

      XUIElement xUIElement = xLayoutManager.getElement(S_MENUBAR);
      if (xUIElement == null)
      {
        xLayoutManager.createElement(S_MENUBAR);
        xUIElement = xLayoutManager.getElement(S_MENUBAR);
      }

      UnoService uiElement = new UnoService(xUIElement);
      UnoService settings = new UnoService(uiElement.xUIElementSettings()
          .getSettings(true));
      uiElement.setPropertyValue("Persistent", Boolean.FALSE);

      Iterator i = null;
      try
      {
        i = bar.getLastChild().queryByChild("TYPE").iterator();
      }
      catch (NodeNotFoundException e)
      {
        // kann nicht auftreten, da oben abgefangen
        Logger.error(e);
      }

      // Iteriere über alle Menueleisten-Einträge:
      while (i.hasNext())
      {
        ConfigThingy element = (ConfigThingy) i.next();
        try
        {
          String pos = getMandatoryAttribute(element, "POSITION").toString();
          UnoProps item = createUIItem(element, root, settings
              .xSingleComponentFactory());
          removeFromSettings(settings, item
              .getPropertyValueAsString("CommandURL"));
          settings.xIndexContainer().insertByIndex(
              Integer.parseInt(pos),
              item.getProps());
          uiElement.xUIElementSettings().setSettings(settings.xIndexAccess());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }
  }

  /**
   * Diese Methode sucht in einem übergebenen settings-container nach einem
   * Element mit der gegebenen URL und löscht dieses aus dem Container. Es wird
   * nur der erste match gelöscht.
   * 
   * @param settings
   * @param url
   */
  private void removeFromSettings(UnoService settings, String url)
  {
    int count = settings.xIndexContainer().getCount();
    for (int i = 0; i < count; i++)
    {
      String itemUrl = null;
      try
      {
        UnoProps item = new UnoProps((Object[]) settings.xIndexContainer()
            .getByIndex(i));
        itemUrl = item.getPropertyValueAsString("CommandURL");
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
      if (itemUrl != null && itemUrl.equals(url))
      {
        try
        {
          settings.xIndexContainer().removeByIndex(i);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
        return;
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
   * @throws ConfigurationErrorException
   * @throws Exception
   * 
   */
  private UnoProps createUIItem(ConfigThingy ct, ConfigThingy root,
      XSingleComponentFactory factory) throws ConfigurationErrorException,
      Exception
  {
    String type = getMandatoryAttribute(ct, "TYPE").toString();

    if (type.equals("menu"))
    {
      return createMenu(ct, root, factory);
    }
    else if (type.equals("button"))
    {
      return createButton(ct);
    }
    else if (type.equals("separator"))
    {
      return createSeparator(ct);
    }
    else if (type.equals("senderbox"))
    {
      return createSenderBox(ct);
    }
    else
    {
      throw new ConfigurationErrorException("Unbekannter TYPE in Element: "
                                            + ct.stringRepresentation());
    }
  }

  /**
   * Diese Methode erzeugt ein neues, leeres PopupMenue.
   * 
   * @param ct
   *          Das ConfigThingy-Element, das das Menue beschreibt.
   * @param root
   *          Das Wurzelelement der Configuration, das die "Menues"-Abschnitte
   *          enthält.
   * @param factory
   *          Eine Factory zur Erzeugung des neuen ItemDescriptorContainers des
   *          PopupMenues - kann üblicherweise aus dem Settings-Objekt des
   *          Wurzel-UI-Elements mit queryInterface erzeugt werden.
   * @return
   * @throws ConfigurationErrorException
   * @throws Exception
   */
  private UnoProps createMenu(ConfigThingy ct, ConfigThingy root,
      XSingleComponentFactory factory) throws ConfigurationErrorException,
      Exception
  {
    String menuref = getMandatoryAttribute(ct, "MENU").toString();
    String label = getHotkeyLabel(ct);

    // create submenu-container:
    UnoService container = new UnoService(factory
        .createInstanceWithContext(ctx));

    UnoProps props = new UnoProps();
    props.setPropertyValue("CommandURL", PREFIX + "menu#" + menuref);
    props.setPropertyValue("Label", label);
    props.setPropertyValue("Type", new Short(ItemType.DEFAULT));
    props.setPropertyValue("ItemDescriptorContainer", container.xIndexAccess());

    // Hole in Menuebeschreibung von menuref:
    ConfigThingy menues = root.query("Menues").query(menuref);
    if (menues.count() > 0)
    {
      Iterator i = null;
      try
      {
        i = menues.getLastChild().queryByChild("TYPE").iterator();
      }
      catch (NodeNotFoundException e)
      {
        // kann nicht auftreten, da oben abgefangen
        Logger.error(e);
      }

      // iterate over entries in the submenu
      for (int counter = 0; i.hasNext();)
      {
        ConfigThingy subCt = (ConfigThingy) i.next();
        try
        {
          UnoProps menuItem = createUIItem(subCt, root, factory);
          container.xIndexContainer().insertByIndex(
              counter,
              menuItem.getProps());
          counter++;
        }
        catch (java.lang.Exception x)
        {
          Logger.error(x);
        }
      }
    }
    else
    {
      Logger.error("Menue \""
                   + menuref
                   + "\" nicht definiert im Eintrag \""
                   + label
                   + "\"");
    }
    return props;
  }

  /**
   * 
   * a senderBox is an item (usually belongs to a toolbar) with drop-down list
   * of entries, which can be selected.
   * 
   * @param ct
   * @return
   * @throws ConfigurationErrorException
   */
  private UnoProps createSenderBox(ConfigThingy ct)
      throws ConfigurationErrorException
  {
    String label = getMandatoryAttribute(ct, "LABEL").toString();

    UnoProps props = new UnoProps();
    props.setPropertyValue("Label", label);
    props.setPropertyValue("Type", new Short(ItemType.DEFAULT));
    props.setPropertyValue("CommandURL", PREFIX + "senderBox");
    return props;
  }

  /**
   * Erzeugt einen Separator.
   * 
   * @param ct
   * @return
   */
  private UnoProps createSeparator(ConfigThingy ct)
  {
    return new UnoProps("Type", new Short(ItemType.SEPARATOR_LINE));
  }

  /**
   * generate a an item for either toolbar or menu element. Corrsponds to the
   * type "button" in the "LHMVorlagenMenue.conf".
   * 
   * @param ct
   * @return
   * @throws ConfigurationErrorException
   */
  private UnoProps createButton(ConfigThingy ct)
      throws ConfigurationErrorException
  {
    String action = getMandatoryAttribute(ct, "ACTION").toString();
    String label = getHotkeyLabel(ct);

    // ACTION + FRAG_ID
    // (a commandURL like ".WollMux:myAction#myArgument")
    if (action.equalsIgnoreCase("openTemplate"))
    {
      String fragid = getMandatoryAttribute(ct, "FRAG_ID").toString();
      action = action + "#" + fragid;
    }

    UnoProps props = new UnoProps();
    props.setPropertyValue("CommandURL", PREFIX + action);
    props.setPropertyValue("Label", label);
    props.setPropertyValue("Type", new Short(ItemType.DEFAULT));
    return props;
  }

  /**
   * Diese Methode stellt sicher, dass ein ConfigThingy-Attribut vorhanden ist
   * und liefert dieses zurück. Ist das Attribut nicht vorhanden, so wird eine
   * ConfigurationErrorException geworfen.
   * 
   * @param ct
   *          Das ConfigThingy Element, das das Attribut beinhalten soll.
   * @param att
   *          Der Name des gesuchten Attributs.
   * @return
   * @throws ConfigurationErrorException
   */
  private static ConfigThingy getMandatoryAttribute(ConfigThingy ct, String att)
      throws ConfigurationErrorException
  {
    try
    {
      return ct.get(att);
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException("Fehlendes Attribut \""
                                            + att
                                            + "\" in Element: "
                                            + ct.stringRepresentation());
    }
  }

  /**
   * creates a labelstring with a hotkey if there is a HOTKEY-attribute
   * specified. The case of the Hotkey letter is ignored. The first occurence of
   * the letter is used as a hotkey.
   * 
   * @param element
   *          Ein Element, das die Attribute LABEL und optional HOTKEY als
   *          Kinder enthält.
   * @return
   * @throws ConfigurationErrorException
   */
  private static String getHotkeyLabel(ConfigThingy element)
      throws ConfigurationErrorException
  {
    String label = getMandatoryAttribute(element, "LABEL").toString();

    try
    {
      String hotkey = element.get("HOTKEY").toString();
      String nLabel = label.replaceFirst(hotkey, "~" + hotkey);
      if (nLabel.equals(label))
      {
        nLabel = label.replaceFirst(hotkey.toLowerCase(), "~"
                                                          + hotkey
                                                              .toLowerCase());
      }
      if (nLabel.equals(label))
      {
        nLabel = label.replaceFirst(hotkey.toUpperCase(), "~"
                                                          + hotkey
                                                              .toUpperCase());
      }
      return nLabel;
    }
    catch (NodeNotFoundException e)
    {
      return label;
    }
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
  public static void main(String[] args)
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
      OOoUserInterface.generateMenues(conf, testmxRemoteContext, xFrame);
      OOoUserInterface
          .generateToolbarEntries(conf, testmxRemoteContext, xFrame);
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Error in main()", e);
    }
    System.exit(0);
  }
}
