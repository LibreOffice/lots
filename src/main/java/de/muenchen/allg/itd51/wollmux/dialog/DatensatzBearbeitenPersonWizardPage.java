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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.LocalOverrideStorageStandardImpl.LOSDJDataset;

public class DatensatzBearbeitenPersonWizardPage extends DatensatzBearbeitenBaseWizardPage
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenPersonWizardPage.class);

  public DatensatzBearbeitenPersonWizardPage(XWindow parentWindow, short pageId,
      LOSDJDataset dataset,
      List<String> dbSchema) throws Exception
  {
    super(pageId, parentWindow, "DatensatzBearbeitenPerson", dataset, dbSchema);

    XControlContainer controlContainerPerson = UnoRuntime.queryInterface(XControlContainer.class,
        window);
    setControlContainer(controlContainerPerson);
    
    try
    {
      XComboBox anredeComboBox = UNO.XComboBox(controlContainerPerson.getControl("Anrede"));
      XTextComponent anredeTextComponent = UNO.XTextComponent(anredeComboBox);
      anredeTextComponent.setText(dataset.get("Anrede") == null ? "" : dataset.get("Anrede"));

      anredeComboBox.removeItems((short) 0, anredeComboBox.getItemCount());
      anredeComboBox.addItems(new String[] { "Herr", "Frau" }, (short) 0);

      anredeComboBox.addItemListener(itemListener);

      for (String columnName : dbSchema)
      {
        XControl xControl = controlContainerPerson.getControl(columnName);

        if (xControl == null)
          continue;

        XTextComponent xTextComponent = UNO.XTextComponent(xControl);

        if (xTextComponent == null)
          continue;

        xTextComponent.setText(dataset.get(columnName) == null ? "" : dataset.get(columnName));
        
        if (dataset.isDifferentFromLdapDataset(columnName, dataset))
        {
          showAcceptLdapValueButton(columnName, true);
          super.setTextColor(xControl, 16711680); // rot
        }
      }
 
      for (XControl control : controlContainerPerson.getControls())
      {
        XTextComponent textComponent = UNO.XTextComponent(control);

        XButton xButton = UNO.XButton(control);

        if (textComponent != null)
          textComponent.addTextListener(textListener);

        if (xButton != null)
          xButton.addActionListener(buttonActionListener);
      }

    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }
  }
}
