/*
 * Dateiname: AllSectionLineViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Enthält alle OneSectionLineViews.
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
 * 24.03.2009 | BNK | Erstellung
 * 23.03.2010 | ERT | [R5721]Unterstützung für Shift-Klick
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

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

import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSectionsSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Enthält alle OneSectionLineViews.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllSectionLineViewsPanel implements View
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
  private List<OneSectionLineView> views = new Vector<OneSectionLineView>();

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
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllSectionLineViewsPanel(SectionModelList sectionModelList,
      FormularMax4000 formularMax4000, XTextDocument doc)
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
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        deleteSelectedElements();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("Neu"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        createNewSectionFromSelection();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("Neu (ganze Seiten)"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        createNewSectionFromAllPagesTouchedBySelection();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
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
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void createNewSectionFromAllPagesTouchedBySelection()
  {
    try
    {
      // doc.getCurrentSelection() ist ein c.s.s.text.TextRanges Service
      XIndexAccess textRanges = UNO.XIndexAccess(doc.getCurrentSelection()); // c.s.s.text.TextRanges
      XTextRange range = UNO.XTextRange(textRanges.getByIndex(0));
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
      Logger.error(x);
    }
  }

  /**
   * Liefert true gdw der erste Absatz in range einen BreakType.PAGE_BEFORE hat.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private boolean firstParagraphOfRangeHasHardBreak(XTextRange range)
  {
    try
    {
      Object paragraph =
        UNO.XEnumerationAccess(range).createEnumeration().nextElement();
      Object breakType = UNO.getProperty(paragraph, "BreakType");
      if (breakType != null
        && breakType.equals(com.sun.star.style.BreakType.PAGE_BEFORE)) return true;
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    return false;
  }

  /**
   * Erzeugt einen neuen Bereich an der Stelle der aktuellen Selektion und erzeugt
   * ein zugehöriges {@link SectionModel} und fügt es zu {@link #sectionModelList}
   * hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void createNewSectionFromSelection()
  {
    try
    {
      // doc.getCurrentSelection() ist ein c.s.s.text.TextRanges Service
      XIndexAccess textRanges = UNO.XIndexAccess(doc.getCurrentSelection()); // c.s.s.text.TextRanges
      XTextRange range = UNO.XTextRange(textRanges.getByIndex(0));
      createNewSectionFromTextRange(range);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Erzeugt einen neuen Textbereich, der range umschließt und erzeugt ein
   * zugehöriges {@link SectionModel} und fügt es zu {@link #sectionModelList} hinzu.
   * 
   * @throws Exception
   *           , wenn was schief geht
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void createNewSectionFromTextRange(XTextRange range) throws Exception
  {
    XNamed section =
      UNO.XNamed(UNO.XMultiServiceFactory(doc).createInstance(
        "com.sun.star.text.TextSection"));
    XTextSectionsSupplier tssupp = UNO.XTextSectionsSupplier(doc);
    XNameAccess textSections = tssupp.getTextSections();
    String baseName = L.m("Sichtbarkeitsbereich");
    int count = 1;
    while (textSections.hasByName(baseName + count))
      ++count;
    String sectionName = baseName + count;
    section.setName(sectionName);
    XTextContent sectionContent = UNO.XTextContent(section);
    doc.getText().insertTextContent(range, sectionContent, true);
    sectionModelList.add(new SectionModel(sectionName, tssupp, formularMax4000));
  }

  /**
   * Fügt dieser View eine {@link OneSectionLineView} für model hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
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
    mainPanel.add(view.JComponent(), mainPanel.getComponentCount() - 1);

    mainPanel.validate();
    scrollPane.validate();
  }

  /**
   * Entfernt view aus diesem Container (falls dort vorhanden).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void removeItem(OneSectionLineView view)
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
      OneSectionLineView view = views.get(I.intValue());
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
      OneSectionLineView view = views.get(i);
      SectionModel model = view.getModel();
      sectionModelList.remove(model);
      model.removeFromDocument();
    }
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements SectionModelList.ItemListener
  {

    public void itemAdded(SectionModel model, int index)
    {
      addItem(model);
    }

    public void itemRemoved(SectionModel model, int index)
    {}

  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneSectionLineView) view);
    }

  }

  private class MyBroadcastListener extends BroadcastListener
  {
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
