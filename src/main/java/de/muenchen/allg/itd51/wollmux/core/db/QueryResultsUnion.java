/*
* Dateiname: QueryResultsUnion.java
* Projekt  : WollMux
* Funktion : Stellt die Vereinigung 2er QueryResults als QueryResults zur Verfügung
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
* 07.11.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Iterator;

/**
 * Stellt die Vereinigung 2er QueryResults als QueryResults zur Verfügung.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class QueryResultsUnion implements QueryResults
{
  private QueryResults results1;
  private QueryResults results2;
  
  /**
   * Erzeugt eine neue Vereinigung, die die Resultate von res1 und die
   * Resultate von res2 enthält in undefinierter Reihenfolge. 
   */
  public QueryResultsUnion(QueryResults res1, QueryResults res2)
  {
    results1 = res1;
    results2 = res2;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.QueryResults#size()
   */
  @Override
  public int size()
  {
    return results1.size() + results2.size();
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.QueryResults#iterator()
   */
  @Override
  public Iterator<Dataset> iterator()
  {
    return new UnionIterator(results1.iterator(), results2.iterator());
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.QueryResults#isEmpty()
   */
  @Override
  public boolean isEmpty()
  {
    return results1.isEmpty() && results2.isEmpty();
  }

  private static class UnionIterator implements Iterator<Dataset>
  {
    private Iterator<Dataset> iter1;
    private Iterator<Dataset> iter2;
    private Iterator<Dataset> iter;
    
    public UnionIterator(Iterator<Dataset> iter1, Iterator<Dataset> iter2)
    {
      this.iter1 = iter1;
      this.iter2 = iter2;
      this.iter = iter1;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext()
    {
      if (!iter.hasNext() && iter == iter1)
      {
        iter = iter2;
      }
      return iter.hasNext();
    }

    @Override
    @SuppressWarnings("squid:S899")
    public Dataset next()
    {
      this.hasNext(); //weiterschalten auf iter2 falls nötig
      return iter.next();
    }
  }
  
}
