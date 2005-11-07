/* TODO Testen von PreferDatasource
* Dateiname: PreferDatasource.java
* Projekt  : WollMux
* Funktion : Datasource, die Daten einer Datenquelle von Datein einer andere
*            Datenquelle verdecken lässt.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datasource, die Daten einer Datenquelle A von Dateien einer anderen
 * Datenquelle B verdecken lässt. Dies funktioniert so, dass Anfragen erst
 * an Datenquelle A gestellt werden und dann für alle Ergebnisdatensätze
 * geprüft wird, ob ein Datensatz (oder mehrere Datensätze) 
 * mit gleichem Schlüssel in Datenquelle B ist. Falls dies so ist, werden
 * für diesen Schlüssel nur die Datensätze aus Datenquelle B zurückgeliefert.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PreferDatasource implements Datasource
{
  private Datasource source1;
  private Datasource source2;
  private String source1Name;
  private String source2Name;
  private Set schema;
  private String name;
  
  /**
   * Erzeugt eine neue PreferDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser PreferDatasource bereits vollständig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser PreferDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen
   *        (zur Zeit nicht verwendet).
   */
  public PreferDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ source1Name = sourceDesc.get("SOURCE").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE der Datenquelle "+name+" fehlt");
    }
    
    try{ source2Name = sourceDesc.get("OVER").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("OVER-Angabe der Datenquelle "+name+" fehlt");
    }
    
    source1 = (Datasource)nameToDatasource.get(source1Name);  
    source2 = (Datasource)nameToDatasource.get(source2Name);
    
    if (source1 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source1+"\" nicht (oder fehlerhaft) definiert");
    
    if (source2 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source2+"\" nicht (oder fehlerhaft) definiert");

    Set schema1 = source1.getSchema();
    Set schema2 = source2.getSchema();
    if (!schema1.containsAll(schema2) || !schema2.containsAll(schema1))
      throw new ConfigurationErrorException("Schemata der Datenquellen \""+source1Name+"\" und \""+source2Name+"\" stimmen nicht überein");
    
    schema = new HashSet(schema1);
  }

  public Set getSchema()
  {
    return new HashSet(schema);
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults results = source2.getDatasetsByKey(keys, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source2Name+" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten");
    return new QueryResultsOverride(results, source1, timeout);
  }

  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults results = source2.find(query, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source2Name+" konnte Anfrage find() nicht schnell genug beantworten");
    return new QueryResultsOverride(results, source1, timeout);
  }

  public String getName()
  {
    return name;
  }

  private static class QueryResultsOverride implements QueryResults
  {
    private int size;
    private Set keyBlacklist = new HashSet();
    private QueryResults overrideResults;
    private QueryResults results;
    
    public QueryResultsOverride(QueryResults results, Datasource override, long timeout)
    throws TimeoutException
    {
      this.results = results;
      size = results.size();
        
      Map keyToCount = new HashMap(); //of int[]
      
      Iterator iter = results.iterator();
      while (iter.hasNext())
      {
        Dataset ds = (Dataset)iter.next();
        String key = ds.getKey();
        if (!keyToCount.containsKey(key))
          keyToCount.put(key, new int[]{0});
        int[] count = (int[])keyToCount.get(key);
        ++count[0];
      }
 
      overrideResults = override.getDatasetsByKey(keyToCount.keySet(),timeout);
      
      size += overrideResults.size();
      
      iter = overrideResults.iterator();
      while (iter.hasNext())
      {
        Dataset ds = (Dataset)iter.next();
        String key = ds.getKey();
        
        int[] count = (int[])keyToCount.get(key);
        size -= count[0];
        count[0] = 0;
        
        keyBlacklist.add(key);
      }
    }
    
    public int size()
    {
      return size;
    }

    public Iterator iterator()
    {
      return new MyIterator();
    }

    public boolean isEmpty()
    {
      return size == 0;
    }
    
    private class MyIterator implements Iterator
    {
      private Iterator iter;
      private boolean inOverride;
      private int remaining;
      
      public MyIterator()
      {
        iter = overrideResults.iterator();
        inOverride = true;
        remaining = size;
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext()
      {
        return (remaining > 0);
      }

      public Object next()
      {
        if (remaining == 0)
          throw new NoSuchElementException();
        
        --remaining;
        
        if (inOverride)
        {
          if (iter.hasNext()) return iter.next();
          inOverride = false;
          iter = results.iterator();
        }
        
        Dataset ds;
        do{
          ds = (Dataset)iter.next();
        }while (keyBlacklist.contains(ds.getKey()));
        
        return ds;
      }
    }
  }
  
}
