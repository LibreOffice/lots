package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.util.InvalidStateException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

public class MailmergeWizardController implements XWizardController
{
  
  private static final Logger LOGGER = LoggerFactory.getLogger(MailmergeWizardController.class);

  private static final int PAGE_COUNT = 4;
  
  public enum PATH {
    STANDRAD(new short[] { 0 }),
    DIRECT_PRINT(new short[] { 0, 2 }),
    MAIL(new short[] { 0, 1, 3 }),
    SINGLE_FILES(new short[] { 0, 1 });
    
    final short[] path;
    
    PATH(short[] path) {
      this.path = path;
    }
  }
  
  private final short[][] PATHS = { PATH.STANDRAD.path, PATH.DIRECT_PRINT.path, PATH.MAIL.path, PATH.SINGLE_FILES.path };
  private PATH currentPath = PATH.STANDRAD;
  
  private enum PAGE_ID {
    START,
    FORMAT,
    PRINTER,
    MAIL;
  }
  
  private String[] title = {
      "Aktionen",
      "Format",
      "Drucker",
      "E-Mail"
  };
  
  private MailMergeParams params;
  
  private XWizardPage[] pages = new XWizardPage[PAGE_COUNT];
  
  private PAGE_ID currentPage = PAGE_ID.START;
  
  private XWizard wizard;
  
  public MailmergeWizardController(MailMergeParams params)
  {
    this.params = params;
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
    return false;
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
        page = new StartWizardPage(parentWindow, pageId, this, params);
        break;
      case FORMAT:
        page = new FormatWizardPage(parentWindow, pageId, this);
        break;
      case PRINTER:
        page = new PrintWizardPage(parentWindow, pageId, params);
        break;
      case MAIL:
        page = new MailWizardPage(parentWindow, pageId, this);
        break;
      }
      pages[pageId] = page;
    } catch (Exception ex) {
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
    currentPage = getPageId(pageId);
    pages[pageId].activatePage();
    activateNextButton(wizard.getCurrentPage().canAdvance());
  }

  @Override
  public void onDeactivatePage(short pageId)
  {
    LOGGER.debug("Deaktiviere Page {} mit Titel {}", pageId, title[pageId]);
  }
  
  public void createWizard()
  {
    wizard = Wizard.createMultiplePathsWizard(UNO.defaultContext, PATHS, this);
    wizard.enableButton(WizardButton.HELP, false);
    wizard.execute();
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
    for(short i : currentPath.path)
    {
      enable = enable && pages[i].canAdvance();
    }
    wizard.enableButton(WizardButton.FINISH, enable);
  }

}
