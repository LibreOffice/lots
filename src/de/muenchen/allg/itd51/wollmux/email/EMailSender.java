/* 
 * Dateiname: EMailSender.java
 * Projekt  : WollMux
 * Funktion : Teil des E-Mail-Wrappers für javamail
 * 
 * Copyright (c) 2011-2019 Landeshauptstadt München
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

  public MailServerSettings getWollMuxMailServerSettings()
      throws ConfigurationErrorException
  { 
    MailServerSettings mailserver = new MailServerSettings();
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
    
    return mailserver;
  }
}
