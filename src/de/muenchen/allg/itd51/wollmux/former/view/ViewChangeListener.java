//TODO L.m()
/*
* Dateiname: ViewChangeListener.java
* Projekt  : WollMux
* Funktion : Interface für Klassen, die an Änderungen einer View interessiert sind. 
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
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
   * weil die view ungültig geworden ist (typischerweise weil das zugrundeliegende Model
   * nicht mehr da ist).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void viewShouldBeRemoved(View view);

}
