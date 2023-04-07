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
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.document.TextRangeRelation;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.util.L;
import org.libreoffice.ext.unohelper.common.TextDocument;
import org.libreoffice.ext.unohelper.util.UnoService;

/**
 * Class contains static methods that are required for the text block system
 *
 * @author bettina.bauer
 */
public class TextModule
{

  private static final Logger LOGGER = LoggerFactory.getLogger(TextModule.class);

  /**
   * Pattern matching insertFrag bookmarks.
   */
  private static final Pattern INSERTFRAG_PATTERN =
    DocumentCommands.getPatternForCommand("insertFrag");

  private TextModule()
  {
    // hide public constructor
  }

  /**
   * Searches backwards from range for valid text fragment identifiers
   * Arguments, puts a document command around each text module to be inserted
   * 'insertFrag' with the arguments found. It stops at the first paragraph in
   * where no snippet identifier could be identified or where already one
   * insertFrag was present.
   *
   * @param doc
   *          Current text document in which to search
   * @param range
   *          Position in which to search for text fragment identifiers. The
   *          Location can be a marked area or a collapsed cursor
   *          from the back to the first line that does not contain a text fragment
   *          is searched for. Most often it is the viewCursor.
   * @param isManual
   *          denotes insertions that have been made manually. Puts
   *          the optional node MODE = "manual"
   *
   * @throws WollMuxFehlerException
   *          if a problem has occurred (e.g. no text module recognized or
   *          an insertFrag command already exists). An exception becomes exact
   *          then thrown if no text module reference is inserted
   *          could. If at least one was inserted, no exception is thrown
   *          thrown, but stopped the scan at the error point.
   */
  public static void createInsertFragFromIdentifier(XTextDocument doc,
      XTextRange range, boolean isManual) throws WollMuxFehlerException
  {
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    // fetches text modules from .conf and collects them in reverse
    // Order in LinkedList tbList. So later defined
    // Text module sections always take precedence.
    LinkedList<ConfigThingy> tbListe = new LinkedList<>();
    ConfigThingy tbConf = conf.query("Textbausteine");
    Iterator<ConfigThingy> iter = tbConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy confTextbaustein = iter.next();
      tbListe.addFirst(confTextbaustein);
    }

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));

    // Special treatment when the viewCursor already marks an area.
    // In this case, only the content of the area should be evaluated
    // become. By comparing completeContent and collectedContent
    // can be determined whether cursor covers the area (see
    // below).
    String completeContent = cursor.getString();
    String collectedContent = "";
    if (!completeContent.equals("")) cursor.collapseToEnd();

    boolean processedAtLeastOneTBSuccessfully = false;
    boolean foundAtLeastOneTBInCurrentParagraph = false;
    while (true)
    {
      String identifierWithArgs = cursor.getString();
      if (!identifierWithArgs.equals(""))
        collectedContent = identifierWithArgs.substring(0, 1) + collectedContent;

      String[] results = parseIdentifier(identifierWithArgs, tbListe);

      if (results != null)
      {
        foundAtLeastOneTBInCurrentParagraph = true;

        /*
         * See if an insertFrag command already exists to prevent
         * that a second one is placed over it, as this is various misconduct
         * can produce.
         */
        Set<String> bms =
          TextDocument.getBookmarkNamesMatching(INSERTFRAG_PATTERN, cursor);

        if (bms.size() == 0)
        {
          createInsertFrag(doc, cursor, results, isManual);
          processedAtLeastOneTBSuccessfully = true;

          // Cursor collapses so that when you continue searching, it doesn't just happen
          // processed phrase identifier still as part of next
          // identifier is used.
          // So the text module search behaves in contrast to the usual way
          // of regular expression matching NOT greedy, but we take
          // the shortest matching identifier
          cursor.collapseToStart();
        }
        else
        {
          /*
           * An insertFrag command has already been issued at the current cursor position
           * found.
           *
           * We only throw an error if we don't have any boilerplate
           * have processed. Otherwise we just stop without error. It
           * is an absolutely legitimate use case that a user first "TB1"
           * type and then "insert text moduleLINK" (note: only with the
           * Inserting a REFERENCES it is possible that an insertFrag bookmark
           * exists.) and then go down one paragraph and type "TB2" and
           * does "insert snippet link" again.
           */
          if (!processedAtLeastOneTBSuccessfully)
            throw new WollMuxFehlerException(
              L.m("At the insertion point there is already a reference to a text block."));
          else
            break;
        }
      }

      if (cursor.isStartOfParagraph())
      {
        // If we haven't found anything in the whole line, then stop.
        if (!foundAtLeastOneTBInCurrentParagraph) break;

        // go to the previous paragraph, resetting matchedInLine.
        cursor.goLeft((short) 1, false);
        foundAtLeastOneTBInCurrentParagraph = false;
      }
      else
      {
        // move one character to the left (allowing the cursor range to increase) and continue
        // make.
        cursor.goLeft((short) 1, true);
      }

      // Here is the comparison completeContent<->collectedContent: if both
      // match can be aborted because the range then
      // has been fully evaluated.
      if (completeContent.length() > 0 && completeContent.equals(collectedContent))
        break;
    }

    if (!processedAtLeastOneTBSuccessfully)
      throw new WollMuxFehlerException(
        L.m("At the insertion place no text block could be found."));
  }

  /**
   * Parses the supplied identifierWithArgs for all mappings of the form (MATCH
   * ... FRAG_ID ...) contained in the boilerplate sections in tbList
   * and returns null if there was no match on the MATCHes or
   * if there was a match, an array with the new one in the first position
   * contains frag_id and the arguments in the following places.
   *
   * @param identifierWithArgs
   *          A string of the form "<identifier>#arg1#...#argN" where the
   *          Changed the separator "#" to text modules via the SEPARATOR key
   *          can be.
   * @param tbList
   *          A list showing the boilerplate sections in order
   *          contains in which they are to be evaluated.
   * @return array of strings with (frag_id + args) or null
   */
  private static String[] parseIdentifier(String identifierWithArgs,
      List<ConfigThingy> tbListe)
  {
    Iterator<ConfigThingy> iterTbListe = tbListe.iterator();
    while (iterTbListe.hasNext())
    {
      ConfigThingy textbausteine = iterTbListe.next();

      String[] results =
        parseIdentifierInTextbausteine(identifierWithArgs, textbausteine);
      if (results != null) return results;
    }
    return null;
  }

  /**
   * Parses the supplied identifierWithArgs for all mappings of the form (MATCH
   * ... FRAG_ID ...), which is written in text modules (=a single text module section)
   * are included and returns null if there is no match with the
   * MATCHes were or if there was a match an array that is at the first
   * Place containing the new frag_id and in the following places the arguments.
   *
   * @param identifierWithArgs
   *          A string of the form "&lt;identifier&gt;#arg1#...#argN" where the
   *          Changed the separator "#" to text modules via the SEPARATOR key
   *          can be.
   * @param textbausteine
   *          Description of a boilerplate section in the form
   *          "Text modules(SEPARATOR ... abbreviation(...))"
   * @return array of strings with (frag_id + args) or null
   */
  public static String[] parseIdentifierInTextbausteine(String identifierWithArgs,
      ConfigThingy textbausteine)
  {
    // Determine the separator for this text module block
    String separatorString = "#";
    ConfigThingy separator = textbausteine.query("SEPARATOR");
    if (separator.count() > 0)
    {
      try
      {
        separatorString = separator.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        // optional
      }
    }

    // Split identifierWithArgs and get first argument when on end
    // SEPERATOR -1 is another empty element in args[]
    // generated
    String[] args = identifierWithArgs.split(separatorString, -1);
    String first = args[0];

    // Iterate over all nodes of the form "(MATCH ... FRAG_ID ...)"
    ConfigThingy mappingsConf = textbausteine.queryByChild("MATCH");
    Iterator<ConfigThingy> iterMappings = mappingsConf.iterator();
    while (iterMappings.hasNext())
    {
      ConfigThingy mappingConf = iterMappings.next();

      String frag_id = null;
      try
      {
        frag_id = mappingConf.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        LOGGER.error("FRAG_ID specification is missing in {}", mappingConf.stringRepresentation());
        continue;
      }

      ConfigThingy matches = null;
      try
      {
        matches = mappingConf.get("MATCH");
      }
      catch (NodeNotFoundException e)
      {
        // does not occur because the above queryByChild always returns MATCH
        continue;
      }

      for (ConfigThingy it : matches)
      {
        String match = it.toString();

        if (first.matches(match))
        {
          try
          {
            args[0] = first.replaceAll(match, frag_id);
          }
          catch (java.lang.Exception e)
          {
            LOGGER.error("The regular expression grouping $<zahl> used by FRAG_ID does not exist in MATCH.", e);
          }
          return args;
        }
      }
    }
    return null; // if nothing in it
  }

  /**
   * Creates a bookmark of type "WM(CMD'insertFrag' FRAG_ID '&lt;args[0]&gt;'
   * ARGS('&lt;args[1]&gt;' '...' '&lt;args[n]&gt;')" in the document doc at position range.
   *
   * @param doc
   *          Current text document
   * @param range
   *          Place where the bookmark should be set
   * @param args
   *          Parameters passed
   * @param isManual
   *          denotes insertions that have been made manually. Sets the optional
   *          node MODE = "manual"
   */
  public static void createInsertFrag(XTextDocument doc, XTextRange range,
      String[] args, boolean isManual)

  {

    // Create new ConfigThingy for "insertFrag text module":
    ConfigThingy root = new ConfigThingy("");
    ConfigThingy werte = new ConfigThingy("WM");
    root.addChild(werte);

    ConfigThingy wm_cmd = new ConfigThingy("CMD");
    werte.addChild(wm_cmd);

    ConfigThingy wm_frag_id = new ConfigThingy("FRAG_ID");
    werte.addChild(wm_frag_id);

    if (args.length > 1)
    {
      ConfigThingy wm_args = new ConfigThingy("ARGS");
      for (int i = 1; i < args.length; i++)
      {
        ConfigThingy wm_args_entry = new ConfigThingy(args[i]);
        wm_args.addChild(wm_args_entry);
      }
      werte.addChild(wm_args);
    }

    ConfigThingy insertFrag = new ConfigThingy("insertFrag");
    wm_cmd.addChild(insertFrag);

    ConfigThingy wm_frag_id_entry = new ConfigThingy(args[0]);
    wm_frag_id.addChild(wm_frag_id_entry);

    if (isManual)
    {
      ConfigThingy wm_mode = new ConfigThingy("MODE");
      werte.addChild(wm_mode);

      ConfigThingy wm_mode_entry = new ConfigThingy("manual");
      wm_mode.addChild(wm_mode_entry);
    }

    String bookmarkName = DocumentCommand.getCommandString(root);

    LOGGER.trace("Erzeuge Bookmark: '{}'", bookmarkName);

    try
    {
      new Bookmark(bookmarkName, doc, range);
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Method jumps from a placeholder field to the current viewCursor
   * next and then starts again from the beginning
   *
   * @param viewCursor
   *          Current ViewCursor in the document
   */
  public static void jumpPlaceholders(XTextDocument doc, XTextCursor viewCursor)
  {
    XTextCursor oldPos = viewCursor.getText().createTextCursorByRange(viewCursor);

    // Jump to next placeholder. In doing so, take into account that
    // .uno:GotoNextPlacemarker not automatically to a possibly directly on
    // View cursor jumps to the adjacent placeholder, but then straight to
    // next.
    XTextField nearPlacemarker = null;
    if (viewCursor.isCollapsed())
      nearPlacemarker = getPlacemarkerStartingWithRange(doc, viewCursor);
    if (nearPlacemarker != null)
      viewCursor.gotoRange(nearPlacemarker.getAnchor(), false);
    else
      UNO.dispatchAndWait(doc, ".uno:GotoNextPlacemarker");

    // Didn't find another placeholder? I recognize this by the fact that either the
    // view cursor (if it was already on the last placeholder of the document)
    // was collapsed or the view cursor is where it was before
    // stayed.
    if (viewCursor.isCollapsed()
      || new TextRangeRelation(oldPos, viewCursor).followsOrderscheme8888())
    {
      // Try again from the beginning of the document
      viewCursor.gotoRange(doc.getText().getStart(), false);
      nearPlacemarker = null;
      if (viewCursor.isCollapsed())
        nearPlacemarker = getPlacemarkerStartingWithRange(doc, viewCursor);
      if (nearPlacemarker != null)
        viewCursor.gotoRange(nearPlacemarker.getAnchor(), false);
      else
        UNO.dispatchAndWait(doc, ".uno:GotoNextPlacemarker");

      // If still no placeholder was found becomes to mark
      // 'setJumpMark' jumped if available otherwise an error message appears
      if (new TextRangeRelation(doc.getText().getStart(), viewCursor).followsOrderscheme8888())
      {
        // Set the view cursor back to its original position.
        viewCursor.gotoRange(oldPos, false);

        // and call handle jumpToMark.
        new OnJumpToMark(doc, true).emit();
      }
    }
  }

  /**
   * Returns the first text field object of type placemarker from the document doc,
   * which starts at the same position as range, or null if one
   * such object is not found.
   */
  private static XTextField getPlacemarkerStartingWithRange(XTextDocument doc,
      XTextCursor range)
  {
    if (UNO.XTextFieldsSupplier(doc) == null)
    {
      return null;
    }
    UnoCollection<XTextField> textFields = UnoCollection.getCollection(UNO.XTextFieldsSupplier(doc).getTextFields(),
        XTextField.class);
    for (XTextField tf : textFields)
    {
      if (tf != null && UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_JUMP_EDIT))
      {
        XTextRangeCompare c = UNO.XTextRangeCompare(range.getText());
        try
        {
          if (c.compareRegionStarts(range, tf.getAnchor()) == 0) return tf;
        }
        catch (IllegalArgumentException e)
        {
          LOGGER.trace("", e);
        }
      }
    }
    return null;
  }
}
