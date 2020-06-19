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
