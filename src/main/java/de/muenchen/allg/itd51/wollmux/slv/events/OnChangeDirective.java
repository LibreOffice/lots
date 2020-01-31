package de.muenchen.allg.itd51.wollmux.slv.events;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveItem;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;

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
      ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);

      // delete current items
      boolean deletedAtLeastOne = removeAllItems(cursor);

      if (!deletedAtLeastOne)
      {
        cursor.collapseToStart();
        cursor.gotoStartOfParagraph(false);
        cursor.gotoEndOfParagraph(true);
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

      viewCursor.gotoRange(cursor, false);
    }
  }

  /**
   * Delete all content based directives touched by the cursor.
   *
   * @param cursor
   *          The cursor which may contain content based directives.
   *
   * @return true, if at least one has been deleted, false otherwise.
   */
  private boolean removeAllItems(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);
    if (paragraphs != null)
    {
      for (XTextRange par : paragraphs)
      {
        ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(par);
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
      }
    }
    return deletedAtLeastOne;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
