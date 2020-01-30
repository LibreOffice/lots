package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.form.sidebar.FormSidebarPanel;

/**
 * Event for moving the cursor to the form field associated with the control in the
 * {@link FormSidebarPanel}
 */
public class OnFocusFormField extends WollMuxEvent
{
  private String fieldId;
  private TextDocumentController documentController;

  /**
   * Create this event.
   * 
   * @param documentController
   *          The document.
   * @param fieldId
   *          Id of the field to focus. If there're more fields with this id the first one without a
   *          transformation is selected.
   */
  public OnFocusFormField(TextDocumentController documentController,
      String fieldId)
  {
    this.documentController = documentController;
    this.fieldId = fieldId;
  }

  @Override
  protected void doit()
  {
    documentController.getModel().focusFormField(fieldId);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().doc + ", '" + fieldId
        + "')";
  }
}
