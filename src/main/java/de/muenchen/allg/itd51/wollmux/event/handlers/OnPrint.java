package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for printing a document.
 */
public class OnPrint extends WollMuxEvent
{
  private XDispatch origDisp;

  private com.sun.star.util.URL origUrl;

  private PropertyValue[] origArgs;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param origDisp
   *          The original dispatch.
   * @param origUrl
   *          The original command.
   * @param origArgs
   *          The original arguments.
   */
  public OnPrint(TextDocumentController documentController, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    this.documentController = documentController;
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.origArgs = origArgs;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    boolean hasPrintFunction = !documentController.getModel().getPrintFunctions().isEmpty();

    if (hasPrintFunction)
    {
      new OnExecutePrintFunction(documentController).emit();
    } else
    {
      if (origDisp != null)
      {
        origDisp.dispatch(origUrl, origArgs);
      }
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}