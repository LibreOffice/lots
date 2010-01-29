/*
 * Dateiname: AllFormControlLineViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Hält in einem Panel FormControlModelLineViews für alle FormControlModels einer FormControlModelList.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 30.08.2006 | BNK | Erstellung
 * 10.09.2006 | BNK | [R3208]Tab-Struktur des Formulars bereits im FM4000 anzeigen
 * 19.01.2007 | BNK | [R5406]+broadcastViewVisibilitySettings()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.control;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.ComboboxMergeDescriptor;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.ViewVisibilityDescriptor;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList.ItemListener;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Hält in einem Panel FormControlModelLineViews für alle
 * {@link de.muenchen.allg.itd51.wollmux.former.control.FormControlModel} einer
 * {@link de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList}.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlLineViewsPanel implements View, ItemListener,
    OneFormControlLineView.ViewChangeListener
{
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Wird (mit wechselndem gridy Wert) verwendet als Constraints für das Hinzufügen
   * von Views zum Panel.
   */
  private GridBagConstraints gbcLineView =
    new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
      GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);

  /**
   * Wird verwendet als Constraints für das Hinzufügen von Glues zu den Tabs.
   */
  private GridBagConstraints gbcBottomGlue =
    new GridBagConstraints(0, FormControlModelList.MAX_MODELS_PER_TAB + 1, 1, 1,
      1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
        0, 0), 0, 0);

  /**
   * Die {@link FormControlModelList}, deren Inhalt in dieser View angezeigt wird.
   */
  private FormControlModelList formControlModelList;

  /**
   * Das Panel, das alle Komponenten dieser View enthält.
   */
  private JPanel myPanel;

  /**
   * Die JTabbedPane, die die Reiter dieser View enthält.
   */
  private JTabbedPane tabPane;

  /**
   * Das Panel, das die ganzen {@link OneFormControlLineView}s enthält.
   */
  private JPanel firstTab;

  /**
   * Enthält die {@link ViewDescriptor}s in der richtigen Reihenfolge.
   */
  private Vector<Object> viewDescriptors = new Vector<Object>();

  /**
   * Die Indizes der ausgewählten Elemente in {@link #viewDescriptors}.
   */
  private IndexList selection = new IndexList();

  /**
   * Die Scrollpane in der sich die {@link #tabPane} befindet.
   */
  private JScrollPane scrollPane;

  /**
   * Der FormularMax4000, zu dem diese View gehört.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Erzeugt eine AllFormControlLineViewsPanel, die den Inhalt von
   * formControlModelList anzeigt. ACHTUNG! formControlModelList sollte leer sein, da
   * nur neu hinzugekommene Elemente in der View angezeigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllFormControlLineViewsPanel(FormControlModelList formControlModelList,
      FormularMax4000 formularMax4000)
  {
    this.formControlModelList = formControlModelList;
    this.formularMax4000 = formularMax4000;
    formControlModelList.addListener(this);
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    myPanel = new JPanel(new GridBagLayout());
    tabPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    firstTab = new JPanel(new GridBagLayout());
    tabPane.addTab("Tab", firstTab);
    firstTab.add(Box.createGlue(), gbcBottomGlue);

    scrollPane = new JScrollPane(tabPane);
    // Scrollbars immer anzeigen, da sonst die preferred size zu klein berechnet
    // wird,
    // was den nervigen Effekt hat, dass das Fenster ein bisschen zu klein ist.
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(
      Common.getVerticalScrollbarUnitIncrement());
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcMainPanel =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    gbcMainPanel.gridx = 0;
    gbcMainPanel.gridy = 0;

    myPanel.add(scrollPane, gbcMainPanel);

    JPanel buttonPanel = new JPanel(new GridBagLayout());
    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcButtonPanel =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    gbcButtonPanel.gridx = 0;
    gbcButtonPanel.gridy = 2;
    myPanel.add(buttonPanel, gbcButtonPanel);

    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    JButton hochButton = new JButton(L.m("Hoch"));
    hochButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (noSelectedElementsOnVisibleTab()) return;
        moveSelectedElementsUp();
        if (noSelectedElementsOnVisibleTab()) showSelection();
      }
    });
    buttonPanel.add(hochButton, gbcButton);

    ++gbcButton.gridx;
    JButton runterButton = new JButton(L.m("Runter"));
    runterButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (noSelectedElementsOnVisibleTab()) return;
        moveSelectedElementsDown();
        if (noSelectedElementsOnVisibleTab()) showSelection();
      }
    });
    buttonPanel.add(runterButton, gbcButton);

    ++gbcButton.gridx;
    JButton killButton = new JButton(L.m("Löschen"));
    killButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (noSelectedElementsOnVisibleTab()) return;
        deleteSelectedElements();
      }
    });
    buttonPanel.add(killButton, gbcButton);

    ++gbcButton.gridx;
    JButton tabButton = new JButton(L.m("Tab"));
    tabButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (noSelectedElementsOnVisibleTab()) return;
        insertNewTab();
        if (noSelectedElementsOnVisibleTab()) showSelection();
      }
    });
    buttonPanel.add(tabButton, gbcButton);

    ++gbcButton.gridx;
    JButton newButton = new JButton(L.m("Neu"));
    newButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertNewElement();
      }
    });
    buttonPanel.add(newButton, gbcButton);
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  // TESTED
  public void itemAdded(FormControlModel model, int index)
  {
    if (index < 0 || index > viewDescriptors.size())
    {
      Logger.error(L.m("Inkonsistenz zwischen Model und View!"));
      return;
    }
    OneFormControlLineView ofclView =
      new OneFormControlLineView(model, this, formularMax4000);

    boolean isTab = (model.getType() == FormControlModel.TAB_TYPE);
    JComponent tab = firstTab;
    int tabIndex = 0;
    int gridY = 0;
    if (viewDescriptors.size() > 0)
    {
      int i = index;
      if (i == viewDescriptors.size()) --i;
      ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
      tabIndex = desc.containingTabIndex;
      tab = (JComponent) tabPane.getComponentAt(tabIndex);
      gridY = desc.gridY;
      if (i != index) ++gridY;
    }

    ViewDescriptor desc = new ViewDescriptor(ofclView, gridY, tabIndex, isTab);
    viewDescriptors.add(index, desc);

    gbcLineView.gridy = gridY;
    tab.add(ofclView.JComponent(), gbcLineView);
    tab.validate();

    fixTabStructure();
    tab = null; // tab ist eventuell entfernt worden!

    tabPane.validate();
    scrollPane.validate();

    selection.fixup(index, 1, viewDescriptors.size() - 1);
  }

  /**
   * Geht die {@link #viewDescriptors} Liste durch und behebt Fehler, die durch
   * strukturelle Änderungen hervorgerufen wurden. Dabei werden auch neue Tabs in
   * {@link #tabPane} angelegt bzw. alte entfernt wenn nötig.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void fixTabStructure()
  {
    Set<Object> toValidate = new HashSet<Object>();
    int gridY = 0;
    int tabIndex = 0;
    JComponent tab = firstTab;
    Iterator<Object> iter = viewDescriptors.iterator();
    while (iter.hasNext())
    {
      ViewDescriptor desc = (ViewDescriptor) iter.next();
      if (desc.isTab && gridY > 0)
      {
        ++tabIndex;
        gridY = 0;
        if (tabIndex >= tabPane.getTabCount())
        {
          JPanel newTab = new JPanel(new GridBagLayout());
          newTab.add(Box.createGlue(), gbcBottomGlue);
          tabPane.addTab("Tab", newTab);
        }

        tab = (JComponent) tabPane.getComponentAt(tabIndex);
      }

      if (desc.containingTabIndex != tabIndex || desc.gridY != gridY)
      {
        JComponent oldTab =
          (JComponent) tabPane.getComponent(desc.containingTabIndex);
        oldTab.remove(desc.view.JComponent());
        gbcLineView.gridy = gridY;
        tab.add(desc.view.JComponent(), gbcLineView);
        desc.containingTabIndex = tabIndex;
        desc.gridY = gridY;
        toValidate.add(tab);
        toValidate.add(oldTab);
      }

      if (desc.isTab)
        tabPane.setTitleAt(desc.containingTabIndex, desc.view.getModel().getLabel());

      ++gridY;
    }

    ++tabIndex;
    while (tabIndex < tabPane.getTabCount())
      tabPane.removeTabAt(tabPane.getTabCount() - 1);

    iter = toValidate.iterator();
    while (iter.hasNext())
    {
      ((JComponent) iter.next()).validate();
    }
  }

  /**
   * Liefert den Index des ViewDescriptors, der zu view gehört in der Liste
   * {@link #viewDescriptors} oder -1, wenn die view nicht enthalten ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private int getDescriptorIndexOf(OneFormControlLineView view)
  {
    for (int i = 0; i < viewDescriptors.size(); ++i)
    {
      if (((ViewDescriptor) viewDescriptors.get(i)).view == view) return i;
    }

    return -1;
  }

  public void viewShouldBeRemoved(View _view)
  { // TESTED
    OneFormControlLineView view = (OneFormControlLineView) _view;
    int index = getDescriptorIndexOf(view);
    if (index < 0) return;

    ViewDescriptor desc = (ViewDescriptor) viewDescriptors.remove(index);
    JComponent tab = (JComponent) tabPane.getComponentAt(desc.containingTabIndex);
    tab.remove(view.JComponent());
    tab.validate();

    fixTabStructure();
    tab = null; // tab ist eventuell entfernt worden!

    tabPane.validate();
    scrollPane.validate();

    selection.remove(index);
    selection.fixup(index, -1, viewDescriptors.size() - 1);
  }

  public void tabTitleChanged(OneFormControlLineView view)
  {
    int index = getDescriptorIndexOf(view);
    if (index < 0) return;
    ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(index);
    tabPane.setTitleAt(desc.containingTabIndex, view.getModel().getLabel());
  }

  /**
   * Schiebt alle ausgewählten Elemente um einen Platz nach oben, d,h, in Richtung
   * niedrigerer Indizes. Falls Element 0 ausgewählt ist wird gar nichts getan.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void moveSelectedElementsUp()
  {
    if (selection.firstElement() > 0)
    {
      formControlModelList.moveElementsUp(selection.iterator());
      // Kein fixupSelectionIndices(0, -1) nötig, weil die itemSwapped() Events schon
      // dafür sorgen
    }
  }

  /**
   * Hebt die Selektion aller Elemente auf.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void clearSelection()
  {
    Iterator<Integer> iter = selection.iterator();
    while (iter.hasNext())
    {
      Integer I = iter.next();
      OneFormControlLineView view =
        ((ViewDescriptor) viewDescriptors.get(I.intValue())).view;
      view.unmark();
    }
    selection.clear();
  }

  /**
   * Schiebt alle ausgewählten Elemente um einen Platz nach unten, d,h, in Richtung
   * niedrigerer Indizes. Falls das letzte Element ausgewählt ist wird gar nichts
   * getan.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void moveSelectedElementsDown()
  {
    if (selection.lastElement() < viewDescriptors.size() - 1)
    {
      formControlModelList.moveElementsDown(selection.reverseIterator());
      // Kein fixupSelectionIndices(0, 1) nötig, weil die itemSwapped() Events schon
      // dafür sorgen
    }
  }

  /**
   * Löscht alle ausgewählten Elemente.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void deleteSelectedElements()
  {
    /**
     * Die folgende Schleife muss auf diese Weise geschrieben werden und nicht mit
     * einem Iterator, weil es ansonsten eine ConcurrentModificationException gibt,
     * da über
     * {@link OneFormControlLineView.ViewChangeListener#viewShouldBeRemoved(OneFormControlLineView)}
     * die Selektion während des remove() gleich verändert wird, was den Iterator
     * invalidieren würde.
     */
    while (!selection.isEmpty())
    {
      int i = selection.lastElement();
      ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
      formControlModelList.remove(desc.view.getModel());
    }
  }

  /**
   * Falls mindestens 2 Elemente selektiert sind und alle momentan selektierten
   * Elemente Checkboxen sind, die nicht-leere IDs haben so werden diese entfernt und
   * stattdessen eine nicht-editierbare ComboBox eingefügt, deren Werte-Liste sich
   * aus den Labels aller entfernten Checkboxen zusammensetzt. Die Labels aller
   * selektierten Checkboxen müssen verschieden sein. Ein leeres Label ist jedoch
   * erlaubt (auch die Combobox enthält dann einen leeren Eintrag).
   * 
   * @return null, falls die Funktion nicht durchgeführt werden konnte (z.B. keine
   *         Checkboxen selektiert oder eine Checkbox hat keine ID). Ansonsten wird
   *         ein {@link ComboboxMergeDescriptor} geliefert, der das Ergebnis der
   *         Operation beschreibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public ComboboxMergeDescriptor mergeCheckboxesIntoCombobox()
  {
    Map<ID, String> mapCheckboxId2ComboboxEntry = new HashMap<ID, String>();
    int count = 0;
    ArrayList<String> itemList = new ArrayList<String>();
    Iterator<Integer> iter = selection.iterator();
    while (iter.hasNext())
    {
      int idx = iter.next().intValue();
      OneFormControlLineView view = ((ViewDescriptor) viewDescriptors.get(idx)).view;
      FormControlModel model = view.getModel();
      if (model.getType() != FormControlModel.CHECKBOX_TYPE)
      {
        Logger.log(L.m("FM4000: Beim Aufruf von Checkbox->Combobox ist ein Element selektiert, das keine Checkbox ist"));
        return null;
      }
      else
      {
        IDManager.ID id = model.getId();
        if (id == null)
        {
          Logger.log(L.m("FM4000: Beim Aufruf von Checkbox->Combobox ist eine Checkbox ohne ID selektiert"));
          return null;
        }
        String label = model.getLabel();
        if (mapCheckboxId2ComboboxEntry.containsValue(label))
        {
          Logger.log(L.m("FM4000: Beim Aufruf von Checkbox->Combobox sind 2 Checkboxen mit gleichem Label selektiert"));
          return null;
        }
        mapCheckboxId2ComboboxEntry.put(id, label);
        itemList.add(label);
        ++count;
      }
    }

    if (count < 2)
    {
      Logger.log(L.m("FM4000: Beim Aufruf von Checkbox->Combobox sind weniger als 2 Checkboxen selektiert"));
      return null;
    }

    String id = formControlModelList.makeUniqueId("CheckCombo");
    String[] items = new String[itemList.size()];
    items = itemList.toArray(items);
    FormControlModel comboModel =
      FormControlModel.createComboBox(L.m("Auswahl"), id, items, formularMax4000);
    comboModel.setEditable(false);
    int index = getInsertionIndex();
    formControlModelList.add(comboModel, index);
    deleteSelectedElements();
    ComboboxMergeDescriptor desc = new ComboboxMergeDescriptor();
    desc.combo = comboModel;
    desc.mapCheckboxId2ComboboxEntry = mapCheckboxId2ComboboxEntry;
    return desc;
  }

  /**
   * Liefert true gdw sich kein ausgewähltes Element auf dem momentan angezeigten Tab
   * befindet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private boolean noSelectedElementsOnVisibleTab()
  {
    int tabIndex = tabPane.getSelectedIndex();
    Iterator<Integer> iter = selection.iterator();
    while (iter.hasNext())
    {
      int i = iter.next().intValue();
      ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
      if (desc.containingTabIndex == tabIndex) return false;
    }
    return true;
  }

  /**
   * Falls mindestens ein Element ausgewählt ist wird das sichtbare Tab so gesetzt,
   * dass das erste ausgewählte Element angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showSelection()
  {
    if (selection.isEmpty()) return;

    int i = selection.firstElement();
    ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
    tabPane.setSelectedIndex(desc.containingTabIndex);
  }

  /**
   * Liefert den Index des ersten momentan sichtbaren vom Benutzer ausgewählten
   * Elements oder (falls kein ausgewähltes Element sichtbar ist) den Index des
   * letzten sichtbaren Elements + 1 oder falls kein Tab ausgewählt ist dann -1.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getInsertionIndex()
  {
    int tabIndex = tabPane.getSelectedIndex();

    /*
     * Zuerst suchen wir die Selektion durch und liefern falls vorhanden das erste
     * ausgewählte Element, das auf dem sichtbaren Tab ist.
     */
    Iterator<Integer> iter = selection.iterator();
    while (iter.hasNext())
    {
      int i = iter.next().intValue();
      ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
      if (desc.containingTabIndex == tabIndex) return i;
    }

    /*
     * Wenn die obige Suche fehlschlägt wird getButtonInsertionIndex() verwendet.
     */
    return getButtonInsertionIndex();
  }

  /**
   * Liefert den Index an dem Buttons auf dem aktuell sichtbaren Tab eingefügt werden
   * sollten oder -1, falls kein Tab ausgewählt ist. Der zurückgelieferte Wert (falls
   * nicht -1) entspricht dem Index des letzten sichtbaren Elements + 1.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getButtonInsertionIndex()
  {
    int tabIndex = tabPane.getSelectedIndex();
    for (int i = 0; i < viewDescriptors.size(); ++i)
    {
      ViewDescriptor desc = (ViewDescriptor) viewDescriptors.get(i);
      if (desc.containingTabIndex > tabIndex) return i;
    }

    return -1;
  }

  /**
   * Fügt vor dem ersten ausgewählten Element (oder ganz am Ende, wenn nichts
   * ausgewählt ist) ein neues Tab zur Liste hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void insertNewTab()
  {
    String id = formControlModelList.makeUniqueId(FormularMax4000.STANDARD_TAB_NAME);
    String label = id;
    FormControlModel model = FormControlModel.createTab(label, id, formularMax4000);
    int index = getInsertionIndex();
    formControlModelList.add(model, index);
  }

  /**
   * Fügt vor dem ersten ausgewählten Element (oder ganz am Ende des sichtbaren Tabs,
   * wenn nichts ausgewählt ist oder die Selektion auf einem unsichtbaren Tab ist)
   * ein neues Steuerelement zur Liste hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void insertNewElement()
  {
    String id = formControlModelList.makeUniqueId("Label");
    FormControlModel model =
      FormControlModel.createLabel(L.m("Text"), id, formularMax4000);
    int index = getInsertionIndex();
    formControlModelList.add(model, index);
  }

  public void itemSwapped(int index1, int index2)
  {
    ViewDescriptor desc1 = (ViewDescriptor) viewDescriptors.get(index1);
    ViewDescriptor desc2 = (ViewDescriptor) viewDescriptors.get(index2);
    viewDescriptors.setElementAt(desc1, index2);
    viewDescriptors.setElementAt(desc2, index1);

    fixTabStructure();

    tabPane.validate();
    scrollPane.validate();

    selection.swap(index1, index2);
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    { // TESTED
      if (b.getClearSelection()) clearSelection();
      FormControlModel model = (FormControlModel) b.getObject();
      for (int index = 0; index < viewDescriptors.size(); ++index)
      {
        OneFormControlLineView view =
          ((ViewDescriptor) viewDescriptors.get(index)).view;
        if (view.getModel() == model)
        {
          int state = b.getState();
          if (state == 0) // toggle
            state = selection.contains(index) ? -1 : 1;

          switch (state)
          {
            case -1: // abwählen
              view.unmark();
              selection.remove(index);
              break;
            case 1: // auswählen
              view.mark();
              selection.add(index);
              break;
          }
        }
      }

    }

    public void broadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
    {
      /*
       * Unabhängige Kopie des Deskriptors machen, da die OneFormControlLineViews
       * sich den Deskriptor merken. Eine Kopie reicht für alle
       * OneFormControlLineViews, da diese keine Änderungen daran vornehmen.
       */
      ViewVisibilityDescriptor newDesc = new ViewVisibilityDescriptor(desc);

      Iterator<Object> iter = viewDescriptors.iterator();
      while (iter.hasNext())
      {
        ViewDescriptor viewDescriptor = (ViewDescriptor) iter.next();
        viewDescriptor.view.setViewVisibilityDescriptor(newDesc);
      }
    }
  }

  /**
   * Ein Eintrag in der Liste {@link AllFormControlLineViewsPanel#viewDescriptors}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class ViewDescriptor
  {
    /**
     * Die View des Elements.
     */
    public OneFormControlLineView view;

    /**
     * Der Index des Elements innerhalb seines Tabs.
     */
    public int gridY;

    /**
     * Der Index des Tabs in {@link AllFormControlLineViewsPanel#tabPane}.
     */
    public int containingTabIndex;

    /**
     * true gdw das Element ein Tab ist.
     */
    public boolean isTab;

    /**
     * Erzeugt einen neuen ViewDescriptor.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public ViewDescriptor(OneFormControlLineView view, int gridY,
        int containingTabIndex, boolean isTab)
    {
      this.view = view;
      this.gridY = gridY;
      this.containingTabIndex = containingTabIndex;
      this.isTab = isTab;
    }
  }
}
