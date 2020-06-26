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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.star.beans.Property;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.uno.AnyConverter;
import com.sun.star.util.XStringSubstitution;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.LogConfig;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Collection of files used by WollMux.
 */
public class WollMuxFiles
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxFiles.class);

  private static final String ETC_WOLLMUX_WOLLMUX_CONF = "/etc/wollmux/wollmux.conf";

  private static final String WOLLMUX_CONF_PATH = "WOLLMUX_CONF_PATH";

  private static boolean debugMode = false;

  /**
   * Windows registry key.
   */
  private static final String WOLLMUX_KEY = "Software\\WollMux";

  /**
   * Windows registry option.
   */
  private static final String WOLLMUX_CONF_PATH_VALUE_NAME = "ConfigPath";

  private static final String WOLLMUX_NOCONF = L.m(
      "Es wurde keine WollMux-Konfiguration (wollmux.conf) gefunden - deshalb läuft WollMux im NoConfig-Modus.");

  /**
   * Default context defined in wollmux.conf.
   */
  private static URL defaultContextURL;

  /**
   * The configuration.
   */
  private static ConfigThingy wollmuxConf;

  /**
   * The .wollmux folder in user space.
   */
  private static File wollmuxDir;

  /**
   * The configuration file.
   */
  private static File wollmuxConfFile;

  /**
   * File of the local override storage.
   */
  private static File losCacheFile;

  private WollMuxFiles()
  {
    // nothing to initialize.
  }

  public static boolean isDebugMode()
  {
    return debugMode;
  }

  /**
   * Creates the '.wollmux' directory in user's home if it doesn't exists.
   *
   * @return File newly created wollmux-Folder file reference.
   */
  private static File setupWollMuxDir()
  {
    String userHome = System.getProperty("user.home");
    wollmuxDir = new File(userHome, ".wollmux");

    if (!wollmuxDir.exists())
    {
      wollmuxDir.mkdirs();
    }

    return wollmuxDir;
  }

  /**
   * Load a configuration.
   *
   * @param wollMuxConfigFile
   *          The configuration file.
   * @return The configuraiton.
   */
  protected static ConfigThingy parseWollMuxConf(File wollMuxConfigFile)
  {
    wollmuxConf = new ConfigThingy("");

    if (wollMuxConfigFile != null && wollMuxConfigFile.exists() && wollMuxConfigFile.isFile())
    {
      try
      {
        wollmuxConf = new ConfigThingy("", wollMuxConfigFile.toURI().toURL());
	String serverURI = wollmuxConf.getString("CONF_SERVER", null);
        if (serverURI != null)
        {
          String user = wollmuxConf.getString("USERNAME", System.getProperty("user.name"));
          HttpClient client = HttpClient.newHttpClient();
          HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverURI))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(("{ \"username\":\"" + user + "\"}")))
              .timeout(Duration.ofSeconds(5)).build();

          HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

          wollmuxConf = new ConfigThingy("", response.body());
        }
      } catch (HttpTimeoutException ex) {
        LOGGER.error("Serverrespond takes more than 5 seconds", ex);
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    } else
    {
      LOGGER.info(WOLLMUX_NOCONF);
    }

    return wollmuxConf;
  }

  /**
   * Initialize localization.
   *
   * @param config
   *          The localization configuration.
   * @return True if successfully initialized, false otherwise.
   */
  public static boolean initLocalization(ConfigThingy config)
  {
    if (config == null)
    {
      return false;
    }

    ConfigThingy l10n = config.query("L10n", 1);
    if (l10n.count() > 0)
    {
      L.init(l10n);
    }

    return true;
  }

  private static File findWollMuxConf(File wollMuxDir)
  {
    String relativeWollMuxConf = "/.wollmux/wollmux.conf";
    ArrayList<String> searchPaths = new ArrayList<>();

    String wollmuxConfPath = null;

    // wollmux.conf set by environment "WOLLMUX_CONF_PATH".
    if (System.getenv(WOLLMUX_CONF_PATH) != null)
    {
      wollmuxConfPath = System.getenv(WOLLMUX_CONF_PATH);
      searchPaths.add(wollmuxConfPath);
    }

    // wollmux.conf set by system property "WOLLMUX_CONF_PATH"
    if (System.getProperty(WOLLMUX_CONF_PATH) != null)
    {
      wollmuxConfPath = System.getProperty(WOLLMUX_CONF_PATH);
      searchPaths.add(wollmuxConfPath);
    }

    searchPaths.add(new File(wollMuxDir, "wollmux.conf").getAbsolutePath());
    searchPaths.add(System.getProperty("user.dir") + relativeWollMuxConf);
    searchPaths.add(ETC_WOLLMUX_WOLLMUX_CONF);

    // Check if windows
    if (System.getProperty("os.name").toLowerCase().contains("windows"))
    {
      // try reading path to wollmux.conf from HKCU registry
      if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, WOLLMUX_KEY))
      {
        wollmuxConfPath = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, WOLLMUX_KEY,
            WOLLMUX_CONF_PATH_VALUE_NAME);
        searchPaths.add(wollmuxConfPath);
      } else
      {
        LOGGER.debug("Kein Registry-Eintrag unter HKEY_CURRENT_USER");
      }

      // try reading path to wollmux.conf from HKLM registry
      if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, WOLLMUX_KEY))
      {
        wollmuxConfPath = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
            WOLLMUX_KEY, WOLLMUX_CONF_PATH_VALUE_NAME);
        searchPaths.add(wollmuxConfPath);
      } else
      {
        LOGGER.debug("Kein Registry-Eintrag unter HKEY_LOCAL_MACHINE");
      }

      Shell32 shell = Shell32.INSTANCE;

      char[] arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + relativeWollMuxConf);

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_COMMON_APPDATA, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + relativeWollMuxConf);

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILESX86, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + relativeWollMuxConf);

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILES, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + relativeWollMuxConf);

    }

    for (String path : searchPaths)
    {
      File file = new File(path);

      if (file.exists())
      {
        return file;
      }
    }

    return null;
  }

  /**
   * Get main folder of WollMux.
   *
   * @return Folder '.wollmux'.
   */
  public static File getWollMuxDir()
  {
    if (wollmuxDir == null)
    {
      wollmuxDir = setupWollMuxDir();
    }

    return wollmuxDir;
  }

  /**
   * Liefert das File-Objekt der wollmux,conf zurück, die gelesen wurde (kann z,B, auch die aus
   * /etc/wollmux/ sein). Darf erst nach setupWollMuxDir() aufgerufen werden.
   */
  public static File getWollMuxConfFile()
  {
    if (wollmuxConfFile == null || !wollmuxConfFile.exists())
      wollmuxConfFile = findWollMuxConf(getWollMuxDir());

    return wollmuxConfFile;
  }

  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück. Darf erst nach
   * setupWollMuxDir() aufgerufen werden.
   *
   * @return das File-Objekt des LocalOverrideStorage Caches.
   */
  public static File getLosCacheFile()
  {
    if (losCacheFile == null || !losCacheFile.exists())
      losCacheFile = new File(getWollMuxDir(), "cache.conf");

    return losCacheFile;
  }

  /**
   * Liefert den Inhalt der wollmux,conf zurück.
   */
  public static ConfigThingy getWollmuxConf()
  {
    if (wollmuxConf == null || wollmuxConf.count() == 0)
      wollmuxConf = parseWollMuxConf(getWollMuxConfFile());

    return wollmuxConf;
  }

  /**
   * Get default context of WollMux.
   *
   * @return The URL of the default context.
   */
  public static URL getDefaultContext()
  {
    return defaultContextURL;
  }

  /**
   * Convert a URL String to a {@link URL}. Relative paths are resolved relative to
   * {@link #getDefaultContext()}.
   *
   * @param urlStr
   *          The string to convert.
   * @return The converted URL.
   * @throws MalformedURLException
   *           The string isn't a valid URL.
   */
  public static URL makeURL(String urlStr) throws MalformedURLException
  {
    return new URL(WollMuxFiles.getDefaultContext(), ConfigThingy.urlEncode(urlStr));
  }

  /**
   * Initialize default context from configuration. If no default context is provided in the
   * configuration the folder of the configuration file is used.
   *
   * If {@link #defaultContextURL} is already set nothing is done.
   */
  public static void determineDefaultContext()
  {
    if (defaultContextURL == null)
    {
      ConfigThingy dc = getWollmuxConf().query("DEFAULT_CONTEXT");
      String urlStr;
      try
      {
        urlStr = dc.getLastChild().toString();
      } catch (NodeNotFoundException e)
      {
        urlStr = "./";
      }

      String urlVerzStr;
      if (urlStr.endsWith("/") || urlStr.endsWith("\\"))
      {
        urlVerzStr = urlStr;
      }
      else
      {
        urlVerzStr = urlStr + "/";
      }

      try
      {
        // Save initialization in case of MalformedURLExceptions
        defaultContextURL = new URL("file:///");

        File file = getWollMuxConfFile();
        if (file != null)
        {
          defaultContextURL = file.toURI().toURL();
        }
        defaultContextURL = new URL(defaultContextURL, urlVerzStr);
      } catch (MalformedURLException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Get the log level from the configuration.
   *
   * @param config
   *          The configuration.
   * @return The log level as string. Defaults to "info".
   */
  public static String getWollMuxConfLoggingMode(ConfigThingy config)
  {
    if (config == null || config.count() == 0)
    {
      return "info";
    }

    ConfigThingy log = config.query("LOGGING_MODE");
    if (log.count() > 0)
    {
      try
      {
        String mode = log.getLastChild().toString();

        if (mode.compareToIgnoreCase("debug") == 0 || mode.compareToIgnoreCase("all") == 0)
        {
          debugMode = true;
        }

        return mode;
      } catch (NodeNotFoundException x)
      {
        LOGGER.error("", x);
      }
    }

    return "info";
  }

  /**
   * Dump information about WollMux in the directory {@link #getWollMuxDir()}.
   *
   * @return The name of the generated file.
   */
  public static String dumpInfo()
  {
    Calendar cal = Calendar.getInstance();
    String date = "" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
        + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.getTimeInMillis();
    File dumpFile = new File(getWollMuxDir(), "dump" + date);
    try (OutputStream outStream = new FileOutputStream(dumpFile);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        PrintWriter printWriter = new PrintWriter(out);)
    {
      out.write("Dump time: " + date + "\n");
      out.write(WollMuxSingleton.getBuildInfo() + "\n");

      deterimeIP(out);

      out.write("OOo-Version: \"" + getConfigValue("/org.openoffice.Setup/Product", "ooSetupVersion") + "\n");

      out.write("DEFAULT_CONTEXT: \"" + getDefaultContext() + "\"\n");
      out.write("CONF_VERSION: " + WollMuxSingleton.getInstance().getConfVersionInfo() + "\n");
      out.write("wollmuxDir: " + getWollMuxDir() + "\n");
      if (getWollMuxConfFile() != null)
      {
        out.write("wollmuxConfFile: " + getWollMuxConfFile() + "\n");
      }
      out.write("losCacheFile: " + getLosCacheFile() + "\n");

      out.write("===================== START LOG4J-Settings ==================\n");
      LogConfig.dumpConfiguration(printWriter);
      out.write("===================== END LOG4J-Settings ==================\n");

      out.write("===================== START JVM-Settings ==================\n");
      dumpJVMSettings(outStream, out);
      out.write("===================== END JVM-Settings ==================\n");

      out.write("===================== START java-properties ==================\n");
      Properties props = System.getProperties();
      Enumeration<?> enu = props.propertyNames();
      while (enu.hasMoreElements())
      {
        String key = enu.nextElement().toString();
        out.write(key + ": " + props.getProperty(key) + "\n");
      }
      out.write("===================== END java-properties ==================\n");

      out.write("===================== START java-memoryinfo ==================\n");
      long maxMemory = Runtime.getRuntime().maxMemory();
      long totalMemory = Runtime.getRuntime().totalMemory();
      long freeMemory = Runtime.getRuntime().freeMemory();
      out.write("No. of Processors: " + ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() + "\n");
      out.write("Maximum Heap Size: " + FileUtils.byteCountToDisplaySize(maxMemory) + "\n");
      out.write("Currently Allocated Heap Size: " + FileUtils.byteCountToDisplaySize(totalMemory) + "\n");
      out.write("Currently Used Memory: " + FileUtils.byteCountToDisplaySize(totalMemory - freeMemory) + "\n");
      out.write("Maximum Physical Memory: " + getSystemMemoryAttribut("TotalPhysicalMemorySize") + " KB\n");
      out.write("Free Physical Memory: " + getSystemMemoryAttribut("FreePhysicalMemorySize") + "\n");
      out.write("Maximum Swap Size: " + getSystemMemoryAttribut("TotalSwapSpaceSize") + "\n");
      out.write("Free Swap Size: " + getSystemMemoryAttribut("FreeSwapSpaceSize") + "\n");

      out.write("===================== END java-memoryinfo ==================\n");

      out.write("===================== START wollmuxConfFile ==================\n");
      out.flush();
      if (getWollMuxConfFile() != null)
      {
        copyFile(getWollMuxConfFile(), outStream);
      }
      outStream.flush();
      out.write("\n");
      out.write("===================== END wollmuxConfFile ==================\n");

      out.write("===================== START wollmux.conf ==================\n");
      out.write(getWollmuxConf().stringRepresentation());
      out.write("===================== END wollmux.conf ==================\n");

      out.write("===================== START losCacheFile ==================\n");
      out.flush();
      copyFile(getLosCacheFile(), outStream);
      outStream.flush();
      out.write("\n");
      out.write("===================== END losCacheFile ==================\n");

      out.write("===================== START OOo-Configuration dump ==================\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Setup/Product") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Setup/L10N") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Office.Paths/") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Office.Writer/") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Inet/") + "\n");
      out.write("===================== END OOo-Configuration dump ==================\n");

      out.write("===================== START OOo datasources ==================\n");
      dumpOfficeDatasources(out);
      out.write("===================== END OOo datasources ==================\n");
    } catch (IOException | NumberFormatException | JMException x)
    {
      LOGGER.error(L.m("Fehler beim Erstellen des Dumps"), x);
      return null;
    }
    return dumpFile.getAbsolutePath();
  }

  private static void dumpOfficeDatasources(BufferedWriter out) throws IOException
  {
    UnoDictionary<Object> dataSources = UnoDictionary.create(UNO.dbContext, Object.class);
    if (dataSources != null)
    {
      for (String dataSource : dataSources.keySet())
      {
        out.write(dataSource);
        out.write("\n");
        try
        {
          XDataSource ds = UNO.XDataSource(UNO.dbContext.getRegisteredObject(dataSource));
          ds.setLoginTimeout(1);
          XConnection conn = ds.getConnection("", "");
          UnoDictionary<Object> tables = UnoDictionary.create(UNO.XTablesSupplier(conn).getTables(), Object.class);
          for (String name : tables.keySet())
          {
            out.write("  " + name + "\n");
          }
        } catch (Exception x)
        {
          out.write("  " + x.toString() + "\n");
        }
      }
    }
  }

  private static void dumpJVMSettings(OutputStream outStream, BufferedWriter out) throws IOException
  {
    try
    {
      XStringSubstitution subst = UNO
          .XStringSubstitution(UnoComponent.createComponentWithContext(UnoComponent.CSS_UTIL_PATH_SUBSTITUTION));
      String jConfPath = new URL(subst.substituteVariables("$(user)/config", true)).toURI()
          .getPath();
      File[] jConfFiles = new File(jConfPath).listFiles();
      Pattern p = Pattern.compile("^javasettings_.*\\.xml$", Pattern.CASE_INSENSITIVE);
      boolean found = false;
      for (int i = 0; i < jConfFiles.length; i++)
      {
        if (p.matcher(jConfFiles[i].getName()).matches())
        {
          out.flush();
          copyFile(jConfFiles[i], outStream);
          outStream.flush();
          out.write("\n");
          found = true;
          break;
        }
      }
      if (!found)
        out.write(
            L.m("Datei '%1' konnte nicht gefunden werden.\n", jConfPath + "/javasettings_*.xml"));
    } catch (java.lang.Exception e)
    {
      out.write(L.m("Kann JVM-Settings nicht bestimmen: %1\n", "" + e));
    }
  }

  private static void deterimeIP(BufferedWriter out) throws IOException
  {
    try
    {
      InetAddress addr = InetAddress.getLocalHost();
      out.write("Host: " + addr.getHostName() + " (" + addr.getHostAddress() + ")\n");
    } catch (UnknownHostException ex)
    {
      LOGGER.error("", ex);
    }
  }

  /**
   * Get the size attribute of the system memory parts.
   * 
   * @param key
   *          Values could be TotalPhysicalMemorySize or FreeSwapSpaceSize.
   * @return The size of the memory part.
   * @throws JMException
   *           If no such attribute is present.
   */
  private static long getSystemMemoryAttribut(String key) throws JMException
  {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    String value = mBeanServer
        .getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), key).toString();
    return Long.parseLong(value) / 1024;
  }

  private static String getConfigValue(String path, String name)
  {
    try
    {
      return UnoConfiguration.getConfiguration(path, name).toString();
    } catch (Exception ex)
    {
      LOGGER.info("", ex);
      return "";
    }
  }

  /**
   * Copy content of a file to a output stream.
   *
   * @param file
   *          The file.
   * @param out
   *          The output stream.
   */
  private static void copyFile(File file, OutputStream out)
  {
    try (InputStream in = new FileInputStream(file))
    {
      byte[] buffy = new byte[2048];
      int len;
      while ((len = in.read(buffy)) >= 0)
      {
        out.write(buffy, 0, len);
      }
    } catch (IOException ex)
    {
      ex.printStackTrace(new PrintWriter(out));
    }
  }

  /**
   * Get the Office configuration with all its children.
   *
   * @param nodePath
   *          The request configuration.
   * @return The configuration as string.
   */
  public static String dumpOOoConfiguration(String nodePath)
  {
    try
    {
      Object cfgProvider = UnoComponent
          .createComponentWithContext(UnoComponent.CSS_CONFIGURATION_CONFIGURATION_PROVIDER);
      Object cfgAccess = UnoService.createServiceWithArguments(UnoService.CSS_CONFIGURATION_CONFIGURATION_ACCESS,
          new UnoProps(UnoProperty.NODEPATH, nodePath).getProps(), cfgProvider);
      return dumpNode(cfgAccess, "");
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
      return L.m("Fehler beim Auslesen der OOo-Konfiguration mit dem Nodepath '%1'", nodePath);
    }
  }

  /**
   * Return content of one Office configuration node.
   *
   * @param element
   *          The configuration node.
   * @param spaces
   *          Indent for children.
   * @return String representation of the node.
   * @throws Exception
   *           Can't access the configuration node.
   */
  public static String dumpNode(Object element, String spaces)
  {
    StringBuilder properties = new StringBuilder("");
    if (UNO.XPropertySet(element) != null)
    {
      Predicate<Pair<String, Object>> predicate = (Pair<String, Object> p) -> UNO.XInterface(p.getValue()) == null
          && !AnyConverter.isVoid(p.getValue());
      Consumer<Pair<String, Object>> elementPrinter = (Pair<String, Object> p) -> {
        StringBuilder propStr = new StringBuilder("'" + p.getValue() + "'");
        if (p.getValue() instanceof Object[])
        {
          Object[] arr = (Object[]) p.getValue();
          propStr.append("[");
          propStr.append(Arrays.stream(arr).map(Object::toString).collect(Collectors.joining(", ")));
          propStr.append("]");
        }
        properties.append(spaces + "|    " + p.getKey() + ": " + propStr + "\n");
      };
      Property[] props = UNO.XPropertySet(element).getPropertySetInfo().getProperties();
      Arrays.stream(props).map((Property prop) -> Pair.of(prop.Name, Utils.getProperty(element, prop.Name)))
          .filter(predicate).sorted().forEach(elementPrinter);
    }

    StringBuilder children = new StringBuilder("");
    UnoDictionary<Object> elements = UnoDictionary.create(element, Object.class);
    if (elements != null)
    {
      elements.values().forEach((Object ele) -> children.append(dumpNode(ele, spaces + "|    ")));
    }

    if (UNO.XNamed(element) != null && (properties.length() > 0 || children.length() > 0))
    {
      return spaces + "+ " + UNO.XNamed(element).getName() + "\n" + properties + children;
    }

    return "";
  }

  /**
   * Write children of the configuration to the file.
   *
   * @param file
   *          The file to write into.
   * @param conf
   *          The configuration to write.
   * @throws IOException
   *           Can't write the file.
   */
  public static void writeConfToFile(File file, ConfigThingy conf) throws IOException
  {
    try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
    {
      out.write("\uFEFF");
      out.write(conf.stringRepresentation(true, '"'));
    }
  }

}
