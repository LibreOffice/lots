/*
* Dateiname: Dialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 04.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionListener;

/**
 * Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
 */
public interface Dialog
{
  /**
   * Liefert eine neue Instanz dieses Dialogs für den gegebenen context.
   * @param context Für jeden Kontext hält der Dialog eine unabhängige Kopie von
   *        seinem Zustand vor. Auf diese Weise lässt sich der Dialog an verschiedenen
   *        Stellen unabhängig voneinander einsetzen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Dialog instantiate(Object context);
  
  /**
   * Liefert den durch id identifizierten Wert des Dialogs (null, falls die
   * id dem Dialog nicht bekannt ist). Diese Funktion darf nur für mit instantiate()
   * erzeugte Instanzen aufgerufen werden. Ansonsten liefert sie immer null.
   * Diese Funktion ist Thread-safe. Insbesondere muss sie nicht im EDT aufgerufen werden.
   * Sie kann sowohl vor, während als auch nach dem Aufruf von show() aufgerufen werden,
   * auch nachdem der Dialog schon geschlossen wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getData(String id);
 
  /**
   * Zeigt den Dialog an. Diese Funktion darf nur für mit instantiate() erzeugte
   * Instanzen aufgerufen werden. Ansonsten tut sie nichts.
   * @param dialogEndListener falls nicht null, wird 
   *        die {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *        Methode aufgerufen (im Event Dispatching Thread), 
   *        nachdem der Dialog geschlossen wurde.
   *        Das actionCommand des ActionEvents gibt die Aktion an, die
   *        das Beenden des Dialogs veranlasst hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(ActionListener dialogEndListener);
}
