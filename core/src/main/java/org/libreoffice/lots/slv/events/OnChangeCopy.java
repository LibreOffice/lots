/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.slv.events;

import java.util.List;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.event.handlers.WollMuxEvent;
import org.libreoffice.lots.slv.ContentBasedDirectiveItem;
import org.libreoffice.lots.slv.ContentBasedDirectiveModel;

/**
 * An event for adding or deleting a copy of a directive.
 */
public class OnChangeCopy extends WollMuxEvent
{

  private ContentBasedDirectiveModel model;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   */
  public OnChangeCopy(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.model = ContentBasedDirectiveModel.createModel(documentController);
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor != null)
    {
      XParagraphCursor cursor = UNO
          .XParagraphCursor(viewCursor.getText().createTextCursorByRange(viewCursor));
      cursor.gotoStartOfParagraph(true);
      List<ContentBasedDirectiveItem> items = model.getItemsFor(cursor);

      // delete current items
      boolean deletedAtLeastOne = false;
      for (ContentBasedDirectiveItem item : items)
      {
        deletedAtLeastOne |= removeCopy(item);
      }

      // create new copy if none was deleted.
      if (!deletedAtLeastOne)
      {
        cursor = UNO.XParagraphCursor(viewCursor.getText().createTextCursorByRange(viewCursor));
        ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
        cursor.collapseToStart();
        if (item.isItem())
        {
          cursor.gotoEndOfParagraph(false);
        }

        int count = countItemsBefore(cursor) + 1;
        cursor.setString("\r" + ContentBasedDirectiveItem.copyString(count) + "\r");
        if (cursor.isStartOfParagraph())
        {
          item.formatDefault();
        }
        cursor.gotoNextParagraph(false);
        cursor.gotoEndOfParagraph(true);
        item.formatCopy();
        cursor.gotoNextParagraph(false);
        viewCursor.gotoRange(cursor, false);
      }

      model.adoptNumbers();
    }
  }

  /**
   * Delete the item if it's a copy.
   *
   * @param item
   *          The item.
   *
   * @return true, if a copy was deleted, false otherwise.
   */
  private boolean removeCopy(ContentBasedDirectiveItem item)
  {
    if (item.isCopy())
    {
      item.remove();
      return true;
    }
    return false;
  }

  /**
   * Count content based directives before a position (included).
   *
   * @param range
   *          The position to start counting
   * @return The number of content based directives.
   */
  private int countItemsBefore(XParagraphCursor range)
  {
    int count = 0;

    ContentBasedDirectiveItem punkt1 = model.getFirstItem();
    if (punkt1 != null)
      count++;

    XParagraphCursor cursor = UNO
        .XParagraphCursor(range.getText().createTextCursorByRange(range.getStart()));
    ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
    if (cursor != null)
      do
      {
        if (item.isItem() && model.isItemVisible(item))
          count++;
      } while (cursor.gotoPreviousParagraph(false));

    return count;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + model + ")";
  }
}
