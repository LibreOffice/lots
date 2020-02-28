package de.muenchen.allg.itd51.wollmux.mailmerge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
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
import de.muenchen.allg.itd51.wollmux.mailmerge.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.PrintToEmail;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.PrintSettings;
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
  public void doMailMerge(PrintSettings settings)
  {
    documentController.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();

    List<String> usePrintFunctions = new ArrayList<>();
    boolean ignoreDocPrintFuncs = true;
    switch (settings.getAction())
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
      switch (settings.getFormat())
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
      switch (settings.getFormat())
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
    switch (settings.getSelection())
    {
    case ALL:
      selected = IntStream.range(0, data.size()).boxed().collect(Collectors.toList());
      break;
    case RANGE:
      int rangeStart = Math.min(settings.getRangeStart(), data.size());
      int rangeEnd = Math.min(settings.getRangeEnd(), data.size());
      selected = IntStream.range(rangeStart - 1, rangeEnd).boxed().collect(Collectors.toList());
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

      // TODO error handling
      settings.getTargetDirectory().ifPresent(
          targetDir -> setPropertyValue(pmod, MailMergePrintFunction.PROP_TARGETDIR, targetDir));

      settings.getFilenameTemplate().ifPresent(
          template -> setPropertyValue(pmod, MailMergePrintFunction.PROP_FILEPATTERN, template));

      settings.getEmailToFieldName()
          .ifPresent(name -> setPropertyValue(pmod, PrintToEmail.PROP_EMAIL_TO_FIELD_NAME, name));
      settings.getEmailFrom()
          .ifPresent(sender -> setPropertyValue(pmod, PrintToEmail.PROP_EMAIL_FROM, sender));
      settings.getEmailSubject().ifPresent(subject -> setPropertyValue(pmod,
          PrintToEmail.PROP_EMAIL_SUBJECT, subject));
      settings.getEmailText().ifPresent(text -> setPropertyValue(pmod,
          PrintToEmail.PROP_EMAIL_MESSAGE_TEXTTAGS, text));
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

  private static boolean setPropertyValue(XPrintModel pmod, String property, Object value)
  {
    try
    {
      pmod.setPropertyValue(property, value);
      return true;
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
      return false;
    }
  }
}
