/* TODO SchemaDatasource testen
* Dateiname: SchemaDatasource.java
* Projekt  : WollMux
* Funktion : Datenquelle, die die Daten einer existierenden Datenquelle 
*            mit geänderten Spalten zur Verfügung stellt. 
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 09.11.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datenquelle, die die Daten einer existierenden Datenquelle 
 *            mit geänderten Spalten zur Verfügung stellt. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SchemaDatasource implements Datasource
{
  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  private Datasource source;
  private String sourceName;
  private String name;
  private Set schema;
  private Map mapNewToOld;
  
  /**
   * Erzeugt eine neue SchemaDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser SchemaDatasource bereits vollständig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser SchemaDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen
   *        (zur Zeit nicht verwendet).
   */
  public SchemaDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ sourceName = sourceDesc.get("SOURCE").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE der Datenquelle "+name+" fehlt");
    }
    
    source = (Datasource)nameToDatasource.get(sourceName);  
      
    if (source == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+sourceName+"\" nicht (oder fehlerhaft) definiert");
  
    ConfigThingy renamesDesc = sourceDesc.query("RENAME");
    int numRenames = renamesDesc.count();
  
    schema = source.getSchema();
    mapNewToOld = new HashMap();
    
    Iterator iter = renamesDesc.iterator();
    for (int i = 0; i < numRenames; ++i)
    {
      ConfigThingy renameDesc = (ConfigThingy)iter.next();
      if (renameDesc.count() != 2)
        throw new ConfigurationErrorException("Fehlerhafte RENAME Angabe in Datenquelle \""+name+"\"");
      
      String spalte1 = "";
      String spalte2 = "";
      try{
        spalte1 = renameDesc.getFirstChild().toString();
        spalte2 = renameDesc.getLastChild().toString();
      }catch(NodeNotFoundException x){}
      
      if (!schema.contains(spalte1))
        throw new ConfigurationErrorException("Spalte \""+spalte1+"\" ist nicht im Schema");
      
      if (!SPALTENNAME.matcher(spalte2).matches())
        throw new ConfigurationErrorException("\""+spalte2+"\" ist kein erlaubter Spaltenname");
      
      mapNewToOld.put(spalte2, spalte1);
    }
    
    schema.removeAll(mapNewToOld.values());
    schema.addAll(mapNewToOld.keySet());
  }

  public Set getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout) throws TimeoutException
  {
    return wrapDatasets(source.getDatasetsByKey(keys, timeout));
  }

  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    List translatedQuery = new Vector(query.size());
    Iterator iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart p = (QueryPart)iter.next();
      String spalte = p.getColumnName();
      String alteSpalte = (String)mapNewToOld.get(spalte);
      if (alteSpalte != null) 
        translatedQuery.add(new QueryPart(alteSpalte,p.getSearchString()));
      else
        translatedQuery.add(p);
    }
    return wrapDatasets(source.find(translatedQuery, timeout));
  }

  public String getName()
  {
    return name;
  }
  
  private QueryResults wrapDatasets(QueryResults res)
  {
    List wrappedRes = new Vector(res.size());
    Iterator iter = res.iterator();
    while (iter.hasNext())
      wrappedRes.add(new RenameDataset((Dataset)iter.next()));
    
    return new QueryResultsList(wrappedRes);
  }
  
  private class RenameDataset implements Dataset
  {
    private Dataset ds;
    
    public RenameDataset(Dataset ds)
    {
      this.ds = ds;
    }
    
    
    public String get(String columnName) throws ColumnNotFoundException
    {
      String alteSpalte = (String)mapNewToOld.get(columnName);
      if (alteSpalte != null) 
        return ds.get(alteSpalte);
      else
        return ds.get(columnName);
    }

    public String getKey()
    {
      return ds.getKey();
    }
  }

}
