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
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

public class DatasourceJoinerFactory
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceJoinerFactory.class);

  /**
   * Enthält den zentralen DataSourceJoiner.
   */
  private static DatasourceJoiner datasourceJoiner;

  private static long datasourceTimeout = 10000;

  private DatasourceJoinerFactory()
  {
    // hide public constructor
  }

  /**
   * Initialisiert den DJ wenn nötig und liefert ihn dann zurück (oder null, falls ein Fehler
   * während der Initialisierung aufgetreten ist).
   */
  public static DatasourceJoiner getDatasourceJoiner()
  {
    if (datasourceJoiner == null)
    {
      ConfigThingy senderSource = WollMuxFiles.getWollmuxConf().query("SENDER_SOURCE", 1);
      String senderSourceStr = null;
      try
      {
        senderSourceStr = senderSource.getLastChild().toString();
      } catch (NodeNotFoundException e)
      {
        // hier geben wir im Vergleich zu früher keine Fehlermeldung mehr aus,
        // sondern erst später, wnn
        // tatsächlich auf die Datenquelle "null" zurück gegriffen wird.
      }

      ConfigThingy dataSourceTimeout = WollMuxFiles.getWollmuxConf().query("DATASOURCE_TIMEOUT", 1);
      try
      {
        long timeout = Long.parseLong(dataSourceTimeout.getLastChild().toString());

        if (timeout <= 0)
        {
          LOGGER.error("DATASOURCE_TIMEOUT muss größer als 0 sein!");
        } else
        {
          datasourceTimeout = timeout;
        }
      } catch (NodeNotFoundException e)
      {
        LOGGER.error("", e);
      } catch (NumberFormatException e)
      {
        LOGGER.error(L.m("DATASOURCE_TIMEOUT muss eine ganze Zahl sein"));
      }

      try
      {
        if (null == senderSourceStr)
          senderSourceStr = DatasourceJoiner.NOCONFIG;

        datasourceJoiner = new DatasourceJoiner(
            collectDatasources(WollMuxFiles.getWollmuxConf(), WollMuxFiles.getDefaultContext()),
            senderSourceStr, createLocalOverrideStorage(senderSourceStr,
                WollMuxFiles.getLosCacheFile(), WollMuxFiles.getDefaultContext()));

        FunctionLibrary funcLib = new FunctionLibrary();
        DialogLibrary dialogLib = new DialogLibrary();
        Map<Object, Object> context = new HashMap<>();
        ColumnTransformer columnTransformer = new ColumnTransformer(
            FunctionFactory.parseTrafos(WollMuxFiles.getWollmuxConf(),
                "AbsenderdatenSpaltenumsetzung", funcLib, dialogLib, context));
        datasourceJoiner.setTransformer(columnTransformer);
      } catch (ConfigurationErrorException e)
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
    for (ConfigThingy sourceDesc : datenquellen)
    {
      String name = sourceDesc.getString("NAME");
      String type = sourceDesc.getString("TYPE");
      if (name == null || type == null)
      {
        LOGGER.error(L.m("Datenquelle ohne NAME oder TYPE gefunden"));
        continue;
      }

      Datasource ds = null;
      try
      {
        switch (type)
        {
        case "conf":
          ds = new ThingyDatasource(datasources, sourceDesc, context);
          break;
        case "union":
          ds = new UnionDatasource(datasources, sourceDesc, context);
          break;
        case "attach":
          ds = new AttachDatasource(datasources, sourceDesc, context);
          break;
        case "overlay":
          ds = new OverlayDatasource(datasources, sourceDesc, context);
          break;
        case "prefer":
          ds = new PreferDatasource(datasources, sourceDesc, context);
          break;
        case "schema":
          ds = new SchemaDatasource(datasources, sourceDesc, context);
          break;
        case "ldap":
          ds = new LDAPDatasource(datasources, sourceDesc, context);
          break;
        case "ooo":
          ds = new OOoDatasource(datasources, sourceDesc);
          break;
        case "funky":
          ds = new FunkyDatasource(datasources, sourceDesc);
          break;
        default:
          LOGGER.error("Ununterstützter Datenquellentyp: {}", type);
          break;
        }
      } catch (Exception x)
      {
        LOGGER.error(
            L.m("Fehler beim Initialisieren von Datenquelle \"%1\" (Typ \"%2\"):", name, type), x);
      }

      if (ds == null)
      {
        LOGGER
            .error(L.m("Datenquelle {} von Typ {} konnte nicht initialisiert werden", name, type));
        /*
         * Falls schon eine alte Datenquelle name registriert ist, entferne diese Registrierung.
         * Ansonsten würde mit der vorher registrierten Datenquelle weitergearbeitet, was seltsame
         * Effekte zur Folge hätte die schwierig nachzuvollziehen sind.
         */
      }

      datasources.put(name, ds);
    }

    return datasources;
  }

  private static LocalOverrideStorage createLocalOverrideStorage(String mainSourceName,
      File losCache, URL context)
  {
    // kann sein, dass noch kein singleton erstellt ist - kein Zugriff auf no config
    if (mainSourceName.equals(DatasourceJoiner.NOCONFIG))
    {
      return new LocalOverrideStorageDummyImpl();// no config, kein cache !
    } else
    {
      return new LocalOverrideStorageStandardImpl(losCache, context);// mit config
    }
  }

  public static long getDatasourceTimeout()
  {
    return datasourceTimeout;
  }

  /**
   * Diese Methode liefert eine Liste aller verlorenen gegangenen Datensätze des DatasourceJoiner
   * zurück.
   *
   * @return List der verlorenen Datensätze im Format "<oid> <vorname> <nachname>".
   */
  public static List<String> getLostDatasetDisplayStrings()
  {
    DatasourceJoiner dj = getDatasourceJoiner();
    ArrayList<String> list = new ArrayList<>();
    if (dj != null)
    {
      for (Dataset ds : dj.getLostDatasets())
      {
        try
        {
          StringBuilder strBuilder = new StringBuilder();
          strBuilder.append(ds.get("OID") == null ? "" : ds.get("OID"));
          strBuilder.append(" ");
          strBuilder.append(ds.get("Vorname") == null ? "" : ds.get("Vorname"));
          strBuilder.append(" ");
          strBuilder.append(ds.get("Nachname") == null ? "" : ds.get("Nachname"));

          list.add(strBuilder.toString());
        } catch (ColumnNotFoundException e)
        {
          LOGGER.error("", e);
        }
      }
    }
    return list;
  }
}
