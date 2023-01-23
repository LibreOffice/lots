/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.chaosfirebolt.converter.RomanInteger;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveItem;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockSignature;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveSettings;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.util.UnoProperty;

/**
 * Print function for printing the directives specified in the property {link
 * {@link ContentBasedDirectivePrint#PROP_SLV_SETTINGS}.
 */
public class ContentBasedDirectivePrintOutput extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContentBasedDirectivePrintOutput.class);

  private static final String EXCEPTION_MESSAGE = "Sichtbarkeit konnte nicht geändert werden.";

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegungOutput" and order 150.
   */
  public ContentBasedDirectivePrintOutput()
  {
    super("SachleitendeVerfuegungOutput", 150);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void print(XPrintModel printModel)
  {
    List<ContentBasedDirectiveSettings> settings = new ArrayList<>();
    try
    {
      settings = (List<ContentBasedDirectiveSettings>) printModel
          .getPropertyValue(ContentBasedDirectivePrint.PROP_SLV_SETTINGS);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    short countMax = 0;
    for (ContentBasedDirectiveSettings v : settings)
      countMax += v.getCopyCount();
    printModel.setPrintProgressMaxValue(countMax);

    short count = 0;
    for (ContentBasedDirectiveSettings v : settings)
    {
      if (printModel.isCanceled())
        return;
      if (v.getCopyCount() > 0)
      {
        printVerfuegungspunkt(printModel, v.directiveId, v.isDraft, v.isOriginal, v.getCopyCount());
      }
      count += v.getCopyCount();
      printModel.setPrintProgressValue(count);
    }
  }

  /**
   * Print the content based directive.
   *
   * @param pmod
   *          The {@link XPrintModel}
   * @param verfPunkt
   *          The number of the content based directive. All following items aren't printed.
   * @param isDraft
   *          If true, all blocks marked as {@link PrintBlockSignature#DRAFT_ONLY} are printed,
   *          otherwise not.
   * @param isOriginal
   *          If true, the number of the first content based directive and all all
   *          {@link PrintBlockSignature#NOT_IN_ORIGINAL} blocks are set invisible. Otherwise they are
   *          visible
   * @param copyCount
   *          The number of copies.
   */
  void printVerfuegungspunkt(XPrintModel pmod, int verfPunkt, boolean isDraft, boolean isOriginal,
      short copyCount)
  {
    XTextDocument doc = pmod.getTextDocument();
    ContentBasedDirectiveModel model = ContentBasedDirectiveModel
        .createModel(DocumentManager.getTextDocumentController(doc));

    // Save current cursor position
    XTextCursor vc = null;
    XTextCursor oldViewCursor = null;
    XTextViewCursorSupplier suppl = UNO
        .XTextViewCursorSupplier(UNO.XModel(pmod.getTextDocument()).getCurrentController());
    if (suppl != null)
      vc = suppl.getViewCursor();
    if (vc != null)
      oldViewCursor = vc.getText().createTextCursorByRange(vc);

    // Initialize counter
    ContentBasedDirectiveItem frameItem = model.getFirstItem();
    int count = 0;
    if (frameItem != null)
    {
      count++;
    }

    // Get invisible section
    XTextRange setInvisibleRange = getInvisibleRange(model, verfPunkt, count);

    // Hide text sections in invisible area and remember their status
    List<XTextSection> hidingSections = getSectionsFromPosition(pmod.getTextDocument(),
        setInvisibleRange);
    HashMap<XTextSection, Boolean> sectionOldState = new HashMap<>();
    hideTextSections(hidingSections, sectionOldState);

    // ensprechende Verfügungspunkte ausblenden
    if (setInvisibleRange != null)
    {
      hideTextRange(setInvisibleRange, true);
    }

    // Show/Hide print blocks
    pmod.setPrintBlocksProps(PrintBlockSignature.DRAFT_ONLY.getName(), isDraft, false);
    pmod.setPrintBlocksProps(PrintBlockSignature.NOT_IN_ORIGINAL.getName(), !isOriginal, false);
    pmod.setPrintBlocksProps(PrintBlockSignature.ORIGINAL_ONLY.getName(), isOriginal, false);
    pmod.setPrintBlocksProps(PrintBlockSignature.ALL_VERSIONS.getName(), true, false);
    pmod.setPrintBlocksProps(PrintBlockSignature.COPY_ONLY.getName(), !isDraft && !isOriginal, false);

    // Show/Hide visibility groups
    pmod.setGroupVisible(PrintBlockSignature.DRAFT_ONLY.getGroupName(), isDraft);
    pmod.setGroupVisible(PrintBlockSignature.NOT_IN_ORIGINAL.getGroupName(), !isOriginal);
    pmod.setGroupVisible(PrintBlockSignature.ORIGINAL_ONLY.getGroupName(), isOriginal);
    pmod.setGroupVisible(PrintBlockSignature.ALL_VERSIONS.getGroupName(), true);
    pmod.setGroupVisible(PrintBlockSignature.COPY_ONLY.getGroupName(), !isDraft && !isOriginal);

    ContentBasedDirectiveItem punkt1;
    if (frameItem == null)
    {
      punkt1 = getFirstContent(model);
    } else
    {
      punkt1 = frameItem;
    }
    // hide first number if necessary
    setVisibilityFirst(isOriginal, punkt1, true);

    // print
    for (int j = 0; j < copyCount; ++j)
    {
      pmod.printWithProps();
    }

    // revert hiding of first number
    setVisibilityFirst(isOriginal, punkt1, false);

    // Show/Hide visibility groups
    pmod.setGroupVisible(PrintBlockSignature.DRAFT_ONLY.getGroupName(), true);
    pmod.setGroupVisible(PrintBlockSignature.NOT_IN_ORIGINAL.getGroupName(), true);
    pmod.setGroupVisible(PrintBlockSignature.ORIGINAL_ONLY.getGroupName(), true);
    pmod.setGroupVisible(PrintBlockSignature.ALL_VERSIONS.getGroupName(), true);
    pmod.setGroupVisible(PrintBlockSignature.COPY_ONLY.getGroupName(), true);

    // Restore old print block settings
    pmod.setPrintBlocksProps(PrintBlockSignature.DRAFT_ONLY.getName(), true, true);
    pmod.setPrintBlocksProps(PrintBlockSignature.NOT_IN_ORIGINAL.getName(), true, true);
    pmod.setPrintBlocksProps(PrintBlockSignature.ORIGINAL_ONLY.getName(), true, true);
    pmod.setPrintBlocksProps(PrintBlockSignature.ALL_VERSIONS.getName(), true, true);
    pmod.setPrintBlocksProps(PrintBlockSignature.COPY_ONLY.getName(), true, true);

    // Restore state of invisible text sections
    for (XTextSection section : hidingSections)
    {
      Boolean oldState = sectionOldState.get(section);
      if (oldState != null)
        Utils.setProperty(section, UnoProperty.IS_VISIBLE, oldState);
    }

    hideTextRange(setInvisibleRange, false);

    // reset cursor position
    if (vc != null && oldViewCursor != null)
      vc.gotoRange(oldViewCursor, false);
  }

  private void hideTextRange(XTextRange textRange, boolean hide)
  {
    if (textRange != null)
    {
      try
      {
        UNO.hideTextRange(textRange, hide);
      } catch (UnoHelperException e)
      {
        LOGGER.error(EXCEPTION_MESSAGE, e);
      }
    }
  }

  private void setVisibilityFirst(boolean isOriginal, ContentBasedDirectiveItem punkt1, boolean hide)
  {
    if (isOriginal && punkt1 != null)
    {
      XTextCursor cursor = punkt1.getZifferOnly();
      // The cursor includes the tab but this should not change visiblity
      if (cursor != null)
      {
        // 'I./t' or 'I.' ('I.' without tab is used left of recipient frame in some cases), so
        // only goLeft(1) to exclude tab.
        if (cursor.getString().contains("\t"))
        {
          cursor.goLeft((short) 1, true);
        }

        hideTextRange(cursor, hide);
      }
    }
  }

  private void hideTextSections(List<XTextSection> hidingSections,
      HashMap<XTextSection, Boolean> sectionOldState)
  {
    for (XTextSection section : hidingSections)
    {
      boolean oldState = AnyConverter.toBoolean(Utils.getProperty(section, UnoProperty.IS_VISIBLE));
      sectionOldState.put(section, oldState);
      Utils.setProperty(section, UnoProperty.IS_VISIBLE, Boolean.FALSE);
    }
  }

  private ContentBasedDirectiveItem getFirstContent(ContentBasedDirectiveModel model)
  {
    XParagraphCursor cursor = UNO.XParagraphCursor(
        model.getTextDocument().getText().createTextCursorByRange(model.getTextDocument().getText().getStart()));
    ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
    if (cursor != null)
    {
      do
      {
        cursor.gotoEndOfParagraph(true);
        if (item.isItem() && model.isItemVisible(item))
        {
          return new ContentBasedDirectiveItem(cursor.getText().createTextCursorByRange(cursor));
        }
      } while (cursor.gotoNextParagraph(false));
    }
    return null;
  }

  private XTextRange getInvisibleRange(ContentBasedDirectiveModel model, int verfPunkt, int count)
  {
    XTextRange setInvisibleRange = null;
    XParagraphCursor cursor = UNO.XParagraphCursor(
        model.getTextDocument().getText().createTextCursorByRange(model.getTextDocument().getText().getStart()));
    ContentBasedDirectiveItem item = new ContentBasedDirectiveItem(cursor);
    if (cursor != null)
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (item.isItem() && model.isItemVisible(item))
        {
          count++;

          if (count == (verfPunkt + 1))
          {
            cursor.collapseToStart();
            cursor.gotoRange(cursor.getText().getEnd(), true);
            setInvisibleRange = cursor;
          }
        }
      } while (setInvisibleRange == null && cursor.gotoNextParagraph(false));
    return setInvisibleRange;
  }

  /**
   * Get all text sections, which are at the same position or behind.
   *
   * @param doc
   *          The document to check.
   * @param pos
   *          The starting position to collect text sections.
   * @return List of all text sections behind the position. May be an empty list.
   */
  private List<XTextSection> getSectionsFromPosition(XTextDocument doc, XTextRange pos)
  {
    List<XTextSection> sectionList = new ArrayList<>();
    if (pos == null)
      return sectionList;
    XTextRangeCompare comp = UNO.XTextRangeCompare(pos.getText());
    if (comp == null)
      return sectionList;
    XTextSectionsSupplier suppl = UNO.XTextSectionsSupplier(doc);
    if (suppl == null)
      return sectionList;

    UnoDictionary<XTextSection> sections = UnoDictionary.create(suppl.getTextSections(), XTextSection.class);
    for (XTextSection section : sections.values())
    {
      if (section != null)
      {
        try
        {
          int diff = comp.compareRegionStarts(pos, section.getAnchor());
          if (diff >= 0)
            sectionList.add(section);
        } catch (IllegalArgumentException e)
        {
          // no errer, exception always occurs if the text ranges are in different text objects.
        }
      }
    }
    return sectionList;
  }
}
