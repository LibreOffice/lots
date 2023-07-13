/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
import org.libreoffice.lots.dispatch.DispatchHelper;
import org.libreoffice.lots.event.WollMuxEventHandler;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

/**
 * Event for handling LibreOffice UpdateInputField dispatches. The LibreOffice own dialog isn't
 * shown.
 */
public class OnUpdateInputFields extends WollMuxEvent
{
  XTextDocument doc;
  DispatchHelper helper;

  /**
   * Create this event.
   *
   * @param doc
   *          The document.
   * @param helper
   *          A helper for calling the original dispatch.
   */
  public OnUpdateInputFields(XTextDocument doc, DispatchHelper helper)
  {
    this.doc = doc;
    this.helper = helper;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XEventListener listener = new XEventListener()
    {
      @Override
      public void disposing(EventObject arg0)
      {
        // nothing to do
      }

      @Override
      public void notifyEvent(com.sun.star.document.EventObject event)
      {
        if (UnoRuntime.areSame(doc, event.Source)
            && WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED.equals(event.EventName))
        {
          helper.dispatchFinished(true);
          new OnRemoveDocumentEventListener(this).emit();
        }
      }
    };
    new OnAddDocumentEventListener(listener).emit();
  }
}
