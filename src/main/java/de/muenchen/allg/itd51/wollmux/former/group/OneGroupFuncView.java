package de.muenchen.allg.itd51.wollmux.former.group;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Stellt die Sichtbarkeitsfunktion einer Sichtbarkeitsgruppe dar und erlaubt deren
 * Bearbeitung.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OneGroupFuncView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Das Model zu dieser View.
   */
  private GroupModel model;

  public OneGroupFuncView(GroupModel model, FunctionLibrary funcLib,
      ViewChangeListener bigDaddy)
  {
    super(model.getConditionAccess(), funcLib,
      model.getFormularMax4000().getIDManager(),
      FormularMax4kController.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.bigDaddy = bigDaddy;
    model.addListener(new MyModelChangeListener());
  }

  private class MyModelChangeListener implements GroupModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(GroupModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneGroupFuncView.this);
    }

    @Override
    public void attributeChanged(GroupModel model, int attributeId, Object newValue)
    {}
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public GroupModel getModel()
  {
    return model;
  }

}
