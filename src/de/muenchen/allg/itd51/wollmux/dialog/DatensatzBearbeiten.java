/*
* Dateiname: DatensatzBearbeiten.java
* Projekt  : WollMux
* Funktion : Dynamisches Erzeugen eines Swing-GUIs für das Bearbeiten eines Datensatzes anhand von ConfigThingy
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.10.2005 | BNK | Erstellung
* 14.10.2005 | BNK | Interaktion mit DJDataset
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;

/**
 * TODO Doku
 * TODO Bei Speichern und Standardwerte wiederherstellen erst sicherheitsabfrage
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatensatzBearbeiten
{
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
  private final static int TF_BORDER = 4;
  private final static int SEP_BORDER = 7;
  private final static int BUTTON_BORDER = 2;
  private DJDataset datensatz;
  private Map fenster;
  private DialogWindow currentWindow;
  private JFrame myFrame;
  private JPanel cardPanel;
  private CardLayout cardLayout;
  private String firstWindow;
  private ActionListener actionListenerDatensatzBearbeiten_abort = new ActionListener()
        { public void actionPerformed(ActionEvent e){ abort(); } };
  private ActionListener actionListenerDatensatzBearbeiten_restoreStandard = new ActionListener()
        { public void actionPerformed(ActionEvent e){ restoreStandard(); } };
    
  
  /**
   * Allgemein gilt für diese Klasse: public-Funktionen dürfen NICHT vom 
   * Event-Dispatching Thread aus aufgerufen werden, private-Funktionen
   * dürfen NUR vom Event-Dispatching Thread aufgerufen werden.
   * @param conf, hiervon wird Unterschlüssel "Fenster" ausgewertet
   * @throws ConfigurationErrorException im Falle eines schwerwiegenden
   *         Konfigurationsfehlers, der es dem Dialog unmöglich macht,
   *         zu funktionieren (z.B. dass der "Fenster" Schlüssel fehlt.
   */
  public DatensatzBearbeiten(ConfigThingy conf, DJDataset datensatz) throws ConfigurationErrorException
  {
    this.datensatz = datensatz;
    
    fenster = new HashMap();
    
    final ConfigThingy fensterDesc = conf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            createGUI(fensterDesc);
        }
      });
    }
    catch(Exception x) {/*Hope for the best*/}
    
    
  }
  
  private void createGUI(ConfigThingy fensterDesc)
  {
    //use system LAF for window decorations
    try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception x){};
    JFrame.setDefaultLookAndFeelDecorated(true);
    
    //Create and set up the window.
    myFrame = new JFrame("Absenderdaten bearbeiten");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);
    cardPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.getContentPane().add(cardPanel);
    
    Iterator iter = fensterDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = (ConfigThingy)iter.next();
      String fensterName = neuesFenster.getName();
      DialogWindow newWindow = new DialogWindow(fensterName, neuesFenster);
      if (firstWindow == null) firstWindow = fensterName;
      fenster.put(fensterName,newWindow);
      cardPanel.add(newWindow.JPanel(),fensterName);
    }
    
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
  }
  
  private void abort(){myFrame.dispose();}
  private void restoreStandard(){}
  
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener(){}
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { abort(); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) {}
  }

  public void dispose()
  {
    //  GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          abort();
        }
      });
    }
    catch(Exception x) {/*Hope for the best*/}
  }

  
  public void show()
  {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showEDT();
      }
    });
  }
  
  private void showEDT()
  {
    if (fenster.size() == 0) return;
    if (currentWindow == null) showWindow(firstWindow);
  }

  
  private void showWindow(String name)
  {
    if (!fenster.containsKey(name)) return;
    
    currentWindow = (DialogWindow)fenster.get(name);

    myFrame.setTitle(currentWindow.getTitle());
    myFrame.setVisible(true);
    cardLayout.show(cardPanel,currentWindow.getName());
  }

  private abstract class DataControl
  {
    protected String startText;
    protected String columnName;
    protected boolean myDatasetIsLocal;
    
    
    public abstract String getTextFromControl();
    public String getColumnName() {return columnName;}
    public abstract void setTextInControl(String text);
    public boolean hasBeenModified() {return !startText.equals(getTextFromControl());}
    public boolean datasetIsLocal() {return myDatasetIsLocal;}
  }
  
  private class TextComponentDataControl extends DataControl
  {
    JTextComponent myComponent;
    
    public TextComponentDataControl(String colName, JTextComponent compo)
    throws ColumnNotFoundException
    {
      columnName = colName;
      myDatasetIsLocal = datensatz.hasLocalOverride(colName);
      myComponent = compo;
      startText = datensatz.get(colName);
      setTextInControl(startText);
    }

    public String getTextFromControl()
    {
      return myComponent.getText();
    }

    public void setTextInControl(String text)
    {
      myComponent.setText(text);
    }
  }
  
  private class ComboBoxDataControl extends DataControl
  {
    JComboBox myComponent;
    
    public ComboBoxDataControl(String colName, JComboBox compo)
    throws ColumnNotFoundException
    {
      columnName = colName;
      myDatasetIsLocal = datensatz.hasLocalOverride(colName);
      myComponent = compo;
      startText = datensatz.get(colName);
      addItem(startText);
      setTextInControl(startText);
    }

    public void addItem(String text)
    {
      for (int i = myComponent.getItemCount() - 1; i >=0 ; --i)
      {
        if (myComponent.getItemAt(i).equals(text)) return;
      }
      myComponent.addItem(text);
    }
    
    public String getTextFromControl()
    {
      return myComponent.getSelectedItem().toString();
    }

    public void setTextInControl(String text)
    {
      myComponent.setSelectedItem(text);
    }
  }
  
  
  private class DialogWindow
  {
    private JPanel myPanel;
    private JPanel myInputPanel;
    private JPanel myButtonPanel;
    private String title;
    private String name;
    
    public JPanel JPanel() {return myPanel;}
    public String getTitle() {return title;}
    
    public DialogWindow(String name, final ConfigThingy conf)
    {
      this.name = name;
      createGUI(conf);
    }
    
    public String getName() {return name;}
    
    public void show(){}
    public void hide(){}
    
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public void createGUI(ConfigThingy conf)
    {
      title = "TITLE fehlt in Fensterbeschreibung";
      try{title = ""+conf.get("TITLE");}catch(NodeNotFoundException x){}
      
      myPanel = new JPanel(new BorderLayout());
      myInputPanel = new JPanel();
      myButtonPanel = new JPanel();
      
      myInputPanel.setLayout(new GridBagLayout());//new BoxLayout(myInputPanel, BoxLayout.PAGE_AXIS));
      myButtonPanel.setLayout(new BoxLayout(myButtonPanel, BoxLayout.LINE_AXIS));
      myButtonPanel.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,0,0));
      
      myPanel.add(myInputPanel, BorderLayout.CENTER);
      myPanel.add(myButtonPanel, BorderLayout.PAGE_END);
      
        //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
      GridBagConstraints gbcBottomglue= new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.PAGE_END,   GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcLabel     = new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcSeparator = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER,     GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0,0,0,10),0,0);
      GridBagConstraints gbcTextfield = new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcTextarea =  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcCombobox  = new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      
      ConfigThingy felder = conf.query("Eingabefelder");
      int y = -1;
      
      Iterator iter = felder.iterator();
      while (iter.hasNext())
      {
        ++y;
        ConfigThingy uiElementDesc = (ConfigThingy)iter.next();
        try{
          
          
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
           * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
           * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
           * NOCH KONSISTENT IST!
           */
          
          
          String type = uiElementDesc.get("TYPE").toString();
          if (type.equals("textfield"))
          {
            JLabel label = new JLabel();
            label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
            gbcLabelLeft.gridy = y;
            myInputPanel.add(label, gbcLabelLeft);
            try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
            
            JPanel uiElement = new JPanel(new GridLayout(1,1));
            JTextField tf = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
            
            try
            {
              new TextComponentDataControl(uiElementDesc.get("DB_SPALTE").toString(), tf);
            } catch (Exception x) { Logger.error(x); }
            
            //Font fnt = tf.getFont();
            //tf.setFont(fnt.deriveFont((float)14.0));
            //tf.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            uiElement.add(tf);
            uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
            gbcTextfield.gridy = y;
            myInputPanel.add(uiElement, gbcTextfield);
          }
          else       
            if (type.equals("textarea"))
            {
              JLabel label = new JLabel();
              label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabelLeft.gridy = y;
              myInputPanel.add(label, gbcLabelLeft);
              try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
              
              int lines = 3;
              try{ lines = Integer.parseInt(uiElementDesc.get("LINES").toString()); } catch(Exception x){}
              JTextArea textarea = new JTextArea(lines,TEXTFIELD_DEFAULT_WIDTH);
              try
              {
                new TextComponentDataControl(uiElementDesc.get("DB_SPALTE").toString(), textarea);
              } catch (Exception x) { Logger.error(x); }

              JPanel uiElement = new JPanel(new GridLayout(1,1));
              JScrollPane scrollPane = new JScrollPane(textarea);//, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_NEVER);
              scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
              scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
              uiElement.add(scrollPane);
              
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcTextarea.gridy = y;
              myInputPanel.add(uiElement, gbcTextarea);
            }
            else
              if (type.equals("separator"))
              {
                JPanel uiElement = new JPanel(new GridLayout(1,1));
                uiElement.add(new JSeparator(SwingConstants.HORIZONTAL));
                uiElement.setBorder(BorderFactory.createEmptyBorder(SEP_BORDER,0,SEP_BORDER,0));
                gbcSeparator.gridy = y;
                myInputPanel.add(uiElement, gbcSeparator);
              }
          if (type.equals("label"))
          {
            JLabel uiElement = new JLabel();
            uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
            gbcLabel.gridy = y;
            myInputPanel.add(uiElement, gbcLabel);
            uiElement.setText(uiElementDesc.get("LABEL").toString());
          }
          else
            if (type.equals("combobox"))
            {
              JLabel label = new JLabel();
              label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabelLeft.gridy = y;
              myInputPanel.add(label, gbcLabelLeft);
              try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
              
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              JComboBox combo = new JComboBox();
              try
              {
                ComboBoxDataControl comboCtrl = new ComboBoxDataControl(uiElementDesc.get("DB_SPALTE").toString(), combo);
                Iterator values = uiElementDesc.query("VALUES").iterator();
                while (values.hasNext())
                {
                  comboCtrl.addItem(values.next().toString());
                }
              } catch (Exception x) { Logger.error(x); }

              uiElement.add(combo);
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcCombobox.gridy = y;
              myInputPanel.add(uiElement, gbcCombobox);
            }
        } catch(NodeNotFoundException x) {Logger.error(x);}
      }
      
      ++y;
      gbcBottomglue.gridy = y;
      myInputPanel.add(Box.createGlue(), gbcBottomglue);
      
      ConfigThingy buttons = conf.query("Buttons");
      iter = buttons.iterator();
      boolean firstButton = true;
      while (iter.hasNext())
      {
        ConfigThingy uiElementDesc = (ConfigThingy)iter.next();
        try{
          
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
           * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
           * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
           * NOCH KONSISTENT IST!
           */
          
          String type = uiElementDesc.get("TYPE").toString();
          if (type.equals("button"))
          {
            String action = uiElementDesc.get("ACTION").toString();
            String label  = uiElementDesc.get("LABEL").toString();
            char hotkey = 0;
            try{
              hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
            }catch(Exception x){}
            
            JButton button = new JButton(label);
            button.setMnemonic(hotkey);
            JPanel uiElement = new JPanel(new GridLayout(1,1));
            int left = BUTTON_BORDER;
            if (firstButton) {left = 0; firstButton = false;}
            int right = BUTTON_BORDER;
            if (!iter.hasNext()) right = 0;
            uiElement.setBorder(BorderFactory.createEmptyBorder(0,left,0,right));
            uiElement.add(button);
            myButtonPanel.add(uiElement);
            
            if (action.equals("DatensatzBearbeiten_abort"))
            {
              button.addActionListener(actionListenerDatensatzBearbeiten_abort);
            }
            if (action.equals("DatensatzBearbeiten_restoreStandard"))
            {
              button.addActionListener(actionListenerDatensatzBearbeiten_restoreStandard);
            }
            if (action.equals("DatensatzBearbeiten_switchWindow"))
            {
              final String window = uiElementDesc.get("WINDOW").toString();
              button.addActionListener( new ActionListener()
                  { public void actionPerformed(ActionEvent e){ showWindow(window); }
                  });
            }
          }
          else if (type.equals("glue"))
          {
            try{
              int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE").toString());
              myButtonPanel.add(Box.createHorizontalStrut(minsize));
            }catch(Exception x){}
            myButtonPanel.add(Box.createHorizontalGlue());
          }
        } catch(NodeNotFoundException x) {Logger.error(x);}
      }
      
    }
  }
  
  public static void main(String[] args) throws Exception
  {
    String confFile = "testdata/AbsenderdatenBearbeiten.conf";
    DatasourceJoiner dj = new DatasourceJoiner();
    DJDataset datensatz = dj.getSelectedDataset();
    ConfigThingy conf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),confFile)); 
    DatensatzBearbeiten ab = new DatensatzBearbeiten(conf.get("AbsenderdatenBearbeiten"), datensatz);
    ab.show();
    Thread.sleep(60000);
    ab.dispose();
  }
}
