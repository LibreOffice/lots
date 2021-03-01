/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.dialog.adapter.AbstractCloseListener;
import de.muenchen.allg.itd51.wollmux.HashableComponent;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.CalcModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DBModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * The model of the mail merge.
 */
public class ConnectionModel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionModel.class);

  /**
   * List of connected data sources.
   */
  private static final Map<HashableComponent, DatasourceModel> connections = new HashMap<>();

  /**
   * List of listeners waiting for changes.
   */
  private static final List<ConnectionModelListener> listeners = new ArrayList<>();

  /**
   * Listener on open connections. If the files are closed, they connection is removed.
   */
  private static AbstractCloseListener documentCloseListener = new AbstractCloseListener()
  {
    @Override
    public void disposing(EventObject event)
    {
      HashableComponent hash = new HashableComponent(event.Source);
      if (connections.containsKey(hash))
      {
        connections.remove(hash);
        listeners.forEach(ConnectionModelListener::connectionsChanged);
      }
    }
  };

  private ConnectionModel()
  {
    // shouldn't be instantiated
  }

  /**
   * Add a new listener waiting for changes.
   *
   * @param listener
   *          The new listener.
   */
  public static void addListener(ConnectionModelListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a listener waiting for changes.
   *
   * @param listener
   *          The new listener.
   */
  public static void removeListener(ConnectionModelListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Activates a data source and table.
   *
   * @param name
   *          The name of the data source and table.
   * @return The model, which should be selected. Can be the same as current.
   * @throws NoTableSelectedException
   *           No table with this name exists.
   */
  public static Optional<DatasourceModel> selectDatasource(String name)
      throws NoTableSelectedException
  {
    if (name == null || name.isEmpty())
    {
      LOGGER.info("Es ist keine Datenquelle mehr ausgewählt.");
      return Optional.empty();
    }

    for (DatasourceModel ds : connections.values())
    {
      for (String tableName : ds.getTableNames())
      {
        if (name.startsWith(ds.getName()) && name.endsWith(tableName))
        {
          LOGGER.info("Datenquelle {} ist ausgewählt.", ds.getName());
          ds.activateTable(tableName);
          return Optional.of(ds);
        }
      }
    }
    LOGGER.info("Es ist keine Datenquelle mehr ausgewählt.");
    return Optional.empty();
  }

  /**
   * Add a new data source.
   *
   * @param document
   *          The document on which the data source is based.
   * @param tableName
   *          If this optional is present it contains the name of the table to select.
   * @return The model, which should be selected. Can be the same as current.
   * @throws NoTableSelectedException
   *           If the table name doesn't exist.
   */
  public static Optional<DatasourceModel> addAndSelectDatasource(Object document,
      Optional<String> tableName) throws NoTableSelectedException
  {
    assert document != null : "Document is null";
    HashableComponent hash = new HashableComponent(document);
    if (!connections.containsKey(hash))
    {
      DatasourceModel model = createDatasourceModel(document);
      if (model != null)
      {
        connections.put(hash, model);

        String name = tableName.orElseGet(() -> {
          List<String> tableNames = model.getTableNames();
          if (!tableNames.isEmpty())
          {
            return model.getTableNames().get(0);
          }
          return "";
        });
        listeners.forEach(ConnectionModelListener::connectionsChanged);
        if (name != null && !name.isEmpty())
        {
          return selectDatasource(buildConnectionName(model.getName(), name));
        }
      }
    }
    return Optional.of(connections.get(hash));
  }

  /**
   * Get a list of connections names ({@link #buildConnectionName(String, String)}).
   *
   * @return A list of names.
   */
  public static List<String> getConnections()
  {
    return connections.values().stream()
        .flatMap(m -> m.getTableNames().stream().map(t -> buildConnectionName(m.getName(), t)))
        .collect(Collectors.toList());
  }

  /**
   * Build a connection name.
   *
   * @param datasource
   *          The data source for which we need the name.
   * @return The name.
   * @throws NoTableSelectedException
   *           If no table is selected.
   */
  public static String buildConnectionName(Optional<DatasourceModel> datasource)
      throws NoTableSelectedException
  {
    if (datasource.isEmpty())
    {
      return "";
    }
    return buildConnectionName(datasource.get().getName(), datasource.get().getActivatedTable());
  }

  /**
   * Build a connection name.
   *
   * @param m
   *          The name of the data source.
   * @param t
   *          The name of the table.
   * @return The connection name.
   */
  public static String buildConnectionName(String m, String t)
  {
    return m + " - " + t;
  }

  /**
   * Is there an open connection.
   *
   * @param name
   *          The name of the database.
   * @param table
   *          The name of the table.
   * @return True if there is a connection, false otherwise.
   */
  public static boolean hasConnection(String name, String table)
  {
    for (DatasourceModel model : connections.values())
    {
      if (name.equals(model.getName()) && model.getTableNames().contains(table))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a {@link DatasourceModel} from a document.
   *
   * @param document
   *          The document.
   * @return The model.
   */
  private static DatasourceModel createDatasourceModel(Object document)
  {
    DatasourceModel model = null;
    XSpreadsheetDocument spread = UNO.XSpreadsheetDocument(document);
    if (spread != null)
    {
      model = new CalcModel(spread);

    } else
    {
      XOfficeDatabaseDocument ds = UnoRuntime.queryInterface(XOfficeDatabaseDocument.class,
          document);
      if (ds != null)
      {
        model = new DBModel(ds);
      }
    }
    if (model != null)
    {
      model.addCloseListener(documentCloseListener);
    }
    return model;
  }

  /**
   * Adds a data source model for each open calc file, if its not already in the list of
   * connections.
   */
  public static void addOpenCalcWindows()
  {
    try
    {
      XSpreadsheetDocument spread = null;
      UnoCollection<XComponent> components = UnoCollection.getCollection(UNO.desktop.getComponents(), XComponent.class);
      boolean notify = false;
      for (XComponent component : components)
      {
        spread = UNO.XSpreadsheetDocument(component);
        if (spread != null)
        {
          HashableComponent hash = new HashableComponent(spread);
          if (!connections.containsKey(hash))
          {
            connections.put(hash, createDatasourceModel(spread));
            notify = true;
          }
        }
      }
      if (notify)
      {
        listeners.forEach(ConnectionModelListener::connectionsChanged);
      }
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

}
