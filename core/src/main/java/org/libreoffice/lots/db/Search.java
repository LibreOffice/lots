/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.libreoffice.lots.config.ConfigThingy;

/**
 * This class provides methods to perform searches in data sources.
 */
public class Search
{

  private Search()
  {
    // hide implicit public constructor
  }

  /**
   * Executes the given search query according to the given search strategy and returns the
   * results in one {@link QueryResults}-object back. If one of the passed parameters
   * <code>null</code> is or if the queryString is empty, will <code>null</code>
   * Delivered back.
   *
   * @param queryString
   *          the search query
   * @param searchStrategy
   *          the search strategy to use
   * @param datasources
   *          Data source to use.
   * @throws IllegalArgumentException
   *           if a data source in which to search does not exist
   * @return Results as an Iterable of Dataset as {@link QueryResults}
   */
  public static QueryResults search(String queryString, SearchStrategy searchStrategy,
      Map<String, Datasource> datasources)
  {
    if (queryString == null || searchStrategy == null || datasources == null)
    {
      return null;
    }

    List<Query> queries = parseQuery(searchStrategy, queryString);

    QueryResults results = null;
    List<QueryResults> listOfQueryResultsList = new ArrayList<>();

    for (Query query : queries)
    {
      if (query.numberOfQueryParts() == 0)
      {
        results = datasources.get(query.getDatasourceName()).getContents();
      } else
      {
        results = datasources.get(query.getDatasourceName()).find(query.getQueryParts());
      }
      listOfQueryResultsList.add(results);
    }
    return mergeListOfQueryResultsList(listOfQueryResultsList);
  }

  /**
   * Merges the sets of results. Multiple results are filtered out.
   *
   * @return cleaned result list.
   */
  private static QueryResults mergeListOfQueryResultsList(List<QueryResults> listOfQueryResultsList)
  {
    QueryResultsSet results = new QueryResultsSet((o1, o2) -> {
      if (o1.getClass() == o2.getClass() && o1.getKey() == o2.getKey())
      {
        return 0;
      }
      return 1;
    });

    if (listOfQueryResultsList.size() == 1)
    {
      return listOfQueryResultsList.get(0);
    } else
    {
      for (QueryResults queryResults : listOfQueryResultsList)
      {
        results.addAll(queryResults);
      }
    }

    return results;
  }

  /**
   * Returns a list of {@link Query}s for the queryString, which are tried in order
   * should be, according to the search strategy searchStrategy (Please refer
   * {@link SearchStrategy#parse(ConfigThingy)}). There are none for the number of words passed
   * Search strategy, the last word is removed until there is either nothing left or
   * a search strategy for the number of words was found.
   *
   * @return the empty list if no list could be determined.
   */
  private static List<Query> parseQuery(SearchStrategy searchStrategy, String queryString)
  {
    List<Query> queryList = new ArrayList<>();

    // Replace commas with space (i.e. "Benkmann,Matthias" -> "Benkmann
    // Matthias")
    queryString = queryString.replaceAll(",", " ");

    // Decompose search string.
    Stream<String> queryStream = Arrays.stream(queryString.trim().split("\\p{Space}+"));
    // Format and remove empty words
    String[] queryArray = queryStream.map(Search::formatQuery).filter(query -> query.length() != 0)
        .toArray(String[]::new);

    int count = queryArray.length;

    // Find a suitable search strategy; If necessary, leave out words at the end.
    while (count >= 0 && searchStrategy.getTemplate(count) == null)
      --count;

    // no search strategy found
    if (count < 0)
    {
      return queryList;
    }

    List<Query> templateList = searchStrategy.getTemplate(count);
    for (Query template : templateList)
    {
      queryList.add(resolveTemplate(template, queryArray, count));
    }

    return queryList;
  }

  /**
   * On the user side, only a single asterisk at the end of a word is accepted. Because of this
   * remove all other asterisks. A period at the end of a word is called an abbreviation
   * interpreted and replaced with asterisks.
   */
  private static String formatQuery(String query)
  {
    boolean suffixStar = query.endsWith("*") || query.endsWith(".");
    String modifiedQuery = query;
    if (query.endsWith("."))
    {
      modifiedQuery = query.substring(0, query.length() - 1);
    }
    modifiedQuery = modifiedQuery.replaceAll("\\*", "");
    if (suffixStar && query.length() != 0)
    {
      modifiedQuery += "*";
    }
    return modifiedQuery;
  }

  /**
   * Accepts a template for a search query (the variable of the form "${searchX}"
   * can contain) and instantiates it with words from words, where only the first wordcount
   * Entries of words are respected.
   */
  private static Query resolveTemplate(Query template, String[] words, int wordcount)
  {
    String dbName = template.getDatasourceName();
    List<QueryPart> listOfQueryParts = new ArrayList<>();
    Iterator<QueryPart> qpIter = template.iterator();
    while (qpIter.hasNext())
    {
      QueryPart templatePart = qpIter.next();
      String str = templatePart.getSearchString();

      for (int i = 0; i < wordcount; ++i)
      {
        str = str.replaceAll("\\$\\{suchanfrage" + (i + 1) + "\\}",
            words[i].replaceAll("\\$", "\\\\\\$"));
      }

      QueryPart part = new QueryPart(templatePart.getColumnName(), str);
      listOfQueryParts.add(part);
    }
    return new Query(dbName, listOfQueryParts);
  }

}
