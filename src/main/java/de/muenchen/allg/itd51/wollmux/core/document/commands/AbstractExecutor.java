package de.muenchen.allg.itd51.wollmux.core.document.commands;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontWeight;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockCommand;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Implementiert einen leer-Executor, von dem abgeleitet werden kann, um konkrete
 * Executoren zu schreiben, mit denen die Dokumentkommandos, die
 * DocumentCommands.iterator() liefert bearbeitet werden können.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public abstract class AbstractExecutor implements DocumentCommand.Executor
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractExecutor.class);

  /**
   * Führt alle Dokumentkommandos aus commands aus, die nicht den Status DONE=true
   * oder ERROR=true besitzen.
   *
   * @param commands
   * @return Anzahl der bei der Ausführung aufgetretenen Fehler.
   */
  protected int executeAll(DocumentCommands commands)
  {
    int errors = 0;

    // Alle DocumentCommands durchlaufen und mit execute aufrufen.
    for (Iterator<DocumentCommand> iter = commands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();

      if (!cmd.isDone() && !cmd.hasError())
      {
        // Kommando ausführen und Fehler zählen
        errors += cmd.execute(this);
      }
    }
    return errors;
  }

  @Override
  public int executeCommand(InsertFrag cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(InsertValue cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(InsertContent cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(Form cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(InvalidCommand cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(UpdateFields cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(SetType cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(InsertFormValue cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(SetGroups cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(SetPrintFunction cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(PrintBlockCommand cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(SetJumpMark cmd)
  {
    return 0;
  }

  @Override
  public int executeCommand(OverrideFrag cmd)
  {
    return 0;
  }

  /**
   * Diese Methode fügt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  public static void insertErrorField(DocumentCommand cmd, XTextDocument doc, java.lang.Exception e)
  {
    String msg = L.m("Fehler in Dokumentkommando '%1'", cmd.getBookmarkName());
    String property = msg + ":\n\n";

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
    {
      LOGGER.error(msg, e);
      property += e.getMessage();
    }
    else
    {
      LOGGER.error(msg);
    }

    cmd.setTextRangeString(L.m("<FEHLER:  >"));

    // Cursor erst NACH Aufruf von cmd.setTextRangeString(...) holen, da Bookmark
    // eventuell dekollabiert wird!
    XTextCursor insCursor = cmd.getTextCursor();
    if (insCursor == null)
    {
      LOGGER.error(L.m("Kann Fehler-Feld nicht einfügen, da kein InsertCursor erzeugt werden konnte."));
      return;
      // Anmerkung: Aufruf von cmd.setTextRangeString() oben macht nichts, falls kein
      // InsertCursor erzeugt werden kann, daher kein Problem, dass die Abfrage nach
      // insCursor == null erst danach geschieht
    }

    // Text fett und rot machen:
    Utils.setProperty(insCursor, UnoProperty.CHAR_COLOR, Integer.valueOf(0xff0000));
    Utils.setProperty(insCursor, UnoProperty.CHAR_WEIGHT, Float.valueOf(FontWeight.BOLD));

    // Ein Annotation-Textfield erzeugen und einfügen:
    try
    {
      XTextRange range = insCursor.getEnd();
      XTextCursor c = range.getText().createTextCursorByRange(range);
      c.goLeft((short) 2, false);
      XTextContent note = UNO.XTextContent(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_ANNOTATION, doc));
      UnoProperty.setProperty(note, UnoProperty.CONTENT, property);
      c.getText().insertTextContent(c, note, false);
    }
    catch (java.lang.Exception x)
    {
      LOGGER.error("", x);
    }
  }
}