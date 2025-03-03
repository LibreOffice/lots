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

import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.document.TextDocumentController;

/**
 * Observer notifies sidebar instances when an instance of TextDocumentController is available.
 *
 * Because of the order of initializations (Sidebars are initialized between onCreate() and
 * onViewCreated()) and a required instance of TextDocumentController (init happens in
 * onViewCreated) for building up the ui within the sidebar we need to notify sidebar instances when
 * an textDocumentController for an (new) document gets initialized.
 *
 */
public class OnTextDocumentControllerInitialized extends WollMuxEvent
{
  private TextDocumentController documentController;

  /**
   * Handled if TextDocumentController is initialized.
   *
   * @param documentController
   *          Instance of TextDocumentController.
   */
  public OnTextDocumentControllerInitialized(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  public TextDocumentController getTextDocumentController()
  {
    return this.documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // nothing to do.
  }
}
