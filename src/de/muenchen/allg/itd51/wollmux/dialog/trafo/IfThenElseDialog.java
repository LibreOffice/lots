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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
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
    
    ifThenElsePanel = new JIfThenElsePanel(params.conf, params.fieldNames);
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
    private static final TestType[] testTypes = {new TestType("genau =","STRCMP"),
                                                 new TestType("numerisch =", "NUMCMP"),
                                                 new TestType("numerisch <", "LT"),
                                                 new TestType("numerisch <=", "LE"),
                                                 new TestType("numerisch >", "GT"),
                                                 new TestType("numerisch >=", "GE"),
                                                 new TestType("regulärer A.", "MATCH")};
    
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
    
    
    
    /**
     * Erzeugt eine Dialog-Komponente, die mit den Werten aus conf vorbelegt ist, wobei
     * fieldNames die angebotenen Feldnamen als Strings enthält.
     *
     * @throws IllegalArgumentException falls conf nicht verstanden wird.
     * 
     * @author Matthias Benkmann (D-III-ITD D.10)
     * TESTED
     */
    public JIfThenElsePanel(ConfigThingy conf, List fieldNames)
    {
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
      //ifBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), "Wenn"));
      ifBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Wenn"));
      ifBox.add(fieldSelector);
      ifBox.add(notSelector);
      ifBox.add(testSelector);
      ifBox.add(compareTo);
      this.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(ifBox));
      
      Box thenControls = Box.createHorizontalBox();
      this.add(thenControls);
      thenControls.add(new JLabel("Dann"));
      AbstractButton thenTextRadioButton = new JRadioButton("Text", true);
      thenControls.add(thenTextRadioButton);
      AbstractButton thenITERadioButton = new JRadioButton("Wenn...Dann..Sonst...");
      thenControls.add(thenITERadioButton);
      ButtonGroup thenRadioGroup = new ButtonGroup();
      thenRadioGroup.add(thenTextRadioButton);
      thenRadioGroup.add(thenITERadioButton);
      thenControls.add(Box.createHorizontalGlue());
      JPotentiallyOverlongPopupMenuButton butt = new JPotentiallyOverlongPopupMenuButton("Serienbrieffeld", makeInsertFieldActions(fieldNames, thenResult.text));
      butt.setFocusable(false);
      thenControls.add(butt); 
                      
      
      this.add(thenResult.scrollPane);
      
      Box elseControls = Box.createHorizontalBox();
      this.add(elseControls);
      elseControls.add(new JLabel("Sonst"));
      AbstractButton elseTextRadioButton = new JRadioButton("Text", true);
      elseControls.add(elseTextRadioButton);
      AbstractButton elseITERadioButton = new JRadioButton("Wenn...Dann..Sonst...");
      elseControls.add(elseITERadioButton);
      ButtonGroup elseRadioGroup = new ButtonGroup();
      elseRadioGroup.add(elseTextRadioButton);
      elseRadioGroup.add(elseITERadioButton);
      elseControls.add(Box.createHorizontalGlue());
      butt = new JPotentiallyOverlongPopupMenuButton("Serienbrieffeld", 
          makeInsertFieldActions(fieldNames, elseResult.text));
      butt.setFocusable(false);
      elseControls.add(butt);
      
      this.add(elseResult.scrollPane);
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
     * {@link #compareTo} auf Basis von conf.
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
      notSelector = new JComboBox(new String[]{"","nicht"});
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
          conf = conf.getFirstChild();
          
          JTextArea textArea = new JTextArea(3, 40);
          textArea.setLineWrap(true);
          res.scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          res.text = new TextComponentTags(textArea); 
          
          if (conf.count() == 0)
          {
            res.type = 0;
            textArea.setText(conf.toString());
            return;
          } else if (conf.count() == 1)
          { //TODO Diesen Fall testen
            res.type = 1;
            res.panel = new JIfThenElsePanel(conf, fieldNames);
            return;
          }
        }
      }catch(NodeNotFoundException e) 
      {
        throw new IllegalArgumentException(e);
      }

      throw new IllegalArgumentException();
    }
    
  }
  
  /**
   * Fügt {@link #ifThenElsePanel} in dialog ein und zeigt ihn an.
   * @param dialog
   * @author Matthias Benkmann (D-III-ITD D.10)
   * TESTED
   */
  private void show(JDialog dialog)
  {
    dialog.setAlwaysOnTop(true);
    oehrchen = new MyWindowListener();
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(oehrchen);
    
    dialog.setLayout(new BorderLayout());
    dialog.add(ifThenElsePanel, BorderLayout.CENTER);
    Box lowerButtons = Box.createHorizontalBox();
    dialog.add(lowerButtons, BorderLayout.SOUTH);
    JButton cancel = new JButton("Abbrechen");
    JButton insert = new JButton("Einfügen");
    lowerButtons.add(cancel);
    lowerButtons.add(Box.createHorizontalGlue());
    lowerButtons.add(insert);
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    this.myDialog = dialog;
    dialog.setVisible(true);
  }
  
  public void show(Dialog owner)
  {
    if (owner == null)
      show(new JDialog());
    else
      show(new JDialog(owner));
  }
  
  public void show(Frame owner)
  {
    if (owner == null)
      show(new JDialog());
    else
      show(new JDialog(owner));
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
     * sind die folgenden 3 Zeilen nötig, damit der MailMerge gc'ed werden
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
    
//    if (abortListener != null)
//      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
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
    ConfigThingy funConf = new ConfigThingy("IF","STRCMP(VALUE \"foo\", \"bar\") THEN(\"Krass, ey!\") ELSE(\"Oder sonst!\")");
    Vector fieldNames = new Vector();
    fieldNames.add("Du");
    fieldNames.add("bist");
    fieldNames.add("doof");
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = funConf;
    params.fieldNames = fieldNames;
    IfThenElseDialog dialog = new IfThenElseDialog(params);
    dialog.show((Dialog)null);
  }

}
