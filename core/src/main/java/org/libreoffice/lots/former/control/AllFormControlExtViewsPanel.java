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
package org.libreoffice.lots.former.control;

import java.util.Iterator;

import org.libreoffice.lots.former.BroadcastListener;
import org.libreoffice.lots.former.BroadcastObjectSelection;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.control.model.FormControlModel;
import org.libreoffice.lots.former.control.model.FormControlModelList;
import org.libreoffice.lots.former.group.model.GroupModelList;
import org.libreoffice.lots.former.view.OnDemandCardView;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.util.L;

/**
 * Eine View, die alle
 * {@link org.libreoffice.lots.former.control.OneFormControlExtView}s
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
   */
  public AllFormControlExtViewsPanel(FormControlModelList formControlModelList,
      FunctionLibrary funcLib, GroupModelList groupModelList,
      FormularMax4kController formularMax4000)
  {
    super(L.m("Extra View"));
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

  @Override
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    FormControlModel m = (FormControlModel) model;
    return new OneFormControlExtView(m, funcLib, groupModelList, viewChangeListener);
  }

  private class MyItemListener implements FormControlModelList.ItemListener
  {

    @Override
    public void itemAdded(FormControlModel model, int index)
    {
      if (model.hasPlausi() || model.hasAutofill() || model.hasGroups())
        addItem(model);
    }

    @Override
    public void itemSwapped(int index1, int index2)
    {
      // nothing to do
    }
  }

  private class MyBroadcastListener implements BroadcastListener
  {
    @Override
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    {
      if (b.getState() == BroadcastObjectSelection.STATE_NORMAL_CLICK)
      {
        show(b.getObject());
      }
      else
      {
        showEmpty();
      }
    }

    @Override
    public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }

    @Override
    public void broadcastGroupModelSelection(BroadcastObjectSelection b)
    {
      showEmpty();
    }
  }

}
