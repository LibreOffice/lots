package de.muenchen.allg.itd51.wollmux.event.handlers;

/**
 * Interface für die Events, die dieser EventHandler abarbeitet.
 */
public interface WollMuxEvent
{
  /**
   * Startet die Ausführung des Events und darf nur aus dem EventProcessor
   * aufgerufen werden.
   */
  public void process();
}
