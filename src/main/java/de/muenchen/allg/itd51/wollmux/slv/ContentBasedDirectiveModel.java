package de.muenchen.allg.itd51.wollmux.slv;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.itd51.wollmux.core.HashableComponent;
import de.muenchen.allg.itd51.wollmux.core.util.PropertyName;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.StyleService;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirectivePrint;

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
    this.documentController = documentController;
    this.doc = documentController.getModel().doc;
    createUsedStyles();
  }

  private ContentBasedDirectiveModel(XTextDocument doc)
  {
    this.doc = doc;
    this.documentController = null;
    createUsedStyles();
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
        XTextFrame frame = UNO.XTextFrame(supplier.getTextFrames().getByName(FRAME_NAME_FIRST_CBD));

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
          Utils.setProperty(cursor, PropertyName.PARA_STYLE_NAME, PARA_STYLE_NAME_FIRST_CBD);
          return item;
        }
      }
    } catch (WrappedTargetException | NoSuchElementException e)
    {
      LOGGER.trace("No frame found", e);
    }
    return null;
  }

  /**
   * Delete all copy lines touched by the item.
   *
   * @param item
   *          The item containing some copy lines.
   *
   * @return true, if a copy was deleted, false otherwise.
   */
  public boolean removeAllCopies(ContentBasedDirectiveItem item)
  {
    boolean deletedAtLeastOne = false;
    String fullText = "";
    fullText = item.getFullLinesOfSelectedCopyLines();
    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(item.getTextRange(),
        XTextRange.class);
    if (paragraphs != null)
    {
      for (XTextRange par : paragraphs)
      {
        String str = Utils.getStringOfXTextRange(par);
        ContentBasedDirectiveItem newItem = new ContentBasedDirectiveItem(par);

        if (newItem.isCopy() && fullText.contains(str))
        {
          // reset existing item
          newItem.remove();
          deletedAtLeastOne = true;
        }
      }
    }
    return deletedAtLeastOne;
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

        if (item.isItem())
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
	cursor.gotoEndOfParagraph(true);

	if (item.isItem() || item.isRecipientLine()
	    || item.isItemWithRecipient())
	{
	  String oldName = AnyConverter.toString(
	      Utils.getProperty(cursor, PropertyName.PARA_STYLE_NAME));

	  // create new style based on old if necessary
	  String newName = mapOldNameToNewName.computeIfAbsent(oldName, key -> {
	    String name = "";
	    do
	    {
	      name = "NO" + new Random().nextInt(1000) + "_" + key;
	    } while (StyleService.createParagraphStyle(doc, name, key) == null);
	    return name;
	  });
	  // Save and restore CharHidden property when changing ParaStyleName
	  Object hidden = Utils.getProperty(cursor, PropertyName.CHAR_HIDDEN);
	  Utils.setProperty(cursor, PropertyName.PARA_STYLE_NAME, newName);
	  Utils.setProperty(cursor, PropertyName.CHAR_HIDDEN, hidden);
	}
      } while (cursor.gotoNextParagraph(false));
    }

    renameFirstItem();
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
    // paragraph styles
    XStyle style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_DEFAULT);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_DEFAULT,
          null);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_HEIGHT, Integer.valueOf(11));
      Utils.setProperty(style, PropertyName.CHAR_FONT_NAME, "Arial");
    }

    style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_CBD);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_CBD,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_WEIGHT,
          Float.valueOf(FontWeight.BOLD));
      Utils.setProperty(style, PropertyName.PARA_FIRST_LINE_INDENT,
          Integer.valueOf(-700));
      Utils.setProperty(style, PropertyName.PARA_TOP_MARGIN,
          Integer.valueOf(460));
    }

    style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_FIRST_CBD);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_FIRST_CBD,
          PARA_STYLE_NAME_CBD);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.PARA_FIRST_LINE_INDENT,
          Integer.valueOf(0));
      Utils.setProperty(style, PropertyName.PARA_TOP_MARGIN,
          Integer.valueOf(0));
    }

    style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_COPY);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_COPY,
          PARA_STYLE_NAME_CBD);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_WEIGHT, Integer.valueOf(100));
      Utils.setProperty(style, PropertyName.PARA_FIRST_LINE_INDENT,
          Integer.valueOf(-700));
      Utils.setProperty(style, PropertyName.PARA_TOP_MARGIN,
          Integer.valueOf(460));
    }

    style = StyleService.getParagraphStyle(doc, PARA_STYLE_NAME_RECIPIENT);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc, PARA_STYLE_NAME_RECIPIENT,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_UNDERLINE, Integer.valueOf(1));
      Utils.setProperty(style, PropertyName.CHAR_WEIGHT,
          Float.valueOf(FontWeight.BOLD));
    }

    style = StyleService.getParagraphStyle(doc,
        PARA_STYLE_NAME_CBD_WITH_RECIPIENT);
    if (style == null)
    {
      style = StyleService.createParagraphStyle(doc,
          PARA_STYLE_NAME_CBD_WITH_RECIPIENT, PARA_STYLE_NAME_CBD);
      Utils.setProperty(style, PropertyName.FOLLOW_STYLE,
          PARA_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_UNDERLINE, Integer.valueOf(1));
    }

    // character styles
    style = StyleService.getCharacterStyle(doc, CHAR_STYLE_NAME_DEFAULT);
    if (style == null)
    {
      style = StyleService.createCharacterStyle(doc, CHAR_STYLE_NAME_DEFAULT,
          null);
      Utils.setProperty(style, PropertyName.CHAR_HEIGHT, Integer.valueOf(11));
      Utils.setProperty(style, PropertyName.CHAR_FONT_NAME, "Arial");
      Utils.setProperty(style, PropertyName.CHAR_UNDERLINE, Integer.valueOf(0));
    }

    style = StyleService.getCharacterStyle(doc, CHAR_STYLE_NAME_NUMBER);
    if (style == null)
    {
      style = StyleService.createCharacterStyle(doc, CHAR_STYLE_NAME_NUMBER,
          CHAR_STYLE_NAME_DEFAULT);
      Utils.setProperty(style, PropertyName.CHAR_WEIGHT,
          Float.valueOf(FontWeight.BOLD));
    }
  }
}
