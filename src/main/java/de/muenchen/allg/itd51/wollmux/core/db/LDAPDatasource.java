/*
 * Dateiname: LDAPDatasource.java
 * Projekt  : WollMux
 * Funktion : Verschafft Zugriff auf LDAP-Verzeichnisdienst als Datasource.
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
 * 03.11.2005 | BNK | Erstellung
 * 16.11.2005 | BNK | Ctrl-Shift-F
 *                  | besser kommentiert
 * 24.11.2005 | BNK | escapen von Werten in LDAP-Suchanfragen
 * 28.11.2005 | BNK | mehr testing und fixing und optimizing
 * 30.11.2005 | BNK | mehr testing und bugfixing
 * 02.12.2005 | BNK | Schlüssel umgestellt und dadurch robuster und 
 *                  | effizienter gemacht 
 * 05.12.2005 | BNK | Schlüssel mit RE überprüfen und ignorieren falls kein Match
 * 16.02.2006 | BNK | mehr Debug-Output
 * 16.02.2006 | BNK | Timeout auch bei new InitialLdapContext()
 * 20.02.2006 | BNK | setTimeout() Funktion
 * 10.05.2006 | BNK | bessere Debug-Meldungen
 * -------------------------------------------------------------------
 *
 * @author Max Meier (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Datasource für Zugriff auf ein LDAP-Verzeichnis
 * 
 * @author Max Meier (D-III-ITD 5.1)
 */
public class LDAPDatasource implements Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(LDAPDatasource.class);

  private List<String> schema;

  private String datasourceName;

  private String url;

  private String baseDN;

  private String objectClass;

  /** properties für die Verbindung zum LDAP-Server */
  private Properties properties = new Properties();

  /** Separator zur Schluesselerzeugung aus mehreren Schluesselwerten */
  private static final String SEPARATOR = "&:=&:%";

  /**
   * Trennt den ersten Teil des Schlüssels, der den Pfaden mit Level 0 entspricht vom
   * Rest des Schlüssels
   */
  private static final String KEY_SEPARATOR_0_NON_0_RE = "==%§%==";

  /** Map von query-Strings auf LDAP-Attributnamen */
  private Map<String, ColumnDefinition> columnDefinitions =
    new HashMap<>();

  /** Key-Attribute (LDAP) (Strings). */
  private List<Object> keyAttributes = new ArrayList<>();

  /**
   * Was für Arten von Pfaden kommen als Schlüsselspalten vor (ABSOLUTE_ONLY,
   * RELATIVE_ONLY, ABSOLUTE_AND_RELATIVE).
   */
  private int keyStatus; // 0:= nur absolute Attribute, 1:= absolute und

  // relative Attribute, 2:= nur relative Attribute
  /** nur Attributpfade der Form 0:*. */
  private static final int ABSOLUTE_ONLY = 0;

  /** nur Attributpfade der Form num:* wobei num nicht 0 ist. */
  private static final int RELATIVE_ONLY = 2;

  /** Sowohl Attributpfade der Form 0:* als auch der Form num:* mit num ungleich 0. */
  private static final int ABSOLUTE_AND_RELATIVE = 1;

  /** regex für erlaubte Bezeichner */
  private static final Pattern SPALTENNAME =
    Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  /**
   * Regex zum Checken der Syntax von BASE_DN. TOD0: Sicher zu restriktiv!
   */
  private static final Pattern BASEDN_RE =
    Pattern.compile("^[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+(,[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+)*$");

  /**
   * Regex zum Checken der Syntax von LDAP-Attributsbezeichnern. TOD0: Sicher zu
   * restriktiv!
   */
  private static final Pattern ATTRIBUTE_RE = Pattern.compile("^[a-zA-Z]+$");

  /**
   * Regex zum Checken, ob ein Schlüssel für die LDAP-Datasource legal ist.
   */
  private static final Pattern KEY_RE =
    Pattern.compile("^(\\(&(\\([^()=]+[^()]*\\))+\\))?" + KEY_SEPARATOR_0_NON_0_RE
      + "([a-zA-Z_][a-zA-Z0-9_]*=.*" + SEPARATOR + ")?$");

  /**
   * temporärer cache für relative Attribute (wird bei jeder neuen Suche neu
   * angelegt)
   */
  private Map<CacheKey, Attributes> attributeCache =
    new HashMap<>();

  /**
   * Erzeugt eine neue LDAPDatasource.
   * 
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser LDAPDatasource
   *          bereits vollständig instanziierten Datenquellen (zur Zeit nicht
   *          verwendet).
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser LDAPDatasource
   *          enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht
   *          verwendet).
   * @throws ConfigurationErrorException
   *           falls in der Definition in sourceDesc ein Fehler ist.
   */
  @SuppressWarnings("squid:S2068")
  public LDAPDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc, URL context)
  {
    datasourceName = parseConfig(sourceDesc, "NAME", () -> L.m("NAME der Datenquelle fehlt"));
    url = parseConfig(sourceDesc, "URL", () -> errorMessage() + L.m("URL des LDAP-Servers fehlt."));
    try
    {
      new URI(url);
    }
    catch (URISyntaxException e)
    {
      throw new ConfigurationErrorException(L.m("Fehler in LDAP-URL \"%1\"", url), e);
    }

    baseDN = parseConfig(sourceDesc, "BASE_DN", () -> errorMessage() + L.m("BASE_DN des LDAP-Servers fehlt."));
    if (!BASEDN_RE.matcher(baseDN).matches())
    {
      throw new ConfigurationErrorException(L.m("BASE_DN-Wert ist ungültig: \"%1\"", baseDN));
    }

    objectClass = parseConfig(sourceDesc, "OBJECT_CLASS", () -> errorMessage() + L.m("Keine OBJECT_CLASS definiert."));
    if (!ATTRIBUTE_RE.matcher(objectClass).matches())
    {
      throw new ConfigurationErrorException(L.m("OBJECT_CLASS enthält unerlaubte Zeichen: \"%1\"", objectClass));
    }

    String user = "";
    String password = "";
    try
    {
      user = sourceDesc.get("USER").toString();
      password = sourceDesc.get("PASSWORD").toString();
    } catch (NodeNotFoundException e)
    {
      LOGGER.debug(L.m("Username oder Passwort für den LDAP-Server fehlt."), e);
    }

    // set properties
    properties.put(Context.INITIAL_CONTEXT_FACTORY,
      "com.sun.jndi.ldap.LdapCtxFactory");
    properties.put(Context.PROVIDER_URL, url);

    if (!user.isEmpty() && !password.isEmpty())
    {
      properties.put(Context.SECURITY_PRINCIPAL, user);
      properties.put(Context.SECURITY_CREDENTIALS, password);
    }

    ConfigThingy spalten = sourceDesc.query("Spalten");

    if (spalten.count() == 0)
      throw new ConfigurationErrorException(errorMessage()
        + L.m("Abschnitt 'Spalten' fehlt."));

    schema = new ArrayList<>();

    // iteriere über alle Spalten-Relationen
    for (ConfigThingy spaltenDesc : spalten)
    {
      // iteriere über eine Spalten-Relation
      for (ConfigThingy spalteDesc : spaltenDesc)
      {
        String spalte = parseConfig(spalteDesc, "DB_SPALTE", () -> errorMessage() + L.m("DB_SPALTE Angabe fehlt"));
        if (!SPALTENNAME.matcher(spalte).matches())
        {
          throw new ConfigurationErrorException(errorMessage()
              + L.m("Spalte \"%1\" entspricht nicht der Syntax eines Bezeichners", spalte));
        }

        String path = parseConfig(spalteDesc, "PATH", () -> L.m("PATH-Angabe fehlt für Spalte %1", spalte));
        int relativePath;
        String attributeName;
        String columnObjectClass = null;
        String lineSeparator = null;

        // get relativePath and attributeName
        String[] splitted = path.split(":");

        if (splitted.length != 2)
        {
          throw new ConfigurationErrorException(errorMessage() + L.m("Syntaxerror bei Pfadangabe von %1", spalte));
        }

        try
        {
          relativePath = Integer.parseInt(splitted[0]);
        }
        catch (NumberFormatException e)
        {
          throw new ConfigurationErrorException(errorMessage()
            + L.m("Syntaxerror bei Angabe des relativen Pfads von %1", spalte));
        }

        attributeName = splitted[1];
        if (!ATTRIBUTE_RE.matcher(attributeName).matches())
          throw new ConfigurationErrorException(L.m(
            "Illegaler Attributsbezeichner: \"%1\"", attributeName));

        columnObjectClass = spalteDesc.getString("OBJECT_CLASS");
        lineSeparator = spalteDesc.getString("LINE_SEPARATOR");

        ColumnDefinition columnAttr =
          new ColumnDefinition(spalte, relativePath, attributeName);
        columnAttr.columnObjectClass = columnObjectClass;
        columnAttr.lineSeparator = lineSeparator;
        columnDefinitions.put(spalte, columnAttr);
        schema.add(spalte);
      }
    }

    // Key-Attribute
    ConfigThingy keys = sourceDesc.query("Schluessel");

    if (keys.count() == 0)
      throw new ConfigurationErrorException(errorMessage() + L.m("Schluessel-Abschnitt fehlt."));

    ConfigThingy keySpalten;

    try
    {
      // der letzte definierte Schluessel wird verwendet
      keySpalten = keys.getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(L.m("Unmöglich. Ich hab doch vorher count() überprüft."), e);
    }

    Iterator<ConfigThingy> keyIterator = keySpalten.iterator();

    if (!keyIterator.hasNext())
    {
      throw new ConfigurationErrorException(errorMessage()
        + L.m("Keine Schluesselspalten angegeben."));
    }

    boolean onlyRelative = true; // true, falls kein Attributpfad der Form 0:*
    boolean onlyAbsolute = true; // true, falls nur Attributspfade der Form 0:*

    // speichere die Schluesselattribute
    while (keyIterator.hasNext())
    {
      String currentName = keyIterator.next().toString();

      ColumnDefinition currentKeyLDAPAttribute = columnDefinitions.get(currentName);

      // ist Schluesselattribut vorhanden?
      if (currentKeyLDAPAttribute == null)
        throw new ConfigurationErrorException(
          L.m(
            "Spalte \"%1\" ist nicht im Schema definiert und kann deshalb nicht als Schluesselspalte verwendet werden.",
            currentName));

      if (currentKeyLDAPAttribute.relativePath != 0)
      {
        onlyAbsolute = false;
      }
      if (currentKeyLDAPAttribute.relativePath == 0)
      {
        onlyRelative = false;
      }

      keyAttributes.add(currentName);
    }

    if (onlyAbsolute)
    {
      keyStatus = ABSOLUTE_ONLY;
    }
    else if (onlyRelative)
    {
      keyStatus = RELATIVE_ONLY;
    }
    else
    {
      keyStatus = ABSOLUTE_AND_RELATIVE;
    }
  }

  /** Setzt die timeout-Properties. */
  private void setTimeout(long timeout)
  {
    properties.setProperty("com.sun.jndi.ldap.connect.timeout", Long.toString(timeout));
    properties.setProperty("com.sun.jndi.dns.timeout.initial", Long.toString(timeout));
    properties.setProperty("com.sun.jndi.dns.timeout.retries", "1");
  }

  /**
   * repräsentiert eine Spaltendefinition
   * 
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class ColumnDefinition
  {

    /**
     * relativer Pfad: 0 := Attribut befindet sich im selben Knoten negativ :=
     * relative Pfadangabe "nach oben" vom aktuellen Knoten aus positiv := relative
     * Pfadangabe von der Wurzel aus
     */
    int relativePath;

    /**
     * Name der Spalte.
     */
    String columnName;

    /** Attributname im LDAP */
    String attributeName = null;

    /** exklusive objectClass */
    String columnObjectClass = null;

    /** line separator */
    String lineSeparator;

    ColumnDefinition(String columnName, int relativePath, String attributeName)
    {
      this.columnName = columnName;
      this.relativePath = relativePath;
      this.attributeName = attributeName;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /**
   * 
   * @param key
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<QueryPart> keyToFindQuery(String key)
  {
    String[] ks = key.split(KEY_SEPARATOR_0_NON_0_RE, 2);
    ks = ks[1].split(SEPARATOR);

    List<QueryPart> query = new ArrayList<>(ks.length);

    for (int i = 0; i < ks.length; ++i)
    {
      String[] q = ks[i].split("=", 2);
      query.add(new QueryPart(q[0], q[1]));
    }

    return query;
  }

  @Override
  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection
   * , long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  {
    if (keys.isEmpty()) {
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    List<Dataset> results = new ArrayList<>(keys.size());

    try
    {
      long endTime = System.currentTimeMillis() + timeout;

      attributeCache.clear();

      if (keyStatus == ABSOLUTE_ONLY || keyStatus == ABSOLUTE_AND_RELATIVE)
      { // absolute Attribute vorhanden
        results.addAll(handleAbsoluteKeys(keys, endTime));
      }
      else
      { // nur relative Attribute
        for (String currentKey : keys)
        {
          List<QueryPart> query = keyToFindQuery(currentKey);
          timeout = endTime - System.currentTimeMillis();
          if (timeout <= 0) {
            throw new TimeoutException();
          }
          QueryResults res = find(query, timeout);
          for (Dataset ds : res)
            results.add(ds);
        }
      }

      return new QueryResultsList(results);
    }
    finally
    {
      attributeCache.clear();
    }
  }

  private List<Dataset> handleAbsoluteKeys(Collection<String> keys, long endTime) throws TimeoutException
  {
    List<Dataset> results = new ArrayList<>();
    // build searchFilter
    StringBuilder searchFilter = new StringBuilder();

    for (String currentKey : keys)
    {
      if (!KEY_RE.matcher(currentKey).matches()) {
        continue;
      }
      String[] ks = currentKey.split(KEY_SEPARATOR_0_NON_0_RE, 2);
      searchFilter.append(ks[0]);
    }
    if (searchFilter.length() == 0)
    {
      return results;
    }
    searchFilter.insert(0, "(|");
    searchFilter.append(")");

    // search LDAP
    NamingEnumeration<SearchResult> currentResults =
      searchLDAP("", searchFilter.toString(), SearchControls.SUBTREE_SCOPE,
        true, endTime);

    while (currentResults.hasMoreElements())
    {
      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException();
      }
      try
      {
        SearchResult currentResult = currentResults.next();
        Dataset dataset = getDataset(currentResult, endTime);
        if (keyStatus == ABSOLUTE_ONLY || keys.contains(dataset.getKey()))
        {
          results.add(dataset);
        }
      }
      catch (NamingException e)
      {
        LOGGER.error(L.m("Error in LDAP-Directory."), e);
      }
    }
    return results;
  }

  /**
   * Speichert eine Liste von (Ldap)Names, die zu einer Suchanfrage über Attribute
   * mit Pfad-Level ungleich 0 gehören (alle mit dem selben Level).
   * 
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class RelativePaths
  {

    public int relative;

    public List<Name> paths; // of (Ldap)Names

    RelativePaths(int relative, List<Name> paths)
    {
      this.relative = relative;
      this.paths = paths;
    }

  }

  /**
   * Ein (Ldap)Name und der Pfad-Level für die Suche. Typischerweise ist der
   * Pfad-Level != 0, aber ein Level von 0 ist auch möglich (kann bei Bildung der
   * Schnittmenge von positiven und negativen Kandidaten entstehen).
   * 
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class RelativePath
  {

    public int relative;

    public Name name;

    RelativePath(int relative, Name name)
    {
      this.relative = relative;
      this.name = name;
    }

  }

  /**
   * Liefert zum Pfadlevel pathLength alle Knoten, die auf die LDAP-Suchanfrage
   * filter passen
   * 
   * @throws TimeoutException
   *           falls die Suche nicht vor endTime beendet werden konnte.
   * @author Max Meier (D-III-ITD 5.1)
   */
  private RelativePaths getPaths(String filter, int pathLength, long endTime)
      throws TimeoutException
  {

    List<Name> paths;

    long timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) {
      throw new TimeoutException();
    }
    if (timeout > Integer.MAX_VALUE) {
      timeout = Integer.MAX_VALUE;
    }

    try
    {
      setTimeout(timeout);
      LOGGER.trace("new InitialLdapContext(properties, null)");
      DirContext ctx = new InitialLdapContext(properties, null);

      LOGGER.trace("ctx.getNameParser(\"\")");
      NameParser np = ctx.getNameParser("");
      int rootSize = np.parse(baseDN).size();
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

      sc.setTimeLimit((int) timeout);

      LOGGER.trace("ctx.search({}, {}, sc) mit Zeitlimit {}", baseDN, filter, sc.getTimeLimit());
      NamingEnumeration<SearchResult> enumer = ctx.search(baseDN, filter, sc);
      LOGGER.trace("ctx.search() abgeschlossen");

      paths = new Vector<>();

      while (enumer.hasMoreElements())
      {
        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }
        SearchResult result = enumer.nextElement();
        String path = preparePath(result.getNameInNamespace());
        Name pathName = np.parse(path);
        /*
         * ACHTUNG: hier kann NICHT (pathLength < 0 && (pathName.size()+rootLength >
         * abs(pathLength))) getestet werden, denn Minus-Bedingungen betreffen die
         * Nachfahren, hier muesste also die Tiefe des tiefsten Nachfahrens
         * ausgewertet werden, die wir nicht kennen.
         */
        if (pathName.size() + rootSize == pathLength || pathLength < 0)
          paths.add(pathName);
      }

    }
    catch (NamingException e)
    {
      throw new TimeoutException(L.m("Internal error in LDAP."), e);
    }

    return new RelativePaths(pathLength, paths);

  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  @Override
  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  {

    long endTime = System.currentTimeMillis() + timeout;

    StringBuilder searchFilter = new StringBuilder();
    List<RelativePaths> positiveSubtreePathLists = new ArrayList<>();

    List<List<RelativePath>> negativeSubtreePathLists = new ArrayList<>();

    Map<Integer, String> mapNon0PathLevelToSearchFilter = new HashMap<>();

    boolean first = true;
    for (QueryPart currentQuery : query)
    {

      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException();
      }

      ColumnDefinition colDef = columnDefinitions.get(currentQuery.getColumnName());

      if (colDef == null) {
        return new QueryResultsList(new Vector<Dataset>(0));
      }

      String attributeName = colDef.attributeName;
      int relativePath = colDef.relativePath;
      String attributeValue = currentQuery.getSearchString();

      if (attributeValue.isEmpty())
      {
        continue;
      }

      String columnObjectClass = colDef.columnObjectClass;
      String currentSearchFilter =
          "(" + ldapEscape(attributeName) + "=" + ldapEscape(attributeValue)
              + ")";
      if (columnObjectClass != null)
      {
        currentSearchFilter =
          "(&" + currentSearchFilter + "(objectClass=" + ldapEscape(columnObjectClass)
            + "))";
      }

      if (relativePath == 0)
      { // edit filter
        if (first)
        {
          searchFilter.append(currentSearchFilter);
          first = false;
        }
        else
        {
          searchFilter.insert(0, "(&" + currentSearchFilter).append(")");
        }
      }
      else
      { // edit searchFilters for subtree searches:

        Integer key = Integer.valueOf(relativePath);

        String non0LevelSearchFilter = mapNon0PathLevelToSearchFilter.get(key);

        if (non0LevelSearchFilter == null)
        {
          non0LevelSearchFilter = currentSearchFilter;
        }
        else
        {
          non0LevelSearchFilter =
            "(&" + currentSearchFilter + non0LevelSearchFilter + ")";
        }

        mapNon0PathLevelToSearchFilter.put(key, non0LevelSearchFilter);

      }

    }

    // TOD0 evtl. Optimierung: attributeKeys nicht in zufälliger Reihenfolge
    // durchgehen
    for (Map.Entry<Integer, String> ent : mapNon0PathLevelToSearchFilter.entrySet())
    {
      Integer currentKey = ent.getKey();

      int relativePath = currentKey.intValue();

      String pathFilter = ent.getValue();

      RelativePaths paths = getPaths(pathFilter, relativePath, endTime);

      if (relativePath > 0)
      {
        positiveSubtreePathLists.add(paths);
      }
      else
      {
        /*
         * RelativePaths in Liste von RelativePath Objekten umwandeln, da nachher im
         * Merge-Schritt eine Liste entstehen soll, in der Pfade verschiedener Stufe
         * gemischt enthalten sein können.
         */
        List<RelativePath> negativeSubtreePaths = new ArrayList<>();
        for (Name currentName : paths.paths)
        {
          RelativePath newNegativePath =
            new RelativePath(paths.relative, currentName);
          negativeSubtreePaths.add(newNegativePath);
        }
        negativeSubtreePathLists.add(negativeSubtreePaths);
      }
    }

    /*
     * bilde die Schnittmenge aller angegebenen positiv relativen Pfade
     * 
     * Dieser Algorithmus vergleicht zwei Listen, die jeweils alle relevanten Pfade
     * eines Suchattributs repräsentieren. Zuerst werden die Listen nach der Länge
     * der enthaltenen Pfade sortiert, danach wird betrachte, ob für jedes Element
     * der Liste der längeren Pfade ein Element aus der Liste der kürzeren Pfade
     * existiert, das ein Prefix des ersten Elements ist. Wenn ja, wird das Element
     * in die Liste mergedPositiveSubtreePathLists aufgenommen und somit weiter
     * betrachtet, wenn nein, ist die Schnittmengeneigenschaft nicht gegeben und der
     * Pfad wird verworfen.
     */
    List<Name> mergedPositiveSubtreePathLists = null;

    int mergedCurrentSize = 0; // TOD0: Der Name ist Bullshit. Die Variable gibt den
    // Level an. okay, size bezieht sicht auf die laenge
    // der (Ldap)Names

    if (!positiveSubtreePathLists.isEmpty())
    /*
     * TODO if nach aussen ziehen (evtl. gleich auf Iterator übergehen, siehe todo
     * weiter unten), damit mergedPositiveSubtreePathLists nicht mit null
     * initialisiert werden muss und damit beweisbar ist, dass es initialisiert ist
     */
    {
      RelativePaths currentSubtreePaths = positiveSubtreePathLists.get(0);
      /*
       * TODO: Hier wird eine Liste von zufälligem Level rausgepickt (entsprechend
       * sortierung von attributeMap.keySet(), Wo ist die oben angesprochene
       * Sortierung?
       */
      mergedPositiveSubtreePathLists = currentSubtreePaths.paths;
      mergedCurrentSize = currentSubtreePaths.relative;
    }
    for (int n = 1; n < positiveSubtreePathLists.size(); n++) // TOD0 Iterator
    // verwenden
    {

      RelativePaths currentSubtreePaths = positiveSubtreePathLists.get(n);

      List<Name> shorterLdapNames;
      List<Name> longerLdapNames;

      if (currentSubtreePaths.relative < mergedCurrentSize)
      {
        shorterLdapNames = currentSubtreePaths.paths;
        longerLdapNames = mergedPositiveSubtreePathLists;
      }
      else
      {
        shorterLdapNames = mergedPositiveSubtreePathLists;
        longerLdapNames = currentSubtreePaths.paths;
        mergedCurrentSize = currentSubtreePaths.relative;
      }

      mergedPositiveSubtreePathLists = new Vector<>();

      for (int m = 0; m < longerLdapNames.size(); m++)
      {
        Name longerName = longerLdapNames.get(m);

        for (int p = 0; p < shorterLdapNames.size(); p++)
        {
          Name shorterName = shorterLdapNames.get(p);
          if (longerName.startsWith(shorterName))
          {
            mergedPositiveSubtreePathLists.add(longerName);
            break;
          }
        }

      }

    }

    /*
     * bilde die Schnittmenge aller angegebenen negativen relativen Pfade
     * 
     * Vergleiche jeweils zwei Listen, die je ein Suchattribut repräsentieren.
     */
    List<RelativePath> mergedNegativeList = null;
    if (!negativeSubtreePathLists.isEmpty()) // TOD0 if nach oben ziehen, um
    // mergedNegativeList nicht mit null
    // initialisieren zu müssen
    {
      mergedNegativeList = negativeSubtreePathLists.get(0);
    }

    for (int n = 1; n < negativeSubtreePathLists.size(); n++)
    {
      List<RelativePath> newMergedNegativeList = new ArrayList<>();

      /*
       * alle Objekte von currentList haben die selbe Stufe.
       */
      List<RelativePath> currentList = negativeSubtreePathLists.get(n);

      for (int m = 0; m < mergedNegativeList.size(); m++)
      {
        RelativePath currentPath = mergedNegativeList.get(m);

        /*
         * Suche zu currentPath in der currentList einen Pfad, der eine Aussage über
         * eine Teilmenge oder eine Obermenge der Nachkommen von currentPath macht,
         * die potentielle Ergebnisse sind. Beispiel
         */
        // A1:-2 Hier ist A1 ein Knoten der auf eine Suchbedingung mit Level -2
        // passt, d.h.
        // / | von dem Enkelkinder potentielle Ergebnisse sind.
        // D B:-1 Bei B sind Kinder potentielle Ergebnisse. Die Enkelkinder von A1
        // sind eine
        // | | \ Obermenge der Kinder von B.
        // | | \
        // E C1 C2
        /*
         * Gibt es so einen Match nicht, dann fliegt currentPath raus (indem es nicht
         * nach newMergedNegativeList übertragen wird). Gibt es so einen Match, so
         * wird der längere Pfad von beiden in die newMergedNegativeList übernommen
         * (im Beispiel B).
         */
        for (int p = 0; p < currentList.size(); p++)
        {
          RelativePath otherPath = currentList.get(p);

          RelativePath shorter;
          RelativePath longer;

          if (currentPath.name.size() < otherPath.name.size())
          {
            shorter = currentPath;
            longer = otherPath;
          }
          else
          {
            shorter = otherPath;
            longer = currentPath;
          }

          if (currentPath.name.size() - currentPath.relative == otherPath.name.size()
            - otherPath.relative
            && longer.name.startsWith(shorter.name))
          {
            newMergedNegativeList.add(longer);
            // *
            // * Achtung: Kein break hier! Beispiel
            // *
            // * A A ist currentPath mit Level -2
            // * / \ B1 und B2 sind in der currentList mit Level -1
            // * / \ Sowohl B1 als auch B2 müssen in die
            // * B1 B2 newMergedNegativeList kommen!
            // * | |
            // * C1 C2
            // *
          }

        }
      }

      mergedNegativeList = newMergedNegativeList;

    }

    /*
     * bilde die Schnittmenge aus den positiv und negativ relativen Listen
     */
    List<RelativePath> mergedNegativeSubtreePaths;
    if (mergedPositiveSubtreePathLists != null && mergedNegativeList != null)
    {

      mergedNegativeSubtreePaths = new Vector<>();

      for (int n = 0; n < mergedNegativeList.size(); n++)
      {
        RelativePath currentPath = mergedNegativeList.get(n);

        for (int m = 0; m < mergedPositiveSubtreePathLists.size(); m++)
        {
          Name currentName = mergedPositiveSubtreePathLists.get(m);

          if (currentPath.name.size() < currentName.size())
          {

            if (currentName.startsWith(currentPath.name)
              && currentPath.name.size() - currentPath.relative >= currentName.size())
            {
              /*
               * Wir bilden einen neuen RelativePath mit dem Namen des positiven
               * (currentName), der tiefer im Baum liegt und einem (negativen) Level,
               * der die selbe Nachfahrenebene selektiert wie der Level des negativen
               * (currentPath). Achtung: Es ist möglich, dass der neu-gebildete Level
               * 0 ist.
               */
              RelativePath newPath =
                new RelativePath(currentName.size() - currentPath.name.size()
                  + currentPath.relative, currentName);
              mergedNegativeSubtreePaths.add(newPath);
              // kein break weil mit dem selben negativen currentPath mehrere
              // Schnitte möglich sind.
            }

          }
          else
          {

            if (currentPath.name.startsWith(currentName))
            {
              mergedNegativeSubtreePaths.add(currentPath);
            }

          }

        }
      }
    }
    else
    {
      mergedNegativeSubtreePaths = mergedNegativeList;
    }

    // TOD0: die Listen sollten nie null sein (siehe vorherige TODOs)
    // entsprechend muss hier auf isEmpty() getestet werden
    if (searchFilter.length() == 0
      && mergedPositiveSubtreePathLists == null
      && mergedNegativeSubtreePaths == null)
    {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    List<SearchResult> currentResultList = new ArrayList<>();

    /*
     * TOD0: besser insgesamt auf havePositiveConstraints und haveNegativeConstrainst
     * Booleans umstellen, anstatt die size zu überprüfen. Könnte zum Irrtum
     * verleiten, dass hier mergedNegativeSubtreePaths getestet werden sollte, was
     * aber nicht stimmt, da wenn die mergedListe leer ist, eine leere Ergebnisliste
     * geliefert werden muss, wenn es positive einschränkungen gibt.
     */
    if (negativeSubtreePathLists.isEmpty())
    {

      List<String> positiveSubtreeStrings = new ArrayList<>();

      // create Strings from Names
      if (positiveSubtreePathLists.isEmpty())
      {
        positiveSubtreeStrings.add("");
      }
      else
      {

        for (int n = 0; n < mergedPositiveSubtreePathLists.size(); n++)
        {
          Name currentName = mergedPositiveSubtreePathLists.get(n);
          positiveSubtreeStrings.add(currentName.toString());
        }

      }

      // allgemeine Suche

      for (String subTree : positiveSubtreeStrings)
      {
        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }

        String comma = ",";
        if (subTree.isEmpty()) {
          comma = "";
        }
        NamingEnumeration<SearchResult> currentResults =
            searchLDAP(subTree + comma, searchFilter.toString(), SearchControls.SUBTREE_SCOPE,
            true, endTime);

        while (currentResults.hasMoreElements())
        {
          SearchResult sr = currentResults.nextElement();
          String name = preparePath(sr.getNameInNamespace());
          sr.setName(name + (name.length() > 0 ? comma : "") + subTree);
          currentResultList.add(sr);
        }

      }
    }
    else
    { // Breitensuche ausgehend von den Knoten der mergedNegativeSubtreePaths
      for (RelativePath currentRelativePath : mergedNegativeSubtreePaths)
      {

        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }

        int depth = -currentRelativePath.relative;
        // ACHTUNG: depth kann 0 sein. Siehe Kommentar bei Bildung des Schnitts aus
        // negativen und positiven Pfaden.

        Name currentName = currentRelativePath.name;
        String currentPath = currentName.toString();
        List<SearchResult> currentSearch =
            searchLDAPLevel(currentPath, searchFilter.toString(), depth, endTime);

        currentResultList.addAll(currentSearch);
      }

    }

    List<Dataset> results = new ArrayList<>();

    // generate Datasets from SearchResults

    attributeCache.clear();

    try
    {
      for (SearchResult currentResult : currentResultList)
      {

        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }

        Dataset ds = getDataset(currentResult, endTime);
        results.add(ds);
      }
    }
    finally
    {
      attributeCache.clear();
    }

    return new QueryResultsList(results);
  }

  /**
   * Escaping nach RFC 2254 Abschnitt 4. Sternderl werden nicht escapet, weil sie
   * ihre normale Sternerl-Bedeutung beibehalten sollen.
   */
  private String ldapEscape(String value)
  {
    return value.replaceAll("\\\\", "\\\\5c").replaceAll("\\(", "\\\\28").replaceAll(
      "\\)", "\\\\29").replaceAll("\\00", "\\\\00");
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  @Override
  public String getName()
  {
    return datasourceName;
  }

  /**
   * generiert einen Schluessel aus einem geordneten(!) Vector der Schluesselwerte
   * 
   * @param values
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String generateKey(Map<String, String> data)
  {
    List<ColumnDefinition> keyColumns = new ArrayList<>();
    Iterator<Object> iter = keyAttributes.iterator();
    while (iter.hasNext())
    {
      String keyAttr = (String) iter.next();
      ColumnDefinition colDef = columnDefinitions.get(keyAttr);
      keyColumns.add(colDef);
    }

    // Spalten alphabetisch und nach Pfad-Level sortieren, um einen
    // wohldefinierten Schlüssel zu erhalten, der unabhängig von der Ordnung
    // der Map ist.
    Collections.sort(keyColumns, (o1, o2) -> {
      ColumnDefinition colDef1 = o1;
      ColumnDefinition colDef2 = o2;
      int comp1 = colDef1.attributeName.compareTo(colDef2.attributeName);
      int comp2 = colDef1.relativePath - colDef2.relativePath;
      int comp = comp1;
      if (comp == 0)
      {
        comp = comp2;
      }
      return comp;
    });

    StringBuilder key = new StringBuilder();
    for (ColumnDefinition colDef : keyColumns)
    {
      if (colDef.relativePath == 0)
      {
        key.append('(');
        key.append(ldapEscape(colDef.attributeName));
        key.append('=');
        String value = data.get(colDef.columnName);
        if (value == null) {
          value = "*";
        }
        key.append(ldapEscape(value));
        key.append(')');
      }
    }

    if (key.length() > 0)
    {
      key.insert(0, "(&");
      key.append(')');
    }

    key.append(KEY_SEPARATOR_0_NON_0_RE);

    for (ColumnDefinition colDef : keyColumns)
    {
      if (colDef.relativePath != 0)
      {
        key.append(colDef.columnName);
        key.append('=');
        String value = data.get(colDef.columnName);
        if (value == null) {
          value = "";
        }
        key.append(value.replaceAll("\\*", "").replaceAll(SEPARATOR, ""));
        key.append(SEPARATOR);
      }
    }

    return key.toString();
  }

  private static class CacheKey
  {
    private static final String CACHE_KEY_SEPARATOR = "/{%§";

    private String hash;

    public CacheKey(Name attributePath, String[] searchAttributes)
    {
      StringBuilder buf = new StringBuilder();
      buf.append(searchAttributes.length);
      buf.append(CACHE_KEY_SEPARATOR);
      buf.append(attributePath.toString());
      for (int n = 0; n < searchAttributes.length; ++n)
      {
        buf.append(searchAttributes[n]);
      }
      hash = buf.toString();
    }

    @Override
    public int hashCode()
    {
      return hash.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == null || other.getClass() != getClass())
      {
        return false;
      }
      CacheKey otherKey = (CacheKey) other;
      return hash.equals(otherKey.hash);
    }
  }

  /**
   * vervollständigt SearchResults um Daten aus dem Verzeichnis und gibt ein Dataset
   * zurück
   * 
   * @param searchResult
   * @param endTime
   * @return
   * @throws TimeoutException
   * @author Max Meier (D-III-ITD 5.1)
   * 
   */
  private Dataset getDataset(SearchResult searchResult, long endTime)
      throws TimeoutException
  {

    Attributes attributes = searchResult.getAttributes();

    Map<String, String> relation = new HashMap<>();

    DirContext ctx = null;
    try
    {
      Name pathName;
      Name rootName;

      try
      {
        String tempPath = searchResult.getNameInNamespace();

        tempPath = preparePath(tempPath);

        long timeout = endTime - System.currentTimeMillis();
        if (timeout <= 0) {
          throw new TimeoutException();
        }
        if (timeout > Integer.MAX_VALUE) {
          timeout = Integer.MAX_VALUE;
        }
        LOGGER.trace("getDataset(): verbleibende Zeit: {}", timeout);
        setTimeout(timeout);
        ctx = new InitialLdapContext(properties, null);

        NameParser nameParser = ctx.getNameParser("");
        pathName = nameParser.parse(tempPath);
        rootName = nameParser.parse(baseDN); // TOD0: Das ist eine Konstante, nur
        // einmal berechnen (ausser, dass dies
        // nur mit funktionierender
        // Netzanbindung moeglich ist). Testen
        // mit rausgezogenem Netzkabel

      }
      catch (NamingException e)
      {
        throw new TimeoutException(
          L.m("Fehler beim Zugriff auf das LDAP-Verzeichnis."), e);
      }

      for (Map.Entry<String, ColumnDefinition> columnDefEntry : columnDefinitions.entrySet())
      {

        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }

        ColumnDefinition currentAttribute = columnDefEntry.getValue();

        int relativePath = currentAttribute.relativePath;
        String attributeName = currentAttribute.attributeName;

        String value = null;

        if (relativePath == 0)
        { // value can be found in the attributes

          try
          {
            if (attributes.get(attributeName) != null)
              value = (String) attributes.get(attributeName).get();
          }
          catch (NamingException | NullPointerException e)
          {
            LOGGER.trace("", e);
            // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
          }

        }
        else
        { // value is stored somewhere else in the directory

          Name attributePath = (Name) rootName.clone();

          try
          {

            if (relativePath < 0)
            { // Pfad relativ zum aktuellen Element

              attributePath.addAll(pathName.getPrefix(pathName.size() + relativePath));

            }
            else
            { // relativePath > 0, Pfad relativ zur Wurzel

              attributePath.addAll(pathName.getPrefix(relativePath - rootName.size()));
            }

            String[] searchAttributes = { attributeName };

            Attributes foundAttributes;

            CacheKey key = new CacheKey(attributePath, searchAttributes);
            foundAttributes = attributeCache.get(key);

            if (foundAttributes == null)
            {
              foundAttributes = ctx.getAttributes(attributePath, searchAttributes);
              attributeCache.put(key, foundAttributes);
            }

            Attribute foundAttribute = foundAttributes.get(attributeName);

            if (foundAttribute != null)
            {
              value = (String) foundAttribute.get();
            }

          }
          catch (NamingException | NullPointerException | IndexOutOfBoundsException e)
          {
            // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
            LOGGER.trace("", e);
          }
        }

        if (value != null)
        {
          String lineSeparator = currentAttribute.lineSeparator;
          if (lineSeparator != null)
          {
            value = value.replaceAll(lineSeparator, "\n");
          }
          relation.put(columnDefEntry.getKey(), value);
        }
      }

      String key = generateKey(relation);

      return new LDAPDataset(key, relation);

    }
    finally
    {
      try
      {
        if (ctx != null) {
          ctx.close();
        }
      }
      catch (Exception e)
      {}
    }
  }

  /**
   * Sucht im Teilbaum path + BASE_DN nach Knoten, auf die Suchkriterium filter
   * passt.
   * 
   * @param path
   *          der Pfad des Startknotens. Wird mit BASE_DN konkateniert.
   * @param filter
   *          der Suchfilter.
   * @param searchScope
   *          SearchControls.SUBTREE_SCOPE, SearchControls.OBJECT_SCOPE oder
   *          SearchControls.ONELEVEL_SCOPE, um anzugeben wo gesucht werden soll.
   * @param onlyObjectClass
   *          falls true, werden nur Knoten zurückgeliefert, deren objectClass
   *          {@link #objectClass} entspricht.
   * @param endTime
   *          wird die Suche nicht vor dieser Zeit beendet, wird eine
   *          TimeoutException geworfen
   * @return die Suchergebnisse
   * @throws TimeoutException
   *           falls die Suche nicht schnell genug abgeschlossen werden konnte.
   * @author Max Meier (D-III-ITD 5.1)
   * 
   */
  private NamingEnumeration<SearchResult> searchLDAP(String path, String filter,
      int searchScope, boolean onlyObjectClass, long endTime)
      throws TimeoutException
  {
    LOGGER.debug("searchLDAP({}, {}, {}, {}, {}) zum Zeitpunkt {}", path, filter, searchScope, onlyObjectClass, endTime,
        System.currentTimeMillis());

    SearchControls searchControls = new SearchControls();

    searchControls.setSearchScope(searchScope);

    long timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) {
      throw new TimeoutException();
    }
    if (timeout > Integer.MAX_VALUE) {
      timeout = Integer.MAX_VALUE;
    }
    searchControls.setTimeLimit((int) timeout);

    if (onlyObjectClass)
    {
      filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    }
    else
    {
      filter = "(&(objectClass=" + "*" + ")" + filter + ")"; // TOD0 das
      // objectClass=* ist
      // doch überflüssig
    }

    DirContext ctx = null;

    NamingEnumeration<SearchResult> result;

    try
    {
      setTimeout(timeout);
      LOGGER.trace("new InitialLdapContext(properties, null)");
      ctx = new InitialLdapContext(properties, null);

      LOGGER.trace("ctx.getNameParser(\"\")");
      NameParser nameParser = ctx.getNameParser("");
      Name name = nameParser.parse(path + baseDN);

      LOGGER.trace("ctx.search({}, {}, searchControls) mit Zeitlimit {}", name, filter, searchControls.getTimeLimit());
      result = ctx.search(name, filter, searchControls);
      LOGGER.trace("ctx.search() abgeschlossen");
    }
    catch (NamingException e)
    {
      throw new TimeoutException(e);
    }
    finally
    {
      try
      {
        if (ctx != null) {
          ctx.close();
        }
      }
      catch (Exception e)
      {
        LOGGER.trace("", e);
      }
    }

    LOGGER.debug("{} (verbleibende Zeit: {})", result.hasMoreElements() ? "Ergebnisse gefunden" : "keine Ergebnisse gefunden",
        endTime - System.currentTimeMillis());
    return result;
  }

  /**
   * Durchsucht die Nachfahren des durch path + BASE_DN bezeichneten Knotens mit
   * Abstand level zu diesem Knoten nach Knoten, die auf die Suchanfrage filter
   * passen. Es werden nur Objekte mit objectClass = {@link #objectClass} geliefert.
   * 
   * @return eine List von {@link SearchResult}s.
   * @throws TimeoutException
   *           falls die Anfrage nicht vor endTime beantwortet werden konnte.
   * @author Max Meier (D-III-ITD 5.1)
   * 
   */
  private List<SearchResult> searchLDAPLevel(String path, String filter, int level,
      long endTime) throws TimeoutException
  {

    List<String> seeds = new ArrayList<>();
    seeds.add(path);

    String comma;

    for (int n = 0; n < (level - 1); n++)
    {
      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException();
      }

      List<String> nextSeeds = new ArrayList<>();

      for (String searchPath : seeds)
      {

        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }

        comma = ",";
        if (searchPath.isEmpty()) {
          comma = "";
        }

        NamingEnumeration<SearchResult> enumer =
          searchLDAP(searchPath + comma, "", SearchControls.ONELEVEL_SCOPE, false,
            endTime);

        while (enumer.hasMoreElements())
        {

          if (System.currentTimeMillis() > endTime) {
            throw new TimeoutException();
          }

          SearchResult currentResult = enumer.nextElement();
          String subPath = preparePath(currentResult.getNameInNamespace());
          comma = ",";
          if (subPath.isEmpty()) {
            comma = "";
          }
          String currentPath = subPath + comma + searchPath;
          nextSeeds.add(currentPath);
        }
      }

      seeds = nextSeeds;

    }

    List<SearchResult> result = new ArrayList<>();

    for (String currentPath : seeds)
    {

      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException();
      }

      comma = ",";
      if (currentPath.isEmpty()) {
        comma = "";
      }

      NamingEnumeration<SearchResult> enumer =
        searchLDAP(currentPath + comma, filter,
          level == 0 ? SearchControls.OBJECT_SCOPE : SearchControls.ONELEVEL_SCOPE,
          true, endTime);

      while (enumer.hasMoreElements())
      {
        if (System.currentTimeMillis() > endTime) {
          throw new TimeoutException();
        }
        SearchResult sr = enumer.nextElement();
        String name = preparePath(sr.getNameInNamespace());
        String actualPath = name + (name.length() > 0 ? comma : "") + currentPath;
        sr.setName(actualPath);
        result.add(sr);
      }

    }

    return result;
  }

  /**
   * Entferne umschliessende Doublequotes aus path falls vorhanden. [Folgende
   * Erklärung stimmt eventuell nicht mehr, seit von getName() auf
   * getNameInNamespace() umgestellt wurde. Eventuell kann das ganze
   * Doublequote-Killen entfallen] Dies muss gemacht werden, da das Zeichen '/' in
   * LDAP Pfadkomponenten erlaubt ist, im JNDI jedoch als Komponententrenner
   * verwendet wird. Deswegen werden Pfade, die '/' enthalten von .getName() in
   * Anführungszeichen gesetzt und können deshalb nicht mehr geparsed werden.]
   * 
   * Nach dem Entfernen der Doublequotes wird geschaut ob path auf {@link #baseDN}
   * endet (ist normalerweise der Fall) und falls ja wird dieses Suffix
   * weggeschnitten.
   * 
   * [Folgendes ist mit der Umstellung auf getNameInNamespace() eventuell auch
   * überholt: TOD0 Ich bin mir nicht sicher, ob hier nicht noch mehr zu tun ist. Was
   * ist z.B. mit enthaltenen Doublequotes? Kann das passieren? Wie werden die
   * escapet?]
   * 
   * @author Max Meier, Matthias Benkmann (D-III-ITD 5.1)
   * 
   */
  private String preparePath(String path)
  {

    int tempEnd = path.length() - 1;
    if (tempEnd > 0 && path.charAt(0) == '"' && path.charAt(tempEnd) == '"')
    {
      path = path.substring(1, tempEnd);
    }
    if (path.endsWith(baseDN))
    {
      int end = path.length() - baseDN.length();
      if (end > 0) { // for the comma present if path != baseDN
        --end;
      }
      path = path.substring(0, end);
    }

    return path;

  }

  private String errorMessage()
  {
    return L.m("Fehler in Definition von Datenquelle \"%1\": ", datasourceName);
  }

  // LDAPDataset
  private class LDAPDataset implements Dataset
  {

    private String key;

    private Map<String, String> relation;

    LDAPDataset(String key, Map<String, String> relation)
    {
      this.key = key;
      this.relation = relation;
    }

    @Override
    public String get(java.lang.String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName)) {
        throw new ColumnNotFoundException();
      }

      return relation.get(columnName);
    }

    @Override
    public String getKey()
    {
      return key;
    }

  }
}
