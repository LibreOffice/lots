/*
* Dateiname: PersoenlicheAbsenderlisteVerwalten.java
* Projekt  : WollMux
* Funktion : Implementiert den Hinzufügen/Entfernen Dialog des BKS
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 17.10.2005 | BNK | Erstellung
* 18.10.2005 | BNK | PAL Verwalten GUI großteils implementiert (aber funktionslos)
* 19.10.2005 | BNK | Suche implementiert
* 20.10.2005 | BNK | Suche getestet
* -------------------------------------------------------------------
*
* TODO Buttons ausgrauen, wenn kein Listenelement markiert
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.TestDatasourceJoiner;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen 
 * Dialogbeschreibung einen (mehrseitigen) Dialog zur Bearbeitung eines
 * {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}s.
 * <b>ACHTUNG:</b> Die public-Funktionen dieser Klasse dürfen NICHT aus dem
 * Event-Dispatching Thread heraus aufgerufen werden. Die private-Funktionen
 * dagegen dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PersoenlicheAbsenderlisteVerwalten
{
  
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
  
  private List buttonsToGreyOutIfNothingSelected = new Vector();
  
  private JList resultsJList;
  private JList palJList;
  private JTextField query;
  
  /**
   * Erzeugt einen neuen Dialog, der allerdings zu Beginn nicht sichtbar ist.
   * @param conf das ConfigThingy, das den Dialog beschreibt (der Vater des
   *        "Fenster"-Knotens.
   * @param datensatz der Datensatz, der mit dem Dialog bearbeitet werden soll.
   * @throws ConfigurationErrorException im Falle eines schwerwiegenden
   *         Konfigurationsfehlers, der es dem Dialog unmöglich macht,
   *         zu funktionieren (z.B. dass der "Fenster" Schlüssel fehlt.
   */
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, DatasourceJoiner dj) throws ConfigurationErrorException
  {
    this.dj = dj;
    
    final ConfigThingy fensterDesc = conf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            try{createGUI(fensterDesc.getLastChild());}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  private void createGUI(ConfigThingy fensterDesc) throws NodeNotFoundException
  {
    //use system LAF for window decorations
    try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception x){};
    JFrame.setDefaultLookAndFeelDecorated(true);
    
    resultsJList = new JList(new DefaultListModel());
    palJList = new JList(new DefaultListModel());
    query = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
    
    fensterDesc = fensterDesc.get("Verwalten");
    String title = "TITLE fehlt für Fenster PersoenlicheAbsenderListeVerwalten/Verwalten";
    try{
      title = fensterDesc.get("TITLE").toString();
    } catch(Exception x){};
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
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
    
    setListElements(palJList, dj.getLOS());
  
    updateButtonStates();
    
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
  }
  
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
            
            if (action.equals("search"))
            {
              tf.addActionListener(actionListener_search);
            }
            else
              Logger.error("Ununterstützte ACTION: "+action);
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
            list.setPrototypeCellValue("Matthias S. Benkmann ist euer Gott (W-OLL-MUX-5.1)");
            
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
              
            if (action.equals("abort"))
            {
              button.addActionListener(actionListener_abort);
            }
            else if (action.equals("search"))
            {
              button.addActionListener(actionListener_search);
            }
            else if (action.equals("addToPAL"))
            {
              button.addActionListener(actionListener_addToPAL);
            }
            else if (action.equals("removeFromPAL"))
            {
              button.addActionListener(actionListener_removeFromPAL);
            }
            else if (action.equals("editEntry"))
            {
              button.addActionListener(actionListener_editEntry);
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            else if (action.equals("copyEntry"))
            {
              button.addActionListener(actionListener_copyEntry);
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            else if (action.equals("newPALEntry"))
            {
              button.addActionListener(actionListener_newPALEntry);
              buttonsToGreyOutIfNothingSelected.add(button);
            }
            else if (action.equals(""))
            {
              button.setEnabled(false);
            }
            else
              Logger.error("Ununterstützte ACTION: "+action);
          }
          else
          {
            Logger.error("Unbekannter TYPE für User Interface Element: "+type);
          }
        } catch(NodeNotFoundException e) {Logger.error(e);}
      }
    }
  }
  
  private void setListElements(JList list, QueryResults data)
  {
    DefaultListModel listModel = (DefaultListModel)list.getModel();
    listModel.clear();
    Iterator iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      listModel.addElement(substituteVars(displayTemplate, ds));
    }
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
     
    Iterator iter = buttonsToGreyOutIfNothingSelected.iterator();
    while (iter.hasNext()) ((JButton)iter.next()).setEnabled(enabled);
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort(){myFrame.dispose();}
  
  private void search()
  {
    /* die möglichen Separatorzeichen zwischen Abteilung und Unterabteilung. */
    final String SEP_CHARS = "-/ _";
    
    String queryString = query.getText();
    
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
         *   Nachname
         *   Email
         *   Email@muenchen.de
         *   Nachn*
         *   Abteilung
         *     *eilung
         *   Vorname
         *   Vorn*
         */
        do{
          results = dj.find("Nachname", wort1);
          if (!results.isEmpty()) break;
          
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

    setListElements(resultsJList, results);
    updateButtonStates();
  }
  
  private void addToPAL(){myFrame.dispose();}
  
  private void removeFromPAL(){myFrame.dispose();}
  
  private void editEntry(){myFrame.dispose();}
  
  private void copyEntry(){myFrame.dispose();}
  
  private void newPALEntry(){myFrame.dispose();}
  
  
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
    public void windowClosing(WindowEvent e) { abort(); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) {}
  }

  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren
   * Aufrufe von Methoden des Dialogs erfolgen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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

  /**
   * Macht den Dialog sichtbar.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show()
  {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showEDT();
      }
    });
  }
  
  /**
   * private Version von show(), die im Event-Dispatching Thread laufen muss.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showEDT()
  {
    myFrame.setVisible(true);
  }
  
  public static void main(String[] args) throws Exception
  {
    String confFile = "testdata/PAL.conf";
    ConfigThingy conf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),confFile));
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    PersoenlicheAbsenderlisteVerwalten dialog = new PersoenlicheAbsenderlisteVerwalten(conf.get("PersoenlicheAbsenderliste"), dj);
    dialog.show();
    Thread.sleep(600000);
    dialog.dispose();
  }
}

