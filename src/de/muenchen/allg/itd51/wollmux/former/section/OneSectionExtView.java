/*
 * Dateiname: OneSectionExtView.java
 * Projekt  : WollMux
 * Funktion : Anzeige erweiterter Eigenschaften eines SectionControlModels.
 * 
 * Copyright (c) 2008-2023 Landeshauptstadt München
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
 * 26.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.group.OneGroupsProviderGroupsEditView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
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

  public JComponent getComponent()
  {
    return myTabbedPane;
  }

  /**
   * Liefert das Model zu dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public SectionModel getModel()
  {
    return model;
  }

  private class MyModelChangeListener implements SectionModel.ModelChangeListener
  {
    public void attributeChanged(SectionModel model, int attributeId, Object newValue)
    {}

    public void modelRemoved(SectionModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneSectionExtView.this);
    }
  }

}
