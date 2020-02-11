package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Observer notifies sidebar instances when an instance of TextDocumentController is available.
 * 
 * Because of the order of initializations (Sidebars are initialized between onCreate() and
 * onViewCreated()) and a required instance of TextDocumentController (init happens in
 * onViewCreated) for building up the ui within the sidebar we need to notify sidebar instances when
 * an textDocumentController for an (new) document gets initialized.
 * 
 */
public class OnTextDocumentControllerInitialized
{
  private final TextDocumentController documentController;

  /**
   * Handled if TextDocumentController is initialized.
   * 
   * @param documentController
   *          Instance of TextDocumentController.
   */
  public OnTextDocumentControllerInitialized(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  public TextDocumentController getTextDocumentController()
  {
    return this.documentController;
  }
}
