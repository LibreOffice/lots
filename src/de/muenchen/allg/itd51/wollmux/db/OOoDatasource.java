/*
 * Dateiname: OOoDatasource.java
 * Projekt  : WollMux
 * Funktion : Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OOoDatasource implements Datasource
{
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
  private Set<String> schema;

  /**
   * Die Namen der Spalten, die den Primärschlüssel bilden.
   */
  private String[] keyColumns;

  /**
   * Benutzername für den Login bei der Datenbank.
   */
  private String userName = "";

  /**
   * Passwort für den Login bei der Datenbank.
   */
  private String password = "";

  /**
   * Wie {@link #OOoDatasource(Map, ConfigThingy, URL, boolean)}, wobei noKey==false
   * übergeben wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public OOoDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context) throws ConfigurationErrorException
  {
    this(nameToDatasource, sourceDesc, context, false);
  }

  /**
   * Erzeugt eine neue OOoDatasource.
   * 
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser OOoDatasource
   *          bereits vollständig instanziierten Datenquellen (zur Zeit nicht
   *          verwendet).
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser OOoDatasource
   *          enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht
   *          verwendet).
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
      ConfigThingy sourceDesc, URL context, boolean noKey)
      throws ConfigurationErrorException
  {
    try
    {
      datasourceName = sourceDesc.get("NAME").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m("NAME der Datenquelle fehlt"));
    }

    try
    {
      oooDatasourceName = sourceDesc.get("SOURCE").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
        L.m(
          "Datenquelle \"%1\": Name der OOo-Datenquelle muss als SOURCE angegeben werden",
          datasourceName));
    }

    try
    {
      oooTableName = sourceDesc.get("TABLE").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
        L.m(
          "Datenquelle \"%1\": Name der Tabelle/Sicht innerhalb der OOo-Datenquelle muss als TABLE angegeben werden",
          datasourceName));
    }

    try
    {
      userName = sourceDesc.get("USER").toString();
    }
    catch (NodeNotFoundException x)
    {}

    try
    {
      password = sourceDesc.get("PASSWORD").toString();
    }
    catch (NodeNotFoundException x)
    {}

    try
    {
      String sqlSyntaxStr = sourceDesc.get("SQL_SYNTAX").toString();
      if (sqlSyntaxStr.equalsIgnoreCase("ansi"))
        sqlSyntax = SQL_SYNTAX_ANSI;
      else if (sqlSyntaxStr.equalsIgnoreCase("oracle"))
        sqlSyntax = SQL_SYNTAX_ORACLE;
      else if (sqlSyntaxStr.equalsIgnoreCase("mysql"))
        sqlSyntax = SQL_SYNTAX_MYSQL;
      else
        throw new ConfigurationErrorException(L.m(
          "SQL_SYNTAX \"%1\" nicht unterstützt", sqlSyntaxStr));
    }
    catch (NodeNotFoundException x)
    {}

    schema = new HashSet<String>();
    ConfigThingy schemaConf = sourceDesc.query("Schema");
    if (schemaConf.count() != 0)
    {
      Iterator<ConfigThingy> iter = schemaConf.iterator().next().iterator();
      String firstColumnName = null;
      while (iter.hasNext())
      {
        String columnName = iter.next().toString();
        if (firstColumnName == null) firstColumnName = columnName;
        schema.add(columnName);
      }
      if (schema.size() == 0)
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\": Schema-Abschnitt ist leer", datasourceName));
      ConfigThingy schluesselConf = sourceDesc.query("Schluessel");
      if (schluesselConf.count() == 0)
        throw new ConfigurationErrorException(L.m(
          "Datenquelle \"%1\": Schluessel-Abschnitt fehlt", datasourceName));

      if (noKey)
      {
        if (firstColumnName != null) keyColumns = new String[] { firstColumnName };
      }
      else
        parseKey(schluesselConf); // Test ob kein Schluessel vorhanden siehe weiter
      // unten
    }
    else
    {
      Logger.debug(L.m(
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
          Logger.debug(L.m(
            "Tabelle \"%1\" nicht gefunden. Versuche es als Namen einer Query zu nehmen",
            oooTableName));
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
        for (int i = 0; i < colNames.length; ++i)
          schema.add(colNames[i]);

        if (schema.size() == 0)
          throw new ConfigurationErrorException(L.m(
            "Datenquelle \"%1\": Tabelle \"%2\" hat keine Spalten", datasourceName,
            oooTableName));

        if (noKey)
        {
          if (colNames.length > 0) keyColumns = new String[] { colNames[0] };
        }
        else
        {
          ConfigThingy schluesselConf = sourceDesc.query("Schluessel");
          if (schluesselConf.count() != 0)
            parseKey(schluesselConf); // Test ob kein Schluessel vorhanden siehe
          // weiter unten
          else
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
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws ConfigurationErrorException
   *           falls eine Schluessel-Spalte nicht im {@link #schema} ist. Es wird
   *           *keine* Exception geworfen, wenn der Schluessel-Abschnitt leer ist.
   *           TESTED
   */
  private void parseKey(ConfigThingy conf) throws ConfigurationErrorException
  {
    conf = conf.iterator().next();
    Iterator<ConfigThingy> iter = conf.iterator();
    ArrayList<String> columns = new ArrayList<String>();
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

  public Set<String> getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  { // TESTED
    if (keys.isEmpty()) return new QueryResultsList(new ArrayList<Dataset>(0));

    long endTime = System.currentTimeMillis() + timeout;
    StringBuilder buffy =
      new StringBuilder("SELECT * FROM " + sqlIdentifier(oooTableName) + " WHERE ");

    Iterator<String> iter = keys.iterator();
    boolean first = true;
    while (iter.hasNext())
    {
      if (!first) buffy.append(" OR ");
      first = false;
      String key = iter.next();
      String[] parts = key.split("#", -1);
      buffy.append('(');
      for (int i = 1; i < parts.length; i += 2)
      {
        if (i > 1) buffy.append(" AND ");
        buffy.append(sqlIdentifier(decode(parts[i - 1])));
        buffy.append('=');
        buffy.append(sqlLiteral(decode(parts[i])));
      }
      buffy.append(')');
    }

    buffy.append(';');

    timeout = endTime - System.currentTimeMillis();
    if (timeout < 1) timeout = 1;
    return sqlQuery(buffy.toString(), timeout, true);
  }

  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  { // TESTED
    if (query.isEmpty()) return new QueryResultsList(new Vector<Dataset>(0));

    StringBuilder buffy =
      new StringBuilder("SELECT * FROM " + sqlIdentifier(oooTableName) + " WHERE ");

    Iterator<QueryPart> iter = query.iterator();
    boolean first = true;
    while (iter.hasNext())
    {
      QueryPart part = iter.next();
      if (!first) buffy.append(" AND ");
      first = false;
      buffy.append('(');
      buffy.append(sqlLower());
      buffy.append('(');
      buffy.append(sqlIdentifier(part.getColumnName()));
      buffy.append(')');
      buffy.append(" LIKE ");
      buffy.append(sqlLower());
      buffy.append('(');
      buffy.append(sqlLiteral(sqlSearchPattern(part.getSearchString())));
      buffy.append(") ESCAPE '|'");

      buffy.append(')');
    }

    buffy.append(';');
    return sqlQuery(buffy.toString(), timeout, true);
  }

  public QueryResults getContents(long timeout) throws TimeoutException
  {
    String command = "SELECT * FROM " + sqlIdentifier(oooTableName) + ";";
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
   * @author bettina.bauer, Matthias Benkmann
   */
  private QueryResults sqlQuery(String query, long timeout, boolean throwOnTimeout)
      throws TimeoutException
  {
    Logger.debug("sqlQuery(\"" + query + "\", " + timeout + ", " + throwOnTimeout
      + ")");
    long endTime = System.currentTimeMillis() + timeout;

    Vector<OOoDataset> datasets = new Vector<OOoDataset>();

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
        if (lgto < 1) lgto = 1;
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
        Map<String, String> data = new HashMap<String, String>();
        Iterator<Map.Entry<String, Integer>> iter =
          mapColumnNameToIndex.entrySet().iterator();
        while (iter.hasNext())
        {
          Map.Entry<String, Integer> entry = iter.next();
          String column = entry.getKey();
          int idx = ((Number) entry.getValue()).intValue();
          String value = null;
          if (idx > 0) value = row.getString(idx);
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
      if (results != null) UNO.XComponent(results).dispose();
      if (conn != null) try
      {
        conn.close();
      }
      catch (Exception e)
      {}
    }

    return new QueryResultsList(datasets);

  }

  /**
   * Liefert eine Abbildung der Spaltennamen aus {@link #schema} auf Integer-Indizes,
   * die die Spaltennummern für XRow(results)::getString() sind. Falls eine Spalte
   * nicht existiert, ist ihr index <= 0.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private Map<String, Integer> getColumnMapping(XResultSet results)
  {
    Map<String, Integer> mapColumnNameToIndex = new HashMap<String, Integer>();
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
      {}
      mapColumnNameToIndex.put(column, Integer.valueOf(idx));
    }
    return mapColumnNameToIndex;
  }

  /**
   * Liefert abhängig von {@link #sqlSyntax} den "richtigen" Namen der
   * lower()-Funktion.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String sqlLower()
  {
    switch (sqlSyntax)
    {
      case SQL_SYNTAX_MYSQL:
        return "lcase";
      default:
        return "lower";
    }
  }

  /**
   * Liefert str zurück, als String-Literal vorbereitet für das Einfügen in
   * SQL-Statements.
   * 
   * @param str
   *          beginnt und endet immer mit einem Apostroph.
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String sqlIdentifier(String str)
  {
    return "\"" + str.replaceAll("\"", "\"\"") + "\"";
  }

  /**
   * Ersetzt das * Wildcard so dass ein SQL-Suchmuster entsteht und escapet Zeichen,
   * die für SQL eine Bedeutung haben mit "|".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String sqlSearchPattern(String str)
  {
    return str.replaceAll("\\|", "||").replaceAll("_", "|_").replaceAll("%", "|%").replaceAll(
      "\\*", "%");
  }

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
     * @author Matthias Benkmann (D-III-ITD 5.1)
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

    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!",
          columnName));
      return data.get(columnName);
    }

    public String getKey()
    {
      return key;
    }

  }

  private static String encode(String str)
  {
    return str.replaceAll("%", "%%").replace("#", "%r");
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

  private static void printQueryResults(Set<String> schema, QueryResults res,
      Vector<String> keys) throws ColumnNotFoundException
  {
    keys.clear();
    Iterator<Dataset> iter;
    iter = res.iterator();
    while (iter.hasNext())
    {
      Dataset data = iter.next();
      keys.add(data.getKey());
      Iterator<String> colIter = schema.iterator();
      while (colIter.hasNext())
      {
        String col = colIter.next();
        String val = data.get(col);
        if (val == null)
          val = "unbelegt";
        else
          val = "\"" + val + "\"";

        System.out.print(col + "=" + val + " ");
      }
      System.out.println("  (Schlüssel: \"" + data.getKey() + "\")");
    }
  }

  /**
   * Gibt results aus.
   * 
   * @param query
   *          ein String der in die Überschrift der Ausgabe geschrieben wird, damit
   *          der Benutzer sieht, was er angezeigt bekommt.
   * @param schema
   *          bestimmt, welche Spalten angezeigt werden von den Datensätzen aus
   *          results.
   * @param results
   *          die Ergebnisse der Anfrage.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printResults(String query, Set<String> schema,
      QueryResults results)
  {
    System.out.println("Results for query \"" + query + "\":");
    Iterator<Dataset> resIter = results.iterator();
    while (resIter.hasNext())
    {
      Dataset result = resIter.next();

      Iterator<String> spiter = schema.iterator();
      while (spiter.hasNext())
      {
        String spalte = spiter.next();
        String wert = "Spalte " + spalte + " nicht gefunden!";
        try
        {
          wert = result.get(spalte);
          if (wert == null)
            wert = "unbelegt";
          else
            wert = "\"" + wert + "\"";
        }
        catch (ColumnNotFoundException x)
        {}
        ;
        System.out.print(spalte + "=" + wert + (spiter.hasNext() ? ", " : ""));
      }
      System.out.println();
    }
    System.out.println();
  }

  /**
   * 
   * @param spaltenName
   * @param suchString
   * @return
   * @throws TimeoutException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private QueryResults simpleFind(String spaltenName, String suchString)
      throws TimeoutException
  {
    List<QueryPart> query = new Vector<QueryPart>();
    query.add(new QueryPart(spaltenName, suchString));
    QueryResults find = find(query, 3000000);
    return find;
  }

  /**
   * 
   * @param spaltenName1
   * @param suchString1
   * @param spaltenName2
   * @param suchString2
   * @return
   * @throws TimeoutException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private QueryResults simpleFind(String spaltenName1, String suchString1,
      String spaltenName2, String suchString2) throws TimeoutException
  {
    List<QueryPart> query = new Vector<QueryPart>();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    QueryResults find = find(query, 3000000);
    return find;
  }

  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   */
  public static void main(String[] args)
  {
    try
    {
      UNO.init();
      Logger.init(System.err, Logger.ALL);
      // Datenquelle(
      // NAME "test"
      // TYPE "ooo"
      // SOURCE "datenbank"
      // TABLE "UserAnsicht"
      // Schema( "UserVorname" "UserNachname" "Beschreibung" )
      // Schluessel("UserVorname" "UserNachname")
      // # Wenn Schema()-Abschnitt angegeben ist, muss auch ein Schluessel-Abschnitt
      // angegeben werden.
      // )
      ConfigThingy conf = new ConfigThingy("Datenquelle");
      conf.add("NAME").add("test");
      conf.add("TYPE").add("ooo");
      conf.add("SOURCE").add("datenbank");
      conf.add("TABLE").add("UserAnsicht");
      ConfigThingy keysConf = conf.add("Schluessel");
      keysConf.add("UserVorname");
      keysConf.add("UserNachname");

      OOoDatasource ds = new OOoDatasource(null, conf, null);
      System.out.println("Name: " + ds.getName());
      System.out.print("Schema: ");
      Set<String> schema = ds.getSchema();
      Iterator<String> iter = schema.iterator();
      while (iter.hasNext())
      {
        System.out.print("\"" + iter.next() + "\" ");
      }
      System.out.println();
      System.out.print("Schlüsselspalten: ");
      for (int i = 0; i < ds.keyColumns.length; ++i)
        System.out.print("\"" + ds.keyColumns[i] + "\" ");

      System.out.println("Datensätze:");
      QueryResults res = ds.getContents(1000000);
      Vector<String> keys = new Vector<String>();
      printQueryResults(schema, res, keys);

      keys.remove(0);
      System.out.println("Rufe Datensätze für folgende Schlüssel ab:");
      iter = keys.iterator();
      while (iter.hasNext())
        System.out.println("    " + iter.next());

      res = ds.getDatasetsByKey(keys, 10000000);
      printQueryResults(schema, res, keys);

      printResults("Beschreibung = *uTTer", schema, ds.simpleFind("Beschreibung",
        "*uTTer"));
      printResults("Beschreibung = *uTTer, UserVorname = Sina", schema,
        ds.simpleFind("Beschreibung", "*uTTer", "UserVorname", "Sina"));
      printResults("UserVorname = Hans, UserNachname = Mu%rster#rmann", schema,
        ds.simpleFind("UserVorname", "Hans", "UserNachname", "Mu%rster#rmann"));
      printResults("Beschreibung = \\Kind", schema, ds.simpleFind("Beschreibung",
        "\\Kind"));
      printResults(
        "UserVorname = Hans, UserNachname = Mu%er#rmann (sic)  muss leer sein",
        schema, ds.simpleFind("UserVorname", "Hans", "UserNachname", "Mu%er#rmann"));
      printResults("UserVorname = *a*", schema, ds.simpleFind("UserVorname", "*a*"));

    }
    catch (Exception x)
    {
      x.printStackTrace();
    }
    System.exit(0);
  }

}
