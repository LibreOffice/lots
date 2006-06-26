/*
* Dateiname: FormGUI.java
* Projekt  : WollMux
* Funktion : managed die Fenster (Writer und FormController) der FormularGUI. 
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
* 05.05.2006 | BNK | Condition -> Function, besser kommentiert 
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
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Managed die Fenster (Writer und FormController) der FormularGUI.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormGUI
{
  /**
   * Die Breite des linken Randes eines Fensters. Diese wird benötigt, um die exakte
   * Platzierung von Writer und Formular-Fenster so zu steuern, dass die Fenster
   * ohne Lücke nebeneinander angeordnet sind.
   */
  int winBorderWidth;
  
  /**
   * Die Breite des oberen Randes eines Fensters. Diese wird benötigt, um die exakte
   * Platzierung von Writer und Formular-Fenster so zu steuern, dass die Fenster
   * auf und mit gleicher Höhe angeordnet sind.
   */
  int winBorderHeight;
  
  /**
   * Das Fenster der Formular-GUI. Hier wird der FormController eingebettet. Auch
   * das Office-Bean wäre hier eingebettet worden, wenn nicht die Entscheidung
   * gegen seine Verwendung gefallen wäre.
   */
  private JFrame myFrame;
  
  /**
   * Das zum Formular gehörende Writer-Dokument (als FormModel gekapselt).
   */
  private FormModel myDoc;
  
  /**
   * Der Titel des Formularfensters (falls nicht anderweitig spezifiziert).
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
  

  /**
   * Zeigt eine neue Formular-GUI an.
   * @param conf der Formular-Knoten, der die Formularbeschreibung enthält.
   * @param doc das zum Formular gehörende Writer-Dokument (gekapselt als FormModel)
   * @param mapIdToPresetValue bildet IDs von Formularfeldern auf Vorgabewerte ab.
   *        Falls hier ein Wert für ein Formularfeld vorhanden ist, so wird dieser
   *        allen anderen automatischen Befüllungen vorgezogen. Wird das Objekt
   *        {@link FormController#FISHY} als Wert für ein Feld übergeben, 
   *        so wird dieses Feld
   *        speziell markiert als ungültig bis der Benutzer es manuell ändert.
   * @param functionContext der Kontext für Funktionen, die einen benötigen.
   * @param funcLib die Funktionsbibliothek, die zur Auswertung von Plausis etc.
   *        herangezogen werden soll.
   * @param dialogLib die Dialogbibliothek, die die Dialoge bereitstellt, die
   *        für automatisch zu befüllende Formularfelder benötigt werden.
   */
  public FormGUI(final ConfigThingy conf, FormModel doc, final Map mapIdToPresetValue,
      final Map functionContext,
      final FunctionLibrary funcLib, final DialogLibrary dialogLib)
  {
    myDoc = doc;
    
    try{
      formTitle = conf.get("TITLE").toString();
    }catch(Exception x) {}
  
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(conf, mapIdToPresetValue, functionContext, funcLib, dialogLib);}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}

  }


  private void createGUI(ConfigThingy conf, Map mapIdToPresetValue,
      Map functionContext,
      FunctionLibrary funcLib, DialogLibrary dialogLib)
  {
    Common.setLookAndFeelOnce();
    
    //Create and set up the window.
    myFrame = new JFrame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    //der ComponentListener sorgt dafür dass bei Verschieben/Größenänderung das
    //Writer-Fenster ebenfalls angepasst wird.
    myFrame.addComponentListener(oehrchen);
    
    FormController formController;
    try{
      formController = new FormController(conf, myDoc, mapIdToPresetValue, functionContext, funcLib, dialogLib, new MyAbortRequestListener());
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
    
    /*
     * Berechnen der Breite des Fensterrahmens.
     */
    Point panelLocation = formController.JPanel().getLocationOnScreen();
    Point frameLocation = myFrame.getLocationOnScreen();
    winBorderWidth  = panelLocation.x - frameLocation.x;
    winBorderHeight = panelLocation.y - frameLocation.y;
    
    /*
     * Muss nach dem Berechnen von winBorderWidth und winBorderHeight aufgerufen
     * werden, da diese in der Methode verwendet werden.
     */
    cuddleWithOpenOfficeWindow();
  }


  /**
   * Arrangiert das Writer Fenster so, dass es neben dem Formular-Fenster
   * sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
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
  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird, sowie
   * ein ComponentListener, der beim Verschieben und Verändern der Größe dafür
   * sorgt, dass das Writer-Fenster entsprechend mitverändert.
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

    public void setVisibleState(String groupId, boolean visible) 
    {
      Logger.log("Gruppe \""+groupId+"\" ist jetzt "+(visible?"sichtbar":"unsichtbar"));
    }

    public void valueChanged(String fieldId, String newValue)
    {
      Logger.log("Feld \""+fieldId+"\" hat jetzt den Wert \""+newValue+"\"");
    }

    public void focusGained(String fieldId)
    {
      Logger.log("Feld \""+fieldId+"\" hat den Fokus bekommen");
    }

    public void focusLost(String fieldId)
    {
      Logger.log("Feld \""+fieldId+"\" hat den Fokus verloren"); 
    }
  }
  
  private class MyAbortRequestListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  }
  
  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    WollMuxFiles.setupWollMuxDir();
    Logger.init(System.err, Logger.DEBUG);
    String confFile = "testdata/formulartest.conf";
    ConfigThingy conf = new ConfigThingy("", new URL(new File(System
        .getProperty("user.dir")).toURL(), confFile));
    XTextDocument doc = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));
    FormModel model = new DummyFormModel(doc);
    Map mapIdToPresetValue = new HashMap();
    mapIdToPresetValue.put("NEFishy", FormController.FISHY);
    mapIdToPresetValue.put("NEPresetInList", "Dings");
    mapIdToPresetValue.put("NEPresetNotInList", "Schwupps");
    mapIdToPresetValue.put("EFishy", FormController.FISHY);
    mapIdToPresetValue.put("EPresetInList", "Dings");
    mapIdToPresetValue.put("EPresetNotInList", "Schwupps");
    mapIdToPresetValue.put("AbtLohn", "TRUE");
    mapIdToPresetValue.put("AbtAnteile", "false");
    mapIdToPresetValue.put("AbtKaution", "true");
    
    Map functionContext = new HashMap();
    DialogLibrary dialogLib = WollMuxFiles.parseFunctionDialogs(conf.get("Formular"), null, functionContext);
    FunctionLibrary funcLib = WollMuxFiles.parseFunctions(conf.get("Formular"), dialogLib, functionContext, null);

    new FormGUI(conf.get("Formular"), model, mapIdToPresetValue, functionContext, funcLib, dialogLib);
  }


}
