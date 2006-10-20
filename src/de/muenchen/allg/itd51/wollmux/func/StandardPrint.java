package de.muenchen.allg.itd51.wollmux.func;

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
    pmod.printVerfuegungspunkt((short) 2, (short) 1, false, true);
    pmod.printVerfuegungspunkt((short) 3, (short) 1, false, true);
    pmod.printVerfuegungspunkt((short) 4, (short) 1, true, true);
  }
}
