package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

/**
 * Print function for creating one odt file per mailmerge record.
 */
public class ToSingleODT extends PrintFunction
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
    MailMergeNew.saveOutputFile(MailMergeNew.createTempDocumentFileByFilePattern(printModel, isODT),
        printModel.getTextDocument());
  }

}
