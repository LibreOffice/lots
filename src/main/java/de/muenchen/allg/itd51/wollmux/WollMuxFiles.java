/*
 * Dateiname: WollMuxFiles.java
 * Projekt  : WollMux
 * Funktion : Managed die Dateien auf die der WollMux zugreift (z.B. wollmux.conf)
 *
 * Copyright (c) 2009-2019 Landeshauptstadt München
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
 * 13.04.2006 | BNK | Erstellung
 * 20.04.2006 | BNK | [R1200] .wollmux-Verzeichnis als Vorbelegung für DEFAULT_CONTEXT
 * 26.05.2006 | BNK | +DJ Initialisierung
 * 20.06.2006 | BNK | keine wollmux.conf mehr anlegen wenn nicht vorhanden
 *                  | /etc/wollmux/wollmux.conf auswerten
 * 26.06.2006 | BNK | Dialoge/FONT_ZOOM auswerten. LookAndFeel setzen.
 * 07.09.2006 | BNK | isDebugMode effizienter gemacht.
 * 21.09.2006 | BNK | Unter Windows nach c:\programme\wollmux\wollmux.conf schauen
 * 19.10.2006 | BNK | +dumpInfo()
 * 05.12.2006 | BNK | +getClassPath()
 * 20.12.2006 | BNK | CLASSPATH:Falls keine Dateierweiterung angegeben, / ans Ende setzen, weil nur so Verzeichnisse erkannt werden.
 * 09.07.2007 | BNK | [R7134]Popup, wenn Server langsam
 * 09.07.2007 | BNK | [R7137]IP-Adresse in Dumpinfo
 * 17.07.2007 | BNK | [R7605]Dateien binär kopieren in dumpInfo(), außerdem immer als UTF-8 schreiben
 * 18.07.2007 | BNK | Alle Java-Properties in dumpInfo() ausgeben
 * 27.07.2007 | BNK | [P1448]WollMuxClassLoader.class.getClassLoader() als parent verwenden
 * 01.09.2008 | BNK | [R28149]Klassen im CLASSPATH aus wollmux.conf haben vorrang vor WollMux-internen.
 * 18.08.2009 | BED | -defaultWollmuxConf
 *                  | andere Strategie für Suche nach wollmux.conf in setupWollMuxDir()
 *                  | Verzeichnis der wollmux.conf als Default für DEFAULT_CONTEXT
 * 12.01.2010 | BED | dumpInfo() gibt nun auch JVM Heap Size + verwendeten Speicher aus
 * 07.10.2010 | ERT | dumpInfo() erweitert um No. of Processors, Physical Memory und Swap Size
 * 19.10.2010 | ERT | dumpInfo() erweitert um IP-Adresse und OOo-Version
 * 22.02.2011 | ERT | dumpInfo() erweitert um LHM-Version
 * 08.05.2012 | jub | fakeSymLink behandlung eingebaut: der verweis auf fragmente, wie er in der
 *                    config datei steht, kann auf einen sog. fake SymLink gehen, eine text-
 *                    datei, in der auf ein anderes fragment inkl. relativem pfad verwiesen wird.
 * 11.12.2012 | jub | fakeSymLinks werden doch nicht gebraucht; wieder aus dem code entfernt
 * 17.05.2013 | ukt | Fontgröße wird jetzt immer gesetzt, unabhängig davon, ob der Wert in der
 *                    wollmuxbar.conf gesetzt ist oder nicht.
 *                    Andernfalls wird die Änderung der Fontgröße von einem Nicht-Defaultwert auf
 *                    den Default-Wert nicht angezeigt, wenn alle anderen Optionswerte ebenfalls
 *                    den Default-Wert haben.
 *
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
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
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XStringSubstitution;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.LogConfig;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 *
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf)
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxFiles.class);

  private static final String ETC_WOLLMUX_WOLLMUX_CONF = "/etc/wollmux/wollmux.conf";

  /**
   * Der Pfad (ohne Wurzel wie HKCU oder HKLM) zu dem Registrierungsschlüssel, unter dem der WollMux
   * seine Registry-Werte speichert
   */
  private static final String WOLLMUX_KEY = "Software\\WollMux";

  private static boolean debugMode = false;

  /**
   * Der Name des String-Wertes, unter dem der WollMux in der Registry den Ort der wollmux.conf
   * speichert
   */
  private static final String WOLLMUX_CONF_PATH_VALUE_NAME = "ConfigPath";

  private static final String WOLLMUX_NOCONF = L.m(
      "Es wurde keine WollMux-Konfiguration (wollmux.conf) gefunden - deshalb läuft WollMux im NoConfig-Modus.");
  /**
   * Die in der wollmux.conf mit DEFAULT_CONTEXT festgelegte URL.
   */
  private static URL defaultContextURL;

  /**
   * Enthält den geparsten Konfigruationsbaum der wollmux.conf
   */
  private static ConfigThingy wollmuxConf;

  /**
   * Das Verzeichnis ,wollmux.
   */
  private static File wollmuxDir;

  /**
   * Enthält einen PrintStream in den die Log-Nachrichten geschrieben werden.
   */
  private static File wollmuxLogFile;

  /**
   * Enthält das File der Konfigurationsdatei wollmux.conf
   */
  private static File wollmuxConfFile;

  /**
   * Enthält das File in des local-overwrite-storage-caches.
   */
  private static File losCacheFile;

  private WollMuxFiles()
  {
  }

  /**
   * Creates the '.wollmux' directory in user's home if it not exists.
   *
   * @return File newly created wollmux-Folder file reference.
   */
  private static File setupWollMuxDir()
  {
    String userHome = System.getProperty("user.home");
    wollmuxDir = new File(userHome, ".wollmux");

    if (!wollmuxDir.exists())
      wollmuxDir.mkdirs();

    return wollmuxDir;
  }

  private static ConfigThingy parseWollMuxConf(File wollMuxConfigFile)
  {
    wollmuxConf = new ConfigThingy("wollmuxConf");

    if (wollMuxConfigFile != null && wollMuxConfigFile.exists() && wollMuxConfigFile.isFile())
    {
      try
      {
        wollmuxConf = new ConfigThingy("wollmuxConf", wollMuxConfigFile.toURI().toURL());
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

  public static boolean initLocalization(ConfigThingy config)
  {
    if (config == null)
    {
      return false;
    }

    // Lokalisierung initialisieren
    ConfigThingy l10n = config.query("L10n", 1);
    if (l10n.count() > 0)
    {
      L.init(l10n);
    }

    return true;
  }

  public static void initLoggerOutputFile(File wollmuxLogFile, Level logLevel)
  {
    LogConfig.init(wollmuxLogFile, logLevel);
  }

  private static File findWollMuxConf(File wollMuxDir)
  {
    // Überprüfen, ob das Betriebssystem Windows ist
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");

    // Zum Aufsammeln der Pfade, an denen die wollmux.conf gesucht wurde:
    ArrayList<String> searchPaths = new ArrayList<>();

    // Pfad zur wollmux.conf
    String wollmuxConfPath = null;

    // wollmux.conf wird über die Umgebungsvariable "WOLLMUX_CONF_PATH" gesetzt.
    if (System.getenv("WOLLMUX_CONF_PATH") != null)
    {
      wollmuxConfPath = System.getenv("WOLLMUX_CONF_PATH");
      searchPaths.add(wollmuxConfPath);
    }

    searchPaths.add(new File(wollMuxDir, "wollmux.conf").getAbsolutePath());
    searchPaths.add(System.getProperty("user.dir") + "/.wollmux/wollmux.conf");
    searchPaths.add(ETC_WOLLMUX_WOLLMUX_CONF);

    if (windowsOS)
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
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + "/.wollmux/wollmux.conf");

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_COMMON_APPDATA, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + "/.wollmux/wollmux.conf");

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILESX86, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + "/.wollmux/wollmux.conf");

      arrWollmuxConfPath = new char[WinDef.MAX_PATH];
      shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILES, null, ShlObj.SHGFP_TYPE_CURRENT,
          arrWollmuxConfPath);
      searchPaths.add(String.valueOf(arrWollmuxConfPath) + "/.wollmux/wollmux.conf");

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
   * Liefert das Verzeichnis ,wollmux zurück.
   */
  public static File getWollMuxDir()
  {
    if (wollmuxDir == null)
      wollmuxDir = setupWollMuxDir();

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
   * Liefert das File-Objekt der Logdatei zurück. Darf erst nach setupWollMuxDir() aufgerufen
   * werden.
   *
   */
  public static File getWollMuxLogFile()
  {
    if (wollmuxLogFile == null || !wollmuxLogFile.exists())
      wollmuxLogFile = new File(getWollMuxDir(), "wollmux.log");

    return wollmuxLogFile;
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
      wollmuxConf = parseWollMuxConf(getWollMuxDir());

    return wollmuxConf;
  }

  /**
   * Diese Methode liefert den letzten in der Konfigurationsdatei definierten DEFAULT_CONTEXT
   * zurück. Ist in der Konfigurationsdatei keine URL definiert bzw. ist die Angabe fehlerhaft, so
   * wird die URL des .wollmux Verzeichnisses zurückgeliefert.
   */
  public static URL getDEFAULT_CONTEXT()
  {
    return defaultContextURL;
  }

  /**
   * Liefert eine URL zum String urlStr, wobei relative Pfade relativ zum DEFAULT_CONTEXT aufgelöst
   * werden.
   * 
   * @throws MalformedURLException
   *           falls urlStr keine legale URL darstellt.
   */
  public static URL makeURL(String urlStr) throws MalformedURLException
  {
    return new URL(WollMuxFiles.getDEFAULT_CONTEXT(), ConfigThingy.urlEncode(urlStr));
  }

  /**
   * Wertet den DEFAULT_CONTEXT aus wollmux.conf aus und erstellt eine entsprechende URL, mit der
   * {@link #defaultContextURL} initialisiert wird. Wenn in der wollmux.conf kein DEFAULT_CONTEXT
   * angegeben ist, so wird das Verzeichnis, in dem die wollmux.conf gefunden wurde, als Default
   * Context verwendet.
   *
   * Sollte {{@link #defaultContextURL} nicht <code>null</code> sein, tut diese Methode nichts.
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

      // url mit einem "/" aufhören lassen (falls noch nicht angegeben).
      String urlVerzStr;
      if (urlStr.endsWith("/") || urlStr.endsWith("\\"))
        urlVerzStr = urlStr;
      else
        urlVerzStr = urlStr + "/";

      try
      {
        /*
         * Die folgenden 3 Statements realisieren ein Fallback-Verhalten. Falls das letzte Statement
         * eine MalformedURLException wirft, dann gilt das vorige Statement. Hat dieses schon eine
         * MalformedURLException geworfen (sollte eigentlich nicht passieren können), so gilt immer
         * noch das erste.
         */
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
   * Wertet die wollmux,conf-Direktive LOGGING_MODE aus und setzt den Logging-Modus entsprechend.
   * Ist kein LOGGING_MODE gegeben, so greift der Standard (siehe Logger.java)
   *
   * @param ct
   */
  public static String getWollMuxConfLoggingMode(ConfigThingy ct)
  {
    if (ct == null || ct.count() == 0)
      return "info";

    ConfigThingy log = ct.query("LOGGING_MODE");
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

  public static boolean isDebugMode()
  {
    return debugMode;
  }

  /**
   * Erstellt eine Dump-Datei im WollMux-Verzeichnis, die wichtige Informationen zur Fehlersuche
   * enthält und liefert den Namen dieser Datei als String zurück, oder null falls bei der
   * Erstellung Fehler auftraten.
   */
  public static String dumpInfo()
  {
    Calendar cal = Calendar.getInstance();
    String date = "" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
        + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.getTimeInMillis();
    File dumpFile = new File(getWollMuxDir(), "dump" + date);
    try (OutputStream outStream = new FileOutputStream(dumpFile);
        BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(outStream, ConfigThingy.CHARSET));)
    {
      out.write("Dump time: " + date + "\n");
      out.write(WollMuxSingleton.getBuildInfo() + "\n");
      StringBuilder buffy = new StringBuilder();

      // IP-Adresse für localhost
      try
      {
        InetAddress addr = InetAddress.getLocalHost();
        out.write("Host: " + addr.getHostName() + " (" + addr.getHostAddress() + ")\n");
      } catch (UnknownHostException ex)
      {
        LOGGER.error("", ex);
        buffy.append("------");
      }

      /*
       * IP-Adressen bestimmen
       */
      try
      {
        InetAddress[] addresses = InetAddress.getAllByName(getDEFAULT_CONTEXT().getHost());
        for (int i = 0; i < addresses.length; ++i)
        {
          if (i > 0)
          {
            buffy.append(", ");
          }
          buffy.append(addresses[i].getHostAddress());
        }
      } catch (UnknownHostException e)
      {
        LOGGER.error("", e);
        buffy.append("------");
      }

      out.write("OOo-Version: \""
          + getConfigValue("/org.openoffice.Setup/Product", "ooSetupVersion") + "\n");

      out.write("DEFAULT_CONTEXT: \"" + getDEFAULT_CONTEXT() + "\" (" + buffy + ")\n");
      out.write("CONF_VERSION: " + WollMuxSingleton.getInstance().getConfVersionInfo() + "\n");
      out.write("wollmuxDir: " + getWollMuxDir() + "\n");
      out.write("wollmuxLogFile: " + getWollMuxLogFile() + "\n");
      if (getWollMuxConfFile() != null)
      {
        out.write("wollmuxConfFile: " + getWollMuxConfFile() + "\n");
      }
      out.write("losCacheFile: " + getLosCacheFile() + "\n");

      out.write("===================== START JVM-Settings ==================\n");
      try
      {
        XStringSubstitution subst = UNO
            .XStringSubstitution(UNO.createUNOService("com.sun.star.util.PathSubstitution"));
        String jConfPath = new URL(subst.substituteVariables("$(user)/config", true)).toURI()
            .getPath();
        File[] jConfFiles = new File(jConfPath).listFiles();
        Pattern p = Pattern.compile("^javasettings_.*\\.xml$", Pattern.CASE_INSENSITIVE);
        boolean found = false;
        for (int i = 0; i < jConfFiles.length; i++)
        {
          if (p.matcher(jConfFiles[i].getName()).matches())
          {
            out.flush(); // weil wir gleich direkt auf den Stream zugreifen
            copyFile(jConfFiles[i], outStream);
            outStream.flush(); // sollte nicht nötig sein, schadet aber nicht
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
      out.write("No. of Processors: "
          + ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() + "\n");
      out.write("Maximum Heap Size: " + (maxMemory / 1024) + " KB\n");
      out.write("Currently Allocated Heap Size: " + (totalMemory / 1024) + " KB\n");
      out.write("Currently Used Memory: " + ((totalMemory - freeMemory) / 1024) + " KB\n");
      out.write("Maximum Physical Memory: " + getSystemMemoryAttribut("TotalPhysicalMemorySize")
          + " KB\n");
      out.write(
          "Free Physical Memory: " + getSystemMemoryAttribut("FreePhysicalMemorySize") + " KB\n");
      out.write("Maximum Swap Size: " + getSystemMemoryAttribut("TotalSwapSpaceSize") + " KB\n");
      out.write("Free Swap Size: " + getSystemMemoryAttribut("FreeSwapSpaceSize") + " KB\n");

      out.write("===================== END java-memoryinfo ==================\n");

      out.write("===================== START wollmuxConfFile ==================\n");
      out.flush(); // weil wir gleich direkt auf den Stream zugreifen
      if (getWollMuxConfFile() != null)
      {
        copyFile(getWollMuxConfFile(), outStream);
      }
      outStream.flush(); // sollte nicht nötig sein, schadet aber nicht
      out.write("\n");
      out.write("===================== END wollmuxConfFile ==================\n");

      out.write("===================== START wollmux.conf ==================\n");
      out.write(getWollmuxConf().stringRepresentation());
      out.write("===================== END wollmux.conf ==================\n");

      out.write("===================== START losCacheFile ==================\n");
      out.flush(); // weil wir gleich direkt auf den Stream zugreifen
      copyFile(getLosCacheFile(), outStream);
      outStream.flush(); // sollte nicht nötig sein, schadet aber nicht
      out.write("\n");
      out.write("===================== END losCacheFile ==================\n");

      out.write("===================== START wollmux.log ==================\n");
      out.flush(); // weil wir gleich direkt auf den Stream zugreifen
      copyFile(getWollMuxLogFile(), outStream);
      outStream.flush(); // sollte nicht nötig sein, schadet aber nicht
      out.write("\n");
      out.write("===================== END wollmux.log ==================\n");

      out.write("===================== START OOo-Configuration dump ==================\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Setup/Product") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Setup/L10N") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Office.Paths/") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Office.Writer/") + "\n");
      out.write(dumpOOoConfiguration("/org.openoffice.Inet/") + "\n");
      out.write("===================== END OOo-Configuration dump ==================\n");

      out.write("===================== START OOo datasources ==================\n");
      try
      {
        String[] datasourceNamesA = UNO.XNameAccess(UNO.dbContext).getElementNames();
        for (int i = 0; i < datasourceNamesA.length; ++i)
        {
          out.write(datasourceNamesA[i]);
          out.write("\n");
          try
          {
            XDataSource ds = UNO
                .XDataSource(UNO.dbContext.getRegisteredObject(datasourceNamesA[i]));
            ds.setLoginTimeout(1);
            XConnection conn = ds.getConnection("", "");
            XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
            for (String name : tables.getElementNames())
              out.write("  " + name + "\n");
          } catch (Exception x)
          {
            out.write("  " + x.toString() + "\n");
          }
        }
      } catch (Exception x)
      {
        out.write(x.toString() + "\n");
      }
      out.write("===================== END OOo datasources ==================\n");
    } catch (IOException | NumberFormatException | JMException x)
    {
      LOGGER.error(L.m("Fehler beim Erstellen des Dumps"), x);
      return null;
    }
    return dumpFile.getAbsolutePath();
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
      XMultiComponentFactory xMultiComponentFactory = UNO.defaultContext.getServiceManager();
      Object oProvider = xMultiComponentFactory.createInstanceWithContext(
          "com.sun.star.configuration.ConfigurationProvider", UNO.defaultContext);
      XMultiServiceFactory xConfigurationServiceFactory = UnoRuntime
          .queryInterface(XMultiServiceFactory.class, oProvider);

      PropertyValue[] lArgs = new PropertyValue[1];
      lArgs[0] = new PropertyValue();
      lArgs[0].Name = "nodepath";
      lArgs[0].Value = path;

      Object configAccess = xConfigurationServiceFactory
          .createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess", lArgs);

      XNameAccess xNameAccess = UnoRuntime.queryInterface(XNameAccess.class, configAccess);

      return xNameAccess.getByName(name).toString();
    } catch (Exception ex)
    {
      LOGGER.info("", ex);
      return "";
    }
  }

  /**
   * Kopiert den Inhalt von file nach out (binär).
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
   * Gibt den Inhalt der OOo-Konfiguration einschließlich aller Unterknoten am Knoten nodePath
   * zurück.
   */
  public static String dumpOOoConfiguration(String nodePath)
  {
    try
    {
      Object cfgProvider = UNO.createUNOService("com.sun.star.configuration.ConfigurationProvider");

      Object cfgAccess = UNO.XMultiServiceFactory(cfgProvider).createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationAccess",
          new UnoProps("nodepath", nodePath).getProps());

      return dumpNode(cfgAccess, "");
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
      return L.m("Fehler beim Auslesen der OOo-Konfiguration mit dem Nodepath '%1'", nodePath);
    }
  }

  /**
   * Gibt den Inhalt eines Knotens element der OOo-Konfiguration mit dem Knotennamen und allen
   * enthaltenen Properties zurück, wobei die Inhalte pro Zeile um den String spaces eingerückt
   * werden.
   *
   * @param element
   * @param spaces
   * @return String-Repräsentation des Knotens aus der OOo-Konfiguration.
   * @throws Exception
   */
  public static String dumpNode(Object element, String spaces)
  {
    // Properties (Elemente mit Werten) durchsuchen:
    StringBuilder properties = new StringBuilder("");
    if (UNO.XPropertySet(element) != null)
    {
      Property[] props = UNO.XPropertySet(element).getPropertySetInfo().getProperties();
      for (int i = 0; i < props.length; i++)
      {
        Object prop = Utils.getProperty(element, props[i].Name);
        if (UNO.XInterface(prop) != null || AnyConverter.isVoid(prop))
        {
          continue;
        }
        StringBuilder propStr = new StringBuilder("'" + prop + "'");
        // arrays anzeigen:
        if (prop instanceof Object[])
        {
          Object[] arr = (Object[]) prop;
          propStr.append("[");
          for (int j = 0; j < arr.length; j++)
            propStr.append("'" + arr[j] + "'" + ((j == arr.length - 1) ? "" : ", "));
          propStr.append("]");
        }
        properties.append(spaces + "|    " + props[i].Name + ": " + propStr + "\n");
      }
    }

    // Kinder durchsuchen.
    StringBuilder childs = new StringBuilder("");
    XNameAccess xna = UNO.XNameAccess(element);
    if (xna != null)
    {
      String[] elements = xna.getElementNames();
      for (int i = 0; i < elements.length; i++)
      {
        try
        {
          childs.append(dumpNode(xna.getByName(elements[i]), spaces + "|    "));
        } catch (java.lang.Exception e)
        {
        }
      }
    }

    // Knoten zusammenbauen: Eigener Name + properties + kinder (nur wenn der
    // Knoten auch angezeigte Properties oder Kinder hat):
    if (UNO.XNamed(element) != null && (properties.length() > 0 || childs.length() > 0))
      return spaces + "+ " + UNO.XNamed(element).getName() + "\n" + properties + childs;

    return "";
  }

  /**
   * Schreibt die Kinder von conf (also keinen umschließenden Wurzel-Abschnitt) in die Datei file.
   * 
   * @throws IOException
   */
  public static void writeConfToFile(File file, ConfigThingy conf) throws IOException
  {
    try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file),
        ConfigThingy.CHARSET))
    {
      out.write("\uFEFF");
      out.write(conf.stringRepresentation(true, '"'));
    }
  }

}
