/*
 * Dateiname: Bookmark.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repräsentiert ein Bookmark in OOo und bietet Methoden
 *            für den vereinfachten Zugriff und die Manipulation von Bookmarks an.
 *
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 17.05.2006 | LUT | Dokumentation ergänzt
 * 07.08.2006 | BNK | +Bookmark(XNamed bookmark, XTextDocument doc)
 * 29.09.2006 | BNK | rename() gibt nun im Fehlerfall das BROKEN-String-Objekt zurück
 * 29.09.2006 | BNK | Unnötige renames vermeiden, um OOo nicht zu stressen
 * 29.09.2006 | BNK | Auch im optimierten Fall wo kein rename stattfindet auf BROKEN testen
 * 20.10.2006 | BNK | rename() Debug-Meldung nicht mehr ausgeben, wenn No Op Optimierung triggert.
 * 31.10.2006 | BNK | +select() zum Setzen des ViewCursors
 * 08.07.2009 | BED | +collapseBookmark()
 *                  | Bookmark(String, XTextDocument, XTextRange) erzeugt kollabierte Bookmarks falls range keine Ausdehnung
 *                  | getTextRange() umbenannt in getTextCursor()
 *                  | decollapseBookmark() gefixt so dass dekollabierte Bookmarks nicht nochmal dekollabiert werden
 * 13.07.2009 | BED | decollapseBookmark() ruft jetzt kein setString() mehr auf
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Diese Klasse repräsentiert ein Bookmark in OOo und bietet Methoden für den
 * vereinfachten Zugriff und die Manipulation von Bookmarks an.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class Bookmark
{

  private static final String COM_SUN_STAR_TEXT_BOOKMARK = "com.sun.star.text.Bookmark";

  private static final Logger LOGGER = LoggerFactory.getLogger(Bookmark.class);

  /**
   * Wird festgestellt, dass das Bookmark aus dem Dokument gelöscht wurde, so wird
   * der Name auf diesen String gesetzt (== vergleichbar).
   */
  public static final String BROKEN = "WM(CMD'bookmarkBroken')";

  /**
   * Enthält den Namen des Bookmarks
   */
  private String name;

  /**
   * Enthält den UnoService des Dokuments dem das Bookmark zugeordnet ist.
   */
  private UnoService document;

  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc bestehenden
   * Bookmarks mit dem Namen name zurück; ist das Bookmark im angebegenen Dokument
   * nicht enthalten, so wird eine NoSuchElementException zurückgegeben.
   *
   * @param name
   *          Der Name des bereits im Dokument vorhandenen Bookmarks.
   * @param doc
   *          Das Dokument, welches Das Bookmark name enthält.
   * @throws NoSuchElementException
   *           Das Bookmark name ist im angegebenen Dokument nicht enthalten.
   */
  public Bookmark(String name, XBookmarksSupplier doc) throws NoSuchElementException
  {
    this.document = new UnoService(doc);
    this.name = name;
    UnoService bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() == null)
      throw new NoSuchElementException(L.m("Bookmark '%1' existiert nicht.", name));
  }

  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc bestehenden
   * Bookmarks bookmark zurück.
   */
  public Bookmark(XNamed bookmark, XTextDocument doc)
  {
    this.document = new UnoService(doc);
    this.name = bookmark.getName();
  }

  /**
   * Der Konstruktor erzeugt ein neues Bookmark name im Dokument doc an der Position,
   * die durch range beschrieben ist. Sollte die range keine Ausdehnung haben, so
   * wird an der Position ein kollabiertes Bookmark erzeugt, ansonsten ein
   * dekollabiertes Bookmark, das die range umschließt.
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
      bookmark = document.create(COM_SUN_STAR_TEXT_BOOKMARK);
    }
    catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }

    // Namen setzen
    if (bookmark.xNamed() != null)
    {
      bookmark.xNamed().setName(name);
    }

    // Bookmark ins Dokument einfügen
    if (document.xTextDocument() != null && bookmark.xTextContent() != null
      && range != null)
    {
      try
      {
        // der TextCursor ist erforderlich, damit auch Bookmarks mit Ausdehnung
        // erfolgreich gesetzt werden können. Das geht mit normalen TextRanges
        // nicht.
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        if (cursor.isCollapsed())
        { // kollabiertes Bookmark einfügen
          range.getText().insertTextContent(cursor, bookmark.xTextContent(), false);
        }
        else
        { // dekollabiertes Bookmark einfügen
          range.getText().insertTextContent(cursor, bookmark.xTextContent(), true);
        }
        this.name = bookmark.xNamed().getName();
      }
      catch (IllegalArgumentException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Vor jedem Zugriff auf den BookmarkService bookmark sollte der Service neu geholt
   * werden, damit auch der Fall behandelt wird, dass das Bookmark inzwischen vom
   * Anwender gelöscht wurde. Ist das Bookmark nicht (mehr) im Dokument vorhanden, so
   * wird ein new UnoService(null) zurückgeliefert, welches leichter verarbeitet
   * werden kann.
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
        return new UnoService(
          document.xBookmarksSupplier().getBookmarks().getByName(name));
      }
      catch (WrappedTargetException e)
      {
        LOGGER.error("", e);
      }
      catch (NoSuchElementException e)
      {
        LOGGER.trace("", e);
      }
    }
    return new UnoService(null);
  }

  /**
   * Diese Methode liefert den (aktuellen) Namen des Bookmarks als String zurück.
   *
   * @return liefert den (aktuellen) Namen des Bookmarks als String zurück.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Setzt den ViewCursor auf dieses Bookmark.
   */
  public void select()
  {
    UnoService bm = getBookmarkService(getName(), document);
    if (bm.getObject() != null)
    {
      try
      {
        XTextRange anchor = bm.xTextContent().getAnchor();
        XTextRange cursor = anchor.getText().createTextCursorByRange(anchor);
        UNO.XTextViewCursorSupplier(document.xModel().getCurrentController()).getViewCursor().gotoRange(
          cursor, false);
      }
      catch (java.lang.Exception x)
      {
        LOGGER.trace("", x);
      }
    }
  }

  /**
   * Diese Methode liefert eine String-Repräsentation mit dem Aufbau {@code Bookmark[<name>]}
   * zurück.
   */
  @Override
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
   * Diese Methode benennt dieses Bookmark in newName um. Ist der Name bereits
   * definiert, so wird automatisch eine Nummer an den Namen angehängt. Die Methode
   * gibt den tatsächlich erzeugten Bookmarknamen zurück.
   *
   * @return den tatsächlich erzeugten Namen des Bookmarks. Falls das Bookmark
   *         verschwunden ist, so wird das Objekt {@link #BROKEN} zurückgeliefert (==
   *         vergleichbar).
   */
  public String rename(String newName)
  {
    XNameAccess bookmarks =
      UNO.XBookmarksSupplier(document.getObject()).getBookmarks();

    // Um OOo nicht zu stressen vermeiden wir unnötige Renames
    // Wir testen aber trotzdem ob das Bookmark BROKEN ist
    if (name.equals(newName))
    {
      if (!bookmarks.hasByName(name)) {
        name = BROKEN;
      }
      return name;
    }

    LOGGER.debug("Rename \"{}\" --> \"{}\"", name, newName);

    // Falls bookmark <newName> bereits existiert, <newName>N verwenden (N ist
    // eine natürliche Zahl)
    if (bookmarks.hasByName(newName))
    {
      int count = 1;
      while (bookmarks.hasByName(newName + count))
        ++count;
      newName = newName + count;
    }

    XNamed bm = null;
    try
    {
      bm = UNO.XNamed(bookmarks.getByName(name));
    }
    catch (NoSuchElementException x)
    {
      LOGGER.debug(L.m("Umbenennung kann nicht durchgeführt werden, da die Textmarke verschwunden ist :~-(", x));
    }
    catch (java.lang.Exception x)
    {
      LOGGER.error("", x);
    }

    if (bm != null)
    {
      bm.setName(newName);
      name = bm.getName();
    }
    else
      name = BROKEN;

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
      UnoService bookmark = document.create(COM_SUN_STAR_TEXT_BOOKMARK);
      bookmark.xNamed().setName(name);
      xTextRange.getText().insertTextContent(xTextRange, bookmark.xTextContent(),
        true);
    }
    catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Liefert einen TextCursor für die TextRange, an der dieses Bookmark verankert
   * ist, zurück oder <code>null</code>, falls das Bookmark nicht mehr existiert
   * (zum Beispiel weil es inzwischen gelöscht wurde). Aufgrund von OOo-Issue #67869
   * ist es besser den von dieser Methode erzeugten Cursor statt direkt die TextRange
   * zu verwenden, da sich mit dem Cursor der Inhalt des Bookmarks sicherer
   * enumerieren lässt.
   *
   * @return einen TextCursor für den Anchor des Bookmarks oder <code>null</code>
   *         wenn das Bookmark nicht mehr existiert
   */
  public XTextCursor getTextCursor()
  {
    // Workaround für OOo-Bug: fehlerhafter Anchor bei Bookmarks in Tabellen.
    // http://www.openoffice.org/issues/show_bug.cgi?id=67869 . Ein
    // TextCursor-Objekt verhält sich dahingehend robuster.
    XTextRange range = getAnchor();
    if (range != null && range.getText() != null) {
      return range.getText().createTextCursorByRange(range);
    }
    return null;
  }

  /**
   * Liefert die TextRange an der dieses Bookmark verankert ist oder null falls das
   * Bookmark nicht mehr existiert. Aufgrund von OOo-Issue #67869 sollte überprüft
   * werden, ob es nicht besser ist statt dieser Methode {@link #getTextCursor()} zu
   * verwenden.
   */
  public XTextRange getAnchor()
  {
    XBookmarksSupplier supp = UNO.XBookmarksSupplier(document.getObject());
    try
    {
      return UNO.XTextContent(supp.getBookmarks().getByName(name)).getAnchor();
    }
    catch (com.sun.star.uno.Exception x)
    {
      LOGGER.trace("", x);
      return null;
    }
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
      }
      catch (NoSuchElementException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Entfernt allen Text (aber keine Bookmarks) aus range.
   *
   * @param doc
   *          das Dokument, das range enthält.
   */
  public static void removeTextFromInside(XTextDocument doc, XTextRange range)
  {
    try
    {
      // ein Bookmark erzeugen, was genau die Range, die wir löschen wollen vom
      // Rest des Textes abtrennt, d.h. welches dafür sorgt, dass unser Text
      // eine
      // eigene Textportion ist.
      Object bookmark =
        UNO.XMultiServiceFactory(doc).createInstance(COM_SUN_STAR_TEXT_BOOKMARK);
      UNO.XNamed(bookmark).setName("killer");
      range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
      String name = UNO.XNamed(bookmark).getName();

      // Aufsammeln der zu entfernenden TextPortions (sollte genau eine sein)
      // und
      // der Bookmarks, die evtl. als Kollateralschaden entfernt werden.
      List<String> collateral = new ArrayList<>();
      List<Object> victims = new ArrayList<>();
      XEnumeration xEnum = UNO.XEnumerationAccess(range).createEnumeration();
      while (xEnum.hasMoreElements())
      {
        boolean kill = false;
        XEnumerationAccess access = UNO.XEnumerationAccess(xEnum.nextElement());
        if (access != null)
        {
          XEnumeration xEnum2 = access.createEnumeration();
          while (xEnum2.hasMoreElements())
          {
            Object textPortion = xEnum2.nextElement();
            if ("Bookmark"
                .equals(Utils.getProperty(textPortion, "TextPortionType")))
            {
              String portionName =
                  UNO.XNamed(Utils.getProperty(textPortion, "Bookmark"))
                      .getName();
              if (name.equals(portionName))
              {
                kill =
                    ((Boolean) Utils.getProperty(textPortion, "IsStart"))
                        .booleanValue();
              }
              else
                collateral.add(portionName);
            }

            if (kill
                && "Text"
                    .equals(Utils.getProperty(textPortion, "TextPortionType")))
            {
              victims.add(textPortion);
            }
          }
        }
      }

      range.setString("");

      UNO.XTextContent(bookmark).getAnchor().getText().removeTextContent(
        UNO.XTextContent(bookmark));

      /*
       * Verlorene Bookmarks regenerieren.
       */
      XNameAccess bookmarks = UNO.XBookmarksSupplier(doc).getBookmarks();
      Iterator<String> iter = collateral.iterator();
      while (iter.hasNext())
      {
        String portionName = iter.next();
        if (!bookmarks.hasByName(portionName))
        {
          LOGGER.debug(L.m("Regeneriere Bookmark '%1'", portionName));
          bookmark =
            UNO.XMultiServiceFactory(doc).createInstance(
              COM_SUN_STAR_TEXT_BOOKMARK);
          UNO.XNamed(bookmark).setName(portionName);
          range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
        }

      }
    }
    catch (com.sun.star.uno.Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Diese Methode liefert den Wert der Property IsCollapsed zurück, wenn das
   * Ankerobjekt des Bookmarks diese Property besitzt, ansonsten wird false
   * geliefert. Das entsprechende Ankerobjekt wird durch entsprechende Enumerationen
   * über das Bookmarkobject gewonnen.
   *
   * @return true, wenn die Property IsCollapsed existiert und true ist. Ansonsten
   *         wird false geliefert.
   */
  public boolean isCollapsed()
  {
    XTextRange anchor = getTextCursor();
    if (anchor == null) {
      return false;
    }
    try
    {
      Object par = UNO.XEnumerationAccess(anchor).createEnumeration().nextElement();
      XEnumeration xenum = UNO.XEnumerationAccess(par).createEnumeration();
      while (xenum.hasMoreElements())
      {
        try
        {
          Object element = xenum.nextElement();
          String tpt = "" + UNO.getProperty(element, "TextPortionType");
          if (!"Bookmark".equals(tpt)) {
            continue;
          }
          XNamed bm = UNO.XNamed(UNO.getProperty(element, "Bookmark"));
          if (bm == null || !name.equals(bm.getName())) {
            continue;
          }
          return AnyConverter.toBoolean(UNO.getProperty(element, "IsCollapsed"));
        }
        catch (java.lang.Exception e2)
        {
          LOGGER.trace("", e2);
        }
      }
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
    return false;
  }

  /**
   * Diese Methode wandelt ein kollabiertes Bookmark (IsCollapsed()==true) in ein
   * nicht-kollabiertes Bookmark (IsCollapsed()==false) ohne Ausdehnung um. Auf diese
   * Weise wird OOo-Issue #73568 umgangen, gemäß dem kein Inhalt in das Bookmark
   * eingefügt werden kann, wenn IsCollapsed==true ist. Ist das Bookmark bereits
   * nicht-kollabiert, so wird nichts unternommen.
   */
  public void decollapseBookmark()
  {
    XTextRange range = getAnchor();
    if (range == null) {
      return;
    }

    // Beenden, wenn nicht-kollabiert.
    if (!isCollapsed()) {
      return;
    }

    // Alte Range sichern
    XTextCursor cursor = range.getText().createTextCursorByRange(range);

    LOGGER.debug(L.m("Dekollabiere Bookmark '%1'", name));

    // altes Bookmark löschen.
    remove();

    // neues Bookmark unter dem alten Namen mit neuer Ausdehnung hinzufügen.
    try
    {
      UnoService bookmark = document.create(COM_SUN_STAR_TEXT_BOOKMARK);
      bookmark.xNamed().setName(name);
      cursor.getText().insertString(cursor, ".", true);
      cursor.getText().insertTextContent(cursor, bookmark.xTextContent(), true);
    }
    catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Diese Methode wandelt ein nicht-kollabiertes Bookmark ({@link #isCollapsed()}==false)
   * in ein kollabiertes Bookmark ({@link #isCollapsed()}==true) um. Ist das
   * Bookmark bereits kollabiert oder existiert nicht mehr, wird nichts unternommen.
   * Beim Kollabieren des Bookmarks wird der vom dekollabierten Bookmark umgebene
   * Inhalt NICHT gelöscht. Das Bookmark befindet sich nach dem Kollabieren direkt
   * vor dem ehemals umschlossenen Inhalt.
   */
  public void collapseBookmark()
  {
    XTextRange range = getAnchor();

    // Nichts tun, falls Bookmark nicht mehr existiert oder bereits kollabiert ist
    if (range == null || isCollapsed())
    {
      return;
    }

    LOGGER.debug(L.m("Kollabiere Bookmark '%1'", name));

    // altes Bookmark löschen.
    remove();

    // neues (kollabiertes) Bookmark unter dem alten Namen hinzufügen.
    try
    {
      UnoService bookmark = document.create(COM_SUN_STAR_TEXT_BOOKMARK);
      bookmark.xNamed().setName(name);
      range.getText().insertTextContent(range.getStart(), bookmark.xTextContent(),
        false);
    }
    catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Definition von equals, damit Bookmarks über HashMaps/HashSets verwaltet werden
   * können.
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
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
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  /**
   * Definition von hashCode, damit Bookmarks über HashMaps/HashSets verwaltet werden
   * können.
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return name.hashCode();
  }

}
