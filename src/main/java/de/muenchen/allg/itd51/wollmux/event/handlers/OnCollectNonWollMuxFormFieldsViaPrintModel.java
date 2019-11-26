package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Sammelt alle Formularfelder des Dokuments model auf, die nicht von
 * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und
 * befüllt werden (derzeit c,s,s,t,textfield,Database-Felder).
 *
 * Das Event wird aus der Implementierung von XPrintModel (siehe TextDocumentModel)
 * geworfen, wenn dort die Methode collectNonWollMuxFormFields aufgerufen wird.
 *
 * @param documentController
 * @param unlockActionListener
 *          Der unlockActionListener wird immer informiert, wenn alle notwendigen
 *          Anpassungen durchgeführt wurden.
 */
public class OnCollectNonWollMuxFormFieldsViaPrintModel extends BasicEvent
{
  private ActionListener listener;
  private TextDocumentController documentController;

  public OnCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentController documentController,
      ActionListener listener)
  {
    this.documentController = documentController;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    documentController.collectNonWollMuxFormFields();

    stabilize();
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}