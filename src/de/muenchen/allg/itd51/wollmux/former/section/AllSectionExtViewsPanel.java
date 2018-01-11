/*
 * Dateiname: AllSectionExtViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Eine View, die alle OneSectionExtViews enthält.
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
 * 26.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.view.OnDemandCardView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Eine View, die alle
 * {@link de.muenchen.allg.itd51.wollmux.former.section.OneSectionExtView}s enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllSectionExtViewsPanel extends OnDemandCardView
{
  /**
   * Diese Sichtbarkeitsgruppen stehen zur Auswahl für das GROUPS-Attribut.
   */
  private GroupModelList groupModelList;

  /**
   * Erzeugt ein {@link AllSectionExtViewsPanel}, das den Inhalt von
   * sectionModelList anzeigt.
   * 
   * @param groupModelList
   *          die Liste der Sichtbarkeitsgruppen die zur Auswahl angeboten werden
   *          sollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllSectionExtViewsPanel(SectionModelList sectionModelList,
      GroupModelList groupModelList, FormularMax4kController formularMax4000)
  {
    super(L.m("GROUPS-View"));
    this.groupModelList = groupModelList;
    sectionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    Iterator<SectionModel> iter = sectionModelList.iterator();
    while (iter.hasNext())
    {
      SectionModel model = iter.next();
      if (model.hasGroups()) addItem(model);
    }
  }

  @Override
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    SectionModel m = (SectionModel) model;
    return new OneSectionExtView(m, groupModelList, viewChangeListener);
  }

  private class MyItemListener implements SectionModelList.ItemListener
  {
    @Override
    public void itemAdded(SectionModel model, int index)
    {
      if (model.hasGroups()) addItem(model);
    }

    @Override
    public void itemRemoved(SectionModel model, int index)
    {
    // Hier muss nicht removeItem(model) aufgerufen werden. Dies behandelt die
    // OnDemandCardView selbst mittels Listener
    }
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    @Override
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }

    @Override
    public void broadcastSectionModelSelection(BroadcastObjectSelection b)
    {
      if (b.getState() == 1)
      {
        show(b.getObject());
      }
      else
      {
        showEmpty();
      }
    }
  }

}
