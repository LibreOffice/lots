/*
 * Dateiname: Search.java
 * Projekt  : WollMux
 * Funktion : Stellt Methoden zum Suchen in Datenquellen zur Verfügung
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 18.03.2010 | BED | Erstellung
 * 11.11.2010 | BED | Leerstring als Sucheingabe wieder möglich
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 *
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in Datenquellen Suchen
 * durchzuführen.
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 */
public class Search
{

  /**
   * Führt die übergebene Suchanfrage gemäß der übergebenen Suchstrategie aus und
   * liefert die Ergebnisse in einem {@link QueryResults}-Objekt zurück. Falls einer
   * der übergebenen Parameter <code>null</code> ist oder falls der queryString leer
   * ist, wird <code>null</code> zurückgeliefert.
   *
   * @param queryString
   *          die Suchanfrage
   * @param searchStrategy
   *          die zu verwendende Suchstrategie
   * @param dj
   *          die virtuelle Datenbank (siehe {@link DatasourceJoiner}), in der
   *          gesucht werden soll
   * @param useDjMainDatasource
   *          gibt an, ob unabhängig von den eventuell in der Suchstrategie
   *          festgelegten Datenquellen auf jeden Fall immer in der Hauptdatenquelle
   *          von dj gesucht werden soll. Wenn hier <code>true</code> übergeben wird,
   *          enthalten die als Ergebnis der Suche zurückgelieferten QueryResults auf
   *          jeden Fall {@link DJDataset}s.
   *
   * @throws TimeoutException
   *           falls ein Fehler beim Bearbeiten der Suche auftritt oder die Anfrage
   *           nicht rechtzeitig beendet werden konnte. In letzterem Fall ist das
   *           Werfen dieser Exception jedoch nicht Pflicht und die bei der Suche
   *           verwendete Datenquelle kann stattdessen den Teil der Ergebnisse
   *           zurückliefern, die in der gegebenen Zeit gewonnen werden konnten.
   * @throws IllegalArgumentException
   *           falls eine Datenquelle, in der gesucht werden soll, nicht existiert
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static QueryResults search(String queryString,
      SearchStrategy searchStrategy, DatasourceJoiner dj, boolean useDjMainDatasource)
      throws TimeoutException, IllegalArgumentException
  {
    if (queryString == null || searchStrategy == null || dj == null)
      return null;

    List<Query> queries = parseQuery(searchStrategy, queryString);

    QueryResults results = null;
    List<QueryResults> listOfQueryResultsList = new ArrayList<QueryResults>();

    for (Query query : queries)
    {
      if (query.numberOfQueryParts() == 0)
      {
        results = (useDjMainDatasource ? dj.getContentsOfMainDatasource()
            : dj.getContentsOf(query.getDatasourceName()));
      } else
      {
        results = (useDjMainDatasource ? dj.find(query.getQueryParts())
            : dj.find(query));
      }
      listOfQueryResultsList.add(results);
    }
    return mergeListOfQueryResultsList(listOfQueryResultsList);
  }

  /**
   * Führt die Ergebnissmengen zusammen. Dabei werden mehrfache Ergebnisse ausgefiltert
   * @return bereinigte Ergebnisliste
   * @author Thien Nghiem Tran
   */
  private static QueryResults mergeListOfQueryResultsList(List<QueryResults> listOfQueryResultsList)
  {
    QueryResultsSet results = new QueryResultsSet(new Comparator<Dataset>()
    {

      @Override
      public int compare(Dataset o1, Dataset o2)
      {
        if (o1.getClass() == o2.getClass() && o1.getKey().equals(o2.getKey()))
        {
          return 0;
        }
        return 1;
      }
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
   * Liefert zur Anfrage queryString eine Liste von {@link Query}s, die der Reihe
   * nach probiert werden sollten, gemäß der Suchstrategie searchStrategy (siehe
   * {@link SearchStrategy#parse(ConfigThingy)}). Gibt es für die übergebene Anzahl
   * Wörter keine Suchstrategie, so wird solange das letzte Wort entfernt bis
   * entweder nichts mehr übrig ist oder eine Suchstrategie für die Anzahl Wörter
   * gefunden wurde.
   *
   * @return die leere Liste falls keine Liste bestimmt werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static List<Query> parseQuery(SearchStrategy searchStrategy,
      String queryString)
  {
    List<Query> queryList = new ArrayList<Query>();

    /*
     * Kommata durch Space ersetzen (d.h. "Benkmann,Matthias" -> "Benkmann Matthias")
     */
    queryString = queryString.replaceAll(",", " ");

    /*
     * Suchstring zerlegen.
     */
    String[] queryArray = queryString.trim().split("\\p{Space}+");

    /*
     * Benutzerseitig wir nur ein einzelnes Sternchen am Ende eines Wortes
     * akzeptiert. Deswegen entferne alle anderen Sternchen. Ein Punkt am Ende eines
     * Wortes wird als Abkürzung interpretiert und durch Sternchen ersetzt. Ausserdem
     * entferne leere Wörter und berechne neue Arraylänge.
     */
    int count = queryArray.length;
    for (int i = 0; i < queryArray.length && queryArray[i] != null; ++i)
    {
      boolean suffixStar =
        queryArray[i].endsWith("*") || queryArray[i].endsWith(".");
      if (queryArray[i].endsWith("."))
        queryArray[i] = queryArray[i].substring(0, queryArray[i].length() - 1);

      queryArray[i] = queryArray[i].replaceAll("\\*", "");
      if (queryArray[i].length() == 0)
      {
        for (int j = i + 1; j < queryArray.length; ++j)
          queryArray[j - 1] = queryArray[j];

        --count;
        --i;
        queryArray[queryArray.length - 1] = null;
      }
      else
      {
        if (suffixStar)
        {
          queryArray[i] = queryArray[i] + "*";
        }
      }
    }

    /*
     * Passende Suchstrategie finden; falls nötig dazu Wörter am Ende weglassen.
     */
    while (count >= 0 && searchStrategy.getTemplate(count) == null)
      --count;

    /*
     * keine Suchstrategie gefunden
     */
    if (count < 0)
    {
      return queryList;
    }

    List<Query> templateList = searchStrategy.getTemplate(count);
    Iterator<Query> iter = templateList.iterator();
    while (iter.hasNext())
    {
      Query template = iter.next();
      queryList.add(resolveTemplate(template, queryArray, count));
    }

    return queryList;
  }

  /**
   * Nimmt ein Template für eine Suchanfrage entgegen (das Variablen der Form
   * "${suchanfrageX}" enthalten kann) und instanziiert es mit Wörtern aus words,
   * wobei nur die ersten wordcount Einträge von words beachtet werden.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static Query resolveTemplate(Query template, String[] words, int wordcount)
  {
    String dbName = template.getDatasourceName();
    List<QueryPart> listOfQueryParts = new ArrayList<QueryPart>();
    Iterator<QueryPart> qpIter = template.iterator();
    while (qpIter.hasNext())
    {
      QueryPart templatePart = qpIter.next();
      String str = templatePart.getSearchString();

      for (int i = 0; i < wordcount; ++i)
      {
        str =
          str.replaceAll("\\$\\{suchanfrage" + (i + 1) + "\\}", words[i].replaceAll(
            "\\$", "\\\\\\$"));
      }

      QueryPart part = new QueryPart(templatePart.getColumnName(), str);
      listOfQueryParts.add(part);
    }
    return new Query(dbName, listOfQueryParts);
  }

}
