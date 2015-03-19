/*
* Dateiname: TrafoDialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
* 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
  public abstract void show(String windowTitle, Dialog owner);
  
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
  public abstract void show(String windowTitle, Frame owner);
  
  /**
   * Schließt den Dialog. Darf nur aufgerufen werden, wenn er gerade angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public abstract void dispose();
}
