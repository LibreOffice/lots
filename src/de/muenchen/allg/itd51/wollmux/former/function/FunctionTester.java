/*
* Dateiname: FunctionTester.java
* Projekt  : WollMux
* Funktion : GUI zum interaktiven Zusammenbauen und Testen von WollMux-Funktionen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 26.07.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.function;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;

/**
 * GUI zum interaktiven Zusammenbauen und Testen von WollMux-Funktionen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionTester
{
  /**
   * Das Fenster des FunctionTesters. 
   */
  private JFrame myFrame;
  
  /**
   * Hauptpanel.
   */
  private JPanel myPanel;
  
  /**
   * Die Textarea in der der Code der aktuellen Funktion angezeigt und editiert wird.
   */
  private JTextArea codeArea;
  
  /**
   * Wird auf {@link #myFrame} registriert.
   */
  private MyWindowListener oehrchen;
  
  /**
   * Falls nicht null wird dieser Listener benachrichtigt, wenn das FunctionTester
   * Fenster geschlossen wird.
   */
  private ActionListener abortListener;
  
  /**
   * Der {@link IDManager}, dessen aktive IDs in Value-Comboboxen auswählbar sind.
   */
  private IDManager idManager;
  
  /**
   * Der Namensraum aus dem die IDs von {@link #idManager} genommen werden.
   */
  private Object namespace;
  
  /**
   * Liste aller angezeigten {@link FunctionTester.ValueBox}es.
   */
  private List valueBoxes = new Vector();
  
  /**
   * Die Funktionsbibliothek, deren Funktionen für BIND zur Verfügung stehen.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Kontext für die parse()-Funktionen von {@link FunctionFactory}.
   */
  private Map myContext = new HashMap();
  
  /**
   * Liefert die Parameter für das Auswerten der Funktion, wobei die Werte aus
   * {@link #valueBoxes} gezogen werden.
   */
  private Values myParameters = new ValueBoxesValues();
  
  /**
   * Die Funktionsdialogbibliothek, deren Funktionsdialoge für DIALOG zur Verfügung stehen.
   */
  private DialogLibrary dialogLib;
  
  /**
   * Erzeugt ein neues FunctionTester-Fenster, das auch gleich angezeigt wird.
   * Darf nur aus dem Event-Dispatching-Thread aufgerufen werden.
   * @param idManager die IDs dieses Managers werden in den ComboBoxen für die 
   *        Werte-Festlegung angeboten.
   * @param namespace aus dem die IDs genommen werden.
   * @param funcLib Die Funktionen dieser Bibliothek stehen für BIND zur
   *        Verfügung.
   * @param abortListener falls nicht null wird dieser Listener benachrichtigt, wenn
   *        das FunctionTester-Fenster geschlossen wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public FunctionTester(FunctionLibrary funcLib, ActionListener abortListener, IDManager idManager, Object namespace)
  {
    this.abortListener = abortListener;
    this.idManager = idManager;
    this.namespace = namespace;
    this.funcLib = funcLib;
    dialogLib = new DialogLibrary();
    
    myFrame = new JFrame("Funktionstester");
    //  leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    
    myPanel = new JPanel(new BorderLayout());
    myFrame.add(myPanel);
    
    JPanel valuePanel = new JPanel();
    valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.Y_AXIS));
    myPanel.add(valuePanel, BorderLayout.NORTH);
    for (int i = 0; i < 6; ++i)
    {
      ValueBox valbox = makeValueBox();
      valueBoxes.add(valbox);
      valuePanel.add(valbox.JComponent());
    }
    
    codeArea = new JTextArea(10,40);
    codeArea.setLineWrap(true);
    JScrollPane codeAreaPane = new JScrollPane(codeArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    myPanel.add(codeAreaPane, BorderLayout.CENTER);
    
    
    myPanel.add(makeFunctionInsertPanel(), BorderLayout.EAST);
    
    myPanel.add(makeEvaluatePanel(), BorderLayout.SOUTH);
    
    myFrame.pack();
    myFrame.setVisible(true);
  }
  
  private ValueBox makeValueBox()
  {
    Box hbox = Box.createHorizontalBox();
    final JComboBox combo = new JComboBox();
    combo.setEditable(true);
    
    updateParameterBox(combo);
    
    combo.addPopupMenuListener(new PopupMenuListener(){
      public void popupMenuWillBecomeVisible(PopupMenuEvent e)
      {
        updateParameterBox(combo);
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e)  {}
      public void popupMenuCanceled(PopupMenuEvent e)  {}
    });
    
    hbox.add(combo);
    
    hbox.add(Box.createHorizontalStrut(5));
    
    JButton butsi = new JButton("VALUE");
    butsi.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertCode("VALUE \""+ getComboBoxValue(combo, "ERROR")+"\"");
      }});
    
    hbox.add(butsi);
    
    hbox.add(Box.createHorizontalStrut(5));
    JTextField text = new JTextField(10);
    hbox.add(text);
    
    return new ValueBox(combo, text, hbox);
  }
  
  /**
   * Ersetzt die aktuelle Selektion der Code-Textarea durch code.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertCode(String code)
  {
    codeArea.replaceSelection(code);
  }
  /**
   * Aktualisiert die Liste der Einträge in combo, so dass sie den aktiven IDs von
   * {@link #idManager} entspricht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void updateParameterBox(JComboBox combo)
  {
    String currentValue = getComboBoxValue(combo, "");
    combo.removeAllItems();
    boolean found = false;
    Iterator iter = idManager.getAllIDs(namespace).iterator();
    while (iter.hasNext())
    {
      IDManager.ID id = (IDManager.ID)iter.next();
      if (id.isActive())
      {
        String idStr = id.toString();
        combo.addItem(idStr);
        if (currentValue.equals(idStr))
        {
          combo.setSelectedIndex(combo.getItemCount() - 1);
          found = true;
        }
      }
    }
    
    if (!found)
    {
      combo.addItem(currentValue);
      combo.setSelectedIndex(combo.getItemCount() - 1);
    }
  }

  /**
   * Liefert ein Panel, das Buttons zum Einfügen von Funktionstemplates 
   * in den Code-Edit-Bereich anbietet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private JComponent makeFunctionInsertPanel()
  {
    JPanel functionInsertPanel = new JPanel();
    functionInsertPanel.setLayout(new GridLayout(13, 2));
    
    functionInsertPanel.add(makeCodeInsertionButton("MATCH", "MATCH(String, RegEx)"));
    functionInsertPanel.add(makeCodeInsertionButton("REPLACE", "REPLACE(String, \"RegEx\", \"RepStr\")"));
    functionInsertPanel.add(makeCodeInsertionButton("CAT", "CAT(String1, String2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("FORMAT", "FORMAT(Zahl MIN \"Zahl\" MAX \"Zahl\")"));
    functionInsertPanel.add(makeCodeInsertionButton("IF", "IF(Bedingung THEN(Dann) ELSE(Sonst))"));
    functionInsertPanel.add(makeCodeInsertionButton("SELECT", "SELECT(\nFunktion1\nFunktion2\n,,,\nELSE(Sonst)\n)"));
    functionInsertPanel.add(makeCodeInsertionButton("AND", "AND(Boolean1, Boolean2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("OR", "OR(Boolean1, Boolean2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("NOT", "NOT(Boolean1, Boolean2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("SUM", "SUM(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("MINUS", "MINUS(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("DIFF", "DIFF(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("PRODUCT", "PRODUCT(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("DIVIDE", "DIVIDE(Zahl BY(Zahl) MIN \"Zahl\" MAX \"Zahl\")"));
    functionInsertPanel.add(makeCodeInsertionButton("ABS", "ABS(Zahl)"));
    functionInsertPanel.add(makeCodeInsertionButton("SIGN", "SIGN(Zahl)"));
    functionInsertPanel.add(makeCodeInsertionButton("STRCMP", "STRCMP(String1, String2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("NUMCMP", "NUMCMP(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("LT", "LT(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("LE", "LE(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("GT", "GT(Zahl1, Zahl2 ,,,)"));
    functionInsertPanel.add(makeCodeInsertionButton("GE", "GE(Zahl1, Zahl2 ,,,)"));
    
    functionInsertPanel.add(makeCodeInsertionButton("BIND", "BIND(FUNCTION(Funktion)\n SET(\"ParamName1\" Wert1)\n SET(\"ParamName2\" Wert2) ,,,\n)"));
    functionInsertPanel.add(makeCodeInsertionButton("EXTERN", "EXTERN(URL \"url\" PARAMS(\"ParamName1\" \"ParamName2\" ,,,))"));
    
    functionInsertPanel.add(makeCodeInsertionButton("DIALOG", "DIALOG(\"Dialogname\", \"Feldname\")"));
    
    return functionInsertPanel;
  }
  
  private JComponent makeEvaluatePanel()
  {
    JPanel evaluatePanel = new JPanel();
    evaluatePanel.setLayout(new BoxLayout(evaluatePanel, BoxLayout.Y_AXIS));
    
    JButton button = new JButton("Auswerten");
    evaluatePanel.add(button);
    
    final JTextArea stringResult = new JTextArea(3,20);
    JScrollPane stringResultPane = new JScrollPane(stringResult, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    evaluatePanel.add(stringResultPane);
    
    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel("Boolean: "));
    final JTextField booleanResult = new JTextField();
    hbox.add(booleanResult);
    
    evaluatePanel.add(hbox);
    
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        stringResult.setText("");
        booleanResult.setText("");
        
        try{
          String codeStr = codeArea.getText().trim();
          if (codeStr.length() == 0) return;
          ConfigThingy codeConf = new ConfigThingy("CAT", codeStr);
          Function func = FunctionFactory.parse(codeConf, funcLib, dialogLib, myContext);
          String result = func.getString(myParameters);
          if (result == Function.ERROR) throw new Exception("Illegaler oder fehlender Parameter!");
          stringResult.setText(result);
          booleanResult.setText(""+func.getBoolean(myParameters));
        } catch(Exception x)
        {
          stringResult.setText(x.getMessage());
        }
      }}
    );
    
    return evaluatePanel;
  }
  
  /**
   * Liefert einen Button, der mit label beschriftet ist und den Code code an Stelle der
   * aktuellen Selektion im Code-Bereich einfügt. 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private JComponent makeCodeInsertionButton(String label, final String code)
  {
    JButton butsi = new JButton(label);
    butsi.addActionListener(new ActionListener(){
    public void actionPerformed(ActionEvent e)
    {
      insertCode(code);
    }});
    
    return butsi;
  }
  
  /**
   * Liefert den aktuellen Wert, der in combo ausgewählt ist, oder errorValue,
   * falls beim Auswerten ein Fehler auftritt. In letzterem Fall wird auch eine
   * Meldung geloggert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getComboBoxValue(JComboBox combo, String errorValue)
  {
    Document comboDoc = ((JTextComponent)combo.getEditor().getEditorComponent()).getDocument();
    try
    {
      return comboDoc.getText(0,comboDoc.getLength());
    }
    catch (BadLocationException e)
    {
      Logger.error(e);
      return errorValue;
    }
  }
  
  /**
   * Eine Box, die eine ComboBox zur Angabe einer ID und ein Textfield zur Angabe eines
   * Wertes enthält.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class ValueBox
  {
    private JComboBox combo;
    private JTextField text;
    private JComponent compo;
    
    public ValueBox(JComboBox combo, JTextField text, JComponent compo)
    {
      this.combo = combo;
      this.text = text;
      this.compo = compo;
    }
    
    public JComponent JComponent()
    {
      return compo; 
    }
  }
  
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { abort(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
  }
  
  public void abort()
  {
    if (myFrame != null)
    {
      myFrame.removeWindowListener(oehrchen);
      myFrame.getContentPane().remove(0);
      myFrame.dispose();
      myFrame = null;
      if (abortListener != null) abortListener.actionPerformed(null);
      abortListener = null;
    }
  }
  
  public void toFront()
  {
    myFrame.toFront();
  }
  
  /**
   * Stellt den jeweils aktuellen Zustand der {@link FunctionTester#valueBoxes} als
   * {@link Values} zur Verfügung.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ValueBoxesValues implements Values
  {
    public boolean hasValue(String id)
    {
      ValueBox box = getValueBox(id);
      return (box != null);
    }
    
    private ValueBox getValueBox(String id)
    {
      Iterator iter = valueBoxes.iterator();
      while (iter.hasNext())
      {
        ValueBox box = (ValueBox)iter.next();
        String boxId = getComboBoxValue(box.combo, "");
        if (boxId.equals(id))
          return box;
      }
      return null;
    }

    public String getString(String id)
    {
      ValueBox box = getValueBox(id);
      if (box == null) return "";
      return box.text.getText();
    }

    public boolean getBoolean(String id)
    {
      ValueBox box = getValueBox(id);
      if (box == null) return false;
      return box.text.getText().equalsIgnoreCase("true");
    }
  }
}
