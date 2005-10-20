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
   */
  public TestDJDataset(Map backingStore, Set schema, boolean isFromLOS)
  {
    myBS = backingStore;
    this.schema = schema;
    this.isFromLOS = isFromLOS;
  }
  
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    if (myLOS.containsKey(spaltenName)) return (String)myLOS.get(spaltenName);
    if (myBS.containsKey(spaltenName)) return (String)myBS.get(spaltenName);
    if (schema != null && !schema.contains(spaltenName)) throw new ColumnNotFoundException("Spalte "+spaltenName+" existiert nicht!"); 
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

  public void discardLocalOverride(String columnName) throws ColumnNotFoundException
  {
    myLOS.remove(columnName);
  }

  public boolean isFromLOS()
  {
    return isFromLOS;
  }
}
