/*
 * Copyright (c) 2011 Landeshauptstadt München
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
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @author Christoph Lutz (D-III-ITD-5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.io.IOException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

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
      pmod.usePrintFunction("SachleitendeVerfuegungOutput");
    }
    catch (NoSuchMethodException e)
    {
      String method = "sachleitendeVerfuegungOutput";
      int order = 150;
      PrintFunction func = getInternalPrintFunction(method, order);
      if (pmod instanceof InternalPrintModel)
      {
        Logger.debug(L.m(
          "Verwende interne Druckfunktion '%1' mit ORDER-Wert '%2' als Fallback.",
          method, order));
        ((InternalPrintModel) pmod).useInternalPrintFunction(func);
      }
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
    OOoBasedMailMerge.oooMailMerge(pmod);
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
    OOoBasedMailMerge.oooMailMerge(pmod);
  }

  /**
   * Druckfunktion zum Abspeichern der durch den Seriendruck erzeugten Daten in
   * einzelnen Dateien.
   * 
   * @author Ignaz Forster (D-III-ITD-D102)
   * @throws Exception
   */
  public static void mailMergeNewSingleODF(final XPrintModel pmod) throws Exception
  {
    MailMergeNew.saveToODF(pmod);
  }

  /**
   * Druckfunktion zum Versenden der durch den Seriendruck erzeugten Dokumente per
   * E-Mail
   * 
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static void mailMergeNewEMail(final XPrintModel pmod)
  {
    MailMergeNew.sendAsEmail(pmod);
  }

  /**
   * Erzeugt eine interne Druckfunktion, die auf die in dieser Klasse definierte
   * Methode mit dem Namen functionName verweist und den Order-Wert order besitzt.
   * 
   * @param functionName
   *          Enthält den Namen einer in dieser Klasse definierten
   *          Standard-Druckfunktion
   * @param order
   *          enthält den Order-Wert, der für die bestimmung der Reihenfolge der
   *          Ausführung benötigt wird.
   * @return die neue Druckfunktion oder null, wenn die Funktion nicht definiert ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static PrintFunction getInternalPrintFunction(final String functionName,
      int order)
  {
    ConfigThingy conf = new ConfigThingy("EXTERN");
    ConfigThingy url = new ConfigThingy("URL");
    url.addChild(new ConfigThingy("java:" + StandardPrint.class.getName() + "."
      + functionName));
    conf.addChild(url);
    try
    {
      return new PrintFunction(conf, functionName, order);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(L.m("Interne Druckfunktion '%1' nicht definiert!", functionName),
        e);
      return null;
    }
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
}
