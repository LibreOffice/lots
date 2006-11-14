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
 * 22.08.2006 | BNK | cleanInsertMarks() und EmptyParagraphCleaner verschmolzen zu
 *                  | SurroundingGarbageCollector und dabei komplettes Rewrite.
 * 23.08.2006 | BNK | nochmal Rewrite. Ich glaube dieser Code hält den Rekord im WollMux
 *                  | was rewrites angeht.
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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.AllVersions;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.DraftOnly;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
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

  private TextDocumentModel model;

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  private WollMuxSingleton mux;

  /**
   * Der geparste Dokumentkommando-Baum
   */
  private DocumentCommandTree tree;

  /**
   * Der FormScanner wird sowohl bei Vorlagen als auch bei Formulardokumenten
   * gestartet. Damit der FormScanner bei Formularvorlagen nicht zweimal
   * aufgerufen wird, wird ein einmal bearbeiteter FormScanner hier abgelgt.
   */
  private FormScanner formScanner;

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
  public DocumentCommandInterpreter(TextDocumentModel model,
      WollMuxSingleton mux)
  {
    this.model = model;
    this.mux = mux;
    this.tree = model.getDocumentCommandTree();
    this.formScanner = null;
  }

  /**
   * Diese Methode sollte vor executeTemplateCommands und vor
   * executeFormCommands aufgerufen werden und sorgt dafür, dass alle globalen
   * Einstellungen des Dokuments an das TextDocumentModel weitergereicht werden.
   */
  public void scanDocumentSettings()
  {
    Logger.debug("scanDocumentSettings");
    new DocumentSettingsScanner().execute(tree);
    model.setDocumentModified(false);
  }

  /**
   * Über diese Methode wird die Ausführung der Kommandos gestartet, die für das
   * Expandieren und Befüllen von Dokumenten notwendig sind.
   * 
   * @throws WMCommandsFailedException
   */
  public void executeTemplateCommands() throws WMCommandsFailedException
  {
    Logger.debug("executeTemplateCommands");

    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    // 1) Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
    // können, damit der DocumentCommandTree vollständig aufgebaut werden
    // kann.
    errors += new DocumentExpander(model.getFragUrls()).execute(tree);

    // Ziffern-Anpassen der Sachleitenden Verfügungen aufrufen:
    SachleitendeVerfuegung.ziffernAnpassen(model.doc);

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

    SurroundingGarbageCollector collect = new SurroundingGarbageCollector();
    errors += collect.execute(tree);
    collect.removeGarbage();

    // 4) Da keine neuen Elemente mehr eingefügt werden müssen, können
    // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
    // InsertContent-Kommandos gelöscht werden.
    // errors += cleanInsertMarks(tree);

    // 5) Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
    // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
    // sauber erkennen und entfernen.
    // errors += new EmptyParagraphCleaner().execute(tree);

    // 6) Scannen aller für das Formular relevanten Informationen:
    if (formScanner == null) formScanner = new FormScanner();
    errors += formScanner.execute(tree);
    model.setIDToFormFields(formScanner.idToFormFields);

    // 7) Die Statusänderungen der Dokumentkommandos auf die Bookmarks
    // übertragen bzw. die Bookmarks abgearbeiteter Kommandos löschen.
    tree.updateBookmarks(mux.isDebugMode());

    // 8) Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    model.setDocumentModified(false);

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
    if (formScanner == null)
    {
      formScanner = new FormScanner();
      errors += formScanner.execute(tree);
      model.setIDToFormFields(formScanner.idToFormFields);
    }
    HashMap idToPresetValue = mapIDToPresetValue(
        model,
        formScanner.idToFormFields);

    // 2) Bookmarks updaten
    tree.updateBookmarks(mux.isDebugMode());

    // 3) Jetzt wird der Dokumenttyp formDocument gesetzt, um das Dokument als
    // Formulardokument auszuzeichnen.
    model.setType("formDocument");

    // 4) Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    model.setDocumentModified(false);

    // FunctionContext erzeugen und im Formular definierte
    // Funktionen/DialogFunktionen parsen:
    ConfigThingy descs = model.getFormDescription();
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
    FormModelImpl fm = new FormModelImpl(model, funcLib,
        formScanner.idToFormFields, tree);

    model.setFormModel(fm);

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
  private static HashMap mapIDToPresetValue(TextDocumentModel model,
      HashMap idToFormFields)
  {
    HashMap idToPresetValue = new HashMap();

    // durch alle Werte, die im FormDescriptor abgelegt sind gehen, und
    // vergleichen, ob sie mit den Inhalten der Formularfelder im Dokument
    // übereinstimmen.
    Iterator idIter = model.getFormFieldIDs().iterator();
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
          value = model.getFormFieldValue(id);
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
        value = model.getFormFieldValue(id);
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
  private static class FormModelImpl implements FormModel
  {
    private final TextDocumentModel textDocumentModel;

    private final FunctionLibrary funcLib;

    private final HashMap idToFormValues;

    private final DocumentCommandTree cmdTree;

    private final HashSet invisibleGroups;

    private final String defaultWindowAttributes;

    private FormGUI formGUI;

    public FormModelImpl(TextDocumentModel textDocumentModel,
        FunctionLibrary funcLib, HashMap idToFormValues,
        DocumentCommandTree cmdTree)
    {
      this.textDocumentModel = textDocumentModel;
      this.funcLib = funcLib;
      this.idToFormValues = idToFormValues;
      this.cmdTree = cmdTree;
      this.invisibleGroups = new HashSet();
      this.formGUI = null;

      // Standard-Fensterattribute vor dem Start der Form-GUI sichern um nach
      // dem Schließen des Formulardokuments die Standard-Werte wieder
      // herstellen zu können. Die Standard-Attribute ändern sich (OOo-seitig)
      // immer dann, wenn ein Dokument (mitsamt Fenster) geschlossen wird. Dann
      // merkt sich OOo die Position und Größe des zuletzt geschlossenen
      // Fensters.
      this.defaultWindowAttributes = getDefaultWindowAttributes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#close()
     */
    public void close()
    {
      WollMuxEventHandler.handleCloseTextDocument(textDocumentModel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormModel#setWindowVisible(boolean)
     */
    public void setWindowVisible(boolean vis)
    {
      WollMuxEventHandler.handleSetWindowVisible(textDocumentModel, vis);
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
          textDocumentModel,
          docX,
          docY,
          docWidth,
          docHeight);
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
          textDocumentModel.doc,
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
          textDocumentModel,
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

    public void print()
    {
      UNO.dispatch(textDocumentModel.doc, DispatchInterceptor.DISP_UNO_PRINT);
    }

    public void pdf()
    {
      // TODO WollMuxEventHandler.handleExportAsPDF(textDocumentModel);
    }
  }

  /**
   * Hierbei handelt es sich um einen minimalen Scanner, der zu aller erst
   * abläuft und die globalen Einstellungen des Dokuments (setType,
   * setPrintFunction) ausliest und dem TextDocumentModel zur Verfügung stellt.
   * 
   * @author christoph.lutz
   */
  private class DocumentSettingsScanner extends TreeExecutor
  {

    public int execute(DocumentCommandTree tree)
    {
      return executeDepthFirst(tree, false);
    }

    public int executeCommand(SetPrintFunction cmd)
    {
      model.setPrintFunction(cmd);

      // Bookmark löschen:
      cmd.setDoneState(true);
      cmd.updateBookmark(false);
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      model.setType(cmd);

      // Wenn eine Mischvorlage zum Bearbeiten geöffnet wurde soll der typ
      // "templateTemplate" NICHT gelöscht werden, ansonsten schon.
      if (!(model.hasURL() && cmd.getType()
          .equalsIgnoreCase("templateTemplate")))
      {
        // Bookmark löschen:
        cmd.setDoneState(true);
        cmd.updateBookmark(false);
      }
      return 0;
    }

    public int executeCommand(DraftOnly cmd)
    {
      model.addDraftOnlyBlock(cmd);
      return 0;
    }

    public int executeCommand(NotInOriginal cmd)
    {
      model.addNotInOriginalBlock(cmd);
      return 0;
    }

    public int executeCommand(AllVersions cmd)
    {
      model.addAllVersionsBlock(cmd);
      return 0;
    }

  }

  /**
   * Der SurroundingGarbageCollector erfasst leere Absätze und Einfügemarker um
   * Dokumentkommandos herum.
   */
  private class SurroundingGarbageCollector extends TreeExecutor
  {
    /**
     * Speichert Muellmann-Objekte, die zu löschenden Müll entfernen.
     */
    private List muellmaenner = new Vector();

    private abstract class Muellmann
    {
      protected XTextRange range;

      public Muellmann(XTextRange range)
      {
        this.range = range;
      }

      public abstract void tueDeinePflicht();
    }

    private class RangeMuellmann extends Muellmann
    {
      public RangeMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        Bookmark.removeTextFromInside(model.doc, range);
      }
    }

    private class ParagraphMuellmann extends Muellmann
    {
      public ParagraphMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        deleteParagraph(range);
      }
    }

    /**
     * Diese Methode erfasst leere Absätze und Einfügemarker, die sich um die im
     * Kommandobaum tree enthaltenen Dokumentkommandos befinden.
     */
    private int execute(DocumentCommandTree tree)
    {
      int errors = 0;
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        errors += cmd.execute(this);
      }

      return errors;
    }

    /**
     * Löscht die vorher als Müll identifizierten Inhalte. type filter text
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void removeGarbage()
    {
      model.setLockControllers(true);
      Iterator iter = muellmaenner.iterator();
      while (iter.hasNext())
      {
        Muellmann muellmann = (Muellmann) iter.next();
        muellmann.tueDeinePflicht();
      }
      model.setLockControllers(false);
    }

    public int executeCommand(InsertFrag cmd)
    {
      collectSurroundingGarbageForCommand(cmd);
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      collectSurroundingGarbageForCommand(cmd);
      return 0;
    }

    // Helper-Methoden:

    /**
     * Diese Methode erfasst Einfügemarken und leere Absätze zum Beginn und zum
     * Ende des übergebenen Dokumentkommandos cmd.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void collectSurroundingGarbageForCommand(DocumentCommand cmd)
    {
      /*
       * Im folgenden steht eine 0 in der ersten Stelle dafür, dass vor dem
       * Einfügemarker kein Text mehr steht (der Marker also am Anfang des
       * Absatzes ist). Eine 0 an der zweiten Stelle steht dafür, dass hinter
       * dem Einfügemarker kein Text mehr folgt (der Einfügemarker also am Ende
       * des Absatzes steht).
       * 
       * Startmarke: Grundsätzlich gibt es die folgenden Fälle zu unterscheiden.
       * Ein "T" an dritter Stelle gibt an, dass hinter dem Absatz des
       * Einfügemarkers eine Tabelle folgt.
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
       * 01, 10, 11: Einfügemarker löschen
       * 
       * 
       */
      XParagraphCursor[] cursor = cmd.getStartMark();
      if (cursor == null) return;

      if (cursor[0].isStartOfParagraph() && cursor[1].isEndOfParagraph())
      {
        muellmaenner.add(new ParagraphMuellmann(cursor[1]));
      }
      else
      // if start mark is not the only text in the paragraph
      {
        cursor[1].goLeft((short) 1, true);
        muellmaenner.add(new RangeMuellmann(cursor[1]));
      }

      cursor = cmd.getEndMark();
      if (cursor[0].isStartOfParagraph() && cursor[1].isEndOfParagraph())
      {
        muellmaenner.add(new ParagraphMuellmann(cursor[1]));
      }
      else
      {
        cursor[0].goRight(cmd.getEndMarkLength(), true);
        muellmaenner.add(new RangeMuellmann(cursor[0]));
      }
    }

    /**
     * Löscht den ganzen ersten Absatz an der Cursorposition textCursor.
     * 
     * @param range
     */
    private void deleteParagraph(XTextRange range)
    {
      // Beim Löschen des Absatzes erzeugt OOo ein ungewolltes
      // "Zombie"-Bookmark.
      // Issue Siehe http://qa.openoffice.org/issues/show_bug.cgi?id=65247

      XTextContent paragraph = null;

      // Ersten Absatz des Bookmarks holen:
      XEnumerationAccess access = UNO.XEnumerationAccess(range);
      if (access != null)
      {
        XEnumeration xenum = access.createEnumeration();
        if (xenum.hasMoreElements()) try
        {
          paragraph = UNO.XTextContent(xenum.nextElement());
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }

      // Lösche den Paragraph
      if (paragraph != null) try
      {
        range.getText().removeTextContent(paragraph);
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
    model.setLockControllers(true);

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

    model.setLockControllers(false);
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
     * Bugs des insertDocumentFromURL der UNO-API. public int
     * execute(DocumentCommandTree tree) { return executeDepthFirst(tree,
     * false); }
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
      String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

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
      if (UNO.XTextFramesSupplier(model.doc) != null)
      {
        String[] names = UNO.XTextFramesSupplier(model.doc).getTextFrames()
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
      model.setLockControllers(true);

      int errors = executeDepthFirst(tree, false);

      model.setLockControllers(false);

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
     * Diese Methode fügt den Rückgabewert einer Funktion ein.
     */
    public int executeCommand(DocumentCommand.InsertFunctionValue cmd)
    {
      cmd.setErrorState(false);

      String value;
      FunctionLibrary funcLib = mux.getGlobalFunctions();
      Function func = funcLib.get(cmd.getFunctionName());
      if (func != null)
      {
        SimpleMap args = new SimpleMap();
        String[] pars = func.parameters();
        Iterator iter = cmd.getArgs().iterator();
        for (int i = 0; i < pars.length && iter.hasNext(); ++i)
        {
          String arg = iter.next().toString();
          args.put(pars[i], arg);
        }
        value = func.getString(args);
      }
      else
      {
        value = "<FEHLER: FUNCTION '"
                + cmd.getFunctionName()
                + "' nicht definiert>";
        Logger.error("Die in Kommando '"
                     + cmd
                     + " verwendete FUNCTION '"
                     + cmd.getFunctionName()
                     + "' ist nicht definiert.");
      }

      XTextCursor insCursor = cmd.createInsertCursor();
      if (insCursor != null) insCursor.setString(value);
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
      model.setLockControllers(true);

      int errors = executeDepthFirst(tree, false);

      model.setLockControllers(false);

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

    private Map bookmarkNameToFormField = new HashMap();

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
        model.addFormCommand(cmd);
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
      FormField field = FormFieldFactory.createFormField(
          model.doc,
          cmd,
          bookmarkNameToFormField);

      if (field != null)
      {
        field.setCommand(cmd);
        fields.add(field);
      }

      return 0;
    }
  }

  // Übergreifende Helper-Methoden:

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
      XTextCursor c = range.getText().createTextCursorByRange(range);
      c.goLeft((short) 2, false);
      XTextContent note = UNO.XTextContent(UNO.XMultiServiceFactory(model.doc)
          .createInstance("com.sun.star.text.TextField.Annotation"));
      UNO.setProperty(note, "Content", msg + ":\n\n" + e.getMessage());
      c.getText().insertTextContent(c, note, false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Diese Methode berechnet die Transformation des Wertes value, wenn in dem
   * Dokumentkommando cmd ein TRAFO-Attribut gesetzt ist, wobei die
   * Transformationsfunktion der Funktionsbibliothek funcLib verwendet wird und
   * allen Parametern, die die Funktion erwartet der Wert value übergeben wird.
   * Ist kein TRAFO-Attribut definiert, so wird der Eingebewert value
   * unverändert zurückgeliefert. Ist das TRAFO-Attribut zwar definiert, die
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
        for (int i = 0; i < pars.length; i++)
        {
          args.put(pars[i], value);
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
