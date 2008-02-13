/*
* Dateiname: JPotentiallyOverlongPopupMenuButton.java
* Projekt  : WollMux
* Funktion : Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist, sehr viele Elemente anzubieten.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 13.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD D.10)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import javax.swing.JButton;

/**
 * Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist,
 * sehr viele Elemente anzubieten.
 * 
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public class JPotentiallyOverlongPopupMenuButton extends JButton
{
  /**
   * Erzeugt einen Button mit Beschriftung label, bei dessen Betätigung
   * eine Popup-Menü erscheint, in dem alle Elemente aus actions enthalten sind.
   * Die Elemente von actions sollten {@link javax.swing.Action} Objekte sein,
   * es sind jedoch alle Elemente erlaubt, die man in ein Menü stecken kann,
   * z.B. Separatoren.
   * @param label
   * @param actions
   * @author Matthias Benkmann (D-III-ITD D.10)
   * TODO Testen
   */
  public JPotentiallyOverlongPopupMenuButton(String label, List actions)
  {
    
  }
}
