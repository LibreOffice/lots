/*
 * Dateiname: PersistentData.java
 * Projekt  : WollMux
 * Funktion : Speichert Daten persistent in einem Writer-Dokument.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 09.11.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.Vector;

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

/**
 * Speichert Daten persistent in einem Writer-Dokument.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PersistentData
{
  /**
   * Der Name des Frames in dem der WollMux seine Metadaten speichert.
   */
  private static final String WOLLMUX_FRAME_NAME = "WollMuxDaten";

  /**
   * Maximale Länge von Textfeldern, die der WollMux schreibt. Die Länge 32000
   * wurde gewählt, weil ich nicht sicher bin, ob die Grenze tatsächlich
   * 64kZeichen oder 64kBytes sind. In letzterem Fall könnten Zeichen, die 2
   * Bytes belegen (eine interne UTF16 oder UTF8 kodierung angenommen) schon
   * früher die Grenze treffen. Also lieber auf der sicheren Seite sein und
   * 32000. Eigentlich wären es ja 32767, aber ich hab lieber den glatten Wert.
   */
  private static final int TEXTFIELD_MAXLEN = 32000;

  /**
   * Das Dokument, in dem die Daten gespeichert werden.
   */
  private XTextDocument doc;

  /**
   * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
   */
  public PersistentData(XTextDocument doc)
  {
    this.doc = doc;
  }

  /**
   * Die Methode liefert die unter ID dataId gespeicherten Daten zurück oder
   * null, wenn keine vorhanden sind.
   */
  public String getData(String dataId)
  {
    Vector textfields = getWollMuxTextFields(dataId, false, 0);
    if (textfields.size() == 0) return null;
    Iterator iter = textfields.iterator();
    StringBuilder buffy = new StringBuilder();
    while (iter.hasNext())
    {
      buffy.append((String) UNO.getProperty(iter.next(), "Content"));
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
   *       Zeichen aufgeteilt in TEXTFIELD_MAXLEN lange Blöcke untergebracht
   *       werden können. Eventuell vorhandene überschüssige Felder werden
   *       gelöscht. Auch bei size == 0 wird mindestens ein Block geliefert.
   * @return leeren Vector falls das Feld nicht existiert und create == false
   *         oder falls ein Fehler auftritt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private Vector getWollMuxTextFields(String fieldName, boolean create, int size)
  {
    Vector textfields = new Vector();
    XTextFramesSupplier supp = UNO.XTextFramesSupplier(doc);
    if (supp != null)
    {
      int blockCount = (size + (TEXTFIELD_MAXLEN - 1)) / TEXTFIELD_MAXLEN;
      if (blockCount == 0) blockCount = 1;
      try
      {
        XNameAccess frameAccess = supp.getTextFrames();
        XShape frame;
        if (frameAccess.hasByName(WOLLMUX_FRAME_NAME))
          frame = UNO.XShape(frameAccess.getByName(WOLLMUX_FRAME_NAME));
        else
        {
          if (!create) return textfields;

          frame = UNO.XShape(UNO.XMultiServiceFactory(doc).createInstance(
              "com.sun.star.text.TextFrame"));
          Size frameSize = new Size();
          frameSize.Height = 5;
          frameSize.Width = 5;
          frame.setSize(frameSize);
          UNO.setProperty(frame, "AnchorType", TextContentAnchorType.AT_PAGE);
          XText text = doc.getText();
          text.insertTextContent(
              text.getStart(),
              UNO.XTextContent(frame),
              false);

          UNO.setProperty(frame, "BackTransparent", Boolean.TRUE);
          UNO.setProperty(frame, "BorderDistance", new Integer(0));
          BorderLine line = new BorderLine(0, (short) 0, (short) 0, (short) 0);
          UNO.setProperty(frame, "LeftBorder", line);
          UNO.setProperty(frame, "TopBorder", line);
          UNO.setProperty(frame, "BottomBorder", line);
          UNO.setProperty(frame, "RightBorder", line);
          UNO.setProperty(frame, "TextWrap", WrapTextMode.THROUGHT);
          UNO.setProperty(frame, "HoriOrient", new Short(HoriOrientation.NONE));
          UNO.setProperty(frame, "HoriOrientPosition", new Integer(0));
          UNO.setProperty(frame, "HoriOrientRelation", new Short(
              RelOrientation.PAGE_LEFT));
          UNO.setProperty(
              frame,
              "VertOrient",
              new Short(VertOrientation.BOTTOM));
          // UNO.setProperty(frame, "VertOrientPosition", new Integer(0));
          UNO.setProperty(frame, "VertOrientRelation", new Short(
              RelOrientation.PAGE_FRAME));
          UNO.setProperty(frame, "FrameIsAutomaticHeight", Boolean.FALSE);

          XNamed frameName = UNO.XNamed(frame);
          frameName.setName(WOLLMUX_FRAME_NAME);
        }

        XEnumeration paragraphEnu = UNO.XEnumerationAccess(frame)
            .createEnumeration();
        while (paragraphEnu.hasMoreElements())
        {
          XEnumeration textportionEnu = UNO.XEnumerationAccess(
              paragraphEnu.nextElement()).createEnumeration();
          while (textportionEnu.hasMoreElements())
          {
            Object textfield = UNO.getProperty(
                textportionEnu.nextElement(),
                "TextField");
            String author = (String) UNO.getProperty(textfield, "Author");
            if (fieldName.equals(author)) // ACHTUNG! author.equals(fieldName)
            // wäre falsch!
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
         * Falls create == true und zu wenige Felder gefunden wurden, dann
         * erzeuge zusätzliche.
         */
        if (create && textfields.size() < blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() < blockCount)
          {
            Object annotation = UNO.XMultiServiceFactory(doc).createInstance(
                "com.sun.star.text.TextField.Annotation");
            frameText.insertTextContent(frameText.getEnd(), UNO
                .XTextContent(annotation), false);
            UNO.setProperty(annotation, "Author", fieldName);
            textfields.add(annotation);
          }
        }

      }
      catch (Exception x)
      {
        return textfields;
      }
    } // if (supp != null)
    return textfields;
  }

  /**
   * Speichert dataValue mit der id dataId persistent im Dokument. Falls bereits
   * Daten mit der selben dataId vorhanden sind, werden sie überschrieben.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void setData(String dataId, String dataValue)
  {
    Vector textfields = getWollMuxTextFields(dataId, true, dataValue.length());
    if (textfields.size() == 0)
    {
      Logger.error("Konnte WollMux-Textfeld(er) \""
                   + dataId
                   + "\" nicht anlegen");
      return;
    }

    Iterator iter = textfields.iterator();
    int start = 0;
    int len = dataValue.length();
    while (iter.hasNext())
    {
      int blocksize = len - start;
      if (blocksize > TEXTFIELD_MAXLEN) blocksize = TEXTFIELD_MAXLEN;
      String str = "";
      if (blocksize > 0)
      {
        str = dataValue.substring(start, start + blocksize);
        start += blocksize;
      }

      UNO.setProperty(iter.next(), "Content", str);
    }
  }

  /**
   * Entfernt die mit dataId bezeichneten Daten, falls vorhanden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void removeData(String dataId)
  {
    Vector textfields = getWollMuxTextFields(dataId, false, 0);
    if (textfields.size() > 0)
    {
      Iterator iter = textfields.iterator();
      while (iter.hasNext())
      {
        XTextContent txt = UNO.XTextContent(iter.next());
        try
        {
          txt.getAnchor().getText().removeTextContent(txt);
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
    }
  }

}
