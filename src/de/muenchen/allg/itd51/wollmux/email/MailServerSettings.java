/* 
 * Dateiname: MailServerSettings.java
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
    this.mailserverport = new Integer(mailserverport);
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
