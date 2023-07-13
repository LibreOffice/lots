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
package org.libreoffice.lots.event;

import java.util.ServiceLoader;

import org.libreoffice.lots.event.handlers.WollMuxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * The global event handler of {@link WollMuxEvent}. It's a singleton.
 */
public class WollMuxEventHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxEventHandler.class);

  /**
   * Value of processing finished event.
   */
  public static final String ON_WOLLMUX_PROCESSING_FINISHED = "OnWollMuxProcessingFinished";

  /**
   * The only instance of this class.
   */
  private static WollMuxEventHandler instance;

  /**
   * The event bus.
   */
  private EventBus eventBus;

  /**
   * Does this event handler accept new events?
   */
  private boolean acceptEvents = false;

  /**
   * Accept or reject new events.
   *
   * @param accept
   *          If true new events are accepted, if false events are ignored.
   */
  public void setAcceptEvents(boolean accept)
  {
    acceptEvents = accept;
    if (accept)
      LOGGER.debug("EventProcessor: akzeptiere neue Events.");
    else
      LOGGER.debug("EventProcessor: blockiere Entgegennahme von Events!");
  }

  /**
   * Create a new WollMux event bus and register all listeners implementing
   * {@link WollMuxEventListener}.
   */
  private WollMuxEventHandler()
  {
    LOGGER.debug("create event handler");
    eventBus = new EventBus();
    ServiceLoader.load(WollMuxEventListener.class, WollMuxEventListener.class.getClassLoader())
        .forEach(listener -> {
      LOGGER.debug("register listener {}", listener);
      eventBus.register(listener);
    });
  }

  /**
   * Get the {@link WollMuxEventHandler}.
   *
   * @return The event handler.
   */
  public static WollMuxEventHandler getInstance()
  {
    if (instance == null)
    {
      instance = new WollMuxEventHandler();
    }

    return instance;
  }

  /**
   * Add a new listener on the event bus.
   *
   * @param listener
   *          A new listener.
   */
  public void registerListener(Object listener)
  {
    eventBus.register(listener);
  }

  /**
   * Remove a listener of the event bus.
   *
   * @param listener
   *          A listener.
   */
  public void unregisterListener(Object listener)
  {
    try
    {
      eventBus.unregister(listener);
    } catch (IllegalArgumentException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Adds a new event to the event queue.
   *
   * @param event
   *          New event to be processed.
   */
  public void handle(WollMuxEvent event)
  {
    if (acceptEvents)
    {
      eventBus.post(event);
    }
  }
}
