package de.muenchen.allg.itd51.wollmux.core.form.model;

/**
 * Ein Listener für Änderungen an den Formularwerten.
 * 
 * @author daniel.sikeler
 *
 */
public interface FormValueChangedListener
{
  /**
   * Wird aufgerufen, wenn sich der Wert von {@link Control#getValue()} ändert.
   * 
   * @param id
   *          Die Id des Formularfeldes das sich geändert hat.
   * @param value
   *          Der neue Wert des Formularfeldes.
   */
  public void valueChanged(String id, String value);

  /**
   * Wird aufgerufen, wenn sich der Wert von {@link Control#isOkay()} ändert.
   * 
   * @param id
   *          Die Id des Formularfeldes das sich geändert hat.
   * @param okay
   *          Der neue Zustand des Formularfeldes.
   */
  public void statusChanged(String id, boolean okay);
}
