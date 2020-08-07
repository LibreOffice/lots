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

import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;

/**
 * Pages for modifying the content of a data set.
 */
public class DatensatzBearbeitenWizardPage extends AbstractXWizardPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DatensatzBearbeitenWizardPage.class);
  private LOSDJDataset dataset;
  private List<String> dbSchema;
  private XControlContainer controlContainer;

  /**
   * Create the new page.
   *
   * @param pageId
   *          The ID of the page.
   * @param parentWindow
   *          The window in which the page is created.
   * @param dialogName
   *          The name of the dialog to be inserted into that page.
   * @param dataset
   *          The data set to be displayed.
   * @param dbSchema
   *          The schema of the datase.t
   * @throws Exception
   *           The page can't be created.
   */
  public DatensatzBearbeitenWizardPage(short pageId, XWindow parentWindow, String dialogName,
      LOSDJDataset dataset, List<String> dbSchema) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux." + dialogName + "?location=application");
    this.dataset = dataset;
    this.dbSchema = dbSchema;
    this.controlContainer = UNO.XControlContainer(window);
    addListenerToControls();
    initControlsWithDatabaseValues();
  }

  private void addListenerToControls()
  {
    for (XControl control : controlContainer.getControls())
    {
      XTextComponent textComponent = UNO.XTextComponent(control);

      XButton xButton = UNO.XButton(control);

      if (textComponent != null)
        textComponent.addTextListener(textListener);

      if (xButton != null)
        xButton.addActionListener(buttonActionListener);
    }
  }

  private void initControlsWithDatabaseValues()
  {
    try
    {
      for (String columnName : dbSchema)
      {
        XTextComponent xTextComponent = UNO.XTextComponent(controlContainer.getControl(columnName));

        if (xTextComponent == null)
          continue;

        // TODO LO allow import of empty values in MenuPopupElement::startChildElement
        if ("Anrede".equals(columnName))
        {
          XComboBox comboBox = UNO.XComboBox(xTextComponent);
          if (comboBox != null)
          {
            comboBox.addItem("", comboBox.getItemCount());
          }
        }

        xTextComponent.setText(dataset.get(columnName) == null ? "" : dataset.get(columnName));
      }
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }
  }

  private void showAcceptLdapValueButton(String columnName, boolean visible)
  {
    if (this.controlContainer == null)
    {
      LOGGER.error("DatensatzBearbeitenBaseWizardPage: showAcceptLdapValueButton: ControlContainer ist NULL.");
      return;
    }

    XControl xControl = this.controlContainer.getControl("btn" + columnName);

    if (xControl == null)
    {
      LOGGER.debug("DatensatzBearbeitenBaseWizardPage: showAcceptLdapValueButton: xControl is NULL.");
      return;
    }

    Utils.setProperty(xControl.getModel(), UnoProperty.ENABLE_VISIBLE, visible);
  }

  private void setTextColor(XControl xControl, int textColor)
  {
    if (xControl == null)
      return;

    XControlModel xControlModel = xControl.getModel();

    if (xControlModel == null)
      return;

    Utils.setProperty(xControlModel, UnoProperty.TEXT_COLOR, textColor);
  }

  private AbstractActionListener buttonActionListener = event -> {
    XControl xControl = UNO.XControl(event.Source);

    if (xControl == null)
      return;

    try
    {
      String label = (String) UnoProperty.getProperty(xControl.getModel(), UnoProperty.NAME);

      // btnVorname -> Vorname
      String buttonLabel = label.substring(3);

      XControl targetTextField = controlContainer.getControl(buttonLabel);

      if (targetTextField == null)
        return;

      XTextComponent xTextComponent = UNO.XTextComponent(targetTextField);

      if (xTextComponent == null)
        return;

      String ldapValue = dataset.getBS().get(buttonLabel);

      int res = 0;

      if (ldapValue == null || ldapValue.isEmpty())
      {
        ldapValue = "";
        res = InfoDialog.showYesNoModal("LDAP Datensatz", "Leeren Wert aus LDAP-Datensatz übernehmen?");
      } else
      {
        res = InfoDialog.showYesNoModal("LDAP Datensatz", "Wert \"" + ldapValue + "\" aus LDAP-Datensatz übernehmen?");
      }

      if (res == MessageBoxResults.YES)
      {
        xTextComponent.setText(ldapValue);

        // hide button
        UNO.XWindow(xControl).setVisible(false);
        setTextColor(targetTextField, 00000000);
      }

    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  };

  private AbstractTextListener textListener = event -> {
    XControl xControl = UNO.XControl(event.Source);

    if (xControl == null)
      return;

    XControlModel xControlModel = xControl.getModel();

    if (xControlModel == null)
      return;

    try
    {
      XTextComponent xTextComponent = UNO.XTextComponent(xControl);

      if (xTextComponent != null)
      {
        String label = (String) UnoProperty.getProperty(xControlModel, UnoProperty.NAME);
        String textComponentText = xTextComponent.getText();
        if (dataset.getBS() == null)
        {
          showAcceptLdapValueButton(label, false);
          setTextColor(xControl, 00000000);
        } else
        {
          String dataSetValue = dataset.getBS().get(label);
          if (dataSetValue == null)
          {
            dataSetValue = "";
          }
          if (!dataSetValue.equals(textComponentText))
          {
            showAcceptLdapValueButton(label, true);
            setTextColor(xControl, 16711680);
          } else
          {
            showAcceptLdapValueButton(label, false);
            setTextColor(xControl, 00000000);
          }
        }
      }

    } catch (IllegalArgumentException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }

  };

  @Override
  public boolean commitPage(short arg0)
  {
    try
    {
      for (XControl xControl : controlContainer.getControls())
      {
        XTextComponent xTextComponent = UNO.XTextComponent(xControl);

        XControlModel xControlModel = xControl.getModel();
        String label = (String) UnoProperty.getProperty(xControlModel, UnoProperty.NAME);

        if (xTextComponent != null)
          dataset.set(label, xTextComponent.getText());
      }
    } catch (ColumnNotFoundException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }

    new OnPALChangedNotify().emit();

    return true;
  }

  @Override
  public boolean canAdvance()
  {
    return true;
  }

}
