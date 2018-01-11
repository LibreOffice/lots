/*
 * Dateiname: OneInsertionTrafoView.java
 * Projekt  : WollMux
 * Funktion : Stellt die TRAFO eines InsertionModels dar und erlaubt ihre Bearbeitung.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public InsertionModel getModel()
  {
    return model;
  }

}
