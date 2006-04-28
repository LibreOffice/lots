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
    if (document.xBookmarksSupplier() != null)
    {
      try
      {
        bookmark = new UnoService(document.xBookmarksSupplier().getBookmarks()
            .getByName(name));
      }
      catch (WrappedTargetException e)
      {
        Logger.error(e);
      }
    }
  }

  public String getName()
  {
    return name;
  }

  public String toString()
  {
    return "Bookmark[" + getName() + "]";
  }

  public int compare(Bookmark b) throws IllegalArgumentException
  {
    return compareTextRanges(this.getTextRange(), b.getTextRange());
  }

  // Positionsangaben als Rückgabewerte von compareTextRanges

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
      throws IllegalArgumentException
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
    int start = compare.compareRegionStarts(a, b) + 1;
    int end = compare.compareRegionEnds(a, b) + 1;
    return (3 * start + 1 * end) - 4;
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
    XTextRange range = getTextRange();
    if (range != null)
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

  public void rerangeBookmark(XTextRange xTextRange) throws Exception
  {
    XTextRange range = getTextRange();
    if (range != null)
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
      bookmark = document.create("com.sun.star.text.Bookmark");
      bookmark.xNamed().setName(name);
      xTextRange.getText().insertTextContent(
          xTextRange,
          bookmark.xTextContent(),
          true);
    }
  }

  public XTextRange getTextRange()
  {
    return bookmark.xTextContent().getAnchor();
  }
}
