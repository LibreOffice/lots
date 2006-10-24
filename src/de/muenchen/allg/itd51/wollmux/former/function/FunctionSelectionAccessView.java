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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.StringReader;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.func.Function;
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
  private static final String EXPERT_ITEM = "<Code>";
  
  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn manuelle Eingabe eines Strings
   * gewünscht ist.
   */
  private static final String STRING_ITEM = "<Wert>";
  
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
   * Die JComboBox, in der der Benutzer die Funktion auswählen kann.
   */
  private JComboBox functionSelectorBox;

  /**
   * Wurde die manuelle Eingabe eines Stringliterals als Funktion gewählt, so erfolgt die
   * Eingabe in dieses Eingabefeld. 
   */
  private JTextField literalValueField;
  
  /**
   * Wurde die manuelle Experten-Eingabe einer Funktion gewählt, so wird diese über diese
   * Textarea abgewickelt.
   */
  private JTextArea complexFunctionArea;
  
  /**
   * Damit nicht bei jedem gedrückten Buchstaben ein neues ConfigThingy erzeugt wird, wird dieser
   * Timer verwendet, um das updaten verzögert und gesammelt anzustoßen.
   */
  private Timer updateExpertFunctionTimer;
  
  /**
   * Wird von {@link #updateExpertFunction()} ausgewertet um festzustellen, ob die Funktion aus
   * {@link #literalValueField} (Fall "false") oder {@link #complexFunctionArea} (fall "true")
   * geparst werden muss. 
   */
  private boolean expertFunctionIsComplex;

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
    
    updateExpertFunctionTimer = new Timer(250, new ActionListener()
        { public void actionPerformed(ActionEvent e)
        {
          updateExpertFunction();
      }});
      updateExpertFunctionTimer.setCoalesce(true);
      updateExpertFunctionTimer.setRepeats(false);
      
    
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
    
    if (functionSelectorBox == null)
      functionSelectorBox = buildFunctionSelector();
    gbcTextfield.gridx = 1;
    gbcTextfield.gridy = y++;
    myPanel.add(functionSelectorBox, gbcTextfield);
    
    JSeparator seppl = new JSeparator(SwingConstants.HORIZONTAL);
    gbcHsep.gridx = 0;
    gbcHsep.gridy = y++;
    myPanel.add(seppl, gbcHsep);
    
    if (funcSel.isExpert())
    {
      ConfigThingy conf = funcSel.getExpertFunction();
      
      if (functionSelectorBox.getSelectedItem().toString().equals(STRING_ITEM))
      {
        expertFunctionIsComplex = false;
        
        label = new JLabel("Wert");
        gbcLabelLeft.gridx = 0;
        gbcLabelLeft.gridy = y;
        myPanel.add(label, gbcLabelLeft);
        
        /*
         * Nur wenn es sich bei der Funktion um einen einfachen String handelt, wird dieser
         * als Vorbelegung genommen. Ist die aktuelle Funktion eine komplexere Funktion, so wird
         * nur der leere String als Vorbelegung genommen.
         */
        String literal = "";
        if (conf.count() == 1 && ((ConfigThingy)conf.iterator().next()).count() == 0)
        {
          literal = conf.toString();
        }
        literalValueField = new JTextField(literal); 
        gbcTextfield.gridx = 1;
        gbcTextfield.gridy = y++;
        myPanel.add(literalValueField, gbcTextfield);
        
        literalValueField.getDocument().addDocumentListener(new ExpertFunctionChangeListener());
        
      } else //komplexere Expertenfunktion
      {
        expertFunctionIsComplex = true;
        
        StringBuilder code = new StringBuilder();
        Iterator iter = conf.iterator();
        while (iter.hasNext())
        {
          code.append(((ConfigThingy)iter.next()).stringRepresentation());
        }
        
        complexFunctionArea = new JTextArea(code.toString());
        gbcTextarea.gridx = 0;
        gbcTextarea.gridy = y++;
        myPanel.add(complexFunctionArea, gbcTextarea);
        
        complexFunctionArea.getDocument().addDocumentListener(new ExpertFunctionChangeListener());
      }
      
    }
    else if (funcSel.isReference())
    {
      //TODO implementieren
    }
    
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
    functionSelectorBox = new JComboBox();
    int selectedIndex = 0;
    int none_index = functionSelectorBox.getItemCount(); functionSelectorBox.addItem(NONE_ITEM);
    int string_index = functionSelectorBox.getItemCount(); functionSelectorBox.addItem(STRING_ITEM);
    Iterator iter = funcLib.getFunctionNames().iterator();
    int i = functionSelectorBox.getItemCount();
 
    while (iter.hasNext())
    {
      String funcName = (String)iter.next();

      if (funcName.equals(funcSel.getFunctionName())) 
        selectedIndex = i;
      functionSelectorBox.addItem(funcName);
      ++i;
    }
    int expert_index = functionSelectorBox.getItemCount(); functionSelectorBox.addItem(EXPERT_ITEM);
    
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
    
    functionSelectorBox.setSelectedIndex(selectedIndex);
    
    functionSelectorBox.addItemListener(new FunctionSelectionBoxItemListener());

    return functionSelectorBox;
  }

  /**
   * Schreibt die aktuelle manuelle Eingabe der Expertenfunktion in den FunctionSelectionAccess
   * zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void updateExpertFunction()
  {
    //Falls updateExpertFunction() außer der Reihe aufgerufen wurde muss
    //der Timer gestoppt werden, damit keine unnötigen (und im Falle, dass sich
    //Rahmenbedingungen zwischenzeitlich geändert haben fehlerhaften) Aufrufe
    //erfolgen.
    updateExpertFunctionTimer.stop();
    
    if (expertFunctionIsComplex)
    {
      try
      {
        ConfigThingy conf = new ConfigThingy("", null, new StringReader(complexFunctionArea.getText()));
        funcSel.setExpertFunction(conf);
        complexFunctionArea.setBackground(Color.WHITE);
      }
      catch (Exception e1)
      {
        complexFunctionArea.setBackground(Color.PINK);
      }
      
    } else
    {
      ConfigThingy conf = new ConfigThingy("EXPERT");
      conf.add(literalValueField.getText());
      funcSel.setExpertFunction(conf);
    }
  }
  
  /**
   * Dieser Listener wird sowohl für {@link FunctionSelectionAccessView#literalValueField} als
   * auch {@link FunctionSelectionAccessView#complexFunctionArea} verwendet, um auf Benutzereingaben
   * zu reagieren.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ExpertFunctionChangeListener implements DocumentListener
  {
    public void insertUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }

    public void removeUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }

    public void changedUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }
  }
  
  /**
   * Wird auf die Funktionsauswahl-Kombobox registriert und reagiert darauf, dass der Benutzer eine
   * andere Funktion auswählt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class FunctionSelectionBoxItemListener implements ItemListener
  {
    /*
     * TESTED
     */
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getStateChange() == ItemEvent.SELECTED)
      {
        if (updateExpertFunctionTimer.isRunning()) updateExpertFunction();
        
        String item = functionSelectorBox.getSelectedItem().toString();
        String functionName = item;
        String[] paramNames = null;
        if (item.equals(EXPERT_ITEM) || item.equals(STRING_ITEM))
          functionName = FunctionSelectionAccess.EXPERT_FUNCTION;
        else if (item.equals(NONE_ITEM))
          functionName = FunctionSelectionAccess.NO_FUNCTION;
        else
        {
          Function func = funcLib.get(functionName);
          if (func == null)
          {
            Logger.error("Funktion \""+functionName+"\" ist verschwunden ?!?");
          }
          paramNames = func.parameters();
        }
          
        funcSel.setFunction(functionName, paramNames);
        buildPanel();
      }
    }
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }
}
