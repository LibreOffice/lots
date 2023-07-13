/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.former.group;

import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.function.FunctionSelectionAccessView;
import org.libreoffice.lots.former.group.model.GroupModel;
import org.libreoffice.lots.former.view.ViewChangeListener;
import org.libreoffice.lots.func.FunctionLibrary;

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
    {
      // nothing to do
    }
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public GroupModel getModel()
  {
    return model;
  }

}
