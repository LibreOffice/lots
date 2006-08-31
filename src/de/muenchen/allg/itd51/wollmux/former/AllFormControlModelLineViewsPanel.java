/*
* Dateiname: AllFormControlModelLineViewsPanel.java
* Projekt  : WollMux
* Funktion : Hält in einem Panel FormControlModelLineViews für alle FormControlModels einer FormControlModelList.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 30.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.muenchen.allg.itd51.wollmux.former.FormControlModelList.ItemListener;

/**
 * Hält in einem Panel FormControlModelLineViews für alle 
 * {@link de.muenchen.allg.itd51.wollmux.former.FormControlModel} einer 
 * {@link de.muenchen.allg.itd51.wollmux.former.FormControlModelList}.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlModelLineViewsPanel implements View, ItemListener, OneFormControlLineView.ViewChangeListener
{
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;
  
  /**
   * Die {@link FormControlModelList}, deren Inhalt in dieser View angezeigt wird.
   */
  private FormControlModelList formControlModelList;
  
  /**
   * Das Panel, das alle Komponenten dieser View enthält.
   */
  private JPanel myPanel;
  
  /**
   * Das Panel, das die ganzen {@link OneFormControlLineView}s enthält.
   */
  private JPanel lineViewPanel;
  
  /**
   * Die Scrollpane in der sich das {@link #lineViewPanel} befindet.
   */
  private JScrollPane scrollPane;
  
  /**
   * Erzeugt eine AllFormControlModelLineViewsPanel, die den Inhalt von
   * formControlModelList anzeigt. ACHTUNG! formControlModelList sollte leer sein,
   * da nur neu hinzugekommene Elemente in der View angezeigt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public AllFormControlModelLineViewsPanel(FormControlModelList formControlModelList)
  {
    this.formControlModelList = formControlModelList;
    formControlModelList.addListener(this);
    myPanel = new JPanel(new GridBagLayout());
    lineViewPanel = new JPanel(new GridBagLayout());
     //    int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcMainPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    gbcMainPanel.gridx = 0;
    gbcMainPanel.gridy = 0;
    scrollPane = new JScrollPane(lineViewPanel);
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
    myPanel.add(scrollPane, gbcMainPanel);
    
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcButtonPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    gbcButtonPanel.gridx = 0;
    gbcButtonPanel.gridy = 1;
    myPanel.add(buttonPanel,gbcButtonPanel);
    
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    buttonPanel.add(new JButton("Hoch"), gbcButton);
    ++gbcButton.gridx;
    buttonPanel.add(new JButton("Runter"), gbcButton);
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  public void itemAdded(FormControlModel model)
  {
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    OneFormControlLineView ofclView = new OneFormControlLineView(model, this);
    gbcTextfield.gridy = lineViewPanel.getComponentCount();
    lineViewPanel.add(ofclView.JComponent(), gbcTextfield);
    lineViewPanel.validate();
  }

  public void viewShouldBeRemoved(OneFormControlLineView view)
  {
    lineViewPanel.remove(view.JComponent());
    //TODO die GridBagConstraints der anderen Elemente anpassen, wenn ein Element in der Mitte entfernt wurde.
  }
  
}
