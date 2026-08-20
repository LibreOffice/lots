/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple class to provide a list as QueryResults.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class QueryResultsList implements QueryResults
{
  /**
   * The list of datasets.
   */
  private List<? extends Dataset> data;

  /**
   * Creates a new QueryResultsList containing the items
   * the iterator returns (they must be datasets!).
   * @param count serves for optimization and should contain the number of elements
   * contain the number of elements that the iterator will return. If this is not known,
   * 0 can be passed.
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
   * Creates a new QueryResultsList from an existing list.
   * Danger! The list is not copied, but adopted as a reference.
   */
  public QueryResultsList(List<? extends Dataset> datasets)
  {
    data = datasets;
  }

  @Override
  public int size() { return data.size();}

  @SuppressWarnings("unchecked")
  // Type safety cannot be compromised here, since according to 
  // http://docs.oracle.com/javase/tutorial/java/generics/wildcardGuidelines.htm 
  // it must be ensured above all that no wrong type is added to the 
  // List<? extends Dataset>. 
  // Such a thing is not possible with an iterator that only knows the methods 
  // hasNext(), next() and remove().
  @Override
  public Iterator<Dataset> iterator()
  {
    return (Iterator<Dataset>) data.iterator();
  }

  @Override
  public boolean isEmpty() { return data.isEmpty(); }
}
