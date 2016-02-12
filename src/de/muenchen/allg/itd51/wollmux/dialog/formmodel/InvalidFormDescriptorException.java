package de.muenchen.allg.itd51.wollmux.dialog.formmodel;

/**
 * Exception die eine ungültige Formularbeschreibung eines Formulardokuments
 * repräsentiert.
 * 
 * @author christoph.lutz
 */
public class InvalidFormDescriptorException extends Exception
{
  private static final long serialVersionUID = -4636262921405770907L;

  public InvalidFormDescriptorException(String message)
  {
    super(message);
  }
}