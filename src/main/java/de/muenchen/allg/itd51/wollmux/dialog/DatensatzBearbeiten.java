package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung einen
 * (mehrseitigen) Dialog zur Bearbeitung eines {@link DJDataset}s. <b>ACHTUNG:</b> Die
 * private-Funktionen dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatensatzBearbeiten
{
  private XWizard wizard = null;

  /**
   * Erzeugt einen Wizard zum Ändern des Datensatzes.
   *
   * @param datensatz
   * @param dbSchema
   */
  protected DatensatzBearbeiten(LOSDJDataset datensatz, List<String> dbSchema)
  {
    wizard = Wizard.createSinglePathWizard(UNO.defaultContext,
        DatensatzBearbeitenWizardController.PATHS,
        new DatensatzBearbeitenWizardController(datensatz, dbSchema));
      wizard.enableButton(WizardButton.HELP, false);
      wizard.setTitle("Datensatz bearbeiten");
  }

  protected short executeWizard()
  {
    return wizard.execute();
  }
}
