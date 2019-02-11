package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.ooo.TextDocument;

/**
 * Erzeugt ein neues WollMuxEvent, das über den Bereich des viewCursors im Dokument
 * doc ein neues Bookmark mit dem Namen "WM(CMD'<blockname>')" legt, wenn nicht
 * bereits ein solches Bookmark im markierten Block definiert ist. Ist bereits ein
 * Bookmark mit diesem Namen vorhanden, so wird dieses gelöscht.
 *
 * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
 * auf den Knopf der OOo-Symbolleiste ein "wollmux:markBlock#<blockname>" dispatch
 * erfolgte.
 *
 * @param documentController
 *          Das Textdokument, in dem der Block eingefügt werden soll.
 * @param blockname
 *          Derzeit werden folgende Blocknamen akzeptiert "draftOnly",
 *          "notInOriginal", "originalOnly", "copyOnly" und "allVersions". Alle
 *          anderen Blocknamen werden ignoriert und keine Aktion ausgeführt.
 */
public class OnMarkBlock extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnMarkBlock.class);

  private String blockname;
  private TextDocumentController documentController;

  private static final String CHAR_BACK_COLOR = "CharBackColor";

  public OnMarkBlock(TextDocumentController documentController,
      String blockname)
  {
    this.documentController = documentController;
    this.blockname = blockname;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (UNO.XBookmarksSupplier(documentController.getModel().doc) == null
        || blockname == null)
      return;

    ConfigThingy slvConf = WollMuxFiles.getWollmuxConf()
        .query("SachleitendeVerfuegungen");
    Integer highlightColor = null;

    XTextCursor range = documentController.getModel().getViewCursor();

    if (range == null)
      return;

    if (range.isCollapsed())
    {
      InfoDialog.showInfoModal(L.m("Fehler"),
          L.m("Bitte wählen Sie einen Bereich aus, der markiert werden soll."));
      return;
    }

    String markChange = null;
    if (blockname.equalsIgnoreCase("allVersions"))
    {
      markChange = L.m("wird immer gedruckt");
      highlightColor = getHighlightColor(slvConf,
          "ALL_VERSIONS_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("draftOnly"))
    {
      markChange = L.m("wird nur im Entwurf gedruckt");
      highlightColor = getHighlightColor(slvConf, "DRAFT_ONLY_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("notInOriginal"))
    {
      markChange = L.m("wird im Original nicht gedruckt");
      highlightColor = getHighlightColor(slvConf,
          "NOT_IN_ORIGINAL_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("originalOnly"))
    {
      markChange = L.m("wird ausschließlich im Original gedruckt");
      highlightColor = getHighlightColor(slvConf,
          "ORIGINAL_ONLY_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("copyOnly"))
    {
      markChange = L.m("wird ausschließlich in Abdrucken gedruckt");
      highlightColor = getHighlightColor(slvConf, "COPY_ONLY_HIGHLIGHT_COLOR");
    } else
         return;

    String bookmarkStart = "WM(CMD '" + blockname + "'";
    String bookmarkName = bookmarkStart
        + TextDocument.parseHighlightColor(highlightColor) + ")";
    List<XNamed> bookmarkByRange = TextDocument.getBookmarkByTextRange(range);
    Pattern bookmarkPattern = DocumentCommands.getPatternForCommand(blockname);
    Set<String> bmNames = TextDocument.getBookmarkNamesMatching(bookmarkPattern,
        range);

    if (bmNames == null || bmNames.isEmpty())
    {
      for (Pattern pattern : getBookmarkPatterns())
      {
        Set<String> existingBookmarks = TextDocument
            .getBookmarkNamesMatching(pattern, range);

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

      // neuen Block anlegen
      documentController.getModel().addNewDocumentCommand(range, bookmarkName);
      if (highlightColor != null)
      {
        Utils.setProperty(range, CHAR_BACK_COLOR, highlightColor);
        // ViewCursor kollabieren, da die Markierung die Farben verfälscht
        // darstellt.
        XTextCursor vc = documentController.getModel().getViewCursor();
        if (vc != null)
          vc.collapseToEnd();
      }
      InfoDialog.showInfoModal(L.m("Block wurde markiert"),
          L.m("Der ausgewählte Block %1.", markChange));
    } else
    {
      for (String matchedBookmark : bmNames)
      {
        for (XNamed bookmark : bookmarkByRange)
        {
          if (bookmark == null)
            return;

          try
          {
            Bookmark bookmarkToDelete = new Bookmark(matchedBookmark,
                UNO.XBookmarksSupplier(documentController.getModel().doc));

            Bookmark wollBook = new Bookmark(bookmark.getName(),
                UNO.XBookmarksSupplier(documentController.getModel().doc));

            XTextRange bookMarkToDeleteAnchor = bookmarkToDelete.getAnchor();
            XTextRange wollBookAnchor = wollBook.getAnchor();

            XTextRangeCompare c = UNO
                .XTextRangeCompare(bookMarkToDeleteAnchor.getText());
            try
            {
              short result = c.compareRegionStarts(bookMarkToDeleteAnchor,
                  wollBookAnchor); //compareRegionStarts(R1,R2)
              UNO.setPropertyToDefault(bookmarkToDelete.getTextCursor(),
                  CHAR_BACK_COLOR);
              bookmarkToDelete.remove();

              //R1 ends behind xR2 || r1 ends before r2
              if ((result == -1 || result == 1)
                  && !(bookmarkToDelete.getName().equals(bookmarkName)))
              {
                setNewDocumentCommand(documentController, bookmarkName, range,
                    highlightColor, markChange);
              } else
              {
                InfoDialog.showInfoModal(
                    L.m("Markierung des Blockes aufgehoben"), L.m(
                        "Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. Die bestehende Markierung wurde aufgehoben.",
                        markChange));
                break;
              }

              //r1 ends at same position as r2
              if (result == 0)
              {
                UNO.setPropertyToDefault(bookmarkToDelete.getTextCursor(),
                    CHAR_BACK_COLOR);
                InfoDialog.showInfoModal(
                    L.m("Markierung des Blockes aufgehoben"), L.m(
                        "Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. Die bestehende Markierung wurde aufgehoben.",
                        markChange));
              }
            } catch (IllegalArgumentException e)
            {
              LOGGER.error("", e);
            }
          } catch (NoSuchElementException e)
          {
            LOGGER.error("", e);
          }
        }
      }
    }

    // PrintBlöcke neu einlesen:
    documentController.getModel().getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
        documentController, WollMuxFiles.isDebugMode());
    dci.scanGlobalDocumentCommands();
    dci.scanInsertFormValueCommands();

    stabilize();
  }

  private static List<Pattern> getBookmarkPatterns()
  {
    Pattern allVersionPattern = DocumentCommands
        .getPatternForCommand("allVersions");
    Pattern draftOnlyPattern = DocumentCommands
        .getPatternForCommand("draftOnly");
    Pattern notInOrginalPattern = DocumentCommands
        .getPatternForCommand("notInOriginal");
    Pattern originalOnlyPattern = DocumentCommands
        .getPatternForCommand("originalOnly");
    Pattern copyOnlyPattern = DocumentCommands.getPatternForCommand("copyOnly");

    List<Pattern> bookmarkPatterns = new ArrayList<>();
    bookmarkPatterns.add(allVersionPattern);
    bookmarkPatterns.add(draftOnlyPattern);
    bookmarkPatterns.add(notInOrginalPattern);
    bookmarkPatterns.add(originalOnlyPattern);
    bookmarkPatterns.add(copyOnlyPattern);

    return bookmarkPatterns;
  }

  private static void setNewDocumentCommand(
      TextDocumentController documentController,
      String bookmarkName,
      XTextCursor range,
      int highlightColor,
      String markChange)
  {
    // neuen Block anlegen
    documentController.getModel().addNewDocumentCommand(range, bookmarkName);
    Utils.setProperty(range, CHAR_BACK_COLOR, highlightColor);
    // ViewCursor kollabieren, da die Markierung die Farben verfälscht
    // darstellt.
    XTextCursor vc = documentController.getModel().getViewCursor();
    if (vc != null)
      vc.collapseToEnd();

    InfoDialog.showInfoModal(L.m("Block wurde markiert"),
        L.m("Der ausgewählte Block %1.", markChange));
  }

  /**
   * Liefert einen Integer der Form AARRGGBB (hex), der den Farbwert repräsentiert,
   * der in slvConf im Attribut attribute hinterlegt ist oder null, wenn das
   * Attribut nicht existiert oder der dort enthaltene String-Wert sich nicht in
   * eine Integerzahl konvertieren lässt.
   *
   * @param slvConf
   * @param attribute
   */
  private static Integer getHighlightColor(ConfigThingy slvConf,
      String attribute)
  {
    try
    {
      String highlightColor = slvConf.query(attribute).getLastChild()
          .toString();
      if (highlightColor.equals("") || highlightColor.equalsIgnoreCase("none"))
        return null;
      int hc = Integer.parseInt(highlightColor, 16);
      return Integer.valueOf(hc);
    } catch (NodeNotFoundException e)
    {
      return null;
    } catch (NumberFormatException e)
    {
      LOGGER.error(L.m("Der angegebene Farbwert im Attribut '%1' ist ungültig!",
          attribute));
      return null;
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getModel().hashCode() + ", '"
        + blockname + "')";
  }
}