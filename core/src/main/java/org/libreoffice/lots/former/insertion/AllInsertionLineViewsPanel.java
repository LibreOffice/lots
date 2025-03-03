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
package org.libreoffice.lots.former.insertion;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.lots.former.BroadcastListener;
import org.libreoffice.lots.former.BroadcastObjectSelection;
import org.libreoffice.lots.former.Common;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.IndexList;
import org.libreoffice.lots.former.insertion.model.InsertionModel;
import org.libreoffice.lots.former.insertion.model.InsertionModel4InsertXValue;
import org.libreoffice.lots.former.insertion.model.InsertionModelList;
import org.libreoffice.lots.former.view.View;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.util.L;

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
  private static final int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View gehört.
   */
  private FormularMax4kController formularMax4000;

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
   * {@link org.libreoffice.lots.former.insertion.OneInsertionLineView}s
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
  private List<OneInsertionLineView> views = new ArrayList<>();

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
   */
  public AllInsertionLineViewsPanel(InsertionModelList insertionModelList,
      FormularMax4kController formularMax4000)
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
    JButton button = new JButton(L.m("Remove (DeMux)"));
    button.addActionListener(e -> demuxSelectedElements());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;

    for (InsertionModel m : insertionModelList)
    {
      addItem(m);
    }
  }

  /**
   * Fügt dieser View eine {@link OneInsertionLineView} für model hinzu.
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
    mainPanel.add(view.getComponent(), mainPanel.getComponentCount() - 1);

    mainPanel.validate();
    scrollPane.validate();
  }

  /**
   * Löscht die WollMux-Bookmarks um alle ausgewählten Elemente. Da es damit keine
   * Einfügestellen mehr sind, werden die entsprechenden LineViews ebenfalls
   * entfernt.
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
      catch (ClassCastException | UnoHelperException x)
      {
        // InsertionModel4InputUser can't be deMux'ed
      }
    }
  }

  @Override
  public JComponent getComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements InsertionModelList.ItemListener
  {

    @Override
    public void itemAdded(InsertionModel model, int index)
    {
      addItem(model);
    }

    @Override
    public void itemRemoved(InsertionModel model, int index)
    {
      // nothing to do
    }

  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    @Override
    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneInsertionLineView) view);
    }

    /**
     * Entfernt view aus diesem Container (falls dort vorhanden).
     */
    private void removeItem(OneInsertionLineView view)
    {
      int index = views.indexOf(view);
      if (index < 0) return;
      views.remove(index);
      mainPanel.remove(view.getComponent());
      mainPanel.validate();
      selection.remove(index);
      selection.fixup(index, -1, views.size() - 1);
    }

  }

  private class MyBroadcastListener implements BroadcastListener
  {
    @Override
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b)
    {
      // nothing to do
    }

    @Override
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
            default:
              break;
          }
        }
        else if (b.getState() == BroadcastObjectSelection.STATE_SHIFT_CLICK
            && ((selindex == -1 && (index > selection.firstElement() || index > selection.lastElement()))
                || (selindex != -1
                    && (index > selindex && (index < selection.firstElement() || index < selection.lastElement())))))
        {
          view.mark();
          selection.add(index);
        }
      }
    }

    /**
     * Hebt die Selektion aller Elemente auf.
     */
    private void clearSelection()
    {
      Iterator<Integer> iter = selection.iterator();
      while (iter.hasNext())
      {
        Integer i = iter.next();
        OneInsertionLineView view = views.get(i.intValue());
        view.unmark();
      }
      selection.clear();
    }
  }

}
