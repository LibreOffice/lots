package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge.OutputType;

/**
 * Print function for mailmerge by LibreOffice mailmerge with type {@link OutputType#toShell}.
 */
public class ToOdtFile extends PrintFunction
{

  /**
   * A {@link PrintFunction} with name "OOoMailMergeToOdtFile" and order 200.
   */
  public ToOdtFile()
  {
    super("OOoMailMergeToOdtFile", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    OOoBasedMailMerge.oooMailMerge(printModel, OOoBasedMailMerge.OutputType.toShell);
  }
}
