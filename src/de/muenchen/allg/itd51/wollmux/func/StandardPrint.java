package de.muenchen.allg.itd51.wollmux.func;

import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

public class StandardPrint
{
  public static void sachleitendeVerfuegung(XPrintModel pmod) {
    Logger.debug("StandardPrint.sachleitendeVerfuegung - started");
    pmod.print((short)4);
    Logger.debug("StandardPrint.sachleitendeVerfuegung - finished");
  }
}
