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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

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
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
        { public void actionPerformed(ActionEvent e){ abort(); } };
        
        /**
         * ActionListener für Buttons mit der ACTION "openTemplate". 
         */
  private ActionListener actionListener_openTemplate = new ActionListener()
  { public void actionPerformed(ActionEvent e){ openTemplate(e.getActionCommand()); } };
  
  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
  
  /**
   * Alle senderboxes (JComboBoxes) der Leiste.
   */
  private List senderboxes = new Vector();
  
  /**
   * @param conf Briefkopfleiste-Knoten 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(final ConfigThingy conf)
  throws ConfigurationErrorException
  {

    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(conf);}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}

  }

  private void createGUI(ConfigThingy barDesc)
  {
    Common.setLookAndFeel();
    
    String title ="WollMux";
    try{
      title = barDesc.get("TITLE").toString();
    }catch(Exception x) {}
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    contentPanel = new JPanel();
    contentPanel.setLayout(new GridBagLayout());
    myFrame.getContentPane().add(contentPanel);
    
    addUIElements(barDesc, "Buttons", contentPanel, 1, 0);
    
    WindowTransformer trafo = new WindowTransformer();
    
    logoFrame = new JFrame("WollMux");
    logoFrame.setUndecorated(true);
    logoFrame.setAlwaysOnTop(true);
    JLabel logo = new JLabel(new ImageIcon("testdata/wollmux_klein.jpg"));
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

    myFrame.setAlwaysOnTop(true);
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
          //String id = "";
          //try{ id = uiElementDesc.get("ID").toString(); }catch(NodeNotFoundException e){}
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
          if (type.equals("senderbox"))
          {
            int lines = 10;
            try{ lines = Integer.parseInt(uiElementDesc.get("LINES").toString()); } catch(Exception e){}
            
            JComboBox senderbox = new JComboBox();
            
            senderbox.setMaximumRowCount(lines);
            senderbox.setPrototypeDisplayValue("Matthias S. Benkmann ist euer Gott (W-OLL-MUX-5.1)");
            senderbox.setEditable(false);
            
            //TODO senderbox.add*listener(...);
            
            String action = "";
            try{ action = uiElementDesc.get("ACTION").toString(); }catch(NodeNotFoundException e){}
            
            ActionListener actionL = getAction(action);
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
              
            JButton button = new JButton(label);
            button.setMnemonic(hotkey);

            gbcButton.gridx = x;
            gbcButton.gridy = y;
            compo.add(button, gbcButton);
            
            ActionListener actionL = getAction(action);
            if (actionL != null) button.addActionListener(actionL);
            String fragment = "";
            try{
              fragment = uiElementDesc.get("FRAG_ID").toString();
            }catch(NodeNotFoundException e){}
            button.setActionCommand(fragment);
            
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
    UNO.init();
    String confFile = "testdata/wollmuxbar.conf";
    ConfigThingy conf = new ConfigThingy("", new URL(new File(System
        .getProperty("user.dir")).toURL(), confFile));
    new WollMuxBar(conf);    
  }
  

}

