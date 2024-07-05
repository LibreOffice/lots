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
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.document.TextDocumentController;

import com.sun.star.text.XTextDocument;

/**
 * Event for adding or removing print functions.
 */
public class OnManagePrintFunction extends WollMuxEvent
{
  private XTextDocument doc;

  private String functionName;

  private boolean remove;

  /**
   * Create this event.
   *
   * @param doc
   *          The document.
   * @param functionName
   *          The name of the print function.
   * @param remove
   *          If true the function is removed, otherwise added.
   */
  public OnManagePrintFunction(XTextDocument doc, String functionName,
      boolean remove)
  {
    this.doc = doc;
    this.functionName = functionName;
    this.remove = remove;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);
    if (remove)
    {
      documentController.removePrintFunction(functionName);
    } else
    {
      documentController.addPrintFunction(functionName);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", '"
        + functionName + "', remove=" + remove + ")";
  }
}
