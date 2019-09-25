/*
 * Dateiname: AllInsertionTrafoViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Eine View, die alle OneInsertionExtViews enthält.
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
 * 28.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.view.OnDemandCardView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Eine View, die alle
 * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionExtView}s
 * enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllInsertionTrafoViewsPanel extends OnDemandCardView
{

  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;

  /**
   * Erzeugt ein {@link AllInsertionTrafoViewsPanel}, das den Inhalt von
   * insertionModelList anzeigt.
   * 
   * @param funcLib
   *          die Funktionsbibliothek, die die Funktionen enthält, die die Views zur
   *          Auswahl anbieten sollen.
   */
  public AllInsertionTrafoViewsPanel(InsertionModelList insertionModelList,
      FunctionLibrary funcLib, FormularMax4kController formularMax4000)
  {
    super(L.m("Trafo-View"));
    this.funcLib = funcLib;
    insertionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    Iterator<InsertionModel> iter = insertionModelList.iterator();
    while (iter.hasNext())
    {
      InsertionModel model = iter.next();
      if (model.hasTrafo()) addItem(model);
    }
  }

  @Override
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    InsertionModel m = (InsertionModel) model;
    return new OneInsertionExtView(m, funcLib, viewChangeListener);
  }

  private class MyItemListener implements InsertionModelList.ItemListener
  {
    @Override
    public void itemAdded(InsertionModel model, int index)
    {
      if (model.hasTrafo()) addItem(model);
    }

    @Override
    public void itemRemoved(InsertionModel model, int index)
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
    public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
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
