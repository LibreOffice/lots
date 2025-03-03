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

import org.libreoffice.lots.document.TextDocumentController;

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
