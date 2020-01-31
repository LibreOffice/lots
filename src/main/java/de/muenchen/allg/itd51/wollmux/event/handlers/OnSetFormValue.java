package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

/**
 * Event for setting form values in a document. If the document is a form document processing is
 * done by the form model of the document to update all dependent fields.
 */
public class OnSetFormValue extends WollMuxEvent
{
  private XTextDocument doc;

  private String id;

  private String value;

  private final ActionListener listener;

  /**
   * Create this event.
   *
   * @param doc
   *          The document.
   * @param id
   *          The ID of the field to update.
   * @param value
   *          The new value of the field.
   * @param listener
   *          A listener to notify after processing is finished.
   */
  public OnSetFormValue(XTextDocument doc, String id, String value,
      ActionListener listener)
  {
    this.doc = doc;
    this.id = id;
    this.value = value;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

    FormController formModel = DocumentManager.getDocumentManager().getFormModel(doc);
    if (formModel != null && formModel.hasFieldId(id))
    {
      formModel.setValue(id, value, e -> new OnSetFormValueFinished(listener).emit());
    } else
    {
      documentController.addFormFieldValue(id, value);
      if (listener != null)
      {
        listener.actionPerformed(null);
      }
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", id='"
        + id
        + "', value='" + value + "')";
  }
}