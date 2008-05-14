/*
* Dateiname: QueryResults.java
* Projekt  : WollMux
* Funktion : Ergebnisse einer Datenbankanfrage.
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
