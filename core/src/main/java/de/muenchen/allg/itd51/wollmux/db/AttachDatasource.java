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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * 
 * A data source that adds columns to another data source. To create the set of result data records,
 * each record from SOURCE1 is used exactly once, and each record from SOURCE2 is used any number of
 * times (including zero times). Differences from a proper join:<br>
 * 
 * <br>
 * a) Prevents a person from appearing twice just because there are 2 entries for transport
 * connections to their address.<br>
 * b) Prevents a person from being excluded because there is no transport connection to their
 * address.<br>
 * c) The keys of the result data records remain those from SOURCE1 and are not combined from
 * SOURCE1 and SOURCE2. This prevents a record from being removed from the local sender list due to
 * a change in address, because it cannot be found during cache refresh.<br>
 * 
 * <br>
 * In the result data source, all columns from SOURCE1 can be found under their original name, and
 * all columns from SOURCE2 can be found under the name of SOURCE2 concatenated with "."
 * concatenated with the column name.<br>
 * 
 * <br>
 * Argument against automatic renaming/aliasing of columns from SOURCE2 whose name does not conflict
 * with a column from SOURCE1:<br>
 * 
 * <br>
 * The alias would disappear if the SOURCE1 source is later extended with a column with the
 * corresponding name. Definitions that used the alias would then silently use the column from
 * SOURCE1, which can lead to difficult-to-find errors.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AttachDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(AttachDatasource.class);

  private static final String CONCAT_SEPARATOR = "__";

  private String name;

  private String source1Name;

  private String source2Name;

  private Datasource source1;

  private Datasource source2;

  private List<String> schema;

  private String[] match1;

  private String[] match2;

  private String source2Prefix;

  /**
   * Creates a new AttachDatasource.
   * 
   * @param nameToDatasource
   *          contains all data sources that have already been fully instantiated up to the point of
   *          defining this AttachDatasource.
   * @param sourceDesc
   *          the "data source" node that contains the description of this AttachDatasource.
   * @param context
   *          the context in which URLs should be resolved (not currently used).
   */

  public AttachDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc, URL context)
  {
    name = sourceDesc.get("NAME", ConfigurationErrorException.class, L.m("NAME of data source is missing")).toString();
    source1Name = sourceDesc
        .get("SOURCE", ConfigurationErrorException.class, L.m("SOURCE1 of data source \"{0}\" is missing", name))
        .toString();
    source2Name = sourceDesc.get("ATTACH", ConfigurationErrorException.class,
        L.m("ATTACH specification of data source {0} is missing", name)).toString();
    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source2Name));

    List<String> schema1 = source1.getSchema();
    List<String> schema2 = source2.getSchema();

    source2Prefix = source2Name + CONCAT_SEPARATOR;

    schema = new ArrayList<>(schema1);
    for (String spalte : schema2)
    {
      spalte = source2Prefix + spalte;
      if (schema1.contains(spalte))
        throw new ConfigurationErrorException(
            L.m("Collision with column \"{0}\" from data source \"{1}\"", spalte, source1Name));

      schema.add(spalte);
    }

    ConfigThingy matchesDesc = sourceDesc.query("MATCH");
    int numMatches = matchesDesc.count();
    if (numMatches == 0)
      throw new ConfigurationErrorException(
          L.m("At least one MATCH-specification has to be made in data source \"{0}\"", name));

    match1 = new String[numMatches];
    match2 = new String[numMatches];

    Iterator<ConfigThingy> iter = matchesDesc.iterator();
    for (int i = 0; i < numMatches; ++i)
    {
      ConfigThingy matchDesc = iter.next();
      if (matchDesc.count() != 2)
        throw new ConfigurationErrorException(L.m("Incorrect MATCH specification in data source \"{0}\"", name));

      String spalte1 = "";
      String spalte2 = "";
      try
      {
        spalte1 = matchDesc.getFirstChild().toString();
        spalte2 = matchDesc.getLastChild().toString();
      } catch (NodeNotFoundException x)
      {
        LOGGER.trace("", x);
      }

      if (!schema1.contains(spalte1))
        throw new ConfigurationErrorException("Spalte " + spalte1 + " ist nicht im Schema.");

      if (!schema2.contains(spalte2))
        throw new ConfigurationErrorException("Spalte " + spalte2 + " ist nicht im Schema.");

      match1[i] = spalte1;
      match2[i] = spalte2;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util. Collection, long)
   */

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return attachColumns(source1.getDatasetsByKey(keys), DatasetPredicate.matchAll);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    List<QueryPart> query1 = new ArrayList<>(query.size() / 2);
    List<QueryPart> query2 = new ArrayList<>(query.size() / 2);
    List<QueryPart> query2WithPrefix = new ArrayList<>(query.size() / 2);
    for (QueryPart p : query)
    {
      if (p.getColumnName().startsWith(source2Prefix))
      {
        query2.add(new QueryPart(p.getColumnName().substring(source2Prefix.length()), p.getSearchString()));
        query2WithPrefix.add(p);
      } else
      {
        query1.add(p);
      }
    }

    /*
     * The ATTACH data source is usually only subordinate, and column conditions for it only
     * minimally restrict the search results. Therefore, if we have at least one condition on the
     * main data source, we evaluate the request on this data source.
     */

    if (!query1.isEmpty())
    {
      QueryResults results = source1.find(query1);

      return attachColumns(results, DatasetPredicate.makePredicate(query2WithPrefix));
    } else
    {
      return attachColumnsReversed(source2.find(query2));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */

  @Override
  public String getName()
  {
    return name;
  }

  private QueryResults attachColumns(QueryResults results, Predicate<Dataset> filter)
  {
    List<Dataset> resultsWithAttachments = new ArrayList<>(results.size());

    for (Dataset ds : results)
    {
      List<QueryPart> query = new ArrayList<>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try
        {
          query.add(new QueryPart(match2[i], ds.get(match1[i])));
        } catch (ColumnNotFoundException x)
        {
          LOGGER.error("", x);
        }
      }

      QueryResults appendix = source2.find(query);

      Dataset newDataset;

      if (appendix.size() == 0)
      {
        newDataset = new ConcatDataset(ds, null);
        if (filter.test(newDataset))
        {
          resultsWithAttachments.add(newDataset);
        }
      } else
      {
        for (Dataset appenixElement : appendix)
        {
          newDataset = new ConcatDataset(ds, appenixElement);
          if (filter.test(newDataset))
          {
            resultsWithAttachments.add(newDataset);
            break;
          }
        }
      }
    }

    return new QueryResultsList(resultsWithAttachments);
  }

  private QueryResults attachColumnsReversed(QueryResults results)
  {
    List<ConcatDataset> resultsWithAttachments = new ArrayList<>(results.size());

    for (Dataset ds : results)
    {
      List<QueryPart> query = new ArrayList<>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try
        {
          query.add(new QueryPart(match1[i], ds.get(match2[i])));
        } catch (ColumnNotFoundException x)
        {
          LOGGER.error("", x);
        }
      }

      QueryResults prependix = source1.find(query);

      if (prependix.size() > 0)
      {
        for (Dataset ds1 : prependix)
        {
          resultsWithAttachments.add(new ConcatDataset(ds1, ds));
        }
      }
    }

    return new QueryResultsList(resultsWithAttachments);
  }

  private class ConcatDataset implements Dataset
  {
    private Dataset ds1;

    private Dataset ds2; // kann null sein!

    public ConcatDataset(Dataset ds1, Dataset ds2)
    {
      this.ds1 = ds1;
      this.ds2 = ds2; // kann null sein!
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column \"{0}\" is not defined in schema", columnName));

      if (columnName.startsWith(source2Prefix))
      {
        if (ds2 == null)
        {
          return null;
        }
        return ds2.get(columnName.substring(source2Prefix.length()));
      } else
      {
        return ds1.get(columnName);
      }
    }

    @Override
    public String getKey()
    {
      return ds1.getKey();
    }
  }

}
