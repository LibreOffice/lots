package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;

/**
 * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das gesamte Office (und
 * damit auch der WollMux) OHNE Sicherheitsabfragen(!) beendet werden soll.
 *
 * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht für
 * Endbenutzer) einen entsprechenden Button besitzt.
 */
public class OnKill extends BasicEvent 
{
    @Override
    protected void doit() throws WollMuxFehlerException
    {
      if (UNO.desktop != null)
      {
        UNO.desktop.terminate();
      }
      else
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