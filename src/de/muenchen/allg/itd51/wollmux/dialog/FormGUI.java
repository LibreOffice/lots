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
* 01.02.2006 | BNK | etwas rumgedoktore mit LayoutManager 
* 02.02.2006 | BNK | Fenster zusammengeklebt
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow2;
import com.sun.star.text.XTextDocument;

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
  int winBorderWidth;
  int winBorderHeight;
  
  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;
  
  private FormModel myDoc;
  
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
  

  public FormGUI(final ConfigThingy conf, FormModel doc, final Map mapIdToPresetValue)
  {
    myDoc = doc;
    
    try{
      formTitle = conf.get("Formular").get("TITLE").toString();
    }catch(Exception x) {}
  
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(conf, mapIdToPresetValue);}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}

  }


  private void createGUI(ConfigThingy conf, Map mapIdToPresetValue)
  {
    Common.setLookAndFeel();
    
    //Create and set up the window.
    myFrame = new JFrame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener(); 
    myFrame.addWindowListener(oehrchen);
    myFrame.addComponentListener(oehrchen);
    
    FormController formController;
    try{
      formController = new FormController(conf, myDoc, mapIdToPresetValue);
    }catch (ConfigurationErrorException x)
    {
      Logger.error(x);
      return;
    }

    myFrame.getContentPane().add(formController.JPanel());

    myFrame.pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    //frameHeight = screenSize.height * 8 / 10;
    //myFrame.setSize(frameWidth, frameHeight);
    int x = 0; //screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
    
    Point panelLocation = formController.JPanel().getLocationOnScreen();
    Point frameLocation = myFrame.getLocationOnScreen();
    winBorderWidth  = panelLocation.x - frameLocation.x;
    winBorderHeight = panelLocation.y - frameLocation.y;
    cuddleWithOpenOfficeWindow();
  }


  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void cuddleWithOpenOfficeWindow()
  {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    int docX = myFrame.getX() + frameWidth + winBorderWidth;
    int docY = myFrame.getY() + winBorderHeight;
    int docWidth = screenSize.width - docX - winBorderWidth;
    int docHeight = frameHeight - winBorderHeight - winBorderWidth;
    myDoc.setWindowPosSize(docX, docY, docWidth, docHeight);
    //UNO.XTopWindow(win).toFront();
    //win.setFocus();
  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener, ComponentListener
  {
    public MyWindowListener(){}
    public void windowActivated(WindowEvent e) 
    { 
      cuddleWithOpenOfficeWindow();
    }
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { closeAction.actionPerformed(null); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) 
    {
      myDoc.setWindowVisible(true);
      cuddleWithOpenOfficeWindow();
    }
    public void windowIconified(WindowEvent e) 
    { 
      myDoc.setWindowVisible(false);
    }
    public void windowOpened(WindowEvent e) {}
    public void componentResized(ComponentEvent e)
    {
      cuddleWithOpenOfficeWindow();
    }
    public void componentMoved(ComponentEvent e)
    {
      cuddleWithOpenOfficeWindow();
    }
    public void componentShown(ComponentEvent e)
    {
    }
    public void componentHidden(ComponentEvent e)
    {
    }
  }
  
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    myDoc.close();
    myFrame.dispose();
    System.exit(0);
  }
  
  
  private static class DummyFormModel implements FormModel
  {
    XTextDocument myDoc;
    XWindow2 myWindow;
    
    public DummyFormModel(XTextDocument doc)
    {
      myDoc = doc;
      myWindow = UNO.XWindow2(myDoc.getCurrentController().getFrame().getContainerWindow()); 
    }
    
    public void setWindowPosSize(int x, int y, int width, int height)
    {
      myWindow.setPosSize(x, y, width, height, PosSize.POSSIZE);
    }

    public void setWindowVisible(boolean vis)
    {
      myWindow.setVisible(vis);
    }

    public void close()
    {
      try{
        UNO.XCloseable(myDoc).close(true);
      }catch(Exception x)
      {
        Logger.error(x);
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
    UNO.init();
    String confFile = "testdata/formulartest.conf";
    ConfigThingy conf = new ConfigThingy("", new URL(new File(System
        .getProperty("user.dir")).toURL(), confFile));
    XTextDocument doc = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));
    FormModel model = new DummyFormModel(doc);
    new FormGUI(conf, model, new HashMap());
  }


}
