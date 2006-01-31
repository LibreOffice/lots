/*
* Dateiname: FormGUI.java
* Projekt  : WollMux
* Funktion : managed die Fenster (OfficeBean-Vorschau und FormController) der FormularGUI. 
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 27.01.2006 | BNK | Erstellung
* 30.01.2006 | BNK | Office-Bean Einbindung
* 31.01.2006 | BNK | Bean im Preview-Modus aufrufen
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.beans.OOoBean;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.FormModel;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Managed die Fenster (OfficeBean-Vorschau und FormController) der FormularGUI.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormGUI
{
  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;
  
  private OOoBean myBean;
  
  /**
   * Der Titel des Formularfensters.
   */
  private String formTitle = "Unbenanntes Formular";
  

  /**
   * ActionListener für Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
        { public void actionPerformed(ActionEvent e){ abort(); } };
  


  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
  

  public FormGUI(final ConfigThingy conf, OOoBean bean)
  {
    myBean = bean;
    
    try{
      formTitle = conf.get("Formular").get("TITLE").toString();
    }catch(Exception x) {}
  
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(conf);}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}

  }

  private void createGUI2(ConfigThingy conf)
  {
    //Common.setLookAndFeel();
    
    //Create and set up the window.
    Frame myFrame = new java.awt.Frame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    //myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    //myFrame.addWindowListener(new MyWindowListener());
    
    Panel contentPanel = new Panel(new BorderLayout());
    //myFrame.setContentPane(contentPanel);
    contentPanel.add(myBean, BorderLayout.CENTER);
    myFrame.add(contentPanel);
    
    
    myFrame.pack();
    int frameWidth = 400;
    int frameHeight = 400;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
    
    try{
      myBean.loadFromURL("private:factory/swriter", null);
      myBean.aquireSystemWindow();
    }catch(Exception e)
    {
      Logger.error(e);
    }


  }
  private void createGUI(ConfigThingy conf)
  {
    Common.setLookAndFeel();
    
    //Create and set up the window.
    myFrame = new JFrame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    JPanel contentPanel = new JPanel(new GridBagLayout());
    myFrame.getContentPane().add(contentPanel);
    
//  int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    GridBagConstraints gbcFormController = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.PAGE_START,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
    GridBagConstraints gbcBean           = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,   GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
    
    FormController formController;
    try{
      formController = new FormController(conf, new DummyFormModel(), new HashMap());
      contentPanel.add(formController.JPanel(), gbcFormController);
    }catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }
    
    ScrollPane scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
    scrollPane.add(myBean);
    contentPanel.add(scrollPane, gbcBean);

    myFrame.pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    myFrame.setSize(frameWidth, screenSize.height*4/5);

    int frameHeight = myFrame.getHeight();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
    myFrame.validate();
    
    try{
      PropertyValue[] arguments = new PropertyValue[2];
      arguments[0] = new PropertyValue();
      arguments[0].Name = "Preview";
      arguments[0].Value = new Boolean(true);
      arguments[1] = new PropertyValue();
      arguments[1].Name = "ReadOnly";
      arguments[1].Value = new Boolean(true);
      myBean.loadFromURL("file:///c:/temp/foobar.odt", arguments);
    }catch(Exception e)
    {
      Logger.error(e);
    }
    contentPanel.invalidate();
    contentPanel.validate();
    contentPanel.repaint();
    scrollPane.invalidate();
    scrollPane.validate();
    scrollPane.repaint();
    myBean.invalidate();
    myBean.validate();
    myBean.repaint();
    scrollPane.invalidate();
    scrollPane.validate();
    scrollPane.repaint();
    myBean.invalidate();
    myBean.validate();
    myBean.repaint();
    contentPanel.invalidate();
    contentPanel.validate();
    contentPanel.repaint();
    scrollPane.invalidate();
    scrollPane.validate();
    scrollPane.repaint();
    myBean.invalidate();
    myBean.validate();
    myBean.repaint();
   
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
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    myFrame.dispose();
    myBean.stopOOoConnection();
    System.exit(0);
  }
  
  
  private static class DummyFormModel implements FormModel
  {
  }
  
  /**
   * @param args
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    String confFile = "testdata/formulartest.conf";
    ConfigThingy conf = new ConfigThingy("", new URL(new File(System
        .getProperty("user.dir")).toURL(), confFile));
    OOoBean bean = new OOoBean();
    System.out.println(""+bean.getOOoConnection());
    
    new FormGUI(conf, bean);
  }


}
