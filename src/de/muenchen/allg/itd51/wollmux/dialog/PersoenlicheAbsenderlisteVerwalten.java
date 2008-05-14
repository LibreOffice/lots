//TODO L.m()
/*
* Dateiname: PersoenlicheAbsenderlisteVerwalten.java
* Projekt  : WollMux
* Funktion : Implementiert den Hinzufügen/Entfernen Dialog des BKS
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330/5980
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 17.10.2005 | BNK | Erstellung
* 18.10.2005 | BNK | PAL Verwalten GUI großteils implementiert (aber funktionslos)
* 19.10.2005 | BNK | Suche implementiert
* 20.10.2005 | BNK | Suche getestet
* 24.10.2005 | BNK | Restliche ACTIONS implementiert
*                  | Doppelklickbehandlung
*                  | Sortierung
*                  | Gegenseitiger Ausschluss der Selektierung
* 25.10.2005 | BNK | besser kommentiert
* 27.10.2005 | BNK | back + CLOSEACTION
* 31.10.2005 | BNK | Behandlung von TimeoutException bei find()
* 02.11.2005 | BNK | +editNewPALEntry()
* 10.11.2005 | BNK | +DEFAULT_* Konstanten
* 14.11.2005 | BNK | Exakter Match "Nachname" entfernt aus 1-Wort-Fall
* 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
* 22.11.2005 | BNK | Bei Initialisierung ist der selectedDataset auch in der Liste 
*                  | selektiert.
* 20.01.2006 | BNK | Default-Anrede für Tinchen WollMux ist "Frau"
* 19.10.2006 | BNK | Credits
* 23.10.2006 | BNK | Bugfix: Bei credits an wurden Personen ohne Mail nicht dargestellt.
* 06.11.2006 | BNK | auf AlwaysOnTop gesetzt.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.TestDatasourceJoiner;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen 
 * Dialogbeschreibung einen Dialog zum Hinzufügen/Entfernen von Einträgen
 * der Persönlichen Absenderliste auf. Die private-Funktionen
 * dagegen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PersoenlicheAbsenderlisteVerwalten
{
  public static final String DEFAULT_ROLLE = "D-WOLL-MUX-5.1";

  public static final String DEFAULT_NACHNAME = "Wollmux";

  public static final String DEFAULT_VORNAME = "Tinchen";
  
  public static final String DEFAULT_ANREDE = "Frau";
  
  private final URL MB_URL = this.getClass().getClassLoader().getResource("data/mb.png");
  private final URL CL_URL = this.getClass().getClassLoader().getResource("data/cl.png");

  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen.
   * %{Spalte} Syntax um entsprechenden Wert des Datensatzes einzufügen.
   */
  private final static String displayTemplate = "%{Nachname}, %{Vorname} (%{Rolle})";
  
  /**
   * Standardbreite für Textfelder
   */
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
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
     * ActionListener für Buttons mit der ACTION "search".
     */
    private ActionListener actionListener_search = new ActionListener()
    { public void actionPerformed(ActionEvent e) { search(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "addToPAL".
     */
    private ActionListener actionListener_addToPAL = new ActionListener()
    { public void actionPerformed(ActionEvent e) { addToPAL(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "removeFromPAL".
     */
    private ActionListener actionListener_removeFromPAL = new ActionListener()
    { public void actionPerformed(ActionEvent e) { removeFromPAL(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "editEntry".
     */
    private ActionListener actionListener_editEntry = new ActionListener()
    { public void actionPerformed(ActionEvent e) { editEntry(); } };
      
    /**
     * ActionListener für Buttons mit der ACTION "copyEntry".
     */
    private ActionListener actionListener_copyEntry = new ActionListener()
    { public void actionPerformed(ActionEvent e) { copyEntry(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "newPALEntry".
     */
    private ActionListener actionListener_newPALEntry = new ActionListener()
    { public void actionPerformed(ActionEvent e) { newPALEntry(); } };
    
    /**
     * ActionListener für Buttons mit der ACTION "newPALEntry".
     */
    private ActionListener actionListener_editNewPALEntry = new ActionListener()
    { public void actionPerformed(ActionEvent e) { editNewPALEntry(); } };
    
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
   * Speichert Referenzen auf die JButtons, die zu deaktivieren sind,
   * wenn kein Eintrag in einer Liste selektiert ist.
   */
  private List<JButton> buttonsToGreyOutIfNothingSelected = new Vector<JButton>();
  
  /**
   * Die Listbox mit den Suchresultaten.
   */
  private JList resultsJList;
  
  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList palJList;
  
  /**
   * Das Textfeld in dem der Benutzer seine Suchanfrage eintippt.
   */
  private JTextField query;
  
  /**
   * Der dem {@link #PersoenlicheAbsenderlisteVerwalten(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener) Konstruktor} 
   * übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;

  
  /**
   * Das ConfigThingy, das den Dialog Datensatz Bearbeiten für das Bearbeiten
   * eines Datensatzes der PAL spezifiziert.
   */
  private ConfigThingy abConf;
  
  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden
   * wenn kein Eintrag selektiert ist.
   */
  private MyListSelectionListener myListSelectionListener = new MyListSelectionListener(); 
  
  /**
   * Erzeugt einen neuen Dialog.
   * @param conf das ConfigThingy, das den Dialog beschreibt (der Vater des
   *        "Fenster"-Knotens.
   * @param abConf das ConfigThingy, das den Absenderdaten Bearbeiten Dialog beschreibt.
   * @param dj der DatasourceJoiner, der die zu bearbeitende Liste verwaltet.
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
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, ConfigThingy abConf, DatasourceJoiner dj, ActionListener dialogEndListener) throws ConfigurationErrorException
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.dialogEndListener = dialogEndListener;
    
    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    final ConfigThingy fensterDesc = fensterDesc1.query("Verwalten");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Verwalten' fehlt in "+conf.getName());
    
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(fensterDesc.getLastChild());}catch(Exception x)
            {
              Logger.error(x);
            };
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
    Common.setLookAndFeelOnce();
    
    resultsJList = new JList(new DefaultListModel());
    ListCellRenderer myRenderer = new MyListCellRenderer();
    resultsJList.setCellRenderer(myRenderer);
    palJList = new JList(new DefaultListModel());
    palJList.setCellRenderer(myRenderer);
    query = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
    
    
    String title = "TITLE fehlt für Fenster PersoenlicheAbsenderListeVerwalten/Verwalten";
    try{
      title = fensterDesc.get("TITLE").toString();
    } catch(Exception x){};
    
    try{
      closeAction = getAction(fensterDesc.get("CLOSEACTION").toString());
    } catch(Exception x){}
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    myFrame.setAlwaysOnTop(true);
    
    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.getContentPane().add(mainPanel);
  
    JPanel introSuche = new JPanel();
    introSuche.setLayout(new BoxLayout(introSuche, BoxLayout.PAGE_AXIS));
    JPanel suchergebnisHinUndHerAbsenderliste = new JPanel();
    suchergebnisHinUndHerAbsenderliste.setLayout(new BoxLayout(suchergebnisHinUndHerAbsenderliste, BoxLayout.LINE_AXIS));
    JPanel fussbereich = new JPanel(new GridBagLayout());
    JPanel intro = new JPanel(new GridBagLayout());
    JPanel suche = new JPanel(new GridBagLayout());
    JPanel suchergebnis = new JPanel(new GridBagLayout());
    JPanel hinUndHer = new JPanel(new GridBagLayout());
    JPanel absenderliste = new JPanel(new GridBagLayout());
    
    mainPanel.add(introSuche, BorderLayout.PAGE_START);
    mainPanel.add(suchergebnisHinUndHerAbsenderliste, BorderLayout.CENTER);
    mainPanel.add(fussbereich, BorderLayout.PAGE_END);
    
    introSuche.add(intro);
    introSuche.add(suche);
    
    suchergebnisHinUndHerAbsenderliste.add(suchergebnis);
    suchergebnisHinUndHerAbsenderliste.add(hinUndHer);
    suchergebnisHinUndHerAbsenderliste.add(absenderliste);
    
    addUIElements(fensterDesc, "Intro", intro, 0, 1);
    addUIElements(fensterDesc, "Suche", suche, 1, 0);
    addUIElements(fensterDesc, "Suchergebnis", suchergebnis, 0, 1);
    addUIElements(fensterDesc, "HinUndHer", hinUndHer, 0, 1);
    addUIElements(fensterDesc, "Absenderliste", absenderliste, 0, 1);
    addUIElements(fensterDesc, "Fussbereich", fussbereich, 1, 0);
    
    Dataset dsToSelect = null;
    try{
      dsToSelect = dj.getSelectedDataset();
    }catch(DatasetNotFoundException x){}
    setListElements(palJList, dj.getLOS(), dsToSelect, false);
  
    updateButtonStates();
    
    myFrame.pack();
    
    /**
     * Beschränkung der Höhe erst nach pack() aufheben, damit Fenster nicht unnötig groß wird.
     */
    palJList.setFixedCellHeight(-1);
    resultsJList.setFixedCellHeight(-1);
    
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }
  
  /** Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu.
   *  compo muss ein GridBagLayout haben. stepx und stepy geben an um
   *  wieviel mit jedem UI Element die x und die y Koordinate der Zelle
   *  erhöht werden soll. Wirklich sinnvoll sind hier nur (0,1) und (1,0).
   */
  private void addUIElements(ConfigThingy conf, String key, JComponent compo, int stepx, int stepy)
  {
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
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
            
          boolean readonly = false;
          String id = "";
          try{ id = uiElementDesc.get("ID").toString(); }catch(NodeNotFoundException e){}
          try{ if (uiElementDesc.get("READONLY").toString().equals("true")) readonly = true; }catch(NodeNotFoundException e){}
          String type = uiElementDesc.get("TYPE").toString();
          
          if (type.equals("textfield"))
          {
            JTextField tf;
            if (id.equals("suchanfrage"))
              tf = query;
            else
              tf = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
            
            tf.setEditable(!readonly);
            gbcTextfield.gridx = x;
            gbcTextfield.gridy = y;
            compo.add(tf, gbcTextfield);
            
            String action = "";
            try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
            
            ActionListener actionL = getAction(action);
            if (actionL != null) tf.addActionListener(actionL);
          }
          else
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
            if (id.equals("suchergebnis"))
              list = resultsJList;
            else if (id.equals("pal")) 
              list = palJList;
            else
              list = new JList(new DefaultListModel());
            
            list.setVisibleRowCount(lines);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setLayoutOrientation(JList.VERTICAL);
            list.setPrototypeCellValue("Al-chman hemnal ulhillim el-WollMux(W-OLL-MUX-5.1)");
            
            list.addListSelectionListener(myListSelectionListener);
            
            String action = "";
            try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
            
            ActionListener actionL = getAction(action);
            if (actionL != null) list.addMouseListener(new MyActionMouseListener(list, actionL));
            
            JScrollPane scrollPane = new JScrollPane(list);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            
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
            if (actionL != null) 
              button.addActionListener(actionL);
            else
              button.setEnabled(false);
            
            if (action.equals("editEntry"))
            {
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            if (action.equals("removeFromPAL"))
            {
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            if (action.equals("addToPAL"))
            {
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            else if (action.equals("copyEntry"))
            {
              buttonsToGreyOutIfNothingSelected.add(button);
            }
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
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link ListElement} Objekte gewrappt.
   * data == null wird interpretiert als leere Liste. Wenn datasetToSelect != null ist,
   * so wird der entsprechende Datensatz in der Liste selektiert, wenn er darin vorhanden 
   * ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @param append Falls true, werden die Elemente an die Liste angehängt anstatt sie
   * zu ersetzen.
   */
  private void setListElements(JList list, QueryResults data, Dataset datasetToSelect, boolean append)
  {
    int selectedIndex = -1;
    ListElement[] elements;
    if (data == null)
      elements = new ListElement[]{};
    else
    {
      elements = new ListElement[data.size()];
      Iterator iter = data.iterator();
      int i = 0;
      while (iter.hasNext()) 
      {
        DJDataset ds = (DJDataset)iter.next();
        Icon icon = null;
        String mail = null; 
        try{
          mail = ds.get("Mail"); //liefert null, wenn nicht belegt.
        }catch(ColumnNotFoundException x){}
        
        if (mail == null) mail = "";
        
        if (WollMuxFiles.showCredits)
        {
          if (mail.equals("matthias.benkmann@muenchen.de"))
            icon = new ImageIcon(MB_URL);
          else if (mail.equals("christoph.lutz@muenchen.de"))
            icon = new ImageIcon(CL_URL);
        }
        
        elements[i++] = new ListElement(ds, icon);
      }
      Arrays.sort(elements, new Comparator<Object>()
          {
        public int compare(Object o1, Object o2)
        {
          return o1.toString().compareTo(o2.toString());
        }
          });
    }
    
    DefaultListModel listModel = (DefaultListModel)list.getModel();
    if (!append) listModel.clear();
    int oldSize = listModel.size();
    for (int i = 0; i < elements.length; ++i)
    {
      listModel.addElement(elements[i]);
      if (datasetToSelect != null && 
          elements[i].getDataset().getKey().equals(datasetToSelect.getKey()))
        selectedIndex = i;
    }
    
    if (selectedIndex >= 0) list.setSelectedIndex(selectedIndex + oldSize);
  }
  
  /**
   * wie {@link #setListElements(JList, QueryResults, Dataset, boolean)}, aber es wird kein Datensatz
   * selektiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @param append Falls true, werden die Elemente an die Liste angehängt anstatt sie
   * zu ersetzen. 
   */
  private void setListElements(JList list, QueryResults data, boolean append)
  {
    setListElements(list, data, null, append);
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
   * Wrapper um ein DJDataset zum Einfügen in eine JList.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ListElement
  {
    private String displayString;
    private DJDataset ds;
    private Icon icon;
    
    public ListElement(DJDataset ds, Icon icon)
    {
      displayString = getDisplayString(ds);
      this.ds = ds;
      this.icon = icon;
    }

    public String toString()
    {
      return displayString;
    }
    
    public Icon getIcon()
    {
      return icon;
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
      try{
        String wert2 = datensatz.get(spalte);
        if (wert2 != null)
          wert = wert2.replaceAll("%", "");
      } catch (ColumnNotFoundException e) { Logger.error(e);}
      str = str.substring(0, m.start()) + wert + str.substring(m.end());
      m = p.matcher(str);
    } while (m.find());
    return str;
  }

  /**
   * Aktiviert oder Deaktiviert die {@link #buttonsToGreyOutIfNothingSelected} gemäss der
   * Selektion oder nicht Selektion von Werten in den Listboxen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void updateButtonStates()
  {
    boolean enabled = false;
    try{
      enabled = (resultsJList.getSelectedIndex() >= 0)
                   || (palJList.getSelectedIndex() >= 0);
    }catch(NullPointerException x)
    {
      Logger.error("Listbox mit ID \"suchergebnisse\" oder \"pal\" fehlt");
    }
     
    Iterator<JButton> iter = buttonsToGreyOutIfNothingSelected.iterator();
    while (iter.hasNext()) iter.next().setEnabled(enabled);
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer
  {
    private static final long serialVersionUID = -540148680826568290L;

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
      try
      {
        ListElement ele = (ListElement) value;

        Icon icon = ele.getIcon();
        if (icon != null)
          value = icon;
        else
          value = ele.toString();
      }
      catch (ClassCastException x) {}
        
      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
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
      if (list != palJList && list != resultsJList) return;
      
      /*
       * Dafür sorgen, dass nie in beiden Listen ein Element selektiert ist.
       */
      JList otherlist = (list == palJList) ? resultsJList: palJList;
      if (list.getSelectedIndex() >= 0) otherlist.clearSelection();
      
      /*
       * Buttons ausgrauen, falls nichts selektiert, einschwarzen sonst.
       */
      updateButtonStates();
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
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf
   * wobei das gegebene actionCommand übergeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this,0,actionCommand));
  }
    
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addToPAL()
  {
    Object[] sel = resultsJList.getSelectedValues();
    addEntries: for (int i = 0; i < sel.length; ++i)
    {
      ListElement e = (ListElement)sel[i];
      DJDataset ds = e.getDataset();
      String eStr = getDisplayString(ds);
      ListModel model = palJList.getModel();
      for (int j = model.getSize() - 1; j >= 0; --j)
      {
        ListElement e2 = (ListElement)model.getElementAt(j);
        if (e2.toString().equals(eStr)) continue addEntries;
      }
      ds.copy();
    }

    listsHaveChanged();
  }

  /**
   * Aktualisiert die Werte in der PAL Listbox, löscht die Selektionen
   * in beiden Listboxen und passt den Ausgegraut-Status der Buttons
   * entsprechend an. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void listsHaveChanged()
  {
    setListElements(palJList, dj.getLOS(), false);
    palJList.clearSelection();
    resultsJList.clearSelection();
    updateButtonStates();
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeFromPAL()
  {
    Object[] sel = palJList.getSelectedValues();
    for (int i = 0; i < sel.length; ++i)
    {
      ListElement e = (ListElement)sel[i];
      e.getDataset().remove();
    }

    listsHaveChanged(); 
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editEntry()
  {
    ListElement e = (ListElement)palJList.getSelectedValue();
    DJDataset ds;
    if (e == null)
    {
      e = (ListElement)resultsJList.getSelectedValue();
      if (e == null) return;
      ds = e.getDataset().copy();
    }
    else ds = e.getDataset();
    
    editDataset(ds);
  }
  
  private void editDataset(DJDataset ds)
  {
    ActionListener del = new MyDialogEndListener(this, myConf, abConf, dj, dialogEndListener, null);
    dialogEndListener = null;
    abort();
    try
    {
      new DatensatzBearbeiten(abConf, ds, del);
    }
    catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }
  }
  
  /**
   * Erzeugt eine Kopue von orig und ändert ihre Rolle auf
   * "Kopie".
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyDJDataset(DJDataset orig)
  {
    DJDataset newDS = orig.copy();
    try
    {
      newDS.set("Rolle", "Kopie");
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyEntry()
  {
    Object[] sel = resultsJList.getSelectedValues();
    for (int i = 0; i < sel.length; ++i)
    {
      ListElement e = (ListElement)sel[i];
      copyDJDataset(e.getDataset());
    }
    
    sel = palJList.getSelectedValues();
    for (int i = 0; i < sel.length; ++i)
    {
      ListElement e = (ListElement)sel[i];
      copyDJDataset(e.getDataset());
    }

    listsHaveChanged();
  }
  
  /**
   * Implementiert die gleichnamige ACTION. Liefert den neuen Datensatz
   * zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private DJDataset newPALEntry()
  {
    DJDataset ds = dj.newDataset();
    try{
      ds.set("Vorname",DEFAULT_VORNAME);
      ds.set("Nachname",DEFAULT_NACHNAME);
      ds.set("Rolle", DEFAULT_ROLLE);
      ds.set("Anrede", DEFAULT_ANREDE);
    }catch(Exception x)
    {
      Logger.error(x);
    }
    listsHaveChanged();
    return ds;
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editNewPALEntry()
  {
    editDataset(newPALEntry());
  }
  
  /**
   * Implementiert die gleichnamige ACTION. Hier stecken die ganzen
   * komplexen Heuristiken drinnen zur Auswertung der Suchanfrage.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void search()
  {
    /* die möglichen Separatorzeichen zwischen Abteilung und Unterabteilung. */
    final String SEP_CHARS = "-/ _";
    
    String queryString = query.getText();
    
    if (queryString.equalsIgnoreCase("credits"))
    {
      credits();
      return;
    }
    
    /*
     * Kommata durch Space ersetzen (d.h. "Benkmann,Matthias" -> "Benkmann Matthias")
     * aber Merkflag setzen, dass ein Komma enthalten war.
     */
    boolean hasComma = queryString.indexOf(',') >= 0;
    queryString = queryString.replaceAll(","," ");
    
    /*
     * Suchstring zerlegen.
     */
    String[] queryArray = queryString.trim().split("\\p{Space}+");
    
    /*
     * Benutzerseitig wir nur ein einzelnes Sternchen am Ende eines Wortes 
     * akzeptiert. Deswegen entferne alle anderen Sternchen. 
     * Ein Punkt am Ende eines Wortes wird als Abkürzung interpretiert
     * und durch Sternchen ersetzt. Ausserdem
     * entferne leere Wörter und berechne neue Arraylänge. 
     */
    int count = queryArray.length;
    for (int i = 0; i < queryArray.length && queryArray[i] != null; ++i)
    {
      boolean suffixStar = queryArray[i].endsWith("*") || queryArray[i].endsWith(".");
      if (queryArray[i].endsWith(".")) 
        queryArray[i] = queryArray[i].substring(0,queryArray[i].length()-1);
      
      queryArray[i] = queryArray[i].replaceAll("\\*","");
      if (queryArray[i].length() == 0)
      {
        for (int j = i + 1; j < queryArray.length; ++j)
          queryArray[j-1] = queryArray[j];
        
        --count;
        --i;
        queryArray[queryArray.length - 1] = null;
      }
      else
      {
        if (suffixStar) queryArray[i] = queryArray[i] + "*";
      }
    }
    
    if (count == 0) return; //leerer Suchstring (bzw. nur einzelne Sternchen)
    
    /* 
     * Falls mehr als 2 Worte übergeben wurden und ein anderes als das letzte 
     * Wort auf Sternchen endet, so betrachte nur die ersten beiden Worte.
     */
    if (count > 2)
    {
      for (int i = 0; i < count - 1; ++i)
        if (queryArray[i].endsWith("*")) count = 2;
    }

    
    QueryResults results = null;
    try{
      if (count == 1)
      {
        String wort1 = queryArray[0];
        
        if (wort1.endsWith("*"))
        {
          /* 1 Wort
           * Sternchen am Ende
           *   Abteilung*
           *   *bteilung*
           *   Nachn*
           *   Vorn*
           *   Email*
           */
          do{
            results = dj.find("OrgaKurz", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("OrgaKurz", "*"+wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Nachname", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Vorname", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Mail", wort1);
            if (!results.isEmpty()) break;
          }while(false);
          
        }
        else
        {
          /* 1 Wort
           * kein Sternchen enthalten 
           *   Email
           *   Email@muenchen.de
           *   Nachn*
           *   Abteilung
           *     *eilung
           *   Vorname
           *   Vorn*
           */
          do{
            results = dj.find("Mail", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Mail", wort1 + "@muenchen.de");
            if (!results.isEmpty()) break;
            
            results = dj.find("Nachname", wort1+"*");
            if (!results.isEmpty()) break;
            
            results = dj.find("OrgaKurz", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("OrgaKurz", "*"+wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Vorname", wort1);
            if (!results.isEmpty()) break;
            
            results = dj.find("Vorname", wort1+"*");
            if (!results.isEmpty()) break;
          }while(false);
        }
      }
      else if (count == 2)
      {
        String wort1 = queryArray[0];
        String wort2 = queryArray[1];
        
        if (hasComma)
        {
          if (wort1.endsWith("*") || wort2.endsWith("*"))
          {
            /* 2 Worte mit Komma
             * Bei Sternchen in einem oder beiden Wörtern werden die Wörter direkt
             * als Suchanfrage verwendet in der Reihenfolge 
             *   Nachn[*] Vorn[*]
             *   Vorn[*] Nachn[*]
             */
            do{
              results = dj.find("Nachname", wort1, "Vorname", wort2);
              if (!results.isEmpty()) break;
              
              results = dj.find("Vorname", wort1, "Nachname", wort2);
              if (!results.isEmpty()) break;
            } while(false);
          }
          else
          {
            /* 2 Worte mit Komma
             * kein Sternchen
             *   Nachname Vorname 
             *   Nachname Vorn*
             *   Nachn* Vorname 
             *   Nachn* Vorn*
             */
            do
            {
              results = dj.find("Nachname", wort1, "Vorname", wort2);
              if (!results.isEmpty()) break;
              
              results = dj.find("Nachname", wort1, "Vorname", wort2+"*");
              if (!results.isEmpty()) break;
              
              results = dj.find("Nachname", wort1+"*", "Vorname", wort2);
              if (!results.isEmpty()) break;
              
              results = dj.find("Nachname", wort1+"*", "Vorname", wort2+"*");
              if (!results.isEmpty()) break;
            } while (false);
            
          }
        }
        else //if (!hasComma)
        {
          if (wort1.endsWith("*"))
          {
            /* 2 Worte, kein Komma
             * Bei Sternchen im ersten oder in beiden Wörtern werden die Wörter 
             * direkt als Suchanfrage verwendet in der Reihenfolge 
             *   Vorn[*] Nachn[*]
             *   Nachn[*] Vorn[*]
             */ 
            do{
              results = dj.find("Vorname", wort1, "Nachname", wort2);
              if (!results.isEmpty()) break;
              
              results = dj.find("Nachname", wort1, "Vorname", wort2);
              if (!results.isEmpty()) break;
            }while(false);
          }
          else if (wort2.endsWith("*"))
          {
            /* 2 Worte, kein Komma
             * Sternchen nur am Ende des 2.Wortes
             *   Abt-eil*
             *   Abt/eil*
             *   Abt<space>eil*
             *   Abt_eil*
             *   *bt-eil*
             *   *bt/eil*
             *   *bt<space>eil*
             *   *bt_eil*
             *   Vorname Nachn*
             *   Nachname Vorn*
             */
            out: do{
              for (int i = 0; i < SEP_CHARS.length(); ++i)
              {
                results = dj.find("OrgaKurz", wort1+SEP_CHARS.charAt(i)+wort2);
                if (!results.isEmpty()) break out;
              }
              
              for (int i = 0; i < SEP_CHARS.length(); ++i)
              {
                results = dj.find("OrgaKurz", "*"+wort1+SEP_CHARS.charAt(i)+wort2);
                if (!results.isEmpty()) break out;
              }
              
              results = dj.find("Vorname", wort1, "Nachname", wort2);
              if (!results.isEmpty()) break out;
              
              results = dj.find("Nachname", wort1, "Vorname", wort2);
              if (!results.isEmpty()) break out;
            }while(false);
          }
          else
          {
            /* 2 Worte, kein Komma
             * kein Sternchen
             *   Vorname Nachname
             *   Nachname Vorname
             *   Vorname Nachn*
             *   Nachname Vorn*
             *   Abt-eilung
             *   Abt/eilung
             *   Abt<space>eilung
             *   Abt_eilung
             *    *t-eilung
             *    *t/eilung
             *    *t<space>eilung
             *    *t_eilung
             *   Vorn* Nachn*
             *   Nachn* Vorn*
             */
            out: do{
              
              results = dj.find("Vorname", wort1, "Nachname", wort2);
              if (!results.isEmpty()) break out;
              
              results = dj.find("Nachname", wort1, "Vorname", wort2);
              if (!results.isEmpty()) break out;
              
              results = dj.find("Vorname", wort1, "Nachname", wort2+"*");
              if (!results.isEmpty()) break out;
              
              results = dj.find("Nachname", wort1, "Vorname", wort2+"*");
              if (!results.isEmpty()) break out;
              
              for (int i = 0; i < SEP_CHARS.length(); ++i)
              {
                results = dj.find("OrgaKurz", wort1+SEP_CHARS.charAt(i)+wort2);
                if (!results.isEmpty()) break out;
              }
              
              for (int i = 0; i < SEP_CHARS.length(); ++i)
              {
                results = dj.find("OrgaKurz", "*"+wort1+SEP_CHARS.charAt(i)+wort2);
                if (!results.isEmpty()) break out;
              }
              
              results = dj.find("Vorname", wort1+"*", "Nachname", wort2+"*");
              if (!results.isEmpty()) break out;
              
              results = dj.find("Nachname", wort1+"*", "Vorname", wort2+"*");
              if (!results.isEmpty()) break out;
              
            } while(false);
          }
        }
      }
      else if (count > 2)
      {
        /* Mehr als 2 Worte, aber höchstens das letzte Wort endet auf "*",
         * da der Fall, dass ein anderes auf "*" endet schon ganz am
         * Anfang abgefangen und auf den Fall count == 2 reduziert wurde.
         * 
         * Ganzer String wird wieder zusammengebaut und als Abteilung
         * interpretiert
         * kein Sternchen
         *   Abteilung       (z.B. KVR-II/213 Team 1)
         *   *bteilung
         *   
         * letztes Wort endet auf Sternchen
         *   *bteilung*
         */
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < count; ++i)
          buf.append((i > 0?" ":"") + queryArray[i]);
        
        results = dj.find("OrgaKurz",buf.toString());
      }
    }catch(TimeoutException x) { Logger.error(x);}

      // kann mit results == null umgehen
    setListElements(resultsJList, results, false); 
    updateButtonStates();
  }
  
  private void credits()
  {
    WollMuxFiles.showCredits = true;
    QueryResults results = null;
    try{
      results = dj.find("Mail", "matthias.benkmann@muenchen.de");
      setListElements(resultsJList, results, false);
      results = dj.find("Mail", "christoph.lutz@muenchen.de");
      setListElements(resultsJList, results, true);
    }catch(TimeoutException x) { Logger.error(x);}
    
    updateButtonStates();
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
    else if (action.equals("search"))
    {
      return actionListener_search;
    }
    else if (action.equals("addToPAL"))
    {
      return actionListener_addToPAL;
    }
    else if (action.equals("removeFromPAL"))
    {
      return actionListener_removeFromPAL;
    }
    else if (action.equals("editEntry"))
    {
      return actionListener_editEntry;
    }
    else if (action.equals("copyEntry"))
    {
      return actionListener_copyEntry;
    }
    else if (action.equals("newPALEntry"))
    {
      return actionListener_newPALEntry;
    }
    else if (action.equals("editNewPALEntry"))
    {
      return actionListener_editNewPALEntry;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error("Ununterstützte ACTION: "+action);
    
    return null;
  }
  
  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;
    private ConfigThingy abConf;
    private DatasourceJoiner dj;
    private ActionListener dialogEndListener;
    private String actionCommand;
    private PersoenlicheAbsenderlisteVerwalten mySource;
    
    /**
     * Falls actionPerformed() mit getActionCommand().equals("back")
     * aufgerufen wird, wird ein neuer  AbsenderAuswaehlen Dialog mit
     * den übergebenen Parametern erzeugt. Ansonsten wird
     * der dialogEndListener mit actionCommand aufgerufen. Falls actionCommand
     * null ist wird das actioncommand und die source des ActionEvents weitergereicht,
     * der actionPerformed() übergeben wird, ansonsten werden die übergebenen Werte
     * für actionCommand und source verwendet.
     */
    public MyDialogEndListener(PersoenlicheAbsenderlisteVerwalten source, ConfigThingy conf, ConfigThingy abConf, DatasourceJoiner dj, ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
      this.mySource = source;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("back"))
        try{
          new PersoenlicheAbsenderlisteVerwalten(conf, abConf, dj, dialogEndListener);
        }catch(Exception x) {Logger.error(x);}
      else
      {
        Object source = mySource;
        if (actionCommand == null) 
        {
          actionCommand = e.getActionCommand();
          source = e.getSource();
        }
        if (dialogEndListener != null)
          dialogEndListener.actionPerformed(new ActionEvent(source,0,actionCommand));
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
    private ConfigThingy abConf;
    
    public RunTest(ConfigThingy conf, ConfigThingy abConf, DatasourceJoiner dj)
    {
      this.dj = dj;
      this.conf = conf;
      this.abConf = abConf;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      try{
        if (e.getActionCommand().equals("abort")) System.exit(0);
      }catch(Exception x){}
      try{
        new PersoenlicheAbsenderlisteVerwalten(conf, abConf, dj, this);
      } catch(ConfigurationErrorException x)
      {
        Logger.error(x);
      }
    }
  }
  
  public static void main(String[] args) throws Exception
  {
    String confFile = "testdata/PAL.conf";
    String abConfFile = "testdata/AbsenderdatenBearbeiten.conf";
    ConfigThingy conf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),confFile));
    ConfigThingy abConf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),abConfFile));
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    RunTest test = new RunTest(conf.get("PersoenlicheAbsenderliste"), abConf.get("AbsenderdatenBearbeiten"), dj);
    test.actionPerformed(null);
    Thread.sleep(600000);
    System.exit(0);
  }

}

