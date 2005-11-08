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
  private final static String SEPARATOR = "!$§%&";
  
  // Map von query-Strings auf LDAP-Attributnamen
  private Map nameMap = new HashMap();
  
  // Nameparser
  private NameParser nameParser;
  
  // Key-Attribute (LDAP)
  private List keyAttributes = new Vector();
  
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
          
        } catch(NodeNotFoundException x) {
          throw new ConfigurationErrorException( errorMessage() + "DB_SPALTE Angabe fehlt");
        }
        
        LDAPAttribute currentPath = new LDAPAttribute(relativePath, attributeName);
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
    
    // speichere die Schluesselattribute
    while(keyIterator.hasNext()) { 
      String currentName = keyIterator.next().toString();
      
      // ist Schluesselattribut vorhanden?
      if (nameMap.get(currentName)==null) throw new ConfigurationErrorException( errorMessage() + "Schluesselelement nicht definiert.");
        
      keyAttributes.add(currentName);
    }
    
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
    
    
    LDAPAttribute(int relativePath, String attributeName) {
      this.relativePath = relativePath;
      this.attributeName = attributeName;
    }
    
  }
  
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


  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException {
    
    long endTime = System.currentTimeMillis() + timeout;
    
    Vector results = new Vector();
      
    Iterator iter = keys.iterator();
    
    while(iter.hasNext()) {
      
      String currentKey = (String) iter.next();
      String searchFilter = keyToQuery(currentKey);
      
      NamingEnumeration currentResults = searchLDAPPerson("",searchFilter,timeout);
      
        try
        {
          while(currentResults.hasMore()) {
            if (System.currentTimeMillis()>endTime) throw new TimeoutException();
            SearchResult currentResult = (SearchResult) currentResults.next();
            results.add(getDataset(currentResult,endTime));
          }
        }
        catch (TimeLimitExceededException e)
        {
          throw new TimeoutException(e);
        }
        catch (NamingException e)
        {
          Logger.error(e);
          throw new TimeoutException("Fehler bei Enumeration der LDAP-Anfrageergebnisse",e);
        }
     
 

    }
    
    results.trimToSize();
    
    return new QueryResultsList(results);
  }
  
  class RelativePaths {
    
    private int relative;
    private List paths;
    
    RelativePaths(int relative, List paths) {
      this.relative = relative;
      this.paths = paths;
    }
    
  }
  
  public RelativePaths getPaths(String filter, int pathLength) throws TimeoutException {
    
    Vector paths;
    
    try
    {
      DirContext ctx = new InitialLdapContext(properties,null);
      NameParser np = ctx.getNameParser("");
      int rootSize = np.parse(baseDN).size();
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
      
      NamingEnumeration enumer = ctx.search(baseDN,filter,sc);
      
      paths = new Vector();
      
      while(enumer.hasMoreElements()) {
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
    List subtreePathLists = new Vector();
    
    boolean first = true;
    while (iter.hasNext()) {
      
      if (System.currentTimeMillis()>endTime) throw new TimeoutException();
      
      QueryPart currentQuery = (QueryPart) iter.next();
      
      LDAPAttribute ldapAttribute = (LDAPAttribute) nameMap.get(currentQuery.getColumnName());
      
      if (ldapAttribute==null) continue;
      
      String attributeName = ldapAttribute.attributeName;
      int relativePath = ldapAttribute.relativePath;
      String attributeValue = currentQuery.getSearchString();
      
      if (relativePath==0) { // edit filter
        if (first) {
          searchFilter = "(" + attributeName + "=" + attributeValue + ")";
          first = false;
        } else {
          searchFilter = "(&(" + attributeName + "=" + attributeValue + ")" + searchFilter + ")";
        }
      } else { // search for subtrees
        
        String pathFilter = "(" + attributeName + "=" + attributeValue + ")";
        
        RelativePaths paths = getPaths(pathFilter,relativePath);
        
        subtreePathLists.add(paths);
        
      }
      
    }
    
    // create subtree paths to search
    
    List subtrees = new Vector();
    
    if (subtreePathLists.size()==0) subtrees.add("");
    else {
      
      for (int n=0; n<subtreePathLists.size(); n++) {
        
        RelativePaths currentRelativePaths = (RelativePaths) subtreePathLists.get(n);
        
        // TODO definiere Ordnung auf den Pfaden
        
        /* betrachte die kleinsten Pfade
         * betrachte nächstgrößere Pfade -> kleinerer Pfad substring des aktuellen?
         * ...
         * die übriggebliebenen längsten Pfade werden in 'subtrees' gespeichert
         */
        
        
        
        for (int m=0; m<currentRelativePaths.paths.size(); m++) {
          String currentName = (String) currentRelativePaths.paths.get(m).toString();
          subtrees.add(currentName);
          
        }
        
        
      }
      
      
      
    }
    
    Vector results = new Vector();
    
    if (searchFilter.equals("")&subtrees.size()==0) return new QueryResultsList(new Vector(0));
    
    
    Iterator subtreeIterator = subtrees.iterator();
    //List resultEnums = new Vector();
    
    List currentResultList = new Vector();
    
    while(subtreeIterator.hasNext()) {
      String subTree = (String)subtreeIterator.next();
      String comma = ",";
      if (subTree.equals("")) comma = "";
      NamingEnumeration currentResults = searchLDAPPerson(subTree + comma,searchFilter,timeout);
      
      while(currentResults.hasMoreElements()) {
        SearchResult sr = (SearchResult) currentResults.nextElement();
        sr.setName(checkQuotes(sr.getName()) + comma + subTree);
        currentResultList.add(sr);
      }
      
      //resultEnums.add(currentResults);
    }
    
    Iterator currentResultsIterator = currentResultList.iterator();
    
    
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
    /*catch (NamingException e)
    {
      Logger.error(e);
      throw new TimeoutException("Fehler bei Enumeration der LDAP-Anfrageergebnisse",e);
    }*/
   
    
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
    for (int n=0; n<keyValues.length; n++) {
      if (n==0) {
        query = "(" + nameMap.get(keyAttributes.get(n)) + "=" + keyValues[n] + ")";
      } else {
        query = "(&(" + nameMap.get(keyAttributes.get(n)) + "=" + keyValues[n] + ")" + query + ")";
      }
    }
    
    return query;
  }
  
  
  // vervollständigt SearchResults um Daten aus dem Verteichnis und gibt ein Dataset zurück
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
            
            attributePath.addAll(pathName.getPrefix(pathName.size()+relativePath-rootName.size()));
            
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
    
    try {
      ctx.close();
    } catch (NamingException e) {}
    
    return new LDAPDataset(key, relation);
  }
  
  
  // Suche im Directory
  private NamingEnumeration searchLDAPPerson(String path, String filter, long timeout) throws TimeoutException {
    
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    
    if (timeout>Integer.MAX_VALUE) timeout = Integer.MAX_VALUE;
    searchControls.setTimeLimit((int) timeout);
    
    filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    
    DirContext ctx = null;
    
    NamingEnumeration result;
    
    try {
      
      ctx = new InitialLdapContext(properties,null);
      
      result = ctx.search(path + baseDN,filter,searchControls);
    
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
    
    
    //printResults("Nachname = *", dj.getSchema(), dj.simpleFind("Nachname","*")); 
    printResults("Nachname = Bruce-Boye", dj.getSchema(), dj.simpleFind("Nachname","augustin","Referat","Sozialreferat"));
    //printResults("Referat = Sozialreferat, Nachname = Bruce-Boye", dj.getSchema(), dj.simpleFind("Referat","Sozialreferat","Nachname","Bruce-Boye")); 
    //printResults("Referat = Sozialreferat", dj.getSchema(), dj.simpleFind("Referat","Sozialreferat"));
    //printResults("Nachname = lOEsewiTZ", dj.getSchema(), dj.simpleFind("Nachname","lOEsewiTZ")); 
    /*printResults("Nachname = *utz", dj.getSchema(), dj.simpleFind("Nachname","*utz"));
    printResults("Nachname = *oe*", dj.getSchema(), dj.simpleFind("Nachname","*oe*"));
    printResults("Nachname = Lutz", dj.getSchema(), dj.simpleFind("Nachname","Lutz"));
    printResults("Nachname = *utz, Vorname = Chris*", dj.getSchema(), dj.simpleFind("Nachname","*utz","Vorname","Chris*"));
    */
  }

}
