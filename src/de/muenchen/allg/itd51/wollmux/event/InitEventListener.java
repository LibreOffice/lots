package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;

public class InitEventListener
{
  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    event.process();
  }
}
