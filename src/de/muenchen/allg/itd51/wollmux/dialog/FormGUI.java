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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.GridLayout;
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
  

  public FormGUI(final ConfigThingy conf)
  {
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

  private void createGUI(ConfigThingy conf)
  {
    Common.setLookAndFeel();
    
    //Create and set up the window.
    myFrame = new JFrame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    JPanel contentPanel = new JPanel(new GridLayout(2,1));
    myFrame.getContentPane().add(contentPanel);
    
    FormController formController;
    try{
      formController = new FormController(conf, new DummyFormModel(), new HashMap());
      contentPanel.add(formController.JPanel());
    }catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }

    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
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
    new FormGUI(conf);
  }


}
