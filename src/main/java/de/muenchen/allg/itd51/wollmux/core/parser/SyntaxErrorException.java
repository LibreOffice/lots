/*
* Dateiname: SyntaxErrorException.java
* Projekt  : WollMux
* Funktion : Signalisiert einen Fehler in einer zu parsenden Zeichenfolge 
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
* 06.10.2005 | BNK | Erstellung
* 11.10.2005 | BNK | Doku
* 13.10.2005 | BNK | +serialVersionUID
* 14.10.2005 | BNK | +Projekt: WollMux
* 14.10.2005 | BNK | SyntaxErrorException ist keine RuntimeException mehr.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.parser;

/**
 * Signalisiert einen Fehler in einer zu parsenden Zeichenfolge
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SyntaxErrorException extends Exception
{
  /**
   * keine Ahnung was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 7215084024054862356L;
  public SyntaxErrorException() {super();}
  public SyntaxErrorException(String message) {super(message);}
  public SyntaxErrorException(String message, Throwable cause) {super(message,cause);}
  public SyntaxErrorException(Throwable cause) {super(cause);}
}
