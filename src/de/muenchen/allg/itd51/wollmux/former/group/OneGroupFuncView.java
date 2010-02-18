/*
 * Dateiname: OneGroupFuncView.java
 * Projekt  : WollMux
 * Funktion : Stellt die Sichtbarkeitsfunktion einer Sichtbarkeitsgruppe dar und erlaubt deren Bearbeitung.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

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
      FormularMax4000.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.bigDaddy = bigDaddy;
    model.addListener(new MyModelChangeListener());
  }

  private class MyModelChangeListener implements GroupModel.ModelChangeListener
  {
    public void modelRemoved(GroupModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneGroupFuncView.this);
    }

    public void attributeChanged(GroupModel model, int attributeId, Object newValue)
    {}
  }

  /**
   * Liefert das Model zu dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModel getModel()
  {
    return model;
  }

}
