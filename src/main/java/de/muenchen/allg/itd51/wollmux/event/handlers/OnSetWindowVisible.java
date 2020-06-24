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

/**
 * Shows or hides a document window.
 */
public class OnSetWindowVisible extends WollMuxEvent
{
  boolean visible;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param visible
   *          If true document is shown, otherwise hidden.
   */
  public OnSetWindowVisible(TextDocumentController documentController,
      boolean visible)
  {
    this.documentController = documentController;
    this.visible = visible;
  }

  @Override
  protected void doit()
  {
    documentController.getFrameController().setWindowVisible(visible);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + visible + ")";
  }
}