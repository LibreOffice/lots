/*
 * Dateiname: DocumentCommand.java
 * Projekt  : WollMux
 * Funktion : Beschreibt ein Element aus einer hierarchischen Struktur von
 *            DokumentKommandos und dessen Status.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 07.11.2005 | LUT | Erstellung als WMCommandState
 * 23.01.2006 | LUT | Erweiterung zum hierarchischen WM-Kommando
 * 26.04.2006 | LUT | Komplette Überarbeitung und Umbenennung in DocumentCommand
 * 17.05.2006 | LUT | Doku überarbeitet
 * 13.11.2006 | BAB | Erweitern von 'insertFrag' um optionale Argumente 'ARGS'
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;

/**
 * Beschreibt ein Dokumentkommando, das als Element eines hierarchischen
 * Kommandobaumes weitere Kinder-Elemente enthalten kann und weitere
 * Eigenschaften wie z.B. die Gruppenzugehörigkeit, Sichtbarkeit und
 * Ausführstatus besitzt.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
abstract public class DocumentCommand
{
  /**
   * Das geparste ConfigThingy des zugehörenden Bookmarks.
   */
  protected ConfigThingy wmCmd;

  /**
   * Die Liste aller Kinder-Elemente.
   */
  private LinkedList childs; // Kinder vom Typ DocumentCommand

  /**
   * Der Vaterknoten des Dokumentkommandos im Kommando-Baum.
   */
  private DocumentCommand parent;

  /**
   * Das zu diesem DokumentCommand gehörende Bookmark.
   */
  protected Bookmark bookmark;

  // Status-Attribute:

  /**
   * Vorbelegung für den Status DONE.
   */
  private static final Boolean STATE_DEFAULT_DONE = new Boolean(false);

  /**
   * Vorbelegung für den Status ERROR
   */
  private static final Boolean STATE_DEFAULT_ERROR = new Boolean(false);

  /**
   * Enthält den aktuellen DONE-Status oder null, falls der Wert nicht verändert
   * wurde.
   */
  private Boolean done;

  /**
   * Enthält den aktuellen EROOR-Status oder null, falls der Wert nicht
   * verändert wurde.
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

  // Relationen zweier Dokument-Kommandos A und B:

  private static final int REL_B_IS_CHILD_OF_A = 1;

  private static final int REL_B_IS_PARENT_OF_A = -1;

  private static final int REL_B_OVERLAPS_A = 0;

  private static final int REL_B_IS_SIBLING_BEFORE_A = -2;

  private static final int REL_B_IS_SIBLING_AFTER_A = 2;

  /* ************************************************************ */

  /**
   * Der Konstruktor liefert eine Instanz eines DocumentCommand an der Stelle
   * des Bookmarks bookmark mit dem geparsten Kommando wmCmd.
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
    this.parent = null;
    this.childs = new LinkedList();
    this.hasInsertMarks = false;
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
   * Diese Methode liefert eine String-Repräsentation des DokumentCommands
   * zurück. Die String-Repräsentation hat den Aufbau DocumentCommand[<bookmarkName>].
   */
  public String toString()
  {
    return ""
           + this.getClass().getSimpleName()
           + "["
           + (isRetired() ? "RETIRED:" : "")
           + (isDone() ? "DONE:" : "")
           + getBookmarkName()
           + "]";
  }

  /**
   * Fügt dem aktuellen Dokumentkommando ein neues Kind-Element b hinzu. Dabei
   * werden die TextRanges der beiden Kommandos verglichen und deren Relation
   * bestimmt. Je nach Relation wird das neue Kommando als Kind vor oder nach
   * einem bereits bestehenden Kind eingehängt oder die Anfrage an ein
   * Kind-Element weitergereicht.
   * 
   * @param b
   *          Das Kindelement b
   * @return Die Anzahl der aufkummulierten TextRange Vergleiche, die zum
   *         Hinzufügen benötigt wurden. Kann für Performance-Statistiken
   *         verwendet werden.
   */
  protected int add(DocumentCommand b)
  {
    int compareCount = 0;

    ListIterator childsIterator = childs.listIterator(childs.size());
    while (childsIterator.hasPrevious())
    {
      DocumentCommand a = (DocumentCommand) childsIterator.previous();
      int rel = a.getRelation(b);
      compareCount++;

      // b liegt vor a: weiter gehen
      if (rel == REL_B_IS_SIBLING_BEFORE_A)
      {
        // Weiter gehen
      }

      // b liegt nach a: danach einfügen
      if (rel == REL_B_IS_SIBLING_AFTER_A)
      {
        b.parent = this;
        childsIterator.next();
        childsIterator.add(b);
        return compareCount;
      }

      // b ist Vater von a: a aushängen und zum Kind von b machen.
      if (rel == REL_B_IS_PARENT_OF_A)
      {
        a.parent = b;
        b.add(a);
        childsIterator.remove();
      }

      // a ist Vater von b:
      if (rel == REL_B_IS_CHILD_OF_A)
      {
        compareCount += a.add(b);
        return compareCount;
      }

      // identische ranges
      if (rel == REL_B_OVERLAPS_A)
      {
        Logger
            .error("Ignoriere Dokumentkommando \""
                   + b.getBookmarkName()
                   + "\", da es sich mit dem Dokumentkommando \""
                   + a.getBookmarkName()
                   + "\" überlappt. Diese beiden Kommandos dürfen sich nicht überlappen!");
        return compareCount;
      }
    }

    // falls bisher noch nicht hinzugefügt: am Anfang einfügen.
    b.parent = this;
    childsIterator.add(b);
    return compareCount;
  }

  /**
   * Die Methode prüft in allen Kindern und Kindeskindern (rekusiv), ob ihre
   * Bookmarks noch vorhanden sind und entfernt alle Kinder, deren Bookmarks
   * nicht mehr vorhanden sind.
   * 
   * @return liefert true, wenn mindestens ein Kind oder Kindeskind entfernt
   *         wurde, ansonsten false.
   */
  public boolean removeRetieredChilds()
  {
    boolean changed = false;

    Vector lostChildren = new Vector();
    Iterator iter = childs.iterator();
    while (iter.hasNext())
    {
      DocumentCommand child = (DocumentCommand) iter.next();

      // zuerst bei den Kindern aufräumen
      if (child.removeRetieredChilds() == true) changed = true;

      // und dann selber aufräumen
      if (child.isRetired())
      {
        // Ungültiges Element entfernen
        iter.remove();
        changed = true;

        // Verloren gegangene Kinder merken
        Iterator lost = child.childs.iterator();
        while (lost.hasNext())
          lostChildren.add(lost.next());
      }
    }

    // Verloren gegangene Kinder wieder hinzufügen:
    iter = lostChildren.iterator();
    while (iter.hasNext())
      add((DocumentCommand) iter.next());

    return changed;
  }

  /**
   * Liefert einen ListIterator über alle Kinder-Elemente des Dokumentkommandos.
   * 
   * @return Liefert einen ListIterator über alle Kinder-Elemente des
   *         Dokumentkommandos.
   */
  protected ListIterator getChildIterator()
  {
    return childs.listIterator();
  }

  /**
   * Diese Methode liefert die Beziehung des aktuellen DocumentCommand und des
   * DocumentCommand b in Form einer Konstante DocumentCommand.REL_xxx zurück.
   * 
   * @param b
   *          Das DocumentCommand b, mit dem das aktuelle DocumentCommand
   *          verglichen wird.
   * @return Die Relation der zwei DocumentCommands in Form einer Konstante
   *         DocumentCommand.REL_xxx.
   */
  protected int getRelation(DocumentCommand b)
  {
    int cmp = bookmark.compare(b.bookmark);

    if (cmp == Bookmark.POS_AABB) return REL_B_IS_SIBLING_AFTER_A;
    if (cmp == Bookmark.POS_BBAA) return REL_B_IS_SIBLING_BEFORE_A;

    if (cmp == Bookmark.POS_88AA) return REL_B_IS_CHILD_OF_A;
    if (cmp == Bookmark.POS_A88A) return REL_B_IS_CHILD_OF_A;
    if (cmp == Bookmark.POS_AA88) return REL_B_IS_CHILD_OF_A;

    if (cmp == Bookmark.POS_88BB) return REL_B_IS_PARENT_OF_A;
    if (cmp == Bookmark.POS_B88B) return REL_B_IS_PARENT_OF_A;
    if (cmp == Bookmark.POS_BB88) return REL_B_IS_PARENT_OF_A;

    if (cmp == Bookmark.POS_8888)
    {
      if (this.canHaveChilds() && b.canHaveChilds())
        return REL_B_IS_CHILD_OF_A;
      else if (this.canHaveChilds())
        return REL_B_IS_CHILD_OF_A;
      else if (b.canHaveChilds()) return REL_B_IS_PARENT_OF_A;
      return REL_B_OVERLAPS_A;
    }

    return REL_B_IS_SIBLING_AFTER_A;
  }

  /**
   * Diese Methode gibt Auskunft darüber, ob ein DocumentCommand Unterkommandos
   * besitzen darf. Ein Kommando darf Kinder besitzen, wenn es theoretisch mehr
   * als einen atomaren Textinhalt umschließen kann. Ein Atom (kleinste, nicht
   * teilbare Einheit) selbst darf jedoch keine Kinder besitzen. Beispiel: Ein
   * WM(CMD'insertFrag'...) kann nach der Auflösung mehrere Unterelemente
   * besitzen und würde daher "true" zurückliefern. Ein Kommando
   * WM(CMD'insertValue'...) hingegen repräsentiert exakt einen atomaren Wert
   * und darf keine weiteren Kinder mehr enthalten.
   * 
   * @return
   */
  protected abstract boolean canHaveChilds();

  /**
   * Callbackfunktion für die Ausführung des Dokumentkommandos in einem
   * DocumentCommand.Executor wie z.B. dem DocumentCommandInterpreter. Die
   * Methode liefert die Anzahl der bei der Ausführung entstandenen Fehler
   * zurück.
   * 
   * @param executor
   * @return Anzahl der aufgetretenen Fehler
   */
  public abstract int execute(DocumentCommand.Executor executor);

  /**
   * Diese Methode ermöglicht den Zugriff auf den vollständigen TextRange des
   * Bookmarks, welches das DocumentCommand definiert - die Methode darf aber
   * nur verwendet werden, um die Inhalte an dieser Positon auszulesen und nicht
   * um neue Textinhalte an dieser Stelle einzufügen (dafür gibt es
   * createInsertCursor()); ist das Bookmark nicht (mehr) vorhanden, wird null
   * zurück geliefert.
   * 
   * @return
   */
  public XTextRange getTextRange()
  {
    return bookmark.getTextRange();
  }

  /**
   * Liefert die TextRange an der das Bookmark dieses Kommandos verankert ist
   * oder null, falls das Bookmark nicht mehr existiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public XTextRange getAnchor()
  {
    return bookmark.getAnchor();
  }

  /**
   * Erzeugt einen Cursor, der ein sicheres Einfügen von Inhalten an der
   * Cursorposition gewährleistet, ohne dabei die Hierarchie oder Ordnung der
   * Dokumentkommandos zu zerstören. Kann das Dokumentkommando Kinder besitzen,
   * so wird der Cursor in INSERT_MARKs eingeschlossen. Kann das
   * Dokumentkommando keine Kinder besitzen, so wird ein einfacher TextCursor
   * aus dem vollen TextRange des Bookmarks erzeugt. Ist das Bookmark nicht
   * (mehr) vorhanden, wird null zurück geliefert. Jeder Aufruf von
   * createInsertCursor überschreibt die bestehenden Inhalte des bisherigen
   * DocumentCommands. Der InsertCursor von Dokumentkommandos mit Kindern sieht
   * in etwa wie folgt aus: "<" CURSOR ">". Der InsertCursor hat direkt nach
   * der Erzeugung keine Ausdehnung. Die INSERT_MARKS "<" und ">" können nach
   * Beendigung der Dokumentgenerierung über die Methode cleanInsertMarks()
   * wieder entfernt werden.
   * 
   * @return XTextCursor zum Einfügen oder null
   */
  public XTextCursor createInsertCursor()
  {
    XTextRange range = bookmark.getTextRange();

    if (range != null)
    {
      if (canHaveChilds())
      {
        // wenn
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        cursor.setString(INSERT_MARK_OPEN + INSERT_MARK_CLOSE);
        hasInsertMarks = true;

        bookmark.rerangeBookmark(cursor);

        cursor.goRight(getStartMarkLength(), false);
        cursor.collapseToStart();

        return cursor;
      }
      else
        return range.getText().createTextCursorByRange(range);
    }
    return null;
  }

  /**
   * Liefert die Länge des End-Einfügemarkers.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public short getEndMarkLength()
  {
    return (short) INSERT_MARK_CLOSE.length();
  }

  /**
   * Liefert die Länge des Start-Einfügemarkers.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public short getStartMarkLength()
  {
    return (short) INSERT_MARK_OPEN.length();
  }

  /**
   * Liefert entweder null falls kein Start-Einfügemarke vorhanden oder liefert
   * 2 Cursor, von denen der erste links neben der zweite rechts neben der
   * Start-Einfügemarke steht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public XParagraphCursor[] getStartMark()
  {
    XTextRange range = bookmark.getTextRange();
    if (range == null || !hasInsertMarks) return null;
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range
        .getStart()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[1].goRight(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert entweder null falls keine End-Einfügemarke vorhanden oder liefert 2
   * Cursor, von denen der erste links neben der zweite rechts neben der
   * End-Einfügemarke steht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public XParagraphCursor[] getEndMark()
  {
    XTextRange range = bookmark.getTextRange();
    if (range == null || !hasInsertMarks) return null;
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range
        .getEnd()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[0].goLeft(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert true zurück, wenn das Dokumentkommando insertMarks besitzt, die
   * zuvor über den Aufruf von createInsertCursor() erzeugt worden sind,
   * ansonsten false.
   */
  public boolean hasInsertMarks()
  {
    return hasInsertMarks;
  }

  /**
   * Teilt dem Dokumentkommando mit, dass die insertMarks des Dokumentkommandos
   * entfernt wurden und hasInsertMarks() in Folge dessen false zurück liefern
   * muss.
   */
  public void unsetHasInsertMarks()
  {
    this.hasInsertMarks = false;
  }

  /**
   * Liefert true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr
   * existiert und das Dokumentkommando daher nicht mehr zu gebrauchen ist oder
   * andernfalls false.
   * 
   * @return true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr
   *         existiert, ansonsten false.
   */
  public boolean isRetired()
  {
    if (bookmark != null) return bookmark.getAnchor() == null;
    return false;
  }

  /**
   * Beschreibt ob das Kommando bereits abgearbeitet wurde. Ist DONE bisher noch
   * nicht definiert oder gesetzt worden, so wird der Defaultwert false
   * zurückgeliefert.
   * 
   * @return true, falls das Kommando bereits bearbeitet wurde, andernfalls
   *         false.
   */
  public boolean isDone()
  {
    if (done != null)
      return done.booleanValue();
    else if (isDefinedState("DONE"))
    {
      String doneStr = getState("DONE").toString();
      if (doneStr.compareToIgnoreCase("true") == 0)
        return true;
      else
        return false;
    }
    else
      return STATE_DEFAULT_DONE.booleanValue();
  }

  /**
   * Markiert ein Dokumentkommando als bearbeitet; Das Dokumentkommando wird
   * daraufhin aus dem Dokument gelöscht, wenn removeBookmark==true oder
   * umbenannt, wenn removeBookmark==false.
   * 
   * @param removeBookmark
   *          true, signalisiert, dass das zugehörige Bookmark gelöscht werden
   *          soll. False signalisiert, dass das Bookmark mit dem Zusatz "<alterName>
   *          STATE(DONE 'true')" versehen wird.
   */
  public void markDone(boolean removeBookmark)
  {

    this.done = Boolean.TRUE;
    flushToBookmark(removeBookmark);
  }

  /**
   * Liefert den Fehler-Status der Kommandobearbeitung zurück. Ist das Attribut
   * ERROR bisher nicht definiert oder kein Fehler gesetzt worden, so wird der
   * Defaultwert false zurückgliefert.
   * 
   * @return true, wenn bei der Kommandobearbeitung Fehler auftraten;
   *         andernfalls false
   */
  public boolean hasError()
  {
    if (error != null)
      return error.booleanValue();
    else if (isDefinedState("ERROR"))
    {
      Boolean errorBool = new Boolean(getState("ERROR").toString());
      return errorBool.booleanValue();
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
    this.error = new Boolean(error);
  }

  /**
   * Liefert ein ConfigThingy, das ein "WM"-Kommando mit allen
   * Statusinformationen enthält. Neue Unterknoten werden dabei nur angelegt,
   * wenn dies unbedingt erforderlich ist, d.h. wenn ein Wert vom Defaultwert
   * abweicht oder der Wert bereits vorher gesetzt war.
   * 
   * @return Ein ConfigThingy, das das "WM"-Kommando mit allen
   *         Statusinformationen enthält.
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
      setOrCreate("DONE", "" + isDone() + "");
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
      setOrCreate("ERRORS", "" + hasError() + "");
    }

    return wmCmd;
  }

  /**
   * Liefert den Inhalt des übergebenen ConfigThingy-Objekts (üblicherweise das
   * wmCmd) als einen String, der geeignet ist, um den ihn in Bookmarknamen
   * verwenden zu können. D.h. alle Newline, Komma und andere für Bookmarknamen
   * unverträgliche Zeichen werden entfernt.
   * 
   * @param conf
   *          Das ConfigThingy-Objekt, zu dem die Stringrepräsentation erzeugt
   *          werden soll.
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
   * löscht ein Bookmark, wenn der Status DONE=true gesetzt ist - Die Methode
   * liefert entweder den Namen des neuen Bookmarks, welches die neuen
   * Statusinformationen enthält zurück, oder null, wenn das zugehörige Bookmark
   * gelöscht wurde. Ist der DEBUG-modus gesetzt, so werden in gar keinem Fall
   * Bookmarks gelöscht, womit die Fehlersuche erleichtert werden soll.
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
      if (!wmCmdString.equals(name)) bookmark.rename(wmCmdString);

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
    return (getState(key) != null);
  }

  /**
   * Liefert das ConfigThingy zu dem gesuchten Key key unterhalt des
   * STATE-Knotens.
   * 
   * @param key
   * @return
   */
  protected ConfigThingy getState(String key)
  {
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
      return state.get(key);
    }
    catch (NodeNotFoundException e1)
    {
      return null;
    }
  }

  /**
   * Setzt einen Schlüssel-Wert-Paar unterhalb des STATE-Knotens. Ist der
   * Schlüssel bereits definiert, wird der bestehende Wert überschrieben. Sind
   * der STATE-Knoten oder der Schlüssel nicht definiert, so werden die
   * entsprechenden Knoten erzeugt und der Key key erhält ein Kindknoten mit dem
   * Value value.
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
   * gibt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando
   * zurück, der über das CharHidden-Attribut realisiert ist.
   * 
   * @return true=sichtbar, false=ausgeblendet
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Setzt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando
   * in dem das CharHidden-Attribut des entsprechenden TextCursors gesetzt wird.
   * Der Status visible wird nicht persistent im Bookmark hinterlegt.
   * 
   * @param visible
   *          true=sichtbar, false=ausgeblendet
   */
  public void setVisible(boolean visible)
  {
    this.visible = visible;
    XTextRange range = getTextRange();
    if (range != null)
    {
      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      UNO.setProperty(cursor, "CharHidden", new Boolean(!visible));
    }
  }

  /**
   * Liefert alle Gruppen, die dem Dokumentkommando zugeordnet sind, wobei jede
   * Gruppe auch die Gruppenzugehörigkeit des Vaterelements im
   * DocumentCommand-Baum erbt.
   * 
   * @return Ein Set, das alle zugeordneten groupId's, einschließlich der vom
   *         Vater geerbten, als Strings enthält.
   */
  public Set getGroups()
  {
    // GROUPS-Attribut auslesen falls vorhanden.
    ConfigThingy groups = new ConfigThingy("");
    try
    {
      groups = wmCmd.get("GROUPS");
    }
    catch (NodeNotFoundException e)
    {
    }

    Set groupsSet = (parent != null) ? parent.getGroups() : new HashSet();
    Iterator i = groups.iterator();
    while (i.hasNext())
    {
      String groupId = i.next().toString();
      groupsSet.add(groupId);
    }

    return groupsSet;
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
    public int executeCommand(DocumentCommand.RootElement cmd);

    public int executeCommand(DocumentCommand.InsertFrag cmd);

    public int executeCommand(DocumentCommand.InsertValue cmd);

    public int executeCommand(DocumentCommand.InsertContent cmd);

    public int executeCommand(DocumentCommand.Form cmd);

    public int executeCommand(DocumentCommand.InvalidCommand cmd);

    public int executeCommand(DocumentCommand.Version cmd);

    public int executeCommand(DocumentCommand.UpdateFields cmd);

    public int executeCommand(DocumentCommand.SetType cmd);

    public int executeCommand(DocumentCommand.InsertFormValue cmd);

    public int executeCommand(DocumentCommand.InsertFunctionValue cmd);

    public int executeCommand(DocumentCommand.SetGroups cmd);

    public int executeCommand(DocumentCommand.SetPrintFunction cmd);

    public int executeCommand(DocumentCommand.DraftOnly cmd);

    public int executeCommand(DocumentCommand.NotInOriginal cmd);

    public int executeCommand(DocumentCommand.AllVersions cmd);

    public int executeCommand(DocumentCommand.SetJumpMark cmd);
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das einen Wert in das Dokument einfügt,
   * der über eine optionale Transformation umgewandelt werden kann (Derzeit
   * implementieren insertValue und insertFormValue dieses Interface).
   */
  public static interface OptionalTrafoProvider
  {
    public String getTrafoName();
  }

  // ********************************************************************************
  /**
   * Eine Exception die geworfen wird, wenn ein Dokumentkommando als ungültig
   * erkannt wurde, z,b, aufgrund eines fehlenden Parameters.
   */
  static public class InvalidCommandException extends Exception
  {
    private static final long serialVersionUID = -3960668930339529734L;

    public InvalidCommandException(String message)
    {
      super(message);
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das aufgrund eines Syntax-Fehlers oder
   * eines fehlenden Parameters ungültig ist, jedoch trotzdem im Dokument als
   * Fehlerfeld dargestellt werden soll.
   * 
   * @author lut
   * 
   */
  static public class InvalidCommand extends DocumentCommand
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

    protected boolean canHaveChilds()
    {
      return true;
    }

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
   * Beschreibt ein noch nicht implementiertes Dokumentkommando, das jedoch für
   * die Zukunft geplant ist und dess Ausführung daher keine Fehler beim
   * Erzeugen des Briefkopfs liefern darf.
   */
  static public class NotYetImplemented extends DocumentCommand
  {
    public NotYetImplemented(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      markDone(false);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt das Dokumentkommando Form, welches Zugriff auf die
   * Formularbeschreibung des Dokuments ermöglicht, die in Form einer Notiz
   * innerhalb des zum Form-Kommando zugehörigen Bookmarks abgelegt ist.
   * 
   * @author lut
   * 
   */
  static public class Form extends DocumentCommand
  {
    public Form(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses besondere Element dient als Wurzel für den DocumentCommandTree.
   */
  static public class RootElement extends DocumentCommand
  {
    public RootElement()
    {
      super(new ConfigThingy("WM"), null);
    }

    public String getBookmarkName()
    {
      return "<root>";
    }

    protected int getRelation(DocumentCommand b)
    {
      return REL_B_IS_CHILD_OF_A;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor e)
    {
      return e.executeCommand(this);
    }

    public Set getGroups()
    {
      return new HashSet();
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertFrag fügt ein externes Textfragment in das Dokument ein.
   */
  static public class InsertFrag extends DocumentCommand
  {
    private String fragID;

    private Vector args = null;

    private String mode;

    private boolean manualMode = false;

    public InsertFrag(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      try
      {
        fragID = wmCmd.get("WM").get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException("Fehlendes Attribut FRAG_ID");
      }

      args = new Vector();
      try
      {
        ConfigThingy argsConf = wmCmd.get("WM").get("ARGS");
        Iterator iter = argsConf.iterator();
        while (iter.hasNext())
        {
          ConfigThingy arg = (ConfigThingy) iter.next();
          args.add(arg.getName());
        }
      }
      catch (NodeNotFoundException e)
      {
        // ARGS sind optional
      }

      try
      {
        mode = wmCmd.get("WM").get("MODE").toString();
        if (mode.equals("manual"))
        {
          manualMode = true;
        }
      }
      catch (NodeNotFoundException e)
      {
        // MODE ist optional;
      }

    }

    public String getFragID()
    {
      return fragID;
    }

    public Vector getArgs()
    {
      return args;
    }

    public boolean isManualMode()
    {
      return manualMode;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected int add(DocumentCommand b)
    {
      if (b instanceof InsertFrag)
      {
        InsertFrag bif = (InsertFrag) b;
        // Falls sich ein Fragment selbst aufruft, wandle das Kommando um in ein
        // InvalidCommand - zur Vermeidung von Endlosschleifen.
        if (!isManualMode())
        {
          if (this.getFragID().equals(bif.getFragID()))
          {
            InvalidCommandException ivc = new InvalidCommandException(
                "Das Fragment mit der FRAG_ID \""
                    + fragID
                    + "\" ruft sich direkt oder indirekt selbst auf "
                    + "und würde damit eine Endlosschleife verursachen.");
            b = new InvalidCommand(b.wmCmd, b.bookmark, ivc);
          }
        }
      }
      return super.add(b);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertContent dient zum Mischen von Dokumenten und ist im
   * Handbuch des WollMux ausführlicher beschrieben.
   */
  static public class InsertContent extends DocumentCommand
  {
    private String fragID;

    public InsertContent(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    public String getFragID()
    {
      return fragID;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  static public class InsertValue extends DocumentCommand implements
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
        throw new InvalidCommandException("Fehlendes Attribut DB_SPALTE");
      }

      // Auswertung der AUTOSEP bzw SEPERATOR-Attribute
      Iterator autoseps = wmCmd.query("AUTOSEP").iterator();
      Iterator seps = wmCmd.query("SEPARATOR").iterator();
      String currentSep = " "; // mit Default-Separator vorbelegt

      while (autoseps.hasNext())
      {
        ConfigThingy as = (ConfigThingy) autoseps.next();
        String sep = currentSep;
        if (seps.hasNext()) sep = ((ConfigThingy) seps.next()).toString();

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
              "Unbekannter AUTOSEP-Typ \""
                  + as.toString()
                  + "\". Erwarte \"left\", \"right\" oder \"both\".");
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

    protected boolean canHaveChilds()
    {
      return false;
    }

    public String getLeftSeparator()
    {
      return leftSeparator;
    }

    public String getRightSeparator()
    {
      return rightSeparator;
    }

    public String getTrafoName()
    {
      return trafo;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  static public class InsertFormValue extends DocumentCommand implements
      OptionalTrafoProvider
  {
    private String id = null;

    private String trafo = null;

    private String md5 = null;

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
        throw new InvalidCommandException("Fehlendes Attribut ID");
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

    public String getID()
    {
      return id;
    }

    public String getTrafoName()
    {
      return trafo;
    }

    public String getMD5()
    {
      if (md5 != null)
        return md5;
      else if (isDefinedState("MD5")) return getState("MD5").toString();
      return null;
    }

    public void setMD5(String md5Str)
    {
      md5 = md5Str;
      flushToBookmark(false);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected ConfigThingy toConfigThingy()
    {
      // MD5 im wmCmd setzen:
      if (md5 != null) setOrCreate("MD5", md5);

      return super.toConfigThingy();
    }

  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt den Rückgabewert einer Funktion in den Briefkopf ein.
   */
  static public class InsertFunctionValue extends DocumentCommand
  {
    private Vector args = null;

    private String function = null;

    public InsertFunctionValue(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      try
      {
        function = wmCmd.get("WM").get("FUNCTION").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException("Fehlendes Attribut FUNCTION");
      }

      args = new Vector();
      try
      {
        ConfigThingy argsConf = wmCmd.get("WM").get("ARGS");
        Iterator iter = argsConf.iterator();
        while (iter.hasNext())
        {
          ConfigThingy arg = (ConfigThingy) iter.next();
          args.add(arg.getName());
        }
      }
      catch (NodeNotFoundException e)
      {
        // ARGS sind optional
      }
    }

    public String getFunctionName()
    {
      return function;
    }

    public Vector getArgs()
    {
      return args;
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando fügt die Versionsnummer der aktuellen WollMux-Installation
   * in das Dokument ein.
   */
  static public class Version extends DocumentCommand
  {
    public Version(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * dieses Kommando sorgt dafür, dass alle unter dem Bookmark liegenden
   * TextFields geupdatet werden.
   */
  static public class UpdateFields extends DocumentCommand
  {
    public UpdateFields(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Über dieses Dokumentkommando kann der Typ des Dokuments festgelegt werden.
   * Es gibt die Typen SETTYPE_normalTemplate, SETTYPE_templateTemplate und
   * SETTYPE_formDocument. Die SetType-Kommandos werden bereits im
   * OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr
   * für den DocumentCommandInterpreter.
   */
  static public class SetType extends DocumentCommand
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
        throw new InvalidCommandException("Fehlendes Attribut TYPE");
      }
      if (type.compareToIgnoreCase("templateTemplate") != 0
          && type.compareToIgnoreCase("normalTemplate") != 0
          && type.compareToIgnoreCase("formDocument") != 0)
        throw new InvalidCommandException(
            "Angegebener TYPE ist ungültig oder falsch geschrieben. Erwarte \"templateTemplate\", \"normalTemplate\" oder \"formDocument\"!");
    }

    String getType()
    {
      return type;
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Über dieses Dokumentkommando kann der Typ des Dokuments festgelegt werden.
   * Es gibt die Typen SETTYPE_normalTemplate, SETTYPE_templateTemplate und
   * SETTYPE_formDocument. Die SetType-Kommandos werden bereits im
   * OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr
   * für den DocumentCommandInterpreter.
   */
  static public class SetPrintFunction extends DocumentCommand
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
        throw new InvalidCommandException("Fehlendes Attribut FUNCTION");
      }
    }

    /**
     * Erzeugt ein neues SetPrintFunction-Dokumentkomando für das Dokument doc
     * das an der Position range liegt und auf die Druckfunktion functionName
     * verweist.
     * 
     * @param doc
     *          das Dokument an dessen doc.Text.Start das neue Bookmark erzeugt
     *          werden soll.
     * @param functionName
     *          der Name der Druckfunktion
     * @param range
     *          Die TextRange, an der das zugehörige Bookmark erzeugt werden
     *          soll.
     * @return das neue SetPrintFunction-Dokumentkommando
     */
    public SetPrintFunction(XTextDocument doc, XTextRange range,
        String functionName)
    {
      super(null, new Bookmark("setPrintFunction_tmp", doc, range));
      setFunctionName(functionName);
      this.funcName = functionName;
    }

    /**
     * Setzt den neuen Funktionsnamen im Attribut FUNCTION auf den Bezeichner
     * functionName oder logged eine Fehlermeldung, falls es sich bei der
     * functionName um einen ungültigen Funktionsbezeichner handelt.
     * 
     * @param functionName
     */
    public void setFunctionName(String functionName)
    {
      if (functionName == null
          || !functionName.matches("^([a-zA-Z_][a-zA-Z_0-9]*)$"))
      {
        Logger.error("SetPrintFunction: ungültiger Funktionsbezeichner '"
                     + functionName
                     + "'");
        return;
      }

      try
      {
        wmCmd = new ConfigThingy("", null, new StringReader(
            "WM(CMD 'setPrintFunction' FUNCTION '" + functionName + "')"));
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      flushToBookmark(false);
    }

    public String getFunctionName()
    {
      return funcName;
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando SetGrups dient dazu, dem vom Bookmark umschlossenen Textinhalt
   * eine oder mehrere Gruppen zuweisen zu können. Im gegensatz zu anderen
   * Dokumentkommandos, die auch das GROUPS Attribut unterstützen, besitzt
   * dieses Kommando ausser der Zuordnung von Gruppen keine weitere Funktion.
   */
  static public class SetGroups extends DocumentCommand
  {
    public SetGroups(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen wird die Ausfertigung, die ALLE
   * definierten Verfügungpunkte enthält als "Entwurf" bezeichnet. Mit einem
   * DraftOnly-Kommando können Blöcke im Text definiert werden (auch an anderen
   * Stellen), die ausschließlich im Entwurf angezeigt werden sollen.
   */
  static public class DraftOnly extends DocumentCommand
  {
    public DraftOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen wird der Verfügungspunkt I als
   * Original bezeichnet. Mit dem NotInOriginal Kommando ist es möglich Blöcke
   * im Text zu definieren, die NIEMALS in Originalen abgedruckt werden sollen,
   * jedoch in allen anderen Ausdrucken, die nicht das Original sind (wie z.B.
   * Abdrücke und Entwurf).
   */
  static public class NotInOriginal extends DocumentCommand
  {
    public NotInOriginal(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verfügungen werden alle Verfügungspunkte
   * unterhalb des ausgewählten Verfügungspunktes ausgeblendet. Mit dem
   * AllVersions Kommando ist es möglich Blöcke im Text zu definieren, die IMMER
   * ausgedruckt werden sollen, d.h. sowohl bei Originalen, als auch bei
   * Abdrucken und Entwürfen.
   */
  static public class AllVersions extends DocumentCommand
  {
    public AllVersions(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Falls nach dem Einfügen eines Textbausteines keine Einfügestelle vorhanden
   * ist wird die Marke 'setJumpMark' falls vorhanden angesprungen. Wird auch
   * falls vorhanden und keine Platzhalter vorhanden ist, mit
   * PlatzhalterAnspringen angesprungen.
   */
  static public class SetJumpMark extends DocumentCommand
  {
    public SetJumpMark(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }
}
