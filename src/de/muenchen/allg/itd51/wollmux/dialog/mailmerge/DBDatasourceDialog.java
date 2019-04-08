package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.dialog.AbstractNotifier;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class DBDatasourceDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DBDatasourceDialog.class);
  private UNODialogFactory dialogFactory = null;
  private SimpleDialogLayout layout = null;
  private MailMergeNew mailMerge;
  private AbstractNotifier listener;

  public DBDatasourceDialog(AbstractNotifier listener)
  {
    dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(600, 400, 0xF2F2F2);
    dialogWindow.addWindowListener(windowAdapter);
    this.listener = listener;
    dialogFactory.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    List<String> oooDatasources = getRegisteredDatabaseNames();
    layout.addControlsToList(addDBDatasourceButtons(oooDatasources));

    layout.draw();
  }

  private ControlModel addDBDatasourceButtons(List<String> oooDatasources)
  {
    List<ControlProperties> dsButtons = new ArrayList<>();

    for (String ds : oooDatasources)
    {
      ControlProperties oooDS = new ControlProperties(ControlType.BUTTON, ds);
      oooDS.setControlPercentSize(50, 40);
      oooDS.setLabel(ds);
      XButton abortXBtn = UNO.XButton(oooDS.getXControl());
      abortXBtn.setActionCommand(ds);
      abortXBtn.addActionListener(oooDSActionListener);
      dsButtons.add(oooDS);
    }

    return new ControlModel(Orientation.VERTICAL, Align.NONE, dsButtons, Optional.empty());
  }

  private AbstractActionListener oooDSActionListener = (event) -> {
    
    // Datenquellen-Listbox der Seriendruck-Sidebar aktualisieren.
    listener.notify(event.ActionCommand);
  };

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowShown(EventObject event)
    {
      getDocumentController().ifPresent(controller -> {
        mailMerge = DocumentManager.getDocumentManager()
            .getCurrentMailMergeNew(controller.getModel().doc);
        if (mailMerge == null)
        {
          mailMerge = new MailMergeNew(controller, actionEvent -> {
            if (actionEvent.getSource() instanceof MailMergeNew)
              WollMuxEventHandler.getInstance().handleMailMergeNewReturned(controller);
          });
          DocumentManager.getDocumentManager().setCurrentMailMergeNew(controller.getModel().doc,
              mailMerge);
        }
      });
    }
  };

  private Optional<TextDocumentController> getDocumentController()
  {
    XTextDocument doc = UNO.getCurrentTextDocument();
    if (doc == null)
      return Optional.ofNullable(null);
    return Optional.ofNullable(DocumentManager.getTextDocumentController(doc));
  }

  /**
   * Returns the names of all datasources registered in OOo.
   *
   */
  private List<String> getRegisteredDatabaseNames()
  {
    List<String> datasourceNames = new ArrayList<>();
    try
    {
      String[] datasourceNamesA = UNO.XNameAccess(UNO.dbContext).getElementNames();
      for (int i = 0; i < datasourceNamesA.length; ++i)
        datasourceNames.add(datasourceNamesA[i]);
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
    return datasourceNames;
  }
}
