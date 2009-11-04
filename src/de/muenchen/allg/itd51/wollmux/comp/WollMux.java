/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : zentraler UNO-Service WollMux 
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import java.util.HashMap;

import com.sun.star.document.XEventListener;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.XWollMuxDocument;
import de.muenchen.allg.itd51.wollmux.event.DispatchHandler;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service hat
 * folgende Funktionen: Als XDispatchProvider und XDispatch behandelt er alle
 * "wollmux:kommando..." URLs und als XWollMux stellt er die Schnittstelle für
 * externe UNO-Komponenten dar. Der Service wird beim Starten von OpenOffice.org
 * automatisch (mehrfach) instanziiert, wenn OOo einen dispatchprovider für die in
 * der Datei Addons.xcu enthaltenen wollmux:... dispatches besorgen möchte (dies
 * geschieht auch bei unsichtbar geöffneten Dokumenten). Als Folge wird das
 * WollMux-Singleton bei OOo-Start (einmalig) initialisiert.
 */
public class WollMux extends WeakBase implements XServiceInfo, XDispatchProvider,
    XWollMux
{

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  private static final java.lang.String[] SERVICENAMES =
    { "de.muenchen.allg.itd51.wollmux.WollMux" };

  /**
   * Der Konstruktor initialisiert das WollMuxSingleton und startet damit den
   * eigentlichen WollMux. Der Konstuktor wird aufgerufen, bevor OpenOffice.org die
   * Methode executeAsync() aufrufen kann, die bei einem ON_FIRST_VISIBLE_TASK-Event
   * über den Job-Mechanismus ausgeführt wird.
   * 
   * @param context
   */
  public WollMux(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);
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
    return DispatchHandler.globalWollMuxDispatches.queryDispatch(aURL,
      sTargetFrameName, iSearchFlags);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
   */
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    return DispatchHandler.globalWollMuxDispatches.queryDispatches(seqDescripts);
  }

  /**
   * Diese Methode liefert eine Factory zurück, die in der Lage ist den UNO-Service
   * zu erzeugen. Die Methode wird von UNO intern benötigt. Die Methoden
   * __getComponentFactory und __writeRegistryServiceInfo stellen das Herzstück des
   * UNO-Service dar.
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
  public synchronized static boolean __writeRegistryServiceInfo(XRegistryKey xRegKey)
  {
    return Factory.writeRegistryServiceInfo(WollMux.class.getName(),
      WollMux.SERVICENAMES, xRegKey);
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgelöst, welches dafür sort, dass sofort ein
   * erster update aller Listener ausgeführt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  public void addPALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.handleAddPALChangeEventListener(l, null);
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert; nach der Registrierung wird geprüft, ob der WollMux
   * und der XPALChangeEventListener die selbe WollMux-Konfiguration verwenden, wozu
   * der Listener den HashCode wollmuxConfHashCode der aktuellen
   * WollMux-Konfiguration übermittelt. Stimmt wollmuxConfHashCode nicht mit dem
   * HashCode der WollMux-Konfiguration des WollMux überein, so erscheint ein Dialog,
   * der vor möglichen Fehlern warnt. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgelöst, welches dafür sort, dass sofort ein
   * erster update aller Listener ausgeführt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * @param l
   *          Der zu registrierende XPALChangeEventListener
   * @param wollmuxConfHashCode
   *          Der HashCode der WollMux-Config der zur Konsistenzprüfung herangezogen
   *          wird und über
   *          WollMuxFiles.getWollMuxConf().getStringRepresentation().hashCode()
   *          erzeugt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListenerWithConsistencyCheck(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener,
   *      int)
   */
  public void addPALChangeEventListenerWithConsistencyCheck(
      XPALChangeEventListener l, int wollmuxConfHashCode)
  {
    WollMuxEventHandler.handleAddPALChangeEventListener(l,
      Integer.valueOf(wollmuxConfHashCode));
  }

  /**
   * Diese Methode registriert einen Listener im WollMux, über den der WollMux über
   * den Status der Dokumentbearbeitung informiert (z.B. wenn ein Dokument
   * vollständig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Tritt ein entstprechendes Ereignis ein, so erfolgt der Aufruf der entsprechenden
   * Methoden XEventListener.notifyEvent(...) immer gleichzeitig (d.h. für jeden
   * Listener in einem eigenen Thread).
   * 
   * Der WollMux liefert derzeit folgende Events:
   * 
   * OnWollMuxProcessingFinished: Dieses Event wird erzeugt, wenn ein Textdokument
   * nach dem Öffnen vollständig vom WollMux bearbeitet und expandiert wurde oder bei
   * allen anderen Dokumenttypen direkt nach dem Öffnen. D.h. für jedes in OOo
   * geöffnete Dokument erfolgt früher oder später ein solches Event.
   * 
   * @param l
   *          Der XEventListener, der bei Statusänderungen der Dokumentbearbeitung
   *          informiert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#addEventListener(com.sun.star.document.XEventListener)
   */
  public void addEventListener(XEventListener l)
  {
    WollMuxEventHandler.handleAddDocumentEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#removePALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  public void removePALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.handleRemovePALChangeEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen mit registerEventListener(XEventListener l)
   * registrierten XEventListener.
   * 
   * @param l
   *          der XEventListener, der deregistriert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#removeEventListener(com.sun.star.document.XEventListener)
   */
  public void removeEventListener(XEventListener l)
  {
    WollMuxEventHandler.handleRemoveDocumentEventListener(l);
  }

  /**
   * Diese Methode setzt den aktuellen Absender der Persönlichen Absenderliste (PAL)
   * auf den Absender sender. Der Absender wird nur gesetzt, wenn die Parameter
   * sender und idx in der alphabetisch sortierten Absenderliste des WollMux
   * übereinstimmen - d.h. die Absenderliste der veranlassenden SenderBox zum
   * Zeitpunkt der Auswahl konsistent zur PAL des WollMux war. Die Methode verwendet
   * für sender das selben Format wie es vom XPALProvider:getCurrentSender()
   * geliefert wird.
   */
  public void setCurrentSender(String sender, short idx)
  {
    Logger.debug2("WollMux.setCurrentSender(\"" + sender + "\", " + idx + ")");
    WollMuxEventHandler.handleSetSender(sender, idx);
  }

  /**
   * Diese Methode liefert den Wert zur Datenbankspalte dbSpalte, der dem Wert
   * entspricht, den das Dokumentkommando WM(CMD'insertValue' DB_SPALTE'<dbSpalte>')
   * in das Dokument einfügen würde, oder den Leerstring "" wenn dieser Wert nicht
   * bestimmt werden kann (z.B. wenn ein ungültiger Spaltennamen dbSpalte übergeben
   * wurde). So ist es z.B. möglich, aus externen Anwendungen (z.B. Basic-Makros) auf
   * Werte des aktuell gesetzten Absenders des WollMux zuzugreifen.
   * 
   * Anmerkung: Ist die aufrufende Anwendung ein Basic-Makro, so muss damit gerechnet
   * werden, dass dieses Makro bereits in einer synchronisierten Umgebung abläuft
   * (das ist Standard bei Basic-Makros). Um Deadlocks in Zusammenhang mit dem
   * WollMux zu vermeiden, darf die Methode also nicht über den WollMuxEventHandler
   * synchonisiert werden. Aufgrund der fehlenden Synchronisierung auf Seiten des
   * WollMux ist die Methode jedoch mit Vorsicht zu genießen. Insbesondere sollten
   * die über diese Methode gelieferten Werte NICHT verwendet werden, um z.B. mit
   * setCurrentSender() Daten des WollMux zu manipulieren.
   * 
   * Zum Auslesen und Reagieren auf Änderungen der PersoenlichenAbsenderlist (PAL)
   * sollten die Methoden verwendet werden, die der XPALProvider anbietet. Diese
   * Methoden sind über den WollMuxEventHandler synchronisiert.
   * 
   * @param dbSpalte
   *          Name der Datenbankspalte deren Wert zurückgeliefert werden soll.
   * @return Der Wert der Datenbankspalte dbSpalte des aktuell ausgewählten Absenders
   *         oder "", wenn der Wert nicht bestimmt werden kann.
   */
  public String getValue(String dbSpalte)
  {
    try
    {
      String value =
        WollMuxSingleton.getInstance().getDatasourceJoiner().getSelectedDatasetTransformed().get(
          dbSpalte);
      if (value == null) value = "";
      return value;
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
      return "";
    }
  }

  /**
   * Macht das selbe wie XWollMuxDocument wdoc = getWollMuxDocument(doc); if (wdoc !=
   * null) wdoc.addPrintFunction(functionName) und sollte durch diese Anweisungen
   * entsprechend ersetzt werden.
   * 
   * @param doc
   *          Das Dokument, dem die Druckfunktion functionName hinzugefügt werden
   *          soll.
   * @param functionName
   *          der Name einer Druckfunktion, die im Abschnitt "Druckfunktionen" der
   *          WollMux-Konfiguration definiert sein muss.
   * @deprecated since 2009-09-18
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void addPrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null) wdoc.removePrintFunction(functionName);
  }

  /**
   * Macht das selbe wie XWollMuxDocument wdoc = getWollMuxDocument(doc); if (wdoc !=
   * null) wdoc.removePrintFunction(functionName) und sollte durch diese Anweisungen
   * entsprechend ersetzt werden.
   * 
   * @param doc
   *          Das Dokument, dem die Druckfunktion functionName genommen werden soll.
   * @param functionName
   *          der Name einer Druckfunktion, die im Dokument gesetzt ist.
   * @deprecated since 2009-09-18
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void removePrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null) wdoc.removePrintFunction(functionName);
  }

  /**
   * Ermöglicht den Zugriff auf WollMux-Funktionen, die spezifisch für das Dokument
   * doc sind. Derzeit ist als doc nur ein c.s.s.t.TextDocument möglich. Wird ein
   * Dokument übergeben, für das der WollMux keine Funktionen anbietet (derzeit zum
   * Beispiel ein Calc-Dokument), so wird null zurückgeliefert. Dass diese Funktion
   * ein nicht-null Objekt zurückliefert bedeutet jedoch nicht zwangsweise, dass der
   * WollMux für das Dokument sinnvolle Funktionen bereitstellt. Es ist möglich, dass
   * Aufrufe der entsprechenden Funktionen des XWollMuxDocument-Interfaces nichts
   * tun.
   * 
   * Hinweis zur Synchronisation: Aufrufe der Funktionen von XWollMuxDocument können
   * ohne weitere Synchronisation sofort erfolgen. Jedoch ersetzt
   * getWollMuxDocument() keinesfalls die Synchronisation mit dem WollMux.
   * Insbesondere ist es möglich, dass getWollMuxDocument() zurückkehrt BEVOR der
   * WollMux das Dokument doc bearbeitet hat. Vergleiche hierzu die Beschreibung von
   * XWollMuxDocument.
   * 
   * @param doc
   *          Ein OpenOffice.org-Dokument, in dem dokumentspezifische Funktionen des
   *          WollMux aufgerufen werden sollen.
   * @return Liefert null, falls doc durch den WollMux nicht bearbeitet wird und eine
   *         Instanz von XWollMuxDocument, falls es sich bei doc prinzipiell um ein
   *         WollMux-Dokument handelt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101), Christoph Lutz (D-III-ITD-D101)
   */
  public XWollMuxDocument getWollMuxDocument(XComponent doc)
  {
    XTextDocument tdoc = UNO.XTextDocument(doc);
    if (tdoc != null) return new WollMuxDocument(tdoc);
    return null;
  }

  /**
   * Implementiert XWollMuxDocument für alle dokumentspezifischen Aktionen
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class WollMuxDocument implements XWollMuxDocument
  {
    private XTextDocument doc;

    private HashMap<String, String> mapDbSpalteToValue;

    public WollMuxDocument(XTextDocument doc)
    {
      this.doc = doc;
      this.mapDbSpalteToValue = new HashMap<String, String>();
    }

    /**
     * Nimmt die Druckfunktion functionName in die Liste der Druckfunktionen des
     * Dokuments auf. Die Druckfunktion wird dabei automatisch aktiv, wenn das
     * Dokument das nächste mal mit Datei->Drucken gedruckt werden soll. Ist die
     * Druckfunktion bereits in der Liste der Druckfunktionen des Dokuments
     * enthalten, so geschieht nichts.
     * 
     * Hinweis: Die Ausführung erfolgt asynchron, d.h. addPrintFunction() kehrt unter
     * Umständen bereits zurück BEVOR die Methode ihre Wirkung entfaltet hat.
     * 
     * @param functionName
     *          der Name einer Druckfunktion, die im Abschnitt "Druckfunktionen" der
     *          WollMux-Konfiguration definiert sein muss.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void addPrintFunction(String functionName)
    {
      WollMuxEventHandler.handleManagePrintFunction(doc, functionName, false);
    }

    /**
     * Löscht die Druckfunktion functionName aus der Liste der Druckfunktionen des
     * Dokuments. Die Druckfunktion wird damit ab dem nächsten Aufruf von
     * Datei->Drucken nicht mehr aufgerufen. Ist die Druckfunktion nicht in der Liste
     * der Druckfunktionen des Dokuments enthalten, so geschieht nichts.
     * 
     * Hinweis: Die Ausführung erfolgt asynchron, d.h. removePrintFunction() kehrt
     * unter Umständen bereits zurück BEVOR die Methode ihre Wirkung entfaltet hat.
     * 
     * @param functionName
     *          der Name einer Druckfunktion, die im Dokument gesetzt ist.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void removePrintFunction(String functionName)
    {
      WollMuxEventHandler.handleManagePrintFunction(doc, functionName, true);
    }

    /**
     * Setzt den Wert mit ID id in der FormularGUI auf Wert mit allen Folgen, die das
     * nach sich zieht (PLAUSIs, AUTOFILLs, Ein-/Ausblendungen,...). Es ist nicht
     * garantiert, dass der Befehl ausgeführt wird, bevor updateFormGUI() aufgerufen
     * wurde. Eine Implementierung mit einer Queue ist möglich.
     * 
     * @param id
     * @param value
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void setFormValue(String id, String value)
    {
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleSetFormValue(doc, id, value, s);
      s.synchronize();
    }

    /**
     * Setzt den Wert, der bei insertValue-Dokumentkommandos mit DB_SPALTE "dbSpalte"
     * eingefügt werden soll auf Wert. Es ist nicht garantiert, dass der neue Wert im
     * Dokument sichtbar wird, bevor updateInsertFields() aufgerufen wurde. Eine
     * Implementierung mit einer Queue ist möglich.
     * 
     * @param dbSpalte
     *          enthält den Namen der Absenderdatenspalte, deren Wert geändert werden
     *          soll.
     * @param value
     *          enthält den neuen Wert für dbSpalte.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void setInsertValue(String dbSpalte, String value)
    {
      mapDbSpalteToValue.put(dbSpalte, value);
    }

    /**
     * Sorgt für die Ausführung aller noch nicht ausgeführten setFormValue()
     * Kommandos. Die Methode kehrt garantiert erst zurück, wenn alle
     * setFormValue()-Kommandos ihre Wirkung im WollMux und im entsprechenden
     * Dokument entfaltet haben.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void updateFormGUI()
    {
    // Wird implementiert, wenn setFormValue(...) so umgestellt werden soll, dass die
    // Änderungen vorerst nur in einer queue gesammelt werden und mit dieser Methode
    // aktiv werden sollen.
    }

    /**
     * Sorgt für die Ausführung aller noch nicht ausgeführten setInsertValue()
     * Kommandos. Die Methode kehrt garantiert erst zurück, wenn alle
     * setInsertValue()-Kommandos ihre Wirkung im WollMux und im entsprechenden
     * Dokument entfaltet haben.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void updateInsertFields()
    {
      HashMap<String, String> m = new HashMap<String, String>(mapDbSpalteToValue);
      mapDbSpalteToValue.clear();
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleSetInsertValues(doc, m, s);
      s.synchronize();
    }
  }
}
