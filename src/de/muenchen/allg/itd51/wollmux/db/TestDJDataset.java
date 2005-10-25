/*
* Dateiname: TestDJDataset.java
* Projekt  : WollMux
* Funktion : Minimal-Implementierung von DJDataset zu Testzwecken.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* 20.10.2005 | BNK | Stark erweitert 
* 20.10.2005 | BNK | Unterstützung für Fallback
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestDJDataset implements DJDataset
{
  private Map myLOS = new HashMap();
  private Map myBS = new HashMap();
  private Set schema = null;
  private boolean isFromLOS = true;
  private Map fallback = null;
  
  /** Erzeugt einen TestDJDataset, der jedes Schema unterstützt und bei
   * als aus dem LOS kommend betrachtet wird.
   */
  public TestDJDataset() {}
  
  /**
   * 
   * @param backingStore mappt Spaltennamen auf den Spaltenwert des Datensatzes
   *        in der Hintergrunddatenbank. Es müssen nicht alle Spalten enthalten
   *        sein. Der Mechanismus zum automatischen Generieren von Spaltenwerten
   *        identisch zum Spaltennamen existiert weiter.
   * @param schema falls nicht null übergeben wird, erzeugen Zugriffe auf
   *        Spalten mit Namen, die nicht in schema sind Exceptions.
   * @param isFromLOS legt fest, ob der Datensatz als aus dem LOS kommend
   *        betrachtet werden soll (also insbesondere ob er {@link #set(String, String)}
   *        unterstützen soll).
   * @param fallback falls fallback nicht null ist, so wird 
   *        falls der Wert für eine Spalte nicht gesetzt ist (nicht
   *        zu verwechseln mit gesetzt auf den leeren String!), so wird versucht,
   *        anhand dieser Map der Spaltenname auf einen anderen Spaltennamen
   *        umzusetzen, dessen Wert dann geliefert wird. 
   */
  public TestDJDataset(Map backingStore, Set schema, boolean isFromLOS, Map fallback)
  {
    myBS = backingStore;
    this.schema = schema;
    this.isFromLOS = isFromLOS;
    this.fallback = fallback;
  }
  
  /** 
   * Liefert die Map, die dem Konstruktor als backingStore Argument übergeben
   * wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Map getBS()
  {
    return myBS;
  }
  
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    if (schema != null && !schema.contains(spaltenName)) throw new ColumnNotFoundException("Spalte "+spaltenName+" existiert nicht!");
    String res;
    res = (String)myLOS.get(spaltenName);
    if (res != null) return res;
    if (myBS != null)
    {
      res = (String)myBS.get(spaltenName);
      if (res != null) return res;
    }
    if (fallback != null && fallback.containsKey(spaltenName)) 
      return get((String)fallback.get(spaltenName)); 
    return spaltenName;
  }

  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    return myLOS.containsKey(columnName);
  }

  public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException  
  {
    if (!isFromLOS) throw new UnsupportedOperationException("Nur Datensätze aus dem LOS können manipuliert werden!");
    myLOS.put(columnName, newValue);
  }

  public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
  {
    if (myBS == null) throw new NoBackingStoreException("Datensatz nicht mit Hintergrundspeicher verknüpft");
    myLOS.remove(columnName);
  }

  public boolean isFromLOS()
  {
    return isFromLOS;
  }
  
  public boolean hasBackingStore() {return myBS != null;}

  public DJDataset copy() { return new TestDJDataset(myBS == null? null: new HashMap(myBS), schema, true, fallback);}
  
  public void remove(){}

  public boolean isSelectedDataset()
  {
    return false;
  }

  public void select() throws UnsupportedOperationException
  {
    if (!isFromLOS()) throw new UnsupportedOperationException();
  };
}
