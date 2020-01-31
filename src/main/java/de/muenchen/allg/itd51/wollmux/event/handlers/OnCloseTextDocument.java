package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for closing a document.
 */
public class OnCloseTextDocument extends WollMuxEvent
{
  private TextDocumentController documentController;

  /**
   * Create this event.
   * 
   * @param documentController
   *          The document to close.
   */
  public OnCloseTextDocument(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit()
  {
    documentController.getModel().close();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ")";
  }
}