//TODO L.m()
/*
* Dateiname: OneFormControlAutofillEditView.java
* Projekt  : WollMux
* Funktion : Stellt das AUTOFILL-Attribut eines FormControlModels dar und erlaubt seine Bearbeitung.
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330/5980
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

import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccessView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Stellt das AUTOFILL-Attribut eines FormControlModels dar und erlaubt seine Bearbeitung.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlAutofillEditView extends FunctionSelectionAccessView
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;
  
  /**
   * Das Model zu dieser View.
   */
  private FormControlModel model;
  
  /**
   * Erzeugt eine neue View.
   * @param model das Model dessen Daten angezeigt werden sollen.
   * @param funcLib die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden sollen.
   * @param myViewChangeListener typischerweise ein Container, der diese View enthält und über
   *        Änderungen informiert werden soll.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneFormControlAutofillEditView(FormControlModel model, FunctionLibrary funcLib, ViewChangeListener bigDaddy)
  {
    super(model.getAutofillAccess(), funcLib, model.getFormularMax4000().getIDManager(), FormularMax4000.NAMESPACE_FORMCONTROLMODEL);
    this.model = model;
    this.bigDaddy = bigDaddy;
    model.addListener(new MyModelChangeListener());
  }
  
  private class MyModelChangeListener implements FormControlModel.ModelChangeListener
  {
    public void modelRemoved(FormControlModel model)
    {
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneFormControlAutofillEditView.this);
    }

    public void attributeChanged(FormControlModel model, int attributeId, Object newValue)
    {
    }
  }
  
  /**
   * Liefert das Model zu dieser View.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

}
