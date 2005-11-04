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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
//  TODO Konstruktor
    
    name = "Unbenannt";
    try{ name = sourceDesc.get("NAME").toString();} catch(NodeNotFoundException x){}
    
    ConfigThingy spalten = sourceDesc.query("Spalten");
    if (spalten.count() == 0)
      throw new ConfigurationErrorException("Abschnitt 'Spalten' fehlt in Datenquelle "+name);
    
    schema = new HashSet();
    
    Iterator iter = spalten.iterator();
    while (iter.hasNext())
    {
      ConfigThingy spaltenDesc = (ConfigThingy)iter.next();
      Iterator iter2 = spaltenDesc.iterator();
      while (iter2.hasNext())
      {
        ConfigThingy spalteDesc = (ConfigThingy)iter2.next();
        String spalte;
        try{
          spalte = spalteDesc.get("DB_SPALTE").toString();
          spalteToPath.put(spalte, path);
        } catch(NodeNotFoundException x) {
          throw new ConfigurationErrorException("Fehler in Definition von Datenquelle "+name+": DB_SPALTE Angabe fehlt");
        }
        schema.add(spalte);
      }
      
      
      
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  public Set getSchema()
  {
    // TODO Auto-generated method stub
    return schema;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout)
      throws TimeoutException
  {
    // TODO Auto-generated method stub
    return emptyResults;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    // TODO Auto-generated method stub
    return emptyResults;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    // TODO Auto-generated method stub
    return name;
  }

  
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
