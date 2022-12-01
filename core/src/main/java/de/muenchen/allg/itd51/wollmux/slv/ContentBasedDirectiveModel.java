/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.XNamed;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.TextRangeRelation;
import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.document.text.StyleService;
import de.muenchen.allg.itd51.wollmux.HashableComponent;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirective;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirectivePrint;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.ooo.TextDocument;
import de.muenchen.allg.util.UnoProperty;


/**
 * A model for working with content based directives. There is only one model per document
 * (singleton).
 */
public class ContentBasedDirectiveModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectiveModel.class);

  private static final String FRAME_NAME_FIRST_CBD = "WollMuxVerfuegungspunkt1";
  static final String PARA_STYLE_NAME_CBD = "WollMuxVerfuegungspunkt";
  static final String PARA_STYLE_NAME_COPY = "WollMuxVerfuegungspunktAbdruck";
  static final String PARA_STYLE_NAME_CBD_WITH_RECIPIENT = "WollMuxVerfuegungspunktMitZuleitung";
  static final String PARA_STYLE_NAME_FIRST_CBD = "WollMuxVerfuegungspunkt1";
  static final String PARA_STYLE_NAME_RECIPIENT = "WollMuxZuleitungszeile";
  static final String PARA_STYLE_NAME_DEFAULT = "Fließtext";
  static final String CHAR_STYLE_NAME_DEFAULT = "Fließtext";
  static final String CHAR_STYLE_NAME_NUMBER = "WollMuxRoemischeZiffer";

  private static final Map<HashableComponent, ContentBasedDirectiveModel> models = new HashMap<>();

  /**
   * Creates a model for the given controller if it doesn't exist. Otherwise
   * returns the associated model.
   *
   * @param doc
   *          A controller of a document.
   * @return A newly created model or the model associated with this controller.
   */
  public static ContentBasedDirectiveModel createModel(
      TextDocumentController doc)
  {
    HashableComponent hash = new HashableComponent(doc.getModel().doc);
    if (!models.containsKey(hash))
    {
      models.put(hash, new ContentBasedDirectiveModel(doc));
    }
    return models.get(hash);
  }

  /**
   * Creates a model for the given document if it doesn't exist. Otherwise
   * returns the associated model.
   *
   * @param doc
   *          A document.
   * @return A newly created model or the model associated with this document.
   */
  public static ContentBasedDirectiveModel createModel(XTextDocument doc)
  {
    HashableComponent hash = new HashableComponent(doc);
    if (!models.containsKey(hash))
    {
      models.put(hash, new ContentBasedDirectiveModel(doc));
    }
    return models.get(hash);
  }

  private TextDocumentController documentController;
  private final XTextDocument doc;

  /**
   * Creates a new model for the document. This implies creating all necessary
   * styles.
   *
   * @param documentController
   *          The controller of the document.
   */
  private ContentBasedDirectiveModel(TextDocumentController documentController)
  {
    this(documentController, documentController.getModel().doc);
  }

  private ContentBasedDirectiveModel(XTextDocument doc)
  {
    this(null, doc);
  }

  private ContentBasedDirectiveModel(TextDocumentController documentController, XTextDocument doc)
  {
    this.documentController = documentController;
    this.doc = doc;
    createUsedStyles();
    List<Bookmark> bookmarks = getAllPrintBlocks().collect(Collectors.toList());
    for (Bookmark bm : bookmarks)
    {
      for (Bookmark bm2 : bookmarks)
      {
        try
        {
          TextRangeRelation relation = TextRangeRelation.compareTextRanges(bm.getAnchor(), bm2.getAnchor());
          if (TextRangeRelation.A_MATCH_B != relation && !TextRangeRelation.DISTINCT.contains(relation))
          {
            InfoDialog.showInfoModal("Überschneidende Druckblöcke",
                "Das Verhalten bei sich überscheidenen Druckblöcken ist nicht definiert.\n"
                    + "Das kann zu unerwartetem Verhalten führen. Bitte kontaktieren Sie den Ersteller der Vorlage.");
            return;
          }
        } catch (UnoHelperException e)
        {
          LOGGER.debug("Can't find a book mark.", e);
        }
      }
    }
  }

  public TextDocumentController getDocumentController()
  {
    if (documentController == null)
    {
      documentController = DocumentManager.getTextDocumentController(doc);
    }
    return documentController;
  }

  public XTextDocument getTextDocument()
  {
    return doc;
  }

  /**
   * Get all items selected by the cursor.
   *
   * @param cursor
   *          The cursor.
   * @return List of items.
   */
  public List<ContentBasedDirectiveItem> getItemsFor(XParagraphCursor cursor)
  {
    List<ContentBasedDirectiveItem> items = new ArrayList<>();
    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);
    for (XTextRange paragraph : paragraphs)
    {
      if (paragraph == null)
      {
        continue;
      }

      ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(
          paragraph.getText().createTextCursorByRange(paragraph));
      items.add(item);
    }
    return items;
  }

  /**
   * Collect information about visible content based directives from the document.
   *
   * @return List of settings for each item.
   */
  public List<ContentBasedDirective> scanItems()
  {
    List<ContentBasedDirective> items = new ArrayList<>();

    // Check if first content based directive is present
    ContentBasedDirectiveItem item = getFirstItem();
    if (item != null)
    {
      ContentBasedDirective original = new ContentBasedDirective(
          L.m(ContentBasedDirectiveConfig.getNumber(1) + " Original"));
      original.addReceiverLine(L.m("Recipient see recipient field"));
      items.add(original);
    }

    // Iterate over all paragraphs
    XParagraphCursor cursor = UNO
        .XParagraphCursor(getTextDocument().getText().createTextCursorByRange(getTextDocument().getText()));

    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);
    ContentBasedDirective currentVerfpunkt = null;
    for (XTextRange paragraph : paragraphs)
    {
      if (paragraph == null)
      {
        continue;
      }

      item = new ContentBasedDirectiveItem(paragraph);
      if (item.isItem() && isItemVisible(item))
      {
        String heading = paragraph.getString();
        currentVerfpunkt = new ContentBasedDirective(heading);
        currentVerfpunkt.setMinNumberOfCopies(1);
        items.add(currentVerfpunkt);
      }

      // Add recipients, if line is visible
      if ((item.isRecipientLine() || item.isItemWithRecipient()) && currentVerfpunkt != null && isItemVisible(item))
      {
        String recipient = paragraph.getString();
        if (!recipient.isEmpty())
        {
          currentVerfpunkt.addReceiverLine(recipient);
        }
      }
    }

    return items;
  }

  /**
   * Gets a {@link ContentBasedDirectiveItem} of the frame {@link #FRAME_NAME_FIRST_CBD} or null if
   * the frame doesn't exists.
   *
   * @return The item or null.
   */
  public ContentBasedDirectiveItem getFirstItem()
  {
    try
    {
      XTextFramesSupplier supplier = UNO.XTextFramesSupplier(doc);
      if (supplier != null)
      {
        XTextFrame frame = UnoDictionary.create(supplier.getTextFrames(), XTextFrame.class).get(FRAME_NAME_FIRST_CBD);

        if (frame != null)
        {
          XParagraphCursor cursor = UNO
              .XParagraphCursor(frame.getText().createTextCursorByRange(frame.getText()));
          ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
          if (item.isItem())
          {
            return item;
          }

          // set style
          UnoProperty.setProperty(cursor, UnoProperty.PARA_STYLE_NAME, PARA_STYLE_NAME_FIRST_CBD);
          return item;
        }
      }
    } catch (UnoHelperException e)
    {
      LOGGER.trace("No frame found", e);
    }
    return null;
  }

  /**
   * Renumbers all paragraphs, which have a format name starting with {@link #PARA_STYLE_NAME_CBD}.
   * If a paragraph doesn't have a number it is created. If there is a frame with name
   * {@link #PARA_STYLE_NAME_FIRST_CBD}, it's always treated as the first content based directive.
   *
   * @return True if there're content based directives, false otherwise.
   */
  public boolean adoptNumbers()
  {
    ContentBasedDirectiveItem punkt1 = getFirstItem();

    int count = 0;
    if (punkt1 != null)
    {
      count++;
    }

    // Iterate all paragraphs
    XParagraphCursor cursor = UNO
        .XParagraphCursor(doc.getText().createTextCursorByRange(doc.getText().getStart()));
    ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
    if (cursor != null)
    {
      do
      {
        // select whole paragraph
        cursor.gotoEndOfParagraph(true);

        if (item.isItem() && isItemVisible(item))
        {
          count++;
          item.adoptNumber(count);
        }
      } while (cursor.gotoNextParagraph(false));
    }

    adoptFirstNumber(count > 1);

    // add or remove print function
    int effectiveCount = (punkt1 != null) ? count - 1 : count;
    boolean hasCounting = effectiveCount > 0;
    if (hasCounting)
    {
      documentController.addPrintFunction(ContentBasedDirectivePrint.PRINT_FUNCTION_NAME);
    } else
    {
      documentController.removePrintFunction(ContentBasedDirectivePrint.PRINT_FUNCTION_NAME);
    }
    return hasCounting;
  }

  /**
   * Adopt the number of the first item.
   *
   * @param visible
   *          If true sets the number, otherwise no number is visible.
   */
  private void adoptFirstNumber(boolean visible)
  {
    ContentBasedDirectiveItem item = getFirstItem();
    if (item != null)
    {
      XTextRange zifferOnly = item.getZifferOnly();
      if (zifferOnly != null)
      {
	if (!visible)
	{
	  zifferOnly.setString("");
	}
      } else
      {
	if (visible)
	{
	  item.getTextRange().getStart()
	      .setString(ContentBasedDirectiveConfig.getNumber(1));
	}
      }
    }
  }

  /**
   * Creates for all content based directives styles in the document. A new
   * style and changes the style usage.
   */
  public void renameTextStyles()
  {
    HashMap<String, String> mapOldNameToNewName = new HashMap<>();
    XParagraphCursor cursor = UNO.XParagraphCursor(
        doc.getText().createTextCursorByRange(doc.getText().getStart()));
    if (cursor != null)
    {
      ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
      do
      {
        try
        {
          cursor.gotoEndOfParagraph(true);

          if (item.isItem() || item.isRecipientLine() || item.isItemWithRecipient())
          {
            String oldName = AnyConverter.toString(UnoProperty.getProperty(cursor, UnoProperty.PARA_STYLE_NAME));

            // create new style based on old if necessary
            String newName = mapOldNameToNewName.computeIfAbsent(oldName, this::createNewStyle);
            // Save and restore CharHidden property when changing ParaStyleName
            Object hidden = UnoProperty.getProperty(cursor, UnoProperty.CHAR_HIDDEN);
            UnoProperty.setProperty(cursor, UnoProperty.PARA_STYLE_NAME, newName);
            UnoProperty.setProperty(cursor, UnoProperty.CHAR_HIDDEN, hidden);
          }
        } catch (UnoHelperException e)
        {
          LOGGER.debug("Can't rename a style", e);
        }
      } while (cursor.gotoNextParagraph(false));
    }

    renameFirstItem();
  }

  /**
   * Create a new style with a random name.
   *
   * @param parentName
   *          The name of the parent style.
   * @return The name of the new style.
   */
  private String createNewStyle(String parentName)
  {
    String name = "";
    XStyle style = null;
    while (style == null)
    {
      name = "NO" + new Random().nextInt(1000) + "_" + parentName;
      try
      {
        style = StyleService.createParagraphStyle(doc, name, parentName);
      } catch (UnoHelperException e)
      {
        LOGGER.trace("Can't create style");
      }
    }
    return name;
  }

  /**
   * Renames the frame of the fist content based directive.
   */
  private void renameFirstItem()
  {
    try
    {
      XNamed frame = UNO.XNamed(
          UNO.XTextFramesSupplier(doc).getTextFrames().getByName(FRAME_NAME_FIRST_CBD));
      if (frame != null)
        frame.setName("NON_" + FRAME_NAME_FIRST_CBD);
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Create all necessary styles if not present.
   */
  private void createUsedStyles()
  {
    try
    {
      // paragraph styles
      XStyle style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_DEFAULT, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_DEFAULT, null);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_HEIGHT, Integer.valueOf(11));
        UnoProperty.setProperty(style, UnoProperty.CHAR_FONT_NAME, "Arial");
      }

      style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_CBD, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_CBD, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_WEIGHT, Float.valueOf(FontWeight.BOLD));
        UnoProperty.setProperty(style, UnoProperty.PARA_FIRST_LINE_INDENT, Integer.valueOf(-700));
        UnoProperty.setProperty(style, UnoProperty.PARA_TOP_MARGIN, Integer.valueOf(460));
      }

      style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_FIRST_CBD, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_FIRST_CBD, PARA_STYLE_NAME_CBD);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.PARA_FIRST_LINE_INDENT, Integer.valueOf(0));
        UnoProperty.setProperty(style, UnoProperty.PARA_TOP_MARGIN, Integer.valueOf(0));
      }

      style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_COPY, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_COPY, PARA_STYLE_NAME_CBD);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_WEIGHT, Integer.valueOf(100));
        UnoProperty.setProperty(style, UnoProperty.PARA_FIRST_LINE_INDENT, Integer.valueOf(-700));
        UnoProperty.setProperty(style, UnoProperty.PARA_TOP_MARGIN, Integer.valueOf(460));
      }

      style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_RECIPIENT, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_RECIPIENT, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_UNDERLINE, Integer.valueOf(1));
        UnoProperty.setProperty(style, UnoProperty.CHAR_WEIGHT, Float.valueOf(FontWeight.BOLD));
      }

      style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_CBD_WITH_RECIPIENT, null);
      if (style == null)
      {
        style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_CBD_WITH_RECIPIENT, PARA_STYLE_NAME_CBD);
        UnoProperty.setProperty(style, UnoProperty.FOLLOW_STYLE, PARA_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_UNDERLINE, Integer.valueOf(1));
      }

      // character styles
      style = StyleService.getCharacterStyle(doc, CHAR_STYLE_NAME_DEFAULT, null);
      if (style == null)
      {
        style = StyleService.createCharacterStyle(doc, CHAR_STYLE_NAME_DEFAULT, null);
        UnoProperty.setProperty(style, UnoProperty.CHAR_HEIGHT, Integer.valueOf(11));
        UnoProperty.setProperty(style, UnoProperty.CHAR_FONT_NAME, "Arial");
        UnoProperty.setProperty(style, UnoProperty.CHAR_UNDERLINE, Integer.valueOf(0));
      }

      style = StyleService.getCharacterStyle(doc, CHAR_STYLE_NAME_NUMBER, null);
      if (style == null)
      {
        style = StyleService.createCharacterStyle(doc, CHAR_STYLE_NAME_NUMBER, CHAR_STYLE_NAME_DEFAULT);
        UnoProperty.setProperty(style, UnoProperty.CHAR_WEIGHT, Float.valueOf(FontWeight.BOLD));
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("Can't create styles", e);
    }
  }

  /**
   * Collect all book marks with print block commands.
   * 
   * @return Stream of all book marks with print block commands.
   */
  public Stream<Bookmark> getAllPrintBlocks()
  {
    List<Pattern> patterns = getBookmarkPatterns();
    return patterns.stream().flatMap(pattern -> TextDocument.getBookmarkNamesMatching(pattern, doc.getText()).stream())
        .map(name -> {
          try
          {
            return new Bookmark(name, UNO.XBookmarksSupplier(doc));
              } catch (UnoHelperException ex)
          {
            LOGGER.debug("", ex);
            return null;
          }
        }).filter(Objects::nonNull).distinct();
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
    Pattern notInOriginalPattern = DocumentCommands.getPatternForCommand("notInOriginal");
    Pattern originalOnlyPattern = DocumentCommands.getPatternForCommand("originalOnly");
    Pattern copyOnlyPattern = DocumentCommands.getPatternForCommand("copyOnly");

    List<Pattern> bookmarkPatterns = new ArrayList<>();
    bookmarkPatterns.add(allVersionPattern);
    bookmarkPatterns.add(draftOnlyPattern);
    bookmarkPatterns.add(notInOriginalPattern);
    bookmarkPatterns.add(originalOnlyPattern);
    bookmarkPatterns.add(copyOnlyPattern);

    return bookmarkPatterns;
  }

  /**
   * Test if a range is visible in the model.
   *
   * @param item
   *          The item to test.
   * @return False if the chars are hidden or the range lies in an invisible section, true
   *         otherwise.
   */
  public boolean isItemVisible(ContentBasedDirectiveItem item)
  {
    // text has CharHidden property
    try
    {
      if ((boolean) Utils.getProperty(item.getTextRange(), UnoProperty.CHAR_HIDDEN))
      {
        return false;
      }
    } catch (ClassCastException ex)
    {
      LOGGER.error("", ex);
    }

    // check if range is in an invisible section
    UnoDictionary<XTextSection> sections = UnoDictionary
        .create(UNO.XTextSectionsSupplier(getTextDocument()).getTextSections(), XTextSection.class);
    for (XTextSection section : sections.values())
    {
      TextRangeRelation relation = TextRangeRelation.compareTextRanges(item.getTextRange(), section.getAnchor());
      if (!TextRangeRelation.DISTINCT.contains(relation))
      {
        boolean visible = (boolean) Utils.getProperty(section, UnoProperty.IS_VISIBLE);
        if (!visible)
        {
          return false;
        }
      }
    }
    return true;
  }
}
