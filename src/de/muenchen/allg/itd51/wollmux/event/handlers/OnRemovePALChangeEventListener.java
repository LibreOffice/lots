package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;


/**
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgelöst wenn sich ein
 * externe XPALChangeEventListener beim WollMux deregistriert. Der zu entfernende
 * XPALChangeEventListerner wird anschließend im WollMuxSingleton aus der Liste der
 * registrierten XPALChangeEventListener genommen.
 *
 * @author christoph.lutz
 */
public class OnRemovePALChangeEventListener extends BasicEvent 
{
    private XPALChangeEventListener listener;

    public OnRemovePALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    @Override
    protected void doit()
    {
      PersoenlicheAbsenderliste.getInstance().removePALChangeEventListener(listener);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
  }