//TODO L.m()
/* 
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
* 10.11.2005 | BNK | Zu SchemaDatasource aufgebohrt
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
import java.util.HashSet;
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
  private static final String EMPTY_COLUMN = "";
  private Datasource source;
  private String sourceName;
  private String name;
  private Set<String> schema;
  private Map<String, String> mapNewToOld;
  
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
  
    schema = new HashSet<String>(source.getSchema());
    mapNewToOld = new HashMap<String, String>();
    
    List<String> columnsToDrop = new Vector<String>();
    
    ConfigThingy drops = sourceDesc.query("DROP");
    Iterator<ConfigThingy> iter = drops.iterator();
    while (iter.hasNext())
    {
      Iterator iter2 = iter.next().iterator();
      while (iter2.hasNext())
      {
        String spalte = iter2.next().toString();
        if (!schema.contains(spalte))
          throw new ConfigurationErrorException("Spalte \""+spalte+"\" ist nicht im Schema");
        columnsToDrop.add(spalte);
      }
    }
    
    List<String> columnsToAdd = new Vector<String>();
    
    ConfigThingy adds = sourceDesc.query("ADD");
    iter = adds.iterator();
    while (iter.hasNext())
    {
      Iterator iter2 = iter.next().iterator();
      while (iter2.hasNext())
      {
        String spalte = iter2.next().toString();
        if (!SPALTENNAME.matcher(spalte).matches())
          throw new ConfigurationErrorException("\""+spalte+"\" ist kein erlaubter Spaltenname");
        columnsToAdd.add(spalte);
        columnsToDrop.remove(spalte);
      }
    }
    
    ConfigThingy renamesDesc = sourceDesc.query("RENAME");
    
    iter = renamesDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy renameDesc = iter.next();
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
      columnsToDrop.add(spalte1);
      columnsToDrop.remove(spalte2);
      columnsToAdd.add(spalte2);
    }
    
    /**
     * Für alle hinzugefügten Spalten, die weder in der Originaldatenbank
     * existieren noch durch einen RENAME auf eine Spalte der Originaldatenbank
     * abgebildet werden, füge ein Pseudomapping auf EMPTY_COLUMN hinzu,
     * damit RenameDataset.get() weiss, dass es für die Spalte null liefern soll.
     */
    for (String spalte : columnsToAdd)
    {
      if (!schema.contains(spalte) && !mapNewToOld.containsKey(spalte))
        mapNewToOld.put(spalte, EMPTY_COLUMN);
    }
    
    schema.removeAll(columnsToDrop);
    schema.addAll(columnsToAdd);
    
  }

  public Set<String> getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout) throws TimeoutException
  {
    return wrapDatasets(source.getDatasetsByKey(keys, timeout));
  }
  
  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return new QueryResultsList(new Vector<RenameDataset>(0));
  }

  public QueryResults find(List<QueryPart> query, long timeout) throws TimeoutException
  {
    List<QueryPart> translatedQuery = new Vector<QueryPart>(query.size());
    Iterator<QueryPart> iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart p = iter.next();
      String spalte = p.getColumnName();
      
      if (!schema.contains(spalte)) //dieser Test ist nicht redundant wegen DROPs
        return new QueryResultsList(new Vector<RenameDataset>(0));
      
      String alteSpalte = mapNewToOld.get(spalte);
      
      if (alteSpalte == /*nicht equals()!!!!*/ EMPTY_COLUMN) 
        return new QueryResultsList(new Vector<RenameDataset>(0));
      
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
    List<RenameDataset> wrappedRes = new Vector<RenameDataset>(res.size());
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
        //dieser Test ist nicht redundant wegen DROPs
      if (!schema.contains(columnName)) throw new ColumnNotFoundException("Spalte "+columnName+" existiert nicht!");
      
      String alteSpalte = mapNewToOld.get(columnName);
      
      if (alteSpalte == /*nicht equals()!!!!*/ EMPTY_COLUMN) 
        return null;
      
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
