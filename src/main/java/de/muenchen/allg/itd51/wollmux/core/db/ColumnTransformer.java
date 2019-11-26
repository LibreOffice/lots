/*
 * Dateiname: ColumnTransformer.java
 * Projekt  : WollMux
 * Funktion : Nimmt ein Dataset und stellt mit Hilfe von WollMux-Funktionen aus dessen Spalten berechnete Pseudo-Spalten zur Verfügung.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 21.10.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

/**
 * Nimmt ein Dataset und stellt mit Hilfe von WollMux-Funktionen aus dessen Spalten
 * berechnete Pseudo-Spalten zur Verfügung.
 */
public class ColumnTransformer
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ColumnTransformer.class);

  /**
   * Die Namen aller Pseudospalten, die für diesen ColumnTransformer definiert sind.
   */
  private Set<String> schema = new HashSet<>();

  /**
   * Bildet den Namen einer Pseudospalte auf die zugehörige Funktion ab.
   */
  private Map<String, Function> columnTranslations = new HashMap<>();

  /**
   * Initialisiert einen ColumnTransformer mit allen Abschnitten, die
   * trafoConf,query(nodeName, 1) zurückliefert.
   * 
   *    * trafoConf hat folgenden Aufbau
   * 
   * <pre>
   *   BeliebigerBezeichner(
   *      Name1( WollMux-Funktion1 )
   *      Name2( WollMux-Funktion2 )
   *      ...
   *   )
   * </pre>
   * 
   * NameX ist jeweils der Name, der zum Zugriff auf den entsprechenden Funktionswert
   * an {@link #get(String, Dataset)} übergeben werden muss. Innerhalb der
   * WollMux-Funktionen verwendete Aufrufe der VALUE-Grundfunktion beziehen sich
   * IMMER auf die untransformierten Spalten des zu übersetzenden {@link Dataset}s,
   * NIEMALS auf Pseudospalten. Wird der selbe NameX mehrfach verwendet, so gilt nur
   * die letzte Definition.
   */
  public ColumnTransformer(Map<String, Function> trafos)
  {
    addTrafos(trafos);
  }

  /**
   * Erzeugt einen ColumnTransformer ohne Pseudospalten. Dieser reicht
   * {@link #get(String, Dataset)} Abfragen einfach zum entsprechenden Datensatz
   * durch.
   */
  public ColumnTransformer()
  {}

  /**
   * Liefert true gdw eine Pseudospalte namens name definiert ist, d,h, wenn
   * {@link #get(String, Dataset)} für diesen Namen einen berechneten Wert und nicht
   * direkt den Wert des {@link Dataset}s zurückliefert.
   */
  public boolean hasPseudoColumn(String name)
  {
    return schema.contains(name);
  }

  private void addTrafos(Map<String, Function> trafos)
  {
    for (Map.Entry<String, Function> trafo : trafos.entrySet())
    {
      columnTranslations.put(trafo.getKey(), trafo.getValue());
      schema.add(trafo.getKey());
    }
  }
  
  /**
   * Liefert die Menge aller Namen von Pseudospalten, die definiert sind, d,h, alle
   * Namen für die {@link #get(String, Dataset)} einen berechneten Wert und nicht
   * direkt den Wert des {@link Dataset}s zurückliefert.
   */
  public List<String> getSchema()
  {
    return new ArrayList<>(schema);
  }

  /**
   * Liefert den Wert der Pseudospalte columnName, der anhand der Umsetzungsregeln
   * aus dem {@link Dataset} ds berechnet wird. Falls keine Umsetzungsregel für
   * columnName existiert, wird direkt der Wert der Spalte columnName von ds
   * zurückgeliefert (null falls nicht belegt).
   * 
   * @throws ColumnNotFoundException
   *           falls weder eine Umsetzungsregel für columnName definiert ist noch ds
   *           eine Spalte mit diesem Namen besitzt.
   */
  public String get(String columnName, Dataset ds) throws ColumnNotFoundException
  {
    Function func = columnTranslations.get(columnName);
    if (func == null) {
      return ds.get(columnName);
    }
    return func.getString(new DatasetValues(ds));
  }

  /**
   * Liefert ein {@link Dataset}, das eine transformierte Sicht von ds darstellt. ACHTUNG! Die
   * Berechnung der Spalten wird on-demand durchgeführt, d.h. spätere Aufrufe von
   * {@link #addTrafos(Map)} wirken sich auf das zurückgelieferte {@link Dataset} aus.
   */
  public Dataset transform(Dataset ds)
  {
    return new TransformedDataset(ds);
  }

  /**
   * Liefert {@link QueryResults}, die eine transformierte Sicht von qres darstellen. ACHTUNG! Die
   * Berechnung der {@link Dataset}s wird on-demand durchgeführt, d.h. spätere Aufrufe von
   * {@link #addTrafos(Map)} wirken sich auf die {@link Dataset}s der {@link QueryResults} aus.
   */
  public QueryResults transform(QueryResults qres)
  {
    return new TranslatedQueryResults(qres);
  }

  /**
   * Stellt die Spalten eines Datasets als Values zur Verfügung.
   */
  private static class DatasetValues implements Values
  {
    private Dataset ds;

    public DatasetValues(Dataset ds)
    {
      this.ds = ds;
    }

    @Override
    public boolean hasValue(String id)
    {
      try
      {
        ds.get(id);
      }
      catch (ColumnNotFoundException x)
      {
        LOGGER.trace("", x);
        return false;
      }
      return true;
    }

    @Override
    public String getString(String id)
    {
      String str = null;
      try
      {
        str = ds.get(id);
      }
      catch (ColumnNotFoundException x)
      {
        LOGGER.trace("", x);
      }

      return str == null ? "" : str;
    }

    @Override
    public boolean getBoolean(String id)
    {
      return "true".equalsIgnoreCase(getString(id));
    }
  }

  /**
   * Wendet Spaltenumsetzungen auf QueryResults an und stellt das Ergebnis wieder als
   * QueryResults zur Verfügung.
   */
  private class TranslatedQueryResults implements QueryResults
  {
    /**
     * Die Original-{@link QueryResults}.
     */
    private QueryResults qres;

    /**
     * Die QueryResults res werden mit dem columnTransformer übersetzt.
     */
    public TranslatedQueryResults(QueryResults res)
    {
      qres = res;
    }

    @Override
    public int size()
    {
      return qres.size();
    }

    @Override
    public Iterator<Dataset> iterator()
    {
      return new Iter();
    }

    @Override
    public boolean isEmpty()
    {
      return qres.isEmpty();
    }

    private class Iter implements Iterator<Dataset>
    {
      private Iterator<Dataset> iterator;

      public Iter()
      {
        iterator = qres.iterator();
      }

      @Override
      public boolean hasNext()
      {
        return iterator.hasNext();
      }

      @Override
      public Dataset next()
      {
        Dataset ds = iterator.next();
        return new TransformedDataset(ds);
      }

      @Override
      public void remove()
      {
        iterator.remove();
      }

    }
  }

  private class TransformedDataset implements Dataset
  {
    private Dataset ds;

    public TransformedDataset(Dataset ds)
    {
      this.ds = ds;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      return ColumnTransformer.this.get(columnName, ds);
    }

    @Override
    public String getKey()
    {
      return ds.getKey();
    }

  }
}
