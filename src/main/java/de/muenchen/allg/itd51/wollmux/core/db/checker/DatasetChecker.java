/*
 * Copyright (c) 2008-2015 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
 *
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db.checker;

import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryPart;

/**
 * Ein DatasetChecker überprüft, ob für ein Dataset eine bestimmte Bedingung erfüllt
 * ist.
 */
public interface DatasetChecker
{
  /**
   * Erzeugt einen DatasetChecker, der die Abfrage query auf der Spalte columnName
   * implementiert.
   * 
   * @param columnName
   *          der Name der zu checkenden Spalte
   * @param query
   *          ein Suchstring, der am Anfang und/oder Ende genau 1 Sternchen haben
   *          kann für Präfix/Suffix/Teilstringsuche
   * @return ein DatasetChecker, der Datensätze überprüft darauf, ob sie in Spalte
   *         columnName den Suchstring query stehen haben.
   */
  public static DatasetChecker makeChecker(String columnName, String query)
  {
    int i = query.startsWith("*") ? 1 : 0;
    i |= query.endsWith("*") ? 2 : 0;
    switch (i)
    {
      case 0:
        return new ColumnIdentityChecker(columnName, query);
      case 1:
        return new ColumnSuffixChecker(columnName, query.substring(1));
      case 2:
        return new ColumnPrefixChecker(columnName, query.substring(0,
          query.length() - 1));
      case 4:
      default:
        return new ColumnContainsChecker(columnName, query.substring(1,
          query.length() - 1));
    }
  }

  /**
   * Erzeugt einen DatasetChecker, der die Bedingungen einer Liste von QueryParts
   * (und-verknüpft) überprüft.
   * 
   * @param query
   *          Liste von QueryParts.
   */
  public static DatasetChecker makeChecker(List<QueryPart> query)
  {
    DatasetChecker checker = new MatchAllDatasetChecker();
    for (QueryPart part : query)
    {
      checker =
        checker.and(DatasetChecker.makeChecker(part.getColumnName(),
          part.getSearchString()));
    }
    return checker;
  }

  /**
   * Liefert true, wenn die Bedingung dieses Checkers auf ds zutrifft.
   */
  public boolean matches(Dataset ds);

  /**
   * Liefert einen DatasetChecker zurück, der die Bedingung von this und zusätzlich
   * die Bedingung von check2 prüft. Die matches() Funktion des zurückgelieferten
   * Checkers liefert nur true, wenn die matches() Methoden von beiden Checkern true
   * liefern.
   */
  public default DatasetChecker and(DatasetChecker check2)
  {
    return new AndDatasetChecker(this, check2);
  }

  /**
   * Liefert einen DatasetChecker zurück, der die Bedingung von this und zusätzlich
   * die Bedingung von check2 prüft. Die matches() Funktion des zurückgelieferten
   * Checkers liefert true, wenn die matches() Methode von mindestens einem der
   * beiden Checker true liefert.
   */
  public default DatasetChecker or(DatasetChecker check2)
  {
    return new OrDatasetChecker(this, check2);
  }
}
