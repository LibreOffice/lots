package de.muenchen.allg.itd51.wollmux.core.parser;

/**
 * wird geworfen, wenn eine Fehlkonfiguration festgestellt wird (d.h. Benutzer hat Config verbockt)
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigurationErrorException extends RuntimeException
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = -2457549809413613658L;
  public ConfigurationErrorException() {super();}
  public ConfigurationErrorException(String message) {super(message);}
  public ConfigurationErrorException(String message, Throwable cause) {super(message,cause);}
  public ConfigurationErrorException(Throwable cause) {super(cause);}
}
