/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.db;

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
