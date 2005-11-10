package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
 * mit einem bestimmten Präfix (CASE-INSENSITIVE) beginnt. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnPrefixChecker extends DatasetChecker
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