/*
* Dateiname: OneFormControlLineView.java
* Projekt  : WollMux
* Funktion : Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement. 
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 29.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlLineView implements View, FormControlModel.ModelChangeListener
{
  /**
   * Standardbreite des Textfelds, das das Label anzeigt.
   */
  private static final int LABEL_COLUMNS = 20;
  
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;
  
  /**
   * Das Panel, das alle Komponenten dieser View enthält.
   */
  private JPanel myPanel;
  
  
  /**
   * Wird vor dem Ändern eines Attributs des Models gesetzt, damit der rekursive Aufruf
   * des ChangeListeners nicht unnötigerweise das Feld updatet, das wir selbst gerade gesetzt
   * haben.
   */
  private boolean ignoreAttributeChanged = false;
  
  /**
   * Das Model zur View.
   */
  private FormControlModel model;

  /**
   * Das JTextField, das das LABEL anzeigt und ändern lässt.
   */
  private JTextField labelTextfield;
  
  /**
   * Erzeugt eine View für model.
   * @param bigDaddy typischerweise ein Container, der die View enthält und daher über Änderungen
   *        auf dem Laufenden gehalten werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneFormControlLineView(FormControlModel model, ViewChangeListener bigDaddy)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    myPanel = new JPanel();
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.add(makeLabelView());
    model.addListener(this);
  }
  
  /**
   * Liefert eine Komponente, die das LABEL des FormControlModels anzeigt und Änderungen
   * an das Model weitergibt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JComponent makeLabelView()
  {
    labelTextfield = new JTextField(model.getLabel(), LABEL_COLUMNS);
    labelTextfield.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        ignoreAttributeChanged = true;
        model.setLabel(labelTextfield.getText());
        ignoreAttributeChanged = false;
      }});
    return labelTextfield;
  }
  
  /**
   * Wird aufgerufen, wenn das LABEL des durch diese View dargestellten {@link FormControlModel}s
   * durch eine andere Ursache als diese View geändert wurde.
   * @param newLabel das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void labelChanged(String newLabel)
  {
    labelTextfield.setText(newLabel);
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }

  public void attributeChanged(FormControlModel model, int attributeId, Object newValue)
  {
    if (ignoreAttributeChanged) return;
    switch(attributeId)
    {
      case FormControlModel.LABEL_ATTR: labelChanged((String)newValue); break;
    }
  }

  public void modelRemoved(FormControlModel model)
  {
    bigDaddy.viewShouldBeRemoved(this);
  }

  /**
   * Interface für Klassen, die an Änderungen dieser View interessiert sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ViewChangeListener
  {
    /**
     * Wird aufgerufen, wenn alle Referenzen auf diese View entfernt werden sollten,
     * weil die view ungültig geworden ist (typischerweise weil das zugrundeliegende Model
     * nicht mehr da ist).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void viewShouldBeRemoved(OneFormControlLineView view);
  }

}
