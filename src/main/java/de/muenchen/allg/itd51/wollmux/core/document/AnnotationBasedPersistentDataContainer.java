package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.Size;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
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

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Implementiert die alte Zugriffsmethode auf persistente Daten in Notizen.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class AnnotationBasedPersistentDataContainer implements
    PersistentDataContainer
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AnnotationBasedPersistentDataContainer.class);

  /**
   * Property von doc zur Steuerung der Änderungsverfolgung, die beim Schreiben von
   * WollMux-Metadaten temporär ausgeschaltet werden muss.
   */
  private static final String RECORD_CHANGES = "RecordChanges";

  /**
   * Der Name des Frames in dem der WollMux seine Metadaten speichert.
   */
  private static final String WOLLMUX_FRAME_NAME = "WollMuxDaten";

  /**
   * Maximale Länge von Textfeldern, die der WollMux schreibt. Die Länge 16000
   * wurde gewählt, wegen http://qa.openoffice.org/issues/show_bug.cgi?id=108709.
   */
  private static final int TEXTFIELD_MAXLEN = 16000;

  /**
   * Das Dokument, in dem die Daten gespeichert werden.
   */
  private XTextDocument doc;

  /**
   * Enthält die dataIDs, die vor dem letzten Aufruf von flush verändert wurden und
   * wird für den Workaround für OOo-Issue 100374 benötigt. Kann mit Entfernen des
   * Workarounds auch wieder entfernt werden.
   */
  private HashSet<DataID> modifiedDataIDs;

  /**
   * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
   */
  public AnnotationBasedPersistentDataContainer(XTextDocument doc)
  {
    this.doc = doc;
    this.modifiedDataIDs = new HashSet<>();
  }

  /**
   * Die Methode liefert die unter ID dataId gespeicherten Daten zurück oder null,
   * wenn keine vorhanden sind.
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
      buffy.append((String) Utils.getProperty(iter.next(), "Content"));
    }
    return buffy.toString();
  }

  /**
   * Liefert alle Informations-Textfelder mit Id fieldName zurück.
   *
   * @param create
   *          falls true so werden entsprechende Felder angelegt, wenn sie nicht
   *          existieren.
   * @size falls create == true werden soviele Felder angelegt, dass darin size
   *       Zeichen aufgeteilt in TEXTFIELD_MAXLEN lange Blöcke untergebracht werden
   *       können. Eventuell vorhandene überschüssige Felder werden gelöscht. Auch
   *       bei size == 0 wird mindestens ein Block geliefert.
   * @return leeren Vector falls das Feld nicht existiert und create == false oder
   *         falls ein Fehler auftritt.
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
        XNameAccess frameAccess = supp.getTextFrames();
        XShape frame;
        if (frameAccess.hasByName(WOLLMUX_FRAME_NAME))
          frame = UNO.XShape(frameAccess.getByName(WOLLMUX_FRAME_NAME));
        else
        {
          if (!create) {
            return textfields;
          }

          frame =
            UNO.XShape(UNO.XMultiServiceFactory(doc).createInstance(
              "com.sun.star.text.TextFrame"));
          Size frameSize = new Size();
          frameSize.Height = 5;
          frameSize.Width = 5;
          frame.setSize(frameSize);
          UNO.setProperty(frame, "AnchorType", TextContentAnchorType.AT_PAGE);
          XText text = doc.getText();
          text.insertTextContent(text.getStart(), UNO.XTextContent(frame), false);

          UNO.setProperty(frame, "BackTransparent", Boolean.TRUE);
          UNO.setProperty(frame, "BorderDistance", Integer.valueOf(0));
          BorderLine line = new BorderLine(0, (short) 0, (short) 0, (short) 0);
          UNO.setProperty(frame, "LeftBorder", line);
          UNO.setProperty(frame, "TopBorder", line);
          UNO.setProperty(frame, "BottomBorder", line);
          UNO.setProperty(frame, "RightBorder", line);
          UNO.setProperty(frame, "TextWrap", WrapTextMode.THROUGHT);
          UNO.setProperty(frame, "HoriOrient", Short.valueOf(HoriOrientation.NONE));
          UNO.setProperty(frame, "HoriOrientPosition", Integer.valueOf(0));
          UNO.setProperty(frame, "HoriOrientRelation",
            Short.valueOf(RelOrientation.PAGE_LEFT));
          UNO.setProperty(frame, "VertOrient",
            Short.valueOf(VertOrientation.BOTTOM));
          UNO.setProperty(frame, "VertOrientRelation",
            Short.valueOf(RelOrientation.PAGE_FRAME));
          UNO.setProperty(frame, "FrameIsAutomaticHeight", Boolean.FALSE);

          XNamed frameName = UNO.XNamed(frame);
          frameName.setName(WOLLMUX_FRAME_NAME);
        }

        XEnumeration paragraphEnu =
          UNO.XEnumerationAccess(frame).createEnumeration();
        while (paragraphEnu.hasMoreElements())
        {
          Object para = paragraphEnu.nextElement();
          if (create) {
            UNO.setProperty(para, "CharHidden", Boolean.TRUE);
          }
          XEnumeration textportionEnu =
            UNO.XEnumerationAccess(para).createEnumeration();
          while (textportionEnu.hasMoreElements())
          {
            Object textfield =
              UNO.getProperty(textportionEnu.nextElement(), "TextField");
            String author = (String) UNO.getProperty(textfield, "Author");
            // ACHTUNG! author.equals(fieldName) wäre falsch, da author null sein
            // kann!
            if (fieldName.equals(author))
            {
              textfields.add(textfield);
            }
          }
        }

        /*
         * Falls create == true und zuviele Felder gefunden wurden, dann loesche
         * die überzähligen.
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
         * Falls create == true und zu wenige Felder gefunden wurden, dann erzeuge
         * zusätzliche.
         */
        if (create && textfields.size() < blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() < blockCount)
          {
            Object annotation =
              UNO.XMultiServiceFactory(doc).createInstance(
                "com.sun.star.text.TextField.Annotation");
            frameText.insertTextContent(frameText.getEnd(),
              UNO.XTextContent(annotation), false);
            UNO.setProperty(annotation, "Author", fieldName);
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
   * Speichert dataValue mit der id dataId persistent im Dokument. Falls bereits
   * Daten mit der selben dataId vorhanden sind, werden sie überschrieben.
   */
  @Override
  public void setData(DataID dataId, String dataValue)
  {
    Object recordChanges = Utils.getProperty(doc, RECORD_CHANGES);
    Utils.setProperty(doc, RECORD_CHANGES, false);
    List<Object> textfields =
      getWollMuxTextFields(dataId.getDescriptor(), true, dataValue.length());
    if (textfields.isEmpty())
    {
      LOGGER.error(L.m("Konnte WollMux-Textfeld(er) \"%1\" nicht anlegen", dataId));
      Utils.setProperty(doc, RECORD_CHANGES, recordChanges);
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
    Utils.setProperty(doc, RECORD_CHANGES, recordChanges);
  }

  /**
   * Entfernt die mit dataId bezeichneten Daten, falls vorhanden.
   */
  @Override
  public void removeData(DataID dataId)
  {
    Object recordChanges = Utils.getProperty(doc, RECORD_CHANGES);
    Utils.setProperty(doc, RECORD_CHANGES, false);
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
    Utils.setProperty(doc, RECORD_CHANGES, recordChanges);
    modifiedDataIDs.remove(dataId);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#flush()
   *
   * TESTED
   */
  @Override
  public void flush()
  {
    //
  }
}