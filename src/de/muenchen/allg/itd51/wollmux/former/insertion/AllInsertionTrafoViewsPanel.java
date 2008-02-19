//TODO L.m()
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

import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.OnDemandCardView;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine View, die alle 
 * {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionExtView}s enthält.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllInsertionTrafoViewsPanel extends OnDemandCardView
{
  
  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl 
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;
  
  
  
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
    super("Trafo-View");
    this.funcLib = funcLib;
    insertionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    
    Iterator iter = insertionModelList.iterator();
    while (iter.hasNext()) 
    {
      InsertionModel model = (InsertionModel)iter.next();
      if (model.hasTrafo()) addItem(model);
    }
  }
  
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    InsertionModel m = (InsertionModel)model;
    return new OneInsertionExtView(m, funcLib, viewChangeListener);
  }
  
  private class MyItemListener implements InsertionModelList.ItemListener
  {
    public void itemAdded(InsertionModel model, int index)
    {
      if (model.hasTrafo()) addItem(model);
    }

    public void itemRemoved(InsertionModel model, int index)
    {
    }
  }
  
  
  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b) 
    {
      showEmpty();
    }
    public void broadcastInsertionModelSelection(BroadcastObjectSelection b) 
    {
      if (b.getState() == 1)
      {
        show(b.getObject());
      }
      else
      {
        showEmpty();
      }
    }
  }

}
