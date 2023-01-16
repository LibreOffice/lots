/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoProperty;

/**
 * Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung.
 */
public class OOoDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OOoDatasource.class);

  /**
   * Maximale Zeit in Sekunden, die die Datenquelle für die Verbindungsaufnahme mit
   * der Datenbank brauchen darf.
   */
  private static final int LOGIN_TIMEOUT = 5;

  /**
   * Konstante für {@link #sqlSyntax}, die angibt, dass SQL Queries in Oracle-Syntax
   * abgesetzt werden sollen.
   */
  private static final int SQL_SYNTAX_ANSI = 0;

  /**
   * Konstante für {@link #sqlSyntax}, die angibt, dass SQL Queries in Oracle-Syntax
   * abgesetzt werden sollen.
   */
  private static final int SQL_SYNTAX_ORACLE = 1;

  /**
   * Konstante für {@link #sqlSyntax}, die angibt, dass SQL Queries in MySQL-Syntax
   * abgesetzt werden sollen.
   */
  private static final int SQL_SYNTAX_MYSQL = 2;

  /**
   * Konstante für {@link #sqlSyntax}, die angibt, dass SQL Queries in PervasiveSQL-Syntax
   * abgesetzt werden sollen.
   */
  private static final int SQL_SYNTAX_PERVASIVESQL = 3;

  /**
   * Welche Syntax soll verwendet werden.
   *
   * Default ist Oracle-Syntax.
   *
   * o *** ANSI ********* <doublequote symbol> ::= 2 consecutive double quote characters
   *
   * (Lexical Elements, 135 Foundation) Ein delimited Identifier ist ein von "..." umschlossener
   * Identifier. Im Identifier enthaltene Doublequotes werden durch <doublequote symbol> ersetzt.
   * (Lexical Elements, 134 Foundation)
   *
   * <quote symbol> ::= <quote><quote> (2 consecutive quote characters) (Lexical elements, 143
   * Foundation) In einem <character string literal> werden einzelne <quote>s durch <quote symbol>
   * ersetzt. (Lexical elements, 143 Foundation)
   *
   * o**** Datensätze zu vorgegebener Schlüsselliste finden *********
   *
   * SELECT * FROM "<id>" WHERE ("<colId>"='<colVal>' AND "<colId>"='<colVal>' AND ...) OR (...) OR
   * ...;
   *
   * In <id> und <colId> sind Doublequotes durch <doublequote symbol> ersetzt.
   *
   * In <colVal> sind Quotes durch <quote symbol> ersetzt.
   *
   * o ***** Datensätze finden, die bestimmte Kriterien erfüllen ********
   *
   * 8.5 <like predicate> (385 Foundation) Der String hinter ESCAPE muss genau ein Zeichen lang
   * sein. Ansonsten gibt es eine Exception (387 Foundation, 8.5 General Rules 3b)) _ und % sowie
   * das ESCAPE-Zeichen selbst müssen im String-Ausdruck hinter LIKE escapet werden (durch
   * Voranstellen des Escape-Zeichens). Andere Zeichen dürfen nicht escapet werden.
   *
   * SELECT * FROM "<id>" WHERE (lower("<colId>") LIKE lower('<pattern>') ESCAPE '|') AND (...) AND
   * ...; In <id> und <colId> sind Doublequotes durch <doublequote symbol> ersetzt. In <pattern>
   * sind "_", "%" und "|" ersetzt durch "|_", "|%" und "||".
   *****
   * Alle Datensätze auslesen ******
   *
   * SELECT * FROM "<id>"; In <id> sind Doublequotes durch <doublequote symbol> ersetzt.
   ****
   * Oracle ***** Wie ANSI MySQL ******* Wie ANSI, aber mit lcase() statt lower() PervasiveSQL
   * ******* Wie ANSI, aber rechts vom LIKE dürfen nur einfache Konstanten (also kein lower oder
   * lcase) stehen. Außerdem wird "DATENBANK.TABELLE" nicht unterstützt. Nur "DATENBANK"."TABELLE",
   * DATENBANK."TABELLE" oder "TABELLE".
   */
  private int sqlSyntax = SQL_SYNTAX_ANSI;

  /**
   * Der Name dieser Datenquelle.
   */
  private String datasourceName;

  /**
   * Der Name der OpenOffice-Datenquelle.
   */
  private String oooDatasourceName;

  /**
   * Der Name der Tabelle in der OpenOffice-Datenquelle.
   */
  private String oooTableName;

  /**
   * Das Schema dieser Datenquelle.
   */
  private List<String> schema;

  /**
   * Die Namen der Spalten, die den Primärschlüssel bilden.
   */
  private String[] keyColumns;

  /**
   * Benutzername für den Login bei der Datenbank.
   */
  private String userName = "";

  private static final String SQL_SELECT_COMMAND = "SELECT * FROM ";

  /**
   * Passwort für den Login bei der Datenbank.
   */
  @SuppressWarnings("squid:S2068")
  private String password = "";

  /**
   * Wie {@link #OOoDatasource(Map, ConfigThingy, boolean)}, wobei noKey==false übergeben wird.
   */
  public OOoDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc)
  {
    this(nameToDatasource, sourceDesc, false);
  }

  /**
   * Erzeugt eine neue OOoDatasource. Wenn kein SQL_SYNTAX Parameter in ConfigThingy
   * gesetzt ist, wird 'mysql' als Standard verwendet.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser OOoDatasource
   *          bereits vollständig instanziierten Datenquellen (zur Zeit nicht
   *          verwendet).
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser OOoDatasource
   *          enthält.
   * @param noKey
   *          Falls true, so wird immer die erste Spalte als Schlüsselspalte
   *          verwendet. Diese Option sollte nur verwendet werden, wenn keine
   *          Operationen getätigt werden sollen, die den Schlüssel verwenden.
   * @throws ConfigurationErrorException
   *           falls in der Definition in sourceDesc ein Fehler ist. Falls sourceDesc
   *           keinen Schema-Unterabschnitt aufweist, wird versucht, das Schema von
   *           der Datenquelle selbst zu bekommen. Tritt dabei ein Fehler auf wird
   *           ebenfalls diese Exception geworfen. *Keine* Exception wird geworfen,
   *           wenn die Spalten des Schema-Abschnitts nicht in der realen Datenbank
   *           vorhanden sind. In diesem Fall werden die entsprechenden Spalten als
   *           leer behandelt. TESTED
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
        parseKey(schluesselConf); // Test ob kein Schluessel vorhanden siehe weiter
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
         * Laut IDL-Doku zu "View" müssen hier auch die Views enthalten sein.
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
            // Test ob kein Schluessel vorhanden siehe weiter unten
            parseKey(schluesselConf);
          } else
          { // Schlüssel von Datenbank abfragen.
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
            // Test ob kein Schluessel vorhanden siehe weiter unten
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
   * Parst das erste Kind von conf (das existieren und ein Schluessel-Knoten sein
   * muss) und setzt {@link #keyColumns} entsprechend.
   * @throws ConfigurationErrorException
   *           falls eine Schluessel-Spalte nicht im {@link #schema} ist. Es wird
   *           *keine* Exception geworfen, wenn der Schluessel-Abschnitt leer ist.
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
          // Rechts vom LIKE können nur einfache Konstanten und keine Funktionen wie
          // lcase oder lower genutzt werden. Daher wird hier über die Java Methode
          // toLowerCase der zu suchende String in Kleinbuchstaben umgewandelt.
          // Die Inhalte der zu durchsuchenden Spalte können wiederum mit lcase/lower
          // behandelt werden. Somit ist sichergestellt, dass der durchsuchende und der zu
          // suchende String nur Kleinbuchstaben enthält.
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
   * Setzt die SQL-Anfrage query an die Datenbank ab und liefert die Resultate.
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
       * EscapeProcessing == false bedeutet, dass OOo die Query nicht selbst anfassen
       * darf, sondern direkt an die Datenbank weiterleiten soll. Wird dies verwendet
       * ist das Ergebnis (derzeit) immer read-only, da OOo keine Updates von
       * Statements durchführen kann, die es nicht geparst hat. Siehe Kommentar zu
       * http://qa.openoffice.org/issues/show_bug.cgi?id=78522 Entspricht dem Button
       * SQL mit grünem Haken (SQL-Kommando direkt ausführen) im Base-Abfrageentwurf.
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
   * Liefert eine Abbildung der Spaltennamen aus {@link #schema} auf Integer-Indizes,
   * die die Spaltennummern für XRow(results)::getString() sind. Falls eine Spalte
   * nicht existiert, ist ihr index <= 0.
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
   * Liefert abhängig von {@link #sqlSyntax} den "richtigen" Namen der
   * lower()-Funktion.
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
   * Liefert str zurück, als String-Literal vorbereitet für das Einfügen in
   * SQL-Statements.
   *
   * @param str
   *          beginnt und endet immer mit einem Apostroph.
   */
  private static String sqlLiteral(String str)
  {
    return "'" + str.replaceAll("'", "''") + "'";
  }

  /**
   * Liefert str zurück, als Identifier-Name vorbereitet für das Einfügen in
   * SQL-Statements.
   *
   * @param str
   *          beginnt und endet immer mit einem Doublequote.
   */
  private String sqlIdentifier(String str)
  {
    if (SQL_SYNTAX_PERVASIVESQL == sqlSyntax && str.contains( "." )) {
        // PervasiveSQL unterstützt "DATENBANK.TABELLE" nicht, wird somit in "TABELLE" geändert
        int dot = str.indexOf( '.' ) + 1;
        str = str.substring( dot );
    }
    return "\"" + str.replaceAll("\"", "\"\"") + "\"";
  }

  /**
   * Ersetzt das * Wildcard so dass ein SQL-Suchmuster entsteht und escapet Zeichen,
   * die für SQL eine Bedeutung haben mit "|".
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
     * Setzt aus den Werten der Schlüsselspalten den Schlüssel zusammen.
     *
     * @param keyCols
     *          die Namen der Schlüsselspalten
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
