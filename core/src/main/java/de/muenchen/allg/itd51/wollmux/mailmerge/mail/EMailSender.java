/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.mailmerge.mail;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;

/**
 * Service for sending mails.
 */
public class EMailSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger(EMailSender.class);

  private Properties props;

  private Message email;

  private Session session;

  /**
   * Creates a new service.
   */
  public EMailSender()
  {
    props = new Properties();
    session = Session.getDefaultInstance(props);
    email = new MimeMessage(session);
  }

  /**
   * Create a new mail.
   *
   * @param from
   *          The sender.
   * @param to
   *          The recipient.
   * @param subject
   *          The subject.
   * @param message
   *          The body.
   * @throws MessagingException
   *           Can't create the mail.
   */
  public void createNewMultipartMail(String from, String to, String subject, String message) throws MessagingException
  {
    InternetAddress addressFrom = new InternetAddress(from);
    email.setFrom(addressFrom);
    InternetAddress addressTo = new InternetAddress(to);
    email.setRecipient(Message.RecipientType.TO, addressTo);
    email.setSubject(subject);

    Multipart multipart = new MimeMultipart();
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setText(message);
    multipart.addBodyPart(messageBodyPart);

    email.setContent(multipart);
  }

  /**
   * Add an attachment to the mail.
   *
   * @param attachment
   *          The attachment.
   * @throws MessagingException
   *           Problems with the mail message.
   * @throws IOException
   *           The attachment can't be found.
   */
  public void addAttachment(File attachment) throws MessagingException, IOException
  {
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.attachFile(attachment);
    ((Multipart) email.getContent()).addBodyPart(messageBodyPart);
  }

  /**
   * Send a message.
   *
   * @param mailServerSettings
   *          The mail server to use.
   * @throws MessagingException
   *           Problems while sending the mail.
   */
  public void sendMessage(MailServerSettings mailServerSettings) throws MessagingException
  {
    // Necessary for mapping MIME types to Java classes.
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try (Transport tr = session.getTransport("smtp");)
    {
      // Empty String activates AUTH so use null if no authentication is required.
      tr.connect(mailServerSettings.getMailserver(), mailServerSettings.getMailserverport(),
          mailServerSettings.getUsername(), mailServerSettings.getPassword());
      email.saveChanges();
      tr.sendMessage(email, email.getAllRecipients());
    }
    catch (MessagingException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Get the mail server settings from the configuration and set them.
   *
   * @param mailserver
   *          The object where the settings are set.
   * @throws ConfigurationErrorException
   *           No SERVER entry in the configuration.
   */
  public void getWollMuxMailServerSettings(MailServerSettings mailserver)
  {
    ConfigThingy wollmuxconf = WollMuxFiles.getWollmuxConf();

    try
    {
      wollmuxconf = wollmuxconf.query("MailSettings").getLastChild();
      mailserver.setMailserver(wollmuxconf.get("SERVER").toString());
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException();
    }

    try
    {
      if (wollmuxconf.query("PORT").count() != 1
          || wollmuxconf.getString("PORT", "").equals(""))
      {
        mailserver.setMailserverport("-1");
      }
      else
      {
        mailserver.setMailserverport(wollmuxconf.getString("PORT", ""));
      }

      if (!wollmuxconf.getString("AUTH_USER_PATTERN", "").isEmpty())
      {
        String username = email.getFrom()[0].toString();
        Pattern pattern = Pattern.compile(wollmuxconf.getString("AUTH_USER_PATTERN", ""));
        Matcher result = pattern.matcher(username);
        if (result.find())
        {
          username = result.group(1);
        } else
        {
          username = pattern.pattern();
        }

        mailserver.setUsername(username);
      }
    }
    catch (MessagingException e)
    {
      throw new ConfigurationErrorException();
    }
  }
}
