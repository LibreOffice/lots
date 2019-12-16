package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Erzeugt ein neues WollMuxEvent das signaisiert, dass die Druckfunktion
 * aufgerufen werden soll, die im TextDocumentModel model aktuell definiert ist.
 * Die Methode erwartet, dass vor dem Aufruf geprüft wurde, ob model eine
 * Druckfunktion definiert. Ist dennoch keine Druckfunktion definiert, so erscheint
 * eine Fehlermeldung im Log.
 *
 * Das Event wird ausgelöst, wenn der registrierte WollMuxDispatchInterceptor eines
 * Dokuments eine entsprechende Nachricht bekommt.
 */
public class OnExecutePrintFunction extends BasicEvent
{
  private TextDocumentController documentController;

  public OnExecutePrintFunction(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    stabilize();

    // Die im Dokument gesetzten Druckfunktionen ausführen:
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);

    // Drucken im Hintergrund, damit der WollMuxEventHandler weiterläuft.
    new Thread()
    {
      @Override
      public void run()
      {
        pmod.printWithProps();
      }
    }.start();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
