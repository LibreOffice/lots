package de.muenchen.allg.itd51.wollmux.db;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.IllegalArgumentException;

import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Ein DataFinder sucht Datensätze im übergebenen dsj, wobei in der Beschreibung
 * der gesuchten Werte Variablen in der Form "${varname}" verwendet werden
 * können, die vor der Suche in einer anderen Datenquelle aufgelöst werden. Die
 * Auflösung erledigt durch die konkrete Klasse.
 */
public abstract class DataFinder
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DataFinder.class);

  private DatasourceJoiner dsj;

  public DataFinder(DatasourceJoiner dsj)
  {
    this.dsj = dsj;
  }

  /**
   * Erwartet ein ConfigThingy, das ein oder zwei Schlüssel-/Wertpaare enthält
   * (in der Form "<KNOTEN>(<dbSpalte1> 'wert1' [<dbSpalte2> 'wert2'])" nach
   * denen in der Datenquelle gesucht werden soll. Die Beiden Wertpaare werden
   * dabei UND verknüpft. Die Werte wert1 und wert2 können über die Syntax
   * "${name}" Variablen referenzieren, die vor der Suche aufgelöst werden.
   *
   * @param conf
   *          Das ConfigThingy, das die Suchabfrage beschreibt.
   * @return Die Anzahl der gefundenen Datensätze.
   */
  public int find(ConfigThingy conf)
  {
    int count = 0;
    String id1 = "";
    String id2 = "";
    String value1 = "";
    String value2 = "";
    for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy element = iter.next();
      if (count == 0)
      {
        id1 = element.getName();
        value1 = element.toString();
        count++;
      } else if (count == 1)
      {
        id2 = element.getName();
        value2 = element.toString();
        count++;
      } else
      {
        LOGGER.error(L.m(
            "Nur max zwei Schlüssel/Wert-Paare werden als Argumente für Suchanfragen akzeptiert!"));
      }
    }

    if (count == 1)
    {
      return find(id1, value1);
    } else if (count == 2)
    {
      return find(id1, value1, id2, value2);
    }
    return 0;
  }

  /**
   * Sucht in der Datenquelle nach Datensätzen deren Feld dbSpalte den
   * evaluierten Wert von value enthält und überträgt die gefundenen Werte in die
   * PAL.
   *
   * @param dbSpalte
   *          der Feldname über den nach dem evaluierten Wert von value gesucht
   *          wird.
   * @param value
   *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
   *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
   *          ersetzt).
   * @return die Anzahl der gefundenen Datensätze
   */
  protected int find(String dbSpalte, String value)
  {
    LOGGER.trace(this.getClass().getSimpleName() + ".tryToFind(" + dbSpalte
        + " '" + value + "')");
    try
    {
      String v = evaluate(value);
      if (v.length() == 0)
        return 0;
      QueryResults r = dsj.find(dbSpalte, v);
      return addToPAL(r);
    } catch (TimeoutException e)
    {
      LOGGER.error("", e);
    } catch (IllegalArgumentException e)
    {
      LOGGER.debug("", e);
    }
    return 0;
  }


  /**
   * Liefert den Wert zu einer Variable namens key und muss von jeder konkreten
   * Finder-Klasse implementiert werden.
   *
   * @param key
   *          Der Schlüssel, zu dem der Wert zurückgeliefert werden soll.
   * @return der zugehörige Wert zum Schlüssel key.
   */
  protected abstract String getValueForKey(String key);

  /**
   * Sucht in der Datenquelle nach Datensätzen wobei die beiden Suchbedingungen
   * (dbSpalte1==evaluate(value1) und dbSpalte2==evaluate(value2)) mit UND
   * verknüpft sind - die gefundenen Werte werden danach in die PAL kopiert.
   *
   * @param dbSpalte1
   *          der Feldname über den nach dem evaluierten Wert von value gesucht
   *          wird.
   * @param value1
   *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
   *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
   *          ersetzt).
   * @param dbSpalte2
   *          der Feldname über den nach dem evaluierten Wert von value gesucht
   *          wird.
   * @param value2
   *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
   *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
   *          ersetzt).
   * @return die Anzahl der gefundenen Datensätze
   */
  public int find(String dbSpalte1, String value1, String dbSpalte2,
      String value2)
  {
    LOGGER.trace(this.getClass().getSimpleName() + ".tryToFind(" + dbSpalte1
        + " '" + value1 + "' " + dbSpalte2 + " '" + value2 + "')");
    try
    {
      String v1 = evaluate(value1);
      String v2 = evaluate(value2);
      if (v1.length() == 0 || v2.length() == 0)
        return 0;
      QueryResults r = dsj.find(dbSpalte1, v1, dbSpalte2, v2);
      return addToPAL(r);
    } catch (TimeoutException e)
    {
      LOGGER.error("", e);
    } catch (IllegalArgumentException e)
    {
      LOGGER.debug("", e);
    }
    return 0;
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

  /**
   * Kopiert alle matches von QueryResults in die PAL.
   */
  private int addToPAL(QueryResults r)
  {
    for (Iterator<Dataset> iter = r.iterator(); iter.hasNext();)
    {
      DJDataset element = (DJDataset) iter.next();
      element.copy();
    }
    return r.size();
  }
}
