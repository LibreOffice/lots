/*
 * Dateiname: PrintIntoFile.java
 * Projekt  : WollMux
 * Funktion : "Druck"funktion, die das zu druckende Dokument an ein Ergebnisdokument anhängt.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 29.10.2007 | BNK | Erstellung
 * 29.01.2008 | BNK | Fertigstellung
 * 30.01.2008 | BNK | Workaround für Issue 73229
 * 04.05.2011 | ERT | (ERT)[R120366][#6797]In appendToFile wurde das 
 *                    Property PageStyleName nicht korrekt ausgelesen.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.ooo.TextDocument;

/**
 * "Druck"funktion, die das zu druckende Dokument an ein Ergebnisdokument anhängt.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PrintIntoFile
{
  /**
   * Dateiname einer temporären Datei.
   */
  private static final String TEMP_FILE_NAME = "serienbrief.odt";

  /**
   * Präfix, das vor den Namen des angelegten temporären Verzeichnisses gesetzt wird.
   */
  private static final String TEMP_DIR_PREFIX = "wollmux-seriendruck-";

  /**
   * Hängt den Inhalt von inputDoc an outputDoc an.
   * 
   * @param firstAppend
   *          muss auf true gesetzt werden, wenn dies das erste Mal ist, das etwas an
   *          das Gesamtdokument angehängt wird. In diesem Fall werden die Formate
   *          aus inputDoc zuerst nach outputDoc übertragen und es wird kein
   *          Zeilenumbruch eingefügt. Außerdem werden in diesem Fall die
   *          com.sun.star.document.Settings von inputDoc auf outputDoc übertragen.
   * @param knownPageStyles
   *          HashSet in dem die Namen von bereits bekannten PageStyles aufgeführt
   *          werden um sicherstellen zu können, dass neu PageStyles bei der
   *          notwendigen Umbenennung eindeutig benannt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  public static void appendToFile(XTextDocument outputDoc, XTextDocument inputDoc,
      HashSet<String> knownPageStyles, boolean firstAppend)
  {
    // dest[0] ist das temp. Verzeichnis, dest[1] die temp. Datei darin
    File[] dest = new File[] {
      null, null };
    try
    {
      String url = storeInTemporaryFile(inputDoc, dest);

      // Ausgabedokument initialisieren
      if (firstAppend)
        initializeOutputDocumentFormatsAndSettings(outputDoc, inputDoc, url);

      // pageOffset bestimmen:
      int pageOffset =
        ((Number) UNO.getProperty(outputDoc.getCurrentController(), "PageCount")).intValue();
      // Ein leeres Dokument hat immer eine Seite, auch wenn es für die
      // Offset-Zählung leer ist.
      if (pageOffset == 1) pageOffset = 0;
      // PageOffset muss eine gerade Zahl sein, da OOo automatisch eine Leerseite
      // einfügt, wenn weiter unten der Seitenumbruch eingefügt wird.
      if (pageOffset % 2 != 0) pageOffset++;

      // Temporäres Dokument vorbereiten für Merge über insertDocumentFromURL()
      String firstPageStyleName =
        prepareTemporaryDocumentForMailMergeInsertion(url, pageOffset,
          knownPageStyles);

      // Einfügen des zweiten Dokuments
      XText text = outputDoc.getText();
      XTextCursor cursor = text.createTextCursorByRange(text.getEnd());
      if (!firstAppend) cursor.setString("\r");
      UNO.XDocumentInsertable(cursor).insertDocumentFromURL(url,
        new PropertyValue[] {});

      /*
       * FIXME: OOo Issue 37417 beachten --> When inserting a document (via
       * "Insert->Document") on the first paragraph of a page after a pagebreak, and
       * the document contains only one paragraph, the pagebreak will be removed.
       * Inserting documents with more than one paragraph works as expected.
       */

      // Seitenumbruch an der ersten eingefügten Seite nachträglich explizit setzen
      // und PageNumberOffset auf 1 resetten.
      cursor.collapseToStart();
      UNO.setProperty(cursor, "PageDescName", firstPageStyleName);
      /*
       * Format-->Absatz-->Textfluss-->Umbrüche--> Checkbox "mit Seitenvorlage" -->
       * Seitennummer 1 (Seitennummer mit 1 beginnen nach dem Seitenumbruch) ACHTUNG!
       * OOo lässt ungerade Seitennummern nur auf wirklich ungeraden Seiten zu. Ist
       * die betreffende Seite eine gerade Seite, so wird durch das Setzen von
       * PageNumberOffset auf 1 eine leere Seite davor eingefügt!
       */
      UNO.setProperty(cursor, "PageNumberOffset", Short.valueOf((short) 1));
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    finally
    {
      try
      {
        dest[1].delete();
      }
      catch (Exception x)
      {}
      try
      {
        dest[0].delete();
      }
      catch (Exception x)
      {}
    }
  }

  /**
   * Initialisiert das Ausgabedokument outputDoc für den WollMux-Seriendruck mit den
   * dafür notwendigen Formatvorlagen und Settings aus dem Eingangsdokument inputDoc
   * (bzw. der serialisierten und der in url spezifizierten Variante von inputDoc).
   * 
   * @param outputDoc
   *          Enthält das Ausgabedokument für den MailMerge
   * @param inputDoc
   *          Enthält das Dokument, das später an das Ausgabedokument angehängt
   *          werden soll
   * @param url
   *          Beschreibt die temporäre, direkt aus inputDoc abgeleitete Eingangsdatei
   *          und wird benötigt, um initial Styles mit loadStylesFromURL in das
   *          Ausgabedokument zu laden.
   * @throws Exception
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void initializeOutputDocumentFormatsAndSettings(
      XTextDocument outputDoc, XTextDocument inputDoc, String url) throws Exception
  {
    UnoProps props = new UnoProps();
    props.setPropertyValue("OverwriteStyles", Boolean.TRUE);
    props.setPropertyValue("LoadCellStyles", Boolean.TRUE);
    props.setPropertyValue("LoadTextStyles", Boolean.TRUE);
    props.setPropertyValue("LoadFrameStyles", Boolean.TRUE);
    props.setPropertyValue("LoadPageStyles", Boolean.TRUE);
    props.setPropertyValue("LoadNumberingStyles", Boolean.TRUE);
    XStyleFamiliesSupplier sfs = UNO.XStyleFamiliesSupplier(outputDoc);
    XStyleLoader loader = UNO.XStyleLoader(sfs.getStyleFamilies());
    loader.loadStylesFromURL(url, props.getProps());

    XPropertySet inSettings =
      UNO.XPropertySet(UNO.XMultiServiceFactory(inputDoc).createInstance(
        "com.sun.star.document.Settings"));
    XPropertySet outSettings =
      UNO.XPropertySet(UNO.XMultiServiceFactory(outputDoc).createInstance(
        "com.sun.star.document.Settings"));

    TextDocument.copySimpleProperties(inSettings, outSettings);
  }

  /**
   * Öffnet das durch url beschriebene Dokument und bereitet es so für die weitere
   * Bearbeitung vor, dass es später ohne weitere Anpassungen mit
   * XDocumentInsertable.insertDocumentFromURL(url, new PropertyValue[] {}) an das
   * Hauptdokument angehängt werden kann, welches bereits pageNumberOffset belegte
   * Seiten enthält.
   * 
   * Eigentlich macht XDocumentInsertable.insertDocumentFromURL(url, new
   * PropertyValue[] {}) schon einige Anpassungen wie z.B. die Umbenennung von
   * Bookmarks oder Textrahmen. Einige, speziell für unseren Merge notwendigen
   * Anpassungen macht insertDocumentFromURL aber nicht wie z.B. die Ersetzung von
   * InputUser-Feldern durch die Textrepräsentation oder das Setzen der PageOffsets
   * bei an der Seite verankerten Elementen. Diese noch notwendigen Anpassungen
   * werden hier erledigt.
   * 
   * Früher wurden für die Anpassungen immer alle Elemente des Gesamtdokuments
   * durchgegangen, was mit steigender Größe des Gesamtdokuments zu deutlichen
   * Performanceeinbußen führte. Mit Hilfe des temporären Dokuments, das in dieser
   * Methode geöffnet und angepasst wird, sind die Anpassungen nur noch auf das
   * temporäre Dokument beschränkt und die Laufzeit bleibt damit linear.
   * 
   * @param url
   *          Beschreibt das temporäre Dokument
   * @param pageNumberOffset
   *          Offset, der auf die Seitennummer von an der Seite verankerten Elemente
   *          addiert werden muss.
   * @param knownPageStyles
   *          HashSet in dem die Namen von bereits bekannten PageStyles aufgeführt
   *          werden um sicherstellen zu können, dass neu PageStyles bei der
   *          notwendigen Umbenennung eindeutig benannt werden.
   * @return Liefert den neuen Namen des PageStyles der ersten Dokumentseite zurück
   *         und wird im späteren Verlauf für die Einfügung des Seitenumbruchs in das
   *         Gesamtdokument benötigt.
   * @throws com.sun.star.io.IOException
   * @throws IllegalArgumentException
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String prepareTemporaryDocumentForMailMergeInsertion(String url,
      int pageNumberOffset, HashSet<String> knownPageStyles)
      throws com.sun.star.io.IOException, IllegalArgumentException
  {
    XTextDocument doc =
      UNO.XTextDocument(UNO.loadComponentFromURL(url, false, true, true));

    // Felder mit Gesamtseitenzahl des Dokuments durch aktuellen festen Wert ersetzen
    fixPageCountFields(UNO.XTextFieldsSupplier(doc).getTextFields(),
      ((Number) UNO.getProperty(doc.getCurrentController(), "PageCount")).intValue());

    // InputUser-Felder durch aktuellen festen Wert ersetzen
    fixInputUserFields(UNO.XTextFieldsSupplier(doc).getTextFields());

    // Falls das Dokument mit Seitenformat "Standard" beginnt, so wird dieses
    // explizit als Seitenformat gesetzt, damit es von renamePageStyles() mit erkannt
    // und behandelt wird.
    String firstPageStyleName = getFirstPageStyle(doc);
    XTextCursor cursor =
      doc.getText().createTextCursorByRange(doc.getText().getStart());
    if ("Standard".equals(firstPageStyleName))
      UNO.setProperty(cursor, "PageDescName", firstPageStyleName);

    // Alle verwendeten PageStyles umbenennen
    cursor.collapseToStart();
    XParagraphCursor paraCursor = UNO.XParagraphCursor(cursor);
    Map<String, String> mapOldPageStyleName2NewPageStyleName =
      new HashMap<String, String>();
    renamePageStyles(paraCursor, doc, mapOldPageStyleName2NewPageStyleName,
      knownPageStyles);

    // An der Seite verankerte Objekte mit neuem pageNumberOffset versehen.
    fixPageAnchoredObjects(
      UNO.XIndexAccess(UNO.XDrawPageSupplier(doc).getDrawPage()), pageNumberOffset);

    // Workaround für http://www.openoffice.org/issues/show_bug.cgi?id=73229
    if (Workarounds.applyWorkaroundForOOoIssue73229())
    {
      XTextContent startingSection = getSectionAtDocumentStart(doc);
      if (startingSection != null)
        try
        {
          startingSection.getAnchor().getText().removeTextContent(startingSection);
        }
        catch (NoSuchElementException e)
        {
          Logger.error(
            "Workaround für Issue 73229: Kann Textbereich nicht aufheben.", e);
        }
    }

    UNO.XStorable(doc).store();
    try
    {
      UNO.XCloseable(doc).close(false);
    }
    catch (CloseVetoException e)
    {
      // Bei meinen temporären Dokumenten hat niemand anderes mitzureden. Deshalb
      // Holzhammer-Methode.
      doc.dispose();
    }

    return mapOldPageStyleName2NewPageStyleName.get(firstPageStyleName);
  }

  /**
   * Liefer den Namen des PageStyles der ersten Dokumentseite von doc zurück oder
   * null, falls aufgrund einer Exception der PageStyle nicht bestimmt werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String getFirstPageStyle(XTextDocument doc)
  {
    XEnumeration enu = UNO.XEnumerationAccess(doc.getText()).createEnumeration();
    while (enu.hasMoreElements())
    {
      Object o = null;
      try
      {
        o = enu.nextElement();
      }
      catch (Exception e)
      {
        Logger.error(e);
        return null;
      }
      Object prop = UNO.getProperty(o, "PageStyleName");
      if (AnyConverter.isString(prop))
      {
        try
        {
          return AnyConverter.toString(prop);
        }
        catch (IllegalArgumentException e)
        {}
      }
    }
    return null;
  }

  /**
   * Falls das Dokument mit einer Section startet, so wird das XTextContent-Objekt
   * dieser Section zurück geliefert, andernfalls null.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  private static XTextContent getSectionAtDocumentStart(XTextDocument doc)
  {
    XTextRange docText = doc.getText();
    XTextRangeCompare compare = UNO.XTextRangeCompare(docText);
    XNameAccess sections = UNO.XTextSectionsSupplier(doc).getTextSections();
    String[] names = sections.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        XTextContent section = UNO.XTextContent(sections.getByName(names[i]));
        XTextRange range = section.getAnchor();
        if (compare.compareRegionStarts(docText, range) == 0) return section;
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
    return null;
  }

  /**
   * Latscht mit dem Cursor solange die Paragraphen durch bis zum Ende und für jedes
   * PageDescName Property wird das entsprechende Seitenformat auf ein neues kopiert
   * mit einem noch nicht verwendeten Namen und das PageDescName-Property
   * entsprechend geändert, dass es auf das neue Format verweist. Das selbe Format
   * wird jeweils nur einmal kopiert.
   * 
   * @param doc
   *          das Dokument in dem der Cursor wandert
   * @param knownPageStyles
   *          HashSet in dem die Namen von bereits bekannten PageStyles aufgeführt
   *          werden um sicherstellen zu können, dass neu PageStyles bei der
   *          notwendigen Umbenennung eindeutig benannt werden.
   * @param oldPageStyles
   *          die PageStyles Familie des alten Dokuments
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  private static void renamePageStyles(XParagraphCursor cursor, XTextDocument doc,
      Map<String, String> mapOldPageStyleName2NewPageStyleName,
      HashSet<String> knownPageStyles)
  {
    XNameAccess pageStyles;
    try
    {
      pageStyles =
        UNO.XNameAccess(UNO.XStyleFamiliesSupplier(doc).getStyleFamilies().getByName(
          "PageStyles"));
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }
    do
    {
      try
      {
        Object ob = UNO.getProperty(cursor, "PageDescName");
        if (AnyConverter.isString(ob))
        {
          String pageDescName = AnyConverter.toString(ob);
          String newPageStyleName =
            mapOldPageStyleName2NewPageStyleName.get(pageDescName);
          if (newPageStyleName == null)
          {
            XPropertySet oldStyle =
              UNO.XPropertySet(pageStyles.getByName(pageDescName));
            do
            {
              newPageStyleName = pageDescName + (int) (Math.random() * 1000000.0);
            } while (knownPageStyles.contains(newPageStyleName));
            TextDocument.copyPageStyle(doc, oldStyle, newPageStyleName);
            mapOldPageStyleName2NewPageStyleName.put(pageDescName, newPageStyleName);
            knownPageStyles.add(newPageStyleName);
          }
          UNO.setProperty(cursor, "PageDescName", newPageStyleName);
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    } while (cursor.gotoNextParagraph(false));
  }

  /**
   * Ersetzt alle TextFields des Typs PageCount in textFields durch den Wert
   * pageCount.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void fixPageCountFields(XEnumerationAccess textFields, int pageCount)
  {
    String pc = "" + pageCount;
    XEnumeration enu = textFields.createEnumeration();
    while (enu.hasMoreElements())
    {
      Object textfield;
      try
      {
        textfield = enu.nextElement();
      }
      catch (Exception e)
      {
        Logger.error(e);
        return;
      }
      // Der eigentlich redundante Test auf das Property NumberingType ist eine
      // Optimierung, da supportsService sehr langsam ist.
      if (UNO.getProperty(textfield, "NumberingType") != null
        && UNO.supportsService(textfield, "com.sun.star.text.textfield.PageCount"))
      {
        XTextRange range = UNO.XTextContent(textfield).getAnchor();
        XTextCursor cursor =
          range.getText().createTextCursorByRange(range.getStart());
        cursor.setString(pc);
        TextDocument.copyDirectValueCharAttributes(UNO.XPropertyState(range),
          UNO.XPropertySet(cursor));
        range.setString("");
      }
    }
  }

  /**
   * Ersetzt alle vom WollMux-Seriendruck verwendeten Textfelder vom Typ
   * c,s,s,t,textfield,InputUser durch ihren Stringwert. Diese Ersetzung ist
   * notwendig, da InputUser-Felder als Spezialfelder (z.B. Wenn...Dann...Sonst...)
   * verwendet werden und sie dokumentglobal nur den selben Wert haben können.
   * 
   * Felder vom Typ c.s.s.t.textfield.User verwenden ebenfalls einen dokumentglobalen
   * Textfieldmaster, müssen aber nicht durch die textuelle Repräsentation ersetzt
   * werden, da sich mit dem Seriendruck nur die durch WollMux gesetzten
   * Textfieldmaster ändern können und es kein Szenario gibt, mit dem davon abhängige
   * User-Felder in ein Dokument eingefügt werden können (Es kann über die OOo-GUI
   * kein User-Feld auf "WM(Function 'Autofunction....')" eingefügt werden, da der
   * Name ein Leerzeichen enthält.
   * 
   * Der Fix wurde in der Vergangenheit auf alle Textfelder des Dokuments angewandt,
   * womit aber PageNumber-Felder in Kopf- und Fußzeilen unbrauchbar wurden. Daher
   * gibt es jetzt nur noch eine "Whitelist" von Feldern, die ersetzt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10), Christoph Lutz (D-III-ITD D.10)
   * @throws WrappedTargetException
   * @throws NoSuchElementException
   */
  private static void fixInputUserFields(XEnumerationAccess textFields)
  {
    XEnumeration enu = textFields.createEnumeration();
    while (enu.hasMoreElements())
    {
      Object textfield;
      try
      {
        textfield = enu.nextElement();
      }
      catch (Exception e)
      {
        Logger.error(e);
        return;
      }

      // Der eigentlich Test, ob der Inhalt des Content-Properties mit "WM(FUNCTION"
      // beginnt ist eine Optimierung, da in der Regel nur die betroffenen
      // InputUser-Textfelder mit diesem Text anfangen und supportsService sehr
      // langsam ist.
      String content = null;
      try
      {
        content = AnyConverter.toString(UNO.getProperty(textfield, "Content"));
      }
      catch (Exception e)
      {}
      if (content != null && content.startsWith("WM(FUNCTION")
        && UNO.supportsService(textfield, "com.sun.star.text.TextField.InputUser"))
      {
        XTextRange range = UNO.XTextContent(textfield).getAnchor();
        XTextCursor cursor =
          range.getText().createTextCursorByRange(range.getStart());
        cursor.setString(cursor.getString());
        TextDocument.copyDirectValueCharAttributes(UNO.XPropertyState(range),
          UNO.XPropertySet(cursor));
        range.setString("");
      }
    }
  }

  /**
   * Addiert auf die AnchorPageNo Property aller Objekte aus objects, die nicht (als
   * HashableComponent) in old enthalten sind den Wert pageNumberOffset.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void fixPageAnchoredObjects(XIndexAccess objects,
      int pageNumberOffset)
  {
    int count = objects.getCount();
    for (int i = 0; i < count; ++i)
    {
      try
      {
        Object ob = objects.getByIndex(i);
        String name = "<Unknown>";
        if (WollMuxFiles.isDebugMode())
        {
          XNamed named = UNO.XNamed(ob);
          if (named != null) name = named.getName();
        }
        if (TextContentAnchorType.AT_PAGE.equals(UNO.getProperty(ob, "AnchorType")))
        {
          int oldPageNo = ((Number) UNO.getProperty(ob, "AnchorPageNo")).intValue();
          int newPageNo = oldPageNo + pageNumberOffset;
          Logger.debug2(L.m("Verschiebe \"%1\" von Seite %2 nach Seite %3", name,
            oldPageNo, newPageNo));
          Object afterMovePageNo =
            UNO.setProperty(ob, "AnchorPageNo", Short.valueOf((short) newPageNo));
          if (null == afterMovePageNo
            || ((Number) afterMovePageNo).intValue() != newPageNo)
          {
            Logger.error(L.m(
              "Kann AnchorPageNo von Objekt #\"%1\" nicht auf %2 setzen", i,
              newPageNo));
          }
        }
        else
        {
          Logger.debug2(L.m(
            "Verschiebe \"%1\" NICHT, weil zwar neu dazugekommen, aber nicht an der Seite verankert",
            name));
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Speichert inputDoc in einer temporären Datei und liefert eine UNO-taugliche URL
   * zu dieser Datei zurück.
   * 
   * @param inputDoc
   *          das zu speichernde Dokument
   * @param dest
   *          Muss ein 2-elementiges Array sein. dest[0] wird auf ein neu angelegtes
   *          temporäres Verzeichnis gesetzt, temp[1] auf die Datei darin, in der das
   *          Dok. gespeichert wurde.
   * @throws IOException
   *           falls was schief geht.
   * @throws MalformedURLException
   *           kann eigentlich nicht passieren
   * @throws com.sun.star.io.IOException
   *           falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static String storeInTemporaryFile(XTextDocument inputDoc, File[] dest)
      throws IOException, MalformedURLException, com.sun.star.io.IOException
  {
    /**
     * Zuerst inputDoc in eine temporäre Datei schreiben
     */
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    if (!tmpDir.isDirectory() && !tmpDir.canWrite())
    {
      throw new IOException(
        L.m(
          "Temporäres Verzeichnis\n\"%1\"\nexistiert nicht oder kann nicht beschrieben werden!",
          tmpDir.getPath()));
    }

    for (int i = 0; i < 1000; ++i)
    {
      dest[0] = new File(tmpDir, TEMP_DIR_PREFIX + i);
      if (dest[0].mkdir())
        break;
      else
        dest[0] = null;
    }

    if (dest[0] == null)
    {
      throw new IOException(
        L.m("Konnte kein temporäres Verzeichnis für die temporären Seriendruckdaten anlegen!"));
    }

    dest[1] = new File(dest[0], TEMP_FILE_NAME);
    String url =
      UNO.getParsedUNOUrl(dest[1].toURI().toURL().toExternalForm()).Complete;

    UnoProps arguments = new UnoProps();
    arguments.setPropertyValue("Overwrite", Boolean.FALSE);
    // FilterName setzen auskommentiert, damit OOo automatisch den besten Filter
    // wählt
    arguments.setPropertyValue("FilterName", "writer8"); // found in
    // /opt/openoffice.org/share/registry/modules/org/openoffice/TypeDetection/Filter/fcfg_writer_filters.xcu
    UNO.XStorable(inputDoc).storeToURL(url, arguments.getProps());
    return url;
  }

  /**
   * Liefert true gdw es sich bei dem in url beschriebenen File um eine temporäre
   * Datei der PrintIntoFile-Druckfunktion (des WollMux-Seriendrucks) handelt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean isTempFile(String url)
  {
    boolean b =
      url.matches(".*/" + TEMP_DIR_PREFIX + "\\d+/" + TEMP_FILE_NAME + "/?$");
    return b;
  }

  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    Logger.init(Logger.ALL);

    final boolean[] done = new boolean[] { false };
    SwingUtilities.invokeAndWait(new Runnable()
    {
      public void run()
      {
        final XTextDocument[] doc = new XTextDocument[] { null };
        final Object[] knownPageStyles = new Object[] { null };
        final boolean[] firstAppend = new boolean[] { true };

        JFrame myFrame = new JFrame("PrintIntoFile");
        myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        myFrame.addWindowListener(new WindowAdapter()
        {
          public void windowClosing(WindowEvent e)
          {
            try
            {
              UNO.XCloseable(doc[0]).close(true);
            }
            catch (Exception x)
            {}
            synchronized (done)
            {
              done[0] = true;
              done.notifyAll();
            }
          }
        });

        myFrame.setLayout(new GridLayout(1, 2));
        JButton button = new JButton("Neues Gesamtdokument");
        button.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (doc[0] != null)
            {
              try
              {
                UNO.XCloseable(doc[0]).close(true);
              }
              catch (Exception x)
              {}
              doc[0] = null;
              knownPageStyles[0] = null;
            }
            firstAppend[0] = true;
            try
            {
              doc[0] =
                UNO.XTextDocument(UNO.loadComponentFromURL(
                  "private:factory/swriter", true, true));
              knownPageStyles[0] = new HashSet<String>();
            }
            catch (Exception x)
            {}
          }
        });
        myFrame.add(button);
        button = new JButton("Dokument anhängen");
        button.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (doc[0] == null) return;

            /*
             * Wenn das aktuelle Vordergrunddok ein Textdokument ist und nicht das
             * Gesamtdokument, so wähle es aus.
             */
            XTextDocument inputDoc =
              UNO.XTextDocument(UNO.desktop.getCurrentComponent());
            if (inputDoc == null || UnoRuntime.areSame(inputDoc, doc[0]))
            {
              /*
               * Ansonsten suchen wir, ob wir ein Textdokument finden, das nicht das
               * Gesamtdokument ist.
               */
              try
              {
                XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
                while (xenu.hasMoreElements())
                {
                  inputDoc = UNO.XTextDocument(xenu.nextElement());
                  if (inputDoc != null && !UnoRuntime.areSame(inputDoc, doc[0]))
                    break;
                }
              }
              catch (Exception x)
              {}
            }

            /*
             * Falls wir keinen andere Kandidaten gefunden haben, so will der
             * Benutzer wohl das Gesamtdokument an sich selbst anhängen.
             */
            if (inputDoc == null) inputDoc = doc[0];

            @SuppressWarnings("unchecked")
            HashSet<String> kps = (HashSet<String>) knownPageStyles[0];
            appendToFile(doc[0], inputDoc, kps, firstAppend[0]);
            firstAppend[0] = false;
          }
        });
        myFrame.add(button);

        myFrame.setAlwaysOnTop(true);
        myFrame.pack();
        int frameWidth = myFrame.getWidth();
        int frameHeight = myFrame.getHeight();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - frameWidth / 2;
        int y = screenSize.height / 2 - frameHeight / 2;
        myFrame.setLocation(x, y);
        myFrame.setResizable(false);
        myFrame.setVisible(true);
      }
    });

    synchronized (done)
    {
      while (!done[0])
        done.wait();
    }

    System.exit(0);
  }
}
