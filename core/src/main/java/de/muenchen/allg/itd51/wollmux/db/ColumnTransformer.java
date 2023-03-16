/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.Values;

/**
 * Takes a dataset and provides pseudo-columns calculated from its columns using WollMux functions.
 */

public class ColumnTransformer
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ColumnTransformer.class);

  /**
   * The names of all pseudo-columns defined for this ColumnTransformer.
   */
  private Set<String> schema = new HashSet<>();

  /**
   * Maps the name of a pseudo-column to its associated function.
   */
  private Map<String, Function> columnTranslations = new HashMap<>();

  /**
   * 
   * Initializes a ColumnTransformer with all sections returned by trafoConf,query(nodeName, 1).
   * trafoConf has the following structure:
   * 
   * <pre>
   AnyIdentifier(
       Name1(WollMux-Function1 )
       Name2( WollMux-Function2 )
       ...
       )
   * </pre>
   * 
   * NameX is the name that must be passed to {@link #get(String, Dataset)} to access the
   * corresponding function value. Calls to the VALUE basic function used within the WollMux
   * functions always refer to the untransformed columns of the Dataset to be translated, NEVER to
   * pseudo-columns. If the same NameX is used multiple times, only the last definition applies.
   */

  public ColumnTransformer(Map<String, Function> trafos)
  {
    addTrafos(trafos);
  }

  /**
   * Creates a ColumnTransformer without pseudo-columns. This simply forwards
   * {@link #get(String, Dataset)} queries to the corresponding dataset.
   */

  public ColumnTransformer()
  {
  }

  /**
   * Returns true if a pseudo column with the given name is defined, i.e. if
   * {@link #get(String, Dataset)} returns a computed value for this name, rather than directly
   * returning the value of the {@link Dataset}.
   */

  public boolean hasPseudoColumn(String name)
  {
    return schema.contains(name);
  }

  private void addTrafos(Map<String, Function> trafos)
  {
    for (Map.Entry<String, Function> trafo : trafos.entrySet())
    {
      columnTranslations.put(trafo.getKey(), trafo.getValue());
      schema.add(trafo.getKey());
    }
  }

  /**
   * Returns the set of all names of pseudo-columns that are defined, i.e. all names for which
   * {@link #get(String, Dataset)} returns a computed value and not the value of the {@link Dataset}
   * directly.
   */

  public List<String> getSchema()
  {
    return new ArrayList<>(schema);
  }

  /**
   * Returns the value of the pseudo column columnName, which is calculated based on the
   * transformation rules from the {@link Dataset} ds. If there is no transformation rule for
   * columnName, the value of the columnName column from ds is returned directly (null if not set).
   * 
   * @throws ColumnNotFoundException
   *           if neither a transformation rule is defined for columnName nor ds has a column with
   *           that name.
   */

  public String get(String columnName, Dataset ds) throws ColumnNotFoundException
  {
    Function func = columnTranslations.get(columnName);
    if (func == null)
    {
      return ds.get(columnName);
    }
    return func.getResult(new DatasetValues(ds));
  }

  /**
   * Returns a {@link Dataset} that represents a transformed view of ds. ATTENTION! The calculation
   * of the columns is performed on-demand, i.e. later calls to {@link #addTrafos(Map)} will affect
   * the returned {@link Dataset}.
   */

  public Dataset transform(Dataset ds)
  {
    return new TransformedDataset(ds);
  }

  /**
   * Returns {@link QueryResults} representing a transformed view of {@code qres}. WARNING! The
   * calculation of the {@link Dataset}s is performed on-demand, i.e. subsequent calls to
   * {@link #addTrafos(Map)} will affect the {@link Dataset}s of the {@link QueryResults}.
   */

  public QueryResults transform(QueryResults qres)
  {
    return new TranslatedQueryResults(qres);
  }

  /**
   * Provides the columns of a dataset as values.
   */
  private static class DatasetValues implements Values
  {
    private Dataset ds;

    public DatasetValues(Dataset ds)
    {
      this.ds = ds;
    }

    @Override
    public boolean hasValue(String id)
    {
      try
      {
        ds.get(id);
      } catch (ColumnNotFoundException x)
      {
        LOGGER.trace("", x);
        return false;
      }
      return true;
    }

    @Override
    public String getString(String id)
    {
      String str = null;
      try
      {
        str = ds.get(id);
      } catch (ColumnNotFoundException x)
      {
        LOGGER.trace("", x);
      }

      return str == null ? "" : str;
    }

    @Override
    public boolean getBoolean(String id)
    {
      return "true".equalsIgnoreCase(getString(id));
    }
  }

  /**
   * Applies column transformations to QueryResults and provides the result again as QueryResults.
   */

  private class TranslatedQueryResults implements QueryResults
  {
    /**
     * The original {@link QueryResults}.
     */
    private QueryResults qres;

    /**
     * The QueryResults res are translated using the columnTransformer.
     */

    public TranslatedQueryResults(QueryResults res)
    {
      qres = res;
    }

    @Override
    public int size()
    {
      return qres.size();
    }

    @Override
    public Iterator<Dataset> iterator()
    {
      return new Iter();
    }

    @Override
    public boolean isEmpty()
    {
      return qres.isEmpty();
    }

    private class Iter implements Iterator<Dataset>
    {
      private Iterator<Dataset> iterator;

      public Iter()
      {
        iterator = qres.iterator();
      }

      @Override
      public boolean hasNext()
      {
        return iterator.hasNext();
      }

      @Override
      public Dataset next()
      {
        Dataset ds = iterator.next();
        return new TransformedDataset(ds);
      }

      @Override
      public void remove()
      {
        iterator.remove();
      }

    }
  }

  private class TransformedDataset implements Dataset
  {
    private Dataset ds;

    public TransformedDataset(Dataset ds)
    {
      this.ds = ds;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      return ColumnTransformer.this.get(columnName, ds);
    }

    @Override
    public String getKey()
    {
      return ds.getKey();
    }

  }
}
