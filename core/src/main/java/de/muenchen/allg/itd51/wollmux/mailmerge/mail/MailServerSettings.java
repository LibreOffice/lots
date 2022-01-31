/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

/**
 * Settings of the mail server used by mail merge.
 */
public class MailServerSettings
{
  private String mailserver;

  private Integer mailserverport;

  private String username;

  private String password;

  public String getMailserver()
  {
    return mailserver;
  }

  public void setMailserver(String mailserver)
  {
    this.mailserver = mailserver;
  }

  public Integer getMailserverport()
  {
    return mailserverport;
  }

  public void setMailserverport(String mailserverport)
  {
    this.mailserverport = Integer.parseInt(mailserverport);
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }
}
