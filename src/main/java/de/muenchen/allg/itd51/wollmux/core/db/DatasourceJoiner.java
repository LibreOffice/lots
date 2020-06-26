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
package de.muenchen.allg.itd51.wollmux.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten aus verschiedenen
 * Hintergrunddatenbanken zieht.
 */
public class DatasourceJoiner
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceJoiner.class);

  /**
   * Dummy-Schema für Datenquellen.
   */
  public static final String NOCONFIG = "noconfig";

  private static final String NACHNAME = "Nachname";

  private static final String VORNAME = "Vorname";

  private static final String ROLLE = "Rolle";

  private static final String DATASET_NOT_FROM_LOS_ERROR_MSG = "Dataset does not come from the local override storage. ";

  /**
   * Muster für erlaubte Suchstrings für den Aufruf von find().
   */
  private static final Pattern SUCHSTRING_PATTERN = Pattern.compile("^\\*?[^*]+\\*?$");

  /**
   * Bildet Datenquellenname auf Datasource-Objekt ab. Nur die jeweils zuletzt unter einem Namen in
   * der Config-Datei aufgeführte Datebank ist hier verzeichnet.
   */
  private Map<String, Datasource> nameToDatasource = new HashMap<>();

  private LocalOverrideStorage myLOS;

  /**
   * Wird von {@link #getSelectedDatasetTransformed()} verwendet; kann null sein!
   */
  private ColumnTransformer columnTransformer;

  /**
   * Die Datenquelle auf die sich find(), getLOS(), etc beziehen.
   */
  protected Datasource mainDatasource;

  /**
   * Eine Liste, die die {@link Dataset}s enthält, die mit einer Hintergrunddatenbank verknüpft
   * sind, deren Schlüssel jedoch darin nicht mehr gefunden wurde und deshalb nicht aktualisiert
   * werden konnte.
   */
  protected List<Dataset> lostDatasets = new ArrayList<>(0);

  /**
   * Erzeugt einen neuen DatasourceJoiner.
   *
   * @param dataSources
   *          Configured Datasources from wollmux.conf file.
   * @param senderSource
   *          Name of the Sender Source.
   * @param los
   *          Instance of an local override storage object.
   */
  public DatasourceJoiner(Map<String, Datasource> dataSources, String senderSource,
      LocalOverrideStorage los)
  {
    init(dataSources, senderSource, los);
  }

  /**
   * Nur für die Verwendung durch abgeleitete Klassen, die den parametrisierten Konstruktor nicht
   * verwenden können, und stattdessen init() benutzen.
   */
  protected DatasourceJoiner()
  {
  }

  protected void init(Map<String, Datasource> dataSources, String senderSource,
      LocalOverrideStorage los)
  {
    for (Map.Entry<String, Datasource> ds : dataSources.entrySet())
    {
      if (ds.getValue() != null)
      {
        nameToDatasource.put(ds.getKey(), ds.getValue());
      } else
      {
        nameToDatasource.remove(ds.getKey());
      }
    }

    myLOS = los;
    List<String> schema = myLOS.getSchema();

    if (!nameToDatasource.containsKey(senderSource))
    {
      if (schema == null)
      {
        throw new ConfigurationErrorException(
            L.m("Datenquelle {} nicht definiert und Cache nicht vorhanden", senderSource));
      }

      if (!senderSource.equals(NOCONFIG))
      {
        LOGGER.error("Datenquelle {} nicht definiert => verwende alte Daten aus Cache",
            senderSource);
        mainDatasource = new EmptyDatasource(schema, senderSource);
      } else
      {
        mainDatasource = new DummyDatasourceWithMessagebox(schema, senderSource);
      }

      nameToDatasource.put(senderSource, mainDatasource);
    } else
    {
      mainDatasource = nameToDatasource.get(senderSource);

      lostDatasets = myLOS.refreshFromDatabase(mainDatasource);
    }
  }

  public Datasource getMainDatasource()
  {
    return mainDatasource;
  }

  /**
   * Get Datasource by given name.
   *
   * @param name
   *          Name of the Datasource, i.e. "personal", "ldap"
   * @return A Datasource Object along with the datasource name.
   */
  public Datasource getDatasource(String name)
  {
    return nameToDatasource.get(name);
  }

  /**
   * Liefert das Schema der Hauptdatenquelle zurück.
   *
   * @return Schema der Hauptdatenquelle.
   */
  public List<String> getMainDatasourceSchema()
  {
    return mainDatasource.getSchema();
  }

  public List<Dataset> getLostDatasets()
  {
    return lostDatasets;
  }

  public void setLostDatasets(List<Dataset> lostDatasets)
  {
    this.lostDatasets = lostDatasets;
  }

  /**
   * Durchsucht die Hauptdatenbank (nicht den LOS) nach Datensätzen, die in Spalte spaltenName den
   * Wert suchString stehen haben. suchString kann vorne und/oder hinten genau ein Sternchen '*'
   * stehen haben, um Präfix/Suffix/Teilstring-Suche zu realisieren. Folgen mehrerer Sternchen oder
   * Sternchen in der Mitte des Suchstrings sind verboten und produzieren eine
   * IllegalArgumentException. Ebenso verboten ist ein suchString, der nur Sternchen enthält oder
   * einer der leer ist. Alle Ergebnisse sind {@link DJDataset}s. Die Suche erfolgt grundsätzlich
   * case-insensitive.
   * <p>
   * Im folgenden eine Liste möglicher Suchanfragen mit Angabe, ob sie unterstützt wird (X) oder
   * nicht (O).
   * </p>
   *
   * <pre>
   * Suche nach
   * X           &quot;vorname.nachname&quot;
   * X           &quot;vorname.nachname@muenchen.de&quot;
   * X           &quot;Nam&quot;
   * O           &quot;ITD5.1&quot;  nicht unterstützt weil Minus vor 5.1 fehlt
   * X           &quot;ITD-5.1&quot;
   * O           &quot;D&quot;   liefert Personen mit Nachname-Anfangsbuchstabe D
   * X           &quot;D-*&quot;
   * O           &quot;ITD5&quot;    nicht unterstützt weil Minus vor 5 fehlt
   * X           &quot;D-HAIII&quot;
   * X           &quot;5.1&quot;
   * X           &quot;D-III-ITD-5.1&quot;
   * O           &quot;D-HAIII-ITD-5.1&quot;   nicht unterstützt, da HA nicht im lhmOUShortname
   * O           &quot;D-HAIII-ITD5.1&quot;    nicht unterstützt (siehe oben)
   *
   * X           &quot;Nam Vorn&quot;
   * X           &quot;Nam, Vorn&quot;
   * X           &quot;Vorname Name&quot;
   * X           &quot;Vorn Nam&quot;
   * X           &quot;ITD 5.1&quot;
   * O           &quot;D-HAIII-ITD 5.1&quot;   steht nicht mit HA im LDAP
   * X           &quot;V. Nachname&quot;
   * X           &quot;Vorname N.&quot;
   * </pre>
   *
   * @param query
   *          of queries as a 2-dimensional String Pair. First string is the 'Column' to search
   *          against the datasource along with a value.
   *
   * @return A filtered List with QueryPart objects.
   */
  public List<QueryPart> buildQuery(List<Pair<String, String>> query)
  {
    List<QueryPart> queryParts = new ArrayList<>();

    for (Pair<String, String> pair : query)
    {
      if (pair.getValue() == null || !SUCHSTRING_PATTERN.matcher(pair.getValue()).matches())
      {
        continue;
      }

      queryParts.add(new QueryPart(pair.getKey(), pair.getValue()));
    }

    return queryParts;
  }

  /**
   * Durchsucht eine beliebige Datenquelle unter Angabe einer beliebigen Anzahl von
   * Spaltenbedingungen. ACHTUNG! Die Ergebnisse sind keine DJDatasets, weil diese Methode nur die
   * Datensätze der in der Query benannten Datenquelle durchreicht. Diese Methode ist also nur
   * deswegen Teil des DJs, weil der DJ die ganzen Datenquellen kennt. Ein Wrappen der Datensätze in
   * DJDatasets wäre also nicht sinnvoll, da es damit möglich wäre durch die copy() Methode
   * Datensätze in den LOS zu kopieren, die gar nicht aus der SENDER_SOURCE kommen.
   *
   * @param query
   *          Query to search against the main datasource.
   * @throws IllegalArgumentException
   *           falls eine Suchanfrage fehlerhaft ist, weil z.B. die entsprechende Datenquelle nicht
   *           existiert.
   * @return Results as {@link QueryResults}
   */
  public QueryResults find(Query query)
  {
    Datasource source = nameToDatasource.get(query.getDatasourceName());
    if (source == null)
    {
      throw new IllegalArgumentException(
          L.m("Datenquelle \"%1\" soll durchsucht werden, ist aber nicht definiert",
              query.getDatasourceName()));
    }

    /*
     * Suchstrings auf Legalität prüfen.
     */
    for (QueryPart part : query)
    {
      String suchString = part.getSearchString();
      if (suchString == null || !SUCHSTRING_PATTERN.matcher(suchString).matches())
      {
        throw new IllegalArgumentException(L.m("Illegaler Suchstring: %1", suchString));
      }
    }

    // Suche ausführen.
    return source.find(query.getQueryParts());
  }

  /**
   * Find matches in the main datasource by a List of {@link QueryPart}.
   *
   * @param query
   *          Query to search against the main datasource.
   * @return Search results as {@link QueryResults}
   */
  public QueryResults find(List<QueryPart> query)
  {
    QueryResults res = mainDatasource.find(query);
    List<DJDatasetWrapper> djDatasetsList = StreamSupport.stream(res.spliterator(), false)
        .map(ds -> new DJDatasetWrapper(ds)).collect(Collectors.toList());

    return new QueryResultsList(djDatasetsList);
  }

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der Datenquelle mit Name
   * datasourceName. Wenn möglich sollte die Datenquelle hier all ihre Datensätze zurückliefern oder
   * zumindest soviele wie möglich. Es ist jedoch auch erlaubt, dass hier gar keine Datensätze
   * zurückgeliefert werden.
   *
   * ACHTUNG! Die Ergebnisse sind keine DJDatasets!
   *
   * @param datasourceName
   *          Name of the datasource.
   * @return {@link QueryResults} All Datasets of the given data source.
   */
  public QueryResults getContentsOf(String datasourceName)
  {
    Datasource source = nameToDatasource.get(datasourceName);
    if (source == null)
      throw new IllegalArgumentException(
          L.m("Datenquelle {} soll abgefragt werden, ist aber nicht definiert", datasourceName));

    return source.getContents();
  }

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der Hauptdatenquelle. Wenn
   * möglich sollte die Datenquelle hier all ihre Datensätze zurückliefern oder zumindest soviele
   * wie möglich. Es ist jedoch auch erlaubt, dass hier gar keine Datensätze zurückgeliefert werden.
   * Die Ergebnisse sind DJDatasets!
   *
   * @return Datensätze aus der Datenquelle.
   */
  public QueryResults getContentsOfMainDatasource()
  {
    QueryResults res = mainDatasource.getContents();
    List<DJDatasetWrapper> djDatasetsList = StreamSupport.stream(res.spliterator(), false)
        .map(ds -> new DJDatasetWrapper(ds)).collect(Collectors.toList());

    return new QueryResultsList(djDatasetsList);
  }

  /**
   * Saves current local override storage with linked cache in in cache.conf.
   *
   * @param cacheFile
   *          File Object as{@link File}
   * @return Cache and local override storage as {@link ConfigThingy} with a preselected default
   *         dataset.
   */
  public ConfigThingy saveCacheAndLOS(File cacheFile)
  {
    LOGGER.debug("Speichere Cache nach {}.", cacheFile);
    List<String> schema = myLOS.getSchema();
    if (schema == null)
    {
      LOGGER.error("Kann Cache nicht speichern, weil nicht initialisiert.");
      return null;
    }

    ConfigThingy conf = new ConfigThingy(cacheFile.getPath());
    ConfigThingy schemaConf = conf.add("Schema");
    Iterator<String> iter = schema.iterator();
    while (iter.hasNext())
    {
      schemaConf.add(iter.next());
    }

    ConfigThingy datenConf = conf.add("Daten");
    myLOS.dumpData(datenConf);

    try
    {
      Dataset ds = getSelectedDataset();
      ConfigThingy ausgewaehlt = conf.add("Ausgewaehlt");
      ausgewaehlt.add(ds.getKey());
      ausgewaehlt.add(Integer.toString(getSelectedDatasetSameKeyIndex()));
    } catch (DatasetNotFoundException x)
    {
      LOGGER.trace("", x);
    }

    return conf;
  }

  /**
   * Copies all datasets from the search result into the personal sender source list.
   *
   * @param results
   *          Search results from a datasource as {@link QueryResults}.
   * @return Size of the param as {@link Integer}
   */
  public int addToPAL(QueryResults results)
  {
    if (results == null || results.isEmpty())
    {
      return 0;
    }

    for (Dataset ds : results)
    {
      DJDataset element = (DJDataset) ds;
      element.copy();
    }
    return results.size();
  }

  /**
   * Liefert den momentan im Lokalen Override Speicher ausgewählten Datensatz.
   *
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz selektiert).
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return myLOS.getSelectedDataset();
  }

  /**
   * Liefert die Anzahl der Datensätze im LOS, die den selben Schlüssel haben wie der ausgewählte,
   * und die vor diesem in der LOS-Liste gespeichert sind.
   *
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz selektiert).
   */
  public int getSelectedDatasetSameKeyIndex() throws DatasetNotFoundException
  {
    return myLOS.getSelectedDatasetSameKeyIndex();
  }

  /**
   * Erlaubt es, einen {@link ColumnTransformer} zu setzen, der von
   * {@link #getSelectedDatasetTransformed()} verwendet wird. Falls null übergeben wird, wird die
   * Transformation deaktiviert und {@link #getSelectedDatasetTransformed()} liefert das selbe
   * Ergebnis wie {@link #getSelectedDataset()}.
   */
  public void setTransformer(ColumnTransformer columnTransformer)
  {
    this.columnTransformer = columnTransformer;
  }

  /**
   * Falls kein {@link ColumnTransformer} gesetzt wurde mit
   * {@link #setTransformer(ColumnTransformer)}, so liefert diese Funktion das selbe wie
   * {@link #getSelectedDataset()}, ansonsten wird das durch den ColumnTransformer transformierte
   * Dataset geliefert.
   *
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz selektiert).
   */
  public Dataset getSelectedDatasetTransformed() throws DatasetNotFoundException
  {
    DJDataset ds = getSelectedDataset();
    if (columnTransformer == null)
    {
      return ds;
    }
    return columnTransformer.transform(ds);
  }

  /**
   * Get all datasets from the local override storage.
   *
   * @return Datasets from local override storage by type {@link QueryResults}.
   */
  public QueryResults getLOS()
  {
    return new QueryResultsList(myLOS.iterator(), myLOS.size());
  }

  public static final Comparator<DJDataset> sortPAL = (ds1, ds2) -> {
    try
    {
      return ds1.get(NACHNAME).compareTo(ds2.get(NACHNAME));
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return 0;
  };

  /**
   * Creates a new Dataset in LocalOverrideStorage that is not linked with a backing store database.
   *
   * @return New Dataset. All fields of the new Dataset are prefilled with the associated column.
   */
  public DJDataset newDataset()
  {
    return myLOS.newDataset();
  }

  /**
   * Ein Wrapper um einfache Datasets, wie sie von Datasources als Ergebnisse von Anfragen
   * zurückgeliefert werden. Der Wrapper ist notwendig, um die auch für Fremddatensätze sinnvollen
   * DJDataset Funktionen anbieten zu können, allen voran copy().
   */
  private class DJDatasetWrapper implements DJDataset
  {
    private Dataset myDS;

    public DJDatasetWrapper(Dataset ds)
    {
      myDS = ds;
    }

    @Override
    public void set(String columnName, String newValue) throws ColumnNotFoundException
    {
      throw new UnsupportedOperationException(DATASET_NOT_FROM_LOS_ERROR_MSG);
    }

    @Override
    public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
    {
      return false;
    }

    @Override
    public boolean hasBackingStore()
    {
      return true;
    }

    @Override
    public boolean isFromLOS()
    {
      return false;
    }

    @Override
    public boolean isSelectedDataset()
    {
      return false;
    }

    @Override
    public void select()
    {
      throw new UnsupportedOperationException(DATASET_NOT_FROM_LOS_ERROR_MSG);
    }

    @Override
    public void discardLocalOverride(String columnName)
        throws ColumnNotFoundException, NoBackingStoreException
    {
      // nichts zu tun
    }

    @Override
    public DJDataset copy()
    {
      return myLOS.copyNonLOSDataset(myDS);
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(DATASET_NOT_FROM_LOS_ERROR_MSG);
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      return myDS.get(columnName);
    }

    @Override
    public String getKey()
    {
      return myDS.getKey();
    }

    @Override
    public String toString()
    {
      StringBuilder stringBuilder = new StringBuilder();

      try
      {
        String rolle = get(ROLLE);
        String nachname = get(NACHNAME);
        String vorname = get(VORNAME);

        stringBuilder.append(rolle == null || rolle.isEmpty() ? "" : "(" + rolle + ") ");
        stringBuilder.append(nachname == null || nachname.isEmpty() ? "" : nachname);
        stringBuilder.append(", ");
        stringBuilder.append(vorname == null || vorname.isEmpty() ? "" : vorname);
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

      return stringBuilder.toString();
    }
  }
}
