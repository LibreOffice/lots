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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Event for notifying listeners registered on an object.
 */
public class OnNotifyDocumentEventListener extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnNotifyDocumentEventListener.class);
  private String eventName;

  private Object source;

  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          If listener == null all registered listeners are notified, otherwise only the provided
   *          listener is notified.
   * @param eventName
   *          Name of the notification event.
   * @param source
   *          The source of the notification event.
   */
  public OnNotifyDocumentEventListener(XEventListener listener,
      String eventName,
      Object source)
  {
    this.listener = listener;
    this.eventName = eventName;
    this.source = source;
  }

  @Override
  protected void doit()
  {
    final com.sun.star.document.EventObject eventObject = new com.sun.star.document.EventObject();
    eventObject.Source = source;
    eventObject.EventName = eventName;

    for (XEventListener docListener : DocumentManager.getDocumentManager()
        .getDocumentEventListener())
    {
      if (this.listener == null || this.listener == docListener)
      {
        LOGGER.trace("notifying XEventListener (event '{}')", eventName);
        new Thread(() -> docListener.notifyEvent(eventObject)).start();
      }
    }

    XComponent compo = UNO.XComponent(source);
    if (compo != null && eventName.equals(WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED))
    {
      DocumentManager.getDocumentManager().setProcessingFinished(compo);
    }
  }
}
