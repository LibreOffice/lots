package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Shows or hides a document window.
 */
public class OnSetWindowVisible extends WollMuxEvent
{
  boolean visible;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param visible
   *          If true document is shown, otherwise hidden.
   */
  public OnSetWindowVisible(TextDocumentController documentController,
      boolean visible)
  {
    this.documentController = documentController;
    this.visible = visible;
  }

  @Override
  protected void doit()
  {
    documentController.getFrameController().setWindowVisible(visible);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + visible + ")";
  }
}