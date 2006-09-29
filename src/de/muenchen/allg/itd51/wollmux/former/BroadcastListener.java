/*
* Dateiname: BroadcastListener.java
* Projekt  : WollMux
* Funktion : Abstrakte Basisklasse für Horcher auf dem globalen Broadcast-Kanal.
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

/**
 * Abstrakte Basisklasse für Horcher auf dem globalen Broadcast-Kanal. 
 * Der globale Broadcast-Kanal wird für Nachrichten 
 * verwendet, die verschiedene permanente
 * Objekte erreichen müssen, die aber von (transienten) Objekten ausgehen, die mit diesen 
 * globalen Objekten
 * wegen des Ausuferns der Verbindungen nicht in einer Beziehung stehen sollen.
 * BroadcastListener dürfen nur
 * permanente Objekte sein, d.h. Objekte deren Lebensdauer nicht vor Beenden des
 * FM4000 endet. 
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class BroadcastListener
{
  /**
   * getObject() ist ein {@link de.muenchen.allg.itd51.wollmux.former.control.FormControlModel}. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastFormControlModelSelection(BroadcastObjectSelection b) {}
  
  /**
   * getObject() ist ein {@link de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel}. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastInsertionModelSelection(BroadcastObjectSelection b) {}
}

