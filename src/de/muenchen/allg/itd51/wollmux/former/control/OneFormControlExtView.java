/*
* Dateiname: OneFormControlExtView.java
* Projekt  : WollMux
* Funktion : Anzeige erweiterter Eigenschaften eines FormControlModels.
* 
* Copyright: Landeshauptstadt München
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

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Anzeige erweiterter Eigenschaften eines FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlExtView implements View
{
  /**
  * Typischerweise ein Container, der die View enthält und daher über Änderungen
  * auf dem Laufenden gehalten werden muss.
  */
 private ViewChangeListener bigDaddy;
  
  /**
   * Die oberste Komponente dieser View.
   */
  private JTabbedPane myTabbedPane;
  
  /**
   * Das Model zu dieser View.
   */
  private FormControlModel model;
  
  /**
   * Erzeugt eine neue View.
   * @param model das Model dessen Daten angezeigt werden sollen.
   * @param funcLib die Funktionsbibliothek deren Funktionen zur Verfügung gestellt werden sollen
   *        für das Auswählen von Attributen, die eine Funktion erfordern.
   * @param myViewChangeListener typischerweise ein Container, der diese View enthält und über
   *        Änderungen informiert werden soll.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneFormControlExtView(FormControlModel model, FunctionLibrary funcLib, ViewChangeListener myViewChangeListener)
  {
    this.bigDaddy = myViewChangeListener;
    this.model = model;
    myTabbedPane = new JTabbedPane();
    
      // als ViewChangeListener wird null übergeben, weil die OneFormControlExtView sich nachher
      // direkt auf dem Model als Listener registriert.
    OneFormControlAutofillEditView autofillView = new OneFormControlAutofillEditView(model, funcLib, null);
    myTabbedPane.addTab("AUTOFILL", autofillView.JComponent());
    OneFormControlPlausiEditView plausiView = new OneFormControlPlausiEditView(model, funcLib, null);
    myTabbedPane.addTab("PLAUSI", plausiView.JComponent());
    
    model.addListener(new MyModelChangeListener());
  }

  public JComponent JComponent()
  {
    return myTabbedPane;
  }
  
  /**
   * Liefert das Model zu dieser View.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }
  
  private class MyModelChangeListener implements FormControlModel.ModelChangeListener
  {
    public void modelRemoved(FormControlModel model)
    {
      if (bigDaddy != null)
        bigDaddy.viewShouldBeRemoved(OneFormControlExtView.this);
    }

    public void attributeChanged(FormControlModel model, int attributeId, Object newValue)
    {
    }
  }



}
