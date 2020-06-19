package de.muenchen.allg.itd51.wollmux.former.view;

/**
 * Interface für Klassen, die an Änderungen einer View interessiert sind.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface ViewChangeListener
{
  /**
   * Wird aufgerufen, wenn alle Referenzen auf die View view entfernt werden sollten,
   * weil die view ungültig geworden ist (typischerweise weil das zugrundeliegende
   * Model nicht mehr da ist).
   */
  public void viewShouldBeRemoved(View view);

}
