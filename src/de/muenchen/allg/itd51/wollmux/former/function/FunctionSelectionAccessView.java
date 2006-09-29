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
package de.muenchen.allg.itd51.wollmux.former.function;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.former.view.View;
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
   * Eintrag für die Funktionsauswahl-ComboBox, wenn manuelle Eingabe eines Strings
   * gewünscht ist.
   */
  private static final String STRING_ITEM = "<Startwert>";
  
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
   * TESTED
   */
  public FunctionSelectionAccessView(FunctionSelectionAccess funcSel, FunctionLibrary funcLib)
  {
    this.funcSel = funcSel;
    this.funcLib = funcLib;
    myPanel = new JPanel(new GridBagLayout());
    
    buildPanel();
  }
  
  /**
   * Baut {@link #myPanel} komplett neu auf für den momentanen Zustand des FunctionSelectionAccess.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void buildPanel()
  {
    myPanel.removeAll();
    //  int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcTextarea  = new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START,   GridBagConstraints.BOTH, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,   GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
    //FIXME Unterscheidung nach Funktionsart und Textarea falls Expert, Werteingabe falls die Expertenfunktion nur ein String-Literal ist; dann TODO Testen
    int y = 0;
    JLabel label = new JLabel("Funktion");
    gbcLabelLeft.gridx = 0;
    gbcLabelLeft.gridy = y;
    myPanel.add(label, gbcLabelLeft);
    
    JComboBox functionSelector = buildFunctionSelector();
    gbcTextfield.gridx = 1;
    gbcTextfield.gridy = y++;
    myPanel.add(functionSelector, gbcTextfield);
    
    JSeparator seppl = new JSeparator(SwingConstants.HORIZONTAL);
    gbcHsep.gridx = 0;
    gbcHsep.gridy = y++;
    myPanel.add(seppl, gbcHsep);
    
    Component glue = Box.createGlue();
    gbcGlue.gridx = 0;
    gbcGlue.gridy = y++;
    myPanel.add(glue, gbcGlue);
    
    myPanel.validate();
  }
  
  /**
   * Liefert eine JComboBox, die die Auswahl einer Funktion aus {@link #funcLib} erlaubt.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JComboBox buildFunctionSelector()
  {
    JComboBox box = new JComboBox();
    int selectedIndex = 0;
    int none_index = box.getItemCount(); box.addItem(NONE_ITEM);
    int string_index = box.getItemCount(); box.addItem(STRING_ITEM);
    Iterator iter = funcLib.getFunctionNames().iterator();
    int i = box.getItemCount();
 
    while (iter.hasNext())
    {
      String funcName = (String)iter.next();

      if (funcName.equals(funcSel.getFunctionName())) 
        selectedIndex = i;
      box.addItem(funcName);
      ++i;
    }
    int expert_index = box.getItemCount(); box.addItem(EXPERT_ITEM);
    
    if (funcSel.isNone())
      selectedIndex = none_index;
    else if (funcSel.isExpert())
    {
      selectedIndex = expert_index;
      try{
        ConfigThingy expertFun = funcSel.getExpertFunction();
        
          //falls die Expertenfunktion leer ist oder nur ein Kind hat und keine Enkel
          //(d.h. wenn die Funktion ein String-Literal ist), dann wird der Spezialeintrag
          //STRING_ITEM gewählt anstatt EXPERT_ITEM.
        if (expertFun.count() == 0 || 
            (expertFun.count() == 1 && expertFun.getFirstChild().count() == 0)) 
        selectedIndex = string_index;
      }catch(NodeNotFoundException x){}
    }
    
    box.setSelectedIndex(selectedIndex);
    
    //FIXME  Listener auf box registrieren und funcSel Model entsprechend ändern wenn dort was gemacht wird
    
    return box;
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }
}
