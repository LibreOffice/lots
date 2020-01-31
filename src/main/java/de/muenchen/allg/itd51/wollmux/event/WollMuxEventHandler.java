package de.muenchen.allg.itd51.wollmux.event;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;

/**
 * The global event handler of {@link WollMuxEvent}.
 */
public class WollMuxEventHandler
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(WollMuxEventHandler.class);

  /**
   * Name des OnWollMuxProcessingFinished-Events.
   */
  public static final String ON_WOLLMUX_PROCESSING_FINISHED = "OnWollMuxProcessingFinished";

  private static WollMuxEventHandler instance;

  private EventBus eventBus;

  private boolean acceptEvents = false;

  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu blockieren.
   * Alle eingehenden Events werden ignoriert, wenn accept auf false gesetzt ist
   * und entgegengenommen, wenn accept auf true gesetzt ist.
   *
   * @param accept
   */
  public void setAcceptEvents(boolean accept)
  {
    acceptEvents = accept;
    if (accept)
      LOGGER.debug(L.m("EventProcessor: akzeptiere neue Events."));
    else
      LOGGER.debug(L.m("EventProcessor: blockiere Entgegennahme von Events!"));
  }

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
