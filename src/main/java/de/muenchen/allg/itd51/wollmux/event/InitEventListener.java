package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;

/**
 * An event listener for WollMux initialization.
 */
public class InitEventListener implements WollMuxEventListener
{

  /**
   * Execute {@link OnInitialize} events.
   *
   * @param event
   *          An event.
   */
  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    WollMuxEventHandler.getInstance().unregisterListener(this);
    event.process();
  }
}
