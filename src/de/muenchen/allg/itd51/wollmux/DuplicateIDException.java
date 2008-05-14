/*
* Dateiname: DuplicateIDException.java
* Projekt  : WollMux
* Funktion : Wird geworfen, wenn versucht wird, die ID eines Elementes auf einen bereits von einem anderen Element verwendeten Wert zu ändern.
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330/5980
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.07.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Wird geworfen, wenn versucht wird, die ID eines Elementes auf einen bereits von
 * einem anderen Element verwendeten Wert zu ändern.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DuplicateIDException extends Exception
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 4349143792168156649L;
  public DuplicateIDException() {};
  public DuplicateIDException(String message) {super(message);}
  public DuplicateIDException(String message, Throwable cause) {super(message,cause);}
  public DuplicateIDException(Throwable cause) {super(cause);}
}
