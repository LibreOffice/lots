/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.former.group;

import org.libreoffice.lots.former.BroadcastListener;
import org.libreoffice.lots.former.BroadcastObjectSelection;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.group.model.GroupModel;
import org.libreoffice.lots.former.group.model.GroupModelList;
import org.libreoffice.lots.former.view.OnDemandCardView;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.util.L;

/**
 * Eine View, die alle
 * {@link org.libreoffice.lots.former.group.OneGroupFuncView}s enthält.
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
   */
  public AllGroupFuncViewsPanel(GroupModelList groupModelList,
      FunctionLibrary funcLib, FormularMax4kController formularMax4000)
  {
    super(L.m("Function View"));
    this.funcLib = funcLib;
    groupModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    for (GroupModel model : groupModelList)
      if (model.hasFunc()) addItem(model);
  }

  @Override
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    GroupModel m = (GroupModel) model;
    return new OneGroupFuncView(m, funcLib, viewChangeListener);
  }

  private class MyItemListener implements GroupModelList.ItemListener
  {
    @Override
    public void itemAdded(GroupModel model, int index)
    {
      if (model.hasFunc()) addItem(model);
    }

    @Override
    public void itemRemoved(GroupModel model, int index)
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
