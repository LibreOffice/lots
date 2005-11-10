/* TODO Testen von UnionDatasource
* Dateiname: UnionDatasource.java
* Projekt  : WollMux
* Funktion : Datasource, die die Vereinigung 2er Datasources darstellt
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 07.11.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datasource, die die Vereinigung 2er Datasources darstellt
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnionDatasource implements Datasource
{
  private Datasource source1;
  private Datasource source2;
  private String source1Name;
  private String source2Name;
  private Set schema;
  private String name;
  
  /**
   * Erzeugt eine neue UnionDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser UnionDatasource bereits vollständig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser UnionDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen
   *        (zur Zeit nicht verwendet).
   */
  public UnionDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ source1Name = sourceDesc.get("SOURCE1").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE1 der Datenquelle "+name+" fehlt");
    }
    
    try{ source2Name = sourceDesc.get("SOURCE2").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE2 der Datenquelle "+name+" fehlt");
    }
    
    source1 = (Datasource)nameToDatasource.get(source1Name);  
    source2 = (Datasource)nameToDatasource.get(source2Name);
    
    if (source1 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source1Name+"\" nicht (oder fehlerhaft) definiert");
    
    if (source2 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source2Name+"\" nicht (oder fehlerhaft) definiert");

    /*
     * Anmerkung: Die folgende Bedingung ist "unnötig" streng, aber um
     * sie aufzuweichen (z.B. Gesamtschema ist Vereinigung der Schemata)
     * wäre es erforderlich, einen Dataset-Wrapper zu implementieren,
     * der dafür sorgt, dass alle Datasets, die in QueryResults zurück-
     * geliefert werden das selbe Schema haben. Solange dafür keine
     * Notwendigkeit ersichtlich ist, spare ich mir diesen Aufwand.
     */
    Set schema1 = source1.getSchema();
    Set schema2 = source2.getSchema();
    if (!schema1.containsAll(schema2) || !schema2.containsAll(schema1))
      throw new ConfigurationErrorException("Schemata der Datenquellen \""+source1Name+"\" und \""+source2Name+"\" stimmen nicht überein");
    
    schema = new HashSet(schema1);
  }

  public Set getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults res1 = source1.getDatasetsByKey(keys, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten");
    QueryResults res2 = source2.getDatasetsByKey(keys, timeout);
    return new QueryResultsUnion(res1,res2);
  }

  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults res1 = source1.find(query, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage find() nicht schnell genug beantworten");
    QueryResults res2 = source2.find(query, timeout);
    return new QueryResultsUnion(res1,res2);
  }

  public String getName()
  {
    return name;
  }

}
