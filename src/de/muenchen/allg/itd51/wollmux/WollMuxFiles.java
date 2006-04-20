/*
* Dateiname: WollMuxFiles.java
* Projekt  : WollMux
* Funktion : Managed die Dateien auf die der WollMux zugreift (z.B. wollmux.conf)
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 13.04.2006 | BNK | Erstellung
* 20.04.2006 | BNK | [R1200] .wollmux-Verzeichnis als Vorbelegung für DEFAULT_CONTEXT
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * 
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf) 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{
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
   * Inhalt der wollmux.conf-Datei, die angelegt wird, wenn noch keine
   * wollmux.conf-Datei vorhanden ist. Ist defaultWollmuxConf==null, so wird gar
   * keine wollmux.conf-Datei angelegt.
   */
  private static final String defaultWollmuxConf = "%include \"<Hier tragen Sie bitte die URL Ihrer zentralen wollmux-Konfigurationsdatei ein>\"\r\n";
  
  /**
   * Erzeugt das ,wollmux-Verzeichnis, falls es noch nicht existiert und
   * erstellt eine Standard-wollmux,conf. Initialisiert auch den Logger.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void setupWollMuxDir()
  {
    String userHome = System.getProperty("user.home");
    wollmuxDir = new File(userHome, ".wollmux");
    
    // .wollmux-Verzeichnis erzeugen falls es nicht existiert
    if (!wollmuxDir.exists()) wollmuxDir.mkdirs();

    wollmuxConfFile = new File(wollmuxDir, "wollmux.conf");

    losCacheFile = new File(wollmuxDir, "cache.conf");
    wollmuxLogFile = new File(wollmuxDir, "wollmux.log");
    
    // Default wollmux.conf erzeugen falls noch keine wollmux.conf existiert.
    if (!wollmuxConfFile.exists() && defaultWollmuxConf != null)
    {
      try
      {
        PrintStream wmconf = new PrintStream(new FileOutputStream(
            wollmuxConfFile));
        wmconf.println(defaultWollmuxConf);
        wmconf.close();
      }
      catch (FileNotFoundException e) {}
    }
    
    /*
     * Zuerst leeres ConfigThingy anlegen, damit wollmuxConf auch dann wohldefiniert
     * ist, wenn die Datei Fehler enthält bzw. fehlt.
     */
    wollmuxConf = new ConfigThingy("wollmuxConf");
    
    // Logger initialisieren:
    if (WollMuxFiles.getWollMuxLogFile() != null) Logger.init(WollMuxFiles.getWollMuxLogFile(), Logger.LOG);
    
    /*
     * Jetzt versuchen, die wollmux.conf zu parsen.
     */
    try
    {
      wollmuxConf = new ConfigThingy("wollmuxConf", getWollMuxConfFile().toURI().toURL());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    setLoggingMode(WollMuxFiles.getWollmuxConf());
    
    determineDefaultContext();
  }

  /**
   * Liefert das Verzeichnis ,wollmux zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static File getWollMuxDir()
  {
    return wollmuxDir;
  }
  
  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück.
   * Darf erst nach setupWollMuxDir() aufgerufen werden.
   * 
   * @return das File-Objekt des LocalOverrideStorage Caches.
   */
  public static File getWollMuxConfFile()
  {
    return wollmuxConfFile;
  }
  
  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück.
   * Darf erst nach setupWollMuxDir() aufgerufen werden.
   * 
   * @return das File-Objekt des LocalOverrideStorage Caches.
   */
  public static File getWollMuxLogFile()
  {
    return wollmuxLogFile;
  }
  
  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück.
   * Darf erst nach setupWollMuxDir() aufgerufen werden.
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
   * DEFAULT_CONTEXT zurück. Ist in der Konfigurationsdatei keine URL definiert
   * bzw. ist die Angabe fehlerhaft, so wird die URL des .wollmux Verzeichnisses 
   * zurückgeliefert.
   */
  public static URL getDEFAULT_CONTEXT()
  {
    return defaultContextURL;
  }
 
  /**
   * Werten den DEFAULT_CONTEXT aus wollmux,conf aus und erstellt eine
   * entsprechende URL. 
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
      String urlVerzStr = (urlStr.endsWith("/")) ? urlStr : urlStr + "/";

      // URL aus urlVerzStr erzeugen
      try
      {
        defaultContextURL = new URL("file:///");
        defaultContextURL = getWollMuxDir().toURI().toURL();
        defaultContextURL = new URL(defaultContextURL,urlVerzStr);
      }
      catch (MalformedURLException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Wertet die undokumentierte wollmux.conf-Direktive LOGGING_MODE aus und
   * setzt den Logging-Modus entsprechend.
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

  
}
