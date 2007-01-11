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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.util.XChangesBatch;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.PrintFailedException;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog;

public class SachleitendeVerfuegung
{
  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  private static final String CHARACTER_STYLES = "CharacterStyles";

  private static final String PARAGRAPH_STYLES = "ParagraphStyles";

  private static final String ParaStyleNameVerfuegungspunkt = "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameAbdruck = "WollMuxVerfuegungspunktAbdruck";

  private static final String ParaStyleNameVerfuegungspunktMitZuleitung = "WollMuxVerfuegungspunktMitZuleitung";

  private static final String ParaStyleNameZuleitungszeile = "WollMuxZuleitungszeile";

  private static final String ParaStyleNameDefault = "Fließtext";

  private static final String CharStyleNameDefault = "Fließtext";

  private static final String CharStyleNameRoemischeZiffer = "WollMuxRoemischeZiffer";

  private static final String FrameNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String zifferPattern = "^([XIV]+|\\d+)\\.\t";

  private static boolean firstTime = true;

  /**
   * Enthält einen Vector mit den ersten 15 römischen Ziffern. Mehr wird in
   * Sachleitenden Verfügungen sicherlich nicht benötigt :-)
   */
  private static final String[] romanNumbers = new String[] {
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
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertVerfuegungspunkt(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    // AutoNummerierung abstellen:
    switchOffAutoNumbering();

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enthält der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gelöscht
    boolean deletedAtLeastOne = removeAllVerfuegungspunkte(cursor);

    if (!deletedAtLeastOne)
    {
      // neuen Verfügungspunkt setzen:
      cursor.collapseToStart();
      cursor.gotoStartOfParagraph(false);
      cursor.gotoEndOfParagraph(true);
      if (isZuleitungszeile(cursor))
        formatVerfuegungspunktWithZuleitung(cursor);
      else
        formatVerfuegungspunkt(cursor);
    }

    // Ziffernanpassung durchführen:
    ziffernAnpassen(model);

    return null;
  }

  /**
   * Erzeugt am Ende des Paragraphen, der von range berührt wird, einen neuen
   * Paragraphen, setzt diesen auf das Absatzformat
   * WollMuxVerfuegungspunktAbdruck und belegt ihn mit dem String "Abdruck von
   * <Vorgänger>" ODER löscht alle Verfügungspunkte die der range berührt, wenn
   * in ihm mindestens ein bereits bestehender Verfügungspunkt enthalten ist.
   * 
   * @param doc
   *          Das Dokument, in dem der Verfügungspunkt eingefügt werden soll
   *          (wird für die Ziffernanpassung benötigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertAbdruck(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    // AutoNummerierung abstellen:
    switchOffAutoNumbering();

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enthält der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gelöscht
    boolean deletedAtLeastOne = removeAllAbdruecke(cursor);

    if (!deletedAtLeastOne)
    {
      // Abdruck einfügen, wenn kein Verfügungspunkt gelöscht wurde:

      // Startposition des cursors setzen. Bereiche werden auf den Anfang
      // kollabiert. Bei Verfügungspunkten wird am Absatzende eingefügt.
      cursor.collapseToStart();
      if (isVerfuegungspunkt(cursor)) cursor.gotoEndOfParagraph(false);

      int count = countVerfPunkteBefore(model.doc, cursor) + 1;
      cursor.setString("\r" + abdruckString(count) + "\r");
      // Falls Cursor auf dem Zeilenanfang stand, wird die Formatierung auf
      // Standardformatierung gesetzt
      if (cursor.isStartOfParagraph()) formatDefault(cursor.getStart());
      cursor.gotoNextParagraph(false);
      cursor.gotoEndOfParagraph(true);
      formatAbdruck(cursor);
      cursor.gotoNextParagraph(false);
    }

    // Ziffern anpassen:
    ziffernAnpassen(model);

    return cursor;
  }

  /**
   * Formatiert alle Paragraphen die der TextRange range berührt mit dem
   * Absatzformat WollMuxZuleitungszeile und markiert diese Zeilen damit auch
   * semantisch als Zuleitungszeilen ODER setzt das Absatzformat der
   * ensprechenden Paragraphen wieder auf Fließtext zurück, wenn mindestens ein
   * Paragraph bereits eine Zuleitungszeile ist.
   * 
   * @param doc
   *          Das Dokument in dem die sich range befindet.
   * @param range
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertZuleitungszeile(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    // AutoNummerierung abstellen:
    switchOffAutoNumbering();

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));
    XTextCursor createdZuleitung = null;

    boolean deletedAtLeastOne = removeAllZuleitungszeilen(cursor);

    if (!deletedAtLeastOne && UNO.XEnumerationAccess(cursor) != null)
    {
      // Im cursor enthaltene Paragraphen einzeln iterieren und je nach Typ
      // entweder eine Zuleitungszeile oder einen Verfügungspunkt mit Zuleitung
      // setzen.
      XEnumeration paragraphs = UNO.XEnumerationAccess(cursor)
          .createEnumeration();
      while (paragraphs.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(paragraphs.nextElement());
        }
        catch (java.lang.Exception e)
        {
        }

        if (par != null)
        {
          if (isAbdruck(par))
          {
            if (cursor.isCollapsed()) // Ignorieren, wenn Bereich ausgewählt.
            {
              // Zuleitung in neuer Zeile erzeugen:
              par.getEnd().setString("\r");
              createdZuleitung = par.getText().createTextCursorByRange(
                  par.getEnd());
              if (createdZuleitung != null)
              {
                createdZuleitung.goRight((short) 1, false);
                formatZuleitungszeile(createdZuleitung);
              }
            }
          }
          else if (isVerfuegungspunkt(par))
            formatVerfuegungspunktWithZuleitung(par);
          else
            formatZuleitungszeile(par);
        }
      }
    }
    return createdZuleitung;
  }

  /**
   * Diese Methode löscht alle Verfügungspunkte, die der Bereich des Cursors
   * cursor berührt, und liefert true zurück, wenn mindestens ein
   * Verfügungspunkt gelöscht wurde oder false, wenn sich in dem Bereich des
   * Cursors kein Verfügungspunkt befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verfügungspunkt gelöscht wurde oder
   *         false, wenn kein der cursor keinen Verfügungspunkt berührt.
   */
  private static boolean removeAllVerfuegungspunkte(XParagraphCursor cursor)
  {
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

        if (par != null)
        {
          boolean isVerfuegungspunktMitZuleitung = isVerfuegungspunktMitZuleitung(par);
          if (isVerfuegungspunkt(par))
          {
            // Einen evtl. bestehenden Verfuegungspunkt zurücksetzen
            removeSingleVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
          if (isVerfuegungspunktMitZuleitung) formatZuleitungszeile(par);
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode löscht alle Abdruck-Zeilen, die der Bereich des Cursors
   * cursor berührt, und liefert true zurück, wenn mindestens ein Abdruck
   * gelöscht wurde oder false, wenn sich in dem Bereich des Cursors kein
   * Abdruck befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Abdrücken gesucht wird.
   * 
   * @return true, wenn mindestens ein Abdruck gelöscht wurde oder false, wenn
   *         kein der cursor keinen Verfügungspunkt berührt.
   */
  private static boolean removeAllAbdruecke(XParagraphCursor cursor)
  {
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

        if (par != null)
        {
          if (isAbdruck(par))
          {
            // Einen evtl. bestehenden Verfuegungspunkt zurücksetzen
            removeSingleVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode löscht alle Zuleitungszeilen, die der Bereich des Cursors
   * cursor berührt, und liefert true zurück, wenn mindestens eine
   * Zuleitungszeile gelöscht wurde oder false, wenn sich in dem Bereich des
   * Cursors keine Zuleitungszeile befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Zuleitungszeilen gesucht wird.
   * 
   * @return true, wenn mindestens eine Zuleitungszeile gelöscht wurde oder
   *         false, wenn kein der cursor keine Zuleitungszeile berührt.
   */
  private static boolean removeAllZuleitungszeilen(XParagraphCursor cursor)
  {
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

        if (par != null)
        {
          if (isZuleitungszeile(par))
          {
            // Zuleitungszeile zurücksetzen
            formatDefault(par);
            deletedAtLeastOne = true;
          }
          else if (isVerfuegungspunktMitZuleitung(par))
          {
            // Zuleitung aus Verfügungspunkt entfernen:
            formatVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Löscht die römische Ziffer+PUNKT+Tab aus einem als
   * "WollMuxVerfuegungspunkt[...]" markierten Absatz heraus und setzt das
   * Absatzformat auf "Fließtext" zurück.
   * 
   * @param par
   *          der Cursor, der sich in der entsprechenden Zeile befinden muss.
   */
  private static void removeSingleVerfuegungspunkt(XTextRange par)
  {
    formatDefault(par);

    // Prüfe, ob der Absatz mit einer römischen Ziffer beginnt.
    XTextCursor zifferOnly = getZifferOnly(par, false);

    // römische Ziffer löschen.
    if (zifferOnly != null) zifferOnly.setString("");

    // wenn es sich bei dem Paragraphen um einen Abdruck handelt, wird dieser
    // vollständig gelöscht.
    if (isAbdruck(par))
    {
      // Den Absatz mit dem String "Ziffer.\tAbdruck von..." löschen
      par.setString("");

      XParagraphCursor parDeleter = UNO.XParagraphCursor(par.getText()
          .createTextCursorByRange(par.getEnd()));

      // Löscht das Leerzeichen vor dem Abdruck und nach dem Abdruck (falls eine
      // Leerzeile folgt)
      parDeleter.goLeft((short) 1, true);
      parDeleter.setString("");
      if (parDeleter.goRight((short) 1, false) && parDeleter.isEndOfParagraph())
      {
        parDeleter.goLeft((short) 1, true);
        parDeleter.setString("");
      }
    }
  }

  /**
   * Formatiert den übergebenen Absatz paragraph in der Standardschriftart
   * "Fließtext".
   * 
   * @param paragraph
   */
  private static void formatDefault(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameDefault);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den übergebenen Absatz paragraph als Abdruck.
   * 
   * @param paragraph
   */
  private static void formatAbdruck(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameAbdruck);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den übergebenen Absatz paragraph als Verfügungspunkt mit
   * Zuleitungszeile.
   * 
   * @param paragraph
   */
  private static void formatVerfuegungspunktWithZuleitung(XTextRange paragraph)
  {
    UNO.setProperty(
        paragraph,
        "ParaStyleName",
        ParaStyleNameVerfuegungspunktMitZuleitung);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den übergebenen Absatz paragraph als Verfügungspunkt.
   * 
   * @param paragraph
   */
  private static void formatVerfuegungspunkt(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameVerfuegungspunkt);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den übergebenen Absatz paragraph als Zuleitungszeile.
   * 
   * @param paragraph
   */
  private static void formatZuleitungszeile(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameZuleitungszeile);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Holt sich aus dem übergebenen Absatz paragraph nur den Breich der römischen
   * Ziffer (+Tab) und formatiert diesen im Zeichenformat
   * WollMuxRoemischeZiffer.
   * 
   * @param paragraph
   */
  private static void formatRoemischeZifferOnly(XTextRange paragraph)
  {
    XTextCursor zifferOnly = getZifferOnly(paragraph, false);
    if (zifferOnly != null)
    {
      UNO
          .setProperty(
              zifferOnly,
              "CharStyleName",
              CharStyleNameRoemischeZiffer);

      // Zeichen danach auf Standardformatierung setzen, damit der Text, der
      // danach geschrieben wird nicht auch obiges Zeichenformat besitzt:
      // ("Standard" gilt laut DevGuide auch in englischen Versionen)
      UNO.setProperty(zifferOnly.getEnd(), "CharStyleName", "Standard");
    }
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
  private static boolean isVerfuegungspunkt(XTextRange paragraph)
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
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen
   * als VerfuegungspunktMitZuleitung markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit
   *         "WollMuxVerfuegungspunktMitZuleitung" beginnt.
   */
  private static boolean isVerfuegungspunktMitZuleitung(XTextRange paragraph)
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
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunktMitZuleitung);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen
   * als Zuleitungszeile markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit "WollMuxZuleitungszeile"
   *         beginnt.
   */
  private static boolean isZuleitungszeile(XTextRange paragraph)
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
    return paraStyleName.startsWith(ParaStyleNameZuleitungszeile);
  }

  /**
   * Liefert true, wenn der übergebene Paragraph paragraph den für Abdrucke
   * typischen String in der Form "Abdruck von I[, II, ...][ und n]" enthält,
   * andernfalls false.
   * 
   * @param paragraph
   *          der zu prüfende Paragraph
   * @return
   */
  private static boolean isAbdruck(XTextRange paragraph)
  {
    String str = paragraph.getString();
    return str.contains("Abdruck von I.")
           || str.contains("Abdruck von <Vorgänger>.");
  }

  /**
   * Liefert true, wenn die Überschrift heading eines Verfügungspunktes nach der
   * römischen Ziffer die Typischen Merkmale einer Wiedervorlage besitzt, d.h.
   * z.B. mit den Strings "w.V." oder ähnlichen beginnt.
   */
  private static boolean isWiedervorlage(String heading)
  {
    final String rest = "(\\s+.*)?";
    if (heading.matches(zifferPattern + "[wW]\\.?\\s?[vV]\\.?" + rest))
      return true;
    if (heading.matches(zifferPattern + "[wW]iedervorlage" + rest))
      return true;
    if (heading.matches(zifferPattern + "[aA]blegen" + rest)) return true;
    if (heading.matches(zifferPattern + "[wW]eglegen" + rest)) return true;
    if (heading.matches(zifferPattern + "[zZ]um\\s[aA]kt" + rest)) return true;
    if (heading.matches(zifferPattern + "[zZ]\\.?\\s?[aA]\\.?" + rest))
      return true;
    return false;
  }

  /**
   * Zählt die Anzahl Verfügungspunkte im Dokument vor der Position von
   * range.getStart() (einschließlich) und liefert deren Anzahl zurück, wobei
   * auch ein evtl. vorhandener Rahmen WollMuxVerfuegungspunkt1 mit gezählt
   * wird.
   * 
   * @param doc
   *          Das Dokument in dem sich range befindet (wird benötigt für den
   *          Rahmen WollMuxVerfuegungspunkt1)
   * @param range
   *          Die TextRange, bei der mit der Zählung begonnen werden soll.
   * @return die Anzahl Verfügungspunkte vor und mit range.getStart()
   */
  public static int countVerfPunkteBefore(XTextDocument doc,
      XParagraphCursor range)
  {
    int count = 0;

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null) count++;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range.getStart()));
    if (cursor != null) do
    {
      if (isVerfuegungspunkt(cursor)) count++;
    } while (cursor.gotoPreviousParagraph(false));

    return count;
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
  public static void ziffernAnpassen(TextDocumentModel model)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    int count = 0;
    if (punkt1 != null) count++;

    // Paragraphen des Texts enumerieren und dabei alle Verfuegungspunkte neu
    // nummerieren. Die Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(model.doc.getText()
        .createTextCursorByRange(model.doc.getText().getStart()));
    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          count++;

          if (isAbdruck(cursor))
          {
            // Behandlung von Paragraphen mit einem "Abdruck"-String
            String abdruckStr = abdruckString(count);
            if (!cursor.getString().equals(abdruckStr))
            {
              cursor.setString(abdruckStr);
              formatRoemischeZifferOnly(cursor);
            }
          }
          else
          {
            // Behandlung von normalen Verfügungspunkten:
            String numberStr = romanNumber(count) + "\t";
            XTextRange zifferOnly = getZifferOnly(cursor, false);
            if (zifferOnly != null)
            {
              // Nummer aktualisieren wenn sie nicht mehr stimmt.
              if (!zifferOnly.getString().equals(numberStr))
                zifferOnly.setString(numberStr);
            }
            else
            {
              // Nummer neu anlegen, wenn wie noch gar nicht existierte
              zifferOnly = cursor.getText().createTextCursorByRange(
                  cursor.getStart());
              zifferOnly.setString(numberStr);
              formatRoemischeZifferOnly(zifferOnly);
            }
          }
        }
      } while (cursor.gotoNextParagraph(false));

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1, false);
      if (zifferOnly != null)
      {
        if (count == 1) zifferOnly.setString("");
      }
      else
      {
        if (count > 1) punkt1.getStart().setString(romanNumbers[0]);
      }
    }

    // Setzte die Druckfunktion SachleitendeVerfuegung wenn mindestens manuell
    // eingefügter Verfügungspunkt vorhanden ist. Ansonsten setze die
    // Druckfunktion zurück.
    int effectiveCount = (punkt1 != null) ? count - 1 : count;
    if (effectiveCount > 0)
      model.setPrintFunction(PRINT_FUNCTION_NAME);
    else
      model.resetPrintFunction(PRINT_FUNCTION_NAME);
  }

  /**
   * Liefert eine XTextRange, die genau die römische Ziffer (falls vorhanden mit
   * darauf folgendem \t-Zeichen) am Beginn eines Absatzes umschließt oder null,
   * falls keine Ziffer gefunden wurde. Bei der Suche nach der Ziffer werden nur
   * die ersten 7 Zeichen des Absatzes geprüft.
   * 
   * @param par
   *          die TextRange, die den Paragraphen umschließt, in dessen Anfang
   *          nach der römischen Ziffer gesucht werden soll.
   * @param includeNoTab
   *          ist includeNoTab == true, so enthält der cursor immer nur die
   *          Ziffer ohne das darauf folgende Tab-Zeichen.
   * @return die TextRange, die genau die römische Ziffer umschließt falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par, boolean includeNoTab)
  {
    XParagraphCursor cursor = UNO.XParagraphCursor(par.getText()
        .createTextCursorByRange(par.getStart()));

    for (int i = 0; i < 7; i++)
    {
      String text = "";
      if (!cursor.isEndOfParagraph())
      {
        cursor.goRight((short) 1, true);
        text = cursor.getString();
        if (includeNoTab) text += "\t";
      }
      else
      {
        // auch eine Ziffer erkennen, die nicht mit \t endet.
        text = cursor.getString() + "\t";
      }
      if (text.matches(zifferPattern + "$")) return cursor;
    }

    return null;
  }

  /**
   * Liefert das Textobjekt des TextRahmens WollMuxVerfuegungspunkt1 oder null,
   * falls der Textrahmen nicht existiert. Der gesamte Text innerhalb des
   * Textrahmens wird dabei automatisch mit dem Absatzformat
   * WollMuxVerfuegungspunkt1 vordefiniert.
   * 
   * @param doc
   *          das Dokument, in dem sich der TextRahmen WollMuxVerfuegungspunkt1
   *          befindet (oder nicht).
   * @return Das Textobjekts des TextRahmens WollMuxVerfuegungspunkt1 oder null,
   *         falls der Textrahmen nicht existiert.
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

    if (frame != null)
    {
      XTextCursor cursor = frame.getText().createTextCursorByRange(
          frame.getText());
      if (isVerfuegungspunkt(cursor)) return cursor;

      // Absatzformat WollMuxVerfuegungspunkt1 setzen wenn noch nicht gesetzt.
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt1);
      return cursor;
    }
    else
      return null;
  }

  /**
   * Erzeugt einen String in der Form "i.<tab>Abdruck von I.[, II., ...][ und
   * <i-1>]", der passend zu einem Abdruck mit der Verfügungsnummer number
   * angezeigt werden soll.
   * 
   * @param number
   *          Die Nummer des Verfügungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verfügungspunkt bei i==0 und i==1
   *         keinen Vorgänger besitzen kann.
   */
  private static String abdruckString(int number)
  {
    String str = romanNumber(number) + "\t" + "Abdruck von " + romanNumber(1);
    for (int j = 2; j < (number - 1); ++j)
      str += ", " + romanNumber(j);
    if (number >= 3) str += " und " + romanNumber(number - 1);
    return str;
  }

  /**
   * Liefert die römische Zahl zum übgebenen integer Wert i. Die römischen
   * Zahlen werden dabei aus dem begrenzten Array romanNumbers ausgelesen. Ist i
   * kein gültiger Index des Arrays, so sieht der Rückgabewert wie folgt aus "<dezimalzahl(i)>.".
   * Hier kann bei Notwendigkeit natürlich auch ein Berechnungsschema für
   * römische Zahlen implementiert werden, was für die Sachleitenden Verfügungen
   * vermutlich aber nicht erforderlich sein wird.
   * 
   * @param i
   *          Die Zahl, zu der eine römische Zahl geliefert werden soll.
   * @return Die römische Zahl, oder "<dezimalzahl(i)>, wenn i nicht in den
   *         Arraygrenzen von romanNumbers.
   */
  private static String romanNumber(int i)
  {
    String number = "" + i + ".";
    if (i > 0 && i < romanNumbers.length) number = romanNumbers[i - 1];
    return number;
  }

  /**
   * Erzeugt einen Vector mit Elementen vom Typ Verfuegungspunkt, der dem
   * Druckdialog übergeben werden kann und alle für den Druckdialog notwendigen
   * Informationen enthält.
   * 
   * @param doc
   *          Das zu scannende Dokument
   * @return Vector of Verfuegungspunkt, der für jeden Verfuegungspunkt im
   *         Dokument doc einen Eintrag enthält.
   */
  private static Vector scanVerfuegungspunkte(XTextDocument doc)
  {
    Vector verfuegungspunkte = new Vector();

    // Verfügungspunkt1 hinzufügen wenn verfügbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
    {
      Verfuegungspunkt original = new Verfuegungspunkt("I. Original");
      original.addZuleitungszeile("Empfänger siehe Empfängerfeld");
      verfuegungspunkte.add(original);
    }

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verfügungspunkte erstellen. Die
    // Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));

    // Wenn kein Rahmen WollMuxVerfuegungspunkt1 vorhanden ist, wird der erste
    // Verfügungspunkt aus dem Textbereich als Verfügungspunkt1 betrachtet. Für
    // diesen gilt die Sonderregelung, dass die numberOfCopies mit 1 vorbelegt
    // ist. (siehe auch weiter unten)
    boolean first = (punkt1 == null);

    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          String heading = cursor.getString();
          currentVerfpunkt = new Verfuegungspunkt(heading);

          // Originale und alle Verfügungspunkte mit Zuleitung oder welche, die
          // Wiedervorlagen sind werden mit numerOfCopies 1 vorbelegt
          if (first)
          {
            first = false;
            currentVerfpunkt.setNumberOfCopies(1);
          }
          if (isWiedervorlage(heading)) currentVerfpunkt.setNumberOfCopies(1);
          if (isVerfuegungspunktMitZuleitung(cursor))
            currentVerfpunkt.setNumberOfCopies(1);

          verfuegungspunkte.add(currentVerfpunkt);
        }
        else if (currentVerfpunkt != null && isZuleitungszeile(cursor))
        {
          String zuleit = cursor.getString();
          // nicht leere Zuleitungszeilen zum Verfügungspunkt hinzufügen.
          if (!zuleit.equals(""))
            currentVerfpunkt.addZuleitungszeile(cursor.getString());
        }

      } while (cursor.gotoNextParagraph(false));

    return verfuegungspunkte;
  }

  /**
   * Repräsentiert einen vollständigen Verfügungspunkt, der aus Überschrift
   * (römische Ziffer + Überschrift) und Inhalt besteht. Die Klasse bietet
   * Methden an, über die auf alle für den Druck wichtigen Eigenschaften des
   * Verfügungspunktes zugegriffen werden kann (z.B. Überschrift, Anzahl
   * Zuleitungszeilen, ...)
   * 
   * @author christoph.lutz
   */
  public static class Verfuegungspunkt
  {
    /**
     * Enthält den vollständigen Text der erste Zeile des Verfügungspunktes
     * einschließlich der römischen Ziffer.
     */
    protected String heading;

    /**
     * Vector of String, der alle Zuleitungszeilen enthält, die mit addParagraph
     * hinzugefügt wurden.
     */
    protected Vector zuleitungszeilen;

    /**
     * Enthält die Anzahl der Audrucke, die mit jeder hinzugefügten
     * Zuleitungszeile erhöht wird, jedoch zusätzlich noch über
     */
    protected int numberOfCopies;

    /**
     * Erzeugt einen neuen Verfügungspunkt, wobei firstPar der Absatz ist, der
     * die erste Zeile mit der römischen Ziffer und der Überschrift enthält.
     * 
     * @param heading
     *          Text der ersten Zeile des Verfügungspunktes mit der römischen
     *          Ziffer und der Überschrift.
     */
    public Verfuegungspunkt(String heading)
    {
      this.heading = heading;
      this.zuleitungszeilen = new Vector();
      this.numberOfCopies = 0;
    }

    /**
     * Fügt einen weitere Zuleitungszeile des Verfügungspunktes hinzu (wenn
     * paragraph nicht null ist).
     * 
     * @param paragraph
     *          XTextRange, das den gesamten Paragraphen der Zuleitungszeile
     *          enthält.
     */
    public void addZuleitungszeile(String zuleitung)
    {
      zuleitungszeilen.add(zuleitung);
      numberOfCopies++;
    }

    /**
     * Liefert die Anzahl der Ausfertigungen zurück, mit denen der
     * Verfügungspunkt ausgeduckt werden soll; Die Anzahl erhöht sich mit jeder
     * hinzugefügten Zuleitungszeile, kann aber auch manuell mit
     * setNumberOfCopies gesetzt werden.
     * 
     * @return Anzahl der Ausfertigungen mit denen der Verfügungspunkt gedruckt
     *         werden soll.
     */
    public int getNumberOfCopies()
    {
      return numberOfCopies;
    }

    /**
     * Setzt die Anzahl der Ausfertigungen zurück, mit denen der Verfügungspunkt
     * ausgeduckt werden soll auf numberOfCopies. Die Anzahl erhöht sich
     * zusätzlich mit jeder hinzugefügten Zuleitungszeile
     * 
     * @param numberOfCopies
     *          Anzahl der Ausfertigungen mit denen der Verfügungspunkt
     *          ausgedruckt werden soll.
     */
    public void setNumberOfCopies(int numberOfCopies)
    {
      this.numberOfCopies = numberOfCopies;
    }

    /**
     * Liefert einen Vector of Strings, der die Texte der Zuleitungszeilen
     * beinhaltet, die dem Verfügungspunkt mit addParagraph hinzugefügt wurden.
     * 
     * @return Vector of Strings mit den Texten der Zuleitungszeilen.
     */
    public Vector getZuleitungszeilen()
    {
      return zuleitungszeilen;
    }

    /**
     * Liefert den Text der Überschrift des Verfügungspunktes einschließlich der
     * römischen Ziffer als String zurück, wobei mehrfache Leerzeichen (\s+)
     * durch einfache Leerzeichen ersetzt werden.
     * 
     * @return römischer Ziffer + Überschrift
     */
    public String getHeading()
    {
      String text = heading;

      // Tabs und Spaces durch single spaces ersetzen
      text = text.replaceAll("\\s+", " ");

      return text;
    }
  }

  /**
   * Zeigt den Druckdialog für Sachleitende Verfügungen an und druckt das
   * Dokument gemäß Druckdialog in den gewünschten Ausfertigungen aus.
   * 
   * ACHTUNG: Diese Methode läuft nicht im WollMuxEventHandler-Thread, und daher
   * darf nur auf die Daten zugegriffen werden, die pmod anbietet.
   * 
   * @param pmod
   */
  public static void showPrintDialog(XPrintModel pmod)
  {
    Logger.debug("SachleitendeVerfuegung.print - started");

    Vector vps = scanVerfuegungspunkte(pmod.getTextDocument());
    Iterator iter = vps.iterator();
    while (iter.hasNext())
    {
      Verfuegungspunkt vp = (Verfuegungspunkt) iter.next();
      String text = "Verfügungspunkt '" + vp.getHeading() + "'";
      Iterator zuleits = vp.getZuleitungszeilen().iterator();
      while (zuleits.hasNext())
      {
        String zuleit = (String) zuleits.next();
        text += "\n  --> '" + zuleit + "'";
      }
      Logger.debug2(text);
    }

    // Beschreibung des Druckdialogs auslesen.
    ConfigThingy conf = WollMuxSingleton.getInstance().getWollmuxConf();
    ConfigThingy svdds = conf.query("Dialoge").query(
        "SachleitendeVerfuegungenDruckdialog");
    ConfigThingy printDialogConf = null;
    try
    {
      printDialogConf = svdds.getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      Logger
          .error(
              "Fehlende Dialogbeschreibung für den Dialog 'SachleitendeVerfuegungenDruckdialog'.",
              e);
      return;
    }

    // Druckdialog starten
    try
    {
      new SachleitendeVerfuegungenDruckdialog(printDialogConf, vps, pmod, null);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      return;
    }

    // pmod.print((short)1);
    Logger.debug("SachleitendeVerfuegung.print - finished");
  }

  /**
   * Druckt den Verfügungpunkt verfPunkt aus dem Dokument doc in der gewünschten
   * Anzahl numberOfCopies aus. ACHTUNG: Diese Methode darf nur aus dem
   * WollMuxEventHandler-Thread gestartet werden, da sie auf Datenstrukturen des
   * WollMux zugreift.
   * 
   * @param model
   *          Das TextDocumentModel, dessen Inhalt Sachleitende Verfügungen
   *          enthält und ausgedruckt werden soll.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verfügungspunktes, wobei alle
   *          folgenden Verfügungspunkte ausgeblendet werden.
   * @param numberOfCopies
   *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt werden
   *          soll.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Blöcke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verfügungspunktes I
   *          ausgeblendet und alle notInOriginal-Blöcke ebenso. Andernfalls
   *          sind Ziffer und notInOriginal-Blöcke eingeblendet.
   * @throws PrintFailedException
   */
  public static void printVerfuegungspunkt(TextDocumentModel model,
      short verfPunkt, short numberOfCopies, boolean isDraft, boolean isOriginal)
      throws PrintFailedException
  {
    // Kontrollkästchen Ausgeblendeter Text (anzeigen) unter
    // "Extras->Optionen->OOoWriter->Formatierungshilfen" deaktivieren, damit
    // die Berechnung der Gesamtseitenzahl in jedem Fall richtig funktioniert.
    // FIXME: updateAccess von hiddenCharacter geht noch nicht und wird erst
    // nach einem Neustart aktiv -> Community fragen
    XChangesBatch updateAccess = UNO
        .getConfigurationUpdateAccess("/org.openoffice.Office.Writer/Content/NonprintingCharacter");
    Object oldhiddenCharacter = setHiddenCharacter(updateAccess, Boolean.FALSE);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);
    int count = 0;
    if (punkt1 != null) count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
    XTextRange lastVisibleVerfPunkt = null;
    XParagraphCursor cursor = UNO.XParagraphCursor(model.doc.getText()
        .createTextCursorByRange(model.doc.getText().getStart()));
    if (cursor != null)
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          // Punkt1 merken
          if (punkt1 == null)
            punkt1 = cursor.getText().createTextCursorByRange(cursor);

          count++;

          // range des zuletzt sichtbaren Verfügungspunktes sichern
          if (count == verfPunkt)
            lastVisibleVerfPunkt = cursor.getText().createTextCursorByRange(
                cursor);

          if (count == (verfPunkt + 1))
          {
            cursor.collapseToStart();
            cursor.gotoRange(cursor.getText().getEnd(), true);
            setInvisibleRange = cursor;
          }
        }
      } while (setInvisibleRange == null && cursor.gotoNextParagraph(false));

    // ensprechende Verfügungspunkte ausblenden
    if (setInvisibleRange != null)
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.TRUE);

    // Sichtbarkeitsstand der all, draftOnly bzw. notInOriginal-Blöcke und
    // merken.
    HashMap /* of DocumentCommand */oldVisibilityStates = new HashMap();

    // Ein/Ausblenden der draftOnly bzw. notInOriginal-Blöcke:
    Iterator iter = model.getDraftOnlyBlocksIterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(isDraft);
    }

    iter = model.getNotInOrininalBlocksIterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(!isOriginal);
    }

    iter = model.getAllVersionsBlocksIterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(true);
    }

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1, true);
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.TRUE);
    }

    // Seitumbruch einbauen, damit der Text des letzten Verfügungspunktes
    // niemals zwischen verschiedenen Seiten auseinander gerissen wird.
    XTextCursor viewCursor = model.getViewCursor();
    XTextCursor oldViewCursor = null;
    try
    {
      // Dieser Aufruf fliegt auf die Schnauze, wenn der Cursor nicht im Text
      // steht, sondern z.b. ein Rahmen markiert ist. Dann kann der viewCursor
      // so nicht verwendet werden.
      oldViewCursor = viewCursor.getText().createTextCursorByRange(viewCursor);
    }
    catch (java.lang.Exception e)
    {
    }

    boolean pageBreakAtlastVisibleVerfPoint = false;

    if (lastVisibleVerfPunkt != null
        && viewCursor != null
        && oldViewCursor != null)
    {
      // (Position) Seitennummer des Textendes holen
      viewCursor.gotoRange(model.doc.getText().getEnd(), false);
      int pageOfLastLine = getPageOfViewCursor(viewCursor);

      // (Position) Seitennummer des letzten sichtbaren Verfügungspunktes holen
      viewCursor.gotoRange(lastVisibleVerfPunkt, false);
      int pageOfLastVisibleVerfPunkt = getPageOfViewCursor(viewCursor);

      // viewCursor wieder auf alte Position zurücksetzen:
      viewCursor.gotoRange(oldViewCursor, false);

      // Seitenumbruch einfügen, wenn die Seitenzahlen der beiden Positionen
      // sich unterscheiden.
      if (pageOfLastLine != pageOfLastVisibleVerfPunkt)
      {
        Object pageDescName = UNO.getProperty(
            lastVisibleVerfPunkt,
            "PageDescName");
        if (pageDescName != null && !pageDescName.toString().equals(""))
        {
          Object pageStyleName = UNO.getProperty(
              lastVisibleVerfPunkt,
              "PageStyleName");
          // ist diese Property gesetzt, so wird der Seitenumbruch eingefügt:
          UNO.setProperty(lastVisibleVerfPunkt, "PageDescName", pageStyleName);
          UNO.setProperty(lastVisibleVerfPunkt, "PageNumberOffset", new Short(
              (short) (pageOfLastVisibleVerfPunkt + 1)));
          pageBreakAtlastVisibleVerfPoint = true;
        }
      }
    }

    // -----------------------------------------------------------------------
    // Druck des Dokuments
    // -----------------------------------------------------------------------
    model.print(numberOfCopies);

    // zuvor eingefügten Seitenumbruch wieder entfernen:
    if (pageBreakAtlastVisibleVerfPoint && lastVisibleVerfPunkt != null)
    {
      UNO.setProperty(lastVisibleVerfPunkt, "PageDescName", "");
    }

    // Ausblendung von Ziffer von Punkt 1 wieder aufheben
    if (punkt1ZifferOnly != null)
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.FALSE);

    // Alte Sichtbarkeitszustände der draftOnly bzw. notInOriginal-Blöcke
    // zurücksetzten.
    iter = oldVisibilityStates.keySet().iterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      cmd.setVisible(((Boolean) oldVisibilityStates.get(cmd)).booleanValue());
    }

    // Verfügungspunkte wieder einblenden:
    if (setInvisibleRange != null)
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.FALSE);

    // Kontrollkästchen Ausgeblendeter Text (anzeigen) unter
    // "Extras->Optionen->OOoWriter->Formatierungshilfen" wieder auf den alten
    // Wert zurücksetzen:
    setHiddenCharacter(updateAccess, oldhiddenCharacter);
    if (UNO.XComponent(updateAccess) != null)
      UNO.XComponent(updateAccess).dispose();
  }

  /**
   * Liefert die Seite (>0) in der sich der ViewCursor viewCursor befindet oder
   * 0, falls der ViewCursor diese Information nicht bereitstellen kann.
   * 
   * @param viewCursor
   * @return
   */
  private static int getPageOfViewCursor(XTextCursor viewCursor)
  {
    if (UNO.XPageCursor(viewCursor) == null) return 0;
    return UNO.XPageCursor(viewCursor).getPage();
  }

  /**
   * Setzt die Property HiddenCharacter des übergebenen UNO-Objekts
   * updateAccess, committed die Änderung an OOo und gibt den Wert zurück, den
   * die Property vor dem setzen besaß.
   * 
   * @param updateAccess
   *          das updateAcess-Objekt, auf dem die Operation durchgeführt werden
   *          soll.
   * @param newValue
   *          der neu zu setzende Wert.
   * @return der Wert, den die Property VORHER hatte.
   */
  private static Object setHiddenCharacter(XChangesBatch updateAccess,
      Object newValue)
  {
    Object oldValue = UNO.getProperty(updateAccess, "HiddenCharacter");
    UNO.setProperty(updateAccess, "HiddenCharacter", newValue);
    if (updateAccess != null) try
    {
      updateAccess.commitChanges();
    }
    catch (WrappedTargetException e)
    {
      Logger.error(e);
    }
    return oldValue;
  }

  /**
   * Schaltet die Option AutoNumbering unter
   * Extras->AutoKorrektur...->Nummerierung anwenden aus, da diese im Umgang mit
   * Sachleitenden Verfügungen nicht einsetzbar ist. OpenOffice interpretiert
   * sonst jeden Verfügungspunkt als Beginn einer Nummerierung.
   */
  private static void switchOffAutoNumbering()
  {
    // FIXME: switchOffAutoNumbering: auch das wird erst nach einem Neustart
    // aktiv. Warum?
    if (firstTime)
    {
      firstTime = false;
      XChangesBatch updateAccess = UNO
          .getConfigurationUpdateAccess("/org.openoffice.Office.Writer/AutoFunction/Format/ByInput/ApplyNumbering");
      UNO.setProperty(updateAccess, "Enable", Boolean.FALSE);
      if (updateAccess != null) try
      {
        updateAccess.commitChanges();
      }
      catch (WrappedTargetException e)
      {
        Logger.error(e);
      }
      if (UNO.XComponent(updateAccess) != null)
        UNO.XComponent(updateAccess).dispose();
    }
  }

  /**
   * Liefert das Absatzformat (=ParagraphStyle) des Dokuments doc mit dem Namen
   * name oder null, falls das Absatzformat nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Absatzformat name gesucht werden
   *          soll.
   * @param name
   *          der Name des gesuchten Absatzformates
   * @return das Absatzformat des Dokuments doc mit dem Namen name oder null,
   *         falls das Absatzformat nicht definiert ist.
   */
  private static XStyle getParagraphStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    if (pss != null) try
    {
      style = UNO.XStyle(pss.getByName(name));
    }
    catch (java.lang.Exception e)
    {
    }
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Absatzformat (=ParagraphStyle) mit dem
   * Namen name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Absatzformat zurück oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Absatzformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Absatzformates
   * @param parentStyleName
   *          Name des Vorgänger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorgänger gesetzt
   *          werden soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Absatzformat oder null, falls das Absatzformat
   *         nicht erzeugt werden konnte.
   */
  private static XStyle createParagraphStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    XStyle style = null;
    try
    {
      style = UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.ParagraphStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {
    }
    return null;
  }

  /**
   * Liefert das Zeichenformat (=CharacterStyle) des Dokuments doc mit dem Namen
   * name oder null, falls das Format nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Zeichenformat name gesucht werden
   *          soll.
   * @param name
   *          der Name des gesuchten Zeichenformates
   * @return das Zeichenformat des Dokuments doc mit dem Namen name oder null,
   *         falls das Absatzformat nicht definiert ist.
   */
  private static XStyle getCharacterStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    if (pss != null) try
    {
      style = UNO.XStyle(pss.getByName(name));
    }
    catch (java.lang.Exception e)
    {
    }
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Zeichenformat (=CharacterStyle) mit dem
   * Namen name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Zeichenformat zurück oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Zeichenformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Zeichenformates
   * @param parentStyleName
   *          Name des Vorgänger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorgänger gesetzt
   *          werden soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Zeichenformat oder null, falls das Zeichenformat
   *         nicht erzeugt werden konnte.
   */
  private static XStyle createCharacterStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    XStyle style = null;
    try
    {
      style = UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.CharacterStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {
    }
    return null;
  }

  /**
   * Liefert den Styles vom Typ type des Dokuments doc.
   * 
   * @param doc
   *          Das Dokument, dessen StyleContainer zurückgeliefert werden soll.
   * @param type
   *          kann z.B. CHARACTER_STYLE oder PARAGRAPH_STYLE sein.
   * @return Liefert den Container der Styles vom Typ type des Dokuments doc
   *         oder null, falls der Container nicht bestimmt werden konnte.
   */
  private static XNameContainer getStyleContainer(XTextDocument doc,
      String containerName)
  {
    try
    {
      return UNO.XNameContainer(UNO.XNameAccess(
          UNO.XStyleFamiliesSupplier(doc).getStyleFamilies()).getByName(
          containerName));
    }
    catch (java.lang.Exception e)
    {
    }
    return null;
  }

  /**
   * Falls die für Sachleitenden Verfügungen notwendigen Absatz- und
   * Zeichenformate nicht bereits existieren, werden sie hier erzeugt und mit
   * fest verdrahteten Werten vorbelegt.
   * 
   * @param doc
   */
  private static void createUsedStyles(XTextDocument doc)
  {
    XStyle style = null;

    // Absatzformate:

    style = getParagraphStyle(doc, ParaStyleNameDefault);
    if (style == null)
    {
      style = createParagraphStyle(doc, ParaStyleNameDefault, null);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharHeight", new Integer(11));
      UNO.setProperty(style, "CharFontName", "Arial");
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunkt,
          ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(-700));
      UNO.setProperty(style, "ParaTopMargin", new Integer(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt1);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunkt1,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(0));
      UNO.setProperty(style, "ParaTopMargin", new Integer(0));
    }

    style = getParagraphStyle(doc, ParaStyleNameAbdruck);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameAbdruck,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Integer(100));
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(-700));
      UNO.setProperty(style, "ParaTopMargin", new Integer(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameZuleitungszeile);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameZuleitungszeile,
          ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", new Integer(1));
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunktMitZuleitung);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunktMitZuleitung,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", new Integer(1));
    }

    // Zeichenformate:

    style = getCharacterStyle(doc, CharStyleNameDefault);
    if (style == null)
    {
      style = createCharacterStyle(doc, CharStyleNameDefault, null);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharHeight", new Integer(11));
      UNO.setProperty(style, "CharFontName", "Arial");
      UNO.setProperty(style, "CharUnderline", new Integer(0));
    }

    style = getCharacterStyle(doc, CharStyleNameRoemischeZiffer);
    if (style == null)
    {
      style = createCharacterStyle(
          doc,
          CharStyleNameRoemischeZiffer,
          CharStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
    }
  }
}
