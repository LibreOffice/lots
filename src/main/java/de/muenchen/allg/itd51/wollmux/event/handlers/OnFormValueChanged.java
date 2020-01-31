package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for updating form fields.
 *
 * Transformations on fields are executed.
 */
public class OnFormValueChanged extends WollMuxEvent
{
  private String fieldId;

  private String newValue;

  private TextDocumentController documentController;

  /**
   * Create this event.
   * 
   * @param documentController
   *          The document containing the fields.
   * @param fieldId
   *          The id of the form fields.
   * @param newValue
   *          The new value of the fields.
   */
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