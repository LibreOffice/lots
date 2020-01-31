package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.document.XEventListener;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

/**
 * Event for unregistering a listener.
 */
public class OnRemoveDocumentEventListener extends WollMuxEvent
{
  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener
   */
  public OnRemoveDocumentEventListener(XEventListener listener)
  {
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    DocumentManager.getDocumentManager().removeDocumentEventListener(listener);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}