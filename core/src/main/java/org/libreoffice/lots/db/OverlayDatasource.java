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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data source that supplements another data source with columns and can replace individual column values.
 * To create the set of result datasets, each record from SOURCE1 is used exactly once,
 * and each record from SOURCE2 can be used any number of times, including zero.
 * Differences from a proper data source include: <br>
 * <br>
 * a) Prevents a person from appearing twice just because there are two entries with traffic connections for their
 *    address<br>
 * b) Prevents a person from being removed because there is no traffic connection to their address <br>
 * c) The keys of the result records remain those from SOURCE1 and are not combined from SOURCE1 and SOURCE2.
 *    This prevents a record from being removed from the local sender list when the address is changed during cache refresh,
 *    because it cannot be found anymore. <br>
 * <br>
 * In the result data source, all columns are available under their original names (difference from AttachDatasource).
 * Conflicts are resolved using the MODE specifier (see lots.conf documentation)<br>
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OverlayDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OverlayDatasource.class);

  private String name;

  private String source1Name;

  private String source2Name;

  private Datasource source1;

  private Datasource source2;

  private List<String> schema;

  private Set<String> schema1;

  private Set<String> schema2;

  private String[] match1;

  private String[] match2;

  /**
   * Contains the names of all column names for which there is an i such that
   * Columnname.equals(match1[i]) and match1[i].equals(match2[i]).
   */
  private Set<String> commonMatchColumns;

  /**
   * true if mode is "so", "sO", "So" or "SO".
   */
  private boolean modeSO;

  /**
   * true if second char in 'MODE' is lower case.
   */
  private boolean treatEmptyStringsAsNull;

  /**
   * Creates a new OverlayDatasource.
   *
   * @param nameToDatasource
   *          Contains all data sources that have already been fully instantiated
   *          up to the time of defining this OverlayDatasource.
   * @param sourceDesc
   *          the 'DataSource' node containing the description of this OverlayDatasource.
   * @param context
   *          the context relative to which URLs should be resolved (currently not used).
   */
  public OverlayDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc,
      URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    String mode = parseConfig(sourceDesc, "MODE",
        () -> L.m("MODE-specification of datasource \"{0}\" is missing", name));

    String lcMode = mode.toLowerCase();
    if ("so".equals(lcMode))
      modeSO = true;
    else if ("os".equals(lcMode))
      modeSO = false;
    else
      throw new ConfigurationErrorException(
          L.m("Incorrect MODE-specification in datasource  \"{0}\": MODE \"{1}\" is not allowed", name,
              mode));

    treatEmptyStringsAsNull = Character.isLowerCase(mode.charAt(1));

    source1Name = parseConfig(sourceDesc, "SOURCE",
        () -> L.m("SOURCE of data source \"{0}\" is missing", name));
    source2Name = parseConfig(sourceDesc, "OVERLAY",
        () -> L.m("OVERLAY-specification of data source \"{0}\" is missing", name));

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source2Name));

    schema1 = new HashSet<>(source1.getSchema());
    schema2 = new HashSet<>(source2.getSchema());

    schema = new ArrayList<>(schema1);
    schema.addAll(schema2);

    ConfigThingy matchesDesc = sourceDesc.query("MATCH");
    int numMatches = matchesDesc.count();
    if (numMatches == 0)
      throw new ConfigurationErrorException(
          L.m("At least one MATCH-specification has to be made in data source \"{0}\"", name));

    match1 = new String[numMatches];
    match2 = new String[numMatches];
    commonMatchColumns = new HashSet<>();

    Iterator<ConfigThingy> iter = matchesDesc.iterator();
    for (int i = 0; i < numMatches; ++i)
    {
      ConfigThingy matchDesc = iter.next();
      if (matchDesc.count() != 2)
        throw new ConfigurationErrorException(
            L.m("Incorrect MATCH specification in data source \"{0}\"", name));

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
        throw new ConfigurationErrorException(L.m("Column \"{0}\" is not defined in schema", spalte1));

      if (!schema2.contains(spalte2))
        throw new ConfigurationErrorException(L.m("Column \"{0}\" is not defined in schema", spalte2));

      match1[i] = spalte1;
      match2[i] = spalte2;
      if (spalte1.equals(spalte2))
      {
        commonMatchColumns.add(spalte1);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return overlayColumns(source1.getDatasetsByKey(keys), DatasetPredicate.matchAll);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#find(java.util.List, long)
   */
  @Override
  public QueryResults find(List<QueryPart> query)
  {
    if (query.isEmpty())
    {
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    List<QueryPart> queryOnly1 = new ArrayList<>();
    List<QueryPart> queryOnly2 = new ArrayList<>();
    List<QueryPart> queryBoth = new ArrayList<>();
    for (QueryPart p : query)
    {
      if (schema1.contains(p.getColumnName()))
      {
        if (schema2.contains(p.getColumnName())
            /*
             * Important optimization: Conditions for columns that occur in both data sources,
             * which are matched (commonMatchColumns), must not be placed after queryBoth,
             * as this in the worst case leads to the (unnecessary) selection
             * of the most inefficient code path in this function.
             */
            && !commonMatchColumns.contains(p.getColumnName()))
          queryBoth.add(p);
        else
          queryOnly1.add(p);
      } else
      {
        queryOnly2.add(p);
      }
    }

    /*
     * The OVERLAY data source is usually subordinate, and column conditions for it only slightly
     * restrict the search results. Therefore, if we have at least one condition that exclusively
     * affects the main data source, we evaluate the query on this basis.
     */
    if (!queryOnly1.isEmpty())
    {
      QueryResults results = source1.find(queryOnly1);

      List<QueryPart> restQuery = new ArrayList<>(queryOnly2.size() + queryBoth.size());
      restQuery.addAll(queryBoth);
      restQuery.addAll(queryOnly2);

      return overlayColumns(results, DatasetPredicate.makePredicate(restQuery));
    } else if (!queryOnly2.isEmpty())
    { /*
       * In this case, we have conditions only for columns that either exist in both data sources or only
       * in the OVERLAY data source. In any case, we have at least one column condition that exclusively
       * affects the OVERLAY data source. We perform the search using the conditions of the OVERLAY data source,
       * then add all possible datasets from it, and then filter again with the column conditions for the common columns.
       */

      return overlayColumnsReversed(source2.find(queryOnly2),
          DatasetPredicate.makePredicate(queryBoth));
    } else
    { /*
       * Only columns present in both data sources are involved in the query.
       * Here it gets complicated because for each column value, the value can come from either
       * one or the other data source separately. Therefore, we cannot determine definitively which
       * datasets belong to the result space based on either SOURCE or OVERLAY.
       * We must determine the appropriate candidates based on both data sources.
       * Unfortunately, this creates the problem of duplicate elimination.
       * Since the keys of datasets are not necessarily unique, it is not so easy to perform this.
       */

      /*
       * Due to the problem that in the result dataset, column 1 may come from SOURCE and column 2 from OVERLAY,
       * we cannot search for multiple column conditions from queryBoth simultaneously.
       * We must select a column condition, search for it, and then filter the datasets
       * that actually match the complete search condition from query.
       */

      QueryPart qp = getMostRestrictingQueryPart(queryBoth);
      List<QueryPart> restrictingQuery = new ArrayList<>(1);
      restrictingQuery.add(qp);

      Predicate<Dataset> predicate = DatasetPredicate.makePredicate(query);

      QueryResults results1 = overlayColumns(source1.find(restrictingQuery), predicate);
      QueryResults results2 = overlayColumnsReversed(source2.find(restrictingQuery), predicate);

      /*
       * At this point, we have all the desired records. However, there may be overlaps between results1 and results2.
       * This is the aforementioned duplicate problem.
       * To eliminate duplicates, we proceed as follows:
       *
       * 1. Determine all record keys that appear in both results1 and results2.
       *
       * 2. Remove all records with keys from the first list (results1) from the results lists
       *
       * 3. In another query, determine all records with keys from the first list and add
       *    those that meet the filter condition back to the results.
       */
      HashSet<String> results1Keys = new HashSet<>();
      for (Dataset ds : results1)
      {
        results1Keys.add(ds.getKey());
      }

      List<Dataset> finalResults = new ArrayList<>(results1.size() + results2.size());

      List<String> dupKeys = new ArrayList<>();
      Iterator<Dataset> iter = results2.iterator();
      while (iter.hasNext())
      {
        Dataset ds = iter.next();
        String key = ds.getKey();
        if (results1Keys.contains(key))
          dupKeys.add(key);
        else
          finalResults.add(ds);
      }

      QueryResults results3 = getDatasetsByKey(dupKeys);

      for (Dataset ds : results3)
        if (predicate.test(ds))
        {
          finalResults.add(ds);
        }

      for (Dataset ds : results1)
        if (!dupKeys.contains(ds.getKey()))
        {
          finalResults.add(ds);
        }

      return new QueryResultsList(finalResults);
    }
  }

  /**
   * Tries to determine the QueryPart from the query (which must not be empty)
   * that most restricts the result set of a search query and returns it.
   * The criteria for this are the number of asterisks and the number of non-asterisk characters in the search string.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   *         TESTED
   */
  private QueryPart getMostRestrictingQueryPart(List<QueryPart> query)
  {
    QueryPart best = query.get(0); // Ensure that 'best' is always initialized
    // ist
    int bestStarCount = Integer.MAX_VALUE;
    int bestNonStarCount = -1;
    for (QueryPart qp : query)
    {
      String str = qp.getSearchString();
      int nonStarCount = str.length();
      int starCount = 0;
      if (str.length() > 0 && str.charAt(0) == '*')
      {
        ++starCount;
        --nonStarCount;
      }
      if (str.length() > 1 && str.charAt(str.length() - 1) == '*')
      {
        ++starCount;
        --nonStarCount;
      }

      if ((starCount == 0 && bestStarCount > 0) || (nonStarCount > bestNonStarCount)
          || (nonStarCount == bestNonStarCount && starCount < bestStarCount))
      {
        best = qp;
        bestStarCount = starCount;
        bestNonStarCount = nonStarCount;
      }
    }

    return best;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getName()
   */
  @Override
  public String getName()
  {
    return name;
  }

  private QueryResults overlayColumns(QueryResults results, Predicate<Dataset> filter)
  {
    List<Dataset> resultsWithOverlayments = new ArrayList<>(results.size());

    Iterator<Dataset> iter = results.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();

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
          resultsWithOverlayments.add(newDataset);
        }
      } else
      {
        Iterator<Dataset> appendixIter = appendix.iterator();
        while (appendixIter.hasNext())
        {
          newDataset = new ConcatDataset(ds, appendixIter.next());
          if (filter.test(newDataset))
          {
            resultsWithOverlayments.add(newDataset);
            break;
          }
        }
      }
    }

    return new QueryResultsList(resultsWithOverlayments);
  }

  private QueryResults overlayColumnsReversed(QueryResults results, Predicate<Dataset> filter)
  {
    List<ConcatDataset> resultsWithOverlayments = new ArrayList<>(results.size());

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

      for (Dataset prepend : prependix)
      {
        ConcatDataset newDataset = new ConcatDataset(prepend, ds);
        if (filter.test(newDataset))
        {
          resultsWithOverlayments.add(newDataset);
        }
      }
    }

    return new QueryResultsList(resultsWithOverlayments);
  }

  private class ConcatDataset implements Dataset
  {
    private Dataset dataset1;

    private Dataset dataset2; // kann null sein!

    /**
     * ds1' is always from 'SOURCE' and 'ds2' is from 'OVERLAY.
     *
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public ConcatDataset(Dataset ds1, Dataset ds2)
    {
      this.dataset1 = ds1;
      this.dataset2 = ds2;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column \"{0}\" is not defined in schema", columnName));

      Dataset ds1;
      Dataset ds2;
      Set<String> schemaOfPriorityDatasource;
      if (modeSO)
      {
        ds1 = this.dataset1;
        ds2 = this.dataset2;
        schemaOfPriorityDatasource = schema2;
      } else
      {
        ds1 = this.dataset2;
        ds2 = this.dataset1;
        schemaOfPriorityDatasource = schema1;
      }

      if (ds2 != null && schemaOfPriorityDatasource.contains(columnName))
      {
        String value = ds2.get(columnName);
        if (treatEmptyStringsAsNull && value != null && value.length() == 0)
          value = null;
        if (value != null)
        {
          return value;
        }
      }

      try
      {
        // ds1' can be null in the case where 'ds1 == this.ds2' (when 'modeSO == false')
        if (ds1 == null)
        {
          return null;
        }
        return ds1.get(columnName);
      } catch (ColumnNotFoundException x)
      {
        // The exception should not be thrown further because the column exists in the overall schema,
        // as tested at the beginning. If we reach this point,
        // it is only in the case that the value in 'ds2' is unset.
        return null;
      }
    }

    @Override
    public String getKey()
    {
      return dataset1.getKey();
    }
  }

}
