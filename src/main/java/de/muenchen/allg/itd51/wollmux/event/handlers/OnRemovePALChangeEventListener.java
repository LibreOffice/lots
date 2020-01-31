package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;

/**
 * Event for unregistering {@link OnRemovePALChangeEventListener}.
 */
public class OnRemovePALChangeEventListener extends WollMuxEvent
{
  private XPALChangeEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener.
   */
  public OnRemovePALChangeEventListener(XPALChangeEventListener listener)
  {
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    PersoenlicheAbsenderliste.getInstance().removePALChangeEventListener(listener);
  }
}