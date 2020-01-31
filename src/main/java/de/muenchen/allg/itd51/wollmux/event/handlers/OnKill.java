package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;

/**
 * Event for shutting down LibreOffice and WollMux without question.
 */
public class OnKill extends WollMuxEvent
{
  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (UNO.desktop != null)
    {
      UNO.desktop.terminate();
    } else
    {
      System.exit(0);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}