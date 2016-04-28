/*
 * Dateiname: PrintModelFactory.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse enthält eine Fabrik für die Erzeugung eines PrintModels
 *            und die Klassendefinitionen des MasterPrintModels und des SlavePrintModels.
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 01.10.2007 | LUT | Erstellung als PrintModelFactory
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Type;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.PrintParametersDialog;
import de.muenchen.allg.itd51.wollmux.dialog.PrintParametersDialog.PageRange;
import de.muenchen.allg.itd51.wollmux.dialog.PrintParametersDialog.PageRangeType;
import de.muenchen.allg.itd51.wollmux.dialog.PrintProgressBar;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.print.PrintFunction;

/**
 * Diese Klasse enthält eine Fabrik für die Erzeugung eines XPrintModels, die
 * Klassendefinitionen des MasterPrintModels und des SlavePrintModels, mit deren
 * Hilfe die Verkettung mehrerer PrintFunctions möglich ist. Ein XPrintModel hält
 * alle Daten und Methoden bereit, die beim Drucken aus einer Druckfunktion heraus
 * benötigt werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class PrintModels
{
  /**
   * Spezial-Property für {@link XPrintModel#setPropertyValue(String, Object)} zum
   * Setzen eines Beschreibungsstrings des aktuell in Bearbeitung befindlichen
   * Druckvorgangs. Für die Verwendung des Stage-Feature innerhalb des WollMux siehe
   * {@link #setStage(XPrintModel, String)}.
   */
  private static final String STAGE = "STAGE";

  /**
   * Erzeugt ein PrintModel-Objekt, das einen Druckvorgang zum Dokument
   * TextDocumentModel model repräsentiert. Pro Druckvorgang wird dabei ein neuer
   * PrintModelMaster erzeugt, der ein oder mehrere PrintModelSlaves anspricht und so
   * eine Verkettung mehrerer Druckfunktionen ermöglicht.
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
   * Liefert ein neues zu diesem TextDocumentModel zugehörige XPrintModel für einen
   * Druckvorgang; ist useDocumentPrintFunctions==true, so werden bereits alle im
   * Dokument gesetzten Druckfunktionen per
   * XPrintModel.usePrintFunctionWithArgument(...) hinzugeladen.
   * 
   * @param model
   *          Das Dokument das gedruckt werden soll
   * @param useDocPrintFunctions
   *          steuert ob das PrintModel mit den im Dokument gesetzten Druckfunktionen
   *          vorbelegt sein soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static XPrintModel createPrintModel(TextDocumentModel model, boolean useDocPrintFunctions)
  {
    XPrintModel pmod = PrintModels.createPrintModel(model);
    if (useDocPrintFunctions)
    {
      for (Iterator<String> iter = model.getPrintFunctions().iterator(); iter.hasNext();)
      {
        String name = iter.next();
        try
        {
          pmod.usePrintFunction(name);
        }
        catch (NoSuchMethodException e)
        {
          Logger.error(e);
        }
      }
    }
    return pmod;
  }



  /**
   * Jedes hier definierte konkrete PrintModel definiert dieses Interface und kann
   * (ausschließlich) innerhalb der Java-VM des WollMux verwendet werden um auf nicht
   * im XPrintModel exportierte Methode der PrintModels zuzugreifen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static interface InternalPrintModel
  {
    /**
     * Lädt die WollMux-interne Druckfunktion printFunction (muss als
     * PrintFunction-Objekt vorliegen) in das XPrintModel und ordnet sie gemäß dem
     * ORDER-Attribut an der richtigen Position in die Aufrufkette der zu
     * bearbeitenden Druckfunktionen ein.
     * 
     * @param printFunction
     *          Druckfunktion, die durch das PrintModel verwaltet werden soll.
     * @return liefert true, wenn die Druckfunktion erfolgreich in die Aufrufkette
     *         übernommen wurde oder bereits geladen war und false, wenn die
     *         Druckfunktion aufgrund vorangegangener Fehler nicht in die Aufrufkette
     *         aufgenommen werden konnte.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean useInternalPrintFunction(PrintFunction printFunction);
  }

  /**
   * Das MasterPrintModel repräsentiert einen kompletten Druckvorgang und verwaltet
   * alle Druckfunktionen, die an diesem Druckvorgang beteiligt sind. Es kann
   * dynamisch weitere Druckfunktionen nachladen und diese in der durch das
   * ORDER-Attribut vorgegebenen Reihenfolge zu einer Aufrufkette anordnen. Für die
   * Kommunikation zwischen den verschiedenen Druckfunktionen implementiert es das
   * XPropertySet()-Interface und kann in einer HashMap beliebige
   * funktionsspezifische Daten ablegen.
   * 
   * Eine einzelne Druckfunktion wird immer mit einem zugehörigen SlavePrintModel
   * ausgeführt, das seine Position in der Aufrufkette des MasterPrintModles kennt
   * und die Weiterleitung an die nächste Druckfunktion der Aufrufkette erledigt. Da
   * die einzelnen Druckfunktionen in eigenen Threads laufen, muss an einer zentralen
   * Stelle sicher gestellt sein, dass die zu erledigenden Aktionen mit dem
   * WollMuxEventHandler-Thread synchronisiert werden. Dies geschieht in dieser
   * Klasse, die über einen lock-wait-callback-Mechanismus die Synchronisierung
   * garantiert. Vor dem Einstellen des Action-Ereignisses in den WollMuxEventHandler
   * wird dabei ein lock gesetzt. Nach dem Einstellen des Ereignisses wird so lange
   * gewartet, bis der WollMuxEventHandler die übergebene Callback-Methode aufruft.
   * 
   * @author christoph.lutz
   */
  private static class MasterPrintModel implements XPrintModel, InternalPrintModel
  {
    /**
     * Schlüssel der Property, über die gesteuert wird, ob der finale Druckdialog mit
     * einem CopyCount-Spinner angezeigt wird.
     */
    private static final String PROP_FINAL_SHOW_COPIES_SPINNER =
      "FinalPF_ShowCopiesSpinner";

    /**
     * Schlüssel der Property, über die die Anzeige des finalen Druckdialogs bei
     * folgenden Aufrufen von finalPrint() abgeschalten werden kann.
     */
    private static final String PROP_FINAL_NO_PARAMS_DIALOG =
      "FinalPF_NoParamsDialog";

    /**
     * Schlüssel der Property, an der die Einstellungen zum Druckbereich für folgende
     * Aufrufe von finalPrint() hinterlegt werden können.
     */
    private static final String PROP_FINAL_PAGE_RANGE = "FinalPF_PageRange";

    /**
     * Schlüssel der Property, an der der Kopienzähler für folgende Aufrufe von
     * finalPrint() hinterlegt werden können.
     */
    private static final String PROP_FINAL_COPY_COUNT = "FinalPF_CopyCount";

    /**
     * Enthält die sortierte Menge aller PrintFunction-Objekte der Aufrufkette.
     */
    private SortedSet<PrintFunction> functions;

    /**
     * Enthält die Properties, die in printWithProps() ausgewertet werden und über
     * die get/setPropertyValue-Methoden frei gesetzt und gelesen werden können.
     */
    private HashMap<String, Object> props;

    /**
     * Das TextDocumentModel zu diesem PrintModel
     */
    private TextDocumentModel model;

    /**
     * Enthält das Flag das Auskunft darüber gibt, ob der Druckauftrag abgebrochen
     * wurde oder nicht.
     */
    private boolean[] isCanceled = new boolean[] { false };

    /**
     * Enthält null oder ab dem ersten Aufruf von setPrintProgress[Max]Value ein
     * gültiges PrintProgressBar-Objekt zur Anzeige des Druckstatus.
     */
    private PrintProgressBar printProgressBar = null;

    /**
     * Enthält die Beschreibung des aktuell ausgeführten Druckvorgangs.
     */
    private String currentStage = L.m("Drucke");

    /**
     * Erzeugt ein neues MasterPrintModel-Objekt für das Dokument model, das einen
     * Druckvorgang repräsentiert, der mit einer leeren Aufrufkette (Liste von
     * Druckfunktionen) und einer leeren HashMap für den Informationsaustausch
     * zwischen den Druckfunktionen vorbelegt ist. Nach der Erzeugung können weitere
     * Druckfunktionen über usePrintFunction/useInternalPrintFunction... hinzugeladen
     * werden und Properties über get/setPropertyValue gesetzt bzw. gelesen werden.
     * 
     * @param model
     */
    private MasterPrintModel(TextDocumentModel model)
    {
      this.model = model;
      this.props = new HashMap<String, Object>();
      this.functions = new TreeSet<PrintFunction>();
    }

    /**
     * Lädt die in der wollmux.conf definierte Druckfunktion mit dem Namen
     * functionName in das XPrintModel und ordnet sie gemäß dem ORDER-Attribut an der
     * richtigen Position in die Aufrufkette der zu bearbeitenden Druckfunktionen
     * ein; Wird die Druckfunktion aufgerufen, so bekommt sie genau ein Argument
     * (dieses XPrintModel) übergeben.
     * 
     * @param functionName
     *          Name der Druckfunktion, die durch das MasterPrintModel verwaltet
     *          werden soll.
     * @throws NoSuchMethodException
     *           Wird geworfen, wenn die Druckfunktion nicht definiert ist.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
     */
    @Override
    public void usePrintFunction(String functionName) throws NoSuchMethodException
    {
      PrintFunction newFunc =
          GlobalFunctions.getInstance().getGlobalPrintFunctions().get(functionName);
      if (newFunc != null)
        useInternalPrintFunction(newFunc);
      else
        throw new NoSuchMethodException(L.m(
          "Druckfunktion '%1' ist nicht definiert.", functionName));
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel#
     * useInternalPrintFunction(de.muenchen.allg.itd51.wollmux.func.PrintFunction)
     */
    @Override
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
     * Alle im MasterPrintModel geladenen Druckfuntkionen werden in die durch das
     * ORDER-Attribut definierte Reihenfolge in einer Aufrufkette angeordnet; Diese
     * Methode liefert die Druckfunktion an der Position idx dieser Aufrufkette (die
     * Zählung beginnt mit 0).
     * 
     * @param idx
     *          Die Position der Druckfunktion
     * @return Die Druckfunktion an der Position idx in der sortierten Reihenfolge
     *         oder null, wenn es an der Position idx keine Druckfunktion gibt (z.B.
     *         IndexOutOfRange)
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
    @Override
    public XTextDocument getTextDocument()
    {
      return model.doc;
    }

    /**
     * Diese Methode ruft numberOfCopies mal printWithProps() auf.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    @Override
    public void print(short numberOfCopies)
    {
      for (int i = 0; i < numberOfCopies; ++i)
        printWithProps();
    }

    /**
     * Druckt das TextDocument auf dem aktuell eingestellten Drucker aus oder leitet
     * die Anfrage an die nächste verfügbare Druckfunktion in der Aufrufkette weiter,
     * wenn eine weitere Druckfunktion vorhanden ist; Abhängig von der gesetzten
     * Druckfunktion werden dabei verschiedene Properties, die über
     * setPropertyValue(...) gesetzt wurden ausgewertet. Die Methode kehrt erst dann
     * wieder zurück, wenn der gesamte Druckvorgang dieser und der darunterliegenden
     * Druckfunktionen vollständig ausgeführt wurde.
     * 
     * Im MasterPrintModel sorgt der Aufruf dieser Methode dafür, dass (nur) die
     * erste verfügbare Druckfunktion aufgerufen wird. Das Weiterreichen der Anfrage
     * an die jeweils nächste Druckfunktion übernimmt dann das SlavePrintModel. Ist
     * die Aufrufkette zum Zeitpunkt des Aufrufs leer, so wird ein Dispatch
     * ".uno:Print" abgesetzt, damit der Standarddruckdialog von OOo aufgerufen wird.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
     */
    @Override
    public void printWithProps()
    {
      if (isCanceled()) return;

      PrintFunction f = getPrintFunction(0);
      if (f != null)
      {
        XPrintModel pmod = new SlavePrintModel(this, 0);
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
        setProperty(PROP_FINAL_SHOW_COPIES_SPINNER, Boolean.TRUE);
        finalPrint();
      }

      if (printProgressBar != null)
      {
        printProgressBar.dispose();
        printProgressBar = null;
      }
    }

    /**
     * Zeigt beim ersten Aufruf den finalen Druckdialog an, über den die
     * Einstellungen für den tatsächlichen Druck auf einen Drucker gesetzt werden
     * können und startet den Druck. Bei folgenden Aufrufe dieser Methode wird kein
     * Druckdialog mehr angezeigt und die zuletzt getroffenen Einstellungen werden
     * verwendet.
     * 
     * @return bei Erfolg true, sonst false.
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    protected void finalPrint()
    {
      Boolean b = (Boolean) getProperty(PROP_FINAL_SHOW_COPIES_SPINNER);
      boolean showCopiesSpinner = (b != null) ? b.booleanValue() : false;

      b = (Boolean) getProperty(PROP_FINAL_NO_PARAMS_DIALOG);
      boolean noParamsDialog = (b != null) ? b.booleanValue() : false;

      // Bei Bedarf den PrintParamsDialog anzeigen.
      if (noParamsDialog == false)
      {
        SyncActionListener s = new SyncActionListener();
        new PrintParametersDialog(model.doc, showCopiesSpinner, s);
        ActionEvent result = s.synchronize();

        // Rückgabewerte des Dialogs speichern für diesen und alle folgenden Aufrufe
        // von finalPrintWithProps()
        PrintParametersDialog ppd = (PrintParametersDialog) result.getSource();
        String actionCommand = result.getActionCommand();

        if (PrintParametersDialog.CMD_CANCEL.equals(actionCommand))
        {
          cancel();
          return;
        }
        setProperty(PROP_FINAL_COPY_COUNT, ppd.getCopyCount());
        setProperty(PROP_FINAL_PAGE_RANGE, ppd.getPageRange());
        setProperty(PROP_FINAL_NO_PARAMS_DIALOG, Boolean.TRUE);
      }

      Short copyCount = (Short) getProperty(PROP_FINAL_COPY_COUNT);
      if (copyCount == null) copyCount = Short.valueOf((short) 1);

      PageRange pageRange = (PageRange) getProperty(PROP_FINAL_PAGE_RANGE);
      if (pageRange == null) pageRange = new PageRange(PageRangeType.ALL, null);

      if (!print(pageRange, copyCount)) cancel();
    }

    /**
     * Druckt den Druckbereich pr des Dokuments copyCount mal auf dem aktuell
     * eingestellten Drucker aus und liefert true zurück, wenn das Drucken
     * erfolgreich war oder false, wenn Fehler auftraten.
     * 
     * @param pr
     *          beschreibt zu druckenden Seitenbereich
     * @param copyCount
     *          enthält die Anzahl der anzufertigenden Kopien
     * @return bei Erfolg true, sonst false.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private boolean print(PageRange pr, Short copyCount)
    {
      UnoProps myProps = new UnoProps("Wait", Boolean.TRUE);

      // Property "CopyCount" bestimmen:
      myProps.setPropertyValue("CopyCount", copyCount);

      // pr mit aktueller Seite vorbelegen (oder 1 als fallback)
      String prStr = "1";
      if (UNO.XPageCursor(model.getViewCursor()) != null)
        prStr = "" + UNO.XPageCursor(model.getViewCursor()).getPage();

      // Property "Pages" bestimmen:
      switch (pr.pageRangeType)
      {
        case ALL:
          // Property Pages muss hier nicht gesetzt werden, da Grundverhalten
          break;

        case USER_DEFINED:
          myProps.setPropertyValue("Pages", pr.pageRangeValue);
          break;

        case CURRENT_PAGE:
          myProps.setPropertyValue("Pages", prStr);
          break;

        case CURRENT_AND_FOLLOWING:
          myProps.setPropertyValue("Pages", prStr + "-" + model.getPageCount());
          break;
      }

      if (UNO.XPrintable(model.doc) != null) try
      {
        UNO.XPrintable(model.doc).print(myProps.getProps());
        return true;
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
      return false;
    }

    /**
     * synchronisiertes Setzen von props
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void setProperty(String prop, Object o)
    {
      synchronized (props)
      {
        props.put(prop, o);
      }
    }

    /**
     * Setzt den die Beschreibung des aktuellen Druckvorgangs auf stage und
     * aktualisiert die Anzeige in der PrintProgressBar (falls eine solche Leiste
     * angezeigt wird).
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void setStage(String stage)
    {
      currentStage = stage;
      if (printProgressBar != null) printProgressBar.setTitle(currentStage);
    }

    /**
     * synchronisiertes Auslesen von props
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private Object getProperty(String prop)
    {
      synchronized (props)
      {
        return props.get(prop);
      }
    }

    /**
     * Falls es sich bei dem zugehörigen Dokument um ein Formulardokument (mit einer
     * Formularbeschreibung) handelt, wird das Formularfeld mit der ID id auf den
     * neuen Wert value gesetzt und alle von diesem Formularfeld abhängigen
     * Formularfelder entsprechend angepasst. Handelt es sich beim zugehörigen
     * Dokument um ein Dokument ohne Formularbeschreibung, so werden nur alle
     * insertFormValue-Kommandos dieses Dokuments angepasst, die die ID id besitzen.
     * 
     * @param id
     *          Die ID des Formularfeldes, dessen Wert verändert werden soll. Ist die
     *          FormGUI aktiv, so werden auch alle von id abhängigen Formularwerte
     *          neu gesetzt.
     * @param value
     *          Der neue Wert des Formularfeldes id
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void setFormValue(String id, String value)
    {
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleSetFormValue(model.doc, id, value, s);
      s.synchronize();
    }

    /**
     * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit z.B.
     * die "Speichern?" Abfrage vor dem Schließen erscheint.
     * 
     * Manche Druckfunktionen verändern u.U. den Inhalt von Dokumenten. Trotzdem kann
     * es sein, dass eine solche Druckfunktion den "Modifiziert"-Status des Dokuments
     * nicht verändern darf um ungewünschte "Speichern?"-Abfragen zu verhindern. In
     * diesem Fall kann der "Modifiziert"-Status mit folgendem Konstrukt innerhalb
     * der Druckfunktion unverändert gehalten werden:
     * 
     * boolean modified = pmod.getDocumentModified();
     * 
     * ...die eigentliche Druckfunktion, die das Dokument verändert...
     * 
     * pmod.setDocumentModified(modified);
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
     */
    @Override
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
    @Override
    public void setDocumentModified(boolean modified)
    {
      // Keine WollMuxEvent notwendig, da keine WollMux-Datenstrukturen
      // angefasst werden.
      model.setDocumentModified(modified);
    }

    /**
     * Sammelt alle Formularfelder des Dokuments auf, die nicht von WollMux-Kommandos
     * umgeben sind, jedoch trotzdem vom WollMux verstanden und befüllt werden
     * (derzeit c,s,s,t,textfield,Database-Felder). So werden z.B. Seriendruckfelder
     * erkannt, die erst nach dem Öffnen des Dokuments manuell hinzugefügt wurden.
     */
    @Override
    public void collectNonWollMuxFormFields()
    {
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleCollectNonWollMuxFormFieldsViaPrintModel(model, s);
      s.synchronize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertySetInfo()
     */
    @Override
    public XPropertySetInfo getPropertySetInfo()
    {
      final HashSet<String> propsKeySet;
      synchronized (props)
      {
        propsKeySet = new HashSet<String>(props.keySet());
      }

      return new XPropertySetInfo()
      {
        @Override
        public boolean hasPropertyByName(String arg0)
        {
          return propsKeySet.contains(arg0);
        }

        @Override
        public Property getPropertyByName(String arg0)
            throws UnknownPropertyException
        {
          if (hasPropertyByName(arg0))
            return new Property(arg0, -1, Type.ANY, PropertyAttribute.OPTIONAL);
          else
            throw new UnknownPropertyException(arg0);
        }

        @Override
        public Property[] getProperties()
        {
          Property[] ps = new Property[propsKeySet.size()];
          int i = 0;
          for (String name : propsKeySet)
          {
            try
            {
              ps[i++] = getPropertyByName(name);
            }
            catch (UnknownPropertyException e)
            {}
          }
          return ps;
        }
      };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#setPropertyValue(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void setPropertyValue(String arg0, Object arg1)
        throws UnknownPropertyException, PropertyVetoException,
        IllegalArgumentException, WrappedTargetException
    {
      setProperty(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertyValue(java.lang.String)
     */
    @Override
    public Object getPropertyValue(String arg0) throws UnknownPropertyException,
        WrappedTargetException
    {
      Object o = getProperty(arg0);
      if (o != null)
        return o;
      else
        throw new UnknownPropertyException(arg0);
    }

    /**
     * Diese Komfortmethode verhält sich wie
     * c.s.s.b.XPropertySet.getPropertyValue([in] string propertyName), mit dem
     * Unterschied, dass sie keine Exceptions schmeißt und im Fehlerfall defaultValue
     * zurück liefert.
     */
    @Override
    public Object getProp(String propertyName, Object defaultValue)
    {
      try
      {
        return getPropertyValue(propertyName);
      }
      catch (Exception e)
      {
        return defaultValue;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#addPropertyChangeListener(java.lang.String,
     * com.sun.star.beans.XPropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
        throws UnknownPropertyException, WrappedTargetException
    {
    // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#removePropertyChangeListener(java.lang.String,
     * com.sun.star.beans.XPropertyChangeListener)
     */
    @Override
    public void removePropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
    // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#addVetoableChangeListener(java.lang.String,
     * com.sun.star.beans.XVetoableChangeListener)
     */
    @Override
    public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
        throws UnknownPropertyException, WrappedTargetException
    {
    // NOT IMPLEMENTED
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#removeVetoableChangeListener(java.lang.String,
     * com.sun.star.beans.XVetoableChangeListener)
     */
    @Override
    public void removeVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
    // NOT IMPLEMENTED
    }

    /**
     * Diese Methode setzt die Eigenschaften "Sichtbar" (visible) und die Anzeige der
     * Hintergrundfarbe (showHighlightColor) für alle Druckblöcke eines bestimmten
     * Blocktyps blockName (z.B. "AllVersions").
     * 
     * @param blockName
     *          Der Blocktyp dessen Druckblöcke behandelt werden sollen. Folgende
     *          Blocknamen werden derzeit unterstützt: "AllVersions", "DraftOnly",
     *          "OriginalOnly", "CopyOnly" und "NotInOriginal"
     * @param visible
     *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
     *          visible==false.
     * @param showHighlightColor
     *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
     *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
     *          ist).
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    @Override
    public void setPrintBlocksProps(String blockName, boolean visible,
        boolean showHighlightColor)
    {
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleSetPrintBlocksPropsViaPrintModel(model.doc,
        blockName, visible, showHighlightColor, s);
      s.synchronize();
    }

    /**
     * Setzt den Sichtbarkeitsstatus der Sichtbarkeitsgruppe groupID auf den neuen
     * Status visible und wirkt sich damit auf alle Dokumentkommandos
     * WM(CMD'setGroups'...) bzw. alle Textbereiche aus, die über eine
     * GROUPS-Zuordnung die Sichtbarkeitsgruppe groupId verknüpft haben.
     * 
     * @param groupID
     *          Name der Sichtbarkeitsgruppe, deren Sichtbarkeitsstatus verändert
     *          werden soll
     * @param visible
     *          Bei dem Wert true ist die Sichtbarkeitsgruppe sichtbar und bei false
     *          unsichtbar.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setGroupVisible(java.lang.String,
     *      boolean)
     */
    @Override
    public void setGroupVisible(String groupID, boolean visible)
    {
      SyncActionListener s = new SyncActionListener();
      WollMuxEventHandler.handleSetVisibleState(model, groupID, visible, s);
      s.synchronize();
    }

    /**
     * Liefert true, wenn der Druckvorgang aufgrund einer Benutzereingabe oder eines
     * vorangegangenen Fehlers abgebrochen wurde (siehe cancel()) und sollte
     * insbesonders von Druckfunktionen ausgewertet werden, die mehrmals
     * printWithProps() aufrufen und dabei aufwendige Vor- und Nacharbeiten leisten
     * müssen (die in diesem Fall sobald sinnvoll möglich eingestellt werden können).
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#isCanceled()
     */
    @Override
    public boolean isCanceled()
    {
      synchronized (isCanceled)
      {
        return isCanceled[0];
      }
    }

    /**
     * Setzt das Flag isCanceled() auf true und sorgt dafür, dass künftige Aufrufe
     * von printWithProps() sofort ohne Wirkung zurückkehren. Die Methode kann von
     * jeder Druckfunktion aufgerufen werden wenn Fehler auftreten oder der
     * Druckvorgang durch den Benutzer abgebrochen wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#cancel()
     */
    @Override
    public void cancel()
    {
      synchronized (isCanceled)
      {
        isCanceled[0] = true;
      }
    }

    /**
     * Diese Methode aktiviert die Anzeige der Fortschrittsleiste und initialisiert
     * die Anzahl der von dieser Druckfunktion zu erwartenden Versionen auf maxValue,
     * wenn maxValue größer 0 ist, oder entfernt die Druckfunktion aus der
     * Fortschrittsanzeige, wenn maxValue gleich 0 ist. Die Fortschrittsanzeige ist
     * prinzipiell in der Lage, den Druckstatus verschiedener verketteter
     * Druckfunktionen anzuzeigen. Die Berechnung der Gesamtausfertigungen und des
     * aktuellen Gesamtstatus wird von der Fortschrittsanzeige übernommen. Damit muss
     * jede Druckfunktion hier auch nur die Anzahl Versionen setzen, die von der
     * Druckfunktion selbst erzeugt werden.
     * 
     * @param maxValue
     *          den maximalen Wert der von dieser Druckfunktion zu druckenden
     *          Ausfertigungen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressMaxValue(short)
     */
    @Override
    public void setPrintProgressMaxValue(short maxValue)
    {
    // nicht auf das MasterPrintModel anwendbar, aber auf SlavePrintModels.
    }

    /**
     * Über diese Methode wird der Fortschrittsleiste ein neuer Fortschrittstatus
     * value (=Anzahl bis jetzt tatsächlich gedruckter Versionen) der aktuellen
     * Druckfunktion übermittelt. Der Wert value muss im Bereich 0 <= value <=
     * maxValue (siehe setPrintProgressMaxValue(maxValue)) liegen.
     * 
     * @param value
     *          Die Anzahl der bis jetzt tatsächlich von dieser Druckfunktion
     *          gedruckten Versionen. Es muss gelten: 0 <= value <= maxValue
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressValue(short)
     */
    @Override
    public void setPrintProgressValue(short value)
    {
    // nicht auf das MasterPrintModel anwendbar, aber auf SlavePrintModels.
    }

    /**
     * Registriert die durch key repräsentierte Druckfunktion mit dem Maximalwert
     * maxValue in der aktuellen Fortschrittsleiste; Ist bis jetzt noch keine
     * Fortschrittsleiste aktiv, so wird eine neue Fortschrittsleiste erzeugt und
     * aktiviert; ist maxValue==0, so wird die durch key repräsentierte Druckfunktion
     * deregistriert.
     * 
     * @param key
     *          repräsentiert eine Druckfunktion (oder genauer Ihr zugehöriges
     *          SlavePrintModel)
     * @param maxValue
     *          den Maximalwert von dieser Funktion zu erwartenden Versionen
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void setPrintProgressMaxValue(Object key, short maxValue)
    {
      if (printProgressBar == null && maxValue > 0)
      {
        printProgressBar = new PrintProgressBar(currentStage, new ActionListener()
        {
          @Override
          public void actionPerformed(ActionEvent e)
          {
            cancel();
          }
        });
      }

      if (printProgressBar != null) printProgressBar.setMaxValue(key, maxValue);
    }

    /**
     * Reicht den Fortschrittswert value der durch key ausgezeichneten Druckfunktion
     * an die Fortschrittsleiste weiter, wenn die Fortschrittsleiste aktiviert wurde
     * (dazu muss mindestens eine Druckfunktion mit setPrintProgressMaxValue(...)
     * registriert worden sein).
     * 
     * @param key
     *          repräsentiert die Druckfunktion (oder genauer ihr zugehöriges
     *          SlavePrintModel)
     * @param value
     *          enthält die Anzahl der von dieser Druckfunktion bereits erstellten
     *          Versionen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void setPrintProgressValue(Object key, short value)
    {
      if (printProgressBar != null) printProgressBar.setValue(key, value);
    }

    /**
     * Erlaubt die Anzeige eines eigenen Textes neben der Fortschrittsleiste.
     * Der Text wird einmalig angezeigt, d.h. wenn danach ein weiterer Aufruf
     * von setPrintProgressValue erfolgt, wird der Text wieder gelöscht und
     * durch den Fortschrittsstatus ersetzt.
     * 
     * @param value
     *          enthält den anzuzeigenden Text
     * 
     * @author Ignaz Forster (ITM-I23)
     */
    @Override
    public void setPrintMessage(String value)
    {
      if (printProgressBar != null) printProgressBar.setMessage(this, value);
    }
  }

  /**
   * Beim Aufruf einer einzelnen Druckfunktion wird dieser Druckfunktion ein
   * XPrintModel, repräsentiert durch das SlavePrintModel, übergeben. Für jede im
   * MaserPrintModel verwaltete und aufgerufene Druckfunktion existiert also genau
   * ein SlavePrintModel, das im wesentlichen alle Anfragen (Methodenaufrufe) an das
   * MasterPrintModel weiterleitet. Das SlavePrintModel kennt seine Position (idx) in
   * der Aufrufkette und sorgt vor allem dafür, dass beim Aufruf von printWithProps()
   * die nächste Druckfunktion der Aufrufkette gestartet wird.
   * 
   * Das SlavePrintModel ist von WeakBase abgeleitet, damit es in der Druckfunktion
   * mit den UNO-Mitteln inspiziert werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class SlavePrintModel extends WeakBase implements XPrintModel,
      InternalPrintModel
  {
    private int idx;

    private MasterPrintModel master;

    /**
     * Enthält die Beschreibung des Druckvorgangs, der in diesem SlavePrintModel
     * bearbeitet wird.
     */
    private String stage;

    /**
     * Erzeugt ein neues SlavePrintModel, das in der Aufrufkette, die durch das
     * MasterPrintModel master verwaltet wird, an der Stelle idx steht.
     * 
     * @param master
     *          Das MasterPrintModel, an das die meisten Anfragen weitergeleitet
     *          werden und das die Aufrufkette der Druckfunktionen verwaltet.
     * @param idx
     *          Die Position der zu diesem SlavePrintModel zugehörigen Druckfunktion
     *          in der Aufrufkette von master.
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
    @Override
    public XTextDocument getTextDocument()
    {
      return master.getTextDocument();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    @Override
    public void print(short numberOfCopies)
    {
      for (int i = 0; i < numberOfCopies && !isCanceled(); ++i)
        printWithProps();
    }

    /**
     * Diese Methode ist die wichtigste Methode im SlavePrintModel, denn sie sorgt
     * dafür, dass beim Aufruf von PrintWithProps die Weiterleitung an die nächste
     * Druckfunktion der Aufrufkette veranlasst wird.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
     */
    @Override
    public void printWithProps()
    {
      if (isCanceled()) return;

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
        master.setPrintProgressMaxValue(pmod, (short) 0);
      }
      else
      {
        master.finalPrint();
      }
      if (stage != null) master.setStage(stage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void setFormValue(String arg0, String arg1)
    {
      master.setFormValue(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
     */
    @Override
    public boolean getDocumentModified()
    {
      return master.getDocumentModified();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setDocumentModified(boolean)
     */
    @Override
    public void setDocumentModified(boolean arg0)
    {
      master.setDocumentModified(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#collectNonWollMuxFormFields()
     */
    @Override
    public void collectNonWollMuxFormFields()
    {
      master.collectNonWollMuxFormFields();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintBlocksProps(java.lang.String
     * , boolean, boolean)
     */
    @Override
    public void setPrintBlocksProps(String arg0, boolean arg1, boolean arg2)
    {
      master.setPrintBlocksProps(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertySetInfo()
     */
    @Override
    public XPropertySetInfo getPropertySetInfo()
    {
      return master.getPropertySetInfo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#setPropertyValue(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void setPropertyValue(String key, Object val)
        throws UnknownPropertyException, PropertyVetoException,
        IllegalArgumentException, WrappedTargetException
    {
      if (STAGE.equalsIgnoreCase(key))
      {
        if (val != null)
        {
          stage = val.toString();
          master.setStage(stage);
        }
      }
      else
        master.setPropertyValue(key, val);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.beans.XPropertySet#getPropertyValue(java.lang.String)
     */
    @Override
    public Object getPropertyValue(String arg0) throws UnknownPropertyException,
        WrappedTargetException
    {
      return master.getPropertyValue(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getProp(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public Object getProp(String arg0, Object arg1)
    {
      return master.getProp(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#addPropertyChangeListener(java.lang.String,
     * com.sun.star.beans.XPropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
        throws UnknownPropertyException, WrappedTargetException
    {
      master.addPropertyChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#removePropertyChangeListener(java.lang.String,
     * com.sun.star.beans.XPropertyChangeListener)
     */
    @Override
    public void removePropertyChangeListener(String arg0,
        XPropertyChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.removePropertyChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#addVetoableChangeListener(java.lang.String,
     * com.sun.star.beans.XVetoableChangeListener)
     */
    @Override
    public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
        throws UnknownPropertyException, WrappedTargetException
    {
      master.addVetoableChangeListener(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.star.beans.XPropertySet#removeVetoableChangeListener(java.lang.String,
     * com.sun.star.beans.XVetoableChangeListener)
     */
    @Override
    public void removeVetoableChangeListener(String arg0,
        XVetoableChangeListener arg1) throws UnknownPropertyException,
        WrappedTargetException
    {
      master.removeVetoableChangeListener(arg0, arg1);
    }

    /**
     * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es, dass
     * nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als der
     * ORDER-Wert der aktuellen Druckfunktion ist.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
     */
    @Override
    public void usePrintFunction(String functionName) throws NoSuchMethodException
    {
      PrintFunction newFunc =
          GlobalFunctions.getInstance().getGlobalPrintFunctions().get(functionName);
      if (newFunc != null)
        useInternalPrintFunction(newFunc);
      else
        throw new NoSuchMethodException(L.m("Druckfunktion '%1' nicht definiert.",
          functionName));
    }

    /**
     * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es, dass
     * nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als der
     * ORDER-Wert der aktuellen Druckfunktion ist.
     * 
     * @see de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel#useInternalPrintFunction(de.muenchen.allg.itd51.wollmux.print.PrintFunction)
     */
    @Override
    public boolean useInternalPrintFunction(PrintFunction function)
    {
      if (function != null)
      {
        PrintFunction currentFunc = master.getPrintFunction(idx);
        if (function.compareTo(currentFunc) <= 0)
        {
          Logger.error(L.m(
            "Druckfunktion '%1' muss einen höheren ORDER-Wert besitzen als die Druckfunktion '%2'",
            function.getFunctionName(), currentFunc.getFunctionName()));
          return false;
        }
        else
          return master.useInternalPrintFunction(function);
      }
      else
      {
        Logger.error(L.m("Die angeforderte interne Druckfunktion ist ungültig."));
        return false;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.XPrintModel#setGroupVisible(java.lang.String,
     * boolean)
     */
    @Override
    public void setGroupVisible(String arg0, boolean arg1)
    {
      master.setGroupVisible(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#isCanceled()
     */
    @Override
    public boolean isCanceled()
    {
      return master.isCanceled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#cancel()
     */
    @Override
    public void cancel()
    {
      master.cancel();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressMaxValue(short)
     */
    @Override
    public void setPrintProgressMaxValue(short maxValue)
    {
      master.setPrintProgressMaxValue(this, maxValue);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressValue(short)
     */
    @Override
    public void setPrintProgressValue(short value)
    {
      master.setPrintProgressValue(this, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintMessage(string)
     */
    @Override
    public void setPrintMessage(String value)
    {
      master.setPrintMessage(value);
    }
  }

  /**
   * Setzt die Beschreibung des aktuellen Druckbearbeitungsvorgangs für das
   * XPrintModel pmod auf stage
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void setStage(XPrintModel pmod, String stage)
  {
    if (pmod == null) return;
    try
    {
      pmod.setPropertyValue(STAGE, stage);
    }
    catch (Exception e)
    {
      Logger.error(L.m("Kann Stage nicht auf '%1' setzen", stage), e);
    }
  }
}
