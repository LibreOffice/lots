package de.muenchen.allg.itd51.wollmux;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UnoService;

/**
 * @author lut
 * 
 */
public class Bookmark
{
  private UnoService bookmark;

  private String name;

  private UnoService document;

  public Bookmark(String name, XComponent doc) throws NoSuchElementException
  {
    this.document = new UnoService(doc);
    this.name = name;
    this.bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() == null)
      throw new NoSuchElementException("Bookmark \""
                                       + name
                                       + "\" existiert nicht.");
  }

  /**
   * Vor jedem Zugriff auf den BookmarkService bookmark sollte der Service neu
   * geholt werden, damit auch der Fall behandelt wird, dass das Bookmark
   * inzwischen gelöscht wurde.
   * 
   * @param name
   * @param document
   * @return
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

  public String getName()
  {
    return name;
  }

  public String toString()
  {
    return "Bookmark[" + getName() + "]";
  }

  /**
   * Vergleicht zwei Bookmarks und liefert ihre Relation zurück. Folgende
   * Rückgabewerte sind möglich: POS_BBAA = B kommt vor A; POS_BB88 = B startet
   * vor A, aber endet gemeinsam mit A; POS_B88B = B enthält A, beginnt und
   * endet aber nicht mit A; POS_88AA = B startet mit A und endet vor A;
   * POS_8888 = A und B starten und enden gleich; POS_88BB = A und B starten
   * gleichzeitig, aber A endet vor B; POS_A88A = A enthält B, beginnt und endet
   * jedoch nicht mit B; POS_AA88 = A startet vor B, aber endet mit B; POS_AABB =
   * A kommt vor B; Im Fehlerfall (wenn z.B. einer der beiden Bookmarks nicht
   * (mehr) im Dokument vorhanden ist), wird POS_AABB zurückgeliefert - es wird
   * also so getan, als käme B nach A.
   * 
   * @param b
   * @return
   */
  public int compare(Bookmark b)
  {
    return compareTextRanges(this.getTextRange(), b.getTextRange());
  }

  // Positionsangaben als Rückgabewerte von compareTextRanges
  // Fälle: A:=a alleine, 8:=Überlagerung von a und b, B:=b alleine

  public static final int POS_BBAA = -4;

  public static final int POS_BB88 = -3;

  public static final int POS_B88B = -2;

  public static final int POS_88AA = -1;

  public static final int POS_8888 = -0;

  public static final int POS_88BB = 1;

  public static final int POS_A88A = 2;

  public static final int POS_AA88 = 3;

  public static final int POS_AABB = 4;

  public static int compareTextRanges(XTextRange a, XTextRange b)
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

    XTextRangeCompare compare = (XTextRangeCompare) UnoRuntime.queryInterface(
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
        Logger.error(e);
      }
    }
    // Im Fehlerfall wird so getan als käme B nach A
    return POS_AABB;
  }

  /**
   * Diese Methode benennt das Bookmark oldName zu dem Namen newName um. Ist der
   * Name bereits definiert, so hängt OpenOffice an den Namen automatisch eine
   * Nummer an. Die Methode gibt den tatsächlich erzeugten Bookmarknamen zurück.
   * 
   * @param newName
   * @return den tatsächlich erzeugten Namen des Bookmarks.
   * @throws Exception
   */
  public String rename(String newName)
  {
    Logger.debug("Rename \"" + name + "\" --> \"" + newName + "\"");

    // altes Bookmark löschen.
    XTextRange range = getTextRange(); // holt auch den BookmarkService neu!
    if (range != null && bookmark.xTextContent() != null)
    {
      try
      {
        range.getText().removeTextContent(bookmark.xTextContent());
        bookmark = new UnoService(null);
        name = null;
      }
      catch (NoSuchElementException e)
      {
        // wenn' schon weg ist, ist es mir auch recht.
      }

      // neuen Bookmark mit neuem Namen hinzufügen.
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
        name = bookmark.xNamed().getName();
      }
      try
      {
        range.getText().insertTextContent(range, bookmark.xTextContent(), true);
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
    }
    return name;
  }

  public void rerangeBookmark(XTextRange xTextRange)
  {
    XTextRange range = getTextRange();
    if (range != null && bookmark.xTextContent() != null)
    {
      // Name merken:
      String name = getName();

      // altes Bookmark löschen.
      try
      {
        range.getText().removeTextContent(bookmark.xTextContent());
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }

      // neuen Bookmark unter dem alten Namen mit Ausdehnung hinzufügen.
      try
      {
        bookmark = document.create("com.sun.star.text.Bookmark");
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
  }

  /**
   * Die Methode gibt die XTextRange des Bookmarks zurück, oder null, falls das
   * Bookmark nicht vorhanden ist (z,B, weil es inzwischen gelöscht wurde).
   * 
   * @return
   */
  public XTextRange getTextRange()
  {
    // Das Bookmark könnte inzwischen vom Benutzer gelöscht worden sein. Aus
    // diesem Grund wird es hier neu geholt.
    bookmark = getBookmarkService(name, document);

    if (bookmark.xTextContent() != null)
      return bookmark.xTextContent().getAnchor();
    return null;
  }
}
