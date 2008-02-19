//TODO L.m()
/*
 * Dateiname: WMCommandsFailedException.java
 * Projekt  : WollMux
 * Funktion : Beim Der Interpretation der WollMux-Kommandos traten
 *            Fehler auf, die eine Überprüfung des erzeugten Dokuments
 *            erforderlich machen. 
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

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
