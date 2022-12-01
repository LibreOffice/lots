/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for setting the cursor to the next bookmark with name "setJumpMark".
 */
public class OnJumpToMark extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnJumpToMark.class);

  private XTextDocument doc;

  private boolean msg;

  /**
   * Create this event.
   *
   * @param doc
   *          The document.
   * @param msg
   *          Show a message if there is no bookmark.
   */
  public OnJumpToMark(XTextDocument doc, boolean msg)
  {
    this.doc = doc;
    this.msg = msg;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor == null)
    {
      return;
    }

    DocumentCommand cmd = documentController.getModel().getFirstJumpMark();

    if (cmd != null)
    {
      try
      {
        XTextRange range = cmd.getTextCursor();
        if (range != null)
          viewCursor.gotoRange(range.getStart(), false);
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }

      boolean modified = documentController.getModel().isDocumentModified();
      cmd.markDone(true);
      documentController.getModel().setDocumentModified(modified);

      documentController.getModel().getDocumentCommands().update();

    } else
    {
      if (msg)
      {
        InfoDialog.showInfoModal(L.m("WollMux"),
            L.m("No placeholder and no 'setJumpMark' flag available!"));
      }
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", " + msg
        + ")";
  }
}
