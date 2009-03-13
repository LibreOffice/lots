/*
 * Dateiname: BroadcastListener.java
 * Projekt  : WollMux
 * Funktion : Abstrakte Basisklasse für Horcher auf dem globalen Broadcast-Kanal.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 04.09.2006 | BNK | Erstellung
 * 16.03.2007 | BNK | +broadcastNewFormControlId()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Set;

/**
 * Abstrakte Basisklasse für Horcher auf dem globalen Broadcast-Kanal. Der globale
 * Broadcast-Kanal wird für Nachrichten verwendet, die verschiedene permanente
 * Objekte erreichen müssen, die aber von (transienten) Objekten ausgehen, die mit
 * diesen globalen Objekten wegen des Ausuferns der Verbindungen nicht in einer
 * Beziehung stehen sollen. BroadcastListener dürfen nur permanente Objekte sein,
 * d.h. Objekte deren Lebensdauer nicht vor Beenden des FM4000 endet.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class BroadcastListener
{
  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.control.FormControlModel}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.group.GroupModel}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastGroupModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * Eine View die Views aller InsertionModel enthält wurde ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastAllInsertionsViewSelected()
  {}

  /**
   * Eine View die Views aller FormControlModels enthält wurde ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastAllFormControlsViewSelected()
  {}

  /**
   * Eine View die Views aller GroupModels enthält wurde ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastAllGroupsViewSelected()
  {}

  /**
   * Eine Menge von Bookmarks wurde ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastBookmarkSelection(Set<String> bookmarkNames)
  {}

  /**
   * Der {@link ViewVisibilityDescriptor} hat sich geändert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void broadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
  {};
}
