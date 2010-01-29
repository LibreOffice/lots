/*
 * Dateiname: BroadcastObjectSelection.java
 * Projekt  : WollMux
 * Funktion : Nachricht, dass in einer View ein Objekt ausgewählt wurde.
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
 * 29.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Nachricht, dass in einer View ein Object ausgewählt wurde. Diese Nachricht wird
 * von anderen Views ausgewertet, um ihre Selektionen ebenfalls anzupassen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class BroadcastObjectSelection implements Broadcast
{
  /**
   * das {@link Object} das ausgewählt wurde.
   */
  private Object myObject;

  /**
   * -1 => abwählen, 1 => anwählen, 0: toggle.
   */
  private int state;

  /**
   * true => Selektion erst ganz löschen vor an/abwählen des Objektes.
   */
  private boolean clearSelection;

  /**
   * Erzeugt eine neue Nachricht.
   * 
   * @param myObject
   *          das {@link Object} das ausgewählt wurde.
   * @param state
   *          -1 => abwählen, 1 => anwählen, 0: toggle
   * @param clearSelection
   *          true => Selektion erst ganz löschen vor an/abwählen von myObject.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public BroadcastObjectSelection(Object model, int state, boolean clearSelection)
  {
    this.myObject = model;
    this.state = state;
    this.clearSelection = clearSelection;
  }

  /**
   * Liefert -1 für abwählen, 1 für auswählen, 0 für toggle.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getState()
  {
    return state;
  }

  /**
   * true => Selektion erst ganz löschen vor an/abwählen des Objekts.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getClearSelection()
  {
    return clearSelection;
  }

  /**
   * Liefert das Objekt zurück, das de/selektiert wurde.
   */
  public Object getObject()
  {
    return myObject;
  }

}
