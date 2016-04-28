/*
 * Dateiname: OnDemandCardView.java
 * Projekt  : WollMux
 * Funktion : Basisklasse für Views mit CardLayout für eine Menge von Objekten, wobei die einzelnen Karten erst on-demand erzeugt werden.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 19.07.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.view;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Basisklasse für Views mit CardLayout für eine Menge von Objekten, wobei die
 * einzelnen Karten erst on-demand erzeugt werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class OnDemandCardView implements View
{
  /**
   * Wird für das CardLayout verwendet als ID-String des leeren Panels, das angezeigt
   * wird, wenn keine bestimmte Einfügung ausgewählt ist.
   */
  private static final String EMPTY_PANEL = "EMPTY_PANEL";

  /**
   * Wird für das CardLayout verwendet als ID-String des Panels, das nur einen Button
   * anzeigt, der für das momentan ausgewählte Objekt die entsprechende View-Karte
   * aktiviert.
   */
  private static final String INACTIVE_PANEL = "INACTIVE PANEL";

  /**
   * Wird auf alle View-Karten registriert.
   */
  private ViewChangeListener myViewChangeListener = new MyViewChangeListener();

  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;

  /**
   * Das CardLayout, das für {@link #myPanel} verwendet wird.
   */
  private CardLayout cards;

  /**
   * Das Model, dessen View-Karte im Augenblick angezeigt wird.
   */
  private Object currentModel;

  /**
   * Bildet ein Model auf das zugehörige {@link ViewCardIdPair} ab.
   */
  private Map<Object, ViewCardIdPair> mapModel2ViewDescriptor =
    new HashMap<Object, ViewCardIdPair>();

  private static class ViewCardIdPair
  {
    public View view;

    public String cardId;

    public ViewCardIdPair(View view, String cardId)
    {
      this.view = view;
      this.cardId = cardId;
    };
  }

  public OnDemandCardView(String label)
  {
    cards = new CardLayout();
    myPanel = new JPanel(cards);
    JPanel emptyPanel = new JPanel();
    emptyPanel.add(new JLabel(label));
    myPanel.add(emptyPanel, EMPTY_PANEL);

    myPanel.add(makeInactivePanel(label), INACTIVE_PANEL);
  }

  /**
   * Liefert eine View-Karte für model.
   * 
   * @param viewChangeListener
   *          wird auf die View registriert, damit die View mitteilen kann, wenn Sie
   *          entfernt werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected abstract View createViewFor(Object model,
      ViewChangeListener viewChangeListener);

  /**
   * Fügt dieser View eine View-Karte für model hinzu. Falls die zugehörige View eine
   * {@link LazyView} ist, wird sie erst bei einem entsprechenden
   * {@link #show(Object)} initialisiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  protected void addItem(Object model)
  {
    View view = createViewFor(model, myViewChangeListener);
    String cardId = getCardIdFor(model);
    mapModel2ViewDescriptor.put(model, new ViewCardIdPair(view, cardId));
    myPanel.add(view.getComponent(), cardId);
    myPanel.validate();
  }

  /**
   * Entfernt view aus diesem Container (falls dort vorhanden). DIESE FUNKTION IST
   * PRIVATE UND MUSS AUCH NICHT PROTECTED SEIN. DAS ENTFERNEN VON VIEWS HANDHABT DIE
   * OnDemandCardView selbstständig über {@link MyViewChangeListener}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void removeItem(View view)
  {
    Iterator<Map.Entry<Object, ViewCardIdPair>> iter =
      mapModel2ViewDescriptor.entrySet().iterator();
    while (iter.hasNext())
    {
      Map.Entry<Object, ViewCardIdPair> entry = iter.next();
      ViewCardIdPair pair = entry.getValue();
      if (pair.view == view)
      {
        iter.remove();
        myPanel.remove(view.getComponent());
        myPanel.validate();
        if (currentModel != null && getCardIdFor(currentModel).equals(pair.cardId))
          showEmpty();

        return;
      }
    }
  }

  /**
   * Liefert einen Identifikationsstring für ob zur Verwendung mit einem
   * {@link CardLayout}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getCardIdFor(Object ob)
  {
    return "" + ob.hashCode();
  }

  /**
   * Liefert ein JPanel, das nur einen Button enthält zum Aktivieren der One...View
   * des aktiven Models.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JPanel makeInactivePanel(String label)
  {
    JPanel inactivePanel = new JPanel();
    inactivePanel.setLayout(new BoxLayout(inactivePanel, BoxLayout.Y_AXIS));

    inactivePanel.add(Box.createVerticalStrut(5));

    Box hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalGlue());
    hbox.add(new JLabel(label));
    hbox.add(Box.createHorizontalGlue());
    inactivePanel.add(hbox);

    inactivePanel.add(Box.createGlue());

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalGlue());
    JButton button = new JButton(L.m("Aktivieren"));
    hbox.add(button);
    hbox.add(Box.createHorizontalGlue());
    inactivePanel.add(hbox);

    inactivePanel.add(Box.createGlue());

    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (currentModel == null) return; // sollte nicht passieren können, aber zur
        // Sicherheit
        addItem(currentModel);
        show(currentModel);
      }
    });

    return inactivePanel;
  }

  public JComponent getComponent()
  {
    return myPanel;
  }

  /**
   * Zeigt die leer View-Karte an.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void showEmpty()
  {
    if (currentModel != null)
    {
      ViewCardIdPair oldPair = mapModel2ViewDescriptor.get(currentModel);
      if (oldPair != null && oldPair.view instanceof LazyView)
        ((LazyView) oldPair.view).viewIsNotVisible();
    }

    cards.show(myPanel, EMPTY_PANEL);
    currentModel = null;
  }

  /**
   * Zeigt die passende View-Karte für model an. Dies ist entweder eine richtige
   * View-Karte, oder die View-Karte zum on-demand aktivieren der Sicht. Falls die
   * zugehörige View eine {@link LazyView} ist, so wird diese initialisiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(Object model)
  {
    if (currentModel != model && currentModel != null)
    {
      ViewCardIdPair oldPair = mapModel2ViewDescriptor.get(currentModel);
      if (oldPair != null && oldPair.view instanceof LazyView)
        ((LazyView) oldPair.view).viewIsNotVisible();
    }

    currentModel = model;
    ViewCardIdPair pair = mapModel2ViewDescriptor.get(model);
    if (pair == null)
      cards.show(myPanel, INACTIVE_PANEL);
    else
    {
      cards.show(myPanel, pair.cardId);
      if (pair.view instanceof LazyView) ((LazyView) pair.view).viewIsVisible();
    }
  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem(view);
    }

  }

}
