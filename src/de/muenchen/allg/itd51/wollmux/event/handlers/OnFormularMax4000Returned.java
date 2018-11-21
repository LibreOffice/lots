package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein FormularMax4000
 * beendet wird und die entsprechenden internen Referenzen gelöscht werden können.
 *
 * Dieses Event wird vom EventProcessor geworfen, wenn der FormularMax zurückkehrt.
 */
public class OnFormularMax4000Returned extends BasicEvent 
{

    private TextDocumentController documentController;

    public OnFormularMax4000Returned(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      DocumentManager.getDocumentManager().setCurrentFormularMax4000(documentController.getModel().doc, null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ")";
    }
  }
