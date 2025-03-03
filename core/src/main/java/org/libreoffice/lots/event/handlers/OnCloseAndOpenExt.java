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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.OpenExt;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.document.TextDocumentController;

/**
 * Event for closing a document and starting an external application.
 *
 * The document is stored as a temporary file and opened by the application.
 */
public class OnCloseAndOpenExt extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnCloseAndOpenExt.class);

  private String ext;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document to store and close.
   * @param ext
   *          Identifier of the external application.
   */
  public OnCloseAndOpenExt(TextDocumentController documentController, String ext)
  {
    this.documentController = documentController;
    this.ext = ext;
  }

  @Override
  protected void doit()
  {
    try
    {
      OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
      openExt.setSource(UNO.XStorable(documentController.getModel().doc));
      openExt.storeIfNecessary();
      openExt.launch(x -> LOGGER.error("", x));
    } catch (Exception x)
    {
      LOGGER.error("", x);
      return;
    }

    documentController.getModel().setDocumentModified(false);
    documentController.getModel().close();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ", " + ext
        + ")";
  }
}
