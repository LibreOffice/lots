package de.muenchen.allg.itd51.wollmux.event;

/**
 * Die enthaltenen Methoden erlauben es mittels Callback den original Dispatch auf zu
 * rufen oder das Ergebnis einer Dispatch-Verarbeitung zu liefern.
 *
 * @author daniel.sikeler
 *
 */
public interface DispatchHelper
{
  /**
   * Führt den original Dispatch mit der original URL und den original Parametern
   * aus.
   */
  void dispatchOriginal();

  /**
   * Sendet einen Result an den Listener, wenn dieser vorhanden ist, und zeigt damit
   * an, dass der Dispatch behandelt wurde.
   * 
   * @param success
   *          War die Behandlung des Dispatch erfolgreich?
   */
  void dispatchFinished(final boolean success);
}
