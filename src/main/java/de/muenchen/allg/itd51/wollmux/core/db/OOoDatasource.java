/*
 * Dateiname: OOoDatasource.java
 * Projekt  : WollMux
 * Funktion : Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 19.12.2006 | BNK | Erstellung
 * 21.12.2006 | BNK | Fertig+Test
 * 22.12.2006 | BNK | USER und PASSWORD unterstützt
 * 09.03.2007 | BNK | [P1257]Neuer Konstruktor, der Datenquelle auch ohne Angabe von Schlüssel erlaubt
 * 20.09.2007 | BNK | EscapeProcessing = false setzen in Abfragen, "|" als ESCAPE-Zeichen
 * 11.01.2011 | Ärztekammer Schleswig-Holstein (Michael Stramm) | Erweitert um die Unterstützung von PervasiveSQL
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.sdbc.XColumnLocate;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sdbc.XResultSet;
import com.sun.star.sdbc.XRow;
import com.sun.star.sdbc.XRowSet;
import com.sun.star.sdbcx.XColumnsSupplier;
import com.sun.star.sdbcx.XKeysSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung.
 */
public class OOoDatasource implements Datasource
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
   * o *** ANSI ********* <doublequote symbol> ::= 2 consecutive double quote
   * characters
   * 
   * (Lexical Elements, 135 Foundation) Ein delimited Identifier ist ein von "..."
   * umschlossener Identifier. Im Identifier enthaltene Doublequotes werden durch
   * <doublequote symbol> ersetzt. (Lexical Elements, 134 Foundation)
   * 
   * <quote symbol> ::= <quote><quote> (2 consecutive quote characters) (Lexical elements, 143 Foundation)
  In einem <character string literal> werden einzelne <quote>s durch <quote symbol> ersetzt.(Lexical elements, 143 Foundation)
  
   o**** Datensätze zu vorgegebener Schlüsselliste finden *********
   
  SELECT * FROM "<id>" WHERE ("<colId>"='<colVal>' AND "<colId>"='<colVal>' AND ...) OR (...) OR ...;

  In <id> und <colId> sind Doublequotes durch <doublequote symbol> ersetzt.
  
  In <colVal> sind Quotes durch <quote symbol> ersetzt.
  
  o ***** Datensätze finden, die bestimmte Kriterien erfüllen ********
  
  8.5 <like predicate> (385 Foundation)
  Der String hinter ESCAPE muss genau ein Zeichen lang sein. Ansonsten gibt es eine Exception (387 Foundation, 8.5 General Rules 3b))
  _ und % sowie das ESCAPE-Zeichen selbst müssen im String-Ausdruck hinter LIKE escapet werden (durch Voranstellen des Escape-Zeichens). Andere Zeichen dürfen nicht escapet werden.

  SELECT * FROM "<id>" WHERE (lower("<colId>") LIKE lower('<pattern>') ESCAPE '|') AND (...) AND ...;
  In <id> und <colId> sind Doublequotes durch <doublequote symbol> ersetzt.
  In <pattern> sind "_",  "%" und "|" ersetzt durch "|_", "|%" und "||".
  
   ***** Alle Datensätze auslesen ******

  SELECT * FROM "<id>";
  In <id> sind Doublequotes durch <doublequote symbol> ersetzt.

   **** Oracle *****
  Wie ANSI
   ****** MySQL *******
  Wie ANSI, aber mit lcase() statt lower()
   ****** PervasiveSQL *******
  Wie ANSI, aber rechts vom LIKE dürfen nur einfache Konstanten (also kein lower oder lcase)
  stehen. Außerdem wird "DATENBANK.TABELLE" nicht unterstützt. Nur "DATENBANK"."TABELLE",
  DATENBANK."TABELLE" oder "TABELLE".
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
  
  private static final String SQLSelectCommand = "SELECT * FROM ";

  /**
   * Passwort für den Login bei der Datenbank.
   */
  @SuppressWarnings("squid:S2068")
  private String password = "";

  /**
   * Wie {@link #OOoDatasource(Map, ConfigThingy, URL, boolean)}, wobei noKey==false
   * übergeben wird.
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
    datasourceName = parseConfig(sourceDesc, "NAME", () -> L.m("NAME der Datenquelle fehlt"));
    oooDatasourceName = parseConfig(sourceDesc, "SOURCE", () -> L.m(
        "Datenquelle \"%1\": Name der OOo-Datenquelle muss als SOURCE angegeben werden", datasourceName));
    oooTableName = parseConfig(sourceDesc, "TABLE", () -> L
        .m("Datenquelle \"%1\": Name der Tabelle/Sicht innerhalb der OOo-Datenquelle muss als TABLE angegeben werden", datasourceName));

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
        "SQL_SYNTAX \"%1\" nicht unterstützt", sqlSyntaxStr));

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
          "Datenquelle \"%1\": Schema-Abschnitt ist leer", datasourceName));
      ConfigThingy schluesselConf = sourceDesc.query("Schluessel");
      if (schluesselConf.count() == 0)
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\": Schluessel-Abschnitt fehlt", datasourceName));

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
      LOGGER.debug(L.m(
        "Schema der Datenquelle %1 nicht angegeben. Versuche, es von der Datenquelle zu erfragen.",
        datasourceName));
      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        ds.setLoginTimeout(LOGIN_TIMEOUT);
        XConnection conn = ds.getConnection(userName, password);

        /*
         * Laut IDL-Doku zu "View" müssen hier auch die Views enthalten sein.
         */
        XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
        XNameAccess columns = null;
        Object table = null;
        try
        {
          table = tables.getByName(oooTableName);
          columns = UNO.XColumnsSupplier(table).getColumns();
        }
        catch (Exception x)
        {
          LOGGER.debug(L.m(
            "Tabelle \"%1\" nicht gefunden. Versuche es als Namen einer Query zu nehmen",
              oooTableName), x);
          try
          {
            XNameAccess queries = UNO.XQueriesSupplier(conn).getQueries();
            table = queries.getByName(oooTableName);
            columns = UNO.XColumnsSupplier(table).getColumns();
          }
          catch (Exception y)
          {
            throw new ConfigurationErrorException(
              L.m(
                "Tabelle oder Abfrage \"%1\" existiert nicht in Datenquelle \"%2\" oder Fehler beim Bestimmen der Spalten ",
                oooTableName, oooDatasourceName), y);
          }
        }
        String[] colNames = columns.getElementNames();
        Arrays.asList(colNames).forEach(colName -> schema.add(colName));

        if (schema.isEmpty())
          throw new ConfigurationErrorException(L.m(
            "Datenquelle \"%1\": Tabelle \"%2\" hat keine Spalten", datasourceName,
            oooTableName));

        if (noKey)
        {
          if (colNames.length > 0) {
            keyColumns = new String[] { colNames[0] };
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
              columns = colSupp.getColumns();
              colNames = columns.getElementNames();
              keyColumns = new String[colNames.length];
              System.arraycopy(colNames, 0, keyColumns, 0, keyColumns.length);
            }
            catch (Exception x)
            {
              throw new ConfigurationErrorException(
                L.m(
                  "Datenquelle \"%1\": Keine Schluessel-Spalten definiert. Automatisches bestimmen der Schlüsselspalten nicht möglich",
                  datasourceName), x);
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
          "Konnte Schema der OOo-Datenquelle \"%1\" nicht auslesen.",
          oooDatasourceName), x);
      }

      if (keyColumns.length == 0)
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\": Keine Schluessel-Spalten definiert", datasourceName));
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
          "Schluessel-Spalte \"%1\" nicht im Schema enthalten", column));
      if (columns.contains(column))
        throw new ConfigurationErrorException(L.m(
          "Schluessel-Spalte \"%1\" ist im Schluessel-Abschnitt doppelt angegeben",
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
  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  { // TESTED
    if (keys.isEmpty()) {
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    long endTime = System.currentTimeMillis() + timeout;
    StringBuilder buffy =
      new StringBuilder(this.SQLSelectCommand + sqlIdentifier(oooTableName) + " WHERE ");

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

    timeout = endTime - System.currentTimeMillis();
    if (timeout < 1) {
      timeout = 1;
    }
    return sqlQuery(buffy.toString(), timeout, true);
  }

  @Override
  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  { // TESTED
    if (query.isEmpty()) {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    StringBuilder buffy =
      new StringBuilder(this.SQLSelectCommand + sqlIdentifier(oooTableName) + " WHERE ");

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
    return sqlQuery(buffy.toString(), timeout, true);
  }

  @Override
  public QueryResults getContents(long timeout) throws TimeoutException
  {
    String command = this.SQLSelectCommand + sqlIdentifier(oooTableName) + ";";
    return sqlQuery(command, timeout, false);
  }

  /**
   * Setzt die SQL-Anfrage query an die Datenbank ab und liefert die Resultate.
   * 
   * @param timeout
   *          maximale Zeit in Millisekunden, die die Anfrage dauern darf.
   * @param throwOnTimeout
   *          falls true wird im Falle des überschreitens des Timeouts eine
   *          TimeoutException geworfen, ansonsten wird die unvollständige
   *          Ergebnisliste zurückgeliefert.
   */
  private QueryResults sqlQuery(String query, long timeout, boolean throwOnTimeout)
      throws TimeoutException
  {
    LOGGER.debug("sqlQuery(\"{}, {}, {})", query, timeout, throwOnTimeout);
    long endTime = System.currentTimeMillis() + timeout;

    List<OOoDataset> datasets = new ArrayList<>();

    if (System.currentTimeMillis() > endTime)
    {
      if (throwOnTimeout)
        throw new TimeoutException(
          L.m("Konnte Anfrage nicht innerhalb der vorgegebenen Zeit vollenden"));
      else
        return new QueryResultsList(datasets);
    }

    XRowSet results = null;
    XConnection conn = null;
    try
    {

      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        long lgto = timeout / 1000;
        if (lgto < 1) {
          lgto = 1;
        }
        ds.setLoginTimeout((int) lgto);
        conn = ds.getConnection(userName, password);
      }
      catch (Exception x)
      {
        throw new TimeoutException(L.m(
          "Kann keine Verbindung zur Datenquelle \"%1\" herstellen",
          oooDatasourceName), x);
      }

      Object rowSet = UNO.createUNOService("com.sun.star.sdb.RowSet");
      results = UNO.XRowSet(rowSet);

      if (results == null)
        throw new NullPointerException(L.m("Konnte kein RowSet erzeugen"));

      XPropertySet xProp = UNO.XPropertySet(results);

      xProp.setPropertyValue("ActiveConnection", conn);

      /*
       * EscapeProcessing == false bedeutet, dass OOo die Query nicht selbst anfassen
       * darf, sondern direkt an die Datenbank weiterleiten soll. Wird dies verwendet
       * ist das Ergebnis (derzeit) immer read-only, da OOo keine Updates von
       * Statements durchführen kann, die es nicht geparst hat. Siehe Kommentar zu
       * http://qa.openoffice.org/issues/show_bug.cgi?id=78522 Entspricht dem Button
       * SQL mit grünem Haken (SQL-Kommando direkt ausführen) im Base-Abfrageentwurf.
       */
      xProp.setPropertyValue("EscapeProcessing", Boolean.FALSE);

      xProp.setPropertyValue("CommandType",
        Integer.valueOf(com.sun.star.sdb.CommandType.COMMAND));

      xProp.setPropertyValue("Command", query);

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
        if (System.currentTimeMillis() > endTime)
        {
          if (throwOnTimeout)
            throw new TimeoutException(
              L.m("Konnte Anfrage nicht innerhalb der vorgegebenen Zeit vollenden"));
          else
            break;
        }
      }

    }
    catch (Exception x)
    {
      throw new TimeoutException(L.m("Fehler beim Absetzen der Anfrage"), x);
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
      catch (Exception x)
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
    { // TESTED
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
        throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!",
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
  { // TESTED
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
