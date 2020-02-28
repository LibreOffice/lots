package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * Print function for sending odt documents via email.
 */
public class ToOdtEmail extends PrintToEmail
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
    sendAsEmail(printModel, isODT);
  }

}
