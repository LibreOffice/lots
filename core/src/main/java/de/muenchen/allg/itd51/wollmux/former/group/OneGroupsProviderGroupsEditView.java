/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.awt.GridLayout;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel.IDChangeListener;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Lässt die Liste der Gruppen eines {@link GroupsProvider} bearbeiten.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OneGroupsProviderGroupsEditView implements View
{
  /**
   * Der Obercontainer dieser View.
   */
  private JPanel myPanel;

  /**
   * Die Liste mit allen Gruppen aus {@link #groupModelList}, wobei die Gruppen aus
   * {@link #groupsProvider} selektiert sind.
   */
  private JList<IdModel> myList;

  /**
   * Das {@link ListModel} zu {@link #myList}.
   */
  private DefaultListModel<IdModel> listModel;

  /**
   * Wessen Gruppen werden angezeigt und bearbeitet.
   */
  private GroupsProvider groupsProvider;

  /**
   * Welche Gruppen stehen zur Auswahl.
   */
  private GroupModelList groupModelList;

  /**
   * Listener auf model und groupModelList, der Hinzufügen und entfernen von Gruppen
   * beobachtet.
   */
  private MyListener myListener;

  /**
   * Wird temporär auf true gesetzt, während einer Aktion die rekursives Aufrufen des
   * Listeners erzeugen könnte, um diese Rekursion zu durchbrechen.
   */
  boolean recursion = false;

  public OneGroupsProviderGroupsEditView(GroupsProvider groupsProvider,
      GroupModelList groupModelList)
  {
    this.groupsProvider = groupsProvider;
    myListener = new MyListener();
    groupsProvider.addGroupsChangedListener(myListener);

    this.groupModelList = groupModelList;
    groupModelList.addListener(myListener);

    myPanel = new JPanel(new GridLayout(1, 1));
    listModel = new DefaultListModel<>();
    myList = new JList<>(listModel);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane);

    for (GroupModel model : groupModelList)
    {
      IdModel id = model.getID();
      listModel.addElement(id);
      id.addIDChangeListener(myListener);
    }

    for (IdModel id : groupsProvider)
    {
      if (!listModel.contains(id)) listModel.addElement(id);
    }

    Set<IdModel> selected = groupsProvider.getGroups();
    int[] indices = new int[selected.size()];
    int i = 0;
    for (IdModel id : selected)
    {
      indices[i++] = listModel.indexOf(id);
    }

    myList.setSelectedIndices(indices);

    myList.addListSelectionListener(myListener);
  }

  /**
   * Gibt von dieser View belegte Ressourcen frei, damit diese View gc'ed werden
   * kann.
   */
  public void dispose()
  {
    groupModelList.removeListener(myListener);
    myList.removeListSelectionListener(myListener);
  }

  @Override
  public JComponent getComponent()
  {
    return myPanel;
  }

  private class MyListener implements GroupModelList.ItemListener,
      GroupsProvider.GroupsChangedListener, ListSelectionListener, IDChangeListener
  {

    /**
     * Zu groupModelList (d.h. zur Menge aller verfügbaren Gruppen) wurde eine Gruppe
     * hinzugefügt.
     */
    @Override
    public void itemAdded(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IdModel id = model.getID();
      if (!listModel.contains(id)) listModel.addElement(id);
      recursion = false;
    }

    /**
     * Aus groupModelList (d.h. aus der Menge aller verfügbaren Gruppen) wurde eine
     * Gruppe entfernt.
     */
    @Override
    public void itemRemoved(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IdModel groupID = model.getID();
      listModel.removeElement(groupID);
      groupsProvider.removeGroup(groupID);
      recursion = false;
    }

    /**
     * Zu groupsProvider (d.h. der Liste der selektierten Gruppen) wurde eine Gruppe
     * hinzugefügt.
     */
    @Override
    public void groupAdded(IdModel groupID)
    {
      if (recursion) return;
      recursion = true;
      int index = listModel.indexOf(groupID);
      if (index >= 0) myList.getSelectionModel().addSelectionInterval(index, index);

      recursion = false;
    }

    /**
     * Aus groupsProvider (d.h. aus der Liste der selektierten Gruppen) wurde eine
     * Gruppe entfernt.
     */
    @Override
    public void groupRemoved(IdModel groupID)
    {
      if (recursion) return;
      recursion = true;
      int index = listModel.indexOf(groupID);
      if (index >= 0)
        myList.getSelectionModel().removeSelectionInterval(index, index);
      recursion = false;
    }

    /**
     * Die Selektion wurde in der GUI geändert.
     */
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
      if (recursion) return;
      if (e.getValueIsAdjusting()) return;
      recursion = true;

      List<IdModel> selected = myList.getSelectedValuesList();
      Iterator<IdModel> iter = groupsProvider.iterator();
      while (iter.hasNext())
      {
        IdModel id = iter.next();
        found: do
        {
          for (IdModel id2 : selected)
          {
            if (id2.equals(id)) break found;
          }

          iter.remove();
        } while (false);
      }

      for (Object o : selected)
      {
        for (GroupModel model : groupModelList)
        {
          IdModel id = model.getID();
          if (o.equals(id)) groupsProvider.addGroup(id);
        }
      }

      recursion = false;
    }

    /**
     * Die ID einer der Gruppen aus groupModelList (d.h. der Liste aller verfügbaren Gruppen) sich
     * geändert.
     */
    @Override
    public void idHasChanged(IdModel id)
    {
      // nothing to do
    }
  }
}
