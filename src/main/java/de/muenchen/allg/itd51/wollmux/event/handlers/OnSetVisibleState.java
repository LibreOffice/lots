package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for updating visiblity states. Commands and sections are updated.
 */
public class OnSetVisibleState extends WollMuxEvent
{
  private String groupId;

  private boolean visible;

  private ActionListener listener;

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param groupId
   *          The ID of the visibility group.
   * @param visible
   *          The new state of the group.
   * @param listener
   *          The listener to notify after completion.
   */
  public OnSetVisibleState(TextDocumentController documentController,
      String groupId,
      boolean visible, ActionListener listener)
  {
    this.documentController = documentController;
    this.groupId = groupId;
    this.visible = visible;
    this.listener = listener;
  }

  public TextDocumentController getDocumentController()
  {
    return documentController;
  }

  @Override
  protected void doit()
  {
    documentController.setVisibleState(groupId, visible);
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "('" + groupId + "', " + visible
        + ")";
  }
}