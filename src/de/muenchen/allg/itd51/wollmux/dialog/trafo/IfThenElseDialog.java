/*
* Dateiname: IfThenElseDialog.java
* Projekt  : WollMux
* Funktion : Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 01.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;

/**
 * Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IfThenElseDialog extends TrafoDialog
{
  private static Border CASCADED_ITE_BORDER = BorderFactory.createCompoundBorder(
          BorderFactory.createEmptyBorder(0, 20, 0, 0),
          BorderFactory.createCompoundBorder(
             BorderFactory.createRaisedBevelBorder(), 
             BorderFactory.createEmptyBorder(5,5,5,5)));
  
  /**
   * Das Panel, das den Dialoginhalt präsentiert.
   */
  private JIfThenElsePanel ifThenElsePanel;
  
  /**
   * Das Objekt, das den Startinhalt des Dialogs spezifiziert (und am Ende verwendet wird,
   * um den Rückgabewert zu speichern).
   */
  private TrafoDialogParameters params;
  
  /**
   * Wenn der Dialog angezeigt wird ist dies der zugehörige JDialog.
   */
  private JDialog myDialog;
  
  /**
   * Der WindowListener, der an {@link #myDialog} hängt.
   */
  private MyWindowListener oehrchen;
  
  public IfThenElseDialog(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null ||
        params.fieldNames.size() == 0) 
      throw new IllegalArgumentException();
    
    params.isValid = false; //erst bei Beendigung mit Okay werden sie wieder valid
    
    ifThenElsePanel = new JIfThenElsePanel(params.conf, params.fieldNames, new MyRepackActionListener());
  }

  private class MyRepackActionListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      repack();
    }
  }
  
  private static class JIfThenElsePanel extends JPanel
  {
    /**
     * Kanitverstan.
     */
    private static final long serialVersionUID = -3752064852698886087L;
    
//    private 

    private static class TestType
    {
      public String label;
      public String func;
      public TestType(String label, String func)
      {
        this.label = label;
        this.func = func;
      }
      public String toString()
      {
        return label;
      }
    }

    /**
     * Die Einträge für {@link #testSelector}.
     */
    private static final TestType[] testTypes = {new TestType(L.m("genau ="),"STRCMP"),
                                                 new TestType(L.m("numerisch ="), "NUMCMP"),
                                                 new TestType(L.m("numerisch <"), "LT"),
                                                 new TestType(L.m("numerisch <="), "LE"),
                                                 new TestType(L.m("numerisch >"), "GT"),
                                                 new TestType(L.m("numerisch >="), "GE"),
                                                 new TestType(L.m("regulärer A."), "MATCH")};
    
    /**
     * Auswahl des zu vergleichenden Feldes.
     */
    private JComboBox fieldSelector;
    
    /**
     * Auswahl zwischen "" und "nicht".
     */
    private JComboBox notSelector;
    
    /**
     * Auswahl des anzuwendenden Tests.
     */
    private JComboBox testSelector;
    
    /**
     * Eingabefeld für den Vergleichswert.
     */
    private JTextField compareTo;

    private static class ConditionalResult
    {
      /**
       * Texteingabe für den Dann oder Sonst-Teil, wenn {@link #type} == 0.
       * Ist niemals null.
       */
      public TextComponentTags text;
      
      /**
       * ScrollPane in der sich {@link #text} befindet. Niemals null.
       */
      public JScrollPane scrollPane;
      
      /**
       * Eingabe des Dann oder Sonst-Teils, wenn {@link #type} == 1. Kann null sein,
       * wenn {@link #type} == 0.
       */
      public JIfThenElsePanel panel;
      
      /**
       * 0 => {@link #text} zählt. 1 => {@link #panel} zählt. 
       */
      public int type;
    }
    
    /**
     * Der Dann-Teil.
     */
    private ConditionalResult thenResult = new ConditionalResult();
    
    /**
     * Der Sonst-Teil.
     */
    private ConditionalResult elseResult = new ConditionalResult();
    
    private ActionListener packNecessary;
    
    /**
     * Erzeugt eine Dialog-Komponente, die mit den Werten aus conf vorbelegt ist, wobei
     * fieldNames die angebotenen Feldnamen als Strings enthält. Der oberste Knoten von
     * conf ist ein beliebiger Bezeichner (typischwerweise der Funktionsname).
     *
     * @param packNecessary wird aufgerufen, wannimmer sich im Panelinhalt soviel getan
     *        hat, dass ein erneutes pack() sinnvoll wäre.
     *
     * @throws IllegalArgumentException falls conf nicht verstanden wird.
     * 
     * @author Matthias Benkmann (D-III-ITD D.10)
     * TESTED
     */
    public JIfThenElsePanel(ConfigThingy conf, List fieldNames, ActionListener packNecessary)
    {
      this.packNecessary = packNecessary;
      
      if (conf.count() != 1) throw new IllegalArgumentException();
      try{ conf = conf.getFirstChild(); }catch(Exception x){};
      if (conf.getName().equals("IF"))
      {
        if (conf.count() == 3)
        {
          Iterator iter = conf.iterator();
          ConfigThingy ifConf = (ConfigThingy)iter.next();
          ConfigThingy thenConf = (ConfigThingy)iter.next();
          ConfigThingy elseConf = (ConfigThingy)iter.next();
          if (thenConf.getName().equals("THEN") && elseConf.getName().equals("ELSE"))
          {
            parseCondition(ifConf, fieldNames);
            parseThenElse(thenConf, thenResult, fieldNames);
            parseThenElse(elseConf, elseResult, fieldNames);
            buildGUI(fieldNames); //GUI Elemente in this einfügen (wir erben ja von JPanel).
            return;
          }
        }
      }
      
      throw new IllegalArgumentException();
    }
    
    private void buildGUI(List fieldNames)
    {
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      Box ifBox = Box.createHorizontalBox();
      Border border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), L.m("Wenn"));
      border = new CompoundBorder(border, new EmptyBorder(2,5,5,5));
      ifBox.setBorder(border);
      ifBox.add(fieldSelector);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(notSelector);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(testSelector);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(compareTo);
      this.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(ifBox));
      
      Box thenElseBox = Box.createVerticalBox();
      thenElseBox.setBorder(new EmptyBorder(10,8,0,8));
      this.add(thenElseBox);
      
      buildConditionalResultGUI(fieldNames, thenElseBox, L.m("Dann"), thenResult); 
      
      thenElseBox.add(Box.createVerticalStrut(10));

      buildConditionalResultGUI(fieldNames, thenElseBox, L.m("Sonst"), elseResult); 
    }

    /**
     * Fügt zu guiContainer die GUI-Elemente zum Bearbeiten von
     * conditionalResult hinzu.
     * @param fieldNames die Namen der Felder, die über einen Button in den Text
     *        eingefügt werden können.
     * @param label "Dann" oder "Sonst" (wird zur Beschriftung verwendet)
     * @author Matthias Benkmann (D-III-ITD D.10)
     */
    private void buildConditionalResultGUI(final List fieldNames, final JComponent guiContainer, String label, final ConditionalResult conditionalResult)
    {
      Box controls = Box.createHorizontalBox();
      guiContainer.add(controls);
      guiContainer.add(Box.createVerticalStrut(4));
      controls.add(new JLabel(label));
      controls.add(Box.createHorizontalGlue());
      AbstractButton textRadioButton = new JRadioButton(L.m("Text"), conditionalResult.type == 0);
      textRadioButton.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          if (conditionalResult.type != 0) 
          {
            conditionalResult.type = 0;
            if (conditionalResult.panel != null)
            {
              for (int i = guiContainer.getComponentCount() - 1; i >= 0; --i)
              {
                if (guiContainer.getComponent(i) == conditionalResult.panel)
                {
                  guiContainer.remove(i);
                  guiContainer.add(conditionalResult.scrollPane, i);
                  break;
                }
              }
              
              guiContainer.revalidate();
            }
          }
        }});
      controls.add(textRadioButton);
      controls.add(Box.createHorizontalGlue());
      AbstractButton ifThenElseRadioButton = new JRadioButton(L.m("Wenn...Dann..Sonst..."), conditionalResult.type == 1);
      ifThenElseRadioButton.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          if (conditionalResult.type != 1) 
          {
            conditionalResult.type = 1;
            
            if (conditionalResult.panel == null)
            {
              ConfigThingy conf = null;
              try{ 
                conf = new ConfigThingy("Func");
                ConfigThingy ifConf = conf.add("IF");
                ConfigThingy strCmpConf = ifConf.add("STRCMP");
                strCmpConf.add("VALUE").add((String)fieldNames.get(0));
                strCmpConf.add("");
                ifConf.add("THEN").add("");
                ifConf.add("ELSE").add("");
                //ACHTUNG! neue JIfThenElsePanels werden noch anderswo instanziiert
                conditionalResult.panel = new JIfThenElsePanel(conf, fieldNames, packNecessary);
                conditionalResult.panel.setBorder(CASCADED_ITE_BORDER);
              } catch(Exception x) 
              { 
                // Kann eigentlich nur passieren, wenn fieldNames verbockt ist.
                conditionalResult.panel = null;
                conditionalResult.type = 0;
              }
            }
           
            if (conditionalResult.panel != null)
            {
              for (int i = guiContainer.getComponentCount() - 1; i >= 0; --i)
              {
                if (guiContainer.getComponent(i) == conditionalResult.scrollPane)
                {
                  guiContainer.remove(i);
                  guiContainer.add(conditionalResult.panel, i);
                  break;
                }
              }
              
              guiContainer.revalidate();
              if (packNecessary != null) packNecessary.actionPerformed(null);
            }
          }

        }});
      controls.add(ifThenElseRadioButton);
      ButtonGroup radioGroup = new ButtonGroup();
      radioGroup.add(textRadioButton);
      radioGroup.add(ifThenElseRadioButton);
      controls.add(Box.createHorizontalGlue());
      JPotentiallyOverlongPopupMenuButton butt = new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"), makeInsertFieldActions(fieldNames, conditionalResult.text));
      butt.setFocusable(false);
      controls.add(butt);
      if (conditionalResult.type == 0)
        guiContainer.add(conditionalResult.scrollPane);
      else
        guiContainer.add(conditionalResult.panel);
    }
    
    /**
     * Liefert zur String-Liste fieldNames eine Liste von Actions, die die entsprechenden
     * Strings in text einfügen.
     * @author Matthias Benkmann (D-III-ITD D.10)
     * TESTED
     */
    private List makeInsertFieldActions(List fieldNames, final TextComponentTags text)
    {
      List actions = new Vector();
      Iterator iter = fieldNames.iterator();
      while (iter.hasNext())
      {
        final String name = (String)iter.next();
        Action action = new AbstractAction(name) {
          public void actionPerformed(ActionEvent e)
          {
            text.insertTag(name);
          }
        };
        actions.add(action);
      }
      return actions;
    }
    
    /**
     * Erzeugt {@link #fieldSelector}, {@link #notSelector}, {@link #testSelector} und
     * {@link #compareTo} auf Basis von conf. Falls das Vergleichsfeld nicht in fieldNames
     * gelistet ist wird es hinzugefügt.
     * genau =         STRCMP(VALUE "feld" "vergleichswert")
     * numerisch =     NUMCMP(VALUE "feld" "vergleichswert")
     * numerisch <     LT(VALUE "feld" "vergleichswert")
     * numerisch >     GT(VALUE "feld" "vergleichswert")
     * numerisch <=    LE(VALUE "feld" "vergleichswert")
     * numerisch >=    GE(VALUE "feld" "vergleichswert")
     * regulärer A.    MATCH(VALUE "feld" "vergleichswert")
     * 
     * @throws IllegalArgumentException falls conf nicht verstanden wird.
     */
    private void parseCondition(ConfigThingy conf, List fieldNames)
    {
      notSelector = new JComboBox(new String[]{"",L.m("nicht")});
      if (conf.getName().equals("NOT"))
      {
        try { conf = conf.getFirstChild(); }
        catch (NodeNotFoundException e){ throw new IllegalArgumentException(e); }
        notSelector.setSelectedIndex(1);
      }
      
      testSelector = new JComboBox(testTypes);
      determineTest: while(true){
        for (int i = 0; i < testTypes.length; ++i)
        {
          if (testTypes[i].func.equals(conf.getName()))
          {
            testSelector.setSelectedItem(testTypes[i]);
            break determineTest;
          }
        }
        throw new IllegalArgumentException();
      }
      
      if (conf.count() == 2)
      {
        try
        {
          ConfigThingy value = conf.getFirstChild();
          if (value.getName().equals("VALUE") && value.count() == 1 && 
              value.getFirstChild().count() == 0)
          {
            String compareConf = conf.getLastChild().toString();
            compareTo = new JTextField(compareConf, 20);
            fieldSelector = new JComboBox(new Vector(fieldNames));
            fieldSelector.setEditable(false);
            String fieldName = value.toString();
            Iterator iter = fieldNames.iterator();
            findFieldName: while(true){
              for (int i = 0; iter.hasNext(); ++i)
              {
                if (fieldName.equals(iter.next())) 
                {
                  fieldSelector.setSelectedIndex(i);
                  break findFieldName;
                }
              }
              fieldNames.add(fieldName);
              fieldSelector.addItem(fieldName);
              fieldSelector.setSelectedItem(fieldName);
              break findFieldName;
            }
          }
          else
            throw new IllegalArgumentException();
        }
        catch (NodeNotFoundException e)
        {
          throw new IllegalArgumentException(e);
        }
      } else 
        throw new IllegalArgumentException();
    }
    
    /**
     * Initialisiert res anhand von conf (was ein ELSE oder THEN Knoten sein muss).
     * @param fieldNames die Feldnamen, die in eventuellen Subpanels angeboten werden sollen.
     * 
     * @throws IllegalArgumentException falls conf nicht verstanden wird.
     *  
     * @author Matthias Benkmann (D-III-ITD D.10)
     * TESTED
     */
    private void parseThenElse(ConfigThingy conf, ConditionalResult res, List fieldNames)
    {
      try{
        if (conf.count() == 1 && 
           (conf.getName().equals("THEN") || conf.getName().equals("ELSE")))
        {
          ConfigThingy innerConf = conf.getFirstChild();
          
          JTextArea textArea = new JTextArea(3, 40);
          textArea.setLineWrap(true);
          res.scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          res.text = new TextComponentTags(textArea); 
          
          if (innerConf.count() == 0 || innerConf.getName().equals("CAT"))
          {
            res.type = 0;
            if (innerConf.getName().equals("CAT"))
              res.text.setContent(TextComponentTags.CAT_VALUE_SYNTAX, innerConf);
            else
              textArea.setText(innerConf.toString());
            return;
          } else
          { 
            res.type = 1; //ACHTUNG! neue JIfThenElsePanels werden noch anderswo instanziiert
            res.panel = new JIfThenElsePanel(conf, fieldNames, packNecessary);
            res.panel.setBorder(CASCADED_ITE_BORDER);
            return;
          }
        }
      }catch(NodeNotFoundException e) 
      {
        throw new IllegalArgumentException(e);
      }

      throw new IllegalArgumentException();
    }
    
    /**
     * Liefert ein frisches ConfigThingy, das die von diesem Panel repräsentierte
     * Trafo darstellt. Oberster Knoten ist immer "IF".
     * @author Matthias Benkmann (D-III-ITD D.10)
     * TESTED
     */
    public ConfigThingy getConf()
    {
      ConfigThingy conf = new ConfigThingy("IF");
      ConfigThingy conditionConf = conf;
      if (notSelector.getSelectedIndex() == 1) 
        conditionConf = conf.add("NOT");
      
      TestType test = (TestType)testSelector.getSelectedItem();
      conditionConf = conditionConf.add(test.func);
      conditionConf.add("VALUE").add(fieldSelector.getSelectedItem().toString());
      conditionConf.add(compareTo.getText());
      
      ConfigThingy thenConf = conf.add("THEN");
      if (thenResult.type == 0)
        thenConf.addChild(thenResult.text.getContent(TextComponentTags.CAT_VALUE_SYNTAX));
      else if (thenResult.type == 1)
        thenConf.addChild(thenResult.panel.getConf());
      
      ConfigThingy elseConf = conf.add("ELSE");
      if (elseResult.type == 0)
        elseConf.addChild(elseResult.text.getContent(TextComponentTags.CAT_VALUE_SYNTAX));
      else if (elseResult.type == 1)
        elseConf.addChild(elseResult.panel.getConf());

      return conf;
    }
  }
  
  /**
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und 
   * setzt params,isValid auf true.
   * 
   */
  private void updateTrafoConf()
  {
    params.conf = new ConfigThingy(params.conf.getName());
    params.conf.addChild(ifThenElsePanel.getConf());
    params.isValid = true;
  }
  
  /**
   * Fügt {@link #ifThenElsePanel} in dialog ein und zeigt ihn an.
   * @param dialog
   * @author Matthias Benkmann (D-III-ITD D.10)
   * TESTED
   */
  private void show(String windowTitle, JDialog dialog)
  {
    dialog.setAlwaysOnTop(true);
    dialog.setTitle(windowTitle);
    oehrchen = new MyWindowListener();
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(oehrchen);
    
    JPanel myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(new EmptyBorder(2,2,2,2));
    dialog.add(myPanel);
    JScrollPane scrollPane = new JScrollPane(ifThenElsePanel);
    scrollPane.setBorder(null);
    myPanel.add(scrollPane, BorderLayout.CENTER);
    Box lowerButtons = Box.createHorizontalBox();
    lowerButtons.setBorder(new EmptyBorder(10,4,5,4));
    myPanel.add(lowerButtons, BorderLayout.SOUTH);
    JButton cancel = new JButton(L.m("Abbrechen"));
    cancel.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }});
    JButton insert = new JButton(L.m("OK"));
    insert.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        updateTrafoConf();
        abort();
      }});
    lowerButtons.add(cancel);
    lowerButtons.add(Box.createHorizontalGlue());
    lowerButtons.add(insert);
    this.myDialog = dialog;
    repack();
  }

  /**
   * Führt myDialog.pack() aus (falls myDialog nicht null) und setzt ihn sichtbar
   * in der Mitte des Bildschirms. 
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  private void repack()
  {
    if (myDialog == null) return;
    myDialog.setVisible(false);
    myDialog.pack();
    int frameWidth = myDialog.getWidth();
    int frameHeight = myDialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myDialog.setLocation(x, y);
    myDialog.setVisible(true);
  }
  
  public void show(String windowTitle, Dialog owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }
  
  public void show(String windowTitle, Frame owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  public TrafoDialogParameters getExitStatus()
  {
    return params;
  }
  
  private void abort()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX) 
     *   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304
     * sind die folgenden 3 Zeilen nötig, damit der Dialog gc'ed werden
     * kann. Die Befehle sorgen dafür, dass kein globales Objekt (wie z.B.
     * der Keyboard-Fokus-Manager) indirekt über den JFrame den MailMerge kennt.  
     */
    if (myDialog != null)
    {
      myDialog.removeWindowListener(oehrchen);
      myDialog.getContentPane().remove(0);
      myDialog.setJMenuBar(null);
      
      myDialog.dispose();
      myDialog = null;
    }
    
    if (params.closeAction != null)
      params.closeAction.actionPerformed(new ActionEvent(this, 0, ""));
  }
  
  public void dispose() 
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{abort();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  };
  
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {abort(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
    
  }
  
  public static void main(String [] args) throws Exception
  {
    ConfigThingy funConf = new ConfigThingy("Func","IF(STRCMP(VALUE \"foo\", \"bar\") THEN(\"Krass, ey!\") ELSE( IF(MATCH(VALUE \"doof\" \"^foo\") THEN \"blarg\" ELSE \"Dusel\")) )");
    Vector fieldNames = new Vector();
    fieldNames.add("Du");
    fieldNames.add("bist");
    fieldNames.add("doof");
    final TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = funConf;
    params.fieldNames = fieldNames;
    params.closeAction = new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        if (params.isValid)
          System.out.println(params.conf.stringRepresentation());
        else
          System.out.println("ABORTED!");
      }};
    IfThenElseDialog dialog = new IfThenElseDialog(params);
    dialog.show("Wenn-Dann-Sonst-Test", (Dialog)null);
  }

}
