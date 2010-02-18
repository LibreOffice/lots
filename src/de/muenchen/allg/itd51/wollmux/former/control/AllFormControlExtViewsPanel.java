/*
 * Dateiname: AllFormControlExtViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Eine View, die alle OneFormControlExtViews enthält.
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
 * 23.10.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.control;

import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.view.OnDemandCardView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine View, die alle
 * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlExtView}s
 * enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlExtViewsPanel extends OnDemandCardView
{
  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;

  /**
   * Die Liste der Gruppen, die zur Auswahl gestellt werden.
   */
  private GroupModelList groupModelList;

  /**
   * Erzeugt ein {@link AllFormControlExtViewsPanel}, das den Inhalt von
   * formControlModelList anzeigt.
   * 
   * @param funcLib
   *          die Funktionsbibliothek, die die Funktionen enthält, die die View zur
   *          Auswahl anbieten sollen.
   * @param groupModelList
   *          die Liste der Gruppen, die zur Auswahl stehen sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllFormControlExtViewsPanel(FormControlModelList formControlModelList,
      FunctionLibrary funcLib, GroupModelList groupModelList,
      FormularMax4000 formularMax4000)
  {
    super(L.m("Extra-View"));
    this.funcLib = funcLib;
    this.groupModelList = groupModelList;
    formControlModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    Iterator<FormControlModel> iter = formControlModelList.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = iter.next();
      if (model.hasPlausi() || model.hasAutofill() || model.hasGroups())
        addItem(model);
    }
  }

  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    FormControlModel m = (FormControlModel) model;
    return new OneFormControlExtView(m, funcLib, groupModelList, viewChangeListener);
  }

  private class MyItemListener implements FormControlModelList.ItemListener
  {

    public void itemAdded(FormControlModel model, int index)
    {
      if (model.hasPlausi() || model.hasAutofill() || model.hasGroups())
        addItem(model);
    }

    public void itemSwapped(int index1, int index2)
    {}
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
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

    public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }

    public void broadcastGroupModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }
  }

}
