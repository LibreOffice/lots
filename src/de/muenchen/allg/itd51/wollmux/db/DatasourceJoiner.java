/*
 * Dateiname: DatasourceJoiner.java
 * Projekt  : WollMux
 * Funktion : stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten
 *            aus verschiedenen Hintergrunddatenbanken zieht.
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten aus verschiedenen
 * Hintergrunddatenbanken zieht.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceJoiner
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatasourceJoiner.class);

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
    new HashMap<String, Datasource>();

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
   * Repräsentiert den Status eines DatasourceJoiners.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class Status
  {
    /**
     * Eine Liste, die die {@link Dataset}s enthält, die mit einer
     * Hintergrunddatenbank verknüpft sind, deren Schlüssel jedoch darin nicht mehr
     * gefunden wurde und deshalb nicht aktualisiert werden konnte.
     */
    public List<Dataset> lostDatasets = new Vector<Dataset>(0);
  }

  private Status status;

  private static final long DATASOURCE_TIMEOUT = 10000;

  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;

  /**
   * Falls true, wurde bereits versucht, den DJ zu initialisieren (über den Erfolg
   * des Versuchs sagt die Variable nichts.)
   */
  private static boolean djInitialized = false;

  /**
   * Erzeugt einen neuen DatasourceJoiner.
   *
   * @param joinConf
   *          ein ConfigThingy mit "Datenquellen" Kindern.
   * @param mainSourceName
   *          der Name der Datenquelle, auf die sich die Funktionen des DJ
   *          (find(),...) beziehen sollen.
   * @param losCache
   *          die Datei, in der der DJ die Datensätze des LOS abspeichern soll. Falls
   *          diese Datei existiert, wird sie vom Konstruktor eingelesen und
   *          verwendet.
   * @param context
   *          der Kontext relativ zu dem Datenquellen URLs in ihrer Beschreibung
   *          auswerten sollen.
   * @param datasourceTimeout
   *          Zeit in ms, die Suchanfragen maximal brauchen dürfen bevor sie
   *          abgebrochen werden.
   * @throws ConfigurationErrorException
   *           falls ein schwerwiegender Fehler auftritt, der die Arbeit des DJ
   *           unmöglich macht, wie z.B. wenn die Datenquelle mainSourceName in der
   *           joinConf fehlt und gleichzeitig kein Cache verfügbar ist.
   */
  protected DatasourceJoiner(ConfigThingy joinConf, String mainSourceName,
      File losCache, URL context, long datasourceTimeout)
      throws ConfigurationErrorException
  {

    init(joinConf, mainSourceName, losCache, context, datasourceTimeout);
  }

  /**
   * Nur für die Verwendung durch abgeleitete Klassen, die den parametrisierten
   * Konstruktor nicht verwenden können, und stattdessen init() benutzen.
   */
  protected DatasourceJoiner()
  {}

  public Status getStatus()
  {
    return status;
  }

  /**
   * Erledigt die Initialisierungsaufgaben des Konstruktors mit den gleichen
   * Parametern. Für die Verwendung durch abgeleitete Klassen, die den
   * parametrisierten Konstruktor nicht verwenden können.
   *
   * @throws ConfigurationErrorException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected void init(ConfigThingy joinConf, String mainSourceName, File losCache,
      URL context, long datasourceTimeout) throws ConfigurationErrorException
  { // TESTED
    queryTimeout = datasourceTimeout;
    status = new Status();

    ConfigThingy datenquellen = joinConf.query("Datenquellen").query("Datenquelle");
    Iterator<ConfigThingy> iter = datenquellen.iterator();
    while (iter.hasNext())
    {
      ConfigThingy sourceDesc = iter.next();
      ConfigThingy c = sourceDesc.query("NAME");
      if (c.count() == 0)
      {
        LOGGER.error(L.m("Datenquelle ohne NAME gefunden"));
        continue;
      }
      String name = c.toString();

      c = sourceDesc.query("TYPE");
      if (c.count() == 0)
      {
        LOGGER.error(L.m("Datenquelle %1 hat keinen TYPE", name));
        continue;
      }
      String type = c.toString();

      Datasource ds = null;
      try
      {
        if ("conf".equals(type))
          ds = new ThingyDatasource(nameToDatasource, sourceDesc, context);
        else if ("union".equals(type))
          ds = new UnionDatasource(nameToDatasource, sourceDesc, context);
        else if ("attach".equals(type))
          ds = new AttachDatasource(nameToDatasource, sourceDesc, context);
        else if ("overlay".equals(type))
          ds = new OverlayDatasource(nameToDatasource, sourceDesc, context);
        else if ("prefer".equals(type))
          ds = new PreferDatasource(nameToDatasource, sourceDesc, context);
        else if ("schema".equals(type))
          ds = new SchemaDatasource(nameToDatasource, sourceDesc, context);
        else if ("ldap".equals(type))
          ds = new LDAPDatasource(nameToDatasource, sourceDesc, context);
        else if ("ooo".equals(type))
          ds = new OOoDatasource(nameToDatasource, sourceDesc, context);
        else if ("funky".equals(type))
          ds = new FunkyDatasource(nameToDatasource, sourceDesc, context);
        else
          LOGGER.error(L.m("Ununterstützter Datenquellentyp: %1", type));
      }
      catch (Exception x)
      {
        LOGGER.error(L.m(
          "Fehler beim Initialisieren von Datenquelle \"%1\" (Typ \"%2\"):", name,
          type), x);
      }

      if (ds == null)
      {
        LOGGER.error(L.m(
          "Datenquelle '%1' von Typ '%2' konnte nicht initialisiert werden", name,
          type));
        /*
         * Falls schon eine alte Datenquelle name registriert ist, entferne diese
         * Registrierung. Ansonsten würde mit der vorher registrierten Datenquelle
         * weitergearbeitet, was seltsame Effekte zur Folge hätte die schwierig
         * nachzuvollziehen sind.
         */
        nameToDatasource.remove(name);
        continue;
      }

      nameToDatasource.put(name, ds);
    }

    // kann sein, dass noch kein singleton erstellt ist - kein Zugriff auf no config
    if (mainSourceName.equals(de.muenchen.allg.itd51.wollmux.NoConfig.NOCONFIG))
    {
      myLOS = new LocalOverrideStorageDummyImpl();// no config, kein cache !
    }
    else
    {
      myLOS = new LocalOverrideStorageStandardImpl(losCache, context);//mit config
    }


    Set<String> schema = myLOS.getSchema();

    if (!nameToDatasource.containsKey(mainSourceName))
    {
      if (schema == null){
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\" nicht definiert und Cache nicht vorhanden",
          mainSourceName));
      }

      if ( ! mainSourceName.equals(de.muenchen.allg.itd51.wollmux.NoConfig.NOCONFIG)){
        LOGGER.error(L.m("Datenquelle \"%1\" nicht definiert => verwende alte Daten aus Cache",
                                        mainSourceName));
        mainDatasource = new EmptyDatasource(schema, mainSourceName);
      }
      else
      {
        mainDatasource = new de.muenchen.allg.itd51.wollmux.db.DummyDatasourceWithMessagebox(schema, mainSourceName);
      }
      nameToDatasource.put(mainSourceName, mainDatasource);
    }
    else
    {
      mainDatasource = nameToDatasource.get(mainSourceName);

      try
      {
        myLOS.refreshFromDatabase(mainDatasource, queryTimeout(), status);
      }
      catch (TimeoutException x)
      {
        LOGGER.error(L.m(
          "Timeout beim Zugriff auf Datenquelle \"%1\" => Benutze Daten aus Cache",
          mainDatasource.getName()), x);
      }

    }
  }

  /**
   * Liefert das Schema der Hauptdatenquelle zurück.
   *
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set<String> getMainDatasourceSchema()
  { // TESTED
    return mainDatasource.getSchema();
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

    List<QueryPart> query = new ArrayList<QueryPart>();
    query.add(new QueryPart(spaltenName, suchString));
    return find(query);
  }

  /**
   * Wie find(spaltenName, suchString), aber mit einer zweiten Spaltenbedingung, die
   * und-verknüpft wird.
   *
   * @throws TimeoutException
   * @author Matthias Benkmann (D-III-ITD 5.1)
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

    List<QueryPart> query = new ArrayList<QueryPart>();
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public QueryResults find(Query query) throws TimeoutException
  {
    Datasource source = nameToDatasource.get(query.getDatasourceName());
    if (source == null)
      throw new IllegalArgumentException(L.m(
        "Datenquelle \"%1\" soll durchsucht werden, ist aber nicht definiert",
        query.getDatasourceName()));

    /*
     * Suchstrings auf Legalität prüfen.
     */
    Iterator<QueryPart> iter = query.iterator();
    while (iter.hasNext())
    {
      String suchString = iter.next().getSearchString();
      if (suchString == null || !SUCHSTRING_PATTERN.matcher(suchString).matches())
        throw new IllegalArgumentException(L.m("Illegaler Suchstring: %1",
          suchString));
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected QueryResults find(List<QueryPart> query) throws TimeoutException
  { // TESTED
    QueryResults res = mainDatasource.find(query, queryTimeout());
    List<DJDatasetWrapper> djDatasetsList = new ArrayList<DJDatasetWrapper>(res.size());
    Iterator<Dataset> iter = res.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();
      djDatasetsList.add(new DJDatasetWrapper(ds));
    }
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   * @return
   * @throws TimeoutException
   *           falls ein Fehler auftritt oder die Anfrage nicht rechtzeitig beendet
   *           werden konnte. In letzterem Fall ist das Werfen dieser Exception
   *           jedoch nicht Pflicht und die Datenquelle kann stattdessen den Teil der
   *           Ergebnisse zurückliefern, die in der gegebenen Zeit gewonnen werden
   *           konnten.
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public QueryResults getContentsOfMainDatasource() throws TimeoutException
  {
    QueryResults res = mainDatasource.getContents(queryTimeout());
    List<DJDatasetWrapper> djDatasetsList = new ArrayList<DJDatasetWrapper>(res.size());
    Iterator<Dataset> iter = res.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();
      djDatasetsList.add(new DJDatasetWrapper(ds));
    }
    return new QueryResultsList(djDatasetsList);
  }

  protected long queryTimeout()
  {
    return queryTimeout;
  }

  /**
   * Speichert den aktuellen LOS samt zugehörigem Cache in die Datei cacheFile.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveCacheAndLOS(File cacheFile) throws IOException
  {
    LOGGER.debug(L.m("Speichere Cache nach %1.", cacheFile));
    Set<String> schema = myLOS.getSchema();
    if (schema == null)
    {
      LOGGER.error(L.m("Kann Cache nicht speichern, weil nicht initialisiert."));
      return;
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
      ausgewaehlt.add("" + getSelectedDatasetSameKeyIndex());
    }
    catch (DatasetNotFoundException x)
    {}

    WollMuxFiles.writeConfToFile(cacheFile, conf);
  }

  /**
   * Liefert den momentan im Lokalen Override Speicher ausgewählten Datensatz.
   *
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz selektiert).
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
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
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public Dataset getSelectedDatasetTransformed() throws DatasetNotFoundException
  {
    DJDataset ds = getSelectedDataset();
    if (columnTransformer == null) {
      return ds;
    }
    return columnTransformer.transform(ds);
  }

  /**
   * Liefert alle Datensätze des Lokalen Override Speichers (als
   * {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}).
   */
  public QueryResults getLOS()
  {
    return new QueryResultsList(myLOS.iterator(), myLOS.size());
  }

  /**
   * Legt einen neuen Datensatz im LOS an, der nicht mit einer Hintergrunddatenbank
   * verknüpft ist und liefert ihn zurück. Alle Felder des neuen Datensatzes sind mit
   * dem Namen der entsprechenden Spalte initialisiert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset newDataset()
  {
    return myLOS.newDataset();
  }

  /**
   * Diese Methode liefert eine Liste mit den über {@link #senderDisplayTemplate}
   * definierten String-Repräsentation aller verlorenen gegangenen Datensätze des
   * DatasourceJoiner (gemäß {@link Status.lostDatasets}) zurück.
   * Die genaue Form der String-Repräsentation ist abhängig von
   * {@link #senderDisplayTemplate}, das in der WollMux-Konfiguration über den Wert
   * von SENDER_DISPLAYTEMPLATE gesetzt werden kann. Gibt es keine verloren
   * gegangenen Datensätze, so bleibt die Liste leer.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static List<String> getLostDatasetDisplayStrings()
  {
    DatasourceJoiner dj = getDatasourceJoiner();
    ArrayList<String> list = new ArrayList<String>();
    for (Dataset ds : dj.getStatus().lostDatasets)
      list.add(new DatasetListElement(ds, PersoenlicheAbsenderliste.getInstance().getSenderDisplayTemplate()).toString());
    return list;
  }

  /**
   * Initialisiert den DJ wenn nötig und liefert ihn dann zurück (oder null, falls
   * ein Fehler während der Initialisierung aufgetreten ist).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */

  public static DatasourceJoiner getDatasourceJoiner()
  {
    if (!DatasourceJoiner.djInitialized)
    {
      DatasourceJoiner.djInitialized = true;
      ConfigThingy senderSource =
        WollMuxFiles.getWollmuxConf().query("SENDER_SOURCE", 1);
      String senderSourceStr = null;
      try
      {
        senderSourceStr = senderSource.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        // hier geben wir im Vergleich zu früher keine Fehlermeldung mehr aus,
        // sondern erst später, wnn
        // tatsächlich auf die Datenquelle "null" zurück gegriffen wird.
      }

      ConfigThingy dataSourceTimeout =
        WollMuxFiles.getWollmuxConf().query("DATASOURCE_TIMEOUT", 1);
      String datasourceTimeoutStr = "";
      long datasourceTimeoutLong = 0;
      try
      {
        datasourceTimeoutStr = dataSourceTimeout.getLastChild().toString();
        try
        {
          datasourceTimeoutLong = new Long(datasourceTimeoutStr).longValue();
        }
        catch (NumberFormatException e)
        {
          LOGGER.error(L.m("DATASOURCE_TIMEOUT muss eine ganze Zahl sein"));
          datasourceTimeoutLong = DatasourceJoiner.DATASOURCE_TIMEOUT;
        }
        if (datasourceTimeoutLong <= 0)
        {
          LOGGER.error(L.m("DATASOURCE_TIMEOUT muss größer als 0 sein!"));
        }
      }
      catch (NodeNotFoundException e)
      {
        datasourceTimeoutLong = DatasourceJoiner.DATASOURCE_TIMEOUT;
      }

      try
      {
        if (null == senderSourceStr)
          senderSourceStr = de.muenchen.allg.itd51.wollmux.NoConfig.NOCONFIG;

        DatasourceJoiner.datasourceJoiner =
          new DatasourceJoiner(WollMuxFiles.getWollmuxConf(), senderSourceStr, WollMuxFiles.getLosCacheFile(),
            WollMuxFiles.getDEFAULT_CONTEXT(), datasourceTimeoutLong);
        /*
         * Zum Zeitpunkt wo der DJ initialisiert wird sind die Funktions- und
         * Dialogbibliothek des WollMuxSingleton noch nicht initialisiert, deswegen
         * können sie hier nicht verwendet werden. Man könnte die Reihenfolge
         * natürlich ändern, aber diese Reihenfolgeabhängigkeit gefällt mir nicht.
         * Besser wäre auch bei den Funktionen WollMuxSingleton.getFunctionDialogs()
         * und WollMuxSingleton.getGlobalFunctions() eine on-demand initialisierung
         * nach dem Prinzip if (... == null) initialisieren. Aber das heben wir uns
         * für einen Zeitpunkt auf, wo es benötigt wird und nehmen jetzt erst mal
         * leere Dummy-Bibliotheken.
         */
        FunctionLibrary funcLib = new FunctionLibrary();
        DialogLibrary dialogLib = new DialogLibrary();
        Map<Object, Object> context = new HashMap<Object, Object>();
        ColumnTransformer columnTransformer =
          new ColumnTransformer(WollMuxFiles.getWollmuxConf(),
            "AbsenderdatenSpaltenumsetzung", funcLib, dialogLib, context);
        DatasourceJoiner.datasourceJoiner.setTransformer(columnTransformer);
      }
      catch (ConfigurationErrorException e)
      {
        LOGGER.error("", e);
      }
    }

    return DatasourceJoiner.datasourceJoiner;
  }

  /**
   * Ein Wrapper um einfache Datasets, wie sie von Datasources als Ergebnisse von
   * Anfragen zurückgeliefert werden. Der Wrapper ist notwendig, um die auch für
   * Fremddatensätze sinnvollen DJDataset Funktionen anbieten zu können, allen voran
   * copy().
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
  }
}
