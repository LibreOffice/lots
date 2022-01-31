/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel;

/**
 * Event for removing text documents from WollMux.
 */
public class OnTextDocumentClosed extends WollMuxEvent
{
  private DocumentManager.Info docInfo;

  /**
   * Creates this event.
   *
   * @param docInfo
   *          The {@link DocumentManager.Info} of the document. It isn't necessary that there's
   *          always a {@link TextDocumentModel}, because
   *          {@link DocumentManager.Info#hasTextDocumentModel()} is called.
   */
  public OnTextDocumentClosed(DocumentManager.Info docInfo)
  {
    this.docInfo = docInfo;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    /*
     * We had deadlocks, if documents are opened and closed immediately afterwards. So we use
     * TextDocumentInfo instead of the model directly.
     */
    if (docInfo.hasTextDocumentModel())
    {
      DocumentManager.getDocumentManager()
          .dispose(docInfo.getTextDocumentController().getModel().doc);
    }
  }
}
