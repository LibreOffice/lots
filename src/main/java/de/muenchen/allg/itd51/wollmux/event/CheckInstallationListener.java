package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnCheckInstallation;

/**
 * Event listener for installation check.
 */
public class CheckInstallationListener implements WollMuxEventListener
{

  /**
   * Execute {@link OnCheckInstallation} events.
   *
   * @param event
   *          An event.
   */
  @Subscribe
  public void onCheckInstallation(OnCheckInstallation event)
  {
    WollMuxEventHandler.getInstance().unregisterListener(this);
    event.process();
  }
}
