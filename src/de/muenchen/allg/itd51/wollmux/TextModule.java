/*
 * Dateiname: TextModul.java
 * Projekt  : WollMux
 * Funktion : Hilfsmethoden zum Textbausteinsystem
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 26.09.2006 | LUT | Erstellung als TextModul
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Klasse enthält statische Methoden die für das Textbausteinsystem benötigt
 * werden
 * 
 * @author bettina.bauer
 */
public class TextModule
{
  /**
   * Sucht ab der Stelle range rückwarts nach gültigen Textfragmentbezeichnern
   * mit Argumenten, legt um jedes einzufügenden Textbaustein ein
   * Dokumentkommando 'insertFrag' mit den gefundenen Argumenten und liefert
   * true zurück wenn mind. ein Dokumentenkommando erzeugt wurde.
   * 
   * @param doc
   *          Aktuelle Textdocument in dem gesucht werden soll
   * @param range
   *          Stelle in der nach Textfragmentbezeichnern gesucht werden soll.
   *          Die Stelle kann ein markierter Bereich sein oder ein kollabierter
   *          Cursor von dem rückwärts bis zur ersten Zeile die kein
   *          Textfragment enthält gesucht wird. Meistens handelt es sich um den
   *          viewCursor.
   * @param isManual
   *          kennzeichnet Einfügungen, die manuell vorgenommen worden sind.
   *          Setzt den optinalen Knoten MODE = "manual"
   */
  public static boolean createInsertFragFromIdentifier(XTextDocument doc,
      XTextRange range, boolean isManual)
  {
    boolean result = false;
    ConfigThingy conf = WollMuxSingleton.getInstance().getWollmuxConf();

    // holt sich Textbausteine aus .conf und sammelt sie in umgekehrter
    // Reihenfolge in der LinkedList tbListe. Damit später definierte
    // Textbaustein Abschnitte immer Vorrang haben.
    LinkedList tbListe = new LinkedList();
    ConfigThingy tbConf = conf.query("Textbausteine");
    Iterator iter = tbConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy confTextbaustein = (ConfigThingy) iter.next();
      tbListe.addFirst(confTextbaustein);
    }

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Sonderbehandlung, wenn der viewCursor bereits eine Bereich markiert.
    // In diesem Fall soll ausschließlich der Inhalt des Bereichs evaluiert
    // werden. Über einen Vergleich von completeContent und collectedContent
    // kann festgestellt werden, ob cursor den Bereich abdeckt (siehe
    // unten).
    String completeContent = cursor.getString();
    String collectedContent = "";
    if (!completeContent.equals("")) cursor.collapseToEnd();

    boolean matchedInLine = false;
    boolean evaluateNext = false;
    do
    {
      String identifierWithArgs = cursor.getString();
      if (!identifierWithArgs.equals(""))
        collectedContent = identifierWithArgs.substring(0, 1)
                           + collectedContent;

      String[] results = parseIdentifier(identifierWithArgs, tbListe);

      if (results != null)
      {
        matchedInLine = true;
        createInsertFrag(doc, cursor, results, isManual);
        cursor.collapseToStart();
        result = true;
      }

      if (cursor.isStartOfParagraph())
      {
        // zum vorherigen Absatz weiter schalten, dabei matchedInLine
        // zurücksetzen. Nur weiter machen, wenn matchedInLine==true
        cursor.goLeft((short) 1, false);
        evaluateNext = matchedInLine;
        matchedInLine = false;
      }
      else
      {
        // ein Zeichen nach links gehen und weiter machen.
        cursor.goLeft((short) 1, true);
        evaluateNext = true;
      }

      // Hier der Vergleich completeContent<->collectedContent: wenn beide
      // übereinstimmen, kann abgebrochen werden, da der Bereich dann
      // vollständig evaluiert wurde.
      if (!completeContent.equals("")
          && completeContent.equals(collectedContent)) evaluateNext = false;
    } while (evaluateNext);

    return result;
  }

  /**
   * Parsed den übergebenen identifierWithArgs nach allen Abbildungen der Form
   * (MATCH ... FRAG_ID ...), die in den Textbausteine-Abschnitten in tbListe
   * enthalten sind und liefert null zurück, wenn es keine Übereinstimmung mit
   * den MATCHes gab oder falls es eine Übereinstimmung gab ein Array, das an
   * der ersten Stelle die neue frag_id enthält und in den folgenden Stellen die
   * Argumente.
   * 
   * @param identifierWithArgs
   *          Ein String in der Form "<identifier>#arg1#...#argN", wobei der
   *          Separator "#" über den SEPARATOR-Schlüssel in textbausteine
   *          verändert werden kann.
   * @param tbListe
   *          Eine Liste, die die Textbausteine-Abschnitte in der Reihenfolge
   *          enthält, in der sie ausgewertet werden sollen.
   * @return Stringarray mit (frag_id + args) oder null
   */
  private static String[] parseIdentifier(String identifierWithArgs,
      List tbListe)
  {
    Iterator iterTbListe = tbListe.iterator();
    while (iterTbListe.hasNext())
    {
      ConfigThingy textbausteine = (ConfigThingy) iterTbListe.next();

      String[] results = parseIdentifierInTextbausteine(
          identifierWithArgs,
          textbausteine);
      if (results != null) return results;
    }
    return null;
  }

  /**
   * Parsed den übergebenen identifierWithArgs nach allen Abbildungen der Form
   * (MATCH ... FRAG_ID ...), die in textbausteine (=ein einzelner
   * Textbausteine-Abschnitt) enthalten sind und liefert null zurück, wenn es
   * keine Übereinstimmung mit den MATCHes gab oder falls es eine
   * Übereinstimmung gab ein Array, das an der ersten Stelle die neue frag_id
   * enthält und in den folgenden Stellen die Argumente.
   * 
   * @param identifierWithArgs
   *          Ein String in der Form "<identifier>#arg1#...#argN", wobei der
   *          Separator "#" über den SEPARATOR-Schlüssel in textbausteine
   *          verändert werden kann.
   * @param textbausteine
   *          Beschreibung eines Textbausteinabschnittes in der Form
   *          "Textbausteine(SEPARATOR ... Kuerzel(...))"
   * @return Stringarray mit (frag_id + args) oder null
   */
  public static String[] parseIdentifierInTextbausteine(
      String identifierWithArgs, ConfigThingy textbausteine)
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
    Iterator iterMappings = mappingsConf.iterator();
    while (iterMappings.hasNext())
    {
      ConfigThingy mappingConf = (ConfigThingy) iterMappings.next();

      String frag_id = null;
      try
      {
        frag_id = mappingConf.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error("FRAG_ID Angabe fehlt in "
                     + mappingConf.stringRepresentation());
        continue;
      }

      Iterator matchesIterator = null;
      try
      {
        matchesIterator = mappingConf.get("MATCH").iterator();
      }
      catch (NodeNotFoundException e)
      {
        // kommt nicht vor, da obiger queryByChild immer MATCH liefert
        continue;
      }

      while (matchesIterator.hasNext())
      {
        String match = matchesIterator.next().toString();

        // if (identifierWithArgs.matches(match)) {
        if (first.matches(match))
        {
          try
          {
            args[0] = first.replaceAll(match, frag_id);
          }
          catch (java.lang.Exception e)
          {
            Logger.error("Die Reguläre Ausdruck Gruppierung $<zahl>, die in FRAG_ID verwendet wird gibt es nicht in MATCH. " + e);         }
          return args;
        }
      }
    }
    return null; // wenn nix drin
  }

  /**
   * Erzeugt ein Bookmark vom Typ "WM(CMD'insertFrag' FRAG_ID '<args[0]>'
   * ARGS('<args[1]>' '...' '<args[n]>')" im Dokument doc an der Stelle range.
   * 
   * @param doc
   *          Aktuelles Textdokument
   * @param range
   *          Stelle an der das Bookmark gesetzt werden soll
   * @param args
   *          Übergebene Parameter
   * @param isManual
   *          kennzeichnet Einfügungen, die manuell vorgenommen worden sind.
   *          Setzt den optinalen Knoten MODE = "manual"
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

    Logger.debug2("Erzeuge Bookmark: '" + bookmarkName + "'");

    new Bookmark(bookmarkName, doc, range);
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
    boolean found = false;

    XTextCursor cursor = UNO.XTextCursor(viewCursor.getText()
        .createTextCursorByRange(viewCursor.getEnd()));

    cursor.gotoRange(cursor.getText().getEnd(), true);

    for (int i = 0; i < 2 && found == false; i++)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(cursor).createEnumeration();
      XEnumerationAccess enuAccess;

      // Schleife über den Textbereich
      while (xEnum.hasMoreElements() && found == false)
      {
        Object ele = null;
        try
        {
          ele = xEnum.nextElement();
        }
        catch (Exception e)
        {
          continue;
        }
        enuAccess = UNO.XEnumerationAccess(ele);
        if (enuAccess != null) // ist wohl ein SwXParagraph
        {
          XEnumeration textPortionEnu = enuAccess.createEnumeration();
          // Schleife über SwXParagraph und schauen ob es Platzhalterfelder gibt
          // diese diese werden dann im Vector placeholders gesammelt
          while (textPortionEnu.hasMoreElements() && found == false)
          {
            Object textPortion;
            try
            {
              textPortion = textPortionEnu.nextElement();
            }
            catch (java.lang.Exception x)
            {
              continue;
            }
            String textPortionType = (String) UNO.getProperty(
                textPortion,
                "TextPortionType");
            // Wenn es ein Textfeld ist
            if (textPortionType.equals("TextField"))
            {
              XTextField textField = null;
              try
              {
                textField = UNO.XTextField(UNO.getProperty(
                    textPortion,
                    "TextField"));
                // Wenn es ein Platzhalterfeld ist, dem Vector placeholders
                // hinzufügen

                if (UNO.supportsService(
                    textField,
                    "com.sun.star.text.TextField.JumpEdit"))
                {
                  viewCursor.gotoRange(textField.getAnchor(), false);
                  found = true;
                }
              }
              catch (java.lang.Exception e)
              {
                continue;
              }
            }
          }
        }
      }
      cursor = UNO.XTextCursor(viewCursor.getText().createTextCursorByRange(
          cursor.getText()));
    }
    // Falls kein Platzhalter gefunden wurde wird zur Marke 'setJumpMark'
    // gesprungen falls vorhanden sonst kommt eine Fehlermeldung -->
    // Übergabeparameter true
    if (found == false)
    {
      WollMuxEventHandler.handleJumpToMark(doc, true);
    }
  }
}
