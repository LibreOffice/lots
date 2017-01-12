package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.AttachDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Datasource;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LDAPDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorage;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageDummyImpl;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl;
import de.muenchen.allg.itd51.wollmux.core.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.OverlayDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.PreferDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.SchemaDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.ThingyDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.UnionDatasource;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class DatasourceJoinerFactory
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceJoinerFactory.class);

  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;

  /**
   * Initialisiert den DJ wenn nötig und liefert ihn dann zurück (oder null, falls
   * ein Fehler während der Initialisierung aufgetreten ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DatasourceJoiner getDatasourceJoiner()
  {
    if (datasourceJoiner == null)
    {
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
          senderSourceStr = DatasourceJoiner.NOCONFIG;
  
        datasourceJoiner =
          new DatasourceJoiner(collectDatasources(WollMuxFiles.getWollmuxConf(),
              WollMuxFiles.getDEFAULT_CONTEXT()), 
              senderSourceStr, 
              createLocalOverrideStorage(senderSourceStr, WollMuxFiles.getLosCacheFile(), WollMuxFiles.getDEFAULT_CONTEXT()),
              datasourceTimeoutLong);
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
        Map<Object, Object> context = new HashMap<>();
        ColumnTransformer columnTransformer =
          new ColumnTransformer(FunctionFactory.parseTrafos(WollMuxFiles.getWollmuxConf(),
            "AbsenderdatenSpaltenumsetzung", funcLib, dialogLib, context));
        datasourceJoiner.setTransformer(columnTransformer);
      }
      catch (ConfigurationErrorException e)
      {
        LOGGER.error("", e);
      }
    }
  
    return datasourceJoiner;
  }

  private static Map<String, Datasource> collectDatasources(ConfigThingy joinConf, URL context)
  {
    HashMap<String, Datasource> datasources = new HashMap<>();
    
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
        if (type.equals("conf"))
          ds = new ThingyDatasource(datasources, sourceDesc, context);
        else if (type.equals("union"))
          ds = new UnionDatasource(datasources, sourceDesc, context);
        else if (type.equals("attach"))
          ds = new AttachDatasource(datasources, sourceDesc, context);
        else if (type.equals("overlay"))
          ds = new OverlayDatasource(datasources, sourceDesc, context);
        else if (type.equals("prefer"))
          ds = new PreferDatasource(datasources, sourceDesc, context);
        else if (type.equals("schema"))
          ds = new SchemaDatasource(datasources, sourceDesc, context);
        else if (type.equals("ldap"))
          ds = new LDAPDatasource(datasources, sourceDesc, context);
        else if (type.equals("ooo"))
          ds = new OOoDatasource(datasources, sourceDesc, context);
        else if (type.equals("funky"))
          ds = new FunkyDatasource(datasources, sourceDesc, context);
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
        datasources.put(name, null);
        continue;
      }

      datasources.put(name, ds);
    }
    
    return datasources;
  }
  
  private static LocalOverrideStorage createLocalOverrideStorage(String mainSourceName, File losCache, URL context)
  {
    // kann sein, dass noch kein singleton erstellt ist - kein Zugriff auf no config
    if (mainSourceName.equals(DatasourceJoiner.NOCONFIG))
    {
      return new LocalOverrideStorageDummyImpl();// no config, kein cache ! 
    }
    else
    {
      return new LocalOverrideStorageStandardImpl(losCache, context);//mit config
    }
  }

  /**
   * Diese Methode liefert eine Liste mit den über {@link #senderDisplayTemplate}
   * definierten String-Repräsentation aller verlorenen gegangenen Datensätze des
   * DatasourceJoiner (gemäß {@link DatasourceJoiner.Status.lostDatasets}) zurück.
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
    ArrayList<String> list = new ArrayList<>();
    for (Dataset ds : dj.getStatus().lostDatasets)
      list.add(new DatasetListElement(ds, PersoenlicheAbsenderliste.getInstance().getSenderDisplayTemplate()).toString());
    return list;
}}
