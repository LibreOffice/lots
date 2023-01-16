/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for reseting document status.
 * <ul>
 * <li>Put cursor on first page</li>
 * <li>set modified state to false</li>
 * </ul>
 */
public class OnResetDocumentState extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnResetDocumentState.class);

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   */
  public OnResetDocumentState(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    try
    {
      documentController.getModel().getViewCursor().gotoRange(
          documentController.getModel().doc.getText().getStart(), false);
    } catch (Exception e)
    {
      LOGGER.debug("", e);
    }

    documentController.getModel().setDocumentModified(false);
  }
}
