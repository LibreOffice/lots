/*
* Dateiname: TestDatasourceJoiner.java
* Projekt  : WollMux
* Funktion : Simple Implementierung eines DatasourceJoiners zum testen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 19.10.2005 | BNK | Erstellung
* 20.10.2005 | BNK | Fertig
* 20.10.2005 | BNK | Fallback Rolle -> OrgaKurz
* 24.10.2005 | BNK | Erweitert um die Features, die PAL Verwalten braucht
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TestDatasourceJoiner extends DatasourceJoiner
{
  private final static String[][] myBSData = new String[][]
{{"Vorname",   "Nachname",      "OrgaKurz",       "Rolle", "Mail"},
 {"Matthias",  "Benkmux",       "D-WOLL-MUX-5.1", null,      "matthias.benkmux@muenchen.de"},
 {"Christoph", "Mux",           "D-WOLL-MUX-5.1", null,      "christoph.mux@muenchen.de"},
 {"Matthias",  "Benkmann",      "D-III-ITD-5.1",  null,      "matthias.benkmann@muenchen.de"},
 {"Christoph", "Lutz",          "D-III-ITD-5.1",  null,      "christoph.lutz@muenchen.de"},
 {"Gertraud",  "Loesewitz",     "D-L",            null,      "gertraud.loesewitz@muenchen.de"},
 {"Kristina",  "Lorenz",        "D-L",            null,      "kristina.lorenz@muenchen.de"},
 {"Peter",     "Hofmann",       "D-III-LIMUX",    null,      "peter.hofmann@muenchen.de"},
 {"Manfred",   "Lubig-Konzett", "D-III-LIMUX",    null,      "manfred.lubig-konzett@muenchen.de"},
 {"Wilhelm",   "Hoegner",       "D-HAIII",        null,      "wilhelm.hoegner@muenchen.de"},
 {"Gerhard",   "Werner",        "D-WOLL-MUX-5.2", null,      "gerhard.werner@woanders.de"},
 {"Werner",    "Gerhard",       "D-WOLL-MUX-5.2", null,      "werner.gerhard@woanders.de"}
 }; 
  
  private List myBS = new Vector();
  private List myLOS = new Vector();
  private int indexOfSelectedDataset = -1;
  private Set mySchema;
  private Map fallback;
  
  public TestDatasourceJoiner()
  {
    this((Set)null);
  }
  
  public TestDatasourceJoiner(Set schema)
  {
    fallback = new HashMap();
    fallback.put("Rolle","OrgaKurz");
    
    String[] spalten = myBSData[0];
    
    if (schema != null)
      mySchema = new HashSet(schema);
    else
    {
      mySchema = new HashSet();
      for (int i = 0; i < spalten.length; ++i)
        mySchema.add(spalten[i]);
    }
      
    for (int i = 1; i < myBSData.length; ++i)
    {
      Map dsBS = new HashMap();
      String[] ds = myBSData[i];
      for (int j = 0; j < ds.length; ++j)
      {
        dsBS.put(spalten[j], ds[j]);
      }
      myBS.add(new MyTestDJDataset(dsBS, mySchema, false, fallback));
    }
  }
  
  
  public QueryResults find(String spaltenName, String suchString)
  {
    suchString = suchString.replaceAll("\\*\\*","*");
    return find(makeChecker(spaltenName, suchString));
  }
  
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2)
  {
    suchString1 = suchString1.replaceAll("\\*\\*","*");
    suchString2 = suchString2.replaceAll("\\*\\*","*");
    return find(makeChecker(spaltenName1, suchString1).and(makeChecker(spaltenName2,suchString2)));
  }
  
  private QueryResults find(DatasetChecker check)
  {
    List results = new Vector();
    
    Iterator iter = myBS.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      if (check.matches(ds)) results.add(ds);
    }
    return new MyQueryResults(results);
  }
  
  /**
   * Liefert den momentan in der Datensatzliste ausgewählten Datensatz.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    if (myLOS.isEmpty()) throw new DatasetNotFoundException("Der Lokale Override Speicher ist leer");
    return (DJDataset)myLOS.get(indexOfSelectedDataset);
  }
  
  /**
   * Liefert alle Datensätze (als {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}) des Lokalen Override Speichers.
   */
  public QueryResults getLOS()
  {
    return new MyQueryResults(new Vector(myLOS));
  }
  
  public DJDataset newDataset()
  {
    DJDataset ds = new MyTestDJDataset(null, mySchema, true, fallback);
    myLOS.add(ds);
    return ds;
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
      } catch (ColumnNotFoundException e) { return false; }
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
      }catch (ColumnNotFoundException e){ return false; }
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
      }catch (ColumnNotFoundException e){ return false; }
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
      }catch (ColumnNotFoundException e){ return false; }
    }
  }

  
  private static class MyQueryResults implements QueryResults
  {
    private List results;
    
    public MyQueryResults(List results)
    {
      this.results = results;
    }
    
    public int size()
    {
      return results.size();
    }

    public Iterator iterator()
    {
      return results.iterator();
    }

    public boolean isEmpty()
    {
      return results.isEmpty();
    }
  }
  
  public static void printResults(String query, Set schema, QueryResults results)
  {
    System.out.println("Results for query \""+query+"\":");
    String[] spalten = myBSData[0];
    Iterator resIter = results.iterator();
    while (resIter.hasNext())
    {
      Dataset result = (Dataset)resIter.next();
      
      for (int i = 0; i < spalten.length; ++i)
      {
        String spalte = spalten[i];
        String wert = "Spalte "+spalte+" nicht gefunden!";
        try{ wert = result.get(spalte); }catch(ColumnNotFoundException x){};
        System.out.print("\""+wert+"\""+(i+1 < spalten.length?", ":""));
      }
      System.out.println();
    }
    System.out.println();
  }
  
  private class MyTestDJDataset extends TestDJDataset
  {
    public MyTestDJDataset(Map backingStore, Set schema, boolean isFromLOS, Map fallback)
    {
      super(backingStore, schema, isFromLOS, fallback);
    }
    
    public DJDataset copy()
    {
      MyTestDJDataset newds = new MyTestDJDataset(new HashMap(this.getBS()), mySchema, true, fallback); 
      myLOS.add(newds);
      return newds;
    }
    
    public void remove()
    {
      if (!isFromLOS()) throw new UnsupportedOperationException("Versuch, einen Datensatz, der nicht aus dem LOS kommt zu entfernen");
      myLOS.remove(this);
    }
  }
  
  public static void main(String[] args)
  {
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    printResults("Nachname = Benkmux", dj.mySchema, dj.find("Nachname","Benkmux"));
    printResults("Nachname = Benkm*", dj.mySchema, dj.find("Nachname","Benkm*"));
    printResults("Nachname = *ux", dj.mySchema, dj.find("Nachname","*ux"));
    printResults("Nachname = *oe*", dj.mySchema, dj.find("Nachname","*oe*"));
    
  }
  
}
