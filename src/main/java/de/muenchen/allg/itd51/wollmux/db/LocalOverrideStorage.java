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

import java.util.List;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

public interface LocalOverrideStorage extends Iterable<Dataset>
{

  /**
   * Falls es im LOS momentan mindestens einen Datensatz mit Schlüssel selectKey
   * gibt, so wird der durch sameKeyIndex bezeichnete zum ausgewählten Datensatz,
   * ansonsten wird, falls der LOS mindestens einen Datensatz enthält, ein
   * beliebiger Datensatz ausgewählt.
   * 
   * @param sameKeyIndex
   *          zählt ab 0 und gibt an, der wievielte Datensatz gewählt werden soll,
   *          wenn mehrere mit gleichem Schlüssel vorhanden sind. Sollte
   *          sameKeyIndex zu hoch sein, wird der letzte Datensatz mit dem
   *          entsprechenden Schlüssel ausgewählt.
   */
  public void selectDataset(String selectKey, int sameKeyIndex);

  /**
   * Erzeugt einen neuen Datensatz, der nicht mit Hintergrundspeicher verknüpft
   * ist.
   */
  public DJDataset newDataset();

  /**
   * Erzeugt eine Kopie im LOS vom Datensatz ds, der nicht aus dem LOS kommen darf.
   */
  public DJDataset copyNonLOSDataset(Dataset ds);

  /**
   * Liefert den momentan im LOS selektierten Datensatz zurück.
   * 
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (sonst ist immer ein Datensatz selektiert).
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException;

  /**
   * Liefert die Anzahl der Datensätze im LOS, die den selben Schlüssel haben wie
   * der ausgewählte, und die vor diesem in der LOS-Liste gespeichert sind.
   * 
   * @throws DatasetNotFoundException
   *           falls der LOS leer ist (ansonsten ist immer ein Datensatz
   *           selektiert).
   */
  public int getSelectedDatasetSameKeyIndex() throws DatasetNotFoundException;

  /**
   * Läd für die Datensätze des LOS aktuelle Daten aus der Datenbank database.
   * 
   * @param database
   *          hiervon wird das Feld lostDatasets geupdatet.
   */
  public List<Dataset> refreshFromDatabase(Datasource database);

  /**
   * Liefert null, falls bislang kein Schema vorhanden (weil das Laden der
   * Cache-Datei im Konstruktur fehlgeschlagen ist).
   */
  public List<String> getSchema();

  /**
   * Fügt conf die Beschreibung der Datensätze im LOS als Kinder hinzu.
   */
  public void dumpData(ConfigThingy conf);

  /**
   * Ändert das Datenbankschema. Spalten des alten Schemas, die im neuen nicht mehr
   * vorhanden sind werden aus den Datensätzen gelöscht. Im neuen Schema
   * hinzugekommene Spalten werden in den Datensätzen als unbelegt betrachtet.
   */
  public void setSchema(List<String> schema);

  /**
   * Liefert die Anzahl der Datensätze im LOS.
   */
  public int size();

  /**
   * true, falls der LOS leer ist.
   */
  public boolean isEmpty();

}
