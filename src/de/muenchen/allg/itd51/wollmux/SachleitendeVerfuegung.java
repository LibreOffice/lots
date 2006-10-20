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
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog;

public class SachleitendeVerfuegung
{
  private static final String ParaStyleNameVerfuegungspunkt = "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameAbdruck = "WollMuxVerfuegungspunktAbdruck";

  private static final String ParaStyleNameZuleitungszeile = "WollMuxZuleitungszeile";

  private static final String ParaStyleNameDefault = "Fließtext";

  private static final String FrameNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String AbdruckDefaultStr = "Abdruck von <Vorgänger>.";

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
   */
  public static void verfuegungspunktEinfuegen(XTextDocument doc,
      XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enthält der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gelöscht
    boolean deletedAtLeastOne = alleVerfuegungspunkteLoeschen(cursor);

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
   */
  public static void abdruck(XTextDocument doc, XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enthält der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gelöscht
    boolean deletedAtLeastOne = alleVerfuegungspunkteLoeschen(cursor);

    if (!deletedAtLeastOne)
    {
      // Abdruck einfügen, wenn kein Verfügungspunkt gelöscht wurde.
      cursor.collapseToEnd();
      cursor.gotoEndOfParagraph(false);
      cursor.setString("\r" + AbdruckDefaultStr);
      cursor.gotoNextParagraph(false);
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameAbdruck);
      cursor.gotoEndOfParagraph(false);
      cursor.setString("\r");
    }

    // Ziffern anpassen:
    ziffernAnpassen(doc);
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
   */
  public static void zuleitungszeile(XTextDocument doc, XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    boolean deletedAtLeastOne = alleZuleitungszeilenLoeschen(cursor);

    if (!deletedAtLeastOne)
    {
      // Sind im markierten Bereich Verfügungspunkte enthalten, so werden diese
      // zurückgesetzt und neu numeriert, damit die entsprechenden Absätze
      // anschließend zu Zuleitungszeilen gemacht werden können.
      if (alleVerfuegungspunkteLoeschen(cursor)) ziffernAnpassen(doc);

      // Absatzformat für Zuleitungszeilen setzen (auf den gesamten Bereich)
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameZuleitungszeile);

      // Nach dem Setzen einer Zuleitungszeile, soll der Cursor auf dem Ende der
      // Markierung stehen, damit man direkt von dort weiter schreiben kann.
      try
      {
        UNO.XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController())
            .getViewCursor().gotoRange(cursor.getEnd(), false);
      }
      catch (Exception e)
      {
      }
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
    String text = paragraph.getString();
    return text.matches(".*Abdruck von (I|<Vorgänger>)\\..*");
  }

  /**
   * Diese Methode löscht alle Verfügungspunkte, die der bereich des Cursors
   * cursor berührt, und liefert true zurück, wenn mindestens ein
   * Verfügungspunkt gelöscht wurde oder false, wenn sich in dem Bereich des
   * Cursors kein Verfügungspunkt befand.
   * 
   * @param doc
   *          Das Dokument, in dem der Verfügungspunkt eingefügt werden soll
   *          (wird für die Ziffernanpassung benötigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verfügungspunkt gelöscht wurde oder
   *         false, wenn kein der cursor keinen Verfügungspunkt berührt.
   */
  private static boolean alleVerfuegungspunkteLoeschen(XParagraphCursor cursor)
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

        if (par != null && isVerfuegungspunkt(par))
        {
          // Einen evtl. bestehenden Verfuegungspunkt zurücksetzen
          verfuegungspunktLoeschen(par);
          deletedAtLeastOne = true;
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode löscht alle Zuleitungszeilen, die der bereich des Cursors
   * cursor berührt, und liefert true zurück, wenn mindestens eine
   * Zuleitungszeile gelöscht wurde oder false, wenn sich in dem Bereich des
   * Cursors keine Zuleitungszeile befand.
   * 
   * @param doc
   *          Das Dokument, in dem der Verfügungspunkt eingefügt werden soll
   *          (wird für die Ziffernanpassung benötigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verfügungspunkt gelöscht wurde oder
   *         false, wenn kein der cursor keinen Verfügungspunkt berührt.
   */
  private static boolean alleZuleitungszeilenLoeschen(XParagraphCursor cursor)
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

        if (par != null && isZuleitungszeile(par))
        {
          // Zuleitungszeile zurücksetzen
          UNO.setProperty(par, "ParaStyleName", ParaStyleNameDefault);

          deletedAtLeastOne = true;
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

    // wenn es sich bei dem Paragraphen um einen Abdruck handelt, wird dieser
    // vollständig gelöscht.
    if (isAbdruck(par))
    {
      // löscht den String "Abdruck von..."
      par.setString("");

      // löscht das Returnzeichen ("\r") zum nächsten Absatz
      XParagraphCursor parDeleter = UNO.XParagraphCursor(par.getText()
          .createTextCursorByRange(par.getEnd()));
      if (parDeleter.goRight((short) 1, false))
      {
        parDeleter.goLeft((short) 1, true);
        parDeleter.setString("");

        // wenn die auf die ehemalige Abdruckzeile folgende Zeile auch noch leer
        // ist, so wird diese Zeile auch noch gelöscht.
        if (parDeleter.isEndOfParagraph())
        {
          parDeleter.goLeft((short) 1, true);
          parDeleter.setString("");
        }
      }
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
  private static void ziffernAnpassen(XTextDocument doc)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(doc);

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
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));
    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          count++;

          // String mit römischer Zahl erzeugen
          String number = romanNumber(count);

          // Enthält der Paragraph einen "Abdruck"-String, so wird dieser neu
          // gesetzt:
          if (isAbdruck(cursor))
          {
            cursor.setString(abdruckString(count));
          }

          XTextRange zifferOnly = getZifferOnly(cursor);
          if (zifferOnly != null)
          {
            // Nummer aktualisieren wenn sie nicht mehr stimmt.
            if (!zifferOnly.getString().equals(number))
              zifferOnly.setString(number);
          }
          else
          {
            // zuerst den Tab einfügen, damit dieses in der Standardformatierung
            // des Absatzes erhalten bleibt.
            cursor.getStart().setString("\t"); // Rechtsverschiebung des
            // Cursors
            cursor.gotoStartOfParagraph(true); // Korrektur der Verschiebung

            // neue Nummer erzeugen mit Formatierung "fett". Die Formatierung
            // darf sich nur auf die Nummer auswirken und nicht auch noch auf
            // das darauffolgende "\t"-Zeichen
            zifferOnly = cursor.getText().createTextCursorByRange(
                cursor.getStart());
            UNO.setProperty(
                zifferOnly,
                "CharWeight",
                new Float(FontWeight.BOLD));
            zifferOnly.setString(number);
          }
        }
      } while (cursor.gotoNextParagraph(false));

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

    // Setzte die Druckfunktion SachleitendeVerfuegung wenn mindestens manuell
    // eingefügter Verfügungspunkt vorhanden ist. Ansonsten setze die
    // Druckfunktion zurück.
    int effectiveCount = (punkt1 != null) ? count - 1 : count;
    if (effectiveCount == 0)
      WollMuxEventHandler.handleSetPrintFunction(doc, "");
    else
      WollMuxEventHandler.handleSetPrintFunction(doc, "SachleitendeVerfuegung");
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
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt1);
      return cursor;
    }
    else
      return null;
  }

  /**
   * Erzeugt einen String in der Form "Abdruck von I.[, II., ...][ und <i-1>]",
   * der passend zu einem Abdruck mit der Verfügungsnummer i angezeigt werden
   * soll.
   * 
   * @param i
   *          Die Nummer des Verfügungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verfügungspunkt bei i==0 und i==1
   *         keinen Vorgänger besitzen kann.
   */
  private static String abdruckString(int i)
  {
    if (i < 2) return AbdruckDefaultStr;

    String str = "Abdruck von " + romanNumber(1);
    for (int j = 2; j < (i - 1); ++j)
      str += ", " + romanNumber(j);
    if (i >= 3) str += " und " + romanNumber(i - 1);
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

  private static Vector scanVerfuegungspunkte(XTextDocument doc)
  {
    Vector verfuegungspunkte = new Vector();

    // Verfügungspunkt1 hinzufügen wenn verfügbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
      verfuegungspunkte.add(new VerfuegungspunktOriginal(punkt1));

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verfügungspunkte erstellen. Die
    // Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));

    if (cursor != null) do
    {
      // ganzen Paragraphen markieren
      cursor.gotoEndOfParagraph(true);

      if (isVerfuegungspunkt(cursor))
      {
        currentVerfpunkt = new Verfuegungspunkt(cursor);
        verfuegungspunkte.add(currentVerfpunkt);
      }
      else if (currentVerfpunkt != null)
      {
        currentVerfpunkt.addParagraph(cursor);
      }

    } while (cursor.gotoNextParagraph(false));

    return verfuegungspunkte;
  }

  /**
   * TODO: Verfuegungspunkt refaktorisieren, so dass keine TextRanges mehr
   * erforderlich sind - Verfuegungspunkt wird jetzt lediglich als Datenspeicher
   * für den Druckdialog benötigt, der keine Zugriff auf die TextRanges
   * benötigt.
   * 
   * Repräsentiert einen vollständigen Verfügungspunkt, der aus Überschrift
   * (römische Ziffer + Überschrift) und Inhalt besteht. Die Klasse bietet
   * Methden an, über die auf alle für den Druck wichtigen Eigenschaften des
   * Verfügungspunktes zugegriffen werden kann (z.B. Überschrift, Anzahl
   * Zuleitungszeilen, ...)
   * 
   * @author christoph.lutz
   * 
   */
  public static class Verfuegungspunkt
  {
    /**
     * Vector mit den XTextRanges aller Paragraphen des Verfügungspunktes.
     */
    protected Vector /* of XTextRange */paragraphs;

    /**
     * Vector of String, der alle Zuleitungszeilen enthält, die mit addParagraph
     * hinzugefügt wurden.
     */
    protected Vector zuleitungszeilen;

    /**
     * Erzeugt einen neuen Verfügungspunkt, wobei firstPar der Absatz ist, der
     * die erste Zeile mit der römischen Ziffer und der Überschrift enthält.
     * 
     * @param firstPar
     *          Die erste Zeile des Verfügungspunktes mit der römischen Ziffer
     *          und der Überschrift.
     * @throws java.lang.NullPointerException
     *           wenn paragraph null ist
     */
    public Verfuegungspunkt(XTextRange firstPar)
    {
      if (firstPar == null)
        throw new NullPointerException("XTextRange firstPar ist null");

      this.paragraphs = new Vector();
      this.zuleitungszeilen = new Vector();

      paragraphs.add(firstPar.getText().createTextCursorByRange(firstPar));
    }

    /**
     * Fügt einen weiteren Paragraphen des Verfügungspunktes hinzu (wenn
     * paragraph nicht null ist).
     * 
     * @param paragraph
     *          XTextRange, das den gesamten Paragraphen enthält.
     */
    public void addParagraph(XTextRange paragraph)
    {
      if (paragraph == null) return;
      XTextCursor par = paragraph.getText().createTextCursorByRange(paragraph);

      paragraphs.add(par);

      if (isZuleitungszeile(par)) zuleitungszeilen.add(par.getString());
    }

    /**
     * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen
     * als Zuleitungszeile markierten Absatz handelt.
     * 
     * @param paragraph
     *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
     *          herangezogen wird.
     * @return true, wenn der Name des Absatzformates mit
     *         "WollMuxZuleitungszeile" beginnt.
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
     * Liefert den gesamten Bereich über den sich der Verfügungspunkt erstreckt,
     * angefangen vom Startpunkt der ersten Zeile des Verfügungspunktes bis hin
     * zum Ende des letzten enthaltenen Paragraphen.
     * 
     * @return der gesamte Bereich über den sich der Verfügungspunkt erstreckt.
     */
    public XTextRange getCompleteRange()
    {
      XTextRange firstPar = UNO.XTextRange(paragraphs.get(0));
      XTextRange lastPar = UNO
          .XTextRange(paragraphs.get(paragraphs.size() - 1));

      XTextCursor cursor = firstPar.getText().createTextCursor();
      cursor.gotoRange(firstPar.getStart(), false);
      cursor.gotoRange(lastPar.getEnd(), true);
      return cursor;
    }

    /**
     * Liefert die Anzahl der Zuleitungszeilen zurück, die dem Verfügungspunkt
     * mit addParagraph hinzugefügt wurden.
     * 
     * @return Anzahl der Zuleitungszeilen dieses Verfügungspunktes.
     */
    public int getZuleitungszeilenCount()
    {
      return zuleitungszeilen.size();
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
     * römischen Ziffer als String zurück.
     * 
     * @return römischer Ziffer + Überschrift
     */
    public String getHeading()
    {
      String text = "";
      XTextRange firstPar = UNO.XTextRange(paragraphs.get(0));
      if (firstPar != null) text = firstPar.getString();

      // Tabs und Spaces durch single spaces ersetzen
      text = text.replaceAll("\\s+", " ");

      return text;
    }
  }

  /**
   * Enthält die Besonderheiten des ersten Verfügungspunktes eines externen
   * Briefkopfes wie z.B. die Darstellung der Überschrift als "I. Original".
   * 
   * @author christoph.lutz
   * 
   */
  public static class VerfuegungspunktOriginal extends Verfuegungspunkt
  {
    public VerfuegungspunktOriginal(XTextRange punkt1)
    {
      super(punkt1);

      if (punkt1 == null)
        throw new NullPointerException("XTextRange punkt1 ist null");

      zuleitungszeilen.add("Empfänger siehe Empfängerfeld");
    }

    public String getHeading()
    {
      return "I. Original";
    }

    public void addParagraph(XTextRange paragraph)
    {
      // addParagraph ergibt bei Verfuegungspunkt1 keinen Sinn und wird daher
      // disabled.
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
      new SachleitendeVerfuegungenDruckdialog(printDialogConf, vps, null);
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
   * @param doc
   * @param verfPunkt
   * @param numberOfCopies
   */
  public static void printVerfuegungspunkt(TextDocumentModel model,
      short verfPunkt, short numberOfCopies, boolean isDraft)
  {
    boolean isOriginal = (verfPunkt == 1);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);
    int count = 0;
    if (punkt1 != null) count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
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

    // Sichtbarkeitsstand der draftOnly bzw. notInOriginal-Blöcke merken.
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

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1);
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.TRUE);
    }

    // Druck des Dokuments mit den entsprechenden Ein/Ausbledungen
    model.print(numberOfCopies);

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
  }

  public static void main(String[] args) throws Exception
  {
    UNO.init();

    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());

    if (doc == null)
    {
      System.err.println("Keine Textdokument");
      return;
    }

  }
}
