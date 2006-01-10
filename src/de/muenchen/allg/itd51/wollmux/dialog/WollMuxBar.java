/*
* Dateiname: WollMuxBar.java
* Projekt  : WollMux
* Funktion : Always-on-top Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.InputStreamReader;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar
{
  private final URL ICON_URL = this.getClass().getClassLoader().getResource("data/wollmux_klein.jpg");
  private static boolean minimize = false;
  private static boolean topbar = false;
  private static boolean normalwindow = false;

  
  /**
   * Der Rahmen der die Steuerelemente enthält.
   */
  private JFrame myFrame;
  
  /**
   * Der Rahmen, der das Logo enthält.
   */
  private JFrame logoFrame;
  
  /**
   * Das Panel, das die Steuerelemente enthält.
   */
  private JPanel contentPanel;
  
  /**
   * Das Panel, das nur das WollMux-Logo enthält.
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
  { public void actionPerformed(ActionEvent e){ openTemplate(e.getActionCommand()); } };
  
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
   * Alle senderboxes (JComboBoxes) der Leiste.
   */
  private List senderboxes = new Vector();
  
  /**
   * @param conf Vater-Knoten von Menues und Symbolleisten 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(final ConfigThingy conf)
  throws ConfigurationErrorException
  {

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
    Common.setLookAndFeel();
    
    String title ="WollMux";
    try{
      title = conf.get("Briefkopfleiste").get("TITLE").toString();
    }catch(Exception x) {}
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    contentPanel = new JPanel();
    contentPanel.setLayout(new GridBagLayout());
    myFrame.getContentPane().add(contentPanel);
    
    try{
      addUIElements(conf.query("Menues"),conf.get("Briefkopfleiste"), "Elemente", contentPanel, 1, 0);
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }

    WindowTransformer trafo = new WindowTransformer();
    
    logoFrame = new JFrame("WollMux");
    logoFrame.setUndecorated(true);
    logoFrame.setAlwaysOnTop(true);
    JLabel logo = new JLabel(new ImageIcon(ICON_URL));
    logo.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel = new JPanel(new GridLayout(1,1));
    logoPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    logoPanel.add(logo);
    logo.addMouseListener(trafo);
    logo.addMouseMotionListener(trafo);
    logoFrame.getContentPane().add(logoPanel);
    logoFrame.pack();
    logoFrame.setLocation(0,0);
    logoFrame.setResizable(false);

    if (!normalwindow) myFrame.setAlwaysOnTop(true);
    myFrame.addWindowFocusListener(trafo);
    
    myFrame.pack();
//    int frameWidth = myFrame.getWidth();
//    int frameHeight = myFrame.getHeight();
//    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//    int x = screenSize.width/2 - frameWidth/2; 
//    int y = screenSize.height/2 - frameHeight/2;
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
  private void addUIElements(ConfigThingy menuConf, ConfigThingy conf, String key, JComponent compo, int stepx, int stepy, String context)
  {
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    //GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel      = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcGlue       = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    GridBagConstraints gbcButton     = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcMenuButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcListBox    = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,           new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcSeparator  = new GridBagConstraints(0, 0, 1, 1, (double)stepx, (double)stepy, GridBagConstraints.CENTER,  stepx == 0? GridBagConstraints.HORIZONTAL : GridBagConstraints.VERTICAL,       new Insets(stepy > 0? SEP_BORDER:0,stepx > 0? SEP_BORDER:0,stepy > 0? SEP_BORDER:0, stepx > 0? SEP_BORDER:0),0,0);
      
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
            senderbox.addItem("Benkmann, Matthias (D-WOL-MUX-5.1)");
            
            senderbox.setMaximumRowCount(lines);
            senderbox.setPrototypeDisplayValue("Matthias B. ist euer Gott (W-OLL-MUX-5.1)");
            senderbox.setEditable(false);
            
            //TODO senderbox.add*listener(...);
            
            //String action = "";
            //try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
            
            //ActionListener actionL = getAction(action);
            //TODO if (actionL != null) list.addMouseListener(new MyActionMouseListener(senderbox, actionL));
            
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
            String fragment = "";
            try{
              fragment = uiElementDesc.get("FRAG_ID").toString();
            }catch(NodeNotFoundException e){}
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
  }
  
  private void addUIElements(ConfigThingy menuConf, ConfigThingy conf, String key, JComponent compo, int stepx, int stepy)
  {
    addUIElements(menuConf, conf, key, compo, stepx, stepy, "panel");
  }
  
  private void parseMenu(Map mapMenuNameToMenu, ConfigThingy menuConf, String menuName, JComponent menu)
  {
    if (mapMenuNameToMenu.containsKey(menuName)) return;

    ConfigThingy conf;
    try
    {
      conf = menuConf.query(menuName).getLastChild();
    }
    catch (NodeNotFoundException x)
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
    addUIElements(menuConf, conf, "Elemente", menu, 0, 1, "menu");
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
    if (action.equals("absenderAuswaehlen"))
    {
      return null;
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
  
  
  private class WindowTransformer implements MouseListener, MouseMotionListener, WindowFocusListener
  {
    private Point dragStart = new Point(0,0);
    private boolean isLogo = false;
    
    public void mouseClicked(MouseEvent e)
    {
      if (!isLogo) return;
      myFrame.setVisible(true);
      logoFrame.setVisible(false);
      isLogo = false;
    }
    public void mousePressed(MouseEvent e)
    {
      dragStart = e.getPoint();
    }
    public void mouseReleased(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseEntered(MouseEvent e)
    {
    }
    
    public void windowGainedFocus(WindowEvent e)
    {
    
    }
    public void windowLostFocus(WindowEvent e)
    {
      if (topbar || normalwindow) return;
      if (minimize) {myFrame.setExtendedState(Frame.ICONIFIED); return;}
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
    public void mouseMoved(MouseEvent e)
    {
      // TODO Auto-generated method stub
      
    }
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    myFrame.dispose();
    System.exit(0);
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void openTemplate(String fragid)
  {
    try{
      UNO.loadComponentFromURL(fragIdToURL(fragid),true,true);
    }catch(Exception x) {Logger.error(x);}
  }

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Menü zugeordnet
   * ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
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
  
  private String fragIdToURL(String fragid)
  {
    if (fragid.equals("externerBriefkopf")) return "http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/vorlagen/WOL_Briefkopf-extern_v1_2005-12-19.ott";
    if (fragid.equals("internerBriefkopf")) return "http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/vorlagen/WOL_Briefkopf-intern_v1_2005-11-23.ott";
    if (fragid.equals("kurzmitteilung")) return "http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/vorlagen/WOL_Briefkopf-Kurzmitteilung_v1_2005-11-25.ott";
    if (fragid.equals("faxVorlage")) return "http://limux.tvc.muenchen.de/ablage/sonstiges/wollmux/vorlagen/WOL_Briefkopf-Fax_v1_2005-12-12.ott";
    return "";
  }
  
  /**
   * @param args
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void main(String[] args) throws Exception
  {
    if (args.length > 0)
    {
      if (args[0].equals("--minimize")) minimize = true;
      else
      if (args[0].equals("--topbar")) topbar = true;
      else
      if (args[0].equals("--normalwindow")) normalwindow = true;
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
    UNO.init();
    String confFile = "data/wollmuxbar.conf";
    URL confURL = WollMuxBar.class.getClassLoader().getResource(confFile);
    
    //ConfigThingy conf = new ConfigThingy("", new URL(new File(System
    //    .getProperty("user.dir")).toURL(), confFile));
    ConfigThingy conf = new ConfigThingy("", confURL, new InputStreamReader(confURL.openStream(),ConfigThingy.CHARSET));
    new WollMuxBar(conf);    
  }
  

}

