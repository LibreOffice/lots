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
package de.muenchen.allg.itd51.wollmux.email;

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
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;

public class EMailSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger(EMailSender.class);

  private Properties props;

  private Message email;

  private Session session;

  public EMailSender()
  {
    props = new Properties();
    session = Session.getDefaultInstance(props);
    email = new MimeMessage(session);
  }

  public void createNewMultipartMail(String from, String to, String subject,
      String message) throws MessagingException
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

  public void addAttachment(File attachment) throws MessagingException, IOException
  {
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.attachFile(attachment);
    ((Multipart) email.getContent()).addBodyPart(messageBodyPart);
  }

  public void sendMessage(MailServerSettings mailServerSettings) throws ConfigurationErrorException,
      MessagingException
  {
    try
    {
      // Notwendig um MIME Types auf Java-Klassen zu mappen.
      // Manchmal funktioniert der ClassLoader nicht richtig
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      Transport tr = session.getTransport("smtp");
      // FYI: falls getUsername() || getPassword() = "" muss NULL übergeben werden,
      // auch bei "" glaubt javamail AUTH aktivieren zu müssen was zu einer Auth-Exception führt.
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
      wollmuxconf = wollmuxconf.query("EMailEinstellungen").getLastChild();
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

      if (wollmuxconf.query("AUTH_USER_PATTERN").count() == 1
          && !wollmuxconf.getString("AUTH_USER_PATTERN", "").equals(""))
      {
        String username = email.getFrom()[0].toString();
        Pattern pattern =
            Pattern.compile(wollmuxconf.getString("AUTH_USER_PATTERN", ""));
        try
        {
          Matcher result = pattern.matcher(username);
          result.find();
          username = result.group(1);
        }
        catch (IllegalStateException e)
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
