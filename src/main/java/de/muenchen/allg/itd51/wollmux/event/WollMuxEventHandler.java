package de.muenchen.allg.itd51.wollmux.event;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;

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
    eventBus.unregister(listener);
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
