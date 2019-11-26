/*
 * Dateiname: DatasourceJoiner.java
 * Projekt  : WollMux
 * Funktion : stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten
 *            aus verschiedenen Hintergrunddatenbanken zieht.
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
 * 06.10.2005 | BNK | Erstellung
 * 24.10.2005 | BNK | +newDataset()
 * 28.10.2005 | BNK | Arbeit an der Baustelle
 * 02.11.2005 | BNK | Testen und Debuggen
 *                  | Aus Cache wird jetzt auch der ausgewählte gelesen
 * 03.11.2005 | BNK | saveCacheAndLOS kriegt jetzt File-Argument
 *                  | saveCacheAndLos implementiert
 * 03.11.2005 | BNK | besser kommentiert
 * 07.11.2005 | BNK | +type "union"
 * 10.11.2005 | BNK | das Suchen der Datensätze für den Refresh hinter die
 *                  |  Schemaanpassung verschoben.
 *                  | Und nochmal die Reihenfolge umgewürfelt, hoffentlich stimmt's
 *                  | jetzt.
 * 10.11.2005 | BNK | Unicode-Marker an den Anfang der Cache-Datei schreiben
 * 06.12.2005 | BNK | +getStatus() (enthält momentan Info über Datensätze, die nicht
 *                  |   in der Datenbank wiedergefunden werden konnten und deshalb
 *                  |   vermutlich neu eingefügt werden sollten, weil sonst auf
 *                  |   Ewigkeit nur der Cache verwendet wird.
 *                  | LOS-only Datensätze werden nun korrekt in dumpData()
 *                  |   wiedergegeben und im Konstruktor restauriert.
 * 12.04.2006 | BNK | [P766]mehrere Datensätze mit gleichem Schlüssel korrekt in
 *                  | cache.conf gespeichert und wieder restauriert, ohne LDAP
 *                  | Anbindung zu verlieren.
 * 18.04.2006 | BNK | Bugfix zur Behebung von P766: ausgewaehlten Datensatz richtig merken
 * 26.05.2006 | BNK | +find(Query)       
 * 30.01.2007 | BNK | Timeout nicht mehr statisch, sondern an Konstruktor übergeben.
 * 19.03.2010 | BED | +getContentsOfMainDatasource()
 * -------------------------------------------------------------------
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

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

  /**
   * Wird an Datasource.find() übergeben, um die maximale Zeit der Bearbeitung einer
   * Suchanfrage zu begrenzen, damit nicht im Falle eines Netzproblems alles
   * einfriert.
   */
  private long queryTimeout;
  
  /**
   * Muster für erlaubte Suchstrings für den Aufruf von find().
   */
  private static final Pattern SUCHSTRING_PATTERN =
    Pattern.compile("^\\*?[^*]+\\*?$");

  /**
   * Bildet Datenquellenname auf Datasource-Objekt ab. Nur die jeweils zuletzt unter
   * einem Namen in der Config-Datei aufgeführte Datebank ist hier verzeichnet.
   */
  private Map<String, Datasource> nameToDatasource =
    new HashMap<>();

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
   * Eine Liste, die die {@link Dataset}s enthält, die mit einer
   * Hintergrunddatenbank verknüpft sind, deren Schlüssel jedoch darin nicht mehr
   * gefunden wurde und deshalb nicht aktualisiert werden konnte.
   */
  protected List<Dataset> lostDatasets = new ArrayList<>(0);
  
  private List<Dataset> cachedQueryResults = new ArrayList<>();

  public static final long DATASOURCE_TIMEOUT = 10000;
  
  /**
   * Repräsentiert den Status eines DatasourceJoiners.
   */
  public static class Status
  {
    /**
     * Eine Liste, die die {@link Dataset}s enthält, die mit einer
     * Hintergrunddatenbank verknüpft sind, deren Schlüssel jedoch darin nicht mehr
     * gefunden wurde und deshalb nicht aktualisiert werden konnte.
     */
    public List<Dataset> lostDatasets = new ArrayList<>(0);
  }

  private Status status;

  /**
   * Erzeugt einen neuen DatasourceJoiner.

   * @param dataSources
   * @param mainSourceName
   * @param los
   * @param datasourceTimeout
   */
  public DatasourceJoiner(Map<String, Datasource> dataSources, String mainSourceName, LocalOverrideStorage los, long datasourceTimeout)
  {
    init(dataSources, mainSourceName, los, datasourceTimeout);
  }

  /**
   * Nur für die Verwendung durch abgeleitete Klassen, die den parametrisierten
   * Konstruktor nicht verwenden können, und stattdessen init() benutzen.
   */
  protected DatasourceJoiner()
  {}
  
  protected void init(Map<String, Datasource> dataSources, String mainSourceName, LocalOverrideStorage los, long datasourceTimeout)
  {
    queryTimeout = datasourceTimeout;
    status = new Status();
    
    for (Map.Entry<String, Datasource> ds : dataSources.entrySet())
    {
      if (ds.getValue() != null)
      {
        nameToDatasource.put(ds.getKey(), ds.getValue());
      }
      else
      {
        nameToDatasource.remove(ds.getKey());
      }
    }

    myLOS = los;
    List<String> schema = myLOS.getSchema();

    if (!nameToDatasource.containsKey(mainSourceName))
    {
      if (schema == null)
      {
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\" nicht definiert und Cache nicht vorhanden",
          mainSourceName));
      }
      
      if (!mainSourceName.equals(NOCONFIG))
      {
        LOGGER.error(L.m("Datenquelle \"%1\" nicht definiert => verwende alte Daten aus Cache",
                                        mainSourceName));
        mainDatasource = new EmptyDatasource(schema, mainSourceName);
      }
      else
      {
        mainDatasource = new DummyDatasourceWithMessagebox(schema, mainSourceName);
      }
      
      nameToDatasource.put(mainSourceName, mainDatasource);
    }
    else
    {
      mainDatasource = nameToDatasource.get(mainSourceName);

      try
      {
        lostDatasets = myLOS.refreshFromDatabase(mainDatasource, queryTimeout(), status);
      }
      catch (TimeoutException x)
      {
        LOGGER.error(L.m(
          "Timeout beim Zugriff auf Datenquelle \"%1\" => Benutze Daten aus Cache",
          mainDatasource.getName()), x);
      }
    }
  }

  public Status getStatus()
  {
    return status;
  }

  public Datasource getMainDatasource()
  {
    return mainDatasource;
  }

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
   * Durchsucht die Hauptdatenbank (nicht den LOS) nach Datensätzen, die in Spalte
   * spaltenName den Wert suchString stehen haben. suchString kann vorne und/oder
   * hinten genau ein Sternchen '*' stehen haben, um Präfix/Suffix/Teilstring-Suche
   * zu realisieren. Folgen mehrerer Sternchen oder Sternchen in der Mitte des
   * Suchstrings sind verboten und produzieren eine IllegalArgumentException. Ebenso
   * verboten ist ein suchString, der nur Sternchen enthält oder einer der leer ist.
   * Alle Ergebnisse sind {@link DJDataset}s. Die Suche erfolgt grundsätzlich
   * case-insensitive.
   * <p>
   * Im folgenden eine Liste möglicher Suchanfragen mit Angabe, ob sie unterstützt
   * wird (X) oder nicht (O).
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
   * @throws TimeoutException
   *           falls die Anfrage nicht innerhalb einer intern vorgegebenen Zeitspanne
   *           beendet werden konnte.
   */
  public QueryResults find(String spaltenName, String suchString)
      throws TimeoutException
  { // TESTED
    if (suchString == null || !SUCHSTRING_PATTERN.matcher(suchString).matches())
      throw new IllegalArgumentException(L.m("Illegaler Suchstring: %1", suchString));

    List<QueryPart> query = new ArrayList<>();
    query.add(new QueryPart(spaltenName, suchString));
    return find(query);
  }

  /**
   * Wie find(spaltenName, suchString), aber mit einer zweiten Spaltenbedingung, die
   * und-verknüpft wird.
   * 
   * @throws TimeoutException
   */
  public QueryResults find(String spaltenName1, String suchString1,
      String spaltenName2, String suchString2) throws TimeoutException
  {
    if (suchString1 == null || !SUCHSTRING_PATTERN.matcher(suchString1).matches())
      throw new IllegalArgumentException(
        L.m("Illegaler Suchstring: %1", suchString1));
    if (suchString2 == null || !SUCHSTRING_PATTERN.matcher(suchString2).matches())
      throw new IllegalArgumentException(
        L.m("Illegaler Suchstring: %1", suchString2));

    List<QueryPart> query = new ArrayList<>();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    return find(query);
  }

  /**
   * Durchsucht eine beliebige Datenquelle unter Angabe einer beliebigen Anzahl von
   * Spaltenbedingungen. ACHTUNG! Die Ergebnisse sind keine DJDatasets, weil diese
   * Methode nur die Datensätze der in der Query benannten Datenquelle durchreicht.
   * Diese Methode ist also nur deswegen Teil des DJs, weil der DJ die ganzen
   * Datenquellen kennt. Ein Wrappen der Datensätze in DJDatasets wäre also nicht
   * sinnvoll, da es damit möglich wäre durch die copy() Methode Datensätze in den
   * LOS zu kopieren, die gar nicht aus der SENDER_SOURCE kommen.
   * 
   * @throws TimeoutException
   * @throws IllegalArgumentException
   *           falls eine Suchanfrage fehlerhaft ist, weil z.B. die entsprechende
   *           Datenquelle nicht existiert.
   */
  public QueryResults find(Query query) throws TimeoutException
  {
    Datasource source = nameToDatasource.get(query.getDatasourceName());
    if (source == null)
    {
      throw new IllegalArgumentException(L.m(
        "Datenquelle \"%1\" soll durchsucht werden, ist aber nicht definiert",
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
        throw new IllegalArgumentException(L.m("Illegaler Suchstring: %1",
          suchString));
      }
    }

    // Suche ausführen.
    return source.find(query.getQueryParts(), queryTimeout());
  }

  /**
   * Findet Datensätze in der Hauptdatenquelle, die query (Liste von QueryParts)
   * entsprechen.
   * 
   * Die Ergebnisse sind {@link DJDataset}s!
   * 
   * @throws TimeoutException
   */
  public QueryResults find(List<QueryPart> query) throws TimeoutException
  { // TESTED
    QueryResults res = mainDatasource.find(query, queryTimeout());
    List<DJDatasetWrapper> djDatasetsList = StreamSupport
        .stream(res.spliterator(), false)
        .map(ds -> new DJDatasetWrapper(ds))
        .collect(Collectors.toList());

    return new QueryResultsList(djDatasetsList);
  }

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der Datenquelle
   * mit Name datasourceName. Wenn möglich sollte die Datenquelle hier all ihre
   * Datensätze zurückliefern oder zumindest soviele wie möglich. Es ist jedoch auch
   * erlaubt, dass hier gar keine Datensätze zurückgeliefert werden. Wenn sinnvoll
   * sollte anstatt des Werfens einer TimeoutException ein Teil der Daten
   * zurückgeliefert werden.
   * 
   * ACHTUNG! Die Ergebnisse sind keine DJDatasets!
   * 
   * @throws TimeoutException
   *           falls ein Fehler auftritt oder die Anfrage nicht rechtzeitig beendet
   *           werden konnte. In letzterem Fall ist das Werfen dieser Exception
   *           jedoch nicht Pflicht und die Datenquelle kann stattdessen den Teil der
   *           Ergebnisse zurückliefern, die in der gegebenen Zeit gewonnen werden
   *           konnten.
   * @throws IllegalArgumentException
   *           falls die Datenquelle nicht existiert.
   */
  public QueryResults getContentsOf(String datasourceName) throws TimeoutException
  {
    Datasource source = nameToDatasource.get(datasourceName);
    if (source == null)
      throw new IllegalArgumentException(L.m(
        "Datenquelle \"%1\" soll abgefragt werden, ist aber nicht definiert",
        datasourceName));

    return source.getContents(queryTimeout());
  }

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der
   * Hauptdatenquelle. Wenn möglich sollte die Datenquelle hier all ihre Datensätze
   * zurückliefern oder zumindest soviele wie möglich. Es ist jedoch auch erlaubt,
   * dass hier gar keine Datensätze zurückgeliefert werden. Wenn sinnvoll sollte
   * anstatt des Werfens einer {@link TimeoutException} ein Teil der Daten
   * zurückgeliefert werden.
   * 
   * Die Ergebnisse sind DJDatasets!
   * 
   * @return Datensätze aus der Datenquelle.
   * @throws TimeoutException
   *           falls ein Fehler auftritt oder die Anfrage nicht rechtzeitig beendet
   *           werden konnte. In letzterem Fall ist das Werfen dieser Exception
   *           jedoch nicht Pflicht und die Datenquelle kann stattdessen den Teil der
   *           Ergebnisse zurückliefern, die in der gegebenen Zeit gewonnen werden
   *           konnten.
   */
  public QueryResults getContentsOfMainDatasource() throws TimeoutException
  {
    QueryResults res = mainDatasource.getContents(queryTimeout());
    List<DJDatasetWrapper> djDatasetsList = StreamSupport
        .stream(res.spliterator(), false)
        .map(ds -> new DJDatasetWrapper(ds))
        .collect(Collectors.toList());

    return new QueryResultsList(djDatasetsList);
  }

  protected long queryTimeout()
  {
    return queryTimeout;
  }

  /**
   * Speichert den aktuellen LOS samt zugehörigem Cache in die Datei cacheFile.
   */
  public ConfigThingy saveCacheAndLOS(File cacheFile)
  {
    LOGGER.debug(L.m("Speichere Cache nach %1.", cacheFile));
    List<String> schema = myLOS.getSchema();
    if (schema == null)
    {
      LOGGER.error(L.m("Kann Cache nicht speichern, weil nicht initialisiert."));
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
    }
    catch (DatasetNotFoundException x)
    {
      LOGGER.trace("", x);
    }

    return conf;
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
   * Liefert die Anzahl der Datensätze im LOS, die den selben Schlüssel haben wie der
   * ausgewählte, und die vor diesem in der LOS-Liste gespeichert sind.
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
   * {@link #getSelectedDatasetTransformed()} verwendet wird. Falls null übergeben
   * wird, wird die Transformation deaktiviert und
   * {@link #getSelectedDatasetTransformed()} liefert das selbe Ergebnis wie
   * {@link #getSelectedDataset()}.
   */
  public void setTransformer(ColumnTransformer columnTransformer)
  {
    this.columnTransformer = columnTransformer;
  }

  /**
   * Falls kein {@link ColumnTransformer} gesetzt wurde mit
   * {@link #setTransformer(ColumnTransformer)}, so liefert diese Funktion das selbe
   * wie {@link #getSelectedDataset()}, ansonsten wird das durch den
   * ColumnTransformer transformierte Dataset geliefert.
   * 
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz selektiert).
   */
  public Dataset getSelectedDatasetTransformed() throws DatasetNotFoundException
  {
    DJDataset ds = getSelectedDataset();
    if (columnTransformer == null){
      return ds;
    }
    return columnTransformer.transform(ds);
  }

  /**
   * Liefert alle Datensätze des Lokalen Override Speichers (als {@link DJDataset}).
   */
  public QueryResults getLOS()
  {
    return new QueryResultsList(myLOS.iterator(), myLOS.size());
  }
  
  public void setCachedLdapResults(QueryResults results)
  {
    results.forEach(ds -> this.cachedQueryResults.add(ds));
  }
  
  public void addCachedLdapResult(Dataset ds)
  {
    Iterator<Dataset> datasetIterator = this.cachedQueryResults.iterator();
    
    while (datasetIterator.hasNext()) {
      Dataset dataset = datasetIterator.next();
      try
      {
        if (dataset.get("OID").equals(ds.get("OID"))) {
          datasetIterator.remove();
        }
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
   }
  
    this.cachedQueryResults.add(ds);
  }
  
  public List<Dataset> getCachedLdapResults()
  {
    return this.cachedQueryResults;
  }
  
  public Dataset getCachedLdapResultByOID(String oid)
  {
    if (this.cachedQueryResults == null || this.cachedQueryResults.isEmpty())
      return null;

    Dataset result = null;
    
    for (Dataset ds : this.cachedQueryResults)
    {
      try
      {
        if (ds.get("OID").equals(oid))
        {
          result = ds;
          break;
        }
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
    }

    return result;
  }

  public Set<String> getOIDsFromLOS() {
    Set<String> oids = new HashSet<>();
    for (Dataset dataset : myLOS) {
      try
      {
        oids.add(dataset.get("OID"));
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
    }
    
    return oids;
  }

  public static final Comparator<DJDataset> sortPAL = (ds1, ds2) ->
  {
    try
    {
      return ds1.get("Nachname").compareTo(ds2.get("Nachname"));
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return 0;
  };

  /**
   * Legt einen neuen Datensatz im LOS an, der nicht mit einer Hintergrunddatenbank
   * verknüpft ist und liefert ihn zurück. Alle Felder des neuen Datensatzes sind mit
   * dem Namen der entsprechenden Spalte initialisiert.
   */
  public DJDataset newDataset()
  {
    return myLOS.newDataset();
  }

  /**
   * Ein Wrapper um einfache Datasets, wie sie von Datasources als Ergebnisse von
   * Anfragen zurückgeliefert werden. Der Wrapper ist notwendig, um die auch für
   * Fremddatensätze sinnvollen DJDataset Funktionen anbieten zu können, allen voran
   * copy().
   */
  private class DJDatasetWrapper implements DJDataset
  {
    private Dataset myDS;

    public DJDatasetWrapper(Dataset ds)
    {
      myDS = ds;
    }

    @Override
    public void set(String columnName, String newValue)
        throws ColumnNotFoundException
    {
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
    }

    @Override
    public boolean hasLocalOverride(String columnName)
        throws ColumnNotFoundException
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
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
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
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
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
        String rolle = get("Rolle");
        String nachname = get("Nachname");
        String vorname = get("Vorname");

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
