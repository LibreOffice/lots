/*
 * Dateiname: DocumentCommand.java
 * Projekt  : WollMux
 * Funktion : Beschreibt ein Dokumentkommando mit allen zugehörigen Eigenschaften.
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
 * 07.11.2005 | LUT | Erstellung als WMCommandState
 * 23.01.2006 | LUT | Erweiterung zum hierarchischen WM-Kommando
 * 26.04.2006 | LUT | Komplette Überarbeitung und Umbenennung in DocumentCommand
 * 17.05.2006 | LUT | Doku überarbeitet
 * 13.11.2006 | BAB | Erweitern von 'insertFrag' um optionale Argumente 'ARGS'
 * 08.07.2009 | BED | getTextRange() aus Interface OptionalHighlightColorProvider entfernt
 *                  | getTextRange() in getTextCursor() umgearbeitet
 *                  | -createInsertCursor(boolean)
 *                  | +getTextCursorWithinInsertMarks()
 *                  | +setTextRangeString(String)
 *                  | +insertTextContentIntoBookmark(XTextContent, boolean)
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.core.document.commands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Beschreibt ein Dokumentkommando mit allen zugehörigen Eigenschaften wie z.B. die
 * Gruppenzugehörigkeit, Sichtbarkeit und Ausführstatus.
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public abstract class DocumentCommand
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DocumentCommand.class);

  /**
   * Das geparste ConfigThingy des zugehörenden Bookmarks.
   */
  protected ConfigThingy wmCmd;

  /**
   * Das zu diesem DokumentCommand gehörende Bookmark.
   */
  private Bookmark bookmark;

  // Status-Attribute:

  /**
   * Vorbelegung für den Status DONE.
   */
  private static final Boolean STATE_DEFAULT_DONE = Boolean.FALSE;

  /**
   * Vorbelegung für den Status ERROR
   */
  private static final Boolean STATE_DEFAULT_ERROR = Boolean.FALSE;

  /**
   * Enthält den aktuellen DONE-Status oder null, falls der Wert nicht verändert
   * wurde.
   */
  private Boolean done;

  /**
   * Enthält den aktuellen EROOR-Status oder null, falls der Wert nicht verändert
   * wurde.
   */
  private Boolean error;

  /**
   * Das Attribut visible gibt an, ob der Textinhalt des Kommandos sichtbar oder
   * ausgeblendet ist. Es wird nicht persistent im bookmark gespeichert.
   */
  private boolean visible = true;

  // Einfügemarken und Status für InsertCursor zum sicheren Einfügen von
  // Inhalten

  private boolean hasInsertMarks;

  private static final String INSERT_MARK_OPEN = "<";

  private static final String INSERT_MARK_CLOSE = ">";

  /* ************************************************************ */

  /**
   * Der Konstruktor liefert eine Instanz eines DocumentCommand an der Stelle des
   * Bookmarks bookmark mit dem geparsten Kommando wmCmd.
   *
   * @param wmCmd
   *          das geparste WM-Kommando
   * @param bookmark
   *          das zugehörige Bookmark
   */
  private DocumentCommand(ConfigThingy wmCmd, Bookmark bookmark)
  {
    this.wmCmd = wmCmd;
    this.bookmark = bookmark;
    this.hasInsertMarks = false;

    // Sicher ist sicher: Fehlermeldung wenn normale Dokumentkommandos ein
    // GROUPS-Attribut besitzen (die jetzt nicht merh unterstützt werden).
    if (wmCmd.query("GROUPS").count() > 0 && !canHaveGroupsAttribute())
    {
      LOGGER.error(L.m(
        "Das Dokumentkommando '%1' darf kein GROUPS-Attribut besitzen.",
        getBookmarkName()));
    }
  }

  /**
   * Seit der Umstellung auf den neuen Scan über DocumentCommands darf nur noch das
   * Dokumentkommando SetGroups ein GROUPS-Attribut besitzen. Diese Methode stellt
   * das sicher und wird vom SetGroups-Kommando mit Rückgabewert true überschrieben.
   *
   * @return false außer es ist ein SetGroups-Kommando
   */
  protected boolean canHaveGroupsAttribute()
  {
    return false;
  }

  /**
   * Liefert true, wenn das Dokumentkommando Textinhalte in das zugehörige Bookmark
   * einfügen möchte, ansonsten false.
   */
  protected boolean insertsTextContent()
  {
    return false;
  }

  /**
   * Liefert den Namen des zugehörigen Bookmarks zurück.
   *
   * @return Liefert den Namen des zugehörigen Bookmarks zurück.
   */
  public String getBookmarkName()
  {
    return bookmark.getName();
  }

  /**
   * Diese Methode liefert eine String-Repräsentation des DokumentCommands zurück.
   * Die String-Repräsentation hat den Aufbau {@code DocumentCommand[<bookmarkName>]}.
   */
  @Override
  public String toString()
  {
    return "" + this.getClass().getSimpleName() + "["
      + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "")
      + getBookmarkName() + "]";
  }

  /**
   * Callbackfunktion für die Ausführung des Dokumentkommandos in einem
   * DocumentCommand.Executor wie z.B. dem DocumentCommandInterpreter. Die Methode
   * liefert die Anzahl der bei der Ausführung entstandenen Fehler zurück.
   *
   * @param executor
   * @return Anzahl der aufgetretenen Fehler
   */
  public abstract int execute(DocumentCommand.Executor executor);

  /**
   * Liefert einen TextCursor für die TextRange, an der das Bookmark dieses
   * {@link DocumentCommand}s verankert ist, zurück oder <code>null</code>, falls das
   * Bookmark nicht mehr existiert (zum Beispiel weil es inzwischen gelöscht wurde).
   * Aufgrund von OOo-Issue #67869 ist es im allgemeinen besser den von dieser
   * Methode erzeugten Cursor statt direkt die TextRange zu verwenden, da sich mit
   * dem Cursor der Inhalt des Bookmarks sicherer enumerieren lässt. Der von dieser
   * Methode zurückgelieferte TextCursor sollte allerdings nicht verwendet werden, um
   * direkt Text innerhalb eines Bookmarks einzufügen! Dafür sind die Methoden
   * {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} und
   * {@link #getTextCursorWithinInsertMarks()} da.
   *
   * @return einen TextCursor für den Anchor des Bookmarks oder <code>null</code>
   *         wenn das Bookmark nicht mehr existiert
   */
  public XTextCursor getTextCursor()
  {
    XTextCursor cursor = bookmark.getTextCursor();
    if (cursor == null)
    {
      LOGGER.debug(L.m(
        "Kann keinen Textcursor erstellen für Dokumentkommando '%1'\nIst das Bookmark vielleicht verschwunden?",
        this.toString()));
    }
    return cursor;
  }

  /**
   * Liefert die TextRange an der das Bookmark dieses Kommandos verankert ist oder
   * null, falls das Bookmark nicht mehr existiert. Die von dieser Methode
   * zurückgelieferte TextRange sollte nicht verwendet werden, um direkt Text
   * innerhalb eines Bookmarks einzufügen! Dafür sind die Methoden
   * {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} und
   * {@link #getTextCursorWithinInsertMarks()} da.
   *
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#getAnchor()
   */
  public XTextRange getAnchor()
  {
    return bookmark.getAnchor();
  }

  /**
   * Liefert einen TextCursor ohne Ausdehnung zum Einfügen von Inhalt innerhalb des
   * Bookmarks dieses {@link DocumentCommand}s zurück, wobei der ursprünglich
   * enthaltene Inhalt des Ankers des Bookmarks durch zwei Insert Marks (
   * {@link #INSERT_MARK_OPEN} und {@link #INSERT_MARK_CLOSE}) ersetzt wird. Sollte
   * das Bookmark kollabiert gewesen sein, so wird es zunächst dekollabiert, so dass
   * die Insert Marks innerhalb des Bookmarks eingefügt werden können. Der
   * zurückgelieferte Cursor besitzt keine Ausdehnung und befindet sich zwischen den
   * beiden eingefügten Insert Marks. Falls das Bookmark nicht mehr existiert liefert
   * die Methode <code>null</code> zurück.
   *
   * @return TextCursor zum Einfügen innerhalb von Insert Marks oder
   *         <code>null</code>
   */
  public XTextCursor getTextCursorWithinInsertMarks()
  {
    // Insert Marks hinzufügen
    // (dabei wird das Bookmark falls nötig dekollabiert)
    this.setTextRangeString(INSERT_MARK_OPEN + INSERT_MARK_CLOSE);
    hasInsertMarks = true;

    XTextCursor cursor = this.getTextCursor(); // Cursor mit Insert Marks
    if (cursor != null)
    {
      cursor.goRight(getStartMarkLength(), false);
      cursor.collapseToStart();
    }

    return cursor;
  }

  /**
   * Setzt den Inhalt der TextRange, an der das Bookmark dieses Kommandos verankert
   * ist, auf den übergebenen String-Wert und kollabiert/dekollabiert das Bookmark je
   * nachdem ob der String leer (bzw. <code>null</code>) oder nicht-leer ist. Bei
   * einem leeren String wird das Bookmark kollabiert (sofern es dies nicht schon
   * ist), bei einem nicht-leeren String wird das Bookmark dekollabiert (sofern nicht
   * schon der Fall) und das dekollabierte Bookmark umfasst anschließend den
   * übergebenen Text.
   *
   * @param text
   *          der einzufügende String; falls leer (oder <code>null</code>) wird
   *          Bookmark kollabiert
   */
  public void setTextRangeString(String text)
  {
    if (text != null && text.length() > 0)
    {
      bookmark.decollapseBookmark();
    }

    XTextRange range = bookmark.getAnchor();
    if (range != null)
    {
      range.setString(text); // setString(null) kein Problem
    }

    if (text == null || text.length() == 0)
    {
      bookmark.collapseBookmark();
    }
  }

  /**
   * Fügt in die TextRange, an der das Bookmark dieses Kommandos verankert ist, den
   * übergebenen TextContent ein, wobei das Bookmark zuvor dekollabiert wird (sollte
   * es das nicht ohnehin schon sein). Über den Parameter replace kann gesteuert
   * werden, ob beim Einfügen der bisherige Inhalt der TextRange des Bookmarks
   * ersetzt werden soll oder nicht. Wird <code>false</code> übergeben, so wird der
   * TextContent an das Ende der TextRange des Bookmarks (aber natürlich noch im
   * Bookmark) hinzugefügt.
   *
   * @param textContent
   *          der einzufügende TextContent
   * @param replace
   *          bestimmt ob der in der TextRange des Bookmarks enthaltene Inhalt von
   *          textContent ersetzt werden soll. Falls <code>true</code> wird der
   *          Inhalt ersetzt, ansonsten wird textContent an das Ende der TextRange
   *          des Bookmarks gehängt
   * @throws IllegalArgumentException
   */
  public void insertTextContentIntoBookmark(XTextContent textContent, boolean replace)
  {
    if (textContent != null)
    {
      bookmark.decollapseBookmark();
      XTextCursor cursor = bookmark.getTextCursor();
      if (cursor != null)
      {
        XText text = cursor.getText();
        text.insertTextContent(cursor, textContent, replace);
      }
    }
  }

  /**
   * Liefert die Länge des End-Einfügemarkers.
   */
  public short getEndMarkLength()
  {
    return (short) INSERT_MARK_CLOSE.length();
  }

  /**
   * Liefert die Länge des Start-Einfügemarkers.
   */
  public short getStartMarkLength()
  {
    return (short) INSERT_MARK_OPEN.length();
  }

  /**
   * Liefert entweder null falls kein Start-Einfügemarke vorhanden oder liefert 2
   * Cursor, von denen der erste links neben der zweite rechts neben der
   * Start-Einfügemarke steht.
   */
  public XParagraphCursor[] getStartMark()
  {
    XTextRange range = bookmark.getTextCursor();
    if (range == null || !hasInsertMarks) {
      return null;
    }
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getStart()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[1].goRight(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert entweder null falls keine End-Einfügemarke vorhanden oder liefert 2
   * Cursor, von denen der erste links neben der zweite rechts neben der
   * End-Einfügemarke steht.
   */
  public XParagraphCursor[] getEndMark()
  {
    XTextRange range = bookmark.getTextCursor();
    if (range == null || !hasInsertMarks) {
      return null;
    }
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getEnd()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[0].goLeft(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert <code>true</code> zurück, wenn das Dokumentkommando insertMarks besitzt,
   * die zuvor über den Aufruf von {@link #getTextCursorWithinInsertMarks()} erzeugt
   * worden sind, ansonsten <code>false</code>.
   */
  public boolean hasInsertMarks()
  {
    return hasInsertMarks;
  }

  /**
   * Teilt dem Dokumentkommando mit, dass die insertMarks des Dokumentkommandos
   * entfernt wurden und {@link #hasInsertMarks()} in Folge dessen <code>false</code>
   * zurück liefern muss.
   */
  public void unsetHasInsertMarks()
  {
    this.hasInsertMarks = false;
  }

  /**
   * Liefert true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr existiert
   * und das Dokumentkommando daher nicht mehr zu gebrauchen ist oder andernfalls
   * false.
   *
   * @return true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr existiert,
   *         ansonsten false.
   */
  public boolean isRetired()
  {
    if (bookmark != null) {
      return bookmark.getAnchor() == null;
    }
    return false;
  }

  /**
   * Beschreibt ob das Kommando bereits abgearbeitet wurde. Ist DONE bisher noch
   * nicht definiert oder gesetzt worden, so wird der Defaultwert false
   * zurückgeliefert.
   *
   * @return true, falls das Kommando bereits bearbeitet wurde, andernfalls false.
   */
  public boolean isDone()
  {
    if (done != null)
      return done.booleanValue();
    else if (isDefinedState("DONE"))
    {
      try
      {
        String doneStr = getState("DONE").toString();
        return doneStr.compareToIgnoreCase("true") == 0;
      } catch (NodeNotFoundException e)
      {
        return false;
      }
    }
    else
      return STATE_DEFAULT_DONE.booleanValue();
  }

  /**
   * Markiert ein Dokumentkommando als bearbeitet; Das Dokumentkommando wird
   * daraufhin aus dem Dokument gelöscht, wenn removeBookmark==true oder umbenannt,
   * wenn removeBookmark==false.
   *
   * @param removeBookmark
   *          true, signalisiert, dass das zugehörige Bookmark gelöscht werden soll.
   *          False signalisiert, dass das Bookmark mit dem Zusatz {@code <alterName>
   *          STATE(DONE 'true')} versehen wird.
   */
  public void markDone(boolean removeBookmark)
  {

    this.done = Boolean.TRUE;
    flushToBookmark(removeBookmark);
  }

  /**
   * Liefert den Fehler-Status der Kommandobearbeitung zurück. Ist das Attribut ERROR
   * bisher nicht definiert oder kein Fehler gesetzt worden, so wird der Defaultwert
   * false zurückgliefert.
   *
   * @return true, wenn bei der Kommandobearbeitung Fehler auftraten; andernfalls
   *         false
   */
  public boolean hasError()
  {
    if (error != null)
      return error.booleanValue();
    else if (isDefinedState("ERROR"))
    {
      try
      {
        return Boolean.parseBoolean(getState("ERROR").toString());
      } catch (NodeNotFoundException e)
      {
        return false;
      }
    }
    else
      return STATE_DEFAULT_ERROR.booleanValue();
  }

  /**
   * Erlaubt das explizite Setzen des Error-Attributs.
   *
   * @param error
   */
  public void setErrorState(boolean error)
  {
    this.error = Boolean.valueOf(error);
  }

  /**
   * Liefert ein ConfigThingy, das ein "WM"-Kommando mit allen Statusinformationen
   * enthält. Neue Unterknoten werden dabei nur angelegt, wenn dies unbedingt
   * erforderlich ist, d.h. wenn ein Wert vom Defaultwert abweicht oder der Wert
   * bereits vorher gesetzt war.
   *
   * @return Ein ConfigThingy, das das "WM"-Kommando mit allen Statusinformationen
   *         enthält.
   */
  protected ConfigThingy toConfigThingy()
  {
    // DONE:
    // Falls der Knoten existiert und sich der Status geändert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefinedState("DONE") && done != null)
    {
      setOrCreate("DONE", done.toString());
    }
    else if (isDone() != STATE_DEFAULT_DONE.booleanValue())
    {
      setOrCreate("DONE", Boolean.toString(isDone()));
    }

    // ERRORS:
    // Falls der Knoten existiert und sich der Status geändert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefinedState("ERROR") && error != null)
    {
      setOrCreate("ERROR", error.toString());
    }
    else if (hasError() != STATE_DEFAULT_ERROR.booleanValue())
    {
      setOrCreate("ERRORS", Boolean.toString(hasError()));
    }

    return wmCmd;
  }

  /**
   * Liefert den Inhalt des übergebenen ConfigThingy-Objekts (üblicherweise das
   * wmCmd) als einen String, der geeignet ist, um den ihn in Bookmarknamen verwenden
   * zu können. D.h. alle Newline, Komma und andere für Bookmarknamen unverträgliche
   * Zeichen werden entfernt.
   *
   * @param conf
   *          Das ConfigThingy-Objekt, zu dem die Stringrepräsentation erzeugt werden
   *          soll.
   * @return Einen String, der als Bookmarkname verwendet werden kann.
   */
  public static String getCommandString(ConfigThingy conf)
  {
    // Neues WM-String zusammenbauen, der keine Zeilenvorschübe, Kommas und
    // abschließende Leerzeichen enthält:
    String wmCmdString = conf.stringRepresentation(true, '\'', true);
    wmCmdString = wmCmdString.replaceAll(",", " ");
    wmCmdString = wmCmdString.replaceAll("[\r\n]+", " ");
    while (wmCmdString.endsWith(" "))
      wmCmdString = wmCmdString.substring(0, wmCmdString.length() - 1);
    return wmCmdString;
  }

  /**
   * Schreibt den neuen Status des Dokumentkommandos in das Dokument zurück oder
   * löscht ein Bookmark, wenn der Status DONE=true gesetzt ist - Die Methode liefert
   * entweder den Namen des neuen Bookmarks, welches die neuen Statusinformationen
   * enthält zurück, oder null, wenn das zugehörige Bookmark gelöscht wurde. Ist der
   * DEBUG-modus gesetzt, so werden in gar keinem Fall Bookmarks gelöscht, womit die
   * Fehlersuche erleichtert werden soll.
   *
   * @return der Name des neuen Bookmarks oder null.
   */
  protected String flushToBookmark(boolean removeIfDone)
  {
    if (isDone() && removeIfDone)
    {
      bookmark.remove();
      return null;
    }
    else
    {
      String wmCmdString = getCommandString(toConfigThingy());

      // Neuen Status rausschreiben, wenn er sich geändert hat:
      String name = bookmark.getName();
      name = name.replaceFirst("\\s*\\d+\\s*$", "");
      if (!wmCmdString.equals(name)) {
        bookmark.rename(wmCmdString);
      }

      return bookmark.getName();
    }
  }

  /**
   * Gibt Auskunft, ob ein Key unterhalb des STATE-Knotens definiert ist. z.B.
   * "WM(...) STATE (KEY '...')"
   *
   * @param key
   * @return true, falls der Key definiert ist, andernfalls false.
   */
  protected boolean isDefinedState(String key)
  {
    try
    {
      return getState(key) != null;
    } catch (NodeNotFoundException e)
    {
      return false;
    }
  }

  /**
   * Liefert das ConfigThingy zu dem gesuchten Key key unterhalt des STATE-Knotens.
   *
   * @param key
   * @return Das ConfigThingy, mit dem Key key.
   * @throws NodeNotFoundException
   */
  protected ConfigThingy getState(String key) throws NodeNotFoundException
  {
    return wmCmd.get("STATE").get(key);
  }

  /**
   * Setzt einen Schlüssel-Wert-Paar unterhalb des STATE-Knotens. Ist der Schlüssel
   * bereits definiert, wird der bestehende Wert überschrieben. Sind der STATE-Knoten
   * oder der Schlüssel nicht definiert, so werden die entsprechenden Knoten erzeugt
   * und der Key key erhält ein Kindknoten mit dem Value value.
   *
   * @param key
   * @param value
   */
  protected void setOrCreate(String key, String value)
  {
    // gewünschte Struktur aufbauen:

    // a) STATE(...)
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
    }
    catch (NodeNotFoundException e1)
    {
      state = wmCmd.add("STATE");
    }

    // b) STATE(KEY ...)
    ConfigThingy ctKey;
    try
    {
      ctKey = state.get(key);
    }
    catch (NodeNotFoundException e)
    {
      ctKey = state.add(key);
    }

    // c) STATE(KEY 'value')
    try
    {
      ctKey.getFirstChild().setName(value);
    }
    catch (NodeNotFoundException e)
    {
      ctKey.add(value);
    }
  }

  /**
   * gibt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando zurück.
   *
   * @return true=sichtbar, false=ausgeblendet
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#isVisible()
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Setzt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando auf
   * visible. Der Status visible wird zudem nicht persistent im Bookmark hinterlegt.
   *
   * @param visible
   *          true=sichtbar, false=ausgeblendet
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#setVisible(boolean)
   */
  public void setVisible(boolean visible)
  {
    this.visible = visible;
    XTextCursor cursor = getTextCursor();
    if (cursor != null)
    {
      try
      {
        UNO.hideTextRange(cursor, !visible);
      }
      catch (UnoHelperException e)
      {
        LOGGER.error("Sichtbarkeit konnte nicht geändert werden.", e);
      }
    }
  }

  // ********************************************************************************
  /**
   * Kommandos werden extern definiert - um das zu ermöglichen greift hier das
   * Prinzip des Visitor-Designpatterns.
   *
   * @author christoph.lutz
   */
  static interface Executor
  {
    public int executeCommand(DocumentCommand.InsertFrag cmd);

    public int executeCommand(DocumentCommand.InsertValue cmd);

    public int executeCommand(DocumentCommand.InsertContent cmd);

    public int executeCommand(DocumentCommand.Form cmd);

    public int executeCommand(DocumentCommand.InvalidCommand cmd);

    public int executeCommand(DocumentCommand.UpdateFields cmd);

    public int executeCommand(DocumentCommand.SetType cmd);

    public int executeCommand(DocumentCommand.InsertFormValue cmd);

    public int executeCommand(DocumentCommand.SetGroups cmd);

    public int executeCommand(DocumentCommand.SetPrintFunction cmd);

    public int executeCommand(DocumentCommand.DraftOnly cmd);

    public int executeCommand(DocumentCommand.CopyOnly cmd);

    public int executeCommand(DocumentCommand.NotInOriginal cmd);

    public int executeCommand(DocumentCommand.OriginalOnly cmd);

    public int executeCommand(DocumentCommand.AllVersions cmd);

    public int executeCommand(DocumentCommand.SetJumpMark cmd);

    public int executeCommand(DocumentCommand.OverrideFrag cmd);
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das einen Wert in das Dokument einfügt, der
   * über eine optionale Transformation umgewandelt werden kann (Derzeit
   * implementieren insertValue und insertFormValue dieses Interface).
   */
  public static interface OptionalTrafoProvider
  {
    public String getTrafoName();
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das das optionale Attribut HIGHLIGHT_COLOR
   * enthalten kann (derzeit AllVersions, DraftOnly, CopyOnly, NotInOriginal und
   * OriginalOnly)
   */
  public static interface OptionalHighlightColorProvider
  {
    public String getHighlightColor();
  }

  // ********************************************************************************
  /**
   * Eine Exception die geworfen wird, wenn ein Dokumentkommando als ungültig erkannt
   * wurde, z,b, aufgrund eines fehlenden Parameters.
   */
  public static class InvalidCommandException extends com.sun.star.uno.Exception
  {
    private static final long serialVersionUID = -3960668930339529734L;

    public InvalidCommandException(String message)
    {
      super(message);
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das aufgrund eines Syntax-Fehlers oder eines
   * fehlenden Parameters ungültig ist, jedoch trotzdem im Dokument als Fehlerfeld
   * dargestellt werden soll.
   *
   * @author lut
   *
   */
  public static class InvalidCommand extends DocumentCommand
  {
    private java.lang.Exception exception;

    public InvalidCommand(ConfigThingy wmCmd, Bookmark bookmark,
        InvalidCommandException exception)
    {
      super(wmCmd, bookmark);
      this.exception = exception;
    }

    public InvalidCommand(Bookmark bookmark, SyntaxErrorException exception)
    {
      super(new ConfigThingy("WM"), bookmark);
      this.exception = exception;
    }

    public java.lang.Exception getException()
    {
      return exception;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    public String updateBookmark()
    {
      // der updateBookmark darf in diesem Fall natürlich nichts rausschreiben,
      // da das Kommando ja nicht mal in einer syntaktisch vollständigen Version
      // vorliegt.
      return getBookmarkName();
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein noch nicht implementiertes Dokumentkommando, das jedoch für die
   * Zukunft geplant ist und dess Ausführung daher keine Fehler beim Erzeugen des
   * Briefkopfs liefern darf.
   */
  public static class NotYetImplemented extends DocumentCommand
  {
    public NotYetImplemented(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      markDone(false);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt das Dokumentkommando Form, welches Zugriff auf die
   * Formularbeschreibung des Dokuments ermöglicht, die in Form einer Notiz innerhalb
   * des zum Form-Kommando zugehörigen Bookmarks abgelegt ist.
   *
   * @author lut
   *
   */
  public static class Form extends DocumentCommand
  {
    public Form(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertFrag fügt ein externes Textfragment in das Dokument ein.
   */
  public static class InsertFrag extends DocumentCommand
  {
    private String fragID;

    private List<String> args = null;

    private boolean manualMode = false;

    private Set<String> styles = null;

    public InsertFrag(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      ConfigThingy wm = wmCmd.query("WM");

      try
      {
        fragID = wm.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID", e));
      }

      args = new Vector<>();
      try
      {
        ConfigThingy argsConf = wm.get("ARGS");
        for (ConfigThingy arg : argsConf)
        {
          args.add(arg.getName());
        }
      }
      catch (NodeNotFoundException e)
      {
        // ARGS sind optional
      }

      String mode = wm.getString("MODE", "");
      manualMode = "manual".equalsIgnoreCase(mode);

      styles = new HashSet<>();
      try
      {
        ConfigThingy stylesConf = wm.get("STYLES");
        for (ConfigThingy style : stylesConf)
        {
          String s = style.toString();
          if ("all".equalsIgnoreCase(s))
          {
            styles.add("textstyles");
            styles.add("pagestyles");
            styles.add("numberingstyles");
          }
          else if ("textStyles".equalsIgnoreCase(s) || "pageStyles".equalsIgnoreCase(s)
              || "numberingStyles".equalsIgnoreCase(s))
          {
            styles.add(s.toLowerCase());
          }
          else
            throw new InvalidCommandException(L.m("STYLE '%1' ist unbekannt.", s));
        }
      }
      catch (NodeNotFoundException e)
      {
        // STYLES ist optional
      }

    }

    public String getFragID()
    {
      return fragID;
    }

    public List<String> getArgs()
    {
      return args;
    }

    public boolean isManualMode()
    {
      return manualMode;
    }

    public boolean importStylesOnly()
    {
      return !styles.isEmpty();
    }

    public Set<String> getStyles()
    {
      return styles;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertContent dient zum Mischen von Dokumenten und ist im Handbuch
   * des WollMux ausführlicher beschrieben.
   */
  public static class InsertContent extends DocumentCommand
  {
    public InsertContent(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  public static class InsertValue extends DocumentCommand implements
      OptionalTrafoProvider
  {
    private String dbSpalte;

    private String leftSeparator = "";

    private String rightSeparator = "";

    private String trafo = null;

    public InsertValue(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      try
      {
        dbSpalte = wmCmd.get("WM").get("DB_SPALTE").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut DB_SPALTE"));
      }

      // Auswertung der AUTOSEP bzw SEPERATOR-Attribute
      Iterator<ConfigThingy> autoseps = wmCmd.query("AUTOSEP").iterator();
      Iterator<ConfigThingy> seps = wmCmd.query("SEPARATOR").iterator();
      String currentSep = " "; // mit Default-Separator vorbelegt

      while (autoseps.hasNext())
      {
        ConfigThingy as = autoseps.next();
        String sep = currentSep;
        if (seps.hasNext()) {
          sep = seps.next().toString();
        }

        if (as.toString().compareToIgnoreCase("left") == 0)
        {
          leftSeparator = sep;
        }
        else if (as.toString().compareToIgnoreCase("right") == 0)
        {
          rightSeparator = sep;
        }
        else if (as.toString().compareToIgnoreCase("both") == 0)
        {
          leftSeparator = sep;
          rightSeparator = sep;
        }
        else
        {
          throw new InvalidCommandException(
            L.m(
              "Unbekannter AUTOSEP-Typ \"%1\". Erwarte \"left\", \"right\" oder \"both\".",
              as.toString()));
        }
        currentSep = sep;
      }

      // Auswertung des optionalen Arguments TRAFO
      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      }
      catch (NodeNotFoundException e)
      {
        // TRAFO ist optional
      }
    }

    public String getDBSpalte()
    {
      return dbSpalte;
    }

    public String getLeftSeparator()
    {
      return leftSeparator;
    }

    public String getRightSeparator()
    {
      return rightSeparator;
    }

    @Override
    public String getTrafoName()
    {
      return trafo;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  public static class InsertFormValue extends DocumentCommand implements
      OptionalTrafoProvider
  {
    private String id = null;

    private String trafo = null;

    public InsertFormValue(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      try
      {
        id = wmCmd.get("WM").get("ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut ID"));
      }

      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      }
      catch (NodeNotFoundException e)
      {
        // TRAFO ist optional
      }
    }

    /**
     * Liefert die im insertFormValues-Kommando gesetzte id, die garantiert != null
     * ist.
     */
    public String getID()
    {
      return id;
    }

    public void setID(String id)
    {
      this.id = id;
      // Dokumentkommando anpassen:
      try
      {
        ConfigThingy idConf = wmCmd.query("WM").query("ID").getLastChild();
        // alten Wert von ID löschen
        for (Iterator<ConfigThingy> iter = idConf.iterator(); iter.hasNext();)
        {
          iter.next();
          iter.remove();
        }
        // neuen Wert für ID setzen
        idConf.addChild(new ConfigThingy(id));
      }
      catch (NodeNotFoundException e)
      {
        LOGGER.error("", e);
      }
      flushToBookmark(false);
    }

    @Override
    public String getTrafoName()
    {
      return trafo;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando sorgt dafür, dass alle unter dem Bookmark liegenden TextFields
   * geupdatet werden.
   */
  public static class UpdateFields extends DocumentCommand
  {
    public UpdateFields(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Über dieses Dokumentkommando kann der Typ des Dokuments festgelegt werden. Es
   * gibt die Typen SETTYPE_normalTemplate, SETTYPE_templateTemplate und
   * SETTYPE_formDocument. Die SetType-Kommandos werden bereits im
   * OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr für
   * den DocumentCommandInterpreter.
   */
  public static class SetType extends DocumentCommand
  {
    private String type;

    public SetType(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      type = "";
      try
      {
        type = wmCmd.get("WM").get("TYPE").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut TYPE"));
      }
      if (type.compareToIgnoreCase("templateTemplate") != 0
        && type.compareToIgnoreCase("normalTemplate") != 0
        && type.compareToIgnoreCase("formDocument") != 0)
        throw new InvalidCommandException(
          L.m("Angegebener TYPE ist ungültig oder falsch geschrieben. Erwarte \"templateTemplate\", \"normalTemplate\" oder \"formDocument\"!"));
    }

    public String getType()
    {
      return type;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando dient zum Überschreiben von FRAG_IDs, die mit insertFrag oder
   * insertContent eingefügt werden sollen.
   */
  public static class OverrideFrag extends DocumentCommand
  {
    private String fragId;

    private String newFragId = null;

    public OverrideFrag(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      fragId = "";
      try
      {
        fragId = wmCmd.get("WM").get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID"));
      }
      try
      {
        newFragId = wmCmd.get("WM").get("NEW_FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        // NEW_FRAG_ID ist optional
      }
    }

    public String getFragID()
    {
      return fragId;
    }

    /**
     * Liefert die neue FragID oder den Leerstring, wenn keine FragID angegeben
     * wurde.
     */
    public String getNewFragID()
    {
      if (newFragId == null) {
        return "";
      }
      return newFragId;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Über dieses Dokumentkommando können Druckfunktionen festgelegt werden, die beim
   * Drucken ausgeführt werden sollen. Die SetPrintFunction-Kommandos werden bereits
   * im OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr
   * für den DocumentCommandInterpreter.
   */
  public static class SetPrintFunction extends DocumentCommand
  {
    private String funcName;

    public SetPrintFunction(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      funcName = "";
      try
      {
        funcName = wmCmd.get("WM").get("FUNCTION").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FUNCTION"));
      }
    }

    public String getFunctionName()
    {
      return funcName;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando SetGroups dient dazu, dem vom Bookmark umschlossenen Textinhalt
   * eine oder mehrere Gruppen zuweisen zu können. Im Gegensatz zu anderen
   * Dokumentkommandos, die auch das GROUPS Attribut unterstützen, besitzt dieses
   * Kommando ausser der Zuordnung von Gruppen keine weitere Funktion.
   */
  public static class SetGroups extends DocumentCommand implements VisibilityElement
  {
    private Set<String> groupsSet;

    public SetGroups(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      groupsSet = new HashSet<>();
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    /**
     * Liefert alle Gruppen, die dem Dokumentkommando zugeordnet sind, wobei jede
     * Gruppe auch die Gruppenzugehörigkeit des Vaterelements im DocumentCommand-Baum
     * erbt.
     *
     * @return Ein Set, das alle zugeordneten groupId's, einschließlich der vom Vater
     *         geerbten, als Strings enthält.
     * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#getGroups()
     */
    @Override
    public Set<String> getGroups()
    {
      // GROUPS-Attribut auslesen falls vorhanden.
      ConfigThingy groups = new ConfigThingy("");
      try
      {
        groups = wmCmd.get("GROUPS");
      }
      catch (NodeNotFoundException e)
      {}

      // Gruppen aus dem GROUPS-Argument in das Set aufnehmen:
      for (ConfigThingy group : groups)
      {
        String groupId = group.toString();
        groupsSet.add(groupId);
      }

      return groupsSet;
    }

    @Override
    public void addGroups(Set<String> groups)
    {
      groupsSet.addAll(groups);
    }

    @Override
    protected boolean canHaveGroupsAttribute()
    {
      return true;
    }

    /**
     * Diese Methode liefert eine String-Repräsentation des DokumentCommands zurück.
     * Die String-Repräsentation hat den Aufbau {@code DocumentCommand[<bookmarkName>]}.
     */
    @Override
    public String toString()
    {
      return "" + this.getClass().getSimpleName() + "["
        + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "") + "GROUPS:"
        + groupsSet.toString() + getBookmarkName() + "]";
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen wird die Ausfertigung, die ALLE
   * definierten Verfügungpunkte enthält als "Entwurf" bezeichnet. Mit einem
   * DraftOnly-Kommando können Blöcke im Text definiert werden (auch an anderen
   * Stellen), die ausschließlich im Entwurf angezeigt werden sollen.
   */
  public static class DraftOnly extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public DraftOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    @Override
    public String getHighlightColor()
    {
      return highlightColor;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen wird der Verfügungspunkt I als
   * Original bezeichnet. Mit dem NotInOriginal Kommando ist es möglich Blöcke im
   * Text zu definieren, die NIEMALS in Originalen abgedruckt werden sollen, jedoch
   * in allen anderen Ausdrucken, die nicht das Original sind (wie z.B. Abdrücke und
   * Entwurf).
   */
  public static class NotInOriginal extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public NotInOriginal(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    @Override
    public String getHighlightColor()
    {
      return highlightColor;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen wird der Verfügungspunkt I als
   * Original bezeichnet. Mit dem OriginalOnly Kommando ist es möglich Blöcke im Text
   * zu definieren, die ausschließlich in Originalen abgedruckt werden sollen.
   */
  public static class OriginalOnly extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public OriginalOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    @Override
    public String getHighlightColor()
    {
      return highlightColor;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen werden die Ausfertigungen, die weder
   * Original noch Entwurf sind, als "Abdrucke" bezeichnet. Mit einem
   * CopyOnly-Kommando können Blöcke im Text definiert werden (auch an anderen
   * Stellen), die ausschließlich in Abdrucken angezeigt werden sollen.
   */
  public static class CopyOnly extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public CopyOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    @Override
    public String getHighlightColor()
    {
      return highlightColor;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen werden alle Verfügungspunkte
   * unterhalb des ausgewählten Verfügungspunktes ausgeblendet. Mit dem AllVersions
   * Kommando ist es möglich Blöcke im Text zu definieren, die IMMER ausgedruckt
   * werden sollen, d.h. sowohl bei Originalen, als auch bei Abdrucken und Entwürfen.
   */
  public static class AllVersions extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public AllVersions(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    @Override
    public String getHighlightColor()
    {
      return highlightColor;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Falls nach dem Einfügen eines Textbausteines keine Einfügestelle vorhanden ist
   * wird die Marke 'setJumpMark' falls vorhanden angesprungen. Wird auch falls
   * vorhanden und keine Platzhalter vorhanden ist, mit PlatzhalterAnspringen
   * angesprungen.
   */
  public static class SetJumpMark extends DocumentCommand
  {
    public SetJumpMark(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }
}
