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

/**
 * 
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf) 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{
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
   * erstellt eine Standard-wollmux,conf.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void setupWollMuxDir()
  {
    String userHome = System.getProperty("user.home");
    File wollmuxDir = new File(userHome, ".wollmux");
    
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
}
