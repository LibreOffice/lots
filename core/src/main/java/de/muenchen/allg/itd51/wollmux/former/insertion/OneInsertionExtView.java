/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import de.muenchen.allg.itd51.wollmux.former.insertion.model.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Anzeige erweiterter Eigenschaften eines InsertionModels.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneInsertionExtView implements View
{
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Die oberste Komponente dieser View.
   */
  private JTabbedPane myTabbedPane;

  /**
   * Das Model zu dieser View.
   */
  private InsertionModel model;

  /**
   * Erzeugt eine neue View.
   * 
   * @param model
   *          das Model dessen Daten angezeigt werden sollen.
   * @param funcLib
   *          die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden
   *          sollen für das Auswählen von Attributen, die eine Funktion erfordern.
   * @param myViewChangeListener
   *          typischerweise ein Container, der diese View enthält und über
   *          Änderungen informiert werden soll.
   */
  public OneInsertionExtView(InsertionModel model, FunctionLibrary funcLib,
      ViewChangeListener myViewChangeListener)
  {
    this.bigDaddy = myViewChangeListener;
    this.model = model;
    myTabbedPane = new JTabbedPane();

    // als ViewChangeListener wird null übergeben, weil die OneInsertionExtView sich
    // nachher
    // direkt auf dem Model als Listener registriert.
    OneInsertionTrafoView trafoView =
      new OneInsertionTrafoView(model, funcLib, null);
    myTabbedPane.addTab("TRAFO", trafoView.getComponent());

    model.addListener(new MyModelChangeListener());
  }

  @Override
  public JComponent getComponent()
  {
    return myTabbedPane;
  }

  /**
   * Liefert das Model zu dieser View.
   */
  public InsertionModel getModel()
  {
    return model;
  }

  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(InsertionModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneInsertionExtView.this);
    }

    @Override
    public void attributeChanged(InsertionModel model, int attributeId, Object newValue)
    {
      // nothing to do
    }
  }

}
