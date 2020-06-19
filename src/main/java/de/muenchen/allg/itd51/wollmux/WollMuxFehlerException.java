package de.muenchen.allg.itd51.wollmux;

/**
 * Repräsentiert einen Fehler, der benutzersichtbar in einem Fehlerdialog angezeigt
 * wird.
 * 
 * @author christoph.lutz
 */
public class WollMuxFehlerException extends java.lang.Exception
{
  private static final long serialVersionUID = 3618646713098791791L;

  public WollMuxFehlerException(String msg)
  {
    super(msg);
  }

  public WollMuxFehlerException(String msg, java.lang.Exception e)
  {
    super(msg, e);
  }
}
