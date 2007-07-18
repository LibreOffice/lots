/*
* Dateiname: AllInsertionTrafoViewsPanel.java
* Projekt  : WollMux
* Funktion : Eine View, die alle OneInsertionExtViews enthält.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.insertion;

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

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine View, die alle 
 * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionExtView}s enthält.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllInsertionTrafoViewsPanel implements View
{
  /**
   * Wird für das CardLayout verwendet als ID-String des leeren Panels, das angezeigt wird, 
   * wenn keine bestimmte Einfügung ausgewählt ist.
   */
  private static final String EMPTY_PANEL = "EMPTY_PANEL";
  
  /**
   * Wird für das CardLayout verwendet als ID-String des Panels, das nur einen Button anzeigt,
   * der für das momentan ausgewählte InsertionModel die entsprechende 
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionExtView}
   * aktiviert.
   */
  private static final String INACTIVE_PANEL = "INACTIVE PANEL";
  
  /**
   * Wird auf alle {@link OneInsertionExtView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;
  
  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl 
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;
  
  /**
   * Das CardLayout, das für {@link #myPanel} verwendet wird.
   */
  private CardLayout cards;
  
  /**
   * Das Model, dessen View im Augenblick angezeigt wird.
   */
  private InsertionModel currentModel;
  
  /**
   * Die Liste der {@link OneInsertionExtView}s in dieser View.
   */
  private List views = new Vector();
  
  /**
   * Enthält alle cardIds (wie von {@link #getCardIdFor(Object)} zurückgeliefert) von
   * InsertionModels mit aktiver Sicht, d,h, von all denen, für die nicht
   * das {@link #INACTIVE_PANEL} angezeigt wird. 
   */
  private Set activeModelCardIds = new HashSet();
  
  /**
   * Erzeugt ein {@link AllInsertionTrafoViewsPanel}, das den Inhalt von
   * insertionModelList anzeigt. 
   * @param funcLib die Funktionsbibliothek, die die Funktionen enthält, die die Views 
   *        zur Auswahl anbieten sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public AllInsertionTrafoViewsPanel(InsertionModelList insertionModelList, FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    this.funcLib = funcLib;
    insertionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    myViewChangeListener = new MyViewChangeListener();
    
    cards = new CardLayout();
    myPanel = new JPanel(cards);
    JPanel emptyPanel = new JPanel();
    emptyPanel.add(new JLabel("TRAFO-View"));
    myPanel.add(emptyPanel, EMPTY_PANEL);
    
    myPanel.add(makeInactivePanel(), INACTIVE_PANEL);
    
    Iterator iter = insertionModelList.iterator();
    while (iter.hasNext()) 
    {
      InsertionModel model = (InsertionModel)iter.next();
      if (model.hasTrafo()) addItem(model);
    }
  }
  
  /**
   * Fügt dieser View eine {@link OneInsertionExtView} für model hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void addItem(InsertionModel model)
  {
    OneInsertionExtView view = new OneInsertionExtView(model, funcLib, myViewChangeListener);
    views.add(view);
    String cardId = getCardIdFor(view.getModel());
    activeModelCardIds.add(cardId);
    myPanel.add(view.JComponent(), cardId);
    myPanel.validate();
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
   * Entfernt view aus diesem Container (falls dort vorhanden).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void removeItem(OneInsertionExtView view)
  {
    int index = views.indexOf(view);
    if (index < 0) return;
    views.remove(index);
    myPanel.remove(view.JComponent());
    myPanel.validate();
    InsertionModel model = view.getModel();
    activeModelCardIds.remove(getCardIdFor(model));
    if (model == currentModel)
    {
      cards.show(myPanel, EMPTY_PANEL);
      currentModel = null;
    }
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }
  
  /**
   * Liefert ein JPanel, das nur einen Button enthält zum Aktivieren der
   * {@link OneInsertionExtView} des aktiven Models.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JPanel makeInactivePanel()
  {
    JPanel inactivePanel = new JPanel();
    inactivePanel.setLayout(new BoxLayout(inactivePanel, BoxLayout.Y_AXIS));
    inactivePanel.add(Box.createGlue());
    Box hbox = Box.createHorizontalBox();
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
  
  
  private class MyItemListener implements InsertionModelList.ItemListener
  {
    public void itemAdded(InsertionModel model, int index)
    {
      if (model.hasTrafo()) addItem(model);
    }

    public void itemRemoved(InsertionModel model, int index)
    {
      if (model == currentModel)
      {
        cards.show(myPanel, EMPTY_PANEL);
        currentModel = null;
      }
    }
  }
  
  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneInsertionExtView)view);
    }
    
  }

  
  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b) 
    {
      cards.show(myPanel, EMPTY_PANEL);
      currentModel = null;
    }
    public void broadcastInsertionModelSelection(BroadcastObjectSelection b) 
    {
      if (b.getState() == 1)
      {
        InsertionModel model = (InsertionModel)b.getObject();
        currentModel = model;
        String cardId = getCardIdFor(model);
        if (activeModelCardIds.contains(cardId))
          cards.show(myPanel, cardId);
        else
          cards.show(myPanel, INACTIVE_PANEL);
      }
      else
      {
        cards.show(myPanel, EMPTY_PANEL);
        currentModel = null;
      }
    }
  }

}
