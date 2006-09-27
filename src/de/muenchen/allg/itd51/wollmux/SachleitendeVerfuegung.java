/*
 * Dateiname: SachleitendeVerfuegung.java
 * Projekt  : WollMux
 * Funktion : Hilfen für Sachleitende Verfügungen.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 26.09.2006 | LUT | Erstellung als SachleitendeVerfuegung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.container.XEnumeration;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;

public class SachleitendeVerfuegung
{
  private static final String ParaStyleNameVerfuegungspunkt = "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameDefault = "Fließtext";

  private static final String FrameNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  /**
   * Enthält einen Vector mit den ersten 15 römischen Ziffern. Mehr wird in
   * Sachleitenden Verfügungen sicherlich nicht benötigt :-)
   */
  private static String[] romanNumbers = new String[] {
                                                       "I.",
                                                       "II.",
                                                       "III.",
                                                       "IV.",
                                                       "V.",
                                                       "VI.",
                                                       "VII.",
                                                       "VIII.",
                                                       "IX.",
                                                       "X.",
                                                       "XI.",
                                                       "XII.",
                                                       "XIII.",
                                                       "XIV.",
                                                       "XV." };

  /**
   * Setzt das Absatzformat des Absatzes, der range berührt, auf
   * "WollMuxVerfuegungspunkt" ODER setzt alle in range enthaltenen
   * Verfügungspunkte auf Fließtext zurück, wenn range einen oder mehrere
   * Verfügungspunkte berührt.
   * 
   * @param range
   *          Die XTextRange, in der sich zum Zeitpunkt des Aufrufs der Cursor
   *          befindet.
   */
  public static void verfuegungspunktEinfuegen(XTextDocument doc,
      XTextRange range)
  {
    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enthält der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gelöscht
    boolean deletedAtLeastOne = false;
    if (UNO.XEnumerationAccess(cursor) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();

      while (xenum.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (par != null && isVerfuegungspunkt(par))
        {
          // Einen evtl. bestehenden Verfuegungspunkt zurücksetzen
          verfuegungspunktLoeschen(par);
          deletedAtLeastOne = true;
        }
      }

    }

    if (!deletedAtLeastOne)
    {
      // Wenn kein Verfügungspunkt gelöscht wurde, sollen alle markierten
      // Paragraphen als Verfuegungspunkte markiert werden.
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt);
    }

    // Ziffernanpassung durchführen:
    ziffernAnpassen(doc);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen
   * als Verfuegungspunkt markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit
   *         "WollMuxVerfuegungspunkt" beginnt.
   */
  private static boolean isVerfuegungspunkt(Object paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName = AnyConverter.toString(UNO.getProperty(
          paragraph,
          "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {
    }
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunkt);
  }

  /**
   * Löscht die römische Ziffer+PUNKT+Tab aus einem als
   * "WollMuxVerfuegungspunkt[...]" markierten Absatz heraus und setzt das
   * Absatzformat auf "Fließtext" zurück.
   * 
   * @param par
   *          der Cursor, der sich in der entsprechenden Zeile befinden muss.
   */
  private static void verfuegungspunktLoeschen(XTextRange par)
  {
    UNO.setProperty(par, "ParaStyleName", ParaStyleNameDefault);

    // Prüfe, ob der Absatz mit einer römischen Ziffer beginnt.
    XTextCursor zifferOnly = getZifferOnly(par);
    if (zifferOnly != null)
    {
      // römische Ziffer löschen.
      zifferOnly.setString("");

      // wenn nächstes Zeichen ein Whitespace-Zeichen ist, wird dieses gelöscht
      zifferOnly.goRight((short) 1, true);
      if (zifferOnly.getString().matches("[ \t]")) zifferOnly.setString("");
    }
  }

  /**
   * Sucht nach allen Absätzen im Haupttextbereich des Dokuments doc (also nicht
   * in Frames), deren Absatzformatname mit "WollMuxVerfuegungspunkt" beginnt
   * und numeriert die bereits vorhandenen römischen Ziffern neu durch oder
   * erzeugt eine neue Ziffer, wenn in einem entsprechenden Verfügungspunkt noch
   * keine Ziffer gesetzt wurde. Ist ein Rahmen mit dem Namen
   * WollMuxVerfuegungspunkt1 vorhanden, der einen als Verfügungpunkt markierten
   * Paragraphen enthält, so wird dieser Paragraph immer (gemäß Konzept) als
   * Verfügungspunkt "I" behandelt.
   * 
   * @param doc
   *          Das Dokument, in dem alle Verfügungspunkte angepasst werden
   *          sollen.
   */
  public static void ziffernAnpassen(XTextDocument doc)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(doc);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    int count = 0;
    if (punkt1 != null) count++;

    // Paragraphen des Texts enumerieren und dabei alle Verfuegungspunkte neu
    // numerieren.
    if (UNO.XEnumerationAccess(doc.getText()) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(doc.getText())
          .createEnumeration();
      while (xenum.hasMoreElements())
      {
        XTextRange paragraph = null;
        try
        {
          paragraph = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (paragraph != null && isVerfuegungspunkt(paragraph))
        {
          // String mit röhmischer Zahl erzeugen
          String number = "" + (count + 1) + ".";
          if (count < romanNumbers.length) number = romanNumbers[count];
          count++;

          // Neue nummer setzen:
          XTextRange zifferOnly = getZifferOnly(paragraph);
          if (zifferOnly != null)
          {
            zifferOnly.setString(number);
          }
          else
          {
            paragraph.getStart().setString(number + "\t");
          }
        }
      }
    }

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1);
      if (zifferOnly != null)
      {
        if (count == 1) zifferOnly.setString("");
      }
      else
      {
        if (count > 1) punkt1.getStart().setString(romanNumbers[0]);
      }
    }
  }

  /**
   * Liefert eine XTextRange, die genau die römische Ziffer am Beginn eines
   * Absatzes umschließt oder null, falls keine Ziffer gefunden wurde. Bei der
   * Suche nach der Ziffer werden nur die ersten 6 Zeichen des Absatzes geprüft.
   * 
   * @param par
   *          die TextRange, die den Paragraphen umschließt, in dessen Anfang
   *          nach der römischen Ziffer gesucht werden soll.
   * @return die TextRange, die genau die römische Ziffer umschließt falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par)
  {
    XTextCursor cursor = par.getText().createTextCursorByRange(par.getStart());

    final String zifferPattern = "^([XIV]+|\\d+)\\.$";

    for (int i = 0; i < 6; i++)
    {
      cursor.goRight((short) 1, true);
      String text = cursor.getString();
      if (text.matches(zifferPattern)) return cursor;
    }

    return null;
  }

  /**
   * Liefert den ersten Paragraphen aus dem TextRahmen WollMuxVerfuegungspunkt1,
   * dessen Absatzformatname mit dem String "WollMuxVerfuegungspunkt" beginnt.
   * 
   * @param doc
   *          das Dokument, in dem sich der TextRahmen WollMuxVerfuegungspunkt1
   *          befindet.
   * @return Die TextRange, die den ganzen Absatz des Verfuegungspunktes
   *         umschließt oder null, falls kein entsprechender TextRahmen oder
   *         darin kein als Verfuegungspunkt markierter Absatz vorhanden ist.
   */
  private static XTextRange getVerfuegungspunkt1(XTextDocument doc)
  {
    XTextFrame frame = null;
    try
    {
      frame = UNO.XTextFrame(UNO.XTextFramesSupplier(doc).getTextFrames()
          .getByName(FrameNameVerfuegungspunkt1));
    }
    catch (java.lang.Exception e)
    {
    }

    if (UNO.XEnumerationAccess(frame) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(frame).createEnumeration();
      while (xenum.hasMoreElements())
      {
        XTextRange paragraph = null;
        try
        {
          paragraph = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (paragraph != null && isVerfuegungspunkt(paragraph))
        {
          return paragraph;
        }
      }
    }

    return null;
  }
}
