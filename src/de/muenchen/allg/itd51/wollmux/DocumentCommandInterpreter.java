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

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.DocumentCommandTree.TreeExecutor;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.FormController;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values.SimpleMap;

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
   * Der geparste Dokumentkommando-Baum
   */
  private DocumentCommandTree tree;

  /**
   * Das Feld beschreibt, ob es sich bei dem Dokument um ein Formular handelt.
   * Das Feld kann erst verwendet werden, nachdem die Methode
   * executeTemplateCommands durchlaufen wurde.
   */
  private boolean isAFormular;

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
  public DocumentCommandInterpreter(XTextDocument xDoc, WollMuxSingleton mux)
  {
    this.document = new UnoService(xDoc);
    this.mux = mux;
    this.tree = new DocumentCommandTree(document.xBookmarksSupplier());
    this.isAFormular = false;
  }

  /**
   * Über diese Methode wird die Ausführung der Kommandos gestartet, die für das
   * Expandieren und Befüllen von Dokumenten notwendig sind.
   * 
   * @throws WMCommandsFailedException
   */
  public void executeTemplateCommands(String[] fragUrls)
      throws WMCommandsFailedException
  {
    Logger.debug("executeTemplateCommands");

    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    // 1) Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
    // können, damit der DocumentCommandTree vollständig aufgebaut werden
    // kann.
    errors += new DocumentExpander(fragUrls).execute(tree);

    // 2) Jetzt können die TextFelder innerhalb der updateFields Kommandos
    // geupdatet werden. Durch die Auslagerung in einen extra Schritt wird die
    // Reihenfolge der Abarbeitung klar definiert (zuerst die updateFields
    // Kommandos, dann die anderen Kommandos). Dies ist wichtig, da
    // insbesondere das updateFields Kommando exakt mit einem anderen Kommando
    // übereinander liegen kann. Ausserdem liegt updateFields thematisch näher
    // am expandieren der Textfragmente, da updateFields im Prinzip nur dessen
    // Schwäche beseitigt.
    errors += new TextFieldUpdater().execute(tree);

    // 3) Hauptverarbeitung: Jetzt alle noch übrigen DocumentCommands (z.B.
    // insertValues) in einem einzigen Durchlauf mit execute bearbeiten.
    errors += new MainProcessor().execute(tree);

    // 4) Da keine neuen Elemente mehr eingefügt werden müssen, können
    // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
    // InsertContent-Kommandos gelöscht werden.
    errors += cleanInsertMarks(tree);

    // 5) Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
    // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
    // sauber erkennen und entfernen.
    errors += new EmptyParagraphCleaner().execute(tree);

    // 6) Die Statusänderungen der Dokumentkommandos auf die Bookmarks
    // übertragen bzw. die Bookmarks abgearbeiteter Kommandos löschen.
    tree.updateBookmarks(mux.isDebugMode());

    // 7) Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    setDocumentModified(false);

    // ggf. eine WMCommandsFailedException werfen:
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
          "Die verwendete Vorlage enthält "
              + ((errors == 1) ? "einen" : "" + errors)
              + " Fehler.\n\n"
              + "Bitte kontaktieren Sie Ihre Systemadministration.");
    }
  }

  /**
   * Die Methode gibt Auskunft darüber, ob das Dokument ein Formular ist (also
   * mindestens ein WM(CMD'Form') Kommando enthält) und darf erst verwendet
   * werden, nachdem die Methode executeTemplateCommands durchlaufen wurde.
   * 
   * @return true, wenn sich während des executeTemplateCommands herausgestellt
   *         hat, dass das Dokument mindestens ein WM(CMD 'Form')-Kommando
   *         enthält. Ansonsten false
   */
  public boolean isFormular()
  {
    return this.isAFormular;
  }

  /**
   * Diese Methode führt alle Kommandos aus, im Zusammenhang mit der
   * Formularbearbeitung ausgeführt werden müssen.
   * 
   * @throws WMCommandsFailedException
   */
  /**
   * @throws WMCommandsFailedException
   */
  public void executeFormCommands() throws WMCommandsFailedException
  {
    Logger.debug("executeFormCommands");
    int errors = 0;

    // 1) Scannen aller für das Formular relevanten Informationen:
    FormScanner fs = new FormScanner();
    errors += fs.execute(tree);

    FormDescriptor fd = fs.getFormDescriptor();
    HashMap idToFormFields = fs.getIDToFormFields();
    HashMap idToPresetValue = mapIDToPresetValue(fd, idToFormFields);

    // 2) Bookmarks updaten
    tree.updateBookmarks(mux.isDebugMode());

    // 3) Jetzt wird ein WM(CMD 'setType' TYPE 'formDocument)-Kommando an den
    // Anfang des Dokuments gesetzt um das Dokument als Formulardokument
    // auszuzeichnen.
    if (document.xTextDocument() != null)
      new Bookmark(DocumentCommand.SETTYPE_formDocument, document
          .xTextDocument(), document.xTextDocument().getText().getStart());

    // 4) Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    setDocumentModified(false);

    // FunctionContext erzeugen und im Formular definierte
    // Funktionen/DialogFunktionen parsen:
    ConfigThingy descs = fd.toConfigThingy();
    Map functionContext = new HashMap();
    DialogLibrary dialogLib = new DialogLibrary();
    FunctionLibrary funcLib = new FunctionLibrary();
    try
    {
      dialogLib = WollMuxFiles.parseFunctionDialogs(descs.get("Formular"), mux
          .getFunctionDialogs(), functionContext);
      funcLib = WollMuxFiles.parseFunctions(
          descs.get("Formular"),
          dialogLib,
          functionContext,
          mux.getGlobalFunctions());
    }
    catch (NodeNotFoundException e)
    {
    }

    // ggf. entsprechende WMCommandsFailedException werfen:
    if (descs.query("Formular").count() == 0)
    {
      throw new WMCommandsFailedException(
          "Die Vorlage bzw. das Formular enthält keine gültige Formularbeschreibung\n\n"
              + "Bitte kontaktieren Sie Ihre Systemadministration.");
    }
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
          "Die verwendete Vorlage enthält "
              + ((errors == 1) ? "einen" : "" + errors)
              + " Fehler.\n\n"
              + "Bitte kontaktieren Sie Ihre Systemadministration.");
    }

    // 5) Formulardialog starten:
    FormModelImpl fm = new FormModelImpl(document.xTextDocument(), funcLib, fd,
        idToFormFields, tree);

    ConfigThingy formFensterConf = new ConfigThingy("");
    try
    {
      formFensterConf = WollMuxFiles.getWollmuxConf().query("Fenster").query(
          "Formular").getLastChild();
    }
    catch (NodeNotFoundException x)
    {
    }
    FormGUI gui = new FormGUI(formFensterConf, descs, fm, idToPresetValue,
        functionContext, funcLib, dialogLib);
    fm.setFormGUI(gui);
  }

  /**
   * Diese Methode bestimmt die Vorbelegung der Formularfelder des Formulars und
   * liefert eine HashMap zurück, die die id eines Formularfeldes auf den
   * bestimmten Wert abbildet. Der Wert ist nur dann klar definiert, wenn alle
   * FormFields zu einer ID unverändert geblieben sind, oder wenn nur
   * untransformierte Felder vorhanden sind, die alle den selben Wert enthalten.
   * Gibt es zu einer ID kein FormField-Objekt, so wird der zuletzt
   * abgespeicherte Wert zu dieser ID aus dem FormDescriptor verwendet.
   * 
   * @param fd
   *          Das FormDescriptor-Objekt, aus dem die zuletzt gesetzten Werte der
   *          Formularfelder ausgelesen werden können.
   * @param idToFormFields
   *          Eine Map, die die vorhandenen IDs auf Vectoren von FormFields
   *          abbildet.
   * @return eine vollständige Zuordnung von Feld IDs zu den aktuellen
   *         Vorbelegungen im Dokument.
   */
  private HashMap mapIDToPresetValue(FormDescriptor fd, HashMap idToFormFields)
  {
    HashMap idToPresetValue = new HashMap();

    // durch alle Werte, die im FormDescriptor abgelegt sind gehen, und
    // vergleichen, ob sie mit den Inhalten der Formularfelder im Dokument
    // übereinstimmen.
    Iterator idIter = fd.getFormFieldIDs().iterator();
    while (idIter.hasNext())
    {
      String id = (String) idIter.next();
      String value;

      Vector fields = (Vector) idToFormFields.get(id);
      if (fields != null && fields.size() > 0)
      {
        boolean allAreUnchanged = true;
        boolean allAreUntransformed = true;
        boolean allUntransformedHaveSameValues = true;

        String refValue = null;

        Iterator j = fields.iterator();
        while (j.hasNext())
        {
          FormField field = (FormField) j.next();
          String thisValue = field.getValue();

          if (field.hasChangedPreviously()) allAreUnchanged = false;

          if (field.hasTrafo())
            allAreUntransformed = false;
          else
          {
            // Referenzwert bestimmen
            if (refValue == null) refValue = thisValue;

            if (thisValue == null || !thisValue.equals(refValue))
              allUntransformedHaveSameValues = false;
          }
        }

        // neuen Formularwert bestimmen. Regeln:
        // 1) Wenn sich kein Formularfeld geändert hat, wird der zuletzt
        // gesetzte Formularwert verwendet.
        // 2) Wenn sich mindestens ein Formularfeld geandert hat, jedoch alle
        // untransformiert sind und den selben Wert enhtalten, so wird dieser
        // gleiche Wert als neuer Formularwert übernommen.
        // 3) in allen anderen Fällen wird FISHY übergeben.
        if (allAreUnchanged)
          value = fd.getFormFieldValue(id);
        else
        {
          if (allAreUntransformed
              && allUntransformedHaveSameValues
              && refValue != null)
            value = refValue;
          else
            value = FormController.FISHY;
        }
      }
      else
      {
        // wenn kein Formularfeld vorhanden ist wird der zuletzt gesetzte
        // Formularwert übernommen.
        value = fd.getFormFieldValue(id);
      }

      // neuen Wert übernehmen:
      idToPresetValue.put(id, value);
      Logger.debug2("Add IDToPresetValue: ID=\""
                    + id
                    + "\" --> Wert=\""
                    + value
                    + "\"");

    }
    return idToPresetValue;
  }

  /**
   * Diese Klasse implementiert das FormModel-Interface und sorgt als Wrapper im
   * Wesentlichen nur dafür, dass alle Methodenaufrufe des FormModels in die
   * ensprechenden WollMuxEvents verpackt werden.
   */
  private static class FormModelImpl implements FormModel, XCloseListener
  {
    private final XTextDocument doc;

    private final FunctionLibrary funcLib;

    private final HashMap idToFormValues;

    private final DocumentCommandTree cmdTree;

    private final HashSet invisibleGroups;

    private final String defaultWindowAttributes;

    private final FormDescriptor formDescriptor;

    private FormGUI formGUI;

    public FormModelImpl(XTextDocument doc, FunctionLibrary funcLib,
        FormDescriptor formDescriptor, HashMap idToFormValues,
        DocumentCommandTree cmdTree)
    {
      this.doc = doc;
      this.funcLib = funcLib;
      this.idToFormValues = idToFormValues;
      this.cmdTree = cmdTree;
      this.invisibleGroups = new HashSet();
      this.formGUI = null;
      this.formDescriptor = formDescriptor;

      // Standard-Fensterattribute vor dem Start der Form-GUI sichern um nach
      // dem Schließen des Formulardokuments die Standard-Werte wieder
      // herstellen zu können. Die Standard-Attribute ändern sich (OOo-seitig)
      // immer dann, wenn ein Dokument (mitsamt Fenster) geschlossen wird. Dann
      // merkt sich OOo die Position und Größe des zuletzt geschlossenen
      // Fensters.
      this.defaultWindowAttributes = getDefaultWindowAttributes();

      // closeListener registrieren
      if (UNO.XCloseable(doc) != null)
      {
        UNO.XCloseable(doc).addCloseListener(this);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#close()
     */
    public void close()
    {
      WollMuxEventHandler.handleCloseTextDocument(doc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#setWindowVisible(boolean)
     */
    public void setWindowVisible(boolean vis)
    {
      WollMuxEventHandler.handleSetWindowVisible(UNO.XModel(doc), vis);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#setWindowPosSize(int, int,
     *      int, int)
     */
    public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
    {
      WollMuxEventHandler.handleSetWindowPosSize(
          UNO.XModel(doc),
          docX,
          docY,
          docWidth,
          docHeight);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#setDocumentZoom(java.lang.String)
     */
    public void setDocumentZoom(String zoomValue)
    {
      WollMuxEventHandler.handleSetDocumentZoom(UNO.XModel(doc), zoomValue);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#setVisibleState(java.lang.String,
     *      boolean)
     */
    public void setVisibleState(String groupId, boolean visible)
    {
      WollMuxEventHandler.handleSetVisibleState(
          cmdTree,
          invisibleGroups,
          groupId,
          visible);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#valueChanged(java.lang.String,
     *      java.lang.String)
     */
    public void valueChanged(String fieldId, String newValue)
    {
      WollMuxEventHandler.handleFormValueChanged(
          formDescriptor,
          idToFormValues,
          fieldId,
          newValue,
          funcLib);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#focusGained(java.lang.String)
     */
    public void focusGained(String fieldId)
    {
      WollMuxEventHandler.handleFocusFormField(idToFormValues, fieldId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#focusLost(java.lang.String)
     */
    public void focusLost(String fieldId)
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.util.XCloseListener#queryClosing(com.sun.star.lang.EventObject,
     *      boolean)
     */
    public void queryClosing(EventObject event, boolean getsOwnership)
        throws CloseVetoException
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.util.XCloseListener#notifyClosing(com.sun.star.lang.EventObject)
     */
    public void notifyClosing(EventObject arg0)
    {
      WollMuxEventHandler.handleDisposeFormModel(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
     */
    public void disposing(EventObject arg0)
    {
      WollMuxEventHandler.handleDisposeFormModel(this);
    }

    /**
     * Diese Methode setzt die Fensterattribute wieder auf den Stand vor dem
     * Starten der FormGUI und teilt der FormGUI mit, dass es (das FormModel)
     * geschlossen wurde und in Zukunft nicht mehr angesprochen werden darf.
     */
    public void dispose()
    {
      if (formGUI != null)
      {
        formGUI.dispose();
        formGUI = null;
      }

      // Rücksetzen des defaultWindowAttributes auf den Wert vor dem Schließen
      // des Formulardokuments.
      if (defaultWindowAttributes != null)
        setDefaultWindowAttributes(defaultWindowAttributes);
    }

    public void setFormGUI(FormGUI formGUI)
    {
      this.formGUI = formGUI;
    }

    /**
     * Diese Hilfsmethode liest das Attribut ooSetupFactoryWindowAttributes aus
     * dem Konfigurationsknoten
     * "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument"
     * der OOo-Konfiguration, welches die Standard-FensterAttribute enthält, mit
     * denen neue Fenster für TextDokumente erzeugt werden.
     * 
     * @return
     */
    private static String getDefaultWindowAttributes()
    {
      try
      {
        Object cp = UNO
            .createUNOService("com.sun.star.configuration.ConfigurationProvider");

        // creation arguments: nodepath
        com.sun.star.beans.PropertyValue aPathArgument = new com.sun.star.beans.PropertyValue();
        aPathArgument.Name = "nodepath";
        aPathArgument.Value = "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument";
        Object[] aArguments = new Object[1];
        aArguments[0] = aPathArgument;

        Object ca = UNO.XMultiServiceFactory(cp).createInstanceWithArguments(
            "com.sun.star.configuration.ConfigurationAccess",
            aArguments);

        return UNO.getProperty(ca, "ooSetupFactoryWindowAttributes").toString();
      }
      catch (java.lang.Exception e)
      {
      }
      return null;
    }

    /**
     * Diese Hilfsmethode setzt das Attribut ooSetupFactoryWindowAttributes aus
     * dem Konfigurationsknoten
     * "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument"
     * der OOo-Konfiguration auf den neuen Wert value, der (am besten) über
     * einen vorhergehenden Aufruf von getDefaultWindowAttributes() gewonnen
     * wird.
     * 
     * @param value
     */
    private static void setDefaultWindowAttributes(String value)
    {
      try
      {
        Object cp = UNO
            .createUNOService("com.sun.star.configuration.ConfigurationProvider");

        // creation arguments: nodepath
        com.sun.star.beans.PropertyValue aPathArgument = new com.sun.star.beans.PropertyValue();
        aPathArgument.Name = "nodepath";
        aPathArgument.Value = "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument";
        Object[] aArguments = new Object[1];
        aArguments[0] = aPathArgument;

        Object ca = UNO.XMultiServiceFactory(cp).createInstanceWithArguments(
            "com.sun.star.configuration.ConfigurationUpdateAccess",
            aArguments);

        UNO.setProperty(ca, "ooSetupFactoryWindowAttributes", value);

        UNO.XChangesBatch(ca).commitChanges();
      }
      catch (java.lang.Exception e)
      {
      }
    }
  }

  /**
   * Der EmptyParagraphCleaner löscht leere Absätze aus allen zuvor eingefügten
   * Textfragmenten heraus.
   */
  private class EmptyParagraphCleaner extends TreeExecutor
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

      int errors = 0;
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        errors += cmd.execute(this);
      }

      setLockControllers(false);

      return errors;
    }

    public int executeCommand(InsertFrag cmd)
    {
      cleanEmptyParagraphsForCommand(cmd);
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      cleanEmptyParagraphsForCommand(cmd);
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
   * Der DocumentExpander sorgt dafür, dass das Dokument nach Ausführung der
   * enthaltenen Kommandos komplett aufgebaut ist und alle Textfragmente
   * eingefügt wurden.
   * 
   * @author christoph.lutz
   * 
   */
  private class DocumentExpander extends TreeExecutor
  {
    private String[] fragUrls;

    private int fragUrlsCount = 0;

    /**
     * Erzeugt einen neuen DocumentExpander, mit der Liste fragUrls, die die
     * URLs beschreibt, von denen die Textfragmente für den insertContent Befehl
     * bezogen werden sollen.
     * 
     * @param fragUrls
     */
    public DocumentExpander(String[] fragUrls)
    {
      this.fragUrls = fragUrls;
      this.fragUrlsCount = 0;
    }

    public int execute(DocumentCommandTree tree)
    {
      return executeDepthFirst(tree, false);
    }

    /**
     * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
     * bookmarkName ein. Im Fehlerfall wird eine entsprechende Fehlermeldung
     * eingefügt.
     */
    public int executeCommand(InsertFrag cmd)
    {
      repeatScan = true;
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

    /**
     * Diese Methode fügt das nächste Textfragment aus der dem
     * WMCommandInterpreter übergebenen frag_urls liste ein. Im Fehlerfall wird
     * eine entsprechende Fehlermeldung eingefügt.
     */
    public int executeCommand(InsertContent cmd)
    {
      repeatScan = true;
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
     * @throws java.io.IOException
     * @throws IOException
     */
    private void insertDocumentFromURL(DocumentCommand cmd, URL url)
        throws IllegalArgumentException, java.io.IOException, IOException
    {

      // Workaround: OOo friert ein, wenn ressource bei insertDocumentFromURL
      // nicht auflösbar. http://qa.openoffice.org/issues/show_bug.cgi?id=57049
      // Hier wird versucht, die URL über den java-Klasse url aufzulösen und bei
      // Fehlern abgebrochen.
      WollMuxSingleton.checkURL(url);

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
  private class MainProcessor extends TreeExecutor
  {
    /**
     * Hauptverarbeitungsschritt starten.
     */
    private int execute(DocumentCommandTree tree)
    {
      setLockControllers(true);

      int errors = executeDepthFirst(tree, false);

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

      String spaltenname = cmd.getDBSpalte();
      String value = null;
      try
      {
        Dataset ds = mux.getDatasourceJoiner().getSelectedDataset();
        value = ds.get(spaltenname);

        // ggf. TRAFO durchführen
        value = getOptionalTrafoValue(value, cmd, mux.getGlobalFunctions());
      }
      catch (DatasetNotFoundException e)
      {
        value = "<FEHLER: Kein Absender ausgewählt!>";
      }
      catch (ColumnNotFoundException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }

      XTextCursor insCursor = cmd.createInsertCursor();
      if (insCursor != null)
      {
        if (value == null || value.equals(""))
        {
          insCursor.setString("");
        }
        else
        {
          insCursor.setString(cmd.getLeftSeparator()
                              + value
                              + cmd.getRightSeparator());
        }
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

    /**
     * Diese Methode setzt nur isFormular auf true. Die Eigentliche Bearbeitung
     * des Formulars erfolgt dann in executeFormCommands().
     */
    public int executeCommand(Form cmd)
    {
      isAFormular = true;
      return super.executeCommand(cmd);
    }
  }

  /**
   * Dieser Executor hat die Aufgabe alle updateFields-Befehle zu verarbeiten.
   */
  private class TextFieldUpdater extends TreeExecutor
  {
    /**
     * Ausführung starten
     */
    private int execute(DocumentCommandTree tree)
    {
      setLockControllers(true);

      int errors = executeDepthFirst(tree, false);

      setLockControllers(false);

      return errors;
    }

    /**
     * Diese Methode updated alle TextFields, die das Kommando cmd umschließt.
     */
    public int executeCommand(UpdateFields cmd)
    {
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
     * Diese Methode durchsucht das Element element bzw. dessen
     * XEnumerationAccess Interface rekursiv nach TextFeldern und ruft deren
     * Methode update() auf.
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

  /**
   * Der FormScanner erstellt alle Datenstrukturen, die für die Ausführung der
   * FormGUI notwendig sind.
   */
  private class FormScanner extends TreeExecutor
  {
    private HashMap idToFormFields = new HashMap();

    private FormDescriptor formDescriptor = new FormDescriptor();

    private FormDescriptor getFormDescriptor()
    {
      return formDescriptor;
    }

    private HashMap getIDToFormFields()
    {
      return idToFormFields;
    }

    /**
     * Ausführung starten
     */
    private int execute(DocumentCommandTree tree)
    {
      return executeDepthFirst(tree, false);
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
        formDescriptor.add(cmd);
      }
      catch (ConfigurationErrorException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.DocumentCommand.Executor#executeCommand(de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue)
     */
    public int executeCommand(InsertFormValue cmd)
    {
      // idToFormFields aufbauen
      String id = cmd.getID();
      Vector fields;
      if (idToFormFields.containsKey(id))
      {
        fields = (Vector) idToFormFields.get(id);
      }
      else
      {
        fields = new Vector();
        idToFormFields.put(id, fields);
      }
      FormField field = FormFieldFactory.createFormField(document
          .xTextDocument(), cmd);
      if (field != null) fields.add(field);

      return 0;
    }

    /**
     * Da der DocumentTree zu diesem Zeitpunkt eigentlich gar kein
     * SetType-Kommando mehr beinhalten darf, wird jedes evtl. noch vorhandene
     * setType-Kommando auf DONE=true gesetzt, damit es beim updateBookmarks
     * entfernt wird.
     */
    public int executeCommand(SetType cmd)
    {
      cmd.setDoneState(true);
      return 0;
    }
  }

  // Übergreifende Helper-Methoden:

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   * 
   * @param state
   */
  private void setDocumentModified(boolean state)
  {
    try
    {
      if (document.xModifiable() != null)
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

  /**
   * Diese Methode berechnet die Transformation des Wertes value, wenn in dem
   * Dokumentkommando cmd ein TRAFO-Attribut gesetzt ist, wobei die
   * Transformationsfunktion der Funktionsbibliothek funcLib verwendet wird. Ist
   * kein TRAFO-Attribut definiert, so wird der Eingebewert value unverändert
   * zurückgeliefert. Ist das TRAFO-Attribut zwar definiert, die
   * Transformationsionfunktion jedoch nicht in der Funktionsbibliothek funcLib
   * enthalten, so wird eine Fehlermeldung zurückgeliefert und eine weitere
   * Fehlermeldung in die Log-Datei geschrieben.
   * 
   * @param value
   *          Der zu transformierende Wert.
   * @param cmd
   *          Das Dokumentkommando, welches das optionale TRAFO Attribut
   *          besitzt.
   * @param funcLib
   *          Die Funktionsbibliothek, in der nach der Transformatiosfunktion
   *          gesucht werden soll.
   * @return Der Transformierte Wert falls das TRAFO-Attribut gesetzt ist und
   *         die Trafo korrekt definiert ist. Ist kein TRAFO-Attribut gesetzt,
   *         so wird value unverändert zurückgeliefert. Ist die TRAFO-Funktion
   *         nicht definiert, wird eine Fehlermeldung zurückgeliefert.
   */
  public static String getOptionalTrafoValue(String value,
      DocumentCommand.OptionalTrafoProvider cmd, FunctionLibrary funcLib)
  {
    String transformed = value;
    if (cmd.getTrafoName() != null)
    {
      Function func = funcLib.get(cmd.getTrafoName());
      if (func != null)
      {
        SimpleMap args = new SimpleMap();
        String[] pars = func.parameters();
        if (pars.length >= 1)
        {
          args.put(pars[0], value);
        }
        transformed = func.getString(args);
      }
      else
      {
        transformed = "<FEHLER: TRAFO '"
                      + cmd.getTrafoName()
                      + "' nicht definiert>";
        Logger.error("Die in Kommando '"
                     + cmd
                     + " verwendete TRAFO '"
                     + cmd.getTrafoName()
                     + "' ist nicht definiert.");
      }
    }

    return transformed;
  }
}
