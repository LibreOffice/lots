package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;

/**
 * Event for reprocessing a document. Only new commands are executed.
 */
public class OnReprocessTextDocument extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnReprocessTextDocument.class);

  TextDocumentModel model;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   */
  public OnReprocessTextDocument(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.model = documentController.getModel();
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (model == null)
    {
      return;
    }

    model.getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
        documentController, WollMuxFiles.isDebugMode());

    try
    {
      dci.executeTemplateCommands();
      dci.scanGlobalDocumentCommands();
      dci.scanInsertFormValueCommands();
    } catch (WMCommandsFailedException e)
    {
      LOGGER.debug("", e);
    }

    stabilize();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + model + ")";
  }
}