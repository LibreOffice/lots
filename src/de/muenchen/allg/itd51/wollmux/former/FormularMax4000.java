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
* 08.08.2006 | BNK | Viel Arbeit reingesteckt.
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.FormDescriptor;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.TextRange;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Visitor;

/**
 * Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormularMax4000
{
  private static final int GENERATED_LABEL_MAXLENGTH = 30;
  
  /**
   * URL des Quelltexts für den Standard-Empfängerauswahl-Tab.
   */
  private final URL EMPFAENGER_TAB_URL = this.getClass().getClassLoader().getResource("data/empfaengerauswahl_controls.conf");
  
  //TODO MAGIC_DESCRIPTOR_PATTERN in FormularMax 4000 Doku dokumentieren
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN = Pattern.compile("\\A(.*)<<(.*)>>\\z");

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
  
  private String formTitle = "Generiert durch FormularMax 4000";
  
  private FormControlModelList formControlModelList = new FormControlModelList();
  
  private FormDescriptor formDescriptor;
  
  public FormularMax4000(final XTextDocument doc)
  {
    formDescriptor = new FormDescriptor(doc);
    init();
    
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
  
  private void createGUI(final XTextDocument doc)
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
    JMenuBar mbar = new JMenuBar();
    
    //========================= Datei ============================
    JMenu menu = new JMenu("Datei");
    JMenuItem menuItem = new JMenuItem("Formularfelder aus Dokument einlesen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        scan(doc);
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formularbeschreibung ins Dokument übertragen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        writeFormDescriptor();
      }});
    menu.add(menuItem);
    
    mbar.add(menu);
//  ========================= Formular ============================
    menu = new JMenu("Formular");
    menuItem = new JMenuItem("Empfängerauswahl-Tab einfügen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardEmpfaengerauswahl();
      }
      });
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formularbeschreibung editieren");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }});
    menu.add(menuItem);

    
    mbar.add(menu);
    
    
    
    myFrame.setJMenuBar(mbar);
    
    myFrame.pack();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Wertet {@link #formDescriptor} aus aus und initialisiert alle internen
   * Strukturen entsprechend.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void init()
  {
    formControlModelList.clear();
    ConfigThingy conf = formDescriptor.toConfigThingy();
    parseGlobalFormInfo(conf);
    
    ConfigThingy fensterAbschnitte = conf.query("Formular").query("Fenster");
    Iterator fensterAbschnittIterator = fensterAbschnitte.iterator();
    while (fensterAbschnittIterator.hasNext())
    {
      ConfigThingy fensterAbschnitt = (ConfigThingy)fensterAbschnittIterator.next();
      Iterator tabIter = fensterAbschnitt.iterator();
      while (tabIter.hasNext())
      {
        ConfigThingy tab = (ConfigThingy)tabIter.next();
        parseTab(tab, -1);
      }
    }
//  TODO writeFormDescriptor();
  }
  
  /**
   * Speichert die Formularbeschreibung in einem Benutzerfeld der DocumentInfo.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void writeFormDescriptor()
  {
    ConfigThingy conf = exportFormDescriptor();
    formDescriptor.fromConfigThingy(conf);
    formDescriptor.writeDocInfoFormularbeschreibung();
  }

  /**
   * Liefert ein ConfigThingy zurück, das den aktuellen Zustand der Formularbeschreibung
   * repräsentiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy exportFormDescriptor()
  {
    ConfigThingy conf = new ConfigThingy("WM");
    ConfigThingy form = conf.add("Formular");
    form.add("TITLE").add(formTitle);
    form.addChild(formControlModelList.export());
    return conf;
  }
  
  /**
   * Extrahiert aus conf die globalen Eingenschaften des Formulars wie z,B, den Formulartitel.
   * @param conf der WM-Knoten der über einer beliebigen Anzahl von Formular-Knoten sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void parseGlobalFormInfo(ConfigThingy conf)
  {
    ConfigThingy tempConf = conf.query("Formular").query("TITLE");
    if (tempConf.count() > 0) formTitle = tempConf.toString();
  }
  
  /**
   * Fügt am Anfang der Liste eine Standard-Empfaengerauswahl-Tab ein.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardEmpfaengerauswahl()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Empfaengerauswahl", EMPFAENGER_TAB_URL);
      int rand = (int)(Math.random()*100); 
      FormControlModel separatorTab = FormControlModel.createTab("Reiter "+rand, "Reiter"+rand);
      formControlModelList.add(separatorTab,0);
      parseTab(conf, 0);
      //TODO writeFormDescriptor();
    }catch(Exception x) {}
  }
  
  /**
   * Parst das Tab conf und fügt entsprechende FormControlModels der 
   * {@link #formControlModelList} hinzu.
   * @param conf der Knoten direkt über "Eingabefelder" und "Buttons".
   * @param idx falls >= 0 werden die Steuerelemente am entsprechenden Index der
   *        Liste in die Formularbeschreibung eingefügt, ansonsten ans Ende angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void parseTab(ConfigThingy conf, int idx)
  {
    String id = conf.getName();
    String label = id;
    String action = FormControlModel.NO_ACTION;
    String tooltip = "";
    char hotkey = 0;
    
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = (ConfigThingy)iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("TITLE")) label = str; 
      else if (name.equals("CLOSEACTION")) action = str;
      else if (name.equals("TIP")) tooltip = str;
      else if (name.equals("HOTKEY")) hotkey = str.length() > 0 ? str.charAt(0) : 0;
    }
    
    FormControlModel tab = FormControlModel.createTab(label, id);
    tab.setAction(action);
    tab.setTooltip(tooltip);
    tab.setHotkey(hotkey);
    
    if (idx >= 0)
    {
      formControlModelList.add(tab, idx++);
      idx += parseGrandchildren(conf.query("Eingabefelder"), idx);
      parseGrandchildren(conf.query("Buttons"), idx);
    }
    else
    {
      formControlModelList.add(tab);
      parseGrandchildren(conf.query("Eingabefelder"), -1);
      parseGrandchildren(conf.query("Buttons"), -1);
    }
    
    
    
  }
  
  /**
   * Parst die Kinder der Kinder von grandma als Steuerelemente und fügt der
   * {@link #formControlModelList} entsprechende FormControlModels hinzu.
   * @param idx falls >= 0 werden die Steuerelemente am entsprechenden Index der
   *        Liste in die Formularbeschreibung eingefügt, ansonsten ans Ende angehängt.
   * @return die Anzahl der erzeugten Steuerelemente.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private int parseGrandchildren(ConfigThingy grandma, int idx)
  {
    int count = 0;
    Iterator grandmaIter = grandma.iterator();
    while (grandmaIter.hasNext())
    {
      Iterator iter = ((ConfigThingy)grandmaIter.next()).iterator();
      while (iter.hasNext())
      {
        FormControlModel model = new FormControlModel((ConfigThingy)iter.next());
        ++count;
        if (idx >= 0)
          formControlModelList.add(model, idx++);
        else
          formControlModelList.add(model);
      }
    }
    return count;
  }
  
  
  
  private void scan(XTextDocument doc)
  {
    try{
      DocumentTree tree = new DocumentTree(doc);
      Visitor visitor = new DocumentTree.Visitor(){
        private Map insertions = new HashMap();
        private StringBuilder text = new StringBuilder();
        
        public boolean container(int count)
        {
          text.setLength(0);
          return true;
        }
        
        public boolean textRange(TextRange textRange)
        {
          text.append(textRange.getString());
          return true;
        }
        
        public boolean insertionBookmark(InsertionBookmark bookmark)
        {
          if (bookmark.isStart())
            insertions.put(bookmark.getName(), bookmark);
          else
            insertions.remove(bookmark.getName());
          
          return true;
        }
        
        public boolean formControl(FormControl control)
        {
          if (insertions.isEmpty())
            registerFormControl(control, text);
          
          return true;
        }
      };
      visitor.visit(tree);
    } 
    catch(Exception x) {Logger.error("Fehler während des Scan-Vorgangs",x);}
  }
  
  //text: Text der im selben Absatz wie das Control vor dem Control steht.
  private void registerFormControl(FormControl control, StringBuilder text)
  {
    String label;
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1);
      id = m.group(2);
    }
    else
    {
      int len = text.length();
      if (len > GENERATED_LABEL_MAXLENGTH) len = GENERATED_LABEL_MAXLENGTH;
      label = text.substring(text.length() - len);
      if (label.length() < 2) label = "Eingabe";
      id = descriptor;
    }
    
    id = makeControlId(label, id);
    
    switch (control.getType())
    {
      case DocumentTree.CHECKBOX_CONTROL: registerCheckbox(control, label, id); break;
      case DocumentTree.DROPDOWN_CONTROL: registerDropdown((DropdownFormControl)control, label, id); break;
      case DocumentTree.INPUT_CONTROL:    registerInput(control, label, id); break;
      default: Logger.error("Unbekannter Typ Formular-Steuerelement");
    }
  }
  
  private void registerCheckbox(FormControl control, String label, String id)
  {
    if (label.length() > 0) //falls label == "" handelt es sich um eine reine Einfügestelle
    {
      FormControlModel model = FormControlModel.createCheckbox(label, id);
      formControlModelList.add(model);
    }
    
    control.surroundWithBookmark(insertFormValue(id));
  }
  
  private void registerDropdown(DropdownFormControl control, String label, String id)
  {
    if (label.length() > 0) //falls label == "" handelt es sich um eine reine Einfügestelle
    {
      FormControlModel model = FormControlModel.createComboBox(label, id, control.getItems());
      formControlModelList.add(model);
    }
    
    control.surroundWithBookmark(insertFormValue(id));
  }
  
  private void registerInput(FormControl control, String label, String id)
  {
    if (label.length() > 0) //falls label == "" handelt es sich um eine reine Einfügestelle
    {
      FormControlModel model = FormControlModel.createTextfield(label, id);
      formControlModelList.add(model);
    }
    
    control.surroundWithBookmark(insertFormValue(id));
  }
  
  /**
   * Macht aus str einen passenden Bezeichner für ein Steuerelement. Falls label == "", so
   * muss der Bezeichner nicht eindeutig sein (dies ist der Marker für eine reine
   * Einfügestelle, für die kein Steuerelement erzeugt werden muss).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private String makeControlId(String label, String str)
  {
    str = str.replaceAll("[^a-zA-Z_0-9]","");
    if (str.length() == 0) str = "Steuerelement";
    if (!str.matches("^[a-zA-Z_].*")) str = "_" + str;
    if (label.length() > 0)
      return formControlModelList.makeUniqueId(str);
    else
      return str;
  }

  private void editFormDescriptor()
  {
    final JFrame editorFrame = new JFrame("Formularbeschreibung bearbeiten");
    editorFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    final JEditorPane editor = new JEditorPane("text/plain", exportFormDescriptor().stringRepresentation());
    JScrollPane scrollPane = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    editorFrame.setContentPane(scrollPane);
    editorFrame.addWindowListener(new WindowAdapter()
        {
          ConfigThingy conf;
          public void windowClosing(WindowEvent e) 
          {
            try
            {
              conf = new ConfigThingy("", null, new StringReader(editor.getText()));
              editorFrame.dispose();
            }
            catch (Exception e1)
            {
              JOptionPane.showMessageDialog(editorFrame, e1.getMessage(), "Fehler beim Parsen der Formularbeschreibung", JOptionPane.WARNING_MESSAGE);
            }
          }
          public void windowClosed(WindowEvent e)
          {
            formDescriptor.fromConfigThingy(conf);
            init();
          }
        });
    
    
    editorFrame.pack();
    editorFrame.setVisible(true);
  }
  
  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD'insertFormValue' ID '"+id+"')";
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
