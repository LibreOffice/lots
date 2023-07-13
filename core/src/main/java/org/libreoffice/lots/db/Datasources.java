/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of all {@link Datasource} registered in WollMux.
 */
public class Datasources
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Datasources.class);

  private static Map<String, Datasource> datasources = null;

  private Datasources()
  {
    // nothing to do
  }

  /**
   * Parse the configuration for {@link Datasource} definitions.
   *
   * @return Mapping from data source name to {@link Datasource}.
   */
  public static Map<String, Datasource> getDatasources()
  {
    if (datasources != null)
    {
      return datasources;
    }

    HashMap<String, Datasource> datasources = new HashMap<>();
    ConfigThingy joinConf = WollMuxFiles.getWollmuxConf();
    URL context = WollMuxFiles.getDefaultContext();

    ConfigThingy datenquellen = joinConf.query("DataSources").query("DataSource");
    for (ConfigThingy sourceDesc : datenquellen)
    {
      String name = sourceDesc.getString("NAME");
      String type = sourceDesc.getString("TYPE");
      if (name == null || type == null)
      {
        LOGGER.error("Data source without NAME or TYPE found");
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
          LOGGER.error("Unsupported data source type: {}", type);
          break;
        }
      } catch (Exception x)
      {
        LOGGER.error("Error during initialization of data source '{}' (Type '{}'):", name, type, x);
      }

      if (ds == null)
      {
        LOGGER.error("Data source {} of type {} could not be initialized", name, type);
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
}
