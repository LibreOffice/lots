/*
* Dateiname: DatasourceJoiner.java
* Projekt  : WollMux
* Funktion : stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten
*            aus verschiedenen Hintergrunddatenbanken zieht.
* 
* Copyright: Landeshauptstadt München
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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten aus
 * verschiedenen Hintergrunddatenbanken zieht.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceJoiner
{
  /**
   * Wird an Datasource.find() übergeben, um die maximale Zeit der
   * Bearbeitung einer Suchanfrage zu begrenzen, damit nicht im Falle
   * eines Netzproblems alles einfriert.
   */
  private static final long QUERY_TIMEOUT = 3000;
  
  /**
   * Muster für erlaubte Suchstrings für den Aufruf von find().
   */
  private static final Pattern SUCHSTRING_PATTERN = Pattern.compile("^\\*?[^*]+\\*?$");
  
  /**
   * Bildet Datenquellenname auf Datasource-Objekt ab. Nur die jeweils zuletzt
   * unter einem Namen in der Config-Datei aufgeführte Datebank ist hier
   * verzeichnet.
   */
  private Map nameToDatasource = new HashMap();
  private LocalOverrideStorage myLOS;
  
  /**
   * Die Datenquelle auf die sich find(), getLOS(), etc beziehen.
   */
  protected Datasource mainDatasource;
  
  /**
   * Erzeugt einen neuen DatasourceJoiner.
   * @param joinConf ein ConfigThingy mit "Datenquellen" Kindern.
   * @param mainSourceName der Name der Datenquelle, auf die sich die
   * Funktionen des DJ (find(),...) beziehen sollen.
   * @param losCache die Datei, in der der DJ die Datensätze des LOS
   *        abspeichern soll. Falls diese Datei existiert, wird sie vom
   *        Konstruktor eingelesen und verwendet.
   * @param context, der Kontext relativ zu dem Datenquellen URLs in ihrer Beschreibung
   *        auswerten sollen.
   * @throws ConfigurationErrorException falls ein schwerwiegender Fehler
   *         auftritt, der die Arbeit des DJ unmöglich macht, wie z.B.
   *         wenn die Datenquelle mainSourceName in der
   *         joinConf fehlt und gleichzeitig kein Cache verfügbar ist.
   */
  public DatasourceJoiner(ConfigThingy joinConf, String mainSourceName, File losCache, URL context)
  throws ConfigurationErrorException
  {
    init(joinConf, mainSourceName, losCache, context);
  }
  
  /**
   * Nur für die Verwendung durch abgeleitete Klassen, die den parametrisierten
   * Konstruktor nicht verwenden können, und stattdessen init() benutzen.
   */
  protected DatasourceJoiner(){};
  
  /**
   * Erledigt die Initialisierungsaufgaben des Konstruktors mit den gleichen
   * Parametern. Für die Verwendung durch abgeleitete Klassen, die den
   * parametrisierten Konstruktor nicht verwenden können.
   * @throws ConfigurationErrorException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected void init(ConfigThingy joinConf, String mainSourceName, File losCache, URL context)
  throws ConfigurationErrorException
  { //TESTED
    ConfigThingy datenquellen = joinConf.query("Datenquellen").query("Datenquelle");
    Iterator iter = datenquellen.iterator();
    while (iter.hasNext())
    {
      ConfigThingy sourceDesc = (ConfigThingy)iter.next();
      ConfigThingy c = sourceDesc.query("NAME");
      if (c.count() == 0)
      {
        Logger.error("Datenquelle ohne NAME gefunden");
        continue;
      }
      String name = c.toString();
      
      c = sourceDesc.query("TYPE");
      if (c.count() == 0)
      {
        Logger.error("Datenquelle "+name+" hat keinen TYPE");
        continue;
      }
      String type = c.toString();
      
      Datasource ds = null;
      try{
        if (type.equals("conf"))
          ds = new ThingyDatasource(nameToDatasource, sourceDesc, context);
        else if (type.equals("ldap"))
          ds = new LDAPDatasource(nameToDatasource, sourceDesc, context);
        else
          Logger.error("Ununterstützter Datenquellentyp: " + type);
      }
      catch(Exception x)
      {
        Logger.error("Fehler beim Initialisieren von Datenquelle \""+name+"\":", x);
      }
  
      if (ds == null)
      {
        Logger.error("Datenquelle '"+name+"' von Typ '"+type+"' konnte nicht initialisiert werden");
        /*
         * Falls schon eine alte Datenquelle name registriert ist, 
         * entferne diese Registrierung. Ansonsten würde mit der vorher
         * registrierten Datenquelle weitergearbeitet, was seltsame Effekte
         * zur Folge hätte die schwierig nachzuvollziehen sind. 
         */
        nameToDatasource.remove(name);
        continue;
      }
      
      nameToDatasource.put(name, ds);
    }
    
    myLOS = new LocalOverrideStorage(losCache, context);
    
    Set schema = myLOS.getSchema();
    
    if (!nameToDatasource.containsKey(mainSourceName))
    { 
      if (schema == null) throw new ConfigurationErrorException("Datenquelle "+mainSourceName+" nicht definiert und Cache nicht vorhanden");
      
      Logger.error("Datenquelle "+mainSourceName+" nicht definiert => verwende alte Daten aus Cache");
      mainDatasource = new EmptyDatasource(schema, mainSourceName);
      nameToDatasource.put(mainSourceName, mainDatasource);
    }
    else
    {
      mainDatasource = (Datasource)nameToDatasource.get(mainSourceName);

      try{
        myLOS.refreshFromDatabase(mainDatasource, QUERY_TIMEOUT);
      } catch(TimeoutException x)
      {
        Logger.error("Timeout beim Zugriff auf Datenquelle "+mainDatasource.getName()+" => Benutze Daten aus Cache");
      }

    }
  }
 
  /**
   * Liefert das Schema der Hauptdatenquelle zurück.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set getMainDatasourceSchema()
  { //TESTED
    return mainDatasource.getSchema();
  }
  
   
  /**
   * Durchsucht die Hauptdatenbank (nicht den LOS) 
   * nach Datensätzen, die in Spalte
   * spaltenName den Wert suchString stehen haben. suchString kann
   * vorne und/oder hinten genau ein Sternchen '*' stehen haben, um
   * Präfix/Suffix/Teilstring-Suche zu realisieren. Folgen mehrerer Sternchen
   * oder Sternchen in der Mitte des Suchstrings sind verboten und
   * produzieren eine IllegalArgumentException. Ebenso verboten ist ein
   * suchString, der nur Sternchen enthält oder einer der leer ist.
   * Alle Ergebnisse sind {@link DJDataset}s.
   * Die Suche erfolgt grundsätzlich case-insensitive.
   * <p>
   * Im folgenden eine Liste möglicher Suchanfragen mit Angabe, ob sie
   * unterstützt wird (X) oder nicht (O).
   * </p>
   * <pre>
Suche nach 
X           "vorname.nachname"
X           "vorname.nachname@muenchen.de"
X           "Nam"
O           "ITD5.1"  nicht unterstützt weil Minus vor 5.1 fehlt
X           "ITD-5.1"
O           "D"   liefert Personen mit Nachname-Anfangsbuchstabe D
X           "D-*"
O           "ITD5"    nicht unterstützt weil Minus vor 5 fehlt
X           "D-HAIII"
X           "5.1"
X           "D-III-ITD-5.1"
O           "D-HAIII-ITD-5.1"   nicht unterstützt, da HA nicht im lhmOUShortname
O           "D-HAIII-ITD5.1"    nicht unterstützt (siehe oben)

X           "Nam Vorn"
X           "Nam, Vorn"
X           "Vorname Name"
X           "Vorn Nam"
X           "ITD 5.1"
O           "D-HAIII-ITD 5.1"   steht nicht mit HA im LDAP
X           "V. Nachname"
X           "Vorname N."
</pre>
   * @throws TimeoutException falls die Anfrage nicht innerhalb einer 
   * intern vorgegebenen Zeitspanne beendet werden konnte.
   */
  public QueryResults find(String spaltenName, String suchString) throws TimeoutException
  { //TESTED
    if (suchString == null || !SUCHSTRING_PATTERN.matcher(suchString).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString);
    
    List query = new Vector();
    query.add(new QueryPart(spaltenName, suchString));
    return find(query);
  }
  
  /**
   * Wie find(spaltenName, suchString), aber mit einer zweiten Spaltenbedingung,
   * die und-verknüpft wird.
   * @throws TimeoutException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2) throws TimeoutException
  {
    if (suchString1 == null || !SUCHSTRING_PATTERN.matcher(suchString1).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString1);
    if (suchString2 == null || !SUCHSTRING_PATTERN.matcher(suchString2).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString2);
    
    List query = new Vector();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    return find(query);
  }
  
  /**
   * Findet Datensätze, die query (Liste von QueryParts) entsprechen.
   * @throws TimeoutException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private QueryResults find(List query) throws TimeoutException
  { //TESTED
    QueryResults res = mainDatasource.find(query, QUERY_TIMEOUT);
    List djDatasetsList = new Vector(res.size());
    Iterator iter = res.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      djDatasetsList.add(new DJDatasetWrapper(ds));
    }
    return new QueryResultsList(djDatasetsList);
  }
  
  
  /**
   * Speichert den aktuellen LOS samt zugehörigem Cache in die Datei
   * cacheFile.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveCacheAndLOS(File cacheFile) throws IOException
  {
    Set schema = myLOS.getSchema();
    if (schema == null)
    {
      Logger.error("Kann Cache nicht speichern, weil nicht initialisiert.");
      return;
    }
    
    ConfigThingy conf = new ConfigThingy(cacheFile.getPath());
    ConfigThingy schemaConf = conf.add("Schema");
    Iterator iter = schema.iterator();
    while (iter.hasNext())
    {
      schemaConf.add((String)iter.next());
    }
    
    ConfigThingy datenConf = conf.add("Daten");
    myLOS.dumpData(datenConf);
    
    try{
      Dataset ds = getSelectedDataset();
      conf.add("Ausgewaehlt").add(ds.getKey());
    }catch(DatasetNotFoundException x) {}
    
    Writer out = new OutputStreamWriter(new FileOutputStream(cacheFile),ConfigThingy.CHARSET);
    out.write(conf.stringRepresentation(true, '"'));
    out.close();
  }
  
  /**
   * Liefert den momentan im Lokalen Override Speicher ausgewählten Datensatz.
   * @throws DatasetNotFoundException falls der LOS leer ist (ansonsten ist
   * immer ein Datensatz selektiert).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return myLOS.getSelectedDataset();
  }
  
  /**
   * Liefert alle Datensätze des Lokalen Override Speichers (als {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}).
   */
  public QueryResults getLOS()
  {
    return new QueryResultsList(myLOS.iterator(), myLOS.size());
  }
  
  /**
   * Legt einen neuen Datensatz im LOS an, der nicht mit einer Hintergrunddatenbank
   * verknüpft ist und liefert ihn zurück. Alle Felder des neuen Datensatzes
   * sind mit dem Namen der entsprechenden Spalte initialisiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset newDataset()
  {
    return myLOS.newDataset();
  };
  
  /**
   * Verwaltet den LOS des DJ.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class LocalOverrideStorage
  {
    /**
     * Präfix, das vor generierte Schlüssel von LOS-only Datensätzen gesetzt
     * wird, um diese eindeutig von anderen Schlüsseln unterscheiden
     * zu können.
     */
    private static final String LOS_ONLY_MAGIC = "GEHORCHE DEM WOLLMUX!";
    /**
     * Liste aller LOSDJDatasets.
     */
    private List data = new LinkedList();
    
    /**
     * Das Schema des LOS. Dies ist null solange es nicht initialisiert wurde.
     * Falls beim Laden des Cache ein Fehler auftritt kann dies auch nach
     * dem Konstruktor noch null sein.
     */
    private Set losSchema = null;
    /**
     * Der ausgewählte Datensatz. Nur dann null, wenn data leer ist.
     */
    private DJDataset selectedDataset = null;
    
    /**
     * Basis für die Erzeugung eines Schlüssels für einen LOS-only Datensatz.
     */
    private long nextGeneratedKey = new Date().getTime();
    
    /**
     * Versucht, den Cache und den LOS aus der Datei losCache (ConfigThingy)
     * zu lesen. %includes in losCache werden relativ zu context aufgelöst.  
     */
    public LocalOverrideStorage(File losCache, URL context)
    { //TESTED
      String selectKey = "";
      if (losCache.canRead())
      {
        try
        {
          ConfigThingy cacheData = new ConfigThingy(losCache.getPath(),context, new InputStreamReader(new FileInputStream(losCache),ConfigThingy.CHARSET));
          /*
           * Falls der Cache korrupt ist sollen keine korrupten Daten in unseren
           * globalen Felder stehen, deswegen erzeugen wir erstmal alles in
           * temporären Variablen und kopieren diese nachher in die Felder
           * losSchema und this.data. 
           */
          Set newSchema = new HashSet();
          List data = new LinkedList();
          Iterator iter = cacheData.get("Schema").iterator();
          while (iter.hasNext())
            newSchema.add(iter.next().toString());
          
          iter = cacheData.get("Daten").iterator();
          while (iter.hasNext())
          {
            ConfigThingy dsconf = (ConfigThingy)iter.next();
            
            Map dscache = new HashMap();
            Iterator iter2 = dsconf.get("Cache").iterator();
            while (iter2.hasNext())
            {
              ConfigThingy dsNode = (ConfigThingy)iter2.next();
              String spalte = dsNode.getName();
              if (!newSchema.contains(spalte))
              {
                Logger.error(losCache.getPath()+" enthält korrupten Datensatz (Spalte "+spalte+" nicht im Schema) => Cache wird ignoriert!");
                return;
              }
              
              dscache.put(spalte, dsNode.toString());
            }
            
            Map dsoverride = new HashMap();
            iter2 = dsconf.get("Override").iterator();
            while (iter2.hasNext())
            {
              ConfigThingy dsNode = (ConfigThingy)iter2.next();
              String spalte = dsNode.getName();
              if (!newSchema.contains(spalte))
              {
                Logger.error(losCache.getPath()+" enthält korrupten Datensatz (Spalte "+spalte+" nicht im Schema) => Cache wird ignoriert!");
                return;
              }
              
              dsoverride.put(spalte, dsNode.toString());
            }
            
            data.add(new LOSDJDataset(dscache, dsoverride, newSchema, dsconf.get("Key").toString()));
            
          }
          
          selectKey = cacheData.get("Ausgewaehlt").toString();
          
          losSchema = newSchema;
          this.data = data;
        }
        catch (FileNotFoundException e) { Logger.error(e); }
        catch (IOException e) { Logger.error(e); }
        catch (SyntaxErrorException e) { Logger.error(e); } 
        catch (NodeNotFoundException e) { Logger.error(e); }
      }
      else
        Logger.log("Cache-Datei "+losCache.getPath()+" kann nicht gelesen werden.");
      
      selectDataset(selectKey);
    }
    
    /**
     * Falls es im LOS momentan einen Datensatz mit Schlüssel selectKey gibt,
     * so wird er zum ausgewählten Datensatz, ansonsten wird, falls der LOS
     * mindestens einen Datensatz enthält, ein beliebiger Datensatz
     * ausgewählt.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void selectDataset(String selectKey)
    { //TESTED
      if (!data.isEmpty()) selectedDataset = (DJDataset)data.get(0);
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        if (selectKey.equals(ds.getKey()))
        {
          selectedDataset = ds;
          return;
        }
      }
      
    }

    /**
     * Generiert einen neuen (eindeutigen) Schlüssel für die Erzeugung
     * eines LOS-only Datensatzes.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private String generateKey()
    {
      return LOS_ONLY_MAGIC + (nextGeneratedKey++);
    }
    
    /**
     * Erzeugt einen neuen Datensatz, der nicht mit Hintergrundspeicher
     * verknüpft ist.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset newDataset()
    {
      Map dsoverride = new HashMap();
      Iterator iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = (String)iter.next();
        dsoverride.put(spalte,spalte);
      }
      DJDataset ds = new LOSDJDataset(null, dsoverride, losSchema, generateKey()); 
      data.add(ds);
      if (selectedDataset == null) selectedDataset = ds;
      return ds;
    }
    
    /**
     * Erzeugt eine Kopie im LOS vom Datensatz ds, der nicht aus dem
     * LOS kommen darf.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset copyNonLOSDataset(Dataset ds)
    {
      if (ds instanceof LOSDJDataset)
        Logger.error("Diese Funktion darf nicht für LOSDJDatasets aufgerufen werden, da sie immer eine Kopie mit Backing Store erzeugt.");
      
      Map dsoverride = new HashMap();
      Map dscache = new HashMap();
      Iterator iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = (String)iter.next();
        try
        {
          String wert = ds.get(spalte);
          dscache.put(spalte,wert);
        }
        catch (ColumnNotFoundException e)
        {
          Logger.error(e);
        }
      }
      DJDataset newDs = new LOSDJDataset(dscache, dsoverride, losSchema, ds.getKey()); 
      data.add(newDs);
      if (selectedDataset == null) selectedDataset = newDs;
      return newDs;
    }

    /**
     * Liefert den momentan im LOS selektierten Datensatz zurück.
     * @throws DatasetNotFoundException falls der LOS leer ist (sonst ist
     * immer ein Datensatz selektiert).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DJDataset getSelectedDataset() throws DatasetNotFoundException
    {
      if (data.isEmpty()) throw new DatasetNotFoundException("Der Lokale Override Speicher ist leer");
      return selectedDataset;
    }

    /**
     * Läd für die Datensätze des LOS aktuelle Daten aus der Datenbank database.
     * @param timeout die maximale Zeit, die database Zeit hat, anfragen
     * zu beantworten.
     * @throws TimeoutException
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void refreshFromDatabase(Datasource database, long timeout) throws TimeoutException
    { //TESTED
      Map keyToLOSDJDataset = new HashMap();
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        keyToLOSDJDataset.put(ds.getKey(), ds);
      }
      QueryResults res = database.getDatasetsByKey(keyToLOSDJDataset.keySet(), timeout);
      
      /*
       * Schema anpassen und DANACH data leeren. Dadurch werden die
       * LOS-Speicher der LOSDJDatasets an das neue Schema angepasst,
       * bevor der Speicher geleert wird. Dies ist notwendig, da die
       * LOS-Speicher später direkt an die aus res neu erzeugten 
       * LOSDJDatasets weitergereicht werden.
       */
      this.setSchema(database.getSchema()); 
      data.clear();
      String selectKey = "";
      if (selectedDataset != null) selectKey = selectedDataset.getKey();
      selectedDataset = null;
      
      /*
       * Neue Datensätze auf Basis der Query erzeugen. Dabei werden die
       * LOS-Speicher von den korrespndierenden alten (gefunden via
       * keyToLOSDJDataset) direkt übernommen.
       * ACHTUNG: Hierbei werden auch temporär im Hintergrundspeicher
       * "verlorene" Datensätze wieder mit dem Hintergrundspeicher
       * verknüpft. Sie langer Kommentar weiter unten.
       * Bei evtl. Änderungen bitte beachten!!!
       */
      
      iter = res.iterator();
      while (iter.hasNext())
      {
        try{
          Dataset sourceDS = (Dataset)iter.next();
          
          Map dscache = new HashMap();
          
          Iterator spalte = losSchema.iterator();
          while (spalte.hasNext())
          {
            String spaltenName = (String)spalte.next();
            String spaltenWert = sourceDS.get(spaltenName);
            if (spaltenWert != null)
              dscache.put(spaltenName, spaltenWert);
          }
          
          String key = sourceDS.getKey();
          
          LOSDJDataset override = (LOSDJDataset)keyToLOSDJDataset.remove(key);
          Map dsoverride;
          if (override == null)
            dsoverride = new HashMap();
          else
            dsoverride = override.getLOS();
          
          data.add(new LOSDJDataset(dscache, dsoverride, losSchema, key));
        }catch(Exception x) {Logger.error(x);}
      }
      
      
      /* TODO Folgender Kommentar sollte vermutlich auch ins Handbuch.
       * Es ist möglich, dass noch Datensätze aus dem alten LOS übrig sind
       * für die keine aktuellen Daten gefunden wurden. Dies sind entweder
       * Datensätze, die von vorneherein nicht mit einer Hintergrunddatenbank
       * verknüpft waren oder Datensätze, die aufgrund von Änderungen des
       * Hintergrundspeichers nicht mehr gefunden wurden. Die Datensätze,
       * die von vorneherein nur im LOS existierten müssen auf jeden Fall
       * erhalten bleiben. Bei den anderen ist es eine gute Frage, was
       * sinnvoll ist. Momentan bleiben auch sie erhalten. Das hat folgende
       * Vor- und Nachteile:
       * Vorteile:
       *   - Falls das Verschwinden des Datensatzes nur ein temporäres Problem
       *     war, so wird er wenn er wieder im Hintergrundspeicher auftaucht
       *     (und den selben Schlüssel hat) wieder damit verknüpft.
       *   - Der Benutzer verliert nie Einträge seiner Absenderliste
       * Nachteile:
       *   - Der Benutzer merkt evtl. nicht, dass er plötzlich vom
       *     Hintergrundspeicher abgekoppelt ist und bekommt gewünschte
       *     Änderungen nicht mit.
       *   - Die Admins haben keine Möglichkeit, einen Eintrag aus der
       *     Absenderliste eines Benutzers zu entfernen (ausser sie
       *     greifen direkt auf sein .wollmux Verzeichnis zu.
       *   - Falls ein Datensatz bewusst entfernt wurde und später ein
       *     neuer Datensatz mit dem selben Schlüssel angelegt wird, so
       *     wird der Eintrag in der Absenderliste mit dem neuen Eintrag
       *     verknüpft, obwohl dieser nichts mit dem alten zu tun hat.
       */
      iter = keyToLOSDJDataset.values().iterator();
      while (iter.hasNext())
        data.add(iter.next());
      
      selectDataset(selectKey);
    }

    /**
     * Liefert null, falls bislang kein Schema vorhanden (weil das Laden
     * der Cache-Datei im Konstruktur fehlgeschlagen ist).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Set getSchema() {return losSchema;} //TESTED
    
    /**
     * Fügt conf die Beschreibung der Datensätze im LOS als Kinder hinzu.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void dumpData(ConfigThingy conf)
    {
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        ConfigThingy dsConf = conf.add("");
        dsConf.add("Key").add(ds.getKey());
        
        ConfigThingy cacheConf = dsConf.add("Cache");
        Iterator entries = ds.getBS().entrySet().iterator();
        while (entries.hasNext())
        {
          Map.Entry ent = (Map.Entry)entries.next();
          String spalte = (String)ent.getKey();
          String wert = (String)ent.getValue();
          if (wert != null) cacheConf.add(spalte).add(wert);
        }
        
        ConfigThingy overrideConf = dsConf.add("Override");
        entries = ds.getLOS().entrySet().iterator();
        while (entries.hasNext())
        {
          Map.Entry ent = (Map.Entry)entries.next();
          String spalte = (String)ent.getKey();
          String wert = (String)ent.getValue();
          if (wert != null) overrideConf.add(spalte).add(wert);
        }
      }
    }
    
    /**
     * Ändert das Datenbankschema. Spalten des alten Schemas, die im neuen
     * nicht mehr vorhanden sind werden aus den Datensätzen gelöscht. 
     * Im neuen Schema hinzugekommene Spalten werden in den Datensätzen
     * als unbelegt betrachtet. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void setSchema(Set schema)
    { //TESTED
      if (losSchema == null)
      {
        losSchema = new HashSet(schema);
        return;
      }
      
      Set spaltenDieDazuGekommenSind = new HashSet(schema);
      spaltenDieDazuGekommenSind.removeAll(losSchema);
      
      losSchema.addAll(spaltenDieDazuGekommenSind);
      
      Set spaltenDieWeggefallenSind = new HashSet(losSchema);
      spaltenDieWeggefallenSind.removeAll(schema);
      
      losSchema.removeAll(spaltenDieWeggefallenSind);
      
      if (spaltenDieWeggefallenSind.isEmpty() 
       && spaltenDieDazuGekommenSind.isEmpty()) return;
      
      Logger.log("Das Datenbank-Schema wurde geändert. Der Cache wird angepasst.");
      
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        
        Iterator spalte = spaltenDieWeggefallenSind.iterator();
        while (spalte.hasNext())
          ds.drop((String)spalte.next());
        
        ds.setSchema(losSchema);
      }
    }
    
     /**
      * Liefert die Anzahl der Datensätze im LOS.
      * @author Matthias Benkmann (D-III-ITD 5.1)
      */
    public int size()
    {
      return data.size();
    }

    /**
     * Iterator über alle Datensätze im LOS.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Iterator iterator()
    {
      return data.iterator();
    }

    /**
     * true, falls der LOS leer ist.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public boolean isEmpty()
    {
      return data.isEmpty();
    }
    
    /**
     * Ein Datensatz im LOS bzw Cache.
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
       * @param dscache die Map, deren Werte den gecachten Werten aus der
       * Hintergrunddatenbank entsprechen.
       * @param dsoverride die Map, deren Werte den lokalen Overrides
       * entsprechen.
       * @param schema das Schema des LOS zu dem dieser Datensatz gehört.
       * @param key der Schlüsselwert dieses Datensatzes.
       */
      public LOSDJDataset(Map dscache, Map dsoverride, Set schema, String key)
      { //TESTED
        super(dscache, dsoverride, schema);
        this.key = key;
      }

      /**
       * Entfernt die Spalte namens columnName aus lokalem Override und Cache
       * dieses Datensatzes.
       * @param columnName
       * @author Matthias Benkmann (D-III-ITD 5.1)
       */
      public void drop(String columnName)
      { //TESTED
        if (isFromLOS()) myLOS.remove(columnName);
        if (hasBackingStore()) myBS.remove(columnName);
      }

      /**
       * Ändert die Referenz auf das Schema dieses Datensatzes. Eine Anpassung
       * der im Datensatz gespeicherten Werte geschieht nicht. Dafür muss
       * drop() verwendet werden.
       * @author Matthias Benkmann (D-III-ITD 5.1)
       */
      public void setSchema(Set losSchema)
      { //TESTED
        this.schema = losSchema;        
      }

      /**
       * Erzeugt eine Kopie dieses Datensatzes im LOS.
       */
      public DJDataset copy()
      {
        DJDataset newDS = new LOSDJDataset(this.myBS, isFromLOS()? new HashMap(this.myLOS): new HashMap(), this.schema, this.key);
        LocalOverrideStorage.this.data.add(newDS);
        if (selectedDataset == null) selectedDataset = newDS;
        return newDS;
      }

      /**
       * Entfernt diesen Datensatz aus dem LOS.
       */
      public void remove() throws UnsupportedOperationException
      {
        //dieser Test ist nur der vollständigkeit halber hier, für den
        //Falls dass diese Funktion mal in anderen Kontext gecopynpastet
        //wird. Ein LOSDJDataset ist immer aus dem LOS.
        if (!isFromLOS()) throw new UnsupportedOperationException("Versuch, einen Datensatz, der nicht aus dem LOS kommt zu entfernen");
        
        LocalOverrideStorage.this.data.remove(this);
        if (selectedDataset == this)
        {
          if (LocalOverrideStorage.this.data.isEmpty()) 
            selectedDataset = null;
          else
            selectedDataset = (DJDataset)LocalOverrideStorage.this.data.get(0);
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
      { //TESTED
        return this.key;
      }
    }
  }

  /**
   * Ein Wrapper um einfache Datasets, wie sie von Datasources als Ergebnisse
   * von Anfragen zurückgeliefert werden. Der Wrapper ist notwendig, um
   * die auch für Fremddatensätze sinnvollen DJDataset Funktionen anbieten
   * zu können, allen voran copy(). 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DJDatasetWrapper implements DJDataset
  {
    private Dataset myDS;
    
    public DJDatasetWrapper(Dataset ds)
    {
      myDS = ds;
    }
    
    public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException, IllegalArgumentException
    {
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
    }

    public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
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
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
    }

    public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
    {
      //nichts zu tun
    }

    public DJDataset copy()
    {
      return myLOS.copyNonLOSDataset(myDS);
    }

    public void remove() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
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
