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
* 05.07.2006 | BNK | optische Verbesserungen, insbes. bzgl. arrangeWindows()
* 19.07.2006 | BNK | mehrere übelste Hacks, damit die Formular-GUI nie unsinnige Größe annimmt beim Starten
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

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
   * Die (vom Betriebssystem gelieferten) Ränder des Formular-GUI-Fensters.
   * Wird benötigt, um die exakte Platzierung der FormGUI und des Writer-Fensters
   * zu bewerkstelligen.
   */
  private Insets windowInsets;
  
  /**
   * Der maximal durch ein Fenster nutzbare Bereich, d,h, Bildschirmgroesse minus
   * Taskbar undsoweiter. 
   */
  private Rectangle maxWindowBounds;
  
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
   * Gibt die Lage und Größe des Fensters der FormGUI an, so wie sie von
   * {@link Common#parseDimensions(ConfigThingy)} geliefert wird.
   */
  private Rectangle formGUIBounds;
  

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
   * @param formFensterConf Der Formular-Unterabschnitt des Fenster-Abschnitts von
   *        wollmux.conf.
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
  public FormGUI(final ConfigThingy formFensterConf, final ConfigThingy conf, FormModel doc, final Map mapIdToPresetValue,
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
            try{createGUI(formFensterConf, conf, mapIdToPresetValue, functionContext, funcLib, dialogLib);}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}

  }


  private void createGUI(ConfigThingy formFensterConf, ConfigThingy conf, Map mapIdToPresetValue,
      Map functionContext,
      FunctionLibrary funcLib, DialogLibrary dialogLib)
  {
    Common.setLookAndFeelOnce();
    
    //Create and set up the window.
    myFrame = new JFrame(formTitle);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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

    myFrame.getContentPane().add(formController.JComponent());

    formGUIBounds = Common.parseDimensions(formFensterConf);
    
    
    /*
     * Leider kann wegen 
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4737732
     * nicht auf einfache Weise die nutzbare Bildschirmflaeche bestimmt werden,
     * deshalb der folgende Hack:
     *   - maxWindowBounds initialisieren (so wie es eigentlich reichen sollte aber
     *     unter KDE leider nicht tut) minus Sicherheitsabzug fuer KDE.
     *     Die Initialisierung ist erforderlich, weil
     *     vor den folgenden Events schon Events kommen können, die ein
     *     arrangeWindows() erforderlich machen. 
     *   - Wir registrieren einen WindowStateListener
     *   - Wir maximieren das Fenster
     *   - Sobald der Event-Handler das erfolgte Maximieren anzeigt, lesen
     *     wir die Größe aus und setzen wieder auf normal.
     *   - Sobald das Normalsetzen beendet ist deregistriert sich der 
     *     WindowStateListener und die Fenster werden arrangiert.
     *     
     * Bemerkung: Den Teil mit Normalsetzen kann man vermutlich entfernen, da
     * das Setzen der Fenstergröße ohnehin den maximierten Zustand verlässt.
     */

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    maxWindowBounds.height-=32; //Sicherheitsabzug für KDE Taskleiste

    myFrame.pack();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
    
    /*
     * Bestimmen der Breite des Fensterrahmens.
     */
    windowInsets = myFrame.getInsets();
    
    myFrame.addWindowStateListener(new WindowStateListener() {
      private int counter = 0;
      public void windowStateChanged(WindowEvent e)
      {
        if (counter++ == 0) createGUI2((e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
        else if (counter++ == 1) createGUI3();
        else if (counter++ == 2) myFrame.removeWindowStateListener(this);
      }}
    );
    
    myFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
  }
  
  private void createGUI2(boolean changeMaxWinBounds)
  {
    if (changeMaxWinBounds) 
    {
      Rectangle newBounds = myFrame.getBounds();
      //sanity check: Falls die neuen Grenzen weniger als 75% der Fläche haben als
      //die alten (die bis auf die Taskleiste korrekt seien sollten), dann
      //werden sie nicht genommen.
      if (newBounds.width * newBounds.height >= 0.75*maxWindowBounds.width*maxWindowBounds.height)
        maxWindowBounds = newBounds; 
    }
    myFrame.setExtendedState(Frame.NORMAL);
  }
  
  private void createGUI3()
  {
    setFormGUISizeAndLocation();
    arrangeWindows();
  }

  /**
   * Setzt Größe und Ort der FormGUI.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void setFormGUISizeAndLocation()
  {
    Rectangle frameBounds = new Rectangle(myFrame.getBounds());
    
    switch (formGUIBounds.width)
    {
      case Common.DIMENSION_UNSPECIFIED: // natural width
        break;
      case Common.DIMENSION_MAX: // max
        frameBounds.width = maxWindowBounds.width;
        break;
      default: // specified width
        frameBounds.width = formGUIBounds.width;
        break;
    }
    
    switch (formGUIBounds.height)
    {
      case Common.DIMENSION_UNSPECIFIED: // natural height
        break;
      case Common.DIMENSION_MAX: // max
        frameBounds.height = maxWindowBounds.height;
        break;
      default: // specified height
        frameBounds.height = formGUIBounds.height;
        break;
    }
    
    switch (formGUIBounds.x)
    {
      case Common.COORDINATE_CENTER: // center
        frameBounds.x = maxWindowBounds.x + (maxWindowBounds.width-frameBounds.width)/2;
        break;
      case Common.COORDINATE_MAX: // max
        frameBounds.x = maxWindowBounds.x + maxWindowBounds.width - frameBounds.width;
        break;
      case Common.COORDINATE_MIN: // min
        frameBounds.x = maxWindowBounds.x;
        break;
      case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal berücksichtigt.
        frameBounds.x = formGUIBounds.x;
        formGUIBounds.x = Common.COORDINATE_UNSPECIFIED;
        break;
    }
    
    switch (formGUIBounds.y)
    {
      case Common.COORDINATE_CENTER: // center
        frameBounds.y = maxWindowBounds.y + (maxWindowBounds.height-frameBounds.height)/2;
        break;
      case Common.COORDINATE_MAX: // max
        frameBounds.y = maxWindowBounds.y + maxWindowBounds.height - frameBounds.height;
        break;
      case Common.COORDINATE_MIN: // min
        frameBounds.y = maxWindowBounds.y;
        break;
      case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal berücksichtigt.
        frameBounds.y = formGUIBounds.y;
        formGUIBounds.y = Common.COORDINATE_UNSPECIFIED;
        break;
    }
    
    /*
     * Workaround für Bug in Java: Standardmaessig werden die MaximumWindowBounds
     * nicht berücksichtigt beim ersten Layout (jedoch schon, wenn sich die
     * Taskleiste verändert).
     */
    if (frameBounds.y + frameBounds.height > maxWindowBounds.y + maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.y + maxWindowBounds.height - frameBounds.y;
    
    myFrame.setBounds(frameBounds);
    myFrame.validate(); //ohne diese wurde in Tests manchmal nicht neu gezeichnet
  }
  
  /**
   * Arrangiert das Writer Fenster so, dass es neben dem Formular-Fenster
   * sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void arrangeWindows()
  {
    Rectangle frameBounds = new Rectangle(myFrame.getBounds());
    Logger.debug("Maximum window bounds "+maxWindowBounds+"| window insets "+windowInsets+"| frame bounds "+frameBounds);
    
    /*
     * Das Addieren von windowInsets.left und windowInsets.right ist eine
     * Heuristik. Da sich setWindowPosSize() unter Windows und Linux anders
     * verhält, gibt es keine korrekte Methode (die mir bekannt ist), um die
     * richtige Ausrichtung zu berechnen.
     */
    int docX = frameBounds.x + frameBounds.width + windowInsets.left + windowInsets.right;
    int docWidth = maxWindowBounds.x + maxWindowBounds.width - docX;
    if (docWidth < 0) 
    {
      docX = maxWindowBounds.x;
      docWidth = maxWindowBounds.width;
    }
    int docY = maxWindowBounds.y + windowInsets.top;
    /*
     * Das Subtrahieren von 2*windowInsets.bottom ist ebenfalls eine Heuristik.
     * (siehe weiter oben)
     */
    int docHeight = maxWindowBounds.y + maxWindowBounds.height - docY - 2*windowInsets.bottom;
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
    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { closeAction.actionPerformed(null); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) 
    {
      //myDoc.setWindowVisible(true);
      arrangeWindows();
    }
    public void windowIconified(WindowEvent e) 
    { 
      //myDoc.setWindowVisible(false);
    }
    public void windowOpened(WindowEvent e) {}
    public void componentResized(ComponentEvent e)
    {
      arrangeWindows();
    }
    public void componentMoved(ComponentEvent e)
    {
      arrangeWindows();
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
  
  /**
   * Schliesst die FormGUI und alle zugehörigen Fenster.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{myFrame.dispose();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
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

    public void dispose()
    {
      Logger.log("Dispose()"); 
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

    ConfigThingy formFensterConf = new ConfigThingy("");
    try{
      formFensterConf = WollMuxFiles.getWollmuxConf().query("Fenster").query("Formular").getLastChild();
    }
    catch(Exception x) {}
    new FormGUI(formFensterConf, conf.get("Formular"), model, mapIdToPresetValue, functionContext, funcLib, dialogLib);
  }


}
