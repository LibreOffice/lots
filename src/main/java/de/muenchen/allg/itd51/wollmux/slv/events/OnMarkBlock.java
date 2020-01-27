package de.muenchen.allg.itd51.wollmux.slv.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.PropertyName;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.itd51.wollmux.event.handlers.BasicEvent;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveConfig;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockSignature;
import de.muenchen.allg.ooo.TextDocument;

/**
 * An event that inserts a bookmark with name {@code WM(CMD '<blockname>')}at the current cursor
 * position. If there is already such a bookmark it is removed.
 *
 * This event is triggered by the toolbar.
 */
public class OnMarkBlock extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnMarkBlock.class);

  private String blockname;
  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   * @param blockname
   *          The name of the block. Only {@link PrintBlockSignature} are supported.
   */
  public OnMarkBlock(TextDocumentController documentController, String blockname)
  {
    this.documentController = documentController;
    this.blockname = blockname;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor range = documentController.getModel().getViewCursor();
    if (UNO.XBookmarksSupplier(documentController.getModel().doc) == null || blockname == null
        || range == null)
    {
      return;
    }

    if (range.isCollapsed())
    {
      InfoDialog.showInfoModal(L.m("Fehler"),
          L.m("Bitte wählen Sie einen Bereich aus, der markiert werden soll."));
      return;
    }

    PrintBlockSignature pbName = PrintBlockSignature.valueOfIgnoreCase(blockname);
    String highlightColor = ContentBasedDirectiveConfig.getHighlightColor(pbName);
    String markChange = pbName.getMessage();

    String bookmarkStart = "WM(CMD '" + blockname + "'";
    String hcAtt = " HIGHLIGHT_COLOR '" + highlightColor + "'";
    String bookmarkName = bookmarkStart + hcAtt + ")";
    List<XNamed> bookmarkByRange = TextDocument.getBookmarkByTextRange(range);
    Pattern bookmarkPattern = DocumentCommands.getPatternForCommand(blockname);
    Set<String> bmNames = TextDocument.getBookmarkNamesMatching(bookmarkPattern, range);

    if (bmNames == null || bmNames.isEmpty())
    {
      deleteCurrentCommand(range);
      setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
    } else
    {
      for (String matchedBookmark : bmNames)
      {
        for (XNamed bookmark : bookmarkByRange)
        {
          if (bookmark != null)
          {
            updateOrDeleteBookmark(range, markChange, bookmark, matchedBookmark, bookmarkName,
                highlightColor);
          }
        }
      }
    }

    // parse commands
    documentController.getModel().getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(documentController,
        WollMuxFiles.isDebugMode());
    dci.scanGlobalDocumentCommands();
    dci.scanInsertFormValueCommands();

    stabilize();
  }

  /**
   * If there is a bookmark with the same name at the text cursor it is deleted otherwise the
   * information is updated.
   *
   * @param range
   *          The current text cursor.
   * @param markChange
   *          The message of the block.
   * @param bookmark
   *          The bookmark in the current Range.
   * @param matchedBookmark
   *          The name of the current bookmark.
   * @param bookmarkName
   *          The name of the new bookmark.
   * @param highlightColor
   *          The highlightColor of updated command.
   */
  private void updateOrDeleteBookmark(XTextCursor range, String markChange, XNamed bookmark,
      String matchedBookmark, String bookmarkName, String highlightColor)
  {
    try
    {
      Bookmark bookmarkToDelete = new Bookmark(matchedBookmark,
          UNO.XBookmarksSupplier(documentController.getModel().doc));

      Bookmark wollBook = new Bookmark(bookmark.getName(),
          UNO.XBookmarksSupplier(documentController.getModel().doc));

      short result = compareBookmarks(bookmarkToDelete, wollBook);
      UNO.setPropertyToDefault(bookmarkToDelete.getTextCursor(), PropertyName.CHAR_BACK_COLOR);
      bookmarkToDelete.remove();

      // bookmarkToDelete ends behind wollBook || bookmarkToDelete ends before wollBook
      if ((result == -1 || result == 1)
          && !(bookmarkToDelete.getName().equals(bookmarkName)))
      {
        setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
      } else // bookmarkToDelete ends at same position as wollBook
      {
        InfoDialog.showInfoModal(L.m("Markierung des Blockes aufgehoben"), L.m(
            "Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. Die bestehende Markierung wurde aufgehoben.",
            markChange));
      }
    } catch (NoSuchElementException ex)
    {
      LOGGER.error("", ex);
    }
  }

  /**
   * Compares the anchor of the bookmarks.
   *
   * @param first
   *          The first bookmark.
   * @param second
   *          The second bookmark.
   * @return {@#link XTextRangeCompare#compareRegionStarts(XTextRange, XTextRange)
   */
  private short compareBookmarks(Bookmark first, Bookmark second)
  {
    XTextRange bookMarkToDeleteAnchor = first.getAnchor();
    XTextRange wollBookAnchor = second.getAnchor();

    XTextRangeCompare c = UNO.XTextRangeCompare(bookMarkToDeleteAnchor.getText());
    return c.compareRegionStarts(bookMarkToDeleteAnchor, wollBookAnchor);
  }

  /**
   * Delete every print block command covered exactly by the range.
   *
   * @param range
   *          The range to check.
   */
  private void deleteCurrentCommand(XTextCursor range)
  {
    for (Pattern pattern : getBookmarkPatterns())
    {
      Set<String> existingBookmarks = TextDocument.getBookmarkNamesMatching(pattern, range);

      for (String bookmark : existingBookmarks)
      {
        try
        {
          Bookmark wollBook = new Bookmark(bookmark,
              UNO.XBookmarksSupplier(documentController.getModel().doc));
          wollBook.remove();
        } catch (NoSuchElementException e)
        {
          LOGGER.error("", e);
        }
      }
    }
  }

  /**
   * Get patterns for all print block commands.
   *
   * @return List of command patterns.
   */
  private static List<Pattern> getBookmarkPatterns()
  {
    Pattern allVersionPattern = DocumentCommands.getPatternForCommand("allVersions");
    Pattern draftOnlyPattern = DocumentCommands.getPatternForCommand("draftOnly");
    Pattern notInOrginalPattern = DocumentCommands.getPatternForCommand("notInOriginal");
    Pattern originalOnlyPattern = DocumentCommands.getPatternForCommand("originalOnly");
    Pattern copyOnlyPattern = DocumentCommands.getPatternForCommand("copyOnly");

    List<Pattern> bookmarkPatterns = new ArrayList<>();
    bookmarkPatterns.add(allVersionPattern);
    bookmarkPatterns.add(draftOnlyPattern);
    bookmarkPatterns.add(notInOrginalPattern);
    bookmarkPatterns.add(originalOnlyPattern);
    bookmarkPatterns.add(copyOnlyPattern);

    return bookmarkPatterns;
  }

  /**
   * Create a new print block command.
   *
   * @param bookmarkName
   *          The name of the bookmark.
   * @param range
   *          The range of the bookmark.
   * @param highlightColor
   *          The background color of the command.
   * @param markChange
   *          The message to display.
   */
  private void setNewDocumentCommand(String bookmarkName, XTextCursor range, String highlightColor,
      String markChange)
  {
    documentController.getModel().addNewDocumentCommand(range, bookmarkName);
    if (highlightColor != null)
    {
      Utils.setProperty(range, PropertyName.CHAR_BACK_COLOR, Integer.parseInt(highlightColor, 16));
      // collapse ViewCursor to show unbiased color
      XTextCursor vc = documentController.getModel().getViewCursor();
      if (vc != null)
      {
        vc.collapseToEnd();
        UNO.setPropertyToDefault(vc, PropertyName.CHAR_BACK_COLOR);
      }
    }
    InfoDialog.showInfoModal(L.m("Block wurde markiert"),
        L.m("Der ausgewählte Block %1", markChange));
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", '"
        + blockname + "')";
  }
}