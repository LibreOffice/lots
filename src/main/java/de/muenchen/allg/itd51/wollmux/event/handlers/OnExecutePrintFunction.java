package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Event for executing a print function defined in the document.
 */
public class OnExecutePrintFunction extends WollMuxEvent
{
  private TextDocumentController documentController;

  /**
   * Create this event.
   * 
   * @param documentController
   *          The document.
   */
  public OnExecutePrintFunction(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);
    new Thread()
    {
      @Override
      public void run()
      {
        pmod.printWithProps();
      }
    }.start();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
