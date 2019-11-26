/*
* Dateiname: QueryResultsList.java
* Projekt  : WollMux
* Funktion : Eine einfache Klasse um eine Liste als QueryResults zur
*            Verfügung zu stellen.
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
* 28.10.2005 | BNK | Erstellung
* 03.11.2005 | BNK | besser kommentiert
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Eine einfache Klasse um eine Liste als QueryResults zur
 *  Verfügung zu stellen.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class QueryResultsList implements QueryResults
{
  /**
   * Die Liste der Datasets.
   */
  private List<? extends Dataset> data;
  
  /**
   * Erzeugt eine neue QueryResultsList, die die Elemente enthält 
   * die iter zurückliefert (müssen Datasets sein!).
   * @param count dient der Optimierung und sollte die Anzahl der Elemente
   * enthalten, die der Iterator zurückliefern wird. Ist dies nicht bekannt,
   * kann 0 übergeben werden.
   */
  public QueryResultsList(Iterator<? extends Dataset> iter, int count)
  {
    List<Dataset> d = new ArrayList<>(count);
    while(iter.hasNext()) {
      d.add(iter.next());
    }
    data = d;
  }

  /**
   * Erzeugt eine neue QueryResultsList aus einer bestehenden Liste.
   * Achtung! Die Liste wird nicht kopiert, sondern als Referenz übernommen.
   */
  public QueryResultsList(List<? extends Dataset> datasets)
  {
    data = datasets;
  }
  
  @Override
  public int size() { return data.size();}
  
  @SuppressWarnings("unchecked") 
  // Die Typsicherheit kann hier nicht gefährdet sein, da laut
  // http://docs.oracle.com/javase/tutorial/java/generics/wildcardGuidelines.htm vor
  // allem gesichert sein muss, dass kein falscher Typ zur List<? extends Dataset>
  // hinzugefügt wird. So etwas ist mit einem Iterator, der nur die Methoden
  // hasNext(), next() und remove() kennt, nicht möglich.
  @Override
  public Iterator<Dataset> iterator()
  {
    return (Iterator<Dataset>) data.iterator(); 
  }
  
  @Override
  public boolean isEmpty() { return data.isEmpty(); }
}
