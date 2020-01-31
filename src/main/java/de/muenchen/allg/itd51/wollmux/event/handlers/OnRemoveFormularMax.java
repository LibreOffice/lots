package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for removing a FormularMax instance.
 */
public class OnRemoveFormularMax extends WollMuxEvent
{

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document which has no more FormularMax.
   */
  public OnRemoveFormularMax(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    DocumentManager.getDocumentManager()
        .setCurrentFormularMax4000(documentController.getModel().doc, null);
  }
}
