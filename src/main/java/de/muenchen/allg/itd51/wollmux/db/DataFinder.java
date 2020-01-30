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

import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Ein DataFinder sucht Datensätze im übergebenen dsj, wobei in der Beschreibung der gesuchten Werte
 * Variablen in der Form "${varname}" verwendet werden können, die vor der Suche in einer anderen
 * Datenquelle aufgelöst werden. Die Auflösung erledigt durch die konkrete Klasse.
 */
public abstract class DataFinder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DataFinder.class);

  private DatasourceJoiner dsj;

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
    for (Pair<String, String> pair : query)
    {
      LOGGER.trace(this.getClass().getSimpleName() + ".find(" + pair.getKey() + ", "
          + pair.getValue() + ")");

      if (pair.getKey() == null || evaluate(pair.getKey()).isEmpty())
      {
        return null;
      }
    }

    return dsj.find(dsj.buildQuery(query));
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
    final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");
    while (true)
    {
      Matcher m = VAR_PATTERN.matcher(exp);
      if (!m.find())
        break;
      String key = m.group(1);
      String value = getValueForKey(key);
      // keine Variablenbegrenzer "${" und "}" in value zulassen:
      value = value.replaceAll("\\$\\{", "<");
      value = value.replaceAll("\\}", ">");
      exp = m.replaceFirst(value);
    }
    return exp;
  }

}
