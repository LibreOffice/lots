/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.former.insertion;

import java.util.Iterator;

import org.libreoffice.lots.former.BroadcastListener;
import org.libreoffice.lots.former.BroadcastObjectSelection;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.insertion.model.InsertionModel;
import org.libreoffice.lots.former.insertion.model.InsertionModelList;
import org.libreoffice.lots.former.view.OnDemandCardView;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.util.L;

/**
 * Eine View, die alle
 * {@link org.libreoffice.lots.former.insertion.OneInsertionExtView}s
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
    super(L.m("TRAFO View"));
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

  private class MyBroadcastListener implements BroadcastListener
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
