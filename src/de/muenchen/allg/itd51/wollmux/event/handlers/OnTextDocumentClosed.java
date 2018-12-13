package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.frame.XFrame;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo;
import de.muenchen.allg.itd51.wollmux.event.DispatchProviderAndInterceptor;
import de.muenchen.allg.itd51.wollmux.event.GlobalEventListener;

/**
 * Erzeugt ein neues WollMuxEvent, das Auskunft darüber gibt, dass ein TextDokument
 * geschlossen wurde und damit auch das TextDocumentModel disposed werden soll.
 *
 * Dieses Event wird ausgelöst, wenn ein TextDokument geschlossen wird.
 *
 * @param docInfo
 *          ein {@link DocumentManager.Info} Objekt, an dem das TextDocumentModel
 *          dranhängt des Dokuments, das geschlossen wurde. ACHTUNG! docInfo hat
 *          nicht zwingend ein TextDocumentModel. Es muss
 *          {@link DocumentManager.Info#hasTextDocumentModel()} verwendet werden.
 *
 *
 *          ACHTUNG! ACHTUNG! Die Implementierung wurde extra so gewählt, dass hier
 *          ein DocumentManager.Info anstatt direkt eines TextDocumentModel
 *          übergeben wird. Es kam nämlich bei einem Dokument, das schnell geöffnet
 *          und gleich wieder geschlossen wurde zu folgendem Deadlock:
 *
 *          {@link OnProcessTextDocument} =>
 *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#getTextDocumentController()}
 *          => {@link TextDocumentModel#TextDocumentModel(XTextDocument)} =>
 *          {@link DispatchProviderAndInterceptor#registerDocumentDispatchInterceptor(XFrame)}
 *          => OOo Proxy =>
 *          {@link GlobalEventListener#notifyEvent(com.sun.star.document.EventObject)}
 *          ("OnUnload") =>
 *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#hasTextDocumentModel()}
 *
 *          Da {@link TextDocumentInfo} synchronized ist kam es zum Deadlock.
 *
 */
public class OnTextDocumentClosed extends BasicEvent
{
  private DocumentManager.Info docInfo;

  public OnTextDocumentClosed(DocumentManager.Info doc)
  {
    this.docInfo = doc;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (docInfo.hasTextDocumentModel())
      DocumentManager.getDocumentManager()
          .dispose(docInfo.getTextDocumentController().getModel().doc);
    System.gc();
  }

  @Override
  public String toString()
  {
    String code = "unknown";
    if (docInfo.hasTextDocumentModel())
      code = "" + docInfo.getTextDocumentController().hashCode();
    return this.getClass().getSimpleName() + "(#" + code + ")";
  }
}