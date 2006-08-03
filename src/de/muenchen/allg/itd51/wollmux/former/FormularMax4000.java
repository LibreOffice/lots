/*
* Dateiname: FormularMax4000.java
* Projekt  : WollMux
* Funktion : Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

/**
 * Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormularMax4000
{

  /**
   * ActionListener für Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
     { public void actionPerformed(ActionEvent e){ abort(); } };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Das Haupt-Fenster des FormularMax4000.
   */
  private JFrame myFrame;
  
  public FormularMax4000(final XTextDocument doc)
  {
     //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(doc);}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  private void createGUI(XTextDocument doc)
  {
    Common.setLookAndFeelOnce();
    
    //  Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    
    JPanel myPanel = new JPanel(new GridLayout(1, 2));
    myFrame.getContentPane().add(myPanel);
    
    myFrame.pack();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    myFrame.dispose();
  }
  
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {closeAction.actionPerformed(null); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
    
  }
  
  /**
   * Ruft den FormularMax4000 für das aktuelle Vordergrunddokument auf, falls dieses
   * ein Textdokument ist. 
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    new FormularMax4000(doc);
  }

}
