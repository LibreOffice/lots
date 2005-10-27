/*
* Dateiname: AbsenderAuswaehlen.java
* Projekt  : WollMux
* Funktion : Implementiert den Absenderdaten auswählen Dialog des BKS
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 25.10.2005 | BNK | Erstellung
* 27.10.2005 | BNK | back + CLOSEACTION
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.TestDatasourceJoiner;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen 
 * Dialogbeschreibung einen Dialog zum Auswählen eines Eintrages aus der
 * Persönlichen Absenderliste. Die private-Funktionen
 * dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AbsenderAuswaehlen
{
  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen.
   * %{Spalte} Syntax um entsprechenden Wert des Datensatzes einzufügen.
   */
  private final static String displayTemplate = "%{Nachname}, %{Vorname} (%{Rolle})";
  
  /**
   * Standardbreite für Textfelder
   */
  //private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
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
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
    { public void actionPerformed(ActionEvent e) { abort(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "back".
     */
    private ActionListener actionListener_back = new ActionListener()
      { public void actionPerformed(ActionEvent e) { back(); } };
      
    /**
     * ActionListener für Buttons mit der ACTION "editList".
     */
    private ActionListener actionListener_editList = new ActionListener()
    { public void actionPerformed(ActionEvent e) { editList(); } };
    
  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
      
  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;
  
  /**
   * Das JPanel der obersten Hierarchiestufe.
   */
  private JPanel mainPanel;
  
  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;
  
  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList palJList;
  
  
  /**
   * Der dem {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener) Konstruktor} 
   * übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;
  
  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten der Absenderliste
   * spezifiziert.
   */
  private ConfigThingy verConf;
  
  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes 
   * der Absenderliste spezifiziert.
   */
  private ConfigThingy abConf;
  
  /**
   * Überwacht Änderungen in der Auswahl und wählt den entsprechenden
   * Datensatz im DJ.
   */
  private MyListSelectionListener myListSelectionListener = new MyListSelectionListener(); 
  
  /**
   * Erzeugt einen neuen Dialog.
   * @param conf das ConfigThingy, das den Dialog beschreibt (der Vater des
   *        "Fenster"-Knotens.
   * @param abConf das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes beschreibt.
   * @param verConf das ConfigThingy, das den Absenderliste Verwalten Dialog beschreibt.
   * @param dj der DatasourceJoiner, der die PAL verwaltet.
   * @param dialogEndListener falls nicht null, wird 
   *        die {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *        Methode aufgerufen (im Event Dispatching Thread), 
   *        nachdem der Dialog geschlossen wurde.
   *        Das actionCommand des ActionEvents gibt die Aktion an, die
   *        das Speichern des Dialogs veranlasst hat. 
   * @throws ConfigurationErrorException im Falle eines schwerwiegenden
   *         Konfigurationsfehlers, der es dem Dialog unmöglich macht,
   *         zu funktionieren (z.B. dass der "Fenster" Schlüssel fehlt.
   */
  public AbsenderAuswaehlen(ConfigThingy conf, ConfigThingy verConf, ConfigThingy abConf, DatasourceJoiner dj, ActionListener dialogEndListener) throws ConfigurationErrorException
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.verConf = verConf;
    this.dialogEndListener = dialogEndListener;
    
    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    final ConfigThingy fensterDesc = fensterDesc1.query("Auswaehlen");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Auswaehlen' fehlt in "+conf.getName());
    
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(fensterDesc.getLastChild());}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  /**
   * Erzeugt das GUI.
   * @param fensterDesc die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void createGUI(ConfigThingy fensterDesc)
  {
    //use system LAF for window decorations
    try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception x){};
    JFrame.setDefaultLookAndFeelDecorated(true);
    
    palJList = new JList(new DefaultListModel());
   
    String title = "TITLE fehlt für Fenster AbsenderAuswaehlen/Auswaehlen";
    try{
      title = fensterDesc.get("TITLE").toString();
    } catch(Exception x){};
    
    try{
      closeAction = getAction(fensterDesc.get("CLOSEACTION").toString());
    } catch(Exception x){}
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.getContentPane().add(mainPanel);
  
    JPanel absenderliste = new JPanel(new GridBagLayout());
    JPanel buttons = new JPanel(new GridBagLayout());
    
    mainPanel.add(absenderliste, BorderLayout.CENTER);
    mainPanel.add(buttons, BorderLayout.PAGE_END);
    
    addUIElements(fensterDesc, "Absenderliste", absenderliste, 0, 1);
    addUIElements(fensterDesc, "Buttons", buttons, 1, 0);
    
    setListElements(palJList, dj.getLOS());
    selectSelectedDataset(palJList);
    
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
  }
  
  /** Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu.
   *  compo muss ein GridBagLayout haben. stepx und stepy geben an um
   *  wieviel mit jedem UI Element die x und die y Koordinate der Zelle
   *  erhöht werden soll. Wirklich sinnvoll sind hier nur (0,1) und (1,0).
   */
  private void addUIElements(ConfigThingy conf, String key, JComponent compo, int stepx, int stepy)
  {
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    //GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel     = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    GridBagConstraints gbcButton    = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcListBox    = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      
    ConfigThingy felderParent = conf.query(key);
    int y = -stepy;
    int x = -stepx; 
      
    Iterator piter = felderParent.iterator();
    while (piter.hasNext())
    {
      Iterator iter = ((ConfigThingy)piter.next()).iterator();
      while (iter.hasNext())
      {
        y += stepy;
        x += stepx;
        
        ConfigThingy uiElementDesc = (ConfigThingy)iter.next();
        try{
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
           * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
           * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
           * NOCH KONSISTENT IST!
           */
            
          //boolean readonly = false;
          String id = "";
          try{ id = uiElementDesc.get("ID").toString(); }catch(NodeNotFoundException e){}
          //try{ if (uiElementDesc.get("READONLY").toString().equals("true")) readonly = true; }catch(NodeNotFoundException e){}
          String type = uiElementDesc.get("TYPE").toString();
          
          if (type.equals("label"))
          {
            JLabel uiElement = new JLabel();
            gbcLabel.gridy = x;
            gbcLabel.gridy = y;
            compo.add(uiElement, gbcLabel);
            uiElement.setText(uiElementDesc.get("LABEL").toString());
          }
          else if (type.equals("glue"))
          {
            Box uiElement = Box.createHorizontalBox();
            try{
              int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE").toString());
              uiElement.add(Box.createHorizontalStrut(minsize));
            }catch(Exception e){}
            uiElement.add(Box.createHorizontalGlue());

            gbcGlue.gridy = x;
            gbcGlue.gridy = y;
            compo.add(uiElement, gbcGlue);
          }
          else
          if (type.equals("listbox"))
          {
            int lines = 10;
            try{ lines = Integer.parseInt(uiElementDesc.get("LINES").toString()); } catch(Exception e){}
            
            JList list;
            if (id.equals("pal")) 
              list = palJList;
            else
              list = new JList(new DefaultListModel());
            
            list.setVisibleRowCount(lines);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setLayoutOrientation(JList.VERTICAL);
            list.setPrototypeCellValue("Matthias S. Benkmann ist euer Gott (W-OLL-MUX-5.1)");
            
            list.addListSelectionListener(myListSelectionListener);
            
            String action = "";
            try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
            
            ActionListener actionL = getAction(action);
            if (actionL != null) list.addMouseListener(new MyActionMouseListener(list, actionL));
            
            JScrollPane scrollPane = new JScrollPane(list);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            
            gbcListBox.gridx = x;
            gbcListBox.gridy = y;
            compo.add(scrollPane, gbcListBox);
          }
          else
          if (type.equals("button"))
          {
            String action = "";
            try{
              action = uiElementDesc.get("ACTION").toString();
            }catch(NodeNotFoundException e){}
              
            String label  = uiElementDesc.get("LABEL").toString();
              
            char hotkey = 0;
            try{
              hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
            }catch(Exception e){}
              
            JButton button = new JButton(label);
            button.setMnemonic(hotkey);

            gbcButton.gridx = x;
            gbcButton.gridy = y;
            compo.add(button, gbcButton);
            
            ActionListener actionL = getAction(action);
            if (actionL != null) button.addActionListener(actionL);
            
          }
          else
          {
            Logger.error("Ununterstützter TYPE für User Interface Element: "+type);
          }
        } catch(NodeNotFoundException e) {Logger.error(e);}
      }
    }
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode
   * eines ActionListeners aus.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyActionMouseListener extends MouseAdapter
  {
    private JList list;
    private ActionListener action;
    
    public MyActionMouseListener(JList list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }
    
    public void mouseClicked(MouseEvent e) 
    {
      if (e.getClickCount() == 2) 
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0) return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location)) return;
        action.actionPerformed(null);
      }
    }
  }

  
  /**
   * Übersetzt den Namen einer ACTION in eine Referenz auf das
   * passende actionListener_... Objekt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ActionListener getAction(String action)
  {
    if (action.equals("abort"))
    {
      return actionListener_abort;
    } 
    else if (action.equals("back"))
    {
      return actionListener_back;
    }
    else if (action.equals("editList"))
    {
      return actionListener_editList;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error("Ununterstützte ACTION: "+action);
    
    return null;
  }
  
  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link ListElement} Objekte gewrappt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList list, QueryResults data)
  {
    Object[] elements = new Object[data.size()];
    Iterator iter = data.iterator();
    int i = 0;
    while (iter.hasNext()) elements[i++] = new ListElement((DJDataset)iter.next());
    Arrays.sort(elements, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return o1.toString().compareTo(o2.toString());
      }
    });
   
    DefaultListModel listModel = (DefaultListModel)list.getModel();
    listModel.clear();
    for (i = 0; i < elements.length; ++i)
      listModel.addElement(elements[i]);
  }
  
  private void selectSelectedDataset(JList list)
  {
    DefaultListModel listModel = (DefaultListModel)list.getModel();
    for (int i = 0; i < listModel.size(); ++i)
      if (((ListElement)listModel.get(i)).getDataset().isSelectedDataset())
        list.setSelectedValue(listModel.get(i), true);
  }
  
  
  /**
   * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getDisplayString(DJDataset ds)
  {
    return substituteVars(displayTemplate, ds);
  }
  
  /**
   * Wrapper um ein DJDataset zum Einfügen in eine JList. Die
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ListElement
  {
    private String displayString;
    private DJDataset ds;
    
    public ListElement(DJDataset ds)
    {
      displayString = getDisplayString(ds);
      this.ds = ds;
    }

    public String toString()
    {
      return displayString;
    }
    
    public DJDataset getDataset() {return ds;}
  }

  /**
   * Ersetzt "%{SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String substituteVars(String str, Dataset datensatz)
  {
    Pattern p = Pattern.compile("%\\{([a-zA-Z0-9]+)\\}");
    Matcher m = p.matcher(str);
    if (m.find()) do
    {
      String spalte = m.group(1);
      String wert = spalte;
      try
      {
        wert = datensatz.get(spalte);
        wert = wert.replaceAll("%", "");
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(e);
      }
      str = str.substring(0, m.start()) + wert + str.substring(m.end());
      m = p.matcher(str);
    } while (m.find());
    return str;
  }


  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden
   * wenn kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      JList list = (JList)e.getSource();
      if (list != palJList) return;
      
      ListElement ele = (ListElement)list.getSelectedValue();
      if (ele == null) 
        selectSelectedDataset(list);
      else
        ele.getDataset().select();
    }
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }

  
  /**
   * Beendet den Dialog und liefer actionCommand an den
   * dialogEndHandler zurück (falls er nicht null ist).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(actionCommand,0,actionCommand));
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editList()
  {
    ActionListener del = new MyDialogEndListener(myConf, verConf, abConf, dj, dialogEndListener, null);
    dialogEndListener = null;
    abort();
    try
    {
      new PersoenlicheAbsenderlisteVerwalten(verConf, abConf, dj, del);
    }
    catch (ConfigurationErrorException x)
    {
     Logger.error(x);
    }
  }
  
    
  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;
    private ConfigThingy abConf;
    private ConfigThingy verConf;
    private DatasourceJoiner dj;
    private ActionListener dialogEndListener;
    private String actionCommand;
    
    /**
     * Falls actionPerformed() mit getActionCommand().equals("back")
     * aufgerufen wird, wird ein neuer  AbsenderAuswaehlen Dialog mit
     * den übergebenen Parametern erzeugt. Ansonsten wird
     * der dialogEndListener mit actionCommand aufgerufen. Falls actionCommand
     * null ist wird das action command des ActionEvents weitergereicht,
     * der actionPerformed() übergeben wird.
     */
    public MyDialogEndListener(ConfigThingy conf, ConfigThingy verConf,
        ConfigThingy abConf, DatasourceJoiner dj,
        ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.verConf = verConf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("back"))
        try{
          new AbsenderAuswaehlen(conf, verConf, abConf, dj, dialogEndListener);
        }catch(Exception x) {Logger.error(x);}
      else
      {
        if (actionCommand == null) actionCommand = e.getActionCommand();
        if (dialogEndListener != null)
          dialogEndListener.actionPerformed(new ActionEvent(actionCommand,0,actionCommand));
      }
    }
  }
  
  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener(){}
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { closeAction.actionPerformed(null); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) {}
  }

  
  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren
   * Aufrufe von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt
   * asynchron. Wurde dem Konstruktor ein entsprechender ActionListener
   * übergeben, so wird seine actionPerformed() Funktion aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    //  GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          abort();
        }
      });
    }
    catch(Exception x) {/*Hope for the best*/}
  }
  
  /**
   * Sorgt für das dauernde Neustarten des Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class RunTest implements ActionListener
  {
    private DatasourceJoiner dj;
    private ConfigThingy conf;
    private ConfigThingy verConf;
    private ConfigThingy abConf;
    
    public RunTest(ConfigThingy conf, ConfigThingy verConf, ConfigThingy abConf, DatasourceJoiner dj)
    {
      this.dj = dj;
      this.conf = conf;
      this.abConf = abConf;
      this.verConf = verConf;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      try{
        try{
          if (e.getActionCommand().equals("abort")) System.exit(0);
        }catch(Exception x){}
        new AbsenderAuswaehlen(conf, verConf, abConf, dj, this);
      } catch(ConfigurationErrorException x)
      {
        Logger.error(x);
      }
    }
  }
  
  public static void main(String[] args) throws Exception
  {
    String confFile = "testdata/WhoAmI.conf";
    String verConfFile = "testdata/PAL.conf";
    String abConfFile = "testdata/AbsenderdatenBearbeiten.conf";
    ConfigThingy conf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),confFile));
    ConfigThingy verConf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),verConfFile));
    ConfigThingy abConf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),abConfFile));
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    QueryResults entries = dj.find("Vorname", "M*");
    Iterator iter = entries.iterator();
    while (iter.hasNext())
    {
      ((DJDataset)iter.next()).copy();
    }
    RunTest test = new RunTest(conf.get("AbsenderAuswaehlen"), verConf.get("PersoenlicheAbsenderliste"), abConf.get("AbsenderdatenBearbeiten"), dj);
    test.actionPerformed(null);
    Thread.sleep(600000);
    System.exit(0);
  }

}

