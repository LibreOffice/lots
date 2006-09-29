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
package de.muenchen.allg.itd51.wollmux.former.control;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;



/**
 * Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlLineView extends LineView
{
  /**
   * Standardbreite des Textfelds, das das Label anzeigt.
   */
  private static final int LABEL_COLUMNS = 20;
  
  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private OneFormControlLineView.ViewChangeListener bigDaddy;
  
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
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();

    
  /**
   * Erzeugt eine View für model.
   * @param bigDaddy typischerweise ein Container, der die View enthält und daher über Änderungen
   *        auf dem Laufenden gehalten werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneFormControlLineView(FormControlModel model, OneFormControlLineView.ViewChangeListener bigDaddy, FormularMax4000 formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeLabelView());
    unmarkedBackgroundColor = myPanel.getBackground();
    model.addListener(new MyModelChangeListener());
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
    Document tfdoc = labelTextfield.getDocument();
    tfdoc.addDocumentListener(new DocumentListener(){
      public void update()
      {
        ignoreAttributeChanged = true;
        model.setLabel(labelTextfield.getText());
        ignoreAttributeChanged = false;
        if (getModel().getType() == FormControlModel.TAB_TYPE)
          bigDaddy.tabTitleChanged(OneFormControlLineView.this);
      }

      public void insertUpdate(DocumentEvent e) {update();}
      public void removeUpdate(DocumentEvent e) {update();}
      public void changedUpdate(DocumentEvent e) {update();}
      });
    
    labelTextfield.setCaretPosition(0);
    labelTextfield.addMouseListener(myMouseListener);
    setTypeSpecificTraits(labelTextfield, model.getType());
    return labelTextfield;
  }
  
  /**
   * Setzt optische Aspekte wie Rand von compo abhängig von type.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setTypeSpecificTraits(JComponent compo, String type)
  {
    if (type == FormControlModel.TAB_TYPE)
    {
      Font f = compo.getFont();
      f = f.deriveFont(Font.BOLD);
      compo.setFont(f);
      compo.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
      compo.setBackground(new Color(230,235,250));
    }
    else if (type == FormControlModel.BUTTON_TYPE)
    {
      compo.setBackground(Color.LIGHT_GRAY);
      compo.setBorder(BorderFactory.createRaisedBevelBorder());
    }
    else if (type == FormControlModel.LABEL_TYPE)
    {
      Font f = compo.getFont();
      f = f.deriveFont(Font.BOLD);
      compo.setFont(f);
    }
  }
  
  /**
   * Wird aufgerufen, wenn das LABEL des durch diese View dargestellten {@link FormControlModel}s
   * DURCH EINE ANDERE URSACHE ALS DIESE VIEW geändert wurde.
   * @param newLabel das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void labelChangedDueToExternalReasons(String newLabel)
  {
    labelTextfield.setText(newLabel);
    if (getModel().getType() == FormControlModel.TAB_TYPE)
      bigDaddy.tabTitleChanged(this);
  }
  
  /**
   * Wird aufgerufen, wenn das TYPE des durch diese View dargestellten {@link FormControlModel}s
   * durch eine andere Ursache als diese View geändert wurde.
   * @param newType das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void typeChanged(String newType)
  {
    setTypeSpecificTraits(labelTextfield, newType);
  }

   /**
   * Liefert das {@link FormControlModel} das zu dieser View gehört.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

  /**
   * Interface für Klassen, die an Änderungen dieser View interessiert sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ViewChangeListener extends de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener
  {
    /**
     * Wird aufgerufen, wenn sich das Label des Tabs, das das Model von view ist
     * geändert hat.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void tabTitleChanged(OneFormControlLineView view);
  }

  private class MyModelChangeListener implements FormControlModel.ModelChangeListener
  {
    public void attributeChanged(FormControlModel model, int attributeId, Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch(attributeId)
      {
        case FormControlModel.LABEL_ATTR: labelChangedDueToExternalReasons((String)newValue); break;
        case FormControlModel.TYPE_ATTR: typeChanged((String)newValue); break;
      }
    }

    public void modelRemoved(FormControlModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneFormControlLineView.this);
    }
  }
  
  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um in
   * Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde. Je nachdem
   * ob CTRL gedrückt ist oder nicht wird die Selektion erweitert oder ersetzt. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyMouseListener implements MouseListener
  {
    public void mouseClicked(MouseEvent e){}
    public void mousePressed(MouseEvent e)
    {
      int state = 1;
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        state = 0;
      //formularMax4000.broadcast(new BroadcastFormControlModelSelection(getModel(), state, state!=0));
      formularMax4000.broadcast(new BroadcastObjectSelection(getModel(), state, state!=0){

        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastFormControlModelSelection(this);
        }});
    }
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
  }
}
