package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResultsWithSchema;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.email.EMailSender;
import de.muenchen.allg.itd51.wollmux.email.MailServerSettings;
import de.muenchen.allg.itd51.wollmux.func.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.func.print.PrintToEmail;
import de.muenchen.allg.itd51.wollmux.func.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Übernimmt die Aufgabe des Controllers bezüglich dieser Klasse (MailMergeParams),
 * die die View darstellt.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class MailMergeController
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeController.class);

  private TextDocumentController documentController;

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   *
   * @param documentController
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   */
  public MailMergeController(TextDocumentController documentController, MailMergeDatasource ds)
  {
    this.documentController = documentController;
    this.ds = ds;
  }

  public TextDocumentController getDocumentController()
  {
    return documentController;
  }

  public MailMergeDatasource getDs()
  {
    return ds;
  }

  /**
   * Gibt Auskunft darüber, ob die Druckfunktion name existiert.
   */
  public boolean hasPrintfunction(String name)
  {
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);
    try
    {
      pmod.usePrintFunction(name);
      return true;
    }
    catch (NoSuchMethodException ex)
    {
      return false;
    }
  }

  /**
   * Liefert die Spaltennamen der aktuellen Datenquelle
   */
  public List<String> getColumnNames()
  {
    return ds.getColumnNames();
  }

  /**
   * Liefert einen Vorschlag für einen Dateinamen zum Speichern der Einzeldokumente
   * (im Fall von Einzeldokumentdruck und E-Mail versandt), so wie er aus
   * Benutzersicht wahrscheinlich erwünscht ist OHNE Suffix.
   */
  public String getDefaultFilename()
  {
    String title = documentController.getFrameController().getTitle();
    // Suffix entfernen:
    if (title.toLowerCase().matches(".+\\.(odt|doc|ott|dot)$"))
      title = title.substring(0, title.length() - 4);
    return simplifyFilename(title);
  }

  /**
   * Liefert das Textdokument für das der Seriendruck gestartet werden soll.
   */
  public XTextDocument getTextDocument()
  {
    return documentController.getModel().doc;
  }

  /**
   * Startet den MailMerge
   */
  public void doMailMerge(ACTION action, FORMAT format, DatasetSelectionType datasetSelectionType,
      Map<SubmitArgument, Object> args)
  {
    documentController.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();

    List<String> usePrintFunctions = new ArrayList<>();
    boolean ignoreDocPrintFuncs = true;
    switch (action)
    {
    case SINGLE_DOCUMENT:
      ignoreDocPrintFuncs = false;
      usePrintFunctions.add("OOoMailMergeToOdtFile");
      break;
    case DIRECT:
      ignoreDocPrintFuncs = false;
      usePrintFunctions.add("OOoMailMergeToPrinter");
      break;
    case MAIL:
      ignoreDocPrintFuncs = true;
      usePrintFunctions.add("MailMergeNewSetFormValue");
      switch (format)
      {
      case ODT:
        usePrintFunctions.add("MailMergeNewToODTEMail");
        break;
      case PDF:
        usePrintFunctions.add("MailMergeNewToPDFEMail");
        break;
      default:
        return;
      }
      break;
    case MULTIPLE_DOCUMENTS:
      ignoreDocPrintFuncs = true;
      usePrintFunctions.add("MailMergeNewSetFormValue");
      switch (format)
      {
      case ODT:
        usePrintFunctions.add("MailMergeNewToSingleODT");
        break;
      case PDF:
        usePrintFunctions.add("MailMergeNewToSinglePDF");
        break;
      default:
        return;
      }
      break;
    default:
        return;
    }

    List<Integer> selected = new ArrayList<>();
    switch (datasetSelectionType)
    {
      case ALL:
        for (int i = 0; i < data.size(); ++i)
          selected.add(i);
        break;
      case INDIVIDUAL:
        IndexSelection indexSelection =
          (IndexSelection) args.get(SubmitArgument.INDEX_SELECTION);
        selected.addAll(indexSelection.selectedIndexes);
        break;
      case RANGE:
        indexSelection = (IndexSelection) args.get(SubmitArgument.INDEX_SELECTION);
        if (indexSelection.rangeStart < 1) {
          indexSelection.rangeStart = 1;
        }
        if (indexSelection.rangeEnd < 1) {
          indexSelection.rangeEnd = data.size();
        }
        if (indexSelection.rangeEnd > data.size())
          indexSelection.rangeEnd = data.size();
        if (indexSelection.rangeStart > data.size())
          indexSelection.rangeStart = data.size();
        if (indexSelection.rangeStart > indexSelection.rangeEnd)
        {
          int t = indexSelection.rangeStart;
          indexSelection.rangeStart = indexSelection.rangeEnd;
          indexSelection.rangeEnd = t;
        }
        for (int i = indexSelection.rangeStart; i <= indexSelection.rangeEnd; ++i)
          selected.add(i - 1); // wir zählen ab 0, anders als rangeStart/End
        break;
      case NOTHING:
        break;
    }

    // PrintModel erzeugen und Parameter setzen:
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, !ignoreDocPrintFuncs);
    try
    {
      pmod.setPropertyValue(SetFormValue.PROP_SCHEMA, data.getSchema());
      pmod.setPropertyValue(SetFormValue.PROP_QUERYRESULTS, data);
      Collections.sort(selected);
      pmod.setPropertyValue(SetFormValue.PROP_RECORD_SELECTION, selected);

      Object o = args.get(SubmitArgument.TARGET_DIRECTORY);
      if (o != null) {
        pmod.setPropertyValue(MailMergePrintFunction.PROP_TARGETDIR, o);
      }

      o = args.get(SubmitArgument.FILENAME_TEMPLATE);
      if (o != null) {
        pmod.setPropertyValue(MailMergePrintFunction.PROP_FILEPATTERN, o);
      }

      o = args.get(SubmitArgument.EMAIL_TO_FIELD_NAME);
      if (o != null) {
        pmod.setPropertyValue(PrintToEmail.PROP_EMAIL_TO_FIELD_NAME, o);
      }

      o = args.get(SubmitArgument.EMAIL_FROM);
      if (o != null) {
        pmod.setPropertyValue(PrintToEmail.PROP_EMAIL_FROM, o);
      }

      o = args.get(SubmitArgument.EMAIL_SUBJECT);
      if (o != null) {
        pmod.setPropertyValue(PrintToEmail.PROP_EMAIL_SUBJECT, o);
      }

      o = args.get(SubmitArgument.EMAIL_TEXT);
      if (o != null) {
        pmod.setPropertyValue(PrintToEmail.PROP_EMAIL_MESSAGE_TEXTTAGS, o);
      }
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
      return;
    }

    // Benötigte Druckfunktionen zu pmod hinzufügen:
    try
    {
      for (String printFunctionName : usePrintFunctions)
        pmod.usePrintFunction(printFunctionName);
    }
    catch (NoSuchMethodException e)
    {
      LOGGER.error("Eine notwendige Druckfunktion ist nicht definiert.", e);
      InfoDialog.showInfoModal(
        L.m("Fehler beim Drucken"),
        L.m(
          "Eine notwendige Druckfunktion ist nicht definiert. Bitte wenden Sie sich an Ihre Systemadministration damit Ihre Konfiguration entsprechend erweitert bzw. aktualisiert werden kann."));
      pmod.cancel();
      return;
    }

    // Drucken im Hintergrund, damit der EDT nicht blockiert.
    new Thread()
    {
      @Override
      public void run()
      {
        long startTime = System.currentTimeMillis();

        documentController.setFormFieldsPreviewMode(true);
        pmod.printWithProps();
        documentController.setFormFieldsPreviewMode(false);

        long duration = (System.currentTimeMillis() - startTime) / 1000;

        LOGGER.debug(L.m("MailMerge finished after %1 seconds", duration));

        // Wenn der Seriendruck per E-Mail versendet wird, sende Zusammenfassung
        // Liste der Empfänger-Emails und Anzahl versendeter Emails
        String eMailFrom = pmod.getProp(PrintToEmail.PROP_EMAIL_FROM, "").toString();

        @SuppressWarnings("unchecked")
        List<String> recipintList = (List<String>) pmod
            .getProp(PrintToEmail.PROP_EMAIL_REPORT_RECIPIENT_LIST, null);
        int mailsSentCount = (int) pmod
            .getProp(PrintToEmail.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT, 0);

        if (recipintList == null)
          return;

        EMailSender mail = new EMailSender();
        StringBuilder buildMessage = new StringBuilder();

        buildMessage.append(
            "Der WollMux-Serienbrief wurde an folgende E-Mail-Adressen versandt:");
        buildMessage.append("\r\n");

        for (String recipient : recipintList)
        {
          buildMessage.append(recipient);
          buildMessage.append("\r\n");
        }

        buildMessage.append("\r\n");
        buildMessage.append("Anzahl gesendeter E-Mails: ");
        buildMessage.append(mailsSentCount);

        buildMessage.append("\r\n");
        buildMessage.append(
            "Wenn eine Nachricht nicht zugestellt werden konnte, erhalten Sie in Kürze eine entsprechende Email.");

        try
        {
          mail.createNewMultipartMail(eMailFrom, eMailFrom,
              "WollMux-Seriendruck: Bericht über Ihren E-Mail-Versand", buildMessage.toString());
        } catch (MessagingException e)
        {
          LOGGER.error("", e);
        }

        try
        {
          MailServerSettings smtpSettings = (MailServerSettings) pmod
              .getPropertyValue(PrintToEmail.PROP_EMAIL_MAIL_SERVER_SETTINGS);
          mail.sendMessage(smtpSettings);

        } catch (ConfigurationErrorException | MessagingException | WrappedTargetException
            | UnknownPropertyException e)
        {
          LOGGER.error("", e);
        }
      }
    }.start();
  }

  enum FORMAT {
    ODT,
    PDF,
    NOTHING;
  }

  enum ACTION {
    SINGLE_DOCUMENT,
    DIRECT,
    MAIL,
    MULTIPLE_DOCUMENTS,
    NOTHING;
  }

  /**
   * Zählt alle Schlüsselwörter auf, die Übergabeargumente für
   * {@link MailMergeController#doMailMerge(ACTION, FORMAT, DatasetSelectionType, Map)} sein können.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public enum SubmitArgument {
    TARGET_DIRECTORY,
    FILENAME_TEMPLATE,
    EMAIL_FROM,
    EMAIL_TO_FIELD_NAME,
    EMAIL_TEXT,
    EMAIL_SUBJECT,
    INDEX_SELECTION,
  }

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   *
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  public enum DatasetSelectionType {
    /**
     * Alle Datensätze.
     */
    ALL,

    /**
     * Der durch {@link IndexSelection#rangeStart} und {@link IndexSelection#rangeEnd} gegebene
     * Wert.
     */
    RANGE,

    /**
     * Die durch {@link IndexSelection#selectedIndexes} bestimmten Datensätze.
     */
    INDIVIDUAL,
    NOTHING;
  }

  public static class IndexSelection
  {
    /**
     * Falls {@link MailmergeWizardController#datasetSelectionType} ==
     * {@link DatasetSelectionType#RANGE} bestimmt dies den ersten zu druckenden Datensatz (wobei
     * der erste Datensatz die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder größer als
     * {@link #rangeEnd} sein. Dies muss dort behandelt werden, wo er verwendet wird.
     */
    public int rangeStart = 1;

    /**
     * Falls {@link MailmergeWizardController#datasetSelectionType} ==
     * {@link DatasetSelectionType#RANGE} bestimmt dies den letzten zu druckenden Datensatz (wobei
     * der erste Datensatz die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder kleiner als
     * {@link #rangeStart} sein. Dies muss dort behandelt werden, wo er verwendet wird.
     */
    public int rangeEnd = Integer.MAX_VALUE;

    /**
     * Falls {@link MailmergeWizardController#datasetSelectionType} ==
     * {@link DatasetSelectionType#INDIVIDUAL} bestimmt dies die Indizes der ausgewählten
     * Datensätze, wobei 1 den ersten Datensatz bezeichnet.
     */
    public List<Integer> selectedIndexes = new ArrayList<>();

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
