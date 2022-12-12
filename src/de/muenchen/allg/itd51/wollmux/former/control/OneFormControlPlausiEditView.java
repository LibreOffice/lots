/*
 * Dateiname: OneFormControlPlausiEditView.java
 * Projekt  : WollMux
 * Funktion : Stellt das PLAUSI-Attribut eines FormControlModels dar und erlaubt seine Bearbeitung.
 * 
 * Copyright (c) 2008-2023 Landeshauptstadt München
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
 * 24.10.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.control;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Stellt das PLAUSI-Attribut eines FormControlModels dar und erlaubt seine
 * Bearbeitung.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlPlausiEditView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

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
   *          die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden
   *          sollen.
   * @param myViewChangeListener
   *          typischerweise ein Container, der diese View enthält und über
   *          Änderungen informiert werden soll.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public OneFormControlPlausiEditView(FormControlModel model,
      FunctionLibrary funcLib, ViewChangeListener bigDaddy)
  {
    super(model.getPlausiAccess(), funcLib,
      model.getFormularMax4000().getIDManager(),
      FormularMax4kController.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.bigDaddy = bigDaddy;
    listener = new MyModelChangeListener();
    model.addListener(listener);
  }

  /**
   * Gibt von dieser View belegte Ressourcen frei, damit diese View gc'ed werden
   * kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
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
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneFormControlPlausiEditView.this);
    }

    @Override
    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue)
    {}
  }

  /**
   * Liefert das Model zu dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

}
