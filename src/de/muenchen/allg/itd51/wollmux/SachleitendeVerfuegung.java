/*
 * Dateiname: SachleitendeVerfuegung.java
 * Projekt  : WollMux
 * Funktion : Hilfen für Sachleitende Verfügungen.
 * 
 * Copyright (c) 2009-2018 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 26.09.2006 | LUT | Erstellung als SachleitendeVerfuegung
 * 31.07.2009 | BED | +"copyOnly"
 * 04.05.2011 | LUT | Ziffernanzeige und String "Abdruck" konfigurierbar
 *                    Patch von Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de)
 * 09.09.2014 | JGM | Update der Dokumentenstruktur vorm Drucken eingefuegt.
 *                    Behebt Bug#12079 "SLV Druckbloecke in Bereichen"
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class SachleitendeVerfuegung
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SachleitendeVerfuegung.class);

  public static final String BLOCKNAME_SLV_ALL_VERSIONS = "AllVersions";

  public static final String BLOCKNAME_SLV_ORIGINAL_ONLY = "OriginalOnly";

  public static final String BLOCKNAME_SLV_NOT_IN_ORIGINAL = "NotInOriginal";

  public static final String BLOCKNAME_SLV_DRAFT_ONLY = "DraftOnly";

  public static final String BLOCKNAME_SLV_COPY_ONLY = "CopyOnly";

  public static final String GROUP_ID_SLV_ALL_VERSIONS =
    "SLV_" + BLOCKNAME_SLV_ALL_VERSIONS;

  public static final String GROUP_ID_SLV_ORIGINAL_ONLY =
    "SLV_" + BLOCKNAME_SLV_ORIGINAL_ONLY;

  public static final String GROUP_ID_SLV_NOT_IN_ORIGINAL =
    "SLV_" + BLOCKNAME_SLV_NOT_IN_ORIGINAL;

  public static final String GROUP_ID_SLV_DRAFT_ONLY =
    "SLV_" + BLOCKNAME_SLV_DRAFT_ONLY;

  public static final String GROUP_ID_SLV_COPY_ONLY =
    "SLV_" + BLOCKNAME_SLV_COPY_ONLY;

  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  private static final String CHARACTER_STYLES = "CharacterStyles";

  private static final String PARAGRAPH_STYLES = "ParagraphStyles";

  private static final String ParaStyleNameVerfuegungspunkt =
    "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 =
    "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameAbdruck =
    "WollMuxVerfuegungspunktAbdruck";

  private static final String ParaStyleNameVerfuegungspunktMitZuleitung =
    "WollMuxVerfuegungspunktMitZuleitung";

  private static final String ParaStyleNameZuleitungszeile =
    "WollMuxZuleitungszeile";

  private static final String ParaStyleNameDefault = "Fließtext";

  private static final String CharStyleNameDefault = "Fließtext";

  private static final String CharStyleNameRoemischeZiffer =
    "WollMuxRoemischeZiffer";

  private static final String FrameNameVerfuegungspunkt1 =
    "WollMuxVerfuegungspunkt1";

  /**
   * Erkennt mindestens eine römische oder eine arabische Ziffer gefolgt von einem
   * "." (auch im römischen Modus werden die Ziffern über 15 arabisch dargestellt).
   */
  private static final String zifferPattern = "^([XIV]+|\\d+)\\.\t";

  /**
   * Enthält einen Vector mit den ersten 15 Ziffern (gemäß der Konfig-Einstellung
   * SachleitendeVerfuegungen/NUMBERS). Mehr wird in Sachleitenden Verfügungen
   * sicherlich nicht benötigt :-). Höhere Ziffern sind automatisch arabische
   * Ziffern.
   */
  private static final String[] romanNumbers = getNumbers();

  /**
   * Enthält den String "Abdruck" oder die per SachleitendeVerfuegungen/ABDRUCK_NAME
   * konfigurierte Alternative.
   */
  private static final String copyName = getCopyName();

  /**
   * Setzt das Absatzformat des Absatzes, der range berührt, auf
   * "WollMuxVerfuegungspunkt" ODER setzt alle in range enthaltenen Verfügungspunkte
   * auf Fließtext zurück, wenn range einen oder mehrere Verfügungspunkte berührt.
   * 
   * @param range
   *          Die XTextRange, in der sich zum Zeitpunkt des Aufrufs der Cursor
   *          befindet.
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll oder
   *         null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertVerfuegungspunkt(TextDocumentController documentController,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(documentController.getModel().doc);

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));

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
    ziffernAnpassen(documentController);

    return null;
  }

  /**
   * Erzeugt am Ende des Paragraphen, der von range berührt wird, einen neuen
   * Paragraphen, setzt diesen auf das Absatzformat WollMuxVerfuegungspunktAbdruck
   * und belegt ihn mit dem String "Abdruck von <Vorgänger>" ODER löscht alle
   * Verfügungspunkte die der range berührt, wenn in ihm mindestens ein bereits
   * bestehender Verfügungspunkt enthalten ist.
   * 
   * @param doc
   *          Das Dokument, in dem der Verfügungspunkt eingefügt werden soll (wird
   *          für die Ziffernanpassung benötigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll oder
   *         null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertAbdruck(TextDocumentController documentController, XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(documentController.getModel().doc);

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));

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

      int count = countVerfPunkteBefore(documentController.getModel().doc, cursor) + 1;
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
    ziffernAnpassen(documentController);

    return cursor;
  }

  /**
   * Formatiert alle Paragraphen die der TextRange range berührt mit dem Absatzformat
   * WollMuxZuleitungszeile und markiert diese Zeilen damit auch semantisch als
   * Zuleitungszeilen ODER setzt das Absatzformat der ensprechenden Paragraphen
   * wieder auf Fließtext zurück, wenn mindestens ein Paragraph bereits eine
   * Zuleitungszeile ist.
   * 
   * @param doc
   *          Das Dokument in dem die sich range befindet.
   * @param range
   * @return die Position zurück, auf die der ViewCursor gesetzt werden soll oder
   *         null, falls der ViewCursor unverändert bleibt.
   */
  public static XTextRange insertZuleitungszeile(TextDocumentController documentController,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(documentController.getModel().doc);

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));
    XTextCursor createdZuleitung = null;

    boolean deletedAtLeastOne = removeAllZuleitungszeilen(cursor);

    if (!deletedAtLeastOne && UNO.XEnumerationAccess(cursor) != null)
    {
      // Im cursor enthaltene Paragraphen einzeln iterieren und je nach Typ
      // entweder eine Zuleitungszeile oder einen Verfügungspunkt mit Zuleitung
      // setzen.
      XEnumeration paragraphs = UNO.XEnumerationAccess(cursor).createEnumeration();
      while (paragraphs.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(paragraphs.nextElement());
        }
        catch (java.lang.Exception e)
        {}

        if (par != null)
        {
          if (isAbdruck(par))
          {
            if (cursor.isCollapsed()) // Ignorieren, wenn Bereich ausgewählt.
            {
              // Zuleitung in neuer Zeile erzeugen:
              par.getEnd().setString("\r");
              createdZuleitung = par.getText().createTextCursorByRange(par.getEnd());
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
   * Diese Methode löscht alle Verfügungspunkte, die der Bereich des Cursors cursor
   * berührt, und liefert true zurück, wenn mindestens ein Verfügungspunkt gelöscht
   * wurde oder false, wenn sich in dem Bereich des Cursors kein Verfügungspunkt
   * befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verfügungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verfügungspunkt gelöscht wurde oder false,
   *         wenn kein der cursor keinen Verfügungspunkt berührt.
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
          LOGGER.error("", e);
        }

        if (par != null)
        {
          boolean isVerfuegungspunktMitZuleitung =
            isVerfuegungspunktMitZuleitung(par);
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
   * Diese Methode löscht alle Abdruck-Zeilen, die der Bereich des Cursors cursor
   * berührt, und liefert true zurück, wenn mindestens ein Abdruck gelöscht wurde
   * oder false, wenn sich in dem Bereich des Cursors kein Abdruck befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Abdrücken gesucht wird.
   * 
   * @return true, wenn mindestens ein Abdruck gelöscht wurde oder false, wenn kein
   *         der cursor keinen Verfügungspunkt berührt.
   */
  private static boolean removeAllAbdruecke(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    String fullText = "";
    fullText = getFullLinesOfSelectedAbdruckLines(cursor);
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
          LOGGER.error("", e);
        }

        String str = getStringOfXTextRange(par);

        if (par != null)
        {
          if (isAbdruck(par) && fullText.contains(str))
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
   * Liefert die vollständingen Textzeilen der markierten Abdruckzeilen zurück
   *
   * @param textRange
   *
   * @return String
   */
  static String getFullLinesOfSelectedAbdruckLines(XParagraphCursor cursor)
  {
    String fullText = "";
    if (UNO.XEnumerationAccess(cursor) != null)
    {
      XEnumeration enumerationAccessFullText = UNO.XEnumerationAccess(cursor)
          .createEnumeration();
      while (enumerationAccessFullText.hasMoreElements())
      {
        XTextRange test;
        try
        {
          test = UNO.XTextRange(enumerationAccessFullText.nextElement());
          fullText += getStringOfXTextRange(test);
        } catch (NoSuchElementException e)
        {
          LOGGER.error("", e);
        } catch (WrappedTargetException e)
        {
          LOGGER.error("", e);
        }
      }
    }
    return fullText;
  }

  /**
   * Diese Methode löscht alle Zuleitungszeilen, die der Bereich des Cursors cursor
   * berührt, und liefert true zurück, wenn mindestens eine Zuleitungszeile gelöscht
   * wurde oder false, wenn sich in dem Bereich des Cursors keine Zuleitungszeile
   * befand.
   *
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Zuleitungszeilen gesucht wird.
   *
   * @return true, wenn mindestens eine Zuleitungszeile gelöscht wurde oder false,
   *         wenn kein der cursor keine Zuleitungszeile berührt.
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
          LOGGER.error("", e);
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

      XParagraphCursor parDeleter =
        UNO.XParagraphCursor(par.getText().createTextCursorByRange(par.getEnd()));

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
    UNO.setProperty(paragraph, "ParaStyleName",
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
   * Ziffer (+Tab) und formatiert diesen im Zeichenformat WollMuxRoemischeZiffer.
   * 
   * @param paragraph
   */
  private static void formatRoemischeZifferOnly(XTextRange paragraph)
  {
    XTextCursor zifferOnly = getZifferOnly(paragraph, false);
    if (zifferOnly != null)
    {
      UNO.setProperty(zifferOnly, "CharStyleName", CharStyleNameRoemischeZiffer);

      // Zeichen danach auf Standardformatierung setzen, damit der Text, der
      // danach geschrieben wird nicht auch obiges Zeichenformat besitzt:
      // ("Standard" gilt laut DevGuide auch in englischen Versionen)
      UNO.setProperty(zifferOnly.getEnd(), "CharStyleName", "Standard");
    }
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * Verfuegungspunkt markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit "WollMuxVerfuegungspunkt"
   *         beginnt.
   */
  private static boolean isVerfuegungspunkt(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunkt);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * VerfuegungspunktMitZuleitung markierten Absatz handelt.
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
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunktMitZuleitung);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * Zuleitungszeile markierten Absatz handelt.
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
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameZuleitungszeile);
  }

  /**
   * Liefert true, wenn der übergebene Paragraph paragraph den für Abdrucke typischen
   * String in der Form "Abdruck von I[, II, ...][ und n]" enthält, andernfalls
   * false.
   * 
   * @param paragraph
   *          der zu prüfende Paragraph
   * @return
   */
  private static boolean isAbdruck(XTextRange paragraph)
  {
	String str = getStringOfXTextRange(paragraph);
    return str.contains(copyName + " von " + romanNumbers[0])
      || str.contains(copyName + " von <Vorgänger>.");
  }

  /**
   * Liefert einen String des übergebenen TextRange zurück   
   * 
   * @param textRange
   *          
   * @return String
   */  
  static String getStringOfXTextRange(XTextRange textRange)
  {
	String str = "";
	XEnumerationAccess enumerationAccess =  UNO.XEnumerationAccess(textRange);
	if( enumerationAccess.hasElements())
	{
	  str = textRange.getString();
	}
	return str;	
  }  
  
  /**
   * Liefert den letzten Teil suffix, der am Ende eines Abdruck-Strings der Form
   * "Abdruck von I[, II, ...][ und n]<suffix>" gefunden wird oder "", wenn der kein
   * Teil gefunden wurde. Das entspricht dem Text, den der Benutzer manuell
   * hinzugefügt hat.
   * 
   * @param paragraph
   *          der Paragraph, der den Abdruck-String enthält.
   * @return den suffix des Abdruck-Strings, der überlicherweise vom Benutzer manuell
   *         hinzugefügt wurde.
   */
  private static String getAbdruckSuffix(XTextRange paragraph)
  {
    String str = paragraph.getString();
    Matcher m =
      Pattern.compile(
        "[XIV0-9]+\\.\\s*" + copyName + " von " + romanNumbers[0]
          + "(, [XIV0-9]+\\.)*( und [XIV0-9]+\\.)?(.*)").matcher(str);
    if (m.matches()) return m.group(3);
    return "";
  }

  /**
   * Zählt die Anzahl Verfügungspunkte im Dokument vor der Position von
   * range.getStart() (einschließlich) und liefert deren Anzahl zurück, wobei auch
   * ein evtl. vorhandener Rahmen WollMuxVerfuegungspunkt1 mit gezählt wird.
   * 
   * @param doc
   *          Das Dokument in dem sich range befindet (wird benötigt für den Rahmen
   *          WollMuxVerfuegungspunkt1)
   * @param range
   *          Die TextRange, bei der mit der Zählung begonnen werden soll.
   * @return die Anzahl Verfügungspunkte vor und mit range.getStart()
   */
  public static int countVerfPunkteBefore(XTextDocument doc, XParagraphCursor range)
  {
    int count = 0;

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null) count++;

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range.getStart()));
    if (cursor != null) do
    {
      if (isVerfuegungspunkt(cursor)) count++;
    } while (cursor.gotoPreviousParagraph(false));

    return count;
  }

  /**
   * Sucht nach allen Absätzen im Haupttextbereich des Dokuments doc (also nicht in
   * Frames), deren Absatzformatname mit "WollMuxVerfuegungspunkt" beginnt und
   * numeriert die bereits vorhandenen römischen Ziffern neu durch oder erzeugt eine
   * neue Ziffer, wenn in einem entsprechenden Verfügungspunkt noch keine Ziffer
   * gesetzt wurde. Ist ein Rahmen mit dem Namen WollMuxVerfuegungspunkt1 vorhanden,
   * der einen als Verfügungpunkt markierten Paragraphen enthält, so wird dieser
   * Paragraph immer (gemäß Konzept) als Verfügungspunkt "I" behandelt.
   * 
   * @param doc
   *          Das Dokument, in dem alle Verfügungspunkte angepasst werden sollen.
   */
  public static void ziffernAnpassen(TextDocumentController documentController)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(documentController.getModel().doc);

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
    XParagraphCursor cursor =
      UNO.XParagraphCursor(documentController.getModel().doc.getText().createTextCursorByRange(
        documentController.getModel().doc.getText().getStart()));
    if (cursor != null) do
    {
      // ganzen Paragraphen markieren
      cursor.gotoEndOfParagraph(true);

      if (isVerfuegungspunkt(cursor))
      {
        count++;

        if (isAbdruck(cursor))
        {
          // Behandlung von Paragraphen mit einem "Abdruck"-String
          String abdruckStr = abdruckString(count) + getAbdruckSuffix(cursor);
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
            zifferOnly = cursor.getText().createTextCursorByRange(cursor.getStart());
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
      documentController.addPrintFunction(PRINT_FUNCTION_NAME);
    else
      documentController.removePrintFunction(PRINT_FUNCTION_NAME);
  }

  /**
   * Liefert eine XTextRange, die genau die römische Ziffer (falls vorhanden mit
   * darauf folgendem \t-Zeichen) am Beginn eines Absatzes umschließt oder null,
   * falls keine Ziffer gefunden wurde. Bei der Suche nach der Ziffer werden nur die
   * ersten 7 Zeichen des Absatzes geprüft.
   * 
   * @param par
   *          die TextRange, die den Paragraphen umschließt, in dessen Anfang nach
   *          der römischen Ziffer gesucht werden soll.
   * @param includeNoTab
   *          ist includeNoTab == true, so enthält der cursor immer nur die Ziffer
   *          ohne das darauf folgende Tab-Zeichen.
   * @return die TextRange, die genau die römische Ziffer umschließt falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par, boolean includeNoTab)
  {
    XParagraphCursor cursor =
      UNO.XParagraphCursor(par.getText().createTextCursorByRange(par.getStart()));

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
   * Liefert das Textobjekt des TextRahmens WollMuxVerfuegungspunkt1 oder null, falls
   * der Textrahmen nicht existiert. Der gesamte Text innerhalb des Textrahmens wird
   * dabei automatisch mit dem Absatzformat WollMuxVerfuegungspunkt1 vordefiniert.
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
      frame =
        UNO.XTextFrame(UNO.XTextFramesSupplier(doc).getTextFrames().getByName(
          FrameNameVerfuegungspunkt1));
    }
    catch (java.lang.Exception e)
    {}

    if (frame != null)
    {
      XTextCursor cursor = frame.getText().createTextCursorByRange(frame.getText());
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
   * <i-1>]", der passend zu einem Abdruck mit der Verfügungsnummer number angezeigt
   * werden soll.
   * 
   * @param number
   *          Die Nummer des Verfügungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verfügungspunkt bei i==0 und i==1 keinen
   *         Vorgänger besitzen kann.
   */
  private static String abdruckString(int number)
  {
    String str = romanNumber(number) + "\t" + copyName + " von " + romanNumber(1);
    for (int j = 2; j < (number - 1); ++j)
      str += ", " + romanNumber(j);
    if (number >= 3) str += " und " + romanNumber(number - 1);
    return str;
  }

  /**
   * Liefert die römische Zahl zum übgebenen integer Wert i. Die römischen Zahlen
   * werden dabei aus dem begrenzten Array romanNumbers ausgelesen. Ist i-1 kein
   * gültiger Index des Arrays, so sieht der Rückgabewert wie folgt aus
   * "<dezimalzahl(i)>.". Hier kann bei Notwendigkeit natürlich auch ein
   * Berechnungsschema für römische Zahlen implementiert werden, was für die
   * Sachleitenden Verfügungen vermutlich aber nicht erforderlich sein wird.
   * 
   * @param i
   *          Die Zahl, zu der eine römische Zahl geliefert werden soll.
   * @return Die römische Zahl, oder "<dezimalzahl(i)>, wenn i-1 nicht in den
   *         Arraygrenzen von romanNumbers.
   */
  private static String romanNumber(int i)
  {
    String number = "" + i + ".";
    if (i > 0 && i <= romanNumbers.length) number = romanNumbers[i - 1];
    return number;
  }

  /**
   * Erzeugt einen Vector mit Elementen vom Typ Verfuegungspunkt, der dem Druckdialog
   * übergeben werden kann und alle für den Druckdialog notwendigen Informationen
   * enthält.
   * 
   * @param doc
   *          Das zu scannende Dokument
   * @return Vector of Verfuegungspunkt, der für jeden Verfuegungspunkt im Dokument
   *         doc einen Eintrag enthält.
   */
  private static Vector<Verfuegungspunkt> scanVerfuegungspunkte(XTextDocument doc)
  {
    Vector<Verfuegungspunkt> verfuegungspunkte = new Vector<Verfuegungspunkt>();

    // Verfügungspunkt1 hinzufügen wenn verfügbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
    {
      Verfuegungspunkt original =
        new Verfuegungspunkt(L.m(romanNumbers[0] + " Original"));
      original.addZuleitungszeile(L.m("Empfänger siehe Empfängerfeld"));
      verfuegungspunkte.add(original);
    }

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verfügungspunkte erstellen. Die
    // Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));

    if (cursor != null) do
    {
      // ganzen Paragraphen markieren
      cursor.gotoEndOfParagraph(true);

      if (isVerfuegungspunkt(cursor))
      {
        String heading = cursor.getString();
        currentVerfpunkt = new Verfuegungspunkt(heading);
        currentVerfpunkt.setMinNumberOfCopies(1);
        verfuegungspunkte.add(currentVerfpunkt);
      }

      // Zuleitungszeilen hinzufügen (auch wenn der Paragraph Verfügungspunkt
      // und Zuleitungszeile zugleich ist)
      if ((isZuleitungszeile(cursor) || isVerfuegungspunktMitZuleitung(cursor))
        && currentVerfpunkt != null
        // ausgeblendete Zeilen ignorieren
        && Boolean.FALSE.equals(UNO.getProperty(cursor, "CharHidden")))
      {
        String zuleit = cursor.getString();
        // nicht leere Zuleitungszeilen zum Verfügungspunkt hinzufügen.
        if (!(zuleit.length() == 0)) currentVerfpunkt.addZuleitungszeile(zuleit);
      }
    } while (cursor.gotoNextParagraph(false));

    return verfuegungspunkte;
  }

  /**
   * Repräsentiert einen vollständigen Verfügungspunkt, der aus Überschrift (römische
   * Ziffer + Überschrift) und Inhalt besteht. Die Klasse bietet Methden an, über die
   * auf alle für den Druck wichtigen Eigenschaften des Verfügungspunktes zugegriffen
   * werden kann (z.B. Überschrift, Anzahl Zuleitungszeilen, ...)
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
    protected Vector<String> zuleitungszeilen;

    /**
     * Enthält die Anzahl der Ausdrucke, die mindestens ausgedruckt werden sollen.
     */
    protected int minNumberOfCopies;

    /**
     * Erzeugt einen neuen Verfügungspunkt, wobei firstPar der Absatz ist, der die
     * erste Zeile mit der römischen Ziffer und der Überschrift enthält.
     * 
     * @param heading
     *          Text der ersten Zeile des Verfügungspunktes mit der römischen Ziffer
     *          und der Überschrift.
     */
    public Verfuegungspunkt(String heading)
    {
      this.heading = heading;
      this.zuleitungszeilen = new Vector<String>();
      this.minNumberOfCopies = 0;
    }

    /**
     * Fügt einen weitere Zuleitungszeile des Verfügungspunktes hinzu (wenn paragraph
     * nicht null ist).
     * 
     * @param paragraph
     *          XTextRange, das den gesamten Paragraphen der Zuleitungszeile enthält.
     */
    public void addZuleitungszeile(String zuleitung)
    {
      zuleitungszeilen.add(zuleitung);
    }

    /**
     * Liefert die Anzahl der Ausfertigungen zurück, mit denen der Verfügungspunkt
     * ausgeduckt werden soll; Die Anzahl erhöht sich mit jeder hinzugefügten
     * Zuleitungszeile. Der Mindestwert kann mit setMinNumberOfCopies gesetzt werden.
     * 
     * @return Anzahl der Ausfertigungen mit denen der Verfügungspunkt gedruckt
     *         werden soll.
     */
    public int getNumberOfCopies()
    {
      if (zuleitungszeilen.size() > minNumberOfCopies)
        return zuleitungszeilen.size();
      else
        return minNumberOfCopies;
    }

    /**
     * Setzt die Anzahl der Ausfertigungen, die Mindestens ausgedruckt werden sollen,
     * auch dann wenn z.B. keine Zuleitungszeilen vorhanden sind.
     * 
     * @param minNumberOfCopies
     *          Anzahl der Ausfertigungen mit denen der Verfügungspunkt mindestens
     *          ausgedruckt werden soll.
     */
    public void setMinNumberOfCopies(int minNumberOfCopies)
    {
      this.minNumberOfCopies = minNumberOfCopies;
    }

    /**
     * Liefert einen Vector of Strings, der die Texte der Zuleitungszeilen
     * beinhaltet, die dem Verfügungspunkt mit addParagraph hinzugefügt wurden.
     * 
     * @return Vector of Strings mit den Texten der Zuleitungszeilen.
     */
    public Vector<String> getZuleitungszeilen()
    {
      return zuleitungszeilen;
    }

    /**
     * Liefert den Text der Überschrift des Verfügungspunktes einschließlich der
     * römischen Ziffer als String zurück, wobei mehrfache Leerzeichen (\s+) durch
     * einfache Leerzeichen ersetzt werden.
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
   * Zeigt den Druckdialog für Sachleitende Verfügungen an und liefert die dort
   * getroffenen Einstellungen als Liste von VerfuegungspunktInfo-Objekten zurück,
   * oder null, wenn Fehler auftraten oder der Druckvorgang abgebrochen wurde.
   * 
   * @param doc
   *          Das Dokument, aus dem die anzuzeigenden Verfügungspunkte ausgelesen
   *          werden.
   */
  public static List<VerfuegungspunktInfo> callPrintDialog(XTextDocument doc)
  {
    //JGM: Update der Dokumentenstruktur (Kommandos und TextSections)
    DocumentManager.getTextDocumentController(doc).updateDocumentCommands();
    Vector<Verfuegungspunkt> vps = scanVerfuegungspunkte(doc);
    Iterator<Verfuegungspunkt> iter = vps.iterator();
    while (iter.hasNext())
    {
      Verfuegungspunkt vp = iter.next();
      String text = L.m("Verfügungspunkt '%1'", vp.getHeading());
      Iterator<String> zuleits = vp.getZuleitungszeilen().iterator();
      while (zuleits.hasNext())
      {
        String zuleit = zuleits.next();
        text += "\n  --> '" + zuleit + "'";
      }
      LOGGER.trace(text);
    }

    // Dialog ausführen und Rückgabewert zurückliefern.
    try
    {
      SyncActionListener s = new SyncActionListener();
      
      new SachleitendeVerfuegungenDruckdialog(vps, s);

      ActionEvent result = s.synchronize();
      String cmd = result.getActionCommand();
      SimpleEntry<List<VerfuegungspunktInfo>, Boolean> slvd =
        (SimpleEntry<List<VerfuegungspunktInfo>, Boolean>) result.getSource();
      
      if (SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT.equals(cmd) && slvd != null)
      {
        List<VerfuegungspunktInfo> verfuegungsPunktInfos = slvd.getKey();
        
        if (verfuegungsPunktInfos == null || verfuegungsPunktInfos.isEmpty())
        {
          LOGGER.debug("Sachleitende Verfuegung: callPrintDialog(): VerfuegungspunktInfos NULL or empty.");
          return new ArrayList<VerfuegungspunktInfo>();
        }
        
        if (slvd.getValue())
        {
          return verfuegungsPunktInfos;
        }
        else
        {
          // sonst in umgekehrter Reihenfolge
          List<VerfuegungspunktInfo> descVerfuegungsPunktInfos = new ArrayList<>();
          ListIterator<VerfuegungspunktInfo> lIt = verfuegungsPunktInfos.listIterator(verfuegungsPunktInfos.size());
          
          while (lIt.hasPrevious()) {
            descVerfuegungsPunktInfos.add(lIt.previous());
          }
          
          return descVerfuegungsPunktInfos;
        }
      }
      return null;
    }
    catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
      return null;
    }
  }

  /**
   * Liefert die Anzahl der im XTextDocument doc enthaltenen Verfügungspunkte zurück.
   *
   * @param doc
   *          das TextDocument in dem gezählt werden soll.
   * @return die Anzahl der im XTextDocument doc enthaltenen Verfügungspunkte
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static int countVerfuegungspunkte(XTextDocument doc)
  {
    if (doc != null)
      return scanVerfuegungspunkte(doc).size();
    else
      return 0;
  }

  /**
   * Druckt den Verfügungpunkt Nummer verfPunkt aus dem Dokument aus, das im
   * XPrintModel pmod hinterlegt ist.
   * 
   * @param pmod
   *          Das PrintModel zu diesem Druckvorgang.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verfügungspunktes, wobei alle folgenden
   *          Verfügungspunkte ausgeblendet werden.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Blöcke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verfügungspunktes I ausgeblendet
   *          und alle notInOriginal-Blöcke ebenso. Andernfalls sind Ziffer und
   *          notInOriginal-Blöcke eingeblendet.
   * @param copyCount
   *          enthält die Anzahl der Kopien, die von diesem Verfügungspunkt erstellt
   *          werden sollen.
   * @throws PrintFailedException
   */
  public static void printVerfuegungspunkt(XPrintModel pmod, int verfPunkt,
      boolean isDraft, boolean isOriginal, short copyCount)
  {
    XTextDocument doc = pmod.getTextDocument();

    // Steht der viewCursor in einem Bereich, der im folgenden ausgeblendet
    // wird, dann wird der ViewCursor in einen sichtbaren Bereich verschoben. Um
    // den viewCursor wieder herstellen zu können, wird er hier gesichert und
    // später wieder hergestellt.
    XTextCursor vc = null;
    XTextCursor oldViewCursor = null;
    XTextViewCursorSupplier suppl =
      UNO.XTextViewCursorSupplier(UNO.XModel(pmod.getTextDocument()).getCurrentController());
    if (suppl != null) vc = suppl.getViewCursor();
    if (vc != null) oldViewCursor = vc.getText().createTextCursorByRange(vc);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    int count = 0;
    if (punkt1 != null) count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));
    if (cursor != null) do
    {
      cursor.gotoEndOfParagraph(true);

      if (isVerfuegungspunkt(cursor))
      {
        // Punkt1 merken
      if (punkt1 == null) punkt1 = cursor.getText().createTextCursorByRange(cursor);

      count++;

      if (count == (verfPunkt + 1))
      {
        cursor.collapseToStart();
        cursor.gotoRange(cursor.getText().getEnd(), true);
        setInvisibleRange = cursor;
      }
    }
  } while (setInvisibleRange == null && cursor.gotoNextParagraph(false));

    // Prüfen, welche Textsections im ausgeblendeten Bereich liegen und diese
    // ebenfalls ausblenden (und den alten Stand merken):
    List<XTextSection> hidingSections =
      getSectionsFromPosition(pmod.getTextDocument(), setInvisibleRange);
    HashMap<XTextSection, Boolean> sectionOldState =
      new HashMap<XTextSection, Boolean>();
    for (XTextSection section : hidingSections)
      try
      {
        boolean oldState =
          AnyConverter.toBoolean(UNO.getProperty(section, "IsVisible"));
        sectionOldState.put(section, oldState);
        UNO.setProperty(section, "IsVisible", Boolean.FALSE);
      }
      catch (IllegalArgumentException x)
      {}

    // ensprechende Verfügungspunkte ausblenden
    if (setInvisibleRange != null) UNO.hideTextRange(setInvisibleRange, true);

    // Ein/Ausblenden Druckblöcke (z.B. draftOnly):
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_DRAFT_ONLY, isDraft, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_NOT_IN_ORIGINAL, !isOriginal, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ORIGINAL_ONLY, isOriginal, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ALL_VERSIONS, true, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_COPY_ONLY, !isDraft && !isOriginal, false);

    // Ein/Ausblenden der Sichtbarkeitsgruppen:
    pmod.setGroupVisible(GROUP_ID_SLV_DRAFT_ONLY, isDraft);
    pmod.setGroupVisible(GROUP_ID_SLV_NOT_IN_ORIGINAL, !isOriginal);
    pmod.setGroupVisible(GROUP_ID_SLV_ORIGINAL_ONLY, isOriginal);
    pmod.setGroupVisible(GROUP_ID_SLV_ALL_VERSIONS, true);
    pmod.setGroupVisible(GROUP_ID_SLV_COPY_ONLY, !isDraft && !isOriginal);

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1, true);
      UNO.hideTextRange(punkt1ZifferOnly, true);
    }

    // -----------------------------------------------------------------------
    // Druck des Dokuments
    // -----------------------------------------------------------------------
    for (int j = 0; j < copyCount; ++j)
      pmod.printWithProps();

    // Ausblendung von Ziffer von Punkt 1 wieder aufheben
    if (punkt1ZifferOnly != null) UNO.hideTextRange(punkt1ZifferOnly, false);

    // Sichtbarkeitsgruppen wieder einblenden
    pmod.setGroupVisible(GROUP_ID_SLV_DRAFT_ONLY, true);
    pmod.setGroupVisible(GROUP_ID_SLV_NOT_IN_ORIGINAL, true);
    pmod.setGroupVisible(GROUP_ID_SLV_ORIGINAL_ONLY, true);
    pmod.setGroupVisible(GROUP_ID_SLV_ALL_VERSIONS, true);
    pmod.setGroupVisible(GROUP_ID_SLV_COPY_ONLY, true);

    // Alte Eigenschaften der Druckblöcke wieder herstellen:
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_DRAFT_ONLY, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_NOT_IN_ORIGINAL, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ORIGINAL_ONLY, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ALL_VERSIONS, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_COPY_ONLY, true, true);

    // ausgeblendete TextSections wieder einblenden
    for (XTextSection section : hidingSections)
    {
      Boolean oldState = sectionOldState.get(section);
      if (oldState != null) UNO.setProperty(section, "IsVisible", oldState);
    }

    // Verfügungspunkte wieder einblenden:
    if (setInvisibleRange != null) UNO.hideTextRange(setInvisibleRange, false);

    // ViewCursor wieder herstellen:
    if (vc != null && oldViewCursor != null) vc.gotoRange(oldViewCursor, false);
  }

  /**
   * Diese Methode liefert in eine Liste aller Textsections aus doc, deren Anker an
   * der selben Position oder hinter der Position der TextRange pos liegt.
   * 
   * @param doc
   *          Textdokument in dem alle enthaltenen Textsections geprüft werden.
   * @param pos
   *          Position, ab der die TextSections in den Vector aufgenommen werden
   *          sollen.
   * @return eine Liste aller TextSections, die an oder nach pos starten oder eine
   *         leere Liste, wenn es Fehler gab oder keine Textsection gefunden wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static List<XTextSection> getSectionsFromPosition(XTextDocument doc,
      XTextRange pos)
  {
    Vector<XTextSection> v = new Vector<XTextSection>();
    if (pos == null) return v;
    XTextRangeCompare comp = UNO.XTextRangeCompare(pos.getText());
    if (comp == null) return v;
    XTextSectionsSupplier suppl = UNO.XTextSectionsSupplier(doc);
    if (suppl == null) return v;

    XNameAccess sections = suppl.getTextSections();
    String[] names = sections.getElementNames();
    for (int i = 0; i < names.length; i++)
    {
      XTextSection section = null;
      try
      {
        section = UNO.XTextSection(sections.getByName(names[i]));
      }
      catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }

      if (section != null)
      {
        try
        {
          int diff = comp.compareRegionStarts(pos, section.getAnchor());
          if (diff >= 0) v.add(section);
        }
        catch (IllegalArgumentException e)
        {
          // kein Fehler, da die Exceptions immer fliegt, wenn die ranges in
          // unterschiedlichen Textobjekten liegen.
        }
      }
    }
    return v;
  }

  /**
   * Liefert das Absatzformat (=ParagraphStyle) des Dokuments doc mit dem Namen name
   * oder null, falls das Absatzformat nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Absatzformat name gesucht werden soll.
   * @param name
   *          der Name des gesuchten Absatzformates
   * @return das Absatzformat des Dokuments doc mit dem Namen name oder null, falls
   *         das Absatzformat nicht definiert ist.
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
    {}
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Absatzformat (=ParagraphStyle) mit dem Namen
   * name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Absatzformat zurück oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Absatzformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Absatzformates
   * @param parentStyleName
   *          Name des Vorgänger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorgänger gesetzt werden
   *          soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Absatzformat oder null, falls das Absatzformat nicht
   *         erzeugt werden konnte.
   */
  private static XStyle createParagraphStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    XStyle style = null;
    try
    {
      style =
        UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.ParagraphStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {}
    return null;
  }

  /**
   * Liefert das Zeichenformat (=CharacterStyle) des Dokuments doc mit dem Namen name
   * oder null, falls das Format nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Zeichenformat name gesucht werden soll.
   * @param name
   *          der Name des gesuchten Zeichenformates
   * @return das Zeichenformat des Dokuments doc mit dem Namen name oder null, falls
   *         das Absatzformat nicht definiert ist.
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
    {}
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Zeichenformat (=CharacterStyle) mit dem Namen
   * name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Zeichenformat zurück oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Zeichenformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Zeichenformates
   * @param parentStyleName
   *          Name des Vorgänger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorgänger gesetzt werden
   *          soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Zeichenformat oder null, falls das Zeichenformat nicht
   *         erzeugt werden konnte.
   */
  private static XStyle createCharacterStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    XStyle style = null;
    try
    {
      style =
        UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.CharacterStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {}
    return null;
  }

  /**
   * Liefert den Styles vom Typ type des Dokuments doc.
   * 
   * @param doc
   *          Das Dokument, dessen StyleContainer zurückgeliefert werden soll.
   * @param type
   *          kann z.B. CHARACTER_STYLE oder PARAGRAPH_STYLE sein.
   * @return Liefert den Container der Styles vom Typ type des Dokuments doc oder
   *         null, falls der Container nicht bestimmt werden konnte.
   */
  private static XNameContainer getStyleContainer(XTextDocument doc,
      String containerName)
  {
    try
    {
      return UNO.XNameContainer(UNO.XNameAccess(
        UNO.XStyleFamiliesSupplier(doc).getStyleFamilies()).getByName(containerName));
    }
    catch (java.lang.Exception e)
    {}
    return null;
  }

  /**
   * Falls die für Sachleitenden Verfügungen notwendigen Absatz- und Zeichenformate
   * nicht bereits existieren, werden sie hier erzeugt und mit fest verdrahteten
   * Werten vorbelegt.
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
      UNO.setProperty(style, "CharHeight", Integer.valueOf(11));
      UNO.setProperty(style, "CharFontName", "Arial");
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt);
    if (style == null)
    {
      style =
        createParagraphStyle(doc, ParaStyleNameVerfuegungspunkt,
          ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", Float.valueOf(FontWeight.BOLD));
      UNO.setProperty(style, "ParaFirstLineIndent", Integer.valueOf(-700));
      UNO.setProperty(style, "ParaTopMargin", Integer.valueOf(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt1);
    if (style == null)
    {
      style =
        createParagraphStyle(doc, ParaStyleNameVerfuegungspunkt1,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "ParaFirstLineIndent", Integer.valueOf(0));
      UNO.setProperty(style, "ParaTopMargin", Integer.valueOf(0));
    }

    style = getParagraphStyle(doc, ParaStyleNameAbdruck);
    if (style == null)
    {
      style =
        createParagraphStyle(doc, ParaStyleNameAbdruck,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", Integer.valueOf(100));
      UNO.setProperty(style, "ParaFirstLineIndent", Integer.valueOf(-700));
      UNO.setProperty(style, "ParaTopMargin", Integer.valueOf(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameZuleitungszeile);
    if (style == null)
    {
      style =
        createParagraphStyle(doc, ParaStyleNameZuleitungszeile, ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", Integer.valueOf(1));
      UNO.setProperty(style, "CharWeight", Float.valueOf(FontWeight.BOLD));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunktMitZuleitung);
    if (style == null)
    {
      style =
        createParagraphStyle(doc, ParaStyleNameVerfuegungspunktMitZuleitung,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", Integer.valueOf(1));
    }

    // Zeichenformate:

    style = getCharacterStyle(doc, CharStyleNameDefault);
    if (style == null)
    {
      style = createCharacterStyle(doc, CharStyleNameDefault, null);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharHeight", Integer.valueOf(11));
      UNO.setProperty(style, "CharFontName", "Arial");
      UNO.setProperty(style, "CharUnderline", Integer.valueOf(0));
    }

    style = getCharacterStyle(doc, CharStyleNameRoemischeZiffer);
    if (style == null)
    {
      style =
        createCharacterStyle(doc, CharStyleNameRoemischeZiffer, CharStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharWeight", Float.valueOf(FontWeight.BOLD));
    }
  }

  /**
   * Wertet die wollmux,conf-Direktive ABDRUCK_NAME aus und setzt diese entsprechend
   * in der OOo Erweiterung. Ist kein ABDRUCK_NAME gegeben, so wird "Abdruck" als
   * Standardwert gesetzt.
   * 
   * @return Kopiebezeichner als String
   * 
   * @author Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de), Christoph Lutz
   */
  private static String getCopyName()
  {
    String name = L.m("Abdruck");
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();
    ConfigThingy nan = conf.query("SachleitendeVerfuegungen").query("ABDRUCK_NAME");
    try
    {
      name = L.m(nan.getLastChild().toString());
      LOGGER.debug(L.m("Verwende ABDRUCK_NAME '%1'", name));
    }
    catch (NodeNotFoundException x)
    {}
    return name;
  }

  /**
   * Wertet die wollmux,conf-Direktive NUMBERS aus und setzt diese entsprechend in
   * der OOo Erweiterung. Ist kein Wert gegeben, so werden römische Ziffern
   * verwendet.
   * 
   * @return Ziffern als String array
   * 
   * @author Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de), Christoph Lutz
   */
  private static String[] getNumbers()
  {
    String numbers = "roman";
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();
    ConfigThingy nan = conf.query("SachleitendeVerfuegungen").query("NUMBERS");
    try
    {
      numbers = nan.getLastChild().toString();
      LOGGER.debug(L.m("Verwende Zahlenformat '%1' aus Attribut NUMBERS.", numbers));
    }
    catch (NodeNotFoundException x)
    {}

    // if arabic is selected set numberArray to arabic numbers
    if ("arabic".equalsIgnoreCase(numbers))
    {
      return new String[] {
        "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.",
        "13.", "14.", "15." };
    }
    else
    {
      // roman is default
      if (!"roman".equalsIgnoreCase(numbers))
        LOGGER.error(L.m(
          "Ungültiger Wert '%1' für Attribut NUMBERS (zulässig: 'roman' und 'arabic'). Verwende 'roman' statt dessen.",
          numbers));
      return new String[] {
        "I.", "II.", "III.", "IV.", "V.", "VI.", "VII.", "VIII.", "IX.", "X.",
        "XI.", "XII.", "XIII.", "XIV.", "XV." };
    }
  }

  /**
   * Sorgt ohne Verlust von sichtbaren Formatierungseigenschaften dafür, dass alle
   * Formatvorlagen des Dokuments doc, die in Sachleitenden Verfügungen eine
   * besondere Rolle spielen, zukünftig nicht mehr vom WollMux interpretiert werden.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void deMuxSLVStyles(XTextDocument doc)
  {
    if (doc == null) return;

    HashMap<String, String> mapOldNameToNewName = new HashMap<String, String>();
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));
    if (cursor != null)
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor) || isZuleitungszeile(cursor)
          || isVerfuegungspunktMitZuleitung(cursor))
        {
          String oldName = "";
          try
          {
            oldName =
              AnyConverter.toString(UNO.getProperty(cursor, "ParaStyleName"));
          }
          catch (IllegalArgumentException e)
          {}

          // Einmalig Style NO<number>_<oldName> erzeugen, der von <oldName> erbt.
          String newName = mapOldNameToNewName.get(oldName);
          XStyle newStyle = null;
          if (newName == null) do
          {
            newName = "NO" + new Random().nextInt(1000) + "_" + oldName;
            mapOldNameToNewName.put(oldName, newName);
            newStyle = createParagraphStyle(doc, newName, oldName);
          } while (newStyle == null);

          if (oldName != null)
          {
            // Das Setzen von ParaStyleName setzt mindestens CharHidden des cursors
            // auf Default zurück. Daher muss der bisherige Stand von CharHidden nach
            // dem Setzen wieder hergestellt werden:
            Object hidden = UNO.getProperty(cursor, "CharHidden");
            UNO.setProperty(cursor, "ParaStyleName", newName);
            UNO.setProperty(cursor, "CharHidden", hidden);
          }
        }
      } while (cursor.gotoNextParagraph(false));

    // Extra-Frame für Verfügungspunkt1 umbenennen
    try
    {
      XNamed frame =
        UNO.XNamed(UNO.XTextFramesSupplier(doc).getTextFrames().getByName(
          FrameNameVerfuegungspunkt1));
      if (frame != null) frame.setName("NON_" + FrameNameVerfuegungspunkt1);
    }
    catch (java.lang.Exception e)
    {}

  }
}
