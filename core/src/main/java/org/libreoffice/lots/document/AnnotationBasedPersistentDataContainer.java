/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.Size;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XShape;
import com.sun.star.table.BorderLine;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.RelOrientation;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.ext.unohelper.util.UnoService;
import org.libreoffice.lots.util.Utils;

/**
 * Implements the old method of accessing persistent data in notes.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class AnnotationBasedPersistentDataContainer implements
    PersistentDataContainer
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AnnotationBasedPersistentDataContainer.class);

  /**
   * The name of the frame in which WollMux stores its metadata.
   */
  private static final String WOLLMUX_FRAME_NAME = "WollMuxDaten";

  /**
   * Maximum length of text fields that WollMux writes. The length 16000
   * was chosen because of http://qa.openoffice.org/issues/show_bug.cgi?id=108709.
   */
  private static final int TEXTFIELD_MAXLEN = 16000;

  /**
   * The document in which the data is stored.
   */
  private XTextDocument doc;

  /**
   * Contains the dataIDs that were changed before the last call to flush and
   * is required for the workaround for OOo issue 100374. Can be done with removing the
   * Workarounds can also be removed again.
   */
  private HashSet<DataID> modifiedDataIDs;

  /**
   * Creates a new persistent data store in the document doc.
   */
  public AnnotationBasedPersistentDataContainer(XTextDocument doc)
  {
    this.doc = doc;
    this.modifiedDataIDs = new HashSet<>();
  }

  /**
   * The method returns the data stored under ID dataId or null,
   * if none exist.
   */
  @Override
  public String getData(DataID dataId)
  {
    List<Object> textfields =
      getWollMuxTextFields(dataId.getDescriptor(), false, 0);
    if (textfields.isEmpty()) {
      return null;
    }
    Iterator<Object> iter = textfields.iterator();
    StringBuilder buffy = new StringBuilder();
    while (iter.hasNext())
    {
      buffy.append((String) Utils.getProperty(iter.next(), UnoProperty.CONTENT));
    }
    return buffy.toString();
  }

  /**
   * Returns all information text fields with Id fieldName.
   *
   * @param create
   *          if true then corresponding fields will be created, if not
   *          exist.
   * @size falls create == true so many fields are created that size
   *      Characters split into TEXTFIELD_MAXLEN long blocks to be accommodated
   *      can. Any excess fields will be deleted. Also
   *      if size == 0, at least one block is returned.
   * @return empty Vector if field does not exist and create == false or
   *         if an error occurs.
   */
  private List<Object> getWollMuxTextFields(String fieldName, boolean create,
      int size)
  {
    List<Object> textfields = new ArrayList<>();
    XTextFramesSupplier supp = UNO.XTextFramesSupplier(doc);
    if (supp != null)
    {
      int blockCount = (size + (TEXTFIELD_MAXLEN - 1)) / TEXTFIELD_MAXLEN;
      if (blockCount == 0) {
        blockCount = 1;
      }
      try
      {
        UnoDictionary<XShape> frames = UnoDictionary.create(supp.getTextFrames(), XShape.class);
        XShape frame;
        if (frames.containsKey(WOLLMUX_FRAME_NAME))
          frame = UNO.XShape(frames.get(WOLLMUX_FRAME_NAME));
        else
        {
          if (!create) {
            return textfields;
          }

          frame = UNO.XShape(UnoService.createService(UnoService.CSS_TEXT_TEXT_FRAME, doc));
          Size frameSize = new Size();
          frameSize.Height = 5;
          frameSize.Width = 5;
          frame.setSize(frameSize);
          UnoProperty.setProperty(frame, UnoProperty.ANCHOR_TYPE, TextContentAnchorType.AT_PAGE);
          XText text = doc.getText();
          text.insertTextContent(text.getStart(), UNO.XTextContent(frame), false);

          UnoProperty.setProperty(frame, UnoProperty.BACK_TRANSPARENT, Boolean.TRUE);
          UnoProperty.setProperty(frame, UnoProperty.BORDER_DISTANCE, Integer.valueOf(0));
          BorderLine line = new BorderLine(0, (short) 0, (short) 0, (short) 0);
          UnoProperty.setProperty(frame, UnoProperty.LEFT_BORDER, line);
          UnoProperty.setProperty(frame, UnoProperty.TOP_BORDER, line);
          UnoProperty.setProperty(frame, UnoProperty.BOTTOM_BORDER, line);
          UnoProperty.setProperty(frame, UnoProperty.RIGHT_BORDER, line);
          UnoProperty.setProperty(frame, UnoProperty.TEXT_WRAP, WrapTextMode.THROUGHT);
          UnoProperty.setProperty(frame, UnoProperty.HORI_ORIENT, Short.valueOf(HoriOrientation.NONE));
          UnoProperty.setProperty(frame, UnoProperty.HORI_ORIENT_POSITION, Integer.valueOf(0));
          UnoProperty.setProperty(frame, UnoProperty.HORI_ORIENT_RELATION, Short.valueOf(RelOrientation.PAGE_LEFT));
          UnoProperty.setProperty(frame, UnoProperty.VERT_ORIENT, Short.valueOf(VertOrientation.BOTTOM));
          UnoProperty.setProperty(frame, UnoProperty.VERT_ORIENT_RELATION, Short.valueOf(RelOrientation.PAGE_FRAME));
          UnoProperty.setProperty(frame, UnoProperty.FRAME_IS_AUTOMATIC_HEIGHT, Boolean.FALSE);

          XNamed frameName = UNO.XNamed(frame);
          frameName.setName(WOLLMUX_FRAME_NAME);
        }

        UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(frame, XTextRange.class);
        for (XTextRange para : paragraphs)
        {
          if (create)
          {
            UnoProperty.setProperty(para, UnoProperty.CHAR_HIDDEN, Boolean.TRUE);
          }
          UnoCollection<XTextRange> portions = UnoCollection.getCollection(para, XTextRange.class);
          for (XTextRange portion : portions)
          {
            Object textfield =
                UnoProperty.getProperty(portion, UnoProperty.TEXT_FIELD);
            String author = (String) UnoProperty.getProperty(textfield, UnoProperty.AUTHOR);
            if (fieldName.equals(author))
            {
              textfields.add(textfield);
            }
          }
        }

        /*
         * If create == true and too many fields were found, then delete the extra ones.
         */
        if (create && textfields.size() > blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() > blockCount)
          {
            Object textfield = textfields.remove(textfields.size() - 1);
            frameText.removeTextContent(UNO.XTextContent(textfield));
          }
        }

        /*
         * If create == true and too few fields were found, then create
         * additional.
         */
        if (create && textfields.size() < blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() < blockCount)
          {
            Object annotation = UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_ANNOTATION, doc);
            frameText.insertTextContent(frameText.getEnd(),
              UNO.XTextContent(annotation), false);
            UnoProperty.setProperty(annotation, UnoProperty.AUTHOR, fieldName);
            textfields.add(annotation);
          }
        }

      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
        return textfields;
      }
    }
    return textfields;
  }

  /**
   * Stores dataValue with the id dataId persistently in the document. If already
   * If there is data with the same dataId, it will be overwritten.
   */
  @Override
  public void setData(DataID dataId, String dataValue)
  {
    Object recordChanges = Utils.getProperty(doc, UnoProperty.RECORD_CHANGES);
    Utils.setProperty(doc, UnoProperty.RECORD_CHANGES, false);
    List<Object> textfields =
      getWollMuxTextFields(dataId.getDescriptor(), true, dataValue.length());
    if (textfields.isEmpty())
    {
      LOGGER.error("Konnte WollMux-Textfeld(er) \"{}\" nicht anlegen", dataId);
      Utils.setProperty(doc, UnoProperty.RECORD_CHANGES, recordChanges);
      return;
    }

    modifiedDataIDs.add(dataId);
    Iterator<Object> iter = textfields.iterator();
    int start = 0;
    int len = dataValue.length();
    while (iter.hasNext())
    {
      int blocksize = len - start;
      if (blocksize > TEXTFIELD_MAXLEN) {
        blocksize = TEXTFIELD_MAXLEN;
      }
      String str = "";
      if (blocksize > 0)
      {
        str = dataValue.substring(start, start + blocksize);
        start += blocksize;
      }

      Utils.setProperty(iter.next(), "Content", str);
    }
    Utils.setProperty(doc, UnoProperty.RECORD_CHANGES, recordChanges);
  }

  /**
   * Removes the data identified by dataId, if any.
   */
  @Override
  public void removeData(DataID dataId)
  {
    Object recordChanges = Utils.getProperty(doc, UnoProperty.RECORD_CHANGES);
    Utils.setProperty(doc, UnoProperty.RECORD_CHANGES, false);
    List<Object> textfields =
      getWollMuxTextFields(dataId.getDescriptor(), false, 0);
    if (!textfields.isEmpty())
    {
      Iterator<Object> iter = textfields.iterator();
      while (iter.hasNext())
      {
        XTextContent txt = UNO.XTextContent(iter.next());
        try
        {
          txt.getAnchor().getText().removeTextContent(txt);
        }
        catch (Exception x)
        {
          LOGGER.error("", x);
        }
      }
    }
    Utils.setProperty(doc, UnoProperty.RECORD_CHANGES, recordChanges);
    modifiedDataIDs.remove(dataId);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.PersistentData.DataContainer#flush()
   *
   * TESTED
   */
  @Override
  public void flush()
  {
    //
  }
}
