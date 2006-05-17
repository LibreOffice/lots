/*
 * Dateiname: DocumentCommandTree.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repräsentiert einen Baum von Dokumentkommandos
 *            eines Dokuments.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 17.05.2005 | LUT | Dokumentation ergänzt
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.RootElement;

/**
 * Diese Klasse repräsentiert einen Baum von Dokumentkommandos eines Dokuments.
 * Der Baum kann sich selbst updaten, d.h. er scannt das Dokument nach neuen
 * Bookmarks und erzeugt ggf. neue Dokumentkommandos, die er zusätzlich in den
 * Baum einhängen kann.
 */
public class DocumentCommandTree
{
  /**
   * Das Dokument, welches die DokumentCommands enthält.
   */
  private XComponent doc;

  /**
   * Ein HashSet aller bereits bekannter Bookmarks, die beim update nicht mehr
   * erneut eingelesen werden müssen.
   */
  private HashSet scannedBookmarks;

  /**
   * Das Wurzelelement des Kommando-Baumes.
   */
  private RootElement root;

  /**
   * Erzeugt einen neuen (vorerst leeren) DocumentCommandTree. Der Baum kann
   * anschließend durch die Methode update() gefüllt werden.
   * 
   * @param doc
   *          Das Dokument, welches die Dokumentkommandos enthält.
   */
  public DocumentCommandTree(XComponent doc)
  {
    this.doc = doc;
    this.scannedBookmarks = new HashSet();
    this.root = new DocumentCommand.RootElement();
  }

  /**
   * Diese Methode durchsucht das Dokument nach neuen, bisher nicht gescannten
   * Dokumentkommandos und fügt diese dem Baum hinzu. Das Kommando liefert das
   * Wurzelelement des Kommandobaumes zurück.
   * 
   * @return Das Wurzelelement des Kommandobaumes.
   */
  public DocumentCommand update()
  {
    // Folgendes Pattern prüft ob es sich bei dem Bookmark um ein gültiges
    // "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
    Pattern wmCmdPattern = Pattern
        .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

    XBookmarksSupplier bms = (XBookmarksSupplier) UnoRuntime.queryInterface(
        XBookmarksSupplier.class,
        doc);
    int compareCount = 0;
    if (bms != null)
    {
      XNameAccess bma = bms.getBookmarks();
      String[] elements = bma.getElementNames();
      for (int i = 0; i < elements.length; i++)
      {
        String bookmarkName = elements[i];

        // zum nächsten Bookmark springen wenn das Bookmark bereits bekannt ist
        if (scannedBookmarks.contains(bookmarkName))
          continue;
        else
          scannedBookmarks.add(bookmarkName);

        // Bookmark-Objekt erzeugen:
        Bookmark bookmark;
        try
        {
          bookmark = new Bookmark(bookmarkName, doc);
        }
        catch (NoSuchElementException e)
        {
          Logger.error(e);
          continue;
        }

        // Bookmark evaluieren. Ist Bookmark ein "WM"-Kommand?
        Matcher m = wmCmdPattern.matcher(bookmarkName);
        ConfigThingy wmCmd = null;
        DocumentCommand command = null;
        if (m.find())
        {
          String cmdString = m.group(1);
          try
          {
            wmCmd = new ConfigThingy("", null, new StringReader(cmdString));
          }
          catch (IOException e)
          {
            Logger.error(e);
          }
          catch (SyntaxErrorException e)
          {
            command = new InvalidCommand(bookmark, e);
          }
        }

        if (wmCmd != null && bookmark != null)
        {
          command = createCommand(wmCmd, bookmark);
        }

        if (command != null)
        {
          Logger.debug2("Adding: " + command);
          compareCount += root.add(command);
        }
      }
    }
    Logger.debug2("Update fertig - compareCount=" + compareCount);

    return root;
  }

  /**
   * Fabrikmethode für die Erzeugung neuer Dokumentkommandos an dem Bookmark
   * bookmark mit dem als ConfigThingy vorgeparsten Dokumentkommando wmCmd.
   * 
   * @param wmCmd
   *          Das vorgeparste Dokumentkommando
   * @param bookmark
   *          Das Bookmark zu dem Dokumentkommando
   * @return Ein passende konkretes DocumentCommand-Instanz.
   */
  private DocumentCommand createCommand(ConfigThingy wmCmd, Bookmark bookmark)
  {
    String cmd = "";
    try
    {
      // CMD-Attribut auswerten
      try
      {
        cmd = wmCmd.get("WM").get("CMD").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException("Fehlendes CMD-Attribut");
      }

      // spezielle Kommando-Instanzen erzeugen
      if (cmd.compareToIgnoreCase("insertFrag") == 0)
      {
        return new DocumentCommand.InsertFrag(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("insertValue") == 0)
      {
        return new DocumentCommand.InsertValue(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("insertContent") == 0)
      {
        return new DocumentCommand.InsertContent(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("form") == 0)
      {
        return new DocumentCommand.Form(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("on") == 0)
      {
        return new DocumentCommand.ON(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("off") == 0)
      {
        return new DocumentCommand.OFF(wmCmd, bookmark);
      }

      if (cmd.compareToIgnoreCase("version") == 0)
      {
        return new DocumentCommand.Version(wmCmd, bookmark);
      }

      throw new InvalidCommandException("Unbekanntes Kommando \"" + cmd + "\"");
    }
    catch (InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }

  /**
   * Diese Methode liefert einen Iterator über alle Elemente des Kommandobaumes
   * ohne dem Wurzelelement, wobei die Elemente bei reverse==false in der
   * Reihenfolge einer Tiefensuche zurückgeliefert werden - bei reverse==true
   * wird ebenfalls eine Tiefensuche durchgeführt, nur dass mit dem jeweils
   * letzten Element der Kinderliste begonnen wird.
   * 
   * @param reverse
   *          gibt an, ob bei der Tiefensuche mit dem jeweils letzten Element
   *          der Kinderliste begonnen wird.
   * @return Iterator über alle Elemente des Kommandobaumes ohne dem
   *         Wurzelelement.
   */
  public Iterator depthFirstIterator(boolean reverse)
  {
    Vector list = new Vector();
    depthFirstAddToList(root, list, reverse);
    return list.iterator();
  }

  /**
   * Hilfsfunktion für die Erstellung des depthFirstIterators - fügt das
   * Kommando cmd und dessen Kinder (rekursiv) zur Liste list hinzu. Ist
   * Reverse=true, so wird beim Hinzufügen der Kinder in umgekehrter Reihenfolge
   * verfahren.
   * 
   * @param cmd
   * @param list
   * @param reverse
   */
  private void depthFirstAddToList(DocumentCommand cmd, List list,
      boolean reverse)
  {
    // Element hinzufügen (ausser RootElement)
    if (!(cmd instanceof RootElement)) list.add(cmd);

    // Kinder hinzufügen
    ListIterator i = cmd.getChildIterator();

    while (i.hasNext())
    {
      DocumentCommand child = (DocumentCommand) i.next();
      if (!reverse) depthFirstAddToList(child, list, false);
    }
    if (!reverse) return;
    // Der vorherige Vorwärtsdurchgang positoniert zugleich den Iterator ans
    // Ende. Von da an kann man jetzt rückwärts gehen.
    while (i.hasPrevious())
    {
      DocumentCommand child = (DocumentCommand) i.previous();
      if (reverse) depthFirstAddToList(child, list, true);
    }
  }

  /**
   * Die Methode gibt eine String Repräsentation des gesamten Kommandobaumes
   * zurück und ist für test- und debugging-Zwecke gedacht.
   */
  public String dumpTree()
  {
    return dumpTree(root, "");
  }

  /**
   * Hilfmethode für dumpTree()
   * 
   * @param cmd
   * @param spaces
   * @return
   */
  private String dumpTree(DocumentCommand cmd, String spaces)
  {
    String str = spaces + this.toString() + "\n";

    // und jetzt die Kinder
    Iterator i = cmd.getChildIterator();
    while (i.hasNext())
    {
      str += dumpTree(((DocumentCommand) i.next()), spaces + "    ");
    }

    return str;
  }

  /**
   * Veranlasst alle Elemente in der Reihenfolge einer Tiefensuche ihre
   * InsertMarks zu löschen.
   */
  public void cleanInsertMarks()
  {
    // Das Löschen muss mit einer Tiefensuche, aber in umgekehrter Reihenfolge
    // ablaufen, da sonst leere Bookmarks (Ausdehnung=0) durch das Entfernen der
    // INSERT_MARKs im unmittelbar darauffolgenden Bookmark ungewollt gelöscht
    // werden. Bei der umgekehrten Reihenfolge tritt dieses Problem nicht auf.
    Iterator i = depthFirstIterator(true);
    while (i.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) i.next();
      cmd.cleanInsertMarks();
    }
  }

  /**
   * Veranlasst alle Elemente in der Reihenfolge einer Tiefensuche die ihre
   * Bookmarks im Dokument auf ihren neuen Status anzupassen um damit den Status
   * im Dokument zu manifestieren.
   */
  public void updateBookmarks(boolean debug)
  {
    Iterator i = depthFirstIterator(false);
    while (i.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) i.next();
      String oldBookmarkName = cmd.getBookmarkName();
      String newBookmarkName = cmd.updateBookmark(debug);

      // Anpassen des scannedBookmarks-Set
      if (newBookmarkName == null)
      {
        scannedBookmarks.remove(oldBookmarkName);
      }
      else if (!oldBookmarkName.equals(newBookmarkName))
      {
        scannedBookmarks.remove(oldBookmarkName);
        scannedBookmarks.add(newBookmarkName);
      }
    }
  }

  /**
   * Main-Methode für Testzwecke. Erzeugt einen Kommandobaum aus dem aktuell in
   * OOo geöffneten Dokument und gibt diesen auf System.out aus.
   * 
   * @param args
   * @throws BootstrapException
   * @throws Exception
   */
  public static void main(String[] args) throws BootstrapException, Exception
  {
    Logger.init("all");

    XComponentContext ctx = Bootstrap.bootstrap();

    XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(
        XDesktop.class,
        ctx.getServiceManager().createInstanceWithContext(
            "com.sun.star.frame.Desktop",
            ctx));

    DocumentCommandTree tree = new DocumentCommandTree(xDesktop
        .getCurrentComponent());
    tree.update();

    System.out.println(tree.dumpTree());

    System.exit(0);
  }
}
