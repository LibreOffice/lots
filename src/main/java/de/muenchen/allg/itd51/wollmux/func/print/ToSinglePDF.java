package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

/**
 * Print function for creating one pdf file per mailmerge record.
 */
public class ToSinglePDF extends PrintFunction
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
    MailMergeNew.saveOutputFile(MailMergeNew.createTempDocumentFileByFilePattern(printModel, isODT),
        printModel.getTextDocument());
  }

}
