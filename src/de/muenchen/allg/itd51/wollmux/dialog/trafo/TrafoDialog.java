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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionListener;

/**
 * Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class TrafoDialog
{
  /**
   * Darf erst aufgerufen werden, wenn der Dialog beendet wurde
   * (siehe {@link TrafoDialogParameters#closeAction}) und liefert dann
   * Informationen über den Endzustand des Dialogs.
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  public abstract TrafoDialogParameters getExitStatus();
  
  /**
   * Zeigt den Dialog an.
   * 
   * @param owner
   *          der Dialog zu dem dieser Dialog gehört. Vergleiche auch
   *          {@link #show(Frame, ActionListener)}
   *          Darf null sein.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public abstract void show(Dialog owner);
  
  /**
   * Zeigt den Dialog an.
   * 
   * @param owner
   *          der Frame zu dem dieser Dialog gehört. Vergleiche auch *
   *          {@link #show(Dialog, ActionListener)}
   *          Darf null sein.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) 
   */
  public abstract void show(Frame owner);
  
  /**
   * Schließt den Dialog. Darf nur aufgerufen werden, wenn er gerade angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public abstract void dispose();
}
