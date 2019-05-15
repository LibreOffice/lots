package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;

public class DatensatzBearbeitenFusszeileWizardPage extends DatensatzBearbeitenBaseWizardPage
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenFusszeileWizardPage.class);

  public DatensatzBearbeitenFusszeileWizardPage(XWindow parentWindow, short pageId,
      DJDataset dataset, Dataset ldapDataset, List<String> dbSchema)
      throws Exception
  {
    super(pageId, parentWindow, "DatensatzBearbeitenFusszeile", dataset, ldapDataset, dbSchema);

    try
    {
      XControlContainer controlContainerFusszeile = UnoRuntime
          .queryInterface(XControlContainer.class, window);
      
      setControlContainer(controlContainerFusszeile);
      
   // wenn ldap-datensatz unterschiedlich zu datensatz aus LOS (cache.conf), textfarbe = rot.
      for (String columnName : dbSchema) {
        XControl xControl = controlContainerFusszeile.getControl(columnName);
        
        if (xControl == null)
          continue;
        
        XTextComponent xTextComponent = UNO.XTextComponent(xControl);
        
        if (xTextComponent == null)
          continue;
        
        if (isDifferentFromLdapDataset(columnName)) {
          setTextColor(xControl, 16711680); // rot
        }
        
        xTextComponent.setText(dataset.get(columnName));
      }
      
      for (XControl control : controlContainerFusszeile.getControls())
      {
        XTextComponent textComponent = UNO.XTextComponent(control);

        if (textComponent == null)
          continue;

        textComponent.addTextListener(textListener);
      }
      
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }
  }

}