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
 * 17.05.2006 | LUT | Dokumentation ergänzt
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
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.AllVersions;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.DraftOnly;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFunctionValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.RootElement;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Version;

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
  private XBookmarksSupplier xBookmarksSupplier;

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
   * @param xBookmarkSupplier
   *          Das Dokument, welches die Dokumentkommandos enthält.
   */
  public DocumentCommandTree(XBookmarksSupplier xBookmarksSupplier)
  {
    this.xBookmarksSupplier = xBookmarksSupplier;
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

    int compareCount = 0;
    if (xBookmarksSupplier != null)
    {
      XNameAccess bma = xBookmarksSupplier.getBookmarks();
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
          bookmark = new Bookmark(bookmarkName, xBookmarksSupplier);
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

      else if (cmd.compareToIgnoreCase("insertValue") == 0)
      {
        return new DocumentCommand.InsertValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertContent") == 0)
      {
        return new DocumentCommand.InsertContent(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("form") == 0)
      {
        return new DocumentCommand.Form(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("updateFields") == 0)
      {
        return new DocumentCommand.UpdateFields(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("version") == 0)
      {
        return new DocumentCommand.Version(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setType") == 0)
      {
        return new DocumentCommand.SetType(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertFormValue") == 0)
      {
        return new DocumentCommand.InsertFormValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertFunctionValue") == 0)
      {
        return new DocumentCommand.InsertFunctionValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setGroups") == 0)
      {
        return new DocumentCommand.SetGroups(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setPrintFunction") == 0)
      {
        return new DocumentCommand.SetPrintFunction(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("draftOnly") == 0)
      {
        return new DocumentCommand.DraftOnly(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("notInOriginal") == 0)
      {
        return new DocumentCommand.NotInOriginal(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("allVersions") == 0)
      {
        return new DocumentCommand.AllVersions(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setJumpMark") == 0)
      {
        return new DocumentCommand.SetJumpMark(wmCmd, bookmark);
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
    String str = spaces + cmd.toString() + " GROUPS: " + cmd.getGroups() + "\n";

    // und jetzt die Kinder
    Iterator i = cmd.getChildIterator();
    while (i.hasNext())
    {
      str += dumpTree(((DocumentCommand) i.next()), spaces + "    ");
    }

    return str;
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

    UnoService compo = new UnoService(xDesktop.getCurrentComponent());
    DocumentCommandTree tree = new DocumentCommandTree(compo
        .xBookmarksSupplier());
    tree.update();

    System.out.println(tree.dumpTree());

    System.exit(0);
  }

  public static class TreeExecutor implements DocumentCommand.Executor
  {
    /**
     * Kann während Ausführung eines Kommandos auf true gesetzt werden. Danach
     * wird der gesamte Baum neu aufgebaut und erneut durchlaufen.
     */
    public boolean repeatScan;

    /**
     * Durchläuft den Dokumentenbaum tree in der in reverse angegebener Richtung
     * und führt alle enthaltenen Dokumentkommandos aus, die nicht den Status
     * DONE=true oder ERROR=true besitzen. Der Wert reverse=false entspricht
     * dabei der normalen Reihenfolge einer Tiefensuche, bei reverse=true wird
     * der Baum zwar ebenfalls in einer Tiefensuche durchlaufen, jedoch wird
     * stets mit dem letzten Kind angefangen.
     * 
     * @param tree
     * @param reverse
     * @return Anzahl der bei der Ausführung aufgetretenen Fehler.
     */
    protected int executeDepthFirst(DocumentCommandTree tree, boolean reverse)
    {
      int errors = 0;
      do
      {
        repeatScan = false;

        // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
        tree.update();
        Iterator iter = tree.depthFirstIterator(reverse);
        while (iter.hasNext())
        {
          DocumentCommand cmd = (DocumentCommand) iter.next();

          if (cmd.isDone() == false && cmd.hasError() == false)
          {
            // Kommando ausführen und Fehler zählen
            errors += cmd.execute(this);
          }
        }
      } while (repeatScan);
      return errors;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFrag cmd)
    {
      return 0;
    }

    public int executeCommand(InsertValue cmd)
    {
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      return 0;
    }

    public int executeCommand(Form cmd)
    {
      return 0;
    }

    public int executeCommand(InvalidCommand cmd)
    {
      return 0;
    }

    public int executeCommand(Version cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFormValue cmd)
    {
      return 0;
    }

    public int executeCommand(SetGroups cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFunctionValue cmd)
    {
      return 0;
    }

    public int executeCommand(SetPrintFunction cmd)
    {
      return 0;
    }

    public int executeCommand(DraftOnly cmd)
    {
      return 0;
    }

    public int executeCommand(NotInOriginal cmd)
    {
      return 0;
    }

    public int executeCommand(AllVersions cmd)
    {
      return 0;
    }

    public int executeCommand(SetJumpMark cmd)
    {
      return 0;
    }
  }

}
