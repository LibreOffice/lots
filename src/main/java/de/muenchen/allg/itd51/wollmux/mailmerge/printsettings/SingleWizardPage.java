package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.dialog.adapter.AbstractXWizardPage;

/**
 * A page of the mail merge wizard. Settings for printing each record in a single file are made
 * here.
 */
public class SingleWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleWizardPage.class);

  private final XTextComponent targetDir;
  private final XButton search;

  private final PrintSettings settings;

  /**
   * Creates this page.
   *
   * @param parentWindow
   *          The containing window.
   * @param pageId
   *          The id of this page.
   * @param controller
   *          The controller of the wizard.
   * @param settings
   *          The print settings.
   * @throws Exception
   *           If the page can't be created.
   */
  public SingleWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller,
      PrintSettings settings) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux.seriendruck_single?location=application");
    this.settings = settings;
    XControlContainer container = UNO.XControlContainer(window);
    targetDir = UNO.XTextComponent(container.getControl("targetDir"));
    targetDir.addTextListener(new AbstractTextListener()
    {

      @Override
      public void textChanged(TextEvent event)
      {
        controller.updateTravelUI();
      }
    });
    search = UNO.XButton(container.getControl("search"));
    search.addActionListener(new AbstractActionListener()
    {

      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        try
        {
          XFolderPicker2 picker = UnoRuntime.queryInterface(XFolderPicker2.class,
              UNO.xMCF.createInstanceWithContext("com.sun.star.ui.dialogs.FolderPicker",
                  UNO.defaultContext));
          short res = picker.execute();
          if (res == ExecutableDialogResults.OK)
          {
            String files = picker.getDirectory();
            targetDir.setText(files);
          }
        } catch (Exception e)
        {
          LOGGER.debug("", e);
        }
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    try
    {
      Object fileContentProvider = UNO.xMCF
          .createInstanceWithContext("com.sun.star.ucb.FileContentProvider", UNO.defaultContext);
      XFileIdentifierConverter xFileConverter = UnoRuntime
          .queryInterface(XFileIdentifierConverter.class, fileContentProvider);
      String path = xFileConverter.getSystemPathFromFileURL(targetDir.getText());
      File f = new File(path.isEmpty() ? targetDir.getText() : path);
      return f.isDirectory() && f.canWrite();
    } catch (Exception e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  @Override
  public boolean commitPage(short reason)
  {
    settings.setTargetDirectory(targetDir.getText());
    window.setVisible(false);
    return true;
  }
}
