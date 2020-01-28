package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnCheckInstallation;

public class CheckInstallationListener implements WollMuxEventListener
{
  public CheckInstallationListener()
  {
  }

  @Subscribe
  public void onCheckInstallation(OnCheckInstallation event)
  {
    WollMuxEventHandler.getInstance().unregisterListener(this);
    event.process();
  }
}
