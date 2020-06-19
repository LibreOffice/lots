package de.muenchen.allg.itd51.wollmux.core.document;

/**
 * Beim Der Interpretation der WollMux-Kommandos traten Fehler auf, die eine
 * Überprüfung des erzeugten Dokuments erforderlich machen.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WMCommandsFailedException extends Exception
{
  private static final long serialVersionUID = -2979607713420165796L;

  public WMCommandsFailedException(String msg)
  {
    super(msg);
  }
}
