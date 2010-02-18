/*
 * Dateiname: OneInsertionExtView.java
 * Projekt  : WollMux
 * Funktion : Anzeige erweiterter Eigenschaften eines InsertionModels.
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
 * 27.10.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.insertion;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
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
    myTabbedPane.addTab("TRAFO", trafoView.JComponent());

    model.addListener(new MyModelChangeListener());
  }

  public JComponent JComponent()
  {
    return myTabbedPane;
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

  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    public void modelRemoved(InsertionModel model)
    {
      if (bigDaddy != null) bigDaddy.viewShouldBeRemoved(OneInsertionExtView.this);
    }

    public void attributeChanged(InsertionModel model, int attributeId,
        Object newValue)
    {}
  }

}
