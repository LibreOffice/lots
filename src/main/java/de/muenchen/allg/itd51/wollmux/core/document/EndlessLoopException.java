/*
 * Dateiname: EndlessLoopException.java
 * Projekt  : WollMux
 * Funktion : Bei einer Textersetzung (z.B. aus einer Variable oder beim insertFrag) 
 *            kam es zu einer Endlosschleife.
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
 * 08.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.document;

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
