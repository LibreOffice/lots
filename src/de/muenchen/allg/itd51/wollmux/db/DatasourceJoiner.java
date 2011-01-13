/*
 * Dateiname: DatasourceJoiner.java
 * Projekt  : WollMux
 * Funktion : stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten
 *            aus verschiedenen Hintergrunddatenbanken zieht.
 * 
 * Copyright (c) 2010 Landeshauptstadt München
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten aus verschiedenen
 * Hintergrunddatenbanken zieht.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceJoiner
{
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

  public Status getStatus()
  {
    return status;
  }

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
  public DatasourceJoiner(ConfigThingy joinConf, String mainSourceName,
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
  {};

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
        Logger.error(L.m("Datenquelle ohne NAME gefunden"));
        continue;
      }
      String name = c.toString();

      c = sourceDesc.query("TYPE");
      if (c.count() == 0)
      {
        Logger.error(L.m("Datenquelle %1 hat keinen TYPE", name));
        continue;
      }
      String type = c.toString();

      Datasource ds = null;
      try
      {
        if (type.equals("conf"))
          ds = new ThingyDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("union"))
          ds = new UnionDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("attach"))
          ds = new AttachDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("overlay"))
          ds = new OverlayDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("prefer"))
          ds = new PreferDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("schema"))
          ds = new SchemaDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("ldap"))
          ds = new LDAPDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("ooo"))
          ds = new OOoDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("funky"))
          ds = new FunkyDatasource(nameToDatasource, sourceDesc, context);
        else
          Logger.error(L.m("Ununterstützter Datenquellentyp: %1", type));
      }
      catch (Exception x)
      {
        Logger.error(L.m(
          "Fehler beim Initialisieren von Datenquelle \"%1\" (Typ \"%2\"):", name,
          type), x);
      }

      if (ds == null)
      {
        Logger.error(L.m(
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

    myLOS = new LocalOverrideStorage(losCache, context);

    Set<String> schema = myLOS.getSchema();

    if (!nameToDatasource.containsKey(mainSourceName))
    {
      if (schema == null)
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\" nicht definiert und Cache nicht vorhanden",
          mainSourceName));

      Logger.error(L.m(
        "Datenquelle \"%1\" nicht definiert => verwende alte Daten aus Cache",
        mainSourceName));
      mainDatasource = new EmptyDatasource(schema, mainSourceName);
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
        Logger.error(L.m(
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

    List<QueryPart> query = new Vector<QueryPart>();
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

    List<QueryPart> query = new Vector<QueryPart>();
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
    List<DJDatasetWrapper> djDatasetsList = new Vector<DJDatasetWrapper>(res.size());
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
    List<DJDatasetWrapper> djDatasetsList = new Vector<DJDatasetWrapper>(res.size());
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
    Logger.debug(L.m("Speichere Cache nach %1.", cacheFile));
    Set<String> schema = myLOS.getSchema();
    if (schema == null)
    {
      Logger.error(L.m("Kann Cache nicht speichern, weil nicht initialisiert."));
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
    if (columnTransformer == null) return ds;
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
  };

  /**
   * Verwaltet den LOS des DJ.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class LocalOverrideStorage
  {
    /**
     * Präfix, das vor generierte Schlüssel von LOS-only Datensätzen gesetzt wird, um
     * diese eindeutig von anderen Schlüsseln unterscheiden zu können.
     */
    private static final String LOS_ONLY_MAGIC = "GEHORCHE DEM WOLLMUX!";

    /**
     * Liste aller LOSDJDatasets. Die Liste muss geordnet sein, damit Datensätze mit
     * gleichem Schlüssel über ihre Position in der Liste identifiziert werden
     * können.
     */
    private List<LOSDJDataset> data = new LinkedList<LOSDJDataset>();

    /**
     * Das Schema des LOS. Dies ist null solange es nicht initialisiert wurde. Falls
     * beim Laden des Cache ein Fehler auftritt kann dies auch nach dem Konstruktor
     * noch null sein.
     */
    private Set<String> losSchema = null;

    /**
     * Der ausgewählte Datensatz. Nur dann null, wenn data leer ist.
     */
    private DJDataset selectedDataset = null;

    /**
     * Basis für die Erzeugung eines Schlüssels für einen LOS-only Datensatz.
     */
    private long nextGeneratedKey = new Date().getTime();

    /**
     * Versucht, den Cache und den LOS aus der Datei losCache (ConfigThingy) zu
     * lesen. %includes in losCache werden relativ zu context aufgelöst.
     */
    public LocalOverrideStorage(File losCache, URL context)
    { // TESTED
      String selectKey = "";
      String sameKeyIndex = "";
      if (losCache.canRead())
      {
        try
        {
          ConfigThingy cacheData =
            new ConfigThingy(losCache.getPath(), context, new InputStreamReader(
              new FileInputStream(losCache), ConfigThingy.CHARSET));
          /*
           * Falls der Cache korrupt ist sollen keine korrupten Daten in unseren
           * globalen Felder stehen, deswegen erzeugen wir erstmal alles in
           * temporären Variablen und kopieren diese nachher in die Felder losSchema
           * und this.data.
           */
          Set<String> newSchema = new HashSet<String>();
          List<LOSDJDataset> data = new LinkedList<LOSDJDataset>();
          Iterator<ConfigThingy> iter = cacheData.get("Schema").iterator();
          while (iter.hasNext())
            newSchema.add(iter.next().toString());

          iter = cacheData.get("Daten").iterator();
          while (iter.hasNext())
          {
            ConfigThingy dsconf = iter.next();

            Map<String, String> dscache = null;
            ConfigThingy cacheColumns = dsconf.query("Cache");
            if (cacheColumns.count() > 0)
            {
              dscache = new HashMap<String, String>();
              Iterator<ConfigThingy> iter2 = cacheColumns.getFirstChild().iterator();
              while (iter2.hasNext())
              {
                ConfigThingy dsNode = iter2.next();
                String spalte = dsNode.getName();
                if (!newSchema.contains(spalte))
                {
                  Logger.error(L.m(
                    "%1 enthält korrupten Datensatz (Spalte %2 nicht im Schema) => Cache wird ignoriert!",
                    losCache.getPath(), spalte));
                  return;
                }

                dscache.put(spalte, dsNode.toString());
              }
            }
            // else LOS-only Datensatz, dscache bleibt null

            Map<String, String> dsoverride = new HashMap<String, String>();
            Iterator<ConfigThingy> iter2 = dsconf.get("Override").iterator();
            while (iter2.hasNext())
            {
              ConfigThingy dsNode = iter2.next();
              String spalte = dsNode.getName();
              if (!newSchema.contains(spalte))
              {
                Logger.error(L.m(
                  "%1 enthält korrupten Datensatz (Spalte %2 nicht im Schema) => Cache wird ignoriert!",
                  losCache.getPath(), spalte));
                return;
              }

              dsoverride.put(spalte, dsNode.toString());
            }

            data.add(new LOSDJDataset(dscache, dsoverride, newSchema, dsconf.get(
              "Key").toString()));

          }

          ConfigThingy ausgewaehlt = cacheData.get("Ausgewaehlt");
          selectKey = ausgewaehlt.getFirstChild().toString();
          sameKeyIndex = ausgewaehlt.getLastChild().toString();

          losSchema = newSchema;
          this.data = data;
        }
        catch (FileNotFoundException e)
        {
          Logger.error(e);
        }
        catch (IOException e)
        {
          Logger.error(e);
        }
        catch (SyntaxErrorException e)
        {
          Logger.error(e);
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(e);
        }
      }
      else
        Logger.log(L.m("Cache-Datei %1 kann nicht gelesen werden.",
          losCache.getPath()));

      int sameKeyIndexInt = 0;
      try
      {
        sameKeyIndexInt = Integer.parseInt(sameKeyIndex);
      }
      catch (NumberFormatException e)
      {}
      selectDataset(selectKey, sameKeyIndexInt);
    }

    /**
     * Falls es im LOS momentan mindestens einen Datensatz mit Schlüssel selectKey
     * gibt, so wird der durch sameKeyIndex bezeichnete zum ausgewählten Datensatz,
     * ansonsten wird, falls der LOS mindestens einen Datensatz enthält, ein
     * beliebiger Datensatz ausgewählt.
     * 
     * @param sameKeyIndex
     *          zählt ab 0 und gibt an, der wievielte Datensatz gewählt werden soll,
     *          wenn mehrere mit gleichem Schlüssel vorhanden sind. Sollte
     *          sameKeyIndex zu hoch sein, wird der letzte Datensatz mit dem
     *          entsprechenden Schlüssel ausgewählt.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void selectDataset(String selectKey, int sameKeyIndex)
    {
      if (!data.isEmpty()) selectedDataset = data.get(0);
      Iterator<LOSDJDataset> iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = iter.next();
        if (selectKey.equals(ds.getKey()))
        {
          selectedDataset = ds;
          if (--sameKeyIndex < 0) return;
        }
      }
    }

    /**
     * Generiert einen neuen (eindeutigen) Schlüssel für die Erzeugung eines LOS-only
     * Datensatzes.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private String generateKey()
    {
      return LOS_ONLY_MAGIC + (nextGeneratedKey++);
    }

    /**
     * Erzeugt einen neuen Datensatz, der nicht mit Hintergrundspeicher verknüpft
     * ist.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset newDataset()
    {
      Map<String, String> dsoverride = new HashMap<String, String>();
      Iterator<String> iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = iter.next();
        dsoverride.put(spalte, spalte);
      }
      LOSDJDataset ds = new LOSDJDataset(null, dsoverride, losSchema, generateKey());
      data.add(ds);
      if (selectedDataset == null) selectedDataset = ds;
      return ds;
    }

    /**
     * Erzeugt eine Kopie im LOS vom Datensatz ds, der nicht aus dem LOS kommen darf.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset copyNonLOSDataset(Dataset ds)
    {
      if (ds instanceof LOSDJDataset)
        Logger.error(L.m("Diese Funktion darf nicht für LOSDJDatasets aufgerufen werden, da sie immer eine Kopie mit Backing Store erzeugt."));

      Map<String, String> dsoverride = new HashMap<String, String>();
      Map<String, String> dscache = new HashMap<String, String>();
      Iterator<String> iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = iter.next();
        try
        {
          String wert = ds.get(spalte);
          dscache.put(spalte, wert);
        }
        catch (ColumnNotFoundException e)
        {
          Logger.error(e);
        }
      }
      LOSDJDataset newDs =
        new LOSDJDataset(dscache, dsoverride, losSchema, ds.getKey());
      data.add(newDs);
      if (selectedDataset == null) selectedDataset = newDs;
      return newDs;
    }

    /**
     * Liefert den momentan im LOS selektierten Datensatz zurück.
     * 
     * @throws DatasetNotFoundException
     *           falls der LOS leer ist (sonst ist immer ein Datensatz selektiert).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset getSelectedDataset() throws DatasetNotFoundException
    {
      if (data.isEmpty())
        throw new DatasetNotFoundException(
          L.m("Der Lokale Override Speicher ist leer"));
      return selectedDataset;
    }

    /**
     * Liefert die Anzahl der Datensätze im LOS, die den selben Schlüssel haben wie
     * der ausgewählte, und die vor diesem in der LOS-Liste gespeichert sind.
     * 
     * @throws DatasetNotFoundException
     *           falls der LOS leer ist (ansonsten ist immer ein Datensatz
     *           selektiert).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int getSelectedDatasetSameKeyIndex() throws DatasetNotFoundException
    {
      DJDataset ds = getSelectedDataset();
      String key = ds.getKey();
      int idx = 0;
      Iterator<LOSDJDataset> iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds2 = iter.next();
        if (ds2 == ds) return idx;
        if (ds2.getKey().equals(key)) ++idx;
      }

      return idx;
    }

    /**
     * Läd für die Datensätze des LOS aktuelle Daten aus der Datenbank database.
     * 
     * @param timeout
     *          die maximale Zeit, die database Zeit hat, anfragen zu beantworten.
     * @param status
     *          hiervon wird das Feld lostDatasets geupdatet.
     * @throws TimeoutException
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void refreshFromDatabase(Datasource database, long timeout, Status status)
        throws TimeoutException
    { // TESTED
      /*
       * Zuallererst das Schema anpassen. Insbesondere muss dies VOR dem Leeren von
       * data erfolgen. Dadurch werden die LOS-Speicher der LOSDJDatasets an das neue
       * Schema angepasst, bevor der Speicher geleert wird. Dies ist notwendig, da
       * die LOS-Speicher später direkt an die aus res neu erzeugten LOSDJDatasets
       * weitergereicht werden.
       */
      this.setSchema(database.getSchema());

      /*
       * Mappt Schlüssel auf Listen mit Datensätzen, die diese Schlüssel haben. Hier
       * werden Listen verwendet, da mehrere Datensätze denselben Schlüssel haben
       * können, z.B. wenn der selbe LDAP-Datensatz mehrfach eingefügt wurde um mit
       * verschiedenen Rollen verwendet zu werden.
       */
      Map<String, List<LOSDJDataset>> keyToLOSDJDatasetList =
        new HashMap<String, List<LOSDJDataset>>();

      Iterator<LOSDJDataset> iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = iter.next();
        String key = ds.getKey();
        if (!keyToLOSDJDatasetList.containsKey(key))
          keyToLOSDJDatasetList.put(key, new Vector<LOSDJDataset>(1));
        List<LOSDJDataset> djdslist = keyToLOSDJDatasetList.get(key);
        djdslist.add(ds);
      }

      /*
       * Aktualisierte Daten abfragen bevor data geleert wird, damit im Falle eines
       * Timeouts nicht der Cache verloren geht.
       */
      QueryResults res =
        database.getDatasetsByKey(keyToLOSDJDatasetList.keySet(), timeout);

      /*
       * Schlüssel und Index des selektierten Datensatzes feststellen, bevor data
       * geleert wird.
       */
      String selectKey = "";
      int sameKeyIndex = 0;
      try
      {
        selectKey = getSelectedDataset().getKey();
        sameKeyIndex = getSelectedDatasetSameKeyIndex();
      }
      catch (DatasetNotFoundException x)
      {}

      data.clear();
      selectedDataset = null;

      /*
       * Neue Datensätze auf Basis der Query erzeugen. Dabei werden die LOS-Speicher
       * von den korrespondierenden alten (gefunden via keyToLOSDJDatasetList) direkt
       * übernommen. ACHTUNG: Hierbei werden auch temporär im Hintergrundspeicher
       * "verlorene" Datensätze wieder mit dem Hintergrundspeicher verknüpft. Siehe
       * langer Kommentar weiter unten. Bei evtl. Änderungen bitte beachten!!!
       */

      for (Dataset sourceDS : res)
      {
        try
        {
          Map<String, String> dscache = new HashMap<String, String>();

          Iterator<String> spalte = losSchema.iterator();
          while (spalte.hasNext())
          {
            String spaltenName = spalte.next();
            String spaltenWert = sourceDS.get(spaltenName);
            if (spaltenWert != null) dscache.put(spaltenName, spaltenWert);
          }

          String key = sourceDS.getKey();

          List<LOSDJDataset> overrideList = keyToLOSDJDatasetList.remove(key);
          if (overrideList == null)
            data.add(new LOSDJDataset(dscache, new HashMap<String, String>(),
              losSchema, key));
          else
          {
            Iterator<LOSDJDataset> djDsIter = overrideList.iterator();
            while (djDsIter.hasNext())
            {
              LOSDJDataset override = djDsIter.next();
              data.add(new LOSDJDataset(dscache, override.getLOS(), losSchema, key));
            }
          }
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }

      /*
       * Es ist möglich, dass noch Datensätze aus dem alten LOS übrig sind für die
       * keine aktuellen Daten gefunden wurden. Dies sind entweder Datensätze, die
       * von vorneherein nicht mit einer Hintergrunddatenbank verknüpft waren oder
       * Datensätze, die aufgrund von Änderungen des Hintergrundspeichers nicht mehr
       * gefunden wurden. Die Datensätze, die von vorneherein nur im LOS existierten
       * müssen auf jeden Fall erhalten bleiben. Bei den anderen ist es eine gute
       * Frage, was sinnvoll ist. Momentan bleiben auch sie erhalten. Das hat
       * folgende Vor- und Nachteile: Vorteile: - Falls das Verschwinden des
       * Datensatzes nur ein temporäres Problem war, so wird er wenn er wieder im
       * Hintergrundspeicher auftaucht (und den selben Schlüssel hat) wieder damit
       * verknüpft. - Der Benutzer verliert nie Einträge seiner Absenderliste
       * Nachteile: - Der Benutzer merkt evtl. nicht, dass er plötzlich vom
       * Hintergrundspeicher abgekoppelt ist und bekommt gewünschte Änderungen nicht
       * mit. - Die Admins haben keine Möglichkeit, einen Eintrag aus der
       * Absenderliste eines Benutzers zu entfernen (ausser sie greifen direkt auf
       * sein .wollmux Verzeichnis zu. - Falls ein Datensatz bewusst entfernt wurde
       * und später ein neuer Datensatz mit dem selben Schlüssel angelegt wird, so
       * wird der Eintrag in der Absenderliste mit dem neuen Eintrag verknüpft,
       * obwohl dieser nichts mit dem alten zu tun hat.
       */
      Vector<Dataset> lostDatasets = new Vector<Dataset>();
      for (List<LOSDJDataset> djDatasetList : keyToLOSDJDatasetList.values())
      {
        for (LOSDJDataset ds : djDatasetList)
        {
          try
          {
            if (ds.hasBackingStore())
              lostDatasets.add(new SimpleDataset(losSchema, ds));
          }
          catch (ColumnNotFoundException x)
          {
            Logger.error(x);
          }
          data.add(ds);
        }
      }

      lostDatasets.trimToSize();
      status.lostDatasets = lostDatasets;

      StringBuffer buffyTheVampireSlayer = new StringBuffer();
      Iterator<Dataset> iter2 = lostDatasets.iterator();
      while (iter2.hasNext())
      {
        Dataset ds = iter2.next();
        buffyTheVampireSlayer.append(ds.getKey());
        if (iter2.hasNext()) buffyTheVampireSlayer.append(", ");
      }
      if (buffyTheVampireSlayer.length() > 0)
        Logger.log(L.m("Die Datensätze mit folgenden Schlüsseln konnten nicht aus der Datenbank aktualisiert werden: ")
          + buffyTheVampireSlayer);

      selectDataset(selectKey, sameKeyIndex);
    }

    /**
     * Liefert null, falls bislang kein Schema vorhanden (weil das Laden der
     * Cache-Datei im Konstruktur fehlgeschlagen ist).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Set<String> getSchema()
    {
      return losSchema;
    } // TESTED

    /**
     * Fügt conf die Beschreibung der Datensätze im LOS als Kinder hinzu.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void dumpData(ConfigThingy conf)
    {
      Iterator<LOSDJDataset> iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = iter.next();
        ConfigThingy dsConf = conf.add("");
        dsConf.add("Key").add(ds.getKey());

        if (ds.hasBackingStore())
        {
          ConfigThingy cacheConf = dsConf.add("Cache");
          Iterator<Map.Entry<String, String>> entries =
            ds.getBS().entrySet().iterator();
          while (entries.hasNext())
          {
            Map.Entry<String, String> ent = entries.next();
            String spalte = ent.getKey();
            String wert = ent.getValue();
            if (wert != null) cacheConf.add(spalte).add(wert);
          }
        }

        ConfigThingy overrideConf = dsConf.add("Override");
        Iterator<Map.Entry<String, String>> entries =
          ds.getLOS().entrySet().iterator();
        while (entries.hasNext())
        {
          Map.Entry<String, String> ent = entries.next();
          String spalte = ent.getKey();
          String wert = ent.getValue();
          if (wert != null) overrideConf.add(spalte).add(wert);
        }
      }
    }

    /**
     * Ändert das Datenbankschema. Spalten des alten Schemas, die im neuen nicht mehr
     * vorhanden sind werden aus den Datensätzen gelöscht. Im neuen Schema
     * hinzugekommene Spalten werden in den Datensätzen als unbelegt betrachtet.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void setSchema(Set<String> schema)
    { // TESTED
      if (losSchema == null)
      {
        losSchema = new HashSet<String>(schema);
        return;
      }

      Set<String> spaltenDieDazuGekommenSind = new HashSet<String>(schema);
      spaltenDieDazuGekommenSind.removeAll(losSchema);

      losSchema.addAll(spaltenDieDazuGekommenSind);

      Set<String> spaltenDieWeggefallenSind = new HashSet<String>(losSchema);
      spaltenDieWeggefallenSind.removeAll(schema);

      losSchema.removeAll(spaltenDieWeggefallenSind);

      if (spaltenDieWeggefallenSind.isEmpty()
        && spaltenDieDazuGekommenSind.isEmpty()) return;

      Logger.log(L.m("Das Datenbank-Schema wurde geändert. Der Cache wird angepasst."));

      Iterator<LOSDJDataset> iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = iter.next();

        Iterator<String> spalte = spaltenDieWeggefallenSind.iterator();
        while (spalte.hasNext())
          ds.drop(spalte.next());

        ds.setSchema(losSchema);
      }
    }

    /**
     * Liefert die Anzahl der Datensätze im LOS.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int size()
    {
      return data.size();
    }

    /**
     * Iterator über alle Datensätze im LOS.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Iterator<? extends Dataset> iterator()
    {
      return data.iterator();
    }

    /**
     * true, falls der LOS leer ist.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public boolean isEmpty()
    {
      return data.isEmpty();
    }

    /**
     * Ein Datensatz im LOS bzw Cache.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private class LOSDJDataset extends DJDatasetBase
    {
      /**
       * Der Schlüsselwert dieses Datensatzes.
       */
      private String key;

      /**
       * Erzeugt einen neuen LOSDJDataset.
       * 
       * @param dscache
       *          die Map, deren Werte den gecachten Werten aus der
       *          Hintergrunddatenbank entsprechen.
       * @param dsoverride
       *          die Map, deren Werte den lokalen Overrides entsprechen.
       * @param schema
       *          das Schema des LOS zu dem dieser Datensatz gehört.
       * @param key
       *          der Schlüsselwert dieses Datensatzes.
       */
      public LOSDJDataset(Map<String, String> dscache,
          Map<String, String> dsoverride, Set<String> schema, String key)
      { // TESTED
        super(dscache, dsoverride, schema);
        this.key = key;
      }

      /**
       * Entfernt die Spalte namens columnName aus lokalem Override und Cache dieses
       * Datensatzes.
       * 
       * @param columnName
       * @author Matthias Benkmann (D-III-ITD 5.1)
       */
      public void drop(String columnName)
      { // TESTED
        if (isFromLOS()) myLOS.remove(columnName);
        if (hasBackingStore()) myBS.remove(columnName);
      }

      /**
       * Ändert die Referenz auf das Schema dieses Datensatzes. Eine Anpassung der im
       * Datensatz gespeicherten Werte geschieht nicht. Dafür muss drop() verwendet
       * werden.
       * 
       * @author Matthias Benkmann (D-III-ITD 5.1)
       */
      public void setSchema(Set<String> losSchema)
      { // TESTED
        this.schema = losSchema;
      }

      /**
       * Erzeugt eine Kopie dieses Datensatzes im LOS.
       */
      public DJDataset copy()
      {
        LOSDJDataset newDS =
          new LOSDJDataset(this.myBS, isFromLOS() ? new HashMap<String, String>(
            this.myLOS) : new HashMap<String, String>(), this.schema, this.key);
        LocalOverrideStorage.this.data.add(newDS);
        if (selectedDataset == null) selectedDataset = newDS;
        return newDS;
      }

      /**
       * Entfernt diesen Datensatz aus dem LOS.
       */
      public void remove() throws UnsupportedOperationException
      {
        // dieser Test ist nur der vollständigkeit halber hier, für den
        // Falls dass diese Funktion mal in anderen Kontext gecopynpastet
        // wird. Ein LOSDJDataset ist immer aus dem LOS.
        if (!isFromLOS())
          throw new UnsupportedOperationException(
            L.m("Versuch, einen Datensatz, der nicht aus dem LOS kommt zu entfernen"));

        LocalOverrideStorage.this.data.remove(this);
        if (selectedDataset == this)
        {
          if (LocalOverrideStorage.this.data.isEmpty())
            selectedDataset = null;
          else
            selectedDataset = LocalOverrideStorage.this.data.get(0);
        }
      }

      public boolean isSelectedDataset()
      {
        return this == selectedDataset;
      }

      public void select() throws UnsupportedOperationException
      {
        if (!isFromLOS()) throw new UnsupportedOperationException();
        selectedDataset = this;
      }

      public String getKey()
      { // TESTED
        return this.key;
      }
    }
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

    public void set(String columnName, String newValue)
        throws ColumnNotFoundException, UnsupportedOperationException,
        IllegalArgumentException
    {
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
    }

    public boolean hasLocalOverride(String columnName)
        throws ColumnNotFoundException
    {
      return false;
    }

    public boolean hasBackingStore()
    {
      return true;
    }

    public boolean isFromLOS()
    {
      return false;
    }

    public boolean isSelectedDataset()
    {
      return false;
    }

    public void select() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
    }

    public void discardLocalOverride(String columnName)
        throws ColumnNotFoundException, NoBackingStoreException
    {
    // nichts zu tun
    }

    public DJDataset copy()
    {
      return myLOS.copyNonLOSDataset(myDS);
    }

    public void remove() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException(
        L.m("Datensatz kommt nicht aus dem LOS"));
    }

    public String get(String columnName) throws ColumnNotFoundException
    {
      return myDS.get(columnName);
    }

    public String getKey()
    {
      return myDS.getKey();
    }
  }
}
