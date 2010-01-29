/*
 * Dateiname: AllGroupFuncViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Eine View die alle OneGroupFuncViews enthält.
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
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.OnDemandCardView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine View, die alle
 * {@link de.muenchen.allg.itd51.wollmux.former.group.OneGroupFuncView}s enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllGroupFuncViewsPanel extends OnDemandCardView
{

  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;

  /**
   * Erzeugt ein {@link AllGroupFuncViewsPanel}, das den Inhalt von groupModelList
   * anzeigt.
   * 
   * @param funcLib
   *          die Funktionsbibliothek, die die Funktionen enthält, die die Views zur
   *          Auswahl anbieten sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllGroupFuncViewsPanel(GroupModelList groupModelList,
      FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    super(L.m("Function-View"));
    this.funcLib = funcLib;
    groupModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    for (GroupModel model : groupModelList)
      if (model.hasFunc()) addItem(model);
  }

  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    GroupModel m = (GroupModel) model;
    return new OneGroupFuncView(m, funcLib, viewChangeListener);
  }

  private class MyItemListener implements GroupModelList.ItemListener
  {
    public void itemAdded(GroupModel model, int index)
    {
      if (model.hasFunc()) addItem(model);
    }

    public void itemRemoved(GroupModel model, int index)
    {
    // Hier muss nicht removeItem(model) aufgerufen werden. Dies behandelt die
    // OnDemandCardView selbst mittels Listener
    }
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }

    public void broadcastGroupModelSelection(BroadcastObjectSelection b)
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
