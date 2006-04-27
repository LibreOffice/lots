package de.muenchen.allg.itd51.wollmux;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.RootElement;

public class DocumentCommandTree
{
  private XComponent doc;

  private HashSet scannedBookmarks;

  private RootElement root;

  public DocumentCommandTree(XComponent doc)
  {
    this.doc = doc;
    this.scannedBookmarks = new HashSet();
    this.root = new DocumentCommand.RootElement();
  }

  /**
   * Diese Methode durchsucht alle Bookmarks des übergebenen Dokuments und
   * erstellt daraus eine hierarchische DocumentCommand-Struktur, dessen
   * Wurzelknoten anschließend zurückgeliefert wird.
   * 
   * @param doc
   * @return
   * @throws CommandsOverlappException
   *           Das Dokument enthält zwei Bookmarks mit atomaren
   *           DocumentCommands, die sich überlappen.
   * @throws SyntaxErrorException
   * @throws IOException
   */
  public DocumentCommand update()
  {
    // Folgendes Pattern prüft ob es sich bei dem Bookmark um ein gültiges
    // "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
    Pattern wmCmdPattern = Pattern
        .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

    XMultiServiceFactory factory = (XMultiServiceFactory) UnoRuntime
        .queryInterface(XMultiServiceFactory.class, doc);

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
        Bookmark bookmark = null;
        XTextContent xTextContent = null;
        try
        {
          xTextContent = (XTextContent) UnoRuntime.queryInterface(
              XTextContent.class,
              bma.getByName(bookmarkName));
        }
        catch (java.lang.Exception x)
        {
          Logger.error(x);
        }
        if (xTextContent != null && factory != null)
        {
          bookmark = new Bookmark(xTextContent, factory);
        }

        // Bookmark evaluieren. Ist Bookmark ein "WM"-Kommand?
        Matcher m = wmCmdPattern.matcher(bookmarkName);
        ConfigThingy wmCmd = null;
        DocumentCommand command = null;
        if (m.find())
        {
          String cmdString = m.group(1);
          Logger.debug2("Parse Dokumentkommando: " + cmdString);
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
    Logger.debug2("createCommandTreeFromDocument(): fertig! compareCount="
                  + compareCount);

    return root;
  }

  public DocumentCommand createCommand(ConfigThingy wmCmd, Bookmark bookmark)
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

      throw new InvalidCommandException("Unbekanntes Kommando \"" + cmd + "\"");
    }
    catch (InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }

  public Iterator depthFirstIterator()
  {
    Vector list = new Vector();
    depthFirstAddToList(root, list);
    return list.iterator();
  }

  private void depthFirstAddToList(DocumentCommand cmd, List list)
  {
    // Element hinzufügen (ausser RootElement)
    if (!(cmd instanceof RootElement)) list.add(cmd);

    // Kinder hinzufügen
    Iterator i = cmd.getChildIterator();
    while (i.hasNext())
    {
      depthFirstAddToList((DocumentCommand) i.next(), list);
    }
  }

  public String dumpTree()
  {
    return dumpTree(root, "");
  }

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
    Iterator i = depthFirstIterator();
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
  public void updateBookmarks()
  {
    Iterator i = depthFirstIterator();
    while (i.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) i.next();
      String oldBookmarkName = cmd.getBookmarkName();
      String newBookmarkName = cmd.updateBookmark();

      // Anpassen des scannedBookmarks-Set
      if (!oldBookmarkName.equals(newBookmarkName))
      {
        scannedBookmarks.remove(oldBookmarkName);
        scannedBookmarks.add(newBookmarkName);
      }
    }
  }

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
