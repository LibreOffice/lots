/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.insertion;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Stellt die TRAFO eines InsertionModels dar und erlaubt ihre Bearbeitung.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneInsertionTrafoView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Das Model zu dieser View.
   */
  private InsertionModel model;

  public OneInsertionTrafoView(InsertionModel model, FunctionLibrary funcLib,
      ViewChangeListener bigDaddy)
  {
    super(model.getTrafoAccess(), funcLib,
      model.getFormularMax4000().getIDManager(),
      FormularMax4kController.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.bigDaddy = bigDaddy;
    model.addListener(new MyModelChangeListener());
  }

  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(InsertionModel model)
    {
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneInsertionTrafoView.this);
    }

    @Override
    public void attributeChanged(InsertionModel model, int attributeId,
        Object newValue)
    {}
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public InsertionModel getModel()
  {
    return model;
  }

}
