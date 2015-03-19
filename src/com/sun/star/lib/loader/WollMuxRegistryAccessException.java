/*
 * Dateiname: WollMuxRegistryAccessException.java
 * Projekt  : WollMux
 * Funktion : Signalisiert, dass ein Fehler beim Lesen von WollMux-Config-Daten
 *            aus der Windows Registry aufgetreten ist
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 14.08.2009 | BED | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 * 
 */
package com.sun.star.lib.loader;

/**
 * Signalisiert, dass ein Fehler beim Lesen von WollMux-Config-Daten aus der Windows
 * Registry aufgetreten ist
 * 
 * @author Daniel Benkmann (D-III-ITD-D101)
 */
public class WollMuxRegistryAccessException extends Exception
{
  private static final long serialVersionUID = 1L;

  public WollMuxRegistryAccessException()
  {};

  public WollMuxRegistryAccessException(String message)
  {
    super(message);
  }

  public WollMuxRegistryAccessException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public WollMuxRegistryAccessException(Throwable cause)
  {
    super(cause);
  }
}
