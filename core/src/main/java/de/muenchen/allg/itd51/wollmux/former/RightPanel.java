/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlExtViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.model.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.group.AllGroupFuncViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.model.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.section.AllSectionExtViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.section.model.SectionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Managet die rechte Hälfte des FM4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class RightPanel implements View
{
  /**
   * Identifikationsstring für {@link CardLayout}.
   */
  private static final String ALL_INSERTION_TRAFO_VIEWS_PANEL =
    "ALL_INSERTION_TRAFO_VIEWS_PANEL";

  /**
   * Identifikationsstring für {@link CardLayout}.
   */
  private static final String ALL_FORMCONTROL_EXT_VIEWS_PANEL =
    "ALL_FORMCONTROL_EXT_VIEWS_PANEL";

  /**
   * Identifikationsstring für {@link CardLayout}.
   */
  private static final String ALL_GROUP_FUNC_VIEWS_PANEL =
    "ALL_GROUP_FUNC_VIEWS_PANEL";

  /**
   * Identifikationsstring für {@link CardLayout}.
   */
  private static final String ALL_SECTION_EXT_VIEWS_PANEL =
    "ALL_SECTION_EXT_VIEWS_PANEL";

  /**
   * Das JPanel, dass alle Inhalte dieser View enthält.
   */
  private JPanel myPanel;

  /**
   * Das CardLayout für myPanel.
   */
  private CardLayout cards;

  /**
   * Erzeugt ein neues RightPanel. Zur Erläuterung der Parameter siehe
   * {@link AllInsertionTrafoViewsPanel#AllInsertionTrafoViewsPanel(InsertionModelList, FunctionLibrary, FormularMax4kController)}
   * und
   * {@link AllFormControlExtViewsPanel#AllFormControlExtViewsPanel(FormControlModelList, FunctionLibrary, GroupModelList, FormularMax4kController)}.
   * und
   * {@link AllGroupFuncViewsPanel#AllGroupFuncViewsPanel(GroupModelList, FunctionLibrary, FormularMax4kController)}
   * und
   * {@link AllSectionExtViewsPanel#AllSectionExtViewsPanel(SectionModelList, GroupModelList, FormularMax4kController)}
   */
  public RightPanel(InsertionModelList insertionModelList,
      FormControlModelList formControlModelList, GroupModelList groupModelList,
      SectionModelList sectionModelList, FunctionLibrary funcLib,
      FormularMax4kController formularMax4000)
  {
    cards = new CardLayout();
    myPanel = new JPanel(cards);
    AllFormControlExtViewsPanel allFormControlExtViewsPanel =
      new AllFormControlExtViewsPanel(formControlModelList, funcLib, groupModelList,
        formularMax4000);
    myPanel.add(allFormControlExtViewsPanel.getComponent(),
      ALL_FORMCONTROL_EXT_VIEWS_PANEL);
    AllInsertionTrafoViewsPanel allInsertionTrafoViewsPanel =
      new AllInsertionTrafoViewsPanel(insertionModelList, funcLib, formularMax4000);
    myPanel.add(allInsertionTrafoViewsPanel.getComponent(),
      ALL_INSERTION_TRAFO_VIEWS_PANEL);
    AllGroupFuncViewsPanel allGroupFuncViewsPanel =
      new AllGroupFuncViewsPanel(groupModelList, new FunctionLibrary(),
        formularMax4000);
    myPanel.add(allGroupFuncViewsPanel.getComponent(), ALL_GROUP_FUNC_VIEWS_PANEL);
    AllSectionExtViewsPanel allSectionExtViewsPanel =
      new AllSectionExtViewsPanel(sectionModelList, groupModelList, formularMax4000);
    myPanel.add(allSectionExtViewsPanel.getComponent(), ALL_SECTION_EXT_VIEWS_PANEL);

    formularMax4000.addBroadcastListener(new MyBroadcastListener());
  }

  public JComponent getComponent()
  {
    return myPanel;
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    @Override
    public void broadcastAllInsertionsViewSelected()
    {
      cards.show(myPanel, ALL_INSERTION_TRAFO_VIEWS_PANEL);
    }

    @Override
    public void broadcastAllFormControlsViewSelected()
    {
      cards.show(myPanel, ALL_FORMCONTROL_EXT_VIEWS_PANEL);
    }

    @Override
    public void broadcastAllGroupsViewSelected()
    {
      cards.show(myPanel, ALL_GROUP_FUNC_VIEWS_PANEL);
    }

    @Override
    public void broadcastAllSectionsViewSelected()
    {
      cards.show(myPanel, ALL_SECTION_EXT_VIEWS_PANEL);
    }
  }
}
