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
import java.util.Enumeration;
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
import com.sun.star.container.XNameAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
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
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
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
    // this.tree = model.getDocumentCommandTree();
    this.formScanner = null;
    model.resetDocumentCommands();
  }

  /**
   * Diese Methode sollte vor executeTemplateCommands und vor
   * executeFormCommands aufgerufen werden und sorgt dafür, dass alle globalen
   * Einstellungen des Dokuments an das TextDocumentModel weitergereicht werden.
   */
  public void scanDocumentSettings()
  {
    Logger.debug("scanDocumentSettings");

    new DocumentSettingsScanner().execute(model.getDocumentCommandTree());

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
    errors += new DocumentExpander(model.getFragUrls()).execute(model
        .getDocumentCommandTree());

    // Ziffern-Anpassen der Sachleitenden Verfügungen aufrufen:
    SachleitendeVerfuegung.ziffernAnpassen(model);

    // 2) Jetzt können die TextFelder innerhalb der updateFields Kommandos
    // geupdatet werden. Durch die Auslagerung in einen extra Schritt wird die
    // Reihenfolge der Abarbeitung klar definiert (zuerst die updateFields
    // Kommandos, dann die anderen Kommandos). Dies ist wichtig, da
    // insbesondere das updateFields Kommando exakt mit einem anderen Kommando
    // übereinander liegen kann. Ausserdem liegt updateFields thematisch näher
    // am expandieren der Textfragmente, da updateFields im Prinzip nur dessen
    // Schwäche beseitigt.
    errors += new TextFieldUpdater().execute(model.getDocumentCommandTree());

    // 3) Hauptverarbeitung: Jetzt alle noch übrigen DocumentCommands (z.B.
    // insertValues) in einem einzigen Durchlauf mit execute bearbeiten.
    errors += new MainProcessor().execute(model.getDocumentCommandTree());

    // 4) Da keine neuen Elemente mehr eingefügt werden müssen, können
    // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
    // InsertContent-Kommandos gelöscht werden.
    // errors += cleanInsertMarks(tree);

    // 5) Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
    // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
    // sauber erkennen und entfernen.
    // errors += new EmptyParagraphCleaner().execute(tree);
    SurroundingGarbageCollector collect = new SurroundingGarbageCollector();
    errors += collect.execute(model.getDocumentCommandTree());
    collect.removeGarbage();

    // da hier bookmarks entfernt werden, muss der Baum upgedatet werden
    model.getDocumentCommandTree().update();

    // 6) Scannen aller für das Formular relevanten Informationen:
    if (formScanner == null) formScanner = new FormScanner();
    errors += formScanner.execute(model.getDocumentCommandTree());
    model.setIDToFormFields(formScanner.idToFormFields);

    // Nicht vom formScanner erfasste Formularfelder erfassen
    model.collectNonWollMuxFormFields();

    // Jetzt wird der Dokumenttyp formDocument gesetzt, um das Dokument als
    // Formulardokument auszuzeichnen.
    if (model.hasFormDescriptor()) model.setType("formDocument");

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
      errors += formScanner.execute(model.getDocumentCommandTree());
      model.setIDToFormFields(formScanner.idToFormFields);

      // Nicht vom formScanner erfasste Formularfelder erfassen
      model.collectNonWollMuxFormFields();
    }
    HashMap idToPresetValue = mapIDToPresetValue(
        model,
        formScanner.idToFormFields);

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
    FormModel fm = new FormModelImpl(model, funcLib);

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
    model.setFormGUI(gui);
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

    private final String defaultWindowAttributes;

    public FormModelImpl(TextDocumentModel textDocumentModel,
        FunctionLibrary funcLib)
    {
      this.textDocumentModel = textDocumentModel;
      this.funcLib = funcLib;

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
          textDocumentModel,
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
      if (fieldId.length() > 0)
        WollMuxEventHandler.handleFormValueChanged(
            textDocumentModel,
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
      WollMuxEventHandler.handleFocusFormField(textDocumentModel, fieldId);
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
      FormGUI formGUI = textDocumentModel.getFormGUI();
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
      UNO.dispatch(textDocumentModel.doc, ".uno:ExportToPDF");
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

      cmd.markDone(true);
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      model.setType(cmd);

      // Wenn eine Mischvorlage zum Bearbeiten geöffnet wurde soll der typ
      // "templateTemplate" NICHT gelöscht werden, ansonsten schon.
      if (!(model.hasURL() && cmd.getType()
          .equalsIgnoreCase("templateTemplate"))) cmd.markDone(true);
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

    public int executeCommand(SetJumpMark cmd)
    {
      model.addSetJumpMarkBlock(cmd);
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
      XTextSectionsSupplier suppl;

      public ParagraphMuellmann(XTextRange range, XTextSectionsSupplier suppl)
      {
        super(range);
        this.suppl = suppl;
      }

      public void tueDeinePflicht()
      {
        deleteParagraph(range, suppl);
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
      if (cmd.hasInsertMarks())
      {
        // ist der ManualMode gesetzt, so darf ein leerer Paragraph am Ende des
        // Dokuments nicht gelöscht werden, da sonst der ViewCursor auf den
        // Start des Textbereiches zurück gesetzt wird. Im Falle der
        // automatischen Einfügung sollen aber ein leer Paragraph am Ende
        // gelöscht werden.
        collectSurroundingGarbageForCommand(cmd, UNO
            .XTextSectionsSupplier(model.doc), cmd.isManualMode());
      }
      cmd.unsetHasInsertMarks();

      // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      if (cmd.hasInsertMarks())
      {
        collectSurroundingGarbageForCommand(cmd, UNO
            .XTextSectionsSupplier(model.doc), false);
      }
      cmd.unsetHasInsertMarks();

      // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    // Helper-Methoden:

    /**
     * Diese Methode erfasst Einfügemarken und leere Absätze zum Beginn und zum
     * Ende des übergebenen Dokumentkommandos cmd, wobei über
     * removeAnLastEmptyParagraph gesteuert werden kann, ob ein Absatz am Ende
     * eines Textes gelöscht werden soll (bei true) oder nicht (bei false).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void collectSurroundingGarbageForCommand(DocumentCommand cmd,
        XTextSectionsSupplier suppl, boolean removeAnLastEmptyParagraph)
    {
      /*
       * Im folgenden steht eine 0 in der ersten Stelle dafür, dass vor dem
       * Einfügemarker kein Text mehr steht (der Marker also am Anfang des
       * Absatzes ist). Eine 0 an der zweiten Stelle steht dafür, dass hinter
       * dem Einfügemarker kein Text mehr folgt (der Einfügemarker also am Ende
       * des Absatzes steht). Ein "T" an dritter Stelle gibt an, dass hinter dem
       * Absatz des Einfügemarkers eine Tabelle folgt. Ein "E" an dritter Stelle
       * gibt an, dass hinter dem Cursor das Dokument aufhört und kein weiterer
       * Absatz kommt.
       * 
       * Startmarke: Grundsätzlich gibt es die folgenden Fälle zu unterscheiden.
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
       * 00E: Einfügemarker und Zeilenumbruch DAVOR löschen
       * 
       * 01, 10, 11: Einfügemarker löschen
       * 
       * Ein Sonderfall ist der Fall, in dem der Inhalt des Dokumentkommandos
       * leer ist und das Dokumentkommando das einzige Element eines Paragraphen
       * ist. In diesem Fall wird der Paragraph gelöscht.
       * 
       */
      XParagraphCursor[] start = cmd.getStartMark();
      XParagraphCursor[] end = cmd.getEndMark();
      if (start == null || end == null) return;

      // Sonderfall: leerer Inhalt alleine im Paragraph
      if (start[0].isStartOfParagraph() && end[1].isEndOfParagraph())
      {
        XParagraphCursor content = UNO.XParagraphCursor(start[1].getText()
            .createTextCursorByRange(start[1]));
        content.gotoRange(end[0], true);
        if (content.isCollapsed())
        {
          muellmaenner.add(new ParagraphMuellmann(start[1], suppl));
          return;
        }
      }

      // Startmarke auswerten:
      if (start[0].isStartOfParagraph() && start[1].isEndOfParagraph())
      {
        muellmaenner.add(new ParagraphMuellmann(start[1], suppl));
      }
      else
      // if start mark is not the only text in the paragraph
      {
        start[1].goLeft((short) 1, true);
        muellmaenner.add(new RangeMuellmann(start[1]));
      }

      // Endemarke auswerten:

      // Prüfen ob der Cursor am Ende des Dokuments steht. Anmerkung: hier kann
      // nicht der bereits vorhandene cursor end[1] zum Testen verwendet werden,
      // weil dieser durch den goRight verändert würde. Man könnte ihn zwar mit
      // goLeft nachträglich wieder zurück schieben, aber das funzt nicht wenn
      // danach eine Tabelle kommt.
      XParagraphCursor docEndTest = cmd.getEndMark()[1];
      boolean isEndOfDocument = !docEndTest.goRight((short) 1, false);

      if (removeAnLastEmptyParagraph == false) isEndOfDocument = false;

      if (end[0].isStartOfParagraph()
          && end[1].isEndOfParagraph()
          && !isEndOfDocument)
      {
        muellmaenner.add(new ParagraphMuellmann(end[1], suppl));
      }
      else
      {
        end[0].goRight(cmd.getEndMarkLength(), true);
        muellmaenner.add(new RangeMuellmann(end[0]));
      }
    }

    /**
     * Löscht den ganzen ersten Absatz an der Cursorposition textCursor.
     * 
     * @param range
     */
    private void deleteParagraph(XTextRange range, XTextSectionsSupplier tsSuppl)
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
      if (paragraph == null) return;

      // Vergleichen, ob paragraph.getAnchor() eine TextSection vollständig
      // ausfüllt und ggf. die entsprechende TextSection löschen.
      if (tsSuppl != null)
      {
        XTextRange anchor = paragraph.getAnchor();
        XTextRangeCompare comp = UNO.XTextRangeCompare(anchor.getText());

        XNameAccess sections = tsSuppl.getTextSections();
        String[] sectionNames = sections.getElementNames();
        for (int i = 0; i < sectionNames.length; i++)
        {
          try
          {
            XTextSection section = UNO.XTextSection(sections
                .getByName(sectionNames[i]));
            if (comp.compareRegionEnds(anchor, section.getAnchor()) == 0
                && comp.compareRegionStarts(anchor, section.getAnchor()) == 0)
              anchor.getText().removeTextContent(UNO.XTextContent(section));
          }
          catch (java.lang.Exception e)
          {
          }
        }
      }

      // Lösche den Paragraph
      try
      {
        // Ist der Paragraph der einzige Paragraph des Textes, dann kann er mit
        // removeTextContent nicht gelöscht werden. In diesme Fall wird hier
        // wenigstens der Inhalt entfernt:
        paragraph.getAnchor().setString("");

        // Paragraph löschen
        range.getText().removeTextContent(paragraph);
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }
    }
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

    // Markierung des ersten nicht ausgefüllten Platzhalter nach dem Einfügen
    // von Textbausteinen
    private boolean firstEmptyPlaceholder = false;

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

    /**
     * Führt die Dokumentkommandos von tree aus, der dabei so lange aktualisiert
     * wird, bis der Baum vollständig aufgebaut ist.
     * 
     * @param tree
     * @return
     */
    public int execute(DocumentCommandTree tree)
    {
      int errors = 0;

      // so lange wiederholen, bis sich der Baum durch das Expandieren nicht
      // mehr ändert.
      do
        errors += executeDepthFirst(tree, false);
      while (tree.update());

      return errors;
    }

    /**
     * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
     * bookmarkName ein. Im Fehlerfall wird eine entsprechende Fehlermeldung
     * eingefügt.
     */
    public int executeCommand(InsertFrag cmd)
    {
      cmd.setErrorState(false);
      boolean found = false;
      String errors = "";
      Vector urls = new Vector();

      try
      {
        urls = VisibleTextFragmentList.getURLsByID(cmd.getFragID());
        if (urls.size() == 0)
        {
          throw new ConfigurationErrorException(
              "Das Textfragment mit der FRAG_ID '"
                  + cmd.getFragID()
                  + "' ist nicht definiert!");
        }
        // Iterator über URLs
        Iterator iter = urls.iterator();
        while (iter.hasNext() && found == false)
        {
          String urlStr = (String) iter.next();
          try
          {
            URL url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);

            Logger.debug("Füge Textfragment \""
                         + cmd.getFragID()
                         + "\" von URL \""
                         + url.toExternalForm()
                         + "\" ein.");

            // fragment einfügen:
            insertDocumentFromURL(cmd, url);
            found = true;
            fillPlaceholders(model.doc, model.getViewCursor(), cmd
                .getTextRange(), cmd.getArgs());
          }
          catch (java.lang.Exception e)
          {
            // Exception wird nicht beachtet. Wenn die aktuelle URL nicht
            // funktioniert wird die nächste URL ausgewertet
            errors += e.getLocalizedMessage() + "\n\n";
            Logger.debug(e);
          }
        }
        if (!found)
        {
          throw new Exception(errors);
        }
      }
      catch (java.lang.Exception e)
      {
        if (cmd.isManualMode())
        {
          WollMuxSingleton.showInfoModal(
              "WollMux-Fehler",
              "Das Textfragment mit der FRAG_ID '"
                  + cmd.getFragID()
                  + "' konnte nicht eingefügt werden:\n\n"
                  + e.getMessage());
        }
        else
        {
          insertErrorField(cmd, e);
          Logger.error(e);
        }
        cmd.setErrorState(true);
        return 1;
      }

      // Kommando als Done markieren aber noch aufheben. Gelöscht wird das
      // Bookmark dann erst durch den SurroundingGarbageCollector.
      cmd.markDone(false);
      return 0;
    }

    /**
     * Diese Methode fügt das nächste Textfragment aus der dem
     * WMCommandInterpreter übergebenen frag_urls liste ein. Im Fehlerfall wird
     * eine entsprechende Fehlermeldung eingefügt.
     */
    public int executeCommand(InsertContent cmd)
    {
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
      // Kommando als Done markieren aber noch aufheben. Gelöscht wird das
      // Bookmark dann erst durch den SurroundingGarbageCollector.
      cmd.markDone(false);
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

    /**
     * Diese Methode füllt die Einfuegestellen(Platzhalter) aus dem eingefügten
     * Textbaustein mit den übergebenen Argumente args
     * 
     * @param range
     *          der Bereich des eingefügten Textbausteins
     * @param args
     *          Argumente die beim Aufruf zum Einfügen übergeben werden
     */
    private void fillPlaceholders(XTextDocument doc, XTextCursor viewCursor,
        XTextRange range, Vector args)
    {
      // Vector mit allen Platzhalterfelder
      Vector placeholders = new Vector();

      XEnumeration xEnum = UNO.XEnumerationAccess(range).createEnumeration();
      XEnumerationAccess enuAccess;
      // Schleife über den Textbereich
      while (xEnum.hasMoreElements())
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
          while (textPortionEnu.hasMoreElements())
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
                  placeholders.add(textField);
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

      // Enumeration über den Vector placeholders mit Platzhalterfeldern die mit
      // den übergebenen Argumenten gefüllt werden
      Enumeration enumPlaceholders = placeholders.elements();
      for (int j = 0; j < args.size() && j < placeholders.size(); j++)
      {
        Object placeholderObj = enumPlaceholders.nextElement();
        XTextField textField = UNO.XTextField(placeholderObj);
        XTextRange textFieldAnchor = textField.getAnchor();

        // bei einem Parameter ohne Inhalt bleibt die Einfügestelle und die
        // erste ist nach dem Einfügen markiert sonst wird
        // sie ersetzt
        if (!(args.elementAt(j).equals("")))
        {
          textFieldAnchor.setString(args.elementAt(j).toString());
          // setzen des ViewCursor auf die erste nicht ausgefüllte Einfügestelle
          // nach dem Einfügen des Textbausteines
        }
        else if (firstEmptyPlaceholder != true)
        {
          try
          {
            firstEmptyPlaceholder = true;
            viewCursor.gotoRange(textFieldAnchor, false);
          }
          catch (java.lang.Exception e)
          {
          }
        }
      }

      // wenn weniger Parameter als Einfügestellen angegeben wurden wird nach
      // dem Einfügen des Textbaustein und füllen der Argumente, die erste
      // unausgefüllte Einfügestelle markiert.
      if (placeholders.size() > args.size())
      {
        if (firstEmptyPlaceholder == false)
        {
          XTextField textField = UNO.XTextField(placeholders
              .get(args.size()));
          XTextRange textFieldAnchor = textField.getAnchor();
          viewCursor.gotoRange(textFieldAnchor, false);
          firstEmptyPlaceholder = true;
        }
      }

      // Wenn nach dem Einfügen keine Platzhalter vorhanden ist springt der
      // Cursor auf die definierte Marke setJumpMark (falls Vorhanden)
      if (placeholders.size() <= args.size())
      {
        WollMuxEventHandler.handleJumpToMark(doc, false);
      }

      // Wenn mehr Platzhalter angegeben als Einfügestellen vorhanden, erscheint
      // ein Eintrag in der wollmux.log. Wenn in einer Conf Datei im Bereich
      // Textbausteine dort im Bereich Warnungen ein Eintrag mit
      // MSG_TOO_MANY_ARGS "true|on|1" ist, erscheint die Fehlermeldung in einem
      // Fenster im Writer.
      if (placeholders.size() < args.size())
      {

        String error = ("Es sind mehr Parameter angegeben als Platzhalter vorhanden sind");

        Logger.error(error);

        ConfigThingy conf = mux.getWollmuxConf();
        ConfigThingy WarnungenConf = conf.query("Textbausteine").query(
            "Warnungen");

        String message = "";
        try
        {
          message = WarnungenConf.getLastChild().toString();
        }
        catch (NodeNotFoundException e)
        {
        }

        if (message.equals("true")
            || message.equals("on")
            || message.equals("1"))
        {
          WollMuxSingleton.showInfoModal("WollMux", error);
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
      cmd.markDone(!mux.isDebugMode());
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
      cmd.markDone(!mux.isDebugMode());
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

      cmd.markDone(!mux.isDebugMode());
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
      cmd.markDone(true);
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

    public int executeCommand(SetJumpMark cmd)
    {
      model.addSetJumpMarkBlock(cmd);
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
      cmd.markDone(!mux.isDebugMode());
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
    public HashMap idToFormFields = new HashMap();

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
