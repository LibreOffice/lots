/*
* Dateiname: NoBackingStoreException.java
* Projekt  : WollMux
* Funktion : Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
* nicht existiert.
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
* 14.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
 * nicht existiert.
 *  
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class NoBackingStoreException extends Exception
{
  private static final long serialVersionUID = -1672676873427003242L;
  public NoBackingStoreException() {super();}
  public NoBackingStoreException(String message) {super(message);}
  public NoBackingStoreException(String message, Throwable cause) {super(message,cause);}
  public NoBackingStoreException(Throwable cause) {super(cause);}
}
