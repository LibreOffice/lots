/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.model.ID;

/**
 * Enthält Informationen über eine erfolgte verschmelzung mehrerer Checkboxen zu
 * einer einzigen Combobox.
 */
public class ComboboxMergeDescriptor
{

  /**
   * Das aus dem Merge neu hervorgegangene {@link FormControlModel}.
   */
  private FormControlModel combo;

  /**
   * Eine {@link Map}, deren Schlüssel die {@link IDManager.ID}s der Checkboxen
   * sind, die verschmolzen wurden, wobei jede dieser IDs auf einen String gemappt
   * wird, der den ComboBox-Wert beschreibt, den auszuwählen dem Aktivieren der alten
   * Checkbox entspricht.
   */
  private Map<ID, String> mapCheckboxId2ComboboxEntry;

  /**
   * New ComboboxMergeDescriptor.
   *
   * @param combo
   *          The new {@link FormControlModel}.
   * @param mapCheckboxId2ComboboxEntry
   *          List of merged checkboxes.
   */
  public ComboboxMergeDescriptor(FormControlModel combo, Map<ID, String> mapCheckboxId2ComboboxEntry)
  {
    this.combo = combo;
    this.mapCheckboxId2ComboboxEntry = mapCheckboxId2ComboboxEntry;
  }

  public FormControlModel getCombo()
  {
    return combo;
  }

  public Map<ID, String> getMapCheckboxId2ComboboxEntry()
  {
    return mapCheckboxId2ComboboxEntry;
  }
}
