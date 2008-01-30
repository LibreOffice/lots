package de.muenchen.allg.itd51.wollmux.func;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.PrintModelProps;

public class StandardPrint
{  
  
  /**
   * GUI der Sachleitenden Verfügungen: Diese Komfortdruckfunktion erzeugt die
   * GUI, mit deren Hilfe die Steuerdaten (in Form der Properties "SLV_*") für
   * den Druck der Sachleitenden Verfügungen festgelegt werden können und leitet
   * den Druck mittels pmod.printWithProps() an die nächste Druckfunktion der
   * Aufrufkette (kann z.B. Seriendruck sein) weiter. Damit die SLVs
   * letztendlich auch wirklich in den benötigten Ausfertigungen gedruckt
   * werden, lädt die Druckfunktion noch das Ausgabemodul
   * "SachleitendeVerfuegungOutput" zur Liste der auszuführenden Druckfunktionen
   * hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    // Druckfunktion SachleitendeVerfuegungOutput für SLV-Ausgabe hinzuladen:
    if (!pmod.usePrintFunction("SachleitendeVerfuegungOutput"))
    {
      String method = "sachleitendeVerfuegungOutput";
      int order = 150;
      PrintFunction func = getInternalPrintFunction(method, order);
      if (pmod instanceof InternalPrintModel) {
        Logger.debug("Verwende interne Druckfunktion '" + method + "' mit ORDER-Wert '" + order + "' als Fallback.");
        ((InternalPrintModel) pmod).useInternalPrintFunction(func);
      }
    }

    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  /**
   * Ausgabemodul der Sachleitenden Verfügungen: Diese Komfortdruckfunktion
   * druckt die Verfügungspunkte aus, die in der Property "SLV_verfPunkte"
   * angegeben sind; dabei werden auch die anderen Steuerdaten in Form der
   * Properties "SLV_*" entsprechend für jeden Verfügungspunkt ausgewertet und
   * der Druck über pmod.printWithProps() gestartet.
   * 
   * So ist es z.B. möglich, die Steuerdaten für den SLV-Druck in einem
   * vorgeschalteten Dialog zu erzeugen und mit dieser Komfortdruckfunktion die
   * Ausfertigungen drucken zu lassen. Es ist aber auch möglich, die Steuerdaten
   * in einem nicht interaktiven Druckermodul zu belegen.
   * 
   * Diese Komfortdruckfunktion überschreibt die Properties "CopyCount",
   * "PageRangeType" und "PageRangeValue"
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegungOutput(XPrintModel pmod)
  {
    try
    {
      Object[] verfPunkte = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_VERF_PUNKTE);
      Object[] isDraftFlags = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_IS_DRAFT_FLAGS);
      Object[] isOriginalFlags = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_IS_ORIGINAL_FLAGS);
      Object[] pageRangeTypes = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_PAGE_RANGE_TYPES);
      Object[] pageRangeValues = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_PAGE_RANGE_VALUES);
      Object[] copyCounts = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_COPY_COUNTS);

      for (int i = 0; i < verfPunkte.length; i++)
      {
        short copyCount = AnyConverter.toShort(copyCounts[i]);
        if (copyCount > 0)
        {
          pmod.setPropertyValue(PrintModelProps.PROP_PAGE_RANGE_TYPE, pageRangeTypes[i]);
          pmod.setPropertyValue(PrintModelProps.PROP_PAGE_RANGE_VALUE, pageRangeValues[i]);
          pmod.setPropertyValue(PrintModelProps.PROP_COPY_COUNT, copyCounts[i]);

          short verfPunkt = AnyConverter.toShort(verfPunkte[i]);
          boolean isDraft = AnyConverter.toBoolean(isDraftFlags[i]);
          boolean isOriginal = AnyConverter.toBoolean(isOriginalFlags[i]);

          SachleitendeVerfuegung.printVerfuegungspunkt(pmod, verfPunkt, isDraft, isOriginal);
        }
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * mit dieser Komfortdruckfuntion habe ich getestet, ob die Parameterübergabe
   * bei Druckfunktionen (arg als ConfigThingy) funktioniert und ob pmod sich
   * über die UNO-Mechanismen korrekt inspizieren lässt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void myTestPrintFunction(XPrintModel pmod, Object arg)
  {
    ConfigThingy conf = new ConfigThingy("ARG");
    if(arg != null && arg instanceof ConfigThingy) conf = (ConfigThingy) arg;
    
    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", conf.stringRepresentation());
    pmod.print((short)1);
    
    new UnoService(pmod).msgboxFeatures();
  }

  /**
   * Druckt das zu pmod gehörende Dokument für jeden Datensatz der aktuell über
   * Bearbeiten/Datenbank austauschen eingestellten Tabelle einmal aus.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithoutSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, false);
  }

  /**
   * Druckt das zu pmod gehörende Dokument für die Datensätze, die der Benutzer in einem Dialog
   * auswählt. Für die Anzeige der Datensätze im Dialog wird die Spalte "WollMuxDescription"
   * verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist und "1", "ja" oder "true"
   * enthält, so ist der entsprechende Datensatz in der Auswahlliste bereits vorselektiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, true);
  }
  
  /**
   * Startet den ultimativen MailMerge für pmod.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    MailMerge.superMailMerge(pmod);
  }

  /**
   * Erzeugt eine interne Druckfunktion, die auf die in dieser Klasse definierte
   * Methode mit dem Namen functionName verweist und den Order-Wert order
   * besitzt.
   * 
   * @param functionName
   *          Enthält den Namen einer in dieser Klasse definierten
   *          Standard-Druckfunktion
   * @param order
   *          enthält den Order-Wert, der für die bestimmung der Reihenfolge der
   *          Ausführung benötigt wird.
   * @return die neue Druckfunktion oder null, wenn die Funktion nicht definiert
   *         ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static PrintFunction getInternalPrintFunction(
      final String functionName, int order)
  {
    ConfigThingy conf = new ConfigThingy("EXTERN");
    ConfigThingy url = new ConfigThingy("URL");
    url.addChild(new ConfigThingy("java:" + StandardPrint.class.getName() + "." + functionName));
    conf.addChild(url);
    try
    {
      return new PrintFunction(conf, functionName, order);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error("Interne Druckfunktion '" + functionName + "' nicht definiert!", e);
      return null;
    }
  }

  /**
   * Hängt das zu pmod gehörige TextDocument an das im Property
   * PrintIntoFile_OutputDocument gespeicherte XTextDocument an. Falls
   * noch kein solches Property existiert, wird ein leeres Dokument angelegt.
   * @throws Exception falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printIntoFile(XPrintModel pmod) throws Exception
  {
    boolean firstAppend = true;
    XTextDocument outputDoc = null;
    try
    {
      outputDoc = UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_OutputDocument"));
      firstAppend = false;
    }
    catch (UnknownPropertyException e)
    {
      outputDoc = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));
      pmod.setPropertyValue("PrintIntoFile_OutputDocument", outputDoc);
    }
    
    PrintIntoFile.appendToFile(outputDoc, pmod.getTextDocument(), firstAppend);
  }
  
  
}
