/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.ui;

import com.sun.star.awt.XComboBox;

import org.libreoffice.ext.unohelper.common.UNO;

/**
 * A wrapper for adding items to a {@link XComboBox}.
 */
public class SpecialField
{
  private SpecialField()
  {
    // nothing to do
  }

  /**
   * Adds some predefined items. The first one is selected.
   *
   * @param comboBox
   *          The {@link XComboBox}.
   */
  public static void addItems(XComboBox comboBox)
  {
    String[] items = new String[] { "Bitte wählen..", "Gender", "Wenn...Dann...Sonst",
        "Datensatznummer", "Serienbriefnummer", "Nächster Datensatz" };
    addItems(comboBox, items);
  }

  /**
   * Add some items. The first one is selected.
   *
   * @param comboBox
   *          The {@link XComboBox}.
   * @param items
   *          The items to set.
   */
  public static void addItems(XComboBox comboBox, String[] items)
  {
    comboBox.addItems(items, (short) 0);
    UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
  }
}
