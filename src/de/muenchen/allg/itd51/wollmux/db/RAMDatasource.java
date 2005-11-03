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
* 03.11.2005 | BNK | besser kommentiert
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
  /**
   * Das Schema dieser Datenquelle.
   */
  private Set schema;
  
  /**
   * Liste aller Datasets, die in dieser Datasource gespeichert sind.
   */
  private List data;
  
  /**
   * Der Name dieser Datenquelle.
   */
  private String name;
  
  /**
   * Erzeugt eine neue RAMDatasource mit Namen name.
   * data und schema werden direkt als Referenz eingebunden, nicht kopiert.
   * @param name der Name der Datenquelle
   * @param schema das Schema der Datenquelle
   * @param data die Datensätze der Datenquelle
   */
  public RAMDatasource(String name, Set schema, List data)
  {
    init(name, schema, data);
  }
  
  /**
   * Führt die Initialisierungsaktionen des Konstruktors mit den gleichen
   * Parametern aus. Diese Methode sollte von abgeleiteten Klassen verwendet
   * werden, wenn sie den Konstruktor ohne Argumente verwenden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected void init(String name, Set schema, List data)
  {
    this.schema = schema; 
    this.data = data;
    this.name = name;
  }
  
  /**
  * Erzeugt eine uninitialisierte RAMDatasource. Eine abgeleitete Klasse, die diesen
  * Konstruktor verwendet sollte init() aufrufen, um die nötigen Initialisierungen
  * zu erledigen. 
  */
  protected RAMDatasource(){};

  public Set getSchema()
  { //TESTED
    return new HashSet(schema);
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout)
      throws TimeoutException
  { //TESTED
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
  { //TESTED
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

  
  /**
   * Erzeugt einen DatasetChecker, der die Abfrage query auf der Spalte
   * columnName implementiert. 
   * @param columnName der Name der zu checkenden Spalte
   * @param query ein Suchstring, der am Anfang und/oder Ende genau 1 Sternchen
   *        haben kann für Präfix/Suffix/Teilstringsuche
   * @return ein DatasetChecker, der Datensätze überprüft darauf, ob sie
   * in Spalte columnName den Suchstring query stehen haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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
  
  /**
   * Ein DatasetChecker überprüft, ob für ein Dataset eine bestimmte Bedingung
   * erfüllt ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static abstract class DatasetChecker
  {
    /**
     * Liefert true, wenn die Bedingung dieses Checkers auf ds zutrifft.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public abstract boolean matches(Dataset ds);

    /**
     * Liefert einen DatasetChecker zurück, der die Bedingung von this und
     * zusätzlich die Bedingung von check2 prüft. Die matches() Funktion des
     * zurückgelieferten Checkers liefert nur true, wenn die matches() Methoden
     * von beiden Checkern true liefern.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DatasetChecker and(DatasetChecker check2)
    { return new AndDatasetChecker(this, check2);}
    
    /**
     * Liefert einen DatasetChecker zurück, der die Bedingung von this und
     * zusätzlich die Bedingung von check2 prüft. Die matches() Funktion des
     * zurückgelieferten Checkers liefert true, wenn die matches() Methode
     * von mindestens einem der beiden Checker true liefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DatasetChecker or(DatasetChecker check2)
    { return new OrDatasetChecker(this, check2);}
  }
  
  /**
   * Ein DatasetChecker, der alle Datensätze durchwinkt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MatchAllDatasetChecker extends DatasetChecker
  {
    public boolean matches(Dataset ds) {return true;}
  }

  
  /**
   * Ein DatasetChecker, der 2 andere Checker auswertet und die und-Verknüpfung
   * ihrer matches() Ergebnisse liefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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

  /**
   * Ein DatasetChecker, der 2 andere Checker auswertet und die oder-Verknüpfung
   * ihrer matches() Ergebnisse liefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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
  
  /**
   * Ein DatasetChecker, der Datensätze darauf überprüft, ob sie einen exakten
   * String (allerdings CASE-INSENSITIVE) in einer Spalte haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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

  /**
   * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
   * mit einem bestimmten Präfix (CASE-INSENSITIVE) beginnt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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

  /**
   * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
   * mit einem bestimmten Suffix (CASE-INSENSITIVE) endet. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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

  /**
   * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
   * einen bestimmten Teilstring (CASE-INSENSITIVE) enthält. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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
