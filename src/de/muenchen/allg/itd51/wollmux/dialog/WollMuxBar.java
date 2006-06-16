/*
* Dateiname: WollMuxBar.java
* Projekt  : WollMux
* Funktion : Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 02.01.2006 | BNK | Erstellung
* 03.01.2006 | BNK | Menüs unterstützt
* 10.01.2006 | BNK | Icon und Config-File pfadunabhängig über Classloader
*                  | switches --minimize, --topbar, --normalwindow
* 06.02.2006 | BNK | Menüleiste hinzugefügt
* 14.02.2006 | BNK | Minimieren rückgängig machen bei Aktivierung der Leiste.
* 15.02.2006 | BNK | ordentliches Abort auch bei schliessen des Icon-Fensters
* 19.04.2006 | BNK | [R1342][R1398]große Aufräumaktion, Umstellung auf WollMuxBarEventHandler
* 20.04.2006 | BNK | [R1207][R1205]Icon der WollMuxBar konfigurierbar, Anzeigemodus konfigurierbar
* 21.04.2006 | BNK | Umgestellt auf UIElementFactory
*                  | Bitte Warten... in der Senderbox solange noch keine Verbindung besteht
*                  | Wenn ein Menü mehrfach verwendet wird, so wird jetzt jedes
*                  | Mal ein neues erzeugt, um Probleme zu vermeiden, die auftreten
*                  | könnten, wenn das selbe JMenu an mehreren Stellen in der
*                  | Komponentenhierarchie erscheint.
* 24.04.2006 | BNK | kleinere Aufräumarbeiten. Code Review.
* 24.04.2006 | BNK | [R1390]Popup-Fenster, wenn Verbindung zu OOo WollMux nicht hergestellt
*                  | werden konnte.
* 24.04.2006 | BNK | [R1460]Popup-Fenster, wenn WollMux nicht konfiguriert.
* 02.05.2006 | BNK | [R1202 Teil 1] Fensterposition und Größe von WollMuxBar konfigurierbar
* 29.05.2006 | BNK | in initFactories() Label Typen explizit genullt.
*                  | Umstellung auf UIElementFactory.Context
* 16.06.2006 | BNK | Fokusverlust wird simuliert jedes Mal wenn der Benutzer was
*                  | drückt, damit sich die WollMuxBar dann minimiert. 
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar
{
  /**
   * Titel des WollMuxBar-Fensters (falls nicht anders konfiguriert).
   */
  private static final String DEFAULT_TITLE = "Vorlagen und Formulare";
  
  /**
   * Spezialeintrag in der Absenderliste, 
   * der genau dann vorhanden ist, wenn die Absenderliste leer ist.
   */
  private static final String LEERE_LISTE = "<kein Absender vorhanden>";
  
  /**
   * Icon in das sich die WollMuxBar verwandelt (falls nicht anders konfiguriert).
   */
  private final URL ICON_URL = this.getClass().getClassLoader().getResource("data/wollmux_klein.jpg");
  
  /**
   * Wenn die WollMuxBar den Fokus verliert, verwandelt sie sich in ein Icon.
   */
  private static final int BECOME_ICON_MODE = 0;
  /**
   * Wenn die WollMuxBar den Fokus verliert, minimiert sich das Fenster.
   */
  private static final int MINIMIZE_TO_TASKBAR_MODE = 1;
  /**
   * Die WollMuxBar verhält sich wie ein normales Fenster. 
   */
  private static final int NORMAL_WINDOW_MODE = 2;
  /**
   * Die WollMuxBar ist immer im Vordergrund.
   */
  private static final int ALWAYS_ON_TOP_WINDOW_MODE = 3;
  
  /**
   * Der Anzeigemodus für die WollMuxBar (z,B, {@link BECOME_ICON_MODE})
   */
  private int windowMode;
  
  /**
   * Regelt den Wechsel zwischen den verschiedenen Anzeigemodi bei Fokusverlust etc.
   */
  private WindowTransformer myWindowTransformer;

  /**
   * Dient der thread-safen Kommunikation mit dem entfernten WollMux.
   */
  private WollMuxBarEventHandler eventHandler;
  
  /**
   * Der Rahmen, der die Steuerelemente enthält.
   */
  private JFrame myFrame;
  
  /**
   * Das Fenster, das das Icon enthält, in das sich die Leiste verwandeln kann.
   */
  private JFrame logoFrame;
  
  /**
   * Das Panel für den Inhalt des Fensters der WollMuxBar (myFrame).
   */
  private JPanel contentPanel;
  
  /**
   * Das Panel für das Icon-Fenster (logoFrame).
   */
  private JPanel logoPanel;

  /**
   * Mappt einen Menü-Namen auf ein entsprechendes JPopupMenu.
   */
  private Map mapMenuNameToJPopupMenu = new HashMap();
  
  /**
   * Die UIElementFactory, die verwendet wird, um das GUI aufzubauen.
   */
  private UIElementFactory uiElementFactory;
  
  /**
   * Kontext für GUI-Elemente in JPanels (für Übergabe an die uiElementFactory).
   */
  private UIElementFactory.Context panelContext;
  
  /**
   * Kontext für GUI-Elemente in JMenus und JPopupMenus (für Übergabe an die uiElementFactory).
   */
  private UIElementFactory.Context menuContext;
  
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
   * Die Fehlermeldung die in einem Popup-Fenster gebracht wird, wenn keine
   * Verbindung zum WollMux in OOo hergestellt werden konnte.
   */
  private static final String CONNECTION_FAILED_MESSAGE = 
  "Es konnte keine Verbindung zur WollMux-Komponente von OpenOffice hergestellt werden.\n"+
  "Eine mögliche Ursache ist ein fehlerhaft installiertes OpenOffice.\n"+
  "Eine weitere mögliche Ursache ist, dass WollMux.uno.pkg nicht oder fehlerhaft "+
  "installiert wurde.";

  private static final String WOLLMUX_CONFIG_ERROR_MESSAGE = 
  "Ihr WollMux ist nicht richtig konfiguriert.\n"+
  "Bitte wenden Sie sich an Ihre Systembetreuerin oder Ihren Systembetreuer.";
  
  /**
   * ActionListener für Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
       { public void actionPerformed(ActionEvent e){ abort(); } };
    
  /**
   * ActionListener für Buttons, denen ein Menü zugeordnet ist. 
   */
  private ActionListener actionListener_openMenu = new ActionListener()
        { public void actionPerformed(ActionEvent e){ openMenu(e); } };
  
  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
  
  /**
   * ItemListener bei Elementänderungen der senderboxes: 
   */
  private ItemListener itemListener = new ItemListener() 
    { public void itemStateChanged(ItemEvent e) { senderBoxItemChanged(e); } };

  /**
   * Alle senderboxes (JComboBoxes) der Leiste.
   */
  private List senderboxes = new Vector();
  
  /**
   * Erzeugt eine neue WollMuxBar.
   * @param winMode Anzeigemodus, z.B. {@link #BECOME_ICON_MODE}
   * @param conf der Inhalt der wollmux.conf 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(int winMode, final ConfigThingy conf)
  {
    windowMode = winMode;

    eventHandler = new WollMuxBarEventHandler(this);
    
    /*
     * Die GUI wird im Event-Dispatching Thread erzeugt wg. Thread-Safety.
     * Auch eventHandler.connectWithWollMux() wird im EDT ausgeführt, um
     * sicherzustellen, dass kein updateSenderBoxes() ausgeführt wird, bevor
     * nicht die Senderboxen erzeugt wurden.
     */
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{
              /*
               * Dieser Befehl steht VOR dem Aufruf von createGUI(), damit
               * OOo schon gestartet wird, während wir noch mit GUI aufbauen
               * beschäftigt sind. Es ist trotztdem sichergestellt, dass
               * updateSenderboxes() nicht vor der Beendigung von createGUI()
               * aufgerufen werden kann, weil updateSenderboxes() durch den
               * WollMuxBarEventHandler ebenfalls mit invokeLater() in den EDT
               * geschickt wird und dort erst zum Zug kommen kann, wenn diese
               * run() Methode beendet ist. 
               */
              eventHandler.connectWithWollMux();
              
              createGUI(conf);
            }catch(Exception x)
            {
              Logger.error(x);
            };
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }

  private void createGUI(ConfigThingy conf)
  {
    Common.setLookAndFeel();
    
    initFactories();
   
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    String title = DEFAULT_TITLE;
    try{title = conf.get("Fenster").get("WollMuxBar").get("TITLE").toString();}catch(Exception x) {}
    
    int myFrame_x = Integer.MIN_VALUE;
    try{myFrame_x = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("X").toString());}catch(Exception x) {}
    
    int myFrame_y = Integer.MIN_VALUE;
    try{myFrame_y = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("Y").toString());}catch(Exception x) {}
    
    int myFrame_width = 0;
    try{myFrame_width = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("WIDTH").toString());}catch(Exception x) {}
    
    int myFrame_height = 0;
    try{myFrame_height = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("HEIGHT").toString());}catch(Exception x) {}
    
    int icon_x = screenSize.width - 96;
    try{icon_x = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("ICONX").toString());}catch(Exception x) {}
    
    int icon_y = screenSize.height - 128;
    try{icon_y = Integer.parseInt(conf.get("Fenster").get("WollMuxBar").get("ICONY").toString());}catch(Exception x) {}
    
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    //Ein WindowListener, der auf den JFrame registriert wird, damit als
    //Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
    myFrame.addWindowListener(new MyWindowListener());
    
    contentPanel = new JPanel();
    contentPanel.setLayout(new GridBagLayout());
    myFrame.getContentPane().add(contentPanel);
    
    try{
      ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
      if(bkl.count() > 0) {
        addUIElements(conf.query("Menues"),bkl.getLastChild(), contentPanel, 1, 0, "panel");
      }
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }
    
    JMenuBar mbar = new JMenuBar();
    try{
      ConfigThingy menubar = conf.query("Menueleiste");
      if(menubar.count() > 0) {
        addUIElements(conf.query("Menues"),menubar.getLastChild(), mbar, 1, 0, "menu");
      }
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }
    myFrame.setJMenuBar(mbar);
    
    logoFrame = new JFrame(title);
    logoFrame.setUndecorated(true);
    logoFrame.setAlwaysOnTop(true);
    //wie bei myFrame soll abort ausgeführt werden, nicht die default Aktion
    logoFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    logoFrame.addWindowListener(new MyWindowListener());
    
    URL iconUrl = ICON_URL;
    try{
      String urlStr = conf.get("Fenster").get("WollMuxBar").get("ICON").toString();
      URL iconUrl2 = new URL(WollMuxFiles.getDEFAULT_CONTEXT(), urlStr);
      iconUrl2.openStream().close(); //testen ob URL erreichbar ist.
      iconUrl = iconUrl2;
    }catch(Exception x) {}
    
    JLabel logo = new JLabel(new ImageIcon(iconUrl));
    logo.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel = new JPanel(new GridLayout(1,1));
    logoPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel.add(logo);
    logoFrame.getContentPane().add(logoPanel);
    
    myWindowTransformer = new WindowTransformer();
    myFrame.addWindowFocusListener(myWindowTransformer);
    logo.addMouseListener(myWindowTransformer);
    logo.addMouseMotionListener(myWindowTransformer);
    
      //in der Hoffnung, dass es verhindert, dass anderen Fenstern der Fokus gestohlen wird
    logoFrame.setFocusableWindowState(false);
    
    logoFrame.pack();
    logoFrame.setLocation(icon_x,icon_y);
    logoFrame.setResizable(false);

    if (windowMode != NORMAL_WINDOW_MODE) myFrame.setAlwaysOnTop(true);
    
    myFrame.pack();
    Dimension frameSize = myFrame.getSize();
    if (myFrame_width > 0) frameSize.width = myFrame_width;
    if (myFrame_height > 0) frameSize.height = myFrame_height;
    myFrame.setSize(frameSize);
    if (myFrame_x == Integer.MIN_VALUE) myFrame_x = screenSize.width/2 - frameSize.width/2; 
    if (myFrame_y == Integer.MIN_VALUE) myFrame_y = (int)(0.8*(screenSize.height/2 - frameSize.height/2));
    myFrame.setLocation(myFrame_x,myFrame_y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Fügt der Komponente compo UI Elemente hinzu, eines für jedes Kind von 
   * elementParent.
   * 
   * @param menuConf die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein,
   *        deren Kinder Menübeschreibungen sind für die Menüs, 
   *        die als UI Elemente verwendet werden.
   * @param elementParent
   * @param context kann die Werte "menu" oder "panel" haben und gibt an, um was
   *        es sich bei compo handelt. Abhängig vom context werden manche 
   *        UI Elemente anders interpretiert, z.B. werden "button" Elemente im
   *        context "menu" zu JMenuItems.        
   * @param compo die Komponente zu der die UI Elemente hinzugefügt werden sollen.
   *        Falls context nicht "menu" ist, muss compo ein GridBagLayout haben.
   * @param stepx stepx und stepy geben an, um wieviel mit jedem UI Element die x 
   *        und die y Koordinate innerhalb des GridBagLayouts erhöht werden sollen.
   *        Sinnvoll sind hier normalerweise nur (0,1) und (1,0).
   * @param stepy siehe stepx
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addUIElements(ConfigThingy menuConf, ConfigThingy elementParent, 
      JComponent compo, int stepx, int stepy, String context)
  {
    addUIElementsChecked(new HashSet(), menuConf, elementParent, compo, stepx, stepy, context);
  }
  
  /**
   * Wie addUIElements, aber reicht den Parameter alreadySeen an parseMenu weiter,
   * um sich gegenseitig enthaltende Menüs zu erkennen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void addUIElementsChecked(Set alreadySeen, ConfigThingy menuConf, ConfigThingy elementParent, 
      JComponent compo, int stepx, int stepy, String context)
  {
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    //GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcMenuButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcSenderbox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,           new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      
    int y = -stepy;
    int x = -stepx; 
      
    UIElementFactory.Context contextMap = context.equals("menu") ? menuContext : panelContext;
    
    Iterator piter = elementParent.iterator();
    while (piter.hasNext())
    {
      ConfigThingy uiElementDesc = (ConfigThingy)piter.next();
      y += stepy;
      x += stepx;
      
      try{
        String type;
        try{
          type = uiElementDesc.get("TYPE").toString();
        }
        catch(NodeNotFoundException e)
        {
          Logger.error("Ein User Interface Element ohne TYPE wurde entdeckt");
          continue;
        }
        
        if (type.equals("senderbox"))
        {
          if (context.equals("menu")) 
          {
            Logger.error("Elemente vom Typ \"senderbox\" können nicht in Menüs eingefügt werden!");
            continue;
          }
          
          int lines = 10;
          try{ lines = Integer.parseInt(uiElementDesc.get("LINES").toString()); } catch(Exception e){}
          
          JComboBox senderbox = new JComboBox();
          
          senderbox.setMaximumRowCount(lines);
          senderbox.setPrototypeDisplayValue("Matthias B. ist euer Gott (W-OLL-MUX-5.1)");
          senderbox.setEditable(false);
          
          senderbox.addItem("Bitte warten...");
          senderboxes.add(senderbox);
          
          gbcSenderbox.gridx = x;
          gbcSenderbox.gridy = y;
          compo.add(senderbox, gbcSenderbox);
        }
        else if (type.equals("menu"))
        {
          String label = "LABEL FEHLT ODER FEHLERHAFT!";
          try{ label = uiElementDesc.get("LABEL").toString(); } catch(Exception e){}
          
          char hotkey = 0;
          try{
            hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
          }catch(Exception e){}
          
          String menuName = "";
          try{
            menuName = uiElementDesc.get("MENU").toString();
          }catch(NodeNotFoundException e){}
          
          AbstractButton button;
          if (context.equals("menu"))
          {
            button = (AbstractButton)parseMenu(alreadySeen, null, menuConf, menuName, new JMenu(label));
            if (button == null)
              button = new JMenu(label);
          }
          else
          { 
            parseMenu(alreadySeen, mapMenuNameToJPopupMenu, menuConf, menuName, new JPopupMenu());
            button = new JButton(label);
            button.addActionListener(actionListener_openMenu) ;
            button.setActionCommand(menuName);
          }
          
          button.setMnemonic(hotkey);
          
          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcMenuButton);
        }
        else
        {
          UIElement uiElement = uiElementFactory.createUIElement(contextMap, uiElementDesc);
          GridBagConstraints gbc = (GridBagConstraints)uiElement.getLayoutConstraints();
          gbc.gridx = x;
          gbc.gridy = y;
          if (context.equals("menu"))
            compo.add(uiElement.getComponent());
          else
            compo.add(uiElement.getComponent(), gbc);
        }
      }
      catch(ConfigurationErrorException e) {Logger.error(e);}
    }
  }
  
  /**
   * Parst eine Menübeschreibung und erzeugt ein entsprechendes Menü.
   * @param menu das JMenu oder JPopupMenu zu dem die UI Elemente hinzugefügt 
   *        werden sollen.
   * @param menuConf die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein,
   *        deren Kinder Menübeschreibungen sind. 
   * @param menuName identifiziert das Menü aus menuConf, das geparst wird. Gibt es
   *        mehrere, so wird das letzte verwendet.
   * @param mapMenuNameToMenu falls nicht-null, so wird falls bereits ein Eintrag
   *                          menuName enthalten ist, dieser zurückgeliefert, 
   *                          ansonsten wird ein Mapping von menuName auf menu
   *                          hinzugefügt.
   *                          Falls null, so wird immer ein neues Menü erzeugt,
   *                          außer das menuName ist in alreadySeen, dann gibt
   *                          es eine Fehlermeldung.
   * @param alreadySeen falls menuName hier enthalten ist und mapMenuNameToMenu==null
   *                    dann wird eine Fehlermeldung ausgegeben und null zurückgeliefert.
   *                          
   * @return menu, falls das Menü erfolgreich aufgebaut werden konnte, null, wenn 
   *         das Menü nicht in menuConf definiert ist oder wenn es in alreadySeen
   *         ist und mapMenuNameToMenu == null.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JComponent parseMenu(Set alreadySeen, Map mapMenuNameToMenu, ConfigThingy menuConf, 
      String menuName, JComponent menu)
  {
    if (mapMenuNameToMenu != null && mapMenuNameToMenu.containsKey(menuName)) 
      return (JComponent)mapMenuNameToMenu.get(menuName);
    
    if (mapMenuNameToMenu == null && alreadySeen.contains(menuName))
    {
      Logger.error("Menü \""+menuName+"\" ist an einer Endlosschleife sich gegenseitig enthaltender Menüs beteiligt");
      return null;
    }

    ConfigThingy conf;
    try
    {
      conf = menuConf.query(menuName).getLastChild().get("Elemente");
    }
    catch (Exception x)
    {
      Logger.error("Menü \"" + menuName + "\" nicht definiert oder enthält keinen Abschnitt \"Elemente()\"");
      return null;
    }
    
    /*
     * Zur Vermeidung von Endlosschleifen müssen die folgenden BEIDEN Statements 
     * vor dem Aufruf von addUIElementsChecked stehen.
     */
    alreadySeen.add(menuName);
    if (mapMenuNameToMenu != null) mapMenuNameToMenu.put(menuName, menu);
    
    addUIElementsChecked(alreadySeen, menuConf, conf, menu, 0, 1, "menu");
    alreadySeen.remove(menuName);
    return menu;
  }
  
  /**
   * Initialisiert uiElementFactory.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void initFactories()
  {
    Map mapTypeToLayoutConstraints = new HashMap();
    Map mapTypeToLabelType = new HashMap();
    Map mapTypeToLabelLayoutConstraints = new HashMap();

    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    GridBagConstraints gbcCombobox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,           new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel =     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcButton    = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
    GridBagConstraints gbcVsep      = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,       new Insets(0,TF_BORDER,0,TF_BORDER),0,0);
    GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    
    
    mapTypeToLayoutConstraints.put("default", gbcButton);
    mapTypeToLabelType.put("default", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("default", null);
    
    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("combobox", null);
    
    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-glue", null);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-glue", null);
    
    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("label", null);
    
    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("button", null);
    
    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-separator", null);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-separator", null);

    UIElementEventHandler myUIElementEventHandler = new MyUIElementEventHandler();
    
    panelContext = new UIElementFactory.Context();
    panelContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    panelContext.mapTypeToLabelType = mapTypeToLabelType;
    panelContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    panelContext.uiElementEventHandler = myUIElementEventHandler;
    panelContext.mapTypeToType = new HashMap();
    panelContext.mapTypeToType.put("separator","v-separator");
    panelContext.mapTypeToType.put("glue","h-glue");
    
    menuContext = new UIElementFactory.Context();
    menuContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    menuContext.mapTypeToLabelType = mapTypeToLabelType;
    menuContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    menuContext.uiElementEventHandler = myUIElementEventHandler;
    menuContext.mapTypeToType = new HashMap();
    menuContext.mapTypeToType.put("separator","h-separator");
    menuContext.mapTypeToType.put("glue","v-glue");
    menuContext.mapTypeToType.put("button", "menuitem");
    
    Set supportedActions = new HashSet();
    supportedActions.add("openTemplate");
    supportedActions.add("absenderAuswaehlen");
    supportedActions.add("openDocument");
    supportedActions.add("abort");
    
    panelContext.supportedActions = supportedActions;
    menuContext.supportedActions = supportedActions;
    
    uiElementFactory = new UIElementFactory();
  }
  
  /**
   * Behandelt die Events der Eingabeelemente, die über die uiElementFactory 
   * erzeugt wurden (also fast alle).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyUIElementEventHandler implements UIElementEventHandler
  {
    public void processUiElementEvent(UIElement source, String eventType, Object[] args)
    {
      if (!eventType.equals("action")) return;
      
      String action = args[0].toString();
      if (action.equals("absenderAuswaehlen"))
      {
        myWindowTransformer.windowLostFocus(null);
        eventHandler.handleWollMuxUrl(WollMux.cmdAbsenderAuswaehlen,"");
      }
      else if (action.equals("openDocument"))
      {
        myWindowTransformer.windowLostFocus(null);
        eventHandler.handleWollMuxUrl(WollMux.cmdOpenDocument, args[1].toString());
      }
      else if (action.equals("openTemplate"))
      {
        myWindowTransformer.windowLostFocus(null);
        eventHandler.handleWollMuxUrl(WollMux.cmdOpenTemplate, args[1].toString());
      }
      else if (action.equals("abort"))
      {
        abort();
      }
    }
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    eventHandler.handleTerminate();
    myFrame.dispose();
    logoFrame.dispose();
    eventHandler.waitForThreadTermination();

    System.exit(0);
  }
  

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Menü zugeordnet
   * ist und lässt dann das entsprechende Menü aus mapMenuNameToJPopupMenu 
   * erscheinen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void openMenu(ActionEvent e)
  {
    String menuName = e.getActionCommand();
    JComponent compo;
    try{
      compo = (JComponent)e.getSource();
    }catch(Exception x)
    {
      Logger.error(x);
      return;
    }
    
    JPopupMenu menu = (JPopupMenu)mapMenuNameToJPopupMenu.get(menuName);
    if (menu == null) return;
    
    menu.show(compo, 0, compo.getHeight());
  }

  /**
   * Diese Methode wird aufgerufen, wenn in der Senderbox ein anderes Element
   * ausgewählt wurde und setzt daraufhin den aktuellen Absender im
   * entfernten WollMux neu.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   * TESTED 
   */
  private void senderBoxItemChanged(ItemEvent e)
  {
    if(e.getStateChange() == ItemEvent.SELECTED && 
        e.getSource() instanceof JComboBox) 
    {
      JComboBox cbox = (JComboBox) e.getSource();
      int id = cbox.getSelectedIndex();
      
      // Sonderrolle: -- Liste bearbeiten --
      if(id == cbox.getItemCount()-1) {
        eventHandler.handleWollMuxUrl(WollMux.cmdPALVerwalten, null);
        return;
      }
      
      String name = cbox.getSelectedItem().toString();
      if(name != null && !name.equals(LEERE_LISTE)) 
      {
        eventHandler.handleSelectPALEntry(name, id);
      }
    }
  }
  
  /**
   * Setzt die Einträge aller Senderboxes neu.
   * @param entries die Einträge, die die Senderboxen enthalten sollen.
   * @param current der ausgewählte Eintrag
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   * TESTED
   */
  public void updateSenderboxes(String[] entries, String current)
  {
    Iterator iter = senderboxes.iterator();
    while(iter.hasNext()) 
    {
      JComboBox senderbox = (JComboBox) iter.next();
      
      // keine items erzeugen beim Update
      senderbox.removeItemListener(itemListener);
      
      // alte Items löschen
      senderbox.removeAllItems();
      
      // neue Items eintragen
      if(entries.length > 0) 
      {
        for (int i = 0; i < entries.length; i++)
        {
          senderbox.addItem(entries[i]);
        }
      } 
      else senderbox.addItem(LEERE_LISTE);

      senderbox.addItem("- - - - Liste bearbeiten - - - -");
      
      // Selektiertes Item setzen:
      
      if (current != null && !current.equals(""))
        senderbox.setSelectedItem(current);
      
      // ItemListener wieder setzen.
      senderbox.addItemListener(itemListener);
    }
  }
  
  /**
   * Erzeugt ein Popup-Fenster, das den Benutzer darüber informiert, dass keine
   * Verbindung zur WollMux-Komponente in OpenOffice hergestellt werden konnte.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectionFailedWarning()
  {
    JOptionPane.showMessageDialog(null, CONNECTION_FAILED_MESSAGE, "WollMux-Fehler", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Ein WindowListener, der auf die JFrames der Leiste und des Icons 
   * registriert wird, damit als
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
   * Wird auf das Icon-Fenster als MouseListener und MouseMotionListener registriert,
   * um das Anklicken und Verschieben des Icons zu managen; wird auf das 
   * Leistenfenster als WindowFocusListener registriert, um falls erforderlich das
   * minimieren zum Icon anzustoßen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class WindowTransformer implements MouseListener, MouseMotionListener, WindowFocusListener
  {
    /**
     * Falls das Icon mit der Maus gezogen wird ist dies der Startpunkt an dem
     * der Mausknopf heruntergedrückt wurde.
     */
    private Point dragStart = new Point(0,0);
    
    /**
     * true, während das Icon-Fenster aktiv ist, false während das Leistenfenster
     * aktiv ist.
     */
    private boolean isLogo = false;
    
    public void mouseClicked(MouseEvent e)
    {
      if (!isLogo) return;
      logoFrame.setVisible(false);
      myFrame.setVisible(true);
      myFrame.setExtendedState(JFrame.NORMAL);
      isLogo = false;
    }
    
    public void mousePressed(MouseEvent e)
    {
      dragStart = e.getPoint();
    }

    public void mouseReleased(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseMoved(MouseEvent e) {}
    public void windowGainedFocus(WindowEvent e) {}
    
    public void windowLostFocus(WindowEvent e)
    {
      /*
       * ACHTUNG! Diese Methode wird auch direkt aufgerufen 
       * aus processUiElementEvent(). Dadurch kann es zu Doppelaufruf kommen.
       * Sollte die Unterscheidung irgendwann mal nötig sein, dann kann das
       * daran erfolgen, dass der Event e im synthetischen Fall null ist.
       */
      
      if (windowMode == ALWAYS_ON_TOP_WINDOW_MODE || windowMode == NORMAL_WINDOW_MODE) return;
      if (windowMode == MINIMIZE_TO_TASKBAR_MODE) {myFrame.setExtendedState(Frame.ICONIFIED); return;}
      if (isLogo) return;
      myFrame.setVisible(false);
      logoFrame.setVisible(true);
      isLogo = true;
    }
    
    public void mouseDragged(MouseEvent e)
    {
      Point winLoc = logoFrame.getLocation();
      Point p = e.getPoint();
      winLoc.x += p.x - dragStart.x;
      winLoc.y += p.y - dragStart.y;
      logoFrame.setLocation(winLoc);
    }
  }
  
  /**
   * Startet die WollMuxBar.
   * @param args --minimize, --topbar, --normalwindow um das Anzeigeverhalten festzulegen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args)
  {
    int windowMode = BECOME_ICON_MODE;
    if (args.length > 0)
    {
      if (args[0].equals("--minimize")) windowMode = MINIMIZE_TO_TASKBAR_MODE;
      else
      if (args[0].equals("--topbar")) windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
      else
      if (args[0].equals("--normalwindow")) windowMode = NORMAL_WINDOW_MODE;
      else
      {
        System.err.println("Unbekannter Aufrufparameter: "+args[0]);
        System.exit(1);
      }
      
      if (args.length > 1)
      {
        System.err.println("Zu viele Aufrufparameter!");
        System.exit(1);
      }
    }
    
    WollMuxFiles.setupWollMuxDir();
    
    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();
    
    try{
      Logger.debug("WollMuxBar gestartet");
      
      try{
        String windowMode2 = wollmuxConf.get("Fenster").get("WollMuxBar").get("MODE").toString();
        if (windowMode2.equals("Icon"))
          windowMode = BECOME_ICON_MODE;
        else if (windowMode2.equals("AlwaysOnTop"))
          windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
        else if (windowMode2.equals("Window"))
          windowMode = NORMAL_WINDOW_MODE;
        else if (windowMode2.equals("Minimize"))
          windowMode = MINIMIZE_TO_TASKBAR_MODE;
        else
          Logger.error("Ununterstützer MODE für WollMuxBar-Fenster: '"+windowMode2+"'");
      }catch(Exception x){}
      
      if (wollmuxConf.query("Symbolleisten").count()==0)
        JOptionPane.showMessageDialog(null, WOLLMUX_CONFIG_ERROR_MESSAGE, "Fehlerhafte Konfiguration", JOptionPane.ERROR_MESSAGE);
      else
        new WollMuxBar(windowMode, wollmuxConf);
      
    } catch(Exception x)
    {
      Logger.error(x);
    }
  }

}
