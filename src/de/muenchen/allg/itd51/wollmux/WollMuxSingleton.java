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
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | LUT | Überarbeitung Code-Kommentare  
 * 20.04.2006 | BNK | DEFAULT_CONTEXT ausgegliedert nach WollMuxFiles
 * 21.04.2006 | LUT | + Robusteres Verhalten bei Fehlern während dem Einlesen 
 *                    von Konfigurationsdateien; 
 *                    + wohldefinierte Datenstrukturen
 *                    + Flag für EventProcessor: acceptEvents
 * 08.05.2006 | LUT | + isDebugMode()
 * 10.05.2006 | BNK | +parseGlobalFunctions()
 *                  | +parseFunctionDialogs()
 * 26.05.2006 | BNK | DJ initialisierung ausgelagert nacht WollMuxFiles
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

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
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten
   * Funktionen.
   */
  private FunctionLibrary globalFunctions;

  /**
   * Enthält die im Funktionsdialoge-Abschnitt der wollmux,conf definierten
   * Dialoge.
   */
  private DialogLibrary funcDialogs;

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
   * Enthält die Menge aller aktuell geöffneter TextDocuments in Form von
   * TextDocumentModel-Elementen. Die HashMap enthält eine Zuordnung der
   * TextDocumentModels auf sich selbst, damit die ursprüngliche Instanz des
   * TextDocumentModels über get() wieder hervorgeholt werden kann.
   */
  private HashMap currentTextDocumentModels;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zurückgeliefert.
    this.ctx = ctx;

    this.currentTextDocumentModels = new HashMap();

    // Initialisiere die UNO-Klasse, so dass auch mit dieser Hilfsklasse
    // gearbeitet werden kann.
    try
    {
      UNO.init(ctx.getServiceManager());
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    boolean successfulStartup = true;

    registeredPALChangeListener = new Vector();

    WollMuxFiles.setupWollMuxDir();

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

    // Versuchen, den DJ zu initialisieren und Flag setzen, falls nicht
    // erfolgreich.
    if (getDatasourceJoiner() == null) successfulStartup = false;

    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions()
     * erfolgen. Als context wird null übergeben, weil globale Funktionen keinen
     * Kontext haben. TODO Überlegen, ob ein globaler Kontext doch Sinn machen
     * könnte. Dadurch könnten globale Funktionen globale Funktionsdialoge
     * darstellen, die global einheitliche Werte haben.
     */
    funcDialogs = WollMuxFiles.parseFunctionDialogs(WollMuxFiles
        .getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * müssen also vorher geparst sein. Als context wird null übergeben, weil
     * globale Funktionen keinen Kontext haben.
     */
    globalFunctions = WollMuxFiles.parseFunctions(
        WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(),
        null,
        null);

    // Initialisiere EventProcessor
    EventProcessor.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster = UnoService.createWithContext(
          "com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster().addEventListener(
          new GlobalEventListener());
    }
    catch (Exception e)
    {
      Logger.error(e);
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
    {
      singletonInstance = new WollMuxSingleton(ctx);

      // Event ON_FIRST_INITIALIZE erzeugen:
      WollMuxEventHandler.handleInitialize();
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
    return WollMuxFiles.getDatasourceJoiner();
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

    if (listener == null) return;

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
   * registriert war.
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

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen
   * enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Dialogbibliothek, die die Dialoge enthält, die in Funktionen
   * (Grundfunktion "DIALOG") verwendung finden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

  /**
   * siehe {@link WollMuxFiles#isDebugMode()}.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public boolean isDebugMode()
  {
    return WollMuxFiles.isDebugMode();
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum übergebenen XTextDocument doc;
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt
   * und das neu erzeugte zurück geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugehörige TextDocumentModel
   *          zurückgeliefert werden soll.
   * @return Das zu doc zugehörige TextDocumentModel.
   */
  public TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    TextDocumentModel refModel = new TextDocumentModel(doc);
    TextDocumentModel curModel = (TextDocumentModel) currentTextDocumentModels
        .get(refModel);
    if (curModel == null)
    {
      curModel = refModel;

      currentTextDocumentModels.put(curModel, curModel);

      curModel.registerCloseListener();
    }
    return curModel;
  }

  /**
   * Löscht das übergebene TextDocumentModel aus der internen Liste aller
   * aktuellen TextDocumentModels.
   * 
   * @param model
   *          Das TextDocumentModel, das aus der internen Liste gelöscht werden
   *          soll.
   */
  public void disposedTextDocumentModel(TextDocumentModel model)
  {
    currentTextDocumentModels.remove(model);
  }

  /**
   * Überprüft, ob von url gelesen werden kann und wirft eine IOException, falls
   * nicht.
   * 
   * @throws IOException
   *           falls von url nicht gelesen werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void checkURL(URL url) throws IOException
  {
    url.openStream().close();
  }

  public static class GlobalEventListener implements
      com.sun.star.document.XEventListener
  {
    /**
     * Wird vom GlobalEventBroadcaster aufgerufen.
     * 
     * @see com.sun.star.document.XEventListener#notifyEvent(com.sun.star.document.EventObject)
     */
    public void notifyEvent(com.sun.star.document.EventObject docEvent)
    {
      int code = 0;
      try
      {
        code = docEvent.Source.hashCode();
      }
      catch (java.lang.Exception x)
      {
      }
      Logger.debug2("Incoming documentEvent for #"
                    + code
                    + ": "
                    + docEvent.EventName);
      UnoService source = new UnoService(docEvent.Source);

      // Bekannte Event-Typen rausziehen:
      if (source.xTextDocument() != null)
      {
        // Beim OnLoadFinished wird die Bearbeitung des Dokuments gestartet.
        // Dort ist das Dokument jedoch noch nicht mit einem Frame verknüpft.
        if (docEvent.EventName.equalsIgnoreCase("OnLoadFinished"))
        {
          WollMuxEventHandler.handleProcessTextDocument(source.xTextDocument());
        }

        // Ab OnLoad oder OnNew steht nun auch der Frame zur Verfügung und der
        // WollMuxDispatchInterceptor kann eingebunden werden.
        else if (docEvent.EventName.equalsIgnoreCase("OnLoad")
                 || docEvent.EventName.equalsIgnoreCase(("OnNew")))
        {
          XFrame frame = getFrame(source);
          if (UNO.XDispatchProviderInterception(frame) != null)
          {
            Logger.debug("register WollMuxDispatchInterceptor for frame #"
                         + frame.hashCode());
            UNO.XDispatchProviderInterception(frame)
                .registerDispatchProviderInterceptor(
                    new WollMuxDispatchInterceptor());
          }
        }
      }
    }

    private XFrame getFrame(UnoService source)
    {
      XFrame frame = null;
      try
      {
        frame = source.xModel().getCurrentController().getFrame();
      }
      catch (java.lang.Exception e)
      {
      }
      return frame;
    }

    public void disposing(EventObject arg0)
    {
      // nothing to do
    }
  }

  /**
   * Diese Klasse ermöglicht es dem WollMux, dispatch-Kommandos abzufangen und
   * statt dessen eigene Aktionen durchzuführen. Jeder Frame
   * 
   * @author christoph.lutz
   * 
   */
  public static class WollMuxDispatchInterceptor implements
      XDispatchProviderInterceptor
  {

    XDispatchProvider slave = null;

    XDispatchProvider master = null;

    public XDispatchProvider getSlaveDispatchProvider()
    {
      return slave;
    }

    public void setSlaveDispatchProvider(XDispatchProvider slave)
    {
      this.slave = slave;
    }

    public XDispatchProvider getMasterDispatchProvider()
    {
      return master;
    }

    public void setMasterDispatchProvider(XDispatchProvider master)
    {
      this.master = master;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
     *      java.lang.String, int)
     */
    public XDispatch queryDispatch(com.sun.star.util.URL url, String frame,
        int frameSearchFlags)
    {
      String urlStr = url.Complete;

      // Logger.debug2("queryDispatch: '" + urlStr + "'");

      final XDispatch origDisp = slave.queryDispatch(
          url,
          frame,
          frameSearchFlags);

      if (urlStr.equals(".uno:Print"))
      {
        Logger.debug("queryDispatch: '" + urlStr + "'");
        return new XDispatch()
        {
          public void dispatch(com.sun.star.util.URL arg0, PropertyValue[] arg1)
          {
            XTextDocument doc = UNO.XTextDocument(UNO.desktop
                .getCurrentComponent());
            if (doc != null)
              WollMuxEventHandler.handlePrintButtonPressed(
                  doc,
                  origDisp,
                  arg0,
                  arg1);
          }

          public void removeStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            if (origDisp != null) origDisp.removeStatusListener(arg0, arg1);
          }

          public void addStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            if (origDisp != null) origDisp.addStatusListener(arg0, arg1);
          }
        };
      }

      // Für Debug-Zwecke kann in der folgenden Zeile der ForwardDispatcher
      // eingeschalten werden, der jede Dispatch-Anfrage durchreicht, dabei aber
      // noch log-Meldungen produziert. ACHTUNG: hier darf (leider) nicht
      // WollMuxFiles.isDebugMode() aufgerufen werden, da die Methode nicht im
      // WollMuxEventHandler-Thread läuft.
      if (false) return new ForwardDispatch(origDisp);

      // Anfrage an das ursprüngliche DispatchObjekt weiterleiten.
      return origDisp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
     */
    public XDispatch[] queryDispatches(DispatchDescriptor[] seqDescripts)
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
  }

  /**
   * Der ForwardDispatch ist ein Dispatch-Handler für Testzwecke, der jede
   * Dispatch-Anfrag auf den Logger protokolliert, die Anfragen aber ansonsten
   * unverändert an den ursprünglichen dispatch-Handler weiterreicht.
   * 
   * @author christoph.lutz
   * 
   */
  private static class ForwardDispatch implements XDispatch
  {
    private XDispatch orig;

    public ForwardDispatch(XDispatch orig)
    {
      this.orig = orig;
    }

    public void dispatch(com.sun.star.util.URL arg0, PropertyValue[] arg1)
    {
      Logger.debug2(ForwardDispatch.class.getName()
                    + ".dispatch('"
                    + arg0.Complete
                    + "')");
      orig.dispatch(arg0, arg1);
    }

    public void addStatusListener(XStatusListener arg0,
        com.sun.star.util.URL arg1)
    {
      Logger.debug2(ForwardDispatch.class.getName()
                    + ".addStatusListener('"
                    + arg0.hashCode()
                    + "')");
      orig.addStatusListener(arg0, arg1);
    }

    public void removeStatusListener(XStatusListener arg0,
        com.sun.star.util.URL arg1)
    {
      Logger.debug2(ForwardDispatch.class.getName()
                    + ".removeStatusListener('"
                    + arg0.hashCode()
                    + "')");
      orig.removeStatusListener(arg0, arg1);
    }
  }

}
