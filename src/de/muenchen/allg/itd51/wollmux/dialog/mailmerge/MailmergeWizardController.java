package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.util.InvalidStateException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.ACTION;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.FORMAT;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIElementAction;

public class MailmergeWizardController implements XWizardController
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmergeWizardController.class);

  private static final int PAGE_COUNT = 5;

  public enum PATH
  {
    STANDRAD(new short[] { 0 }),
    DIRECT_PRINT(new short[] { 0, 2 }),
    MAIL(new short[] { 0, 1, 3 }),
    SINGLE_FILES(new short[] { 0, 1, 4 });

    final short[] path;

    PATH(short[] path)
    {
      this.path = path;
    }
  }

  private static final short[][] paths = { PATH.STANDRAD.path, PATH.DIRECT_PRINT.path, PATH.MAIL.path,
      PATH.SINGLE_FILES.path };
  private PATH currentPath = PATH.STANDRAD;

  private enum PAGE_ID
  {
    START,
    FORMAT,
    PRINTER,
    MAIL,
    SINGLE;
  }

  private String[] title = { "Aktionen", "Format", "Drucker", "E-Mail", "Zielverzeichnis" };

  private XWizardPage[] pages = new XWizardPage[PAGE_COUNT];

  private XWizard wizard;

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setActionType}-Action angegeben war. Beispiel:
   *
   * Wird in der GUI das Formularelement '(LABEL "Gesamtdokument erstellen" TYPE "radio" ACTION
   * "setActionType" VALUE "gesamtdok")' ausgewählt, dann enthält diese Variable den Wert
   * "gesamtdok".
   */
  public ACTION currentActionType = ACTION.NOTHING;

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   */
  public DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  public FORMAT format = FORMAT.ODT;

  /**
   * Enthält den Wert des zuletzt ausgeführten
   * {@link RuleStatement#IGNORE_DOC_PRINTFUNCTIONS}-Statements
   */
  public boolean ignoreDocPrintFuncs = true;
  public Map<SubmitArgument, Object> arguments;
  MailMergeController controller;

  public MailMergeController getController()
  {
    return controller;
  }

  XTextDocument doc;

  public MailmergeWizardController(MailMergeController controller, XTextDocument doc)
  {
    arguments = new EnumMap<>(SubmitArgument.class);
    this.controller = controller;
    this.doc = doc;
  }

  @Override
  public boolean canAdvance()
  {
    LOGGER.debug("canAdvance");
    return false;
  }

  @Override
  public boolean confirmFinish()
  {
    return true;
  }

  @Override
  public XWizardPage createPage(XWindow parentWindow, short pageId)
  {
    LOGGER.debug("createPage with id {}", pageId);
    parentWindow.setPosSize(0, 0, 650, 550, PosSize.SIZE);
    XWizardPage page = null;
    try
    {
      switch (getPageId(pageId))
      {
      case START:
        page = new StartWizardPage(parentWindow, pageId, this);
        break;
      case FORMAT:
        page = new FormatWizardPage(parentWindow, pageId, this);
        break;
      case PRINTER:
        page = new PrintWizardPage(parentWindow, pageId, doc);
        break;
      case MAIL:
        page = new MailWizardPage(parentWindow, pageId, this);
        break;
      case SINGLE:
        page = new SingleWizardPage(parentWindow, pageId, this);
        break;
      }
      pages[pageId] = page;
    } catch (Exception ex)
    {
      LOGGER.error("Page {} konnte nicht erstellt werden", pageId);
      LOGGER.error("", ex);
    }
    return page;
  }

  @Override
  public String getPageTitle(short pageId)
  {
    return title[pageId];
  }

  @Override
  public void onActivatePage(short pageId)
  {
    LOGGER.debug("Aktiviere Page {} mit Titel {}", pageId, title[pageId]);
    pages[pageId].activatePage();
    activateNextButton(wizard.getCurrentPage().canAdvance());
  }

  @Override
  public void onDeactivatePage(short pageId)
  {
    LOGGER.debug("Deaktiviere Page {} mit Titel {}", pageId, title[pageId]);
  }

  public void startWizard()
  {
    wizard = Wizard.createMultiplePathsWizard(UNO.defaultContext, paths, this);
    wizard.enableButton(WizardButton.HELP, false);
    wizard.enableButton(WizardButton.NEXT, false);
    short result = wizard.execute();
    if (result == ExecutableDialogResults.OK)
    {
      controller.doMailMerge(currentActionType, format, datasetSelectionType, arguments);
    }
  }

  public void changePath(PATH newPath)
  {
    LOGGER.debug("Neuer Pfad {}", newPath);
    if (wizard != null)
    {
      currentPath = newPath;
      try
      {
        wizard.activatePath((short) newPath.ordinal(), true);
      } catch (NoSuchElementException | InvalidStateException e)
      {
        LOGGER.error("Seriendruck Dialog Pfad {} konnte nicht aktiviert werden", newPath);
        LOGGER.error("", e);
      }
    }
  }

  public void activateNextButton(boolean activate)
  {
    wizard.enableButton(WizardButton.NEXT, activate);
    enableFinishButton();
  }

  private PAGE_ID getPageId(short pageId)
  {
    return PAGE_ID.values()[pageId];
  }

  private void enableFinishButton()
  {
    boolean enable = true;
    for (short i : currentPath.path)
    {
      enable = enable && pages[i].canAdvance();
    }
    wizard.enableButton(WizardButton.FINISH, enable);
  }

  public ACTION getCurrentActionType()
  {
    return currentActionType;
  }

  public void setCurrentActionType(ACTION currentActionType)
  {
    this.currentActionType = currentActionType;
  }

  public DatasetSelectionType getDatasetSelectionType()
  {
    return datasetSelectionType;
  }

  public void setDatasetSelectionType(DatasetSelectionType datasetSelectionType)
  {
    this.datasetSelectionType = datasetSelectionType;
  }
}
