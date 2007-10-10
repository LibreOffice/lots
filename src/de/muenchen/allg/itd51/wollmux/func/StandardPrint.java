package de.muenchen.allg.itd51.wollmux.func;

import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

public class StandardPrint
{  
  
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    // Druckfunktion SachleitendeVerfuegungOutput für SLV-Ausgabe hinzuladen:
    if(! pmod.usePrintFunction("SachleitendeVerfuegungOutput")) {
      String method = "sachleitendeVerfuegungOutput";
      int order = 150;
      Logger.debug("Verwende interne Druckfunktion '" + method + "' mit ORDER-Wert '" + order + "' als Fallback.");
      pmod.useInternalPrintFunction(getInternalPrintFunction(method, order));
    }

    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  public static void sachleitendeVerfuegungOutput(XPrintModel pmod)
  {
    try
    {
      Object[] verfPunkte = (Object[]) pmod.getPropertyValue("SLV_verfPunkte");
      Object[] isDraftFlags = (Object[]) pmod.getPropertyValue("SLV_isDraftFlags");
      Object[] isOriginalFlags = (Object[]) pmod.getPropertyValue("SLV_isOriginalFlags");
      Object[] pageRangeTypes = (Object[]) pmod.getPropertyValue("SLV_PageRangeTypes");
      Object[] pageRangeValues = (Object[]) pmod.getPropertyValue("SLV_PageRangeValues");
      Object[] copyCounts = (Object[]) pmod.getPropertyValue("SLV_CopyCounts");

      for (int i = 0; i < verfPunkte.length; i++)
      {
        short verfPunkt = AnyConverter.toShort(verfPunkte[i]);
        boolean isDraft = AnyConverter.toBoolean(isDraftFlags[i]);
        boolean isOriginal = AnyConverter.toBoolean(isOriginalFlags[i]);
        short pageRangeType = AnyConverter.toShort(pageRangeTypes[i]);
        String pageRangeValue = AnyConverter.toString(pageRangeValues[i]);
        short copyCount = AnyConverter.toShort(copyCounts[i]);

        pmod.printVerfuegungspunkt(verfPunkt, copyCount, isDraft, isOriginal, pageRangeType, pageRangeValue);
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  public static void printVerfuegungspunktTest(XPrintModel pmod)
  {
    pmod.printVerfuegungspunkt((short) 1, (short) 1, false, true, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 2, (short) 1, false, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 3, (short) 1, false, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 4, (short) 1, true, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
  }

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
  private static PrintFunction getInternalPrintFunction(
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
  
  
}
