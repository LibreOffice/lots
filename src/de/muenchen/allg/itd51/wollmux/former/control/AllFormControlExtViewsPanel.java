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
 * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlExtView}s enthält.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlExtViewsPanel extends OnDemandCardView
{
  /**
   * Die Funktionsbibliothek, die die Funktionen enthält, die die Views zur Auswahl 
   * anbieten sollen.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Erzeugt ein {@link AllFormControlExtViewsPanel}, das den Inhalt von
   * formControlModelList anzeigt. 
   * @param funcLib die Funktionsbibliothek, die die Funktionen enthält, die die Views 
   *        zur Auswahl anbieten sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public AllFormControlExtViewsPanel(FormControlModelList formControlModelList, FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    super("Extra-View");
    this.funcLib = funcLib;
    formControlModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    
    Iterator iter = formControlModelList.iterator();
    while (iter.hasNext()) 
    {
      FormControlModel model = (FormControlModel)iter.next();
      if (model.hasPlausi() || model.hasAutofill()) addItem(model);
    }
  }
  
  public View createViewFor(Object model, ViewChangeListener viewChangeListener)
  {
    FormControlModel m = (FormControlModel)model;
    return new OneFormControlExtView(m, funcLib, viewChangeListener); 
  }
  
  
  
  private class MyItemListener implements FormControlModelList.ItemListener
  {

    public void itemAdded(FormControlModel model, int index)
    {
      if (model.hasPlausi() || model.hasAutofill()) addItem(model);
    }

    public void itemSwapped(int index1, int index2) {}
  }
  

  
  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastObjectSelection b) 
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

    public void broadcastInsertionModelSelection(BroadcastObjectSelection b) 
    {
      showEmpty();
    }
  }

}
