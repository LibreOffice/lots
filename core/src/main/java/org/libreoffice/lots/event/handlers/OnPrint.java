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

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;

/**
 * Event for printing a document.
 */
public class OnPrint extends WollMuxEvent
{
  private XDispatch origDisp;

  private com.sun.star.util.URL origUrl;

  private PropertyValue[] origArgs;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param origDisp
   *          The original dispatch.
   * @param origUrl
   *          The original command.
   * @param origArgs
   *          The original arguments.
   */
  public OnPrint(TextDocumentController documentController, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    this.documentController = documentController;
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.origArgs = origArgs;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    boolean hasPrintFunction = !documentController.getModel().getPrintFunctions().isEmpty();

    if (hasPrintFunction)
    {
      new OnExecutePrintFunction(documentController).emit();
    } else
    {
      if (origDisp != null)
      {
        origDisp.dispatch(origUrl, origArgs);
      }
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
