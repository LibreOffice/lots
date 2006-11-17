package de.muenchen.allg.itd51.wollmux.func;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

public class StandardPrint
{
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  public static void printVerfuegungspunktTest(XPrintModel pmod)
  {
    pmod.printVerfuegungspunkt((short) 1, (short) 1, false, true);
    pmod.printVerfuegungspunkt((short) 2, (short) 1, false, false);
    pmod.printVerfuegungspunkt((short) 3, (short) 1, false, false);
    pmod.printVerfuegungspunkt((short) 4, (short) 1, true, false);
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
}
