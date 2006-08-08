/*
 * Dateiname: Bookmark.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repräsentiert ein Bookmark in OOo und bietet Methoden
 *            für den vereinfachten Zugriff und die Manipulation von Bookmarks an.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 17.05.2006 | LUT | Dokumentation ergänzt
 * 07.08.2006 | BNK | +Bookmark(XNamed bookmark, XTextDocument doc)
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UnoService;

/**
 * Diese Klasse repräsentiert ein Bookmark in OOo und bietet Methoden für den
 * vereinfachten Zugriff und die Manipulation von Bookmarks an.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class Bookmark
{
  /**
   * Enthält den Namen des Bookmarks
   */
  private String name;

  /**
   * Enthält den UnoService des Dokuments dem das Bookmark zugeordnet ist.
   */
  private UnoService document;

  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc
   * bestehenden Bookmarks mit dem Namen name zurück; ist das Bookmark im
   * angebegenen Dokument nicht enthalten, so wird eine NoSuchElementException
   * zurückgegeben.
   * 
   * @param name
   *          Der Name des bereits im Dokument vorhandenen Bookmarks.
   * @param doc
   *          Das Dokument, welches Das Bookmark name enthält.
   * @throws NoSuchElementException
   *           Das Bookmark name ist im angegebenen Dokument nicht enthalten.
   */
  public Bookmark(String name, XBookmarksSupplier doc)
      throws NoSuchElementException
  {
    this.document = new UnoService(doc);
    this.name = name;
    UnoService bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() == null)
      throw new NoSuchElementException("Bookmark \""
                                       + name
                                       + "\" existiert nicht.");
  }
  
  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc
   * bestehenden Bookmarks bookmark zurück.
   */
  public Bookmark(XNamed bookmark, XTextDocument doc)
  {
    this.document = new UnoService(doc);
    this.name = bookmark.getName();
  }

  /**
   * Der Konstruktor erzeugt ein neues Bookmark name im Dokument doc an der
   * Position, die durch range beschrieben ist.
   * 
   * @param name
   *          Der Name des neu zu erstellenden Bookmarks.
   * @param doc
   *          Das Dokument, welches das Bookmark name enthalten soll.
   * @param range
   *          Die Position, an der das Dokument liegen soll.
   */
  public Bookmark(String name, XTextDocument doc, XTextRange range)
  {
    this.document = new UnoService(doc);
    this.name = name;

    // Bookmark-Service erzeugen
    UnoService bookmark = new UnoService(null);
    try
    {
      bookmark = document.create("com.sun.star.text.Bookmark");
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // Namen setzen
    if (bookmark.xNamed() != null)
    {
      bookmark.xNamed().setName(name);
    }

    // Bookmark ins Dokument einfügen
    if (document.xTextDocument() != null
        && bookmark.xTextContent() != null
        && range != null)
    {
      try
      {
        // der TextCursor ist erforderlich, damit auch Bookmarks mit Ausdehnung
        // erfolgreich gesetzt werden können. Das geht mit normalen TextRanges
        // nicht.
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        range.getText()
            .insertTextContent(cursor, bookmark.xTextContent(), true);
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Vor jedem Zugriff auf den BookmarkService bookmark sollte der Service neu
   * geholt werden, damit auch der Fall behandelt wird, dass das Bookmark
   * inzwischen vom Anwender gelöscht wurde. Ist das Bookmark nicht (mehr) im
   * Dokument vorhanden, so wird ein new UnoService(null) zurückgeliefert,
   * welches leichter verarbeitet werden kann.
   * 
   * @param name
   *          Der Name des bereits im Dokument vorhandenen Bookmarks.
   * @param document
   *          Das Dokument, welches Das Bookmark name enthält.
   * @return Den UnoService des Bookmarks name im Dokument document.
   */
  private static UnoService getBookmarkService(String name, UnoService document)
  {
    if (document.xBookmarksSupplier() != null)
    {
      try
      {
        return new UnoService(document.xBookmarksSupplier().getBookmarks()
            .getByName(name));
      }
      catch (WrappedTargetException e)
      {
        Logger.error(e);
      }
      catch (NoSuchElementException e)
      {
      }
    }
    return new UnoService(null);
  }

  /**
   * Diese Methode liefert den (aktuellen) Namen des Bookmarks als String
   * zurück.
   * 
   * @return liefert den (aktuellen) Namen des Bookmarks als String zurück.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Diese Methode liefert eine String-Repräsentation mit dem Aufbau "Bookmark[<name>]"
   * zurück.
   */
  public String toString()
  {
    return "Bookmark[" + getName() + "]";
  }

  /**
   * Diese Methode liefert das Dokument zu dem das Bookmark gehört.
   */
  public XTextDocument getDocument()
  {
    return document.xTextDocument();
  }

  /**
   * Diese Methode vergleicht zwei Bookmarks und liefert ihre Relation zurück.
   * Folgende Rückgabewerte sind möglich: POS_BBAA = B kommt vor A; POS_BB88 = B
   * startet vor A, aber endet gemeinsam mit A; POS_B88B = B enthält A, beginnt
   * und endet aber nicht mit A; POS_88AA = B startet mit A und endet vor A;
   * POS_8888 = A und B starten und enden gleich; POS_88BB = A und B starten
   * gleichzeitig, aber A endet vor B; POS_A88A = A enthält B, beginnt und endet
   * jedoch nicht mit B; POS_AA88 = A startet vor B, aber endet mit B; POS_AABB =
   * A kommt vor B; Im Fehlerfall (wenn z.B. einer der beiden Bookmarks nicht
   * (mehr) im Dokument vorhanden ist), wird POS_AABB zurückgeliefert - es wird
   * also so getan, als käme B nach A.
   * 
   * @param b
   *          Das Bookmark B, das mit this (Bookmark A) verglichen werden soll.
   * @return Die Relation der beiden Bookmark in Form einer Konstante
   *         Bookmark.POS_XXX (siehe Beschreibung)
   */
  public int compare(Bookmark b)
  {
    Logger.debug2("compare: " + this + " <--> " + b);
    return compareTextRanges(this.getTextRange(), b.getTextRange());
  }

  // Positionsangaben als Rückgabewerte von compareTextRanges
  // Fälle: A:=a alleine, 8:=Überlagerung von a und b, B:=b alleine

  /**
   * Das Bookmark B tritt im Dokument vor dem Bookmark A auf.
   */
  public static final int POS_BBAA = -4;

  /**
   * Das Bookmark B startet vor dem Bookmark A, aber hört gleichzeitig mit A
   * auf.
   */
  public static final int POS_BB88 = -3;

  /**
   * Das Bookmark B enthält das Bookmark A vollständig.
   */
  public static final int POS_B88B = -2;

  /**
   * Das Bookmark B startet mit dem Bookmark A, hört jedoch vor dem Bookmark A
   * auf.
   */
  public static final int POS_88AA = -1;

  /**
   * A und B liegen an der selben Position.
   */
  public static final int POS_8888 = -0;

  /**
   * Das Bookmark A startet mit dem Bookmark B, hört jedoch vor dem Bookmark B
   * auf.
   */
  public static final int POS_88BB = 1;

  /**
   * Das Bookmark A enthält das Bookmark B vollständig.
   */
  public static final int POS_A88A = 2;

  /**
   * Das Bookmark A startet vor dem Bookmark B, hört jedoch gemeinsam mit dem
   * Bookmark B auf.
   */
  public static final int POS_AA88 = 3;

  /**
   * Das Bookmark A liegt im Dokument vor dem Bookmark B.
   */
  public static final int POS_AABB = 4;

  /**
   * Diese Methode vergleicht die beiden TextRanges a und b und liefert ihre
   * Relation in Form der Konstanten Bookmark.POS_xxx zurück.
   * 
   * @param a
   * @param b
   * @return Die Relation von a und b in Form einer Konstanten Bookmark.POS_xxx.
   */
  private static int compareTextRanges(XTextRange a, XTextRange b)
  {
    // Fälle: A:=a alleine, 8:=Überlagerung von a und b, B:=b alleine
    // -4 = BBBBAAAA bzw. BB88AA
    // -3 = BB88
    // -2 = B88B
    // -1 = 88AA
    // +0 = 8888
    // +1 = 88BB
    // +2 = A88A
    // +3 = AA88
    // +4 = AAAABBBB bzw. AA88BB

    XTextRangeCompare compare = null;
    if (a != null)
      compare = (XTextRangeCompare) UnoRuntime.queryInterface(
          XTextRangeCompare.class,
          a.getText());
    if (compare != null && a != null && b != null)
    {
      try
      {
        int start = compare.compareRegionStarts(a, b) + 1;
        int end = compare.compareRegionEnds(a, b) + 1;
        return (3 * start + 1 * end) - 4;
      }
      catch (IllegalArgumentException e)
      {
        // nicht loggen! Tritt regulär auf, wenn Bookmarks aus unterschiedlichen
        // Frames verglichen werden.
      }
    }
    // Im Fehlerfall wird so getan als käme B nach A
    return POS_AABB;
  }

  /**
   * Diese Methode benennt das Bookmark oldName zu dem Namen newName um. Ist der
   * Name bereits definiert, so hängt OOo an den Namen automatisch eine Nummer
   * an. Die Methode gibt den tatsächlich erzeugten Bookmarknamen zurück.
   * 
   * @param newName
   * @return den tatsächlich erzeugten Namen des Bookmarks.
   * @throws Exception
   */
  public String rename(String newName)
  {
    Logger.debug("Rename \"" + name + "\" --> \"" + newName + "\"");

    XTextRange oldRange = getTextRange();
    if (oldRange != null)
    {
      // altes Bookmark löschen:
      remove();

      // neues Bookmark mit neuem Namen hinzufügen.
      UnoService bookmark = new UnoService(null);
      try
      {
        bookmark = document.create("com.sun.star.text.Bookmark");
      }
      catch (Exception e)
      {
        Logger.error(e);
      }

      if (bookmark.xNamed() != null)
      {
        bookmark.xNamed().setName(newName);
      }
      try
      {
        oldRange.getText().insertTextContent(
            oldRange,
            bookmark.xTextContent(),
            true);
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
      // Nach dem Einfügen den neuen Namen holen, da OOo einen Suffix an den
      // Namen anhängt, wenn der selbe Name bereits vergeben ist.
      if (bookmark.xNamed() != null)
      {
        name = bookmark.xNamed().getName();
      }
    }
    return name;
  }

  /**
   * Diese Methode weist dem Bookmark einen neuen TextRange (als Anchor) zu.
   * 
   * @param xTextRange
   *          Der neue TextRange des Bookmarks.
   */
  public void rerangeBookmark(XTextRange xTextRange)
  {
    // altes Bookmark löschen.
    remove();

    // neues Bookmark unter dem alten Namen mit neuer Ausdehnung hinzufügen.
    try
    {
      UnoService bookmark = document.create("com.sun.star.text.Bookmark");
      bookmark.xNamed().setName(name);
      xTextRange.getText().insertTextContent(
          xTextRange,
          bookmark.xTextContent(),
          true);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Die Methode gibt die XTextRange des Bookmarks zurück, oder null, falls das
   * Bookmark nicht vorhanden ist (z,B, weil es inzwischen gelöscht wurde).
   * 
   * @return
   */
  public XTextRange getTextRange()
  {
    UnoService bookmark = getBookmarkService(name, document);

    if (bookmark.xTextContent() != null)
    {
      XTextRange range = bookmark.xTextContent().getAnchor();

      // Workaround für OOo-Bug: fehlerhafter Anchor bei Bookmarks in Tabellen.
      // http://www.openoffice.org/issues/show_bug.cgi?id=67869 . Ein
      // TextCursor-Objekt verhält sich dahingehend robuster.
      if (range != null)
        range = range.getText().createTextCursorByRange(range);

      return range;
    }
    return null;
  }

  /**
   * Diese Methode löscht das Bookmark aus dem Dokument.
   */
  public void remove()
  {
    UnoService bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() != null)
    {
      try
      {
        XTextRange range = bookmark.xTextContent().getAnchor();
        range.getText().removeTextContent(bookmark.xTextContent());
        bookmark = new UnoService(null);
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }
    }
  }
}
