package de.muenchen.allg.itd51.wollmux.slv.print;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.itd51.wollmux.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextRangeRelation;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveConfig;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveItem;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveDialog;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveSettings;
import de.muenchen.allg.util.UnoProperty;

/**
 * Print function for configuration of the content based directive print. It creates a GUI and
 * passes the settings as a property called {@link #PROP_SLV_SETTINGS} of the {@link XPrintModel} to
 * the next {@link PrintFunction}.
 */
public class ContentBasedDirectivePrint extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectivePrint.class);

  /**
   * Key for saving the content based directive settings as a property of a {@link XPrintModel}.
   * This property is read by {@link ContentBasedDirectivePrintOutput}.
   *
   * The property type is a {@link List} of {@link ContentBasedDirectiveSettings}.
   */
  public static final String PROP_SLV_SETTINGS = "SLV_Settings";

  /**
   * The name of this {@link PrintFunction}.
   */
  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegung" and order 50.
   */
  public ContentBasedDirectivePrint()
  {
    super(PRINT_FUNCTION_NAME, 50);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    // add print function SachleitendeVerfuegungOutput
    ContentBasedDirectiveModel model = ContentBasedDirectiveModel
        .createModel(DocumentManager.getTextDocumentController(printModel.getTextDocument()));
    if (model.adoptNumbers())
    {
      try
      {
        printModel.usePrintFunction("SachleitendeVerfuegungOutput");
      } catch (NoSuchMethodException e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
    }

    List<ContentBasedDirectiveSettings> settings = callPrintDialog(model);
    if (settings != null && !settings.isEmpty())
    {
      try
      {
        printModel.setPropertyValue(PROP_SLV_SETTINGS, settings);
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
      printModel.printWithProps();
    }
  }

  /**
   * Show the print dialog for content based directives.
   *
   * @param model
   *          The content based directive model of the document to print.
   *
   * @return A list of settings for each content based directive item.
   */
  private List<ContentBasedDirectiveSettings> callPrintDialog(ContentBasedDirectiveModel model)
  {
    // update document structure (commands, text sections)
    model.getDocumentController().updateDocumentCommands();
    List<ContentBasedDirective> directives = scanItems(model);
    List<ContentBasedDirectiveSettings> settings = new ArrayList<>();

    // call dialog and wait for return.
    try
    {
      SyncActionListener s = new SyncActionListener();
      new ContentBasedDirectiveDialog(directives, s);
      ActionEvent result = s.synchronize();

      if (ContentBasedDirectiveDialog.CMD_SUBMIT.equals(result.getActionCommand()))
      {
        ContentBasedDirectiveDialog dialog = (ContentBasedDirectiveDialog) result.getSource();
        settings = dialog.getSettings();

        if (settings == null || settings.isEmpty())
        {
          LOGGER.debug(
              "Sachleitende Verfuegung: callPrintDialog(): VerfuegungspunktInfos NULL or empty.");
          settings = new ArrayList<>();
        }

        if (!dialog.getPrintOrderAsc())
        {
          // print in descending order.
          List<ContentBasedDirectiveSettings> descSettings = new ArrayList<>();
          ListIterator<ContentBasedDirectiveSettings> lIt = settings.listIterator(settings.size());

          while (lIt.hasPrevious())
          {
            descSettings.add(lIt.previous());
          }
          settings = descSettings;
        }
      }
    } catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
    }
    return settings;
  }

  /**
   * Collect information about content based directives from the document.
   *
   * @param model
   *          The content based directive model of the document to print.
   * @return List of settings for each item.
   */
  private List<ContentBasedDirective> scanItems(ContentBasedDirectiveModel model)
  {
    List<ContentBasedDirective> items = new ArrayList<>();

    // Check if first content based directive is present
    ContentBasedDirectiveItem item = model.getFirstItem();
    if (item != null)
    {
      ContentBasedDirective original = new ContentBasedDirective(
          L.m(ContentBasedDirectiveConfig.getNumber(1) + " Original"));
      original.addReceiverLine(L.m("Empfänger siehe Empfängerfeld"));
      items.add(original);
    }

    // Iterate over all paragraphs
    XParagraphCursor cursor = UNO.XParagraphCursor(model.getTextDocument().getText()
        .createTextCursorByRange(model.getTextDocument().getText()));

    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(cursor, XTextRange.class);
    ContentBasedDirective currentVerfpunkt = null;
    for (XTextRange paragraph : paragraphs)
    {
      if (paragraph == null)
      {
	continue;
      }

      item = new ContentBasedDirectiveItem(paragraph);
      if (item.isItem())
      {
	String heading = paragraph.getString();
	currentVerfpunkt = new ContentBasedDirective(heading);
	currentVerfpunkt.setMinNumberOfCopies(1);
	items.add(currentVerfpunkt);
      }

      boolean rangeVisible = isRangeVisible(model, paragraph);
      // Add recipients, if line is visible
      if ((item.isRecipientLine() || item.isItemWithRecipient())
          && currentVerfpunkt != null && rangeVisible)
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
   * Test if a range is visible in the model.
   *
   * @param model
   *          The document model.
   * @param range
   *          The range to test.
   * @return False if the chars are hidden or the range lies in an invisible section, true
   *         otherwise.
   */
  private boolean isRangeVisible(ContentBasedDirectiveModel model, XTextRange range)
  {
    // text has CharHidden property
    if ((boolean) Utils.getProperty(range, UnoProperty.CHAR_HIDDEN))
    {
      return false;
    }

    // check if range is in an invisible section
    UnoDictionary<XTextSection> sections = UnoDictionary
        .create(UNO.XTextSectionsSupplier(model.getTextDocument()).getTextSections(), XTextSection.class);
    for (XTextSection section : sections.values())
    {
      TextRangeRelation relation = new TextRangeRelation(range, section.getAnchor());
      if (!(relation.followsOrderschemeAABB() || relation.followsOrderschemeBBAA()))
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
