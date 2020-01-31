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
 * An event for changing recipients of content based directives.
 */
public class OnChangeRecipient extends WollMuxEvent
{

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   */
  public OnChangeRecipient(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    ContentBasedDirectiveModel.createModel(documentController);
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor != null)
    {
      XParagraphCursor cursor = UNO
          .XParagraphCursor(viewCursor.getText().createTextCursorByRange(viewCursor));
      XTextCursor createdZuleitung = null;

      boolean deletedAtLeastOne = removeAllZuleitungszeilen(cursor);
      UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);

      if (!deletedAtLeastOne && paragraphs != null)
      {
        createdZuleitung = createRecipient(cursor, paragraphs);
      }
      if (createdZuleitung != null)
      {
        viewCursor.gotoRange(createdZuleitung, false);
      }
    }
  }

  /**
   * Iterate all paragraphs and make them a recipient line or content based directive with recipient
   * line.
   *
   * @param cursor
   *          The current cursor.
   * @param paragraphs
   *          The paragraphs to iterate.
   * @return
   */
  private XTextCursor createRecipient(XParagraphCursor cursor, UnoCollection<XTextRange> paragraphs)
  {
    XTextCursor createdZuleitung = null;
    for (XTextRange paragraph : paragraphs)
    {
      ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(paragraph);
      if (item.isCopy())
      {
        if (cursor.isCollapsed()) // Ignore if range is selected
        {
          // Create recipient in new line
          paragraph.getEnd().setString("\r");
          createdZuleitung = UNO
              .XTextCursor(paragraph.getText().createTextCursorByRange(paragraph.getEnd()));
          ContentBasedDirectiveItem newItem = new ContentBasedDirectiveItem(createdZuleitung);
          if (createdZuleitung != null)
          {
            createdZuleitung.goRight((short) 1, false);
            newItem.formatRecipientLine();
          }
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
    }
    return createdZuleitung;
  }

  /**
   * Delete all recipient lines touched by the cursor.
   *
   * @param cursor
   *          The cursor which may contain recipient lines.
   *
   * @return true, if at least one has been deleted, false otherwise.
   */
  private boolean removeAllZuleitungszeilen(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);
    if (paragraphs != null)
    {
      for (XTextRange paragraph : paragraphs)
      {
        ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(paragraph);
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