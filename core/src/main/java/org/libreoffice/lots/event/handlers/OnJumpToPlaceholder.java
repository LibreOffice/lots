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

import org.libreoffice.lots.TextModule;
import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.document.TextDocumentController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;

/**
 * Set the cursor to the next placeholder.
 */
public class OnJumpToPlaceholder extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnJumpToPlaceholder.class);

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   */
  public OnJumpToPlaceholder(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();

    try
    {
      TextModule.jumpPlaceholders(documentController.getModel().doc,
          viewCursor);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
