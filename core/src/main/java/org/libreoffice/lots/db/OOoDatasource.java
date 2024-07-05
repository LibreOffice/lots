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
package org.libreoffice.lots.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.sdb.CommandType;
import com.sun.star.sdb.XColumn;
import com.sun.star.sdbc.SQLException;
import com.sun.star.sdbc.XColumnLocate;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sdbc.XResultSet;
import com.sun.star.sdbc.XRow;
import com.sun.star.sdbc.XRowSet;
import com.sun.star.sdbcx.XColumnsSupplier;
import com.sun.star.sdbcx.XKeysSupplier;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.util.L;

/**
 * Provides an OpenOffice.org data source as a WollMux data source.
 */
public class OOoDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OOoDatasource.class);

  /**
   * Maximum time in seconds that the data source is allowed to take for connecting to the database.
   */
  private static final int LOGIN_TIMEOUT = 5;

  /**
   * Constant for {@link #sqlSyntax} indicating that SQL queries should be executed in Oracle syntax.
   */
  private static final int SQL_SYNTAX_ANSI = 0;

  /**
   * Constant for {@link #sqlSyntax} indicating that SQL queries should be executed in Oracle syntax.
   */
  private static final int SQL_SYNTAX_ORACLE = 1;

  /**
   * Constant for {@link #sqlSyntax} indicating that SQL queries should be executed in MySQL syntax.
   */
  private static final int SQL_SYNTAX_MYSQL = 2;

  /**
   * Constant for {@link #sqlSyntax} indicating that SQL queries should be executed in PervasiveSQL syntax.
   */
  private static final int SQL_SYNTAX_PERVASIVESQL = 3;

  /**
   * Which syntax should be used?
   *
   * The default is Oracle syntax
   *
   * o *** ANSI ********* <doublequote symbol> ::= 2 consecutive double quote characters
   *
   * (Lexical Elements, 135 Foundation)A delimited Identifier is an Identifier enclosed in '...' double quotes.
   * Double quotes contained within the Identifier are replaced by <doublequote symbol>.
   * (Lexical Elements, 134 Foundation)
   *
   * <quote symbol> ::= <quote><quote> (2 consecutive quote characters) (Lexical elements, 143
   * Foundation) In a <character string literal> individual <quote>s by <quote symbol>
   * replaced. (Lexical elements, 143 Foundation)
   *
   * o**** Find records based on a given key list *********
   *
   * SELECT * FROM "<id>" WHERE ("<colId>"='<colVal>' AND "<colId>"='<colVal>' AND ...) OR (...) OR
   * ...;
   *
   * In <id> and <colId> Doublequotes are replaced by <doublequote symbol>.
   *
   * In <colVal> Quotes are replaced by <quote symbol>.
   *
   * o ***** Find records that meet certain criteria ********
   *
   * 8.5 <like predicate> (385 Foundation)The string behind ESCAPE must be exactly one character long.
   * Otherwise, an exception is thrown (387 Foundation, 8.5 General Rules 3b)) _ and % as well as the
   * ESCAPE character itself must be escaped in the string expression behind
   * LIKE (by prepending the escape character). Other characters must not be escaped.
   *
   * SELECT * FROM "<id>" WHERE (lower("<colId>") LIKE lower('<pattern>') ESCAPE '|') AND (...) AND
   * ...; In <id> und <colId> Doublequotes are replaced by <doublequote symbol>. In <pattern>
   * are "_", "%" and "|" replaced by "|_", "|%" und "||".
   *****
   * Read all records ******
   *
   * SELECT * FROM "<id>"; In <id> Doublequotes is replaced by <doublequote symbol>.
   ****
   * Oracle ***** Like ANSI MySQL ******* Like ANSI, but with lcase() instead of lower() for PervasiveSQL.
   * ******* Like ANSI, but to the right of LIKE, only simple constants are allowed (no lower or lcase).
   * Additionally, "DATABASE.TABLE" is not supported.
   * Only "DATABASE"."TABLE", DATABASE."TABLE", or "TABLE"." is allowed.
   */
  private int sqlSyntax = SQL_SYNTAX_ANSI;

  /**
   * The name of this data source.
   */
  private String datasourceName;

  /**
   * The name of the OpenOffice data source.
   */
  private String oooDatasourceName;

  /**
   * The name of the table in the OpenOffice data source.
   */
  private String oooTableName;

  /**
   * The schema of this data source.
   */
  private List<String> schema;

  /**
   * The names of the columns that form the primary key.
   */
  private String[] keyColumns;

  /**
   * Username for logging in to the database.
   */
  private String userName = "";

  private static final String SQL_SELECT_COMMAND = "SELECT * FROM ";

  /**
   * Password for logging in to the database.
   */
  @SuppressWarnings("squid:S2068")
  private String password = "";

  /**
   * How {@link #OOoDatasource(Map, ConfigThingy, boolean)}, where noKey==false is passed.
   */
  public OOoDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc)
  {
    this(nameToDatasource, sourceDesc, false);
  }

  /**
   * Creates a new OOoDatasource. If no SQL_SYNTAX parameter is set in ConfigThingy, 'mysql' is used as the default.
   *
   * @param nameToDatasource
   *          Contains all data sources that were fully instantiated up to the
   *          time of defining this OOoDatasource (currently not used).
   * @param sourceDesc
   *          The 'DataSource' node containing the description of this OOoDatasource.
   * @param noKey
   *          If true, the first column is always used as the key column.
   *          This option should only be used if no operations are to be performed that use the key.
   * @throws ConfigurationErrorException
   *           If there is an error in the definition in sourceDesc. 
   *           If sourceDesc does not have a schema subsection,
   *           an attempt is made to obtain the schema from the data source itself.
   *           If an error occurs in this process, this exception is also thrown.
   *           No exception is thrown if the columns of the schema section do not exist in the real database.
   *           In this case, the corresponding columns are treated as empty. TESTED
   */
  public OOoDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, boolean noKey)
  {
    datasourceName = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    oooDatasourceName = parseConfig(sourceDesc, "SOURCE", () -> L.m(
        "Data source \"{0}\": Name of OOo-data source has to be specified as SOURCE", datasourceName));
    oooTableName = parseConfig(sourceDesc, "TABLE",
        () -> L.m("Data source \"{0}\": Name of table/view within the "
            + "OOo-Data Source has to be specified as TABLE", datasourceName));

    userName = sourceDesc.getString("USER", "");
    password = sourceDesc.getString("PASSWORD", "");

    String sqlSyntaxStr = sourceDesc.getString("SQL_SYNTAX", "");

    sqlSyntaxStr = sqlSyntaxStr == null || sqlSyntaxStr.isEmpty() ? "mysql" : sqlSyntaxStr;

    if ("ansi".equalsIgnoreCase(sqlSyntaxStr))
      sqlSyntax = SQL_SYNTAX_ANSI;
    else if ("oracle".equalsIgnoreCase(sqlSyntaxStr))
      sqlSyntax = SQL_SYNTAX_ORACLE;
    else if ("mysql".equalsIgnoreCase(sqlSyntaxStr))
      sqlSyntax = SQL_SYNTAX_MYSQL;
    else if ("pervasivesql".equalsIgnoreCase(sqlSyntaxStr))
      sqlSyntax = SQL_SYNTAX_PERVASIVESQL;
    else
      throw new ConfigurationErrorException(L.m(
        "SQL_SYNTAX \"{0}\" not supported", sqlSyntaxStr));

    schema = new ArrayList<>();
    ConfigThingy schemaConf = sourceDesc.query("Schema");
    if (schemaConf.count() != 0)
    {
      Iterator<ConfigThingy> iter = schemaConf.iterator().next().iterator();
      String firstColumnName = null;
      while (iter.hasNext())
      {
        String columnName = iter.next().toString();
        if (firstColumnName == null) {
          firstColumnName = columnName;
        }
        schema.add(columnName);
      }
      if (schema.isEmpty())
        throw new ConfigurationErrorException(L.m(
          "Data source \"{0}\": Schema-section is empty", datasourceName));
      ConfigThingy schluesselConf = sourceDesc.query("Schluessel");
      if (schluesselConf.count() == 0)
        throw new ConfigurationErrorException(L.m(
          "Data source \"{0}\": Key-section is missing", datasourceName));

      if (noKey)
      {
        if (firstColumnName != null) {
          keyColumns = new String[] { firstColumnName };
        }
      }
      else
        parseKey(schluesselConf); // Test if no key is present, see further.
      // unten
    }
    else
    {
      LOGGER.debug("Schema of the data source {} not specified. Try to get it from the data source.", datasourceName);
      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        ds.setLoginTimeout(LOGIN_TIMEOUT);
        XConnection conn = ds.getConnection(userName, password);

        /*
         * According to the IDL documentation for 'View,' views must also be included here
         */
        UnoDictionary<XColumnsSupplier> tables = UnoDictionary
            .create(UNO.XTablesSupplier(conn).getTables(), XColumnsSupplier.class);
        XColumnsSupplier table = null;
        UnoDictionary<XColumn> columns = null;
        try
        {
          table = tables.get(oooTableName);
          columns = UnoDictionary.create(table.getColumns(), XColumn.class);
        }
        catch (Exception x)
        {
          LOGGER.debug("Table '{}' not found. Try to use it as query name.", oooTableName, x);
          try
          {
            UnoDictionary<XColumnsSupplier> queries = UnoDictionary
                .create(UNO.XQueriesSupplier(conn).getQueries(), XColumnsSupplier.class);
            table = queries.get(oooTableName);
            columns = UnoDictionary.create(table.getColumns(), XColumn.class);
          }
          catch (Exception y)
          {
            throw new ConfigurationErrorException(L.m("Table or query \"{0}\" does not exist in data source "
                + "\"{1}\" or error when determining the columns ", oooTableName, oooDatasourceName), y);
          }
        }
        Set<String> colNames = columns.keySet();
        colNames.forEach(colName -> schema.add(colName));

        if (schema.isEmpty())
          throw new ConfigurationErrorException(L.m(
            "Data source \"{0}\": Table \"{1}\" has no columns", datasourceName,
            oooTableName));

        if (noKey)
        {
          if (!colNames.isEmpty())
          {
            keyColumns = new String[] { colNames.toArray(new String[] {})[0] };
          }
        }
        else
        {
          ConfigThingy schluesselConf = sourceDesc.query("Schluessel");
          if (schluesselConf.count() != 0)
          {
            // Test if no key is present, see further below
            parseKey(schluesselConf);
          } else
          { // Query keys from the database.
            try
            {
              XKeysSupplier keysSupp = UNO.XKeysSupplier(table);
              XColumnsSupplier colSupp =
                UNO.XColumnsSupplier(keysSupp.getKeys().getByIndex(0));
              columns = UnoDictionary.create(colSupp.getColumns(), XColumn.class);
              colNames = columns.keySet();
              keyColumns = colNames.toArray(new String[] {});
            }
            catch (Exception x)
            {
              throw new ConfigurationErrorException(L.m("Data source \"{0}\": No key columns defined."
                  + " Automatic determination of key column not possible.", datasourceName), x);
            }
            // Test if no key is present, see further below
          }
        }
      }
      catch (ConfigurationErrorException x)
      {
        throw x;
      }
      catch (Exception x)
      {
        throw new ConfigurationErrorException(L.m(
          "Schema of OOo-datasource \"{0}\" could not be read.",
          oooDatasourceName), x);
      }

      if (keyColumns.length == 0)
        throw new ConfigurationErrorException(L.m(
          "Data source \"{0}\": No Key column defined", datasourceName));
    }
  }

  /**
   * Parses the first child of conf (which must exist and be a key node) and sets {@link #keyColumns} accordingly.
   * @throws ConfigurationErrorException
   *           If a key column is not in the {@link #schema}. No exception is thrown if the key section is empty.
   *           TESTED
   */
  private void parseKey(ConfigThingy conf)
  {
    conf = conf.iterator().next();
    Iterator<ConfigThingy> iter = conf.iterator();
    ArrayList<String> columns = new ArrayList<>();
    while (iter.hasNext())
    {
      String column = iter.next().toString();
      if (!schema.contains(column))
        throw new ConfigurationErrorException(L.m(
          "Key column \"{0}\" not defined in schema", column));
      if (columns.contains(column))
        throw new ConfigurationErrorException(L.m(
          "Key column \"{0}\" was specified twice in the key section",
          column));

      columns.add(column);
    }
    keyColumns = new String[columns.size()];
    keyColumns = columns.toArray(keyColumns);
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    if (keys.isEmpty()) {
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    StringBuilder buffy =
        new StringBuilder(SQL_SELECT_COMMAND + sqlIdentifier(oooTableName) + " WHERE ");

    Iterator<String> iter = keys.iterator();
    boolean first = true;
    while (iter.hasNext())
    {
      if (!first) {
        buffy.append(" OR ");
      }
      first = false;
      String key = iter.next();
      String[] parts = key.split("#", -1);
      buffy.append('(');
      for (int i = 1; i < parts.length; i += 2)
      {
        if (i > 1) {
          buffy.append(" AND ");
        }
        buffy.append(sqlIdentifier(decode(parts[i - 1])));
        buffy.append('=');
        buffy.append(sqlLiteral(decode(parts[i])));
      }
      buffy.append(')');
    }

    buffy.append(';');

    return sqlQuery(buffy.toString());
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    if (query.isEmpty()) {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    StringBuilder buffy =
        new StringBuilder(SQL_SELECT_COMMAND + sqlIdentifier(oooTableName) + " WHERE ");

    Iterator<QueryPart> iter = query.iterator();
    boolean first = true;
    while (iter.hasNext())
    {
      QueryPart part = iter.next();
      if (!first) {
        buffy.append(" AND ");
      }
      first = false;
      buffy.append('(');
      buffy.append(sqlLower());
      buffy.append('(');
      buffy.append(sqlIdentifier(part.getColumnName()));
      buffy.append(')');
      buffy.append(" LIKE ");

      if (SQL_SYNTAX_PERVASIVESQL == sqlSyntax) {
          // To the right of LIKE, only simple constants and no functions like lcase or lower can be used.
          // Therefore, the search string is converted to lowercase using the Java method toLowerCase.
          // The contents of the column to be searched can in turn be treated with lcase/lower.
          // This ensures that the searching and the search string both contain only lowercase letters.
          buffy.append(sqlLiteral(sqlSearchPattern(part.getSearchString())).toLowerCase());
      } else {
        buffy.append(sqlLower());
        buffy.append('(');
        buffy.append(sqlLiteral(sqlSearchPattern(part.getSearchString())));
        buffy.append(") ESCAPE '|'");
      }

      buffy.append(')');
    }

    buffy.append(';');
    return sqlQuery(buffy.toString());
  }

  @Override
  public QueryResults getContents()
  {
    return sqlQuery(SQL_SELECT_COMMAND + sqlIdentifier(oooTableName) + ";");
  }

  /**
   * Executes the SQL query query against the database and returns the results.
   */
  private QueryResults sqlQuery(String query)
  {
    LOGGER.debug("sqlQuery(\"{}\")", query);

    List<OOoDataset> datasets = new ArrayList<>();

    XRowSet results = null;
    XConnection conn = null;
    try
    {

      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));

        ds.setLoginTimeout((int) Datasource.getDatasourceTimeout());
        conn = ds.getConnection(userName, password);
      }
      catch (SQLException x)
      {
        LOGGER.error("Kann keine Verbindung zur Datenquelle herstellen", x);
      }

      results = UNO.XRowSet(UnoComponent.createComponentWithContext(UnoComponent.CSS_SDB_ROW_SET));

      if (results == null)
        throw new NullPointerException(L.m("Could not create RowSet"));

      UnoProperty.setProperty(results, UnoProperty.ACTIVE_CONNECTION, conn);

      /*
       * EscapeProcessing == false means that OOo should not manipulate the query
       * itself but should pass it directly to the database. When this is used,
       * the result (currently) is always read-only, as OOo cannot perform updates on
       * statements it has not parsed. See comment at http://qa.openoffice.org/issues/show_bug.cgi?id=78522.
       * Equivalent to the button 'Execute SQL Command Directly' in the Base Query Design.
       */
      UnoProperty.setProperty(results, UnoProperty.ESCAPE_PROCESSING, Boolean.FALSE);

      UnoProperty.setProperty(results, UnoProperty.COMMAND_TYPE, CommandType.COMMAND);
      UnoProperty.setProperty(results, UnoProperty.COMMAND, query);

      results.execute();

      Map<String, Integer> mapColumnNameToIndex = getColumnMapping(results);
      XRow row = UNO.XRow(results);

      while (results.next())
      {
        Map<String, String> data = new HashMap<>();
        Iterator<Map.Entry<String, Integer>> iter =
          mapColumnNameToIndex.entrySet().iterator();
        while (iter.hasNext())
        {
          Map.Entry<String, Integer> entry = iter.next();
          String column = entry.getKey();
          int idx = entry.getValue().intValue();
          String value = null;
          if (idx > 0) {
            value = row.getString(idx);
          }
          data.put(column, value);
        }
        datasets.add(new OOoDataset(data));
      }

    }
    catch (Exception x)
    {
      LOGGER.error("Fehler beim Absetzen der Anfrage", x);
    }
    finally
    {
      if (results != null) {
        UNO.XComponent(results).dispose();
      }
      if (conn != null) {
        try
        {
          conn.close();
        }
        catch (Exception e)
        {
          LOGGER.trace("", e);
        }
      }
    }

    return new QueryResultsList(datasets);

  }

  /**
   * Returns a mapping of column names from schema to integer indices,
   * which are the column numbers for XRow(results)::getString().
   * If a column does not exist, its index is <= 0.
   */
  private Map<String, Integer> getColumnMapping(XResultSet results)
  {
    Map<String, Integer> mapColumnNameToIndex = new HashMap<>();
    XColumnLocate loc = UNO.XColumnLocate(results);
    Iterator<String> iter = getSchema().iterator();
    while (iter.hasNext())
    {
      String column = iter.next();
      int idx = -1;
      try
      {
        idx = loc.findColumn(column);
      }
      catch (SQLException x)
      {
        LOGGER.trace("", x);
      }
      mapColumnNameToIndex.put(column, Integer.valueOf(idx));
    }
    return mapColumnNameToIndex;
  }

  /**
   * Returns the 'correct' name of the lower() function depending on sqlSyntax.
   */
  private String sqlLower()
  {
    if (SQL_SYNTAX_MYSQL == sqlSyntax) {
      return "lcase";
    } else {
      return "lower";
    }
  }

  /**
   * Returns str as a string literal prepared for insertion into SQL statements.
   *
   * @param str
   *          The string literal returned by prepareForSqlInsert always begins and ends with a single apostrophe (').
   */
  private static String sqlLiteral(String str)
  {
    return "'" + str.replaceAll("'", "''") + "'";
  }

  /**
   * Returns str as an identifier name prepared for insertion into SQL statements.
   *
   * @param str
   *          begins and ends with a double quote.
   */
  private String sqlIdentifier(String str)
  {
    if (SQL_SYNTAX_PERVASIVESQL == sqlSyntax && str.contains( "." )) {
        // PervasiveSQL does not support 'DATABASE.TABLE', thus it is changed to 'TABLE'
        int dot = str.indexOf( '.' ) + 1;
        str = str.substring( dot );
    }
    return "\"" + str.replaceAll("\"", "\"\"") + "\"";
  }

  /**
   * Replace the * wildcard so that an SQL search pattern is created and escape characters that have meaning in SQL with '|'.
   */
  private static String sqlSearchPattern(String str)
  {
    return str.replaceAll("\\|", "||").replaceAll("_", "|_").replaceAll("%", "|%").replaceAll(
      "\\*", "%");
  }

  @Override
  public String getName()
  {
    return datasourceName;
  }

  private class OOoDataset implements Dataset
  {
    private Map<String, String> data;

    private String key;

    public OOoDataset(Map<String, String> data)
    {
      this.data = data;
      initKey(keyColumns);
    }

    /**
     * Assembles the key from the values of the key columns.
     *
     * @param keyCols
     *          The names of the key columns
     */
    private void initKey(String[] keyCols)
    {
      StringBuilder buffy = new StringBuilder();
      for (int i = 0; i < keyCols.length; ++i)
      {
        String str = data.get(keyCols[i]);
        if (str != null)
        {
          buffy.append(encode(keyCols[i]));
          buffy.append('#');
          buffy.append(encode(str));
          buffy.append('#');
        }
      }

      key = buffy.toString();
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column {0} does not exist!",
          columnName));
      return data.get(columnName);
    }

    @Override
    public String getKey()
    {
      return key;
    }

    private String encode(String str)
    {
      return str.replaceAll("%", "%%").replace("#", "%r");
    }
  }

  private static String decode(String str)
  {
    StringBuilder buffy = new StringBuilder(str);
    int i = 0;
    while (0 <= (i = buffy.indexOf("%", i)))
    {
      ++i;
      if (buffy.charAt(i) == 'r')
        buffy.replace(i - 1, i + 1, "#");
      else
        buffy.deleteCharAt(i);
    }
    return buffy.toString();
  }
}
