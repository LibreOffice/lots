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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;
import javax.naming.directory.*;
import javax.naming.*;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * TODO Doku
 * @author Max Meier (D-III-ITD 5.1)
 */
public class LDAPDatasource implements Datasource
{
  private static QueryResults emptyResults = new QueryResultsList(new Vector(0));
  private Set schema;
  private String name;
  
  private String url;
  private String baseDN;  
  private String objectClass;
  
  
  // properties für die Verbindung zum LDAP-Server
  private Properties properties = new Properties();
  
  // Separator zur Schluesselerzeugung aus mehreren Schluesselwerten
  private final static String SEPARATOR = "!$§%&";
  
  private final static int COUNT_LIMIT = 500;
  
  // Map von query-Strings auf LDAP-Attributnamen
  private Hashtable nameMap = new Hashtable();
  
  // Key-Attribute (LDAP)
  private Vector keyAttributes = new Vector();
  
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
      throw new ConfigurationErrorException(errorMessage() + "URL des LDAP-Servers fehlt.");
    }
    
    try {
      baseDN = sourceDesc.get("BASE_DN").toString();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException(errorMessage() + "BASE_DN des LDAP-Servers fehlt.");
    }
    
    try {
      objectClass = sourceDesc.get("OBJECT_CLASS").toString();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException(errorMessage() + "Keine OBJECT_CLASS definiert.");
    }
    
    // set properties
    properties.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
    properties.put(Context.PROVIDER_URL,url); // + "/" + baseDN);

    ConfigThingy spalten = sourceDesc.query("Spalten");
    
    if (spalten.count() == 0)
      throw new ConfigurationErrorException(errorMessage() + "Abschnitt 'Spalten' fehlt.");
    
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
        
        try {
          
          spalte = spalteDesc.get("DB_SPALTE").toString();
          String path = spalteDesc.get("PATH").toString();
          
          // get relativePath and attributeName
          String[] splitted = path.split(":");
          
          if (splitted.length!=2) throw new ConfigurationErrorException(errorMessage() + "Syntaxerror bei Pfadangabe von " + spalte);
          
          try {
            relativePath = Integer.parseInt(splitted[0]);
          } catch (NumberFormatException e) {
            throw new ConfigurationErrorException(errorMessage() + "Syntaxerror bei Angabe des relativen Pfads von " + spalte);
          } 
          
          attributeName = splitted[1];
          
        } catch(NodeNotFoundException x) {
          throw new ConfigurationErrorException(errorMessage() + "DB_SPALTE Angabe fehlt");
        }
        
        LDAPAttribute currentPath = new LDAPAttribute(relativePath, attributeName);
        nameMap.put(spalte,currentPath);
        schema.add(spalte);
      }  
    }
    
    // Key-Attribute
    ConfigThingy keys = sourceDesc.query("Schluessel");
    
    if (keys.count()==0)
      throw new ConfigurationErrorException(errorMessage() + "Schluesselfeld fehlt.");
    
    ConfigThingy keySpalten;
    
    try {
      keySpalten = keys.getLastChild();
    } catch (NodeNotFoundException e) {
      throw new ConfigurationErrorException(errorMessage() + "Syntaxerror bei Schluesseldefinition.");
    }
    
    Iterator keyIterator = keySpalten.iterator();
      
    while(keyIterator.hasNext()) {   
      String currentName = keyIterator.next().toString();
      keyAttributes.add(currentName);
    }
    
    if (keyAttributes.size()==0)
      throw new ConfigurationErrorException(errorMessage() + "Kein Schluesselement angegeben.");
    
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
    
    
    public LDAPAttribute(int relativePath, String attributeName) {
      this.relativePath = relativePath;
      this.attributeName = attributeName;
    }
    
  }
  
  private class LDAPDataset implements Dataset {
    
    private String key;
    private Hashtable relation = new Hashtable();
    
    public LDAPDataset(String key, Hashtable relation) {
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
  
  private class LDAPQueryResults implements QueryResults {
    
    private Vector results;
    
    
    public LDAPQueryResults(Vector results) {
      this.results = results;
    }

    public int size() {
      return results.size();
    }

    public Iterator iterator() {
      return results.iterator();
    }

    public boolean isEmpty() {
      return results.isEmpty();
    }
    
  }
  
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  public Set getSchema()
  {
    return schema;
  }
  

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException { 
    
    // TODO: timeout!
    
    Vector results = new Vector();
      
    Iterator iter = keys.iterator();
    
    while(iter.hasNext()) {
      
      String currentKey = (String) iter.next();
      String searchFilter = keyToQuery(currentKey);
      
      NamingEnumeration currentResults = searchLDAP(searchFilter);
      
      try {
        while(currentResults.hasMore()) {
          SearchResult currentResult = (SearchResult) currentResults.next();
          results.add(getDataset(currentResult));
        }
      } catch (Exception e) {
        throw new TimeoutException();
      }

    }
    
    return new LDAPQueryResults(results);
  }
  

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List query, long timeout) throws TimeoutException {
    
    // TODO timeout!
    
    Iterator iter = query.iterator();
    
    String searchFilter = "";
    
    boolean first = true;
    while (iter.hasNext()) {
      QueryPart currentQuery = (QueryPart) iter.next();
      
      LDAPAttribute ldapAttribute = (LDAPAttribute) nameMap.get(currentQuery.getColumnName());
      
      String attributeName = ldapAttribute.attributeName;
      String attributeValue = currentQuery.getSearchString();
      
      if (first) {
        searchFilter = "(" + attributeName + "=" + attributeValue + ")";
        first = false;
      } else {
        searchFilter = "(&(" + attributeName + "=" + attributeValue + ")" + searchFilter + ")";
      }
      
    }
    
    Vector results = new Vector();
    
    NamingEnumeration currentResults = searchLDAP(searchFilter);
    
    try {
      while(currentResults.hasMore()) {
        SearchResult currentResult = (SearchResult) currentResults.next();
        results.add(getDataset(currentResult));
      }
    } catch (Exception e) {
      throw new TimeoutException(e);
    }
       
    return new LDAPQueryResults(results);
  }
  

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }
  
  
  // generiert einen Schluessel aus einem geordneten(!) Vector der Schluesselwerte
  private String generateKey(Vector values) {
    
    /*if (values.size()!=keyAttributes.size())
      throw new Exception();
      */
    
    String key = "";
    
    for (int n=0; n<values.size(); n++) {
      if (n==0) {
        key = (String) values.get(n);
      } else {
        key = key + SEPARATOR + (String) values.get(n);
      }
    }
    
    return key;
  }
  
  
  // generiert einen Suchausdruck aus einem Schluessel
  private String keyToQuery(String key) {
    
    String[] keyValues = key.split(SEPARATOR);
    
    /*if (keyValues.length!=keyAttributes.size())
      throw new Exception();
      */
    
    String query = "";   
    for (int n=0; n<keyValues.length; n++) {
      if (n==0) {
        query = "(" + nameMap.get(keyAttributes.get(n)) + "=" + keyValues[n] + ")";
      } else {
        query = "(&(" + nameMap.get(keyAttributes.get(n)) + "=" + keyValues[n] + ")" + query + ")";
      }
    }
    
    return query;
  }
  
  
  private Dataset getDataset(SearchResult searchResult) throws TimeoutException {
    
    Attributes attributes = searchResult.getAttributes();
    Enumeration keys = nameMap.keys();
    
    Hashtable relation = new Hashtable();
    
    while(keys.hasMoreElements()) {
      
      String currentKey = (String) keys.nextElement();
      LDAPAttribute currentAttribute = (LDAPAttribute) nameMap.get(currentKey);
      
      int relativePath = currentAttribute.relativePath;
      String attributeName = currentAttribute.attributeName;
      
      String value = null;
      
      if (relativePath==0) {
        
        try {
          value = (String) attributes.get(attributeName).get();
        } catch (Exception e) {}
        
      } else {
        
        String pathName = searchResult.getName();

        //value = getAttributeValue(name,attributeName,relativePath);

        boolean flag = false;
        if (pathName.charAt(0)=='\"' & pathName.charAt(pathName.length()-1)=='\"') flag = true;
        
        if (flag) {
          pathName = pathName.substring(1);
          pathName = pathName.substring(0,pathName.length()-1);
        }
        
        if (!flag & pathName.charAt(0)=='\"') {
          flag = true;
          pathName = pathName.substring(pathName.length()-1);
        }
        
        String[] split = pathName.split("(?<!\\\\),");
        
        //System.out.println("FULLPATH::: " + searchResult.getName());
        
        if (relativePath < 0) {
          relativePath = split.length + relativePath;
        }
        
        if (relativePath<split.length) {
          
          String path = "";
          
          for (int n=0; n<relativePath; n++) {
            
            if (n==0) {
              path = split[split.length-1];
            } else {
              path = split[split.length-1-n] + "," + path;
            }
            
          }
          
          //System.out.println("!!!PATH:  " + path);
          
          value = getAttributeValue(path,attributeName,flag);
              
        }
        

      }
      
      if (value!=null)
        relation.put(currentKey, value);
    }
    
    // generate Key
    Vector keyValues = new Vector();
    
    for (int n=0; n<keyAttributes.size(); n++) {
      String currentValue = (String) relation.get(keyAttributes.get(n));
      keyValues.add(currentValue);
    }
    
    String key = generateKey(keyValues);
    
    return new LDAPDataset(key, relation);
  }
  
  
  private NamingEnumeration searchLDAP(String filter) throws TimeoutException {
    
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setCountLimit(COUNT_LIMIT);
    
    filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    
    DirContext ctx = null;
    
    NamingEnumeration result;
    
    try {
      
      ctx = new InitialDirContext(properties);
        
      result = ctx.search(baseDN,filter,searchControls);
      
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
  
  private String getAttributeValue(String path, String name,boolean flag) throws TimeoutException {
    
    DirContext ctx = null;
    
    String result;
    
    try {
      
      ctx = new InitialDirContext(properties);
      
      String fullPath = path + "," + baseDN;
      
      if (flag) fullPath = "\"" + fullPath + "\"";
      
      String[] searchAttributes = {name};
      
      Attributes attributes = ctx.getAttributes(fullPath,searchAttributes);
      
      Attribute attribute = attributes.get(name);
      
      if (attribute==null) result = null;
      else {
        result = (String) attribute.get();
      }

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
  
/*private String getAttributeValue(String path, String attrName, int relativePath) throws TimeoutException {
    
    DirContext ctx = null;
    
    String result;
    
    Name name;
    
    try {
      
      ctx = new InitialDirContext(properties);
      
      NameParser parser;
      
        
      NameParser nameParser = ctx.getNameParser("");
      
      name = nameParser.parse(path);
      
      
      if (relativePath>0) {
        name = name.getSuffix(name.size()-relativePath);
      } else {
        name = name.getSuffix(-relativePath);
        
      }
      
      Name base = nameParser.parse(baseDN);
      
      Name fullPath = name.addAll(base);
      
      System.out.println("full path:" + fullPath);
      
      String[] searchAttributes = {attrName};
      
      Attributes attributes = ctx.getAttributes(fullPath,searchAttributes);
      
      Attribute attribute = attributes.get(attrName);
      
      if (attribute==null) result = null;
      else {
        result = (String) attribute.get();
      }
 
      return result;
          
      } catch (Exception e) {
        System.out.println(e);
      } finally {
        if (ctx!=null)
        try {
          ctx.close();
        } catch (Exception e) {}
      }

      return null;
      
    
  }
  */
  
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
    return find(query,3000);
  }
  
  private QueryResults simpleFind(String spaltenName1, String suchString1,String spaltenName2, String suchString2) throws TimeoutException
  {
    List query = new Vector();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    return find(query,3000);
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
    printResults("Nachname = lOEsewiTZ", dj.getSchema(), dj.simpleFind("Nachname","lOEsewiTZ"));
    printResults("Nachname = benkm*", dj.getSchema(), dj.simpleFind("Nachname","benkm*"));
    printResults("Nachname = *utz", dj.getSchema(), dj.simpleFind("Nachname","*utz"));
    printResults("Nachname = *oe*", dj.getSchema(), dj.simpleFind("Nachname","*oe*"));
    printResults("Nachname = Lutz", dj.getSchema(), dj.simpleFind("Nachname","Lutz"));
    printResults("Nachname = *utz, Vorname = Chris*", dj.getSchema(), dj.simpleFind("Nachname","*utz","Vorname","Chris*"));
    
    
  }

}
