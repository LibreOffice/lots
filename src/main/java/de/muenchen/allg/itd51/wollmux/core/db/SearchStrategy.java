/*
 * Dateiname: SearchStrategy.java
 * Projekt  : WollMux
 * Funktion : Suchstrategie für Suchanfragen in Datenquellen
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

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
