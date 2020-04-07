package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.TextEvent;
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
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;
import de.muenchen.allg.util.UnoProperty;

public abstract class DatensatzBearbeitenBaseWizardPage extends AbstractXWizardPage
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenBaseWizardPage.class);
  protected LOSDJDataset dataset;
  protected List<String> dbSchema;
  private XControlContainer controlContainer;
  private short selectedItemIndex = 0;

  public DatensatzBearbeitenBaseWizardPage(short pageId, XWindow parentWindow, String dialogName,
      DJDataset dataset, List<String> dbSchema) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux." + dialogName + "?location=application");
    this.dataset = (LOSDJDataset) dataset;
    this.dbSchema = dbSchema;
  }

  protected void setControlContainer(XControlContainer controlContainer)
  {
    this.controlContainer = controlContainer;
  }

  protected void showAcceptLdapValueButton(String columnName, boolean visible)
  {
    if (this.controlContainer == null)
    {
      LOGGER.error(
          "DatensatzBearbeitenBaseWizardPage: showAcceptLdapValueButton: ControlContainer ist NULL.");
      return;
    }

    XControl xControl = this.controlContainer.getControl("btn" + columnName);

    if (xControl == null)
    {
      LOGGER
          .debug("DatensatzBearbeitenBaseWizardPage: showAcceptLdapValueButton: xControl is NULL.");
      return;
    }

    Utils.setProperty(xControl.getModel(), UnoProperty.ENABLE_VISIBLE, visible);
  }

  protected void setTextColor(XControl xControl, int textColor)
  {
    if (xControl == null)
      return;

    XControlModel xControlModel = xControl.getModel();

    if (xControlModel == null)
      return;

    Utils.setProperty(xControlModel, UnoProperty.TEXT_COLOR, textColor);
  }

  protected AbstractActionListener buttonActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XControl xControl = UNO.XControl(arg0.Source);

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
          res = InfoDialog.showYesNoModal("LDAP Datensatz",
              "Leeren Wert aus LDAP-Datensatz übernehmen?");
        } else
        {
          res = InfoDialog.showYesNoModal("LDAP Datensatz",
              "Wert \"" + ldapValue + "\" aus LDAP-Datensatz übernehmen?");
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
    }
  };

  protected AbstractTextListener textListener = new AbstractTextListener()
  {
    @Override
    public void textChanged(TextEvent arg0)
    {
      XControl xControl = UNO.XControl(arg0.Source);

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
          String dataSetValue = dataset.get(label);

          if (dataset.getBS() != null
              && (dataSetValue == null || !dataSetValue.equals(textComponentText)))
          {
            showAcceptLdapValueButton(label, true);
            setTextColor(xControl, 16711680);
          } else
          {
            showAcceptLdapValueButton(label, false);
            setTextColor(xControl, 00000000);
          }
        }

      } catch (IllegalArgumentException | UnoHelperException | ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

    }
  };

  protected AbstractItemListener itemListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      selectedItemIndex = (short) arg0.Selected;
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

        XComboBox xComboBox = UNO.XComboBox(xControl);

        XControlModel xControlModel = xControl.getModel();
        String label = (String) UnoProperty.getProperty(xControlModel, UnoProperty.NAME);

        if (xTextComponent != null)
          dataset.set(label, xTextComponent.getText());
        else if (xComboBox != null)
          dataset.set(label, xComboBox.getItem(selectedItemIndex));
      }
    } catch (ColumnNotFoundException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }

    new OnPALChangedNotify().emit();

    window.setVisible(false);

    return true;
  }

}
