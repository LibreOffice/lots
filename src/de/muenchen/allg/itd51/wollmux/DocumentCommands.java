/*
 * Dateiname: DocumentCommands.java
 * Projekt  : WollMux
 * Funktion : Verwaltet alle in einem Dokument enthaltenen Dokumentkommandos.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 04.05.2007 | LUT | Erstellung als DocumentCommands als Ablösung/Optimierung
 *                    von DocumentCommandTree.
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
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.text.XBookmarksSupplier;

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
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;

/**
 * Diese Klasse verwaltet die Dokumentkommandos eines Textdokuments und kann
 * sich über update() aktualisieren, so dass es erkennt wenn z.B. neue
 * Dokumentkommandos in das Dokument hinzugekommen sind oder die Bookmarks zu
 * bestehenden Dokumentkommandos entfernt wurden, und somit das Dokumentkommando
 * ungültig wird. Die Klasse enthält die Menge aller Dokumentkommandos eines
 * Dokuments und darüber hinaus spezielle Sets bzw. Listen, die nur
 * Dokumentkommandos eines bestimmten Typs beinhalten - je nach Bedarf können
 * die speziellen Listen auch besonders behandelt (z.B. sortiert) sein.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class DocumentCommands
{
  /**
   * Folgendes Pattern prüft ob es sich bei einem Bookmark um ein gültiges
   * "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
   */
  public static final Pattern wmCmdPattern = Pattern
      .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

  /**
   * Das Dokument, in dem die Bookmarks enthalten sind und das dazu ein
   * XBookmarksSupplier sein muss.
   */
  private XBookmarksSupplier doc;

  /**
   * Enthält die Menge aller Dokumentkommandos und wird über update()
   * aktualisiert.
   */
  private HashSet allCommands;

  /**
   * Enthält die Menge aller setGroups-Kommandos des Dokuments und wird über
   * update() aktualisiert.
   */
  private HashSet setGroupsCommands;

  /**
   * Enthält eine nach Position sortierte Liste aller setJumpMark-Kommandos und
   * wird über update() aktualisiert.
   */
  private LinkedList setJumpMarkCommands;

  /**
   * Enthält ein Set aller notInOrininal-Dokumentkommandos des Dokuments, die
   * für die Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet notInOriginalCommands;

  /**
   * Enthält ein Set aller draftOnly-Dokumentkommandos des Dokuments, die für
   * die Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet draftOnlyCommands;

  /**
   * Enthält ein Set aller all-Dokumentkommandos des Dokuments, die für die
   * Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet allVersionsCommands;

  /**
   * Erzeugt einen neuen Container für DocumentCommands im TextDocument doc.
   * 
   * @param doc
   */
  public DocumentCommands(XBookmarksSupplier doc)
  {
    this.doc = doc;
    this.allCommands = new HashSet();

    this.setGroupsCommands = new HashSet();
    this.setJumpMarkCommands = new LinkedList();
    this.notInOriginalCommands = new HashSet();
    this.draftOnlyCommands = new HashSet();
    this.allVersionsCommands = new HashSet();
  }

  /**
   * Diese Methode aktualisiert die Dokumentkommandos, so dass neue und
   * entfernte Dokumentkommandos im Dokument erkannt und mit den Datenstrukturen
   * abgeglichen werden. Die Methode liefert true zurück, wenn seit dem letzten
   * update() Änderungen am Bestand der Dokumentkommandos erkannt wurden und
   * false, wenn es keine Änderungen gab.
   * 
   * @return true, wenn sich die Menge der Dokumentkommandos seit dem letzten
   *         update() verändert hat und false, wenn es keine Änderung gab.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean update()
  {
    if (doc == null) return false;
    long startTime = System.currentTimeMillis();

    // HashSets mit den Namen der bekannten, gültigen Dokumentkommandos
    // und den ungültigen Dokumentkommandos erstellen:
    HashSet knownBookmarks = new HashSet();
    HashSet retiredDocumentCommands = new HashSet();
    for (Iterator iter = allCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd.isRetired())
        retiredDocumentCommands.add(cmd);
      else
        knownBookmarks.add(cmd.getBookmarkName());
    }

    // Bookmarks scannen und HashSet mit allen neuen Dokumentkommandos aufbauen:
    HashSet newDocumentCommands = new HashSet();
    String[] bookmarkNames = doc.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarkNames.length; i++)
    {
      String name = bookmarkNames[i];
      Matcher m = wmCmdPattern.matcher(name);

      if (m.find() && !knownBookmarks.contains(name))
      {
        DocumentCommand cmd = createCommand(name, m.group(1), doc);
        if (cmd != null) newDocumentCommands.add(cmd);
      }
    }

    // lokale Kommandosets aktualisieren:
    removeRetiredDocumentCommands(retiredDocumentCommands);
    addNewDocumentCommands(newDocumentCommands);

    Logger.debug2("Update fertig nach "
                  + (System.currentTimeMillis() - startTime)
                  + " ms. Entfernte/Neue Dokumentkommandos: "
                  + retiredDocumentCommands.size()
                  + " / "
                  + newDocumentCommands.size());
    return retiredDocumentCommands.size() > 0 || newDocumentCommands.size() > 0;
  }

  /**
   * Abgleich der internen Datenstrukturen: fügt alle in newDocumentCommands
   * enthaltenen Dokumentkommandos in die entsprechenden Sets/Listen hinzu.
   * 
   * @param newDocumentCommands
   *          Set mit allen Dokumentkommandos, die seit dem letzten update
   *          hinzugekommen sind.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void addNewDocumentCommands(HashSet newDocumentCommands)
  {
    for (Iterator iter = newDocumentCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();

      allCommands.add(cmd);

      if (cmd instanceof SetGroups) addNewSetGroups((SetGroups) cmd);
      if (cmd instanceof SetJumpMark) addNewSetJumpMark((SetJumpMark) cmd);
      if (cmd instanceof NotInOriginal) notInOriginalCommands.add(cmd);
      if (cmd instanceof DraftOnly) draftOnlyCommands.add(cmd);
      if (cmd instanceof AllVersions) allVersionsCommands.add(cmd);
    }
  }

  /**
   * Fügt das neue SetGroups-Kommando cmdB zum internen Set der
   * SetGroups-Kommandos hinzu, wobei die Gruppenzuordnung von verschachtelten
   * setGroups-Kommandos gemäß der Vererbungsstruktur der Sichtbarkeitsgruppen
   * auf untergeordnete SetGroups-Kommandos übertragen werden.
   * 
   * @param cmdB
   *          das hinzuzufügende SetGroups-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void addNewSetGroups(SetGroups cmdB)
  {
    for (Iterator iter = setGroupsCommands.iterator(); iter.hasNext();)
    {
      SetGroups cmdA = (SetGroups) iter.next();
      int rel = cmdB.getRelation(cmdA);
      if (rel == DocumentCommand.REL_B_IS_CHILD_OF_A)
        cmdB.addGroups(cmdA.getGroups());
      else if (rel == DocumentCommand.REL_B_IS_PARENT_OF_A)
        cmdA.addGroups(cmdB.getGroups());
      else if (rel == DocumentCommand.REL_B_OVERLAPS_A)
      {
        cmdA.addGroups(cmdB.getGroups());
        cmdB.addGroups(cmdA.getGroups());
      }
    }
    setGroupsCommands.add(cmdB);
  }

  /**
   * Fügt ein neues SetJumpMark-Kommando cmdB sortiert in die (bereits)
   * vorsortierten Liste aller SetJumpMark-Kommandos ein.
   * 
   * @param cmdB
   *          das hinzuzufügende SetJumpMark-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void addNewSetJumpMark(SetJumpMark cmdB)
  {
    ListIterator iter = setJumpMarkCommands.listIterator();
    while (iter.hasNext())
    {
      SetJumpMark cmdA = (SetJumpMark) iter.next();
      if (cmdA.getRelation(cmdB) == DocumentCommand.REL_B_IS_SIBLING_BEFORE_A)
      {
        iter.previous();
        break;
      }
    }
    iter.add(cmdB);
  }

  /**
   * Abgleich der internen Datenstrukturen: löscht alle in newDocumentCommands
   * enthaltenen Dokumentkommandos aus den entsprechenden Sets/Listen.
   * 
   * @param retiredDocumentCommands
   *          Set mit allen Dokumentkommandos, die seit dem letzten update
   *          ungültig (gelöscht) wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void removeRetiredDocumentCommands(HashSet retiredDocumentCommands)
  {
    allCommands.removeAll(retiredDocumentCommands);
    setGroupsCommands.removeAll(retiredDocumentCommands);
    setJumpMarkCommands.removeAll(retiredDocumentCommands);
    notInOriginalCommands.removeAll(retiredDocumentCommands);
    draftOnlyCommands.removeAll(retiredDocumentCommands);
    allVersionsCommands.removeAll(retiredDocumentCommands);
  }

  /**
   * Liefert einen Iterator über alle Dokumentkommandos des Dokuments in
   * undefinierter Reihenfolge - sollten bestimmte Dokumentkommandos in einer
   * definierten Reihenfolge oder mit definierten Eigenschaften benötigt werden,
   * so können die speziellen Methoden wie z.B. getSetJumpMarksIterator()
   * verwendet werden.
   * 
   * @return ein Iterator über alle Dokumentkommandos des Dokuments in
   *         undefinierter Reihenfolge.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator iterator()
  {
    return allCommands.iterator();
  }

  /**
   * Liefert einen Iterator über die Menge aller setGroups-Dokumentkommandos in
   * undefinierter Reihenfolge, wobei sichergestellt ist, dass die
   * Gruppenzugehörigkeit verschachtelter SetGroups-Kommandos gemäß der
   * Vererbungsstruktur von Sichbarkeitsgruppen auf untergeordnete
   * SetGroups-Kommandos vererbt wurde.
   * 
   * @return ein Iterator über alle setGroups-Dokumentkommandos.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator setGroupsIterator()
  {
    return setGroupsCommands.iterator();
  }

  /**
   * Liefert die aktuell erste JumpMark dieses Dokuments oder null, wenn keine
   * Jumpmark verfügbar ist.
   * 
   * @return Die aktuell erste JumpMark des Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public SetJumpMark getFirstJumpMark()
  {
    try
    {
      return (SetJumpMark) setJumpMarkCommands.getFirst();
    }
    catch (NoSuchElementException e)
    {
      return null;
    }
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * NotInOrininal-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller
   *         NotInOrininal-Dokumentkommandos dieses Dokuments ermöglicht. Der
   *         Iterator kann auch keine Elemente enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator notInOriginalIterator()
  {
    return notInOriginalCommands.iterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * DraftOnly-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller DraftOnly-Dokumentkommandos
   *         dieses Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator draftOnlyIterator()
  {
    return draftOnlyCommands.iterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * All-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller All-Dokumentkommandos dieses
   *         Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator allVersionsIterator()
  {
    return allVersionsCommands.iterator();
  }

  /**
   * Erzeugt ein neues Dokumentkommando zum Bookmark bookmarkName, mit dem um
   * die Kommandostring cmdStr (das ist der Bookmarkname ohne die abschließenden
   * Ziffern zur unterscheidung verschiedener Bookmarks mit dem selben
   * Dokumentkommando) und dem BookmarksSupplier doc und liefert das neue
   * Dokumentkommando zurück oder null, wenn das Dokumentkommando nicht erzeugt
   * werden konnte.
   * 
   * @param bookmarkName
   * @param cmdStr
   * @param doc
   * @return
   */
  private static DocumentCommand createCommand(String bookmarkName,
      String cmdStr, XBookmarksSupplier doc)
  {
    try
    {
      Bookmark b = new Bookmark(bookmarkName, doc);
      try
      {
        ConfigThingy wmCmd = new ConfigThingy("", null,
            new StringReader(cmdStr));
        return createCommand(wmCmd, b);
      }
      catch (SyntaxErrorException e)
      {
        return new InvalidCommand(b, e);
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    return null;
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
  private static DocumentCommand createCommand(ConfigThingy wmCmd,
      Bookmark bookmark)
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
        throw new DocumentCommand.InvalidCommandException(
            "Fehlendes CMD-Attribut");
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

      throw new DocumentCommand.InvalidCommandException(
          "Unbekanntes Kommando \"" + cmd + "\"");
    }
    catch (DocumentCommand.InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }

  /**
   * Implementiert einen leer-Executor, von dem abgeleitet werden kann, um
   * konkrete Executoren zu schreiben, mit denen die Dokumentkommandos, die
   * DocumentCommands.iterator() liefert bearbeitet werden können.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class Executor implements DocumentCommand.Executor
  {
    /**
     * Durchläuft den Dokumentenbaum tree in der in reverse angegebener Richtung
     * und führt alle enthaltenen Dokumentkommandos aus, die nicht den Status
     * DONE=true oder ERROR=true besitzen. Der Wert reverse=false entspricht
     * dabei der normalen Reihenfolge einer Tiefensuche, bei reverse=true wird
     * der Baum zwar ebenfalls in einer Tiefensuche durchlaufen, jedoch wird
     * stets mit dem letzten Kind angefangen.
     * 
     * @param commands
     * @param reverse
     * @return Anzahl der bei der Ausführung aufgetretenen Fehler.
     */
    protected int executeAll(DocumentCommands commands)
    {
      int errors = 0;

      // Alle DocumentCommands durchlaufen und mit execute aufrufen.
      for (Iterator iter = commands.iterator(); iter.hasNext();)
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();

        if (cmd.isDone() == false && cmd.hasError() == false)
        {
          // Kommando ausführen und Fehler zählen
          errors += cmd.execute(this);
        }
      }
      return errors;
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
