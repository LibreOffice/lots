package de.muenchen.allg.itd51.wollmux.core.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.PropertyName;

/**
 * Helper for working with LibreOffice book marks.
 */
public class Bookmark
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Bookmark.class);

  private static final String COM_SUN_STAR_TEXT_BOOKMARK = "com.sun.star.text.Bookmark";

  /**
   * The name of book marks, which were removed.
   */
  public static final String BROKEN = "WM(CMD'bookmarkBroken')";

  /**
   * The name of the book mark.
   */
  private String name;

  /**
   * The document containing the book mark..
   */
  private XTextDocument document;

  /**
   * Access an existing book mark in the document.
   *
   * @param name
   *          The name of the book mark.
   * @param doc
   *          The document.
   * @throws NoSuchElementException
   *           A book mark with this name doesn't exist.
   */
  public Bookmark(String name, XBookmarksSupplier doc) throws NoSuchElementException
  {
    this.document = UNO.XTextDocument(doc);
    this.name = name;
    XTextContent bookmark = getBookmarkService(name);
    if (bookmark == null)
    {
      throw new NoSuchElementException(L.m("Bookmark '%1' existiert nicht.", name));
    }
  }

  /**
   * Access an existing book mark in the document.
   *
   * @param bookmark
   *          The name of the book mark.
   * @param doc
   *          The document.
   */
  public Bookmark(XNamed bookmark, XTextDocument doc)
  {
    this.document = doc;
    this.name = bookmark.getName();
  }

  /**
   * Create a new book mark at the given position. If the position has no dimension, a collapsed
   * book mark is created.
   *
   * @param name
   *          The name of the book mark.
   * @param doc
   *          The document.
   * @param range
   *          The position of the book mark.
   */
  public Bookmark(String name, XTextDocument doc, XTextRange range)
  {
    this.document = doc;
    this.name = name;

    XTextContent bookmark = null;
    try
    {
      bookmark = UNO.XTextContent(UNO.XMultiServiceFactory(document).createInstance(COM_SUN_STAR_TEXT_BOOKMARK));
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    if (UNO.XNamed(bookmark) != null)
    {
      UNO.XNamed(bookmark).setName(name);
    }

    // add book mark to the document
    if (document != null && bookmark != null && range != null)
    {
      try
      {
        // use text cursor instead of text range because they have more functionalty
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        range.getText().insertTextContent(cursor, bookmark, !cursor.isCollapsed());
        this.name = UNO.XNamed(bookmark).getName();
      } catch (IllegalArgumentException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  public String getName()
  {
    return name;
  }

  public XTextDocument getDocument()
  {
    return document;
  }

  /**
   * Select the text under the book mark.
   */
  public void select()
  {
    XTextContent bm = getBookmarkService(getName());
    if (bm != null)
    {
      try
      {
        XTextRange anchor = bm.getAnchor();
        XTextRange cursor = anchor.getText().createTextCursorByRange(anchor);
        UNO.XTextViewCursorSupplier(UNO.XModel(document).getCurrentController()).getViewCursor().gotoRange(cursor,
            false);
      } catch (java.lang.Exception x)
      {
        LOGGER.trace("", x);
      }
    }
  }

  /**
   * Rename this book mark. If the given name is already remitted, a number is added at the end.
   *
   * @param newName
   *          The requested name of the book mark.
   * @return The new name or {@link #BROKEN} if this book mark doesn't exist anymore.
   */
  public String rename(String newName)
  {
    UnoDictionary<XTextContent> bookmarks = new UnoDictionary<>(UNO.XBookmarksSupplier(document).getBookmarks());

    // this book mark has already the request name
    if (name.equals(newName))
    {
      if (!bookmarks.hasKey(name))
      {
        name = BROKEN;
      }
      return name;
    }

    LOGGER.debug("Rename \"{}\" --> \"{}\"", name, newName);

    // add number if names is already remitted
    if (bookmarks.hasKey(newName))
    {
      int count = 1;
      while (bookmarks.hasKey(newName + count))
      {
        ++count;
      }
      newName = newName + count;
    }

    XNamed bm = UNO.XNamed(bookmarks.get(name));
    if (bm != null)
    {
      bm.setName(newName);
      name = bm.getName();
    } else
    {
      name = BROKEN;
    }

    return name;
  }

  /**
   * Move the book mark to a new text range.
   *
   * @param xTextRange
   *          The new range.
   */
  public void rerangeBookmark(XTextRange xTextRange)
  {
    // delete old book mark
    remove();

    // create new book mark with old name and new dimension
    try
    {
      XTextContent bookmark = UNO
          .XTextContent(UNO.XMultiServiceFactory(document).createInstance(COM_SUN_STAR_TEXT_BOOKMARK));
      UNO.XNamed(bookmark).setName(name);
      xTextRange.getText().insertTextContent(xTextRange, bookmark, true);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Get a {@link XTextCursor} of the book mark. This text cursor is more robust than the anchor of
   * {@link #getAnchor()}.
   *
   * @return A text cursor over the range where this book mark is anchoring or null if the book mark
   *         doesn't exist anymore.
   */
  public XTextCursor getTextCursor()
  {
    /**
     * work around for https://bz.apache.org/ooo/show_bug.cgi?id=67869: wrong anchor for book marks
     * in tables. A text cursor is more robust.
     */
    XTextRange range = getAnchor();
    if (range != null && range.getText() != null)
    {
      return range.getText().createTextCursorByRange(range);
    }
    return null;
  }

  /**
   * Get the {@link XTextRange} of this book mark. You may use {@link #getTextCursor()} because of
   * OOo-Issue #67869.
   *
   * @return The text range where this book mark is anchoring or null if the book mark doesn't exist
   *         anymore.
   */
  public XTextRange getAnchor()
  {
    XBookmarksSupplier supp = UNO.XBookmarksSupplier(document);
    try
    {
      return UNO.XTextContent(supp.getBookmarks().getByName(name)).getAnchor();
    } catch (NoSuchElementException | WrappedTargetException x)
    {
      LOGGER.trace("", x);
      return null;
    }
  }

  /**
   * Delete this book mark.
   */
  public void remove()
  {
    XTextContent bookmark = getBookmarkService(name);
    if (bookmark != null)
    {
      try
      {
        XTextRange range = bookmark.getAnchor();
        range.getText().removeTextContent(bookmark);
      } catch (NoSuchElementException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Get the value of the property "IsCollapsed". Therefore the {@link XTextCursor} is iterated.
   *
   * @return True, if the property "IsCollapsed" exists and is true., false otherwise.
   */
  public boolean isCollapsed()
  {
    XTextRange anchor = getTextCursor();
    if (anchor == null)
    {
      return false;
    }
    try
    {
      Object par = UNO.XEnumerationAccess(anchor).createEnumeration().nextElement();
      UnoCollection<Object> collection = UnoCollection.getCollection(par, Object.class);
      for (Object content : collection)
      {
        try
        {
          String tpt = "" + UNO.getProperty(content, PropertyName.TEXT_PROTION_TYPE);
          if (PropertyName.BOOKMARK.equals(tpt))
          {
            XNamed bm = UNO.XNamed(UNO.getProperty(content, PropertyName.BOOKMARK));
            if (bm != null && name.equals(bm.getName()))
            {
              return AnyConverter.toBoolean(UNO.getProperty(content, PropertyName.IS_COLLAPSED));
            }
          }
        } catch (UnoHelperException e2)
        {
          LOGGER.trace("", e2);
        }
      }
    } catch (NoSuchElementException | WrappedTargetException e)
    {
      LOGGER.trace("", e);
    }
    return false;
  }

  /**
   * Expand a book mark. It has no dimension.
   */
  public void decollapseBookmark()
  {
    XTextRange range = getAnchor();
    if (range == null || !isCollapsed())
    {
      return;
    }

    XTextCursor cursor = range.getText().createTextCursorByRange(range);
    LOGGER.debug("Dekollabiere Bookmark '{}'", name);
    remove();

    // create new book mark with old name and new range
    try
    {
      XTextContent bookmark = UNO
          .XTextContent(UNO.XMultiServiceFactory(document).createInstance(COM_SUN_STAR_TEXT_BOOKMARK));
      UNO.XNamed(bookmark).setName(name);
      cursor.getText().insertString(cursor, ".", true);
      cursor.getText().insertTextContent(cursor, bookmark, true);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Collapse a book mark. Its content isn't deleted. The new book mark is right in front of the
   * content.
   */
  public void collapseBookmark()
  {
    XTextRange range = getAnchor();

    // do nothing, if book mark doesn't exist or is already collapsed
    if (range == null || isCollapsed())
    {
      return;
    }

    LOGGER.debug("Kollabiere Bookmark '{}'", name);
    remove();

    // create new collapsed book mark with old name
    try
    {
      XTextContent bookmark = UNO
          .XTextContent(UNO.XMultiServiceFactory(document).createInstance(COM_SUN_STAR_TEXT_BOOKMARK));
      UNO.XNamed(bookmark).setName(name);
      range.getText().insertTextContent(range.getStart(), bookmark, false);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public boolean equals(Object b)
  {
    if (b == null)
    {
      return false;
    }

    if (this.getClass() != b.getClass())
    {
      return false;
    }

    try
    {
      return name.equals(((Bookmark) b).name);
    } catch (java.lang.Exception e)
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return name.hashCode();
  }

  @Override
  public String toString()
  {
    return "Bookmark[" + getName() + "]";
  }

  /**
   * Get a book mark. Before each access to the book mark it should be collect from the document. It
   * may has been removed in the mean time.
   *
   * @param name
   *          The name of the book mark.
   * @return The text content of the book mark.
   */
  private XTextContent getBookmarkService(String name)
  {
    if (UNO.XBookmarksSupplier(document) != null)
    {
      try
      {
        return UNO.XTextContent(UNO.XBookmarksSupplier(document).getBookmarks().getByName(name));
      } catch (WrappedTargetException e)
      {
        LOGGER.error("", e);
      } catch (NoSuchElementException e)
      {
        LOGGER.trace("", e);
      }
    }
    return null;
  }

}
