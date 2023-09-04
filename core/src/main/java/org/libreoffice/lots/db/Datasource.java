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

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for data sources that the DJ can manage. CAUTION! The constructors of this class must not perform 
 * potentially long-blocking actions (e.g., establishing a network connection). 
 * They must not fail if any preconditions are not met, which are only relevant for
 * accessing the records (e.g., connection to the LDAP server).
 * The constructor should (and must) only fail if it's not possible to bring the data source 
 * into a state where it can execute methods that are independent of the records. 
 * Most importantly, this includes methods for querying the schema.
 * For methods that access records, their failure due to conditions (e.g., no network)
 * must not render the data source object unusable. Wherever possible,
 * it should be possible to retry an operation at a later time if the conditions have changed,
 * and the operation should succeed then. This particularly means that connection establishment to servers,
 * where necessary, should be retried as needed and not just once in the constructor.
 * In this context, it should be noted that connections should be explicitly
 * closed using close() (typically in a finally() block to ensure it's executed even in exceptional cases)
 * because Java's Garbage Collection may do this very late. <br>
 * <br> Arguments against the "override" data source type: - (correct) search implementation
 * would be difficult and inefficient - would likely result in poorer data maintenance in
 * LDAP because it's easier to introduce an override
 */
public abstract class Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Datasource.class);

  private static Long datasourceTimeout = null;

  /**
   * Returns a list containing the titles of all columns in the data source.
   */
  public abstract List<String> getSchema();

  /**
   * Returns all records whose keys are included in the collection 'keys.'
   * Please note that the uniqueness of keys is only a recommendation.
   * The number of returned records may exceed the number of provided keys.
   *
   * @param keys
   *          Keys to search against.
   * @return Results as {@link QueryResults}
   */
  public abstract QueryResults getDatasetsByKey(Collection<String> keys);

  /**
   * Returns all records that satisfy all conditions specified in the 'query' (a list of {@link QueryPart}s).
   * If 'query' is empty, no records are returned. If 'query' contains conditions on columns that the database
   * does not have, no records are returned.
   *
   * @param query
   *          Query to search against the main datasource.
   * @return Results as {@link QueryResults}
   */
  public abstract QueryResults find(List<QueryPart> query);

  /**
   * Returns an implementation-dependent subset of records from the data source.
   * Ideally, the data source should return all of its records here, or at least as many as possible.
   * However, it is also permissible for no records to be returned in this context.
   */
  public abstract QueryResults getContents();

  /**
   * Returns the name of this data source.
   */
  public abstract String getName();

  /**
   * Gets datasource value by given {@link ConfigThingy} and key.
   *
   * @param source
   *          {@link ConfigThingy} ConfigThingy that should contain a configured datasource.
   * @param key
   *          Name of the datasource that should be found.
   * @param errorMessage
   *          ErrorMessage that is thrown if configuration could not be parsed successfully.
   * @return Value of the datasource, i.e. 'ldap://test.ip'
   */
  public String parseConfig(ConfigThingy source, String key, Supplier<String> errorMessage)
  {
    return source.get(key, ConfigurationErrorException.class, errorMessage.get()).toString();
  }

  public static long getDatasourceTimeout()
  {
    if (datasourceTimeout == null)
    {
      datasourceTimeout = 10000l;
      ConfigThingy dataSourceTimeout = WollMuxFiles.getWollmuxConf().query("DATASOURCE_TIMEOUT", 1);
      try
      {
        long timeout = Long.parseLong(dataSourceTimeout.getLastChild().toString());

        if (timeout <= 0)
        {
          LOGGER.error("DATASOURCE_TIMEOUT has to be greater than 0");
        } else
        {
          datasourceTimeout = timeout;
        }
      } catch (NodeNotFoundException e)
      {
        LOGGER.error("", e);
      } catch (NumberFormatException e)
      {
        LOGGER.error("DATASOURCE_TIMEOUT has to be an integer number");
      }
    }
    return datasourceTimeout;
  }
}
