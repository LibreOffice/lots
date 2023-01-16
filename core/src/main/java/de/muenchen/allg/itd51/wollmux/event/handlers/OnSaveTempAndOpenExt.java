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

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for saving a temporary file and opening it with an external application.
 */
public class OnSaveTempAndOpenExt extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnSaveTempAndOpenExt.class);

  private String ext;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document to save and open.
   * @param ext
   *          The identifier of the external application
   */
  public OnSaveTempAndOpenExt(TextDocumentController documentController,
      String ext)
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
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ", " + ext
        + ")";
  }
}
