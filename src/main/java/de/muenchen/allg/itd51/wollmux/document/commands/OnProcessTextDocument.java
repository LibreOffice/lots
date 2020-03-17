package de.muenchen.allg.itd51.wollmux.document.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentControllerInitialized;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

/**
 * Event for processing a document.
 */
public class OnProcessTextDocument implements WollMuxEventListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnProcessTextDocument.class);

  @Subscribe
  public void onTextDocumentControllerInitialized(OnTextDocumentControllerInitialized event)
  {
    TextDocumentController documentController = event.getTextDocumentController();

    if (documentController == null)
    {
      LOGGER.trace("{} : DocumentController is NULL.", this.getClass().getSimpleName());
      return;
    }

    try
    {
      ConfigThingy tds = WollMuxFiles.getWollmuxConf().query("Fenster").query("Textdokument")
          .getLastChild();
      documentController.getFrameController().setWindowViewSettings(tds);
    } catch (NodeNotFoundException e)
    {
      // configuration for Fenster isn't mandatory
    }

    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
        documentController, WollMuxFiles.isDebugMode());

    try
    {
      // scan global document commands
      dci.scanGlobalDocumentCommands();

      int actions = documentController.evaluateDocumentActions(GlobalFunctions
          .getInstance().getDocumentActionFunctions().iterator());

      // if it is a template execute the commands
      if ((actions < 0 && documentController.getModel().isTemplate())
          || (actions == Integer.MAX_VALUE))
      {
        dci.executeTemplateCommands();

        // there can be new commands now
        dci.scanGlobalDocumentCommands();
      }
      dci.scanInsertFormValueCommands();

      // if it is a form execute form commands
      if (actions != 0 && documentController.getModel().isFormDocument())
      {

        try
        {
          documentController.getFrameController().setDocumentZoom(WollMuxFiles.getWollmuxConf()
              .query("Fenster").query("Formular").getLastChild().query("ZOOM"));
        } catch (java.lang.Exception e)
        {
          // configuration for Fenster isn't mandatory
        }

        try
        {
          FormController formController = documentController.getFormController();
          formController.createFormGUI();
          formController.formControllerInitCompleted();
        } catch (FormModelException e)
        {
          throw new WMCommandsFailedException(L.m(
              "Die Vorlage bzw. das Formular enthält keine gültige Formularbeschreibung\n\nBitte kontaktieren Sie Ihre Systemadministration."));
        }
      }
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    // notify listeners about processing finished
    new OnNotifyDocumentEventListener(null, WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED,
        documentController.getModel().doc).emit();

    // ContextChanged to update dispatches
    try
    {
      documentController.getFrameController().getFrame().contextChanged();
    } catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }
  }

}