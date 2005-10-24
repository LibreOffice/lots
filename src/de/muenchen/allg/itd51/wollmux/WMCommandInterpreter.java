/*
 * Dateiname: WMCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Scannt alle Bookmarks eines Dokuments und interpretiert ggf. die 
 *            WM-Kommandos.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WMCommandInterpreter
{

  /**
   * Das Dokument, das interpretiert werden soll.
   */
  private UnoService document;

  /**
   * URLs werden relativ zum documentCtx aufgelöst.
   */
  private URL documentCtx;

  /**
   * Abbruchwert für die Anzahl der Interationsschritte beim Auswerten neu
   * hinzugekommener Bookmarks.
   */
  private static final int MAXCOUNT = 100;

  /**
   * Verstecktes Trennzeichen, das zum Beginn und zum Ende eines Textfragments
   * eingefügt wird um verschachtelte WollMux-Kommandos zu ermöglichen.
   */
  private static final String FRAGMENT_MARK = " X ";

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Kommandos
   * im übergebenen xDoc scannt und entsprechend auflöst. Wird der Interpreter
   * über diesen Konstruktor erzeugt, können keine relativen URLs aufgelöst
   * werden.
   * 
   * @param xDoc
   */
  public WMCommandInterpreter(XTextDocument xDoc)
  {
    this.document = new UnoService(xDoc);
    this.documentCtx = null;
  }

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Kommandos
   * im übergebenen xDoc scannt und entsprechend auflöst. Der documentCtx
   * beschreibt die Original-Vorlage, aus der das zu bearbeitende Dokument
   * abgeleitet wurde. Alle relativen Pfadangaben (derzeit in
   * include-Anweisungen oder bei Textfragment-URLs) können aufgelöst werden und
   * beziehen sich auf die URL der original-Vorlage.
   * 
   * @param xDoc
   * @param documentCtx
   */
  public WMCommandInterpreter(XTextDocument xDoc, URL documentCtx)
  {
    this.document = new UnoService(xDoc);
    this.documentCtx = documentCtx;
  }

  /**
   * Über diese Methode wird die eigentliche Ausführung der Interpretation der
   * WM-Kommandos gestartet. Ein WM-Kommando besitzt sie Syntax "WM (
   * Unterkommando ){Zahl}", wobei Spaces an jeder Stelle auftauchen oder
   * weggelassen werden dürfen. Der Wert {Zahl} am Ende des Kommandos dient zur
   * Unterscheidung verschiedener Bookmarks in OOo und ist optional.
   * 
   * Alle Bookmarks, die nicht dieser Syntax entsprechen, werden als normale
   * Bookmarks behandelt und nicht vom Interpreter bearbeitet.
   */
  public void interpret()
  {
    // Die Sichtbare Darstellung in OOo abschalten:
    // document.xModel().lockControllers();
    // Wurde auskommentiert, da der Aufbau zu langsam ist und die arbeitsweise
    // transparenter ist, wenn der Benutzer sieht dass sich was tut...

    // Bereits abgearbeitete Bookmarks merken.
    HashMap evaluatedBookmarks = new HashMap();

    // Folgendes Pattern prüft ob es sich bei dem Bookmark um ein gültiges
    // WM-Kommando handelt und entfernt evtl. vorhandene Zahlen-Prefixe.
    Pattern wmCmd = Pattern
        .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*\\d*\\z");

    // Solange durch die ständig neu erzeugte Liste aller Bookmarks gehen, bis
    // alle Bookmarks ausgewertet wurden oder die Abbruchbedingung zur
    // Vermeindung von Endlosschleifen erfüllt ist.
    boolean changed = true;
    for (int count = 0; changed && count < MAXCOUNT; ++count)
    {
      changed = false;
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      String[] bookmarks = bookmarkAccess.getElementNames();

      // Alle Bookmarks durchlaufen und ggf. die execute-Routine aufrufen.
      for (int i = 0; i < bookmarks.length; i++)
      {
        if (!evaluatedBookmarks.containsKey(bookmarks[i]))
        {
          Logger.debug2("Evaluate Bookmark \"" + bookmarks[i] + "\".");
          changed = true;
          evaluatedBookmarks.put(bookmarks[i], Boolean.TRUE);

          // Bookmark evaluieren.
          Matcher m = wmCmd.matcher(bookmarks[i]);
          if (m.find())
          {
            Logger.debug2("Found WM-Command: " + m.group(1));
            execute(m.group(1), bookmarks[i]);
          }
          else
          {
            Logger.debug2("Normales Bookmark gefunden, kein WM-Kommando.");
          }
        }
      }
    }

    // Lock-Controllers wieder aufheben:
    // document.xModel().unlockControllers();
  }

  /**
   * Generisches Execute eines Wollmux-Kommandos.
   * 
   * @param cmdString
   *          Das WollMux-Kommando, das in dem Bookmark enthalten ist.
   * @param bookmarkName
   *          Der Name des zugehörigen Bookmarks für die weitere Verarbeitung.
   *          Im Fehlerfall wird auf den Bookmarknamen verwiesen.
   */
  private void execute(String cmdString, String bookmarkName)
  {
    try
    {
      ConfigThingy wm = new ConfigThingy("WMCmd", documentCtx,
          new StringReader(cmdString)).get("WM");
      ConfigThingy cmd = wm.get("CMD");

      // insertFrag
      if (cmd.toString().equals("insertFrag"))
      {
        Logger.debug2("Cmd: insertFrag mit FRAG_ID \""
                      + wm.get("FRAG_ID").toString()
                      + "\"");
        executeInsertFrag(wm.get("FRAG_ID").toString(), bookmarkName);
      }

      // insertValue
      else if (cmd.toString().equals("insertValue"))
      {
        Logger.debug2("Cmd: insertValue mit DB_SPALTE \""
                      + wm.get("DB_SPALTE").toString()
                      + "\"");
        executeInsertValue(wm.get("DB_SPALTE").toString(), bookmarkName);
      }

      // unbekanntes Kommando
      else
      {
        String msg = bookmarkName
                     + ": "
                     + "Unbekanntes WollMux-Kommando \""
                     + cmd.toString()
                     + "\"";
        Logger.error(msg);
        fillBookmark(bookmarkName, msg);
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
    }
  }

  /**
   * Diese Methode fügt einen Spaltenwert aus dem aktuellen Datensatz ein. Im
   * Fehlerfall wird die Fehlermeldung eingefügt.
   * 
   * @param spaltenname
   *          Name der Datenbankspalte
   * @param bookmarkName
   *          Name des Bookmarks in das der Wert eingefügt werden soll.
   */
  private void executeInsertValue(String spaltenname, String bookmarkName)
  {
    try
    {
      Dataset ds = WollMux.getDatasourceJoiner().getSelectedDataset();
      fillBookmark(bookmarkName, ds.get(spaltenname));
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
    }
  }

  /**
   * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
   * bookmarkName ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
   * 
   * @param frag_id
   *          FRAG_ID, des im Abschnitt Textfragmente in der Konfigurationsdatei
   *          definierten Textfragments.
   * @param bookmarkName
   *          Name des bookmarks, in das das Fragment eingefügt werden soll.
   */
  private void executeInsertFrag(String frag_id, String bookmarkName)
  {
    XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
    try
    {
      // Ablauf:
      // "x" ist die explizit sichtbare FRAGMENT_MARK, die später auf
      // Hidden ("h") gesetzt wird.
      // 1) bookmarkCursor = "xx"
      // 2) insertCursor exakt in die Mitte der xx setzen.
      // 3) Inhalt aus Fragmentdatei einfügen in insCursor
      // 4) "x" auf "h" setzen
      // Ergebnis: bookmarCursor = "h<inhalt>h"

      // TextCursor erzeugen, der den gesamten Ersetzungsbereich des Bookmarks
      // umschließt und mit dem Inhalt der beiden FRAGMENT_MARKs vorbelegen.
      UnoService bookmark = new UnoService(bookmarkAccess
          .getByName(bookmarkName));
      UnoService text = new UnoService(document.xTextDocument().getText());
      UnoService bookmarkCursor = new UnoService(text.xText()
          .createTextCursorByRange(bookmark.xTextContent().getAnchor()));
      bookmarkCursor.xTextCursor().setString(FRAGMENT_MARK + FRAGMENT_MARK);
      bookmarkCursor.setPropertyValue("CharHidden", Boolean.FALSE);

      // InsertCurser erzeugen, in den das Textfragment eingefügt wird.
      UnoService insCursor = new UnoService(text.xText()
          .createTextCursorByRange(bookmarkCursor.xTextCursor()));
      insCursor.xTextCursor().goRight((short) FRAGMENT_MARK.length(), false);
      insCursor.xTextCursor().collapseToStart();

      // URL aus Fragmentliste erzeugen.
      String urlStr = WollMux.getTextFragmentList().getURLByID(frag_id);
      if (documentCtx != null)
      {
        // Verwende URL im gegebenen Kontext.
        urlStr = new URL(documentCtx, urlStr).toExternalForm();
      }
      urlStr = unoURL(urlStr);

      Logger.debug("Füge Textfragment \""
                   + frag_id
                   + "\" von URL \""
                   + urlStr
                   + "\" ein.");

      // Textfragment einfügen
      insCursor.xDocumentInsertable().insertDocumentFromURL(
          urlStr,
          new PropertyValue[] {});
      // wird benötigt, damit das erste Element nicht unsichtbar ist...
      insCursor.xTextCursor().collapseToEnd();

      // FRAGMENT_MARKen verstecken:
      UnoService hiddenCursor = new UnoService(text.xText().createTextCursor());
      // start-Marke
      hiddenCursor.xTextCursor().gotoRange(
          bookmarkCursor.xTextRange().getStart(),
          false);
      hiddenCursor.xTextCursor().goRight((short) FRAGMENT_MARK.length(), true);
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);
      // end-Marke
      hiddenCursor.xTextCursor().gotoRange(
          bookmarkCursor.xTextRange().getEnd(),
          false);
      hiddenCursor.xTextCursor().goLeft((short) FRAGMENT_MARK.length(), true);
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);

      // Bookmark an neuen Range anpassen
      reRangeBookmark(bookmarkName, bookmarkCursor.xTextRange());

    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
    }
  }

  /**
   * Diese Methode ordnet dem Bookmark bookmarkName eine neue Range xTextRang
   * zu. Damit kann z.B. ein Bookmark ohne Ausdehnung eine Ausdehnung xTextRange
   * erhalten.
   * 
   * @param bookmarkName
   * @param xTextRange
   * @throws WrappedTargetException
   * @throws NoSuchElementException
   */
  private void reRangeBookmark(String bookmarkName, XTextRange xTextRange)
      throws NoSuchElementException, WrappedTargetException
  {
    // Ein Bookmark ohne Ausdehnung kann nicht einfach nachträglich
    // erweitert werden. Dies geht nur beim Erzeugen und Einfügen mit
    // insertTextContent(...). Um ein solches Bookmark mit einer
    // Ausdehnung versehen zu können, muss es zu erst gelöscht, und
    // anschließend wieder neu erzeugt und mit der Ausdehnung xTextRange
    // eingefügt werden.

    // alten Bookmark löschen.
    UnoService oldBookmark = new UnoService(document.xBookmarksSupplier()
        .getBookmarks().getByName(bookmarkName));

    document.xTextDocument().getText().removeTextContent(
        oldBookmark.xTextContent());

    // neuen Bookmark unter dem alten Namen mit Ausdehnung hinzufügen.
    UnoService newBookmark;
    try
    {
      newBookmark = document.create("com.sun.star.text.Bookmark");
      newBookmark.xNamed().setName(bookmarkName);
      document.xTextDocument().getText().insertTextContent(
          xTextRange,
          newBookmark.xTextContent(),
          true);
    }
    catch (Exception e)
    {
      // Fehler beim Erzeugen des Service Bookmark. Sollt normal nicht
      // passieren.
      Logger.error(e);
      Logger.error("ReRange: Bookmark \""
                   + bookmarkName
                   + "\" konnte nicht neu erzeugt werden und ging "
                   + "verloren.");
    }
  }

  /**
   * Diese Methode füllt ein Bookmark bookmarkName mit einem übergebenen Text
   * text. Das Bookmark umschließt diesen Text hinterher vollständig.
   * 
   * @param bookmarkName
   * @param text
   */
  private void fillBookmark(String bookmarkName, String text)
  {
    XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
    try
    {

      // Textcursor erzeugen und mit dem neuen Text ausdehnen.
      UnoService bookmark = new UnoService(bookmarkAccess
          .getByName(bookmarkName));
      UnoService cursor = new UnoService(document.xTextDocument().getText()
          .createTextCursorByRange(bookmark.xTextContent().getAnchor()));
      cursor.xTextCursor().setString(text);

      // Bookmark an neuen Range anpassen
      reRangeBookmark(bookmarkName, cursor.xTextRange());

    }
    catch (NoSuchElementException e)
    {
      // Dieser Fall kann normalerweise nicht auftreten, da nur Bookmarks
      // verarbeitet werden, die auch wirklich existieren.
      Logger.error(e);
    }
    catch (WrappedTargetException e)
    {
      // interner UNO-Fehler beim Holen des Bookmarks. Sollte
      // normalerweise nicht auftreten.
      Logger.error(e);
    }
  }

  /**
   * Diese Methode erzeugt mit Hilfe des URLTranformers eine URL, die für die
   * Kommandos loadComponentFromURL und insertDocumentFromURL geeignet ist.
   * 
   * @param urlStr
   * @return
   * @throws Exception
   *           Problem bei der Erzeugung der unoURL.
   */
  private static String unoURL(String urlStr) throws Exception
  {
    UnoService trans;
    trans = UnoService.createWithContext(
        "com.sun.star.util.URLTransformer",
        WollMux.getXComponentContext());
    com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
    unoURL[0].Complete = urlStr;
    trans.xURLTransformer().parseStrict(unoURL);
    return unoURL[0].Complete;
  }

  /**
   * Methode zum Testen des WMCommandoInterpreters.
   * 
   * @param args
   */
  public static void main(String[] args)
  {
    try
    {
      if (args.length < 2)
      {
        System.out.println("USAGE: <config_url> <document_url>");
        System.exit(0);
      }
      File cwd = new File(".");

      args[0] = args[0].replaceAll("\\\\", "/");
      args[1] = args[1].replaceAll("\\\\", "/");

      // Remote-Kontext herstellen
      UNO.init();

      // WollMux starten
      new WollMux(UNO.defaultContext);
      WollMux.initialize(System.err, new File(cwd, args[0]));
      WollMux.getInstance().startupWollMux();

      Logger.init(Logger.ALL);

      // Dokument zum Parsen Öffnen
      URL url = new URL(cwd.toURL(), args[1]);
      UNO.loadComponentFromURL(unoURL(url.toExternalForm()), true, false);

      new WMCommandInterpreter(UNO.XTextDocument(UNO.compo), url).interpret();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
    // System.exit(0);
  }
}
