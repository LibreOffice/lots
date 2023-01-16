/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.control;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import de.muenchen.allg.itd51.wollmux.former.FormMaxConstants;
import de.muenchen.allg.itd51.wollmux.former.control.model.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.group.OneGroupsProviderGroupsEditView;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.view.LazyView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Anzeige erweiterter Eigenschaften eines FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlExtView implements LazyView
{
  /**
   * Da alle Views ihre eigene {@link JTabbedPane} haben, wird über diese (über
   * {@link ChangeListener} gepflegte) statische Variable dafür gesorgt, dass beim
   * Umschalten von einer auf die andere View trotzdem das selbe Tab selektiert ist.
   */
  private static int selectedTab = 0;

  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Die oberste Komponente dieser View.
   */
  private JTabbedPane myTabbedPane;

  /**
   * Das Model zu dieser View.
   */
  private FormControlModel model;

  /**
   * Die Liste mit den Gruppen, die zur Auswahl angeboten werden sollen.
   */
  private GroupModelList groupModelList;

  /**
   * Die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden sollen
   * für das Auswählen von Attributen, die eine Funktion erfordern.
   */
  private FunctionLibrary funcLib;

  /**
   * Der AUTOFILL-Reiter.
   */
  private OneFormControlAutofillEditView autofillView;

  /**
   * Der PLAUSI-Reiter.
   */
  private OneFormControlPlausiEditView plausiView;

  /**
   * Der GROUPS-Reiter.
   */
  private OneGroupsProviderGroupsEditView groupsView;

  /**
   * Erzeugt eine neue View.
   *
   * @param model
   *          das Model dessen Daten angezeigt werden sollen.
   * @param funcLib
   *          die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden
   *          sollen für das Auswählen von Attributen, die eine Funktion erfordern.
   * @param groupModelList
   *          die Liste mit den Gruppen, die zur Auswahl angeboten werden sollen.
   * @param myViewChangeListener
   *          typischerweise ein Container, der diese View enthält und über
   *          Änderungen informiert werden soll.
   */
  public OneFormControlExtView(FormControlModel model, FunctionLibrary funcLib,
      GroupModelList groupModelList, ViewChangeListener myViewChangeListener)
  {
    this.bigDaddy = myViewChangeListener;
    this.model = model;
    this.groupModelList = groupModelList;
    this.funcLib = funcLib;
    myTabbedPane = new JTabbedPane();
    model.addListener(new MyModelChangeListener());
    myTabbedPane.addChangeListener(e -> {
      if (myTabbedPane.getTabCount() > 1 && myTabbedPane.getSelectedIndex() >= 0)
      {
        synchronized (OneFormControlExtView.class)
        {
          selectedTab = myTabbedPane.getSelectedIndex();
        }
      }
    });
  }

  /**
   * Baut myTabbedPane auf.
   */
  private void initPanel()
  {
    autofillView = new OneFormControlAutofillEditView(model, funcLib, null);
    myTabbedPane.addTab(FormMaxConstants.AUTOFILL, autofillView.getComponent());
    plausiView = new OneFormControlPlausiEditView(model, funcLib, null);
    myTabbedPane.addTab("PLAUSI", plausiView.getComponent());
    groupsView =
      new OneGroupsProviderGroupsEditView(model.getGroupsProvider(), groupModelList);
    myTabbedPane.addTab("GROUPS", groupsView.getComponent());
    myTabbedPane.setSelectedIndex(selectedTab);
  }

  @Override
  public void viewIsVisible()
  {
    if (myTabbedPane.getTabCount() == 0) initPanel();
  }

  @Override
  public void viewIsNotVisible()
  {
    if (myTabbedPane.getTabCount() != 0)
    {
      autofillView.dispose();
      autofillView = null;
      plausiView.dispose();
      plausiView = null;
      groupsView.dispose();
      groupsView = null;
      myTabbedPane.removeAll();
    }
  }

  @Override
  public JComponent getComponent()
  {
    return myTabbedPane;
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public FormControlModel getModel()
  {
    return model;
  }

  private class MyModelChangeListener implements
      FormControlModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(FormControlModel model)
    {
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneFormControlExtView.this);
    }

    @Override
    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue)
    {
      // nothing to do
    }
  }

}
