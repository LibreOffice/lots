/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.group;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
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
  private List<GroupModel> models = new LinkedList<>();

  /**
   * Liste aller {@link ItemListener}, die über Änderungen des Listeninhalts
   * informiert werden wollen.
   */
  private List<ItemListener> listeners = new ArrayList<>(1);

  /**
   * Der FormularMax4000 zu dem diese GroupModelList gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Erzeugt eine neue GroupModelList.
   *
   * @param formularMax4000
   *          der FormularMax4000 zu dem diese Liste gehört.
   */
  public GroupModelList(FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }

  /**
   * Fügt model dieser Liste hinzu.
   */
  public void add(GroupModel model)
  {
    int idx = models.size();
    models.add(idx, model);
    notifyListeners(model, idx, false);
  }

  /**
   * Löscht alle bestehenden GroupModels aus der Liste.
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
   */
  public boolean isEmpty()
  {
    return models.isEmpty();
  }

  /**
   * Liefert ein ConfigThingy, dessen Wurzel ein "Sichtbarkeit"-Knoten ist.
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
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * listener wird in Zukunft nicht mehr über Änderungen der Liste informiert.
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
     */
    public void itemAdded(GroupModel model, int index);

    /**
     * Wird aufgerufen, nachdem model aus der Liste entfernt wurde.
     *
     * @param index
     *          der alte Index von model in der Liste.
     */
    public void itemRemoved(GroupModel model, int index);
  }

}
