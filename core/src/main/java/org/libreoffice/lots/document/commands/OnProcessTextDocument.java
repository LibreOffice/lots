/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document.commands;

import org.libreoffice.lots.GlobalFunctions;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.event.WollMuxEventHandler;
import org.libreoffice.lots.event.WollMuxEventListener;
import org.libreoffice.lots.event.handlers.OnActivateSidebar;
import org.libreoffice.lots.event.handlers.OnNotifyDocumentEventListener;
import org.libreoffice.lots.event.handlers.OnTextDocumentControllerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Processes text documents.
 */
public class OnProcessTextDocument implements WollMuxEventListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnProcessTextDocument.class);

  /**
   * Processes the document given by the {@link TextDocumentController} of the
   * event.
   *
   * @param event
   *          An {@link OnTextDocumentControllerInitialized} event.
   */
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

    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(documentController, WollMuxFiles.isDebugMode());

    try
    {
      // scan global document commands
      dci.scanGlobalDocumentCommands();

      int actions = documentController
          .evaluateDocumentActions(GlobalFunctions.getInstance().getDocumentActionFunctions().iterator());

      // if it is a template execute the commands
      if ((actions < 0 && documentController.getModel().isTemplate()) || (actions == Integer.MAX_VALUE))
      {
        dci.executeTemplateCommands();

        // there can be new commands now
        dci.scanGlobalDocumentCommands();
      }

    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    new OnActivateSidebar(documentController).emit();

    // notify listeners about processing finished
    new OnNotifyDocumentEventListener(null, WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED,
        documentController.getModel().doc).emit();

    notifyContextChanged(documentController);
  }

  private void notifyContextChanged(TextDocumentController documentController)
  {
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
