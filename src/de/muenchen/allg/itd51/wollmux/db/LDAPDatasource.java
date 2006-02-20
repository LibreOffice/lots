/*
 * Dateiname: LDAPDatasource.java
 * Projekt  : WollMux
 * Funktion : Verschafft Zugriff auf LDAP-Verzeichnisdienst als Datasource.
 * 
 * Copyright: Landeshauptstadt München
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
 * -------------------------------------------------------------------
 *
 * @author Max Meier (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datasource für Zugriff auf ein LDAP-Verzeichnis
 * 
 * @author Max Meier (D-III-ITD 5.1)
 */
public class LDAPDatasource implements Datasource
{
  private Set schema;

  private String name;

  private String url;

  private String baseDN;

  private String objectClass;

  /** properties für die Verbindung zum LDAP-Server*/
  private Properties properties = new Properties();

  /** Separator zur Schluesselerzeugung aus mehreren Schluesselwerten*/
  private final static String SEPARATOR = "&:=&:%";
  
  /**
   * Trennt den ersten Teil des Schlüssels, der den Pfaden mit Level 0
   * entspricht vom Rest des Schlüssels
   */
  private static final String KEY_SEPARATOR_0_NON_0_RE = "==%§%==";

  /** Map von query-Strings auf LDAP-Attributnamen */
  private Map columnDefinitions = new HashMap();

  /** Key-Attribute (LDAP) (Strings).*/
  private List keyAttributes = new Vector();

  /** Was für Arten von Pfaden kommen als Schlüsselspalten vor (ABSOLUTE_ONLY, RELATIVE_ONLY, ABSOLUTE_AND_RELATIVE).*/
  private int keyStatus; // 0:= nur absolute Attribute, 1:= absolute und
                          // relative Attribute, 2:= nur relative Attribute
  /** nur Attributpfade der Form 0:*. */
  private static final int ABSOLUTE_ONLY = 0;
  
  /** nur Attributpfade der Form num:* wobei num nicht 0 ist.*/
  private static final int RELATIVE_ONLY = 2;
  
  /** Sowohl Attributpfade der Form 0:* als auch der Form num:* mit num ungleich 0. */
  private static final int ABSOLUTE_AND_RELATIVE = 1;

  /** regex für erlaubte Bezeichner*/
  private static final Pattern SPALTENNAME = Pattern
      .compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  
  /** Regex zum Checken der Syntax von BASE_DN. 
   * TOD0: Sicher zu restriktiv!
   */
  private static final Pattern BASEDN_RE = Pattern
      .compile("^[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+(,[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+)*$");
  
  /** Regex zum Checken der Syntax von LDAP-Attributsbezeichnern. 
   * TOD0: Sicher zu restriktiv!
   */
  private static final Pattern ATTRIBUTE_RE = Pattern
      .compile("^[a-zA-Z]+$");
  
  /** Regex zum Checken, ob ein Schlüssel für die LDAP-Datasource legal ist.
   */
  private static final Pattern KEY_RE = Pattern
      .compile("^(\\(&(\\([^()=]+[^()]*\\))+\\))?"+KEY_SEPARATOR_0_NON_0_RE+"([a-zA-Z_][a-zA-Z0-9_]*=.*"+SEPARATOR+")?$");


  /** temporärer cache für relative Attribute (wird bei jeder neuen Suche neu
  * angelegt) */
  private Map attributeCache = new HashMap();

  /**
   * Erzeugt eine neue LDAPDatasource.
   * 
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser
   *          LDAPDatasource bereits vollständig instanziierten Datenquellen
   *          (zur Zeit nicht verwendet).
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser
   *          LDAPDatasource enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit
   *          nicht verwendet).
   * @throws ConfigurationErrorException
   *           falls in der Definition in sourceDesc ein Fehler ist.
   */
  public LDAPDatasource(Map nameToDatasource, ConfigThingy sourceDesc,
      URL context) throws ConfigurationErrorException
  {

    try
    {
      name = sourceDesc.get("NAME").toString();
    }
    catch (NodeNotFoundException x)
    { throw new ConfigurationErrorException("NAME der Datenquelle fehlt"); }

    try
    { 
      url = sourceDesc.get("URL").toString();
      url = "ldap://tucker.afd.dir.muenchen.de:389";
      try
      {
        new URI(url);
      }
      catch (URISyntaxException e)
      {
        throw new ConfigurationErrorException("Fehler in LDAP-URL \""+url+"\"");
      }
      
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(errorMessage()
                                            + "URL des LDAP-Servers fehlt.");
    }

    try
    { 
      baseDN = sourceDesc.get("BASE_DN").toString();
      if (!BASEDN_RE.matcher(baseDN).matches())
        throw new ConfigurationErrorException("BASE_DN-Wert ist ungültig: \""+baseDN+"\"");
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(errorMessage()
                                            + "BASE_DN des LDAP-Servers fehlt.");
    }

    try
    {
      objectClass = sourceDesc.get("OBJECT_CLASS").toString();
      if (!ATTRIBUTE_RE.matcher(objectClass).matches())
        throw new ConfigurationErrorException("OBJECT_CLASS enthält unerlaubte Zeichen: \""+objectClass+"\"");

    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(errorMessage()
                                            + "Keine OBJECT_CLASS definiert.");
    }

    // set properties
    properties.put(
        Context.INITIAL_CONTEXT_FACTORY,
        "com.sun.jndi.ldap.LdapCtxFactory");
    properties.put(Context.PROVIDER_URL, url); // + "/" + baseDN);

    ConfigThingy spalten = sourceDesc.query("Spalten");

    if (spalten.count() == 0)
      throw new ConfigurationErrorException(errorMessage()
                                            + "Abschnitt 'Spalten' fehlt.");

    schema = new HashSet();

    Iterator iter = spalten.iterator();

    // iteriere über alle Spalten-Relationen
    while (iter.hasNext())
    {

      ConfigThingy spaltenDesc = (ConfigThingy) iter.next();
      Iterator iter2 = spaltenDesc.iterator();

      // iteriere über eine Spalten-Relation
      while (iter2.hasNext())
      {

        ConfigThingy spalteDesc = (ConfigThingy) iter2.next();

        String spalte;
        int relativePath;
        String attributeName;
        String objectClass = null;
        String lineSeparator = null;

        try{ spalte = spalteDesc.get("DB_SPALTE").toString(); } 
        catch (NodeNotFoundException x)
        {
          throw new ConfigurationErrorException(errorMessage()
                                                + "DB_SPALTE Angabe fehlt");
        }

        if (!SPALTENNAME.matcher(spalte).matches())
          throw new ConfigurationErrorException(
              errorMessage()
                  + "Spalte \""
                  + spalte
                  + "\" entspricht nicht der Syntax eines Bezeichners");

        String path;
        try{ path = spalteDesc.get("PATH").toString(); }
        catch (NodeNotFoundException e1)
        {
          throw new ConfigurationErrorException("PATH-Angabe fehlt für Spalte "+spalte);
        } 

        // get relativePath and attributeName
        String[] splitted = path.split(":");

        if (splitted.length != 2)
          throw new ConfigurationErrorException(
              errorMessage() + "Syntaxerror bei Pfadangabe von " + spalte);

        try
        {
          relativePath = Integer.parseInt(splitted[0]);
        }
        catch (NumberFormatException e)
        {
          throw new ConfigurationErrorException(
              errorMessage()
                  + "Syntaxerror bei Angabe des relativen Pfads von "
                  + spalte);
        }

        attributeName = splitted[1]; 
        if (!ATTRIBUTE_RE.matcher(attributeName).matches())
          throw new ConfigurationErrorException("Illegaler Attributsbezeichner: \""+attributeName+"\"");

        try
        {
          objectClass = spalteDesc.get("OBJECT_CLASS").toString();
        }
        catch (NodeNotFoundException x)
        {
          // do nothing... (Angabe von OBJECT_CLASS optional)
        }

        try
        {
          lineSeparator = spalteDesc.get("LINE_SEPARATOR").toString();
        }
        catch (NodeNotFoundException x)
        {
          // do nothing... (Angabe von LINE_SEPARATOR optional)
        }

        
        ColumnDefinition columnAttr = new ColumnDefinition(spalte, relativePath,
            attributeName);
        columnAttr.objectClass = objectClass;
        columnAttr.lineSeparator = lineSeparator;
        columnDefinitions.put(spalte, columnAttr);
        schema.add(spalte);
      }
    }

    // Key-Attribute
    ConfigThingy keys = sourceDesc.query("Schluessel");

    if (keys.count() == 0)
      throw new ConfigurationErrorException(errorMessage()
                                            + "Schluessel-Abschnitt fehlt.");

    ConfigThingy keySpalten;

    try
    {
      // der letzte definierte Schluessel wird verwendet
      keySpalten = keys.getLastChild();
    }
    catch (NodeNotFoundException e)
    { throw new RuntimeException("Unmöglich. Ich hab doch vorher count() überprüft.");}

    Iterator keyIterator = keySpalten.iterator();
    
    if (!keyIterator.hasNext())
      throw new ConfigurationErrorException(
          errorMessage() + "Keine Schluesselspalten angegeben.");
    
    boolean onlyRelative = true; //true, falls kein Attributpfad der Form 0:*
    boolean onlyAbsolute = true;  //true, falls nur Attributspfade der Form 0:*

    // speichere die Schluesselattribute
    while (keyIterator.hasNext())
    {
      String currentName = keyIterator.next().toString();

      ColumnDefinition currentKeyLDAPAttribute = (ColumnDefinition) columnDefinitions
          .get(currentName);

      // ist Schluesselattribut vorhanden?
      if (currentKeyLDAPAttribute == null)
        throw new ConfigurationErrorException( 
            "Spalte \""+currentName+"\" ist nicht im Schema definiert und kann deshalb nicht als Schluesselspalte verwendet werden.");

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
      keyStatus = ABSOLUTE_ONLY;
    else if (onlyRelative)
      keyStatus = RELATIVE_ONLY;
    else
      keyStatus = ABSOLUTE_AND_RELATIVE;

    
  }

  /** Setzt die timeout-Properties. */
  private void setTimeout(Properties props, long timeout)
  {
    properties.setProperty("com.sun.jndi.ldap.connect.timeout", ""+timeout);
    properties.setProperty("com.sun.jndi.dns.timeout.initial", ""+timeout);
    properties.setProperty("com.sun.jndi.dns.timeout.retries", "1");
  }
  
  /**
   * repräsentiert eine Spaltendefinition
   * @author Max  Meier (D-III-ITD 5.1)
   */
  private class ColumnDefinition
  {

    /**
     * relativer Pfad: 0 := Attribut befindet sich im selben Knoten negativ :=
     * relative Pfadangabe "nach oben" vom aktuellen Knoten aus positiv :=
     * relative Pfadangabe von der Wurzel aus
     */
    int relativePath;

    /**
     * Name der Spalte.
     */
    String columnName;
    
    /** Attributname im LDAP*/
    String attributeName = null;

    /** exklusive objectClass */
    String objectClass = null;

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
  public Set getSchema()
  {
    return schema;
  }

  /**
   * 
   * @param key
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List keyToFindQuery(String key)
  {
    String[] ks = key.split(KEY_SEPARATOR_0_NON_0_RE, 2);
    ks = ks[1].split(SEPARATOR);
    
    List query = new Vector(ks.length);

    for (int i = 0; i < ks.length; ++i)
    {
      String[] q = ks[i].split("=",2);
      query.add(new QueryPart(q[0], q[1]));
    }

    return query;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection,
   *      long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout)
      throws TimeoutException
  {
    if (keys.isEmpty()) return new QueryResultsList(new Vector(0));
    
    Vector results = new Vector(keys.size());
    
    try
    {
      long endTime = System.currentTimeMillis() + timeout;

      attributeCache.clear();

      if (keyStatus == ABSOLUTE_ONLY || keyStatus == ABSOLUTE_AND_RELATIVE)
      { // absolute Attribute vorhanden

        Iterator iter = keys.iterator();

        // build searchFilter
        String searchFilter = "";

        while (iter.hasNext())
        {
          String currentKey = (String) iter.next();
          if (!KEY_RE.matcher(currentKey).matches()) continue;
          String[] ks = currentKey.split(KEY_SEPARATOR_0_NON_0_RE,2);
          searchFilter = searchFilter + ks[0];
        }
        if (searchFilter.length() == 0) return new QueryResultsList(new Vector(0));
        searchFilter = "(|" + searchFilter + ")";

        // search LDAP
        NamingEnumeration currentResults = searchLDAP(
            "",
            searchFilter,
            SearchControls.SUBTREE_SCOPE,
            true,
            endTime);

        while (currentResults.hasMoreElements())
        {
          if (System.currentTimeMillis() > endTime)
            throw new TimeoutException();
          try
          {
            SearchResult currentResult = (SearchResult) currentResults.next();
            Dataset dataset = getDataset(currentResult, endTime);
            if (keyStatus == ABSOLUTE_ONLY)
            {
              results.add(dataset);
            }
            else if (keys.contains(dataset.getKey()))
            {
              results.add(dataset);
            }
          }
          catch (NamingException e)
          {
            Logger.error("Error in LDAP-Directory.", e);
          }
        }
      }
      else //if (keyStatus == RELATIVE_ONLY)
      { // nur relative Attribute
        Iterator iter = keys.iterator();
        while (iter.hasNext())
        {
          List query = keyToFindQuery((String) iter.next());
          timeout = endTime - System.currentTimeMillis();
          if (timeout <= 0)
            throw new TimeoutException();
          QueryResults res = find(query, timeout);
          Iterator iter2 = res.iterator();
          while (iter2.hasNext()) results.add(iter2.next());
        }
      }
      
      results.trimToSize();
      return new QueryResultsList(results);
      
    }
    finally
    {
      attributeCache.clear();
    }
  }

  /**
   * Speichert eine Liste von (Ldap)Names, die zu einer Suchanfrage über Attribute
   * mit Pfad-Level ungleich 0 gehören (alle mit dem selben Level). 
   * @author Max Meier (D-III-ITD 5.1)
   */
  private class RelativePaths
  {

    public int relative;

    public List paths; //of (Ldap)Names

    RelativePaths(int relative, List paths)
    {
      this.relative = relative;
      this.paths = paths;
    }

  }

  /**
   * Ein (Ldap)Name und der Pfad-Level für die Suche. Typischerweise ist der Pfad-Level != 0,
   * aber ein Level von 0 ist auch möglich (kann bei Bildung der Schnittmenge von positiven
   * und negativen Kandidaten entstehen). 
   * @author Max Meier (D-III-ITD 5.1)
   */
  private class RelativePath
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
   * Liefert zum Pfadlevel pathLength alle Knoten, die auf die LDAP-Suchanfrage filter passen
   * @throws TimeoutException falls die Suche nicht vor endTime beendet werden konnte.
   * @author Max Meier (D-III-ITD 5.1)
   */
  private RelativePaths getPaths(String filter, int pathLength, 
      long endTime) throws TimeoutException
  {

    Vector paths;

    long timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException();
    if (timeout > Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;

    try
    { 
      setTimeout(properties, timeout);
      Logger.debug2("new InitialLdapContext(properties, null)");
      DirContext ctx = new InitialLdapContext(properties, null);
      
      Logger.debug2("ctx.getNameParser(\"\")");
      NameParser np = ctx.getNameParser("");
      int rootSize = np.parse(baseDN).size();  
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
    
      sc.setTimeLimit((int) timeout);
      
      Logger.debug2("ctx.search("+baseDN+","+filter+",sc) mit Zeitlimit "+sc.getTimeLimit());
      NamingEnumeration enumer = ctx.search(baseDN, filter, sc);
      Logger.debug2("ctx.search() abgeschlossen");

      paths = new Vector();

      while (enumer.hasMoreElements())
      {
        if (System.currentTimeMillis() > endTime) throw new TimeoutException();
        SearchResult result = (SearchResult) enumer.nextElement();
        String path = checkQuotes(result.getName());
        Name pathName = np.parse(path);
        /*
         * ACHTUNG: hier kann NICHT 
         * (pathLength < 0 && (pathName.size()+rootLength > abs(pathLength))) 
         * getestet werden, denn Minus-Bedingungen betreffen die Nachfahren, 
         * hier muesste also die Tiefe des tiefsten Nachfahrens ausgewertet werden, 
         * die wir nicht kennen.
         */
        if (pathName.size() + rootSize == pathLength || pathLength < 0)  
          paths.add(pathName);
      }

    }
    catch (NamingException e)
    {
      throw new TimeoutException("Internal error in LDAP.", e);
    }

    return new RelativePaths(pathLength, paths);

  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List,
   *      long)
   *      
   */
  public QueryResults find(List query, long timeout) throws TimeoutException
  {

    long endTime = System.currentTimeMillis() + timeout;

    Iterator iter = query.iterator();

    String searchFilter = "";
    List positiveSubtreePathLists = new Vector();

    List negativeSubtreePathLists = new Vector();

    Map mapNon0PathLevelToSearchFilter = new HashMap(); 

    boolean first = true;
    while (iter.hasNext())
    {

      if (System.currentTimeMillis() > endTime) throw new TimeoutException();

      QueryPart currentQuery = (QueryPart) iter.next();

      ColumnDefinition colDef = (ColumnDefinition) columnDefinitions.get(currentQuery
          .getColumnName()); 

      if (colDef == null) return new QueryResultsList(new Vector(0));

      String attributeName = colDef.attributeName;
      int relativePath = colDef.relativePath;
      String attributeValue = currentQuery.getSearchString();

      String objectClass = colDef.objectClass;
      String currentSearchFilter = "(" 
                                   + ldapEscape(attributeName)
                                   + "="
                                   + ldapEscape(attributeValue)
                                   + ")";
      if (objectClass != null)
      {
        currentSearchFilter = "(&"
                              + currentSearchFilter
                              + "(objectClass="
                              + ldapEscape(objectClass)
                              + "))";
      }

      if (relativePath == 0)
      { // edit filter
        if (first)
        {
          searchFilter = currentSearchFilter;
          first = false;
        }
        else
        {
          searchFilter = "(&" + currentSearchFilter + searchFilter + ")";
        }
      }
      else
      { // edit searchFilters for subtree searches:

        Integer key = new Integer(relativePath);

        String non0LevelSearchFilter = (String) mapNon0PathLevelToSearchFilter.get(key);

        if (non0LevelSearchFilter == null)
        {
          non0LevelSearchFilter = currentSearchFilter;
        }
        else
        {
          non0LevelSearchFilter = "(&"
                             + currentSearchFilter
                             + non0LevelSearchFilter
                             + ")";
        }

        mapNon0PathLevelToSearchFilter.put(key, non0LevelSearchFilter);

      }

    }

    Iterator attributeKeys = mapNon0PathLevelToSearchFilter.keySet().iterator();
//TOD0 evtl. Optimierung: attributeKeys nicht in zufälliger Reihenfolge durchgehen
    while (attributeKeys.hasNext())
    {
      Integer currentKey = (Integer) attributeKeys.next();

      int relativePath = currentKey.intValue();

      String pathFilter = (String) mapNon0PathLevelToSearchFilter.get(currentKey);

      RelativePaths paths = getPaths(pathFilter, relativePath, endTime);

      if (relativePath > 0)
      {
        positiveSubtreePathLists.add(paths);
      }
      else
      { 
        /*
         * RelativePaths in Liste von RelativePath Objekten umwandeln, da nachher
         * im Merge-Schritt eine Liste entstehen soll, in der Pfade verschiedener
         * Stufe gemischt enthalten sein können.
         */
        List negativeSubtreePaths = new Vector();
        for (int n = 0; n < paths.paths.size(); n++) //TOD0 Iterator verwenden
        {
          Name currentName = (Name) paths.paths.get(n);
          RelativePath newNegativePath = new RelativePath(paths.relative,
              currentName);
          negativeSubtreePaths.add(newNegativePath);
        }
        negativeSubtreePathLists.add(negativeSubtreePaths);

      }

    }

    /*
     * bilde die Schnittmenge aller angegebenen positiv relativen Pfade
     * 
     * Dieser Algorithmus vergleicht zwei Listen, die jeweils alle relevanten
     * Pfade eines Suchattributs repräsentieren. Zuerst werden die Listen nach
     * der Länge der enthaltenen Pfade sortiert, danach wird betrachte, ob für
     * jedes Element der Liste der längeren Pfade ein Element aus der Liste der
     * kürzeren Pfade existiert, das ein Prefix des ersten Elements ist. Wenn
     * ja, wird das Element in die Liste mergedPositiveSubtreePathLists
     * aufgenommen und somit weiter betrachtet, wenn nein, ist die
     * Schnittmengeneigenschaft nicht gegeben und der Pfad wird verworfen.
     */
    List mergedPositiveSubtreePathLists = null;

    int mergedCurrentSize = 0; //TOD0: Der Name ist Bullshit. Die Variable gibt den Level an. okay, size bezieht sicht auf die laenge der (Ldap)Names

    if (positiveSubtreePathLists.size() > 0) //TOD0 if nach aussen ziehen (evtl. gleich auf Iterator übergehen, siehe todo weiter unten), damit mergedPositiveSubtreePathLists nicht mit null initialisiert werden muss und damit beweisbar ist, dass es initialisiert ist
    {
      RelativePaths currentSubtreePaths = (RelativePaths) positiveSubtreePathLists
          .get(0); //TOD0: Hier wird eine Liste von zufälligem Level rausgepickt (entsprechend sortierung von attributeMap.keySet(), Wo ist die oben angesprochene Sortierung?
      mergedPositiveSubtreePathLists = currentSubtreePaths.paths;
      mergedCurrentSize = currentSubtreePaths.relative;
    }

    for (int n = 1; n < positiveSubtreePathLists.size(); n++) //TOD0 Iterator verwenden
    {

      RelativePaths currentSubtreePaths = (RelativePaths) positiveSubtreePathLists
          .get(n);

      List shorterLdapNames, longerLdapNames; //of (Ldap)Names

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

      mergedPositiveSubtreePathLists = new Vector();

      for (int m = 0; m < longerLdapNames.size(); m++)
      {
        Name longerName = (Name) longerLdapNames.get(m);

        for (int p = 0; p < shorterLdapNames.size(); p++)
        {
          Name shorterName = (Name) shorterLdapNames.get(p);
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
     * 
     */
    List mergedNegativeList = null;
    if (negativeSubtreePathLists.size() > 0) //TOD0 if nach oben ziehen, um mergedNegativeList nicht mit null initialisieren zu müssen
    {
      mergedNegativeList = (List) negativeSubtreePathLists.get(0);
    }

    for (int n = 1; n < negativeSubtreePathLists.size(); n++)
    {
      List newMergedNegativeList = new Vector();

      /* 
       * alle Objekte von currentList haben die selbe Stufe.
       */ 
      List currentList = (List) negativeSubtreePathLists.get(n);

      for (int m = 0; m < mergedNegativeList.size(); m++)
      {
        RelativePath currentPath = (RelativePath) mergedNegativeList.get(m);

        /* 
         * Suche zu currentPath in der currentList einen Pfad, der eine Aussage über eine
         * Teilmenge oder eine Obermenge der Nachkommen von currentPath macht, die potentielle
         * Ergebnisse sind. Beispiel*/
//      A1:-2     Hier ist A1 ein Knoten der auf eine Suchbedingung mit Level -2 passt, d.h.
//     / |        von dem Enkelkinder potentielle Ergebnisse sind.      
//    D  B:-1     Bei B sind Kinder potentielle Ergebnisse. Die Enkelkinder von A1 sind eine
//    |  | \      Obermenge der Kinder von B. 
//    |  |  \   
//    E  C1  C2

         /* 
         * Gibt es so einen Match nicht,
         * dann fliegt currentPath raus (indem es nicht nach newMergedNegativeList 
         * übertragen wird). Gibt es so einen Match, so wird der längere Pfad von beiden in
         * die newMergedNegativeList übernommen (im Beispiel B).
         */
        for (int p = 0; p < currentList.size(); p++)
        {
          RelativePath otherPath = (RelativePath) currentList.get(p);

          RelativePath shorter, longer;

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

          if (currentPath.name.size() - currentPath.relative == otherPath.name
              .size() - otherPath.relative              
              && longer.name.startsWith(shorter.name)) 
          {
            newMergedNegativeList.add(longer);
//            * 
//            * Achtung: Kein break hier! Beispiel
//            * 
//            *            A          A ist currentPath mit Level -2
//            *           / \         B1 und B2 sind in der currentList mit Level -1
//            *          /   \        Sowohl B1 als auch B2 müssen in die
//            *         B1   B2       newMergedNegativeList kommen!
//            *         |     |
//            *         C1    C2      
//            *
          }

        }
      }

      mergedNegativeList = newMergedNegativeList;

    }

    /*
     * bilde die Schnittmenge aus den positiv und negativ relativen Listen
     */
    List mergedNegativeSubtreePaths;
    if (mergedPositiveSubtreePathLists != null && mergedNegativeList != null)
    {

      mergedNegativeSubtreePaths = new Vector();

      for (int n = 0; n < mergedNegativeList.size(); n++)
      {
        RelativePath currentPath = (RelativePath) mergedNegativeList.get(n);

        for (int m = 0; m < mergedPositiveSubtreePathLists.size(); m++)
        {
          Name currentName = (Name) mergedPositiveSubtreePathLists.get(m);

          if (currentPath.name.size() < currentName.size())
          {

            if (currentName.startsWith(currentPath.name)
                && currentPath.name.size() - currentPath.relative >= currentName.size())
            { 
              /*
               * Wir bilden einen neuen RelativePath mit dem Namen des positiven (currentName),
               * der tiefer im Baum liegt und einem (negativen) Level, der die selbe Nachfahrenebene
               * selektiert wie der Level des negativen (currentPath).
               * Achtung: Es ist möglich, dass der neu-gebildete Level 0 ist.
               */
              RelativePath newPath = new RelativePath(currentName.size() - currentPath.name.size() + currentPath.relative, currentName);
              mergedNegativeSubtreePaths.add(newPath);
              //kein break weil mit dem selben negativen currentPath mehrere Schnitte möglich sind.
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

    if (searchFilter.equals("") //TOD0: die Listen sollten nie null sein (siehe vorherige TODOs) entsprechend muss hier auf isEmpty() getestet werden
        && mergedPositiveSubtreePathLists == null 
        && mergedNegativeSubtreePaths == null)
    {
      return new QueryResultsList(new Vector(0));
    }
    
    List currentResultList = new Vector();

    /* 
     * TOD0: besser insgesamt auf havePositiveConstraints und haveNegativeConstrainst 
     * Booleans umstellen, anstatt die size zu überprüfen. Könnte zum Irrtum verleiten, 
     * dass hier  mergedNegativeSubtreePaths getestet werden sollte, was aber nicht stimmt,
     * da wenn die mergedListe leer ist, eine leere Ergebnisliste geliefert werden muss, 
     * wenn es positive einschränkungen gibt. 
     */
    if (negativeSubtreePathLists.size() == 0) 
    {
      
      List positiveSubtreeStrings = new Vector();
      
      // create Strings from Names
      if (positiveSubtreePathLists.size() == 0)
      {
        positiveSubtreeStrings.add("");
      }
      else
      {
        
        for (int n = 0; n < mergedPositiveSubtreePathLists.size(); n++)
        {
          Name currentName = (Name) mergedPositiveSubtreePathLists.get(n);
          positiveSubtreeStrings.add(currentName.toString());
        }
        
      }
      
      Iterator subtreeIterator = positiveSubtreeStrings.iterator();
      
      // allgemeine Suche
      
      while (subtreeIterator.hasNext())
      {
        
        if (System.currentTimeMillis() > endTime) throw new TimeoutException();
        
        String subTree = (String) subtreeIterator.next();
        String comma = ",";
        if (subTree.equals("")) comma = "";
        NamingEnumeration currentResults = searchLDAP(
            subTree + comma,
            searchFilter,
            SearchControls.SUBTREE_SCOPE,
            true,
            endTime); 

        while (currentResults.hasMoreElements())
        {
          SearchResult sr = (SearchResult) currentResults.nextElement();
          String name = checkQuotes(sr.getName());
          sr.setName(name + (name.length()>0?comma:"") + subTree);
          currentResultList.add(sr);
        }

      }
    }
    else
    { // Breitensuche ausgehend von den Knoten der mergedNegativeSubtreePaths
      for (int n = 0; n < mergedNegativeSubtreePaths.size(); n++) //TOD0 Iterator verwenden
      {

        if (System.currentTimeMillis() > endTime) throw new TimeoutException();

        RelativePath currentRelativePath = (RelativePath) mergedNegativeSubtreePaths
            .get(n);
        int depth = -currentRelativePath.relative;
        //ACHTUNG: depth kann 0 sein. Siehe Kommentar bei Bildung des Schnitts aus negativen
        //und positiven Pfaden.

        Name currentName = currentRelativePath.name;
        String currentPath = currentName.toString();
        List currentSearch = searchLDAPLevel(
            currentPath,
            searchFilter,
            depth,
            endTime);

        currentResultList.addAll(currentSearch);
      }

    }

    Iterator currentResultsIterator = currentResultList.iterator();

    Vector results = new Vector();

    // generate Datasets from SearchResults

    attributeCache.clear();

    try
    {

      while (currentResultsIterator.hasNext())
      {

        if (System.currentTimeMillis() > endTime) throw new TimeoutException();

        SearchResult currentResult = (SearchResult) currentResultsIterator
            .next();
        Dataset ds = getDataset(currentResult, endTime);
        results.add(ds);
      }
    }
    finally 
    {
      attributeCache.clear();
    }

    results.trimToSize();

    return new QueryResultsList(results);
  }

  /**
   * Escaping nach RFC 2254 Abschnitt 4. 
   * Sternderl werden nicht escapet, weil sie ihre normale Sternerl-Bedeutung
   * beibehalten sollen.
   */  
  private String ldapEscape(String value)
  {
    return value.replaceAll("\\\\", "\\\\5c").replaceAll("\\(", "\\\\28")
        .replaceAll("\\)", "\\\\29").replaceAll("\\00","\\\\00");
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }

  /**
   * generiert einen Schluessel aus einem geordneten(!) Vector der
   * Schluesselwerte
   * @param values
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String generateKey(Map data)
  {
    List keyColumns = new Vector(); 
    Iterator iter = keyAttributes.iterator();
    while (iter.hasNext())
    {
      String keyAttr = (String)iter.next();
      ColumnDefinition colDef = (ColumnDefinition)columnDefinitions.get(keyAttr);
      keyColumns.add(colDef);
    }
    
    //Spalten alphabetisch und nach Pfad-Level sortieren, um einen
    //wohldefinierten Schlüssel zu erhalten, der unabhängig von der Ordnung 
    //der Map ist.
    Collections.sort(keyColumns, new Comparator()
    {
        public int compare(Object o1, Object o2)
        {
          ColumnDefinition colDef1 = (ColumnDefinition)o1;
          ColumnDefinition colDef2 = (ColumnDefinition)o2;
          int comp1 = colDef1.attributeName.compareTo(colDef2.attributeName);
          int comp2 = colDef1.relativePath - colDef2.relativePath;
          int comp = comp1;
          if (comp == 0) comp = comp2;
          return comp;
        }
    });
    
    
    StringBuffer key = new StringBuffer();
    iter = keyColumns.iterator();
    while (iter.hasNext())
    {
      ColumnDefinition colDef = (ColumnDefinition)iter.next();
      if (colDef.relativePath == 0) 
      {
        key.append('(');
        key.append(ldapEscape(colDef.attributeName));
        key.append('=');
        String value = (String)data.get(colDef.columnName);
        if (value == null) value = "*";
        key.append(ldapEscape(value));
        key.append(')');
      }
      
    }
    
    if (key.length() > 0) { key.insert(0,"(&"); key.append(')');}
    
    key.append(KEY_SEPARATOR_0_NON_0_RE);
    
    iter = keyColumns.iterator();
    while (iter.hasNext())
    {
      ColumnDefinition colDef = (ColumnDefinition)iter.next();
      if (colDef.relativePath != 0) 
      {
        key.append(colDef.columnName);
        key.append('=');
        String value = (String)data.get(colDef.columnName);
        if (value == null) value = "";
        key.append(value.replaceAll("\\*","").replaceAll(SEPARATOR,""));
        key.append(SEPARATOR);
      }
    }
    
    return key.toString();
  }
  
  private static class CacheKey
  {
    private static final String SEPARATOR = "/{%§";
    private String hash;
    
    public CacheKey(Name attributePath, String[] searchAttributes)
    {
      StringBuffer buf = new StringBuffer();
      buf.append(searchAttributes.length);
      buf.append(SEPARATOR);
      buf.append(attributePath.toString());
      for (int n = 0; n < searchAttributes.length; ++n)
      {
        buf.append(searchAttributes[n]);
      }
      hash = buf.toString();
    }
    
    public int hashCode()
    {
      return hash.hashCode();
    }
    
    public boolean equals(Object other)
    {
      try{
        CacheKey otherKey = (CacheKey) other;
        return hash.equals(otherKey.hash);
      }catch(Exception e)
      {
        return false;
      }
    }
    
  }
  
  /**
   * vervollständigt SearchResults um Daten aus dem Verzeichnis und gibt ein
   * Dataset zurück
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

    Map relation = new HashMap();

    DirContext ctx = null;
    try{
      Name pathName;
      Name rootName;
      
      try
      {
        
        String tempPath = searchResult.getName();
        
        tempPath = checkQuotes(tempPath);
        
        long timeout = endTime - System.currentTimeMillis();
        if (timeout <= 0) throw new TimeoutException();
        if (timeout > Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;
        setTimeout(properties, timeout);
        Logger.debug2("new InitialLdapContext(properties, null)");
        ctx = new InitialLdapContext(properties, null);
        
        NameParser nameParser = ctx.getNameParser("");
        pathName = nameParser.parse(tempPath);
        rootName = nameParser.parse(baseDN); //TOD0: Das ist eine Konstante, nur einmal berechnen (ausser, dass dies nur mit funktionierender Netzanbindung moeglich ist). Testen mit rausgezogenem Netzkabel
        
      }
      catch (NamingException e)
      {
        throw new TimeoutException(
            "Fehler beim Zugriff auf das LDAP-Verzeichnis.", e);
      }
      
      Iterator columnDefIter = columnDefinitions.entrySet().iterator();
      
      while (columnDefIter.hasNext())
      {
        
        if (System.currentTimeMillis() > endTime) throw new TimeoutException();
        
        Map.Entry columnDefEntry = (Map.Entry)columnDefIter.next();
        ColumnDefinition currentAttribute = (ColumnDefinition)columnDefEntry.getValue();
        
        int relativePath = currentAttribute.relativePath;
        String attributeName = currentAttribute.attributeName;
        
        String value = null;
        
        if (relativePath == 0)
        { // value can be found in the attributes
          
          try
          {
            value = (String) attributes.get(attributeName).get();
          }
          catch (NamingException e)
          {
            // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
          }
          catch (NullPointerException e)
          {
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
          
              attributePath.addAll(pathName.getPrefix(pathName.size()
                  + relativePath));
              
            }
            else
            { // relativePath > 0, Pfad relativ zur Wurzel
              
              attributePath.addAll(pathName.getPrefix(relativePath
                  - rootName.size()));
            }
            
            String[] searchAttributes = { attributeName };
            
            Attributes foundAttributes;
            
            CacheKey key = new CacheKey(attributePath, searchAttributes);
            foundAttributes = (Attributes) attributeCache.get(key);
            
            if (foundAttributes == null)
            {
              foundAttributes = ctx //TOD0: Ist das sinnvoll, nur einzelne Attribute rauszuholen und zu cachen? Wäre es nicht besser, den ganzen Knoten samt aller Attribute zu holen und zu cachen? Das ganze sollte in eine Cache-Klasse ausgelagert werden, an die die Anfrage weitergeleitet wird, so dass dieses Verhalten (wie auch das handeln einer maximalgroesse des Cache) an einem Ort gemanaget wird
              //TODO geschwindikeitsvergleich mit/ohne Cache
              .getAttributes(attributePath, searchAttributes); //TOD0: Ein "searchAttributes" zusammenstellen am Anfang auf Basis des Schemas und überall verwenden, um Suchen einzuschränken
              attributeCache.put(key, foundAttributes);
            }
            
            Attribute foundAttribute = foundAttributes.get(attributeName);
            
            if (foundAttribute != null)
            {
              value = (String) foundAttribute.get();
            }
            
          }
          catch (NamingException e)
          {
            // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
          }
          catch (NullPointerException e)
          {
            // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
          }
          catch (IndexOutOfBoundsException e)
          {
            // auch hier: do nothing (Attributwert befindet sich unterhalb der
            // aktuellen lhmPerson)
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
      
    }finally
    {
      try{ctx.close();} catch(Exception e){}
    }
  }

  /**
   * Sucht im Teilbaum path + BASE_DN nach Knoten, auf die Suchkriterium filter
   * passt.
   * 
   * @param path der Pfad des Startknotens. Wird mit BASE_DN konkateniert.
   * @param filter der Suchfilter.
   * @param searchScope SearchControls.SUBTREE_SCOPE, SearchControls.OBJECT_SCOPE oder
   *                    SearchControls.ONELEVEL_SCOPE, um anzugeben wo gesucht werden soll.
   * @param onlyObjectClass falls true, werden nur Knoten zurückgeliefert, deren objectClass
   *        {@link #objectClass} entspricht.
   * @param endTime wird die Suche nicht vor dieser Zeit beendet, wird eine TimeoutException 
   *        geworfen
   * @return die Suchergebnisse
   * @throws TimeoutException falls die Suche nicht schnell genug abgeschlossen werden
   *         konnte.
   * @author Max Meier (D-III-ITD 5.1)
   *
   */
  private NamingEnumeration searchLDAP(String path, String filter,
      int searchScope, boolean onlyObjectClass, long endTime)
      throws TimeoutException
  {
    Logger.debug("searchLDAP("+path+","+filter+","+searchScope+","+onlyObjectClass+","+endTime+") zum Zeitpunkt "+System.currentTimeMillis());
    
    SearchControls searchControls = new SearchControls();

    searchControls.setSearchScope(searchScope);
    
    long timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException();
    if (timeout > Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;
    searchControls.setTimeLimit((int) timeout);

    if (onlyObjectClass)
    {
      filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    }
    else
    {
      filter = "(&(objectClass=" + "*" + ")" + filter + ")"; //TOD0 das objectClass=* ist doch überflüssig
    }

    DirContext ctx = null;

    NamingEnumeration result;

    try
    {
      setTimeout(properties, timeout);
      Logger.debug2("new InitialLdapContext(properties, null)");
      ctx = new InitialLdapContext(properties, null);

      Logger.debug2("ctx.getNameParser(\"\")");
      NameParser nameParser = ctx.getNameParser("");
      Name name = nameParser.parse(path + baseDN);

      Logger.debug2("ctx.search("+name+","+filter+",searchControls) mit Zeitlimit "+searchControls.getTimeLimit());
      result = ctx.search(name, filter, searchControls);
      Logger.debug2("ctx.search() abgeschlossen");

    }
    catch (TimeLimitExceededException e)
    {
      throw new TimeoutException(e);
    }
    catch (NamingException e)
    {
      throw new TimeoutException(e);
    }
    finally
    {
      try{ctx.close();}catch (Exception e){}
    }

    return result;
  }

  /**
   * Durchsucht die Nachfahren des durch path + BASE_DN bezeichneten Knotens mit Abstand
   * level zu diesem Knoten nach Knoten, die auf die Suchanfrage filter passen.
   * Es werden nur Objekte mit objectClass = {@link #objectClass} geliefert.
   * @return eine List von {@link SearchResult}s.
   * @throws TimeoutException falls die Anfrage nicht vor endTime beantwortet werden konnte.
   * @author Max Meier (D-III-ITD 5.1)
   * 
   */
  private List searchLDAPLevel(String path, String filter, int level,
      long endTime) throws TimeoutException
  {

    List seeds = new Vector();
    seeds.add(path);

    String comma = ",";

    for (int n = 0; n < (level - 1); n++)
    {
      if (System.currentTimeMillis() > endTime) throw new TimeoutException();

      List nextSeeds = new Vector();

      for (int m = 0; m < seeds.size(); m++) //TOD0 Iterator verwenden
      {

        if (System.currentTimeMillis() > endTime) throw new TimeoutException();

        String searchPath = (String) seeds.get(m);

        comma = ",";
        if (searchPath.equals("")) comma = "";

        NamingEnumeration enumer = searchLDAP(
            searchPath + comma,
            "",
            SearchControls.ONELEVEL_SCOPE,
            false,
            endTime);

        while (enumer.hasMoreElements())
        {

          if (System.currentTimeMillis() > endTime)
            throw new TimeoutException();

          SearchResult currentResult = (SearchResult) enumer.nextElement();
          String subPath = checkQuotes(currentResult.getName());
          comma = ",";
          if (subPath.equals("")) comma = "";
          String currentPath = subPath + comma + searchPath;
          nextSeeds.add(currentPath);
        }
      }

      seeds = nextSeeds;

    }

    List result = new Vector();

    for (int n = 0; n < seeds.size(); n++) //TOD0 Iterator verwenden
    {

      if (System.currentTimeMillis() > endTime) throw new TimeoutException();

      String currentPath = (String) seeds.get(n);

      comma = ",";
      if (currentPath.equals("")) comma = "";

      NamingEnumeration enumer = searchLDAP(
          currentPath + comma,
          filter,
          level == 0? SearchControls.OBJECT_SCOPE:SearchControls.ONELEVEL_SCOPE,
          true,
          endTime);

      while (enumer.hasMoreElements())
      {
        if (System.currentTimeMillis() > endTime) throw new TimeoutException();
        SearchResult sr = (SearchResult)enumer.nextElement();
        String name = checkQuotes(sr.getName());
        String actualPath = name + (name.length()>0?comma:"") + currentPath;
        sr.setName(actualPath);
        result.add(sr);
      }

    }

    return result;
  }

  /**
   * Entferne umschliessende Doublequotes aus path falls vorhanden.
   * Dies muss gemacht werden, da das Zeichen '/' in LDAP Pfadkomponenten 
   * erlaubt ist, im JNDI jedoch als Komponententrenner verwendet wird. 
   * Deswegen
   * werden Pfade, die '/' enthalten von .getName() in Anführungszeichen
   * gesetzt und können deshalb nicht mehr geparsed werden.
   *
   * TOD0 Ich bin mir nicht sicher, ob hier nicht noch mehr zu tun ist.
   * Was ist z.B. mit enthaltenen Doublequotes? Kann das passieren?
   * Wie werden die escapet?
   * @author Max Meier (D-III-ITD 5.1)
   * 
   */
  private String checkQuotes(String path)
  {
    
    int tempEnd = path.length() - 1;
    if (tempEnd > 0)
    {
      if (path.charAt(0) == '"' && path.charAt(tempEnd) == '"')
      {
        path = path.substring(1, tempEnd);
      }
    }

    return path;

  }

  private String errorMessage()
  {
    return "Fehler in Definition von Datenquelle " + name + ": ";
  }

  // LDAPDataset
  private class LDAPDataset implements Dataset
  {

    private String key;

    private Map relation;

    LDAPDataset(String key, Map relation)
    {
      this.key = key;
      this.relation = relation;
    }

    public String get(java.lang.String columnName)
        throws ColumnNotFoundException
    {
      if (!schema.contains(columnName)) throw new ColumnNotFoundException();

      return (String) relation.get(columnName);
    }

    public String getKey()
    {
      return key;
    }

  }

  
  // TESTFUNKTIONEN

  /**
   * Gibt results aus.
   * 
   * @param query
   *          ein String der in die Überschrift der Ausgabe geschrieben wird,
   *          damit der Benutzer sieht, was er angezeigt bekommt.
   * @param schema
   *          bestimmt, welche Spalten angezeigt werden von den Datensätzen aus
   *          results.
   * @param results
   *          die Ergebnisse der Anfrage.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printResults(String query, Set schema, QueryResults results)
  {
    System.out.println("Results for query \"" + query + "\":");
    Iterator resIter = results.iterator();
    while (resIter.hasNext())
    {
      Dataset result = (Dataset) resIter.next();

      Iterator spiter = schema.iterator();
      while (spiter.hasNext())
      {
        String spalte = (String) spiter.next();
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
        {
        }
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
    List query = new Vector();
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
    List query = new Vector();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    QueryResults find = find(query, 3000000);
    return find;
  }

  /**
   * 
   * @param args
   * @throws IOException
   * @throws SyntaxErrorException
   * @throws NodeNotFoundException
   * @throws TimeoutException
   * @throws ConfigurationErrorException
   * @author Max Meier (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException,
      SyntaxErrorException, NodeNotFoundException, TimeoutException,
      ConfigurationErrorException
  {
    File curDir = new File(System.getProperty("user.dir"));
    URL context = curDir.toURL();
    URL confURL = new URL(context, "testdata/ldap.conf");
    ConfigThingy ldapConf = new ConfigThingy("", confURL);
    Map nameToDatasource = new HashMap();
    ConfigThingy sourceDesc = ldapConf.query("Datenquelle").getFirstChild();
    LDAPDatasource source = new LDAPDatasource(nameToDatasource, sourceDesc,
        context);

    // Test keys
    System.out.println("Schlüssel für Anfrage OrgaEmail=direktorium@muenchen.de:");
    QueryResults qr = source.simpleFind(
        "OrgaEmail",
        "direktorium@muenchen.de");
    Iterator iter = qr.iterator();

    Collection keys = new Vector();

    while (iter.hasNext())
    {
      Dataset ds = (Dataset) iter.next();
      String key = ds.getKey();
      System.out.println("Key: " + ds.getKey());

      keys.add(key);

    }

    QueryResults qr2 = source.getDatasetsByKey(keys, 3000000);

    printResults("Datensätze zu vorigen Schlüsseln: ", source.schema, qr2);

    printResults("OrgaEmail =  direktorium@muenchen.de , Gertraud = Gertraud", source
        .getSchema(), source.simpleFind(
        "OrgaEmail",
        "direktorium@muenchen.de",
        "Gertraud",
        "Gertraud"));
    
    printResults("OrgaEmail = linux-client.it.dir@muenchen.de, Referat = Direktorium", source
        .getSchema(), source.simpleFind(
        "OrgaEmail",
        "linux-client.it.dir@muenchen.de",
        "Referat",
        "Direktorium"));
    
    printResults("Gertraud = Gertraud, Referat = Direktorium", source
        .getSchema(), source.simpleFind(
        "Gertraud",
        "Gertraud",
        "Referat",
        "Direktorium"));
    
    printResults("OrgaKurz = D-L, UberOrga = d", source
        .getSchema(), source.simpleFind(
        "OrgaKurz",
        "D-L",
        "UberOrga",
        "d"));
    
    printResults("UberOrga = d", source
        .getSchema(), source.simpleFind(
        "UberOrga",
        "d"));
    
    printResults(
        "Orga2 = Stadtarchiv , Referat = Direktorium",
        source.getSchema(),
        source.simpleFind("Orga2", "Stadtarchiv", "Referat", "Direktorium"));
    
    printResults(
        "Referat = Sozialreferat , Nachname = Me\\)er",
        source.getSchema(),
        source.simpleFind("Referat", "Sozialreferat", "Nachname", "Me\\)er"));

    // printResults("Nachname =r*", dj.getSchema(),
    // dj.simpleFind("Nachname","r*"));

    printResults("Nachname = *utz", source.getSchema(), source.simpleFind(
        "Nachname",
        "*utz"));
    printResults("Nachname = *oe*", source.getSchema(), source.simpleFind(
        "Nachname",
        "*oe*"));
    printResults("Nachname = Lutz", source.getSchema(), source.simpleFind(
        "Nachname",
        "Lutz"));
    printResults("Nachname = *utz, Vorname = Chris*", source.getSchema(), source
        .simpleFind("Nachname", "Lutz", "Vorname", "Chris*"));
    printResults("Nachname = Benkmann, Vorname = Matthias", source.getSchema(), source
        .simpleFind("Nachname", "Benkmann", "Vorname", "Matthias"));

  }

}
