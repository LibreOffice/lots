/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.mailmerge.ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.sdb.CommandType;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sdbc.SQLException;
import com.sun.star.sdbc.XColumnLocate;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XRow;
import com.sun.star.sdbc.XRowSet;
import com.sun.star.sdbcx.XColumnsSupplier;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XModifyListener;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.mailmerge.FieldSubstitution;
import org.libreoffice.lots.mailmerge.NoTableSelectedException;

/**
 * A data source model based on LibreOffice Base.
 */
public class DBModel implements DatasourceModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DBModel.class);

  /**
   * The name of this database.
   */
  private String datasourceName;

  /**
   * The row set for executing queries.
   */
  private XRowSet results;

  /**
   * The database connection.
   */
  private XConnection conn;

  /**
   * The base document.
   */
  private XOfficeDatabaseDocument document;

  /**
   * The name of the currently selected table.
   */
  private String tableName = null;

  /**
   * The data of the currently selected table.
   */
  HashBasedTable<Integer, String, String> data = HashBasedTable.create();

  /**
   * Login timeout in seconds.
   */
  private static final int MAILMERGE_LOGIN_TIMEOUT = 30;

  /**
   * Listener for changes in the base file.
   */
  private XModifyListener modifyListener = new XModifyListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // nothing to do
    }

    @Override
    public void modified(EventObject event)
    {
      try
      {
        readTable();
      } catch (NoTableSelectedException e)
      {
        LOGGER.debug("", e);
      }
    }
  };

  /**
   * Creates a new model.
   *
   * @param ds
   *          The underlying Base document.
   */
  public DBModel(XOfficeDatabaseDocument ds)
  {
    results = UNO.XRowSet(UnoComponent.createComponentWithContext(UnoComponent.CSS_SDB_ROW_SET));
    try
    {
      this.datasourceName = UnoProperty.getProperty(ds.getDataSource(), UnoProperty.NAME).toString();
      this.document = ds;
      UNO.XModifiable(document).addModifyListener(modifyListener);

      ds.getDataSource().setLoginTimeout(DBModel.MAILMERGE_LOGIN_TIMEOUT);
      conn = ds.getDataSource().getConnection("", "");

      UnoProperty.setProperty(results, UnoProperty.ACTIVE_CONNECTION, conn);
      UnoProperty.setProperty(results, UnoProperty.ESCAPE_PROCESSING, false);
      UnoProperty.setProperty(results, UnoProperty.COMMAND_TYPE, CommandType.COMMAND);
    } catch (SQLException x)
    {
      LOGGER.error("Kann keine Verbindung zur Datenquelle {} herstellen", datasourceName);
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  @Override
  public void addDatasourceListener(DatasourceModelListener listener)
  {
    // model doesn't react on changes in the database.
  }

  @Override
  public void removeDatasourceListener(DatasourceModelListener listener)
  {
    // model doesn't react on changes in the database.
  }

  @Override
  public String getActivatedTable()
  {
    return tableName;
  }

  @Override
  public void activateTable(String tableName) throws NoTableSelectedException
  {
    this.tableName = tableName;
    readTable();
    LOGGER.debug("Tabelle {} wurde ausgewählt", tableName);
  }

  @Override
  public void dispose()
  {
    UNO.XModifiable(document).removeModifyListener(modifyListener);
    if (conn != null)
    {
      try
      {
        conn.close();
      } catch (SQLException e)
      {
        LOGGER.error("", e);
      }
    }
    if (results != null)
    {
      UNO.XComponent(results).dispose();
    }
  }

  @Override
  public String getName()
  {
    return this.datasourceName;
  }

  @Override
  public List<String> getTableNames()
  {
    List<String> tableNames = new ArrayList<>();

    try
    {
      UnoDictionary<Object> tables = UnoDictionary.create(UNO.XTablesSupplier(conn).getTables(), Object.class);
      tableNames.addAll(tables.keySet());
      UnoDictionary<Object> queries = UnoDictionary.create(UNO.XQueriesSupplier(conn).getQueries(), Object.class);
      tableNames.addAll(queries.keySet());
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }

    return tableNames;
  }

  @Override
  public Set<String> getColumnNames() throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    return data.columnKeySet();
  }

  @Override
  public Table<Integer, String, String> getData() throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    return data;
  }

  @Override
  public int getNumberOfRecords() throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    return data.rowMap().size();
  }

  @Override
  public void toFront()
  {
    XModel documentModel = UNO.XModel(document);
    if (documentModel != null)
    {
      XTopWindow win = UNO
          .XTopWindow(documentModel.getCurrentController().getFrame().getContainerWindow());
      win.toFront();
      UNO.XWindow(win).setVisible(true);
    }
  }

  @Override
  public boolean supportsAddColumns()
  {
    return false;
  }

  @Override
  public void addColumns(Map<String, FieldSubstitution> mapIdToSubstitution)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConfigThingy getSettings() throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    ConfigThingy dq = new ConfigThingy("DataSource");
    if (datasourceName != null && !getTableNames().isEmpty())
    {
      ConfigThingy arg = new ConfigThingy("TYPE");
      arg.addChild(new ConfigThingy("ooo"));
      dq.addChild(arg);
      arg = new ConfigThingy("SOURCE");
      arg.addChild(new ConfigThingy(datasourceName));
      dq.addChild(arg);
      arg = new ConfigThingy("TABLE");
      arg.addChild(new ConfigThingy(tableName));
      dq.addChild(arg);
    }
    return dq;
  }

  @Override
  public Map<String, String> getRecord(int index) throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    if (!data.containsRow(index))
    {
      Collections.emptyMap();
    }
    return data.row(index);
  }

  @Override
  public void addCloseListener(XCloseListener listener)
  {
    UNO.XCloseable(document).addCloseListener(listener);
  }

  @Override
  public void removeCloseListener(XCloseListener listener)
  {
    UNO.XCloseable(document).removeCloseListener(listener);
  }

  /**
   * Read the data from the currently selected table.
   *
   * @throws NoTableSelectedException
   *           No sheet is selected.
   */
  private void readTable() throws NoTableSelectedException
  {
    if (tableName == null)
    {
      throw new NoTableSelectedException();
    }

    data.clear();
    try
    {
      UnoDictionary<XColumnsSupplier> tables = UnoDictionary.create(UNO.XTablesSupplier(conn)
          .getTables(), XColumnsSupplier.class);
      XColumnsSupplier columnsSupplier = tables.get(tableName);

      if (columnsSupplier == null)
        return;
      UnoDictionary<Object> columns = UnoDictionary.create(columnsSupplier.getColumns(), Object.class);
      Set<String> columnNames = columns.keySet();

      UnoProperty.setProperty(results, UnoProperty.COMMAND, "SELECT * FROM " + sqlIdentifier(tableName) + ";");
      results.execute();
      XRow row = UNO.XRow(results);
      XColumnLocate locate = UNO.XColumnLocate(results);
      int id = 1;
      while (results.next())
      {
        for (String column : columnNames)
        {
          String value = row.getString(locate.findColumn(column));
          data.put(id, column, value);
        }
        id++;
      }
    } catch (SQLException | IllegalArgumentException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Escapes a string to be used in SQL statements.
   *
   * @param str
   *          The string the escape.
   * @result An escaped string always starting and ending with double quotes.
   */
  private static String sqlIdentifier(String str)
  {
    return "\"" + str.replaceAll("\"", "\"\"") + "\"";
  }
}
