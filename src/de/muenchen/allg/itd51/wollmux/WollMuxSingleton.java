/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton für zentrale WollMux-Methoden.
 * 
 * Copyright (c) 2010-2016 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
 * 16.12.2009 | ERT | Cast XTextField-Interface entfernt
 * 07.04.2010 | BED | Konfigurierbares SENDER_DISPLAYTEMPLATE 
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
import com.sun.star.frame.XModel;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.XInterface;
import com.sun.star.util.XChangesBatch;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;
import de.muenchen.allg.itd51.wollmux.event.GlobalEventListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.StandardPrint;

/**
 * Diese Klasse ist ein Singleton, welches den WollMux initialisiert und alle
 * zentralen WollMux-Methoden zur Verfügung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der früher zentraler Anlaufpunkt war,
 * bedient sich größtenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton implements XPALProvider
{
  public static final String OVERRIDE_FRAG_DB_SPALTE = "OVERRIDE_FRAG_DB_SPALTE";

  /**
   * Default-Wert für {@link #senderDisplayTemplate}, wenn kein Wert in der
   * Konfiguration explizit angegeben ist.
   * 
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_SENDER_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";

  /**
   * Der String, der in der String-Repräsentation von PAL-Einträgen, den Schlüssel
   * des PAL-Eintrags vom Rest des PAL-Eintrags abtrennt (siehe auch Dokumentation
   * der Methoden des {@link XPALProvider}-Interfaces.
   */
  public static final String SENDER_KEY_SEPARATOR = "§§%=%§§";

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Versionsstring des WollMux.
   */
  private String version = null;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary globalFunctions;

  /**
   * Enthält die im Dokumentaktionen der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary documentActionFunctions;

  /**
   * Enthält die im Funktionsdialoge-Abschnitt der wollmux,conf definierten Dialoge.
   */
  private DialogLibrary funcDialogs;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  /**
   * Der Wert von {@link #OVERRIDE_FRAG_DB_SPALTE}, d,h, der Name der Spalte, die die
   * persönliche OverrideFrag-Liste enthält. "" falls nicht definiert.
   */
  private String overrideFragDbSpalte;

  /**
   * Gibt an, wie die String-Repräsentation von PAL-Einträgen aussehen, die über die
   * XPALProvider-Methoden zurückgeliefert werden. Syntax mit %{Spalte} um
   * entsprechenden Wert des Datensatzes anzuzeigen, z.B. "%{Nachname}, %{Vorname}"
   * für die Anzeige in der Form "Meier, Hans" etc. Kann in der WollMux-Konfiguration
   * über SENDER_DISPLAYTEMPLATE gesetzt werden.
   */
  private String senderDisplayTemplate;

  /**
   * Enthält den default XComponentContext in dem der WollMux (bzw. das OOo) läuft.
   */
  private XComponentContext ctx;

  /**
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private Vector<XPALChangeEventListener> registeredPALChangeListener;

  /**
   * Enthält alle registrierten XEventListener, die bei Statusänderungen der
   * Dokumentbearbeitung informiert werden.
   */
  private Vector<XEventListener> registeredDocumentEventListener;

  /**
   * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
   */
  private DocumentManager docManager;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zurückgeliefert.
    this.ctx = ctx;

    this.docManager = new DocumentManager();

    // Initialisiere die UNO-Klasse, so dass auch mit dieser Hilfsklasse
    // gearbeitet werden kann.
    try
    {
      UNO.init(ctx.getServiceManager());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    boolean successfulStartup = true;

    registeredPALChangeListener = new Vector<XPALChangeEventListener>();

    registeredDocumentEventListener = new Vector<XEventListener>();

    WollMuxFiles.setupWollMuxDir();

    Logger.debug(L.m("StartupWollMux"));
    Logger.debug("Build-Info: " + getBuildInfo());
    Logger.debug("wollmuxConfFile = " + WollMuxFiles.getWollMuxConfFile().toString());
    Logger.debug("DEFAULT_CONTEXT \"" + WollMuxFiles.getDEFAULT_CONTEXT().toString()
      + "\"");
    Logger.debug("CONF_VERSION: " + getConfVersionInfo());

    /*
     * Datenquellen/Registriere Abschnitte verarbeiten. ACHTUNG! Dies muss vor
     * getDatasourceJoiner() geschehen, da die entsprechenden Datenquellen womöglich
     * schon für WollMux-Datenquellen benötigt werden.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(),
      WollMuxFiles.getDEFAULT_CONTEXT());

    // Versuchen, den DJ zu initialisieren und Flag setzen, falls nicht
    // erfolgreich.
    if (getDatasourceJoiner() == null) successfulStartup = false;

    // Setzen von senderDisplayTemplate
    this.senderDisplayTemplate = DEFAULT_SENDER_DISPLAYTEMPLATE;
    try
    {
      this.senderDisplayTemplate =
        WollMuxFiles.getWollmuxConf().query("SENDER_DISPLAYTEMPLATE").getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      Logger.log(L.m(
        "Keine Einstellung für SENDER_DISPLAYTEMPLATE gefunden! Verwende Fallback: %1",
        DEFAULT_SENDER_DISPLAYTEMPLATE));
      // SENDER_DISPLAYTEMPLATE sollte eigentlich verpflichtend sein und wir
      // sollten an dieser Stelle einen echten Error loggen bzw. eine
      // Meldung in der GUI ausgeben und evtl. sogar abbrechen. Wir tun
      // dies allerdings nicht, da das SENDER_DISPLAYTEMPLATE erst mit
      // WollMux 6.4.0 eingeführt wurde und wir abwärtskompatibel zu alten
      // WollMux-Konfigurationen bleiben müssen und Benutzer alter
      // Konfigurationen nicht mit Error-Meldungen irritieren wollen.
      // Dies ist allerdings nur eine Übergangslösung. Die obige Meldung
      // sollte nach ausreichend Zeit genauso wie DEFAULT_SENDER_DISPLAYTEMPLATE
      // entfernt werden (bzw. wie oben gesagt überarbeitet).
    }

    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions()
     * erfolgen. Als context wird null übergeben, weil globale Funktionen keinen
     * Kontext haben. TODO Überlegen, ob ein globaler Kontext doch Sinn machen
     * könnte. Dadurch könnten globale Funktionen globale Funktionsdialoge
     * darstellen, die global einheitliche Werte haben.
     */
    funcDialogs =
      WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * müssen also vorher geparst sein. Als context wird null übergeben, weil globale
     * Funktionen keinen Kontext haben.
     */
    globalFunctions =
      WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(), null, null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions =
      WollMuxFiles.parsePrintFunctions(WollMuxFiles.getWollmuxConf());
    StandardPrint.addInternalDefaultPrintFunctions(globalPrintFunctions);

    /*
     * Dokumentaktionen parsen. Diese haben weder Kontext noch Dialoge.
     */
    documentActionFunctions = new FunctionLibrary(null, true);
    WollMuxFiles.parseFunctions(documentActionFunctions,
      WollMuxFiles.getWollmuxConf(), "Dokumentaktionen", null, null);

    // Initialisiere EventProcessor
    WollMuxEventHandler.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster =
        UnoService.createWithContext("com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster().addEventListener(
        new GlobalEventListener(docManager));
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    /*
     * FIXME: Darf nur im Falle des externen WollMux gemacht werden, da ansonsten
     * endlosschleifen mit dem ProtocolHandler möglich sind. Evtl. auch lösbar
     * dadurch, dass URLS, die mit ignorecase("wollmux:") anfangen, niemals an den
     * Slave delegiert werden. Ist aber nicht so schön als Lösung.
     * UNO.XDispatchProviderInterception
     * (UNO.desktop).registerDispatchProviderInterceptor(
     * DispatchHandler.globalWollMuxDispatches);
     */

    // setzen von shortcuts
    ConfigThingy tastenkuerzel = new ConfigThingy("");
    try
    {
      tastenkuerzel =
        WollMuxFiles.getWollmuxConf().query("Tastenkuerzel").getLastChild();
    }
    catch (NodeNotFoundException e)
    {}
    try
    {
      Shortcuts.createShortcuts(tastenkuerzel);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // "Extras->Seriendruck (WollMux)" erzeugen:
    List<String> removeButtonsFor = new ArrayList<String>();
    removeButtonsFor.add(Dispatch.DISP_wmSeriendruck);
    createMenuButton(Dispatch.DISP_wmSeriendruck, L.m("Seriendruck (WollMux)"),
      ".uno:ToolsMenu", ".uno:MailMergeWizard", removeButtonsFor);

    // "Help->Info über WollMux" erzeugen:
    removeButtonsFor.clear();
    removeButtonsFor.add(Dispatch.DISP_wmAbout);
    createMenuButton(Dispatch.DISP_wmAbout,
      L.m("Info über Vorlagen und Formulare (WollMux)"), ".uno:HelpMenu",
      ".uno:About", removeButtonsFor);

    // Setzen der in den Abschnitten OOoEinstellungen eingestellten
    // Konfigurationsoptionen
    ConfigThingy oooEinstellungenConf =
      WollMuxFiles.getWollmuxConf().query("OOoEinstellungen");
    for (Iterator<ConfigThingy> iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy settings = iter.next();
      setConfigurationValues(settings);
    }
  }

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux noch
   * nicht initialisiert, so liefert die Methode null!
   * 
   * @return Instanz des WollMuxSingletons oder null.
   */
  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch nicht
   * initialisiert wurde)
   */
  public static synchronized void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);

      // Prüfen ob Doppelt- oder Halbinstallation vorliegt.
      WollMuxEventHandler.handleCheckInstallation();

      // Event ON_FIRST_INITIALIZE erzeugen:
      WollMuxEventHandler.handleInitialize();
    }
  }

  /**
   * Liefert die Versionsnummer des WollMux (z.B. "5.9.2") zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public String getVersion()
  {
    if (version == null) getBuildInfo();
    return version;
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zurück. Der Build-Status wird während dem Build-Prozess mit
   * dem Kommando "svn info" auf das Projektverzeichnis erstellt. Die Buildinfo-Datei
   * buildinfo enthält die Paketnummer und die svn-Revision und ist im
   * WollMux.oxt-Paket sowie in der WollMux.uno.jar-Datei abgelegt.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende Ersatzmeldung
   * erzeugt (siehe Sourcecode).
   * 
   * @return Der Build-Status der aktuellen WollMux-Installation.
   */
  public String getBuildInfo()
  {
    version = "unknown";
    BufferedReader in = null;
    try
    {
      URL url = WollMuxSingleton.class.getClassLoader().getResource("buildinfo");
      if (url != null)
      {
        in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str = in.readLine();
        if (str != null)
        {
          if (str.startsWith("Version: "))
            version = str.substring(9, str.indexOf(','));
          return str;
        }
      }
    }
    catch (Exception x)
    {}
    finally
    {
      try
      {
        in.close();
      }
      catch (Exception y)
      {}
    }

    return L.m("Version: unbekannt");
  }

  /**
   * Diese Methode liefert die Versionsinformation der aktuell verwendeten
   * wollmux-Konfiguration (z.B. "wollmux-standard-config-2.2.1") als String zurück,
   * wenn in der Konfiguration ein entsprechender CONF_VERSION-Schlüssel definiert
   * ist, oder "unbekannt", falls der dieser Schlüssel nicht existiert.
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
      return L.m("unbekannt");
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
   * Diese Methode liefert eine Instanz auf den aktuellen DatasourceJoiner zurück.
   * 
   * @return Returns the datasourceJoiner.
   */
  public DatasourceJoiner getDatasourceJoiner()
  {
    return WollMuxFiles.getDatasourceJoiner();
  }

  /**
   * Diese Methode liefert eine Liste mit den über {@link #senderDisplayTemplate}
   * definierten String-Repräsentation aller verlorenen gegangenen Datensätze des
   * DatasourceJoiner (gemäß {@link DatasourceJoiner.Status.lostDatasets}) zurück.
   * Die genaue Form der String-Repräsentation ist abhängig von
   * {@link #senderDisplayTemplate}, das in der WollMux-Konfiguration über den Wert
   * von SENDER_DISPLAYTEMPLATE gesetzt werden kann. Gibt es keine verloren
   * gegangenen Datensätze, so bleibt die Liste leer.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public List<String> getsLostDatasetDisplayStrings()
  {
    DatasourceJoiner dj = getDatasourceJoiner();
    ArrayList<String> list = new ArrayList<String>();
    for (Dataset ds : dj.getStatus().lostDatasets)
      list.add(new DatasetListElement(ds, senderDisplayTemplate).toString());
    return list;
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
    Iterator<ConfigThingy> iter =
      conf.query("Datenquellen").query("Registriere").iterator();
    while (iter.hasNext())
    {
      ConfigThingy regConf = iter.next();
      String name;
      try
      {
        name = regConf.get("NAME").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
          L.m("NAME-Attribut fehlt in Datenquellen/Registriere-Abschnitt"), e);
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
          L.m(
            "URL-Attribut fehlt in Datenquellen/Registriere-Abschnitt für Datenquelle '%1'",
            name), e);
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
          catch (Exception x) // vor allem NodeNotFoundException
          {
            continue;
          }
        }
      }
      catch (Exception x)
      {
        Logger.error(L.m(
          "Fehler beim Überprüfen, ob Datenquelle '%1' bereits registriert ist",
          name), x);
      }

      Logger.debug(L.m(
        "Versuche, Datenquelle '%1' bei OOo zu registrieren für URL '%2'", name,
        urlStr));

      String parsedUrl;
      try
      {
        URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      }
      catch (Exception x)
      {
        Logger.error(L.m(
          "Fehler beim Registrieren von Datenquelle '%1': Illegale URL: '%2'", name,
          urlStr), x);
        continue;
      }

      try
      {
        Object datasource = UNO.XNameAccess(UNO.dbContext).getByName(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(UNO.dbContext.getRegisteredObject(name), datasource))
          Logger.error(L.m(
            "Testzugriff auf Datenquelle '%1' nach Registrierung fehlgeschlagen",
            name));
      }
      catch (Exception x)
      {
        Logger.error(
          L.m(
            "Fehler beim Registrieren von Datenquelle '%1'. Stellen Sie sicher, dass die URL '%2' gültig ist.",
            name, parsedUrl), x);
        continue;
      }

    }
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addPALChangeEventListener()");

    if (listener == null) return;

    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode registriert einen XEventListener, der Nachrichten empfängt wenn
   * sich der Status der Dokumentbearbeitung ändert (z.B. wenn ein Dokument
   * vollständig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void addDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addDocumentEventListener()");

    if (listener == null) return;

    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
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
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removePALChangeEventListener()");
    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Diese Methode deregistriert einen XEventListener wenn er bereits registriert
   * war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void removeDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removeDocumentEventListener()");
    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Erzeugt einen persistenten Menüeintrag mit der KommandoUrl cmdUrl und dem Label
   * label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmenü des Writers
   * und ordnet ihn direkt oberhalb des bereits bestehenden Menüpunktes mit der URL
   * insertBeforeElementUrl an. Alle Buttons, deren Url in der Liste removeCmdUrls
   * aufgeführt sind werden dabei vorher gelöscht (v.a. sollte cmdUrl aufgeführt
   * sein, damit nicht der selbe Button doppelt erscheint).
   */
  private static void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl,
      List<String> removeCmdUrls)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      // Menüleiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl =
        UNO.XModuleUIConfigurationManagerSupplier(UNO.createUNOService("com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr =
        UNO.XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO.XIndexAccess(cfgMgr.getSettings(settingsUrl, true));

      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        // Elemente des .uno:ToolsMenu besorgen:
        XIndexContainer toolsMenu =
          UNO.XIndexContainer(desc.getPropertyValue("ItemDescriptorContainer"));

        // Seriendruck-Button löschen, wenn er bereits vorhanden ist.
        for (String rCmdUrl : removeCmdUrls)
        {
          idx = findElementWithCmdURL(toolsMenu, rCmdUrl);
          if (idx >= 0) toolsMenu.removeByIndex(idx);
        }

        // SeriendruckAssistent suchen
        idx = findElementWithCmdURL(toolsMenu, insertBeforeElementUrl);
        if (idx >= 0)
        {
          UnoProps newDesc = new UnoProps();
          newDesc.setPropertyValue("CommandURL", cmdUrl);
          newDesc.setPropertyValue("Type", FormButtonType.PUSH);
          newDesc.setPropertyValue("Label", label);
          toolsMenu.insertByIndex(idx, newDesc.getProps());
          cfgMgr.replaceSettings(settingsUrl, menubar);
          UNO.XUIConfigurationPersistence(cfgMgr).store();
        }
      }
    }
    catch (Exception e)
    {}
  }

  /**
   * Liefert den Index des ersten Menüelements aus dem Menü menu zurück, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element gefunden
   * wurde.
   * 
   * @return Liefert den Index des ersten Menüelements mit CommandURL cmdUrl oder -1.
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
    catch (Exception e)
    {}
    return -1;
  }

  /**
   * Setzt die im ConfigThingy übergebenen OOoEinstellungen-Abschnitt enthaltenen
   * Einstellungen in der OOo-Registry.
   * 
   * @param oooEinstellungenConf
   *          Der Knoten OOoEinstellungen eines solchen Abschnitts.
   */
  private static void setConfigurationValues(ConfigThingy oooEinstellungenConf)
  {
    for (Iterator<ConfigThingy> iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy element = iter.next();
      try
      {
        String node = element.get("NODE").toString();
        String prop = element.get("PROP").toString();
        String type = element.get("TYPE").toString();
        String value = element.get("VALUE").toString();
        Object v = getObjectByType(type, value);

        setConfigurationValue(node, prop, v);
      }
      catch (Exception e)
      {
        Logger.error(L.m("OOoEinstellungen: Konnte Einstellung '%1'nicht setzen:",
          element.stringRepresentation()), e);
      }
    }
  }

  /**
   * Konvertiert den als String übergebenen Wert value in ein Objekt vom Typ type
   * oder liefert eine IllegalArgumentException, wenn die Werte nicht konvertiert
   * werden können.
   * 
   * @param type
   *          Der Typ in den konvertiert werden soll ('boolean', 'integer', 'float',
   *          'string').
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
      return Boolean.valueOf(value);
    }
    else if (type.equalsIgnoreCase("integer"))
    {
      return Integer.valueOf(value);
    }
    else if (type.equalsIgnoreCase("float"))
    {
      return Float.valueOf(value);
    }
    else if (type.equalsIgnoreCase("string"))
    {
      return value;
    }

    throw new IllegalArgumentException(
      L.m(
        "Der TYPE '%1' ist nicht gültig. Gültig sind 'boolean', 'integer', 'float' und 'string'.",
        type));
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
  private static void setConfigurationValue(String node, String prop, Object value)
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
  public Iterator<XPALChangeEventListener> palChangeListenerIterator()
  {
    return registeredPALChangeListener.iterator();
  }

  /**
   * Liefert einen Iterator auf alle registrierten XEventListener-Objekte, die über
   * Änderungen am Status der Dokumentverarbeitung informiert werden sollen.
   * 
   * @return Iterator auf alle registrierten XEventListener-Objekte.
   */
  public Iterator<XEventListener> documentEventListenerIterator()
  {
    return registeredDocumentEventListener.iterator();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste mit
   * String-Repräsentationen aller Einträge der Persönlichen Absenderliste (PAL) in
   * einem String-Array. Die genaue Form der String-Repräsentationen ist abhängig von
   * {@link #senderDisplayTemplate}, das in der WollMux-Konfiguration über den Wert
   * von SENDER_DISPLAYTEMPLATE gesetzt werden kann. Unabhängig von
   * {@link #senderDisplayTemplate} enthalten die über diese Methode
   * zurückgelieferten String-Repräsentationen der PAL-Einträge aber auf jeden Fall
   * immer am Ende den String "§§%=%§§" gefolgt vom Schlüssel des entsprechenden
   * Eintrags!
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
  public String[] getPALEntries()
  {
    DJDatasetListElement[] pal = getSortedPALEntries();
    String[] elements = new String[pal.length];
    for (int i = 0; i < pal.length; i++)
    {
      elements[i] =
        pal[i].toString() + SENDER_KEY_SEPARATOR + pal[i].getDataset().getKey();
    }
    return elements;
  }

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Persönlichen Absenderliste
   * (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * Wichtig: Diese Methode ist nicht im XPALProvider-Interface enthalten. Die
   * String-Repräsentation der zurückgelieferten DJDatasetListElements entsprechen
   * zwar {@link #senderDisplayTemplate}, aber sie enthalten im Gegensatz zu den
   * Strings, die man über {@link #getPALEntries()} erhält, NICHT zwangsläufig am
   * Ende die Schlüssel der Datensätze. Wenn man nicht direkt an die Dataset-Objekte
   * der PAL heran will, sollte man statt dieser Methode auf jeden Fall besser
   * {@link #getPALEntries()} verwenden!
   * 
   * @return alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = getDatasourceJoiner().getLOS();

    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] =
        new DJDatasetListElement((DJDataset) iter.next(), senderDisplayTemplate);
    Arrays.sort(elements);

    return elements;
  }

  /**
   * Diese Methode liefert eine String-Repräsentation des aktuell aus der
   * persönlichen Absenderliste (PAL) ausgewählten Absenders zurück. Die genaue Form
   * der String-Repräsentation ist abhängig von {@link #senderDisplayTemplate}, das
   * in der WollMux-Konfiguration über den Wert von SENDER_DISPLAYTEMPLATE gesetzt
   * werden kann. Unabhängig von {@link #senderDisplayTemplate} enthält die über
   * diese Methode zurückgelieferte String-Repräsentation aber auf jeden Fall immer
   * am Ende den String "§§%=%§§" gefolgt vom Schlüssel des aktuell ausgewählten
   * Absenders. Ist die PAL leer oder noch kein Absender ausgewählt, so liefert die
   * Methode den Leerstring "" zurück. Dieser Sonderfall sollte natürlich
   * entsprechend durch die aufrufende Methode behandelt werden.
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
      return new DJDatasetListElement(selected, senderDisplayTemplate).toString()
        + SENDER_KEY_SEPARATOR + selected.getKey();
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
   * Liefert eine Referenz auf den von diesem WollMux verwendeten
   * {@link DocumentManager}.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public DocumentManager getDocumentManager()
  {
    return docManager;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die Dokumentaktionen enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary getDocumentActionFunctions()
  {
    return documentActionFunctions;
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
   * Liefert die persönliche OverrideFrag-Liste des aktuell gewählten Absenders.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  /* package private */ConfigThingy getInitialOverrideFragMap()
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");
    if (overrideFragDbSpalte == null)
    {
      ConfigThingy overrideFragDbSpalteConf =
        getWollmuxConf().query(OVERRIDE_FRAG_DB_SPALTE, 1);
      try
      {
        overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
      }
      catch (NodeNotFoundException x)
      {
        // keine OVERRIDE_FRAG_DB_SPALTE Direktive gefunden
        overrideFragDbSpalte = "";
      }
    }

    if (overrideFragDbSpalte.length() > 0)
    {
      try
      {
        Dataset ds = getDatasourceJoiner().getSelectedDatasetTransformed();
        String value = ds.get(overrideFragDbSpalte);
        if (value == null) value = "";
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      }
      catch (DatasetNotFoundException e)
      {
        Logger.log(L.m("Kein Absender ausgewählt => %1 bleibt wirkungslos",
          OVERRIDE_FRAG_DB_SPALTE));
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(L.m("%2 spezifiziert Spalte '%1', die nicht vorhanden ist",
          overrideFragDbSpalte, OVERRIDE_FRAG_DB_SPALTE), e);
      }
      catch (IOException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
      catch (SyntaxErrorException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
    }

    return overrideFragConf;
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
   * Diese Methode tut nichts ausser zu prüfen, ob es sich bei dem übergebenen String
   * id um einen gültigen Bezeichner gemäß der Syntax für WollMux-Config-Dateien
   * handelt und im negativen Fall eine InvalidIdentifierException zu werfen.
   * 
   * @param id
   *          zu prüfende ID
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @throws InvalidIdentifierException
   */
  public static void checkIdentifier(String id) throws InvalidIdentifierException
  {
    if (!id.matches("^[a-zA-Z_][a-zA-Z_0-9]*$"))
      throw new InvalidIdentifierException(id);
  }

  public static class InvalidIdentifierException extends Exception
  {
    private static final long serialVersionUID = 495666967644874471L;

    private String invalidId;

    public InvalidIdentifierException(String invalidId)
    {
      this.invalidId = invalidId;
    }

    public String getMessage()
    {
      return L.m(
        "Der Bezeichner '%1' ist ungültig, und darf nur die Zeichen a-z, A-Z, _ und 0-9 enthalten, wobei das erste Zeichen keine Ziffer sein darf.",
        invalidId);
    }
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum übergebenen XTextDocument doc;
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt und das
   * neu erzeugte zurück geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugehörige TextDocumentModel
   *          zurückgeliefert werden soll.
   * @return Das zu doc zugehörige TextDocumentModel.
   */
  public TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    DocumentManager.Info info = docManager.getInfo(doc);
    if (info == null)
    {
      Logger.error(
        L.m("Irgendwer will hier ein TextDocumentModel für ein Objekt was der DocumentManager nicht kennt. Das sollte nicht passieren!"),
        new Exception());

      // Wir versuchen trotzdem sinnvoll weiterzumachen.
      docManager.addTextDocument(doc);
      info = docManager.getInfo(doc);
    }

    return info.getTextDocumentModel();
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene AnnotationField oder null, wenn keines gefunden
   *          wurde.
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
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
    }

    Object textField = UNO.getProperty(element, "TextField");
    if (textField != null
      && UNO.supportsService(textField, "com.sun.star.text.TextField.Annotation"))
    {
      return UNO.XTextField(textField);
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
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück. Der sichtbare Text
   * wird dabei ab einer Länge von 50 Zeichen automatisch umgebrochen.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    showInfoModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück.
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
    showDialog(sTitle, sMessage, margin, javax.swing.JOptionPane.INFORMATION_MESSAGE,
      javax.swing.JOptionPane.DEFAULT_OPTION);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige einer Frage
   * und kehrt erst nach Beendigung des Dialogs wieder zurück. Der sichtbare Text
   * wird dabei ab einer Länge von 50 Zeichen automatisch umgebrochen. 
   * Wenn die Benutzerin oder der Benutzer "OK" geklickt hat, gibt die Methode
   * true zurück, andernfalls false.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @return true wenn die Benutzerin oder der Benutzer "OK" geklickt hat, false sonst.
   */
  public static boolean showQuestionModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    return showQuestionModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück.
   * Wenn die Benutzerin oder der Benutzer "OK" geklickt hat, gibt die Methode
   * true zurück, andernfalls false.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   * @return true wenn die Benutzerin oder der Benutzer "OK" geklickt hat, false sonst.
   */
  public static boolean showQuestionModal(java.lang.String sTitle,
      java.lang.String sMessage, int margin)
  {
    return showDialog(sTitle, sMessage, margin, javax.swing.JOptionPane.QUESTION_MESSAGE,
      javax.swing.JOptionPane.YES_NO_OPTION);
  }

  private static boolean showDialog(java.lang.String sTitle, 
      java.lang.String sMessage, int margin, int messageType, int optionType)
  {    
    boolean ret = false;
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

      JOptionPane pane =
        new JOptionPane(formattedMessage, messageType, optionType);
      JDialog dialog = pane.createDialog(null, sTitle);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
      Integer retValue = (Integer)pane.getValue();
      if (retValue.intValue() == 0){
        ret = true;
      }
      Logger.debug(retValue.toString());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    return ret;
  }

  /**
   * Holt sich den Frame von doc, führt auf diesem ein queryDispatch() mit der zu
   * urlStr gehörenden URL aus und liefert den Ergebnis XDispatch zurück oder null,
   * falls der XDispatch nicht verfügbar ist.
   * 
   * @param doc
   *          Das Dokument, dessen Frame für den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht verfügbar
   *         ist.
   */
  public static XDispatch getDispatchForModel(XModel doc, com.sun.star.util.URL url)
  {
    if (doc == null) return null;

    XDispatchProvider dispProv = null;
    try
    {
      dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());
    }
    catch (Exception e)
    {}

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self",
        com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }
}
