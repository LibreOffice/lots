/*
* Dateiname: ComboboxMergeDescriptor.java
* Projekt  : WollMux
* Funktion : Beschreibt das Verschmelzen von Checkboxen zu einer ComboBox.
* 
* Copyright: Landeshauptstadt München
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
 * Enthält Informationen über eine erfolgte verschmelzung mehrerer Checkboxen
 * zu einer einzigen Combobox.
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
   * Eine {@link Map}, deren Schlüssel die {@link IDManager.ID}s der Checkboxen sind, die
   * verschmolzen wurden, wobei jede dieser IDs auf einen String gemappt wird, der
   * den ComboBox-Wert beschreibt, den auszuwählen dem Aktivieren der alten Checkbox entspricht.
   */
  public Map mapCheckboxId2ComboboxEntry;
}
