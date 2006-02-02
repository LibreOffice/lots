/*
* Dateiname: Condition.java
* Projekt  : WollMux
* Funktion : Repräsentiert eine Bedingung, die wahr oder falsch sein kann und Repräsentiert eine Bedingung, die wahr oder falsch sein kann und von verschiedenen Werten abhängt.
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

import java.util.Collection;
import java.util.Map;

/**
 * Repräsentiert eine Bedingung, die wahr oder falsch sein kann und Repräsentiert eine Bedingung, die wahr oder falsch sein kann 
 * und von verschiedenen Werten abhängt.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Condition
{
  /**
   * Liefert true, wenn die Bedingung für die Values aus mapIdToValue
   * erf erfüllt ist.
   * @param mapIdToValue bildet IDs auf ihre Values ab.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean check(Map mapIdToValue);
  
  /**
   * Liefert eine Collection der IDs der Values von denen diese
   * Condition abhängt, d,h, die IDs die mindestens in der Map vorhanden sein
   * müssen, die an check() übergeben wird. ACHTUNG! Die zurückgelieferte
   * Collection darf nicht verändert werden!
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Collection dependencies();

}
