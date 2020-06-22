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

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for collecting form fields not surrounded by WollMux commands.
 *
 * @see TextDocumentController#collectNonWollMuxFormFields()
 */
public class OnCollectNonWollMuxFormFieldsViaPrintModel extends WollMuxEvent
{
  private ActionListener listener;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document which contains form fields.
   * @param listener
   *          Listener, which is notified as soon as this event is processed.
   */
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

    if (listener != null)
    {
      listener.actionPerformed(null);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}