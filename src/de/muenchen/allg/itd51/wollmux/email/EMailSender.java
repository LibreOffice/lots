/* 
 * Dateiname: EMailSender.java
 * Projekt  : WollMux
 * Funktion : Teil des E-Mail-Wrappers für javamail
 * 
 * Copyright (c) 2011-2015 Landeshauptstadt München
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class EMailSender
{
  private Properties props;

  private Message email;

  private Session session;

  static private MailServerSettings mailserver = null;

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
    DataSource dataSource = new FileDataSource(attachment);
    messageBodyPart.setDataHandler(new DataHandler(dataSource));
    messageBodyPart.setFileName(attachment.getName());
    ((Multipart) email.getContent()).addBodyPart(messageBodyPart);
  }

  public void sendMessage() throws ConfigurationErrorException,
      MessagingException
  {

    if (mailserver == null)
    {
      mailserver = getWollMuxMailServerSettings();
    }
    try
    {
      Workarounds.applyWorkaroundForOOoIssue102164();
      Transport tr = session.getTransport("smtp");
      Workarounds.applyWorkaroundForOOoIssue102164();
      tr.connect(mailserver.getMailserver(), mailserver.getMailserverport(),
        mailserver.getUsername(), mailserver.getPassword());
      Workarounds.applyWorkaroundForOOoIssue102164();
      email.saveChanges();

      Workarounds.applyWorkaroundForOOoIssue102164();
      tr.sendMessage(email, email.getAllRecipients());
    }
    catch (MessagingException e)
    {
      mailserver = null;
      throw new MessagingException(e.getMessage(), e);
    }
  }

  private MailServerSettings getWollMuxMailServerSettings()
      throws ConfigurationErrorException
  {
    MailServerSettings mailserver = new MailServerSettings();
    ConfigThingy wollmuxconf = WollMuxFiles.getWollmuxConf();
    try
    {
      wollmuxconf = wollmuxconf.query("EMailEinstellungen").getLastChild();
      mailserver.setMailserver(wollmuxconf.get("SERVER").toString());
    }
    catch (Exception e)
    {
      throw new ConfigurationErrorException();
    }

    try
    {
      if (wollmuxconf.query("PORT").count() != 1
        || wollmuxconf.get("PORT").toString().equals(""))
        mailserver.setMailserverport("-1");
      else
        mailserver.setMailserverport(wollmuxconf.get("PORT").toString());

      if (wollmuxconf.query("AUTH_USER_PATTERN").count() == 1
        && !wollmuxconf.get("AUTH_USER_PATTERN").toString().equals(""))
      {
        String username = email.getFrom()[0].toString();
        Pattern pattern =
          Pattern.compile(wollmuxconf.get("AUTH_USER_PATTERN").toString());
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

        JLabel jUserName = new JLabel(L.m("Benutzername"));
        JTextField userNameField = new JTextField(username);
        JLabel jPassword = new JLabel(L.m("Passwort"));
        JTextField passwordField = new JPasswordField();
        String jAuthPrompt =
          new String(
            L.m("Bitte geben Sie Benutzername und Passwort für den E-Mail-Server ein."));
        Object[] dialogElements = {
          jAuthPrompt, jUserName, userNameField, jPassword, passwordField };
        int dialog =
          JOptionPane.showConfirmDialog(null, dialogElements,
            L.m("Authentifizierung am E-Mail-Server"), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (dialog == JOptionPane.OK_OPTION)
        {
          mailserver.setUsername(userNameField.getText());
          mailserver.setPassword(passwordField.getText());
        }
      }
    }
    catch (NodeNotFoundException e)
    {}
    catch (Exception e)
    {
      throw new ConfigurationErrorException();
    }
    return mailserver;
  }
}
