/*
 * Dateiname: RightPanel.java
 * Projekt  : WollMux
 * Funktion : Managet die rechte Hälfte des FM4000.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlExtViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.group.AllGroupFuncViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.section.AllSectionExtViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;

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
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel#AllInsertionTrafoViewsPanel(InsertionModelList, FunctionLibrary, FormularMax4000)}
   * und
   * {@link de.muenchen.allg.itd51.wollmux.former.control.AllFormControlExtViewsPanel#AllFormControlExtViewsPanel(FormControlModelList, FunctionLibrary, FormularMax4000)}.
   * und
   * {@link AllGroupFuncViewsPanel#AllGroupFuncViewsPanel(GroupModelList, FunctionLibrary, FormularMax4000)}
   * und
   * {@link AllSectionExtViewsPanel#AllSectionExtViewsPanel(SectionModelList, GroupModelList, FormularMax4000)}
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
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
    public void broadcastAllInsertionsViewSelected()
    {
      cards.show(myPanel, ALL_INSERTION_TRAFO_VIEWS_PANEL);
    }

    public void broadcastAllFormControlsViewSelected()
    {
      cards.show(myPanel, ALL_FORMCONTROL_EXT_VIEWS_PANEL);
    }

    public void broadcastAllGroupsViewSelected()
    {
      cards.show(myPanel, ALL_GROUP_FUNC_VIEWS_PANEL);
    }

    public void broadcastAllSectionsViewSelected()
    {
      cards.show(myPanel, ALL_SECTION_EXT_VIEWS_PANEL);
    }
  }
}
