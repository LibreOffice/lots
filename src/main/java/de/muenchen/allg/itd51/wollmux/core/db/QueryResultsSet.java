package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Stellt die Vereinigung mehrerer QueryResults dar.
 *
 * Dabei werden doppelte Ergebnisse herausgefiltert. Es handelt sich um eine mathematische Vereinigung.
 * @author daniel.sikeler
 */
public class QueryResultsSet implements QueryResults
{
  /**
   * Mathematische Vereinigung der QueryResults.
   */
  private final List<Dataset> results = new ArrayList<>();
  /**
   * Anhand dieses Comparators wird die Gleichheit zweiter Datasets erkannt.
   */
  private final Comparator<Dataset> comparator;

  /**
   * Erstellt eine leere Menge.
   * @param comparator Der Comparator, der beim Hinzufügen für Vergleiche verwendet wird.
   */
  public QueryResultsSet(Comparator<Dataset> comparator)
  {
    this.comparator = comparator;
  }

  /**
   * Erstellt eine Ergebnisliste mit den Datensätzen aus queryResults.
   * @param comparator Der Comparator, der beim Hinzufügen für Vergleiche verwendet wird.
   * @param queryResults Die Liste der neuen Datensätze.
   */
  public QueryResultsSet(Comparator<Dataset> comparator, QueryResults queryResults)
  {
    this(comparator);
    addAll(queryResults);
  }

  /**
   * Fügt einen neuen Datensatz hinzu, solange er noch nicht enthalten ist.
   * @param dataset Der neue Datensatz.
   */
  public void add(Dataset dataset)
  {
    boolean present = false;
    for (Dataset ds : results)
    {
      if (comparator.compare(dataset, ds) == 0)
      {
        present = true;
        break;
      }
    }
    if (!present)
    {
      results.add(dataset);
    }
  }

  /**
   * Fügt alle Datensätze der Liste hinzu, solange sie noch nicht enthalten sind.
   * @param queryResults Liste mit neuen Datensätzen.
   */
  public void addAll(QueryResults queryResults)
  {
    for (Dataset ds : queryResults)
    {
      add(ds);
    }
  }

  @Override
  public int size()
  {
    return results.size();
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    return results.iterator();
  }

  @Override
  public boolean isEmpty()
  {
    return results.isEmpty();
  }

}
