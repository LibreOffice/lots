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
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.former.control.model.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel;

/**
 * Contains information about a happened fusing together of multiple checkboxes to
 * a single combobox.
 */
public class ComboboxMergeDescriptor
{

  /**
   * The arised {@link FormControlModel} from the merger.
   */
  private FormControlModel combo;

  /**
   * A {@link Map}, which keys are the {@link IdModel}s of the checkboxes,
   * which have been merged, in which every of these IDs were mapped on a string,
   * which describes the combobox value. The choose one of them matches the activation of the old
   * checkbox.
   */
  private Map<IdModel, String> mapCheckboxId2ComboboxEntry;

  /**
   * New ComboboxMergeDescriptor.
   *
   * @param combo
   *          The new {@link FormControlModel}.
   * @param mapCheckboxId2ComboboxEntry
   *          List of merged checkboxes.
   */
  public ComboboxMergeDescriptor(FormControlModel combo, Map<IdModel, String> mapCheckboxId2ComboboxEntry)
  {
    this.combo = combo;
    this.mapCheckboxId2ComboboxEntry = mapCheckboxId2ComboboxEntry;
  }

  public FormControlModel getCombo()
  {
    return combo;
  }

  public Map<IdModel, String> getMapCheckboxId2ComboboxEntry()
  {
    return mapCheckboxId2ComboboxEntry;
  }
}
