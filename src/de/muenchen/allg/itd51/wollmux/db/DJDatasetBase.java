/*
* Dateiname: DJDatasetBase.java
* Projekt  : WollMux
* Funktion : Basisklasse für DJDataset-Implementierungen
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Map;
import java.util.Set;

public abstract class DJDatasetBase implements DJDataset
{
  protected Map myLOS;
  protected Map myBS;
  protected Set schema;
  
  /**
   * 
   * @param backingStore mappt Spaltennamen auf den Spaltenwert des Datensatzes
   *        in der Hintergrunddatenbank. Spalten, die nicht enthalten sind
   *        werden als im Datensatz unbelegt betrachtet.
   *        Als backingStore kann null übergeben werden (für einen Datensatz,
   *        der nur aus dem LOS kommt ohne Hintergrundspeicher).
   * @param overrideStore mappt Spaltenname auf den Spaltenwert im LOS-
   *        Ist eine Spalte nicht vorhanden, so hat sie keinen Override im LOS.
   *        Wird für overrideStore null übergeben, so wird der Datensatz
   *        als nicht aus dem LOS kommend betrachtet.
   * @param schema falls nicht null übergeben wird, erzeugen Zugriffe auf
   *        Spalten mit Namen, die nicht in schema sind Exceptions.
   */
  public DJDatasetBase(Map backingStore, Map overrideStore, Set schema)
  {
    myBS = backingStore;
    myLOS = overrideStore;
    this.schema = schema;
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
  
  /** 
   * Liefert die Map, die dem Konstruktor als overrideStore Argument übergeben
   * wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Map getLOS()
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
      return res;
    }
    return null;
  }

  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    if (hasBackingStore())
      return myLOS.get(columnName)!=null;
    else
      return true;
  }

  public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException  
  {
    if (!isFromLOS()) throw new UnsupportedOperationException("Nur Datensätze aus dem LOS können manipuliert werden");
    if (newValue == null) throw new IllegalArgumentException("Override kann nicht null sein");
    myLOS.put(columnName, newValue);
  }

  public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
  {
    if (!isFromLOS()) return;
    if (!hasBackingStore()) throw new NoBackingStoreException("Datensatz nicht mit Hintergrundspeicher verknüpft");
    myLOS.remove(columnName);
  }

  public boolean isFromLOS(){return myLOS != null;}
  
  public boolean hasBackingStore() {return myBS != null;}

  public abstract DJDataset copy();
  
  public abstract void remove() throws UnsupportedOperationException;

  public abstract boolean isSelectedDataset();
  
  public abstract void select() throws UnsupportedOperationException;
}
