package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

/**
 * A print function, which sets the mailmerge fields to the values of the next mailmerge data.
 */
public class SetFormValue extends PrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewSetFormValue" and order 75.
   */
  public SetFormValue()
  {
    super("MailMergeNewSetFormValue", 75);
  }

  @Override
  public void print(XPrintModel printModel) throws PrintException
  {
    try
    {
      MailMergeNew.mailMergeNewSetFormValue(printModel, null);
    } catch (Exception ex)
    {
      throw new PrintException(ex.toString(), ex);
    }
  }

}
