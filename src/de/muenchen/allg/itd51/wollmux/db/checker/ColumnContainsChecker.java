package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der überprüft ob der Wert einer gegebenen Spalte
 * einen bestimmten Teilstring (CASE-INSENSITIVE) enthält. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnContainsChecker extends DatasetChecker
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