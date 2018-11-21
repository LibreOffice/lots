package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnCheckInstallation;

public class CheckInstallation 
{
    @Subscribe
    public void OnCheckInstallation(OnCheckInstallation event)
    {
      event.process();
    }
}
