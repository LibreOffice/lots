/*
* Dateiname: Value.java
* Projekt  : WollMux
* Funktion : Ein Wert, der als verschiedene Datentypen abrufbar ist.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 02.02.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Ein Wert, der als verschiedene Datentypen abrufbar ist
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Value
{
  /**
   * Der aktuelle Wert als String. Falls es sich um einen booleschen Wert
   * handelt, wird der String "true" oder "false" zurückgeliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString();
  
  /**
   * Der aktuelle Wert als boolean. Falls der Wert seiner Natur nach ein
   * String ist, so wird true geliefert, falls der aktuelle
   * Wert nicht der leere String ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean();
}
