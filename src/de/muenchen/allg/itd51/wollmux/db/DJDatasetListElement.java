/*
 * Dateiname: DJDatasetListElement.java
 * Projekt  : WollMux
 * Funktion : Wrapper um ein vom DJ gelieferten Datensatz für die Darstellung 
 *            in einer Liste.
 * 
 * Copyright (c) 2008-2016 Landeshauptstadt München
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
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 03.11.2005 | LUT | Erstellung
 * 06.04.2010 | BED | +Icon
 * 07.04.2010 | BED | displayTemplate als Instanzvariable
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import javax.swing.Icon;

public class DJDatasetListElement extends DatasetListElement
{
  /**
   * Enthält das DJDataset-Element.
   */
  private DJDataset ds;

  /**
   * Erzeugt ein neues DJDatasetListElement für die Darstellung in einer Liste (ohne
   * Icon).
   * 
   * @param ds
   *          Das DJDatasetElement das über das DJDatasetListElement dargestellt
   *          werden soll.
   * @param displayTemplate
   *          gibt an, wie die Personen in den Listen angezeigt werden sollen.
   *          %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen,
   *          z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public DJDatasetListElement(DJDataset ds, String displayTemplate)
  {
    this(ds, displayTemplate, null);
  }

  /**
   * Erzeugt ein neues DJDatasetListElement für die Darstellung in einer Liste.
   * 
   * @param ds
   *          das DJDatasetElement das über das DJDatasetListElement dargestellt
   *          werden soll.
   * @param displayTemplate
   *          gibt an, wie die Personen in den Listen angezeigt werden sollen.
   *          %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen,
   *          z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   * @param icon
   *          das Icon, das in der Liste für das Element verwendet werden soll. Falls
   *          kein Icon vorhanden ist, kann <code>null</code> übergeben werden.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public DJDatasetListElement(DJDataset ds, String displayTemplate, Icon icon)
  {
    super(ds, displayTemplate, icon);
    this.ds = ds;
  }

  /**
   * Liefert den DJDataset dieses DJDatasetListElements.
   * 
   * @return den DJDataset dieses DJDatasetListElements.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public DJDataset getDataset()
  {
    return ds;
  }
}
