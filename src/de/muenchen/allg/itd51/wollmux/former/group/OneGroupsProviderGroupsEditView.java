/*
 * Dateiname: OneGroupsProviderGroupsEditView.java
 * Projekt  : WollMux
 * Funktion : Lässt die Liste der Gruppen eines GroupsProvider bearbeiten.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
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

import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.IDManager.IDChangeListener;
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
  private JList<IDManager.ID> myList;

  /**
   * Das {@link ListModel} zu {@link #myList}.
   */
  private DefaultListModel<IDManager.ID> listModel;

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
    listModel = new DefaultListModel<IDManager.ID>();
    myList = new JList<IDManager.ID>(listModel);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane);

    for (GroupModel model : groupModelList)
    {
      IDManager.ID id = model.getID();
      listModel.addElement(id);
      id.addIDChangeListener(myListener);
    }

    for (IDManager.ID id : groupsProvider)
    {
      if (!listModel.contains(id)) listModel.addElement(id);
    }

    Set<IDManager.ID> selected = groupsProvider.getGroups();
    int[] indices = new int[selected.size()];
    int i = 0;
    for (IDManager.ID id : selected)
    {
      indices[i++] = listModel.indexOf(id);
    }

    myList.setSelectedIndices(indices);

    myList.addListSelectionListener(myListener);
  }

  /**
   * Gibt von dieser View belegte Ressourcen frei, damit diese View gc'ed werden
   * kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void dispose()
  {
    groupModelList.removeListener(myListener);
    myList.removeListSelectionListener(myListener);
  }

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
    public void itemAdded(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IDManager.ID id = model.getID();
      if (!listModel.contains(id)) listModel.addElement(id);
      recursion = false;
    }

    /**
     * Aus groupModelList (d.h. aus der Menge aller verfügbaren Gruppen) wurde eine
     * Gruppe entfernt.
     */
    public void itemRemoved(GroupModel model, int index)
    {
      if (recursion) return;
      recursion = true;
      IDManager.ID groupID = model.getID();
      listModel.removeElement(groupID);
      groupsProvider.removeGroup(groupID);
      recursion = false;
    }

    /**
     * Zu groupsProvider (d.h. der Liste der selektierten Gruppen) wurde eine Gruppe
     * hinzugefügt.
     */
    public void groupAdded(ID groupID)
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
    public void groupRemoved(ID groupID)
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
    public void valueChanged(ListSelectionEvent e)
    {
      if (recursion) return;
      if (e.getValueIsAdjusting()) return;
      recursion = true;

      List<IDManager.ID> selected = myList.getSelectedValuesList();
      Iterator<IDManager.ID> iter = groupsProvider.iterator();
      while (iter.hasNext())
      {
        IDManager.ID id = iter.next();
        found: do
        {
          for (IDManager.ID id2 : selected)
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
          IDManager.ID id = model.getID();
          if (o.equals(id)) groupsProvider.addGroup(id);
        }
      }

      recursion = false;
    }

    /**
     * Die ID einer der Gruppen aus groupModelList (d.h. der Liste aller verfügbaren
     * Gruppen) sich geändert.
     */
    public void idHasChanged(ID id)
    {}
  }
}
