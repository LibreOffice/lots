/*
* Dateiname: AllFormControlExtViewsPanel.java
* Projekt  : WollMux
* Funktion : Eine View, die alle OneFormControlExtViews enthält.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 23.10.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.control;

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
 * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlExtView}s enthält.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlExtViewsPanel implements View
{
  /**
   * Wird für das CardLayout verwendet als ID-String des leeren Panels, das angezeigt wird, 
   * wenn keine bestimmte Einfügung ausgewählt ist.
   */
  private static final String EMPTY_PANEL = "EMPTY_PANEL";
  
  /**
   * Wird auf alle {@link OneFormControlExtView}s registriert.
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
   * Das Model dessen View momentan angezeigt wird.
   */
  private FormControlModel currentModel;
  
  /**
   * Die Liste der {@link OneFormControlExtView}s in dieser View.
   */
  private List views = new Vector();
  
  /**
   * Erzeugt ein {@link AllFormControlExtViewsPanel}, das den Inhalt von
   * formControlModelList anzeigt. ACHTUNG! formControlModelList sollte leer sein,
   * da nur neu hinzugekommene Elemente in der View angezeigt werden.
   * @param funcLib die Funktionsbibliothek, die die Funktionen enthält, die die Views 
   *        zur Auswahl anbieten sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public AllFormControlExtViewsPanel(FormControlModelList formControlModelList, FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    this.funcLib = funcLib;
    formControlModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    myViewChangeListener = new MyViewChangeListener();
    
    cards = new CardLayout();
    myPanel = new JPanel(cards);
    JPanel emptyPanel = new JPanel();
    emptyPanel.add(new JLabel("Extra-View"));
    myPanel.add(emptyPanel, EMPTY_PANEL);
  }
  
  /**
   * Fügt dieser View eine {@link OneFormControlExtView} für model hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void addItem(FormControlModel model)
  {
    OneFormControlExtView view = new OneFormControlExtView(model, funcLib, myViewChangeListener);
    views.add(view);
    
    myPanel.add(view.JComponent(), getCardIdFor(model));
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
  private void removeItem(OneFormControlExtView view)
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
  
  
  
  private class MyItemListener implements FormControlModelList.ItemListener
  {

    public void itemAdded(FormControlModel model, int index)
    {
      addItem(model);
    }

    public void itemSwapped(int index1, int index2) {}
  }
  
  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneFormControlExtView)view);
    }
    
  }

  
  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b) 
    {
      if (b.getState() == 1)
      {
        FormControlModel model = (FormControlModel)b.getObject();
        cards.show(myPanel, getCardIdFor(model));
        currentModel = model;
      }
      else
      {
        cards.show(myPanel, EMPTY_PANEL);
        currentModel = null;
      }
    }

    public void broadcastInsertionModelSelection(BroadcastObjectSelection b) 
    {
      cards.show(myPanel, EMPTY_PANEL);
      currentModel = null;
    }
  }

}
