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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.HashMap;
import java.util.Map;

public class TestDJDataset implements DJDataset
{
  private Map data = new HashMap();
  
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    if (data.containsKey(spaltenName)) return (String)data.get(spaltenName);
    return spaltenName;
  }

  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    return data.containsKey(columnName);
  }

  public void set(String columnName, String newValue) throws ColumnNotFoundException  
  {
    data.put(columnName, newValue);
  }

  public void discardLocalOverride(String columnName) throws ColumnNotFoundException
  {
    data.remove(columnName);
  }
}
