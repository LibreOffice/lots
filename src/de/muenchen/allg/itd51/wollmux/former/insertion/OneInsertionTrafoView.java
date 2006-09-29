/*
* Dateiname: OneInsertionTrafoView.java
* Projekt  : WollMux
* Funktion : Stellt die TRAFO eines InsertionModels dar und erlaubt ihre Bearbeitung.
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

import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Stellt die TRAFO eines InsertionModels dar und erlaubt ihre Bearbeitung.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneInsertionTrafoView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;
  
  /**
   * Das Model zu dieser View.
   */
  private InsertionModel model;
  
  public OneInsertionTrafoView(InsertionModel model, FunctionLibrary funcLib, ViewChangeListener bigDaddy)
  {
    super(model.getTrafoAccess(), funcLib);
    this.model = model;
    this.bigDaddy = bigDaddy;
    model.addListener(new MyModelChangeListener());
  }
  
  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    public void modelRemoved(InsertionModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneInsertionTrafoView.this);
    }

    public void attributeChanged(InsertionModel model, int attributeId, Object newValue)
    {
    }
  }
  
  /**
   * Liefert das Model zu dieser View.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public InsertionModel getModel()
  {
    return model;
  }

}
