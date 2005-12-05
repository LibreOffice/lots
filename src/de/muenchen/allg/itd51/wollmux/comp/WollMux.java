//FIXME Bei Uninstall des WollMux Paketes geht das LHM-Vorlagen Menü nicht weg.
//TODO Ich halte es nicht für klug, die Versionsnummer in den Package-Namen reinzukodieren
//(gemeint ist das UNO-Package WollMux.uno.pkg, nicht das .deb Package. Letzteres muss
//natürlich die Versionsnummer im Namen enthalten) .
//Dies führt nur dazu, dass mehrere WollMux-Packages parallel installiert werden. Ich weiss
//auch nicht ob die Software-Verteilung damit umgehen kann.
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

package de.muenchen.allg.itd51.wollmux.comp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.beans.NamedValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XAsyncJob;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Event;
import de.muenchen.allg.itd51.wollmux.EventProcessor;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.XSenderBox;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service dient
 * als Einstiegspunkt des WollMux und initialisiert alle benötigten
 * Programmmodule. Sämtliche Felder und öffentliche Methoden des Services sind
 * static und ermöglichen den Zugriff aus anderen Programmmodulen.
 */
public class WollMux extends WeakBase implements XServiceInfo, XAsyncJob,
    XDispatch, XDispatchProvider
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
   * Enthält den geparsten Konfigruationsbaum der wollmux.conf
   */
  private static ConfigThingy wollmuxConf;

  /**
   * Enthält die geparste Textfragmentliste, die in der wollmux.conf definiert
   * wurde.
   */
  private static VisibleTextFragmentList textFragmentList;

  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;

  /**
   * Der XComponentContext in dem der WollMux läuft.
   */
  private static XComponentContext xComponentContext;

  /**
   * Die URL des Wertes DEFAULT_CONTEXT aus der Konfigurationsdatei.
   * defaultContext überschreibt den in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT.
   */
  private static URL defaultContext;

  /**
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private static Vector registeredSenderBoxes;

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  public static final java.lang.String[] SERVICENAMES = { "com.sun.star.task.AsyncJob" };

  /**
   * Dieses Feld enthält den Namen des Protokolls der WollMux-Kommando-URLs
   */
  public static final String wollmuxProtocol = "wollmux";

  /*
   * Hier kommt die Definition der Befehlsnamen, die über WollMux-Kommando-URLs
   * abgesetzt werden können.
   */

  public static final String cmdAbsenderAuswaehlen = "AbsenderAuswaehlen";

  public static final String cmdOpenTemplate = "OpenTemplate";

  public static final String cmdSenderBox = "SenderBox";

  private static final String cmdMenu = "Menu";

  /**
   * Inhalt der wollmux.conf-Datei, die angelegt wird, wenn noch keine
   * wollmux.conf-Datei vorhanden ist. Ist defaultWollmuxConf==null, so wird gar
   * keine wollmux.conf-Datei angelegt.
   */
  private static final String defaultWollmuxConf = "%include \"http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/wollmux.conf\"\r\n";

  /**
   * Der Konstruktor erzeugt einen neues WollMux-Service im XComponentContext
   * context. Wurde der WollMux bereits in einem anderen Kontext erzeugt, so
   * wird eine RuntimeException geworfen.
   * 
   * @param context
   */
  public WollMux(XComponentContext context)
  {
    // Context sichern, Ausführung in anderem Kontext verhindern.
    if (xComponentContext == null)
    {
      xComponentContext = context;
      registeredSenderBoxes = new Vector();
    }
    else if (!UnoRuntime.areSame(context, xComponentContext))
      throw new RuntimeException(
          "WollMux kann nur in einem Kontext erzeugt werden.");
  }

  /**
   * Diese Methode initialisiert den WollMux mit den festen Standardwerten:
   * wollMuxDir=$HOME/.wollmux, wollMuxLog=wollmux.log, wollMuxConf=wollmux.conf
   */
  public static void initialize()
  {
    // Das hier sollte die einzige Stelle sein, an der Pfade hart
    // verdrahtet sind...
    String userHome = System.getProperty("user.home");
    File wollmuxDir = new File(userHome, ".wollmux");
    wollmuxConfFile = new File(wollmuxDir, "wollmux.conf");
    losCacheFile = new File(wollmuxDir, "cache.conf");
    wollmuxLogFile = new File(wollmuxDir, "wollmux.log");

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
  }

  /**
   * Über dies Methode können die wichtigsten Startwerte des WollMux manuell
   * gesetzt werden. Dies ist vor allem zum Testen der Anwendung im
   * Remote-Betrieb notwendig.
   * 
   * @param logStream
   *          Den Ausgabestream, in den die Logging Nachrichten geschrieben
   *          werden sollen.
   * @param wollmuxConf
   *          die Wollmux-Konfigruationsdatei.
   * @param defaultContext
   *          die URL des Wertes DEFAULT_CONTEXT aus der Konfigurationsdatei.
   *          defaultContext überschreibt den in der Konfigurationsdatei
   *          definierten DEFAULT_CONTEXT.
   */
  public static void initialize(File wollmuxConf, File losCache,
      URL defaultContext)
  {
    WollMux.wollmuxConfFile = wollmuxConf;
    WollMux.losCacheFile = losCache;
    WollMux.defaultContext = defaultContext;
  }

  /**
   * Diese Methode übernimmt den eigentlichen Bootstrap des WollMux.
   */
  public static void startupWollMux()
  {
    try
    {
      // Logger initialisieren:
      if (wollmuxLogFile != null) try
      {
        Logger.init(wollmuxLogFile, Logger.LOG);
      }
      catch (FileNotFoundException x)
      {
        // dann gibts halt kein logging - pech gehabt.
      }

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
      ConfigThingy ssource = wollmuxConf.query("SENDER_SOURCE").getLastChild();
      datasourceJoiner = new DatasourceJoiner(wollmuxConf, ssource.toString(),
          losCacheFile, getDEFAULT_CONTEXT());

      // register global EventListener
      UnoService eventBroadcaster = UnoService.createWithContext(
          "com.sun.star.frame.GlobalEventBroadcaster",
          WollMux.getXComponentContext());
      eventBroadcaster.xEventBroadcaster().addEventListener(
          EventProcessor.create());

      // Event ON_FIRST_INITIALIZE erzeugen:
      EventProcessor.create().addEvent(new Event(Event.ON_INITIALIZE));
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
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
  public static String getBuildInfo()
  {
    try
    {
      URL url = WollMux.class.getClassLoader().getResource("buildinfo");
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
   * @return Returns the textFragmentList.
   */
  public static VisibleTextFragmentList getTextFragmentList()
  {
    return textFragmentList;
  }

  /**
   * @return Returns the wollmuxConf.
   */
  public static ConfigThingy getWollmuxConf()
  {
    return wollmuxConf;
  }

  /**
   * @return Returns the xComponentContext.
   */
  public static XComponentContext getXComponentContext()
  {
    return xComponentContext;
  }

  /**
   * Diese Methode liefert den letzten in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT zurück. Ist in der Konfigurationsdatei keine URL definiert,
   * so wird die URL "file:/" zurückgeliefert und eine Fehlermeldung in die
   * Loggdatei geschrieben.
   * 
   * @return der letzte in der Konfigurationsdatei definierte DEFAULT_CONTEXT.
   */
  public static URL getDEFAULT_CONTEXT()
  {
    if (defaultContext == null)
    {
      try
      {
        ConfigThingy dc = wollmuxConf.query("DEFAULT_CONTEXT").getLastChild();
        String urlStr = dc.toString();
        // url mit einem "/" aufhören lassen (falls noch nicht angegeben).
        if (urlStr.endsWith("/"))
          defaultContext = new URL(dc.toString());
        else
          defaultContext = new URL(dc.toString() + "/");
      }
      catch (Exception e)
      {
        Logger.error(e);
        try
        {
          defaultContext = new URL("file:/");
        }
        catch (MalformedURLException x)
        {
          // kommt nicht vor, da obige file url korrekt!
          Logger.error(x);
        }
      }
    }
    return defaultContext;
  }

  /**
   * Der AsyncJob wird mit dem Event OnFirstVisibleTask gestartet und besitzt
   * nur die Aufgabe, den WollMux über die Methode startupWollMux() zu starten.
   * 
   * @see com.sun.star.task.XAsyncJob#executeAsync(com.sun.star.beans.NamedValue[],
   *      com.sun.star.task.XJobListener)
   */
  public synchronized void executeAsync(com.sun.star.beans.NamedValue[] lArgs,
      com.sun.star.task.XJobListener xListener)
      throws com.sun.star.lang.IllegalArgumentException
  {
    if (xListener == null)
      throw new com.sun.star.lang.IllegalArgumentException("invalid listener");

    com.sun.star.beans.NamedValue[] lEnvironment = null;

    // Hole das Environment-Argument
    for (int i = 0; i < lArgs.length; ++i)
    {
      if (lArgs[i].Name.equals("Environment"))
      {
        lEnvironment = (com.sun.star.beans.NamedValue[]) com.sun.star.uno.AnyConverter
            .toArray(lArgs[i].Value);
      }
    }
    if (lEnvironment == null)
      throw new com.sun.star.lang.IllegalArgumentException("no environment");

    // Hole Event-Informationen
    String sEnvType = null;
    String sEventName = null;
    for (int i = 0; i < lEnvironment.length; ++i)
    {
      if (lEnvironment[i].Name.equals("EnvType"))
        sEnvType = com.sun.star.uno.AnyConverter
            .toString(lEnvironment[i].Value);
      else if (lEnvironment[i].Name.equals("EventName"))
        sEventName = com.sun.star.uno.AnyConverter
            .toString(lEnvironment[i].Value);
    }

    // Prüfe die property "EnvType":
    if ((sEnvType == null)
        || ((!sEnvType.equals("EXECUTOR")) && (!sEnvType.equals("DISPATCH"))))
    {
      java.lang.String sMessage = "\""
                                  + sEnvType
                                  + "\" isn't a valid value for EnvType";
      throw new com.sun.star.lang.IllegalArgumentException(sMessage);
    }

    /***************************************************************************
     * Starte den WollMux!
     */
    if (sEventName.equals("onFirstVisibleTask"))
    {
      initialize();
      startupWollMux();
    }
    /** *************************************************** */

    xListener.jobFinished(this, new NamedValue[] {});
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  public String[] getSupportedServiceNames()
  {
    return SERVICENAMES;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
  public boolean supportsService(String sService)
  {
    int len = SERVICENAMES.length;
    for (int i = 0; i < len; i++)
    {
      if (sService.equals(SERVICENAMES[i])) return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getImplementationName()
   */
  public String getImplementationName()
  {
    return (WollMux.class.getName());
  }

  /**
   * Diese Methode liefert eine Factory zurück, die in der Lage ist den
   * UNO-Service zu erzeugen. Die Methode wird von UNO intern benötigt. Die
   * Methoden __getComponentFactory und __writeRegistryServiceInfo stellen das
   * Herzstück des UNO-Service dar.
   * 
   * @param sImplName
   * @return
   */
  public synchronized static XSingleComponentFactory __getComponentFactory(
      java.lang.String sImplName)
  {
    com.sun.star.lang.XSingleComponentFactory xFactory = null;
    if (sImplName.equals(WollMux.class.getName()))
      xFactory = Factory.createComponentFactory(WollMux.class, SERVICENAMES);
    return xFactory;
  }

  /**
   * Diese Methode registriert den UNO-Service. Sie wird z.B. beim unopkg-add im
   * Hintergrund aufgerufen. Die Methoden __getComponentFactory und
   * __writeRegistryServiceInfo stellen das Herzstück des UNO-Service dar.
   * 
   * @param xRegKey
   * @return
   */
  public synchronized static boolean __writeRegistryServiceInfo(
      XRegistryKey xRegKey)
  {
    return Factory.writeRegistryServiceInfo(
        WollMux.class.getName(),
        WollMux.SERVICENAMES,
        xRegKey);
  }

  /**
   * @return Returns the datasourceJoiner.
   */
  public static DatasourceJoiner getDatasourceJoiner()
  {
    return datasourceJoiner;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   *      java.lang.String, int)
   */
  public XDispatch queryDispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */String sTargetFrameName,
  /* IN */int iSearchFlags)
  {
    XDispatch xRet = null;
    try
    {
      URI uri = new URI(aURL.Complete);
      Logger.debug2("queryDispatch: " + uri.toString());
      if (uri.getScheme().compareToIgnoreCase(wollmuxProtocol) == 0)
      {
        if (uri.getSchemeSpecificPart().compareToIgnoreCase(
            cmdAbsenderAuswaehlen) == 0) xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdOpenTemplate) == 0)
          xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdSenderBox) == 0)
          xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdMenu) == 0)
          xRet = this;
      }
    }
    catch (URISyntaxException e)
    {
      Logger.error(e);
    }
    return xRet;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
   */
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    int nCount = seqDescripts.length;
    XDispatch[] lDispatcher = new XDispatch[nCount];

    for (int i = 0; i < nCount; ++i)
      lDispatcher[i] = queryDispatch(
          seqDescripts[i].FeatureURL,
          seqDescripts[i].FrameName,
          seqDescripts[i].SearchFlags);

    return lDispatcher;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
  public void addStatusListener(XStatusListener arg0, com.sun.star.util.URL arg1)
  {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
   *      com.sun.star.beans.PropertyValue[])
   */
  public void dispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */com.sun.star.beans.PropertyValue[] aArguments)
  {
    try
    {
      URI uri = new URI(aURL.Complete);

      if (uri.getScheme().compareToIgnoreCase(wollmuxProtocol) == 0)
      {
        if (uri.getSchemeSpecificPart().compareToIgnoreCase(
            cmdAbsenderAuswaehlen) == 0)
        {
          Logger
              .debug2("Dispatch: Aufruf von WollMux:AbsenderdatenBearbeitenDialog");
          EventProcessor.create().addEvent(
              new Event(Event.ON_ABSENDER_AUSWAEHLEN));
        }

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdOpenTemplate) == 0)
        {
          Logger.debug2("Dispatch: Aufruf von WollMux:OpenFrag mit Frag:"
                        + uri.getFragment());
          EventProcessor.create().addEvent(
              new Event(Event.ON_OPENTEMPLATE, uri.getFragment()));
        }

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdMenu) == 0)
        {
          Logger.debug2("Dispatch: Aufruf von WollMux:menu mit menu:"
                        + uri.getFragment());
        }
      }
    }
    catch (URISyntaxException e)
    {
      Logger.error(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
  public void removeStatusListener(XStatusListener arg0,
      com.sun.star.util.URL arg1)
  {
  }

  /**
   * main-Methode zum Testen des WollMux über einen Remote-Context.
   */
  public static void main(String[] args)
  {
    try
    {
      if (args.length < 2)
      {
        System.out.println("USAGE: <config_url> <losCache>");
        System.exit(0);
      }
      File cwd = new File("testdata");

      args[0] = args[0].replaceAll("\\\\", "/");

      // Remote-Context herstellen
      UNO.init();

      // WollMux starten
      new WollMux(UNO.defaultContext);
      WollMux.initialize(new File(cwd, args[0]), new File(cwd, args[1]), cwd
          .toURL());
      WollMux.startupWollMux();
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode registriert eine XSenderBox, die updates empfängt wenn sich
   * die PAL ändert. Die selbe XSenderBox kann mehrmals registriert werden.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XWollMux#addSenderBox(de.muenchen.allg.itd51.wollmux.XSenderBox)
   */
  public static void registerSenderBox(XSenderBox senderBox)
  {
    registeredSenderBoxes.add(senderBox);
    Logger.debug2("WollMux::added senderBox.");
  }

  /**
   * Diese methode deregistriert eine XSenderBox. Ist die XSenderBox mehrfach
   * registriert, so wird nur das erste registrierte Element entfernt.
   * 
   * @param senderBox
   */
  public static void deregisterSenderBox(XSenderBox senderBox)
  {
    Iterator i = registeredSenderBoxes.iterator();
    while (i.hasNext())
    {
      if (((XSenderBox) i.next()) == senderBox)
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
  public static File getLosCacheFile()
  {
    return losCacheFile;
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   * 
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  public static Iterator senderBoxesIterator()
  {
    return registeredSenderBoxes.iterator();
  }

}
