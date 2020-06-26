/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.TextRangeRelation;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.ooo.TextDocument;
import de.muenchen.allg.util.UnoService;

/**
 * Klasse enthält statische Methoden die für das Textbausteinsystem benötigt werden
 *
 * @author bettina.bauer
 */
public class TextModule
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TextModule.class);

  /**
   * Pattern, das insertFrag-Bookmarks matcht.
   */
  private static final Pattern INSERTFRAG_PATTERN =
    DocumentCommands.getPatternForCommand("insertFrag");

  /**
   * Sucht ab der Stelle range rückwarts nach gültigen Textfragmentbezeichnern mit
   * Argumenten, legt um jeden einzufügenden Textbaustein ein Dokumentkommando
   * 'insertFrag' mit den gefundenen Argumenten. Aufgehört wird beim ersten Absatz in
   * dem kein Textbausteinbezeichner identifiziert werden konnte oder wo bereits ein
   * insertFrag vorhanden war.
   *
   * @param doc
   *          Aktuelle Textdocument in dem gesucht werden soll
   * @param range
   *          Stelle in der nach Textfragmentbezeichnern gesucht werden soll. Die
   *          Stelle kann ein markierter Bereich sein oder ein kollabierter Cursor
   *          von dem rückwärts bis zur ersten Zeile die kein Textfragment enthält
   *          gesucht wird. Meistens handelt es sich um den viewCursor.
   * @param isManual
   *          kennzeichnet Einfügungen, die manuell vorgenommen worden sind. Setzt
   *          den optionalen Knoten MODE = "manual"
   *
   * @throws WollMuxFehlerException
   *           falls ein Problem aufgetreten ist (z.B. kein Textbaustein erkannt oder
   *           bereits ein insertFrag-Befehl vorhanden). Eine Exception wird genau
   *           dann geworfen, wenn gar kein Textbausteinverweis eingefügt werden
   *           konnte. Wurde mindestens einer eingefügt, wird keine Exception
   *           geworfen, sondern an der Fehlerstelle mit dem Scan aufgehört.
   */
  public static void createInsertFragFromIdentifier(XTextDocument doc,
      XTextRange range, boolean isManual) throws WollMuxFehlerException
  {
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    // holt sich Textbausteine aus .conf und sammelt sie in umgekehrter
    // Reihenfolge in der LinkedList tbListe. Damit später definierte
    // Textbaustein Abschnitte immer Vorrang haben.
    LinkedList<ConfigThingy> tbListe = new LinkedList<ConfigThingy>();
    ConfigThingy tbConf = conf.query("Textbausteine");
    Iterator<ConfigThingy> iter = tbConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy confTextbaustein = iter.next();
      tbListe.addFirst(confTextbaustein);
    }

    XParagraphCursor cursor =
      UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));

    // Sonderbehandlung, wenn der viewCursor bereits eine Bereich markiert.
    // In diesem Fall soll ausschließlich der Inhalt des Bereichs evaluiert
    // werden. Über einen Vergleich von completeContent und collectedContent
    // kann festgestellt werden, ob cursor den Bereich abdeckt (siehe
    // unten).
    String completeContent = cursor.getString();
    String collectedContent = "";
    if (!completeContent.equals("")) cursor.collapseToEnd();

    boolean processedAtLeastOneTBSuccessfully = false;
    boolean foundAtLeastOneTBInCurrentParagraph = false;
    while (true)
    {
      String identifierWithArgs = cursor.getString();
      if (!identifierWithArgs.equals(""))
        collectedContent = identifierWithArgs.substring(0, 1) + collectedContent;

      String[] results = parseIdentifier(identifierWithArgs, tbListe);

      if (results != null)
      {
        foundAtLeastOneTBInCurrentParagraph = true;

        /*
         * Schauen, ob bereits ein insertFrag-Befehl vorhanden ist, um zu verhindern,
         * dass ein zweiter darüber gelegt wird, da dies diverses Fehlverhalten
         * produzieren kann.
         */
        Set<String> bms =
          TextDocument.getBookmarkNamesMatching(INSERTFRAG_PATTERN, cursor);

        if (bms.size() == 0)
        {
          createInsertFrag(doc, cursor, results, isManual);
          processedAtLeastOneTBSuccessfully = true;

          // Cursor kollabieren, damit beim Weitersuchen nicht der gerade schon
          // verarbeitete Textbausteinbezeichner noch als Teil des nächsten
          // Bezeichners verwendet wird.
          // Die Textbausteinsuche verhält sich also im Gegensatz zur üblichen Art
          // des Matchens von regulären Ausdrücken NICHT greedy, sondern wir nehmen
          // den kürzesten matchenden Bezeichner
          cursor.collapseToStart();
        }
        else
        {
          /*
           * Es wurde bereits ein insertFrag-Kommando an der aktuellen Cursorposition
           * gefunden.
           *
           * Wir werfen nur dann einen Fehler, wenn wir noch gar keinen Textbaustein
           * verarbeitet haben. Ansonsten hören wir einfach nur auf ohne Fehler. Es
           * ist ein absolut legitimer Anwendungsfall, dass ein Anwender erst "TB1"
           * tippt und dann "TextbausteinVERWEIS einfügen" (man beachte: nur beim
           * Einfügen eines VERWEISEs ist es möglich, dass ein insertFrag Bookmark
           * existiert.) macht und dann einen Absatz runtergeht und "TB2" tippt und
           * wieder "Textbausteinverweis einfügen" macht.
           */
          if (!processedAtLeastOneTBSuccessfully)
            throw new WollMuxFehlerException(
              L.m("An der Einfügestelle befindet sich bereits ein Verweis auf einen Textbaustein."));
          else
            break;
        }
      }

      if (cursor.isStartOfParagraph())
      {
        // Falls wir in der ganzen Zeile nichts gefunden haben, dann aufhören.
        if (!foundAtLeastOneTBInCurrentParagraph) break;

        // zum vorherigen Absatz weiter schalten, dabei matchedInLine zurücksetzen.
        cursor.goLeft((short) 1, false);
        foundAtLeastOneTBInCurrentParagraph = false;
      }
      else
      {
        // ein Zeichen nach links gehen (dabei Cursorrange wachsen lassen) und weiter
        // machen.
        cursor.goLeft((short) 1, true);
      }

      // Hier der Vergleich completeContent<->collectedContent: wenn beide
      // übereinstimmen, kann abgebrochen werden, da der Bereich dann
      // vollständig evaluiert wurde.
      if (completeContent.length() > 0 && completeContent.equals(collectedContent))
        break;
    }

    if (!processedAtLeastOneTBSuccessfully)
      throw new WollMuxFehlerException(
        L.m("An der Einfügestelle konnte kein Textbaustein gefunden werden."));
  }

  /**
   * Parsed den übergebenen identifierWithArgs nach allen Abbildungen der Form (MATCH
   * ... FRAG_ID ...), die in den Textbausteine-Abschnitten in tbListe enthalten sind
   * und liefert null zurück, wenn es keine Übereinstimmung mit den MATCHes gab oder
   * falls es eine Übereinstimmung gab ein Array, das an der ersten Stelle die neue
   * frag_id enthält und in den folgenden Stellen die Argumente.
   *
   * @param identifierWithArgs
   *          Ein String in der Form "<identifier>#arg1#...#argN", wobei der
   *          Separator "#" über den SEPARATOR-Schlüssel in textbausteine verändert
   *          werden kann.
   * @param tbListe
   *          Eine Liste, die die Textbausteine-Abschnitte in der Reihenfolge
   *          enthält, in der sie ausgewertet werden sollen.
   * @return Stringarray mit (frag_id + args) oder null
   */
  private static String[] parseIdentifier(String identifierWithArgs,
      List<ConfigThingy> tbListe)
  {
    Iterator<ConfigThingy> iterTbListe = tbListe.iterator();
    while (iterTbListe.hasNext())
    {
      ConfigThingy textbausteine = iterTbListe.next();

      String[] results =
        parseIdentifierInTextbausteine(identifierWithArgs, textbausteine);
      if (results != null) return results;
    }
    return null;
  }

  /**
   * Parsed den übergebenen identifierWithArgs nach allen Abbildungen der Form (MATCH
   * ... FRAG_ID ...), die in textbausteine (=ein einzelner Textbausteine-Abschnitt)
   * enthalten sind und liefert null zurück, wenn es keine Übereinstimmung mit den
   * MATCHes gab oder falls es eine Übereinstimmung gab ein Array, das an der ersten
   * Stelle die neue frag_id enthält und in den folgenden Stellen die Argumente.
   *
   * @param identifierWithArgs
   *          Ein String in der Form "<identifier>#arg1#...#argN", wobei der
   *          Separator "#" über den SEPARATOR-Schlüssel in textbausteine verändert
   *          werden kann.
   * @param textbausteine
   *          Beschreibung eines Textbausteinabschnittes in der Form
   *          "Textbausteine(SEPARATOR ... Kuerzel(...))"
   * @return Stringarray mit (frag_id + args) oder null
   */
  public static String[] parseIdentifierInTextbausteine(String identifierWithArgs,
      ConfigThingy textbausteine)
  {
    // Separator für diesen Textbaustein-Block bestimmen
    String separatorString = "#";
    ConfigThingy separator = textbausteine.query("SEPARATOR");
    if (separator.count() > 0)
    {
      try
      {
        separatorString = separator.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        // optional
      }
    }

    // identifierWithArgs splitten und erstes Argument holen, wenn am Schuß
    // SEPERATOR steht wird -1 noch ein weiteres leeres Element in args[]
    // erzeugt
    String[] args = identifierWithArgs.split(separatorString, -1);
    String first = args[0];

    // Iterieren über alle Knoten der Form "(MATCH ... FRAG_ID ...)"
    ConfigThingy mappingsConf = textbausteine.queryByChild("MATCH");
    Iterator<ConfigThingy> iterMappings = mappingsConf.iterator();
    while (iterMappings.hasNext())
    {
      ConfigThingy mappingConf = iterMappings.next();

      String frag_id = null;
      try
      {
        frag_id = mappingConf.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        LOGGER.error(L.m("FRAG_ID Angabe fehlt in %1",
          mappingConf.stringRepresentation()));
        continue;
      }

      ConfigThingy matches = null;
      try
      {
        matches = mappingConf.get("MATCH");
      }
      catch (NodeNotFoundException e)
      {
        // kommt nicht vor, da obiger queryByChild immer MATCH liefert
        continue;
      }

      for (ConfigThingy it : matches)
      {
        String match = it.toString();

        // if (identifierWithArgs.matches(match)) {
        if (first.matches(match))
        {
          try
          {
            args[0] = first.replaceAll(match, frag_id);
          }
          catch (java.lang.Exception e)
          {
            LOGGER.error(L.m("Die Reguläre Ausdruck Gruppierung $<zahl>, die in FRAG_ID verwendet wird gibt es nicht in MATCH. ")
              ,e);
          }
          return args;
        }
      }
    }
    return null; // wenn nix drin
  }

  /**
   * Erzeugt ein Bookmark vom Typ "WM(CMD'insertFrag' FRAG_ID '&lt;args[0]&gt;'
   * ARGS('&lt;args[1]&gt;' '...' '&lt;args[n]&gt;')" im Dokument doc an der Stelle range.
   *
   * @param doc
   *          Aktuelles Textdokument
   * @param range
   *          Stelle an der das Bookmark gesetzt werden soll
   * @param args
   *          Übergebene Parameter
   * @param isManual
   *          kennzeichnet Einfügungen, die manuell vorgenommen worden sind. Setzt den optinalen
   *          Knoten MODE = "manual"
   */
  public static void createInsertFrag(XTextDocument doc, XTextRange range,
      String[] args, boolean isManual)

  {

    // Neues ConfigThingy für "insertFrag Textbaustein" erzeugen:
    ConfigThingy root = new ConfigThingy("");
    ConfigThingy werte = new ConfigThingy("WM");
    root.addChild(werte);

    ConfigThingy wm_cmd = new ConfigThingy("CMD");
    werte.addChild(wm_cmd);

    ConfigThingy wm_frag_id = new ConfigThingy("FRAG_ID");
    werte.addChild(wm_frag_id);

    if (args.length > 1)
    {
      ConfigThingy wm_args = new ConfigThingy("ARGS");
      for (int i = 1; i < args.length; i++)
      {
        ConfigThingy wm_args_entry = new ConfigThingy(args[i]);
        wm_args.addChild(wm_args_entry);
      }
      werte.addChild(wm_args);
    }

    ConfigThingy insertFrag = new ConfigThingy("insertFrag");
    wm_cmd.addChild(insertFrag);

    ConfigThingy wm_frag_id_entry = new ConfigThingy(args[0]);
    wm_frag_id.addChild(wm_frag_id_entry);

    if (isManual)
    {
      ConfigThingy wm_mode = new ConfigThingy("MODE");
      werte.addChild(wm_mode);

      ConfigThingy wm_mode_entry = new ConfigThingy("manual");
      wm_mode.addChild(wm_mode_entry);
    }

    String bookmarkName = DocumentCommand.getCommandString(root);

    LOGGER.trace(L.m("Erzeuge Bookmark: '%1'", bookmarkName));

    try
    {
      new Bookmark(bookmarkName, doc, range);
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Methode springt ab dem aktuellen viewCursor von einem Platzhalterfeld zum
   * nächsten und fängt dann nochmal von vorne an
   *
   * @param viewCursor
   *          Aktueller ViewCursor im Dokument
   */
  public static void jumpPlaceholders(XTextDocument doc, XTextCursor viewCursor)
  {
    XTextCursor oldPos = viewCursor.getText().createTextCursorByRange(viewCursor);

    // Nächsten Platzhalter anspringen. Dabei berücksichtigen, dass
    // .uno:GotoNextPlacemarker nicht automatisch zu einem evtl. direkt am
    // View-Cursor angrenzenden Platzhalter springt, sondern dann gleich zum
    // nächsten.
    XTextField nearPlacemarker = null;
    if (viewCursor.isCollapsed())
      nearPlacemarker = getPlacemarkerStartingWithRange(doc, viewCursor);
    if (nearPlacemarker != null)
      viewCursor.gotoRange(nearPlacemarker.getAnchor(), false);
    else
      UNO.dispatchAndWait(doc, ".uno:GotoNextPlacemarker");

    // Keinen weiteren Platzhalter gefunden? Dies erkenne ich daran, dass entwder der
    // View-Cursor (falls er bereits auf dem letzten Platzhalter des Dokuments stand)
    // kollabiert wurde oder der View-Cursor auf der selben Stelle wie früher stehen
    // geblieben ist.
    if (viewCursor.isCollapsed()
      || new TextRangeRelation(oldPos, viewCursor).followsOrderscheme8888())
    {
      // Proiere nochmal ab dem Anfang des Dokuments
      viewCursor.gotoRange(doc.getText().getStart(), false);
      nearPlacemarker = null;
      if (viewCursor.isCollapsed())
        nearPlacemarker = getPlacemarkerStartingWithRange(doc, viewCursor);
      if (nearPlacemarker != null)
        viewCursor.gotoRange(nearPlacemarker.getAnchor(), false);
      else
        UNO.dispatchAndWait(doc, ".uno:GotoNextPlacemarker");

      // Falls immer noch kein Platzhalter gefunden wurde wird zur Marke
      // 'setJumpMark' gesprungen falls vorhanden sonst kommt eine Fehlermeldung
      if (new TextRangeRelation(doc.getText().getStart(), viewCursor).followsOrderscheme8888())
      {
        // ViewCursor wieder auf Ausgangsposition setzen.
        viewCursor.gotoRange(oldPos, false);

        // und handle jumpToMark aufrufen.
        new OnJumpToMark(doc, true).emit();
      }
    }
  }

  /**
   * Liefert aus dem Dokument doc das erste Text-Field Objekt vom Typ Placemarker,
   * das gemeinsam mit range an der selben Position startet, oder null falls ein
   * solches Objekt nicht gefunden wird.
   */
  private static XTextField getPlacemarkerStartingWithRange(XTextDocument doc,
      XTextCursor range)
  {
    if (UNO.XTextFieldsSupplier(doc) == null)
    {
      return null;
    }
    UnoCollection<XTextField> textFields = UnoCollection.getCollection(UNO.XTextFieldsSupplier(doc).getTextFields(),
        XTextField.class);
    for (XTextField tf : textFields)
    {
      if (tf != null && UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_JUMP_EDIT))
      {
        XTextRangeCompare c = UNO.XTextRangeCompare(range.getText());
        try
        {
          if (c.compareRegionStarts(range, tf.getAnchor()) == 0) return tf;
        }
        catch (IllegalArgumentException e)
        {
          LOGGER.trace("", e);
        }
      }
    }
    return null;
  }
}
