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
import java.util.List;
import java.util.Vector;

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
   * Erzeugt ein {@link AllInsertionTrafoViewsPanel}, das den Inhalt von
   * insertionModelList anzeigt. ACHTUNG! insertionModelList sollte leer sein,
   * da nur neu hinzugekommene Elemente in der View angezeigt werden.
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
    
    myPanel.add(view.JComponent(), getCardIdFor(view.getModel()));
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
    if (view.getModel() == currentModel)
    {
      cards.show(myPanel, EMPTY_PANEL);
      currentModel = null;
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
        cards.show(myPanel, getCardIdFor(model));
        currentModel = model;
      }
      else
      {
        cards.show(myPanel, EMPTY_PANEL);
        currentModel = null;
      }
    }
  }

}
