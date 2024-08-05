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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.libreoffice.lots.config.ConfigThingy;

/**
 * A search strategy provides a list of templates for search queries for a given number of words,
 * which should be tried in sequence with the words until a result is found.
 */
public class SearchStrategy
{
  /**
   * Maps a word count to a list of {@link Query} objects that represent matching templates.
   */
  private Map<Integer, List<Query>> mapWordcountToListOfQuerys;

  /**
   * {@link #mapWordcountToListOfQuerys} is included by reference and
   * corresponding results from this map are returned by {@link #getTemplate(int)}.
   */
  
  private SearchStrategy()
  {
    this.mapWordcountToListOfQuerys = new HashMap<>();
  }

  /**
   * Parses the "SearchStrategy" section of conf and returns a corresponding
   * SearchStrategy.
   *
   * @param conf
   *          the {@link ConfigThingy} whose "SearchStrategy" section should be parsed.
   */
  public static SearchStrategy parse(ConfigThingy conf)
  {
    SearchStrategy strategy = new SearchStrategy();
    conf = conf.query("SearchStrategy");
    for (ConfigThingy searchConfig : conf)
    {
      for (ConfigThingy queryConf : searchConfig)
      {
        String datasource = queryConf.getName();
        List<QueryPart> listOfQueryParts = new ArrayList<>();
        int wordcount = 0;
        for (ConfigThingy qconf : queryConf)
        {
          String columnName = qconf.getName();
          String searchString = qconf.toString();
          Matcher m = Pattern.compile("\\$\\{suchanfrage[1-9]\\}").matcher(searchString);
          while (m.find())
          {
            int wordnum = searchString.charAt(m.end() - 2) - '0';
            if (wordnum > wordcount) {
              wordcount = wordnum;
            }
          }
          listOfQueryParts.add(new QueryPart(columnName, searchString));
        }

        strategy.addListOfQueryParts(wordcount, datasource, listOfQueryParts);
      }
    }

    return strategy;
  }

  private void addListOfQueryParts(int wordcount, String datasource, List<QueryPart> queryParts)
  {
    if (!mapWordcountToListOfQuerys.containsKey(wordcount))
    {
      mapWordcountToListOfQuerys.put(wordcount, new ArrayList<Query>());
    }
    List<Query> listOfQueries = mapWordcountToListOfQuerys.get(wordcount);
    listOfQueries.add(new Query(datasource, queryParts));
  }

  /**
   * Returns a list of {@link Query} objects, each representing a template for a
   * query that should be executed for a search request with wordcount words.
   * The queries should be executed in the order they appear in the list until one of them returns a result.
   *
   * @return <code>null</code> if no strategy is available for the given wordcount.
   */
  public List<Query> getTemplate(int wordcount)
  {
    return mapWordcountToListOfQuerys.get(wordcount);
  }
}
