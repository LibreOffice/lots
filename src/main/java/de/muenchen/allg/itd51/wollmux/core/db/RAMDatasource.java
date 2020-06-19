package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * Oberklasse für Datasources, die ihre Daten vollständig im Speicher halten
 */
public class RAMDatasource implements Datasource
{
  /**
   * Das Schema dieser Datenquelle.
   */
  private List<String> schema;

  /**
   * Liste aller Datasets, die in dieser Datasource gespeichert sind.
   */
  private List<Dataset> data;

  /**
   * Der Name dieser Datenquelle.
   */
  private String name;

  /**
   * Erzeugt eine neue RAMDatasource mit Namen name. data und schema werden direkt als Referenz
   * eingebunden, nicht kopiert.
   * 
   * @param name
   *          der Name der Datenquelle
   * @param schema
   *          das Schema der Datenquelle
   * @param data
   *          die Datensätze der Datenquelle
   */
  public RAMDatasource(String name, List<String> schema, List<Dataset> data)
  {
    init(name, schema, data);
  }

  /**
   * Erzeugt eine uninitialisierte RAMDatasource. Eine abgeleitete Klasse, die diesen Konstruktor
   * verwendet sollte init() aufrufen, um die nötigen Initialisierungen zu erledigen.
   */
  protected RAMDatasource()
  {
  }

  /**
   * Führt die Initialisierungsaktionen des Konstruktors mit den gleichen Parametern aus. Diese
   * Methode sollte von abgeleiteten Klassen verwendet werden, wenn sie den Konstruktor ohne
   * Argumente verwenden.
   */
  protected void init(String name, List<String> schema, List<Dataset> data)
  {
    this.schema = schema;
    this.data = data;
    this.name = name;
  }

  @Override
  public List<String> getSchema()
  {
    return new ArrayList<>(schema);
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    List<Dataset> res = new ArrayList<>();
    Iterator<Dataset> iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();
      if (keys.contains(ds.getKey()))
      {
        res.add(ds);
      }
    }

    return new QueryResultsList(res);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  @Override
  public QueryResults find(List<QueryPart> query)
  {
    if (query.isEmpty())
    {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    List<Dataset> results = new ArrayList<>();

    data.forEach(ds -> {
      if (pred.test(ds))
      {
        results.add(ds);
      }
    });

    return new QueryResultsList(results);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<>(data));
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  @Override
  public String getName()
  {
    return name;
  }

}
