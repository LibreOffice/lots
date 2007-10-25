/*
 * Dateiname: PrintModelFactory.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse enthält eine Fabrik für die Erzeugung eines PrintModels
 *            und die Klassendefinitionen des MasterPrintModels und des SlavePrintModels.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 01.10.2007 | LUT | Erstellung als PrintModelFactory
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyChangeListener;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.beans.XVetoableChangeListener;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Type;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.func.PrintFunction;

/**
 * Diese Klasse enthält eine Fabrik für die Erzeugung eines XPrintModels, die
 * Klassendefinitionen des MasterPrintModels und des SlavePrintModels, mit deren
 * Hilfe die Verkettung mehrerer PrintFunctions möglich ist und die Definitionen
 * der Properties, die beim Drucken zum Einsatz kommen. Ein XPrintModel hält
 * alle Daten und Methoden bereit, die beim Drucken aus einer Druckfunktion
 * heraus benötigt werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class PrintModels
{
  /**
   * Diese Klasse definiert alle Properties, die in einem PrintModel gesetzt
   * werden können und beim Drucken über die Methode printWithProps() von den
   * mit dem WollMux mitgelieferten Druckfunktionen ausgewertet werden.
   * Benutzerdefinierte (also nicht mit dem WollMux mitgeliferte)
   * Druckfunktionen können natürlich auch weitere Properties auswerten, die
   * nicht hier aufgeführt sind.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class PrintModelProps
  {
    /**
     * Diese Property vom Typ Short enthält die Anzahl der Ausfertigungen die
     * von dem Dokument gedruckt werden sollen. Sie wird von der letzten
     * Druckfunktion ausgewertet, die bei printWithProps() automatisch gestartet
     * wird, wenn keine andere Druckfunktion mehr in der Aufrufkette vorhanden
     * ist.
     */
    public static final String PROP_COPY_COUNT = "CopyCount";

    /**
     * Diese Property vom Typ Short enthält einen der Werte PAGE_RANGE_TYPE_*
     * und spezifiziert den Typ des Seitenbereichs, der ausgedruckt werden soll.
     * Dabei sind über diese Property auch automatische Bestimmungen des
     * Seitenbereichs wie z.B. über die aktuelle Cursorposition möglich. Die
     * Property ist nur in Verbindung mit der Property PROP_PAGE_RANGE_VALUE
     * sinnvoll. Sie wird von der letzten Druckfunktion ausgewertet, die bei
     * printWithProps() automatisch gestartet wird, wenn keine andere
     * Druckfunktion mehr in der Aufrufkette vorhanden ist.
     */
    public static final String PROP_PAGE_RANGE_TYPE = "PageRangeType";

    /**
     * Diese Property vom Typ String spezifiziert den zu druckenden
     * Seitenbereich als String in der Form, wie er auch bei Datei->Drucken in
     * OOo angegeben werden kann (z.B. "1-3", "1,2,5"). Die Property wird nur in
     * Verbindung mit dem Wert PAGE_RANGE_TYPE_MANUAL der Property
     * PROP_PAGE_RANGE_TYPE gelesen. Sie wird von der letzten Druckfunktion
     * ausgewertet, die bei printWithProps() automatisch gestartet wird, wenn
     * keine andere Druckfunktion mehr in der Aufrufkette vorhanden ist.
     */
    public static final String PROP_PAGE_RANGE_VALUE = "PageRangeValue";

    /**
     * Ist diese Property vom Typ String gesetzt, so wird der Wert direkt als
     * der zu druckende Seitenbereich übernommen. Er ist in der Form, wie er
     * auch bei Datei->Drucken in OOo angegeben werden kann (z.B. "1-3",
     * "1,2,5"). Die Property hat Vorrang vor den Properties PROP_PAGE_RANGE_*
     * und ermöglicht so die direkte manuelle Angabe des zu druckenden
     * Seitenbereichs. Sie wird von der letzten Druckfunktion ausgewertet, die
     * bei printWithProps() automatisch gestartet wird, wenn keine andere
     * Druckfunktion mehr in der Aufrufkette vorhanden ist.
     */
    public static final String PROP_PAGES = "Pages";

    /**
     * Diese Property vom Typ Array Of Short enthält die Anzahl der
     * Ausfertigungen, die für jeden in PROP_SLV_VERF_PUNKTE angegebenen
     * Verfügungspunkt beim Drucken gesetzt werden soll. Sie wird von der
     * Komfortdruckfunktion SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_COPY_COUNTS = "SLV_CopyCounts";

    /**
     * Diese Property vom Typ Array Of String enthält den PageRange-Wert, der
     * für jeden in PROP_SLV_VERF_PUNKTE angegebenen Verfügungspunkt beim
     * Drucken gesetzt werden soll. Sie wird von der Komfortdruckfunktion
     * SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_PAGE_RANGE_VALUES = "SLV_PageRangeValues";

    /**
     * Diese Property vom Typ Array Of Short enthält den PageRange-Typ, der für
     * jeden in PROP_SLV_VERF_PUNKTE angegebenen Verfügungspunkt beim Drucken
     * gesetzt werden soll. Sie wird von der Komfortdruckfunktion
     * SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_PAGE_RANGE_TYPES = "SLV_PageRangeTypes";

    /**
     * Diese Property vom Typ Array Of Boolean enthält das isOriginal-Flag, das
     * für jeden in PROP_SLV_VERF_PUNKTE angegebenen Verfügungspunkt beim
     * Drucken gesetzt werden soll. Sie wird von der Komfortdruckfunktion
     * SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_IS_ORIGINAL_FLAGS = "SLV_isOriginalFlags";

    /**
     * Diese Property vom Typ Array Of Boolean enthält das isDraft-Flag, das für
     * jeden in PROP_SLV_VERF_PUNKTE angegebenen Verfügungspunkt beim Drucken
     * gesetzt werden soll. Sie wird von der Komfortdruckfunktion
     * SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_IS_DRAFT_FLAGS = "SLV_isDraftFlags";

    /**
     * Diese Property vom Typ Array Of Short enthält die Nummern der zu
     * druckenden Verfügungspunkte. Sie wird von der Komfortdruckfunktion
     * SachleitendeVerfuegungOutput ausgewertet.
     */
    public static final String PROP_SLV_VERF_PUNKTE = "SLV_verfPunkte";

    /**
     * Wert der Property PROP_PAGE_RANGE_TYPE: Alle Seiten des Textdokuments
     * sollen gedruckt werden.
     */
    public static final short PAGE_RANGE_TYPE_ALL = 1;

    /**
     * Wert der Property PROP_PAGE_RANGE_TYPE: Nur die aktuelle Seite des
     * Textdokuments, auf der der Cursor gerade steht, soll gedruckt werden.
     */
    public static final short PAGE_RANGE_TYPE_CURRENT = 2;

    /**
     * Wert der Property PROP_PAGE_RANGE_TYPE: Der Druckbereich erstreckt sich
     * von der aktuellen Seite des Textdokuments, auf der der Cursor gerade
     * steht, bis zum Dokumentende.
     */
    public static final short PAGE_RANGE_TYPE_CURRENTFF = 3;

    /**
     * Wert der Property PROP_PAGE_RANGE_TYPE: Sorgt dafür, dass der Wert der
     * Property PAGE_RANGE_VALUE als Druckbereich übernommen wird.
     */
    public static final short PAGE_RANGE_TYPE_MANUAL = 4;
  }

  /**
   * Erzeugt ein PrintModel-Objekt, das einen Druckvorgang zum Dokument
   * TextDocumentModel model repräsentiert. Pro Druckvorgang wird dabei ein
   * neuer PrintModelMaster erzeugt, der ein oder mehrere PrintModelSlaves
   * anspricht und so eine Verkettung mehrerer Druckfunktionen ermöglicht.
   * 
   * @param model
   *          Das Dokument das gedruckt werden soll
   * @return das neue PrintModel für diesen Druckvorgang
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static XPrintModel createPrintModel(TextDocumentModel model)
  {
    return new MasterPrintModel(model);
  }

  /**
   * Jedes hier definierte konkrete PrintModel definiert dieses Interface und
   * kann (ausschließlich) innerhalb der Java-VM des WollMux verwendet werden um
   * auf nicht im XPrintModel exportierte Methode der PrintModels zuzugreifen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static interface InternalPrintModel
  {
    /**
     * Lädt die WollMux-interne Druckfunktion printFunction (muss als
     * PrintFunction-Objekt vorliegen) in das XPrintModel und ordnet sie gemäß
     * dem ORDER-Attribut an der richtigen Position in die Aufrufkette der zu
     * bearbeitenden Druckfunktionen ein.
     * 
     * @param printFunction
     *          Druckfunktion, die durch das PrintModel verwaltet werden soll.
     * @return liefert true, wenn die Druckfunktion erfolgreich in die
     *         Aufrufkette übernommen wurde oder bereits geladen war und false,
     *         wenn die Druckfunktion aufgrund vorangegangener Fehler nicht in
     *         die Aufrufkette aufgenommen werden konnte.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean useInternalPrintFunction(PrintFunction printFunction);
  }

  /**
   * Das MasterPrintModel repräsentiert einen kompletten Druckvorgang und
   * verwaltet alle Druckfunktionen, die an diesem Druckvorgang beteiligt sind.
   * Es kann dynamisch weitere Druckfunktionen nachladen und diese in der durch
   * das ORDER-Attribut vorgegebenen Reihenfolge zu einer Aufrufkette anordnen.
   * Für die Kommunikation zwischen den verschiedenen Druckfunktionen
   * implementiert es das XPropertySet()-Interface und kann in einer HashMap
   * beliebige funktionsspezifische Daten ablegen.
   * 
   * Eine einzelne Druckfunktion wird immer mit einem zugehörigen
   * SlavePrintModel ausgeführt, das seine Position in der Aufrufkette des
   * MasterPrintModles kennt und die Weiterleitung an die nächste Druckfunktion
   * der Aufrufkette erledigt. Da die einzelnen Druckfunktionen in eigenen
   * Threads laufen, muss an einer zentralen Stelle sicher gestellt sein, dass
   * die zu erledigenden Aktionen mit dem WollMuxEventHandler-Thread
   * synchronisiert werden. Dies geschieht in dieser Klasse, die über einen
   * lock-wait-callback-Mechanismus die Synchronisierung garantiert. Vor dem
   * Einstellen des Action-Ereignisses in den WollMuxEventHandler wird dabei ein
   * lock gesetzt. Nach dem Einstellen des Ereignisses wird so lange gewartet,
   * bis der WollMuxEventHandler die übergebene Callback-Methode aufruft.
   * 
   * @author christoph.lutz
   */
  private static class MasterPrintModel implements XPrintModel,
      InternalPrintModel
  {
    /**
     * Enthält die sortierte Menge aller PrintFunction-Objekte der Aufrufkette.
     */
    private SortedSet functions;

    /**
     * Enthält die Properties, die in printWithProps() ausgewertet werden und
     * über die get/setPropertyValue-Methoden frei gesetzt und gelesen werden
     * können.
     */
    private HashMap props;

    /**
     * Das TextDocumentModel zu diesem PrintModel
     */
    private TextDocumentModel model;

    /**
     * Das lock-Flag, das vor dem Einstellen eines WollMuxEvents auf true
     * gesetzt werden muss und signalisiert, ob das WollMuxEvent erfolgreich
     * abgearbeitet wurde.
     */
    private boolean[] lock = new boolean[] { true };

    /**
     * Erzeugt ein neues MasterPrintModel-Objekt für das Dokument model, das
     * einen Druckvorgang repräsentiert, der mit einer leeren Aufrufkette (Liste
     * von Druckfunktionen) und einer leeren HashMap für den
     * Informationsaustausch zwischen den Druckfunktionen vorbelegt ist. Nach
     * der Erzeugung können weitere Druckfunktionen über
     * usePrintFunction/useInternalPrintFunction... hinzugeladen werden und
     * Properties über get/setPropertyValue gesetzt bzw. gelesen werden.
     * 
     * @param model
     */
    private MasterPrintModel(TextDocumentModel model)
    {
      this.model = model;
      this.props = new HashMap();
      this.functions = new TreeSet();
    }

    /**
     * Lädt die in der wollmux.conf definierte Druckfunktion mit dem Namen
     * functionName in das XPrintModel und ordnet sie gemäß dem ORDER-Attribut
     * an der richtigen Position in die Aufrufkette der zu bearbeitenden
     * Druckfunktionen ein; Wird die Druckfunktion aufgerufen, so bekommt sie
     * genau ein Argument (dieses XPrintModel) übergeben.
     * 
     * @param functionName
     *          Name der Druckfunktion, die durch das MasterPrintModel verwaltet
     *          werden soll.
     * @return liefert true, wenn die Druckfunktion erfolgreich in die
     *         Aufrufkette übernommen wurde oder bereits geladen war und false,
     *         wenn die Druckfunktion aufgrund vorangegangener Fehler nicht in
     *         die Aufrufkette aufgenommen werden konnte.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
     */
    public boolean usePrintFunction(String functionName)
    {
      PrintFunction newFunc = WollMuxSingleton.getInstance()
          .getGlobalPrintFunctions().get(functionName);
      if (newFunc != null)
      {
        return useInternalPrintFunction(newFunc);
      }
      else
      {
        Logger.error("Druckfunktion '" + functionName + "' nicht definiert.");
        return false;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel#useInternalPrintFunction(de.muenchen.allg.itd51.wollmux.func.PrintFunction)
     */
    public boolean useInternalPrintFunction(PrintFunction printFunction)
    {
      if (printFunction != null)
      {
        functions.add(printFunction);
        return true;
      }
      return false;
    }

    /**
     * Alle im MasterPrintModel geladenen Druckfuntkionen werden in die durch
     * das ORDER-Attribut definierte Reihenfolge in einer Aufrufkette
     * angeordnet; Diese Methode liefert die Druckfunktion an der Position idx
     * dieser Aufrufkette (die Zählung beginnt mit 0).
     * 
     * @param idx
     *          Die Position der Druckfunktion
     * @return Die Druckfunktion an der Position idx in der sortierten
     *         Reihenfolge oder null, wenn es an der Position idx keine
     *         Druckfunktion gibt (z.B. IndexOutOfRange)
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    protected PrintFunction getPrintFunction(int idx)
    {
      Object[] funcs = functions.toArray();
      if (idx >= 0 && idx < funcs.length)
        return (PrintFunction) funcs[idx];
      else
        return null;
    }

    /**
     * Liefert das XTextDocument mit dem die Druckfunktion aufgerufen wurde.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
     */
    public XTextDocument getTextDocument()
    {
      return model.doc;
    }

    /**
     * Deprecated: Bitte benutzen Sie statt dieser Methode die Methode
     * printWithProps() mit dem entsprechend gesetzten Property "CopyCount".
     * Diese Methode dient nur der Abwärtskompatibilität mit älteren
     * XPrintModels und verwendet intern ebenfalls die Methode printWithProps()
     * und der Property "CopyCount".
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    public void print(short numberOfCopies)
    {
      try
      {
        setPropertyValue(PrintModelProps.PROP_COPY_COUNT, new Short(
            numberOfCopies));
        printWithProps();
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }

    /**
     * Druckt das TextDocument auf dem aktuell eingestellten Drucker aus oder
     * leitet die Anfrage an die nächste verfügbare Druckfunktion in der
     * Aufrufkette weiter, wenn eine weitere Druckfunktion vorhanden ist;
     * Abhängig von der gesetzten Druckfunktion werden dabei verschiedene
     * Properties, die über setPropertyValue(...) gesetzt wurden ausgewertet.
     * 
     * Im MasterPrintModel sorgt der Aufruf dieser Methode dafür, dass (nur) die
     * erste verfügbare Druckfunktion aufgerufen wird. Das Weiterreichen der
     * Anfrage an die jeweils nächste Druckfunktion übernimmt dann das
     * SlavePrintModel. Ist die Aufrufkette zum Zeitpunkt des Aufrufs leer, so
     * wird ein Dispatch ".uno:Print" abgesetzt, damit der Standarddruckdialog
     * von OOo aufgerufen wird.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
     */
    public void printWithProps()
    {
      PrintFunction f = getPrintFunction(0);
      if (f != null)
      {
        XPrintModel pmod = new SlavePrintModel(this, 0);
        f.invoke(pmod);
      }
      else
      {
        // Es ist keine Druckfunktion definiert, darum wird ein erneuter
        // Dispatch-Aufruf gestartet, der den Standard-Druckdialog aufruft.
        UNO.dispatch(model.doc, ".uno:Print");
      }
    }

    /**
     * Wird vom SlavePrintModel aufgerufen, wenn beim Aufruf von
     * printWithProps() keine weitere Druckfunktion mehr in der Aufrufkette
     * verfügbar ist und das Dokument damit tatsächlich physikalisch gedruckt
     * werden soll.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    protected void finalPrintWithProps()
    {
      setLock();
      WollMuxEventHandler.handlePrintViaPrintModel(
          model.doc,
          props,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Zeigt den PrintSetupDialog an, über den der aktuelle Drucker ausgewählt
     * und geändert werden kann.
     * 
     * @param onlyOnce
     *          Gibt an, dass der Dialog nur beim ersten Aufruf (aus Sicht eines
     *          Dokuments) der Methode angezeigt wird. Wurde bereits vor dem
     *          Aufruf ein PrintSetup-Dialog gestartet, so öffnet sich der
     *          Dialog nicht und die Methode endet ohne Aktion.
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#showPrinterSetupDialog()
     */
    public void showPrinterSetupDialog(boolean onlyOnce)
    {
      setLock();
      WollMuxEventHandler.handleShowPrinterSetupDialog(
          model.doc,
          onlyOnce,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Falls es sich bei dem zugehörigen Dokument um ein Formulardokument (mit
     * einer Formularbeschreibung) handelt, wird das Formularfeld mit der ID id
     * auf den neuen Wert value gesetzt und alle von diesem Formularfeld
     * abhängigen Formularfelder entsprechend angepasst. Handelt es sich beim
     * zugehörigen Dokument um ein Dokument ohne Formularbeschreibung, so werden
     * nur alle insertFormValue-Kommandos dieses Dokuments angepasst, die die ID
     * id besitzen.
     * 
     * @param id
     *          Die ID des Formularfeldes, dessen Wert verändert werden soll.
     *          Ist die FormGUI aktiv, so werden auch alle von id abhängigen
     *          Formularwerte neu gesetzt.
     * @param value
     *          Der neue Wert des Formularfeldes id
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     *      java.lang.String)
     */
    public void setFormValue(String id, String value)
    {
      setLock();
      WollMuxEventHandler.handleSetFormValueViaPrintModel(
          model.doc,
          id,
          value,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit
     * z.B. die "Speichern?" Abfrage vor dem Schließen erscheint.
     * 
     * Manche Druckfunktionen verändern u.U. den Inhalt von Dokumenten. Trotzdem
     * kann es sein, dass eine solche Druckfunktion den "Modifiziert"-Status des
     * Dokuments nicht verändern darf um ungewünschte "Speichern?"-Abfragen zu
     * verhindern. In diesem Fall kann der "Modifiziert"-Status mit folgendem
     * Konstrukt innerhalb der Druckfunktion unverändert gehalten werden:
     * 
     * boolean modified = pmod.getDocumentModified();
     * 
     * ...die eigentliche Druckfunktion, die das Dokument verändert...
     * 
     * pmod.setDocumentModified(modified);
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
     */
    public boolean getDocumentModified()
    {
      // Keine WollMuxEvent notwendig, da keine WollMux-Datenstrukturen
      // angefasst werden.
      return model.getDocumentModified();
    }

    /**
     * Diese Methode setzt den DocumentModified-Status auf modified.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setDocumentModified(boolean)
     */
    public void setDocumentModified(boolean modified)
    {
      // Keine WollMuxEvent notwendig, da keine WollMux-Datenstrukturen
      // angefasst werden.
      model.setDocumentModified(modified);
    }

    /**
     * Sammelt alle Formularfelder des Dokuments auf, die nicht von
     * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden
     * und befüllt werden (derzeit c,s,s,t,textfield,Database-Felder). So werden
     * z.B. Seriendruckfelder erkannt, die erst nach dem Öffnen des Dokuments
     * manuell hinzugefügt wurden.
     */
    public void collectNonWollMuxFormFields()
    {
      setLock();
      WollMuxEventHandler.handleCollectNonWollMuxFormFieldsViaPrintModel(
          model,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um eine Synchronisierung mit
     * dem WollMuxEventHandler-Thread zu realisieren. setLock() sollte stets vor
     * dem Absetzen des WollMux-Events erfolgen, nach dem Absetzen des
     * WollMux-Events folgt der Aufruf der waitForUnlock()-Methode. Das
     * WollMuxEventHandler-Event erzeugt bei der Beendigung ein ActionEvent, das
     * dafür sorgt, dass setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      synchronized (lock)
      {
        lock[0] = true;
      }
    }

    /**
     * Macht einen mit setLock() gesetzten Lock rückgängig und bricht damit eine
     * evtl. wartende waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der
     * Methode setUnlock() aufgehoben wird. So kann die Synchronisierung mit
     * Events aus dem WollMuxEventHandler-Thread realisiert werden. setLock()
     * sollte stets vor dem Aufruf des Events erfolgen, nach dem Aufruf des
     * Events folgt der Aufruf der waitForUnlock()-Methode. Das Event erzeugt
     * bei der Beendigung ein ActionEvent, das dafür sorgt, dass setUnlock
     * aufgerufen wird.
     */
    protected void waitForUnlock()
    {
      try
      {
        synchronized (lock)
        {
          while (lock[0] == true)
            lock.wait();
        }
      }
      catch (InterruptedException e)
      {
      }
    }

    /**
     * Dieser ActionListener kann WollMuxHandler-Events übergeben werden und
     * sorgt in Verbindung mit den Methoden setLock() und waitForUnlock() dafür,
     * dass eine Synchronisierung mit dem WollMuxEventHandler-Thread realisiert
     * werden kann.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        setUnlock();
        actionEvent = arg0;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertySetInfo()
     */
    public XPropertySetInfo getPropertySetInfo()
    {
      return new XPropertySetInfo()
      {
        public boolean hasPropertyByName(String arg0)
        {
          return props.containsKey(arg0);
        }

        public Property getPropertyByName(String arg0)
            throws UnknownPropertyException
        {
          if (hasPropertyByName(arg0))
            return new Property(arg0, -1, Type.ANY, PropertyAttribute.OPTIONAL);
          else
            throw new UnknownPropertyException(arg0);
        }

        public Property[] getProperties()
        {
          Property[] ps = new Property[props.size()];
          int i = 0;
          for (Iterator iter = props.keySet().iterator(); iter.hasNext();)
          {
            String name = (String) iter.next();
            try
            {
              ps[i++] = getPropertyByName(name);
            }
            catch (UnknownPropertyException e)
            {
            }
          }
          return ps;
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#setPropertyValue(java.lang.String,
     *      java.lang.Object)
     */
    public void setPropertyValue(String arg0, Object arg1)
        throws UnknownPropertyException, PropertyVetoException,
        IllegalArgumentException, WrappedTargetException
    {
      props.put(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertyValue(java.lang.String)
     */
    public Object getPropertyValue(String arg0)
        throws UnknownPropertyException, WrappedTargetException
    {
      if (props.containsKey(arg0))
        return props.get(arg0);
      else
        throw new UnknownPropertyException(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#addPropertyChangeListener(java.lang.String,
     *      com.sun.star.beans.XPropertyChangeListener)
     */
    public void addPropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#removePropertyChangeListener(java.lang.String,
     *      com.sun.star.beans.XPropertyChangeListener)
     */
    public void removePropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#addVetoableChangeListener(java.lang.String,
     *      com.sun.star.beans.XVetoableChangeListener)
     */
    public void addVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#removeVetoableChangeListener(java.lang.String,
     *      com.sun.star.beans.XVetoableChangeListener)
     */
    public void removeVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      // NOT IMPLEMENTED
    }

    /**
     * Diese Methode setzt die Eigenschaften "Sichtbar" (visible) und die
     * Anzeige der Hintergrundfarbe (showHighlightColor) für alle Druckblöcke
     * eines bestimmten Blocktyps blockName (z.B. "AllVersions").
     * 
     * @param blockName
     *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
     *          Folgende Blocknamen werden derzeit unterstützt: "AllVersions",
     *          "DraftOnly" und "NotInOriginal"
     * @param visible
     *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
     *          visible==false.
     * @param showHighlightColor
     *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur,
     *          wenn zu einem betroffenen Druckblock auch eine Hintergrundfarbe
     *          angegeben ist).
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void setPrintBlocksProps(String blockName, boolean visible,
        boolean showHighlightColor)
    {
      setLock();
      WollMuxEventHandler.handleSetPrintBlocksPropsViaPrintModel(
          model.doc,
          blockName,
          visible,
          showHighlightColor,
          unlockActionListener);
      waitForUnlock();
    }
  }

  /**
   * Beim Aufruf einer einzelnen Druckfunktion wird dieser Druckfunktion ein
   * XPrintModel, repräsentiert durch das SlavePrintModel, übergeben. Für jede
   * im MaserPrintModel verwaltete und aufgerufene Druckfunktion existiert also
   * genau ein SlavePrintModel, das im wesentlichen alle Anfragen
   * (Methodenaufrufe) an das MasterPrintModel weiterleitet. Das SlavePrintModel
   * kennt seine Position (idx) in der Aufrufkette und sorgt vor allem dafür,
   * dass beim Aufruf von printWithProps() die nächste Druckfunktion der
   * Aufrufkette gestartet wird.
   * 
   * Das SlavePrintModel ist von WeakBase abgeleitet, damit es in der
   * Druckfunktion mit den UNO-Mitteln inspiziert werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class SlavePrintModel extends WeakBase implements XPrintModel,
      InternalPrintModel
  {
    private int idx;

    private MasterPrintModel master;

    /**
     * Erzeugt ein neues SlavePrintModel, das in der Aufrufkette, die durch das
     * MasterPrintModel master verwaltet wird, an der Stelle idx steht.
     * 
     * @param master
     *          Das MasterPrintModel, an das die meisten Anfragen weitergeleitet
     *          werden und das die Aufrufkette der Druckfunktionen verwaltet.
     * @param idx
     *          Die Position der zu diesem SlavePrintModel zugehörigen
     *          Druckfunktion in der Aufrufkette von master.
     */
    public SlavePrintModel(MasterPrintModel master, int idx)
    {
      this.master = master;
      this.idx = idx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
     */
    public XTextDocument getTextDocument()
    {
      return master.getTextDocument();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    public void print(short numberOfCopies)
    {
      try
      {
        setPropertyValue(PrintModelProps.PROP_COPY_COUNT, new Short(
            numberOfCopies));
        printWithProps();
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }

    /**
     * Diese Methode ist die wichtigste Methode im SlavePrintModel, denn sie
     * sorgt dafür, dass beim Aufruf von PrintWithProps die Weiterleitung an die
     * nächste Druckfunktion der Aufrufkette veranlasst wird.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
     */
    public void printWithProps()
    {
      PrintFunction f = master.getPrintFunction(idx + 1);
      if (f != null)
      {
        XPrintModel pmod = new SlavePrintModel(master, idx + 1);
        Thread t = f.invoke(pmod);
        try
        {
          t.join();
        }
        catch (InterruptedException e)
        {
          Logger.error(e);
        }
      }
      else
      {
        master.finalPrintWithProps();
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#showPrinterSetupDialog(boolean)
     */
    public void showPrinterSetupDialog(boolean arg0)
    {
      master.showPrinterSetupDialog(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     *      java.lang.String)
     */
    public void setFormValue(String arg0, String arg1)
    {
      master.setFormValue(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
     */
    public boolean getDocumentModified()
    {
      return master.getDocumentModified();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setDocumentModified(boolean)
     */
    public void setDocumentModified(boolean arg0)
    {
      master.setDocumentModified(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#collectNonWollMuxFormFields()
     */
    public void collectNonWollMuxFormFields()
    {
      master.collectNonWollMuxFormFields();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintBlocksProps(java.lang.String,
     *      boolean, boolean)
     */
    public void setPrintBlocksProps(String arg0, boolean arg1, boolean arg2)
    {
      master.setPrintBlocksProps(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertySetInfo()
     */
    public XPropertySetInfo getPropertySetInfo()
    {
      return master.getPropertySetInfo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#setPropertyValue(java.lang.String,
     *      java.lang.Object)
     */
    public void setPropertyValue(String arg0, Object arg1)
        throws UnknownPropertyException, PropertyVetoException,
        IllegalArgumentException, WrappedTargetException
    {
      master.setPropertyValue(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertyValue(java.lang.String)
     */
    public Object getPropertyValue(String arg0)
        throws UnknownPropertyException, WrappedTargetException
    {
      return master.getPropertyValue(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#addPropertyChangeListener(java.lang.String,
     *      com.sun.star.beans.XPropertyChangeListener)
     */
    public void addPropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.addPropertyChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#removePropertyChangeListener(java.lang.String,
     *      com.sun.star.beans.XPropertyChangeListener)
     */
    public void removePropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.removePropertyChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#addVetoableChangeListener(java.lang.String,
     *      com.sun.star.beans.XVetoableChangeListener)
     */
    public void addVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.addVetoableChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#removeVetoableChangeListener(java.lang.String,
     *      com.sun.star.beans.XVetoableChangeListener)
     */
    public void removeVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.removeVetoableChangeListener(arg0, arg1);
    }

    /**
     * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es,
     * dass nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als
     * der ORDER-Wert der aktuellen Druckfunktion ist.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
     */
    public boolean usePrintFunction(String functionName)
    {
      PrintFunction newFunc = WollMuxSingleton.getInstance()
          .getGlobalPrintFunctions().get(functionName);
      if (newFunc != null)
      {
        return useInternalPrintFunction(newFunc);
      }
      else
      {
        Logger.error("Druckfunktion '" + functionName + "' nicht definiert.");
        return false;
      }
    }

    /**
     * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es,
     * dass nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als
     * der ORDER-Wert der aktuellen Druckfunktion ist.
     * 
     * @see de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel#useInternalPrintFunction(de.muenchen.allg.itd51.wollmux.func.PrintFunction)
     */
    public boolean useInternalPrintFunction(PrintFunction function)
    {
      if (function != null)
      {
        PrintFunction currentFunc = master.getPrintFunction(idx);
        if (function.compareTo(currentFunc) <= 0)
        {
          Logger
              .error("Druckfunktion '"
                     + function.getFunctionName()
                     + "' muss einen höheren ORDER-Wert besitzen als die Druckfunktion '"
                     + currentFunc.getFunctionName()
                     + "'");
          return false;
        }
        else
          return master.useInternalPrintFunction(function);
      }
      else
      {
        Logger.error("Die angeforderte interne Druckfunktion ist ungültig.");
        return false;
      }
    }

  }
}
