/*
 * Dateiname: AllInsertionLineViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Enthält alle OneInsertionLineViews.
 * 
 * Copyright (c) 2010 Landeshauptstadt München
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
 * 13.09.2006 | BNK | Erstellung
 * 23.03.2010 | ERT | [R5721]Unterstützung für Shift-Klick
 * 20.04.2010 | BED | "Löschen"-Button weg, "DeMux" umbenannt in "Entfernen (DeMux)" 
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Enthält alle OneInsertionLineViews.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllInsertionLineViewsPanel implements View
{
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View gehört.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Wird auf alle {@link OneInsertionLineView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;

  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;

  /**
   * Das JPanel, das die
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionLineView}s
   * enthält.
   */
  private JPanel mainPanel;

  /**
   * Die JScrollPane, die {@link #mainPanel} enthält.
   */
  private JScrollPane scrollPane;

  /**
   * Die Liste der {@link OneInsertionLineView}s in dieser View.
   */
  private List<OneInsertionLineView> views = new Vector<OneInsertionLineView>();

  /**
   * Liste von Indizes der selektierten Objekte in der {@link #views} Liste.
   */
  private IndexList selection = new IndexList();

  /**
   * Die Liste der Models, zu denen diese View die LineViews enthält.
   */
  private InsertionModelList insertionModelList;

  /**
   * Erzeugt ein AllInsertionLineViewsPanel, die den Inhalt von insertionModelList
   * anzeigt. ACHTUNG! insertionModelList sollte leer sein, da nur neu hinzugekommene
   * Elemente in der View angezeigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllInsertionLineViewsPanel(InsertionModelList insertionModelList,
      FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    this.insertionModelList = insertionModelList;
    insertionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    myViewChangeListener = new MyViewChangeListener();

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    mainPanel.add(Box.createGlue());

    scrollPane =
      new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(
      Common.getVerticalScrollbarUnitIncrement());

    JPanel buttonPanel = new JPanel(new GridBagLayout());

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(scrollPane, BorderLayout.CENTER);
    myPanel.add(buttonPanel, BorderLayout.SOUTH);

    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    JButton button = new JButton(L.m("Entfernen (DeMux)"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        demuxSelectedElements();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;

  }

  /**
   * Fügt dieser View eine {@link OneInsertionLineView} für model hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void addItem(InsertionModel model)
  {
    OneInsertionLineView view =
      new OneInsertionLineView(model, myViewChangeListener, formularMax4000);
    views.add(view);

    /*
     * view vor dem letzten Element von mainPanel einfügen, weil das letzte Element
     * immer ein Glue sein soll.
     */
    mainPanel.add(view.JComponent(), mainPanel.getComponentCount() - 1);

    mainPanel.validate();
    scrollPane.validate();
  }

  /**
   * Entfernt view aus diesem Container (falls dort vorhanden).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void removeItem(OneInsertionLineView view)
  {
    int index = views.indexOf(view);
    if (index < 0) return;
    views.remove(index);
    mainPanel.remove(view.JComponent());
    mainPanel.validate();
    selection.remove(index);
    selection.fixup(index, -1, views.size() - 1);
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
      OneInsertionLineView view = views.get(I.intValue());
      view.unmark();
    }
    selection.clear();
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
     * da über {@link ViewChangeListener#viewShouldBeRemoved(View)} die Selektion
     * während des remove() gleich verändert wird, was den Iterator invalidieren
     * würde.
     */
    while (!selection.isEmpty())
    {
      int i = selection.lastElement();
      OneInsertionLineView view = views.get(i);
      InsertionModel model = view.getModel();
      model.removeFromDocument();
      insertionModelList.remove(model);
    }
  }

  /**
   * Löscht die WollMux-Bookmarks um alle ausgewählten Elemente. Da es damit keine
   * Einfügestellen mehr sind, werden die entsprechenden LineViews ebenfalls
   * entfernt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void demuxSelectedElements()
  {
    /**
     * Die folgende Schleife muss auf diese Weise geschrieben werden und nicht mit
     * einem Iterator, weil es ansonsten eine ConcurrentModificationException gibt,
     * da über {@link ViewChangeListener#viewShouldBeRemoved(View)} die Selektion
     * während des remove() gleich verändert wird, was den Iterator invalidieren
     * würde.
     */
    while (!selection.isEmpty())
    {
      int i = selection.lastElement();
      OneInsertionLineView view = views.get(i);
      try
      {
        // throws ClassCastException for other InsertionModel subtypes
        InsertionModel4InsertXValue model =
          (InsertionModel4InsertXValue) view.getModel();
        model.removeBookmark();
        insertionModelList.remove(model);
      }
      catch (ClassCastException x)
      {
        // InsertionModel4InputUser can't be deMux'ed
      }
    }
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements InsertionModelList.ItemListener
  {

    public void itemAdded(InsertionModel model, int index)
    {
      addItem(model);
    }

    public void itemRemoved(InsertionModel model, int index)
    {}

  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneInsertionLineView) view);
    }

  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    {}

    public void broadcastInsertionModelSelection(BroadcastObjectSelection b)
    {
      if (b.getClearSelection()) clearSelection();
      InsertionModel model = (InsertionModel) b.getObject();

      int selindex = -1;

      for (int index = 0; index < views.size(); ++index)
      {
        OneInsertionLineView view = views.get(index);

        if (view.getModel() == model)
        {
          int state = b.getState();
          if (state == BroadcastObjectSelection.STATE_CTRL_CLICK) // toggle
            state = selection.contains(index) ? -1 : 1;
          else if (state == BroadcastObjectSelection.STATE_SHIFT_CLICK)
          {
            state = 1;
            selindex = index;
          }

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
        else if (b.getState() == BroadcastObjectSelection.STATE_SHIFT_CLICK)
        {
          boolean sel = false;
          if ((selindex == -1 && (index > selection.firstElement() || index > selection.lastElement())))
            sel = true;
          else if (selindex != -1
            && (index > selindex && (index < selection.firstElement() || index < selection.lastElement())))
            sel = true;

          if (sel)
          {
            view.mark();
            selection.add(index);
          }
        }
      }
    }
  }

}
