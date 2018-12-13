package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnHandleMailMergeNewReturned extends BasicEvent
{
  private TextDocumentController documentController;

  public OnHandleMailMergeNewReturned(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    DocumentManager.getDocumentManager()
        .setCurrentMailMergeNew(documentController.getModel().doc, null);
    documentController.setFormFieldsPreviewMode(true);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ")";
  }
}