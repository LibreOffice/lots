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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

/**
 * Ein DataFinder sucht Datensätze im übergebenen dsj, wobei in der Beschreibung der gesuchten Werte
 * Variablen in der Form "${varname}" verwendet werden können, die vor der Suche in einer anderen
 * Datenquelle aufgelöst werden. Die Auflösung erledigt durch die konkrete Klasse.
 */
public abstract class DataFinder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DataFinder.class);

  private DatasourceJoiner dsj;

  /**
   * Create a finder.
   *
   * @param dsj
   *          The data source to scan.
   */
  public DataFinder(DatasourceJoiner dsj)
  {
    this.dsj = dsj;
  }

  /**
   * Erwartet ein ConfigThingy, das ein oder zwei Schlüssel-/Wertpaare enthält (in der Form
   * "<KNOTEN>(<dbSpalte1> 'wert1' [<dbSpalte2> 'wert2'])" nach denen in der Datenquelle gesucht
   * werden soll. Die Beiden Wertpaare werden dabei UND verknüpft. Die Werte wert1 und wert2 können
   * über die Syntax "${name}" Variablen referenzieren, die vor der Suche aufgelöst werden.
   *
   * @param conf
   *          Das ConfigThingy, das die Suchabfrage beschreibt.
   * @return Die Anzahl der gefundenen Datensätze.
   */
  public QueryResults find(ConfigThingy conf)
  {
    List<Pair<String, String>> query = new ArrayList<>();

    for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy element = iter.next();
      query.add(new ImmutablePair<>(element.getName(), element.toString()));
    }

    if (query.size() > 2)
    {
      LOGGER.error(
          "Nur max zwei Schlüssel/Wert-Paare werden als Argumente für Suchanfragen akzeptiert!");
    }

    return find(query);
  }

  /**
   * Liefert den Wert zu einer Variable namens key und muss von jeder konkreten Finder-Klasse
   * implementiert werden.
   *
   * @param key
   *          Der Schlüssel, zu dem der Wert zurückgeliefert werden soll.
   * @return der zugehörige Wert zum Schlüssel key.
   */
  protected abstract String getValueForKey(String key);

  /**
   * Sucht in der Datenquelle nach Datensätzen wobei die beiden Suchbedingungen
   * (dbSpalte1==evaluate(value1) und dbSpalte2==evaluate(value2)) mit UND verknüpft sind - die
   * gefundenen Werte werden danach in die PAL kopiert.
   *
   * @param query
   *          value0: der Feldname über den nach dem evaluierten Wert von value gesucht wird.
   *          value1: wird vor der Suche mittels evaluate() evaluiert (d.h. evtl. vorhandene
   *          Variablen durch die entsprechenden Inhalte ersetzt ersetzt).
   *
   * @return die Anzahl der gefundenen Datensätze
   */
  public QueryResults find(List<Pair<String, String>> query)
  {
    List<Pair<String, String>> evaluatedQuery = new ArrayList<>(query.size());
    for (Pair<String, String> pair : query)
    {
      LOGGER.trace("{}.find({}, {})", this.getClass().getSimpleName(), pair.getKey(), pair.getValue() + ")");

      if (pair.getKey() == null || evaluate(pair.getKey()).isEmpty())
      {
        return null;
      }
      evaluatedQuery.add(ImmutablePair.of(evaluate(pair.getKey()), evaluate(pair.getValue())));
    }

    return dsj.find(dsj.buildQuery(evaluatedQuery));
  }

  /**
   * Ersetzt die Variablen in exp durch deren evaluierten Inhalt, wobei die Evaluierung über
   * getValueForKey() erfolgt, die von jeder konkreten Klasse implementiert wird. Evaluate() stellt
   * auch sicher, dass die von getValueForKey() zurückgelieferten Werte nicht selbst Variablen
   * enthalten können (indem die Variablenbegrenzer "${" und "}" durch "&lt;" bzw. "&gt;" ersetzt
   * werden.
   *
   * @param exp
   *          der zu evaluierende Ausdruck
   * @return Der evaluierte Ausdruck.
   */
  protected String evaluate(String exp)
  {
    final Pattern pattern = Pattern.compile("\\$\\{([^\\}]*)\\}");
    while (true)
    {
      Matcher m = pattern.matcher(exp);
      if (!m.find())
        break;
      String key = m.group(1);
      String value = getValueForKey(key);
      value = replaceVariableBoundaries(value);
      exp = m.replaceFirst(value);
    }
    return exp;
  }

  private String replaceVariableBoundaries(String value)
  {
    value = value.replaceAll("\\$\\{", "<");
    value = value.replaceAll("\\}", ">");
    return value;
  }

}
