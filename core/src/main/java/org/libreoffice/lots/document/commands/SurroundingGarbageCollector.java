/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.document.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.common.TextDocument;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.ext.unohelper.util.UnoService;
import org.libreoffice.lots.document.commands.DocumentCommand.InsertContent;
import org.libreoffice.lots.document.commands.DocumentCommand.InsertFrag;
import org.libreoffice.lots.util.Utils;

/**
 * Der SurroundingGarbageCollector erfasst leere Absätze und Einfügemarker um
 * Dokumentkommandos herum.
 */
class SurroundingGarbageCollector extends AbstractExecutor
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SurroundingGarbageCollector.class);

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
  private List<Cleaner> muellmaenner = new ArrayList<>();

  private abstract class Cleaner
  {
    protected XTextRange range;

    public Cleaner(XTextRange range)
    {
      this.range = range;
    }

    /**
     * Cleans the garbage.
     */
    public abstract void clean();
  }

  private class RangeCleaner extends Cleaner
  {
    public RangeCleaner(XTextRange range)
    {
      super(range);
    }

    /**
     * Remove all text but no book marks from the range.
     */
    @Override
    public void clean()
    {
      try
      {
        XTextDocument doc = SurroundingGarbageCollector.this.documentCommandInterpreter.getModel().doc;
        // create a book mark, so that the range contains a new text portion
        Object bookmark = UnoService.createService(UnoService.CSS_TEXT_BOOKMARK, doc);
        UNO.XNamed(bookmark).setName("killer");
        range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
        String name = UNO.XNamed(bookmark).getName();

        // collect text portions for deletion and book marks which may be accidently removed to
        List<String> collateral = new ArrayList<>();
        UnoCollection<Object> ranges = UnoCollection.getCollection(range, Object.class);

        for (Object r : ranges)
        {
          UnoCollection<Object> textPortions = UnoCollection.getCollection(r, Object.class);
          if (textPortions != null)
          {
            collectTextPotions(name, collateral, textPortions);
          }
        }

        range.setString("");
        UNO.XTextContent(bookmark).getAnchor().getText().removeTextContent(UNO.XTextContent(bookmark));

        // recreate lost book marks.
        UnoDictionary<XTextContent> bookmarks = UnoDictionary
            .create(UNO.XBookmarksSupplier(doc).getBookmarks(), XTextContent.class);
        for (String portionName : collateral)
        {
          if (!bookmarks.containsKey(portionName))
          {
            LOGGER.debug("Regeneriere Bookmark '{}'", portionName);
            bookmark = UnoService.createService(UnoService.CSS_TEXT_BOOKMARK, doc);
            UNO.XNamed(bookmark).setName(portionName);
            range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
          }
        }
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    private void collectTextPotions(String name, List<String> collateral, UnoCollection<Object> textPortions)
    {
      for (Object textPortion : textPortions)
      {
        if ("Bookmark".equals(Utils.getProperty(textPortion, UnoProperty.TEXT_PROTION_TYPE)))
        {
          String portionName = UNO.XNamed(Utils.getProperty(textPortion, UnoProperty.BOOKMARK)).getName();
          if (!name.equals(portionName))
          {
            collateral.add(portionName);
          }
        }
      }
    }
  }

  private class ParagraphCleaner extends Cleaner
  {
    public ParagraphCleaner(XTextRange range)
    {
      super(range);
    }

    @Override
    public void clean()
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
    Iterator<Cleaner> iter = muellmaenner.iterator();
    while (iter.hasNext())
    {
      Cleaner muellmann = iter.next();
      muellmann.clean();
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
    if (start.length == 0 || end.length == 0)
    {
      return;
    }

    // Startmarke auswerten:
    if (start[0].isStartOfParagraph() && start[1].isEndOfParagraph())
    {
      muellmaenner.add(new ParagraphCleaner(start[1]));
    }
    else
    // if start mark is not the only text in the paragraph
    {
      start[1].goLeft((short) 1, true);
      muellmaenner.add(new RangeCleaner(start[1]));
    }

    // Endemarke auswerten:

    // Prüfen ob der Cursor am Ende des Dokuments steht. Anmerkung: hier kann
    // nicht der bereits vorhandene cursor end[1] zum Testen verwendet werden,
    // weil dieser durch den goRight verändert würde. Man könnte ihn zwar mit
    // goLeft nachträglich wieder zurück schieben, aber das funzt nicht wenn
    // danach eine Tabelle kommt.
    XParagraphCursor docEndTest = cmd.getEndMark()[1];
    boolean isEndOfDocument = !docEndTest.goRight((short) 1, false);

    if (!removeAnLastEmptyParagraph)
    {
      isEndOfDocument = false;
    }

    if (end[0].isStartOfParagraph() && end[1].isEndOfParagraph()
      && !isEndOfDocument)
    {
      muellmaenner.add(new ParagraphCleaner(end[1]));
    }
    else
    {
      end[0].goRight(cmd.getEndMarkLength(), true);
      muellmaenner.add(new RangeCleaner(end[0]));
    }
  }
}
