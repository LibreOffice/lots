/*
 * Dateiname: TrafoDialogFactory.java
 * Projekt  : WollMux
 * Funktion : Erzeugt zu einem Satz von Parametern einen passenden TrafoDialog.
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
 * 01.02.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.lang.reflect.Constructor;

import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Erzeugt zu einem Satz von Parametern einen passenden TrafoDialog.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TrafoDialogFactory
{
  private static final Class<?>[] dialogClasses = {
    GenderDialog.class, IfThenElseDialog.class };

  private static final Class<?>[] DIALOG_CONSTRUCTOR_SIGNATURE =
    { TrafoDialogParameters.class };

  /**
   * Versucht, einen zu params passenden Dialog zu instanziieren und liefert ihn
   * zurück, falls es klappt.
   * 
   * @param params
   *          spezifiziert die Informationen, die der Dialog braucht und bestimmt,
   *          was für ein Dialog angezeigt wird. ACHTUNG! Das Objekt darf nach dem
   *          Aufruf dieser Methode nicht mehr verändert werden, da der Dialog es
   *          evtl. permanent speichert und für seine Arbeit verwendet.
   * 
   * @throws UnavailableException
   *           wenn kein passender Dialog gefunden wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static TrafoDialog createDialog(TrafoDialogParameters params)
      throws UnavailableException
  {
    Object[] oparams = { params };
    for (int i = 0; i < dialogClasses.length; ++i)
    {
      try
      {
        Constructor<?> cons =
          dialogClasses[i].getConstructor(DIALOG_CONSTRUCTOR_SIGNATURE);
        return (TrafoDialog) cons.newInstance(oparams);
      }
      catch (Exception x)
      {}
      ;
    }

    throw new UnavailableException(L.m(
      "Kein Dialog verfügbar für die übergebenen Parameter: %1", params));
  }

}
