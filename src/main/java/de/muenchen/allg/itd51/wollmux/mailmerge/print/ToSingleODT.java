package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * Print function for creating one odt file per mailmerge record.
 */
public class ToSingleODT extends MailMergePrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToSingleODT" and order 200.
   */
  public ToSingleODT()
  {
    super("MailMergeNewToSingleODT", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = true;
    saveOutputFile(createTempDocument(printModel, isODT), printModel.getTextDocument());
  }

}
