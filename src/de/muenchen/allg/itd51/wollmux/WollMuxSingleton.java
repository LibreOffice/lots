/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton für zentrale WollMux-Methoden.
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
 * 05.12.2005 | BNK | line.separator statt \n     
 <<<<<<< .mine
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | LUT | Überarbeitung Code-Kommentare  
 =======
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | BNK | DEFAULT_CONTEXT ausgegliedert nach WollMuxFiles  
 >>>>>>> .r773
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.uno.UnoRuntime;
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
 * Diese Klasse ist ein Singleton, welcher den WollMux initialisiert und alle
 * zentralen WollMux-Methoden zur Verfügung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der früher zentraler Anlaufpunkt
 * war, bedient sich größtenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton implements XPALProvider
{

  private static WollMuxSingleton singletonInstance = null;

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
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private Vector registeredPALChangeListener;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    registeredPALChangeListener = new Vector();
    this.ctx = ctx;

    WollMuxFiles.setupWollMuxDir();

    try
    {
      Logger.debug("StartupWollMux");
      Logger.debug("Build-Info: " + getBuildInfo());
      Logger.debug("wollmuxConfFile = "
                   + WollMuxFiles.getWollMuxConfFile().toString());
      Logger.debug("DEFAULT_CONTEXT \""
                   + WollMuxFiles.getDEFAULT_CONTEXT().toString()
                   + "\"");

      // VisibleTextFragmentList erzeugen
      textFragmentList = new VisibleTextFragmentList(WollMuxFiles
          .getWollmuxConf());

      // DatasourceJoiner erzeugen
      ConfigThingy ssource = WollMuxFiles.getWollmuxConf().query(
          "SENDER_SOURCE");
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
      datasourceJoiner = new DatasourceJoiner(WollMuxFiles.getWollmuxConf(),
          ssourceStr, WollMuxFiles.getLosCacheFile(), WollMuxFiles
              .getDEFAULT_CONTEXT());

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

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux
   * noch nicht initialisiert, so liefert die Methode null!
   * 
   * @return Instanz des WollMuxSingletons oder null.
   */
  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch
   * nicht initialisiert wurde)
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
   * @return Der Build-Status der aktuellen WollMux-Installation.
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
   * @return Returns the textFragmentList.
   */
  public VisibleTextFragmentList getTextFragmentList()
  {
    return textFragmentList;
  }

  /**
   * @return Returns the xComponentContext.
   */
  public XComponentContext getXComponentContext()
  {
    return ctx;
  }

  /**
   * Diese Methode liefert eine Instanz auf den aktuellen DatasourceJoiner
   * zurück.
   * 
   * @return Returns the datasourceJoiner.
   */
  public DatasourceJoiner getDatasourceJoiner()
  {
    return datasourceJoiner;
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates
   * empfängt wenn sich die PAL ändert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addPALChangeEventListener()");
    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      Object l = i.next();
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war. Nach dem Deregistrieren des letzten
   * XPALChangeEventListener wird der Desktop (und damit die aktuelle
   * OpenOffice.org-Instanz) geschlossen, wenn keine weitere
   * OpenOffice.org-Komponente geöffnet ist. Ein evtl. vorhandener
   * Schnellstarter wird jedoch nicht beendet.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss über den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removePALChangeEventListener()");
    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      Object l = i.next();
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
    if (registeredPALChangeListener.size() == 0)
    {
      // Versuche den desktop zu schließen wenn kein Eintrag mehr da ist
      // und der Desktop auch sonst keine Elemente enthält:
      getEventProcessor().addEvent(new Event(Event.ON_TRY_TO_CLOSE_OOO));
    }
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

  /**
   * Diese Methode liefert eine Instanz auf den EventProcessor, der u.A. dazu
   * benötigt wird, neue Events in die Eventqueue einzuhängen.
   * 
   * @return die Instanz des aktuellen EventProcessors
   */
  public EventProcessor getEventProcessor()
  {
    return EventProcessor.getInstance();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste aller
   * Einträge der Persönlichen Absenderliste (PAL) in einem String-Array, wobei
   * die einzelnen Einträge in der Form "<Nachname>, <Vorname> (<Rolle>)"
   * sind.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
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

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Persönlichen
   * Absenderliste (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * @return alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
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

  /**
   * Diese Methode liefert den aktuell aus der persönlichen Absenderliste (PAL)
   * ausgewählten Absender im Format "<Nachname>, <Vorname> (<Rolle>)" zurück.
   * Ist die PAL leer oder noch kein Absender ausgewählt, so liefert die Methode
   * den Leerstring "" zurück. Dieser Sonderfall sollte natürlich entsprechend
   * durch die aufrufende Methode behandelt werden.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   * 
   * @return den aktuell aus der PAL ausgewählten Absender als String. Ist kein
   *         Absender ausgewählt wird der Leerstring "" zurückgegeben.
   */
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

  /**
   * siehe {@link WollMuxFiles#getWollmuxConf()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getWollmuxConf()
  {
    return WollMuxFiles.getWollmuxConf();
  }

  /**
   * siehe {@link WollMuxFiles#getDEFAULT_CONTEXT()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public URL getDEFAULT_CONTEXT()
  {
    return WollMuxFiles.getDEFAULT_CONTEXT();
  }
}
