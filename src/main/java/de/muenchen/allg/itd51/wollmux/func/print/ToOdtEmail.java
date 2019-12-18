package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

/**
 * Print function for sending odt documents via email.
 */
public class ToOdtEmail extends PrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToODTEMail" and order 200.
   */
  public ToOdtEmail()
  {
    super("MailMergeNewToODTEMail", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = true;
    MailMergeNew.sendAsEmail(printModel, isODT);
  }

}
