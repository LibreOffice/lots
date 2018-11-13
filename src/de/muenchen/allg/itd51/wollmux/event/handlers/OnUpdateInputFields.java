package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.DispatchHelper;

public class OnUpdateInputFields extends BasicEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnUpdateInputFields.class);

  TextDocumentController documentController;
  DispatchHelper helper;

  public OnUpdateInputFields(TextDocumentController documentController, DispatchHelper helper)
  {
    this.documentController = documentController;
    this.helper = helper;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (documentController.getModel().isFormDocument())
    {
      LOGGER.info(
          "LibreOffice Formulareingabe unterdrückt, da es sich um ein WollMux-Formular handelt.");
      helper.dispatchFinished(true);
    } else
    {
      helper.dispatchOriginal();
    }
  }

}
