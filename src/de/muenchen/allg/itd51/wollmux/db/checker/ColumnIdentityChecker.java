//TODO L.m()
package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der Datensätze darauf überprüft, ob sie einen exakten
 * String (allerdings CASE-INSENSITIVE) in einer Spalte haben.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnIdentityChecker extends DatasetChecker
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