/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.mail.AuthenticationDialog;
import de.muenchen.allg.itd51.wollmux.mailmerge.mail.EMailSender;
import de.muenchen.allg.itd51.wollmux.mailmerge.mail.MailServerSettings;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * An abstract print function for sending files with LibreOffice mailmerge.
 */
public abstract class PrintToEmail extends MailMergePrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintToEmail.class);

  /**
   * Key for saving the name of the field, which contains the mail of the recipients, as a property
   * of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_EMAIL_TO_FIELD_NAME = "MailMergeNew_EMailToFieldName";

  /**
   * Key for saving the mail of the sender as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_EMAIL_FROM = "MailMergeNew_EMailFrom";

  /**
   * Key for saving the subject of the mails as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_EMAIL_SUBJECT = "MailMergeNew_EMailSubject";

  /**
   * Key for saving the text of the mails as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_EMAIL_MESSAGE_TEXTTAGS = "MailMergeNew_EMailMessageTextTags";

  /**
   * Key for saving the list of recipient mails as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link List} of Strings.
   */
  public static final String PROP_EMAIL_REPORT_RECIPIENT_LIST = "MailMergeNew_EMailReportReciptienList";

  /**
   * Key for saving the number of sent email as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_EMAIL_REPORT_EMAILS_SENT_COUNT = "MailMergeNew_EMailReportEMailsSentCount";

  /**
   * Key for saving the mail server settings as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link MailServerSettings}.
   */
  public static final String PROP_EMAIL_MAIL_SERVER_SETTINGS = "MailMergeNew_MailServerSettings";

  /**
   * Title for dialogs indicating an error.
   */
  public static final String MAIL_ERROR_MESSAGE_TITLE = L.m("Fehler beim E-Mail-Versand");

  /**
   * Create a new print function with name and order.
   *
   * @param functionName
   *          The name of the print function.
   * @param order
   *          The order of the print function.
   */
  public PrintToEmail(String functionName, int order)
  {
    super(functionName, order);
  }

  /**
   * Creates a temporary document and sends it with a mail.
   *
   * @param pmod
   *          The {@link XPrintModel}.
   * @param isODT
   *          If true, sends documents as odt, otherwise as pdf.
   */
  public void sendAsEmail(XPrintModel pmod, boolean isODT)
  {
    String from = pmod.getProp(PROP_EMAIL_FROM, "").toString();
    if (!isMailAddress(from))
    {
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE,
          L.m("Die Absenderadresse '%1' ist ungültig.", from));
      pmod.cancel();
      return;
    }

    String fieldName = pmod.getProp(PROP_EMAIL_TO_FIELD_NAME, "").toString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> ds = new HashMap<>((HashMap<String, String>) pmod
        .getProp(SetFormValue.PROP_DATASET_EXPORT, new HashMap<String, String>()));
    String to = ds.get(fieldName);
    PrintModels.setStage(pmod, L.m("Sende an %1", to));
    if (!isMailAddress(to))
    {
      boolean res = InfoDialog.showCancelModal("ungültige Empfängeradresse", L.m(
          "Die Empfängeradresse '%1' ist ungültig!\n\nDiesen Datensatz überspringen und fortsetzen?",
          to));
      if (res)
      {
        pmod.cancel();
      }
      return;
    }

    String subject = pmod.getProp(PROP_EMAIL_SUBJECT, L.m("<kein Betreff>")).toString();
    String message = (String) pmod.getProp(PROP_EMAIL_MESSAGE_TEXTTAGS, null);

    try
    {
      EMailSender mail = new EMailSender();
      mail.createNewMultipartMail(from, to, replaceMergeFieldInText(ds, subject),
          replaceMergeFieldInText(ds, message));

      MailServerSettings smtpSettings = getMailServerSettings(pmod, mail);

      File document = saveOutputFile(createTempDocument(pmod, isODT), pmod.getTextDocument());

      sendMail(mail, smtpSettings, document);

      // Initialize properties if not already set.
      if (!pmod.getPropertySetInfo().hasPropertyByName(PROP_EMAIL_REPORT_RECIPIENT_LIST))
      {
        pmod.setPropertyValue(PROP_EMAIL_REPORT_RECIPIENT_LIST, new ArrayList<String>());
      }

      if (!pmod.getPropertySetInfo().hasPropertyByName(PROP_EMAIL_REPORT_EMAILS_SENT_COUNT))
      {
        pmod.setPropertyValue(PROP_EMAIL_REPORT_EMAILS_SENT_COUNT, 0);
      }

      @SuppressWarnings("unchecked")
      List<String> reportRecipientList = (List<String>) pmod
          .getPropertyValue(PROP_EMAIL_REPORT_RECIPIENT_LIST);
      int mailsSentCount = (int) pmod.getPropertyValue(PROP_EMAIL_REPORT_EMAILS_SENT_COUNT);

      if (reportRecipientList == null)
        reportRecipientList = new ArrayList<>();

      reportRecipientList.add(to);
      mailsSentCount++;

      pmod.setPropertyValue(PROP_EMAIL_REPORT_RECIPIENT_LIST, reportRecipientList);
      pmod.setPropertyValue(PROP_EMAIL_REPORT_EMAILS_SENT_COUNT, mailsSentCount);
    } catch (ConfigurationErrorException e)
    {
      LOGGER.error("Kein Mailserver", e);
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
          "Es konnten keine Angaben zum Mailserver gefunden werden - eventuell ist die WollMux-Konfiguration "
              + "nicht vollständig."));
      pmod.cancel();
    } catch (MessagingException e)
    {
      LOGGER.error("Email versenden fehlgeschlagen", e);
      InfoDialog.showInfoModal(MAIL_ERROR_MESSAGE_TITLE,
          L.m("Der Versand der E-Mail ist fehlgeschlagen."));
      pmod.cancel();
    } catch (Exception e)
    {
      LOGGER.error("", e);
      pmod.cancel();
    }
  }

  /**
   * Initialize the SMTP settings. There are three possibilities to get the settings
   * <ol>
   * <li>from the property {@link #PROP_EMAIL_MAIL_SERVER_SETTINGS}</li>
   * <li>from the {@link EMailSender} mail</li>
   * <li>from an authentication dialog</li>
   * </ol>
   *
   * @param pmod
   *          The {@link XPrintModel}
   * @param mail
   *          The {@link EMailSender}
   * @return {@link MailServerSettings} for this mailmerge
   * @throws PrintException
   *           Could not set the mail server settings.
   */
  private MailServerSettings getMailServerSettings(XPrintModel pmod, EMailSender mail)
      throws PrintException
  {
    MailServerSettings smtpSettings = (MailServerSettings) pmod
        .getProp(PROP_EMAIL_MAIL_SERVER_SETTINGS, new MailServerSettings());

    if (smtpSettings.getMailserver() == null)
    {
      mail.getWollMuxMailServerSettings(smtpSettings);
      try
      {
        pmod.setPropertyValue(PROP_EMAIL_MAIL_SERVER_SETTINGS, smtpSettings);
      } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
          | WrappedTargetException e)
      {
        throw new PrintException("Could not set mail server settings", e);
      }
    }

    BiConsumer<String, String> authDialogListener = (String username, String password) -> {
      if (username == null || password == null)
        return;

      smtpSettings.setUsername(username);
      smtpSettings.setPassword(password);
      try
      {
        pmod.setPropertyValue(PROP_EMAIL_MAIL_SERVER_SETTINGS, smtpSettings);
      } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
          | WrappedTargetException e)
      {
        LOGGER.error("Could not set mail server settings", e);
      }
    };

    if ((smtpSettings.getUsername() != null && !smtpSettings.getUsername().isEmpty())
        && (smtpSettings.getPassword() == null || smtpSettings.getPassword().isEmpty()))
    {
      AuthenticationDialog.show(smtpSettings.getUsername(), authDialogListener);
    }
    return smtpSettings;
  }

  /**
   * Test if mail is a valid mail address.
   *
   * @param mail
   *          String to test for a valid mail.
   * @return true, if it's a valid mail, false otherwise.
   */
  private boolean isMailAddress(String mail)
  {
    try
    {
      new InternetAddress(mail).validate();
      return true;
    } catch (AddressException ex)
    {
      LOGGER.debug("invalid sender mail address", ex);
    }
    return false;
  }

  /**
   * Send a document by mail.
   *
   * @param mail
   *          The email, to which the document is attached, and than send.
   * @param smtpSettings
   *          The settings of the mail server.
   * @param document
   *          The document to send.
   */
  private void sendMail(EMailSender mail, MailServerSettings smtpSettings, File document)
  {
    try
    {
      String path = document.getPath();
      if (!document.isAbsolute())
      {
        // fix path for windows systems
        path = path.replaceFirst("file:\\\\", "");
        // fix path for unix systems
        path = path.replaceFirst("file:", "");
      }
      mail.addAttachment(new File(path));
      mail.sendMessage(smtpSettings);
    } catch (ConfigurationErrorException | MessagingException | IOException e)
    {
      LOGGER.error("", e);
    } finally
    {
      try
      {
        Files.delete(document.toPath());
      } catch (IOException e)
      {
        LOGGER.trace("Couldn't delete temporary document", e);
      }
    }
  }
}
