package de.muenchen.allg.itd51.wollmux.former.control;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Stellt das AUTOFILL-Attribut eines FormControlModels dar und erlaubt seine
 * Bearbeitung.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlAutofillEditView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener viewChangeListener;

  /**
   * Das Model zu dieser View.
   */
  private FormControlModel model;

  private MyModelChangeListener listener;

  /**
   * Erzeugt eine neue View.
   *
   * @param model
   *          das Model dessen Daten angezeigt werden sollen.
   * @param funcLib
   *          die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden sollen.
   * @param viewChangeListener
   *          typischerweise ein Container, der diese View enthält und über Änderungen informiert
   *          werden soll.
   */
  public OneFormControlAutofillEditView(FormControlModel model,
      FunctionLibrary funcLib, ViewChangeListener viewChangeListener)
  {
    super(model.getAutofillAccess(), funcLib,
      model.getFormularMax4000().getIDManager(),
      FormularMax4kController.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.viewChangeListener = viewChangeListener;
    listener = new MyModelChangeListener();
    model.addListener(listener);
  }

  /**
   * Gibt von dieser View belegte Ressourcen frei, damit diese View gc'ed werden
   * kann.
   */
  public void dispose()
  {
    model.removeListener(listener);
  }

  private class MyModelChangeListener implements
      FormControlModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(FormControlModel model)
    {
      if (viewChangeListener != null)
        viewChangeListener.viewShouldBeRemoved(OneFormControlAutofillEditView.this);
    }

    @Override
    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue)
    {}
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public FormControlModel getModel()
  {
    return model;
  }

}
