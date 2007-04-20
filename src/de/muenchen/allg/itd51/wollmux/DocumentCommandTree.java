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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.table.XCell;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextTable;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;

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
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFunctionValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;

/**
 * Diese Klasse repräsentiert eine Liste von Dokumentkommandos eines Dokuments,
 * die in einer baumartigen Struktur (jeder Knoten kann einen Elternknoten
 * besitzen) angeordnet sind. Der Baum kann sich selbst updaten, d.h. er scannt
 * das Dokument nach neuen Bookmarks und erzeugt ggf. neue Dokumentkommandos,
 * die er zusätzlich in die Liste einhängen kann.
 */
public class DocumentCommandTree
{
  /**
   * Folgendes Pattern prüft ob es sich bei einem Bookmark um ein gültiges
   * "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
   */
  public static final Pattern wmCmdPattern = Pattern
      .compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'([^']*)'.*\\))\\s*\\d*\\z");

  /**
   * Das Dokument, welches die DokumentCommands enthält.
   */
  private XBookmarksSupplier xBookmarksSupplier;

  /**
   * Die Liste aller Dokumentkommandos dieses Dokuments in der Reihenfolge, in
   * der die Dokumentkommandos im Dokument vorkommen.
   */
  private List commands;

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
    this.commands = new ArrayList();
  }

  /**
   * Diese Methode durchsucht das Dokument nach neuen, bisher nicht gescannten
   * Dokumentkommandos und fügt diese dem Baum hinzu oder entfernt
   * Dokumentkommandos, die nicht mehr gültig sind, weil ihr zugehöriges
   * Bookmark nicht mehr existiert. Das Kommando liefert true zurück, wenn sich
   * der Kommandobaum durch das update() verändert hat, ansonsten false.
   * 
   * @return true, wenn sich der Kommandobaum durch das update verändert hat und
   *         false, wenn der Baum unverändert ist.
   */
  public boolean update()
  {
    long millis = System.currentTimeMillis();
    Logger.debug("update(#" + xBookmarksSupplier.hashCode() + ")");

    // map der bereits bekannten bookmarks/Dokumentkommandos erstellen.
    HashMap knownDocumentCommands = new HashMap();
    for (Iterator iter = iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      knownDocumentCommands.put(cmd.getBookmarkName(), cmd);
    }

    // Baum neu aufbauen:
    List marks = new DocumentCommandMarksScanner().scan(UNO
        .XTextDocument(xBookmarksSupplier));
    Parser p = new Parser();
    commands = p.parse(marks, knownDocumentCommands, xBookmarksSupplier);

    Logger.debug("update finished in "
                 + (System.currentTimeMillis() - millis)
                 + " millis: "
                 + ((p.hasChanged())
                                    ? "Dokumentkommandobaum wurde geändert"
                                    : "keine Änderung")
                 + " --> "
                 + commands.size()
                 + " Dokumentkommandos.");
    return p.hasChanged();
  }

  /**
   * Diese Methode liefert einen Iterator über alle Elemente des Kommandobaumes
   * wobei die Elemente in der Reihenfolge geliefert werden, in der sie im
   * Dokument erscheinen.
   * 
   * @return Iterator über alle Elemente des Kommandobaumes in
   *         Dokumentreihenfolge.
   */
  public Iterator iterator()
  {
    return commands.iterator();
  }

  /**
   * Repräsentiert eine Start- oder Endemarke eines Dokumentkommandos mit allen
   * Informationen, die der DocumentCommandsScanner benötigt um eine geordnete
   * Liste aller Dokumentkommando-Marken zurückliefern zu können. Eine
   * DocumentCommandMark lässt sich am besten über dessen String-Repräsentation
   * beschreiben, die folgenden Aufbau hat:
   * 
   * CMD[posA, posB]: <BOOKMARK-NAME> (bei Startmarken) bzw.
   * 
   * CMD[posA, posB]: </BOOKMARK-NAME> (bei Endemarken)
   * 
   * z.B.: insertFrag[0, 5]: <WM(CMD'insertFrag' FRAG_ID'a')1>
   * 
   * posA beschreibt dabei die Wertigkeit der Stelle an der die Marke selbst im
   * Dokument liegt. Die Stelle ergibt sich während des Scanvorgangs, der einen
   * Zähler immer dann erhöht, wenn auf ein Bookmark ein Element folgt, das kein
   * Bookmark ist. So können z.B. zwei Bookmarks an der selben Stelle im
   * Dokument starten und erhalten den selben Stellenwert, da zwischen den
   * beiden Marken kein Nicht-Bookmarkelement enthalten ist.
   * 
   * In einem konsistenten Dokument besitzt jede Startmarke eine zugehörige
   * Endemarke und umgekehrt. posB beschreibt die Wertigkeit der Stelle des
   * Partnerelements. Die Partnermarke wird im Lauf des Scanvorgangs bestimmt.
   * 
   * Das Kommando CMD beschreibt den Inhalt des CMD-Attributs des
   * Dokumentkommandos, das in compare() ausgewertet wird um sicherzustellen,
   * dass eine Vorrangregelung von Dokumentkommandos realisiert werden kann,
   * wenn zwei Dokumentkommandos an der selben Stelle im Dokument starten oder
   * enden und die Reihenfolge damit sonst nicht exakt definiert wäre.
   * 
   * Der Name der Marke wird in BOOKMARK-NAME angezeigt und der Typ (Startmarke
   * oder Endemarke) an den an die HTML-Syntax angelehnte Syntax der
   * <BOOKMARK-NAME> bzw. </BOOKMARK-NAME>.
   * 
   * @author christoph.lutz
   * 
   */
  private static class DocumentCommandMark implements Comparable
  {
    /**
     * Wird wie oben beschrieben vom Scanner gesetzt und enthält die Position
     * dieser Marke selbst.
     */
    private Long posA;

    /**
     * Wird wie oben beschrieben vom Scanner gesetzt und enthält die Position
     * der zugehörigen Parnermarke.
     */
    private Long posB;

    /**
     * Enthält den vollständigen Namen des Bookmarks
     */
    public final String name;

    /**
     * ist true, wenn es sich um eine Startmarke handelt und false, wenn es sich
     * um eine Endemarke handelt.
     */
    public final boolean isStart;

    /**
     * Enthält das CMD-Attribut des Dokumentkommandos.
     */
    public final String cmd;

    /**
     * Enthält den Kommandostring des Dokumentkommandos, der durch ein
     * ConfigThingy geparst werden kann und entspricht somit dem Bookmarknamen
     * ohne die abschließenden Nummern. Der Wert wird vom Scanner als
     * Nebenprodukt mit erzeugt und hier für den späteren Parserdurchlauf
     * aufgehoben, der das Feld dann auswertet.
     */
    public final String wmStr;

    /**
     * Bookmarks mit diesen CMD-Attributen erhalten bei der Sortierung Vorrang
     * vor anderen Bookmarks, wenn die verglichenen Bookmarks an der selben
     * Stelle starten und an der selben Stelle enden.
     */
    public final static String[] precedence = new String[] {
                                                            "updateFields",
                                                            "insertFrag",
                                                            "insertContent",
                                                            "setGroups",
                                                            "draftonly",
                                                            "notInOrininal",
                                                            "allVersions" };

    /**
     * Erzeugt eine neue DocumentCommandMark mit dem Bookmarknamen name, dem
     * wm-KommandoString wmStr, dem CMD-Attribut cmd und dem Typ
     * (Startmarke/Endemarke), der durch isStart beschrieben wird.
     */
    public DocumentCommandMark(String name, String wmStr, String cmd,
        boolean isStart)
    {
      this.name = name;
      this.wmStr = wmStr;
      this.cmd = cmd;
      this.isStart = isStart;
    }

    /**
     * Kann vom Scanner aufgerufen werden, um die Position dieser Marke
     * festzulegen.
     */
    public void setPosA(Long pos)
    {
      posA = pos;
    }

    /**
     * Liefert die Position dieser Marke, wenn sie durch den Scanner gesetzt
     * wurde oder null, wenn noch keine Marke gesetzt wurde.
     */
    public Long getPosA()
    {
      return posA;
    }

    /**
     * Kann vom Scanner aufgerufen werden, um die Position der Partnermarke
     * festzulegen.
     */
    public void setPosB(Long pos)
    {
      posB = pos;
    }

    /**
     * Liefert die Position dier Parnermarke, wenn sie durch den Scanner gesetzt
     * wurde oder null, wenn noch keine Marke gesetzt wurde.
     */
    public Long getPosB()
    {
      return posB;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
      String str = cmd + "[" + posA + ", " + posB + "]: ";
      if (isStart)
        str += "<" + name + ">";
      else
        str += "</" + name + ">";
      return str;
    }

    /**
     * Enthält die Vorschrift für die Sortierung von DocumentCommandMarks mit
     * dem Ziel, eine eindeutige Reihenfolge von Start-/Endemarken zu
     * definieren, die insbesondere dann gilt, wenn die Marken an selben Stellen
     * anfangen oder enden und die Reihenfolge damit nicht bereits durch die
     * Dokumentreihenfolge eindeutig definiert ist. Die Sortierung zweier Marken
     * this und other erfolgt nach folgenden Kriterien:
     * 
     * 1) Vergleich der eigenen Position posA (= Dokumentreihenfolge):
     * Aufsteigende Sortierung nach posA, wenn sich this.posA und other.posA
     * unterscheiden.
     * 
     * 2) Wenn this an der selben Stelle endet, an der other startet, so soll
     * der endende Marker (also this) vor dem startenden Marker einsortiert
     * werden, damit die zwei Bookmarks als disjunkte Bookmarks verstanden
     * werden und sich nicht überschneiden. Z.B. StartA|StartB,EndeA|EndeB soll
     * in die Reihenfolge StartA|EndeA,StartB|EndeB gebracht werden. "|"
     * symbolisiert dabei den Start einer neuen Position. Im Beispiel wäre
     * this=EndeA und other=StartB. Ein Marker, der ein an einer früheren Stelle
     * geöffnetes Element abschließt wird dadurch erkannt, dass posB < posA ist.
     * 
     * 3) Vergleich der Position der Parnermarke in absteigender Sortierung,
     * wenn sich this.posB und other.posB unterscheiden: Stellt sicher, dass
     * geschachtelte Elemente auch in umgekehrter Reihenfolge geschlossen
     * werden.
     * 
     * Ab jetzt gilt this.posA = other.posA und this.posB = other.posB -> die
     * Bookmarks liegen also exakt übereinander.
     * 
     * 4) Um sicherzustellen, dass übereinander liegende, kollabierte
     * Dokumentkommandos, in der korrekten Reihenfolge starten und enden, werden
     * Startmarken bevorzugt, wenn this eine Startmarke und other eine Endemarke
     * ist oder umgekehrt.
     * 
     * 5) Vorrangregelung für identische Bookmarks: Treffen zwei
     * Dokumentkommandos zusammen, deren Bookmarks den selben Bereich abdecken,
     * so soll das Dokumentkommando bevorzugt werden, welches Kinderelemente
     * enthalten kann, und das Dokumentkommando umschlossen werden, das keine
     * Kinderelemente enthalten kann, wenn sich die beiden Dokumentkommandos
     * bezüglich der Fähigkeit, Kinder aufnehmen zu können, unterscheiden. Für
     * zwei Endemarken gilt das umgekehrte Ergebnis um die korrekte
     * Verschachtelung sicherzustellen.
     * 
     * Ab jetzt gilt: this und other unterscheiden sich nicht in der Fähigkeit,
     * Kinder aufnehmen zu können und liegen exakt übereinander. this und other
     * sind außerdem vom selben Typ (Startmarke/Endemarke)
     * 
     * 6) Nachdem jetzt alle Möglichkeiten zur Bestimmung einer eindeutigen
     * Reihenfolge abgeschöpft sind, die aus funktionaler Sicht notwendig sind,
     * bleibt hier nur noch übrig, die bookmarknamen nach Alphabet (Aufsteigend
     * bei Startmarken und Absteigend bei Endemarken) zu sortieren. Diese
     * Defaultregelung ist trotzdem wichtig, um immer gleiches Verhalten bei den
     * selben Eingabedaten sicher zu stellen.
     * 
     * @param other
     *          Das Element other, mit dem Verglichen wird
     * @return Einen negativen Wert, wenn this vor other einsortiert werden soll
     *         und einen positiven Wert, wenn other vor this einsortiert werden
     *         soll. Da eine diese Abbildungsvorschrift eine eindeutige
     *         Reihenfolge festlegt, wird 0 (=identisch) nur dann
     *         zurückgeliefert, wenn this==other ist.
     */
    public int compareTo(Object other)
    {
      DocumentCommandMark b = (DocumentCommandMark) other;

      // Vergleich nach 1)
      int comp = posA.compareTo(b.posA);
      if (comp != 0) return comp;

      // Vergleich nach 2)
      if (posB.compareTo(posA) < 0 && b.posB.compareTo(posA) >= 0)
        return -1;
      else if (posB.compareTo(posA) >= 0 && b.posB.compareTo(posA) < 0)
        return 1;

      // Vergleich nach 3)
      comp = b.posB.compareTo(posB);
      if (comp != 0) return comp;

      // Vergleich nach 4)
      if (isStart != b.isStart) return (isStart) ? -1 : 1;

      // Vergleich nach 5)
      boolean precThis = false;
      boolean precOther = false;
      for (int i = 0; i < precedence.length; i++)
      {
        if (cmd.equalsIgnoreCase(precedence[i])) precThis = true;
        if (b.cmd.equalsIgnoreCase(precedence[i])) precOther = true;
      }
      if (precThis != precOther)
        return ((isStart) ? 1 : -1) * ((precThis) ? -1 : 1);

      // Vergleich nach 6)
      if (isStart)
        return name.compareTo(b.name);
      else
        return -name.compareTo(b.name);
    }
  }

  /**
   * Der DocumentCommandMarksScanner scannt das Dokument und liefert eine
   * sortiert Liste mit den Start- und Endemarken der enthaltenen
   * Dokumentkommandos zurück, aus der sich eine eindeutige Hierarchie von
   * Dokumentkommandos (durch den Parser) ableiten lässt.
   * 
   * Um die korrekte Reihenfolge der Start- und Endemarken bestimmen zu können,
   * reicht die Enumeration, die XBookmarksSupplier liefert nicht aus. Jedes
   * Element des Dokuments (Text, TextFrames, Paragraph, TextTable) wird hier
   * einzeln durchsucht und über die entsprechende Iterationsmöglichkeit des
   * Elements (z.B. XEnumerationAccess) iteriert.
   * 
   * Start- und Endemarken, zwischen denen sich Elemente befinden, die keine
   * Bookmarks sind, lassen sich klar von einander abgrenzen. Es gibt aber auch
   * Start- und Endemarken, die bei der Itaration direkt aufeinander folgen. In
   * diesem Fall liegen diese Marken an der selben Stelle im Dokument. Da die
   * Reihenfolge der Marken an der selben Stelle von Dokument zu Dokument
   * variieren kann und nicht klar definiert ist, muss hier eine eindeutige
   * Reihenfolge festgelegt werden. Dies geschieht über Elemente des Typs
   * DocumentCommandMark, die alle Informationen enthalten, mit denen eine
   * eindeutige Reihenfolge festgelegt werden kann. Der Scanner erzeugt eine
   * Liste solcher DocumentCommandMark-Objekte, versorgt die Objekte mit den
   * notwendigen Informationen (z.B. Position des Elements und des
   * Partnerelements) und führt die Sortierung der Elemente durch.
   * 
   * Desweiteren stellt der Scanner sicher, dass jeder Startmarke auch eine
   * Endemarke zugeordnet ist. Dies ist notwendig, da die Konsistenz seitens OOo
   * nicht immer gegeben ist. So verliert OOo z.B. die Endemarken von manchen
   * insertFrag-Befehlen, wenn die erste- und letzte Zeile des Fragments vom
   * GarbageCollector entfernt wird.
   * 
   * Der Scanner verhindert nicht, dass sich Bookmarks überlappen können (z.B.
   * StartA, StartB, EndeA, EndeB). Es ist Aufgabe des Parsers, den Fall
   * überlappender Bookmarks zu behandeln.
   * 
   * @author christoph.lutz
   * 
   */
  private static class DocumentCommandMarksScanner
  {
    /**
     * Enthält die Liste aller gesammelten Marken.
     */
    private List marksList = new ArrayList();

    /**
     * Enthält die Namen aller Bookmarks, die vor der aktuellen Position
     * gestartet, jedoch noch nicht abgeschlossen wurden.
     */
    private HashMap started = new HashMap();

    /**
     * Bestimmung der Position: incrementCount wird auf true gesetzt wenn ein
     * Dokumentkommando gefunden wurde und dann zurückgesetzt, wenn das erste
     * NICHT-Dokumentkommando gefunden wurde, über das die Erhöhung des Counters
     * getriggert wird.
     */
    private boolean incrementPosition = false;

    /**
     * Enthält die aktuelle Position und wird immer dann erhöht, wenn auf ein
     * Bookmark das erste NICHT-Bookmark Element folgt.
     */
    private long position = 0;

    /**
     * Erhöht die Position um 1, wenn incrementPosition==true ist und setzt
     * incrementPosition zurück auf false.
     */
    private void incrementPosition()
    {
      if (incrementPosition)
      {
        incrementPosition = false;
        position++;
      }
    }

    /**
     * Scannt das Dokument doc nach Start- und Endemarken von Dokumentkommandos
     * und liefert diese (auch dann wenn die Marken an der selben Stelle im
     * Dokument liegen) in einer definierten Reihenfolge zurück. Die
     * Eigenschaften der zurückgegebenen List sind oben ausführlich beschrieben.
     * 
     * @param bookmarksSupplier
     *          das Dokument, das durchsucht werden soll.
     * @return Liste aller Start- und Endemarken von Dokumentkommandos
     */
    public List scan(XTextDocument doc)
    {
      if (doc == null) return marksList;

      // Zuerst die TextFrames durchsuchen.
      scanTextFrames(doc);

      // Dann den Body-Text durchsuchen.
      scanText(doc.getText());

      // remove incomplete marks
      for (Iterator iter = marksList.iterator(); iter.hasNext();)
      {
        DocumentCommandMark mark = (DocumentCommandMark) iter.next();
        if (mark.getPosA() == null || mark.getPosB() == null)
        {
          iter.remove();
          Logger
              .debug("scan WARNUNG: Inkonsistenz in den bookmarks: ignoriere unvollständiges Bookmark: '"
                     + mark.name
                     + "'");
        }
      }

      // sort marks:
      Collections.sort(marksList);

      return marksList;
    }

    private void scanTextFrames(Object doc)
    {
      XTextFramesSupplier tfs = UNO.XTextFramesSupplier(doc);
      if (tfs == null) return;

      XIndexAccess ia = UNO.XIndexAccess(tfs.getTextFrames());
      if (ia == null) return;

      for (int i = 0; i < ia.getCount(); ++i)
      {
        try
        {
          scanText(ia.getByIndex(i));
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    private void scanText(Object text)
    {
      XEnumerationAccess ea = UNO.XEnumerationAccess(text);
      if (ea == null) return;

      XEnumeration xenum = ea.createEnumeration();
      while (xenum.hasMoreElements())
      {
        Object parOrTable = null;
        try
        {
          parOrTable = xenum.nextElement();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
          continue;
        }

        try
        {
          if (UNO.XEnumerationAccess(parOrTable) != null)
          {
            // parOrTable ist ein Paragraph
            scanParagraph(parOrTable);
            incrementPosition();
          }
          else
          {
            // parOrTable ist wohl eine TextTable
            scanTextTable(parOrTable);
          }
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    private void scanParagraph(Object par)
    {
      XEnumerationAccess ea = UNO.XEnumerationAccess(par);
      if (ea == null) return;

      XEnumeration xenum = ea.createEnumeration();
      while (xenum.hasMoreElements())
      {
        try
        {
          scanTextPortion(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    private void scanTextTable(Object textTable)
    {
      XTextTable table = UNO.XTextTable(textTable);
      if (table == null) return;

      String[] cellNames = table.getCellNames();
      for (int i = 0; i < cellNames.length; ++i)
      {
        XCell cell = table.getCellByName(cellNames[i]);
        scanText(cell);
      }
    }

    private void scanTextPortion(Object content)
    {
      String type = "" + UNO.getProperty(content, "TextPortionType");

      if (type.equals("Bookmark"))
      {
        XNamed bookmark = UNO.XNamed(UNO.getProperty(content, "Bookmark"));
        if (bookmark == null) return;

        String name = bookmark.getName();

        Matcher m = wmCmdPattern.matcher(name);
        if (!m.find()) return;
        String wmStr = m.group(1);
        String cmd = m.group(2);

        boolean isStart = false;
        try
        {
          isStart = AnyConverter.toBoolean(UNO.getProperty(content, "IsStart"));
        }
        catch (IllegalArgumentException e)
        {
          return;
        }

        boolean isCollapsed = false;
        try
        {
          isCollapsed = AnyConverter.toBoolean(UNO.getProperty(
              content,
              "IsCollapsed"));
        }
        catch (IllegalArgumentException e)
        {
          return;
        }

        incrementPosition = true;

        if (isStart)
        {
          DocumentCommandMark start = new DocumentCommandMark(name, wmStr, cmd,
              isStart);
          start.setPosA(new Long(position));
          marksList.add(start);

          started.put(name, start);
        }

        if (!isStart || isCollapsed)
        {
          DocumentCommandMark end = new DocumentCommandMark(name, wmStr, cmd,
              false);
          end.setPosA(new Long(position));
          marksList.add(end);

          DocumentCommandMark start = (DocumentCommandMark) started.get(name);
          if (start != null)
          {
            end.setPosB(start.getPosA());
            start.setPosB(end.getPosA());
          }
        }
      }
      else
        incrementPosition();
    }
  }

  /**
   * Der Parser bekommt die vorsortierte Liste des Scanners mit Start- und
   * Endemarken von Dokumentkommandos übergeben und erzeugt daraus eine Liste
   * mit Dokumentkommandos in der Reihenfolge, in der die Dokumentkommandos im
   * Dokument vorkommen. Der Parser ist in der Lage bereits vorhandene
   * Dokumentkommandos zu berücksichtigen und diese nicht neu zu erzeugen.
   * Ausgehend von der Menge der bereits vorhandenen Dokumentkommandos kann der
   * Scanner ermitteln, ob sich der Bestand der Dokumentkommandos durch den scan
   * verändert hat.
   * 
   * @author christoph.lutz
   * 
   */
  private static class Parser
  {
    /**
     * Enthält den changed-Status des letzten parse-Vorgangs.
     */
    private boolean changed = false;

    /**
     * parst die in marks enthaltene vorsortierte Liste mit Start- und
     * Endemarken von Dokumentkommandos und erzeugt daraus eine Liste mit
     * Objekten vom Typ DocumentCommand, wobei Dokumentkommandos, die in
     * knownDocumentCommands vorkommen nicht neu erzeugt, sondern von dort
     * verwendet werden. Bei der Erzeugung der Dokumentkommandos muss der
     * XBookmarksSupplier übergeben werden, der für die Erstellung der
     * Dokumentkommandos (genauer deren Bookmark-Objekte) notwendig ist.
     * 
     * Der Parser versteht die Hierarchie, die die Start- und Endemarken
     * beschreiben und setzt entsprechend die Elternknoten von Dokumentkommandos
     * über DocumentCommand.setParent(...). Der Parser kommt auch damit klar,
     * wenn zwei Dokumentkommandos überschneiden (z.B. StartA, StartB, EndA,
     * EndB). In diesem Fall wird das erste Element der Liste zum Elternknoten
     * und das spätere Element der Kindknoten (im Beispiel: B wäre Kind von A).
     * 
     * @param marks
     *          Enhält die vorsortierte Liste mit Objekten vom Typ
     *          DocumentCommandMark
     * @param knownDocumentCommands
     *          enthält eine Zuordnung von Bookmark-Namen auf bereits vorhandene
     *          Dokumentkommandos.
     * @param bookmarksSupplier
     *          Enthält den BookmarksSupplier (also das TextDokument)
     * @return Eine Liste der Dokumentkommandos in Dokumentreihenfolge.
     */
    public List parse(List marks, HashMap knownDocumentCommands,
        XBookmarksSupplier bookmarksSupplier)
    {
      changed = false;
      Stack stack = new Stack();
      HashSet finished = new HashSet();
      ArrayList commands = new ArrayList();

      for (Iterator iter = marks.iterator(); iter.hasNext();)
      {
        DocumentCommandMark mark = (DocumentCommandMark) iter.next();
        if (mark.isStart)
        {
          // bestehendes Dokumentkommando holen oder neues erzeugen:
          DocumentCommand cmd = (DocumentCommand) knownDocumentCommands
              .remove(mark.name);
          if (cmd == null)
          {
            cmd = createCommand(mark.wmStr, mark.name, bookmarksSupplier);
            changed = true;
          }
          if (cmd == null) continue;

          stack.push(cmd);
          commands.add(cmd);
        }
        else
        {
          finished.add(mark.name);
          reduceStack(stack, finished);
        }
      }

      if (knownDocumentCommands.size() > 0) changed = true;

      return commands;
    }

    /**
     * Nimmt das oberste Element des Stacks stack vom stack, falls es im HashSet
     * finished vorkommt, und setzt den Elternknoten bei diesen Elementen
     * entsprechend. Diese Vorgehensweise ist notwendig, um mit Bookmarks klar
     * zu kommen, die nicht exakt verschachtelt sind und sich überschneiden.
     * 
     * @param stack
     * @param finished
     */
    private void reduceStack(Stack stack, HashSet finished)
    {
      if (stack.size() == 0) return;
      String top = ((DocumentCommand) stack.peek()).getBookmarkName();
      if (finished.contains(top))
      {
        finished.remove(top);
        DocumentCommand child = (DocumentCommand) stack.pop();
        if (stack.size() > 0)
        {
          DocumentCommand parent = (DocumentCommand) stack.peek();
          if (parent.canHaveChilds())
            child.setParent(parent);
          else
          {
            child.setParent(null);
            Logger.error("Die beiden Dokumentkommandos "
                         + child
                         + " und "
                         + parent
                         + " dürfen sich nicht überschneiden!");
          }
          reduceStack(stack, finished);
        }
        else
          child.setParent(null);
      }
    }

    /**
     * Liefert Auskunft, ob sich der Bestand der Dokumentkommandos seit dem
     * letzten Parse-Vorgang verändert hat.
     * 
     * @return true, wenn der Bestand verändert wurde und false, wenn sich keine
     *         Änderung ergeben hat.
     */
    public boolean hasChanged()
    {
      return changed;
    }

    /**
     * erzeugt ein neues Dokumentkommando mit dem KommandoString cmdStr
     * (entspricht dem Bookmarknamen ohne die Abschließenden Ziffern zur
     * Unterscheidung gleichbenannter Bookmarks) für das Bookmark bookmarkName,
     * und den bookmarksSupplier.
     * 
     * @param wmCmd
     *          KommandoString cmdStr (entspricht dem Bookmarknamen ohne die
     *          Abschließenden Ziffern zur Unterscheidung gleichbenannter
     *          Bookmarks)
     * @param bookmarkName
     *          Name des zugehörigen Bookmarks
     * @param bookmarksSupplier
     *          Der bookmarksSupplier
     * @return das erzeugte Dokumentkommando
     */
    private static DocumentCommand createCommand(String wmCmd,
        String bookmarkName, XBookmarksSupplier bookmarksSupplier)
    {
      Bookmark bookmark;
      try
      {
        bookmark = new Bookmark(bookmarkName, bookmarksSupplier);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
        return null;
      }

      try
      {
        ConfigThingy wmCmdConf = new ConfigThingy("", null, new StringReader(
            wmCmd));
        return createCommand(wmCmdConf, bookmark);
      }
      catch (IOException e)
      {
        Logger.error(e);
        return null;
      }
      catch (SyntaxErrorException e)
      {
        return new InvalidCommand(bookmark, e);
      }
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

        throw new InvalidCommandException("Unbekanntes Kommando \""
                                          + cmd
                                          + "\"");
      }
      catch (InvalidCommandException e)
      {
        return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
      }
    }
  }

  public static class TreeExecutor implements DocumentCommand.Executor
  {
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
    protected int executeDepthFirst(DocumentCommandTree tree)
    {
      int errors = 0;

      // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
      Iterator iter = tree.iterator();
      while (iter.hasNext())
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

  /**
   * Main-Methode für Testzwecke. Erzeugt einen Kommandobaum aus dem aktuell in
   * OOo geöffneten Dokument und gibt diesen auf System.out aus.
   * 
   * @param args
   * @throws BootstrapException
   * @throws Exception
   */
  public static void main(String[] args) throws java.lang.Exception
  {
    Logger.init("all");

    UNO.init();

    // Test der compare-Methode:
    List marks = new ArrayList();
    DocumentCommandMark mark = null;

    /*
     * 1) Vergleich der eigenen Position posA (= Dokumentreihenfolge):
     * Aufsteigende Sortierung nach posA, wenn sich this.posA und other.posA
     * unterscheiden.
     * 
     * Erwartet: <A></A>
     */
    mark = new DocumentCommandMark("A", "egal", "egal", false);
    mark.setPosA(new Long(11));
    mark.setPosB(new Long(20));
    marks.add(mark);

    mark = new DocumentCommandMark("A", "egal", "egal", true);
    mark.setPosA(new Long(10));
    mark.setPosB(new Long(20));
    marks.add(mark);

    /*
     * 2) Wenn this an der selben Stelle endet, an der other startet, so soll
     * der endende Marker (also this) vor dem startenden Marker einsortiert
     * werden, damit die zwei Bookmarks als disjunkte Bookmarks verstanden
     * werden und sich nicht überschneiden. Z.B. StartA|StartB,EndeA|EndeB soll
     * in die Reihenfolge StartA|EndeA,StartB|EndeB gebracht werden. "|"
     * symbolisiert dabei den Start einer neuen Position. Im Beispiel wäre
     * this=EndeA und other=StartB. Ein Marker, der ein an einer früheren Stelle
     * geöffnetes Element abschließt wird dadurch erkannt, dass posB < posA ist.
     * 
     * Erwartet: </B><C>
     */
    mark = new DocumentCommandMark("C", "egal", "egal", true);
    mark.setPosA(new Long(22));
    mark.setPosB(new Long(22));
    marks.add(mark);

    mark = new DocumentCommandMark("B", "egal", "egal", false);
    mark.setPosA(new Long(22));
    mark.setPosB(new Long(5));
    marks.add(mark);

    /*
     * 3) Vergleich der Position der Parnermarke in absteigender Sortierung,
     * wenn sich this.posB und other.posB unterscheiden: Stellt sicher, dass
     * geschachtelte Elemente auch in umgekehrter Reihenfolge geschlossen
     * werden.
     * 
     * Erwartet: <D><E></E></D>
     */
    mark = new DocumentCommandMark("D", "egal", "egal", true);
    mark.setPosA(new Long(30));
    mark.setPosB(new Long(32));
    marks.add(mark);

    mark = new DocumentCommandMark("E", "egal", "egal", true);
    mark.setPosA(new Long(31));
    mark.setPosB(new Long(32));
    marks.add(mark);

    mark = new DocumentCommandMark("E", "egal", "egal", false);
    mark.setPosA(new Long(32));
    mark.setPosB(new Long(31));
    marks.add(mark);

    mark = new DocumentCommandMark("D", "egal", "egal", false);
    mark.setPosA(new Long(32));
    mark.setPosB(new Long(30));
    marks.add(mark);

    /*
     * Ab jetzt gilt this.posA = other.posA und this.posB = other.posB -> die
     * Bookmarks liegen also exakt übereinander.
     * 
     * 4) Um sicherzustellen, dass übereinander liegende, kollabierte
     * Dokumentkommandos, in der korrekten Reihenfolge starten und enden, werden
     * Startmarken bevorzugt, wenn this eine Startmarke und other eine Endemarke
     * ist oder umgekehrt.
     * 
     * Erwartet: <G></H>
     */
    mark = new DocumentCommandMark("H", "egal", "egal", false);
    mark.setPosA(new Long(40));
    mark.setPosB(new Long(40));
    marks.add(mark);

    mark = new DocumentCommandMark("G", "egal", "egal", true);
    mark.setPosA(new Long(40));
    mark.setPosB(new Long(40));
    marks.add(mark);

    /*
     * 5) Vorrangregelung für identische Bookmarks: Treffen zwei
     * Dokumentkommandos zusammen, deren Bookmarks den selben Bereich abdecken,
     * so soll das Dokumentkommando bevorzugt werden, welches Kinderelemente
     * enthalten kann, und das Dokumentkommando umschlossen werden, das keine
     * Kinderelemente enthalten kann, wenn sich die beiden Dokumentkommandos
     * bezüglich der Fähigkeit, Kinder aufnehmen zu können, unterscheiden. Für
     * zwei Endemarken gilt das umgekehrte Ergebnis um die korrekte
     * Verschachtelung sicherzustellen.
     * 
     * Erwartet: <I><J></J></I>
     */
    mark = new DocumentCommandMark("J", "egal", "insertValue", false);
    mark.setPosA(new Long(51));
    mark.setPosB(new Long(50));
    marks.add(mark);

    mark = new DocumentCommandMark("I", "egal", "insertFrag", false);
    mark.setPosA(new Long(51));
    mark.setPosB(new Long(50));
    marks.add(mark);

    mark = new DocumentCommandMark("I", "egal", "insertFrag", true);
    mark.setPosA(new Long(50));
    mark.setPosB(new Long(51));
    marks.add(mark);

    mark = new DocumentCommandMark("J", "egal", "insertValue", true);
    mark.setPosA(new Long(50));
    mark.setPosB(new Long(51));
    marks.add(mark);

    /*
     * Ab jetzt gilt: this und other unterscheiden sich nicht in der Fähigkeit,
     * Kinder aufnehmen zu können und liegen exakt übereinander. this und other
     * sind außerdem vom selben Typ (Startmarke/Endemarke)
     * 
     * 6) Nachdem jetzt alle Möglichkeiten zur Bestimmung einer eindeutigen
     * Reihenfolge abgeschöpft sind, die aus funktionaler Sicht notwendig sind,
     * bleibt hier nur noch übrig, die bookmarknamen nach Alphabet (Aufsteigend
     * bei Startmarken und Absteigend bei Endemarken) zu sortieren. Diese
     * Defaultregelung ist trotzdem wichtig, um immer gleiches Verhalten bei den
     * selben Eingabedaten sicher zu stellen.
     * 
     * Erwartet: <K><L></L></K>
     */
    mark = new DocumentCommandMark("L", "egal", "insertValue", true);
    mark.setPosA(new Long(60));
    mark.setPosB(new Long(61));
    marks.add(mark);

    mark = new DocumentCommandMark("K", "egal", "insertValue", true);
    mark.setPosA(new Long(60));
    mark.setPosB(new Long(61));
    marks.add(mark);

    mark = new DocumentCommandMark("L", "egal", "insertValue", false);
    mark.setPosA(new Long(61));
    mark.setPosB(new Long(60));
    marks.add(mark);

    mark = new DocumentCommandMark("K", "egal", "insertValue", false);
    mark.setPosA(new Long(61));
    mark.setPosB(new Long(60));
    marks.add(mark);

    Collections.sort(marks);
    for (Iterator iter = marks.iterator(); iter.hasNext();)
    {
      mark = (DocumentCommandMark) iter.next();
      System.out.println(mark);
    }

    // Aktuelles Dokument auslesen:
    marks = new DocumentCommandMarksScanner().scan(UNO
        .XTextDocument(UNO.desktop.getCurrentComponent()));

    for (Iterator iter = marks.iterator(); iter.hasNext();)
    {
      mark = (DocumentCommandMark) iter.next();
      System.out.println(mark);
    }

    System.exit(0);
  }
}
