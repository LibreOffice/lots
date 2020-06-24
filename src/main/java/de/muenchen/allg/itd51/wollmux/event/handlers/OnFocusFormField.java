/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
