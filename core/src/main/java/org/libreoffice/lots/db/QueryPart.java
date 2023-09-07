/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;


/**
 * Part of a database query. Currently, it consists of only a column name and a search string.
 * It selects all records that have the search string in the corresponding column.
 * The search string can have exactly one asterisk '*' at the beginning and/or at the end to perform prefix/suffix/partial
 * string search. Multiple asterisks or asterisks in the middle of the search string are prohibited
 * and result in undefined behavior. Also, a search string that consists solely of asterisks or is empty is not allowed.
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
   * Returns the name of the columns to be tested.
   */
  public String getColumnName()
  {
    return columnName;
  }

  /**
   * Returns the search string to test for.
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
