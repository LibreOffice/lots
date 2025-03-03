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
package org.libreoffice.lots.former.section;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.libreoffice.lots.former.group.OneGroupsProviderGroupsEditView;
import org.libreoffice.lots.former.group.model.GroupModelList;
import org.libreoffice.lots.former.section.model.SectionModel;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.former.view.ViewChangeListener;

/**
 * Anzeige erweiterter Eigenschaften eines {@link SectionModel}s.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneSectionExtView implements View
{
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
  private SectionModel model;

  /**
   * Erzeugt eine neue View.
   *
   * @param model
   *          das Model dessen Daten angezeigt werden sollen.
   * @param groupModelList
   *          die Liste mit den Gruppen, die zur Auswahl angeboten werden sollen.
   * @param myViewChangeListener
   *          typischerweise ein Container, der diese View enthält und über
   *          Änderungen informiert werden soll.
   */
  public OneSectionExtView(SectionModel model, GroupModelList groupModelList,
      ViewChangeListener myViewChangeListener)
  {
    this.bigDaddy = myViewChangeListener;
    this.model = model;
    myTabbedPane = new JTabbedPane();

    OneGroupsProviderGroupsEditView groupsView =
      new OneGroupsProviderGroupsEditView(model.getGroupsProvider(), groupModelList);
    myTabbedPane.addTab("GROUPS", groupsView.getComponent());

    model.addListener(new MyModelChangeListener());
  }

  @Override
  public JComponent getComponent()
  {
    return myTabbedPane;
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public SectionModel getModel()
  {
    return model;
  }

  private class MyModelChangeListener implements SectionModel.ModelChangeListener
  {
    @Override
    public void attributeChanged(SectionModel model, int attributeId, Object newValue)
    {
      // nothing to do
    }

    @Override
    public void modelRemoved(SectionModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneSectionExtView.this);
    }
  }

}
