/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.db;


/**
 * Teil einer Datenbankabfrage. Zur Zeit einfach nur ein Spaltenname und ein
 * Suchstring. Selektiert werden alle Datensätze, die in der entsprechenden Spalte
 * den Suchstring haben. Der Suchstring kann vorne und/oder hinten genau ein
 * Sternchen '*' stehen haben, um Präfix/Suffix/Teilstring-Suche zu realisieren.
 * Folgen mehrerer Sternchen oder Sternchen in der Mitte des Suchstrings sind
 * verboten und produzieren undefiniertes Verhalten. Ebenso verboten ist ein
 * Suchstring, der nur Sternchen enthält oder einer der leer ist.
 */
public class QueryPart
{
  private String columnName;

  private String searchString;

  public QueryPart(String spaltenName, String suchString)
  {
    if (spaltenName == null) {
      throw new NullPointerException();
    }
    if (suchString == null) {
      suchString = "";
    }
    columnName = spaltenName;
    searchString = suchString;
  }

  /**
   * Liefert den Name der zu testenden Spalten.
   */
  public String getColumnName()
  {
    return columnName;
  }

  /**
   * Liefert den Suchstring auf den getestet werden soll.
   */
  public String getSearchString()
  {
    return searchString;
  }

  @Override
  public String toString()
  {
    return columnName + "=" + searchString;
  }
}
