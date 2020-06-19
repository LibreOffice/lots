package de.muenchen.allg.itd51.wollmux.former;

/**
 * Interface für Nachrichten auf dem globalen Broadcast-Kanal. Siehe auch
 * {@link de.muenchen.allg.itd51.wollmux.former.BroadcastListener}.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Broadcast
{
  /**
   * Sendet diese Broadcast-Nachricht an listener.
   */
  public void sendTo(BroadcastListener listener);
}
