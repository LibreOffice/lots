/*
* Dateiname: BroadcastFormControlModelSelection.java
* Projekt  : WollMux
* Funktion : Nachricht, dass in einer View ein FormControlModel ausgewählt wurde.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 04.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;

/**
 * Nachricht, dass in einer View ein FormControlModel ausgewählt wurde. Diese Nachricht wird
 * von anderen Views ausgewertet, um ihre Selektionen ebenfalls anzupassen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class BroadcastFormControlModelSelection implements Broadcast
{
  /**
   * das {@link FormControlModel} das ausgewählt wurde.
   */
  private FormControlModel model;
  
  /**
   * -1 => abwählen, 1 => anwählen, 0: toggle.
   */
  private int state;
  
  /**
   * true => Selektion erst ganz löschen vor an/abwählen des Models.
   */
  private boolean clearSelection;
  
  /**
   * Erzeugt eine neue Nachricht.
   * @param model das {@link FormControlModel} das ausgewählt wurde.
   * @param state -1 => abwählen, 1 => anwählen, 0: toggle
   * @param clearSelection true => Selektion erst ganz löschen vor an/abwählen von model. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public BroadcastFormControlModelSelection(FormControlModel model, int state, boolean clearSelection)
  {
    this.model = model;
    this.state = state;
    this.clearSelection = clearSelection;
  }

  public void sendTo(BroadcastListener listener)
  {
    listener.broadcastFormControlModelSelection(this);
  }
  
  /**
   * Liefert das {@link FormControlModel}, das aus- oder abgewählt wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel() {return model; }
  
  /**
   * Liefert -1 für abwählen, 1 für auswählen, 0 für toggle. 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getState() {return state;}
  
  /**
   * true => Selektion erst ganz löschen vor an/abwählen des Models.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getClearSelection() {return clearSelection;}

}
