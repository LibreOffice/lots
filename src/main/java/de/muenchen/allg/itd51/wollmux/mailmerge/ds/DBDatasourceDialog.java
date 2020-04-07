package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.AbstractNotifier;

/**
 * A dialog for selecting a data source which is registered in LibreOffice as database.
 */
public class DBDatasourceDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DBDatasourceDialog.class);

  /**
   * Start the dialog.
   *
   * @param listener
   *          The listener is called when the dialog is closed.
   */
  public DBDatasourceDialog(AbstractNotifier listener)
  {
    UNODialogFactory dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(600, 400, 0xF2F2F2);
    dialogFactory.showDialog();

    SimpleDialogLayout layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    AbstractActionListener oooDSActionListener = event -> {
      listener.notify(event.ActionCommand);
      dialogFactory.closeDialog();
    };

    Set<String> oooDatasources = getRegisteredDatabaseNames();
    layout.addControlsToList(addDBDatasourceButtons(oooDatasources, oooDSActionListener));

    layout.draw();
  }

  /**
   * Add a button for each list entry.
   *
   * @param oooDatasources
   *          List of entries.
   * @param oooDSActionListener
   *          Listener to call on click.
   * @return A list of buttons.
   */
  private ControlModel addDBDatasourceButtons(Set<String> oooDatasources,
      AbstractActionListener oooDSActionListener)
  {
    List<ControlProperties> dsButtons = new ArrayList<>();

    for (String ds : oooDatasources)
    {
      ControlProperties oooDS = new ControlProperties(ControlType.BUTTON, ds);
      oooDS.setControlPercentSize(50, 40);
      oooDS.setLabel(ds);
      XButton dsButton = UNO.XButton(oooDS.getXControl());
      dsButton.setActionCommand(ds);
      dsButton.addActionListener(oooDSActionListener);
      dsButtons.add(oooDS);
    }

    return new ControlModel(Orientation.VERTICAL, Align.NONE, dsButtons, Optional.empty());
  }

  /**
   * Returns the names of all data sources registered in OOo.
   *
   * @return List of all registered data sources.
   */
  private Set<String> getRegisteredDatabaseNames()
  {
    try
    {
      return UnoDictionary.create(UNO.dbContext, Object.class).keySet();
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
    return Collections.emptySet();
  }
}
