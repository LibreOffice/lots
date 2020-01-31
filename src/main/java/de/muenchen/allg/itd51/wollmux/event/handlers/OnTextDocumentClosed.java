package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

/**
 * Event for removing text documents from WollMux.
 */
public class OnTextDocumentClosed extends WollMuxEvent
{
  private DocumentManager.Info docInfo;

  /**
   * Creates this event.
   *
   * @param docInfo
   *          The {@link DocumentManager.Info} of the document. It isn't necessary that there's
   *          always a {@link TextDocumentModel}, because
   *          {@link DocumentManager.Info#hasTextDocumentModel()} is called.
   */
  public OnTextDocumentClosed(DocumentManager.Info docInfo)
  {
    this.docInfo = docInfo;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    /*
     * We had deadlocks, if documents are opened and closed immediately afterwards. So we use
     * TextDocumentInfo instead of the model directly.
     */
    if (docInfo.hasTextDocumentModel())
    {
      DocumentManager.getDocumentManager()
          .dispose(docInfo.getTextDocumentController().getModel().doc);
    }
  }
}