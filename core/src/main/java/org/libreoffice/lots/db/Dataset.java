/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
 * Interface for records of a table.
 */
public interface Dataset
{
  /**
   * Returns the value of the record from the column columnName (null if not filled).
   *
   * @throws ColumnNotFoundException
   *           If the column does not exist. Note that this is a property of the database schema
   *           and has nothing to do with whether the value of the record is set in the
   *           corresponding column.
   */
  public String get(String columnName) throws ColumnNotFoundException;

  /**
   * Returns the key value of this record. This should uniquely identify the record in its database, but it's not mandatory.
   */
  public String getKey();
}
