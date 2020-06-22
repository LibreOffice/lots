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
package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Interface für Datensätze einer Tabelle.
 */
public interface Dataset
{
  /**
   * Liefert den Wert des Datensatzes aus der Spalte columnName (null falls nicht belegt).
   * 
   * @throws ColumnNotFoundException
   *           falls die Spalte nicht existiert. Man beachte, dass dies eine Eigenschaft des
   *           Datenbankschemas ist und nichts damit zu tun hat, ob der Wert des Datensatzes in der
   *           entsprechenden Spalte gesetzt ist.
   */
  public String get(String columnName) throws ColumnNotFoundException;

  /**
   * Liefert den Schlüsselwert dieses Datensatzes. Dieser sollte den Datensatz in seiner Datenbank
   * eindeutig identifizieren muss es aber nicht.
   */
  public String getKey();
}
