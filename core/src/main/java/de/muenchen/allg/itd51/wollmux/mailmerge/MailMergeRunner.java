/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.mailmerge;

import java.util.List;
import java.util.Set;
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

import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.mail.EMailSender;
import de.muenchen.allg.itd51.wollmux.mailmerge.mail.MailServerSettings;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.PrintToEmail;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.PrintSettings;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Configures and performs a mail merge.
 */
public class MailMergeRunner implements Runnable
{
  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeRunner.class);

  /**
   * The print model of the mail merge.
   */
  final XPrintModel pmod;

  /**
   * Setup a mail merge.
   *
   * @param documentController
   *          The controller of the input document.
   * @param model
   *          The mail merge model.
   * @param settings
   *          The settings of the mail merge.
   * @throws NoTableSelectedException
   *           No data source was selected.
   */
  public MailMergeRunner(TextDocumentController documentController, DatasourceModel model,
      PrintSettings settings) throws NoTableSelectedException
  {
    documentController.collectNonWollMuxFormFields();
    pmod = PrintModels.createPrintModel(documentController);
    collectPrintFunctions(settings, documentController.getModel().getPrintFunctions());
    collectRecordIds(settings, model.getNumberOfRecords());
    setPropertyValue(SetFormValue.PROP_QUERYRESULTS, model.getData());
    setProperties(settings);
  }

  @Override
  public void run()
  {
    if (pmod.isCanceled())
    {
      return;
    }

    long startTime = System.currentTimeMillis();
    pmod.printWithProps();
    long duration = (System.currentTimeMillis() - startTime) / 1000;
    LOGGER.debug("MailMerge finished after {} seconds", duration);

    sendMailSummary();
  }

  /**
   * If mail merge is send via mail, send list of recipients and number of sent mails to sender
   * address.
   */
  private void sendMailSummary()
  {
    String eMailFrom = pmod.getProp(PrintToEmail.PROP_EMAIL_FROM, "").toString();

    @SuppressWarnings("unchecked")
    List<String> recipintList = (List<String>) pmod
        .getProp(PrintToEmail.PROP_EMAIL_REPORT_RECIPIENT_LIST, null);
    int mailsSentCount = (int) pmod.getProp(PrintToEmail.PROP_EMAIL_REPORT_EMAILS_SENT_COUNT, 0);

    if (recipintList == null)
      return;

    EMailSender mail = new EMailSender();
    StringBuilder buildMessage = new StringBuilder();

    buildMessage.append("Der WollMux-Serienbrief wurde an folgende E-Mail-Adressen versandt:");
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
      MailServerSettings smtpSettings = (MailServerSettings) pmod
          .getPropertyValue(PrintToEmail.PROP_EMAIL_MAIL_SERVER_SETTINGS);
      mail.sendMessage(smtpSettings);
    } catch (MessagingException | UnknownPropertyException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Set properties if they are present in the settings. If an error occurs, the {@link #pmod} is
   * canceled.
   *
   * @param settings
   *          The settings of the mail merge.
   */
  private void setProperties(PrintSettings settings)
  {
    settings.getTargetDirectory()
        .ifPresent(targetDir -> setPropertyValue(MailMergePrintFunction.PROP_TARGETDIR, targetDir));

    settings.getFilenameTemplate()
        .ifPresent(template -> setPropertyValue(MailMergePrintFunction.PROP_FILEPATTERN, template));

    settings.getEmailToFieldName()
        .ifPresent(name -> setPropertyValue(PrintToEmail.PROP_EMAIL_TO_FIELD_NAME, name));
    settings.getEmailFrom()
        .ifPresent(sender -> setPropertyValue(PrintToEmail.PROP_EMAIL_FROM, sender));
    settings.getEmailSubject()
        .ifPresent(subject -> setPropertyValue(PrintToEmail.PROP_EMAIL_SUBJECT, subject));
    settings.getEmailText()
        .ifPresent(text -> setPropertyValue(PrintToEmail.PROP_EMAIL_MESSAGE_TEXTTAGS, text));
  }

  /**
   * Collect the Id of the records to use. If an error occurs, the {@link #pmod} is canceled.
   *
   * @param settings
   *          The print settings.
   * @param maxRecords
   *          The Id of the last record.
   * @return A sorted list of the Id.
   */
  private void collectRecordIds(PrintSettings settings, int maxRecords)
  {
    switch (settings.getSelection())
    {
    case ALL:
      setPropertyValue(SetFormValue.PROP_RECORD_SELECTION,
          IntStream.rangeClosed(1, maxRecords).boxed().sorted().collect(Collectors.toList()));
      break;
    case RANGE:
      int rangeStart = Math.min(settings.getRangeStart(), maxRecords);
      int rangeEnd = Math.min(settings.getRangeEnd(), maxRecords);
      setPropertyValue(SetFormValue.PROP_RECORD_SELECTION, IntStream
          .rangeClosed(rangeStart, rangeEnd).boxed().sorted().collect(Collectors.toList()));
      break;
    case NOTHING:
      break;
    }
  }

  /**
   * Collect the names of the {@link PrintFunction} necessary for the print action
   * ({@link PrintSettings#getAction()}) and adds them to the {@link XPrintModel}. If print
   * functions of the document are allowed they're added, too. If an error occurs, the {@link #pmod}
   * is canceled.
   *
   * @param settings
   *          The print settings.
   * @param docPrintFunctions
   *          The print functions of the document.
   */
  private void collectPrintFunctions(PrintSettings settings, Set<String> docPrintFunctions)
  {
    try
    {
      boolean ignoreDocPrintFuncs = true;
      switch (settings.getAction())
      {
      case SINGLE_DOCUMENT_ODT:
        ignoreDocPrintFuncs = false;
        pmod.usePrintFunction("OOoMailMergeToOdtFile");
        pmod.usePrintFunction("OOoMailMergeToShowOdtFile");
        break;
      case SINGLE_DOCUMENT_PDF:
        ignoreDocPrintFuncs = false;
        pmod.usePrintFunction("OOoMailMergeToOdtFile");
        pmod.usePrintFunction("OOoMailMergeToPdfFile");
        pmod.usePrintFunction("ShowDocument");
        break;
      case DIRECT:
        ignoreDocPrintFuncs = false;
        pmod.usePrintFunction("OOoMailMergeToPrinter");
        break;
      case MAIL:
        pmod.usePrintFunction("MailMergeNewSetFormValue");
        switch (settings.getFormat())
        {
        case ODT:
          pmod.usePrintFunction("MailMergeNewToODTEMail");
          break;
        case PDF:
          pmod.usePrintFunction("MailMergeNewToPDFEMail");
          break;
        default:
          pmod.cancel();
          break;
        }
        break;
      case MULTIPLE_DOCUMENTS:
        pmod.usePrintFunction("MailMergeNewSetFormValue");
        switch (settings.getFormat())
        {
        case ODT:
          pmod.usePrintFunction("MailMergeNewToSingleODT");
          break;
        case PDF:
          pmod.usePrintFunction("MailMergeNewToSinglePDF");
          break;
        default:
          pmod.cancel();
          break;
        }
        break;
      default:
        pmod.cancel();
        break;
      }
      if (!ignoreDocPrintFuncs)
      {
        for (String printFunctionName : docPrintFunctions)
          pmod.usePrintFunction(printFunctionName);
      }
    } catch (NoSuchMethodException e)
    {
      LOGGER.error("A necessary print function is not defined.", e);
      InfoDialog.showInfoModal(L.m("Error during printing"), L.m(
          "A necessary print function is not defined. Please contact your system administrator "
              + "so your configuration can be updated."));
      pmod.cancel();
    }
  }

  /**
   * Set a property on {@link #pmod}. If this fails {@link #pmod} is canceled.
   *
   * @param property
   *          The name of the property.
   * @param value
   *          The value of the property.
   */
  private void setPropertyValue(String property, Object value)
  {
    try
    {
      pmod.setPropertyValue(property, value);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
      pmod.cancel();
    }
  }
}
