package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das signasisiert, dass der FormController (der
 * zeitgleich mit einer FormGUI zum TextDocument model gestartet wird) vollständig
 * initialisiert ist und notwendige Aktionen wie z.B. das Zurücksetzen des
 * Modified-Status des Dokuments durchgeführt werden können. Vor dem Zurücksetzen
 * des Modified-Status, wird auf die erste Seite des Dokuments gesprungen.
 *
 * Das Event wird vom FormModel erzeugt, wenn es vom FormController eine
 * entsprechende Nachricht erhält.
 */
public class OnFormControllerInitCompleted extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnFormControllerInitCompleted.class);

  private TextDocumentController documentController;

  public OnFormControllerInitCompleted(
      TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // Springt zum Dokumentenanfang
    try
    {
      documentController.getModel().getViewCursor().gotoRange(
          documentController.getModel().doc.getText().getStart(), false);
    } catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }

    // Beim Öffnen eines Formulars werden viele Änderungen am Dokument
    // vorgenommen (z.B. das Setzen vieler Formularwerte), ohne dass jedoch
    // eine entsprechende Benutzerinteraktion stattgefunden hat. Der
    // Modified-Status des Dokuments wird daher zurückgesetzt, damit nur
    // wirkliche Interaktionen durch den Benutzer modified=true setzen.
    documentController.getModel().setDocumentModified(false);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "("
        + documentController.getModel() + ")";
  }
}