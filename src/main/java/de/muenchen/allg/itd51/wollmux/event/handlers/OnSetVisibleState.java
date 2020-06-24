/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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