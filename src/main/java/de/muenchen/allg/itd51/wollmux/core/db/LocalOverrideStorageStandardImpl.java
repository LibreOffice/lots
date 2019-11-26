package de.muenchen.allg.itd51.wollmux.core.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner.Status;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Verwaltet den LOS des DJ.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class LocalOverrideStorageStandardImpl implements LocalOverrideStorage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalOverrideStorageStandardImpl.class);

  /**
   * Präfix, das vor generierte Schlüssel von LOS-only Datensätzen gesetzt wird,
   * um diese eindeutig von anderen Schlüsseln unterscheiden zu können.
   */
  private static final String LOS_ONLY_MAGIC = "GEHORCHE DEM WOLLMUX!";

  /**
   * Liste aller LOSDJDatasets. Die Liste muss geordnet sein, damit Datensätze
   * mit gleichem Schlüssel über ihre Position in der Liste identifiziert werden
   * können.
   */
  private List<LOSDJDataset> data = new LinkedList<>();

  /**
   * Das Schema des LOS. Dies ist null solange es nicht initialisiert wurde.
   * Falls beim Laden des Cache ein Fehler auftritt kann dies auch nach dem
   * Konstruktor noch null sein.
   */
  private List<String> losSchema = null;

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
  public LocalOverrideStorageStandardImpl(File losCache, URL context)
  { // TESTED
    String selectKey = "";
    String sameKeyIndex = "";
    if (losCache.canRead())
    {
      try
      {
        ConfigThingy cacheData = new ConfigThingy(losCache.getPath(), context,
            new InputStreamReader(new FileInputStream(losCache), ConfigThingy.CHARSET));
        /*
         * Falls der Cache korrupt ist sollen keine korrupten Daten in unseren
         * globalen Felder stehen, deswegen erzeugen wir erstmal alles in
         * temporären Variablen und kopieren diese nachher in die Felder
         * losSchema und this.data.
         */
        List<String> newSchema = new ArrayList<>();
        List<LOSDJDataset> newData = new LinkedList<>();
        for (ConfigThingy schema : cacheData.get("Schema"))
          newSchema.add(schema.toString());

        for (ConfigThingy dsconf : cacheData.get("Daten"))
        {
          LOSDJDataset ds = parseData(losCache.getPath(), dsconf, newSchema);
          if (ds == null)
          {
            return;
          }
          newData.add(ds);
        }

        ConfigThingy ausgewaehlt = cacheData.get("Ausgewaehlt");
        selectKey = ausgewaehlt.getFirstChild().toString();
        sameKeyIndex = ausgewaehlt.getLastChild().toString();

        losSchema = newSchema;
        this.data = newData;
      } catch (IOException | SyntaxErrorException | NodeNotFoundException e)
      {
        LOGGER.error("", e);
      }
    } else
      LOGGER.info(L.m("Cache-Datei %1 kann nicht gelesen werden.", losCache.getPath()));

    int sameKeyIndexInt = 0;
    try
    {
      sameKeyIndexInt = Integer.parseInt(sameKeyIndex);
    } catch (NumberFormatException e)
    {
      LOGGER.trace("", e);
    }
    selectDataset(selectKey, sameKeyIndexInt);
  }

  private LOSDJDataset parseData(String losCachePath, ConfigThingy dsconf, List<String> newSchema)
      throws NodeNotFoundException
  {
    Map<String, String> dscache = null;
    ConfigThingy cacheColumns = dsconf.query("Cache");
    if (cacheColumns.count() > 0)
    {
      dscache = new HashMap<>();
      for (ConfigThingy dsNode : cacheColumns.getFirstChild())
      {
        String spalte = dsNode.getName();
        if (!newSchema.contains(spalte))
        {
          LOGGER.error(
              L.m("%1 enthält korrupten Datensatz (Spalte %2 nicht im Schema) => Cache wird ignoriert!", losCachePath, spalte));
          return null;
        }

        dscache.put(spalte, dsNode.toString());
      }
    }
    // else LOS-only Datensatz, dscache bleibt null

    Map<String, String> dsoverride = new HashMap<>();
    for (ConfigThingy dsNode : dsconf.get("Override"))
    {
      String spalte = dsNode.getName();
      if (!newSchema.contains(spalte))
      {
        LOGGER
            .error(L.m("%1 enthält korrupten Datensatz (Spalte %2 nicht im Schema) => Cache wird ignoriert!", losCachePath, spalte));
        return null;
      }

      dsoverride.put(spalte, dsNode.toString());
    }
    return new LOSDJDataset(dscache, dsoverride, newSchema, dsconf.get("Key").toString());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.db.LOSInterface#selectDataset(java.lang.
   * String, int)
   */
  @Override
  public void selectDataset(String selectKey, int sameKeyIndex)
  {
    if (!data.isEmpty())
    {
      selectedDataset = data.get(0);
    }

    for (LOSDJDataset ds : data)
    {
      if (selectKey.equals(ds.getKey()))
      {
        selectedDataset = ds;
        if (--sameKeyIndex < 0)
        {
          return;
        }
      }
    }
  }

  /**
   * Generiert einen neuen (eindeutigen) Schlüssel für die Erzeugung eines
   * LOS-only Datensatzes.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String generateKey()
  {
    return LOS_ONLY_MAGIC + (nextGeneratedKey++);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#newDataset()
   */
  @Override
  public DJDataset newDataset()
  {
    Map<String, String> dsoverride = new HashMap<>();
    Iterator<String> iter = losSchema.iterator();
    while (iter.hasNext())
    {
      String spalte = iter.next();
      dsoverride.put(spalte, spalte);
    }
    LOSDJDataset ds = new LOSDJDataset(null, dsoverride, losSchema, generateKey());
    data.add(ds);
    if (selectedDataset == null)
    {
      selectedDataset = ds;
    }
    return ds;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#copyNonLOSDataset(de.
   * muenchen.allg.itd51.wollmux.db.Dataset)
   */
  @Override
  public DJDataset copyNonLOSDataset(Dataset ds)
  {
    if (ds instanceof LOSDJDataset)
      LOGGER
          .error(L.m("Diese Funktion darf nicht für LOSDJDatasets aufgerufen werden, da sie immer eine Kopie mit Backing Store erzeugt."));

    Map<String, String> dsoverride = new HashMap<>();
    Map<String, String> dscache = new HashMap<>();
    Iterator<String> iter = losSchema.iterator();
    while (iter.hasNext())
    {
      String spalte = iter.next();
      try
      {
        String wert = ds.get(spalte);
        dscache.put(spalte, wert);
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
    }
    LOSDJDataset newDs = new LOSDJDataset(dscache, dsoverride, losSchema, ds.getKey());
    data.add(newDs);
    if (selectedDataset == null)
    {
      selectedDataset = newDs;
    }
    return newDs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#getSelectedDataset()
   */
  @Override
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    if (data.isEmpty())
      throw new DatasetNotFoundException(L.m("Der Lokale Override Speicher ist leer"));
    return selectedDataset;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#
   * getSelectedDatasetSameKeyIndex()
   */
  @Override
  public int getSelectedDatasetSameKeyIndex() throws DatasetNotFoundException
  {
    DJDataset ds = getSelectedDataset();
    String key = ds.getKey();
    int idx = 0;
    Iterator<LOSDJDataset> iter = data.iterator();
    while (iter.hasNext())
    {
      LOSDJDataset ds2 = iter.next();
      if (ds2 == ds)
      {
        return idx;
      }
      if (ds2.getKey().equals(key))
      {
        ++idx;
      }
    }

    return idx;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#refreshFromDatabase(de.
   * muenchen.allg.itd51.wollmux.db.Datasource, long,
   * de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner.Status)
   */
  @Override
  public List<Dataset> refreshFromDatabase(Datasource database, long timeout, Status status) throws TimeoutException
  { // TESTED
    /*
     * Zuallererst das Schema anpassen. Insbesondere muss dies VOR dem Leeren
     * von data erfolgen. Dadurch werden die LOS-Speicher der LOSDJDatasets an
     * das neue Schema angepasst, bevor der Speicher geleert wird. Dies ist
     * notwendig, da die LOS-Speicher später direkt an die aus res neu erzeugten
     * LOSDJDatasets weitergereicht werden.
     */
    this.setSchema(database.getSchema());

    /*
     * Mappt Schlüssel auf Listen mit Datensätzen, die diese Schlüssel haben.
     * Hier werden Listen verwendet, da mehrere Datensätze denselben Schlüssel
     * haben können, z.B. wenn der selbe LDAP-Datensatz mehrfach eingefügt wurde
     * um mit verschiedenen Rollen verwendet zu werden.
     */
    Map<String, List<LOSDJDataset>> keyToLOSDJDatasetList = new HashMap<>();

    for (LOSDJDataset ds : data)
    {
      String key = ds.getKey();
      keyToLOSDJDatasetList.computeIfAbsent(key, k -> new ArrayList<>(1)).add(ds);
    }

    /*
     * Aktualisierte Daten abfragen bevor data geleert wird, damit im Falle
     * eines Timeouts nicht der Cache verloren geht.
     */
    QueryResults res = database.getDatasetsByKey(keyToLOSDJDatasetList.keySet(), timeout);

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
    } catch (DatasetNotFoundException x)
    {
      LOGGER.trace("", x);
    }

    data.clear();
    selectedDataset = null;

    /*
     * Neue Datensätze auf Basis der Query erzeugen. Dabei werden die
     * LOS-Speicher von den korrespondierenden alten (gefunden via
     * keyToLOSDJDatasetList) direkt übernommen. ACHTUNG: Hierbei werden auch
     * temporär im Hintergrundspeicher "verlorene" Datensätze wieder mit dem
     * Hintergrundspeicher verknüpft. Siehe langer Kommentar weiter unten. Bei
     * evtl. Änderungen bitte beachten!!!
     */
    for (Dataset sourceDS : res)
    {
      try
      {
        Map<String, String> dscache = new HashMap<>();

        for (String spaltenName : losSchema)
        {
          String spaltenWert = sourceDS.get(spaltenName);
          if (spaltenWert != null)
          {
            dscache.put(spaltenName, spaltenWert);
          }
        }

        String key = sourceDS.getKey();

        List<LOSDJDataset> overrideList = keyToLOSDJDatasetList.remove(key);
        if (overrideList == null)
          data.add(new LOSDJDataset(dscache, new HashMap<String, String>(), losSchema, key));
        else
        {
          overrideList.forEach(override -> data.add(new LOSDJDataset(dscache, override.getLOS(), losSchema, key)));
        }
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    /*
     * Es ist möglich, dass noch Datensätze aus dem alten LOS übrig sind für die
     * keine aktuellen Daten gefunden wurden. Dies sind entweder Datensätze, die
     * von vorneherein nicht mit einer Hintergrunddatenbank verknüpft waren oder
     * Datensätze, die aufgrund von Änderungen des Hintergrundspeichers nicht
     * mehr gefunden wurden. Die Datensätze, die von vorneherein nur im LOS
     * existierten müssen auf jeden Fall erhalten bleiben. Bei den anderen ist
     * es eine gute Frage, was sinnvoll ist. Momentan bleiben auch sie erhalten.
     * Das hat folgende Vor- und Nachteile: Vorteile: - Falls das Verschwinden
     * des Datensatzes nur ein temporäres Problem war, so wird er wenn er wieder
     * im Hintergrundspeicher auftaucht (und den selben Schlüssel hat) wieder
     * damit verknüpft. - Der Benutzer verliert nie Einträge seiner
     * Absenderliste Nachteile: - Der Benutzer merkt evtl. nicht, dass er
     * plötzlich vom Hintergrundspeicher abgekoppelt ist und bekommt gewünschte
     * Änderungen nicht mit. - Die Admins haben keine Möglichkeit, einen Eintrag
     * aus der Absenderliste eines Benutzers zu entfernen (ausser sie greifen
     * direkt auf sein .wollmux Verzeichnis zu. - Falls ein Datensatz bewusst
     * entfernt wurde und später ein neuer Datensatz mit dem selben Schlüssel
     * angelegt wird, so wird der Eintrag in der Absenderliste mit dem neuen
     * Eintrag verknüpft, obwohl dieser nichts mit dem alten zu tun hat.
     */
    List<Dataset> lostDatasets = new ArrayList<>();
    for (List<LOSDJDataset> djDatasetList : keyToLOSDJDatasetList.values())
    {
      for (LOSDJDataset ds : djDatasetList)
      {
        try
        {
          if (ds.hasBackingStore())
            lostDatasets.add(new SimpleDataset(losSchema, ds));
        } catch (ColumnNotFoundException x)
        {
          LOGGER.error("", x);
        }
        data.add(ds);
      }
    }

    status.lostDatasets = lostDatasets;

    StringBuilder buf = new StringBuilder();
    Iterator<Dataset> iter2 = lostDatasets.iterator();
    while (iter2.hasNext())
    {
      Dataset ds = iter2.next();
      buf.append(ds.getKey());
      if (iter2.hasNext())
      {
        buf.append(", ");
      }
    }
    if (buf.length() > 0)
      LOGGER.info("{}{}", L.m("Die Datensätze mit folgenden Schlüsseln konnten nicht aus der Datenbank aktualisiert werden: "), buf);

    selectDataset(selectKey, sameKeyIndex);

    return lostDatasets;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return losSchema;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.db.LOSInterface#dumpData(de.muenchen.allg.
   * itd51.parser.ConfigThingy)
   */
  @Override
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
        Iterator<Map.Entry<String, String>> entries = ds.getBS().entrySet().iterator();
        while (entries.hasNext())
        {
          Map.Entry<String, String> ent = entries.next();
          String spalte = ent.getKey();
          String wert = ent.getValue();
          if (wert != null)
          {
            cacheConf.add(spalte).add(wert);
          }
        }
      }

      ConfigThingy overrideConf = dsConf.add("Override");
      Iterator<Map.Entry<String, String>> entries = ds.getLOS().entrySet().iterator();
      while (entries.hasNext())
      {
        Map.Entry<String, String> ent = entries.next();
        String spalte = ent.getKey();
        String wert = ent.getValue();
        if (wert != null)
        {
          overrideConf.add(spalte).add(wert);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.db.LOSInterface#setSchema(java.util.Set)
   */
  @Override
  public void setSchema(List<String> schema)
  {
    if (losSchema == null)
    {
      losSchema = new ArrayList<>(schema);
      return;
    }

    Set<String> spaltenDieDazuGekommenSind = new HashSet<>(schema);
    spaltenDieDazuGekommenSind.removeAll(losSchema);

    losSchema.addAll(spaltenDieDazuGekommenSind);

    Set<String> spaltenDieWeggefallenSind = new HashSet<>(losSchema);
    spaltenDieWeggefallenSind.removeAll(schema);

    losSchema.removeAll(spaltenDieWeggefallenSind);

    if (spaltenDieWeggefallenSind.isEmpty() && spaltenDieDazuGekommenSind.isEmpty())
      return;

    LOGGER.info(L.m("Das Datenbank-Schema wurde geändert. Der Cache wird angepasst."));

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

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#size()
   */
  @Override
  public int size()
  {
    return data.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#iterator()
   */
  @Override
  public Iterator<Dataset> iterator()
  {
    return new ArrayList<Dataset>(data).iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.LOSInterface#isEmpty()
   */
  @Override
  public boolean isEmpty()
  {
    return data.isEmpty();
  }

  /**
   * Ein Datensatz im LOS bzw Cache.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public class LOSDJDataset extends DJDatasetBase
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
    public LOSDJDataset(Map<String, String> dscache, Map<String, String> dsoverride,
        List<String> schema, String key)
    {
      super(dscache, dsoverride, schema);
      this.key = key;
    }

    /**
     * Entfernt die Spalte namens columnName aus lokalem Override und Cache
     * dieses Datensatzes.
     * 
     * @param columnName
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void drop(String columnName)
    { // TESTED
      if (isFromLOS())
      {
        myLOS.remove(columnName);
      }
      if (hasBackingStore())
      {
        myBS.remove(columnName);
      }
    }

    public boolean isDifferentFromLdapDataset(String columnName, LOSDJDataset dataset)
    {
      if (dataset.getLOS() == null || dataset.getBS() == null)
        return false;

      String losValue = dataset.getLOS().get(columnName);
      String bsValue = dataset.getBS().get(columnName);

      if (losValue == null)
      {
        if (bsValue == null || bsValue.isEmpty())
          return false;
        return false;
      }

      if (losValue.isEmpty())
      {
        if (bsValue == null || bsValue.isEmpty())
          return false;
        return true;
      }

      return !losValue.equals(bsValue);
    }

    /**
     * Ändert die Referenz auf das Schema dieses Datensatzes. Eine Anpassung der
     * im Datensatz gespeicherten Werte geschieht nicht. Dafür muss drop()
     * verwendet werden.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void setSchema(List<String> losSchema)
    {
      this.schema = losSchema;
    }

    /**
     * Erzeugt eine Kopie dieses Datensatzes im LOS.
     */
    @Override
    public DJDataset copy()
    {
      LOSDJDataset newDS = new LOSDJDataset(this.myBS, isFromLOS() ? new HashMap<>(this.myLOS) : new HashMap<String, String>(), this.schema,
          this.key);
      LocalOverrideStorageStandardImpl.this.data.add(newDS);
      if (selectedDataset == null)
      {
        selectedDataset = newDS;
      }
      return newDS;
    }

    /**
     * Entfernt diesen Datensatz aus dem LOS.
     */
    @Override
    public void remove()
    {
      // dieser Test ist nur der vollständigkeit halber hier, für den
      // Falls dass diese Funktion mal in anderen Kontext gecopynpastet
      // wird. Ein LOSDJDataset ist immer aus dem LOS.
      if (!isFromLOS())
        throw new UnsupportedOperationException(L.m("Versuch, einen Datensatz, der nicht aus dem LOS kommt zu entfernen"));

      LocalOverrideStorageStandardImpl.this.data.remove(this);
      if (selectedDataset == this)
      {
        if (LocalOverrideStorageStandardImpl.this.data.isEmpty())
          selectedDataset = null;
        else
          selectedDataset = LocalOverrideStorageStandardImpl.this.data.get(0);
      }
    }

    @Override
    public boolean isSelectedDataset()
    {
      return this == selectedDataset;
    }

    @Override
    public void select()
    {
      if (!isFromLOS())
      {
        throw new UnsupportedOperationException();
      }
      selectedDataset = this;
    }

    @Override
    public String getKey()
    { // TESTED
      return this.key;
    }
  }
}
