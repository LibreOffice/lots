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
   */
  public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel}.
   */
  public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.group.GroupModel}.
   */
  public void broadcastGroupModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * getObject() ist ein
   * {@link de.muenchen.allg.itd51.wollmux.former.section.SectionModel}.
   */
  public void broadcastSectionModelSelection(BroadcastObjectSelection b)
  {}

  /**
   * Eine View die Views aller InsertionModel enthält wurde ausgewählt.
   */
  public void broadcastAllInsertionsViewSelected()
  {}

  /**
   * Eine View die Views aller FormControlModels enthält wurde ausgewählt.
   */
  public void broadcastAllFormControlsViewSelected()
  {}

  /**
   * Eine View die Views aller GroupModels enthält wurde ausgewählt.
   */
  public void broadcastAllGroupsViewSelected()
  {}

  /**
   * Eine View die Views aller SectionModels enthält wurde ausgewählt.
   */
  public void broadcastAllSectionsViewSelected()
  {}

  /**
   * Eine Menge von Bookmarks wurde ausgewählt.
   */
  public void broadcastBookmarkSelection(Set<String> bookmarkNames)
  {}

  /**
   * Der {@link ViewVisibilityDescriptor} hat sich geändert.
   */
  public void broadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
  {
  }
}
