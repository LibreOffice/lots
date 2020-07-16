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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.db.LocalOverrideStorageStandardImpl.LOSDJDataset;

/**
 * Wizard page for modifying the personal data.
 */
public class DatensatzBearbeitenPersonWizardPage extends DatensatzBearbeitenBaseWizardPage
{

  /**
   * Create the new page.
   *
   * @param parentWindow
   *          The window in which the page is created.
   * @param pageId
   *          The ID of the page.
   * @param dataset
   *          The data set to be displayed.
   * @param dbSchema
   *          The schema of the datase.t
   * @throws Exception
   *           The page can't be created.
   */
  public DatensatzBearbeitenPersonWizardPage(XWindow parentWindow, short pageId, LOSDJDataset dataset,
      List<String> dbSchema) throws Exception
  {
    super(pageId, parentWindow, "DatensatzBearbeitenPerson", dataset, dbSchema);

    XComboBox anredeComboBox = UNO.XComboBox(UNO.XControlContainer(window).getControl("Anrede"));

    anredeComboBox.removeItems((short) 0, anredeComboBox.getItemCount());
    anredeComboBox.addItems(new String[] { "Herr", "Frau" }, (short) 0);
  }
}
