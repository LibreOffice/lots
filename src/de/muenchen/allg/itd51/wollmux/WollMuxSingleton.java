/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton für zentrale WollMux-Methoden.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n     
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | LUT | Überarbeitung Code-Kommentare  
 * 20.04.2006 | BNK | DEFAULT_CONTEXT ausgegliedert nach WollMuxFiles
 * 21.04.2006 | LUT | + Robusteres Verhalten bei Fehlern während dem Einlesen 
 *                    von Konfigurationsdateien; 
 *                    + wohldefinierte Datenstrukturen
 *                    + Flag für EventProcessor: acceptEvents
 * 08.05.2006 | LUT | + isDebugMode()
 * 10.05.2006 | BNK | +parseGlobalFunctions()
 *                  | +parseFunctionDialogs()
 * 26.05.2006 | BNK | DJ initialisierung ausgelagert nacht WollMuxFiles
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * 19.12.2006 | BAB | + setzen von Shortcuts im Konstruktor
 * 29.12.2006 | BNK | +registerDatasources()
 * 27.03.2007 | BNK | Default-oooEinstellungen ausgelagert nach data/...
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Date;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.document.XEventListener;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.XInterface;
import com.sun.star.util.XChangesBatch;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

/**
 * Diese Klasse ist ein Singleton, welcher den WollMux initialisiert und alle
 * zentralen WollMux-Methoden zur Verfügung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der früher zentraler Anlaufpunkt
 * war, bedient sich größtenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton implements XPALProvider
{
  /**
   * Default-oooEinstellungen-Abschnitt (Übergangslösung).
   */
  private final URL OOOEINSTELLUNGEN_URL = this.getClass().getClassLoader()
      .getResource("data/oooEinstellungen.conf");

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten
   * Funktionen.
   */
  private FunctionLibrary globalFunctions;

  /**
   * Enthält die im Funktionsdialoge-Abschnitt der wollmux,conf definierten
   * Dialoge.
   */
  private DialogLibrary funcDialogs;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten
   * Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  /**
   * Enthält den default XComponentContext in dem der WollMux (bzw. das OOo)
   * läuft.
   */
  private XComponentContext ctx;

  /**
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private Vector registeredPALChangeListener;

  /**
   * Enthält alle registrierten XEventListener, die bei Statusänderungen der
   * Dokumentbearbeitung informiert werden.
   */
  private Vector registeredDocumentEventListener;

  /**
   * Enthält eine Zuordnung von HashableComponent Objekten, die die
   * XTextDocumente repräsentieren, auf die zugehörigen TextDocumentModels
   */
  private HashMap currentTextDocumentModels;

  /**
   * Enthält null (wenn noch keine Installations-Prüfung erfolgte) oder den
   * Zeitstempel der jungsten WollMuxInstallation, die auf dem System gefunden
   * wurde. Ändert sich dieser Zeitstempel im laufenden Betrieb, dann liegt eine
   * Mehrfachinstallation des WollMux vor
   * {@see WollMuxEventHandler.handleCheckInstallation()}.
   */
  private Date currentWollMuxInstallationDate;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zurückgeliefert.
    this.ctx = ctx;

    this.currentTextDocumentModels = new HashMap();

    this.currentWollMuxInstallationDate = null;

    // Initialisiere die UNO-Klasse, so dass auch mit dieser Hilfsklasse
    // gearbeitet werden kann.
    try
    {
      UNO.init(ctx.getServiceManager());
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    boolean successfulStartup = true;

    registeredPALChangeListener = new Vector();

    registeredDocumentEventListener = new Vector();

    WollMuxFiles.setupWollMuxDir();

    Logger.debug("StartupWollMux");
    Logger.debug("Build-Info: " + getBuildInfo());
    Logger.debug("wollmuxConfFile = "
                 + WollMuxFiles.getWollMuxConfFile().toString());
    Logger.debug("DEFAULT_CONTEXT \""
                 + WollMuxFiles.getDEFAULT_CONTEXT().toString()
                 + "\"");
    Logger.debug("CONF_VERSION: " + getConfVersionInfo());

    /*
     * Datenquellen/Registriere Abschnitte verarbeiten. ACHTUNG! Dies muss vor
     * getDatasourceJoiner() geschehen, da die entsprechenden Datenquellen
     * womöglich schon für WollMux-Datenquellen benötigt werden.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(), WollMuxFiles
        .getDEFAULT_CONTEXT());

    // Versuchen, den DJ zu initialisieren und Flag setzen, falls nicht
    // erfolgreich.
    if (getDatasourceJoiner() == null) successfulStartup = false;

    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions()
     * erfolgen. Als context wird null übergeben, weil globale Funktionen keinen
     * Kontext haben. TODO Überlegen, ob ein globaler Kontext doch Sinn machen
     * könnte. Dadurch könnten globale Funktionen globale Funktionsdialoge
     * darstellen, die global einheitliche Werte haben.
     */
    funcDialogs = WollMuxFiles.parseFunctionDialogs(WollMuxFiles
        .getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * müssen also vorher geparst sein. Als context wird null übergeben, weil
     * globale Funktionen keinen Kontext haben.
     */
    globalFunctions = WollMuxFiles.parseFunctions(
        WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(),
        null,
        null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions = WollMuxFiles.parsePrintFunctions(WollMuxFiles
        .getWollmuxConf());

    // Initialisiere EventProcessor
    WollMuxEventHandler.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster = UnoService.createWithContext(
          "com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster().addEventListener(
          new GlobalEventListener());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // setzen von shortcuts
    ConfigThingy tastenkuerzel = new ConfigThingy("");
    try
    {
      tastenkuerzel = WollMuxFiles.getWollmuxConf().query("Tastenkuerzel")
          .getLastChild();
    }
    catch (NodeNotFoundException e)
    {
    }
    try
    {
      Shortcuts.createShortcuts(tastenkuerzel);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    // "Extras->Seriendruck (WollMux)" erzeugen:
    createMenuButton(
        DispatchHandler.DISP_wmSeriendruck,
        "Seriendruck (WollMux)",
        ".uno:ToolsMenu",
        ".uno:MailMergeWizard");
    // "Help->Info über WollMux" erzeugen:
    createMenuButton(
        DispatchHandler.DISP_wmAbout,
        "Info über Vorlagen und Formulare (WollMux)",
        ".uno:HelpMenu",
        ".uno:About");

    // Setzen der in den Abschnitten OOoEinstellungen eingestellten
    // Konfigurationsoptionen
    ConfigThingy oooEinstellungenConf = WollMuxFiles.getWollmuxConf().query(
        "OOoEinstellungen");
    // ggf. fest verdrahtete Standardeinstellungen verwenden
    // Solange wir dieses Fallback-Verhalten haben (es soll 2008 entfernt
    // werden, (siehe R5973)), wenden wir es auch an, wenn ein leerer
    // OOoEinstellungen-Abschnitt gefunden wird. Wir wollen nicht, dass
    // besonders "clevere" Admins unsere Fehlermeldung totmachen, indem sie
    // einfach einen leeren Abschnitt anlegen, ohne dass sie wissen, was sie
    // damit anrichten.
    if (oooEinstellungenConf.count() == 0
        || ((ConfigThingy) oooEinstellungenConf.iterator().next()).count() == 0)
      try
      {
        oooEinstellungenConf = new ConfigThingy("DefaultSettings",
            OOOEINSTELLUNGEN_URL);
        Logger
            .error("Kein Konfigurationsabschnitt OOoEinstellungen gefunden => Verwende interne Vorgabe. ACHTUNG! Dieses Fallback-Verhalten wird mittelfristig entfernt. Bitte updaten Sie auf eine neue Standardkonfig, oder falls Sie dies nicht können/wollen wenden Sie sich an D-III-ITD-5.1");
      }
      catch (java.lang.Exception e)
      {
      }
    for (Iterator iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy settings = (ConfigThingy) iter.next();
      setConfigurationValues(settings);
    }
  }

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux
   * noch nicht initialisiert, so liefert die Methode null!
   * 
   * @return Instanz des WollMuxSingletons oder null.
   */
  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch
   * nicht initialisiert wurde)
   */
  public static void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);

      // Prüfen ob Dippelt- oder Halbinstallation vorliegt.
      WollMuxEventHandler.handleCheckInstallation();

      // Event ON_FIRST_INITIALIZE erzeugen:
      WollMuxEventHandler.handleInitialize();
    }
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zurück. Der Build-Status wird während dem
   * Build-Prozess mit dem Kommando "svn info" auf das Projektverzeichnis
   * erstellt. Die Buildinfo-Datei buildinfo enthält die Paketnummer und die
   * svn-Revision und ist im WollMux.uno.pkg-Paket sowie in der
   * WollMux.uno.jar-Datei abgelegt.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende
   * Ersatzmeldung erzeugt (siehe Sourcecode).
   * 
   * @return Der Build-Status der aktuellen WollMux-Installation.
   */
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxSingleton.class.getClassLoader()
          .getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in = new BufferedReader(new InputStreamReader(url
            .openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {
    }
    return "Version: unbekannt";
  }

  /**
   * Diese Methode liefert die Versionsinformation der aktuell verwendeten
   * wollmux-Konfiguration (z.B. "wollmux-standard-config-2.2.1") als String
   * zurück, wenn in der Konfiguration ein entsprechender CONF_VERSION-Schlüssel
   * definiert ist, oder "unbekannt", falls der dieser Schlüssel nicht
   * existiert.
   * 
   * @return Der Versionsinformation der aktuellen WollMux-Konfiguration (falls
   *         definiert) oder "unbekannt", falls nicht.
   */
  public String getConfVersionInfo()
  {
    ConfigThingy versions = getWollmuxConf().query("CONF_VERSION");
    try
    {
      return versions.getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      return "unbekannt";
    }
  }

  /**
   * @return Returns the xComponentContext.
   */
  public XComponentContext getXComponentContext()
  {
    return ctx;
  }

  /**
   * Diese Methode liefert eine Instanz auf den aktuellen DatasourceJoiner
   * zurück.
   * 
   * @return Returns the datasourceJoiner.
   */
  public DatasourceJoiner getDatasourceJoiner()
  {
    return WollMuxFiles.getDatasourceJoiner();
  }

  /**
   * Verarbeitet alle Datenquellen/Registriere-Unterabschnitte von conf und
   * registriert die entsprechenden Datenquellen in OOo, falls dort noch nicht
   * vorhanden.
   * 
   * @param context
   *          gibt an relativ zu was relative URLs aufgelöst werden sollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void registerDatasources(ConfigThingy conf, URL context)
  {
    Iterator iter = conf.query("Datenquellen").query("Registriere").iterator();
    while (iter.hasNext())
    {
      ConfigThingy regConf = (ConfigThingy) iter.next();
      String name;
      try
      {
        name = regConf.get("NAME").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
            "NAME-Attribut fehlt in Datenquellen/Registriere-Abschnitt",
            e);
        continue;
      }

      String urlStr;
      try
      {
        urlStr = regConf.get("URL").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
            "URL-Attribut fehlt in Datenquellen/Registriere-Abschnitt für Datenquelle \""
                + name
                + "\"",
            e);
        continue;
      }

      try
      {
        if (UNO.XNameAccess(UNO.dbContext).hasByName(name))
        {
          try
          {
            if (!regConf.get("REFRESH").toString().equalsIgnoreCase("true"))
              continue;

            // hierher (und damit weiter ohne continue) kommen wir nur, wenn
            // ein REFRESH-Abschnitt vorhanden ist und "true" enthält.
          }
          catch (java.lang.Exception x) // vor allem NodeNotFoundException
          {
            continue;
          }
        }
      }
      catch (java.lang.Exception x)
      {
        Logger.error("Fehler beim Überprüfen, ob Datenquelle \""
                     + name
                     + "\" bereits registriert ist", x);
      }

      Logger.debug("Versuche, Datenquelle \""
                   + name
                   + "\" bei OOo zu registrieren für URL \""
                   + urlStr
                   + "\"");

      String parsedUrl;
      try
      {
        URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      }
      catch (java.lang.Exception x)
      {
        Logger.error("Fehler beim Registrieren von Datenquelle \""
                     + name
                     + "\": Illegale URL: \""
                     + urlStr
                     + "\"", x);
        continue;
      }

      try
      {
        Object datasource = UNO.XNameAccess(UNO.dbContext).getByName(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(
            UNO.dbContext.getRegisteredObject(name),
            datasource))
          Logger.error("Testzugriff auf Datenquelle \""
                       + name
                       + "\" nach Registrierung fehlgeschlagen");
      }
      catch (Exception x)
      {
        Logger.error("Fehler beim Registrieren von Datenquelle \""
                     + name
                     + "\". Stellen Sie sicher, dass die URL \""
                     + parsedUrl
                     + "\" gültig ist.", x);
        continue;
      }

    }
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates
   * empfängt wenn sich die PAL ändert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addPALChangeEventListener()");

    if (listener == null) return;

    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode registriert einen XEventListener, der Nachrichten empfängt
   * wenn sich der Status der Dokumentbearbeitung ändert (z.B. wenn ein Dokument
   * vollständig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void addDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addDocumentEventListener()");

    if (listener == null) return;

    Iterator i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredDocumentEventListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removePALChangeEventListener()");
    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Diese Methode deregistriert einen XEventListener wenn er bereits
   * registriert war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void removeDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removeDocumentEventListener()");
    Iterator i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Erzeugt einen persistenten Menüeintrag mit der KommandoUrl cmdUrl und dem
   * Label label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmenü
   * des Writers und ordnet ihn direkt oberhalb des bereits bestehenden
   * Menüpunktes mit der URL insertBeforeElementUrl an. Ist der Button bereits
   * definiert, so wird der bestehende Eintrag vorher gelöscht.
   */
  private static void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      // Menüleiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl = UNO
          .XModuleUIConfigurationManagerSupplier(UNO
              .createUNOService("com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr = UNO.XUIConfigurationManager(suppl
          .getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO.XIndexAccess(cfgMgr.getSettings(
          settingsUrl,
          true));

      // Elemente des .uno:ToolsMenu besorgen:
      XIndexContainer toolsMenu = null;
      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        toolsMenu = UNO.XIndexContainer(desc
            .getPropertyValue("ItemDescriptorContainer"));
      }

      // Seriendruck-Button löschen, wenn er bereits vorhanden ist.
      idx = findElementWithCmdURL(toolsMenu, cmdUrl);
      if (idx >= 0) toolsMenu.removeByIndex(idx);

      // SeriendruckAssistent suchen
      idx = findElementWithCmdURL(toolsMenu, insertBeforeElementUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps();
        desc.setPropertyValue("CommandURL", cmdUrl);
        desc.setPropertyValue("Type", FormButtonType.PUSH);
        desc.setPropertyValue("Label", label);
        toolsMenu.insertByIndex(idx, desc.getProps());
        cfgMgr.replaceSettings(settingsUrl, menubar);
        UNO.XUIConfigurationPersistence(cfgMgr).store();
      }
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Liefert den Index des ersten Menüelements aus dem Menü menu zurück, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element
   * gefunden wurde.
   * 
   * @return Liefert den Index des ersten Menüelements mit CommandURL cmdUrl
   *         oder -1.
   */
  private static int findElementWithCmdURL(XIndexAccess menu, String cmdUrl)
  {
    try
    {
      for (int i = 0; i < menu.getCount(); ++i)
      {
        PropertyValue[] desc = (PropertyValue[]) menu.getByIndex(i);
        for (int j = 0; j < desc.length; j++)
        {
          if ("CommandURL".equals(desc[j].Name) && cmdUrl.equals(desc[j].Value))
            return i;
        }
      }
    }
    catch (java.lang.Exception e)
    {
    }
    return -1;
  }

  /**
   * Setzt die im ConfigThingy übergebenen OOoEinstellungen-Abschnitt
   * enthaltenen Einstellungen in der OOo-Registry.
   * 
   * @param oooEinstellungenConf
   *          Der Knoten OOoEinstellungen eines solchen Abschnitts.
   */
  private static void setConfigurationValues(ConfigThingy oooEinstellungenConf)
  {
    for (Iterator iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();
      try
      {
        String node = element.get("NODE").toString();
        String prop = element.get("PROP").toString();
        String type = element.get("TYPE").toString();
        String value = element.get("VALUE").toString();
        Object v = getObjectByType(type, value);

        setConfigurationValue(node, prop, v);
      }
      catch (java.lang.Exception e)
      {
        Logger.error("OOoEinstellungen: Konnte Einstellung '"
                     + element.stringRepresentation()
                     + "'nicht setzen:", e);
      }
    }
  }

  /**
   * Konvertiert den als String übergebenen Wert value in ein Objekt vom Typ
   * type oder liefert eine IllegalArgumentException, wenn die Werte nicht
   * konvertiert werden können.
   * 
   * @param type
   *          Der Typ in den konvertiert werden soll ('boolean', 'integer',
   *          'float', 'string').
   * @param value
   *          Der zu konvertierende Wert.
   * @return Das neue Objekt vom entsprechenden Typ.
   * @throws IllegalArgumentException
   *           type oder value sind ungültig oder fehlerhaft.
   */
  private static Object getObjectByType(String type, String value)
      throws IllegalArgumentException
  {
    if (type.equalsIgnoreCase("boolean"))
    {
      return new Boolean(value);
    }
    else if (type.equalsIgnoreCase("integer"))
    {
      return new Integer(value);
    }
    else if (type.equalsIgnoreCase("float"))
    {
      return new Float(value);
    }
    else if (type.equalsIgnoreCase("string"))
    {
      return new String(value);
    }

    throw new IllegalArgumentException(
        "Der TYPE '"
            + type
            + "' ist nicht gültig. Gültig sind 'boolean', 'integer', 'float' und 'string'.");
  }

  /**
   * Setzt eine Einstellung value in der OOo-Registry, wobei die Position im
   * Registry-Baum durch node und prop beschrieben wird.
   * 
   * @param node
   *          z.B. "/org.openoffice.Inet/Settings"
   * @param prop
   *          z.B. "ooInetProxyType"
   * @param value
   *          der zu setzende Wert als Objekt vom entsprechenden Typ.
   */
  private static void setConfigurationValue(String node, String prop,
      Object value)
  {
    XChangesBatch updateAccess = UNO.getConfigurationUpdateAccess(node);
    if (value != null) UNO.setProperty(updateAccess, prop, value);
    if (updateAccess != null) try
    {
      updateAccess.commitChanges();
    }
    catch (WrappedTargetException e)
    {
      Logger.error(e);
    }
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   * 
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  public Iterator palChangeListenerIterator()
  {
    return registeredPALChangeListener.iterator();
  }

  /**
   * Liefert einen Iterator auf alle registrierten XEventListener-Objekte, die
   * über Änderungen am Status der Dokumentverarbeitung informiert werden
   * sollen.
   * 
   * @return Iterator auf alle registrierten XEventListener-Objekte.
   */
  public Iterator documentEventListenerIterator()
  {
    return registeredDocumentEventListener.iterator();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste aller
   * Einträge der Persönlichen Absenderliste (PAL) in einem String-Array, wobei
   * die einzelnen Einträge in der Form "<Nachname>, <Vorname> (<Rolle>)"
   * sind.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
  public String[] getPALEntries()
  {
    DJDatasetListElement[] pal = getSortedPALEntries();
    String[] elements = new String[pal.length];
    for (int i = 0; i < pal.length; i++)
    {
      elements[i] = pal[i].toString();
    }
    return elements;
  }

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Persönlichen
   * Absenderliste (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * @return alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = getDatasourceJoiner().getLOS();

    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] = new DJDatasetListElement((DJDataset) iter.next());
    Arrays.sort(elements);

    return elements;
  }

  /**
   * Diese Methode liefert den aktuell aus der persönlichen Absenderliste (PAL)
   * ausgewählten Absender im Format "<Nachname>, <Vorname> (<Rolle>)" zurück.
   * Ist die PAL leer oder noch kein Absender ausgewählt, so liefert die Methode
   * den Leerstring "" zurück. Dieser Sonderfall sollte natürlich entsprechend
   * durch die aufrufende Methode behandelt werden.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   * 
   * @return den aktuell aus der PAL ausgewählten Absender als String. Ist kein
   *         Absender ausgewählt wird der Leerstring "" zurückgegeben.
   */
  public String getCurrentSender()
  {
    try
    {
      DJDataset selected = getDatasourceJoiner().getSelectedDataset();
      return new DJDatasetListElement(selected).toString();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }

  /**
   * siehe {@link WollMuxFiles#getWollmuxConf()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getWollmuxConf()
  {
    return WollMuxFiles.getWollmuxConf();
  }

  /**
   * siehe {@link WollMuxFiles#getDEFAULT_CONTEXT()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public URL getDEFAULT_CONTEXT()
  {
    return WollMuxFiles.getDEFAULT_CONTEXT();
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen
   * enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Druckfunktionen
   * enthält.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Liefert die Dialogbibliothek, die die Dialoge enthält, die in Funktionen
   * (Grundfunktion "DIALOG") verwendung finden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

  /**
   * Liefert null (wenn noch keine Installations-Prüfung erfolgte) oder den
   * Zeitstempel der jungsten WollMuxInstallation, die auf dem System gefunden
   * wurde. Ändert sich dieser Zeitstempel im laufenden Betrieb, dann liegt eine
   * Mehrfachinstallation des WollMux vor
   * {@see WollMuxEventHandler.handleCheckInstallation()}.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Date getCurrentWollMuxInstallationDate()
  {
    return currentWollMuxInstallationDate;
  }

  /**
   * Ermöglicht das Setzen des Datums der jüngsten WollMux-Installation, die in
   * WollMuxEventHandler.handleCheckInstallation(...) auf dem System gefunden
   * wurde.
   * 
   * @param currentWollMuxInstallationDate
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setCurrentWollMuxInstallationDate(
      Date currentWollMuxInstallationDate)
  {
    this.currentWollMuxInstallationDate = currentWollMuxInstallationDate;
  }

  /**
   * siehe {@link WollMuxFiles#isDebugMode()}.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public boolean isDebugMode()
  {
    return WollMuxFiles.isDebugMode();
  }

  /**
   * Diese Methode tut nichts ausser zu prüfen, ob es sich bei dem übergebenen
   * String id um einen gültigen Bezeichner gemäß der Syntax für
   * WollMux-Config-Dateien handelt und im negativen Fall eine
   * InvalidIdentifierException zu werfen.
   * 
   * @param id
   *          zu prüfende ID
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @throws InvalidIdentifierException
   */
  public static void checkIdentifier(String id)
      throws InvalidIdentifierException
  {
    if (!id.matches("^[a-zA-Z_][a-zA-Z_0-9]*$"))
      throw new InvalidIdentifierException(id);
  }

  public static class InvalidIdentifierException extends java.lang.Exception
  {
    private static final long serialVersionUID = 495666967644874471L;

    private String invalidId;

    public InvalidIdentifierException(String invalidId)
    {
      this.invalidId = invalidId;
    }

    public String getMessage()
    {
      return "Der Bezeichner '"
             + invalidId
             + "' ist ungültig, und darf nur die Zeichen a-z, A-Z, _ und 0-9 enthalten, wobei das erste Zeichen keine Ziffer sein darf.";
    }
  }

  /**
   * Liefert true, wenn für das XTextDocument doc ein TextDocumentModel im
   * WollMuxSingleton registriert ist, ansonsten false.
   * 
   * @param doc
   *          das Dokument für das ein TextDocumentModel gesucht wird.
   * @return true, wenn für das XTextDocument doc ein TextDocumentModel im
   *         WollMuxSingleton registriert ist, ansonsten false.
   */
  public boolean hasTextDocumentModel(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);

    return currentTextDocumentModels.containsKey(key);
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum übergebenen XTextDocument doc;
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt
   * und das neu erzeugte zurück geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugehörige TextDocumentModel
   *          zurückgeliefert werden soll.
   * @return Das zu doc zugehörige TextDocumentModel.
   */
  public TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);

    TextDocumentModel model = (TextDocumentModel) currentTextDocumentModels
        .get(key);
    if (model == null)
    {
      // Neues TextDocumentModel erzeugen, wenn es noch nicht existiert.
      model = new TextDocumentModel(doc);
      currentTextDocumentModels.put(key, model);
    }
    return model;
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum übergebenen XFrame frame oder
   * null, falls dem Frame kein entsprechendes TextDocumentModel zugeordnet
   * werden kann.
   * 
   * ACHTUNG: Die Methode wird vom DispatchHandler verwendet, um das
   * TextDocumentModel eines Frames zu bestimmen und wird daher NICHT mit dem
   * WollMuxEventHandler synchronisiert aufgerufen. Daher darf z.B. im Gegensatz
   * zu getTextDocumentModel(XTextDocument) hier auch kein TextDocumentModel
   * erzeugt werden, wenn es nicht bereits existiert.
   * 
   * Der Zugriff auf das TextDocumentModel in der nicht synchronisierten
   * Aufrufmethode des DispatchHandler darf daher auch auschließend lesend sein.
   * 
   * @param frame
   *          Der Frame, über den das TextDocumentModel gesucht werden soll.
   * @return Das zu frame zugehörige TextDocumentModel oder null, falls keine
   *         Zuordnung gefunden werden konnte.
   */
  public TextDocumentModel getTextDocumentModelForFrame(XFrame frame)
  {
    if (frame == null) return null;

    try
    {
      HashableComponent key = new HashableComponent(frame.getController()
          .getModel());
      return (TextDocumentModel) currentTextDocumentModels.get(key);
    }
    catch (java.lang.Exception e)
    {
      return null;
    }
  }

  /**
   * Löscht das übergebene TextDocumentModel von doc aus der internen Liste
   * aller aktuellen TextDocumentModels.
   * 
   * @param doc
   *          Das XTextDocument, dessen zugehöriges TextDocumentModel aus der
   *          internen Liste gelöscht werden soll.
   */
  public void disposedTextDocument(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);
    currentTextDocumentModels.remove(key);
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene AnnotationField oder null, wenn keines
   *          gefunden wurde.
   */
  public static XTextField findAnnotationFieldRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          Object child = xEnum.nextElement();
          XTextField found = findAnnotationFieldRecursive(child);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch schauen, ob es sich bei dem Element um eine Annotation
    // handelt:
    if (UNO.XTextField(element) != null)
    {
      Object textField = UNO.getProperty(element, "TextField");
      if (UNO.supportsService(
          textField,
          "com.sun.star.text.TextField.Annotation"))
      {
        return UNO.XTextField(textField);
      }
    }

    return null;
  }

  /**
   * Überprüft, ob von url gelesen werden kann und wirft eine IOException, falls
   * nicht.
   * 
   * @throws IOException
   *           falls von url nicht gelesen werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void checkURL(URL url) throws IOException
  {
    url.openStream().close();
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von
   * Informationen und kehrt erst nach Beendigung des Dialogs wieder zurück. Der
   * sichtbare Text wird dabei ab einer Länge von 50 Zeichen automatisch
   * umgebrochen.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage)
  {
    showInfoModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von
   * Informationen und kehrt erst nach Beendigung des Dialogs wieder zurück.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   */
  public static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage, int margin)
  {
    try
    {
      // zu lange Strings ab margin Zeichen umbrechen:
      String formattedMessage = "";
      String[] lines = sMessage.split("\n");
      for (int i = 0; i < lines.length; i++)
      {
        String[] words = lines[i].split(" ");
        int chars = 0;
        for (int j = 0; j < words.length; j++)
        {
          String word = words[j];
          if (margin > 0 && chars > 0 && chars + word.length() > margin)
          {
            formattedMessage += "\n";
            chars = 0;
          }
          formattedMessage += word + " ";
          chars += word.length() + 1;
        }
        if (i != lines.length - 1) formattedMessage += "\n";
      }

      // infobox ausgeben:
      Common.setLookAndFeelOnce();

      JOptionPane pane = new JOptionPane(formattedMessage,
          javax.swing.JOptionPane.INFORMATION_MESSAGE);
      JDialog dialog = pane.createDialog(null, sTitle);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Holt sich den Frame von doc, führt auf diesem ein queryDispatch() mit der
   * zu urlStr gehörenden URL aus und liefert den Ergebnis XDispatch zurück oder
   * null, falls der XDispatch nicht verfügbar ist.
   * 
   * @param doc
   *          Das Dokument, dessen Frame für den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht
   *         verfügbar ist.
   */
  public static XDispatch getDispatchForModel(XModel doc,
      com.sun.star.util.URL url)
  {
    if (doc == null) return null;

    XDispatchProvider dispProv = null;
    try
    {
      dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());
    }
    catch (java.lang.Exception e)
    {
    }

    if (dispProv != null)
    {
      return dispProv.queryDispatch(
          url,
          "_self",
          com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Der GlobalEventListener sorgt dafür, dass der WollMux alle wichtigen
   * globalen Ereignisse wie z.B. ein OnNew on OnLoad abfangen und darauf
   * reagieren kann. In diesem Fall wird die Methode notifyEvent aufgerufen.
   * 
   * @author christoph.lutz
   */
  public static class GlobalEventListener implements
      com.sun.star.document.XEventListener
  {
    public void notifyEvent(com.sun.star.document.EventObject docEvent)
    {
      XTextDocument doc = UNO.XTextDocument(docEvent.Source);

      if (doc != null)
      {
        Logger.debug2("Incoming documentEvent for #"
                      + doc.hashCode()
                      + ": "
                      + docEvent.EventName);

        if (docEvent.EventName.equalsIgnoreCase("OnLoad")
            || docEvent.EventName.equalsIgnoreCase(("OnNew")))
        {
          // Verarbeitung von TextDocuments anstossen:
          WollMuxEventHandler.handleProcessTextDocument(doc);
        }
      }
      else
      {
        WollMuxEventHandler.handleNotifyDocumentEventListener(
            "OnWollMuxProcessingFinished",
            docEvent.Source);
      }
    }

    public void disposing(EventObject arg0)
    {
      // nothing to do
    }
  }
}
