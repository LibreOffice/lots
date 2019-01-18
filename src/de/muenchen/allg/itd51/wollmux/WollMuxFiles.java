/*
 * Dateiname: WollMuxFiles.java
 * Projekt  : WollMux
 * Funktion : Managed die Dateien auf die der WollMux zugreift (z.B. wollmux.conf)
 *
 * Copyright (c) 2009-2018 Landeshauptstadt München
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
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

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.management.OperatingSystemMXBean;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lib.loader.RegistryAccess;
import com.sun.star.lib.loader.RegistryAccessException;
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
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

/**
 *
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf)
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{
  private static final String ETC_WOLLMUX_WOLLMUX_CONF = "/etc/wollmux/wollmux.conf";

  /**
   * Der Pfad (ohne Wurzel wie HKCU oder HKLM) zu dem Registrierungsschlüssel, unter
   * dem der WollMux seine Registry-Werte speichert
   */
  private final static String WOLLMUX_KEY = "Software\\WollMux";

  /**
   * Der Name des String-Wertes, unter dem der WollMux in der Registry den Ort der
   * wollmux.conf speichert
   */
  private final static String WOLLMUX_CONF_PATH_VALUE_NAME = "ConfigPath";

  private static final String WOLLMUX_NOCONF =
    L.m("Es wurde keine WollMux-Konfiguration (wollmux.conf) gefunden - deshalb läuft WollMux im NoConfig-Modus.");

  /**
   * Wenn nach dieser Anzahl Millisekunden die Konfiguration noch nicht vollständig
   * eingelesen ist, wird ein Popup mit der Meldung {@link #SLOW_SERVER_MESSAGE}
   * gebracht.
   */
  private static final long SLOW_SERVER_TIMEOUT = 10000;

  /**
   * Siehe {@link #SLOW_SERVER_TIMEOUT}.
   */
  private static final String SLOW_SERVER_MESSAGE =
    L.m("Ihr Vorlagen-Server und/oder Ihre Netzwerkverbindung sind sehr langsam.\nDies kann die Arbeit mit Office stark beeinträchtigen.");

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

  /**
   * Gibt an, ob der debug-Modus aktiviert ist.
   */
  private static boolean debugMode;

  /**
   * Gibt an, dass die WollMuxBar im Falle einer fehlenden WollMux-Komponente in OOo
   * ihren eigenen WollMux instanziieren soll.
   */
  private static boolean externalWollMuxEnabled = false;

  private static boolean showCredits = false;

  /**
   * wollmux.conf: QA_TEST_HANDLER "true/false".
   */
  private static boolean installQATestHandler = false;

  /**
   * Erzeugt das .wollmux-Verzeichnis im Home-Verzeichnis des Benutzers (falls es
   * noch nicht existiert), sucht nach der wollmux.conf und parst sie. Initialisiert
   * auch den Logger.
   * <p>
   * Die wollmux.conf wird an folgenden Stellen in der angegebenen Reihenfolge
   * gesucht:
   *
   * <ol>
   * <li>unter dem Dateipfad (inkl. Dateiname!), der im Registrierungswert
   * "ConfigPath" des Schlüssels HKCU\Software\WollMux\ festgelegt ist (nur Windows!)
   * </li>
   * <li>$HOME/.wollmux/wollmux.conf (wobei $HOME unter Windows das Profilverzeichnis
   * bezeichnet)</li>
   * <li>unter dem Dateipfad (inkl. Dateiname!), der im Registrierungswert
   * "ConfigPath" des Schlüssels HKLM\Software\WollMux\ festgelegt ist (nur Windows!)
   * </li>
   * <li>unter dem Dateipfad, der in der Konstanten
   * {@link #C_PROGRAMME_WOLLMUX_WOLLMUX_CONF} festgelegt ist (nur Windows!)</li>
   * <li>unter dem Dateipfad, der in der Konstanten {@link #ETC_WOLLMUX_WOLLMUX_CONF}
   * festgelegt ist (nur Linux!)</li>
   * </ol>
   *
   * @return false für den den Fall no Config, true bei gefundener wollmux.conf
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static boolean setupWollMuxDir()
  {
    long time = System.currentTimeMillis(); // Zeitnahme fürs Debuggen
    boolean noConf = false; // kein no conf mode

    String userHome = System.getProperty("user.home");
    wollmuxDir = new File(userHome, ".wollmux");

    // .wollmux-Verzeichnis im userHome erzeugen falls es nicht existiert
    // Selbst wenn die wollmux.conf nicht im .wollmux-Verzeichnis liegt,
    // wird es dennoch für die cache.conf und wollmux.log benötigt
    if (!wollmuxDir.exists()) wollmuxDir.mkdirs();

    // cache.conf und wollmux.log im .wollmux-Verzeichnis
    losCacheFile = new File(wollmuxDir, "cache.conf");
    wollmuxLogFile = new File(wollmuxDir, "wollmux.log");

    StringBuilder debug2Messages = new StringBuilder();
    try
    {
      findWollMuxConf(debug2Messages);
      debug2Messages.append("wollmux.conf gefunden in "
          + wollmuxConfFile.getAbsolutePath());
        debug2Messages.append('\n');
    }
    catch (FileNotFoundException ex)
    {
      debug2Messages.append(ex.getLocalizedMessage());
      debug2Messages.append('\n');
    }

    // Bevor wir versuchen zu parsen wird auf jeden Fall ein leeres ConfigThingy
    // angelegt, damit wollmuxConf auch dann wohldefiniert ist, wenn die Datei
    // Fehler enthält bzw. fehlt.
    wollmuxConf = new ConfigThingy("wollmuxConf");

    SlowServerWatchdog fido = new SlowServerWatchdog(SLOW_SERVER_TIMEOUT);

    // Jetzt versuchen, die wollmux.conf zu parsen, falls sie existiert
    if (wollmuxConfFile != null && wollmuxConfFile.exists() && wollmuxConfFile.isFile())
    {
      fido.start();
      try
      {
        wollmuxConf =
          new ConfigThingy("wollmuxConf", wollmuxConfFile.toURI().toURL());
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
    else
    { // wollmux.conf existiert nicht - damit wechseln wir in den no config mode.
      noConf = true;
      Logger.log(WOLLMUX_NOCONF);
    }

    fido.dontBark();
    fido.logTimes();

    // Logging-Mode setzen
    setLoggingMode(WollMuxFiles.getWollmuxConf());

    // Gesammelte debug2 Messages rausschreiben
    Logger.debug2(debug2Messages.toString());

    // Lokalisierung initialisieren
    ConfigThingy l10n = getWollmuxConf().query("L10n", 1);
    if (l10n.count() > 0) L.init(l10n);
    Logger.debug(L.flushDebugMessages());

    showCredits =
      WollMuxFiles.getWollmuxConf().query("SHOW_CREDITS", 1).query("on").count() > 0;

    installQATestHandler =
      WollMuxFiles.getWollmuxConf().query("QA_TEST_HANDLER", 1).query("true").count() > 0;

    determineDefaultContext();

    initDebugMode();

    try
    { // TODO Switch dokumentieren, sobald er fehlerfrei implementiert ist
      // (Ticket #645/R4904)
      externalWollMuxEnabled =
        getWollmuxConf().get("ALLOW_EXTERNAL_WOLLMUX", 1).toString().equalsIgnoreCase(
          "true");
    }
    catch (Exception x)
    {}

    setLookAndFeel();

    Logger.debug(L.m(".wollmux init time: %1ms", ""
      + (System.currentTimeMillis() - time)));

    return !noConf;
  }

  private static void findWollMuxConf(StringBuilder debug2Messages)
      throws FileNotFoundException
  {
    // Überprüfen, ob das Betriebssystem Windows ist
    boolean windowsOS =
      System.getProperty("os.name").toLowerCase().contains("windows");

    // Zum Aufsammeln der Pfade, an denen die wollmux.conf gesucht wurde:
    ArrayList<String> searchPaths = new ArrayList<String>();

    // Logger initialisieren:
    Logger.init(wollmuxLogFile, Logger.LOG);

    // Pfad zur wollmux.conf
    String wollmuxConfPath = null;

    // wollmux.conf wird über die Umgebungsvariable "WOLLMUX_CONF_PATH" gesetzt.
    if (wollmuxConfFile == null || !wollmuxConfFile.exists())
    {
      if (System.getenv("WOLLMUX_CONF_PATH") != null)
      {
        wollmuxConfPath = System.getenv("WOLLMUX_CONF_PATH");
        searchPaths.add(wollmuxConfPath);
      }

      searchPaths.add(new File(wollmuxDir, "wollmux.conf").getAbsolutePath());
      searchPaths.add(System.getProperty("user.dir") + "/.wollmux/wollmux.conf");
      searchPaths.add(ETC_WOLLMUX_WOLLMUX_CONF);

      // Falls Windows:
      if (windowsOS)
      {
        // Versuch den Pfad der wollmux.conf aus HKCU-Registry zu lesen
        try
        {
          wollmuxConfPath =
            RegistryAccess.getStringValueFromRegistry(WinReg.HKEY_CURRENT_USER,
              WOLLMUX_KEY, WOLLMUX_CONF_PATH_VALUE_NAME);
          searchPaths.add(wollmuxConfPath);
        }
        catch (RegistryAccessException ex)
        {
          Logger.debug("Kein Registry-Eintrag unter HKEY_CURRENT_USER");
        }

        // Versuch den Pfad der wollmux.conf aus HKLM-Registry zu lesen
        try
        {
          wollmuxConfPath =
            RegistryAccess.getStringValueFromRegistry(WinReg.HKEY_LOCAL_MACHINE,
              WOLLMUX_KEY, WOLLMUX_CONF_PATH_VALUE_NAME);
          searchPaths.add(wollmuxConfPath);
        }
        catch (RegistryAccessException ex)
        {
          Logger.debug("Kein Registry-Eintrag unter HKEY_LOCAL_MACHINE");
        }

        Shell32 shell = Shell32.INSTANCE;

        char[] arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");
        
        arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_COMMON_APPDATA, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");

        arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILESX86, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");

        arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILES, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");
        
      }

      for (String path : searchPaths)
      {
        File confPath = new File(path);
        if (confPath.exists())
        {
          wollmuxConfFile = confPath;
          return;
        }
      }

      throw new FileNotFoundException(
        "wollmux.conf konnte nicht gefunden werden. Suchpfade.");
    }
  }

  /**
   * Liefert das Verzeichnis ,wollmux zurück.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static File getWollMuxDir()
  {
    return wollmuxDir;
  }

  /**
   * Liefert das File-Objekt der wollmux,conf zurück, die gelesen wurde (kann z,B,
   * auch die aus /etc/wollmux/ sein). Darf erst nach setupWollMuxDir() aufgerufen
   * werden.
   */
  public static File getWollMuxConfFile()
  {
    return wollmuxConfFile;
  }

  /**
   * Liefert das File-Objekt der Logdatei zurück. Darf erst nach setupWollMuxDir()
   * aufgerufen werden.
   *
   */
  public static File getWollMuxLogFile()
  {
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
    return losCacheFile;
  }

  /**
   * Liefert den Inhalt der wollmux,conf zurück.
   */
  public static ConfigThingy getWollmuxConf()
  {
    return wollmuxConf;
  }

  /**
   * Diese Methode liefert den letzten in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT zurück. Ist in der Konfigurationsdatei keine URL definiert bzw.
   * ist die Angabe fehlerhaft, so wird die URL des .wollmux Verzeichnisses
   * zurückgeliefert.
   */
  public static URL getDEFAULT_CONTEXT()
  {
    return defaultContextURL;
  }

  /**
   * Liefert eine URL zum String urlStr, wobei relative Pfade relativ zum
   * DEFAULT_CONTEXT aufgelöst werden.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws MalformedURLException
   *           falls urlStr keine legale URL darstellt.
   */
  public static URL makeURL(String urlStr) throws MalformedURLException
  {
    return new URL(WollMuxFiles.getDEFAULT_CONTEXT(), ConfigThingy.urlEncode(urlStr));
  }

  /**
   * Wertet den DEFAULT_CONTEXT aus wollmux.conf aus und erstellt eine entsprechende
   * URL, mit der {@link #defaultContextURL} initialisiert wird. Wenn in der
   * wollmux.conf kein DEFAULT_CONTEXT angegeben ist, so wird das Verzeichnis, in dem
   * die wollmux.conf gefunden wurde, als Default Context verwendet.
   *
   * Sollte {{@link #defaultContextURL} nicht <code>null</code> sein, tut diese
   * Methode nichts.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void determineDefaultContext()
  {
    if (defaultContextURL == null)
    {
      ConfigThingy dc = getWollmuxConf().query("DEFAULT_CONTEXT");
      String urlStr;
      try
      {
        urlStr = dc.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
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
         * Die folgenden 3 Statements realisieren ein Fallback-Verhalten. Falls das
         * letzte Statement eine MalformedURLException wirft, dann gilt das vorige
         * Statement. Hat dieses schon eine MalformedURLException geworfen (sollte
         * eigentlich nicht passieren können), so gilt immer noch das erste.
         */
        defaultContextURL = new URL("file:///");
        if (getWollMuxConfFile() != null)
        {
          defaultContextURL = getWollMuxConfFile().toURI().toURL();
        }
        defaultContextURL = new URL(defaultContextURL, urlVerzStr);
      }
      catch (MalformedURLException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Wertet die FONT_ZOOM-Direktive des Dialoge-Abschnitts aus und zoomt die Fonts
   * falls erforderlich.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeel()
  {
    Common.setLookAndFeelOnce();
    double zoomFactor = 1.0;
    ConfigThingy zoom = getWollmuxConf().query("Dialoge").query("FONT_ZOOM", 2);
    if (zoom.count() > 0)
    {
      try
      {
        zoomFactor = Double.parseDouble(zoom.getLastChild().toString());
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
    zoomFonts(zoomFactor);
  }

  /**
   * Zoomt die Fonts auf zoomFactor, falls erforderlich.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void zoomFonts(double zoomFactor)
  {

    if (zoomFactor < 0.5 || zoomFactor > 3)
    {
      Logger.error(L.m("Unsinniger FONT_ZOOM Wert angegeben: %1", "" + zoomFactor));
    }
    else
    {
      // Frühere Prüfung (nur werte kleiner 0.99 oder größer 1.01) entfernt,
      // seitdem die Fontgröße im Gui eingestellt werden kann.
      // Mit dieser Prüfung wäre z. b. ein Zurückstellen
      // von Zoomfaktor 2 auf 1 abgelehnt worden.
      Common.zoomFonts(zoomFactor);
    }

  }

  /**
   * Wertet die wollmux,conf-Direktive LOGGING_MODE aus und setzt den Logging-Modus
   * entsprechend. Ist kein LOGGING_MODE gegeben, so greift der Standard (siehe
   * Logger.java)
   *
   * @param ct
   */
  private static void setLoggingMode(ConfigThingy ct)
  {
    ConfigThingy log = ct.query("LOGGING_MODE");
    if (log.count() > 0)
    {
      try
      {
        String mode = log.getLastChild().toString();
        Logger.init(mode);
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Gibt Auskunft darüber, sich der WollMux im debug-modus befindet; Der debug-modus
   * wird automatisch aktiviert, wenn der LOGGING_MODE auf "debug" oder "all" gesetzt
   * wurde. Im debug-mode werden z.B. die Bookmarks abgearbeiteter Dokumentkommandos
   * nach der Ausführung nicht entfernt, damit sich Fehler leichter finden lassen.
   *
   * @return
   */
  public static boolean isDebugMode()
  {
    return debugMode;
  }

  public static boolean externalWollMuxEnabled()
  {
    return externalWollMuxEnabled;
  }

  private static void initDebugMode()
  {
    ConfigThingy log = getWollmuxConf().query("LOGGING_MODE");
    if (log.count() > 0)
    {
      try
      {
        String mode = log.getLastChild().toString();
        if (mode.compareToIgnoreCase("debug") == 0
          || mode.compareToIgnoreCase("all") == 0)
        {
          debugMode = true;
        }
      }
      catch (Exception e)
      {}
    }
    else
      debugMode = false;
  }

  /**
   * Erstellt eine Dump-Datei im WollMux-Verzeichnis, die wichtige Informationen zur
   * Fehlersuche enthält und liefert den Namen dieser Datei als String zurück, oder
   * null falls bei der Erstellung Fehler auftraten.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz
   */
  public static String dumpInfo()
  {
    Calendar cal = Calendar.getInstance();
    String date =
      "" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
        + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.getTimeInMillis();
    File dumpFile = new File(getWollMuxDir(), "dump" + date);
    try
    {
      OutputStream outStream = new FileOutputStream(dumpFile);
      BufferedWriter out =
        new BufferedWriter(new OutputStreamWriter(outStream, ConfigThingy.CHARSET));
      out.write("Dump time: " + date + "\n");
      out.write(WollMuxSingleton.getBuildInfo() + "\n");
      StringBuilder buffy = new StringBuilder();

      // IP-Adresse für localhost
      try
      {
        InetAddress addr = InetAddress.getLocalHost();
        out.write("Host: " + addr.getHostName() + " (" + addr.getHostAddress()
          + ")\n");
      }
      catch (UnknownHostException ex)
      {
        Logger.error(ex);
        buffy.append("------");
      }

      /*
       * IP-Adressen bestimmen
       */
      try
      {
        InetAddress[] addresses =
          InetAddress.getAllByName(getDEFAULT_CONTEXT().getHost());
        for (int i = 0; i < addresses.length; ++i)
        {
          if (i > 0) buffy.append(", ");
          buffy.append(addresses[i].getHostAddress());
        }
      }
      catch (UnknownHostException e)
      {
        Logger.error(e);
        buffy.append("------");
      }

      out.write("OOo-Version: \""
        + getConfigValue("/org.openoffice.Setup/Product", "ooSetupVersion") + "\n");
      out.write("LHM-Version: \""
        + getConfigValue("/de.muenchen.LHM.LHMBuild/Version", "lhmVersion") + "\n");

      out.write("DEFAULT_CONTEXT: \"" + getDEFAULT_CONTEXT() + "\" (" + buffy
        + ")\n");
      out.write("CONF_VERSION: "
        + WollMuxSingleton.getInstance().getConfVersionInfo() + "\n");
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
        XStringSubstitution subst =
          UNO.XStringSubstitution(UNO.createUNOService("com.sun.star.util.PathSubstitution"));
        String jConfPath =
          new URL(subst.substituteVariables("$(user)/config", true)).toURI().getPath();
        File[] jConfFiles = new File(jConfPath).listFiles();
        Pattern p =
          Pattern.compile("^javasettings_.*\\.xml$", Pattern.CASE_INSENSITIVE);
        boolean found = false;
        for (int i = 0; i < jConfFiles.length; i++)
        {
          if (!p.matcher(jConfFiles[i].getName()).matches()) continue;
          out.flush(); // weil wir gleich direkt auf den Stream zugreifen
          copyFile(jConfFiles[i], outStream);
          outStream.flush(); // sollte nicht nötig sein, schadet aber nicht
          out.write("\n");
          found = true;
          break;
        }
        if (!found)
          out.write(L.m("Datei '%1' konnte nicht gefunden werden.\n", jConfPath
            + "/javasettings_*.xml"));
      }
      catch (java.lang.Exception e)
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
      OperatingSystemMXBean osmb =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
      out.write("No. of Processors: " + osmb.getAvailableProcessors() + "\n");
      out.write("Maximum Heap Size: " + (maxMemory / 1024) + " KB\n");
      out.write("Currently Allocated Heap Size: " + (totalMemory / 1024) + " KB\n");
      out.write("Currently Used Memory: " + ((totalMemory - freeMemory) / 1024)
        + " KB\n");
      out.write("Maximum Physical Memory: "
        + (osmb.getTotalPhysicalMemorySize() / 1024) + " KB\n");
      out.write("Free Physical Memory: " + (osmb.getFreePhysicalMemorySize() / 1024)
        + " KB\n");
      out.write("Maximum Swap Size: " + (osmb.getTotalSwapSpaceSize() / 1024)
        + " KB\n");
      out.write("Free Swap Size: " + (osmb.getFreeSwapSpaceSize() / 1024) + " KB\n");

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
            XDataSource ds =
              UNO.XDataSource(UNO.dbContext.getRegisteredObject(datasourceNamesA[i]));
            ds.setLoginTimeout(1);
            XConnection conn = ds.getConnection("", "");
            XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
            for (String name : tables.getElementNames())
              out.write("  " + name + "\n");
          }
          catch (Exception x)
          {
            out.write("  " + x.toString() + "\n");
          }
        }
      }
      catch (Exception x)
      {
        out.write(x.toString() + "\n");
      }
      out.write("===================== END OOo datasources ==================\n");

      out.close();
    }
    catch (IOException x)
    {
      Logger.error(L.m("Fehler beim Erstellen des Dumps"), x);
      return null;
    }
    return dumpFile.getAbsolutePath();
  }

  private static String getConfigValue(String path, String name)
  {
    try
    {
      XMultiComponentFactory xMultiComponentFactory =
        UNO.defaultContext.getServiceManager();
      Object oProvider =
        xMultiComponentFactory.createInstanceWithContext(
          "com.sun.star.configuration.ConfigurationProvider", UNO.defaultContext);
      XMultiServiceFactory xConfigurationServiceFactory =
        UnoRuntime.queryInterface(XMultiServiceFactory.class,
        oProvider);

      PropertyValue[] lArgs = new PropertyValue[1];
      lArgs[0] = new PropertyValue();
      lArgs[0].Name = "nodepath";
      lArgs[0].Value = path;

      Object configAccess =
        xConfigurationServiceFactory.createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationAccess", lArgs);

      XNameAccess xNameAccess =
        UnoRuntime.queryInterface(XNameAccess.class, configAccess);

      return xNameAccess.getByName(name).toString();
    }
    catch (Exception ex)
    {
      Logger.log(ex);
      return "";
    }
  }

  /**
   * Kopiert den Inhalt von file nach out (binär).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void copyFile(File file, OutputStream out)
  {
    InputStream in = null;
    try
    {
      in = new FileInputStream(file);
      byte[] buffy = new byte[2048];
      int len;
      while ((len = in.read(buffy)) >= 0)
      {
        out.write(buffy, 0, len);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace(new PrintWriter(out));
    }
    finally
    {
      try
      {
        in.close();
      }
      catch (Exception x)
      {}
    }
  }

  /**
   * Gibt den Inhalt der OOo-Konfiguration einschließlich aller Unterknoten am Knoten
   * nodePath zurück.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static String dumpOOoConfiguration(String nodePath)
  {
    try
    {
      Object cfgProvider =
        UNO.createUNOService("com.sun.star.configuration.ConfigurationProvider");

      Object cfgAccess =
        UNO.XMultiServiceFactory(cfgProvider).createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationAccess",
          new UnoProps("nodepath", nodePath).getProps());

      return dumpNode(cfgAccess, "");
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
      return L.m("Fehler beim Auslesen der OOo-Konfiguration mit dem Nodepath '%1'",
        nodePath);
    }
  }

  /**
   * Gibt den Inhalt eines Knotens element der OOo-Konfiguration mit dem Knotennamen
   * und allen enthaltenen Properties zurück, wobei die Inhalte pro Zeile um den
   * String spaces eingerückt werden.
   *
   * @param element
   * @param spaces
   * @return
   * @throws Exception
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static String dumpNode(Object element, String spaces)
  {
    // Properties (Elemente mit Werten) durchsuchen:
    String properties = "";
    if (UNO.XPropertySet(element) != null)
    {
      Property[] props =
        UNO.XPropertySet(element).getPropertySetInfo().getProperties();
      for (int i = 0; i < props.length; i++)
      {
        Object prop = UNO.getProperty(element, props[i].Name);
        if (UNO.XInterface(prop) != null) continue;
        if (AnyConverter.isVoid(prop)) continue;
        String propStr = "'" + prop + "'";
        // arrays anzeigen:
        if (prop instanceof Object[])
        {
          Object[] arr = (Object[]) prop;
          propStr = "[";
          for (int j = 0; j < arr.length; j++)
            propStr += "'" + arr[j] + "'" + ((j == arr.length - 1) ? "" : ", ");
          propStr += "]";
        }
        properties += spaces + "|    " + props[i].Name + ": " + propStr + "\n";
      }
    }

    // Kinder durchsuchen.
    String childs = "";
    XNameAccess xna = UNO.XNameAccess(element);
    if (xna != null)
    {
      String[] elements = xna.getElementNames();
      for (int i = 0; i < elements.length; i++)
      {
        try
        {
          childs += dumpNode(xna.getByName(elements[i]), spaces + "|    ");
        }
        catch (java.lang.Exception e)
        {}
      }
    }

    // Knoten zusammenbauen: Eigener Name + properties + kinder (nur wenn der
    // Knoten auch angezeigte Properties oder Kinder hat):
    if (UNO.XNamed(element) != null
      && (properties.length() > 0 || childs.length() > 0))
      return spaces + "+ " + UNO.XNamed(element).getName() + "\n" + properties
        + childs;

    return "";
  }

  /**
   * Schreibt die Kinder von conf (also keinen umschließenden Wurzel-Abschnitt) in
   * die Datei file.
   */
  public static void writeConfToFile(File file, ConfigThingy conf)
      throws UnsupportedEncodingException, FileNotFoundException, IOException
  {
    Writer out = null;
    try
    {
      out = new OutputStreamWriter(new FileOutputStream(file), ConfigThingy.CHARSET);
      out.write("\uFEFF");
      out.write(conf.stringRepresentation(true, '"'));
    }
    finally
    {
      try
      {
        out.close();
      }
      catch (Exception x)
      {}
    }
  }

  private static class SlowServerWatchdog extends Thread
  {
    private long initTime;

    private long startTime;

    private long endTime;

    private long timeout;

    private long testTime;

    private long dontBarkTime = 0;

    private boolean[] bark = new boolean[] { true };

    public SlowServerWatchdog(long timeout)
    {
      initTime = System.currentTimeMillis();
      this.timeout = timeout;
      setDaemon(true);
    }

    @Override
    public void run()
    {
      startTime = System.currentTimeMillis();
      endTime = startTime + timeout;
      while (true)
      {
        long wait = endTime - System.currentTimeMillis();
        if (wait <= 0) break;
        try
        {
          Thread.sleep(wait);
        }
        catch (InterruptedException e)
        {}
      }

      synchronized (bark)
      {
        testTime = System.currentTimeMillis();
        if (!bark[0]) return;
      }

      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          Logger.error(SLOW_SERVER_MESSAGE);
          JOptionPane pane =
            new JOptionPane(SLOW_SERVER_MESSAGE, JOptionPane.WARNING_MESSAGE,
              JOptionPane.DEFAULT_OPTION);
          JDialog dialog = pane.createDialog(null, L.m("Hinweis"));
          dialog.setModal(false);
          dialog.setVisible(true);
        }
      });
    }

    public void dontBark()
    {
      synchronized (bark)
      {
        dontBarkTime = System.currentTimeMillis();
        bark[0] = false;
      }
    }

    public void logTimes()
    {
      Logger.debug("init: " + initTime + " start: " + startTime + " end: " + endTime
        + " test: " + testTime + " dontBark: " + dontBarkTime);
    }

  }

  public static boolean installQATestHandler()
  {
    return installQATestHandler;
  }

  public static boolean showCredits()
  {
    return showCredits;
  }

  public static void showCredits(boolean showCredits)
  {
    WollMuxFiles.showCredits = showCredits;
  }

}
