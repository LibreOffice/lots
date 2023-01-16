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

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.itd51.wollmux.TextModule;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for inserting a boilerplate. It can be inserted as a reference or the reference can be
 * resolved immediately.
 */
public class OnTextbausteinEinfuegen extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnTextbausteinEinfuegen.class);

  private boolean reprocess;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param reprocess
   *          If true the reference is resolved, otherwise a reference is inserted.
   */
  public OnTextbausteinEinfuegen(TextDocumentController documentController,
      boolean reprocess)
  {
    this.documentController = documentController;
    this.reprocess = reprocess;

  }

  @Override
  protected void doit()
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    try
    {
      TextModule.createInsertFragFromIdentifier(
          documentController.getModel().doc, viewCursor, reprocess);
      if (reprocess)
      {
        new OnReprocessTextDocument(documentController).emit();
      } else
      {
        InfoDialog.showInfoModal(L.m("Insertion succesful"), L.m("The text block reference was inserted."));
      }
    } catch (WollMuxFehlerException e)
    {
      LOGGER.error("The text block reference could not be inserted", e);
      InfoDialog.showInfoModal(L.m("Insertion failed"), e.getMessage());
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ", " + reprocess + ")";
  }
}
