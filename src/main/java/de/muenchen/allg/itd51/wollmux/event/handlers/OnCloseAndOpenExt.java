package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for closing a document and starting an external application.
 *
 * The document is stored as a temporary file and opened by the application.
 */
public class OnCloseAndOpenExt extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnCloseAndOpenExt.class);

  private String ext;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document to store and close.
   * @param ext
   *          Identifier of the external application.
   */
  public OnCloseAndOpenExt(TextDocumentController documentController, String ext)
  {
    this.documentController = documentController;
    this.ext = ext;
  }

  @Override
  protected void doit()
  {
    try
    {
      OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
      openExt.setSource(UNO.XStorable(documentController.getModel().doc));
      openExt.storeIfNecessary();
      openExt.launch(x -> LOGGER.error("", x));
    } catch (Exception x)
    {
      LOGGER.error("", x);
      return;
    }

    documentController.getModel().setDocumentModified(false);
    documentController.getModel().close();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ", " + ext
        + ")";
  }
}
