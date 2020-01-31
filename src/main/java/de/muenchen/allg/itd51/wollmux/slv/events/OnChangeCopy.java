package de.muenchen.allg.itd51.wollmux.slv.events;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveItem;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;

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
      ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);

      // delete current items
      boolean deletedAtLeastOne = model.removeAllCopies(item);

      // create new copy of none was deleted.
      if (!deletedAtLeastOne)
      {
        cursor.collapseToStart();
        if (item.isItem())
          cursor.gotoEndOfParagraph(false);

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
      }

      model.adoptNumbers();

      viewCursor.gotoRange(cursor, false);
    }
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
        if (item.isItem())
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