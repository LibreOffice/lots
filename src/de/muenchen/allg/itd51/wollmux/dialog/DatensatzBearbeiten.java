/*
* Dateiname: DatensatzBearbeiten.java
* Projekt  : WollMux
* Funktion : Dynamisches Erzeugen eines Swing-GUIs für das Bearbeiten eines Datensatzes anhand von ConfigThingy
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.10.2005 | BNK | Erstellung
* 14.10.2005 | BNK | Interaktion mit DJDataset
* 14.10.2005 | BNK | Kommentiert
* 17.10.2005 | BNK | Unterstützung für immer ausgegraute Buttons.
* 17.10.2005 | BNK | Unterstützung für READONLY
* 18.10.2005 | BNK | Zusätzliche Exceptions loggen
* 24.10.2005 | BNK | dialogEndListener wird am Ende aufgerufen
*                  | show() entfernt zur Vermeidung von Thread-Problemen
* 24.10.2005 | BNK | restoreStandard() Buttons nicht mehr ausgegraut, wenn 
*                  | Werte nicht geändert wurden, aber bereits aus dem LOS sind.
* 27.10.2005 | BNK | back + CLOSEACTION
* 02.11.2005 | BNK | +saveAndBack()
* 15.11.2005 | BNK | Endlosschleife beseitigt durch vertauschen der || Operanden
* 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
* 11.01.2006 | BNK | EDIT "true" bei comboboxen unterstützt
* 25.01.2006 | BNK | Auch editierbare Comboboxen ändern nun den Hintergrund korrekt.
* 19.04.2006 | BNK | [R1337]Fehlermeldung, bei unbekanntem TYPE
* 15.05.2006 | BNK | nicht-editierbare Comboboxen funktionieren jetzt hoffentlich 
*                  | richtig mit Vorgabewerten, die nicht in der Liste sind.
* 29.09.2006 | BNK | Verbessertes Auslesen von ComboBox-Daten                 
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.NoBackingStoreException;
import de.muenchen.allg.itd51.wollmux.db.TestDJDataset;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen 
 * Dialogbeschreibung einen (mehrseitigen) Dialog zur Bearbeitung eines
 * {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}s.
 * <b>ACHTUNG:</b> Die private-Funktionen
 * dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatensatzBearbeiten
{
  /**
   * Standardbreite für Textfelder
   */
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  /**
   * Rand über und unter einem horizontalen Separator (in Pixeln).
   */
  private final static int SEP_BORDER = 7;
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;
  /**
   * der Datensatz, der durch den Dialog bearbeitet wird.
   */
  private DJDataset datensatz;
  /**
   * Bildet Fensternamen (umschliessender Schlüssel in der Beschreibungssprache)
   * auf {@link DialogWindow}s ab. Wird unter anderem zum Auflösen der Bezeichner
   * der switchTo-ACTION verwendet.
   */
  private Map fenster;
  /**
   * das momentan angezeigte Dialogfenster.
   */
  private DialogWindow currentWindow;
  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;
  /**
   * Ein mit CardLayout versehenes Panel, das die verschiedenen Dialogseiten
   * managt.
   */
  private JPanel cardPanel;
  /**
   * Das CardLayout von cardPanel.
   */
  private CardLayout cardLayout;
  /**
   * Der Name (siehe {@link #fenster}) des ersten Fensters des Dialogs,
   * das ist das erste Fenster, das in der Dialog-Beschreibung aufgeführt ist.
   */
  private String firstWindow;
  /**
   * Die mit MODIFY_MARKER_COLOR gesetzte Farbe.
   */
  private Color modColor;
  
  /**
   * ActionListener für Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
        { public void actionPerformed(ActionEvent e){ abort(); } };
 
        /**
         * ActionListener für Buttons mit der ACTION "back".
         */
        private ActionListener actionListener_back = new ActionListener()
          { public void actionPerformed(ActionEvent e) { back(); } };
          
        /**
         * ActionListener für Buttons mit der ACTION "restoreStandard". 
         */        
  private ActionListener actionListener_restoreStandard = new ActionListener()
        { public void actionPerformed(ActionEvent e){ restoreStandard(); } };
        /**
         * ActionListener für Buttons mit der ACTION "save". 
         */
  private ActionListener actionListener_save = new ActionListener()
        { public void actionPerformed(ActionEvent e){ save(); } };
        /**
         * ActionListener für Buttons mit der ACTION "saveAndExit". 
         */
  private ActionListener actionListener_saveAndExit = new ActionListener()
        { public void actionPerformed(ActionEvent e){ saveAndExit(); } };
 
   /**
    * ActionListener für Buttons mit der ACTION "saveAndExit". 
    */
   private ActionListener actionListener_saveAndBack = new ActionListener()
     { public void actionPerformed(ActionEvent e){ saveAndBack(); } };

  
  /**
  * wir bei Ende des Dialogs aufgerufen wenn nicht null (siehe Konstruktor).
  */
  private ActionListener dialogEndListener = null;
  
  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
    
  /**
   * Erzeugt einen neuen Dialog und zeigt ihn an.
   * @param conf das ConfigThingy, das den Dialog beschreibt (der Vater des
   *        "Fenster"-Knotens.
   * @param datensatz der Datensatz, der mit dem Dialog bearbeitet werden soll.
   * @param dialogEndListener falls nicht null, wird 
   *        die {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *        Methode wird aufgerufen (im Event Dispatching Thread), 
   *        nachdem der Dialog geschlossen wurde.
   *        Das actionCommand des ActionEvents gibt die Aktion an, die
   *        das Speichern des Dialogs veranlasst hat.
   * @throws ConfigurationErrorException im Falle eines schwerwiegenden
   *         Konfigurationsfehlers, der es dem Dialog unmöglich macht,
   *         zu funktionieren (z.B. dass der "Fenster" Schlüssel fehlt.
   */
  public DatensatzBearbeiten(ConfigThingy conf, DJDataset datensatz, ActionListener dialogEndListener) throws ConfigurationErrorException
  {
    this.datensatz = datensatz;
    this.dialogEndListener = dialogEndListener;
    
    fenster = new HashMap();
    
    modColor = Color.PINK;
    try{
      modColor = Color.decode(conf.get("MODIFY_MARKER_COLOR").getLastChild().toString());
    }catch(Exception x){Logger.error(x);}
    
    final ConfigThingy fensterDesc = conf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI(fensterDesc.getLastChild());}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  /**
   * Wie {@link #DatensatzBearbeiten(ConfigThingy, DJDataset, ActionListener)}
   * mit null als dialogEndListener.
   * @param conf
   * @param datensatz
   * @throws ConfigurationErrorException
   */
  public DatensatzBearbeiten(ConfigThingy conf, DJDataset datensatz) throws ConfigurationErrorException
  {
    this(conf, datensatz, null);
  }
  
  private void createGUI(ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();
    
    //Create and set up the window.
    myFrame = new JFrame("Absenderdaten bearbeiten");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    
    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);
    cardPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.getContentPane().add(cardPanel);
    
    Iterator iter = fensterDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = (ConfigThingy)iter.next();
      String fensterName = neuesFenster.getName();
      DialogWindow newWindow = new DialogWindow(fensterName, neuesFenster);
      if (firstWindow == null) firstWindow = fensterName;
      fenster.put(fensterName,newWindow);
      cardPanel.add(newWindow.JPanel(),fensterName);
    }
    
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
    showEDT();
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }
  
  /**
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf
   * wobei das gegebene actionCommand übergeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this,0,actionCommand));
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void restoreStandard()
  {
    if (!datensatz.hasBackingStore() || !currentWindow.hasLocalValues()) return;
    int res = JOptionPane.showConfirmDialog(myFrame, "Wollen Sie Ihre persönlichen Änderungen wirklich verwerfen\nund die Felder dieser Dialogseite wieder mit der zentralen Datenbank synchronisieren?","Lokale Änderungen wirklich verwerfen?",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (res != JOptionPane.YES_OPTION) return;
    currentWindow.restoreStandard();
  };

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private boolean save() 
  {
    boolean hasChanges = false;
    Iterator iter = fenster.values().iterator();
    while (iter.hasNext())
      hasChanges = ((DialogWindow)iter.next()).hasChanges() || hasChanges;
    
    if (!hasChanges) return true;
    int res = JOptionPane.showConfirmDialog(myFrame, "Wollen Sie Ihre Änderungen wirklich speichern\nund auf die Aktualisierung der entsprechenden Felder\naus der zentralen Datenbank verzichten?","Änderungen speichern?",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (res != JOptionPane.YES_OPTION) return false;
    
    iter = fenster.values().iterator();
    while (iter.hasNext()) ((DialogWindow)iter.next()).save();
    return true; 
  };

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void saveAndExit()
  {
    if (save()) dialogEnd("saveAndExit");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void saveAndBack()
  {
    if (save()) dialogEnd("back");
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
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren
   * Aufrufe von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt
   * asynchron. Wurde dem Konstruktor ein entsprechender ActionListener
   * übergeben, so wird seine actionPerformed() Funktion aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    //  GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          abort();
        }
      });
    }
    catch(Exception x) {/*Hope for the best*/}
  }

  
  /**
   * Zeigt den Dialog an.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showEDT()
  {
    if (fenster.size() == 0) return;
    if (currentWindow == null) showWindow(firstWindow);
  }

  
  /**
   * aktiviert das Dialog-Fenster namens name.
   * @param name
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showWindow(String name)
  {
    if (!fenster.containsKey(name)) return;
    
    currentWindow = (DialogWindow)fenster.get(name);

    myFrame.setTitle(currentWindow.getTitle());
    myFrame.setVisible(true);
    cardLayout.show(cardPanel,currentWindow.getName());
    closeAction = currentWindow.getCloseAction();
    myFrame.requestFocus();
  }

  /**
   * Dieses Interface dient dazu, dass {@link DataControl}s jemandem
   * (nämlich {@link DialogWindow}s) mitteilen können, wenn sie ihre Farbe
   * geändert haben (weil sich ihr "geändert" Zustand geändert hat).
   * Dies erlaubt es den DialogWindows, Buttons zu aktualisieren, deren
   * Ausgegrautseinszustand davon abhängt, ob angezeigte Felder geändert wurden
   * oder nicht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private interface ColorChangeListener
  {
    public void colorChanged();
  }
  
  /**
   * Ein DataControl kümmert sich um die Verwaltung von Eingabe-Controls eines
   * DialogWindows. Es stellt insbesondere die Schnittstelle zwischen dem
   * Control und dem Datensatz her.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private abstract class DataControl
  {
    /**
     * der Text des Datenbankfeldes dieses Controls. Wenn startText nicht mit
     * dem aktuell im Control angezeigten Text übereinstimmt, dann hat der
     * Benutzer den Wert seit den letzten Save geändert.
     */
    protected String startText;
    /**
     * Der Name der Datenbankspalte, die mit dem Control editiert wird.
     */
    protected String columnName;
    
    /**
     * Cacht den Wert von DJDataset.hasLocalOverride(columnName).
     */
    protected boolean myDatasetIsLocal;
    /**
     * die Farbe, die der Hintergrund des Controls annehmen soll, wenn
     * {@link #myDatasetIsLocal} true ist.
     */
    protected Color localColor;
    /**
     * die Farbe, die der Hintegrund des Controls annehmen soll,  wenn
     * {@link #myDatasetIsLocal} false ist.
     */ 
    protected Color normalColor;
    /**
     * Das Control.
     */
    protected JComponent myComponent;
    /**
     * true, falls der Hintegrund des Controls aktuell in normalColor eingefärbt ist.
     */
    boolean isCurrentlyNormalColor;
    List listeners = new Vector();
    
    public void initCompo(String colName, JComponent compo, Color localColor)
    {
      this.localColor = localColor;
      columnName = colName;
      myComponent = compo;
      normalColor = myComponent.getBackground();
      isCurrentlyNormalColor = true;
    }
    
    public void initText() throws ColumnNotFoundException
    {
      myDatasetIsLocal = datensatz.hasLocalOverride(columnName);
      startText = datensatz.get(columnName);
      if (startText == null) startText = "";
      setTextInControl(startText);
      updateBackground();
    }
    
    public void addColorChangeListener(ColorChangeListener l)
    {
      if (!listeners.contains(l)) listeners.add(l);
    }
    
    public void notifyColorChangeListeners()
    {
      Iterator iter = listeners.iterator();
      while (iter.hasNext()) ((ColorChangeListener)iter.next()).colorChanged();
    }
    
    public void setBackground(Color c)
    {
      myComponent.setBackground(c);
    }
    public abstract String getTextFromControl();
    public String getColumnName() {return columnName;}
    public abstract void setTextInControl(String text);
    public boolean hasBeenModified() {return !startText.equals(getTextFromControl());}
    public boolean datasetIsLocal() {return myDatasetIsLocal;}
    public void updateBackground()
    {
      if (datasetIsLocal() || hasBeenModified())
      {
        setBackground(localColor);
        if (isCurrentlyNormalColor)
        {
          isCurrentlyNormalColor = false;
          notifyColorChangeListeners();
        }
      }
      else
      {
        setBackground(normalColor);
        if (!isCurrentlyNormalColor)
        {
          isCurrentlyNormalColor = true;
          notifyColorChangeListeners();
        }
      }
    }
    
    /**
     * Falls hasBeenModified() wird der aktuell im Control stehende Wert
     * in die Datenbank zurückgespeichert.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void save()
    {
      if (hasBeenModified())
      {
        try{ 
          String text = getTextFromControl();
          datensatz.set(columnName, text);
          startText = text;
          myDatasetIsLocal = datensatz.hasLocalOverride(columnName);
        }catch(ColumnNotFoundException x){}
        updateBackground();
      }
    }
    
    /**
     * Der Wert des Controls wird aus der Datenbank aktualisiert und
     * daran gekoppelt. D.h. lokale Änderungen werden verworfen. 
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void restoreStandard()
    {
      try{ 
        datensatz.discardLocalOverride(columnName);
        initText();
      }catch(ColumnNotFoundException x){}
       catch(NoBackingStoreException x)
       {
         Logger.error("Es hätte nie passieren dürfen, aber restoreStandard() wurde für einen Datensatz ohne Backing Store aufgerufen!");
       }
      updateBackground();
    }
      
  }
  
  /**
   * Ein {@link DataControl} für JTextComponents.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class TextComponentDataControl extends DataControl implements DocumentListener
  {
    public TextComponentDataControl(String colName, JTextComponent compo, Color localColor)
    throws ColumnNotFoundException
    {
      initCompo(colName, compo, localColor);
      initText();
      compo.getDocument().addDocumentListener(this);
    }

    public String getTextFromControl()
    {
      return ((JTextComponent)myComponent).getText();
    }

    public void setTextInControl(String text)
    {
      ((JTextComponent)myComponent).setText(text);
    }

    public void changedUpdate(DocumentEvent e) { updateBackground(); }
    public void insertUpdate(DocumentEvent e) { updateBackground(); }
    public void removeUpdate(DocumentEvent e) { updateBackground(); }
    
  }
  
/**
 * Ein {@link DataControl} für JComboBoxes.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
  private class ComboBoxDataControl extends DataControl implements ActionListener, ItemListener 
  {
    public ComboBoxDataControl(String colName, JComboBox compo, Color localColor)
    throws ColumnNotFoundException
    {
      initCompo(colName, compo, localColor);
      addItem(datensatz.get(columnName));
      initText();
      
      compo.getEditor().addActionListener(this);
      compo.addItemListener(this);
    }

    public void setBackground(Color c)
    {
      super.setBackground(c);
      ((JComboBox)myComponent).getEditor().getEditorComponent().setBackground(c);
    }
    
    public void addItem(String text)
    {
      if (text == null) text = "";
      for (int i = ((JComboBox)myComponent).getItemCount() - 1; i >=0 ; --i)
      {
        if (((JComboBox)myComponent).getItemAt(i).equals(text)) return;
      }
      ((JComboBox)myComponent).addItem(text);
    }
    
    public String getTextFromControl()
    {
      JComboBox combo = (JComboBox)myComponent;
      if (combo.isEditable())
      {
        Document comboDoc = ((JTextComponent)combo.getEditor().getEditorComponent()).getDocument(); 
        try{
          return comboDoc.getText(0,comboDoc.getLength());
        }catch(BadLocationException x)
        {
          Logger.error(x);
          return "";
        }
      } else
      {
        Object selected = combo.getSelectedItem();
        return selected == null ? "" : selected.toString();
      }
    }

    public void setTextInControl(String text)
    {
      JComboBox myBox = (JComboBox)myComponent;
      boolean edit = myBox.isEditable();
      myBox.setEditable(true);
      myBox.setSelectedItem(text);
      myBox.setEditable(edit);
    }
    
    public void actionPerformed(ActionEvent e) { updateBackground(); }
    public void itemStateChanged(ItemEvent e) { updateBackground(); }
  }
  
  /**
   * Verwaltet eine Seite des Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DialogWindow implements ColorChangeListener
  {
    /**
     * Das Panel, das die ganze Dialogseite enthält.
     */
    private JPanel myPanel;
    /**
     * Unter-Panel, das die ganzen Eingabe-Elemente enthält. 
     */
    private JPanel myInputPanel;
    /**
     * Unter-Panel, das die Buttons enthält.
     */
    private JPanel myButtonPanel;
    /**
     * Titel, den der Frame bekommen soll, wenn diese Dialogseite angezeigt wird.
     */
    private String title;
    /**
     * Der Name dieses Fensters (vergleiche {@link DatensatzBearbeiten#fenster}).
     */
    private String name;
    /**
     * Alle {@link DataControl}s zu dieser Dialogseite. Falls die Konfigurationsdaten
     * des Dialogs fehlerhaft sind kann es sein, dass nicht zu jedem Control ein
     * DataControl existiert.
     */
    private List dataControls = new Vector();
    /**
     * Liste aller JButtons, die ausgegraut werden müssen, wenn keines der
     * DataControls einen hasBeenModified() Zustand hat.
     */
    private List buttonsToGreyOutIfNoChanges = new Vector();
    
    private ActionListener dialogWindowCloseAction = actionListener_abort;
    
    /**
     * Liefert das JPanel für diese Dialogseite zurück.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public JPanel JPanel() {return myPanel;}
    /**
     * Liefert den Titel zurück, den der Frame haben soll, wenn diese
     * Dialogseite angezeigt wird.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getTitle() {return title;}
    
    /**
     * Erzeugt ein neues DialogWindow, dessen Name name ist 
     * (vergleiche {@link DatensatzBearbeiten#fenster}) und das seinen
     * Aufbau aus conf bezieht. conf sollte ein Kind des Knotens "Fenster"
     * aus der gesamten Dialogbeschreibung sein.
     */
    public DialogWindow(String name, final ConfigThingy conf)
    {
      this.name = name;
      createGUI(conf);
    }
    
    /**
     * liefert den Namen zurück, der dem Konstruktor übergeben wurde
     * (vergleiche {@link DatensatzBearbeiten#fenster}.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getName() {return name;}
    
    public ActionListener getCloseAction() {return this.dialogWindowCloseAction;}
    
    public void save()
    {
      Iterator iter = dataControls.iterator();
      while (iter.hasNext()) ((DataControl)iter.next()).save();
    }
    
    public boolean hasChanges()
    {
      Iterator iter = dataControls.iterator();
      while (iter.hasNext()) if (((DataControl)iter.next()).hasBeenModified()) return true;
      return false;
    }
    
    public boolean hasLocalValues()
    {
      Iterator iter = dataControls.iterator();
      while (iter.hasNext()) 
      {
        DataControl ctrl = (DataControl)iter.next();
        if (ctrl.datasetIsLocal() || ctrl.hasBeenModified()) return true;
      }
      return false;
    }
    
    public void colorChanged()
    {
      boolean enabled = hasLocalValues() && datensatz.hasBackingStore();
      Iterator iter = buttonsToGreyOutIfNoChanges.iterator();
      while (iter.hasNext()) ((JButton)iter.next()).setEnabled(enabled);
    }
    
    public void restoreStandard()
    {
      Iterator iter = dataControls.iterator();
      while (iter.hasNext()) ((DataControl)iter.next()).restoreStandard();
    }
    
/**
 * Ersetzt "%{SPALTENNAME}" in str durch den Wert der entsprechenden Spalte
 * im Datensatz, der durch den Dialog bearbeitet wird. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
    public String substituteVars(String str)
    {
      Pattern p = Pattern.compile("%\\{([a-zA-Z0-9]+)\\}");
      Matcher m = p.matcher(str);
      if (m.find())
      do{
        String spalte = m.group(1);
        String wert = spalte;
        try{
          String wert2 = datensatz.get(spalte);
          if (wert2 != null)
            wert = wert2.replaceAll("%","");
        } catch (ColumnNotFoundException e) { Logger.error(e); }
        str = str.substring(0,m.start())+wert+str.substring(m.end());
        m = p.matcher(str);
      }while(m.find());
      return str;
    }
    
    public void createGUI(ConfigThingy conf)
    {
      title = "TITLE fehlt in Fensterbeschreibung";
      try{title = substituteVars(""+conf.get("TITLE"));}catch(NodeNotFoundException x){}
      
      try{
        dialogWindowCloseAction = getAction(conf.get("CLOSEACTION").toString());
      } catch(Exception x){}
      
      myPanel = new JPanel(new BorderLayout());
      myInputPanel = new JPanel();
      myButtonPanel = new JPanel();
      
      myInputPanel.setLayout(new GridBagLayout());//new BoxLayout(myInputPanel, BoxLayout.PAGE_AXIS));
      myButtonPanel.setLayout(new BoxLayout(myButtonPanel, BoxLayout.LINE_AXIS));
      myButtonPanel.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,0,0));
      
      myPanel.add(myInputPanel, BorderLayout.CENTER);
      myPanel.add(myButtonPanel, BorderLayout.PAGE_END);
      
        //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
      GridBagConstraints gbcBottomglue= new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.PAGE_END,   GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcLabel     = new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcSeparator = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER,     GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0,0,0,10),0,0);
      GridBagConstraints gbcTextfield = new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcTextarea =  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcCombobox  = new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
      
      ConfigThingy felderParent = conf.query("Eingabefelder");
      int y = -1;
      
      Iterator piter = felderParent.iterator();
      while (piter.hasNext())
      {
        Iterator iter = ((ConfigThingy)piter.next()).iterator();
        while (iter.hasNext())
        {
          ++y;
          ConfigThingy uiElementDesc = (ConfigThingy)iter.next();
          try{
            
            
            /*
             * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
             * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
             * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
             * NOCH KONSISTENT IST!
             */
            
            boolean readonly = false;
            try{ if (uiElementDesc.get("READONLY").toString().equals("true")) readonly = true; }catch(NodeNotFoundException x){}
            String type = uiElementDesc.get("TYPE").toString();
            if (type.equals("textfield"))
            {
              JLabel label = new JLabel();
              label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabelLeft.gridy = y;
              myInputPanel.add(label, gbcLabelLeft);
              try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
              
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              JTextField tf = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
              tf.setEditable(!readonly);
              
              try
              {
                dataControls.add(new TextComponentDataControl(uiElementDesc.get("DB_SPALTE").toString(), tf, modColor));
              } catch (Exception x) { Logger.error(x); }
              
              //Font fnt = tf.getFont();
              //tf.setFont(fnt.deriveFont((float)14.0));
              //tf.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
              uiElement.add(tf);
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcTextfield.gridy = y;
              myInputPanel.add(uiElement, gbcTextfield);
            }
            else if (type.equals("textarea"))
            {
              JLabel label = new JLabel();
              label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabelLeft.gridy = y;
              myInputPanel.add(label, gbcLabelLeft);
              try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
              
              int lines = 3;
              try{ lines = Integer.parseInt(uiElementDesc.get("LINES").toString()); } catch(Exception x){}
              JTextArea textarea = new JTextArea(lines,TEXTFIELD_DEFAULT_WIDTH);
              textarea.setEditable(!readonly);
              textarea.setFont(new JTextField().getFont());
              
              try
              {
                dataControls.add(new TextComponentDataControl(uiElementDesc.get("DB_SPALTE").toString(), textarea, modColor));
              } catch (Exception x) { Logger.error(x); }
              
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              JScrollPane scrollPane = new JScrollPane(textarea);//, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_NEVER);
              scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
              scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
              uiElement.add(scrollPane);
              
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcTextarea.gridy = y;
              myInputPanel.add(uiElement, gbcTextarea);
            }
            else if (type.equals("separator"))
            {
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              uiElement.add(new JSeparator(SwingConstants.HORIZONTAL));
              uiElement.setBorder(BorderFactory.createEmptyBorder(SEP_BORDER,0,SEP_BORDER,0));
              gbcSeparator.gridy = y;
              myInputPanel.add(uiElement, gbcSeparator);
            }
            else if (type.equals("label"))
            {
              JLabel uiElement = new JLabel();
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabel.gridy = y;
              myInputPanel.add(uiElement, gbcLabel);
              uiElement.setText(uiElementDesc.get("LABEL").toString());
            }
            else if (type.equals("combobox"))
            {
              JLabel label = new JLabel();
              label.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcLabelLeft.gridy = y;
              myInputPanel.add(label, gbcLabelLeft);
              try{ label.setText(uiElementDesc.get("LABEL").toString()); } catch(Exception x){}
              
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              JComboBox combo = new JComboBox();
              combo.setEnabled(!readonly);
              boolean editable = false;
              try{ if (uiElementDesc.get("EDIT").toString().equals("true")) editable = true; }catch(NodeNotFoundException x){}
              combo.setEditable(editable);
              try
              {
                ComboBoxDataControl comboCtrl = new ComboBoxDataControl(uiElementDesc.get("DB_SPALTE").toString(), combo, modColor);
                Iterator values = uiElementDesc.get("VALUES").iterator();
                while (values.hasNext())
                {
                  comboCtrl.addItem(values.next().toString());
                }
                dataControls.add(comboCtrl);
              } catch (Exception x) { Logger.error(x); }
              
              uiElement.add(combo);
              uiElement.setBorder(BorderFactory.createEmptyBorder(TF_BORDER,0,TF_BORDER,0));
              gbcCombobox.gridy = y;
              myInputPanel.add(uiElement, gbcCombobox);
            }
            else
            {
              Logger.error("Ununterstützter TYPE für User Interface Element: "+type);
            }
          } catch(NodeNotFoundException x) {Logger.error(x);}
        }
      }
      
      ++y;
      gbcBottomglue.gridy = y;
      myInputPanel.add(Box.createGlue(), gbcBottomglue);
      
      ConfigThingy buttonParents = conf.query("Buttons");
      piter = buttonParents.iterator();
      boolean firstButton = true;
      while (piter.hasNext())
      {
        Iterator iter = ((ConfigThingy)piter.next()).iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiElementDesc = (ConfigThingy)iter.next();
          try{
            
            /*
             * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN,
             * DASS DER ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET()
             * UND EINER EVTL. DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION
             * NOCH KONSISTENT IST!
             */
            
            String type = uiElementDesc.get("TYPE").toString();
            if (type.equals("button"))
            {
              String action = "";
              try{
                action = uiElementDesc.get("ACTION").toString();
              }catch(NodeNotFoundException x){}
              
              String label  = uiElementDesc.get("LABEL").toString();
              
              char hotkey = 0;
              try{
                hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
              }catch(Exception x){}
              
              JButton button = new JButton(label);
              button.setMnemonic(hotkey);
              JPanel uiElement = new JPanel(new GridLayout(1,1));
              int left = BUTTON_BORDER;
              if (firstButton) {left = 0; firstButton = false;}
              int right = BUTTON_BORDER;
              if (!iter.hasNext()) right = 0;
              uiElement.setBorder(BorderFactory.createEmptyBorder(0,left,0,right));
              uiElement.add(button);
              myButtonPanel.add(uiElement);
              
              ActionListener actionL = getAction(action);
              if (actionL != null) 
                button.addActionListener(actionL);
              else
                button.setEnabled(false);
              
              if (action.equals("restoreStandard"))
              {
                buttonsToGreyOutIfNoChanges.add(button);
              } else
              if (action.equals("switchWindow"))
              {
                final String window = uiElementDesc.get("WINDOW").toString();
                button.addActionListener( new ActionListener()
                    { public void actionPerformed(ActionEvent e){ showWindow(window); }
                    });
              
                button.setEnabled(true);
              }
              
            }
            else if (type.equals("glue"))
            {
              try{
                int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE").toString());
                myButtonPanel.add(Box.createHorizontalStrut(minsize));
              }catch(Exception x){}
              myButtonPanel.add(Box.createHorizontalGlue());
            }
          } catch(NodeNotFoundException x) {Logger.error(x);}
        }
      }
      
      Iterator iter = dataControls.iterator();
      while (iter.hasNext()) ((DataControl)iter.next()).addColorChangeListener(this);
      colorChanged();

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
    else if (action.equals("back"))
    {
      return actionListener_back;
    }
    else if (action.equals("restoreStandard"))
    {
      return actionListener_restoreStandard;
    }
    else if (action.equals("save"))
    {
      return actionListener_save;
    }
    else if (action.equals("saveAndExit"))
    {
      return actionListener_saveAndExit;
    }
    else if (action.equals("saveAndBack"))
    {
      return actionListener_saveAndBack;
    }
    else if (action.equals("switchWindow"))
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
  
  public static void main(String[] args) throws Exception
  {
    String confFile = "testdata/AbsenderdatenBearbeiten.conf";
    DJDataset datensatz = new TestDJDataset();
    ConfigThingy conf = new ConfigThingy("",new URL(new File(System.getProperty("user.dir")).toURL(),confFile)); 
    DatensatzBearbeiten ab = new DatensatzBearbeiten(conf.get("AbsenderdatenBearbeiten"), datensatz);
    Thread.sleep(60000);
    ab.dispose();
  }
}
