package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;

/**
 * Event for showing FormularMax.
 */
public class OnFormularMax4000Show extends WollMuxEvent
{

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document associated with the FormularMax.
   */
  public OnFormularMax4000Show(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // move existing FormularMax to front or create a new one
    FormularMax4kController max = DocumentManager.getDocumentManager()
        .getCurrentFormularMax4000(documentController.getModel().doc);
    if (max != null)
    {
      max.toFront();
    } else
    {
      ActionListener l = actionEvent -> {
        if (actionEvent.getSource() instanceof FormularMax4kController)
        {
          new OnRemoveFormularMax(documentController).emit();
        }
      };

      max = new FormularMax4kController(documentController, l,
          GlobalFunctions.getInstance().getGlobalFunctions(),
          GlobalFunctions.getInstance().getGlobalPrintFunctions());
      DocumentManager.getDocumentManager()
          .setCurrentFormularMax4000(documentController.getModel().doc, max);
      max.run();
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}