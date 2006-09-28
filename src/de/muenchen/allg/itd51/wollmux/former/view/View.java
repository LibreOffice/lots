/*
* Dateiname: View.java
* Projekt  : WollMux
* Funktion : Über-Interface für alle Views im FormularMax 4000
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 29.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.view;

import javax.swing.JComponent;

/**
 * Über-Interface für alle Views im FormularMax 4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface View
{
  /**
   * Liefert die Komponente für diese View.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public JComponent JComponent();
}
