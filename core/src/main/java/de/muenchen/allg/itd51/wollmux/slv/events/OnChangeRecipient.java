/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.slv.events;

import java.util.List;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveItem;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;

/**
 * An event for changing recipients of content based directives.
 */
public class OnChangeRecipient extends WollMuxEvent
{

  private TextDocumentController documentController;
  private ContentBasedDirectiveModel model;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   */
  public OnChangeRecipient(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.model = ContentBasedDirectiveModel.createModel(documentController);
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor == null)
    {
      return;
    }

    XParagraphCursor cursor = UNO.XParagraphCursor(viewCursor.getText().createTextCursorByRange(viewCursor));
    cursor.gotoStartOfParagraph(true);
    List<ContentBasedDirectiveItem> items = model.getItemsFor(cursor);

    // delete current items
    boolean deletedAtLeastOne = false;
    for (ContentBasedDirectiveItem item : items)
    {
      deletedAtLeastOne |= removeZuleitungszeile(item);
    }

    if (!deletedAtLeastOne)
    {
      XTextCursor createCursor = null;
      for (ContentBasedDirectiveItem item : items)
      {
        if (items.size() > 1 && item.isCopy())
        {
          continue;
        }
        createCursor = createRecipient(item);
      }
      if (createCursor != null)
      {
        viewCursor.gotoRange(createCursor, false);
      }
    }
  }

  /**
   * Make the item a recipient line or content based directive with recipient line.
   *
   * @param item
   *          The item.
   * @return A newly created cursor for recipient lines of copies.
   */
  private XTextCursor createRecipient(ContentBasedDirectiveItem item)
  {
    XTextCursor createdZuleitung = null;
    if (item.isCopy())
    {
      XTextCursor paragraph = UNO.XTextCursor(item.getTextRange());
      // Create recipient in new line
      paragraph.getEnd().setString("\r");
      createdZuleitung = UNO.XTextCursor(paragraph.getText().createTextCursorByRange(paragraph.getEnd()));
      ContentBasedDirectiveItem newItem = new ContentBasedDirectiveItem(createdZuleitung);
      if (createdZuleitung != null)
      {
        newItem.formatRecipientLine();
      }
    } else
    {
      if (item.isItem())
      {
        item.formatVerfuegungspunktWithZuleitung();
      } else
      {
        item.formatRecipientLine();
      }
    }
    return createdZuleitung;
  }

  /**
   * Delete the item if it's recipient line.
   *
   * @param item
   *          The item.
   *
   * @return true, if at least one has been deleted, false otherwise.
   */
  private boolean removeZuleitungszeile(ContentBasedDirectiveItem item)
  {
    boolean deletedAtLeastOne = false;
    if (item.isRecipientLine())
    {
      item.formatDefault();
      deletedAtLeastOne = true;
    } else if (item.isItemWithRecipient())
    {
      // Remove recipient from content based directive
      item.formatItem();
      deletedAtLeastOne = true;
    }
    return deletedAtLeastOne;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
