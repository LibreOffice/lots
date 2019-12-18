package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

/**
 * Print function for sending pdf documents via email.
 */
public class ToPdfEmail extends PrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToPDFEMail" and order 200.
   */
  public ToPdfEmail()
  {
    super("MailMergeNewToPDFEMail", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = false;
    MailMergeNew.sendAsEmail(printModel, isODT);
  }

}
