/*
 * Dateiname: GroupModelList.java
 * Projekt  : WollMux
 * Funktion : Verwaltet eine Liste von GroupModels.
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
 * 15.11.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.former.group;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;

/**
 * Verwaltet eine Liste von GroupModels
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class GroupModelList implements Iterable<GroupModel>
{
  /**
   * Die Liste der {@link GroupModel}s.
   */
  private List<GroupModel> models = new LinkedList<GroupModel>();

  /**
   * Liste aller {@link ItemListener}, die über Änderungen des Listeninhalts
   * informiert werden wollen.
   */
  private List<ItemListener> listeners = new ArrayList<ItemListener>(1);

  /**
   * Der FormularMax4000 zu dem diese GroupModelList gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Erzeugt eine neue GroupModelList.
   * 
   * @param formularMax4000
   *          der FormularMax4000 zu dem diese Liste gehört.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModelList(FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    // this.formularMax4000.addBroadcastListener(new MyBroadcastListener());
  }

  /**
   * Fügt model dieser Liste hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(GroupModel model)
  {
    int idx = models.size();
    models.add(idx, model);
    notifyListeners(model, idx, false);
  }

  /**
   * Löscht alle bestehenden GroupModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      GroupModel model = models.remove(index);
      model.hasBeenRemoved();
      notifyListeners(model, index, true);
    }
  }

  /**
   * Liefert true gdw keine GroupModels in der Liste vorhanden sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isEmpty()
  {
    return models.isEmpty();
  }

  /**
   * Liefert ein ConfigThingy, dessen Wurzel ein "Sichtbarkeit"-Knoten ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    ConfigThingy conf = new ConfigThingy("Sichtbarkeit");
    Iterator<GroupModel> iter = models.iterator();
    while (iter.hasNext())
    {
      GroupModel model = iter.next();
      conf.addChild(model.export());
    }
    return conf;
  }

  /**
   * Bittet die GroupModelList darum, das Element model aus sich zu entfernen (falls
   * es in der Liste ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void remove(GroupModel model)
  {
    int index = models.indexOf(model);
    if (index < 0) return;
    models.remove(index);
    model.hasBeenRemoved();
    notifyListeners(model, index, true);
  }

  /**
   * Liefert einen Iterator über alle Models dieser Liste.
   */
  @Override
  public Iterator<GroupModel> iterator()
  {
    return models.iterator();
  }

  /**
   * listener wird über Änderungen der Liste informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * listener wird in Zukunft nicht mehr über Änderungen der Liste informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void removeListener(ItemListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Benachrichtigt alle ItemListener über das Hinzufügen oder Entfernen von model
   * zur bzw. aus der Liste an/von Index index.
   * 
   * @param removed
   *          falls true, wurde model entfernt, ansonsten hinzugefügt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(GroupModel model, int index, boolean removed)
  {
    Iterator<ItemListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = iter.next();
      if (removed)
        listener.itemRemoved(model, index);
      else
        listener.itemAdded(model, index);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Interface für Klassen, die interessiert sind, zu erfahren, wenn sich die Liste
   * ändert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ItemListener
  {
    /**
     * Wird aufgerufen nachdem model zur Liste hinzugefügt wurde (an Index index).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemAdded(GroupModel model, int index);

    /**
     * Wird aufgerufen, nachdem model aus der Liste entfernt wurde.
     * 
     * @param index
     *          der alte Index von model in der Liste.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemRemoved(GroupModel model, int index);
  }

  /*
   * private class MyBroadcastListener extends BroadcastListener {}
   */

}
