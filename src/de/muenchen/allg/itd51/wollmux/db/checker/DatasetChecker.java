package de.muenchen.allg.itd51.wollmux.db.checker;

import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.QueryPart;

/**
 * Ein DatasetChecker überprüft, ob für ein Dataset eine bestimmte Bedingung
 * erfüllt ist.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class DatasetChecker
{
  /**
   * Erzeugt einen DatasetChecker, der die Abfrage query auf der Spalte
   * columnName implementiert. 
   * @param columnName der Name der zu checkenden Spalte
   * @param query ein Suchstring, der am Anfang und/oder Ende genau 1 Sternchen
   *        haben kann für Präfix/Suffix/Teilstringsuche
   * @return ein DatasetChecker, der Datensätze überprüft darauf, ob sie
   * in Spalte columnName den Suchstring query stehen haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DatasetChecker makeChecker(String columnName, String query)
  {
    int i = query.startsWith("*") ? 1 : 0;
    i |= query.endsWith("*") ? 2 : 0;
    switch(i)
    {
      case 0: return new ColumnIdentityChecker(columnName, query);
      case 1: return new ColumnSuffixChecker(columnName, query.substring(1));
      case 2: return new ColumnPrefixChecker(columnName, query.substring(0, query.length()-1));
      case 4: 
      default:  return new ColumnContainsChecker(columnName, query.substring(1,query.length()-1));
    }
  }
  
  /**
   * Erzeugt einen DatasetChecker, der die Bedingungen einer Liste von
   * QueryParts (und-verknüpft) überprüft.
   * @param query Liste von QueryParts.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static DatasetChecker makeChecker(List query)
  {
    DatasetChecker checker = new MatchAllDatasetChecker();
    Iterator iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart part = (QueryPart)iter.next();
      checker = checker.and(DatasetChecker.makeChecker(part.getColumnName(), part.getSearchString()));
    }
    return checker;
  }

  
  /**
   * Liefert true, wenn die Bedingung dieses Checkers auf ds zutrifft.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public abstract boolean matches(Dataset ds);

  /**
   * Liefert einen DatasetChecker zurück, der die Bedingung von this und
   * zusätzlich die Bedingung von check2 prüft. Die matches() Funktion des
   * zurückgelieferten Checkers liefert nur true, wenn die matches() Methoden
   * von beiden Checkern true liefern.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DatasetChecker and(DatasetChecker check2)
  { return new AndDatasetChecker(this, check2);}
  
  /**
   * Liefert einen DatasetChecker zurück, der die Bedingung von this und
   * zusätzlich die Bedingung von check2 prüft. Die matches() Funktion des
   * zurückgelieferten Checkers liefert true, wenn die matches() Methode
   * von mindestens einem der beiden Checker true liefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DatasetChecker or(DatasetChecker check2)
  { return new OrDatasetChecker(this, check2);}
}