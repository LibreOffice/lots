/*
 * Dateiname: OneFormControlExtView.java
 * Projekt  : WollMux
 * Funktion : Anzeige erweiterter Eigenschaften eines FormControlModels.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 24.10.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.control;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.group.OneGroupsProviderGroupsEditView;
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
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
    myTabbedPane.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if (myTabbedPane.getTabCount() > 1 && myTabbedPane.getSelectedIndex() >= 0)
          selectedTab = myTabbedPane.getSelectedIndex();
      }
    });
  }

  /**
   * Baut myTabbedPane auf.
   */
  private void initPanel()
  {
    autofillView = new OneFormControlAutofillEditView(model, funcLib, null);
    myTabbedPane.addTab("AUTOFILL", autofillView.JComponent());
    plausiView = new OneFormControlPlausiEditView(model, funcLib, null);
    myTabbedPane.addTab("PLAUSI", plausiView.JComponent());
    groupsView =
      new OneGroupsProviderGroupsEditView(model.getGroupsProvider(), groupModelList);
    myTabbedPane.addTab("GROUPS", groupsView.JComponent());
    myTabbedPane.setSelectedIndex(selectedTab);
  }

  public void viewIsVisible()
  {
    if (myTabbedPane.getTabCount() == 0) initPanel();
  }

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

  public JComponent JComponent()
  {
    return myTabbedPane;
  }

  /**
   * Liefert das Model zu dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

  private class MyModelChangeListener implements
      FormControlModel.ModelChangeListener
  {
    public void modelRemoved(FormControlModel model)
    {
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneFormControlExtView.this);
    }

    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue)
    {}
  }

}
