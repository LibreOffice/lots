/*
* Dateiname: OOoDatasource.java
* Projekt  : WollMux
* Funktion : Stellt eine OOo-Datenquelle als WollMux-Datenquelle zur Verfügung
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 19.12.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.star.container.XNameAccess;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.sdbc.TransactionIsolation;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sdbcx.XTablesSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
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
   * Maximale Zeit in Sekunden, die die Datenquelle für die Verbindungsaufnahme mit der
   * Datenbank brauchen darf.
   */
  private static final int LOGIN_TIMEOUT = 5;
  
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
  private Set schema;
  
  /**
   * Erzeugt eine neue OOoDatasource.
   * 
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser
   *          OOoDatasource bereits vollständig instanziierten Datenquellen
   *          (zur Zeit nicht verwendet).
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser
   *          OOoDatasource enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit
   *          nicht verwendet).
   * @throws ConfigurationErrorException
   *           falls in der Definition in sourceDesc ein Fehler ist.
   *           Falls sourceDesc keinen Schema-Unterabschnitt aufweist, wird versucht,
   *           das Schema von der Datenquelle selbst zu bekommen. Tritt dabei ein
   *           Fehler auf wird ebenfalls diese Exception geworfen.
   */
  public OOoDatasource(Map nameToDatasource, ConfigThingy sourceDesc,
      URL context) throws ConfigurationErrorException
  {
    try
    {
      datasourceName = sourceDesc.get("NAME").toString();
    }
    catch (NodeNotFoundException x)
    { throw new ConfigurationErrorException("NAME der Datenquelle fehlt"); }
    
    try
    {
      oooDatasourceName = sourceDesc.get("SOURCE").toString();
    }
    catch (NodeNotFoundException x)
    { throw new ConfigurationErrorException("Datenquelle \""+datasourceName+"\": Name der OOo-Datenquelle muss als SOURCE angegeben werden"); }
    
    try
    {
      oooTableName = sourceDesc.get("TABLE").toString();
    }
    catch (NodeNotFoundException x)
    { throw new ConfigurationErrorException("Datenquelle \""+datasourceName+"\": Name der Tabelle/Sicht innerhalb der OOo-Datenquelle muss als TABLE angegeben werden"); }
    
    
    schema = new HashSet();
    ConfigThingy schemaConf = sourceDesc.query("Schema");
    if (schemaConf.count() != 0)
    {
      Iterator iter = ((ConfigThingy)schemaConf.iterator().next()).iterator();
      while (iter.hasNext())
      {
        schema.add(iter.next().toString());
      }
      if (schema.size() == 0) throw new ConfigurationErrorException("Datenquelle \""+datasourceName+"\": Schema-Abschnitt ist leer"); 
    }
    else
    {
      Logger.debug("Schema der Datenquelle "+datasourceName+" nicht angegeben. Versuche, es von der Datenquelle zu erfragen.");
      try{
        XDataSource ds = UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        ds.setLoginTimeout(LOGIN_TIMEOUT);
        XConnection conn = ds.getConnection("","");
        /*
         * Wir wollen nur die Tabellenspaltennamen auslesen. Da brauchen wir keine
         * Isolierung.
         */
        conn.setTransactionIsolation(TransactionIsolation.READ_UNCOMMITTED);
        
        /*
         * Laut IDL-Doku zu "View" müssen hier auch die Views enthalten sein.
         */
        XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
        XNameAccess columns = UNO.XColumnsSupplier(tables.getByName(oooTableName)).getColumns();
        String[] colNames = columns.getElementNames();
        for (int i = 0; i < colNames.length; ++i)
          schema.add(colNames[i]);
        
        if (schema.size() == 0) throw new ConfigurationErrorException("Datenquelle \""+datasourceName+"\": Tabelle \""+oooTableName+"\" hat keine Spalten");
      }
      catch(Exception x)
      {
        throw new ConfigurationErrorException("Konnte Schema der OOo-Datenquelle \""+oooDatasourceName+"\" nicht auslesen.", x);
      }
    }
  }
  
  public Set getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException
  {
    return null;
  }

  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    return null;
  }

  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return null;
  }

  public String getName()
  {
    return datasourceName;
  }

  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   */
  public static void main(String[] args) throws Exception
  {
    // Datenquelle(
    //   NAME "test"
    //   TYPE "ooo"
    //   SOURCE "datenbank"
    //   TABLE "UserAnsicht"
    //   Schema( "UserVorname" "UserNachname" "Beschreibung" )
    // )
    ConfigThingy conf = new ConfigThingy("Datenquelle");
    conf.add("NAME").add("test");
    conf.add("TYPE").add("ooo");
    conf.add("SOURCE").add("datenbank");
    conf.add("TABLE").add("UserAnsicht");
    
    OOoDatasource ds = new OOoDatasource(null, conf, null);
    System.out.println("Name: "+ds.getName());
    System.out.print("Schema: ");
    Iterator iter = ds.getSchema().iterator();
    while (iter.hasNext())
    {
      System.out.print("\""+iter.next()+"\" ");
    }
    System.out.println();
  }

  
}
