/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.former.control;

import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.control.model.FormControlModel;
import org.libreoffice.lots.former.function.FunctionSelectionAccessView;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.func.FunctionLibrary;

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
    {
      // nothing to do
    }
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public FormControlModel getModel()
  {
    return model;
  }

}
