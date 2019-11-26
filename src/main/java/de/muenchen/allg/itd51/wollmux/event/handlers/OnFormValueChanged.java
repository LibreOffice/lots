package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle Formularfelder
 * Dokument auf den neuen Wert gesetzt werden. Bei Formularfeldern mit
 * TRAFO-Funktion wird die Transformation entsprechend durchgeführt.
 *
 * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
 * Formular-GUI der Wert des Formularfeldes fieldID geändert wurde und sorgt dafür,
 * dass die Wertänderung auf alle betroffenen Formularfelder im Dokument doc
 * übertragen werden.
 *
 * @param idToFormValues
 *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
 *          FormFields mit der ID fieldID liefert.
 * @param fieldId
 *          Die ID der Formularfelder, deren Werte angepasst werden sollen.
 * @param newValue
 *          Der neue untransformierte Wert des Formularfeldes.
 * @param funcLib
 *          Die Funktionsbibliothek, die zur Gewinnung der Trafo-Funktion verwendet
 *          werden soll.
 */
public class OnFormValueChanged extends BasicEvent
{
  private String fieldId;

  private String newValue;

  private TextDocumentController documentController;

  public OnFormValueChanged(TextDocumentController documentController,
      String fieldId,
      String newValue)
  {
    this.fieldId = fieldId;
    this.newValue = newValue;
    this.documentController = documentController;
  }

  @Override
  protected void doit()
  {
    documentController.addFormFieldValue(fieldId, newValue);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + fieldId + "', '" + newValue
        + "')";
  }
}