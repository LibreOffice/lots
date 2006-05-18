/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Interpretiert die in einem Dokument enthaltenen Dokumentkommandos.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung als WMCommandInterpreter
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * 02.05.2006 | LUT | Komplett-Überarbeitung und Umbenennung in
 *                    DocumentCommandInterpreter.
 * 05.05.2006 | BNK | Dummy-Argument zum Aufruf des FormGUI Konstruktors hinzugefügt.
 * 17.05.2006 | LUT | Doku überarbeitet.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Executor;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.RootElement;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Version;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  private WollMuxSingleton mux;

  /**
   * Das Dokument, das interpretiert werden soll.
   */
  private UnoService document;

  /**
   * Die Liste der Fragment-urls, die bei den Kommandos "insertContent"
   * eingefügt werden sollen.
   */
  private String[] fragUrls;

  /**
   * Die Liste der Fragment-urls, die bei den Kommandos "insertContent"
   * eingefügt werden sollen.
   */
  private int fragUrlsCount = 0;

  /**
   * Das ConfigThingy enthält alle Form-Desriptoren, die im Lauf des
   * interpret-Vorgangs aufgesammelt werden.
   */
  private ConfigThingy formDescriptors;

  /**
   * Dieses Flag wird in executeForm auf true gesetzt, wenn das Dokument
   * mindestens eine Formularbeschreibung enthält.
   */
  private boolean documentIsAFormular;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im übergebenen Dokument xDoc scannen und interpretieren
   * kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgeführt werden sollen.
   * @param mux
   *          Die Instanz des zentralen WollMux-Singletons
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die für das Kommando insertContent
   *          benötigt wird.
   */
  public DocumentCommandInterpreter(XTextDocument xDoc, WollMuxSingleton mux,
      String[] frag_urls)
  {
    this.document = new UnoService(xDoc);
    this.mux = mux;
    this.formDescriptors = new ConfigThingy("Forms");
    this.documentIsAFormular = false;
    this.fragUrls = frag_urls;
  }

  /**
   * Über diese Methode wird die eigentliche Ausführung der Interpretation der
   * Dokumentkommandos gestartet. Ein WM-Kommando besitzt sie Syntax "WM (
   * Unterkommando ){Zahl}", wobei Spaces an jeder Stelle auftauchen oder
   * weggelassen werden dürfen. Der Wert {Zahl} am Ende des Kommandos dient zur
   * Unterscheidung verschiedener Bookmarks in OOo und ist optional.
   * 
   * Alle Bookmarks, die nicht dieser Syntax entsprechen, werden als normale
   * Bookmarks behandelt und nicht vom Interpreter bearbeitet.
   * 
   * @throws WMCommandsFailedException
   */
  public void interpret() throws WMCommandsFailedException
  {

    // Dokumentkommando-Baum scannen:
    DocumentCommandTree tree = new DocumentCommandTree(document.xComponent());
    tree.update();

    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    if (isOpenAsTemplate(tree))
    {
      // 1) Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
      // können, damit der DocumentCommandTree vollständig aufgebaut werden
      // kann.
      errors += new DocumentExpander().execute(tree);

      // 2) Und jetzt nochmal alle (übrigen) DocumentCommands (z.B.
      // insertValues) in einem einzigen Durchlauf mit execute aufrufen.
      errors += new MainProcessor().execute(tree);

      // 3) Jetzt können die TextFelder innerhalb der updateFields Kommandos
      // geupdatet werden.
      errors += new TextFieldUpdater().execute(tree);

      // 4) Da keine neuen Elemente mehr eingefügt werden müssen, können
      // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
      // InsertContent-Kommandos gelöscht werden.
      errors += cleanInsertMarks(tree);

      // 5) Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
      // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
      // sauber erkennen und entfernen.
      errors += new EmptyParagraphCleaner().execute(tree);

      // 6) Die Statusänderungen der Dokumentkommandos auf die Bookmarks
      // übertragen bzw. die Bookmarks abgearbeiteter Kommandos löschen. Der
      // Schritt soll aus folgenden Gründen auch dann ausgeführt werden, wenn
      // das
      // Dokument "als Dokument" geöffnet wurde:
      // a) Damit ein evtl. vorhandenes Dokumentkommando BeADocument gelöscht
      // wird.
      // b) Zur Normierung der enthaltenen Bookmarks.
      tree.updateBookmarks(mux.isDebugMode());

      // 7) Document-Modified auf false setzen, da nur wirkliche
      // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
      setDocumentModified(false);
    }

    // ggf. eine WMCommandsFailedException werfen:
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
          "Bei der Dokumenterzeugung mit dem Briefkopfsystem trat(en) "
              + errors
              + " Fehler auf.\n\n"
              + "Bitte überprüfen Sie das Dokument und kontaktieren ggf. die "
              + "für Sie zuständige Systemadministration.");
    }

    // Formulardialog starten:
    if (documentIsAFormular)
    {
      startFormGUI();
    }
  }

  /**
   * Die Methode ermittelt, ob das Dokument als Vorlage behandelt wird und damit
   * die enthaltenen Dokumentkommandos ausgewertet werden sollen (bei true) oder
   * nicht (bei false).
   */
  private boolean isOpenAsTemplate(DocumentCommandTree tree)
  {
    // Vorbelegung: im Zweifelsfall immer als Template öffnen.
    boolean isTemplate = true;

    // Bei Templates besitzt das xDoc kein URL-Attribut, bei normalen
    // Dokumenten schon.
    if (document.xTextDocument() != null)
      isTemplate = (document.xTextDocument().getURL() == null || document
          .xTextDocument().getURL().equals(""));

    return isTemplate;
  }

  private class EmptyParagraphCleaner implements Executor
  {

    /**
     * Diese Methode löscht leere Absätze, die sich um die im Kommandobaum tree
     * enthaltenen Dokumentkommandos vom Typ FragmentInserter befinden.
     * 
     * @param tree
     *          Der Kommandobaum in dem nach FragmentInserter-Kommandos gesucht
     *          wird.
     */
    private int execute(DocumentCommandTree tree)
    {
      setLockControllers(true);

      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        if (cmd.isDone() == true
            && cmd instanceof DocumentCommand.FragmentInserter)
        {
          cmd.execute(this);
        }
      }

      setLockControllers(false);
      return 0;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFrag cmd)
    {
      cleanEmptyParagraphsForCommand(cmd);
      return 0;
    }

    public int executeCommand(InsertValue cmd)
    {
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      cleanEmptyParagraphsForCommand(cmd);
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

    public int executeCommand(Version cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      return 0;
    }

    // Helper-Methoden:

    /**
     * Diese Methode löscht die leeren Absätze zum Beginn und zum Ende des
     * übergebenen Dokumentkommandos cmd, falls leere Absätze vorhanden sind.
     * 
     * @param cmd
     *          Das Dokumentkommando, um das die leeren Absätze entfernt werden
     *          sollen.
     */
    private void cleanEmptyParagraphsForCommand(DocumentCommand cmd)
    {
      Logger.debug2("cleanEmptyParagraphsForCommand(" + cmd + ")");

      // Ersten Absatz löschen, falls er leer ist:

      // benötigte TextCursor holen:
      XTextRange range = cmd.getTextRange();
      UnoService fragStart = new UnoService(null);
      UnoService marker = new UnoService(null);
      if (range != null)
      {
        fragStart = new UnoService(range.getText().createTextCursorByRange(
            range.getStart()));
        marker = new UnoService(range.getText().createTextCursor());
      }

      if (fragStart.xParagraphCursor() != null
          && fragStart.xParagraphCursor().isEndOfParagraph())
      {
        // Der Cursor ist am Ende des Absatzes. Das sagt uns, dass der erste
        // eingefügte Absatz ein leerer Absatz war. Damit soll dieser Absatz
        // gelöscht werden:

        // Jetzt wird der marker verwendet, um die zu löschenden Absatzvorschübe
        // zu markieren. Hier muss man zuerst eins nach rechts gehen und den
        // Bereich von rechts nach links aufziehen, denn sonst würden nach dem
        // kommenden Löschvorgang (setString("")) die Absatzmerkmale des
        // vorherigen Absatzes benutzt und nicht die des nächsten Absatzes wie
        // gewünscht.
        marker.xTextCursor().gotoRange(fragStart.xTextRange(), false);
        marker.xParagraphCursor().goRight((short) 1, false);
        marker.xParagraphCursor().goLeft((short) 1, true);

        // In manchen Fällen verhält sich der Textcursor nach den obigen zwei
        // Zeilen anders als erwartet. Z.B. wenn der nächsten Absatz eine
        // TextTable ist. In diesem Fall ist nach obigen zwei Zeilen die ganze
        // Tabelle markiert und nicht nur das Absatztrennzeichen. Der Cursor
        // markiert also mehr Inhalt als nur den erwarteten Absatzvorschub. In
        // einem solchen Fall, darf der markierte Inhalt nicht gelöscht werden.
        // Anders ausgedrückt, darf der Absatz nur gelöscht werden, wenn beim
        // Markieren ausschließlich Text markiert wurde.
        if (isFollowedByTextParagraph(marker.xEnumerationAccess()))
        {
          // Normalfall: hier darf gelöscht werden
          Logger.debug2("Loesche Absatzvorschubzeichen");

          // Workaround: Normalerweise reicht der setString("") zum Löschen des
          // Zeichens. Jedoch im Spezialfall, dass der zweite Absatz auch leer
          // ist, würde der zweite Absatz ohne den folgenden Workaround seine
          // Formatierung verlieren. Das Problem ist gemeldet unter:
          // http://qa.openoffice.org/issues/show_bug.cgi?id=65384

          // Workaround: bevor der Absatz gelöscht wird, füge ich in den zweiten
          // Absatz einen Inhalt ein.
          marker.xTextCursor().goRight((short) 1, false); // cursor korrigieren
          marker.xTextCursor().setString("c");
          marker.xTextCursor().collapseToStart();
          marker.xTextCursor().goLeft((short) 1, true); // cursor wie vorher

          // hier das eigentliche Löschen des Absatzvorschubs
          marker.xTextCursor().setString("");

          // Workaround: Nun wird der vorher eingefügte Inhalt wieder gelöscht.
          marker.xTextCursor().goRight((short) 1, true);
          marker.xTextCursor().setString("");
        }
        else
        {
          // In diesem Fall darf normalerweise nichts gelöscht werden, ausser
          // der
          // Einfügepunkt des insertFrags/insertContent selbst ist ein
          // leerer Absatz. Dieser leere Absatz kann als ganzes gelöscht
          // werden. Man erkennt den Fall daran, dass fragStart auch der Anfang
          // des Absatzes ist.
          if (fragStart.xParagraphCursor().isStartOfParagraph())
          {
            Logger.debug2("Loesche den ganzen leeren Absatz");
            deleteParagraph(fragStart.xTextCursor());
            // Hierbei wird das zugehörige Bookmark ungültig, da es z.B. eine
            // enthaltene TextTable nicht mehr umschließt. Aus diesem Grund
            // werden
            // Bookmarks nach erfolgreicher Ausführung gelöscht...
          }
        }
      }

      // Letzten Absatz löschen, falls er leer ist:

      // der Range muss hier nochmal geholt werden, für den Fall, dass obige
      // Zeilen das Bookmark mit löschen (der delete Paragraph tut dies z.B.
      // beim
      // insertFrag "Fusszeile" im Zusammenspiel mit TextTables).
      range = cmd.getTextRange();
      UnoService fragEnd = new UnoService(null);
      if (range != null)
      {
        fragEnd = new UnoService(range.getText().createTextCursorByRange(
            range.getEnd()));
        marker = new UnoService(range.getText().createTextCursor());
      }

      if (fragEnd.xParagraphCursor() != null
          && fragEnd.xParagraphCursor().isStartOfParagraph())
      {
        marker.xTextCursor().gotoRange(fragEnd.xTextRange(), false);
        marker.xTextCursor().goLeft((short) 1, true);
        marker.xTextCursor().setString("");
      }
    }

    /**
     * Die Methode prüft, ob der zweite Paragraph des markierte Bereichs ein
     * TextParagraph ist und gibt true zurück, wenn der zweite Paragraph nicht
     * vorhanden ist oder er den Service com.sun.star.text.Paragraph
     * implementiert.
     * 
     * @param enumAccess
     *          die XEnumerationAccess Instanz des markierten Bereichts (ein
     *          TextCursors).
     * @return true oder false
     */
    private boolean isFollowedByTextParagraph(XEnumerationAccess enumAccess)
    {
      if (enumAccess != null)
      {
        XEnumeration xenum = enumAccess.createEnumeration();
        Object element2 = null;

        if (xenum.hasMoreElements()) try
        {
          xenum.nextElement();
        }
        catch (Exception e)
        {
        }

        if (xenum.hasMoreElements())
        {
          try
          {
            element2 = xenum.nextElement();
          }
          catch (Exception e)
          {
          }
        }
        else
          return true;

        return new UnoService(element2)
            .supportsService("com.sun.star.text.Paragraph");
      }
      return false;
    }

    /**
     * Löscht den ganzen ersten Absatz an der Cursorposition textCursor.
     * 
     * @param textCursor
     */
    private void deleteParagraph(XTextCursor textCursor)
    {
      // Beim Löschen des Absatzes erzeugt OOo ein ungewolltes
      // "Zombie"-Bookmark.
      // Issue Siehe http://qa.openoffice.org/issues/show_bug.cgi?id=65247

      UnoService cursor = new UnoService(textCursor);

      // Ersten Absatz des Bookmarks holen:
      UnoService par = new UnoService(null);
      if (cursor.xEnumerationAccess().createEnumeration() != null)
      {
        XEnumeration xenum = cursor.xEnumerationAccess().createEnumeration();
        if (xenum.hasMoreElements()) try
        {
          par = new UnoService(xenum.nextElement());
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }

      // Lösche den Paragraph
      if (cursor.xTextCursor() != null && par.xTextContent() != null) try
      {
        cursor.xTextCursor().getText().removeTextContent(par.xTextContent());
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Veranlasst alle Dokumentkommandos in der Reihenfolge einer Tiefensuche ihre
   * InsertMarks zu löschen.
   */
  public int cleanInsertMarks(DocumentCommandTree tree)
  {
    setLockControllers(true);

    // Das Löschen muss mit einer Tiefensuche, aber in umgekehrter Reihenfolge
    // ablaufen, da sonst leere Bookmarks (Ausdehnung=0) durch das Entfernen der
    // INSERT_MARKs im unmittelbar darauffolgenden Bookmark ungewollt gelöscht
    // werden. Bei der umgekehrten Reihenfolge tritt dieses Problem nicht auf.
    Iterator i = tree.depthFirstIterator(true);
    while (i.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) i.next();
      cmd.cleanInsertMarks();
    }

    setLockControllers(false);
    return 0;
  }

  /**
   * Diese Methode startet die FormularGUI. Sie enthält vorerst nur eine
   * dummy-implementierung, bei der auch noch keine im Dokument gesetzten
   * Feldinhalte ausgewertet werden. D.h. die GUI startet immer mit einer leeren
   * IdToPreset-Map.
   */
  private void startFormGUI()
  {
    FormModel fm = new FormModelImpl(document.xComponent());
    new FormGUI(formDescriptors, fm, new HashMap(), mux.getGlobalFunctions(),
        mux.getFunctionDialogs());
  }

  /**
   * Der DocumentExpander sorgt dafür, dass das Dokument nach Ausführung der
   * enthaltenen Kommandos komplett aufgebaut ist und alle Textfragmente
   * eingefügt wurden.
   * 
   * @author christoph.lutz
   * 
   */
  private class DocumentExpander implements DocumentCommand.Executor
  {
    private boolean changed;

    private int execute(DocumentCommandTree tree)
    {
      int errors = 0;
      do
      {
        changed = false;

        // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
        tree.update();
        Iterator iter = tree.depthFirstIterator(false);
        while (iter.hasNext())
        {
          DocumentCommand cmd = (DocumentCommand) iter.next();

          if (cmd.isDone() == false && cmd.hasError() == false)
          {
            // Kommando ausführen und Fehler zählen
            errors += cmd.execute(this);
          }
        }
      } while (changed);
      return errors;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFrag cmd)
    {
      changed = true;
      cmd.setErrorState(false);
      try
      {
        // Fragment-URL holen und aufbereiten. Kontext ist der DEFAULT_CONTEXT.
        String urlStr = mux.getTextFragmentList().getURLByID(cmd.getFragID());

        URL url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);

        Logger.debug("Füge Textfragment \""
                     + cmd.getFragID()
                     + "\" von URL \""
                     + url.toExternalForm()
                     + "\" ein.");

        // fragment einfügen:
        insertDocumentFromURL(cmd, url);
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      cmd.setDoneState(true);
      return 0;
    }

    public int executeCommand(InsertValue cmd)
    {
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      changed = true;
      cmd.setErrorState(false);
      if (fragUrls.length > fragUrlsCount)
      {
        String urlStr = fragUrls[fragUrlsCount++];

        try
        {
          Logger.debug("Füge Textfragment von URL \"" + urlStr + "\" ein.");

          insertDocumentFromURL(cmd, new URL(urlStr));
        }
        catch (java.lang.Exception e)
        {
          insertErrorField(cmd, e);
          cmd.setErrorState(true);
          return 1;
        }
      }
      cmd.setDoneState(true);
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

    public int executeCommand(Version cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      return 0;
    }

    // Helper-Methoden:

    /**
     * Die Methode fügt das externe Dokument von der URL url an die Stelle von
     * cmd ein. Die Methode enthält desweiteren notwendige Workarounds für die
     * Bugs des insertDocumentFromURL der UNO-API.
     * 
     * @param cmd
     *          Einfügeposition
     * @param url
     *          die URL des einzufügenden Textfragments
     * @throws java.io.IOException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private void insertDocumentFromURL(DocumentCommand cmd, URL url)
        throws java.io.IOException, IOException, IllegalArgumentException
    {

      // Workaround: OOo friert ein, wenn ressource bei insertDocumentFromURL
      // nicht auflösbar. http://qa.openoffice.org/issues/show_bug.cgi?id=57049
      // Hier wird versucht, die URL über den java-Klasse url aufzulösen und bei
      // Fehlern abgebrochen.
      if (url.openConnection().getContentLength() <= 0)
      {
        throw new IOException("Das Textfragment mit der URL \""
                              + url.toExternalForm()
                              + "\" ist leer oder nicht verfügbar");
      }

      // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
      // verarbeitet werden kann.
      String urlStr = null;
      try
      {
        UnoService trans = UnoService.createWithContext(
            "com.sun.star.util.URLTransformer",
            mux.getXComponentContext());
        com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
        unoURL[0].Complete = url.toExternalForm();
        trans.xURLTransformer().parseStrict(unoURL);
        urlStr = unoURL[0].Complete;
      }
      catch (Exception e)
      {
        Logger.error(e);
      }

      // Workaround: Alten Paragraphenstyle merken. Problembeschreibung siehe
      // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
      String paraStyleName = null;
      UnoService endCursor = new UnoService(null);
      XTextRange range = cmd.getTextRange();
      if (range != null)
      {
        endCursor = new UnoService(range.getText().createTextCursorByRange(
            range.getEnd()));
      }
      try
      {
        if (endCursor.xPropertySet() != null)
          paraStyleName = endCursor.getPropertyValue("ParaStyleName")
              .getObject().toString();
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      // Liste aller TextFrames vor dem Einfügen zusammenstellen (benötigt für
      // das
      // Updaten der enthaltenen TextFields später).
      HashSet textFrames = new HashSet();
      if (document.xTextFramesSupplier() != null)
      {
        String[] names = document.xTextFramesSupplier().getTextFrames()
            .getElementNames();
        for (int i = 0; i < names.length; i++)
        {
          textFrames.add(names[i]);
        }
      }

      // Textfragment einfügen:
      UnoService insCursor = new UnoService(cmd.createInsertCursor());
      if (insCursor.xDocumentInsertable() != null && urlStr != null)
      {
        insCursor.xDocumentInsertable().insertDocumentFromURL(
            urlStr,
            new PropertyValue[] {});
      }

      // Workaround: ParagraphStyleName für den letzten eingefügten Paragraphen
      // wieder setzen (siehe oben).
      if (endCursor.xPropertySet() != null && paraStyleName != null)
      {
        try
        {
          endCursor.setPropertyValue("ParaStyleName", paraStyleName);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }
  }

  /**
   * Der Hauptverarbeitungsschritt, in dem vor allem die Textinhalte gefüllt
   * werden.
   * 
   * @author christoph.lutz
   * 
   */
  private class MainProcessor implements DocumentCommand.Executor
  {

    /**
     * Diese Methode führt alle Dokumentkommandos aus, wenn Sie nicht den Status
     * DONE=true oder ERROR=true besitzen.
     * 
     * @param tree
     *          Der Kommandobaum der die auszuführenden Kommandos enthält.
     * @return die Anzahl der bei der Ersetzung aufgetretenen Fehler.
     */
    private int execute(DocumentCommandTree tree)
    {
      // Während der Bearbeitung der einfachen Kommandos (z.B. insertValues)
      // muss man nicht jede Änderung sofort sehen (Geschwindigkeitsvorteil, da
      // OOo nicht neu rendern muss). Ist im Debug-Modus abgeschalten:
      setLockControllers(true);

      // Ausführung der Kommandos
      int errors = 0;
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        if (cmd.isDone() == false && cmd.hasError() == false)
        {
          // Kommandos ausführen und Fehler zählen
          errors += cmd.execute(this);
        }
      }

      // Jetzt darf man wieder was sehen:
      setLockControllers(false);

      return errors;
    }

    /**
     * Diese Methode bearbeitet ein InvalidCommand und fügt ein Fehlerfeld an
     * der Stelle des Dokumentkommandos ein.
     */
    public int executeCommand(DocumentCommand.InvalidCommand cmd)
    {
      insertErrorField(cmd, cmd.getException());
      cmd.setErrorState(true);
      return 1;
    }

    /**
     * Diese Methode fügt einen Spaltenwert aus dem aktuellen Datensatz der
     * Absenderdaten ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
     */
    public int executeCommand(DocumentCommand.InsertValue cmd)
    {
      cmd.setErrorState(false);
      try
      {
        String spaltenname = cmd.getDBSpalte();
        Dataset ds;
        try
        {
          ds = mux.getDatasourceJoiner().getSelectedDataset();
        }
        catch (DatasetNotFoundException e)
        {
          throw new Exception(
              "Kein Absender ausgewählt! Bitte wählen Sie einen Absender aus!");
        }
        XTextCursor insCursor = cmd.createInsertCursor();
        if (insCursor != null)
        {
          if (ds.get(spaltenname) == null || ds.get(spaltenname).equals(""))
          {
            insCursor.setString("");
          }
          else
          {
            insCursor.setString(cmd.getLeftSeparator()
                                + ds.get(spaltenname)
                                + cmd.getRightSeparator());
          }
        }
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      cmd.setDoneState(true);
      return 0;
    }

    /**
     * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
     * bookmarkName ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
     */
    public int executeCommand(DocumentCommand.InsertFrag cmd)
    {
      return 0;
    }

    /**
     * Diese Methode fügt das nächste Textfragment aus der dem
     * WMCommandInterpreter übergebenen frag_urls liste ein. Im Fehlerfall wird
     * die Fehlermeldung eingefügt.
     */
    public int executeCommand(DocumentCommand.InsertContent cmd)
    {
      return 0;
    }

    /**
     * Hinter einem Form-Kommando verbirgt sich eine Notiz, die das Formular
     * beschreibt, das in der FormularGUI angezeigt werden soll. Das Kommando
     * executeForm sammelt alle solchen Formularbeschreibungen im
     * formDescriptor. Enthält der formDescriptor mehr als einen Eintrag, wird
     * nach dem interpret-Vorgang die FormGUI gestartet.
     */
    public int executeCommand(DocumentCommand.Form cmd)
    {
      cmd.setErrorState(false);
      try
      {
        XTextRange range = cmd.getTextRange();
        Object content = null;
        if (range != null)
        {
          UnoService cursor = new UnoService(range.getText()
              .createTextCursorByRange(range));
          UnoService textfield = cursor.getPropertyValue("TextField");
          content = textfield.getPropertyValue("Content").getObject();
        }
        if (content != null)
        {
          ConfigThingy ct = new ConfigThingy("", null, new StringReader(content
              .toString()));
          ConfigThingy formulars = ct.query("Formular");
          if (formulars.count() == 0)
            throw new ConfigurationErrorException(
                "Formularbeschreibung enthält keinen Abschnitt \"Formular\".");
          documentIsAFormular = true;
          Iterator formIter = formulars.iterator();
          while (formIter.hasNext())
          {
            ConfigThingy form = (ConfigThingy) formIter.next();
            formDescriptors.addChild(form);
          }
        }
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      cmd.setDoneState(true);
      return 0;
    }

    /**
     * Gibt Informationen über die aktuelle Install-Version des WollMux aus.
     */
    public int executeCommand(DocumentCommand.Version cmd)
    {
      XTextCursor insCurs = cmd.createInsertCursor();
      if (insCurs != null)
        insCurs.setString("Build-Info: " + mux.getBuildInfo());

      cmd.setDoneState(true);
      return 0;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      return 0;
    }
  }

  private class TextFieldUpdater implements DocumentCommand.Executor
  {
    private int execute(DocumentCommandTree tree)
    {
      setLockControllers(true);

      // Ausführung der Kommandos
      int errors = 0;
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        if (cmd.isDone() == false && cmd.hasError() == false)
        {
          // Kommandos ausführen und Fehler zählen
          errors += cmd.execute(this);
        }
      }

      // Jetzt darf man wieder was sehen:
      setLockControllers(false);

      return errors;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
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

    public int executeCommand(Version cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      Logger.debug("Ausführen des Kommandos \"" + cmd + "\"");
      XTextRange range = cmd.getTextRange();
      if (range != null)
      {
        UnoService cursor = new UnoService(range.getText()
            .createTextCursorByRange(range));
        updateTextFieldsRecursive(cursor);
      }
      cmd.setDoneState(true);
      return 0;
    }

    /**
     * Diese Methode durchsucht das Element element rekursiv nach TextFeldern
     * und ruft deren Methode update() auf.
     * 
     * @param element
     *          Das Element das geupdated werden soll.
     */
    private void updateTextFieldsRecursive(UnoService element)
    {
      // zuerst die Kinder durchsuchen (falls vorhanden):
      if (element.xEnumerationAccess() != null)
      {
        XEnumeration xEnum = element.xEnumerationAccess().createEnumeration();

        while (xEnum.hasMoreElements())
        {
          try
          {
            UnoService child = new UnoService(xEnum.nextElement());
            updateTextFieldsRecursive(child);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      }

      // jetzt noch update selbst aufrufen (wenn verfügbar):
      if (element.xTextField() != null)
      {
        try
        {
          UnoService textField = element.getPropertyValue("TextField");
          if (textField.xUpdatable() != null)
          {
            textField.xUpdatable().update();
          }
        }
        catch (Exception e)
        {
        }
      }
    }
  }

  // Helper-Methoden:

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   * 
   * @param state
   */
  private void setDocumentModified(boolean state)
  {
    try
    {
      document.xModifiable().setModified(state);
    }
    catch (PropertyVetoException x)
    {
      // wenn jemand was dagegen hat, dann setze ich halt nichts.
    }
  }

  /**
   * Diese Methode blockt/unblock die Contoller, die für das Rendering der
   * Darstellung in den Dokumenten zuständig sind, jedoch nur, wenn nicht der
   * debug-modus gesetzt ist.
   * 
   * @param state
   */
  private void setLockControllers(boolean lock)
  {
    if (mux.isDebugMode() == false && document.xModel() != null) if (lock)
      document.xModel().lockControllers();
    else
      document.xModel().unlockControllers();
  }

  /**
   * Diese Methode fügt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, java.lang.Exception e)
  {
    String msg = "Fehler in Dokumentkommando \"" + cmd.getBookmarkName() + "\"";

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
      Logger.error(msg, e);
    else
      Logger.error(msg);

    UnoService cursor = new UnoService(cmd.createInsertCursor());
    cursor.xTextCursor().setString("<FEHLER:  >");

    // Text fett und rot machen:
    try
    {
      cursor.setPropertyValue("CharColor", new Integer(0xff0000));
      cursor.setPropertyValue("CharWeight", new Float(FontWeight.BOLD));
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }

    // Ein Annotation-Textfield erzeugen und einfügen:
    try
    {
      XTextRange range = cursor.xTextCursor().getEnd();
      UnoService c = new UnoService(range.getText().createTextCursorByRange(
          range));
      c.xTextCursor().goLeft((short) 2, false);
      UnoService note = document
          .create("com.sun.star.text.TextField.Annotation");
      note.setPropertyValue("Content", msg + ":\n\n" + e.getMessage());
      c.xTextRange().getText().insertTextContent(
          c.xTextRange(),
          note.xTextContent(),
          false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
  }

}
