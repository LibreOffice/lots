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
package org.libreoffice.lots.former;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.libreoffice.lots.former.control.AllFormControlLineViewsPanel;
import org.libreoffice.lots.former.control.model.FormControlModel;
import org.libreoffice.lots.former.control.model.FormControlModelList;
import org.libreoffice.lots.former.group.AllGroupLineViewsPanel;
import org.libreoffice.lots.former.group.model.GroupModel;
import org.libreoffice.lots.former.group.model.GroupModelList;
import org.libreoffice.lots.former.insertion.AllInsertionLineViewsPanel;
import org.libreoffice.lots.former.insertion.model.InsertionModel;
import org.libreoffice.lots.former.insertion.model.InsertionModelList;
import org.libreoffice.lots.former.section.AllSectionLineViewsPanel;
import org.libreoffice.lots.former.section.model.SectionModel;
import org.libreoffice.lots.former.section.model.SectionModelList;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.util.L;

import com.sun.star.text.XTextDocument;

/**
 * Der Übercontainer für die linke Hälfte des FormularMax 4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class LeftPanel implements View
{
  /**
   * Hält in einem Panel FormControlModelLineViews für alle {@link FormControlModel}s.
   */
  private AllFormControlLineViewsPanel allFormControlModelLineViewsPanel;

  /**
   * Hält in einem Panel InsertionModelLineViews für alle {@link InsertionModel}s.
   */
  private AllInsertionLineViewsPanel allInsertionModelLineViewsPanel;

  /**
   * Hält in einem Panel GroupModelLineViews für alle {@link GroupModel}s.
   */
  private AllGroupLineViewsPanel allGroupModelLineViewsPanel;

  /**
   * Hält in einem Panel SectionModelLineViews für alle {@link SectionModel}s.
   */
  private AllSectionLineViewsPanel allSectionModelLineViewsPanel;

  /**
   * Enthält alle im linken Panel angezeigten Views.
   */
  private JTabbedPane myTabbedPane;

  /**
   * Der FormularMax4000 zu dem dieses Panel gehört.
   */
  private FormularMax4kController formularMax4000;

  public LeftPanel(InsertionModelList insertionModelList,
      FormControlModelList formControlModelList, GroupModelList groupModelList,
      SectionModelList sectionModelList, FormularMax4kController formularMax4000,
      XTextDocument doc)
  {
    this.formularMax4000 = formularMax4000;
    allFormControlModelLineViewsPanel =
      new AllFormControlLineViewsPanel(formControlModelList, formularMax4000);
    allInsertionModelLineViewsPanel =
      new AllInsertionLineViewsPanel(insertionModelList, formularMax4000);
    allGroupModelLineViewsPanel =
      new AllGroupLineViewsPanel(groupModelList, formularMax4000);
    allSectionModelLineViewsPanel =
      new AllSectionLineViewsPanel(sectionModelList, formularMax4000, doc);

    myTabbedPane =
      new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    myTabbedPane.add(L.m("Form GUI"),
      allFormControlModelLineViewsPanel.getComponent());
    myTabbedPane.add(L.m("Insertion"),
      allInsertionModelLineViewsPanel.getComponent());
    myTabbedPane.add(L.m("Visibility"), allGroupModelLineViewsPanel.getComponent());
    myTabbedPane.add(L.m("Sections"), allSectionModelLineViewsPanel.getComponent());

    myTabbedPane.addChangeListener(e -> tabSwitched());
  }

  private void tabSwitched()
  {
    if (myTabbedPane.getSelectedComponent() == allFormControlModelLineViewsPanel.getComponent())
    {
      formularMax4000.broadcast(BroadcastListener::broadcastAllFormControlsViewSelected);
    }
    else if (myTabbedPane.getSelectedComponent() == allInsertionModelLineViewsPanel.getComponent())
    {
      formularMax4000.broadcast(BroadcastListener::broadcastAllInsertionsViewSelected);
    }
    else if (myTabbedPane.getSelectedComponent() == allGroupModelLineViewsPanel.getComponent())
    {
      formularMax4000.broadcast(BroadcastListener::broadcastAllGroupsViewSelected);
    }
    else if (myTabbedPane.getSelectedComponent() == allSectionModelLineViewsPanel.getComponent())
    {
      formularMax4000.broadcast(BroadcastListener::broadcastAllSectionsViewSelected);
    }
  }

  /**
   * Liefert den Index an dem Buttons auf dem aktuell sichtbaren Tab des
   * {@link AllFormControlLineViewsPanel} eingefügt werden sollten oder -1, falls
   * dort kein Tab ausgewählt ist. Der zurückgelieferte Wert (falls nicht -1)
   * entspricht dem Index des letzten sichtbaren Elements + 1.
   */
  public int getButtonInsertionIndex()
  {
    return allFormControlModelLineViewsPanel.getButtonInsertionIndex();
  }

  @Override
  public JComponent getComponent()
  {
    return myTabbedPane;
  }

  /**
   * Ruft {@link AllFormControlLineViewsPanel#mergeCheckboxesIntoCombobox()} auf und
   * liefert dessen Ergebnis zurück.
   */
  public ComboboxMergeDescriptor mergeCheckboxesIntoCombobox()
  {
    return allFormControlModelLineViewsPanel.mergeCheckboxesIntoCombobox();
  }

}
