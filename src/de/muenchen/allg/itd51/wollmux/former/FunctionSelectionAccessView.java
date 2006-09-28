/*
* Dateiname: FunctionSelectionAccessView.java
* Projekt  : WollMux
* Funktion : Eine Sicht, die das Bearbeiten von {@link FunctionSelectionAccess} Objekten erlaubt.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 27.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Eine Sicht, die das Bearbeiten von {@link FunctionSelectionAccess} Objekten erlaubt.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionSelectionAccessView implements View
{
  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn keine Funktion gewünscht ist.
   */
  private static final String NONE_ITEM = "<keine>";
  
  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn manuelle Eingabe gewünscht ist.
   */
  private static final String EXPERT_ITEM = "<Experte>";
  
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  
  /**
   * Das Panel, das alle Elemente dieser View enthält.
   */
  private JPanel myPanel;
  
  /**
   * Das {@link FunctionSelectionAccess} Objekt, das diese View anzeigt und bearbeitet.
   */
  private FunctionSelectionAccess funcSel;
  
  /**
   * Die Funktionsbibliothek, deren Funktionen auswählbar sind.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Erzeugt eine neue View über die funcSel angezeigt und bearbeitet werden kann.
   * @param funcLib die Funktionsbibliothek, deren Funktionen auswählbar sein sollen. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FunctionSelectionAccessView(FunctionSelectionAccess funcSel, FunctionLibrary funcLib)
  {
    this.funcSel = funcSel;
    this.funcLib = funcLib;
    myPanel = new JPanel(new GridBagLayout());
    
    buildPanel();
  }
  
  private void buildPanel()
  {
    myPanel.removeAll();
    //  int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcTextarea  = new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START,   GridBagConstraints.BOTH, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    
    int y = 0;
    JLabel label = new JLabel("Funktion");
    gbcLabelLeft.gridx = 0;
    gbcLabelLeft.gridy = y++;
    myPanel.add(label, gbcLabelLeft);
    
    JComboBox functionSelector = buildFunctionSelector();
    gbcTextfield.gridx = 1;
    gbcTextfield.gridy = y++;
    myPanel.add(functionSelector, gbcTextfield);
    
    JSeparator seppl = new JSeparator(SwingConstants.HORIZONTAL);
    gbcHsep.gridx = 0;
    gbcHsep.gridy = y++;
    myPanel.add(seppl, gbcHsep);
    
    myPanel.validate();
  }
  
  private JComboBox buildFunctionSelector()
  {
    JComboBox box = new JComboBox();
    box.addItem(NONE_ITEM);
    Iterator iter = funcLib.getFunctionNames().iterator();
    while (iter.hasNext())
      box.addItem(iter.next());
    box.addItem(EXPERT_ITEM);
    return box;
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }
}
