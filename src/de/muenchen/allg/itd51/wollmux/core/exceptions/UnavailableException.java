/*
* Dateiname: UnavailableException.java
* Projekt  : WollMux
* Funktion : Zeigt an, dass die gewünschte(n) Funktion(en)/Daten nicht verfügbar sind.
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
* 18.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.exceptions;

/**
 * Zeigt an, dass die gewünschte(n) Funktion(en)/Daten nicht verfügbar sind.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnavailableException extends Exception
{

  /**
   * Unterdrückt Warnung.
   */
  private static final long serialVersionUID = 5874615503838299278L;

  public UnavailableException()
  {
    super();
  }

  public UnavailableException(String message)
  {
    super(message);
  }

  public UnavailableException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public UnavailableException(Throwable cause)
  {
    super(cause);
  }

}
