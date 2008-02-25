//TODO L.m()
/*
* Dateiname: OnDemandCardView.java
* Projekt  : WollMux
* Funktion : Basisklasse für Views mit CardLayout für eine Menge von Objekten, wobei die einzelnen Karten erst on-demand erzeugt werden.
* 
* Copyright: Landeshauptstadt München
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionExtView;

/**
 * Basisklasse für Views mit CardLayout für eine Menge von Objekten, wobei die einzelnen
 * Karten erst on-demand erzeugt werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class OnDemandCardView implements View
{
  /**
   * Wird für das CardLayout verwendet als ID-String des leeren Panels, das angezeigt wird, 
   * wenn keine bestimmte Einfügung ausgewählt ist.
   */
  private static final String EMPTY_PANEL = "EMPTY_PANEL";
  
  /**
   * Wird für das CardLayout verwendet als ID-String des Panels, das nur einen Button anzeigt,
   * der für das momentan ausgewählte Objekt die entsprechende 
   * View-Karte aktiviert.
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
   * Die Liste der aktuell angezeigten View-Karten mit ihren IDs 
   * ({@link OnDemandCardView.ViewCardIdPair}).
   */
  private List<ViewCardIdPair> viewCardIdPairs = new Vector<ViewCardIdPair>();
  
  private static class ViewCardIdPair
  {
    public View view;
    public String cardId;
    public ViewCardIdPair(View view, String cardId) {this.view = view; this.cardId = cardId;};
  }
  
  
  /**
   * Enthält alle cardIds (wie von {@link #getCardIdFor(Object)} zurückgeliefert) von
   * InsertionModels mit aktiver Sicht, d,h, von all denen, für die nicht
   * das {@link #INACTIVE_PANEL} angezeigt wird. 
   */
  private Set<String> activeModelCardIds = new HashSet<String>();
  
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
   * @param viewChangeListener wird auf die View registriert, damit die View
   *        mitteilen kann, wenn Sie entfernt werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected abstract View createViewFor(Object model, ViewChangeListener viewChangeListener);
  
  /**
   * Fügt dieser View eine View-Karte für model hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  protected void addItem(Object model)
  {
    View view = createViewFor(model, myViewChangeListener);
    String cardId = getCardIdFor(model);
    viewCardIdPairs.add(new ViewCardIdPair(view, cardId));
    activeModelCardIds.add(cardId);
    myPanel.add(view.JComponent(), cardId);
    myPanel.validate();
  }


  /**
   * Entfernt view aus diesem Container (falls dort vorhanden).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void removeItem(View view)
  {
    Iterator<ViewCardIdPair> iter = viewCardIdPairs.iterator();
    while (iter.hasNext())
    {
      ViewCardIdPair pair = iter.next();
      if (pair.view == view)
      {
        iter.remove();
        myPanel.remove(view.JComponent());
        myPanel.validate();
        activeModelCardIds.remove(pair.cardId);
        if (currentModel != null && getCardIdFor(currentModel).equals(pair.cardId))
          showEmpty();
        
        return;
      }
    }
  }
  
  
  /**
   * Liefert einen Identifikationsstring für ob zur Verwendung mit einem {@link CardLayout}.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getCardIdFor(Object ob)
  {
    return "" + ob.hashCode();
  }

  
  /**
   * Liefert ein JPanel, das nur einen Button enthält zum Aktivieren der
   * {@link OneInsertionExtView} des aktiven Models.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
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
    JButton button = new JButton("Aktivieren");
    hbox.add(button);
    hbox.add(Box.createHorizontalGlue());
    inactivePanel.add(hbox);
    
    inactivePanel.add(Box.createGlue());
    
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        if (currentModel == null) return; //sollte nicht passieren können, aber zur Sicherheit
        addItem(currentModel);
        cards.show(myPanel, getCardIdFor(currentModel));
      }}
    );
    
    return inactivePanel;
  }

  public JComponent JComponent()
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
    cards.show(myPanel, EMPTY_PANEL);
    currentModel = null;    
  }
  
  /**
   * Zeigt die passende View-Karte für model an. Dies ist entweder eine richtige View-Karte,
   * oder die View-Karte zum on-demand aktivieren der Sicht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(Object model)
  {
    currentModel = model;
    String cardId = getCardIdFor(model);
    if (activeModelCardIds.contains(cardId))
      cards.show(myPanel, cardId);
    else
      cards.show(myPanel, INACTIVE_PANEL);
  }
  
  
  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem(view);
    }
    
  }

}
