package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for collecting form fields not surrounded by WollMux commands.
 *
 * @see TextDocumentController#collectNonWollMuxFormFields()
 */
public class OnCollectNonWollMuxFormFieldsViaPrintModel extends WollMuxEvent
{
  private ActionListener listener;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document which contains form fields.
   * @param listener
   *          Listener, which is notified as soon as this event is processed.
   */
  public OnCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentController documentController,
      ActionListener listener)
  {
    this.documentController = documentController;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    documentController.collectNonWollMuxFormFields();

    if (listener != null)
    {
      listener.actionPerformed(null);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}