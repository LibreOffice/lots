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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.HashableComponent;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.ooo.TextDocument;

/**
 * "Druck"funktion, die das zu druckende Dokument an ein Ergebnisdokument anhängt.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PrintIntoFile
{
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void appendToFile(XTextDocument outputDoc, XTextDocument inputDoc,
      boolean firstAppend)
  {
    File[] dest = new File[] {
      null, null }; // dest[0] ist das temp. Verzeichnis, dest[1] die temp. Datei
    // darin
    try
    {
      String url = storeInTemporaryFile(inputDoc, dest);

      String inputDocFirstPageStyleName =
        (String) UNO.getProperty(
          UNO.XEnumerationAccess(inputDoc.getText()).createEnumeration().nextElement(),
          "PageStyleName");

      boolean startsWithSection = startsWithSection(inputDoc);

      XText text = outputDoc.getText();
      if (firstAppend)
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
      else
      // if (!firstAppend)
      {
        // Neuen Absatz am Ende einfügen für den Seitenumbruch
        text.getEnd().setString("\r");
      }

      XTextCursor cursor = text.createTextCursorByRange(text.getEnd());
      if (!(firstAppend && "Standard".equals(inputDocFirstPageStyleName)))
      {
        UNO.setProperty(cursor, "PageDescName", inputDocFirstPageStyleName);
      }

      if (!firstAppend)
      {
        /**
         * Format-->Absatz-->Textfluss-->Umbrüche--> Checkbox "mit Seitenvorlage" -->
         * Seitennummer 1 (Seitennummer mit 1 beginnen nach dem Seitenumbruch)
         * ACHTUNG! OOo lässt ungerade Seitennummern nur auf wirklich ungeraden
         * Seiten zu. Ist die betreffende Seite eine gerade Seite, so wird durch das
         * Setzen von PageNumberOffset auf 1 eine leere Seite davor eingefügt! Siehe
         * dazu auch den Kommentar weiter unten bei pageNumberOffset
         */
        UNO.setProperty(cursor, "PageNumberOffset", Short.valueOf((short) 1));
      }

      /**
       * OOo lässt nicht zu, dass eine Seite mit gerader realer Nummer eine ungerade
       * Seitenummer bekommt (durch einen Seitenumbruch mit gesetztem
       * PageNumberOffset (wie wir ihn ein Stück weiter oben eingefügt haben). Wird
       * dies doch getan, dann fügt OOo eine leere Seite ein, die zwar gezählt, aber
       * nicht angezeigt wird. Es ist also wichtig das pageNumberOffset so wie hier
       * nach dem Einfügen des Seitenumbruchs zu berechnen, weil der eine eingefügte
       * Seitenumbruch evtl. die Seitenanzahl um 2 steigen lässt.
       */
      int pageNumberOffset =
        ((Number) UNO.getProperty(outputDoc.getCurrentController(), "PageCount")).intValue();
      --pageNumberOffset; // Die gerade eben angehängte Seite darf nicht mitgezählt
      // werden.

      // String[] frameNames =
      // UNO.XTextFramesSupplier(outputDoc).getTextFrames().getElementNames();
      // String[] imageNames =
      // UNO.XTextGraphicObjectsSupplier(outputDoc).getGraphicObjects().getElementNames();
      XIndexAccess shapes =
        UNO.XIndexAccess(UNO.XDrawPageSupplier(outputDoc).getDrawPage());
      Set<HashableComponent> oldShapes =
        new HashSet<HashableComponent>(shapes.getCount());
      int shapeCount = shapes.getCount();
      for (int i = 0; i < shapeCount; ++i)
        oldShapes.add(new HashableComponent(shapes.getByIndex(i)));

      Set<String> oldSections = new HashSet<String>();
      if (startsWithSection)
      {
        String[] sectionNames =
          UNO.XTextSectionsSupplier(outputDoc).getTextSections().getElementNames();
        oldSections.addAll(Arrays.asList(sectionNames));
      }

      /**
       * Einfügen des 2. Dokuments OOo Issue 37417 beachten --> When inserting a
       * document (via "Insert->Document") on the first paragraph of a page after a
       * pagebreak, and the document contains only one paragraph, the pagebreak will
       * be removed. Inserting documents with more than one paragraph works as
       * expected.
       */
      cursor = text.createTextCursorByRange(text.getEnd());
      Logger.debug2("================= OID dump BEFORE insert ==================");
      dumpOids(outputDoc);
      UNO.XDocumentInsertable(cursor).insertDocumentFromURL(url,
        new PropertyValue[] {});
      Logger.debug2("================= OID dump AFTER insert ==================");
      dumpOids(outputDoc);

      cursor.collapseToStart();

      // Workaround für
      // http://www.openoffice.org/issues/show_bug.cgi?id=73229
      if (startsWithSection
        && !rangeStartTouchesNewSection(cursor, oldSections, outputDoc))
        TextDocument.disappearParagraph(cursor);

      XParagraphCursor paraCursor = UNO.XParagraphCursor(cursor);
      XNameAccess inputDocPageStyles =
        UNO.XNameAccess(UNO.XStyleFamiliesSupplier(inputDoc).getStyleFamilies().getByName(
          "PageStyles"));
      renamePageStyles(paraCursor, outputDoc, inputDocPageStyles);

      if (!firstAppend)
      {
        // fixPageAnchoredObjects(UNO.XTextFramesSupplier(outputDoc).getTextFrames(),
        // frameNames, pageNumberOffset);
        // fixPageAnchoredObjects(UNO.XTextGraphicObjectsSupplier(outputDoc).getGraphicObjects(),
        // imageNames, pageNumberOffset);
        fixPageAnchoredObjects(shapes, oldShapes, pageNumberOffset);
      }

      int pageCount =
        ((Number) UNO.getProperty(outputDoc.getCurrentController(), "PageCount")).intValue();
      pageCount -= pageNumberOffset;
      fixPageCountFields(UNO.XTextFieldsSupplier(outputDoc).getTextFields(),
        pageCount);
      fixInputUserFields(UNO.XTextFieldsSupplier(outputDoc).getTextFields());

      oldShapes = null;
      System.gc();
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
   * Liefert true gdw der Anfang von range mit dem Anfang einer Section aus doc
   * zusammenfällt, deren Name nicht in oldSections ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static boolean rangeStartTouchesNewSection(XTextRange range,
      Set<String> oldSectionNames, XTextDocument doc)
  {
    XTextRange docText = doc.getText();
    XTextRangeCompare compare = UNO.XTextRangeCompare(docText);
    XNameAccess sections = UNO.XTextSectionsSupplier(doc).getTextSections();
    String[] names = sections.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      if (!oldSectionNames.contains(names[i]))
      {
        try
        {
          XTextRange sectionRange =
            UNO.XTextContent(sections.getByName(names[i])).getAnchor();
          if (compare.compareRegionStarts(range, sectionRange) == 0) return true;
        }
        catch (Exception x)
        {
          // keine Logger-Meldung. Dies tritt regulär auf bei Bereichen, die in
          // anderen
          // Rahmen liegen und daher nicht mit einem Cursor im Dokumenthaupttext
          // vergleichbar sind.
        }
      }
    }
    return false;
  }

  /**
   * Liefert true gdw der Start von doc mit dem Starter einer Section von doc
   * zusammenfällt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static boolean startsWithSection(XTextDocument doc)
  {
    XTextRange docText = doc.getText();
    XTextRangeCompare compare = UNO.XTextRangeCompare(docText);
    XNameAccess sections = UNO.XTextSectionsSupplier(doc).getTextSections();
    String[] names = sections.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        XTextRange range =
          UNO.XTextContent(sections.getByName(names[i])).getAnchor();
        if (compare.compareRegionStarts(docText, range) == 0) return true;
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
    return false;
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
   * @param oldPageStyles
   *          die PageStyles Familie des alten Dokuments
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void renamePageStyles(XParagraphCursor cursor, XTextDocument doc,
      XNameAccess oldPageStyles)
  {
    XNameAccess newPageStyles;
    try
    {
      newPageStyles =
        UNO.XNameAccess(UNO.XStyleFamiliesSupplier(doc).getStyleFamilies().getByName(
          "PageStyles"));
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }
    Map<String, String> mapOldPageStyleName2NewPageStyleName =
      new HashMap<String, String>();
    while (true)
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
              UNO.XPropertySet(oldPageStyles.getByName(pageDescName));
            do
            {
              newPageStyleName = pageDescName + (int) (Math.random() * 1000000.0);
            } while (newPageStyles.hasByName(newPageStyleName));
            TextDocument.copyPageStyle(doc, oldStyle, newPageStyleName);
            mapOldPageStyleName2NewPageStyleName.put(pageDescName, newPageStyleName);
          }
          UNO.setProperty(cursor, "PageDescName", newPageStyleName);
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      if (!cursor.gotoNextParagraph(false)) break;
    }
  }

  /**
   * Gibt die OIDs aller Shapes von outputDoc aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void dumpOids(XTextDocument outputDoc)
  {
    if (!WollMuxFiles.isDebugMode()) return;
    XIndexAccess shapes =
      UNO.XIndexAccess(UNO.XDrawPageSupplier(outputDoc).getDrawPage());
    int shapeCount = shapes.getCount();
    try
    {
      for (int i = 0; i < shapeCount; ++i)
      {
        Object ob = shapes.getByIndex(i);
        XNamed named = UNO.XNamed(ob);
        String name = "<Unknown>";
        if (named != null) name = named.getName();
        Logger.debug2(name + " -> " + UnoRuntime.generateOid(ob));
      }
    }
    catch (Exception x)
    {
      Logger.debug2(x);
    }
  }

  /**
   * Ersetzt alle TextFields des Typs PageCount in textFields durch den Wert
   * pageCount.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws WrappedTargetException
   * @throws NoSuchElementException
   *           sollte nie geworfen werden
   */
  private static void fixPageCountFields(XEnumerationAccess textFields, int pageCount)
      throws NoSuchElementException, WrappedTargetException
  {
    String pc = "" + pageCount;
    XEnumeration enu = textFields.createEnumeration();
    while (enu.hasMoreElements())
    {
      Object textfield = enu.nextElement();
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
   * Ersetzt alle Textfelder vom Typ c,s,s,t,textfield,InputUser durch ihren
   * Stringwert. Diese Ersetzung ist notwendig, da InputUser-Felder als
   * Seriendruck-Spezialfelder (z.B. Wenn...Dann...Sonst...) verwendet werden und sie
   * dokumentglobal nur den selben Wert haben können. Der Fix wurde in der
   * Vergangenheit auf alle Textfelder des Dokuments angewandt, womit aber
   * PageNumber-Felder (Gesamtseitenzahl) unbrauchbar wurden.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10), Christoph Lutz (D-III-ITD D.10)
   * @throws WrappedTargetException
   * @throws NoSuchElementException
   */
  private static void fixInputUserFields(XEnumerationAccess textFields)
      throws NoSuchElementException, WrappedTargetException
  {
    XEnumeration enu = textFields.createEnumeration();
    while (enu.hasMoreElements())
    {
      Object textfield = enu.nextElement();

      // Der eigentlich Test, ob der Inhalt des Content-Properties mit "WM(FUNCTION"
      // beginnt ist eine Optimierung, da in der Regel nur die betroffenen
      // InputUser-Textfelder mit diesem Text anfangen und supportsService sehr
      // langsam ist.
      String content = null;
      try
      {
        content = AnyConverter.toString(UNO.getProperty(textfield, "Content"));
      }
      catch (IllegalArgumentException e)
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
      Set<HashableComponent> old, int pageNumberOffset)
  {
    int count = objects.getCount();
    for (int i = 0; i < count; ++i)
    {
      try
      {
        Object ob = objects.getByIndex(i);
        XNamed named = UNO.XNamed(ob);
        String name = "<Unknown>";
        if (named != null) name = named.getName();
        if (!old.contains(new HashableComponent(ob)))
        {
          if (TextContentAnchorType.AT_PAGE.equals(UNO.getProperty(ob, "AnchorType")))
          {
            int oldPageNo =
              ((Number) UNO.getProperty(ob, "AnchorPageNo")).intValue();
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
        else
        {
          Logger.debug2(L.m("Verschiebe \"%1\" NICHT, weil nicht neu dazugekommen",
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

    dest[1] = new File(dest[0], "serienbrief.odt");
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
            }
            firstAppend[0] = true;
            try
            {
              doc[0] =
                UNO.XTextDocument(UNO.loadComponentFromURL(
                  "private:factory/swriter", true, true));
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

            appendToFile(doc[0], inputDoc, firstAppend[0]);
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
