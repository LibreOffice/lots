package de.muenchen.allg.itd51.wollmux.func;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

public class StandardPrint
{
  
  
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  public static void printVerfuegungspunktTest(XPrintModel pmod)
  {
    pmod.printVerfuegungspunkt((short) 1, (short) 1, false, true, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 2, (short) 1, false, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 3, (short) 1, false, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
    pmod.printVerfuegungspunkt((short) 4, (short) 1, true, false, TextDocumentModel.PAGE_RANGE_TYPE_ALL, "");
  }

  public static void myTestPrintFunction(XPrintModel pmod)
  {
    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", "Hallo, ich bin's");
    pmod.setFormValue("SGAnrede", "Herr");
    pmod.setFormValue("AbtAnteile", "true");
    pmod.print((short)1);

    pmod.setFormValue("EmpfaengerZeile1", "Noch eine Empfängerzeile");
    pmod.setFormValue("SGAnrede", "Frau");
    pmod.setFormValue("AbtAnteile", "false");
    pmod.setFormValue("AbtKaution", "true");
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
  
  
}
