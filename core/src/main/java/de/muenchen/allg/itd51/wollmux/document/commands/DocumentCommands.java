/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.document.commands;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.document.TextSection;
import de.muenchen.allg.itd51.wollmux.document.TreeRelation;
import de.muenchen.allg.itd51.wollmux.document.VisibilityElement;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockCommand;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Diese Klasse verwaltet die Dokumentkommandos eines Textdokuments und kann sich
 * über update() aktualisieren, so dass es erkennt wenn z.B. neue Dokumentkommandos
 * in das Dokument hinzugekommen sind oder die Bookmarks zu bestehenden
 * Dokumentkommandos entfernt wurden, und somit das Dokumentkommando ungültig wird.
 * Die Klasse enthält die Menge aller Dokumentkommandos eines Dokuments und darüber
 * hinaus spezielle Sets bzw. Listen, die nur Dokumentkommandos eines bestimmten Typs
 * beinhalten - je nach Bedarf können die speziellen Listen auch besonders behandelt
 * (z.B. sortiert) sein.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class DocumentCommands implements Iterable<DocumentCommand>
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DocumentCommands.class);

  /**
   * Folgendes Pattern prüft ob es sich bei einem Bookmark um ein gültiges
   * "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
   */
  public static final Pattern wmCmdPattern =
    Pattern.compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

  /**
   * Das Pattern zur Erkennung von TextSections mit einem GROUPS-Attribut als
   * Namenszusatz zur Definition der Sichtbarkeitsgruppen dieses Bereichs
   */
  private static final Pattern sectionWithGROUPSPattern =
    Pattern.compile("\\A.*(GROUPS.*[^\\d])\\d*\\z");

  /**
   * Das Dokument, in dem die Bookmarks enthalten sind und das dazu ein
   * XBookmarksSupplier sein muss.
   */
  private XBookmarksSupplier doc;

  /**
   * Enthält die Menge aller Dokumentkommandos und wird über update() aktualisiert.
   */
  private HashSet<DocumentCommand> allCommands;

  /**
   * Enthält die aktuelle Menge aller TextSections mit GROUPS-Attribut und wird über
   * update() aktualisiert.
   */
  private HashSet<TextSection> allTextSectionsWithGROUPS;

  /**
   * Enthält die nach Position sortierte Liste aller Sichtbarkeitselemente
   * (setGroups-Kommando und TextSection mit GROUPS-Attribut) des Dokuments und wird
   * über update() aktualisiert.
   */
  private LinkedList<VisibilityElement> visibilityElements;

  /**
   * Enthält eine nach Position sortierte Liste aller setJumpMark-Kommandos und wird
   * über update() aktualisiert.
   */
  private LinkedList<SetJumpMark> setJumpMarkCommands;

  /**
   * Enthält ein Set aller notInOrininal-Dokumentkommandos des Dokuments, die für die
   * Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet<PrintBlockCommand> printBlocks;

  /**
   * Pattern zum Erkennen von insertValue und insertFormValue-Bookmarks.
   */
  public static final Pattern INSERTION_BOOKMARK =
    getPatternForCommand("((insertValue)|(insertFormValue))");

  /**
   * Erzeugt einen neuen Container für DocumentCommands im TextDocument doc.
   *
   * @param doc
   */
  public DocumentCommands(XBookmarksSupplier doc)
  {
    this.doc = doc;
    this.allCommands = new HashSet<>();

    this.visibilityElements = new LinkedList<>();
    this.setJumpMarkCommands = new LinkedList<>();
    this.printBlocks = new HashSet<>();
    this.allTextSectionsWithGROUPS = new HashSet<>();
  }

  /**
   * Liefert ein {@link Pattern}, das auf Bookmarknamen für das WollMux-Kommando
   * wollmuxCommand matcht. Das Pattern berücksichtigt optionale Whitespace sowie die
   * optionale Zahl am Ende des Bookmarknamens. wollmuxCommand wird direkt ohne
   * Escaping in den regulären Ausdruck eingebaut. Es ist also möglich, \w* zu
   * übergeben, um alle Kommandos zu matchen. Gruppe 1 des Ausdrucks umschließt das
   * ganze Kommando ab "WM" bis inklusive der letzten Klammer vor dem optionalen
   * Zahlenteil am Ende.
   */
  public static Pattern getPatternForCommand(String wollmuxCommand)
  {
    return Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'" + wollmuxCommand
      + "'.*\\))\\s*\\d*\\z");
  }

  /**
   * Diese Methode aktualisiert die Dokumentkommandos und TextSections des Dokuments,
   * so dass neue und manuell gelöschte Dokumentkommandos oder TextSections erkannt
   * und mit den Datenstrukturen abgeglichen werden. Die Methode liefert true zurück,
   * wenn seit dem letzten update() Änderungen am Bestand der Dokumentkommandos bzw.
   * TextSections erkannt wurden und false, wenn es keine Änderungen gab.
   *
   * @return true, wenn sich die Menge der Dokumentkommandos bzw. TextSections seit
   *         dem letzten update() verändert hat und false, wenn es keine Änderung
   *         gab.
   */
  public boolean update()
  {
    boolean bookmarksChanged = updateBookmarks();
    boolean textSectionsChanged = updateTextSections();
    return bookmarksChanged || textSectionsChanged;
  }

  /**
   * Fügt ein neues Dokumentkommando mit dem Kommandostring cmdStr, der in der Form
   * "WM(...)" erwartet wird, in das Dokument an der TextRange r ein. Dabei wird ein
   * neues Bookmark erstellt und dieses als Dokumenkommando registriert. Dieses
   * Bookmark wird genau über r gelegt, so dass abhängig vom Dokumentkommando der
   * Inhalt der TextRange r durch eine eventuelle spätere Ausführung des
   * Dokumentkommandos überschrieben wird (wenn r keine Ausdehnung hat, wird ein
   * kollabiertes Bookmark erzeugt und es kann logischerweise auch nichts
   * überschrieben werden). cmdStr muss nur das gewünschte Kommando enthalten ohne
   * eine abschließende Zahl, die zur Herstellung eindeutiger Bookmarks benötigt wird
   * - diese Zahl wird bei Bedarf automatisch an den Bookmarknamen angehängt.
   *
   * @param r
   *          Die TextRange, an der das neue Bookmark mit diesem Dokumentkommando
   *          eingefügt werden soll. r darf auch null sein und wird in diesem Fall
   *          ignoriert.
   * @param cmdStr
   *          Das Kommando als String der Form "WM(...)".
   */
  public void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    if (r == null) {
      return;
    }
    try
    {
      new Bookmark(cmdStr, UNO.XTextDocument(doc), r);
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
    update();
  }

  /**
   * Diese Methode aktualisiert die Dokumentkommandos, so dass neue und entfernte
   * Dokumentkommandos im Dokument erkannt und mit den Datenstrukturen abgeglichen
   * werden. Die Methode liefert true zurück, wenn seit dem letzten update()
   * Änderungen am Bestand der Dokumentkommandos erkannt wurden und false, wenn es
   * keine Änderungen gab.
   *
   * @return true, wenn sich die Menge der Dokumentkommandos seit dem letzten
   *         update() verändert hat und false, wenn es keine Änderung gab.
   */
  private boolean updateBookmarks()
  {
    if (doc == null) {
      return false;
    }
    long startTime = System.currentTimeMillis();

    // HashSets mit den Namen der bekannten, gültigen Dokumentkommandos
    // und den ungültigen Dokumentkommandos erstellen:
    HashSet<String> knownBookmarks = new HashSet<>();
    HashSet<DocumentCommand> retiredDocumentCommands =
      new HashSet<>();
    for (Iterator<DocumentCommand> iter = allCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();
      if (cmd.isRetired())
        retiredDocumentCommands.add(cmd);
      else
        knownBookmarks.add(cmd.getBookmarkName());
    }

    // Bookmarks scannen und HashSet mit allen neuen Dokumentkommandos aufbauen:
    HashSet<DocumentCommand> newDocumentCommands = new HashSet<>();
    try
    {
      String[] bookmarkNames = doc.getBookmarks().getElementNames();
      for (int i = 0; i < bookmarkNames.length; i++)
      {
        String name = bookmarkNames[i];
        Matcher m = wmCmdPattern.matcher(name);

        if (m.find() && !knownBookmarks.contains(name))
        {
          DocumentCommand cmd = createCommand(name, m.group(1), doc);
          if (cmd != null) {
            newDocumentCommands.add(cmd);
          }
        }
      }

      // lokale Kommandosets aktualisieren:
      removeRetiredDocumentCommands(retiredDocumentCommands);
      addNewDocumentCommands(newDocumentCommands);
    }
    catch (Exception e)
    {
      LOGGER.trace("", e);
    }

    LOGGER.trace("updateBookmarks fertig nach {} ms. Entfernte/Neue Dokumentkommandos: {} / {}",
        Integer.valueOf((int) (System.currentTimeMillis() - startTime)), retiredDocumentCommands.size(),
        newDocumentCommands.size());
    return !retiredDocumentCommands.isEmpty() || !newDocumentCommands.isEmpty();
  }

  /**
   * Diese Methode aktualisiert die TextSections, so dass neue und gelöschte
   * TextSections im Dokument erkannt und mit den Datenstrukturen abgeglichen werden.
   * Die Methode liefert true zurück, wenn seit dem letzten update() Änderungen am
   * Bestand der TextSections erkannt wurden und false, wenn es keine Änderungen gab.
   *
   * @return true, wenn sich die Menge der TextSections seit dem letzten update()
   *         verändert hat und false, wenn es keine Änderung gab.
   */
  private boolean updateTextSections()
  {
    XTextSectionsSupplier supp = UNO.XTextSectionsSupplier(doc);
    if (supp == null) {
      return false;
    }
    long startTime = System.currentTimeMillis();

    // HashSets mit den Namen der bekannten, gültigen TextSections
    // und den ungültigen TextSections erstellen:
    HashSet<String> knownTextSections = new HashSet<>();
    HashSet<TextSection> invalidTextSections = new HashSet<>();
    for (Iterator<TextSection> iter = allTextSectionsWithGROUPS.iterator(); iter.hasNext();)
    {
      TextSection s = iter.next();
      if (s.isInvalid())
        invalidTextSections.add(s);
      else
        knownTextSections.add(s.getName());
    }

    // TextSections scannen und HashSet mit allen TextSections aufbauen:
    HashSet<TextSection> newTextSections = new HashSet<>();
    String[] textSectionNames = supp.getTextSections().getElementNames();
    for (int i = 0; i < textSectionNames.length; i++)
    {
      String name = textSectionNames[i];
      Matcher m = sectionWithGROUPSPattern.matcher(name);

      if (m.find() && !knownTextSections.contains(name))
      {
        TextSection s =
          createTextSection(name, m.group(1), UNO.XTextSectionsSupplier(doc));
        if (s != null) {
          newTextSections.add(s);
        }
      }
    }

    // lokale Kommandosets aktualisieren:
    removeInvalidTextSections(invalidTextSections);
    addNewTextSections(newTextSections);

    LOGGER.trace("updateTextSections fertig nach {} ms. Entfernte/Neue TextSections: {} / {}",
        Integer.valueOf((int) (System.currentTimeMillis() - startTime)), invalidTextSections.size(),
        newTextSections.size());
    return !invalidTextSections.isEmpty() || !newTextSections.isEmpty();
  }

  /**
   * Abgleich der internen Datenstrukturen: fügt alle in newDocumentCommands
   * enthaltenen Dokumentkommandos in die entsprechenden Sets/Listen hinzu.
   *
   * @param newDocumentCommands
   *          Set mit allen Dokumentkommandos, die seit dem letzten update
   *          hinzugekommen sind.
   */
  private void addNewDocumentCommands(HashSet<DocumentCommand> newDocumentCommands)
  {
    long[] times = new long[] {
        0, 0, 0, 0, };
    long[] counters = new long[] {
        0, 0, 0, 0, };
    LOGGER.trace("addNewDocumentCommands");

    long lastTime = System.currentTimeMillis();
    for (Iterator<DocumentCommand> iter = newDocumentCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();

      int id = 0;
      allCommands.add(cmd);

      if (cmd instanceof SetGroups)
      {
        addNewVisibilityElement((SetGroups) cmd);
        id = 1;
      }
      else if (cmd instanceof SetJumpMark)
      {
        addNewSetJumpMark((SetJumpMark) cmd);
        id = 2;
      }
      else if (cmd instanceof PrintBlockCommand)
      {
        printBlocks.add((PrintBlockCommand) cmd);
        id = 3;
      }

      long currentTime = System.currentTimeMillis();
      if (id >= 0)
      {
        times[id] += currentTime - lastTime;
        counters[id]++;
      }
      lastTime = currentTime;
    }

    LOGGER.trace("addNewDocumentCommands statistics (number of elements, overalltime to add):");
    LOGGER.trace("- SetGroups:     {}, {} ms", counters[1], times[1]);
    LOGGER.trace("- SetJumpMark:   {}, {} ms", counters[2], times[2]);
    LOGGER.trace("- PrintBlocks:   {}, {} ms", counters[3], times[3]);
    LOGGER.trace("- Others:        {}, {} ms", counters[0], times[0]);
  }

  /**
   * Abgleich der internen Datenstrukturen: fügt alle in newElements enthaltenen
   * TextSections in die entsprechenden Sets/Listen hinzu.
   *
   * @param newElements
   *          Set mit allen TextSections, die seit dem letzten update hinzugekommen
   *          sind.
   */
  private void addNewTextSections(HashSet<TextSection> newElements)
  {
    for (Iterator<TextSection> iter = newElements.iterator(); iter.hasNext();)
    {
      TextSection s = iter.next();

      allTextSectionsWithGROUPS.add(s);

      addNewVisibilityElement(s);
    }
  }

  /**
   * Fügt das neue Sichtbarkeitselement (SetGroups-Kommando bzw. Textbereich mit
   * GROUPS im Namen) sortiert in die bereits vorsortierte LinkedList aller
   * Sichtbarkeitselemente hinzu, wobei die Gruppenzuordnung von verschachtelten
   * Sichtbarkeitselementen gemäß der Vererbungsstruktur der Sichtbarkeitsgruppen auf
   * untergeordnete Sichtbarkeitselemente übertragen werden.
   *
   * @param element
   *          das hinzuzufügende Sichtbarkeitselement.
   */
  private void addNewVisibilityElement(VisibilityElement element)
  {
    boolean inserted = false;
    XTextRange anchor = element.getAnchor();
    if (anchor == null) {
      return;
    }

    ListIterator<VisibilityElement> iter = visibilityElements.listIterator();
    while (iter.hasNext())
    {
      VisibilityElement current = iter.next();

      TreeRelation rel = new TreeRelation(current.getAnchor(), anchor);

      // setzen der Gruppen:
      if (rel.isAEqualB())
      {
        element.addGroups(current.getGroups());
        current.addGroups(element.getGroups());
      }
      else if (rel.isAChildOfB())
      {
        current.addGroups(element.getGroups());
      }
      else if (rel.isBChildOfA())
      {
        element.addGroups(current.getGroups());
      }

      // sortiertes Einfügen:
      if (!inserted && (rel.isAGreaterThanB() || rel.isAEqualB()))
      {
        iter.previous();
        iter.add(element);
        inserted = true;
        iter.next();
      }
    }
    if (!inserted) {
      visibilityElements.add(element);
    }
  }

  /**
   * Fügt ein neues SetJumpMark-Kommando cmdB sortiert in die (bereits) vorsortierten
   * Liste aller SetJumpMark-Kommandos ein.
   *
   * @param cmdB
   *          das hinzuzufügende SetJumpMark-Kommando.
   */
  private void addNewSetJumpMark(SetJumpMark cmdB)
  {
    ListIterator<SetJumpMark> iter = setJumpMarkCommands.listIterator();
    while (iter.hasNext())
    {
      SetJumpMark cmdA = iter.next();
      TreeRelation rel = new TreeRelation(cmdA.getAnchor(), cmdB.getAnchor());
      if (rel.isAGreaterThanB() || rel.isAEqualB())
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
   * @param retired
   *          Set mit allen Dokumentkommandos, die seit dem letzten update ungültig
   *          (gelöscht) wurden.
   */
  private void removeRetiredDocumentCommands(HashSet<DocumentCommand> retired)
  {
    allCommands.removeAll(retired);
    setJumpMarkCommands.removeAll(retired);
    printBlocks.removeAll(retired);
  }

  /**
   * Abgleich der internen Datenstrukturen: löscht alle in newDocumentCommands
   * enthaltenen Dokumentkommandos aus den entsprechenden Sets/Listen.
   *
   * @param invalid
   *          Set mit allen Dokumentkommandos, die seit dem letzten update ungültig
   *          (gelöscht) wurden.
   */
  private void removeInvalidTextSections(HashSet<TextSection> invalid)
  {
    allTextSectionsWithGROUPS.removeAll(invalid);
    visibilityElements.removeAll(invalid);
  }

  /**
   * Liefert einen Iterator über alle Dokumentkommandos des Dokuments in
   * undefinierter Reihenfolge - sollten bestimmte Dokumentkommandos in einer
   * definierten Reihenfolge oder mit definierten Eigenschaften benötigt werden, so
   * können die speziellen Methoden wie z.B. getSetJumpMarksIterator() verwendet
   * werden.
   *
   * @return ein Iterator über alle Dokumentkommandos des Dokuments in undefinierter
   *         Reihenfolge.
   */
  @Override
  public Iterator<DocumentCommand> iterator()
  {
    return allCommands.iterator();
  }

  /**
   * Liefert List aller setGroups-Dokumentkommandos in
   * undefinierter Reihenfolge, wobei sichergestellt ist, dass die
   * Gruppenzugehörigkeit verschachtelter SetGroups-Kommandos gemäß der
   * Vererbungsstruktur von Sichbarkeitsgruppen auf untergeordnete
   * SetGroups-Kommandos vererbt wurde.
   *
   * @return eine Liste aller setGroups-Dokumentkommandos.
   */
  public List<VisibilityElement> getSetGroups()
  {
    return visibilityElements;
  }

  /**
   * Liefert die aktuell erste JumpMark dieses Dokuments oder null, wenn keine
   * Jumpmark verfügbar ist.
   *
   * @return Die aktuell erste JumpMark des Dokuments.
   */
  public SetJumpMark getFirstJumpMark()
  {
    if (setJumpMarkCommands.isEmpty())
    {
      LOGGER.trace("Keine Jump Mark gefunden");
      return null;
    }
    return setJumpMarkCommands.getFirst();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller PrintBlock-Dokumentkommandos dieses
   * Dokuments ermöglicht.
   *
   * @return ein Iterator, der die Iteration aller PrintBlock-Dokumentkommandos dieses Dokuments
   *         ermöglicht. Der Iterator kann auch keine Elemente enthalten.
   */
  public Set<PrintBlockCommand> printBlockCommands()
  {
    return printBlocks;
  }

  /**
   * Erzeugt ein neues Dokumentkommando zum Bookmark bookmarkName, mit dem um die
   * Kommandostring cmdStr (das ist der Bookmarkname ohne die abschließenden Ziffern
   * zur unterscheidung verschiedener Bookmarks mit dem selben Dokumentkommando) und
   * dem BookmarksSupplier doc und liefert das neue Dokumentkommando zurück oder
   * null, wenn das Dokumentkommando nicht erzeugt werden konnte.
   *
   * @param bookmarkName
   * @param cmdStr
   * @param doc
   * @return Das Dokumentkommando oder null.
   */
  private static DocumentCommand createCommand(String bookmarkName, String cmdStr,
      XBookmarksSupplier doc)
  {
    try
    {
      Bookmark b = new Bookmark(bookmarkName, doc);
      try
      {
        ConfigThingy wmCmd = new ConfigThingy("", null, new StringReader(cmdStr));
        return createCommand(wmCmd, b);
      }
      catch (SyntaxErrorException e)
      {
        return new InvalidCommand(b, e);
      }
    }
    catch (NoSuchElementException e)
    {
      // Eigentlich sollte diese Exception niemals fliegen, da nur ültige Namen
      // beim Scan verwendet werden. Es kommt leider gelegentlich vor, dass
      // XBookmarksSupplier.getElementNames() auch Namen von korrupten Bookmarks
      // liefert, die nicht mehr über XBookmarksSupplier.getByName(...) gefunden
      // werden können. Die korrupten Bookmarks zeichnen sich dadurch aus, dass
      // sie zwar eine Startmarke, jedoch keine Endemarke besitzen. Häufig
      // entstehen solche korrupten Bookmarks beim Löschen der leeren
      // Paragraphen im GarbageCollector. Ich habe bereits probiert, einen
      // minimalen Testfall ohne WollMux für dieses Problem zu extrahieren, war
      // aber damit nicht erfolgreich.
      LOGGER.debug(L.m("Warning: inconsistent bookmark detected:"), e);
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
    }
    return null;
  }

  /**
   * Erzeugt ein neues TextSection-Objekt zur TextSection name aus dem
   * TextSectionsSupplier doc, dessen Gruppen mit dem in groupsStr beschriebenen
   * ConfigThingy definiert sind oder null, wenn die TextSection nicht erzeugt werden
   * konnte.
   *
   * @param name
   *          Der Name der zu erzeugenden TextSection
   * @param groupsStr
   *          der GROUPS-Knoten im ConfigThingy-Format als String
   * @param doc
   *          der XTextSectionsSupplier (das Dokument)
   * @return Die TextSection oder null.
   */
  private static TextSection createTextSection(String name, String groupsStr,
      XTextSectionsSupplier doc)
  {
    if (doc == null) {
      return null;
    }
    UnoDictionary<XTextSection> sections = UnoDictionary.create(doc.getTextSections(), XTextSection.class);

    // HashSet mit allen Gruppen GROUPS aufbauen:
    Set<String> groups = new HashSet<>();
    try
    {
      ConfigThingy groupsCfg =
        new ConfigThingy("", null, new StringReader(groupsStr));

      for (ConfigThingy c : groupsCfg.get("GROUPS"))
      {
        groups.add(c.toString());
      }
    }
    catch (IOException | SyntaxErrorException | NodeNotFoundException e)
    {
      LOGGER.error(
        L.m(
          "The textarea with the name '%1' contains an incorrect GROUPS attribute.",
          name), e);
    }

    try
    {
      XTextSection section = sections.get(name);
      if (section != null)
      {
        return new TextSection(section, groups);
      }
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    return null;
  }

  /**
   * Fabrikmethode für die Erzeugung neuer Dokumentkommandos an dem Bookmark bookmark
   * mit dem als ConfigThingy vorgeparsten Dokumentkommando wmCmd.
   *
   * @param wmCmd
   *          Das vorgeparste Dokumentkommando
   * @param bookmark
   *          Das Bookmark zu dem Dokumentkommando
   * @return Ein passende konkretes DocumentCommand-Instanz.
   */
  private static DocumentCommand createCommand(ConfigThingy wmCmd, Bookmark bookmark)
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
          L.m("Missing CMD-attribute"));
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

      else if (cmd.compareToIgnoreCase("setGroups") == 0)
      {
        return new DocumentCommand.SetGroups(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setPrintFunction") == 0)
      {
        return new DocumentCommand.SetPrintFunction(wmCmd, bookmark);
      }

      else if (cmd.equalsIgnoreCase("draftOnly") || cmd.equalsIgnoreCase("copyOnly")
          || cmd.equalsIgnoreCase("notInOriginal") || cmd.equalsIgnoreCase("originalOnly")
          || cmd.equalsIgnoreCase("allVersions"))
      {
        return new PrintBlockCommand(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setJumpMark") == 0)
      {
        return new DocumentCommand.SetJumpMark(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("overrideFrag") == 0)
      {
        return new DocumentCommand.OverrideFrag(wmCmd, bookmark);
      }

      throw new DocumentCommand.InvalidCommandException(L.m(
        "Unknown command '%1'", cmd));
    }
    catch (DocumentCommand.InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }
}
