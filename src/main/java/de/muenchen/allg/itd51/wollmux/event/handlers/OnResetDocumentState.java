package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for reseting document status.
 * <ul>
 * <li>Put cursor on first page</li>
 * <li>set modified state to false</li>
 * </ul>
 */
public class OnResetDocumentState extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnResetDocumentState.class);

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   */
  public OnResetDocumentState(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    try
    {
      documentController.getModel().getViewCursor().gotoRange(
          documentController.getModel().doc.getText().getStart(), false);
    } catch (Exception e)
    {
      LOGGER.debug("", e);
    }

    documentController.getModel().setDocumentModified(false);
  }
}