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
package de.muenchen.allg.itd51.wollmux.former.section;

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

import com.sun.star.container.XNamed;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoList;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.Common;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.section.model.SectionModel;
import de.muenchen.allg.itd51.wollmux.former.section.model.SectionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Enthält alle OneSectionLineViews.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllSectionLineViewsPanel implements View
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AllSectionLineViewsPanel.class);

  /**
   * Rand um Buttons (in Pixeln).
   */
  private static final int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Wird auf alle {@link OneSectionLineView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;

  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;

  /**
   * Das JPanel, das die
   * {@link de.muenchen.allg.itd51.wollmux.former.section.OneSectionLineView}s
   * enthält.
   */
  private JPanel mainPanel;

  /**
   * Die JScrollPane, die {@link #mainPanel} enthält.
   */
  private JScrollPane scrollPane;

  /**
   * Die Liste der {@link OneSectionLineView}s in dieser View.
   */
  private List<OneSectionLineView> views = new ArrayList<>();

  /**
   * Liste von Indizes der selektierten Objekte in der {@link #views} Liste.
   */
  private IndexList selection = new IndexList();

  /**
   * Die Liste der Models, zu denen diese View die LineViews enthält.
   */
  private SectionModelList sectionModelList;

  /**
   * Das Dokument des Bereiche angezeigt werden.
   */
  private XTextDocument doc;

  /**
   * Erzeugt ein AllSectionLineViewsPanel, die den Inhalt von sectionModelList
   * anzeigt. ACHTUNG! sectionModelList sollte leer sein, da nur neu hinzugekommene
   * Elemente in der View angezeigt werden.
   */
  public AllSectionLineViewsPanel(SectionModelList sectionModelList,
      FormularMax4kController formularMax4000, XTextDocument doc)
  {
    this.formularMax4000 = formularMax4000;
    this.sectionModelList = sectionModelList;
    this.doc = doc;
    sectionModelList.addListener(new MyItemListener());
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
    JButton button = new JButton(L.m("Aufheben"));
    button.addActionListener(e -> deleteSelectedElements());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("Neu"));
    button.addActionListener(e -> createNewSectionFromSelection());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("Neu (ganze Seiten)"));
    button.addActionListener(e -> createNewSectionFromAllPagesTouchedBySelection());
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;

    for (SectionModel m : sectionModelList)
    {
      addItem(m);
    }
  }

  /**
   * Erzeugt einen neuen Textbereich beginnend beim ersten harten Seitenumbruch vor
   * dem Beginn der aktuellen Selektion (oder dem Dokumentbeginn, falls es keinen
   * solchen gibt) und endend beim ersten harten Seitenumbruch nach dem Ende der
   * aktuellen Selektion (oder dem Dokumentende, falls es keinen solchen gibt) und
   * erzeugt ein zugehöriges {@link SectionModel} und fügt es zu
   * {@link #sectionModelList} hinzu.
   *
   * ACHTUNG! Derzeit wird nur BreakType.PAGE_BEFORE unterstützt. Andere Breaks
   * werden ignoriert.
   */
  private void createNewSectionFromAllPagesTouchedBySelection()
  {
    try
    {
      UnoList<XTextRange> textRanges = UnoList.create(doc.getCurrentSelection(), XTextRange.class);
      XTextRange range = textRanges.get(0);
      XParagraphCursor cursor1 =
        UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));
      XParagraphCursor cursor2 =
        UNO.XParagraphCursor(range.getText().createTextCursorByRange(range));
      cursor1.collapseToStart();
      cursor1.gotoStartOfParagraph(false);
      cursor2.collapseToEnd();

      while (true)
      {
        if (firstParagraphOfRangeHasHardBreak(cursor1)) break;
        if (!cursor1.gotoPreviousParagraph(false)) break;
        cursor1.gotoStartOfParagraph(false);
      }

      while (true)
      {
        /*
         * Es ist wichtig, dass diese Schleife andersrum aufgebaut ist als die für
         * cursor1. Der Test, ob wir das Ende des Dokuments erreicht haben bzw. das
         * um einen Absatz weiterwandern muss vor dem Test auf einen Seitenumbruch
         * geschehen, da wir ansonsten wenn der Cursor bereits zu Anfang in einem
         * Absatz mit Seitenumbruch davor steht, ein falsches Ergebnis produzieren.
         */
        if (!cursor2.gotoNextParagraph(false)) break;
        cursor2.gotoStartOfParagraph(false);

        if (firstParagraphOfRangeHasHardBreak(cursor2))
        {
          // den Absatz mit dem Umbruch davor wollen wir nicht mehr drin haben
          cursor2.gotoPreviousParagraph(false);
          break;
        }
      }

      cursor2.gotoEndOfParagraph(false);
      cursor1.gotoRange(cursor2, true);
      createNewSectionFromTextRange(cursor1);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Liefert true gdw der erste Absatz in range einen BreakType.PAGE_BEFORE hat.
   */
  private boolean firstParagraphOfRangeHasHardBreak(XTextRange range)
  {
    try
    {
      Object paragraph =
        UNO.XEnumerationAccess(range).createEnumeration().nextElement();
      Object breakType = UnoProperty.getProperty(paragraph, UnoProperty.BREAK_TYPE);
      if (breakType != null
        && breakType.equals(com.sun.star.style.BreakType.PAGE_BEFORE)) return true;
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
    return false;
  }

  /**
   * Erzeugt einen neuen Bereich an der Stelle der aktuellen Selektion und erzeugt
   * ein zugehöriges {@link SectionModel} und fügt es zu {@link #sectionModelList}
   * hinzu.
   */
  private void createNewSectionFromSelection()
  {
    try
    {
      UnoList<XTextRange> textRanges = UnoList.create(doc.getCurrentSelection(), XTextRange.class);
      XTextRange range = textRanges.get(0);
      createNewSectionFromTextRange(range);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Erzeugt einen neuen Textbereich, der range umschließt und erzeugt ein
   * zugehöriges {@link SectionModel} und fügt es zu {@link #sectionModelList} hinzu.
   *
   * @throws Exception
   *           , wenn was schief geht
   */
  private void createNewSectionFromTextRange(XTextRange range) throws Exception
  {
    XNamed section = UNO.XNamed(UnoService.createService(UnoService.CSS_TEXT_TEXT_SECTION, doc));
    XTextSectionsSupplier tssupp = UNO.XTextSectionsSupplier(doc);
    UnoDictionary<XTextSection> textSections = UnoDictionary.create(tssupp.getTextSections(), XTextSection.class);
    String baseName = L.m("Sichtbarkeitsbereich");
    int count = 1;
    while (textSections.containsKey(baseName + count))
      ++count;
    String sectionName = baseName + count;
    section.setName(sectionName);
    XTextContent sectionContent = UNO.XTextContent(section);
    doc.getText().insertTextContent(range, sectionContent, true);
    sectionModelList.add(new SectionModel(sectionName, tssupp, formularMax4000));
  }

  /**
   * Fügt dieser View eine {@link OneSectionLineView} für model hinzu.
   */
  private void addItem(SectionModel model)
  {
    OneSectionLineView view =
      new OneSectionLineView(model, myViewChangeListener, formularMax4000);
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
      OneSectionLineView view = views.get(i);
      SectionModel model = view.getModel();
      sectionModelList.remove(model);
      model.removeFromDocument();
    }
  }

  @Override
  public JComponent getComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements SectionModelList.ItemListener
  {

    @Override
    public void itemAdded(SectionModel model, int index)
    {
      addItem(model);
    }

    @Override
    public void itemRemoved(SectionModel model, int index)
    {
      // nothing to do
    }

  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    @Override
    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneSectionLineView) view);
    }
    
    /**
     * Entfernt view aus diesem Container (falls dort vorhanden).
     */
    private void removeItem(OneSectionLineView view)
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
    public void broadcastSectionModelSelection(BroadcastObjectSelection b)
    {
      if (b.getClearSelection()) clearSelection();
      SectionModel model = (SectionModel) b.getObject();

      int selindex = -1;

      for (int index = 0; index < views.size(); ++index)
      {
        OneSectionLineView view = views.get(index);
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
        OneSectionLineView view = views.get(i.intValue());
        view.unmark();
      }
      selection.clear();
    }
  }
}
