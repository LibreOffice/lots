/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventBroadcaster;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.common.UnoProps;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.LibreOfficeEventListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.former.Common;
import de.muenchen.allg.itd51.wollmux.sender.SenderService;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.LogConfig;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.util.UnoConfiguration;

/**
 * This class is a singleton, which initializes the WollMux and all central
 * Provides WollMux methods. Even the WollMux service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, which used to be the central contact point, is used
 * mostly from the central methods of the singleton.
 */
public class WollMuxSingleton
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxSingleton.class);

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Contains the default XComponentContext in which WollMux (or OOo) runs.
   */
  private XComponentContext ctx;

  /**
   * Manages information about the NoConfig mode.
   */
  private boolean noConfig;

  private boolean menusCreated = false;

  /**
   * The WollMux main class is implemented as a singleton.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    this.ctx = ctx;

    L.initTranslations();

    // init UNO helper class.
    try
    {
      UNO.init(ctx.getServiceManager());
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

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

    ConfClassLoader.initClassLoader(WollMuxFiles.getWollmuxConf().query("CLASSPATH", 1));

    LOGGER.debug("StartupWollMux");
    if (WollMuxFiles.getWollMuxConfFile() != null)
    {
      LOGGER.debug("wollmuxConfFile = {}", WollMuxFiles.getWollMuxConfFile());
    }
    LOGGER.debug("DEFAULT_CONTEXT \"{}\"", WollMuxFiles.getDefaultContext());
    LOGGER.debug("CONF_VERSION: {}", getConfVersionInfo());

    /*
     * Process data sources/register sections. DANGER! This must be done before getDatasourceJoiner()
     * done because the corresponding data sources may already be for WollMux data sources
     * are required.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(), WollMuxFiles.getDefaultContext());

    // Try to initialize the DJ
    if (SenderService.getInstance() != null)
    {
      // Initialize EventProcessor
      WollMuxEventHandler.getInstance().setAcceptEvents(true);

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

      // set shortcuts
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

      // Set the settings set in the OOoSettings sections
      // configuration options
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
   * This method returns the instance of the WollMux singleton. Is not the WollMux yet
   * initialized, the method returns null!
   *
   * @return instance of WollMuxSingleton or null.
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
   * This method initializes the WollMuxSingleton (only if it's not already initialized
   * became)
   */
  public static synchronized void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);
    }
  }

  /**
   * Returns the version number of WollMux (e.g. "5.9.2").
   */
  public static String getVersion()
  {
    return Utils.getWollMuxProperties().getProperty("wollmux.version");
  }

  /**
   * This method returns the version information of the Wollmux configuration currently in use
   * (e.g. "wollmux-standard-config-2.2.1") as a string if in the configuration a
   * corresponding CONF_VERSION key is defined, or "unknown" if that
   * Key does not exist.
   *
   * @return The version information of the current WollMux configuration (if defined) or
   * "unknown" if not.
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
        return L.m("none loaded");
      } else
      {
        return L.m("unknown");
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
   * Processes all data sources/register subsections of conf and registers the
   * Corresponding data sources in OOo, if not already available there.
   *
   * @param context
   *          indicates relative to what relative URLs should be resolved.
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
        LOGGER.error("NAME attribute is missing in 'Datenquellen'/'Registriere' section", e);
        continue;
      }

      String urlStr;
      try
      {
        urlStr = regConf.get("URL").toString();
      } catch (NodeNotFoundException e)
      {
        LOGGER.error("URL attribute is missing in 'DatenquellenRegistriere' section for data source '{0}'", name, e);
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

            // we only come here (and thus continue without continue) if
            // there is a REFRESH section and it contains "true".
          } catch (NodeNotFoundException x)
          {
            continue;
          }
        }
      } catch (Exception x)
      {
        LOGGER.error("Error during checking whether the data source '{}' is already registered", name, x);
      }

      LOGGER.debug("Trying to register data source '{}' for URL '{}'", name, urlStr);

      String parsedUrl;
      try
      {
        URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      } catch (Exception x)
      {
        LOGGER.error("Error during registration of data source '{0}': Illegal URL: '{1}'", name, urlStr, x);
        continue;
      }

      try
      {
        Object datasource = UnoDictionary.create(UNO.dbContext, Object.class).get(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(UNO.dbContext.getRegisteredObject(name), datasource))
          LOGGER.error("Test access to data source '{0}' failed after registration", name);
      } catch (Exception x)
      {
        LOGGER.error("Error during registration of data source '{}'. Make sure that the URL '{}' is valid.", name, parsedUrl, x);
        continue;
      }

    }
  }

  /**
   * Sets the settings contained in the ConfigThingy passed OOoEinstellungen section to
   * the OOo-Registry.
   *
   * @param oooEinstellungenConf
   *          The OOoEinstellungen node of such a section.
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
        LOGGER.error("OOoConfiguration: Configuration '{}' could not be set:", element.stringRepresentation(), e);
      }
    }
  }

  /**
   * Converts the value passed as a string into an object of type type or returns a
   * IllegalArgumentException if the values ​​cannot be converted.
   *
   * @param type
   *          The type to convert to ('boolean', 'integer', 'float', 'string').
   * @param value
   *          The value to convert.
   * @return The new object of the appropriate type.
   * @throws IllegalArgumentException
   *           type or value is invalid or contains errors.
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
        "The TYPE \"{0}\" is invalid. Valid are 'boolean', 'integer', 'float' and 'string'.",
        type));
  }

  /**
   * Sets a setting value in the OOo registry, taking its position in the registry tree through
   * node and prop is described.
   *
   * @param node
   *          z.B. "/org.openoffice.Inet/Settings"
   * @param prop
   *          e.g. "ooInetProxyType"
   * @param value
   *          the value to be set as an object of the appropriate type.
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
   * Checks if url is readable and throws an IOException if not.
   *
   * @throws IOException
   *           if url is not readable.
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
