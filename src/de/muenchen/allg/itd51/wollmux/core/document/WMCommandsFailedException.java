/*
 * Dateiname: WMCommandsFailedException.java
 * Projekt  : WollMux
 * Funktion : Beim Der Interpretation der WollMux-Kommandos traten
 *            Fehler auf, die eine Überprüfung des erzeugten Dokuments
 *            erforderlich machen. 
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
