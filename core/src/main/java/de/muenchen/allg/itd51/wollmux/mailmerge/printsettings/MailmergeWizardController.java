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
package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.util.InvalidStateException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.mailmerge.MailMergeRunner;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * Handles the wizard for configuring mail merge.
 */
public class MailmergeWizardController implements XWizardController
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmergeWizardController.class);

  private static final int PAGE_COUNT = PAGE_ID.values().length;

  /**
   * Possible paths through the wizard.
   */
  public enum PATH
  {
    /**
     * Printing as one large document (default).
     */
    STANDARD(new short[] { 0 }),
    /**
     * Path for printing on a printer.
     */
    DIRECT_PRINT(new short[] { 0, 2 }),
    /**
     * Path for sending mails.
     */
    MAIL(new short[] { 0, 1, 3 }),
    /**
     * Path for printing as single documents.
     */
    SINGLE_FILES(new short[] { 0, 1, 4 });

    final short[] path;

    PATH(short[] path)
    {
      this.path = path;
    }
  }

  private static final short[][] paths = { PATH.STANDARD.path, PATH.DIRECT_PRINT.path,
      PATH.MAIL.path, PATH.SINGLE_FILES.path };
  private PATH currentPath = PATH.STANDARD;

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
  private DatasourceModel model;
  private PrintSettings settings;

  private TextDocumentController textDocumentController;

  private boolean initSize = true;

  /**
   * Create a new wizard controller.
   *
   * @param model
   *          The model of the mail merge.
   * @param textDocumentController
   *          The controller of the document.
   * @throws NoTableSelectedException
   *           No table in the model was selected.
   */
  public MailmergeWizardController(DatasourceModel model,
      TextDocumentController textDocumentController) throws NoTableSelectedException
  {
    this.model = model;
    this.textDocumentController = textDocumentController;
    settings = new PrintSettings(model.getNumberOfRecords());
  }

  public DatasourceModel getModel()
  {
    return model;
  }

  @Override
  public boolean canAdvance()
  {
    return wizard.getCurrentPage().canAdvance();
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
    XWizardPage page = null;
    try
    {
      switch (getPageId(pageId))
      {
      case START:
        page = new StartWizardPage(parentWindow, pageId, this, settings);
        break;
      case FORMAT:
        page = new FormatWizardPage(parentWindow, pageId, this, settings);
        break;
      case PRINTER:
        page = new PrintWizardPage(parentWindow, pageId, textDocumentController.getModel());
        break;
      case MAIL:
        page = new MailWizardPage(parentWindow, pageId, this, settings);
        break;
      case SINGLE:
        page = new SingleWizardPage(parentWindow, pageId, this, settings);
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
    updateTravelUI();
    if(initSize)
    {
      wizard.getDialogWindow().setPosSize(0, 0, 750, 550, PosSize.SIZE);
      initSize = false;
    }
  }

  @Override
  public void onDeactivatePage(short pageId)
  {
    LOGGER.debug("Deaktiviere Page {} mit Titel {}", pageId, title[pageId]);
  }

  /**
   * Creates and shows a wizard for configuring a mail merge.
   */
  public void startWizard()
  {
    wizard = Wizard.createMultiplePathsWizard(UNO.defaultContext, paths, this);
    wizard.enableButton(WizardButton.HELP, false);
    wizard.setTitle("WollMux Seriendruck - Optionen");
    
    short result = wizard.execute();
    if (result == ExecutableDialogResults.OK)
    {
      try
      {
	new Thread(new MailMergeRunner(textDocumentController, model, settings))
	    .start();
      } catch (NoTableSelectedException e)
      {
        InfoDialog.showInfoModal("Seriendruck fehlgeschlagen", e.getMessage());
      }
    }
  }

  /**
   * Changes the wizard path.
   *
   * @param newPath
   *          The new path.
   */
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

  /**
   * Activates the "next" button ({@link XWizard#updateTravelUI()}).
   *
   * Activates the "finish" button if all active pages can be finished.
   */
  public void updateTravelUI()
  {
    wizard.updateTravelUI();
    boolean finishActivated = true;
    for (short id : currentPath.path)
    {
      finishActivated &= pages[id] != null && pages[id].canAdvance();
    }
    wizard.enableButton(WizardButton.FINISH, finishActivated);
  }

  public String getDefaultFilename()
  {
    String fileName = textDocumentController.getFrameController().getTitle();
    // remove suffix
    if (fileName.toLowerCase().matches(".+\\.(odt|doc|ott|dot)$"))
      fileName = fileName.substring(0, fileName.length() - 4);
    // replace malicious chars
    return fileName.replaceAll("[^\\p{javaLetterOrDigit},.()=+_-]", "_");
  }

  private PAGE_ID getPageId(short pageId)
  {
    return PAGE_ID.values()[pageId];
  }
}
