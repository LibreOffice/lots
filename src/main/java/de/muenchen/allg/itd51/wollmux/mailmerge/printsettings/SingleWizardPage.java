package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.mailmerge.MailMergeController.SubmitArgument;

public class SingleWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleWizardPage.class);

  private final XTextComponent targetDir;
  private final XButton search;

  private MailmergeWizardController controller;

  public SingleWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller)
      throws Exception
  {
    super(pageId, parentWindow, "seriendruck_single");
    this.controller = controller;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    targetDir = UNO.XTextComponent(container.getControl("targetDir"));
    search = UNO.XButton(container.getControl("search"));
    search.addActionListener(new AbstractActionListener()
    {

      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        try
        {
          XFolderPicker2 picker = UnoRuntime.queryInterface(XFolderPicker2.class, UNO.xMCF
              .createInstanceWithContext("com.sun.star.ui.dialogs.FolderPicker", UNO.defaultContext));
          short res = picker.execute();
          if (res == ExecutableDialogResults.OK)
          {
            String files = picker.getDirectory();
            targetDir.setText(files);
            controller.enableFinishButton(true);
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
    return !targetDir.getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    controller.arguments.put(SubmitArgument.TARGET_DIRECTORY, targetDir.getText());
    window.setVisible(false);
    return true;
  }
}
