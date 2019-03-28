/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 *
 * Copyright (c) 2010-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
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
 * 11.10.2007 | BNK | Erstellung
 * 25.05.2010 | ERT | Aufruf von PDFGesamtdruck-Druckfunktion
 * 20.12.2010 | ERT | Bei ungültigem indexSelection.rangeEnd wird der
 *                    Wert auf den letzten Datensatz gesetzt
 * 08.05.2012 | jub | um beim serienbrief/emailversand die auswahl zwischen odt und pdf
 *                    anhängen anbieten zu können, sendAsEmail() und saveToFile() mit
 *                    einer flage versehen, die zwischen den beiden formaten
 *                    unterscheidet.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XStorable;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.email.AuthenticationDialog;
import de.muenchen.allg.itd51.wollmux.email.EMailSender;
import de.muenchen.allg.itd51.wollmux.email.IAuthenticationDialogListener;
import de.muenchen.allg.itd51.wollmux.email.MailServerSettings;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MailMergeNew.class);

  /**
   * true gdw wir uns im Vorschau-Modus befinden.
   */
  public boolean previewMode;

  /**
   * Die Nummer des zu previewenden Datensatzes. ACHTUNG! Kann aufgrund von
   * Veränderung der Daten im Hintergrund größer sein als die Anzahl der Datensätze.
   * Darauf muss geachtet werden.
   */
  private int previewDatasetNumber = 1;

  /**
   * Die beim letzten Aufruf von {@link #updatePreviewFields()} aktuelle Anzahl an
   * Datensätzen in {@link #ds}.
   */
  private int previewDatasetNumberMax = Integer.MAX_VALUE;

  private static File attachment = null;

  private XNumericField datasetNumber;

  private Collection<XWindow> elementsDisabledWhenNoDatasourceSelected =
    new ArrayList<>();

  private Collection<XWindow> elementsDisabledWhenNotInPreviewMode =
    new ArrayList<>();

  private Collection<XWindow> elementsDisabledWhenFirstDatasetSelected =
    new ArrayList<>();

  private Collection<XWindow> elementsDisabledWhenLastDatasetSelected =
    new ArrayList<>();

  /**
   * Enthält alle elementsDisabledWhen... Collections.
   */
  private ArrayList<Collection<XWindow>> listsOfElementsDisabledUnderCertainCircumstances =
    new ArrayList<>();

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

  private MailMergeParams mailMergeParams = new MailMergeParams();

  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * ID der Property in der das Zielverzeichnis für den Druck in Einzeldokumente
   * gespeichert wird.
   */
  private static final String PROP_TARGETDIR = "MailMergeNew_TargetDir";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_FILEPATTERN = "MailMergeNew_FilePattern";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten ist.
   */
  private static final String PROP_EMAIL_TO_FIELD_NAME =
    "MailMergeNew_EMailToFieldName";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten sind.
   */
  private static final String PROP_EMAIL_FROM = "MailMergeNew_EMailFrom";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_SUBJECT = "MailMergeNew_EMailSubject";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_MESSAGE_TEXTTAGS =
    "MailMergeNew_EMailMessageTextTags";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_DATASET_EXPORT = "MailMergeNew_DatasetExport";

  /**
   * ID der Property, die einen List der Indizes der zu druckenden Datensätze
   * speichert.
   */
  private static final String PROP_MAILMERGENEW_SELECTION = "MailMergeNew_Selection";

  private static final String TEMP_MAIL_DIR_PREFIX = "wollmuxmail";

  private static final String MAIL_ERROR_MESSAGE_TITLE =
    L.m("Fehler beim E-Mail-Versand");
  
  private TextDocumentController documentController;
  
  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  public MailMergeDatasource getDs()
  {
    return ds;
  }

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   *
   * @param documentController
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public MailMergeNew(TextDocumentController documentController, ActionListener abortListener)
  {
    this.documentController = documentController;
    this.ds = new MailMergeDatasource(documentController);
    this.abortListener = abortListener;
  }

  /**
   * Schliesst den MailMergeNew und alle zugehörigen Fenster.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public void dispose()
  {
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }

  /**
   * Öffnet den Dialog zum Einfügen eines Spezialfeldes, das über die Funktion
   * trafoConf beschrieben ist, erzeugt daraus ein transformiertes Feld und fügt
   * dieses Feld in das Dokument mod ein; Es erwartet darüber hinaus den Namen des
   * Buttons buttonName, aus dem das Label des Dialogs, und später der Mouse-Over
   * hint erzeugt wird und die Liste der aktuellen Felder, die evtl. im Dialog zur
   * Verfügung stehen sollen.
   *
   * @param fieldNames
   *          Eine Liste der Feldnamen, die der Dialog anzeigt, falls er Buttons zum
   *          Einfügen von Serienbrieffeldern bereitstellt.
   * @param buttonName
   *          Der Name des Buttons, aus dem die Titelzeile des Dialogs und der
   *          Mouse-Over Hint des neu erzeugten Formularfeldes generiert wird.
   * @param trafoConf
   *          ConfigThingy, das die Funktion und damit den aufzurufenden Dialog
   *          spezifiziert. Der von den Dialogen benötigte äußere Knoten
   *          "Func(...trafoConf...) wird dabei von dieser Methode erzeugt, so dass
   *          trafoConf nur die eigentliche Funktion darstellen muss.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void insertFieldFromTrafoDialog(List<String> fieldNames,
      final String buttonName, ConfigThingy trafoConf)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(trafoConf);
    params.isValid = true;
    params.fieldNames = fieldNames;
    params.closeAction = event -> {
      TrafoDialog dialog = (TrafoDialog) event.getSource();
      TrafoDialogParameters status = dialog.getExitStatus();
      if (status.isValid)
      {
        try
        {
          documentController.replaceSelectionWithTrafoField(status.conf, buttonName);
        } catch (Exception x)
        {
          LOGGER.error("", x);
        }
      }
    };

    try
    {
      TrafoDialogFactory.createDialog(params).show(L.m("Spezialfeld %1 einfügen", buttonName));
    }
    catch (UnavailableException e)
    {
      LOGGER.error(L.m("Das darf nicht passieren!"));
    }
  }

  /**
   * Prüft, ob sich in der akutellen Selektion ein transformiertes Feld befindet und
   * liefert ein mit Hilfe der TrafoDialogFactory erzeugtes zugehöriges
   * TrafoDialog-Objekt zurück, oder null, wenn keine transformierte Funktion
   * selektiert ist oder für die Trafo kein Dialog existiert.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private TrafoDialog getTrafoDialogForCurrentSelection()
  {
    ConfigThingy trafoConf = documentController.getModel().getFormFieldTrafoFromSelection();
    if (trafoConf == null) {
      return null;
    }

    final String trafoName = trafoConf.getName();

    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = trafoConf;
    params.isValid = true;
    params.fieldNames = ds.getColumnNames();
    params.closeAction = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            documentController.setTrafo(trafoName, status.conf);
          }
          catch (Exception x)
          {
            LOGGER.error("", x);
          }
        }
      }
    };

    try
    {
      return TrafoDialogFactory.createDialog(params);
    }
    catch (UnavailableException e)
    {
      return null;
    }
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und
   * die Seriendruckfelder im Dokument entsprechend setzt. Herangezogen werden die
   * Properties {@link #PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})
   * und "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält, sowie
   * {@link #PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz). Dies funktioniert
   * natürlich nur dann, wenn pmod kein Proxy ist.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    mailMergeNewSetFormValue(pmod, null);
  }

  /**
   * Implementierung einer Druckfunktion, die das jeweils nächste Element der
   * Seriendruckdaten nimmt und die Seriendruckfelder im Dokument entsprechend setzt;
   * wird der SimulationsResultsProcessor simProc übergeben, so werden die
   * Dokumentänderungen nur simuliert und nicht tatsächlich im Dokument ausgeführt.
   * Im Fall, dass simProc != null ist, wird auch die nächste Druckfunktion in der
   * Aufrufkette nicht aufgerufen, sondern statt dessen der in simProc enthaltene
   * handler. Die Druckfunktion zieht folgende Properties heran:
   *
   * <ul>
   * <li>{@link #PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})</li>
   *
   * <li>"MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält</li>
   *
   * <li>{@link #PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz).</li> *
   * <ul>
   *
   * @param pmod
   *          PrintModel welches das Hauptdokument des Seriendrucks beschreibt.
   * @param simProc
   *          Ist simProc != null, so werden die Wertänderungen nur simuliert und
   *          nach jedem Datensatz einmal der in simProc enthaltene handler
   *          aufgerufen.
   * @throws Exception
   *           Falls irgend etwas schief geht
   *
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   *         TESTED
   */
  @SuppressWarnings("unchecked")
  public static void mailMergeNewSetFormValue(XPrintModel pmod,
      SimulationResultsProcessor simProc) throws Exception
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(pmod.getTextDocument());

    QueryResults data = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection<String> schema = (Collection<String>) pmod.getPropertyValue("MailMergeNew_Schema");
    List<Integer> selection =
      (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    if (selection.isEmpty()) {
      return;
    }

    Iterator<Dataset> iter = data.iterator();
    Iterator<Integer> selIter = selection.iterator();
    int selectedIdx = selIter.next();

    pmod.setPrintProgressMaxValue((short) selection.size());

    int index = -1;
    int serienbriefNummer = 1;
    while (iter.hasNext() && selectedIdx >= 0)
    {
      if (pmod.isCanceled()) {
        return;
      }

      Dataset ds = iter.next();
      if (++index < selectedIdx) {
        continue;
      }

      int datensatzNummer = index + 1; // same as datensatzNummer = selectedIdx+1;

      if (selIter.hasNext())
        selectedIdx = selIter.next();
      else
        selectedIdx = -1;

      if (simProc != null) {
        documentController.startSimulation();
      }

      HashMap<String, String> dataSetExport = new HashMap<>();
      try
      {
        pmod.setPropertyValue(PROP_DATASET_EXPORT, dataSetExport);
      }
      catch (Exception x)
      {}

      for(String spalte : schema)
      {
        String value = ds.get(spalte);
        pmod.setFormValue(spalte, value);
        dataSetExport.put(spalte, value);
      }
      pmod.setFormValue(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      dataSetExport.put(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      pmod.setFormValue(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);
      dataSetExport.put(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);

      // Weiterreichen des Drucks an die nächste Druckfunktion. Dies findet nicht
      // statt, wenn simProc != null ist, da die Verarbeitung in diesem Fall über
      // simProc durchgeführt wird.
      if (simProc == null)
        pmod.printWithProps();
      else
        simProc.processSimulationResults(documentController.stopSimulation());

      pmod.setPrintProgressValue((short) serienbriefNummer);
      ++serienbriefNummer;
    }
  }

  /**
   * Liefert die Größe der von MailMergeNew im XPrintModel gesetzten Selection.
   */
  @SuppressWarnings("unchecked")
  public static int mailMergeNewGetSelectionSize(XPrintModel pmod)
  {
    List<Integer> selection;
    try
    {
      selection = (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      return 0;
    }
    return selection.size();
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static File saveToFile(XPrintModel pmod, boolean isODT)
  {
    XTextDocument textDocument = pmod.getTextDocument();

    File outputDir =
      new File(pmod.getProp(PROP_TARGETDIR,
        System.getProperty("user.home") + "/Seriendruck").toString());

    String filename;
    TextComponentTags filePattern =
      (TextComponentTags) pmod.getProp(PROP_FILEPATTERN, null);
    if (filePattern != null)
      filename = createOutputPathFromPattern(filePattern, pmod);
    else
      filename = L.m("Dokument.odt");

    // jub .odt/.pdf ergänzen, falls nicht angegeben.
    if (!filename.toLowerCase().endsWith(".odt")
      && !filename.toLowerCase().endsWith(".pdf"))
    {
      if (isODT)
        filename = filename + ".odt";
      else
        filename = filename + ".pdf";
    }

    File file = new File(outputDir, filename);

    saveOutputFile(file, textDocument);

    return file;
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static void sendAsEmail(XPrintModel pmod, boolean isODT)
  {
    String targetDir = (String) pmod.getProp(PROP_TARGETDIR, null);
    File tmpOutDir = null;
    if (targetDir != null)
      tmpOutDir = new File(targetDir);
    else
      try
      {
        tmpOutDir = File.createTempFile(TEMP_MAIL_DIR_PREFIX, null);
        tmpOutDir.delete();
        tmpOutDir.mkdir();
        try
        {
          pmod.setPropertyValue(PROP_TARGETDIR, tmpOutDir.toString());
        }
        catch (Exception e)
        {
          LOGGER.error(L.m("darf nicht vorkommen"), e);
        }
      }
      catch (java.io.IOException e)
      {
        LOGGER.error("", e);
      }
    if (tmpOutDir == null)
    {
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Das temporäre Verzeichnis %1 konnte nicht angelegt werden.",
        TEMP_MAIL_DIR_PREFIX));
      pmod.cancel();
      return;
    }

    String from = pmod.getProp(PROP_EMAIL_FROM, "").toString();
    if (!isMailAddress(from))
    {
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Die Absenderadresse '%1' ist ungültig.", from));
      pmod.cancel();
      return;
    }

    String fieldName = pmod.getProp(PROP_EMAIL_TO_FIELD_NAME, "").toString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> ds =
      new HashMap<>((HashMap<String, String>) pmod.getProp(
        PROP_DATASET_EXPORT, new HashMap<String, String>()));
    String to = ds.get(fieldName);
    PrintModels.setStage(pmod, L.m("Sende an %1", to));
    if (!isMailAddress(to))
    {
      boolean res =
          InfoDialog.showCancelModal("ungültige Empfängeradresse",
              L.m("Die Empfängeradresse '%1' ist ungültig!\n\nDiesen Datensatz überspringen und fortsetzen?",
                  to));
      if (res) {
        pmod.cancel();
      }
      return;
    }

    String subject =
      pmod.getProp(PROP_EMAIL_SUBJECT, L.m("<kein Betreff>")).toString();

    String message = "";
    TextComponentTags messageTags =
      (TextComponentTags) pmod.getProp(PROP_EMAIL_MESSAGE_TEXTTAGS, null);
    if (messageTags != null) {
      message = messageTags.getContent(ds);
    }

    try
    {
      attachment = saveToFile(pmod, isODT);
      EMailSender mail = new EMailSender();
      mail.createNewMultipartMail(from, to, subject, message);
      MailServerSettings smtpSettings = mail.getWollMuxMailServerSettings();
      
      IAuthenticationDialogListener authDialogListener = (String username, String password) ->
      {
        if (username == null || password == null)
          return;

        smtpSettings.setUsername(username);
        smtpSettings.setPassword(password);

        try
        {
          mail.addAttachment(attachment);
          mail.sendMessage(smtpSettings);
        } catch (ConfigurationErrorException | MessagingException | IOException e)
        {
          LOGGER.error("", e);
        }
      };
      
      new AuthenticationDialog(smtpSettings.getUsername(), authDialogListener);
    }
    catch (ConfigurationErrorException e)
    {
      LOGGER.error("Kein Mailserver", e);
      InfoDialog.showInfoModal(
        MAIL_ERROR_MESSAGE_TITLE,
        L.m("Es konnten keine Angaben zum Mailserver gefunden werden - eventuell ist die WollMux-Konfiguration nicht vollständig."));
      pmod.cancel();
      return;
    }
    catch (MessagingException e)
    {
      LOGGER.error("Email versenden fehlgeschlagen", e);
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE,
        L.m("Der Versand der E-Mail ist fehlgeschlagen."));
      pmod.cancel();
      return;
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      pmod.cancel();
      return;
    }
    finally
    {
      if (attachment != null) {
        attachment.delete();
      }
    }
  }

  /**
   * grobe Plausiprüfung, ob E-Mailadresse gültig ist.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static boolean isMailAddress(String mail)
  {
    return mail != null && mail.length() > 0 && mail.matches("[^ ]+@[^ ]+");
  }

  /**
   * Speichert doc unter dem in outFile angegebenen Dateipfad und schließt dann doc.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   *         TESTED
   */
  private static void saveOutputFile(File outFile, XTextDocument doc)
  {
    try
    {
      String unparsedUrl = outFile.toURI().toURL().toString();

      XStorable store = UNO.XStorable(doc);
      PropertyValue[] options;

      /*
       * For more options see:
       *
       * http://wiki.services.openoffice.org/wiki/API/Tutorials/PDF_export
       */
      if (unparsedUrl.endsWith(".pdf"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "writer_pdf_Export";
      }
      else if (unparsedUrl.endsWith(".doc"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "MS Word 97";
      }
      else
      {
        if (!unparsedUrl.endsWith(".odt")) {
          unparsedUrl = unparsedUrl + ".odt";
        }

        options = new PropertyValue[0];
      }

      com.sun.star.util.URL url = UNO.getParsedUNOUrl(unparsedUrl);

      /*
       * storeTOurl() has to be used instead of storeASurl() for PDF export
       */
      store.storeToURL(url.Complete, options);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Nimmt filePattern, ersetzt darin befindliche Tags durch entsprechende
   * Spaltenwerte aus ds und setzt daraus einen Dateipfad mit Elternverzeichnis
   * targetDir zusammen. Die Spezialtags {@link MailMergeParams#TAG_DATENSATZNUMMER}
   * und {@link MailMergeParams#TAG_SERIENBRIEFNUMMER} werden durch die Strings
   * datensatzNummer und serienbriefNummer ersetzt.
   *
   * @param totalDatasets
   *          die Gesamtzahl aller Datensätze (auch der für den aktuellen
   *          Druckauftrag nicht gewählten). Wird verwendet um datensatzNummer und
   *          serienbriefNummer mit 0ern zu padden.
   *
   * @throws MissingMapEntryException
   *           wenn ein Tag verwendet wird, zu dem es keine Spalte im aktuellen
   *           Datensatz existiert.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private static String createOutputPathFromPattern(TextComponentTags filePattern,
      XPrintModel pmod)
  {
    int digits = 4;
    try
    {
      QueryResults r = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
      digits = ("" + r.size()).length();
    }
    catch (Exception e)
    {}

    @SuppressWarnings("unchecked")
    HashMap<String, String> dataset =
      new HashMap<>((HashMap<String, String>) pmod.getProp(PROP_DATASET_EXPORT,
        new HashMap<String, String>()));

    // Zähler für #DS und #SB mit gleicher Länge erzeugen (ggf. mit 0en auffüllen)
    fillWithLeading0(dataset, MailMergeParams.TAG_DATENSATZNUMMER, digits);
    fillWithLeading0(dataset, MailMergeParams.TAG_SERIENBRIEFNUMMER, digits);

    String fileName = filePattern.getContent(dataset);
    return simplifyFilename(fileName);
  }

  /**
   * Holt sich Element key aus dataset, sorgt dafür, dass der Wert digit-stellig wird
   * und speichert diesen Wert wieder in dataset ab.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void fillWithLeading0(HashMap<String, String> dataset, String key,
      int digits)
  {
    String value = dataset.get(key);
    if (value == null) {
      value = "";
    }
    while (value.length() < digits)
      value = "0" + value;
    dataset.put(key, value);
  }

  /**
   * Ersetzt alle möglicherweise bösen Zeichen im Dateinamen name durch eine
   * Unterstrich.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String simplifyFilename(String name)
  {
    return name.replaceAll("[^\\p{javaLetterOrDigit},.()=+_-]", "_");
  }
}
