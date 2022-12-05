/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Eine Datenquelle, die eine andere Datenquelle um Spalten ergänzt und einzelne Spaltenwerte
 * ersetzen kann. Zur Erstellung der Menge der Ergebnisdatensätze wird jeder Datensatz aus SOURCE1
 * genau einmal verwendet und jeder Datensatz aus SOURCE2 beliebig oft (auch keinmal). Unterschiede
 * zu einem richtigen Join:<br>
 * <br>
 * a) Verhindert, dass eine Person 2 mal auftaucht, nur weil es 2 Einträge mit Verkehrsverbindungen
 * für ihre Adresse gibt<br>
 * b) Verhindert, dass eine Person rausfliegt, weil es zu ihrer Adresse keine Verkehrsverbindung
 * gibt<br>
 * c) Die Schlüssel der Ergebnisdatensätze bleiben die aus SOURCE1 und werden nicht kombiniert aus
 * SOURCE1 und SOURCE2. Das verhindert, dass ein Datensatz bei einer Änderung der Adresse aus der
 * lokalen Absenderliste fliegt, weil er beim Cache-Refresh nicht mehr gefunden wird. <br>
 * <br>
 * In der Ergebnisdatenquelle sind alle Spalten unter ihren ursprünglichen Namen verfügbar
 * (Unterschied zu {@link AttachDatasource}). Konflikte werden über den MODE-Spezifizierer aufgelöst
 * (siehe wollmux.conf Doku).<br>
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OverlayDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OverlayDatasource.class);

  private String name;

  private String source1Name;

  private String source2Name;

  private Datasource source1;

  private Datasource source2;

  private List<String> schema;

  private Set<String> schema1;

  private Set<String> schema2;

  private String[] match1;

  private String[] match2;

  /**
   * Enthält die Namen aller Spaltennamen für die es ein i gibt, so dass
   * Spaltenname,equals(match1[i]) und match1[i],equals(match2[i]).
   */
  private Set<String> commonMatchColumns;

  /**
   * true if mode is "so", "sO", "So" or "SO".
   */
  private boolean modeSO;

  /**
   * true if second char in 'MODE' is lower case.
   */
  private boolean treatEmptyStringsAsNull;

  /**
   * Erzeugt eine neue OverlayDatasource.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser OverlayDatasource bereits
   *          vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser OverlayDatasource enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht verwendet).
   */
  public OverlayDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc,
      URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    String mode = parseConfig(sourceDesc, "MODE",
        () -> L.m("MODE-specification of datasource '{0}' is missing", name));

    String lcMode = mode.toLowerCase();
    if ("so".equals(lcMode))
      modeSO = true;
    else if ("os".equals(lcMode))
      modeSO = false;
    else
      throw new ConfigurationErrorException(
          L.m("Incorrect MODE-specification in datasource  '{0}': MODE \"{1}\" is not allowed", name,
              mode));

    treatEmptyStringsAsNull = Character.isLowerCase(mode.charAt(1));

    source1Name = parseConfig(sourceDesc, "SOURCE",
        () -> L.m("SOURCE of data source {0} is missing", name));
    source2Name = parseConfig(sourceDesc, "OVERLAY",
        () -> L.m("OVERLAY-specification of data source {0} is missing", name));

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source2Name));

    schema1 = new HashSet<>(source1.getSchema());
    schema2 = new HashSet<>(source2.getSchema());

    schema = new ArrayList<>(schema1);
    schema.addAll(schema2);

    ConfigThingy matchesDesc = sourceDesc.query("MATCH");
    int numMatches = matchesDesc.count();
    if (numMatches == 0)
      throw new ConfigurationErrorException(
          L.m("At least one MATCH-specification has to be made in data source \"{0}\"", name));

    match1 = new String[numMatches];
    match2 = new String[numMatches];
    commonMatchColumns = new HashSet<>();

    Iterator<ConfigThingy> iter = matchesDesc.iterator();
    for (int i = 0; i < numMatches; ++i)
    {
      ConfigThingy matchDesc = iter.next();
      if (matchDesc.count() != 2)
        throw new ConfigurationErrorException(
            L.m("Incorrect MATCH specification in data source \"{0}\"", name));

      String spalte1 = "";
      String spalte2 = "";
      try
      {
        spalte1 = matchDesc.getFirstChild().toString();
        spalte2 = matchDesc.getLastChild().toString();
      } catch (NodeNotFoundException x)
      {
        LOGGER.trace("", x);
      }

      if (!schema1.contains(spalte1))
        throw new ConfigurationErrorException(L.m("Column \"{0}\" is not defined in schema", spalte1));

      if (!schema2.contains(spalte2))
        throw new ConfigurationErrorException(L.m("Column \"{0}\" is not defined in schema", spalte2));

      match1[i] = spalte1;
      match2[i] = spalte2;
      if (spalte1.equals(spalte2))
      {
        commonMatchColumns.add(spalte1);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return overlayColumns(source1.getDatasetsByKey(keys), DatasetPredicate.matchAll);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
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
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    List<QueryPart> queryOnly1 = new ArrayList<>();
    List<QueryPart> queryOnly2 = new ArrayList<>();
    List<QueryPart> queryBoth = new ArrayList<>();
    for (QueryPart p : query)
    {
      if (schema1.contains(p.getColumnName()))
      {
        if (schema2.contains(p.getColumnName())
            /*
             * Wichtige Optimierung: Bedingungen für Spalten, die in beiden Datenquellen vorkommen,
             * die geMATCHt werden (commonMatchColumns), dürfen nicht nach queryBoth gesteckt
             * werden, da dies im schlechtesten Fall dazu führt, dass (unnötigerweise) der
             * ineffizienteste Code-Pfad in dieser Funktion genommen wird.
             */
            && !commonMatchColumns.contains(p.getColumnName()))
          queryBoth.add(p);
        else
          queryOnly1.add(p);
      } else
      {
        queryOnly2.add(p);
      }
    }

    /*
     * Die OVERLAY-Datenquelle ist normalerweise nur untergeordnet und Spaltenbedingungen dafür
     * schränken die Suchergebnisse wenig ein. Deshalb werten wir falls wir mindestens eine
     * Bedingung haben, die exklusiv die Hauptdatenquelle betrifft, die Anfrage auf dieser Basis
     * aus.
     */
    if (!queryOnly1.isEmpty())
    {
      QueryResults results = source1.find(queryOnly1);

      List<QueryPart> restQuery = new ArrayList<>(queryOnly2.size() + queryBoth.size());
      restQuery.addAll(queryBoth);
      restQuery.addAll(queryOnly2);

      return overlayColumns(results, DatasetPredicate.makePredicate(restQuery));
    } else if (!queryOnly2.isEmpty())
    { /*
       * in diesem Fall haben wir nur Bedingungen für Spalten, die entweder bei beiden Datenquellen
       * vorkommen oder nur in der OVERLAY-Datenquelle. Auf jeden Fall haben wir mindestens eine
       * Spaltenbedingung, die nur die OVERLAY-Datenquelle betrifft. Wir führen die Suche mit den
       * Bedingungen der OVERLAY-Datenquelle durch, ergänzen dann daraus alle möglichen Datasets und
       * Filtern dann nochmal mit den Spaltenbedingungen für die gemeinsamen Spalten.
       */

      return overlayColumnsReversed(source2.find(queryOnly2),
          DatasetPredicate.makePredicate(queryBoth));
    } else
    { /*
       * An der Abfrage sind nur Spalten beteiligt, die in beiden Datenquellen vorhanden sind. Hier
       * wird's kompliziert, weil für jeden Spaltenwert getrennt der Wert jeweils aus der einen oder
       * der anderen Datenquelle kommen kann. Deswegen lassen sich weder aus SOURCE noch aus OVERLAY
       * Datensätze bestimmen, die definitiv zum Ergebnisraum gehören. Wir müssen also auf Basis
       * beider Datenquellen entsprechende Kandidaten bestimmen. Leider entsteht dabei das Problem
       * der Duplikatelimination. Da die Schlüssel von Datensätzen nicht zwingend eindeutig sind,
       * ist es nicht so leicht, diese durchzuführen.
       */

      /*
       * Wegen dem Problem, das im Ergebnisdatensatz evtl. Spalte 1 aus SOURCE und Spalte 2 aus
       * OVERLAY kommt, können wir nicht nach mehreren Spaltenbedingungen aus queryBoth gleichzeitig
       * suchen. Wir müssen uns also eine Spaltenbedingung heraussuchen, nach dieser suchen und dann
       * mit einem Filter, der die komplette Suchbedingung aus query testet die Datensätze
       * rausfiltern, die tatsächlich die gesuchten sind.
       */

      QueryPart qp = getMostRestrictingQueryPart(queryBoth);
      List<QueryPart> restrictingQuery = new ArrayList<>(1);
      restrictingQuery.add(qp);

      Predicate<Dataset> predicate = DatasetPredicate.makePredicate(query);

      QueryResults results1 = overlayColumns(source1.find(restrictingQuery), predicate);
      QueryResults results2 = overlayColumnsReversed(source2.find(restrictingQuery), predicate);

      /*
       * An dieser Stelle haben wir alle gesuchten Datensätze. Allerdings kann es zwischen results1
       * und results2 Überschneidungen geben. Dies ist das oben angesprochene Duplikat-Problem. Um
       * die Duplikate zu eliminieren gehen wir wie folgt vor:
       *
       * 1. Alle Datensatzschlüssel bestimmen, die sowohl in results1 als auch in results2
       * vorkommen.
       *
       * 2. Alle Datensätze mit Schlüsseln aus 1. entfernen aus den results Listen
       *
       * 3. In einer weiteren Abfrage alle Datensätze mit den Schlüsseln aus 1. bestimmen und
       * diejenigen, die die Filterbedingung erfüllen den Ergebnissen wieder hinzufügen.
       */
      HashSet<String> results1Keys = new HashSet<>();
      for (Dataset ds : results1)
      {
        results1Keys.add(ds.getKey());
      }

      List<Dataset> finalResults = new ArrayList<>(results1.size() + results2.size());

      List<String> dupKeys = new ArrayList<>();
      Iterator<Dataset> iter = results2.iterator();
      while (iter.hasNext())
      {
        Dataset ds = iter.next();
        String key = ds.getKey();
        if (results1Keys.contains(key))
          dupKeys.add(key);
        else
          finalResults.add(ds);
      }

      QueryResults results3 = getDatasetsByKey(dupKeys);

      for (Dataset ds : results3)
        if (predicate.test(ds))
        {
          finalResults.add(ds);
        }

      for (Dataset ds : results1)
        if (!dupKeys.contains(ds.getKey()))
        {
          finalResults.add(ds);
        }

      return new QueryResultsList(finalResults);
    }
  }

  /**
   * Versucht, den QueryPart aus query (darf nicht leer sein) zu bestimmen, der den Ergebnisraum
   * einer Suchanfrage am meisten einschränkt und liefert diesen zurück. Kriterium hierfür ist die
   * Anzahl der Sternchen und die Anzahl der Nicht-Sternchen-Zeichen im Suchstring.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   *         TESTED
   */
  private QueryPart getMostRestrictingQueryPart(List<QueryPart> query)
  {
    QueryPart best = query.get(0); // Sicherstellen, dass best immer initialisiert
    // ist
    int bestStarCount = Integer.MAX_VALUE;
    int bestNonStarCount = -1;
    for (QueryPart qp : query)
    {
      String str = qp.getSearchString();
      int nonStarCount = str.length();
      int starCount = 0;
      if (str.length() > 0 && str.charAt(0) == '*')
      {
        ++starCount;
        --nonStarCount;
      }
      if (str.length() > 1 && str.charAt(str.length() - 1) == '*')
      {
        ++starCount;
        --nonStarCount;
      }

      if ((starCount == 0 && bestStarCount > 0) || (nonStarCount > bestNonStarCount)
          || (nonStarCount == bestNonStarCount && starCount < bestStarCount))
      {
        best = qp;
        bestStarCount = starCount;
        bestNonStarCount = nonStarCount;
      }
    }

    return best;
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

  private QueryResults overlayColumns(QueryResults results, Predicate<Dataset> filter)
  {
    List<Dataset> resultsWithOverlayments = new ArrayList<>(results.size());

    Iterator<Dataset> iter = results.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();

      List<QueryPart> query = new ArrayList<>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try
        {
          query.add(new QueryPart(match2[i], ds.get(match1[i])));
        } catch (ColumnNotFoundException x)
        {
          LOGGER.error("", x);
        }
      }

      QueryResults appendix = source2.find(query);

      Dataset newDataset;

      if (appendix.size() == 0)
      {
        newDataset = new ConcatDataset(ds, null);
        if (filter.test(newDataset))
        {
          resultsWithOverlayments.add(newDataset);
        }
      } else
      {
        Iterator<Dataset> appendixIter = appendix.iterator();
        while (appendixIter.hasNext())
        {
          newDataset = new ConcatDataset(ds, appendixIter.next());
          if (filter.test(newDataset))
          {
            resultsWithOverlayments.add(newDataset);
            break;
          }
        }
      }
    }

    return new QueryResultsList(resultsWithOverlayments);
  }

  private QueryResults overlayColumnsReversed(QueryResults results, Predicate<Dataset> filter)
  {
    List<ConcatDataset> resultsWithOverlayments = new ArrayList<>(results.size());

    for (Dataset ds : results)
    {
      List<QueryPart> query = new ArrayList<>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try
        {
          query.add(new QueryPart(match1[i], ds.get(match2[i])));
        } catch (ColumnNotFoundException x)
        {
          LOGGER.error("", x);
        }
      }

      QueryResults prependix = source1.find(query);

      for (Dataset prepend : prependix)
      {
        ConcatDataset newDataset = new ConcatDataset(prepend, ds);
        if (filter.test(newDataset))
        {
          resultsWithOverlayments.add(newDataset);
        }
      }
    }

    return new QueryResultsList(resultsWithOverlayments);
  }

  private class ConcatDataset implements Dataset
  {
    private Dataset dataset1;

    private Dataset dataset2; // kann null sein!

    /**
     * ds1 is always from SOURCE and ds2 from OVERLAY.
     *
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public ConcatDataset(Dataset ds1, Dataset ds2)
    {
      this.dataset1 = ds1;
      this.dataset2 = ds2;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column \"{0}\" is not defined in schema", columnName));

      Dataset ds1;
      Dataset ds2;
      Set<String> schemaOfPriorityDatasource;
      if (modeSO)
      {
        ds1 = this.dataset1;
        ds2 = this.dataset2;
        schemaOfPriorityDatasource = schema2;
      } else
      {
        ds1 = this.dataset2;
        ds2 = this.dataset1;
        schemaOfPriorityDatasource = schema1;
      }

      if (ds2 != null && schemaOfPriorityDatasource.contains(columnName))
      {
        String value = ds2.get(columnName);
        if (treatEmptyStringsAsNull && value != null && value.length() == 0)
          value = null;
        if (value != null)
        {
          return value;
        }
      }

      try
      {
        // ds1 kann null sein in dem Fall wo ds1 == this.ds2 (bei modeSO == false)
        if (ds1 == null)
        {
          return null;
        }
        return ds1.get(columnName);
      } catch (ColumnNotFoundException x)
      {
        // Die Exception darf nicht weitergeworfen werden, denn die Spalte existiert
        // ja im Gesamtschema, wie ganz oben getestet. Wenn wir hier hinkommen, dann
        // nur in in dem Fall, dass der Wert in ds2 unbelegt ist.
        return null;
      }
    }

    @Override
    public String getKey()
    {
      return dataset1.getKey();
    }
  }

}
