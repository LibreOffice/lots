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

import java.util.ArrayList;
import java.util.List;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Adds listener for notification about document processing.
 */
public class OnAddDocumentEventListener extends WollMuxEvent
{
  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener to register.
   */
  public OnAddDocumentEventListener(XEventListener listener)
  {
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    DocumentManager.getDocumentManager().addDocumentEventListener(listener);

    List<XComponent> processedDocuments = new ArrayList<>();
    DocumentManager.getDocumentManager()
        .getProcessedDocuments(processedDocuments);

    for (XComponent compo : processedDocuments)
    {
      new OnNotifyDocumentEventListener(listener,
          WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED, compo).emit();
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
