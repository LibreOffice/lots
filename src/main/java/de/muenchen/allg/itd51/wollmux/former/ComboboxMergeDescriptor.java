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
