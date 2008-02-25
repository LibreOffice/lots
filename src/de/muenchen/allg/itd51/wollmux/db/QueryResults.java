//TODO L.m()
/*
* Dateiname: QueryResults.java
* Projekt  : WollMux
* Funktion : Ergebnisse einer Datenbankanfrage.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Iterator;

/**
 * Ergebnisse einer Datenbankanfrage.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface QueryResults extends Iterable<Dataset>
{
  /**
   * Die Anzahl der Ergebnisse.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int size();
  
  /**
   * Iterator über die Ergebnisse ({@link Dataset} Objekte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Iterator<Dataset> iterator();
  
  /**
   * Liefert true, falls es keine Ergebnisse gibt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isEmpty();
}
