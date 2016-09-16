package de.muenchen.allg.itd51.wollmux.document.commands;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.ooo.TextDocument;

/**
 * Der SurroundingGarbageCollector erfasst leere Absätze und Einfügemarker um
 * Dokumentkommandos herum.
 */
class SurroundingGarbageCollector extends AbstractExecutor
{
  /**
   * 
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  /**
   * @param documentCommandInterpreter
   */
  SurroundingGarbageCollector(DocumentCommandInterpreter documentCommandInterpreter)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
  }

  /**
   * Speichert Muellmann-Objekte, die zu löschenden Müll entfernen.
   */
  private List<Muellmann> muellmaenner = new Vector<Muellmann>();

  private abstract class Muellmann
  {
    protected XTextRange range;

    public Muellmann(XTextRange range)
    {
      this.range = range;
    }

    public abstract void tueDeinePflicht();
  }

  private class RangeMuellmann extends Muellmann
  {
    public RangeMuellmann(XTextRange range)
    {
      super(range);
    }

    @Override
    public void tueDeinePflicht()
    {
      Bookmark.removeTextFromInside(SurroundingGarbageCollector.this.documentCommandInterpreter.getModel().doc, range);
    }
  }

  private class ParagraphMuellmann extends Muellmann
  {
    public ParagraphMuellmann(XTextRange range)
    {
      super(range);
    }

    @Override
    public void tueDeinePflicht()
    {
      TextDocument.deleteParagraph(range);
    }
  }

  /**
   * Diese Methode erfasst leere Absätze und Einfügemarker, die sich um die im
   * Kommandobaum tree enthaltenen Dokumentkommandos befinden.
   */
  int execute(DocumentCommands commands)
  {
    int errors = 0;
    Iterator<DocumentCommand> iter = commands.iterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = iter.next();
      errors += cmd.execute(this);
    }

    return errors;
  }

  /**
   * Löscht die vorher als Müll identifizierten Inhalte. type filter text
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  void removeGarbage()
  {
    try
    {
      this.documentCommandInterpreter.getDocumentController().setLockControllers(true);
      Iterator<Muellmann> iter = muellmaenner.iterator();
      while (iter.hasNext())
      {
        Muellmann muellmann = iter.next();
        muellmann.tueDeinePflicht();
      }
    }
    finally
    {
      this.documentCommandInterpreter.getDocumentController().setLockControllers(false);
    }
  }

  @Override
  public int executeCommand(InsertFrag cmd)
  {
    if (cmd.hasInsertMarks())
    {
      // ist der ManualMode gesetzt, so darf ein leerer Paragraph am Ende des
      // Dokuments nicht gelöscht werden, da sonst der ViewCursor auf den
      // Start des Textbereiches zurück gesetzt wird. Im Falle der
      // automatischen Einfügung soll aber ein leerer Paragraph am Ende
      // gelöscht werden.
      collectSurroundingGarbageForCommand(cmd, cmd.isManualMode());
    }
    cmd.unsetHasInsertMarks();

    // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
    cmd.markDone(!this.documentCommandInterpreter.debugMode);

    return 0;
  }

  @Override
  public int executeCommand(InsertContent cmd)
  {
    if (cmd.hasInsertMarks())
    {
      collectSurroundingGarbageForCommand(cmd, false);
    }
    cmd.unsetHasInsertMarks();

    // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
    cmd.markDone(!this.documentCommandInterpreter.debugMode);

    return 0;
  }

  // Helper-Methoden:

  /**
   * Diese Methode erfasst Einfügemarken und leere Absätze zum Beginn und zum Ende
   * des übergebenen Dokumentkommandos cmd, wobei über removeAnLastEmptyParagraph
   * gesteuert werden kann, ob ein Absatz am Ende eines Textes gelöscht werden soll
   * (bei true) oder nicht (bei false).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void collectSurroundingGarbageForCommand(DocumentCommand cmd,
      boolean removeAnLastEmptyParagraph)
  {
    /*
     * Im folgenden steht eine 0 in der ersten Stelle dafür, dass vor dem
     * Einfügemarker kein Text mehr steht (der Marker also am Anfang des Absatzes
     * ist). Eine 0 an der zweiten Stelle steht dafür, dass hinter dem
     * Einfügemarker kein Text mehr folgt (der Einfügemarker also am Ende des
     * Absatzes steht). Ein "T" an dritter Stelle gibt an, dass hinter dem Absatz
     * des Einfügemarkers eine Tabelle folgt. Ein "E" an dritter Stelle gibt an,
     * dass hinter dem Cursor das Dokument aufhört und kein weiterer Absatz kommt.
     * 
     * Startmarke: Grundsätzlich gibt es die folgenden Fälle zu unterscheiden.
     * 
     * 00: Einfügemarker und Zeilenumbruch DAHINTER löschen
     * 
     * 01: nur Einfügemarker löschen
     * 
     * 10: nur Einfügemarker löschen
     * 
     * 11: nur Einfügemarker löschen
     * 
     * 00T: Einfügemarker und Zeilenumbruch DAVOR löschen
     * 
     * Die Fälle 01T, 10T und 11T werden nicht unterstützt.
     * 
     * Endmarke: Es gibt die folgenden Fälle:
     * 
     * 00: Einfügemarker und Zeilenumbruch DAHINTER löschen
     * 
     * 00E: Einfügemarker und Zeilenumbruch DAVOR löschen
     * 
     * 01, 10, 11: Einfügemarker löschen
     * 
     * DO NOT TOUCH THIS CODE ! Dieser Code ist komplex und fehleranfällig.
     * Kleinste Änderungen können dafür sorgen, dass irgendeine der 1000e von
     * Vorlagen plötzlich anders dargestellt wird. Das gewünschte Verhalten dieses
     * Codes ist in diesem Kommentar vollständig dokumentiert und Änderungen
     * sollten nur erfolgen, falls obiger Kommentar nicht korrekt umgesetzt wurde.
     * Um neue Anforderungen umzusetzen sollten unbedingt alle anderen
     * Möglichkeiten in Betracht gezogen werden bevor hier eine Änderung erfolgt.
     * Sollte eine Änderung unumgehbar sein, so ist sie VOR der Implementierung im
     * Wiki und in obigem Kommentar zu dokumentieren. Dabei ist darauf zu achten,
     * dass ein neuer Fall sich mit keinem der anderen Fälle überschneidet.
     */
    XParagraphCursor[] start = cmd.getStartMark();
    XParagraphCursor[] end = cmd.getEndMark();
    if (start == null || end == null) return;

    // Startmarke auswerten:
    if (start[0].isStartOfParagraph() && start[1].isEndOfParagraph())
    {
      muellmaenner.add(new ParagraphMuellmann(start[1]));
    }
    else
    // if start mark is not the only text in the paragraph
    {
      start[1].goLeft((short) 1, true);
      muellmaenner.add(new RangeMuellmann(start[1]));
    }

    // Endemarke auswerten:

    // Prüfen ob der Cursor am Ende des Dokuments steht. Anmerkung: hier kann
    // nicht der bereits vorhandene cursor end[1] zum Testen verwendet werden,
    // weil dieser durch den goRight verändert würde. Man könnte ihn zwar mit
    // goLeft nachträglich wieder zurück schieben, aber das funzt nicht wenn
    // danach eine Tabelle kommt.
    XParagraphCursor docEndTest = cmd.getEndMark()[1];
    boolean isEndOfDocument = !docEndTest.goRight((short) 1, false);

    if (removeAnLastEmptyParagraph == false) isEndOfDocument = false;

    if (end[0].isStartOfParagraph() && end[1].isEndOfParagraph()
      && !isEndOfDocument)
    {
      muellmaenner.add(new ParagraphMuellmann(end[1]));
    }
    else
    {
      end[0].goRight(cmd.getEndMarkLength(), true);
      muellmaenner.add(new RangeMuellmann(end[0]));
    }
  }
}