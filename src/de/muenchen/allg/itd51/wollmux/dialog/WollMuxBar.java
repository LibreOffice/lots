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
* 06.02.2006 | BNK | Menüleiste hinzugefügt
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
import java.io.StringReader;
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

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.frame.XDispatch;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XPALProvider;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar implements XPALChangeEventListener
{
  private static final String DEFAULT_TITLE = "Vorlagen und Formulare";
  
  private final URL ICON_URL = this.getClass().getClassLoader().getResource("data/wollmux_klein.jpg");
  private static boolean minimize = false;
  private static boolean topbar = false;
  private static boolean normalwindow = false;

  
  /**
   * Der WollMux-Service, mit dem die WollMuxBar Informationen austauscht.
   * Der WollMux-Service sollte nicht über dieses Feld, sondern ausschließlich über
   * die Methode getRemoteWollMux bezogen werden, da diese mit einem möglichen
   * Schließen von OOo während die WollMuxBar läuft klarkommt.
   */
  private Object __mux;
  
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
   * enthält die Anzahl der Einträge in der Senderbox (ohne den "-- Liste bearbeiten --"-Eintrag).
   */
  private int senderBoxEntries = 0;

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
       { public void actionPerformed(ActionEvent e){ dispatchWollMuxUrl(WollMux.cmdOpenTemplate, e.getActionCommand()); } };

  /**
   * ActionListener für Buttons mit der ACTION "absenderAuswaehlen". 
   */
  private ActionListener actionListener_absenderAuswaehlen = new ActionListener()
      { public void actionPerformed(ActionEvent e){ dispatchWollMuxUrl(WollMux.cmdAbsenderAuswaehlen, e.getActionCommand()); } };
  
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
  private ItemListener itemListener = new ItemListener() { 
    public void itemStateChanged(ItemEvent e) { senderBoxItemChanged(e); } };


  /**
   * Alle senderboxes (JComboBoxes) der Leiste.
   */
  private List senderboxes = new Vector();
  
  
  /**
   * Dieses Flag ist true, wenn sie WollMux Bar einen PALUpdateListener beim
   * entfernten WollMux registriert hat.
   */
  private boolean palUpdateListenerIsRegistered = false;
  
  /**
   * @param conf Vater-Knoten von Menues und Symbolleisten 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar()
  throws ConfigurationErrorException, SyntaxErrorException, IOException
  {
    XWollMux mux = getRemoteWollMux(true);
    String confStr = "";
    if(mux!=null) {
      confStr = mux.getWollmuxConfAsString();
    }
      
    final ConfigThingy conf = new ConfigThingy("", null, new StringReader(confStr));

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
    
    String title = DEFAULT_TITLE;
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
      addUIElements(conf.query("Menues"),conf.get("Briefkopfleiste"), contentPanel, 1, 0, "panel");
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }
    
    JMenuBar mbar = new JMenuBar();
    addUIElements(conf.query("Menues"),conf.query("Menueleiste"), mbar, 1, 0, "menu");
    myFrame.setJMenuBar(mbar);
    
    //myFrame.setUndecorated(true);

    WindowTransformer trafo = new WindowTransformer();
    
    logoFrame = new JFrame(DEFAULT_TITLE);
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
              
              senderbox.addItemListener(itemListener);
              
              //String action = "";
              //try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
              
              //ActionListener actionL = getAction(action);
              //TODO if (actionL != null) list.addMouseListener(new MyActionMouseListener(senderbox, actionL));
              
              senderboxes.add(senderbox);
              
              gbcListBox.gridx = x;
              gbcListBox.gridy = y;
              compo.add(senderbox, gbcListBox);
              
              // updateListener registrieren:
              registerPALUpdateListener(getRemoteWollMux(true));
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
      if (e.getOppositeWindow() != null) return;
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
    deregisterPALUpdateListener(getRemoteWollMux(false));

    myFrame.dispose();

    System.exit(0);
  }
  
  /**
   * Diese Methode erzeugt eine wollmuxUrl und übergibt sie dem WollMux zur Bearbeitung.
   *  
   * @param dispatchCmd das Kommando, das der WollMux ausführen soll. (z.B. "openTemplate")
   * @param arg ein optionales Argument (z.B. "{fragid}"). Ist das Argument null oder
   *        der Leerstring, so wird es nicht mit übergeben.
   */
  private void dispatchWollMuxUrl(String dispatchCmd, String arg) {
    XDispatch disp = null;
    disp = (XDispatch) UnoRuntime.queryInterface(XDispatch.class, getRemoteWollMux(true));
    if(disp != null) {
      if(arg != null && !arg.equals("")) arg = "#" + arg;
      else arg = "";
      com.sun.star.util.URL url = new com.sun.star.util.URL();
      url.Complete = WollMux.wollmuxProtocol + ":" + dispatchCmd + arg;
      disp.dispatch(url, new PropertyValue[] {});
    }
  }

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Menü zugeordnet
   * ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void openMenu(ActionEvent e)
  {
    Logger.debug2("openMenu");
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
   * @param e
   */
  private void senderBoxItemChanged(ItemEvent e)
  {
    if(e.getStateChange() == ItemEvent.SELECTED && e.getSource() instanceof JComboBox) {
      JComboBox cbox = (JComboBox) e.getSource();
      int id = cbox.getSelectedIndex();
      
      // Sonderrolle: -- Liste bearbeiten --
      if(id == senderBoxEntries) {
        dispatchWollMuxUrl(WollMux.cmdPALVerwalten, null);
        return;
      }
      
      String name = cbox.getSelectedItem().toString();
      
      // TODO: Hier könnte ich auch eine neues WollMux-dispatch-Kommando einführen und verwenden.
      // wollmux informieren:
      XWollMux mux = getRemoteWollMux(true);
      if (mux != null)
      {
        mux.setCurrentSender(name, (short) id);
      }
    }
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
    
    new WollMuxBar();    
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventListener#updateContent(com.sun.star.lang.EventObject)
   */
  public void updateContent(EventObject eventObject)
  {
    XPALProvider palProv = (XPALProvider) UnoRuntime.queryInterface(
        XPALProvider.class,
        eventObject.Source);
    if (palProv != null)
    {
      Iterator iter = senderboxes.iterator();
      while(iter.hasNext()) {
        
        JComboBox senderbox = (JComboBox) iter.next();
        
        // keine items erzeugen beim Update
        senderbox.removeItemListener(itemListener);
        
        // alte Items löschen
        senderbox.removeAllItems();
        
        // neue Items eintragen
        String[] entries = palProv.getPALEntries();
        senderBoxEntries = entries.length;
        for (int i = 0; i < entries.length; i++)
        {
          senderbox.addItem(entries[i]);
        }
        senderbox.addItem("- - - - Liste bearbeiten - - - -");
        
        // Selektiertes Item setzen:
        if (palProv.getCurrentSender() != null)
          senderbox.setSelectedItem(palProv.getCurrentSender());
        
        // ItemListener wieder setzen.
        senderbox.addItemListener(itemListener);
      }
    } else {
      System.err.println("NO palProvider!");
    }
    
  }

  public void disposing(EventObject arg0)
  {
  }

  /**
   * Diese Methode liefert eine Instanz auf den entfernten WollMux zurück.
   * Wurde noch gar keine UNO-Verbindung hergestellt, so wird eine Verbindung
   * zu OOo hiermit aufgebaut und der WollMux instanziiert. 
   * 
   * Ist der Übergabewert reconnect true, wird die Verbindung auch dann wieder
   * hergestellt, wenn eine vorhergehende Verbindung aufgrund einer 
   * DisposedException unterbrochen wurde.
   * 
   * Konnte keine Verbindung hergestellt werden, oder eine unterbrochene
   * Verbindung nicht wieder hergestellt werden, so liefert die Methode null zurück.
   *
   * @param reconnect
   * @return Instanz eines gültigen WollMux oder null, falls keine Instanz 
   *         erzeugt werden konnte. 
   */
  private XWollMux getRemoteWollMux(boolean reconnect) {
    if(__mux != null) {
      try {
        return (XWollMux) UnoRuntime.queryInterface(XWollMux.class, __mux);
      } catch (DisposedException e) {
        __mux = null;
        if(!reconnect) return null;
      }
    }
    
    XWollMux mux = null;
    try
    {
      XComponentContext ctx = Bootstrap.bootstrap();
      XMultiServiceFactory factory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, ctx.getServiceManager());
      this.__mux = factory.createInstance("de.muenchen.allg.itd51.wollmux.WollMux");
      mux = (XWollMux) UnoRuntime.queryInterface(XWollMux.class, __mux);
    } catch (Exception e) { Logger.error(e); }

    // re-register Listener if they were registered in the previous connection:
    if(palUpdateListenerIsRegistered) {
      palUpdateListenerIsRegistered = false;
      registerPALUpdateListener(mux);
    }

    return mux;
  }
  
  /**
   * registriert den PALUpdateListener auf dem entfernten WollMux.
   * @param mux
   */
  private void deregisterPALUpdateListener(XWollMux mux) {
    if(mux != null && palUpdateListenerIsRegistered) {
      mux.removePALChangeEventListener(this);
      palUpdateListenerIsRegistered = false;
    }
  }
  
  private void registerPALUpdateListener(XWollMux mux)
  {
    if(mux != null && !palUpdateListenerIsRegistered) {
      mux.addPALChangeEventListener(this);
      palUpdateListenerIsRegistered = true;
    }
  }

}

