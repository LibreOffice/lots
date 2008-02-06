/*
* Dateiname: TrafoDialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
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
 * Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class TrafoDialog
{
  /**
   * Zeigt den Dialog an.
   * @param closeAction wird aufgerufen, wenn der Dialog beendet wird. Als source wird
   * im übergebenen ActionEvent ein {@link TrafoDialogParameters} Objekt übergeben, das
   * den geänderten Trafo-Zustand beschreibt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public abstract void show(ActionListener closeAction);
  
  /**
   * Schließt den Dialog. Darf nur aufgerufen werden, wenn er gerade angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public abstract void dispose();
}
