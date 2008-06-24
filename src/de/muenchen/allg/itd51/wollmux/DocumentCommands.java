/*
 * Dateiname: DocumentCommands.java
 * Projekt  : WollMux
 * Funktion : Verwaltet alle in einem Dokument enthaltenen Dokumentkommandos.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.container.XNameAccess;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.AllVersions;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.DraftOnly;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OriginalOnly;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.TextRangeRelation.TreeRelation;

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
public class DocumentCommands
{
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
    Pattern.compile("\\A.*(GROUPS.*)\\d*\\z");

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
  private HashSet<DocumentCommand> notInOriginalCommands;

  /**
   * Enthält ein Set aller OrininalOnly-Dokumentkommandos des Dokuments, die für die
   * Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet<DocumentCommand> originalOnlyCommands;

  /**
   * Enthält ein Set aller draftOnly-Dokumentkommandos des Dokuments, die für die
   * Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet<DocumentCommand> draftOnlyCommands;

  /**
   * Enthält ein Set aller all-Dokumentkommandos des Dokuments, die für die
   * Ein/Ausblendungen in Sachleitenden Verfügungen benötigt werden.
   */
  private HashSet<DocumentCommand> allVersionsCommands;

  /**
   * Erzeugt einen neuen Container für DocumentCommands im TextDocument doc.
   * 
   * @param doc
   */
  public DocumentCommands(XBookmarksSupplier doc)
  {
    this.doc = doc;
    this.allCommands = new HashSet<DocumentCommand>();

    this.visibilityElements = new LinkedList<VisibilityElement>();
    this.setJumpMarkCommands = new LinkedList<SetJumpMark>();
    this.notInOriginalCommands = new HashSet<DocumentCommand>();
    this.originalOnlyCommands = new HashSet<DocumentCommand>();
    this.draftOnlyCommands = new HashSet<DocumentCommand>();
    this.allVersionsCommands = new HashSet<DocumentCommand>();
    this.allTextSectionsWithGROUPS = new HashSet<TextSection>();
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
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
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
   * Dokumentkommandos überschrieben wird. cmdStr muss nur das gewünschte Kommando
   * enthalten ohne eine abschließende Zahl, die zur Herstellung eindeutiger
   * Bookmarks benötigt wird - diese Zahl wird bei Bedarf automatisch an den
   * Bookmarknamen angehängt.
   * 
   * @param r
   *          Die TextRange, an der das neue Bookmark mit diesem Dokumentkommando
   *          eingefügt werden soll. r darf auch null sein und wird in diesem Fall
   *          ignoriert.
   * @param cmdStr
   *          Das Kommando als String der Form "WM(...)".
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    if (r == null) return;
    new Bookmark(cmdStr, UNO.XTextDocument(doc), r);
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
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private boolean updateBookmarks()
  {
    if (doc == null) return false;
    long startTime = System.currentTimeMillis();

    // HashSets mit den Namen der bekannten, gültigen Dokumentkommandos
    // und den ungültigen Dokumentkommandos erstellen:
    HashSet<String> knownBookmarks = new HashSet<String>();
    HashSet<DocumentCommand> retiredDocumentCommands =
      new HashSet<DocumentCommand>();
    for (Iterator<DocumentCommand> iter = allCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();
      if (cmd.isRetired())
        retiredDocumentCommands.add(cmd);
      else
        knownBookmarks.add(cmd.getBookmarkName());
    }

    // Bookmarks scannen und HashSet mit allen neuen Dokumentkommandos aufbauen:
    HashSet<DocumentCommand> newDocumentCommands = new HashSet<DocumentCommand>();
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

    Logger.debug2(L.m(
      "updateBookmarks fertig nach %1 ms. Entfernte/Neue Dokumentkommandos: ",
      new Integer((int) (System.currentTimeMillis() - startTime)))
      + retiredDocumentCommands.size() + " / " + newDocumentCommands.size());
    return retiredDocumentCommands.size() > 0 || newDocumentCommands.size() > 0;
  }

  /**
   * Diese Methode aktualisiert die TextSections, so dass neue und gelöschte
   * TextSections im Dokument erkannt und mit den Datenstrukturen abgeglichen werden.
   * Die Methode liefert true zurück, wenn seit dem letzten update() Änderungen am
   * Bestand der TextSections erkannt wurden und false, wenn es keine Änderungen gab.
   * 
   * @return true, wenn sich die Menge der TextSections seit dem letzten update()
   *         verändert hat und false, wenn es keine Änderung gab.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private boolean updateTextSections()
  {
    XTextSectionsSupplier supp = UNO.XTextSectionsSupplier(doc);
    if (supp == null) return false;
    long startTime = System.currentTimeMillis();

    // HashSets mit den Namen der bekannten, gültigen TextSections
    // und den ungültigen TextSections erstellen:
    HashSet<String> knownTextSections = new HashSet<String>();
    HashSet<TextSection> retiredTextSections = new HashSet<TextSection>();
    for (Iterator<TextSection> iter = allTextSectionsWithGROUPS.iterator(); iter.hasNext();)
    {
      TextSection s = iter.next();
      if (s.isRetired())
        retiredTextSections.add(s);
      else
        knownTextSections.add(s.getName());
    }

    // TextSections scannen und HashSet mit allen TextSections aufbauen:
    HashSet<TextSection> newTextSections = new HashSet<TextSection>();
    String[] textSectionNames = supp.getTextSections().getElementNames();
    for (int i = 0; i < textSectionNames.length; i++)
    {
      String name = textSectionNames[i];
      Matcher m = sectionWithGROUPSPattern.matcher(name);

      if (m.find() && !knownTextSections.contains(name))
      {
        TextSection s =
          createTextSection(name, m.group(1), UNO.XTextSectionsSupplier(doc));
        if (s != null) newTextSections.add(s);
      }
    }

    // lokale Kommandosets aktualisieren:
    removeRetiredTextSections(retiredTextSections);
    addNewTextSections(newTextSections);

    Logger.debug2(L.m(
      "updateTextSections fertig nach %1 ms. Entfernte/Neue TextSections: ",
      new Integer((int) (System.currentTimeMillis() - startTime)))
      + retiredTextSections.size() + " / " + newTextSections.size());
    return retiredTextSections.size() > 0 || newTextSections.size() > 0;
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
  private void addNewDocumentCommands(HashSet<DocumentCommand> newDocumentCommands)
  {
    for (Iterator<DocumentCommand> iter = newDocumentCommands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();

      allCommands.add(cmd);

      if (cmd instanceof SetGroups) addNewVisibilityElement((SetGroups) cmd);
      if (cmd instanceof SetJumpMark) addNewSetJumpMark((SetJumpMark) cmd);
      if (cmd instanceof NotInOriginal) notInOriginalCommands.add(cmd);
      if (cmd instanceof OriginalOnly) originalOnlyCommands.add(cmd);
      if (cmd instanceof DraftOnly) draftOnlyCommands.add(cmd);
      if (cmd instanceof AllVersions) allVersionsCommands.add(cmd);
    }
  }

  /**
   * Abgleich der internen Datenstrukturen: fügt alle in newElements enthaltenen
   * TextSections in die entsprechenden Sets/Listen hinzu.
   * 
   * @param newElements
   *          Set mit allen TextSections, die seit dem letzten update hinzugekommen
   *          sind.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
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
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void addNewVisibilityElement(VisibilityElement element)
  {
    boolean inserted = false;
    XTextRange anchor = element.getAnchor();
    if (anchor == null) return;

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
    if (!inserted) visibilityElements.add(element);
  }

  /**
   * Fügt ein neues SetJumpMark-Kommando cmdB sortiert in die (bereits) vorsortierten
   * Liste aller SetJumpMark-Kommandos ein.
   * 
   * @param cmdB
   *          das hinzuzufügende SetJumpMark-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
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
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void removeRetiredDocumentCommands(HashSet<DocumentCommand> retired)
  {
    allCommands.removeAll(retired);
    visibilityElements.removeAll(retired);
    setJumpMarkCommands.removeAll(retired);
    notInOriginalCommands.removeAll(retired);
    originalOnlyCommands.removeAll(retired);
    draftOnlyCommands.removeAll(retired);
    allVersionsCommands.removeAll(retired);
  }

  /**
   * Abgleich der internen Datenstrukturen: löscht alle in newDocumentCommands
   * enthaltenen Dokumentkommandos aus den entsprechenden Sets/Listen.
   * 
   * @param retired
   *          Set mit allen Dokumentkommandos, die seit dem letzten update ungültig
   *          (gelöscht) wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void removeRetiredTextSections(HashSet<TextSection> retired)
  {
    allTextSectionsWithGROUPS.removeAll(retired);
    visibilityElements.removeAll(retired);
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
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator<DocumentCommand> iterator()
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
  public Iterator<VisibilityElement> setGroupsIterator()
  {
    return visibilityElements.iterator();
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
      return setJumpMarkCommands.getFirst();
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
   * @return ein Iterator, der die Iteration aller NotInOrininal-Dokumentkommandos
   *         dieses Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator<DocumentCommand> notInOriginalIterator()
  {
    return notInOriginalCommands.iterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * OrininalOnly-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller OriginalOnly-Dokumentkommandos
   *         dieses Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator<DocumentCommand> originalOnlyIterator()
  {
    return originalOnlyCommands.iterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * DraftOnly-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller DraftOnly-Dokumentkommandos dieses
   *         Dokuments ermöglicht. Der Iterator kann auch keine Elemente enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator<DocumentCommand> draftOnlyIterator()
  {
    return draftOnlyCommands.iterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller All-Dokumentkommandos
   * dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller All-Dokumentkommandos dieses
   *         Dokuments ermöglicht. Der Iterator kann auch keine Elemente enthalten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Iterator<DocumentCommand> allVersionsIterator()
  {
    return allVersionsCommands.iterator();
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
   * @return
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
      Logger.debug(L.m("Warnung: inkonsistentes Bookmark entdeckt:"));
      Logger.debug(e);
    }
    catch (Exception e)
    {
      Logger.error(e);
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
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static TextSection createTextSection(String name, String groupsStr,
      XTextSectionsSupplier doc)
  {
    if (doc == null) return null;
    XNameAccess sectionsAccess = doc.getTextSections();

    // HashSet mit allen Gruppen GROUPS aufbauen:
    Set<String> groups = new HashSet<String>();
    try
    {
      ConfigThingy groupsCfg =
        new ConfigThingy("", null, new StringReader(groupsStr));
      Iterator<ConfigThingy> giter = groupsCfg.get("GROUPS").iterator();
      while (giter.hasNext())
        groups.add(giter.next().toString());
    }
    catch (java.lang.Exception e)
    {
      Logger.error(
        L.m(
          "Der Textbereich mit dem Namen '%1' enthält ein fehlerhaftes GROUPS-Attribut.",
          name), e);
    }

    try
    {
      XTextSection section = UNO.XTextSection(sectionsAccess.getByName(name));
      if (section != null)
      {
        return new TextSection(section, groups);
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
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
          L.m("Fehlendes CMD-Attribut"));
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

      else if (cmd.compareToIgnoreCase("draftOnly") == 0)
      {
        return new DocumentCommand.DraftOnly(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("notInOriginal") == 0)
      {
        return new DocumentCommand.NotInOriginal(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("originalOnly") == 0)
      {
        return new DocumentCommand.OriginalOnly(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("allVersions") == 0)
      {
        return new DocumentCommand.AllVersions(wmCmd, bookmark);
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
        "Unbekanntes Kommando '%1'", cmd));
    }
    catch (DocumentCommand.InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }

  /**
   * Implementiert einen leer-Executor, von dem abgeleitet werden kann, um konkrete
   * Executoren zu schreiben, mit denen die Dokumentkommandos, die
   * DocumentCommands.iterator() liefert bearbeitet werden können.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class Executor implements DocumentCommand.Executor
  {
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

    public int executeCommand(OriginalOnly cmd)
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

    public int executeCommand(OverrideFrag cmd)
    {
      return 0;
    }
  }
}
