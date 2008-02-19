//TODO L.m()
package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
 * mit einem bestimmten Suffix (CASE-INSENSITIVE) endet. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnSuffixChecker extends DatasetChecker
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