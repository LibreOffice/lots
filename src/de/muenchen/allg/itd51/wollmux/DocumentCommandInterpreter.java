/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Scannt alle Bookmarks eines Dokuments und interpretiert die enthaltenen
 *            Dokumentkommandos.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
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
import java.util.Iterator;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.io.IOException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.ExecutableCommand;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter implements DocumentCommand.Executor
{

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
   * Abbruchwert für die Anzahl der Interationsschritte beim Auswerten neu
   * hinzugekommener Bookmarks.
   */
  private static final int MAXCOUNT = 100;

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
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Kommandos
   * im übergebenen xDoc scannen und entsprechend auflösen kann.
   * 
   * @param xDoc
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
   * WM-Kommandos gestartet. Ein WM-Kommando besitzt sie Syntax "WM (
   * Unterkommando ){Zahl}", wobei Spaces an jeder Stelle auftauchen oder
   * weggelassen werden dürfen. Der Wert {Zahl} am Ende des Kommandos dient zur
   * Unterscheidung verschiedener Bookmarks in OOo und ist optional.
   * 
   * Alle Bookmarks, die nicht dieser Syntax entsprechen, werden als normale
   * Bookmarks behandelt und nicht vom Interpreter bearbeitet.
   * 
   * @throws EndlessLoopException
   * @throws WMCommandsFailedException
   */
  public void interpret() throws EndlessLoopException,
      WMCommandsFailedException
  {
    // Dokumentkommando-Baum scannen:
    DocumentCommandTree tree = new DocumentCommandTree(document.xComponent());

    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errorCount = 0;

    // Zuerst alle insertFrags und insertContents abarbeiten bis sich der Baum
    // nicht mehr ändert. Der loopCount dient zur Vermeidung von
    // Endlosschleifen.
    boolean changed = true;
    int loopCount = 0;
    while (changed && MAXCOUNT > ++loopCount)
    {
      changed = false;

      // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
      tree.update();
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        if ((cmd instanceof DocumentCommand.InsertFrag || cmd instanceof DocumentCommand.InsertContent)
            && cmd.isDone() == false)
        {
          changed = true;

          // Kommandos ausführen
          Object result = ((ExecutableCommand) cmd).execute(this);
          errorCount += ((Integer) result).intValue();
        }
      }
    }

    // Bei der Bearbeitung der insertValues muss man nicht jede Änderung sofort
    // sehen:
    if (document.xModel() != null) document.xModel().lockControllers();

    // Und jetzt nochmal alle (übrigen) DocumentCommands in einem einzigen
    // Durchlauf mit execute aufrufen.
    tree.update();
    Iterator iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd instanceof ExecutableCommand && cmd.isDone() == false)
      {
        // Kommandos ausführen
        Object result = ((ExecutableCommand) cmd).execute(this);
        errorCount += ((Integer) result).intValue();
      }
    }

    // entfernen der INSERT_MARKS
    tree.cleanInsertMarks();

    // jetzt nochmal den Baum durchgehen und alle leeren Absätze zum Beginn und
    // Ende der insertFrags und insertContents entfernen.
    iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if ((cmd instanceof DocumentCommand.InsertFrag || cmd instanceof DocumentCommand.InsertContent)
          && cmd.isDone() == false)
      {
        cleanEmptyParagraphs(cmd);
      }
    }
    
    
    // jetzt soll man wieder was sehen:
    if (document.xModel() != null) document.xModel().unlockControllers();

    // updates der Bookmarks:
    tree.updateBookmarks();

    // Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    try
    {
      document.xModifiable().setModified(false);
    }
    catch (PropertyVetoException x)
    {
      // wenn jemand was dagegen hat, dann setze ich halt nichts.
    }

    // ggf. EndlessLoopException mit dem Namen des Dokuments schmeissen.
    if (loopCount == MAXCOUNT)
    {
      UnoService frame = new UnoService(document.xModel()
          .getCurrentController().getFrame());
      String name;
      try
      {
        name = frame.getPropertyValue("Title").toString();
      }
      catch (Exception e)
      {
        name = "";
      }

      throw new EndlessLoopException(
          "Endlosschleife bei der Textfragment-Ersetzung in Dokument \""
              + name
              + "\"");
    }

    // ggf. eine WMCommandsFailedException werfen:
    if (errorCount != 0)
    {
      throw new WMCommandsFailedException(
          "Bei der Dokumenterzeugung mit dem Briefkopfsystem trat(en) "
              + errorCount
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

  private void cleanEmptyParagraphs(DocumentCommand cmd)
  {
    XTextRange range = cmd.getTextRange();
    // Falls die erste Zeile des eingefügten Textfragments leer ist, wird die
    // erste Zeile gelöscht.
    UnoService cursor = new UnoService(range.getText()
        .createTextCursorByRange(range.getStart()));
    if (cursor.xParagraphCursor().isEndOfParagraph())
    {
      cursor.xTextCursor().goRight((short) 1, true);
      cursor.xTextCursor().setString("");
    }

    // Falls die letzte Zeile des eingefügten Textfragments leer ist, wird die
    // letzte Zeile gelöscht.
    cursor = new UnoService(range.getText()
        .createTextCursorByRange(range.getEnd()));
    if (cursor.xParagraphCursor().isStartOfParagraph())
    {
      cursor.xTextCursor().goLeft((short) 1, true);
      cursor.xTextCursor().setString("");
    }
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
    new FormGUI(formDescriptors, fm, new HashMap());
  }

  public Object executeCommand(DocumentCommand.InvalidCommand cmd)
  {
    insertErrorField(
        cmd,
        "Dokumentkommando ist ungültig: " + cmd.getMessage(),
        null);
    return new Integer(1);
  }

  /**
   * Diese Methode fügt einen Spaltenwert aus dem aktuellen Datensatz ein. Im
   * Fehlerfall wird die Fehlermeldung eingefügt.
   */
  public Object executeCommand(DocumentCommand.InsertValue cmd)
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
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, "", e);
      cmd.setErrorState(true);
      return new Integer(1);
    }
    cmd.setDoneState(true);
    return new Integer(0);
  }

  /**
   * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
   * bookmarkName ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
   * 
   * @param frag_id
   *          FRAG_ID, des im Abschnitt Textfragmente in der Konfigurationsdatei
   *          definierten Textfragments.
   * @param bookmarkName
   *          Name des bookmarks, in das das Fragment eingefügt werden soll.
   */
  public Object executeCommand(DocumentCommand.InsertFrag cmd)
  {
    cmd.setErrorState(false);
    try
    {
      // Fragment-URL holen und aufbereiten. Kontext ist der DEFAULT_CONTEXT.
      String urlStr = mux.getTextFragmentList().getURLByID(cmd.getFragID());
      URL url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
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

      Logger.debug("Füge Textfragment \""
                   + cmd.getFragID()
                   + "\" von URL \""
                   + urlStr
                   + "\" ein.");

      // Workaround für den insertDocumentFromURL-Fehler (Einfrieren von OOo
      // wenn Ressource nicht auflösbar).
      if (url.openConnection().getContentLength() <= 0)
      {
        throw new IOException("Fragment "
                              + cmd.getFragID()
                              + " ("
                              + url.toExternalForm()
                              + ") ist leer oder nicht verfügbar");
      }

      // Textfragment einfügen:
      UnoService insCursor = new UnoService(cmd.createInsertCursor());
      insCursor.xDocumentInsertable().insertDocumentFromURL(
          urlStr,
          new PropertyValue[] {});
    }
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, "", e);
      cmd.setErrorState(true);
      return new Integer(1);
    }
    cmd.setDoneState(true);
    return new Integer(0);
  }

  /**
   * Diese Methode fügt das nächste Textfragment aus der dem
   * WMCommandInterpreter übergebenen frag_urls liste ein. Im Fehlerfall wird
   * die Fehlermeldung eingefügt.
   * 
   * @param frag_id
   *          FRAG_ID, des im Abschnitt Textfragmente in der Konfigurationsdatei
   *          definierten Textfragments.
   * @param bookmarkName
   *          Name des bookmarks, in das das Fragment eingefügt werden soll.
   */
  public Object executeCommand(DocumentCommand.InsertContent cmd)
  {
    cmd.setErrorState(false);
    if (fragUrls.length > fragUrlsCount)
    {
      String urlStr = fragUrls[fragUrlsCount++];

      try
      {
        Logger.debug("Füge Textfragment von URL \"" + urlStr + "\" ein.");

        // Textfragment einfügen:
        UnoService insCursor = new UnoService(cmd.createInsertCursor());
        insCursor.xDocumentInsertable().insertDocumentFromURL(
            urlStr,
            new PropertyValue[] {});
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, "", e);
        cmd.setErrorState(true);
        return new Integer(1);
      }
    }
    cmd.setDoneState(true);
    return new Integer(0);
  }

  /**
   * Hinter einem Form-Kommando verbirgt sich eine Notiz, die das Formular
   * beschreibt, das in der FormularGUI angezeigt werden soll. Das Kommando
   * executeForm sammelt alle solchen Formularbeschreibungen im formDescriptor.
   * Enthält der formDescriptor mehr als einen Eintrag, wird nach dem
   * interpret-Vorgang die FormGUI gestartet.
   * 
   * @param bookmarkName
   * @param state
   * @return
   */
  public Object executeCommand(DocumentCommand.Form cmd)
  {
    cmd.setErrorState(false);
    try
    {
      XTextRange range = cmd.getTextRange();
      UnoService cursor = new UnoService(range.getText()
          .createTextCursorByRange(range));
      UnoService textfield = cursor.getPropertyValue("TextField");
      Object content = textfield.getPropertyValue("Content").getObject();
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
      insertErrorField(cmd, "", e);
      cmd.setErrorState(true);
      return new Integer(1);
    }
    cmd.setDoneState(true);
    return new Integer(0);
  }

  /**
   * Diese Methode fügt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, String text,
      java.lang.Exception e)
  {
    String msg = "Fehler in Dokumentkommando \""
                 + cmd.getBookmarkName()
                 + "\": ";

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
      Logger.error(msg + text, e);
    else
      Logger.error(msg + text);

    // Meldung für die mehrzeilige Ausgabe aufbereiten
    text = msg + "\n\n" + text;

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

    // msg += Stacktrace von e
    if (e != null)
    {
      if (e.getMessage() != null)
        text += e.getMessage() + "\n\n" + e.getClass().getName() + ":\n";
      else
        text += e.toString() + "\n\n";
      StackTraceElement[] element = e.getStackTrace();
      for (int i = 0; i < element.length; i++)
      {
        text += element[i].toString() + "\n";
      }
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
      note.setPropertyValue("Content", text);
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
