/*
* Dateiname: IfThenElseDialog.java
* Projekt  : WollMux
* Funktion : Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 01.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.event.ActionListener;

/**
 * Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IfThenElseDialog extends TrafoDialog
{
  public IfThenElseDialog(TrafoDialogParameters params)
  {
    throw new IllegalArgumentException();
    
    /*
     * gleich (mit gross/klein):  STRCMP(VALUE "feld" "vergleichswert")
     * ungleich (mit gross/klein): NOT(STRCMP(VALUE "feld" "vergleichswert"))
     * 
     */
  }
  
  public void show(ActionListener closeAction)
  {}
  
  
  public void dispose() {};
}
