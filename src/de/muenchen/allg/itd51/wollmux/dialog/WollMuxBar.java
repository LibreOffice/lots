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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
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
   * Der Anzeigemodus für die WollMuxBar (z.B. {@link BECOME_ICON_MODE})
   */
  private int windowMode; 

  /**
   * Dient der thread-safen Kommunikation mit dem entfernten WollMux.
   */
  private WollMuxBarEventHandler eventHandler;
  
  /**
   * Das Fenster das je nach Situation die WollMuxBar oder das Icon enthält.
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
   * Mappt einen Menü-Namen auf ein entsprechendes JMenu.
   */
  private Map mapMenuNameToJMenu = new HashMap();
  
  /**
   * Rand über und unter einem horizontalen bzw links und rechts neben vertikalem
   * Separator (in Pixeln).
   */
  private final static int SEP_BORDER = 4;
  
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
       { public void actionPerformed(ActionEvent e){ abort(); } };
        
  /**
   * ActionListener für Buttons mit der ACTION "openTemplate". 
   */
  private ActionListener actionListener_openTemplate = new ActionListener()
       { public void actionPerformed(ActionEvent e){ 
         eventHandler.handleWollMuxUrl(WollMux.cmdOpenTemplate, e.getActionCommand()); } };

  /**
   * ActionListener für Buttons mit der ACTION "openDocument". 
   */
  private ActionListener actionListener_openDocument = new ActionListener()
       { public void actionPerformed(ActionEvent e){ 
         eventHandler.handleWollMuxUrl(WollMux.cmdOpenDocument, e.getActionCommand()); } };

  /**
   * ActionListener für Buttons mit der ACTION "absenderAuswaehlen". 
   */
  private ActionListener actionListener_absenderAuswaehlen = new ActionListener()
      { public void actionPerformed(ActionEvent e){ 
        eventHandler.handleWollMuxUrl(WollMux.cmdAbsenderAuswaehlen, e.getActionCommand()); } };
  
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(int winMode, final ConfigThingy conf)
  throws ConfigurationErrorException, SyntaxErrorException, IOException
  {
    windowMode = winMode;

    eventHandler = new WollMuxBarEventHandler(this);
    eventHandler.connectWithWollMux();
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(conf);}catch(Exception x)
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
    Logger.debug("WollMuxBar.createGUI");
    Common.setLookAndFeel();
    
    String title = DEFAULT_TITLE;
    try{
      title = conf.get("Briefkopfleiste").get("TITLE").toString();
    }catch(Exception x) {}
    
    //Create and set up the window.
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
    
    //myFrame.setUndecorated(true);
    
    logoFrame = new JFrame(DEFAULT_TITLE);
    logoFrame.setUndecorated(true);
    logoFrame.setAlwaysOnTop(true);
    
    JLabel logo = new JLabel(new ImageIcon(ICON_URL));
    logo.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel = new JPanel(new GridLayout(1,1));
    logoPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel.add(logo);
    logoFrame.getContentPane().add(logoPanel);
    
    WindowTransformer trafo = new WindowTransformer();
    myFrame.addWindowFocusListener(trafo);
    logo.addMouseListener(trafo);
    logo.addMouseMotionListener(trafo);
    
    logoFrame.addWindowListener(new MyWindowListener());
    
    logoFrame.setFocusableWindowState(false);
    logoFrame.pack();
    logoFrame.setLocation(0,0);
    logoFrame.setResizable(false);

    if (windowMode != NORMAL_WINDOW_MODE) myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    myFrame.setLocation(0,0);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
  }
  
  
  /** Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu.
   *  compo muss ein GridBagLayout haben. stepx und stepy geben an um
   *  wieviel mit jedem UI Element die x und die y Koordinate der Zelle
   *  erhöht werden soll. Wirklich sinnvoll sind hier nur (0,1) und (1,0).
   *  menuConf muss als Kinder "Menues"-Knoten haben, die als ihre Kinder
   *  Menübeschreibungen haben für die Menüs, die als UI Elemente verwendet
   *  werden.
   *  context kann "menu" oder "panel" sein.
   */
  private void addUIElements(ConfigThingy menuConf, ConfigThingy elementParent, JComponent compo, int stepx, int stepy, String context)
  {
    // TODO: Umstellen auf uiElementFactory
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    //GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel      = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcGlue       = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    GridBagConstraints gbcButton     = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcMenuButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcListBox    = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,           new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcSeparator  = new GridBagConstraints(0, 0, 1, 1, (double)stepx, (double)stepy, GridBagConstraints.CENTER,  stepx == 0? GridBagConstraints.HORIZONTAL : GridBagConstraints.VERTICAL,       new Insets(stepy > 0? SEP_BORDER:0,stepx > 0? SEP_BORDER:0,stepy > 0? SEP_BORDER:0, stepx > 0? SEP_BORDER:0),0,0);
      
    int y = -stepy;
    int x = -stepx; 
      
    Iterator piter = elementParent.iterator();
    while (piter.hasNext())
    {
      ConfigThingy uiElementDesc = (ConfigThingy)piter.next();
      y += stepy;
      x += stepx;
      
      try{
        /*
         * TODO: ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
         * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
         * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
         * NOCH KONSISTENT IST!
         */
        
        //boolean readonly = false;
        //String id = "";
        //try{ id = uiElementDesc.get("ID").toString(); }catch(NodeNotFoundException e){}
        //try{ if (uiElementDesc.get("READONLY").toString().equals("true")) readonly = true; }catch(NodeNotFoundException e){}
        String type = uiElementDesc.get("TYPE").toString();
        
        if (type.equals("label"))
        {
          JLabel uiElement = new JLabel();
          gbcLabel.gridx = x;
          gbcLabel.gridy = y;
          if (context.equals("menu"))
            compo.add(uiElement);
          else
            compo.add(uiElement, gbcLabel);
          uiElement.setText(uiElementDesc.get("LABEL").toString());
        }
        else
          if (type.equals("separator"))
          {
            JComponent uiElement = new JSeparator(stepx == 0? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);
            if (context.equals("menu"))
            {
              JPanel p = new JPanel(new GridLayout(1,1));
              p.add(uiElement);
              uiElement = p;
              uiElement.setBorder(BorderFactory.createEmptyBorder(stepy > 0? SEP_BORDER:0,stepx > 0? SEP_BORDER:0,stepy > 0? SEP_BORDER:0, stepx > 0? SEP_BORDER:0));
            }
            
            gbcSeparator.gridx = x;
            gbcSeparator.gridy = y;
            if (context.equals("menu"))
              compo.add(uiElement);
            else
              compo.add(uiElement, gbcSeparator);
          }
          else if (type.equals("glue"))
          {
            Box uiElement = Box.createHorizontalBox();
            try{
              int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE").toString());
              uiElement.add(Box.createHorizontalStrut(minsize));
            }catch(Exception e){}
            uiElement.add(Box.createHorizontalGlue());
            
            gbcGlue.gridx = x;
            gbcGlue.gridy = y;
            if (context.equals("menu"))
              compo.add(uiElement);
            else
              compo.add(uiElement, gbcGlue);
          }
          else
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
              
              senderboxes.add(senderbox);
              
              gbcListBox.gridx = x;
              gbcListBox.gridy = y;
              compo.add(senderbox, gbcListBox);
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
                
                AbstractButton button;
                if (context.equals("menu"))
                  button = new JMenuItem(label);
                else 
                  button = new JButton(label);
                
                button.setMnemonic(hotkey);
                
                ActionListener actionL = getAction(action);
                if (actionL != null) button.addActionListener(actionL);
                
                // Erzeuge frag_id-Liste
                String fragment = "";
                ConfigThingy fids = uiElementDesc.query("FRAG_ID");
                if(fids.count() == 0) {
                  //TODO: Wieso ist dies im WollMux-Branch ein TODO, aber hier war es das nicht?? Bitte erst nach Funktionstest der Log-Meldung wieder aktivieren. Logger.error("Keine FRAG_ID definiert in Element " + uiElementDesc.stringRepresentation());
                } else {
                  Iterator i = fids.iterator();
                  fragment = i.next().toString();
                  while (i.hasNext())
                  {
                    fragment += "&" + i.next().toString();
                  }
                }

                button.setActionCommand(fragment);
                
                gbcButton.gridx = x;
                gbcButton.gridy = y;
                if (context.equals("menu"))
                  compo.add(button);
                else
                  compo.add(button, gbcButton);
              }
              else
                if (type.equals("menu"))
                {
                  String label  = uiElementDesc.get("LABEL").toString();
                  
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
                    parseMenu(mapMenuNameToJMenu, menuConf, menuName, new JMenu(label));
                    //FIXME Ich glaube nicht, dass es wirklich funktioniert, das selbe JMenu an mehreren Stellen in die GUI-Hierarchie einzufügen. Insofern ist das mit der Map für JMenus wohl Schmarrn und es sollte jedes mal ein neues JMenu erzeugt werden. Selbiges gilt natürlich genauso innerhalb von parseMenu. Im Falle der JPopupMenus ist das allerdings kein Problem, da diese ja nirgends in die Hierarchie eingehängt werden.
                    button = (AbstractButton)mapMenuNameToJMenu.get(menuName);
                    if (button == null)
                      button = new JMenu(label);
                  }
                  else
                  {
                    parseMenu(mapMenuNameToJPopupMenu, menuConf, menuName, new JPopupMenu());
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
                  Logger.error("Ununterstützter TYPE für User Interface Element: "+type);
                }
      } catch(NodeNotFoundException e) {Logger.error(e);}
    }
  }
  
  private void parseMenu(Map mapMenuNameToMenu, ConfigThingy menuConf, String menuName, JComponent menu)
  {
    if (mapMenuNameToMenu.containsKey(menuName)) return;

    ConfigThingy conf;
    try
    {
      conf = menuConf.query(menuName).getLastChild().get("Elemente");
    }
    catch (Exception x)
    {
      Logger.error("Menü \"" + menuName + "\" nicht definiert");
      return;
    }
    
    /*
     * Zur Vermeidung von Endlosschleifen muss dieses Statement vor dem
     * Aufruf von addUIElements stehen.
     */
    mapMenuNameToMenu.put(menuName, menu);
    
//    menu.setLayout(new GridBagLayout());
    
    addUIElements(menuConf, conf, menu, 0, 1, "menu");
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
    if (action.equals("openTemplate"))
    {
      return actionListener_openTemplate;
    }
    if (action.equals("openDocument"))
    {
      return actionListener_openDocument;
    }
    if (action.equals("absenderAuswaehlen"))
    {
      return actionListener_absenderAuswaehlen;
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
     * Falls das Icon mit der Maus gezogen wird ist diese der Startpunkt an dem
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
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    eventHandler.handleTerminate();
    myFrame.dispose();
    eventHandler.waitForThreadTermination();

    System.exit(0);
  }
  

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Menü zugeordnet
   * ist.
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
   * ausgewählt wurde. Die Methode setzt daraufhin den aktuellen Absender im
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
   * @param entries die Einträge, die die Senderbox enthalten soll.
   * @param current der ausgewählte Eintrag
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
    Logger.init(WollMuxFiles.getWollMuxLogFile(), Logger.LOG);
    
    try{
      ConfigThingy wollmuxConf = new ConfigThingy("wollmuxConf", WollMuxFiles.getWollMuxConfFile().toURL());
      
      /*
       * Wertet die undokumentierte wollmux.conf-Direktive LOGGING_MODE aus und
       * setzt den Logging-Modus entsprechend.
       */
      ConfigThingy log = wollmuxConf.query("LOGGING_MODE");
      if (log.count() > 0)
      {
        String mode = log.getLastChild().toString();
        Logger.init(mode);
      }
      
      new WollMuxBar(windowMode, wollmuxConf);
      
    } catch(Exception x)
    {
      Logger.error(x);
    }
  }

}
