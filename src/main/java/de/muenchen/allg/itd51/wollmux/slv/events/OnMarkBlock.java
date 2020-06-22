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
package de.muenchen.allg.itd51.wollmux.slv.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.MessageBoxResults;
import com.sun.star.text.XTextCursor;

import de.muenchen.allg.afid.TextRangeRelation;
import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveConfig;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockSignature;
import de.muenchen.allg.util.UnoProperty;

/**
 * An event that inserts a bookmark with name {@code WM(CMD '<blockname>')}at the current cursor
 * position. If there is already such a bookmark it is removed.
 *
 * This event is triggered by the toolbar.
 */
public class OnMarkBlock extends WollMuxEvent
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
    if (UNO.XBookmarksSupplier(documentController.getModel().doc) == null || blockname == null || range == null)
    {
      return;
    }

    if (range.isCollapsed())
    {
      InfoDialog.showInfoModal(L.m("Fehler"), L.m("Bitte wählen Sie einen Bereich aus, der markiert werden soll."));
      return;
    }

    PrintBlockSignature pbName = PrintBlockSignature.valueOfIgnoreCase(blockname);
    String highlightColor = ContentBasedDirectiveConfig.getHighlightColor(pbName);
    String markChange = pbName.getMessage();

    String bookmarkStart = "WM(CMD '" + blockname + "'";
    String hcAtt = " HIGHLIGHT_COLOR '" + highlightColor + "'";
    String bookmarkName = bookmarkStart + hcAtt + ")";
    ContentBasedDirectiveModel model = ContentBasedDirectiveModel.createModel(documentController);

    Map<TextRangeRelation, List<Bookmark>> relations = model.getAllPrintBlocks().collect(Collectors.toMap(b -> {
      try
      {
        return TextRangeRelation.compareTextRanges(range, b.getAnchor());
      } catch (UnoHelperException e)
      {
        LOGGER.debug("Can't compare text ranges", e);
        return TextRangeRelation.IMPOSSIBLE;
      }
    }, List::of, (l1, l2) -> {
      List<Bookmark> merged = new ArrayList<>(l1);
      merged.addAll(l2);
          return merged;
        }));

    if (relations.isEmpty())
    {
      setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
    } else if (relations.containsKey(TextRangeRelation.A_MATCH_B))
    {
      deleteOrUpdate(range, highlightColor, markChange, bookmarkName, relations.get(TextRangeRelation.A_MATCH_B));
    } else if (TextRangeRelation.DISTINCT.containsAll(relations.keySet()))
    {
      // ranges don't overlap
      setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
    } else
    {
      for (Map.Entry<TextRangeRelation, List<Bookmark>> relation : relations.entrySet())
      {
        if (!TextRangeRelation.DISTINCT.contains(relation.getKey()))
        {
          deleteOrUpdateWithWarning(range, highlightColor, markChange, bookmarkName, relation.getValue());
          break;
        }
      }
    }

    // parse commands
    documentController.getModel().getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(documentController, WollMuxFiles.isDebugMode());
    dci.scanGlobalDocumentCommands();
    dci.scanInsertFormValueCommands();
  }

  /**
   * The list of bookmarks is deleted if it contains only bookmarks with the same command. Otherwise
   * the bookmarks arent'deleted and the user is request to confirm, that he wants to create a
   * possibly conflicting command.
   * 
   * @param range
   *          The range of the new bookmark.
   * @param highlightColor
   *          The background color of the new bookmark.
   * @param markChange
   *          The text to show in a dialog.
   * @param bookmarkName
   *          The name of the new bookmark.
   * @param bookmarks
   *          List of other bookmarks in this range.
   */
  private void deleteOrUpdateWithWarning(XTextCursor range, String highlightColor, String markChange,
      String bookmarkName, List<Bookmark> bookmarks)
  {
    boolean allMatch = bookmarks.stream().allMatch(bookmark -> bookmark.getName().equalsIgnoreCase(bookmarkName));
    if (allMatch)
    {
      bookmarks.forEach(this::deleteBookmark);
      InfoDialog.showInfoModal(L.m("Markierung des Blockes aufgehoben"),
          L.m("Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. "
              + "Die bestehende Markierung wurde aufgehoben.", markChange));
    } else
    {
      short result = InfoDialog.showYesNoModal(L.m("Verschiedene Blöcke"),
          L.m("Der ausgewählte Bock enthält bereits eine andere Markierungen.\n"
              + "Soll der neue Block wirklich angelegt werden?\n"
              + "Das kann zu unerwartetem Verhatlten führen und zeigt einen\nWarndialog beim Öffnen der Datei."));
      if (result == MessageBoxResults.YES)
      {
        setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
      }
    }
  }

  /**
   * The list of bookmarks is deleted. If the list of bookmarks contains bookmarks with other
   * commands a new command is created.
   * 
   * @param The
   *          range of the new bookmark.
   * @param highlightColor
   *          The background color of the new bookmark.
   * @param markChange
   *          The text to show in a dialog.
   * @param bookmarkName
   *          The name of the new bookmark.
   * @param bookmarks
   *          List of other bokmarks in this range.
   */
  private void deleteOrUpdate(XTextCursor range, String highlightColor, String markChange, String bookmarkName,
      List<Bookmark> bookmarks)
  {
    boolean allMatch = bookmarks.stream().allMatch(bookmark -> bookmark.getName().equalsIgnoreCase(bookmarkName));
    bookmarks.forEach(this::deleteBookmark);
    if (allMatch)
    {
      InfoDialog.showInfoModal(L.m("Markierung des Blockes aufgehoben"), L.m(
          "Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. "
              + "Die bestehende Markierung wurde aufgehoben.",
          markChange));
    } else
    {
      // if there is an other command, update it
      setNewDocumentCommand(bookmarkName, range, highlightColor, markChange);
    }
  }

  /**
   * Delete a bookmark and reset the background color.
   * 
   * @param bookmark
   *          The bookmark to delete.
   */
  private void deleteBookmark(Bookmark bookmark)
  {
    try
    {
      UnoProperty.setPropertyToDefault(bookmark.getTextCursor(), UnoProperty.CHAR_BACK_COLOR);
      bookmark.remove();
    } catch (UnoHelperException e)
    {
      LOGGER.debug("Can't delete the book mark.", e);
    }
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
  private void setNewDocumentCommand(String bookmarkName, XTextCursor range, String highlightColor, String markChange)
  {
    documentController.getModel().addNewDocumentCommand(range, bookmarkName);
    if (highlightColor != null)
    {
      try
      {
        UnoProperty.setProperty(range, UnoProperty.CHAR_BACK_COLOR, Integer.parseInt(highlightColor, 16));
        // collapse ViewCursor to show unbiased color
        XTextCursor vc = documentController.getModel().getViewCursor();
        if (vc != null)
        {
          vc.collapseToEnd();
          UnoProperty.setPropertyToDefault(vc, UnoProperty.CHAR_BACK_COLOR);
        }
      } catch (UnoHelperException ex)
      {
        LOGGER.error("Couldn't set background color.");
      }
    }
    InfoDialog.showInfoModal(L.m("Block wurde markiert"), L.m("Der ausgewählte Block %1", markChange));
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", '" + blockname + "')";
  }
}