/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Eine Suchstrategie liefert für eine gegebene Wortzahl eine Liste von Templates für
 * Suchanfragen, die der Reihe nach mit den Wörtern probiert werden sollen bis ein
 * Ergebnis gefunden ist.
 */
public class SearchStrategy
{
  /**
   * Bildet eine Wortanzahl ab auf eine Liste von {@link Query}-Objekten, die
   * passende Templates darstellen.
   */
  private Map<Integer, List<Query>> mapWordcountToListOfQuerys;

  /**
   * {@link #mapWordcountToListOfQuerys} wird per Referenz eingebunden und
   * entsprechende Ergebnisse aus dieser Map werden von {@link #getTemplate(int)}
   * zurückgeliefert.
   */
  private SearchStrategy()
  {
    this.mapWordcountToListOfQuerys = new HashMap<>();
  }

  /**
   * Parst den "Suchstrategie"-Abschnitt von conf und liefert eine entsprechende
   * SearchStrategy.
   * 
   * @param conf
   *          das {@link ConfigThingy}, dessen "Suchstrategie"-Abschnitt geparst
   *          werden soll.
   */
  public static SearchStrategy parse(ConfigThingy conf)
  {
    SearchStrategy strategy = new SearchStrategy();
    conf = conf.query("Suchstrategie");
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
   * Liefert eine Liste von {@link Query}-Objekten, die jeweils ein Template für eine
   * Query sind, die bei einer Suchanfrage mit wordcount Wörtern durchgeführt werden
   * soll. Die Querys sollen in der Reihenfolge in der sie in der Liste stehen
   * durchgeführt werden solange bis eine davon ein Ergebnis liefert.
   * 
   * @return <code>null</code> falls keine Strategie für den gegebenen wordcount
   *         vorhanden ist.
   */
  public List<Query> getTemplate(int wordcount)
  {
    return mapWordcountToListOfQuerys.get(wordcount);
  }
}
