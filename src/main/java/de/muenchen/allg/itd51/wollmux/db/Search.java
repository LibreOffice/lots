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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in Datenquellen Suchen durchzuführen.
 */
public class Search
{

  private Search()
  {
    // hide implicit public constructor
  }

  /**
   * Führt die übergebene Suchanfrage gemäß der übergebenen Suchstrategie aus und liefert die
   * Ergebnisse in einem {@link QueryResults}-Objekt zurück. Falls einer der übergebenen Parameter
   * <code>null</code> ist oder falls der queryString leer ist, wird <code>null</code>
   * zurückgeliefert.
   * 
   * @param queryString
   *          die Suchanfrage
   * @param searchStrategy
   *          die zu verwendende Suchstrategie
   * @param datasources
   *          Data source to use.
   * @throws IllegalArgumentException
   *           falls eine Datenquelle, in der gesucht werden soll, nicht existiert
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
   * Führt die Ergenismengen zusammen. Dabei werden mehrfache Ergebnisse ausgefiltert.
   * 
   * @return bereinigte Ergebnisliste.
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
   * Liefert zur Anfrage queryString eine Liste von {@link Query}s, die der Reihe nach probiert
   * werden sollten, gemäß der Suchstrategie searchStrategy (siehe
   * {@link SearchStrategy#parse(ConfigThingy)}). Gibt es für die übergebene Anzahl Wörter keine
   * Suchstrategie, so wird solange das letzte Wort entfernt bis entweder nichts mehr übrig ist oder
   * eine Suchstrategie für die Anzahl Wörter gefunden wurde.
   * 
   * @return die leere Liste falls keine Liste bestimmt werden konnte.
   */
  private static List<Query> parseQuery(SearchStrategy searchStrategy, String queryString)
  {
    List<Query> queryList = new ArrayList<>();

    // Kommata durch Space ersetzen (d.h. "Benkmann,Matthias" -> "Benkmann
    // Matthias")
    queryString = queryString.replaceAll(",", " ");

    // Suchstring zerlegen.
    Stream<String> queryStream = Arrays.stream(queryString.trim().split("\\p{Space}+"));
    // Formatieren und leere Wörter entfernen
    String[] queryArray = queryStream.map(Search::formatQuery).filter(query -> query.length() != 0)
        .toArray(String[]::new);

    int count = queryArray.length;

    // Passende Suchstrategie finden; falls nötig dazu Wörter am Ende weglassen.
    while (count >= 0 && searchStrategy.getTemplate(count) == null)
      --count;

    // keine Suchstrategie gefunden
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
   * Benutzerseitig wir nur ein einzelnes Sternchen am Ende eines Wortes akzeptiert. Deswegen
   * entferne alle anderen Sternchen. Ein Punkt am Ende eines Wortes wird als Abkürzung
   * interpretiert und durch Sternchen ersetzt.
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
   * Nimmt ein Template für eine Suchanfrage entgegen (das Variablen der Form "${suchanfrageX}"
   * enthalten kann) und instanziiert es mit Wörtern aus words, wobei nur die ersten wordcount
   * Einträge von words beachtet werden.
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
