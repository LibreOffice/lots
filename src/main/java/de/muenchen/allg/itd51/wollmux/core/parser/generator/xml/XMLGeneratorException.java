package de.muenchen.allg.itd51.wollmux.core.parser.generator.xml;

/**
 * A Exception produced by the generator.
 * 
 * @author Daniel Sikeler
 */
public class XMLGeneratorException extends Exception
{

  /** Serial version ID. */
  private static final long serialVersionUID = 6474901851185961604L;

  /**
   * Constructor for generator exceptions.
   * 
   * @param message
   *          The description of the exception.
   */
  public XMLGeneratorException(final String message)
  {
    super(message);
  }

  /**
   * Constructor for generator exceptions.
   * 
   * @param message
   *          Description of the exception.
   * @param cause
   *          The exception which caused this one.
   */
  public XMLGeneratorException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

}
