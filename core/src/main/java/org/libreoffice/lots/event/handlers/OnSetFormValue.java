/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.event.handlers;

import java.awt.event.ActionListener;

import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.form.control.FormController;

import com.sun.star.text.XTextDocument;

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

    FormController formModel = DocumentManager.getDocumentManager().getFormController(doc);
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
