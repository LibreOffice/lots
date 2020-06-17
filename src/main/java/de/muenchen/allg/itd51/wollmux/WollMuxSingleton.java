/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventBroadcaster;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.LogConfig;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.LibreOfficeEventListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.former.Common;
import de.muenchen.allg.util.UnoConfiguration;

/**
 * Diese Klasse ist ein Singleton, welches den WollMux initialisiert und alle zentralen
 * WollMux-Methoden zur Verfügung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der früher zentraler Anlaufpunkt war, bedient sich
 * größtenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxSingleton.class);

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Enthält den default XComponentContext in dem der WollMux (bzw. das OOo) läuft.
   */
  private XComponentContext ctx;

  /**
   * Verwaltet Informationen zum NoConfig mode.
   */
  private boolean noConfig;

  private boolean menusCreated = false;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    this.ctx = ctx;

    // init UNO helper class.
    try
    {
      UNO.init(ctx.getServiceManager());
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    boolean successfulStartup = true;

    noConfig = WollMuxFiles.getWollmuxConf() != null && WollMuxFiles.getWollmuxConf().count() == 0;

    // set font's zoom mode
    Common.zoomFonts(Common.getFontZoomFactor(WollMuxFiles.getWollmuxConf()));

    // init Logging.
    String logLevel = WollMuxFiles.getWollMuxConfLoggingMode(WollMuxFiles.getWollmuxConf());
    LogConfig.init(logLevel);

    // init Localization
    if (!WollMuxFiles.initLocalization(WollMuxFiles.getWollmuxConf()))
    {
      LOGGER
          .info("No localization found in wollmux.conf. WollMux starts with default localization.");
    }

    // init default context
    WollMuxFiles.determineDefaultContext();

    WollMuxClassLoader.initClassLoader();

    LOGGER.debug(L.m("StartupWollMux"));
    LOGGER.debug("Build-Info: " + getBuildInfo());
    if (WollMuxFiles.getWollMuxConfFile() != null)
    {
      LOGGER.debug("wollmuxConfFile = " + WollMuxFiles.getWollMuxConfFile().toString());
    }
    LOGGER.debug("DEFAULT_CONTEXT \"{}\"", WollMuxFiles.getDefaultContext());
    LOGGER.debug("CONF_VERSION: " + getConfVersionInfo());

    /*
     * Datenquellen/Registriere Abschnitte verarbeiten. ACHTUNG! Dies muss vor getDatasourceJoiner()
     * geschehen, da die entsprechenden Datenquellen womöglich schon für WollMux-Datenquellen
     * benötigt werden.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(), WollMuxFiles.getDefaultContext());

    // Versuchen, den DJ zu initialisieren und Flag setzen, falls nicht
    // erfolgreich.
    if (DatasourceJoinerFactory.getDatasourceJoiner() == null)
    {
      successfulStartup = false;
    } else
    {
      // Initialisiere EventProcessor
      WollMuxEventHandler.getInstance().setAcceptEvents(successfulStartup);

      // register global EventListener
      try
      {
        XEventBroadcaster eventBroadcaster = UNO.XEventBroadcaster(ctx.getServiceManager()
            .createInstanceWithContext("com.sun.star.frame.GlobalEventBroadcaster", ctx));
        eventBroadcaster
            .addEventListener(new LibreOfficeEventListener(DocumentManager.getDocumentManager()));
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }

      /*
       * FIXME: Darf nur im Falle des externen WollMux gemacht werden, da ansonsten endlosschleifen
       * mit dem ProtocolHandler möglich sind. Evtl. auch lösbar dadurch, dass URLS, die mit
       * ignorecase("wollmux:") anfangen, niemals an den Slave delegiert werden. Ist aber nicht so
       * schön als Lösung. UNO.XDispatchProviderInterception
       * (UNO.desktop).registerDispatchProviderInterceptor(
       * DispatchHandler.globalWollMuxDispatches);
       */

      // setzen von shortcuts
      ConfigThingy tastenkuerzel = new ConfigThingy("");
      try
      {
        tastenkuerzel = WollMuxFiles.getWollmuxConf().query("Tastenkuerzel").getLastChild();
      } catch (NodeNotFoundException e)
      {
        LOGGER.error("", e);
      }

      try
      {
        Shortcuts.createShortcuts(tastenkuerzel);
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }

      // Setzen der in den Abschnitten OOoEinstellungen eingestellten
      // Konfigurationsoptionen
      this.setOOoConfiguration(WollMuxFiles.getWollmuxConf().query("OOoEinstellungen"));
    }
  }

  private void setOOoConfiguration(ConfigThingy oooEinstellungenConf)
  {
    for (ConfigThingy settings : oooEinstellungenConf)
    {
      setConfigurationValues(settings);
    }
  }

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux noch nicht
   * initialisiert, so liefert die Methode null!
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
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch nicht initialisiert
   * wurde)
   */
  public static synchronized void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);
    }
  }

  /**
   * Liefert die Versionsnummer des WollMux (z.B. "5.9.2") zurück.
   */
  public static String getVersion()
  {
    return getBuildInfo();
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zurück. Der Build-Status wird während dem Build-Prozess mit dem Kommando
   * "svn info" auf das Projektverzeichnis erstellt. Die Buildinfo-Datei buildinfo enthält die
   * Paketnummer und die svn-Revision und ist im WollMux.oxt-Paket sowie in der
   * WollMux.uno.jar-Datei abgelegt.
   *
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende Ersatzmeldung erzeugt (siehe
   * Sourcecode).
   *
   * @return Der Build-Status der aktuellen WollMux-Installation.
   */
  public static String getBuildInfo()
  {
    URL url = WollMuxSingleton.class.getClassLoader().getResource("buildinfo");

    if (url == null)
    {
      return L.m("Version: unbekannt");
    }

    try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream())))
    {
      String str = in.readLine();
      if (str != null)
      {
        return str;
      }
    } catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    return L.m("Version: unbekannt");
  }

  /**
   * Diese Methode liefert die Versionsinformation der aktuell verwendeten wollmux-Konfiguration
   * (z.B. "wollmux-standard-config-2.2.1") als String zurück, wenn in der Konfiguration ein
   * entsprechender CONF_VERSION-Schlüssel definiert ist, oder "unbekannt", falls der dieser
   * Schlüssel nicht existiert.
   *
   * @return Der Versionsinformation der aktuellen WollMux-Konfiguration (falls definiert) oder
   *         "unbekannt", falls nicht.
   */
  public String getConfVersionInfo()
  {
    ConfigThingy versions = WollMuxFiles.getWollmuxConf().query("CONF_VERSION");
    try
    {
      return versions.getLastChild().toString();
    } catch (NodeNotFoundException e)
    {
      if (noConfig)
      {
        return L.m("keine geladen");
      } else
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
   * Verarbeitet alle Datenquellen/Registriere-Unterabschnitte von conf und registriert die
   * entsprechenden Datenquellen in OOo, falls dort noch nicht vorhanden.
   *
   * @param context
   *          gibt an relativ zu was relative URLs aufgelöst werden sollen.
   */
  private static void registerDatasources(ConfigThingy conf, URL context)
  {
    Iterator<ConfigThingy> iter = conf.query("Datenquellen").query("Registriere").iterator();
    while (iter.hasNext())
    {
      ConfigThingy regConf = iter.next();
      String name;
      try
      {
        name = regConf.get("NAME").toString();
      } catch (NodeNotFoundException e)
      {
        LOGGER.error(L.m("NAME-Attribut fehlt in Datenquellen/Registriere-Abschnitt"), e);
        continue;
      }

      String urlStr;
      try
      {
        urlStr = regConf.get("URL").toString();
      } catch (NodeNotFoundException e)
      {
        LOGGER.error(
            L.m("URL-Attribut fehlt in Datenquellen/Registriere-Abschnitt für Datenquelle '%1'",
                name),
            e);
        continue;
      }

      try
      {
        if (UnoDictionary.create(UNO.dbContext, Object.class).containsKey(name))
        {
          try
          {
            if (!regConf.get("REFRESH").toString().equalsIgnoreCase("true"))
            {
              continue;
            }

            // hierher (und damit weiter ohne continue) kommen wir nur, wenn
            // ein REFRESH-Abschnitt vorhanden ist und "true" enthält.
          } catch (NodeNotFoundException x)
          {
            continue;
          }
        }
      } catch (Exception x)
      {
        LOGGER.error(
            L.m("Fehler beim Überprüfen, ob Datenquelle '%1' bereits registriert ist", name), x);
      }

      LOGGER.debug(
          L.m("Versuche, Datenquelle '%1' bei OOo zu registrieren für URL '%2'", name, urlStr));

      String parsedUrl;
      try
      {
        URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      } catch (Exception x)
      {
        LOGGER.error(
            L.m("Fehler beim Registrieren von Datenquelle '%1': Illegale URL: '%2'", name, urlStr),
            x);
        continue;
      }

      try
      {
        Object datasource = UnoDictionary.create(UNO.dbContext, Object.class).get(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(UNO.dbContext.getRegisteredObject(name), datasource))
          LOGGER.error(
              L.m("Testzugriff auf Datenquelle '%1' nach Registrierung fehlgeschlagen", name));
      } catch (Exception x)
      {
        LOGGER.error(L.m(
            "Fehler beim Registrieren von Datenquelle '%1'. Stellen Sie sicher, dass die URL '%2' gültig ist.",
            name, parsedUrl), x);
        continue;
      }

    }
  }

  /**
   * Setzt die im ConfigThingy übergebenen OOoEinstellungen-Abschnitt enthaltenen Einstellungen in
   * der OOo-Registry.
   *
   * @param oooEinstellungenConf
   *          Der Knoten OOoEinstellungen eines solchen Abschnitts.
   */
  private static void setConfigurationValues(ConfigThingy oooEinstellungenConf)
  {
    for (ConfigThingy element : oooEinstellungenConf)
    {
      try
      {
        String node = element.get("NODE").toString();
        String prop = element.get("PROP").toString();
        String type = element.get("TYPE").toString();
        String value = element.get("VALUE").toString();
        Object v = getObjectByType(type, value);

        setConfigurationValue(node, prop, v);
      } catch (Exception e)
      {
        LOGGER.error(L.m("OOoEinstellungen: Konnte Einstellung '%1'nicht setzen:",
            element.stringRepresentation()), e);
      }
    }
  }

  /**
   * Konvertiert den als String übergebenen Wert value in ein Objekt vom Typ type oder liefert eine
   * IllegalArgumentException, wenn die Werte nicht konvertiert werden können.
   *
   * @param type
   *          Der Typ in den konvertiert werden soll ('boolean', 'integer', 'float', 'string').
   * @param value
   *          Der zu konvertierende Wert.
   * @return Das neue Objekt vom entsprechenden Typ.
   * @throws IllegalArgumentException
   *           type oder value sind ungültig oder fehlerhaft.
   */
  private static Object getObjectByType(String type, String value)
  {
    try
    {
      if (type.equalsIgnoreCase("boolean"))
      {
        return Boolean.valueOf(value);
      } else if (type.equalsIgnoreCase("integer"))
      {
        return Integer.valueOf(value);
      } else if (type.equalsIgnoreCase("float"))
      {
        return Float.valueOf(value);
      } else if (type.equalsIgnoreCase("string"))
      {
        return value;
      }
    } catch (NumberFormatException e)
    {
      LOGGER.error("", e);
    }

    throw new IllegalArgumentException(L.m(
        "Der TYPE '%1' ist nicht gültig. Gültig sind 'boolean', 'integer', 'float' und 'string'.",
        type));
  }

  /**
   * Setzt eine Einstellung value in der OOo-Registry, wobei die Position im Registry-Baum durch
   * node und prop beschrieben wird.
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
    try
    {
      UnoConfiguration.setConfiguration(node, new UnoProps(prop, value));
    } catch (UnoHelperException e1)
    {
      LOGGER.error("Can't modify the configuration.", e1);
    }
  }

  /**
   * Überprüft, ob von url gelesen werden kann und wirft eine IOException, falls nicht.
   *
   * @throws IOException
   *           falls von url nicht gelesen werden kann.
   */
  public static void checkURL(URL url) throws IOException
  {
    url.openStream().close();
  }

  public boolean isNoConfig()
  {
    return noConfig;
  }
}
