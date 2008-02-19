//TODO L.m()
/*
 * Dateiname: EndlessLoopException.java
 * Projekt  : WollMux
 * Funktion : Bei einer Textersetzung (z.B. aus einer Variable oder beim insertFrag) 
 *            kam es zu einer Endlosschleife.
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
 * Bei einer Textersetzung (z.B. aus einer Variable oder beim insertFrag) kam es
 * zu einer Endlosschleife.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class EndlessLoopException extends Exception
{
  private static final long serialVersionUID = -3679814069994462633L;

  public EndlessLoopException(String msg)
  {
    super(msg);
  }
}
