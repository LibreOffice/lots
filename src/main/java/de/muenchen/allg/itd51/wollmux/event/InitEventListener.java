package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;

public class InitEventListener implements WollMuxEventListener
{
  public InitEventListener()
  {
  }

  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    WollMuxEventHandler.getInstance().unregisterListener(this);
    event.process();
  }
}
