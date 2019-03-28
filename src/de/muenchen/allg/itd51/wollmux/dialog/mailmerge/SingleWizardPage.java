package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.SubmitArgument;

public class SingleWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleWizardPage.class);

  private final XTextComponent targetDir;
  private final XButton search;

  private MailmergeWizardController controller;
  private XTextDocument doc;

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
    controller.arguments.put(SubmitArgument.targetDirectory, targetDir.getText());
    window.setVisible(false);
    return true;
  }

  /**
   * Holt sich den Frame von doc, führt auf diesem ein queryDispatch() mit der zu urlStr gehörenden
   * URL aus und liefert den Ergebnis XDispatch zurück oder null, falls der XDispatch nicht
   * verfügbar ist.
   *
   * @param doc
   *          Das Dokument, dessen Frame für den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht verfügbar ist.
   */
  private XDispatch getDispatchForModel(com.sun.star.util.URL url)
  {
    XDispatchProvider dispProv = null;

    dispProv = UNO.XDispatchProvider(UNO.XModel(doc).getCurrentController().getFrame());

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self", com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }
}
