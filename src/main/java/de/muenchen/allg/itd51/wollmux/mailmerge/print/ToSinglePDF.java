package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * Print function for creating one pdf file per mailmerge record.
 */
public class ToSinglePDF extends MailMergePrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToSinglePDF" and order 200.
   */
  public ToSinglePDF()
  {
    super("MailMergeNewToSinglePDF", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = false;
    saveOutputFile(createTempDocument(printModel, isODT), printModel.getTextDocument());
  }

}
