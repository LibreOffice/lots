/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : UNO-Service WollMux; Singleton und zentrale WollMux-Instanz.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service dient
 * als Einstiegspunkt des WollMux und initialisiert alle benötigten
 * Programmmodule. Sämtliche Felder und öffentliche Methoden des Services sind
 * static und ermöglichen den Zugriff aus anderen Programmmodulen.
 */
public class WollMuxSingleton implements XPALChangeEventBroadcaster,
    XPALProvider
{

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Enthält einen PrintStream in den die Log-Nachrichten geschrieben werden.
   */
  private File wollmuxLogFile;

  /**
   * Enthält das File der Konfigurationsdatei wollmux.conf
   */
  private File wollmuxConfFile;

  /**
   * Enthält das File in des local-overwrite-storage-caches.
   */
  private File losCacheFile;

  /**
   * Enthält den geparsten Konfigruationsbaum der wollmux.conf
   */
  private ConfigThingy wollmuxConf;

  /**
   * Enthält die geparste Textfragmentliste, die in der wollmux.conf definiert
   * wurde.
   */
  private VisibleTextFragmentList textFragmentList;

  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private DatasourceJoiner datasourceJoiner;

  /**
   * Enthält den default XComponentContext in dem der WollMux (bzw. das OOo)
   * läuft.
   */
  private XComponentContext ctx;

  /**
   * Die URL des Wertes DEFAULT_CONTEXT aus der Konfigurationsdatei.
   * defaultContext überschreibt den in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT.
   */
  private URL defaultContextURL;

  /**
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private Vector registeredPALChangeListener;

  /**
   * Inhalt der wollmux.conf-Datei, die angelegt wird, wenn noch keine
   * wollmux.conf-Datei vorhanden ist. Ist defaultWollmuxConf==null, so wird gar
   * keine wollmux.conf-Datei angelegt.
   */
  private final String defaultWollmuxConf = null; 
  //"%include \"http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/wollmux.conf\"\r\n";

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    registeredPALChangeListener = new Vector();
    this.ctx = ctx;

    // Das hier sollte die einzige Stelle sein, an der Pfade hart
    // verdrahtet sind...
    String userHome = System.getProperty("user.home");
    File wollmuxDir = new File(userHome, ".wollmux");
    this.wollmuxConfFile = new File(wollmuxDir, "wollmux.conf");
    this.losCacheFile = new File(wollmuxDir, "cache.conf");
    this.wollmuxLogFile = new File(wollmuxDir, "wollmux.log");

    // .wollmux-Verzeichnis erzeugen falls es nicht existiert
    if (!wollmuxDir.exists()) wollmuxDir.mkdirs();

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
      catch (FileNotFoundException e)
      {
      }
    }

    try
    {
      // Logger initialisieren:
      if (wollmuxLogFile != null) Logger.init(wollmuxLogFile, Logger.LOG);

      // Parsen der Konfigurationsdatei
      wollmuxConf = new ConfigThingy("wollmuxConf", wollmuxConfFile.toURL());

      // Auswertung von LOGGING_MODE und erste debug-Meldungen loggen:
      setLoggingMode(wollmuxConf);
      Logger.debug("StartupWollMux");
      Logger.debug("Build-Info: " + getBuildInfo());
      Logger.debug("wollmuxConfFile = " + wollmuxConfFile.toString());

      // VisibleTextFragmentList erzeugen
      textFragmentList = new VisibleTextFragmentList(wollmuxConf);

      // DatasourceJoiner erzeugen
      ConfigThingy ssource = wollmuxConf.query("SENDER_SOURCE");
      String ssourceStr;
      try
      {
        ssourceStr = ssource.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new ConfigurationErrorException(
            "Keine Hauptdatenquelle (SENDER_SOURCE) definiert.");
      }
      datasourceJoiner = new DatasourceJoiner(wollmuxConf, ssourceStr,
          losCacheFile, getDEFAULT_CONTEXT());

      // register global EventListener
      UnoService eventBroadcaster = UnoService.createWithContext(
          "com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster()
          .addEventListener(getEventProcessor());

      // Event ON_FIRST_INITIALIZE erzeugen:
      getEventProcessor().addEvent(new Event(Event.ON_INITIALIZE));
    }
    catch (java.lang.Exception e)
    {
      Logger.error("WollMux konnte nicht gestartet werden:", e);
    }
  }

  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert den WollMux mit den festen Standardwerten:
   * wollMuxDir=$HOME/.wollmux, wollMuxLog=wollmux.log, wollMuxConf=wollmux.conf
   */
  public static void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
      singletonInstance = new WollMuxSingleton(ctx);
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zurück. Der Build-Status wird während dem
   * Build-Prozess mit dem Kommando "svn info" auf das Projektverzeichnis
   * erstellt. Die Buildinfo-Datei buildinfo enthält die Paketnummer und die
   * svn-Revision und ist im WollMux.uno.pkg-Paket sowie in der
   * WollMux.uno.jar-Datei abgelegt.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende
   * Ersatzmeldung erzeugt (siehe Sourcecode).
   * 
   * @param insertChars
   *          Einen String, der zur möglichen Einrückung der einzelnen Zeilen
   *          der buildinfo-Datei vor jede Zeile gehängt wird. Soll nicht
   *          eingerückt werden ist der String der Leerstring "".
   * @return Der Build-Status der aktuellen WollMux-Installation.
   * @return
   */
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxSingleton.class.getClassLoader()
          .getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in = new BufferedReader(new InputStreamReader(url
            .openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {
    }
    return "Die Datei buildinfo konnte nicht gelesen werden.";
  }

  /**
   * Wertet die undokumentierte wollmux.conf-Direktive LOGGING_MODE aus und
   * setzt den Logging-Modus entsprechend.
   * 
   * @param ct
   */
  private void setLoggingMode(ConfigThingy ct)
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
   * @return Returns the textFragmentList.
   */
  public VisibleTextFragmentList getTextFragmentList()
  {
    return textFragmentList;
  }

  /**
   * @return Returns the wollmuxConf.
   */
  public ConfigThingy getWollmuxConf()
  {
    return wollmuxConf;
  }

  /**
   * @return Returns the xComponentContext.
   */
  public XComponentContext getXComponentContext()
  {
    return ctx;
  }

  /**
   * Diese Methode liefert den letzten in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT zurück. Ist in der Konfigurationsdatei keine URL definiert,
   * so wird die URL "file:/" zurückgeliefert und eine Fehlermeldung in die
   * Loggdatei geschrieben.
   * 
   * @return der letzte in der Konfigurationsdatei definierte DEFAULT_CONTEXT.
   * @throws ConfigurationErrorException
   */
  public URL getDEFAULT_CONTEXT() throws ConfigurationErrorException
  {
    if (defaultContextURL == null)
    {
      ConfigThingy dc = wollmuxConf.query("DEFAULT_CONTEXT");
      String urlStr;
      try
      {
        urlStr = dc.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        urlStr = "file:";
        Logger.log("Kein DEFAULT_CONTEXT definiert. Verwende \""
                   + urlStr
                   + "\"");
      }

      // url mit einem "/" aufhören lassen (falls noch nicht angegeben).
      String urlVerzStr = (urlStr.endsWith("/")) ? urlStr : urlStr + "/";

      // URL aus urlVerzStr erzeugen
      try
      {
        defaultContextURL = new URL(urlVerzStr);
      }
      catch (MalformedURLException e)
      {
        try
        {
          defaultContextURL = new URL("file:");
        }
        catch (MalformedURLException x)
        {
        }
        Logger.log("Fehlerhafter DEFAULT_CONTEXT \""
                   + urlStr
                   + "\". Verwende \""
                   + defaultContextURL.toString()
                   + "\"");
      }
    }
    return defaultContextURL;
  }

  /**
   * @return Returns the datasourceJoiner.
   */
  public DatasourceJoiner getDatasourceJoiner()
  {
    return datasourceJoiner;
  }

  /**
   * Diese Methode registriert eine XSenderBox, die updates empfängt wenn sich
   * die PAL ändert. Die selbe XSenderBox kann mehrmals registriert werden.
   * 
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMux::addPALChangeEventListener()");
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese methode deregistriert eine XSenderBox. Ist die XSenderBox mehrfach
   * registriert, so wird nur das erste registrierte Element entfernt.
   * 
   * @param senderBox
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMux::removePALChangeEventListener()");
    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      if (((XPALChangeEventListener) i.next()) == listener)
      {
        i.remove();
        break;
      }
    }
  }

  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück.
   * 
   * @return das File-Objekt des LocalOverrideStorage Caches.
   */
  public File getLosCacheFile()
  {
    return losCacheFile;
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   * 
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  public Iterator palChangeListenerIterator()
  {
    return registeredPALChangeListener.iterator();
  }

  public EventProcessor getEventProcessor()
  {
    return EventProcessor.getInstance();
  }

  public String[] getPALEntries()
  {
    DJDatasetListElement[] pal = getSortedPALEntries();
    String[] elements = new String[pal.length];
    for (int i = 0; i < pal.length; i++)
    {
      elements[i] = pal[i].toString();
    }
    return elements;
  }

  private DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = getDatasourceJoiner().getLOS();

    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] = new DJDatasetListElement((DJDataset) iter.next());
    Arrays.sort(elements);

    return elements;
  }

  public String getCurrentSender()
  {
    try
    {
      DJDataset selected = getDatasourceJoiner().getSelectedDataset();
      return new DJDatasetListElement(selected).toString();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }

  public void setCurrentSender(String sender, short idx)
  {
    DJDatasetListElement[] pal = getSortedPALEntries();

    if (idx >= 0 && idx < pal.length)
    {
      if (pal[idx].toString().equals(sender))
      {
        getEventProcessor().addEvent(
            new Event(Event.ON_SET_SENDER, null, pal[idx].getDataset()));
      }
    }
    // TODO: was machen wenn's nicht klappt?
  }

}
