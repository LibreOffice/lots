/*
 * Dateiname: LazyView.java
 * Projekt  : WollMux
 * Funktion : Eine View die erst wenn sie das erste mal angezeigt wird initialisiert wird.
 * 
 * Copyright (c) 2010-2019 Landeshauptstadt München
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
 * 22.01.2010 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias S. Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.view;

/**
 * Eine View die erst wenn sie das erste mal angezeigt wird initialisiert wird.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public interface LazyView extends View
{
  /**
   * Muss aufgerufen werden, wenn die View angezeigt wird. Dies veranlasst die View,
   * sich komplett zu initialisieren. Vor dem Aufruf dieser Funktion ist nicht
   * garantiert, dass in der View tatsächlich Inhalt angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void viewIsVisible();

  /**
   * Sollte (muss aber nicht) aufgerufen werden, wenn die View nicht mehr angezeigt
   * wird. Sie kann dann falls möglich Ressourcen freigeben.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void viewIsNotVisible();
}
