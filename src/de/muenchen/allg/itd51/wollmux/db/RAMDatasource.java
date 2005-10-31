/*
* Dateiname: RAMDatasource.java
* Projekt  : WollMux
* Funktion : Oberklasse für Datasources, die ihre Daten vollständig
*            im Speicher halten
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 31.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Oberklasse für Datasources, die ihre Daten vollständig
 *  im Speicher halten
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class RAMDatasource implements Datasource
{
  private Set schema;
  private List data;
  private String name;
  
  /**
   * Achtung: data und schema werden direkt als Referenz eingebunden, 
   * nicht kopiert.
   * @param name der Name der Datenquelle
   * @param schema das Schema der Datenquelle
   * @param data die Datensätze der Datenquelle
   */
  public RAMDatasource(String name, Set schema, List data)
  {
    init(name, schema, data);
  }
  
  protected void init(String name, Set schema, List data)
  {
    this.schema = schema; 
    this.data = data;
    this.name = name;
  }
  
  protected RAMDatasource(){};

  public Set getSchema()
  {
    return new HashSet(schema);
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout)
      throws TimeoutException
  {
    Vector res = new Vector();
    Iterator iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      if (keys.contains(ds.getKey())) res.add(ds);
    }
        
    return new QueryResultsList(res);
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List query, long timeout) throws TimeoutException
  {
    DatasetChecker checker = new MatchAllDatasetChecker();
    Iterator iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart part = (QueryPart)iter.next();
      checker = checker.and(makeChecker(part.getColumnName(), part.getSearchString()));
    }
    
    List results = new Vector();
    
    iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      if (checker.matches(ds)) results.add(ds);
    }
    return new QueryResultsList(results);
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }

  
  private DatasetChecker makeChecker(String columnName, String query)
  {
    int i = query.startsWith("*") ? 1 : 0;
    i |= query.endsWith("*") ? 2 : 0;
    switch(i)
    {
      case 0: return new ColumnIdentityChecker(columnName, query);
      case 1: return new ColumnSuffixChecker(columnName, query.substring(1));
      case 2: return new ColumnPrefixChecker(columnName, query.substring(0, query.length()-1));
      case 4: 
      default:  return new ColumnContainsChecker(columnName, query.substring(1,query.length()-1));
    }
  }
  
  private static abstract class DatasetChecker
  {
    public abstract boolean matches(Dataset ds);

    public DatasetChecker and(DatasetChecker check2)
    { return new AndDatasetChecker(this, check2);}
    
    public DatasetChecker or(DatasetChecker check2)
    { return new OrDatasetChecker(this, check2);}
  }
  
  private static class MatchAllDatasetChecker extends DatasetChecker
  {
    public boolean matches(Dataset ds) {return true;}
  }

  
  private static class AndDatasetChecker extends DatasetChecker
  {
    private DatasetChecker check1;
    private DatasetChecker check2;
    
    public AndDatasetChecker(DatasetChecker check1, DatasetChecker check2)
    {
      this.check1 = check1;
      this.check2 = check2;
    }
    
    public boolean matches(Dataset ds)
    {
      return check1.matches(ds) && check2.matches(ds);
    }
  }
  
  private static class OrDatasetChecker extends DatasetChecker
  {
    private DatasetChecker check1;
    private DatasetChecker check2;
    
    public OrDatasetChecker(DatasetChecker check1, DatasetChecker check2)
    {
      this.check1 = check1;
      this.check2 = check2;
    }
    
    public boolean matches(Dataset ds)
    {
      return check1.matches(ds) || check2.matches(ds);
    }
  }
  
  private static class ColumnIdentityChecker extends DatasetChecker
  {
    private String columnName;
    private String compare;
    
    public ColumnIdentityChecker(String columnName, String compareValue)
    {
      this.columnName = columnName;
      this.compare = compareValue.toLowerCase();
    }
    
    public boolean matches(Dataset ds)
    {
      try{
        return ds.get(columnName).equalsIgnoreCase(compare);
      } catch (Exception e) { return false; }
    }
  }

  private static class ColumnPrefixChecker extends DatasetChecker
  {
    private String columnName;
    private String compare;
    
    public ColumnPrefixChecker(String columnName, String compareValue)
    {
      this.columnName = columnName;
      this.compare = compareValue.toLowerCase();
    }
    
    public boolean matches(Dataset ds)
    {
      try{
        return ds.get(columnName).toLowerCase().startsWith(compare);
      }catch (Exception e){ return false; }
    }
  }

  private static class ColumnSuffixChecker extends DatasetChecker
  {
    private String columnName;
    private String compare;
    
    public ColumnSuffixChecker(String columnName, String compareValue)
    {
      this.columnName = columnName;
      this.compare = compareValue.toLowerCase();
    }
    
    public boolean matches(Dataset ds)
    {
      try{
        return ds.get(columnName).toLowerCase().endsWith(compare);
      }catch (Exception e){ return false; }
    }
  }

  private static class ColumnContainsChecker extends DatasetChecker
  {
    private String columnName;
    private String compare;
    
    public ColumnContainsChecker(String columnName, String compareValue)
    {
      this.columnName = columnName;
      this.compare = compareValue.toLowerCase();
    }
    
    public boolean matches(Dataset ds)
    {
      try{
        return ds.get(columnName).toLowerCase().indexOf(compare) >= 0;
      }catch (Exception e){ return false; }
    }
  }

  
}
