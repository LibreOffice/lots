/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton für zentrale WollMux-Methoden.
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
import java.util.Iterator;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.GlobalEventListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse ist ein Singleton, welches den WollMux initialisiert und alle
 * zentralen WollMux-Methoden zur Verfügung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der früher zentraler Anlaufpunkt war,
 * bedient sich größtenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton
{

   private static WollMuxSingleton singletonInstance = null;

  /**
   * Enthält den default XComponentContext in dem der WollMux (bzw. das OOo) läuft.
   */
  private XComponentContext ctx;

  /**
   * Verwaltet Informationen zum NoConfig mode.
   */
  private NoConfig noConfig;
  
  private boolean menusCreated = false;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zurückgeliefert.
    this.ctx = ctx;

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

    if (!WollMuxFiles.setupWollMuxDir())
    {
      noConfig = new NoConfig(true);
      showNoConfigInfo();
    } 
    else
    {
      noConfig = new NoConfig(false);
    }
    
    WollMuxClassLoader.initClassLoader();
    
    Logger.debug(L.m("StartupWollMux"));
    Logger.debug("Build-Info: " + getBuildInfo());
    if (WollMuxFiles.getWollMuxConfFile() != null)
    {
      Logger.debug("wollmuxConfFile = " + WollMuxFiles.getWollMuxConfFile().toString());
    }
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
    if (DatasourceJoiner.getDatasourceJoiner() == null) successfulStartup = false;

    // Initialisiere EventProcessor
    WollMuxEventHandler.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster =
        UnoService.createWithContext("com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster().addEventListener(
        new GlobalEventListener(DocumentManager.getDocumentManager()));
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

  public boolean isMenusCreated()
  {
    return menusCreated;
  }

  public void setMenusCreated(boolean menusCreated)
  {
    this.menusCreated = menusCreated;
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
  public static String getVersion()
  {
    return getBuildInfo();
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
  public static String getBuildInfo()
  {
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
    ConfigThingy versions = WollMuxFiles.getWollmuxConf().query("CONF_VERSION");
    try
    {
      return versions.getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      if (noConfig != null && noConfig.isNoConfig())
      {
        return L.m("keine geladen");
      } 
      else
      {
        return L.m("unbekannt");
      }
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
   * Git zurück, ob sich der WollMux im NoConfig-Modus befindet,
   * d.h. es wurde keine Config-Datei gefunden.
   * 
   * @return
   */
  public boolean isNoConfig()
  {
    return (null != noConfig) ? noConfig.isNoConfig() : false;
  }
  
  /**
   * Schreibt Meldung in die Log-Datei, wenn der NoConfig-Modus aktiv ist.
   * 
   * @return
   */
  public boolean showNoConfigInfo()
  {
    return noConfig.showNoConfigInfo();
  }
}
