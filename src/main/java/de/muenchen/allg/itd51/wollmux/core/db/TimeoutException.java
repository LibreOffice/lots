/*
* Dateiname: TimeoutException.java
* Projekt  : WollMux
* Funktion : wird geworfen, wenn etwas nicht in einem gegebenen Zeitrahmen getan werden konnte.
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
* 28.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * wird geworfen, wenn etwas nicht in einem gegebenen Zeitrahmen getan werden konnte.
 */
public class TimeoutException extends Exception
{
  private static final long serialVersionUID = -2334582583309727084L;
  public TimeoutException() {super();}
  public TimeoutException(String message) {super(message);}
  public TimeoutException(String message, Throwable cause) {super(message,cause);}
  public TimeoutException(Throwable cause) {super(cause);}
}
