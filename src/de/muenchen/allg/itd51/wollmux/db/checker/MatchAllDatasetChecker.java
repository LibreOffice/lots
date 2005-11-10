package de.muenchen.allg.itd51.wollmux.db.checker;

import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Ein DatasetChecker, der alle Datensätze durchwinkt.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MatchAllDatasetChecker extends DatasetChecker
{
  public boolean matches(Dataset ds) {return true;}
}