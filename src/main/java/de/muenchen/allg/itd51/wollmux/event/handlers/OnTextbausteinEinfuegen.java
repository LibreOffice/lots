package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.itd51.wollmux.TextModule;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for inserting a boilerplate. It can be inserted as a reference or the reference can be
 * resolved immediately.
 */
public class OnTextbausteinEinfuegen extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnTextbausteinEinfuegen.class);

  private boolean reprocess;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param reprocess
   *          If true the reference is resolved, otherwise a reference is inserted.
   */
  public OnTextbausteinEinfuegen(TextDocumentController documentController,
      boolean reprocess)
  {
    this.documentController = documentController;
    this.reprocess = reprocess;

  }

  @Override
  protected void doit()
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    try
    {
      TextModule.createInsertFragFromIdentifier(
          documentController.getModel().doc, viewCursor, reprocess);
      if (reprocess)
      {
        new OnReprocessTextDocument(documentController).emit();
      } else
      {
        InfoDialog.showInfoModal(L.m("WollMux"), L.m("Der Textbausteinverweis wurde eingefügt."));
      }
    } catch (WollMuxFehlerException e)
    {
      LOGGER.error("Textbausteinverweis konnte nicht eingefügt werden.", e);
      InfoDialog.showInfoModal(L.m("WollMux-Fehler"), e.getMessage());
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ", " + reprocess + ")";
  }
}