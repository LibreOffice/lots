/*
 * Dateiname: TextfragmentNotDefinedException.java
 * Projekt  : WollMux
 * Funktion : Ein angefragtes Textfragment ist nicht definiert.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 08.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * Ein angefragtes Textfragment ist nicht definiert.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class TextFragmentNotDefinedException extends Exception
{
  private static final long serialVersionUID = -7265020323269743988L;

  public TextFragmentNotDefinedException(String id)
  {
    super("Das abgefragte Textfragment mit der FRAG_ID \""
          + id
          + "\" ist nicht definiert.");
  }
}
