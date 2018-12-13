package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.itd51.wollmux.TextModule;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnJumpToPlaceholder extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnJumpToPlaceholder.class);

  private TextDocumentController documentController;

  public OnJumpToPlaceholder(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();

    try
    {
      TextModule.jumpPlaceholders(documentController.getModel().doc,
          viewCursor);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    stabilize();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}