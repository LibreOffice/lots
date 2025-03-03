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

import java.awt.event.ActionListener;

import org.libreoffice.lots.GlobalFunctions;
import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.former.FormularMax4kController;

/**
 * Event for showing FormularMax.
 */
public class OnFormularMax4000Show extends WollMuxEvent
{

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document associated with the FormularMax.
   */
  public OnFormularMax4000Show(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // move existing FormularMax to front or create a new one
    FormularMax4kController max = DocumentManager.getDocumentManager()
        .getCurrentFormularMax4000(documentController.getModel().doc);
    if (max != null)
    {
      max.toFront();
    } else
    {
      ActionListener l = actionEvent -> {
        if (actionEvent.getSource() instanceof FormularMax4kController)
        {
          new OnRemoveFormularMax(documentController).emit();
        }
      };

      max = new FormularMax4kController(documentController, l,
          GlobalFunctions.getInstance().getGlobalFunctions(),
          GlobalFunctions.getInstance().getGlobalPrintFunctions());
      DocumentManager.getDocumentManager()
          .setCurrentFormularMax4000(documentController.getModel().doc, max);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
