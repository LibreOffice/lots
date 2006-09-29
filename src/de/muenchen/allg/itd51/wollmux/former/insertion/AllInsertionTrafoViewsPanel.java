/*
* Dateiname: AllInsertionTrafoViewsPanel.java
* Projekt  : WollMux
* Funktion : Eine View, die alle OneInsertionTrafoViews enthält.
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
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine View, die alle 
 * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionTrafoView}s enthält.
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
   * Wird auf alle {@link OneInsertionTrafoView}s registriert.
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
   * Die Liste der {@link OneInsertionTrafoView}s in dieser View.
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
    myPanel.add(new JPanel(), EMPTY_PANEL);
  }
  
  /**
   * Fügt dieser View eine {@link OneInsertionTrafoView} für model hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void addItem(InsertionModel model)
  {
    OneInsertionTrafoView view = new OneInsertionTrafoView(model, funcLib, myViewChangeListener);
    views.add(view);
    
    myPanel.add(view.JComponent(), getCardIdFor(view));
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
  private void removeItem(OneInsertionTrafoView view)
  {
    int index = views.indexOf(view);
    if (index < 0) return;
    views.remove(index);
    myPanel.remove(view.JComponent());
    myPanel.validate();
    //FIXME Wenn view die aktuell angezeigte Karte war, dann muss auf das leere Panel umgeschaltet werden
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
      removeItem((OneInsertionTrafoView)view);
    }
    
  }

  
  private class MyBroadcastListener extends BroadcastListener
  {
    
  }

}
