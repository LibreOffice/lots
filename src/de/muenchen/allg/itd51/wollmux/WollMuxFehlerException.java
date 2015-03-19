/*
 * Dateiname: WollMuxFehlerException.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert einen Fehler, der benutzersichtbar in einem Fehlerdialog angezeigt wird. 
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
 */

package de.muenchen.allg.itd51.wollmux;

/**
 * Repräsentiert einen Fehler, der benutzersichtbar in einem Fehlerdialog angezeigt
 * wird.
 * 
 * @author christoph.lutz
 */
public class WollMuxFehlerException extends java.lang.Exception
{
  private static final long serialVersionUID = 3618646713098791791L;

  public WollMuxFehlerException(String msg)
  {
    super(msg);
  }

  public WollMuxFehlerException(String msg, java.lang.Exception e)
  {
    super(msg, e);
  }
}
