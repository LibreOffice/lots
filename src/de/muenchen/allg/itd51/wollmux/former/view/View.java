/*
 * Dateiname: View.java
 * Projekt  : WollMux
 * Funktion : Über-Interface für alle Views im FormularMax 4000
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 29.08.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.view;

import javax.swing.JComponent;

/**
 * Über-Interface für alle Views im FormularMax 4000.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface View
{
  /**
   * Liefert die Komponente für diese View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public JComponent getComponent();
}
