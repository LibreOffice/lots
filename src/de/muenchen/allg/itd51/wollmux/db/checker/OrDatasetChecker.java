package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der 2 andere Checker auswertet und die oder-Verknüpfung
 * ihrer matches() Ergebnisse liefert.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OrDatasetChecker extends DatasetChecker
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