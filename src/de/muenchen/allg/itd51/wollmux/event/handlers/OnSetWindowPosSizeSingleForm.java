package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.form.control.FormController;

public class OnSetWindowPosSizeSingleForm extends BasicEvent
{
  private int docX;
  private int docY;
  private int docWidth;
  private int docHeight;
  private FormController formModel;

  public OnSetWindowPosSizeSingleForm(FormController formModel, int docX,
      int docY, int docWidth, int docHeight)
  {
    this.formModel = formModel;
    this.docX = docX;
    this.docY = docY;
    this.docWidth = docWidth;
    this.docHeight = docHeight;
  }

  @Override
  protected void doit()
  {
    formModel.setWindowPosSize(docX, docY, docWidth, docHeight);
  }
}
