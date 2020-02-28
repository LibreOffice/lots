package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sdbc.SQLException;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XRow;
import com.sun.star.sdbc.XRowSet;
import com.sun.star.sdbcx.XColumnsSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;

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
    results = UNO.XRowSet(UNO.createUNOService("com.sun.star.sdb.RowSet"));
    try
    {
      this.datasourceName = UNO.XPropertySet(ds.getDataSource()).getPropertyValue("Name")
          .toString();
      this.document = ds;
      UNO.XModifiable(document).addModifyListener(modifyListener);

      ds.getDataSource().setLoginTimeout(DBModel.MAILMERGE_LOGIN_TIMEOUT);
      conn = ds.getDataSource().getConnection("", "");

      XPropertySet xProp = UNO.XPropertySet(results);
      xProp.setPropertyValue("ActiveConnection", conn);
      xProp.setPropertyValue("EscapeProcessing", Boolean.FALSE);
      xProp.setPropertyValue("CommandType", Integer.valueOf(com.sun.star.sdb.CommandType.COMMAND));
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
      XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
      for (String name : tables.getElementNames())
        tableNames.add(name);
      XNameAccess queries = UNO.XQueriesSupplier(conn).getQueries();
      for (String name : queries.getElementNames())
        tableNames.add(name);
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

    ConfigThingy dq = new ConfigThingy("Datenquelle");
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
      XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
      XColumnsSupplier columnsSupplier = UnoRuntime.queryInterface(XColumnsSupplier.class,
          tables.getByName(tableName));

      if (columnsSupplier == null)
        return;
      XNameAccess xColumns = columnsSupplier.getColumns();
      String[] aColumnNames = xColumns.getElementNames();
      List<String> columns = Arrays.asList(aColumnNames);

      UNO.setProperty(results, "Command", "SELECT * FROM " + sqlIdentifier(tableName) + ";");
      results.execute();
      XRow row = UNO.XRow(results);
      int id = 1;
      while (results.next())
      {
        for (int i = 0; i < columns.size(); i++)
        {
          String column = columns.get(i);
          String value = row.getString(i + 1);
          data.put(id, column, value);
        }
        id++;
      }
    } catch (SQLException | IllegalArgumentException | UnoHelperException | NoSuchElementException
        | WrappedTargetException e)
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
