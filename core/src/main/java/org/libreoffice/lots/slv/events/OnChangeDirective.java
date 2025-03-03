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
 * An event for creating or deleting a directive.
 */
public class OnChangeDirective extends WollMuxEvent
{

  private TextDocumentController documentController;
  private ContentBasedDirectiveModel model;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   */
  public OnChangeDirective(TextDocumentController documentController)
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
        deletedAtLeastOne |= removeItem(item);
      }

      if (!deletedAtLeastOne && !items.isEmpty())
      {
        ContentBasedDirectiveItem item = items.get(0);
        if (item.isRecipientLine())
        {
          item.formatVerfuegungspunktWithZuleitung();
        }
        else
        {
          item.formatItem();
        }
      }

      model.adoptNumbers();
    }
  }

  /**
   * Delete the item if it's a directive.
   *
   * @param item
   *          The item.
   *
   * @return true, if at least one has been deleted, false otherwise.
   */
  private boolean removeItem(ContentBasedDirectiveItem item)
  {
    boolean deletedAtLeastOne = false;
    boolean isVerfuegungspunktMitZuleitung = item.isItemWithRecipient();
    if (item.isItem())
    {
      item.remove();
      deletedAtLeastOne = true;
    }
    if (isVerfuegungspunktMitZuleitung)
    {
      item.formatRecipientLine();
    }
    return deletedAtLeastOne;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
