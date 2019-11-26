/*
 * Dateiname: ComboboxMergeDescriptor.java
 * Projekt  : WollMux
 * Funktion : Beschreibt das Verschmelzen von Checkboxen zu einer ComboBox.
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 22.08.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;

/**
 * Enthält Informationen über eine erfolgte verschmelzung mehrerer Checkboxen zu
 * einer einzigen Combobox.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ComboboxMergeDescriptor
{
  /**
   * Das aus dem Merge neu hervorgegangene {@link FormControlModel}.
   */
  public FormControlModel combo;

  /**
   * Eine {@link Map}, deren Schlüssel die {@link IDManager.ID}s der Checkboxen
   * sind, die verschmolzen wurden, wobei jede dieser IDs auf einen String gemappt
   * wird, der den ComboBox-Wert beschreibt, den auszuwählen dem Aktivieren der alten
   * Checkbox entspricht.
   */
  public Map<IDManager.ID, String> mapCheckboxId2ComboboxEntry;
}
