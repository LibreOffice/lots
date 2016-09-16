/*
 * Copyright (c) 2011-2015 Landeshauptstadt München
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
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 *            |     | Erstellung
 * 08.05.2012 | jub | mailMergeNewToEMail() getrennt in mailMergeNewToODTEMail() und
 *                    mailMergeNewToPDFEMail(): sowohl in der enummeration der 
 *                    druckfunktionen, als auch als mehthoden;
 *                    aufruf von saveToFile() zur unterscheidung mit pdf/odt flage.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @author Christoph Lutz (D-III-ITD-5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.print.MailMerge;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge;
import de.muenchen.allg.itd51.wollmux.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;
import de.muenchen.allg.itd51.wollmux.print.PrintIntoFile;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge.OutputType;

public class StandardPrint
{
  /**
   * Unter diesem Key werden in den Properties eines XPrintModels die Einstellungen
   * zu den Sachleitenden Verfügungen als Objekte vom Typ List<VerfuegungspunktInfo>
   * abgelegt, die von der Druckfunktion SachleitendeVerfuegungOutput wieder
   * ausgelesen werden.
   */
  public static final String PROP_SLV_SETTINGS = "SLV_Settings";

  /**
   * GUI der Sachleitenden Verfügungen: Diese Komfortdruckfunktion erzeugt die GUI,
   * mit deren Hilfe die Steuerdaten (in Form der Properties "SLV_SettingsFromGUI")
   * für den Druck der Sachleitenden Verfügungen festgelegt werden können und leitet
   * den Druck mittels pmod.printWithProps() an die nächste Druckfunktion der
   * Aufrufkette (kann z.B. Seriendruck sein) weiter. Damit die SLVs letztendlich
   * auch wirklich in den benötigten Ausfertigungen gedruckt werden, lädt die
   * Druckfunktion noch das Ausgabemodul "SachleitendeVerfuegungOutput" zur Liste der
   * auszuführenden Druckfunktionen hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    // Druckfunktion SachleitendeVerfuegungOutput für SLV-Ausgabe hinzuladen:
    try
    {
      pmod.usePrintFunction(InternalPrintFunction.SachleitendeVerfuegungOutput.name());
    }
    catch (NoSuchMethodException e)
    {
      Logger.error(e);
      pmod.cancel();
      return;
    }

    List<VerfuegungspunktInfo> settings =
      SachleitendeVerfuegung.callPrintDialog(pmod.getTextDocument());
    if (settings != null)
    {
      try
      {
        pmod.setPropertyValue(PROP_SLV_SETTINGS, settings);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
        pmod.cancel();
        return;
      }
      pmod.printWithProps();
    }
  }

  /**
   * Ausgabemodul der Sachleitenden Verfügungen: Diese Komfortdruckfunktion druckt
   * die Verfügungspunkte aus, die über die GUI ausgewählt wurden. Dabei wird für
   * jeden Verfügungspunkt die Methode printVerfuegungspunkt(...) ausgeführt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  @SuppressWarnings("unchecked")
  public static void sachleitendeVerfuegungOutput(XPrintModel pmod)
  {
    List<VerfuegungspunktInfo> settings = new ArrayList<VerfuegungspunktInfo>();
    try
    {
      settings =
        (List<VerfuegungspunktInfo>) pmod.getPropertyValue(PROP_SLV_SETTINGS);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    short countMax = 0;
    for (VerfuegungspunktInfo v : settings)
      countMax += v.copyCount;
    pmod.setPrintProgressMaxValue(countMax);

    short count = 0;
    for (VerfuegungspunktInfo v : settings)
    {
      if (pmod.isCanceled()) return;
      if (v.copyCount > 0)
      {
        SachleitendeVerfuegung.printVerfuegungspunkt(pmod, v.verfPunktNr, v.isDraft,
          v.isOriginal, v.copyCount);
      }
      count += v.copyCount;
      pmod.setPrintProgressValue(count);
    }
  }

  /**
   * mit dieser Komfortdruckfuntion habe ich getestet, ob die Parameterübergabe bei
   * Druckfunktionen (arg als ConfigThingy) funktioniert und ob pmod sich über die
   * UNO-Mechanismen korrekt inspizieren lässt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void myTestPrintFunction(XPrintModel pmod, Object arg)
  {
    ConfigThingy conf = new ConfigThingy("ARG");
    if (arg != null && arg instanceof ConfigThingy) conf = (ConfigThingy) arg;

    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", conf.stringRepresentation());
    pmod.print((short) 1);

    new UnoService(pmod).msgboxFeatures();
  }

  /**
   * Druckt das zu pmod gehörende Dokument für jeden Datensatz der aktuell über
   * Bearbeiten/Datenbank austauschen eingestellten Tabelle einmal aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeWithoutSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, false);
  }

  /**
   * Druckt das zu pmod gehörende Dokument für die Datensätze, die der Benutzer in
   * einem Dialog auswählt. Für die Anzeige der Datensätze im Dialog wird die Spalte
   * "WollMuxDescription" verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist
   * und "1", "ja" oder "true" enthält, so ist der entsprechende Datensatz in der
   * Auswahlliste bereits vorselektiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeWithSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, true);
  }

  /**
   * Startet den ultimativen MailMerge für pmod.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    MailMerge.superMailMerge(pmod);
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und
   * die Seriendruckfelder im Dokument entsprechend setzt. Siehe
   * {@link MailMergeNew#mailMergeNewSetFormValue(XPrintModel)}.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  public static void mailMergeNewSetFormValue(final XPrintModel pmod)
      throws Exception
  {
    MailMergeNew.mailMergeNewSetFormValue(pmod, null);
  }

  /**
   * Druckfunktion zum Aufruf des Seriendrucks über den OOo-MailMergeService.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void oooMailMergeToOdtFile(final XPrintModel pmod) throws Exception
  {
    // Meldung, wenn die maximal verarbeitbare Größe der Selection überschritten ist.
    Integer maxProcessableDatasets =
      Workarounds.workaroundForTDFIssue89783(pmod.getTextDocument());
    if (maxProcessableDatasets != null
      && MailMergeNew.mailMergeNewGetSelectionSize(pmod) > maxProcessableDatasets)
    {
      ModalDialogs.showInfoModal(
        L.m("WollMux-Seriendruck Fehler"),
        L.m(
          "Bei diesem Seriendruck-Hauptdokument kann Ihre aktuelle Office-Version maximal %1 Datensätze verarbeiten. "
            + "Bitte schränken Sie die Anzahl der Datensätze im Druckdialog unter 'folgende Datensätze verwenden' entsprechend ein!",
          maxProcessableDatasets));
      pmod.cancel();
      return;
    }
    
    OOoBasedMailMerge.oooMailMerge(pmod, OOoBasedMailMerge.OutputType.toShell);
  }

  /**
   * Druckfunktion zum Aufruf des Seriendrucks über den OOo-MailMergeService.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void oooMailMergeToPrinter(final XPrintModel pmod) throws Exception
  {
    OOoBasedMailMerge.oooMailMerge(pmod, OOoBasedMailMerge.OutputType.toPrinter);
  }

  /**
   * Druckfunktion zum Abspeichern der durch den Seriendruck erzeugten Daten in
   * einzelnen Dateien.
   * 
   * @author Ignaz Forster (D-III-ITD-D102)
   * @throws Exception
   */
  public static void mailMergeNewToSingleODT(final XPrintModel pmod)
      throws Exception
  {
    boolean isODT = true;
    MailMergeNew.saveToFile(pmod, isODT);
  }
  
  /**
   * Druckfunktion zum Abspeichern der durch den Seriendruck erzeugten Daten in
   * einzelnen Dateien.
   * 
   * @author Ignaz Forster (D-III-ITD-D102)
   * @throws Exception
   */
  public static void mailMergeNewToSinglePDF(final XPrintModel pmod)
      throws Exception
  {
    boolean isODT = false;
    MailMergeNew.saveToFile(pmod, isODT);
  }

  /**
   * <b>DEPRECATED</b>: Druckfunktion zum Versenden der durch den Seriendruck erzeugten odt Dokumente per
   * E-Mail. Funktion existiert noch für die Kompatibilität zu alten Konfigurationen. Zukünftig werden 
   * <b>"mailMergeNewToODTEMail"</b> bzw. <b>"mailMergeNewToPDFEMail"</b> diese Funktion ersetzen.
   * 
   * @author Stefan Ströbl (ITM-B17)
   */
  public static void mailMergeNewToEMail(final XPrintModel pmod)
  {
    //TODO Funktion entfernen, wenn die Standard-Config 13.X schon länger (~2 Jahre) im Einsatz ist
    boolean isODT = true;
    MailMergeNew.sendAsEmail(pmod, isODT);
  }
  
  /**
   * Druckfunktion zum Versenden der durch den Seriendruck erzeugten odt Dokumente per
   * E-Mail
   * 
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static void mailMergeNewToODTEMail(final XPrintModel pmod)
  {
    boolean isODT = true;
    MailMergeNew.sendAsEmail(pmod, isODT);
  }
  
  /**
   * Druckfunktion zum Versenden der durch den Seriendruck erzeugten pdf Dokumente per
   * E-Mail
   * 
   * @author judith baur (D-III-ITD-D102)
   */
  public static void mailMergeNewToPDFEMail(final XPrintModel pmod)
  {
    boolean isODT = false;
    MailMergeNew.sendAsEmail(pmod, isODT);
  }
  
  /**
   * Hängt das zu pmod gehörige TextDocument an das im Property
   * PrintIntoFile_OutputDocument gespeicherte XTextDocument an. Falls noch kein
   * solches Property existiert, wird ein leeres Dokument angelegt.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printIntoFile(final XPrintModel pmod) throws Exception
  {
    XTextDocument outputDoc = null;
    try
    {
      outputDoc =
        UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_OutputDocument"));
    }
    catch (UnknownPropertyException e)
    {
      outputDoc = createNewTargetDocument(pmod, false);
    }

    boolean firstAppend = true;
    try
    {
      XTextDocument previousDoc =
        UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_PreviousOutputDocument"));

      /*
       * It is important to do the firstAppend check via this comparison as opposed
       * to just storing a boolean property. This is because in the case of a mail
       * merge with multiple output documents this print function will be called
       * several times with the same PrintModel but different documents in the
       * PrintIntoFile_OutputDocument property.
       */
      firstAppend = !(UnoRuntime.areSame(outputDoc, previousDoc));
    }
    catch (UnknownPropertyException e)
    {}

    PrintIntoFile.appendToFile(outputDoc, pmod.getTextDocument(), firstAppend);

    if (firstAppend)
      pmod.setPropertyValue("PrintIntoFile_PreviousOutputDocument", outputDoc);
  }

  /**
   * Erzeugt abhängig von hidden ein sichtbares oder unsichtbares neues leeres
   * Dokument für {@link PrintIntoFile} und setzt die entsprechenden Properties von
   * pmod, damit das Dokument verwendet wird.
   * 
   * @param pmod
   *          Das XPrintModel in dem die Property gesetzt wird
   * @param hidden
   *          wenn hidden==true ist, wird das Dokument unsichtbar erzeugt.
   * 
   * @return das erzeugte neue Zieldokument.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static XTextDocument createNewTargetDocument(final XPrintModel pmod,
      boolean hidden) throws Exception
  {
    XTextDocument outputDoc =
      UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true,
        true, hidden));
    pmod.setPropertyValue("PrintIntoFile_OutputDocument", outputDoc);

    return outputDoc;
  }

  /**
   * Enthält alle internen Druckfunktionen dieser Klasse, die als
   * Default-Druckfunktionen verwendet werden, wenn sie nicht sowieso in der
   * Konfiguration definiert sind.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private enum InternalPrintFunction {
    SachleitendeVerfuegung("sachleitendeVerfuegung", 50),
  
    MailMergeNewSetFormValue("mailMergeNewSetFormValue", 75),
  
    SachleitendeVerfuegungOutput("sachleitendeVerfuegungOutput", 150),
  
    MailMergeNewToSingleODT("mailMergeNewToSingleODT", 200),
    
    MailMergeNewToSinglePDF("mailMergeNewToSinglePDF", 200),
  
    MailMergeNewToODTEMail("mailMergeNewToODTEMail", 200),
    
    MailMergeNewToPDFEMail("mailMergeNewToPDFEMail", 200),
  
    OOoMailMergeToPrinter("oooMailMergeToPrinter", 200),
  
    OOoMailMergeToOdtFile("oooMailMergeToOdtFile", 200);
  
    private String intMethodName;
  
    private int order;
  
    InternalPrintFunction(String intMethodName, int order)
    {
      this.intMethodName = intMethodName;
      this.order = order;
    }
  
    /**
     * Erzeugt die interne Druckfunktion
     * 
     * @return die neue Druckfunktion oder null, wenn die Funktion nicht definiert
     *         ist.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public PrintFunction createPrintFunction()
    {
      ConfigThingy conf = new ConfigThingy("EXTERN");
      ConfigThingy url = new ConfigThingy("URL");
      url.addChild(new ConfigThingy("java:" + StandardPrint.class.getName() + "."
        + intMethodName));
      conf.addChild(url);
      try
      {
        return new PrintFunction(conf, intMethodName, order);
      }
      catch (ConfigurationErrorException e)
      {
        Logger.error(L.m("Interne Druckfunktion '%1' nicht definiert!",
          intMethodName), e);
        return null;
      }
    }
  }

  /**
   * Diese Methode fügt der PrintFunctionLibrary funcLib alle in dieser Klasse
   * definierten Druckfunktionen hinzu, wenn sie nicht bereits in funcLib enthalten
   * sind.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void addInternalDefaultPrintFunctions(PrintFunctionLibrary funcLib)
  {
    Set<String> names = funcLib.getFunctionNames();
    for (InternalPrintFunction f : InternalPrintFunction.values())
      if (!names.contains(f.name()))
      {
        funcLib.add(f.name(), f.createPrintFunction());
        Logger.debug(L.m("Registriere interne Druckfunktion %1 als Fallback",
          f.name()));
      }
  }
}
