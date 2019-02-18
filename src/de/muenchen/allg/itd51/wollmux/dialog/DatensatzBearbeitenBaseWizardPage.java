package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.Set;

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
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public abstract class DatensatzBearbeitenBaseWizardPage extends AbstractXWizardPage
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenBaseWizardPage.class);
  protected DJDataset dataset;
  protected Dataset ldapDataset;
  protected Set<String> dbSchema;
  private XControlContainer controlContainer;
  private short selectedItemIndex = 0;
  private static final String TEXT_COLOR = "TextColor";

  public DatensatzBearbeitenBaseWizardPage(short pageId, XWindow parentWindow, String dialogName,
      DJDataset dataset, Dataset ldapDataset, Set<String> dbSchema) throws Exception
  {
    super(pageId, parentWindow, dialogName);
    this.dataset = dataset;
    this.ldapDataset = ldapDataset;
    this.dbSchema = dbSchema;
  }

  protected void setControlContainer(XControlContainer controlContainer)
  {
    this.controlContainer = controlContainer;
  }

  protected void showAcceptLdapValueButton(String columnName)
  {
    if (this.controlContainer == null)
    {
      LOGGER.error(
          "DatensatzBearbeitenBaseWizardPage: showAcceptLdapValueButton: ControlContainer ist NULL.");
      return;
    }

    XControl xControl = this.controlContainer.getControl("btn" + columnName);

    XPropertySet props = UNO.XPropertySet(xControl.getModel());

    try
    {
      props.setPropertyValue("EnableVisible", true);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }

  protected boolean isDifferentFromLdapDataset(String columnName)
  {
    try
    {
      if (ldapDataset == null)
        return false;

      String datasetValue = dataset.get(columnName);
      String ldapDSValue = ldapDataset.get(columnName);

      if ((ldapDSValue == null && datasetValue != null && !datasetValue.isEmpty())
          || ldapDSValue != null && datasetValue != null && !ldapDSValue.equals(datasetValue))
      {
        return true;
      }
    } catch (ColumnNotFoundException e1)
    {
      LOGGER.error("", e1);
    }

    return false;
  }

  protected void setTextColor(XControl xControl, int textColor)
  {
    if (xControl == null)
      return;

    XControlModel xControlModel = xControl.getModel();

    if (xControlModel == null)
      return;

    XPropertySet propertySet = UNO.XPropertySet(xControlModel);
    try
    {
      propertySet.setPropertyValue(TEXT_COLOR, textColor);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }

  protected AbstractActionListener buttonActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      System.out.println("actionPerformend");
      XControl xControl = UNO.XControl(arg0.Source);

      if (xControl == null)
        return;

      XPropertySet propertySet = UNO.XPropertySet(xControl.getModel());
      try
      {
        String label = (String) propertySet.getPropertyValue("Name");

        // btnVorname -> Vorname
        String buttonLabel = label.substring(3);

        XControl targetTextField = controlContainer.getControl(buttonLabel);

        if (targetTextField == null)
          return;

        XTextComponent xTextComponent = UNO.XTextComponent(targetTextField);

        if (xTextComponent == null)
          return;

        String ldapValue = ldapDataset.get(buttonLabel);
        
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

      } catch (UnknownPropertyException | WrappedTargetException | ColumnNotFoundException e)
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

      XPropertySet propertySet = UNO.XPropertySet(xControlModel);

      try
      {
        XTextComponent xTextComponent = UNO.XTextComponent(xControl);

        if (xTextComponent != null)
        {
          String label = (String) propertySet.getPropertyValue("Name");
          String textComponentText = xTextComponent.getText();
          String dataSetValue = dataset.get(label);

          if (dataSetValue == null || !dataSetValue.equals(textComponentText))
          {
            setTextColor(xControl, 16711680);
          } else
          {
            setTextColor(xControl, 00000000);
          }
        }

      } catch (IllegalArgumentException | UnknownPropertyException
          | WrappedTargetException | ColumnNotFoundException e)
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
        XPropertySet propertySet = UNO.XPropertySet(xControlModel);
        String label = (String) propertySet.getPropertyValue("Name");

        if (xTextComponent != null)
          dataset.set(label, xTextComponent.getText());
        else if (xComboBox != null)
          dataset.set(label, xComboBox.getItem(selectedItemIndex));
      }
    } catch (ColumnNotFoundException | UnknownPropertyException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }

    WollMuxEventHandler.getInstance().handlePALChangedNotify();

    window.setVisible(false);

    return true;
  }

}
