/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.former;

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
public interface BroadcastListener
{
  /**
   * getObject() ist ein
   * {@link org.libreoffice.lots.former.control.model.FormControlModel}.
   */
  public default void broadcastFormControlModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link org.libreoffice.lots.former.insertion.model.InsertionModel}.
   */
  public default void broadcastInsertionModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link org.libreoffice.lots.former.group.model.GroupModel}.
   */
  public default void broadcastGroupModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link org.libreoffice.lots.former.section.model.SectionModel}.
   */
  public default void broadcastSectionModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * Eine View die Views aller InsertionModel enthält wurde ausgewählt.
   */
  public default void broadcastAllInsertionsViewSelected()
  {}

  /**
   * Eine View die Views aller FormControlModels enthält wurde ausgewählt.
   */
  public default void broadcastAllFormControlsViewSelected()
  {}

  /**
   * Eine View die Views aller GroupModels enthält wurde ausgewählt.
   */
  public default void broadcastAllGroupsViewSelected()
  {}

  /**
   * Eine View die Views aller SectionModels enthält wurde ausgewählt.
   */
  public default void broadcastAllSectionsViewSelected()
  {}

  /**
   * Eine Menge von Bookmarks wurde ausgewählt.
   */
  public default void broadcastBookmarkSelection(Set<String> bookmarkNames)
  {}

  /**
   * Der {@link ViewVisibilityDescriptor} hat sich geändert.
   */
  public default void broadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
  {
  }
}
