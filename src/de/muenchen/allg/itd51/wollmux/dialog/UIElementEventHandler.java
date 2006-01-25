/*
* Dateiname: UIElementEventHandler.java
* Projekt  : WollMux
* Funktion : Interface für Klassen, die auf Events reagieren, die von UIElements verursacht werden.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

/**
 * Interface für Klassen, die auf Events reagieren, die von UIElements verursacht werden.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface UIElementEventHandler
{
  public void processUiElementEvent(UIElement source, String eventType, Object[] args);
}
