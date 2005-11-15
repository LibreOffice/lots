/*
* Dateiname: LDAPDatasource.java
* Projekt  : WollMux
* Funktion : Verschafft zugriff auf LDAP-Verzeichnisdienst als Datasource.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.11.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Max Meier (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.directory.*;
import javax.naming.*;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datasource
 * Zugriff auf ein LDAP-Verzeichnis
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
 
  // properties für die Verbindung zum LDAP-Server
  private Properties properties = new Properties();
  
  // Separator zur Schluesselerzeugung aus mehreren Schluesselwerten
  private final static String SEPARATOR = "&:=&:%";
  
  // Map von query-Strings auf LDAP-Attributnamen
  private Map nameMap = new HashMap();
  
  // Nameparser
  private NameParser nameParser;
  
  // Key-Attribute (LDAP)
  private List keyAttributes = new Vector();
  private int keyStatus; // 0:= nur absolute Attribute, 1:= absolute und relative Attribute, 2:= nur relative Attribute
  
  // regex für erlaubte Bezeichner
  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  
  /**
   * Erzeugt eine neue LDAPDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser LDAPDatasource bereits vollständig instanziierten
   *        Datenquellen (zur Zeit nicht verwendet).
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser LDAPDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen
   *        (zur Zeit nicht verwendet).
   * @throws ConfigurationErrorException falls in der Definition in
   *         sourceDesc ein Fehler ist.
   */
  public LDAPDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    
    name = "Unbenannt";
    try{ name = sourceDesc.get("NAME").toString();} catch(NodeNotFoundException x){}
    
    try {
      url = sourceDesc.get("URL").toString();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException( errorMessage() + "URL des LDAP-Servers fehlt.");
    }
    
    try {
      baseDN = sourceDesc.get("BASE_DN").toString();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException( errorMessage() + "BASE_DN des LDAP-Servers fehlt.");
    }
    
    try {
      objectClass = sourceDesc.get("OBJECT_CLASS").toString();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException( errorMessage() + "Keine OBJECT_CLASS definiert.");
    }
    
    
    // set properties
    properties.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
    properties.put(Context.PROVIDER_URL,url); // + "/" + baseDN);

    ConfigThingy spalten = sourceDesc.query("Spalten");
    
    if (spalten.count() == 0)
      throw new ConfigurationErrorException( errorMessage() + "Abschnitt 'Spalten' fehlt.");
    
    schema = new HashSet();
    
    Iterator iter = spalten.iterator();
    
    // iteriere über alle Spalten-Relationen
    while (iter.hasNext()) {
      
      ConfigThingy spaltenDesc = (ConfigThingy)iter.next();
      Iterator iter2 = spaltenDesc.iterator();
      
      // iteriere über eine Spalten-Relation
      while (iter2.hasNext()) {
        
        ConfigThingy spalteDesc = (ConfigThingy)iter2.next();
        
        String spalte;
        int relativePath;
        String attributeName;
        String objectClass = null;
        String lineSeparator = null;
        
        try {
          
          spalte = spalteDesc.get("DB_SPALTE").toString();
          
          if (!SPALTENNAME.matcher(spalte).matches())
            throw new ConfigurationErrorException( errorMessage() +  "Spalte \""+spalte+"\" entspricht nicht der Syntax eines Bezeichners"); 
          
          String path = spalteDesc.get("PATH").toString();
          
          // get relativePath and attributeName
          String[] splitted = path.split(":");
          
          if (splitted.length!=2) throw new ConfigurationErrorException( errorMessage() + "Syntaxerror bei Pfadangabe von " + spalte);
          
          try {
            relativePath = Integer.parseInt(splitted[0]);
          } catch (NumberFormatException e) {
            throw new ConfigurationErrorException( errorMessage() + "Syntaxerror bei Angabe des relativen Pfads von " + spalte);
          } 
          
          attributeName = splitted[1];
          
          try {
            objectClass = spalteDesc.get("OBJECT_CLASS").toString();
          } catch (NodeNotFoundException x) {
            // do nothing... (Angabe von OBJECT_CLASS optional)
          }
          
          try {
            lineSeparator = spalteDesc.get("LINE_SEPARATOR").toString();
          } catch (NodeNotFoundException x) {
            // do nothing... (Angabe von LINE_SEPARATOR optional)
          }
          
        } catch(NodeNotFoundException x) {
          throw new ConfigurationErrorException( errorMessage() + "DB_SPALTE Angabe fehlt");
        }
        
        LDAPAttribute currentPath = new LDAPAttribute(relativePath, attributeName);
        if(objectClass!=null) {
          currentPath.objectClass = objectClass;
        }
        if(lineSeparator!=null) {
          currentPath.lineSeparator = lineSeparator;
        }
        nameMap.put(spalte,currentPath);
        schema.add(spalte);
      }  
    }
    
    // Key-Attribute
    ConfigThingy keys = sourceDesc.query("Schluessel");
    
    if (keys.count()==0)
      throw new ConfigurationErrorException( errorMessage() + "Schluesselfeld fehlt.");
    
    ConfigThingy keySpalten;
    
    try {
      // der letzte definierte Schluessel wird verwendet
      keySpalten = keys.getLastChild();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException( errorMessage() + "Syntaxerror bei Schluesseldefinition.");
    }
    
    Iterator keyIterator = keySpalten.iterator();
    
    boolean onlyRelative = true;
    boolean onlyAbsolute = true;
    
    // speichere die Schluesselattribute
    while(keyIterator.hasNext()) { 
      String currentName = keyIterator.next().toString();
      
      LDAPAttribute currentKeyLDAPAttribute = (LDAPAttribute) nameMap.get(currentName);
      
      // ist Schluesselattribut vorhanden?
      if (currentKeyLDAPAttribute==null) throw new ConfigurationErrorException( errorMessage() + "Schluesselelement nicht definiert.");
      
      if (currentKeyLDAPAttribute.relativePath != 0) {
        onlyAbsolute = false;
      }
      if (currentKeyLDAPAttribute.relativePath == 0) {
        onlyRelative = false;
      }
      
      keyAttributes.add(currentName);
    }
   
    
    if (onlyAbsolute) keyStatus = 0;
    else if (onlyRelative) keyStatus = 2;
    else keyStatus = 1;
    
    if (keyAttributes.size()==0)
      throw new ConfigurationErrorException(errorMessage() + "Kein Schluesseleleement angegeben.");
    
  }
  
  // repräsentiert ein LDAP-Attribut
  private class LDAPAttribute {
    
    /*
     * relativer Pfad:
     * 0 := Attribut befindet sich im selben Knoten
     * negativ := relative Pfadangabe "nach oben" vom aktuellen Knoten aus
     * positiv :=  relative Pfadangabe von der Wurzel aus
     */
    int relativePath;
    
    // Attributname im LDAP
    String attributeName;
    
    // exklusive objectClass
    String objectClass;
    
    // line separator
    String lineSeparator;
    
    
    LDAPAttribute(int relativePath, String attributeName) {
      this.relativePath = relativePath;
      this.attributeName = attributeName;
    }
    
  }
  
  // LDAPDataset
  private class LDAPDataset implements Dataset {
    
    private String key;
    private Map relation;
    
    
    LDAPDataset(String key, Map relation) {
      this.key = key;
      this.relation = relation;
    }
    
    public String get(java.lang.String columnName) throws ColumnNotFoundException {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException();
      
      return (String) relation.get(columnName);
    }
    
    public String getKey() {
      return key;
    }
    
  }
  
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  public Set getSchema()
  {
    return schema;
  }

  
  private List keyToFindQuery(String key)
  {
    String[] keyValues = key.split(SEPARATOR);
    
    List query = new Vector(keyValues.length);
    
    for (int n=0; n<keyValues.length; n++)
      query.add(new QueryPart((String)keyAttributes.get(n),keyValues[n]));
      
    return query;
  }
 

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException {
    
    long endTime = System.currentTimeMillis() + timeout;
    
    if (keyStatus == 0 | keyStatus == 1) { // absolute Attribute vorhanden
      
      Iterator iter = keys.iterator();
      
      // build searchFilter
      String searchFilter = "";
      
      boolean first = true;
      while(iter.hasNext()) {
        
        String currentKey = (String) iter.next();

        String currentSearchFilter = keyToQuery(currentKey);
        
        
        if (first) {
          searchFilter = currentSearchFilter;
          first = false;
        } else {
          searchFilter = "(|" + currentSearchFilter + searchFilter + ")";
        }

      }
      
      
      // search LDAP
      NamingEnumeration currentResults = searchLDAPPerson("", searchFilter, true, true,timeout);
      
      Vector results = new Vector();
      
      while(currentResults.hasMoreElements()) {
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        try
        {
          SearchResult currentResult = (SearchResult) currentResults.next();
          Dataset dataset = getDataset(currentResult,endTime);
          if (keyStatus==0) {
            results.add(dataset);
          } else if (keys.contains(dataset.getKey())) {
            results.add(dataset);
          }
        }
        catch (NamingException e)
        {
          Logger.error("Error in LDAP-Directory.",e);
        }
        
          
      }

      //results.trimToSize();
      
      return new QueryResultsList(results);  
      
    }
    
    if (keyStatus == 2) { // nur relative Attribute
      List results = new Vector(keys.size());
      
       Iterator iter = keys.iterator();
       while (iter.hasNext())
       {
         if (System.currentTimeMillis()>endTime) throw new TimeoutException();
         List query = keyToFindQuery((String)iter.next());
         QueryResults res = find(query,endTime);
         Iterator iter2 = res.iterator();
         while (iter2.hasNext()) {
           if (System.currentTimeMillis()>endTime) throw new TimeoutException();
           results.add(iter2.next());
         }
  
       }
      return new QueryResultsList(results);
    }
     
    return null;
  }
  
  private class RelativePaths {
    
    private int relative;
    private List paths;
    
    RelativePaths(int relative, List paths) {
      this.relative = relative;
      this.paths = paths;
    }
    
  }
  
  private class RelativePath {
    
    private int relative;
    private Name name;
    
    RelativePath(int relative, Name name) {
      this.relative = relative;
      this.name = name;
    }
  
  }
  
  private RelativePaths getPaths(String filter, int pathLength, long timeout, long endTime) throws TimeoutException {
    
    Vector paths;
    
    try
    {
      DirContext ctx = new InitialLdapContext(properties,null);
      NameParser np = ctx.getNameParser("");
      int rootSize = np.parse(baseDN).size();
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
      
      if (timeout>Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;
      sc.setTimeLimit((int)timeout);

      NamingEnumeration enumer = ctx.search(baseDN,filter,sc);
      
      paths = new Vector();
      
      while(enumer.hasMoreElements()) {
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        SearchResult result = (SearchResult) enumer.nextElement();
        String path = checkQuotes(result.getName());
        Name pathName = np.parse(path);
        if (pathName.size()+rootSize==pathLength | pathLength<0) paths.add(pathName);
      }
      
    }
    catch (NamingException e)
    {
      throw new TimeoutException("Internal error in LDAP.",e);
    }
    
    return new RelativePaths(pathLength,paths);
    
  }
  
  

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List query, long timeout) throws TimeoutException {
    
    long endTime = System.currentTimeMillis() + timeout;
    
    Iterator iter = query.iterator();
    
    String searchFilter = "";
    List positiveSubtreePathLists = new Vector();

    List negativeSubtreePathLists = new Vector();
    
    Map attributeMap = new HashMap();
    
    boolean first = true;
    while (iter.hasNext()) {
      
      if (System.currentTimeMillis()>endTime) throw new TimeoutException();
      
      QueryPart currentQuery = (QueryPart) iter.next();
      
      LDAPAttribute ldapAttribute = (LDAPAttribute) nameMap.get(currentQuery.getColumnName());
      
      if (ldapAttribute==null) continue;
      
      String attributeName = ldapAttribute.attributeName;
      int relativePath = ldapAttribute.relativePath;
      String attributeValue = currentQuery.getSearchString();
      
      String objectClass = ldapAttribute.objectClass;
      String currentSearchFilter = "(" + attributeName + "=" + attributeValue + ")";
      if (objectClass!=null) {
        currentSearchFilter = "(&" + currentSearchFilter + "(objectClass=" + objectClass + "))";
      }

      if (relativePath==0) { // edit filter   
        if (first) {
          searchFilter = currentSearchFilter;
          first = false;
        } else {
          searchFilter = "(&" + currentSearchFilter + searchFilter + ")";
        }
      } else { // edit searchFilters for subtree searches:
        
        Integer key = new Integer(relativePath);
        
        String formerSearchPath = (String) attributeMap.get(key);
        
        if (formerSearchPath==null) {
          formerSearchPath = currentSearchFilter;
        } else {
          formerSearchPath = "(&" + currentSearchFilter + formerSearchPath + ")";
        }
        
        attributeMap.put(key,formerSearchPath);
        
      }
      
    }
    
    Iterator attributeKeys = attributeMap.keySet().iterator();
    
    while (attributeKeys.hasNext()) {
      Integer currentKey = (Integer) attributeKeys.next();
      
      int relativePath = currentKey.intValue();
      
      String pathFilter = (String) attributeMap.get(currentKey);
      
      RelativePaths paths = getPaths(pathFilter,relativePath,timeout,endTime);
      
      if (relativePath>0) {
        positiveSubtreePathLists.add(paths);
      } else {
        List negativeSubtreePaths = new Vector();
        for (int n=0; n<paths.paths.size(); n++) {
          Name currentName = (Name)paths.paths.get(n);
          RelativePath newNegativePath = new RelativePath(paths.relative,currentName);
          negativeSubtreePaths.add(newNegativePath);
        }
        negativeSubtreePathLists.add(negativeSubtreePaths);
        
      }
      
    }
    
    /* bilde die Schnittmenge aller angegebenen positiv relativen Pfade
     * 
     * Dieser Algorithmus vergleicht zwei Listen, die jeweils
     * alle relevanten Pfade eines Suchattributs repräsentieren.
     * Zuerst werden die Listen nach der Länge der enthaltenen Pfade
     * sortiert, danach wird betrachte, ob für jedes Element der Liste
     * der längeren Pfade ein Element aus der Liste der kürzeren Pfade existiert,
     * dass ein Prefix des ersten Elements ist.
     * Wenn ja, wird das Element in die Liste mergedPositiveSubtreePathLists aufgenommen
     * und somit weiter betrachtet, wenn nein, ist die Schnittmengeneigenschaft
     * nicht gegeben und der Pfad wird verworfen.
    */
    List mergedPositiveSubtreePathLists = null;

    int mergedCurrentSize = 0;
    
    if (positiveSubtreePathLists.size()>0) {
      RelativePaths currentSubtreePaths = (RelativePaths) positiveSubtreePathLists.get(0);
      mergedPositiveSubtreePathLists = currentSubtreePaths.paths;
      mergedCurrentSize = currentSubtreePaths.relative;
    }
    
    for (int n=1; n<positiveSubtreePathLists.size(); n++) {
      
      RelativePaths currentSubtreePaths = (RelativePaths) positiveSubtreePathLists.get(n);
      
      List shorter, longer;
          
      if (currentSubtreePaths.relative<mergedCurrentSize) {
        shorter = currentSubtreePaths.paths;
        longer = mergedPositiveSubtreePathLists;
      } else {
        shorter = mergedPositiveSubtreePathLists;
        longer = currentSubtreePaths.paths;
        mergedCurrentSize = currentSubtreePaths.relative;
      }
      
      mergedPositiveSubtreePathLists = new Vector();
      
      for (int m=0; m<longer.size(); m++) {
        Name longerName = (Name) longer.get(m);
        
        for (int p=0; p<shorter.size(); p++) {
          Name shorterName = (Name) shorter.get(p);
          if (longerName.startsWith(shorterName)) {
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
    if (negativeSubtreePathLists.size()>0) {
      mergedNegativeList = (List) negativeSubtreePathLists.get(0);
    }

    for (int n=1; n<negativeSubtreePathLists.size(); n++) {
      
      List newMergedNegativeList = new Vector();
      

      List currentList = (List) negativeSubtreePathLists.get(n);

      for (int m=0; m<mergedNegativeList.size(); m++) {
        RelativePath currentPath = (RelativePath) mergedNegativeList.get(m);
        
        for (int p=0; p<currentList.size(); p++) {
          RelativePath otherPath = (RelativePath) currentList.get(p);
          
          RelativePath shorter, longer;
          
          if (currentPath.name.size()<otherPath.name.size()) {
            shorter = currentPath;
            longer = otherPath;
          } else {
            shorter = otherPath;
            longer = currentPath;
          }
          
          if (currentPath.name.size()-currentPath.relative == otherPath.name.size()-otherPath.relative && longer.name.startsWith(shorter.name)) {
            newMergedNegativeList.add(longer);
          }
          
        }
      }
      
      mergedNegativeList = newMergedNegativeList;
      
       
    }
    
    /*
     * bilde die Schnittmenge aus den positiv und negativ relativen Listen
     */
    List mergedNegativeSubtreePaths;
    if (mergedPositiveSubtreePathLists!=null && mergedNegativeList!=null) {
      
      mergedNegativeSubtreePaths = new Vector();
      
      for (int n=0; n<mergedNegativeList.size();n++) {
        RelativePath currentPath = (RelativePath) mergedNegativeList.get(n);
        
        for (int m=0; m<mergedPositiveSubtreePathLists.size(); m++) {
          Name currentName = (Name) mergedPositiveSubtreePathLists.get(m);
          
          if (currentPath.name.size()<currentName.size()) {
            
            if (currentName.startsWith(currentPath.name) && currentPath.name.size()-currentPath.relative>=currentName.size()) {
              mergedNegativeSubtreePaths.add(currentPath);
            }
            
          } else {
            
            if(currentPath.name.startsWith(currentName)) {
              mergedNegativeSubtreePaths.add(currentPath);
            }
            
          }
          
          
        }
      }
    } else {
      mergedNegativeSubtreePaths = mergedNegativeList;
    }
     
    
    if (searchFilter.equals("")&mergedPositiveSubtreePathLists==null&mergedNegativeSubtreePaths==null) return new QueryResultsList(new Vector(0));
    
    List positiveSubtreeStrings = new Vector();
    
    // create Strings from Names
    if (positiveSubtreePathLists.size()==0) {
      positiveSubtreeStrings.add("");
    } else {
      
      for (int n=0; n<mergedPositiveSubtreePathLists.size(); n++) {
        Name currentName = (Name) mergedPositiveSubtreePathLists.get(n);
        positiveSubtreeStrings.add(currentName.toString());
      }
      
    }
    
    
    Iterator subtreeIterator = positiveSubtreeStrings.iterator();
    
    List currentResultList = new Vector();
    
    
    if (negativeSubtreePathLists.size()==0) {
      // allgemeine Suche
      
      while(subtreeIterator.hasNext()) {
        
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        
        String subTree = (String)subtreeIterator.next();
        String comma = ",";
        if (subTree.equals("")) comma = "";
        NamingEnumeration currentResults = searchLDAPPerson(subTree + comma,searchFilter,true, true,timeout);
        
        while(currentResults.hasMoreElements()) {
          SearchResult sr = (SearchResult) currentResults.nextElement();
          sr.setName(checkQuotes(sr.getName()) + comma + subTree);
          currentResultList.add(sr);
        }
        
      } 
    } else {
      // Breitensuche muss verwendet werden
      
      for (int n=0; n<mergedNegativeSubtreePaths.size(); n++) {
        
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        
        RelativePath currentRelativePath = (RelativePath) mergedNegativeSubtreePaths.get(n);
        int depth = -currentRelativePath.relative;

        Name currentName = currentRelativePath.name;
        String currentPath = currentName.toString();
        List currentSearch = bfsSearchLDAPPerson(currentPath,searchFilter,depth,endTime);
        String comma = ",";
        if (currentPath.equals("")) comma = "";
        
        for (int m=0; m<currentSearch.size(); m++) {
          SearchResult sr = (SearchResult) currentSearch.get(m);
          String actualPath = checkQuotes(sr.getName()) + comma + currentPath;
          sr.setName(actualPath);
          currentResultList.add(sr);
        }
          
      }
      
      
    }
    
    Iterator currentResultsIterator = currentResultList.iterator();
    
    
    Vector results = new Vector();
    
    // generate Datasets from SearchResults
    try
    {
      
        while(currentResultsIterator.hasNext()) {
          
          if (System.currentTimeMillis()>endTime) throw new TimeoutException();
          
          SearchResult currentResult = (SearchResult) currentResultsIterator.next();     
          Dataset ds = getDataset(currentResult,endTime);
          results.add(ds);
        }
    }
    catch (TimeoutException e) {
      throw new TimeoutException(e);
    }
   
    
    results.trimToSize();
       
    return new QueryResultsList(results);
  }
  

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }
  
  
  // generiert einen Schluessel aus einem geordneten(!) Vector der Schluesselwerte
  private String generateKey(List values) {
    
    String key = "";
    
    for (int n=0; n<values.size(); n++) {
      if (n==0) {
        Object value = values.get(n);
        if (value==null) {
          key = "";
        }
        else {
          key = (String) values.get(n);
        }
      } else {
        key = key + SEPARATOR + (String) values.get(n);
      }
    }
    
    return key;
  }
  
  
  // generiert einen Suchausdruck aus einem Schluessel
  private String keyToQuery(String key) {

    String[] keyValues = key.split(SEPARATOR);
    
    String query = "";   
    boolean first = true;
    for (int n=0; n<keyValues.length; n++) {

      LDAPAttribute currentAttribute = (LDAPAttribute) nameMap.get(keyAttributes.get(n));
      
      if (currentAttribute.relativePath==0) {
        
        String attributeName = currentAttribute.attributeName;
        String objectClass = currentAttribute.objectClass;
        
        String currentSearchFilter = "(" + attributeName + "=" + keyValues[n] + ")";
        
        if (objectClass!=null) {
          currentSearchFilter = "(&" + currentSearchFilter + "(objectClass=" + objectClass +"))"; 
        }
        
        if (first) {  
          query = currentSearchFilter;
          first = false;
        } else {
          query = "(&" + currentSearchFilter + query + ")";
        }
        
      }
    }
    return query;
  }
  
  
  // vervollständigt SearchResults um Daten aus dem Verzeichnis und gibt ein Dataset zurück
  private Dataset getDataset(SearchResult searchResult, long endTime) throws TimeoutException {
    
    Attributes attributes = searchResult.getAttributes();
    
    
    Hashtable relation = new Hashtable();
    
    DirContext ctx = null;
    Name pathName;
    Name rootName;
    
    try {
      
      String tempPath = searchResult.getName();
      
      tempPath = checkQuotes(tempPath);
         
      ctx = new InitialLdapContext(properties,null);
      
      NameParser nameParser = ctx.getNameParser("");
      pathName = nameParser.parse(tempPath);
      rootName = nameParser.parse(baseDN);
      
    } catch (NamingException e) {
      try {
        ctx.close();
      } catch (NamingException e2) {}
      throw new TimeoutException("Fehler beim Zugriff auf das LDAP-Verzeichnis.",e);
    }
    
    Iterator keys = nameMap.keySet().iterator();
    
    while(keys.hasNext()) {
      
      if (System.currentTimeMillis()>endTime) throw new TimeoutException();
      
      String currentKey = (String) keys.next();
      LDAPAttribute currentAttribute = (LDAPAttribute) nameMap.get(currentKey);
      
      int relativePath = currentAttribute.relativePath;
      String attributeName = currentAttribute.attributeName;
      
      String value = null;
      
      if (relativePath==0) { // value can be found in the attributes
        
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

        
      } else { // value is stored somewhere else in the directory
        
        
        Name attributePath = (Name) rootName.clone();
        
        try {
        
          if (relativePath < 0) { // Pfad relativ zum aktuellen Element
            
            attributePath.addAll(pathName.getPrefix(pathName.size()+relativePath));

          } else { // relativePath > 0, Pfad relativ zur Wurzel
            
            attributePath.addAll(pathName.getPrefix(relativePath-rootName.size()));
            
          }        
              
          String[] searchAttributes = { attributeName }; 
          Attributes foundAttributes = ctx.getAttributes(attributePath,searchAttributes);
          Attribute foundAttribute = foundAttributes.get(attributeName); 
          
          if (foundAttribute!=null) {
            value = (String) foundAttribute.get();
          }  
          
        } catch (NamingException e) {
           // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
        }
        catch (NullPointerException e)
        {
          // do nothing (Attributwert nicht vorhanden und bleibt somit 'null')
        }
        catch (ArrayIndexOutOfBoundsException e) {
          // auch hier: do nothing (Attributwert befindet sich unterhalb der aktuellen lhmPerson)
        }

      }

      if (value!=null) {
        String lineSeparator = currentAttribute.lineSeparator;
        if (lineSeparator!=null) {
          value = value.replaceAll(lineSeparator,"\n");
        }
        relation.put(currentKey, value);
      }
    }
    
    // generate Key
    Vector keyValues = new Vector();
    
    for (int n=0; n<keyAttributes.size(); n++) {
      String currentValue = (String) relation.get(keyAttributes.get(n));
      keyValues.add(currentValue);
    }
    
    String key = generateKey(keyValues);
    
    try {
      ctx.close();
    } catch (NamingException e) {}
    
    return new LDAPDataset(key, relation);
  }  

  
  /* allgemeine Suche im Directory
   * 
   */
  private NamingEnumeration searchLDAPPerson(String path, String filter, boolean subTreeScope, boolean onlyObjectClass, long timeout) throws TimeoutException {
    
    
    SearchControls searchControls = new SearchControls();
    
    if (subTreeScope) {
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    } else {
      searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    }
    
    if (timeout>Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;
    searchControls.setTimeLimit((int) timeout);
    
    if (onlyObjectClass) {
      filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    } else {
      filter = "(&(objectClass=" + "*" + ")" + filter + ")";
    }
    
    DirContext ctx = null;
    
    NamingEnumeration result;
    
    try {
      
      ctx = new InitialLdapContext(properties,null);
      
      NameParser nameParser = ctx.getNameParser("");
      Name name = nameParser.parse(path + baseDN);
      
      result = ctx.search(name,filter,searchControls);

    
    } catch (TimeLimitExceededException e) {
      throw new TimeoutException(e);
    } catch (NamingException e) {
      throw new TimeoutException(e);
    } finally {
      if (ctx!=null)
      try {
        ctx.close();
      } catch (Exception e) {}
    }
 
    return result;
  }
  
  /* Breitensuche im Directory
   * Sucht alle LDAPPersons, die auf dem angegebenen Level über dem angegebenen Pfadnamen liegen.
  */
  private List bfsSearchLDAPPerson(String path, String filter, int level, long endTime) throws TimeoutException {
    
    List seeds = new Vector();
    seeds.add(path);
    
    String comma = ",";
    
    
    for (int n=0; n<(level-1); n++) {
      
      if (System.currentTimeMillis()>endTime) throw new TimeoutException();
      
      List nextSeeds = new Vector();
      
      for (int m=0; m<seeds.size(); m++) {
        
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        
        String searchPath = (String)seeds.get(m);
        
        comma = ",";
        if (searchPath.equals("")) comma = "";
      
        NamingEnumeration enumer = searchLDAPPerson(searchPath + comma,"",false,false,endTime);
        
        while (enumer.hasMoreElements()) {
          
          if (System.currentTimeMillis()>endTime) throw new TimeoutException();
          
          SearchResult currentResult = (SearchResult)enumer.nextElement();
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
    
    for (int n=0; n<seeds.size(); n++) {
      
      if (System.currentTimeMillis()>endTime) throw new TimeoutException();
      
      String currentPath = (String)seeds.get(n);
      
      comma = ",";
      if (currentPath.equals("")) comma = "";
      
      NamingEnumeration enumer = searchLDAPPerson(currentPath + comma,filter,false,true,endTime);
      
      while (enumer.hasMoreElements()) {
        if (System.currentTimeMillis()>endTime) throw new TimeoutException();
        result.add(enumer.nextElement());
      }
      
    }
    
    return result;
  }
  
  
  private String checkQuotes(String tempPath) {
    /*
     * delete surrounding quotation marks
     * 
     * Der Character '/' ist in LDAP erlaubt, jedoch nicht im JNDI.
     * Deswegen werden Pfade, die '/' enthalten von .getName() in Anführungszeichen
     * gesetzt und können deshalb nicht mehr geparsed werden. 
     */
    int  tempEnd = tempPath.length() - 1; 
    if (tempEnd>0) {
      if (tempPath.charAt(0) == '"' && tempPath.charAt(tempEnd) == '"') {
            tempPath = tempPath.substring(1,tempEnd);
      }
    }
    
    return tempPath;
       
  }
  

  private String errorMessage() {
    return "Fehler in Definition von Datenquelle " + name + ": ";
  }
  
  
  
  

  
  
  
  // TESTFUNKTIONEN

  /**
   * Gibt results aus. 
   * @param query ein String der in die Überschrift der Ausgabe geschrieben wird,
   * damit der Benutzer sieht, was er angezeigt bekommt.
   * @param schema bestimmt, welche Spalten angezeigt werden von den
   * Datensätzen aus results.
   * @param results die Ergebnisse der Anfrage.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printResults(String query, Set schema, QueryResults results)
  {
    System.out.println("Results for query \""+query+"\":");
    Iterator resIter = results.iterator();
    while (resIter.hasNext())
    {
      Dataset result = (Dataset)resIter.next();
      
      Iterator spiter = schema.iterator();
      while (spiter.hasNext())
      {
        String spalte = (String)spiter.next();
        String wert = "Spalte "+spalte+" nicht gefunden!";
        try{ 
          wert = result.get(spalte);
          if (wert == null) 
            wert = "unbelegt";
          else
            wert = "\""+wert+"\"";
        }catch(ColumnNotFoundException x){};
        System.out.print(spalte+"="+wert+(spiter.hasNext()?", ":""));
      }
      System.out.println();
    }
    System.out.println();
  }
  
  private QueryResults simpleFind(String spaltenName, String suchString) throws TimeoutException
  {
    List query = new Vector();
    query.add(new QueryPart(spaltenName, suchString));
    QueryResults find = find(query,3000000);
    return find;
  }
  
  private QueryResults simpleFind(String spaltenName1, String suchString1,String spaltenName2, String suchString2) throws TimeoutException
  {
    List query = new Vector();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    QueryResults find = find(query,3000000);
    return find;
  }

  public static void main(String[] args) throws IOException, SyntaxErrorException, NodeNotFoundException, TimeoutException, ConfigurationErrorException
  {
    File curDir = new File(System.getProperty("user.dir"));
    URL context = curDir.toURL();
    URL confURL = new URL(context,"testdata/ldap.conf");
    ConfigThingy ldapConf = new ConfigThingy("",confURL);
    Map nameToDatasource = new HashMap();
    ConfigThingy sourceDesc = ldapConf.query("Datenquelle").getFirstChild();
    LDAPDatasource dj = new LDAPDatasource(nameToDatasource, sourceDesc, context);
    
    
   
    // Test keys
   /*QueryResults qr = dj.simpleFind("OrgaEmail","  r.kom@muenchen.de","Orga1","Referatsleitung");
    Iterator iter = qr.iterator();
    
    Collection keys = new Vector();
    
    while(iter.hasNext()) {
      Dataset ds = (Dataset) iter.next();
      String key = ds.getKey();
      System.out.println("Key: " + ds.getKey());
      
      keys.add(key);
      
    }
    
    
    QueryResults qr2 = dj.getDatasetsByKey(keys,30000);
    
    dj.printResults("Get and find keys: ",dj.schema,qr2);
    */
    
    printResults("OrgaEmail = r.kom@muenchen.de , Orga1 = Referatsleitung", dj.getSchema(), dj.simpleFind("OrgaEmail"," r.kom@muenchen.de","Orga1","Referatsleitung"));
    printResults("OrgaEmail = r.kom@muenchen.de , Orga3 = Referatsleitung", dj.getSchema(), dj.simpleFind("OrgaEmail","  r.kom@muenchen.de","Orga3","Referatsleitung"));
    printResults("Orga2 = Stadtarchiv , Referat = Direktorium", dj.getSchema(), dj.simpleFind("Orga2","Stadtarchiv","Referat","Direktorium")); 
    printResults("Referat = Sozialreferat , Nachname = Meier", dj.getSchema(), dj.simpleFind("Referat","Sozialreferat","Nachname","Meier")); 
    //printResults("Nachname =r*", dj.getSchema(), dj.simpleFind("Nachname","r*"));
    //printResults("Nachname = *utz", dj.getSchema(), dj.simpleFind("Nachname","*utz"));
    //printResults("Nachname = *oe*", dj.getSchema(), dj.simpleFind("Nachname","*oe*"));
    //printResults("Nachname = Lutz", dj.getSchema(), dj.simpleFind("Nachname","Lutz"));
    printResults("Nachname = *utz, Vorname = Chris*", dj.getSchema(), dj.simpleFind("Nachname","Lutz","Vorname","Chris*"));
    printResults("Nachname = *utz, Vorname = Chris*", dj.getSchema(), dj.simpleFind("Nachname","Benkmann","Vorname","Matthias"));
  
  }

}
