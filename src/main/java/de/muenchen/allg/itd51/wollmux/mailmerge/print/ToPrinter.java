package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.OOoBasedMailMerge.OutputType;

/**
 * Print function for mailmerge by LibreOffice mailmerge with type {@link OutputType#toPrinter}.
 */
public class ToPrinter extends PrintFunction
{
  /**
   * A {@link PrintFunction} with name "OOoMailMergeToPrinter" and order 200.
   */
  public ToPrinter()
  {
    super("OOoMailMergeToPrinter", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    OOoBasedMailMerge.oooMailMerge(printModel, OOoBasedMailMerge.OutputType.toPrinter);
  }

}
