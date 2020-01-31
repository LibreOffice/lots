package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;

/**
 * Event for notification that the form model as updated all fields.
 */
public class OnSetFormValueFinished extends WollMuxEvent
{
  private ActionListener listener;

  /**
   * Create this event.
   *
   * @param unlockActionListener
   *          The listener to notify.
   */
  public OnSetFormValueFinished(ActionListener unlockActionListener)
  {
    this.listener = unlockActionListener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}