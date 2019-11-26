package de.muenchen.allg.itd51.wollmux.core.form.model;

/**
 * Ein Listener für Änderungen von Sichtbarkeiten.
 * 
 * @author daniel.sikeler
 *
 */
public interface VisibilityChangedListener
{
  /**
   * Wird aufgerufen sobald sich die Sichtbarkeit einer Gruppe ändert.
   * 
   * @param id
   *          Die Id der Gruppe.
   * @param visible
   *          Der neue Zustand (true = sichtbar, false = unsichtbar).
   */
  public void visibilityChanged(String id, boolean visible);
}
