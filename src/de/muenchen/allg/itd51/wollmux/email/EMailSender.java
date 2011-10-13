/* 
 * Dateiname: EMailSender.java
 * Projekt  : WollMux
 * Funktion : Teil des E-Mail-Wrappers für javamail
 * 
 * Copyright (c) 2011 Landeshauptstadt München
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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;

public class EMailSender
{
  private Properties props;

  private Message email;

  public EMailSender() throws IncompleteMailserverConfigException
  {
    MailServerSettings mailserver = getWollMuxMailServerSettings();
    props = new Properties();
    props.put("mail.smtp.host", mailserver.getMailserver());
    Session session = Session.getDefaultInstance(props);
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
    DataSource dataSource = new FileDataSource(attachment);
    messageBodyPart.setDataHandler(new DataHandler(dataSource));
    messageBodyPart.setFileName(attachment.getName());
    ((Multipart) email.getContent()).addBodyPart(messageBodyPart);
  }

  public void sendMessage() throws MessagingException
  {
    Workarounds.applyWorkaroundForOOoIssue102164();
    Transport.send(email);
  }

  private MailServerSettings getWollMuxMailServerSettings()
      throws IncompleteMailserverConfigException
  {
    MailServerSettings mailserver = new MailServerSettings();
    ConfigThingy wollmuxconf = WollMuxFiles.getWollmuxConf();
    try
    {
      wollmuxconf = wollmuxconf.query("EMailEinstellungen").getLastChild();
      mailserver.setMailserver(wollmuxconf.get("SERVER").toString());
      mailserver.setMailserverport(new Integer(wollmuxconf.get("PORT").toString()));
    }
    catch (NodeNotFoundException e)
    {
      throw new IncompleteMailserverConfigException();
    }
    return mailserver;
  }
}
