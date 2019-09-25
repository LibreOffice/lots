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
import java.util.Map.Entry;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
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

  private static MailServerSettings smtpSettings = null;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

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
   */
  public MailMergeNew(TextDocumentController documentController, ActionListener abortListener)
  {
    this.ds = new MailMergeDatasource(documentController);
    this.abortListener = abortListener;
  }

  /**
   * Schliesst den MailMergeNew und alle zugehörigen Fenster.
   */
  public void dispose()
  {
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und die
   * Seriendruckfelder im Dokument entsprechend setzt. Herangezogen werden die Properties
   * {@link MailMergeController#PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults}) und
   * "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält, sowie
   * {@link MailMergeController#PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz). Dies funktioniert natürlich nur dann,
   * wenn pmod kein Proxy ist.
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    mailMergeNewSetFormValue(pmod, null);
  }

  /**
   * Implementierung einer Druckfunktion, die das jeweils nächste Element der Seriendruckdaten nimmt
   * und die Seriendruckfelder im Dokument entsprechend setzt; wird der SimulationsResultsProcessor
   * simProc übergeben, so werden die Dokumentänderungen nur simuliert und nicht tatsächlich im
   * Dokument ausgeführt. Im Fall, dass simProc != null ist, wird auch die nächste Druckfunktion in
   * der Aufrufkette nicht aufgerufen, sondern statt dessen der in simProc enthaltene handler. Die
   * Druckfunktion zieht folgende Properties heran:
   *
   * <ul>
   * <li>{@link MailMergeController#PROP_QUERYRESULTS} (ein Objekt vom Typ
   * {@link QueryResults})</li>
   *
   * <li>"MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält</li>
   *
   * <li>{@link MailMergeController#PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz).</li> *
   * <ul>
   *
   * @param pmod
   *          PrintModel welches das Hauptdokument des Seriendrucks beschreibt.
   * @param simProc
   *          Ist simProc != null, so werden die Wertänderungen nur simuliert und nach jedem
   *          Datensatz einmal der in simProc enthaltene handler aufgerufen.
   * @throws Exception
   *           Falls irgend etwas schief geht
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod,
      SimulationResultsProcessor simProc) throws Exception
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(pmod.getTextDocument());

    QueryResults data = (QueryResults) pmod.getPropertyValue(MailMergeController.PROP_QUERYRESULTS);
    Collection<String> schema = (Collection<String>) pmod.getPropertyValue("MailMergeNew_Schema");
    List<Integer> selection =
      (List<Integer>) pmod.getPropertyValue(MailMergeController.PROP_MAILMERGENEW_SELECTION);
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

      int datensatzNummer = index + 1;

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
        pmod.setPropertyValue(MailMergeController.PROP_DATASET_EXPORT, dataSetExport);
      }
      catch (Exception x)
      {}

      for(String spalte : schema)
      {
        String value = ds.get(spalte);
        pmod.setFormValue(spalte, value);
        dataSetExport.put(spalte, value);
      }
      pmod.setFormValue(MailMergeController.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      dataSetExport.put(MailMergeController.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      pmod.setFormValue(MailMergeController.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);
      dataSetExport.put(MailMergeController.TAG_SERIENBRIEFNUMMER, ""
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
      selection = (List<Integer>) pmod.getPropertyValue(MailMergeController.PROP_MAILMERGENEW_SELECTION);
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      return 0;
    }
    return selection.size();
  }

  public static File createTempDocumentFileByFilePattern(XPrintModel pmod, boolean isODT)
  {
    File outputDir =
      new File(pmod.getProp(MailMergeController.PROP_TARGETDIR,
        System.getProperty("user.home") + "/Seriendruck").toString());

    HashMap<String, String> dataset = new HashMap<>((HashMap<String, String>) pmod
        .getProp(MailMergeController.PROP_DATASET_EXPORT, new HashMap<String, String>()));

    String filename = replaceTextByMergeFieldValue((String) pmod.getProp(MailMergeController.PROP_FILEPATTERN, null),
        dataset);

    if (!filename.toLowerCase().endsWith(".odt")
      && !filename.toLowerCase().endsWith(".pdf"))
    {
      if (isODT)
        filename = filename + ".odt";
      else
        filename = filename + ".pdf";
    }

    return new File(outputDir, filename);
  }

  /**
   * Ersetzt Serienbrieffelder im Format '{{Name}}' durch den entsprechenden Wert im Datensatz.
   *
   * @param text: Zu ersetzender Text der Serienbrieffelder im Format '{{Name}}' enthält
   * @param dataset: Key = Serienbrieffeld, Value = Wert des Datensatzes
   * @return
   */
  private static String replaceTextByMergeFieldValue(String text,
      HashMap<String, String> dataset)
  {
    for (Entry<String, String> entry : dataset.entrySet())
    {
      String mergeFieldWithTags = addMergeFieldTags(entry.getKey());

      if (text.contains(mergeFieldWithTags))
      {
        text = text.replace(mergeFieldWithTags, entry.getValue());
      }
    }

    return text;
  }

  public static String addMergeFieldTags(String mergeField) {
    return "{{" + mergeField + "}}";
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   */
  public static void sendAsEmail(XPrintModel pmod, boolean isODT)
  {
    String targetDir = (String) pmod.getProp(MailMergeController.PROP_TARGETDIR, null);
    File tmpOutDir = null;
    if (targetDir != null)
      tmpOutDir = new File(targetDir);
    else
      try
      {
        tmpOutDir = File.createTempFile(MailMergeController.TEMP_MAIL_DIR_PREFIX, null);
        tmpOutDir.delete();
        tmpOutDir.mkdir();
        try
        {
          pmod.setPropertyValue(MailMergeController.PROP_TARGETDIR,
              tmpOutDir.toURI().toString());
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
      InfoDialog.showInfoModal(MailMergeController.MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Das temporäre Verzeichnis %1 konnte nicht angelegt werden.",
        MailMergeController.TEMP_MAIL_DIR_PREFIX));
      pmod.cancel();
      return;
    }

    String from = pmod.getProp(MailMergeController.PROP_EMAIL_FROM, "").toString();
    if (!isMailAddress(from))
    {
      InfoDialog.showInfoModal(MailMergeController.MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Die Absenderadresse '%1' ist ungültig.", from));
      pmod.cancel();
      return;
    }

    String fieldName = pmod.getProp(MailMergeController.PROP_EMAIL_TO_FIELD_NAME, "").toString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> ds = new HashMap<>((HashMap<String, String>) pmod
        .getProp(
            MailMergeController.PROP_DATASET_EXPORT, new HashMap<String, String>()));
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
      pmod.getProp(MailMergeController.PROP_EMAIL_SUBJECT, L.m("<kein Betreff>")).toString();
    String message =
      (String) pmod.getProp(MailMergeController.PROP_EMAIL_MESSAGE_TEXTTAGS, null);

    try
    {
      EMailSender mail = new EMailSender();
      mail.createNewMultipartMail(from, to, replaceTextByMergeFieldValue(subject, ds),
          replaceTextByMergeFieldValue(message, ds));

      smtpSettings = (MailServerSettings) pmod
          .getProp(MailMergeController.PROP_EMAIL_MAIL_SERVER_SETTINGS, null);

      if (smtpSettings == null)
      {
        smtpSettings = mail.getWollMuxMailServerSettings();
        pmod.setPropertyValue(MailMergeController.PROP_EMAIL_MAIL_SERVER_SETTINGS, smtpSettings);
      }

      IAuthenticationDialogListener authDialogListener = (String username, String password) ->
      {
        if (username == null || password == null)
          return;

        smtpSettings.setUsername(username);
        smtpSettings.setPassword(password);
        try
        {
          pmod.setPropertyValue(MailMergeController.PROP_EMAIL_MAIL_SERVER_SETTINGS, smtpSettings);
        } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
            | WrappedTargetException e)
        {
          LOGGER.error("", e);
        }
      };

      if ((smtpSettings.getUsername() != null && !smtpSettings.getUsername().isEmpty())
          && (smtpSettings.getPassword() == null || smtpSettings.getPassword().isEmpty()))
        new AuthenticationDialog(smtpSettings.getUsername(), authDialogListener);

      File document = saveOutputFile(createTempDocumentFileByFilePattern(pmod, isODT),
          pmod.getTextDocument());

      sendMail(mail, smtpSettings, document);

      // Wenn Properties noch nicht gesetzt worden sind initial setzen da
      // sonst bei getPropertyValue() UnknownPropertyException geworfen wird.
      if (!pmod.getPropertySetInfo()
          .hasPropertyByName(MailMergeController.PROP_EMAIL_REPORT_RECIPIENT_LIST))
      {
        pmod.setPropertyValue(MailMergeController.PROP_EMAIL_REPORT_RECIPIENT_LIST,
          new ArrayList<String>());
      }

      if (!pmod.getPropertySetInfo()
          .hasPropertyByName(MailMergeController.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT))
      {
        pmod.setPropertyValue(MailMergeController.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT, 0);
      }

      List<String> reportRecipientList = (List<String>) pmod
          .getPropertyValue(MailMergeController.PROP_EMAIL_REPORT_RECIPIENT_LIST);
      int mailsSentCount = (int) pmod
          .getPropertyValue(MailMergeController.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT);

      if (reportRecipientList == null)
        reportRecipientList = new ArrayList<>();

      reportRecipientList.add(to);
      mailsSentCount++;

      pmod.setPropertyValue(MailMergeController.PROP_EMAIL_REPORT_RECIPIENT_LIST,
          reportRecipientList);
      pmod.setPropertyValue(MailMergeController.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT,
          mailsSentCount);
    }
    catch (ConfigurationErrorException e)
    {
      LOGGER.error("Kein Mailserver", e);
      InfoDialog.showInfoModal(
          MailMergeController.MAIL_ERROR_MESSAGE_TITLE,
        L.m("Es konnten keine Angaben zum Mailserver gefunden werden - eventuell ist die WollMux-Konfiguration nicht vollständig."));
      pmod.cancel();
      return;
    }
    catch (MessagingException e)
    {
      LOGGER.error("Email versenden fehlgeschlagen", e);
      InfoDialog.showInfoModal(MailMergeController.MAIL_ERROR_MESSAGE_TITLE,
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
  }

  private static void sendMail(EMailSender mail, MailServerSettings smtpSettings, File document)
  {
    try
    {
      String path = document.getPath();
      if (!document.isAbsolute())
      {
        // Pfad unter Windows korrigieren
        path = path.replaceFirst("file:\\\\", "");
        // Pfad unter Linux korrigieren
        path = path.replaceFirst("file:", "");
      }
      mail.addAttachment(new File(path));
      mail.sendMessage(smtpSettings);
    } catch (ConfigurationErrorException | MessagingException | IOException e)
    {
      LOGGER.error("", e);
    } finally
    {
      document.delete();
    }
  }

  /**
   * grobe Plausiprüfung, ob E-Mailadresse gültig ist.
   */
  private static boolean isMailAddress(String mail)
  {
    return mail != null && mail.length() > 0 && mail.matches("[^ ]+@[^ ]+");
  }

  /**
   * Speichert doc unter dem in outFile angegebenen Dateipfad und schließt dann doc.
   */
  public static File saveOutputFile(File outFile, XTextDocument doc)
  {
    try
    {
      String outFilePath = outFile.getPath();
      XStorable store = UNO.XStorable(doc);
      PropertyValue[] options;

      // fyi: http://wiki.services.openoffice.org/wiki/API/Tutorials/PDF_export
      if (outFilePath.endsWith(".pdf"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "writer_pdf_Export";
      }
      else if (outFilePath.endsWith(".doc"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "MS Word 97";
      }
      else
      {
        if (!outFilePath.endsWith(".odt"))
        {
          outFilePath = outFilePath + ".odt";
        }

        options = new PropertyValue[0];
      }

      if (System.getProperty("os.name").toLowerCase().contains("windows"))
      {
    	  outFilePath = outFilePath.replace("\\", "/");
      }

      com.sun.star.util.URL url = UNO.getParsedUNOUrl(outFilePath);

      // storeTOurl() has to be used instead of storeASurl() for PDF export
      store.storeToURL(url.Complete, options);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }

    return outFile;
  }
}
