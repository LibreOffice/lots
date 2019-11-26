/*
* Dateiname: ConfigurationErrorException.java
* Projekt  : WollMux
* Funktion : wird geworfen, wenn eine Fehlkonfiguration festgestellt wird (d.h. Benutzer hat Config verbockt)
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
* 11.10.2005 | BNK | Erstellung
* 13.10.2005 | BNK | +serialVersionUID
* 14.10.2005 | BNK | keine RuntimeException mehr
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
*
*/
package de.muenchen.allg.itd51.wollmux.core.parser;

/**
 * wird geworfen, wenn eine Fehlkonfiguration festgestellt wird (d.h. Benutzer hat Config verbockt)
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigurationErrorException extends RuntimeException
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = -2457549809413613658L;
  public ConfigurationErrorException() {super();}
  public ConfigurationErrorException(String message) {super(message);}
  public ConfigurationErrorException(String message, Throwable cause) {super(message,cause);}
  public ConfigurationErrorException(Throwable cause) {super(cause);}
}
