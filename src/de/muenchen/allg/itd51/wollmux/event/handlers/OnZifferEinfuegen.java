package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine weitere Ziffer der
 * Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende Ziffer gelöscht
 * werden soll.
 *
 * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
 * auf den Knopf der OOo-Symbolleiste ein "wollmux:ZifferEinfuegen" dispatch
 * erfolgte.
 */
public class OnZifferEinfuegen extends BasicEvent
{
  private TextDocumentController documentController;

  public OnZifferEinfuegen(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor != null)
    {
      XTextRange vc = SachleitendeVerfuegung
          .insertVerfuegungspunkt(documentController, viewCursor);
      if (vc != null)
        viewCursor.gotoRange(vc, false);
    }

    stabilize();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
