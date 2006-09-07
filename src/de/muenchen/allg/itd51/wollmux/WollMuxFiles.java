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
* 26.05.2006 | BNK | +DJ Initialisierung
* 20.06.2006 | BNK | keine wollmux.conf mehr anlegen wenn nicht vorhanden
*                  | /etc/wollmux/wollmux.conf auswerten
* 26.06.2006 | BNK | Dialoge/FONT_ZOOM auswerten. LookAndFeel setzen. 
* 07.09.2006 | BNK | isDebugMode effizienter gemacht.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DatasourceSearchDialog;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * 
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf) 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{
  private static final String ETC_WOLLMUX_WOLLMUX_CONF = "/etc/wollmux/wollmux.conf";
  
  /**
   * Die in der wollmux.conf mit DEFAULT_CONTEXT festgelegte URL.
   */
  private static URL defaultContextURL;
  
  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;
  
  /**
   * Falls true, wurde bereits versucht, den DJ zu initialisieren (über den
   * Erfolg des Versuchs sagt die Variable nichts.)
   */
  private static boolean djInitialized = false;

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
   * Inhalt der wollmux.conf-Datei, die angelegt wird, wenn noch keine
   * wollmux.conf-Datei vorhanden ist. Ist defaultWollmuxConf==null, so wird gar
   * keine wollmux.conf-Datei angelegt.
   */
  private static final String defaultWollmuxConf = null; //"# %include \"<Entfernen Sie das # am Anfang der Zeile und tragen Sie hier die URL Ihrer zentralen wollmux-Konfigurationsdatei ein>\"\r\n";
  
  /**
   * Erzeugt das ,wollmux-Verzeichnis, falls es noch nicht existiert und
   * erstellt eine Standard-wollmux,conf. Initialisiert auch den Logger.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
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
     * Jetzt versuchen, die wollmux.conf zu parsen (falls die Datei existiert).
     */
    try
    {
      if (getWollMuxConfFile().exists())
        wollmuxConf = new ConfigThingy("wollmuxConf", getWollMuxConfFile().toURI().toURL());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    
    /*
     * Logging-Mode zum ersten Mal setzen. Wird nachher nochmal gesetzt, nachdem
     * wir /etc/wollmux/wollmux.conf geparst haben.
     */
    setLoggingMode(WollMuxFiles.getWollmuxConf());
    
    /*
     * Falls die obige wollmux.conf keinen DEFAULT_CONTEXT definiert,
     * so wird falls /etc/wollmux/wollmux.conf existiert diese der oben
     * geparsten wollmux.conf aus dem HOME-Verzeichnis vorangestellt.
     */
    if (wollmuxConf.query("DEFAULT_CONTEXT", 1).count()==0)
    try {
      File etc_wollmux_conf = new File(ETC_WOLLMUX_WOLLMUX_CONF);
      if (etc_wollmux_conf.exists())
      {
        ConfigThingy etcWollmuxConf = new ConfigThingy("etcWollmuxConf", etc_wollmux_conf.toURI().toURL());
        Logger.debug(ETC_WOLLMUX_WOLLMUX_CONF+" gelesen");
        Iterator iter = wollmuxConf.iterator();
        while (iter.hasNext()) etcWollmuxConf.addChild((ConfigThingy)iter.next());
        wollmuxConf = etcWollmuxConf;
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    /*
     * Logging-Mode endgültig setzen.
     */
    setLoggingMode(WollMuxFiles.getWollmuxConf());
    
    determineDefaultContext();
    
    initDebugMode();
    
    setLookAndFeel();
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
   * Initialisiert den DJ wenn nötig und liefert ihn dann zurück (oder null,
   * falls ein Fehler während der Initialisierung aufgetreten ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DatasourceJoiner getDatasourceJoiner()
  {
    if (!djInitialized)
    {
      djInitialized = true;
      ConfigThingy senderSource = WollMuxFiles.getWollmuxConf().query(
      "SENDER_SOURCE");
      String senderSourceStr = "";
      try
      {
        senderSourceStr = senderSource.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error("Keine Hauptdatenquelle SENDER_SOURCE definiert! Setze SENDER_SOURCE=\"\".");
      }
      try
      {
        datasourceJoiner = new DatasourceJoiner(getWollmuxConf(),
            senderSourceStr, getLosCacheFile(), getDEFAULT_CONTEXT());
      }
      catch (ConfigurationErrorException e)
      {
        Logger.error(e);
      }
    }
    
    return datasourceJoiner;
  }
 
  /**
   * Werten den DEFAULT_CONTEXT aus wollmux,conf aus und erstellt eine
   * entsprechende URL. 
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED 
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

      try
      {
        /*
         * Die folgenden 3 Statements realisieren ein Fallback-Verhalten.
         * Falls das letzte Statement eine MalformedURLException wirft, dann
         * gilt das vorige Statement. Hat dieses schon eine MalformedURLException
         * geworfen (sollte eigentlich nicht passieren können), so gilt immer noch
         * das erste.
         */
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
   * Wertet die FONT_ZOOM-Direktive des Dialoge-Abschnitts aus und zoomt die
   * Fonts falls erforderlich.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeel()
  {
    Common.setLookAndFeelOnce();
    ConfigThingy zoom = getWollmuxConf().query("Dialoge").query("FONT_ZOOM", 2);
    if (zoom.count() > 0)
    {
      try {
        double zoomFactor = Double.parseDouble(zoom.getLastChild().toString());
        if (zoomFactor < 0.5 || zoomFactor > 10)
        {
          Logger.error("Unsinniger FONT_ZOOM Wert angegeben: "+zoomFactor);
        }
        else
        {
          if (zoomFactor < 0.99 || zoomFactor >1.01) 
            Common.zoomFonts(zoomFactor);
        }
      } catch(Exception x)
      {
        Logger.error(x);
      }
    }
  }
  
  /**
   * Wertet die undokumentierte wollmux.conf-Direktive LOGGING_MODE aus und
   * setzt den Logging-Modus entsprechend. Ist kein LOGGING_MODE gegeben, so
   * greift der Standard (siehe Logger.java)
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
   * Gibt Auskunft darüber, sich der WollMux im debug-modus befindet; 
   * Der debug-modus wird automatisch aktiviert, wenn der LOGGING_MODE
   * auf "debug" oder "all" gesetzt wurde. Im debug-mode werden z.B.
   * die Bookmarks abgearbeiteter Dokumentkommandos nach der Ausführung
   * nicht entfernt, damit sich Fehler leichter finden lassen.
   *  
   * @return
   */
  public static boolean isDebugMode() 
  {
    return debugMode;
  }
  
  private static void initDebugMode()
  {
    ConfigThingy log = getWollmuxConf().query("LOGGING_MODE");
    if(log.count() > 0) {
      try
      {
        String mode = log.getLastChild().toString();
        if(mode.compareToIgnoreCase("debug") == 0 
            || mode.compareToIgnoreCase("all") == 0)
        {
          debugMode = true;
        }
      } catch (Exception e) {}
    }
    else
      debugMode = false;
  }

  /**
   * Parst die "Funktionsdialoge" Abschnitte aus conf und liefert als Ergebnis
   * eine DialogLibrary zurück.
   * 
   * @param baselib falls nicht-null wird diese als Fallback verlinkt, um Dialoge
   *        zu liefern, die anderweitig nicht gefunden werden.
   * @param context der Kontext in dem in Dialogen enthaltene Funktionsdefinitionen 
   *        ausgewertet werden sollen (insbesondere DIALOG-Funktionen). 
   *        ACHTUNG! Hier werden Werte
   *        gespeichert, es ist nicht nur ein Schlüssel.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DialogLibrary parseFunctionDialogs(ConfigThingy conf, DialogLibrary baselib, Map context)
  {
    DialogLibrary funcDialogs = new DialogLibrary(baselib);

    Set dialogsInBlock = new HashSet();

    conf = conf.query("Funktionsdialoge");
    Iterator parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      dialogsInBlock.clear();
      Iterator iter = ((ConfigThingy) parentIter.next()).iterator();
      while (iter.hasNext())
      {
        ConfigThingy dialogConf = (ConfigThingy) iter.next();
        String name = dialogConf.getName();
        if (dialogsInBlock.contains(name))
          Logger
              .error("Funktionsdialog \""
                     + name
                     + "\" im selben Funktionsdialoge-Abschnitt mehrmals definiert");
        dialogsInBlock.add(name);
        try
        {
          funcDialogs.add(name, DatasourceSearchDialog.create(dialogConf, getDatasourceJoiner()));
        }
        catch (ConfigurationErrorException e)
        {
          Logger.error("Fehler in Funktionsdialog" + name, e);
        }
      }
    }
    
    return funcDialogs;
  }

  /**
   * Parst die "Funktionen" Abschnitte aus conf und liefert eine entsprechende
   * FunctionLibrary.
   * 
   * @param context der Kontext in dem die Funktionsdefinitionen ausgewertet werden
   *        sollen (insbesondere DIALOG-Funktionen). ACHTUNG! Hier werden Werte
   *        gespeichert, es ist nicht nur ein Schlüssel.
   *        
   * @param baselib falls nicht-null wird diese als Fallback verlinkt, um Funktionen
   *        zu liefern, die anderweitig nicht gefunden werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FunctionLibrary parseFunctions(ConfigThingy conf, DialogLibrary dialogLib, Map context, FunctionLibrary baselib)
  {
    FunctionLibrary funcs = new FunctionLibrary(baselib);

    conf = conf.query("Funktionen");
    Iterator parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      Iterator iter = ((ConfigThingy) parentIter.next()).iterator();
      while (iter.hasNext())
      {
        ConfigThingy funcConf = (ConfigThingy) iter.next();
        String name = funcConf.getName();
        try
        {
          Function func = FunctionFactory.parseChildren(
              funcConf,
              funcs,
              dialogLib,
              context);
          funcs.add(name, func);
        }
        catch (ConfigurationErrorException e)
        {
          Logger.error("Fehler beim Parsen der Funktion \"" + name + "\"", e);
        }
      }
    }
    
    return funcs;
  }

}
