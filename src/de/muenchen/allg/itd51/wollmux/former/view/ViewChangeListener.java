/*
 * Dateiname: ViewChangeListener.java
 * Projekt  : WollMux
 * Funktion : Interface für Klassen, die an Änderungen einer View interessiert sind. 
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 28.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.view;

/**
 * Interface für Klassen, die an Änderungen einer View interessiert sind.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface ViewChangeListener
{
  /**
   * Wird aufgerufen, wenn alle Referenzen auf die View view entfernt werden sollten,
   * weil die view ungültig geworden ist (typischerweise weil das zugrundeliegende
   * Model nicht mehr da ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void viewShouldBeRemoved(View view);

}
