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
* 24.04.2006 | BNK | Kommentiert.
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
  /**
   * Wird aufgerufen, wenn auf einem UI Element ein Event registriert wird.
   * @param source das UIElement auf dem der Event registriert wurde.
   * @param eventType die Art des Events. Zur Zeit werden folgende Typen unterstützt
   * (diese Liste kann erweitert werden, auch für existierende UIElemente; ein
   * Handler sollte also zwingend den Typ überprüfen und unbekannte Typen ohne
   * Fehlermeldung ignorieren):
   * <dl>
   *   <dt>action</dt>
   *   <dd>Eine ACTION wurde ausgelöst (normalerweise durch einen Button).
   *   Das Array args enthält als erstes Element den Namen der ACTION. Falls die
   *   ACTION weitere Parameter benötigt, so werden diese in den folgenden
   *   Arrayelementen übergeben.</dd>
   *   
   *   <dt>valueChanged</dt>
   *   <dd>Wird von Elementen ausgelöst, die der Benutzer bearbeiten kann 
   *   (zum Beispiel Textfields), wenn der Wert geändert wurde. Achtung! Es ist 
   *   nicht
   *   garantiert, dass der Wert sich tatsächlich geändert hat. Dieser Event wird
   *   auch ausgelöst, wenn der Benutzer aus einer Auswahl (z.B. ComboBox)
   *   ein Element ausgewählt hat.</dd>
   * </dl> 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void processUiElementEvent(UIElement source, String eventType, Object[] args);
}
