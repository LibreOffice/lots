/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.group;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.Common;
import de.muenchen.allg.itd51.wollmux.former.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Enthält alle OneInsertionLineViews.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllGroupLineViewsPanel implements View
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AllGroupLineViewsPanel.class);
  
  /**
   * Rand um Buttons (in Pixeln).
   */
  private static final int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Wird auf alle {@link OneGroupLineView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;

  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;

  /**
   * Das JPanel, das die
   * {@link de.muenchen.allg.itd51.wollmux.former.group.OneGroupLineView}s enthält.
   */
  private JPanel mainPanel;

  /**
   * Die JScrollPane, die {@link #mainPanel} enthält.
   */
  private JScrollPane scrollPane;

  /**
   * Die Liste der {@link OneGroupLineView}s in dieser View.
   */
  private List<OneGroupLineView> views = new ArrayList<>();

  /**
   * Liste von Indizes der selektierten Objekte in der {@link #views} Liste.
   */
  private IndexList selection = new IndexList();

  /**
   * Die Liste der Models, zu denen diese View die LineViews enthält.
   */
  private GroupModelList groupModelList;

  /**
   * Erzeugt ein AllGroupLineViewsPanel, die den Inhalt von groupModelList anzeigt.
   * ACHTUNG! groupModelList sollte leer sein, da nur neu hinzugekommene Elemente in
   * der View angezeigt werden.
   */
  public AllGroupLineViewsPanel(GroupModelList groupModelList,
      FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    this.groupModelList = groupModelList;
    groupModelList.addListener(new MyItemListener());
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
    JButton button = new JButton(L.m("Delete"));
    button.addActionListener(e -> deleteSelectedElements());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("New"));
    button.addActionListener(e -> createNewGroup());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;

    for (GroupModel m : groupModelList)
    {
      addItem(m);
    }
  }

  private void createNewGroup()
  {
    int num = 1;
    IdModel id;
    while (true)
    {
      try
      {
        id =
          formularMax4000.getIDManager().getActiveID(
            FormularMax4kController.NAMESPACE_GROUPS, "Sichtbarkeit" + num);
        break;
      }
      catch (DuplicateIDException x)
      {
        ++num;
      }
    }
    FunctionSelection condition = new FunctionSelection();
    ConfigThingy funConf = new ConfigThingy("Code");
    funConf.add("CAT").add("true");
    condition.setExpertFunction(funConf);
    GroupModel model = new GroupModel(id, condition, formularMax4000);
    groupModelList.add(model);
  }

  /**
   * Fügt dieser View eine {@link OneGroupLineView} für model hinzu.
   */
  private void addItem(GroupModel model)
  {
    OneGroupLineView view =
      new OneGroupLineView(model, myViewChangeListener, formularMax4000);
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
   * Löscht alle ausgewählten Elemente.
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
      OneGroupLineView view = views.get(i);
      GroupModel model = view.getModel();
      groupModelList.remove(model);

      /**
       * Es gibt keine Möglichkeit, eine ID aus dem IDManager zu entfernen und ein
       * Inaktivieren reicht nicht, um Namenskollisionen zu verhindern. Deswegen
       * benennen wir die ID einfach um in einen zufälligen String.
       */
      IdModel id = model.getID();
      while (true)
      {
        try
        {
          id.setID("ExistiertNichtMehr" + Math.random());
          break;
        }
        catch (DuplicateIDException x)
        {
          LOGGER.trace("", x);
        }
      }
    }
  }

  @Override
  public JComponent getComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements GroupModelList.ItemListener
  {

    @Override
    public void itemAdded(GroupModel model, int index)
    {
      addItem(model);
    }

    @Override
    public void itemRemoved(GroupModel model, int index)
    {
      // nothing to do
    }

  }

  private class MyViewChangeListener implements ViewChangeListener
  {
    @Override
    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneGroupLineView) view);
    }
    
    /**
     * Entfernt view aus diesem Container (falls dort vorhanden).
     */
    private void removeItem(OneGroupLineView view)
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
    public void broadcastGroupModelSelection(BroadcastObjectSelection b)
    {
      if (b.getClearSelection()) clearSelection();
      GroupModel model = (GroupModel) b.getObject();

      int selindex = -1;

      for (int index = 0; index < views.size(); ++index)
      {
        OneGroupLineView view = views.get(index);
        if (view.getModel() == model)
        {
          int state = b.getState();
          if (state == BroadcastObjectSelection.STATE_CTRL_CLICK) // toggle
            state = selection.contains(index) ? -1 : 1;
          else if (state == BroadcastObjectSelection.STATE_SHIFT_CLICK)
          {
            state = 1;
          }

          if (state == -1) {
            view.unmark();
            selection.remove(index);
          }
          else {
            view.mark();
            selection.add(index);
          }

          break;
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
        OneGroupLineView view = views.get(i.intValue());
        view.unmark();
      }
      selection.clear();
    }

}}
