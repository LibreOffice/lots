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
* 28.08.2006 | BNK | kommentiert
* 31.08.2006 | BNK | Code-Editor-Fenster wird jetzt in korrekter Größe dargestellt
*                  | Das Hauptfenster passt sein Größe an, wenn Steuerelemente dazukommen oder verschwinden
* 06.09.2006 | BNK | Hoch und Runterschieben funktionieren jetzt.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
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
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.sun.star.document.XDocumentInfo;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.FormDescriptor;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Container;
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
  public static final String STANDARD_TAB_NAME = "Reiter";

  /**
   * Regex für Test ob String mit Buchstabe oder Underscore beginnt.
   * ACHTUNG! Das .* am Ende ist notwendig, da String.matches() immer den
   * ganzen String testet.
   */
  private static final String STARTS_WITH_LETTER_RE = "^[a-zA-Z_].*";

  /**
   * Der Standard-Formulartitel, solange kein anderer gesetzt wird.
   */
  private static final String GENERATED_FORM_TITLE = "Generiert durch FormularMax 4000";

  /**
   * Maximale Anzahl Zeichen für ein automatisch generiertes Label.
   */
  private static final int GENERATED_LABEL_MAXLENGTH = 30;
  
  /**
   * Wird als Label gesetzt, falls kein sinnvolles Label automatisch generiert werden
   * konnte.
   */
  private static final String NO_LABEL = "";
  
  /**
   * Wird temporär als Label gesetzt, wenn kein Label benötigt wird, weil es sich nur um
   * eine Einfügestelle handelt, die nicht als Formularsteuerelement erfasst werden soll.
   */
  private static final String INSERTION_ONLY = "<<InsertionOnly>>";
  
  /**
   * URL des Quelltexts für den Standard-Empfängerauswahl-Tab.
   */
  private final URL EMPFAENGER_TAB_URL = this.getClass().getClassLoader().getResource("data/empfaengerauswahl_controls.conf");
  
  /**
   * URL des Quelltexts für die Standardbuttons für einen mittleren Tab.
   */
  private final URL STANDARD_BUTTONS_MIDDLE_URL = this.getClass().getClassLoader().getResource("data/standardbuttons_mitte.conf");
  
  /**
   * URL des Quelltexts für die Standardbuttons für den letzten Tab.
   */
  private final URL STANDARD_BUTTONS_LAST_URL = this.getClass().getClassLoader().getResource("data/standardbuttons_letztes.conf");
  
  /**
   * Beim Import neuer Formularfelder oder Checkboxen schaut der FormularMax4000 nach
   * speziellen Hinweisen/Namen/Einträgen, die diesem Muster entsprechen. 
   * Diese Zusatzinformationen werden herangezogen um Labels, IDs und andere Informationen zu
   * bestimmen.
   * 
   * Eingabefeld: Als "Hinweis" kann    "Label<<ID>>" angegeben werden und wird beim Import
   *              entsprechend berücksichtigt. Wird nur "<<ID>>" angegeben, so markiert das
   *              Eingabefeld eine reine Einfügestelle (insertValue oder insertContent) und
   *              beim Import wird dafür kein Formularsteuerelement erzeugt. Wird ID
   *              ein "glob:" vorangestellt, so wird gleich ein insertValue-Bookmark
   *              erstellt.
   * 
   * Eingabeliste/Dropdown: Als "Name" kann "Label<<ID>>" angegeben werden und wird beim
   *                        Import berücksichtigt.
   *                        Als Spezialeintrag in der Liste kann "<<Freitext>>" eingetragen werden
   *                        und signalisiert dem FM4000, dass die ComboBox im Formular
   *                        auch die Freitexteingabe erlauben soll.
   *                        Wie bei Eingabefeldern auch ist die Angabe "<<ID>>" ohne Label
   *                        möglich und signalisiert, dass es sich um eine reine Einfügestelle
   *                        handelt, die kein Formularelement erzeugen soll.
   * 
   * Checkbox: Bei Checkboxen kann als "Hilfetext" "Label<<ID>>" angegeben werden und wird
   *           beim Import entsprechend berücksichtigt.
   *           
   * Technischer Hinweis: Auf dieses Pattern getestet wird grundsätzlich der String, der von
   * {@link DocumentTree.FormControl#getDescriptor()} geliefert wird.
   * 
   */
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN = Pattern.compile("\\A(.*)<<(.*)>>\\z");
  
  /**
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen, dass
   * ein insertValue anstatt eines insertFormValue erzeugt werden soll.
   */
  private static final String GLOBAL_PREFIX = "glob:";

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
   * Falls nicht null wird dieser Listener aufgerufen nachdem der FM4000
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;
  
  /**
   * Das Haupt-Fenster des FormularMax4000.
   */
  private JFrame myFrame;
  
  
  /**
   * Das Fenster, das für das Bearbeiten des Quelltextes geöffnet wird.
   * FIXME: vielleicht besser im selben Frame öffnen, damit niemand aus versehen parallel viel Arbeit im normalen Fenster macht und diese dann verliert beim Schliessen des Code-Fensters.
   */
  JFrame editorFrame = null;
  
  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;
  
  /**
   * Das Dokument, an dem dieser FormularMax 4000 hängt.
   */
  private XTextDocument doc;
  
  /**
   * Verwaltet die FormControlModels dieses Formulars.
   */
  private FormControlModelList formControlModelList;
  
  /**
   * Verwaltet die {@link InsertionModel}s dieses Formulars.
   */
  private InsertionModelList insertionModelList;
  
  /**
   * Hält in einem Panel FormControlModelLineViews für alle 
   * {@link FormControlModel}s aus {@link #formControlModelList}. 
   */
  private AllFormControlModelLineViewsPanel allFormControlModelLineViewsPanel;
  
  /**
   * Wird verwendet für das Auslesen und Zurückspeichern der Formularbeschreibung.
   */
  private FormDescriptor formDescriptor;
  
  /**
   * Der globale Broadcast-Kanal wird für Nachrichten verwendet, die verschiedene permanente
   * Objekte erreichen müssen, die aber von (transienten) Objekten ausgehen, die mit diesen 
   * globalen Objekten
   * wegen des Ausuferns der Verbindungen nicht in einer Beziehung stehen sollen. Diese Liste
   * enthält alle {@link BroadcastListener}, die auf dem globalen Broadcast-Kanal horchen. 
   * Dies dürfen nur
   * permanente Objekte sein, d.h. Objekte deren Lebensdauer nicht vor Beenden des
   * FM4000 endet. 
   */
  private List broadcastListeners = new Vector();
  
  /**
   * Sendet die Nachricht b an alle Listener, die auf dem globalen Broadcast-Kanal registriert
   * sind.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public void broadcast(Broadcast b)
  {
    Iterator iter = broadcastListeners.iterator();
    while (iter.hasNext())
    {
      b.sendTo((BroadcastListener)iter.next());
    }
  }
  
  /**
   * listener wird über globale {@link Broadcast}s informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public void addBroadcastListener(BroadcastListener listener)
  {
    if (!broadcastListeners.contains(listener))
      broadcastListeners.add(listener);
  }
  
  /**
   * Startet eine Instanz des FormularMax 4000 für das Dokument doc.
   * @param abortListener (falls nicht null) wird aufgerufen, nachdem der FormularMax 4000 geschlossen wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000(XTextDocument doc, ActionListener abortListener)
  {
    this.doc = doc;
    this.abortListener = abortListener;
    initFormDescriptor(doc);
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI();}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  private void createGUI()
  {
    Common.setLookAndFeelOnce();
    
    
    formControlModelList = new FormControlModelList();
    insertionModelList = new InsertionModelList();
    
    //  Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    
    allFormControlModelLineViewsPanel = new AllFormControlModelLineViewsPanel(formControlModelList, this);
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(allFormControlModelLineViewsPanel.JComponent(), BorderLayout.CENTER);
    myFrame.getContentPane().add(contentPanel);
    
    JMenuBar mbar = new JMenuBar();
    
    //========================= Datei ============================
    JMenu menu = new JMenu("Datei");
    
    JMenuItem menuItem = new JMenuItem("Formularfelder aus Dokument einlesen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        scan(doc);
        setFrameSize();
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formulartitel setzen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        setFormTitle();
        setFrameSize();
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formularbeschreibung ins Dokument übertragen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        writeFormDescriptor(doc);
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("WollMux-Formularmerkmale aus Dokument entfernen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        //TODO deForm(doc); insertFrags und insertValues in Ruhe lassen, aber insertFormValue, setGroups, setType und Forumlarbeschreibungsnotiz entfernen
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formularbeschreibung editieren");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }});
    menu.add(menuItem);

    
    mbar.add(menu);
//  ========================= Einfügen ============================
    menu = new JMenu("Einfügen");
    menuItem = new JMenuItem("Empfängerauswahl-Tab");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardEmpfaengerauswahl();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen, <-Zurück, Weiter->");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsMiddle();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen, <-Zurück, PDF, Drucken");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsLast();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    
    mbar.add(menu);
    
    myFrame.setJMenuBar(mbar);
    

    initModelsAndViews();
    
    
    setFrameSize();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Wertet {@link #formDescriptor}, sowie die Bookmarks von {@link #doc} aus und initialisiert 
   * alle internen
   * Strukturen entsprechend. Dies aktualisiert auch die entsprechenden Views.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void initModelsAndViews()
  {
    //TODO Wenn TRAFOs unterstützt werden von InsertionModel, dann müssen die entsprechenden Aspekte der InsertionModels ebenfalls neu gesetzt werden
    
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
    
    /*
     * Immer mindestens 1 Tab in der Liste.
     */
    if (formControlModelList.isEmpty())
    {
      String id = formControlModelList.makeUniqueId(STANDARD_TAB_NAME);
      FormControlModel separatorTab = FormControlModel.createTab(id, id);
      formControlModelList.add(separatorTab,0);
    }
    
    insertionModelList.clear();
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc); 
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try{
        String bookmark = bookmarks[i];
        if (InsertionModel.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel(bookmark, bmSupp));
      }catch(Exception x)
      {
        Logger.error(x);
      }
    }
    
//  TODO writeFormDescriptor();
    setFrameSize();
  }
  
  /**
   * Initialisiert den formDescriptor mit den Formularbeschreibungsdaten des
   * Dokuments doc.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initFormDescriptor(XTextDocument doc)
  {
    formDescriptor = new FormDescriptor(doc);
  }
  
  /**
   * Bringt einen modalen Dialog zum Bearbeiten des Formulartitels.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFormTitle()
  {
    String newTitle = JOptionPane.showInputDialog(myFrame, "Bitte Formulartitel eingeben", formTitle);
    if (newTitle != null)
      formTitle = newTitle;
  }
  
  /**
   * Speichert die Formularbeschreibung im Dokument.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void writeFormDescriptor(XTextDocument doc)
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
      parseTab(conf, 0);
      //TODO writeFormDescriptor();
    }catch(Exception x) { Logger.error(x);}
  }
  
  /**
   * Hängt die Standardbuttons für einen mittleren Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsMiddle()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_MIDDLE_URL);
      int index = allFormControlModelLineViewsPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      //TODO writeFormDescriptor();
    }catch(Exception x) { Logger.error(x);}
  }
  
  /**
   * Hängt die Standardbuttons für den letzten Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsLast()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_LAST_URL);
      int index = allFormControlModelLineViewsPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      //TODO writeFormDescriptor();
    }catch(Exception x) { Logger.error(x);}
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
      idx += parseGrandchildren(conf.query("Eingabefelder"), idx, true);
      parseGrandchildren(conf.query("Buttons"), idx, false);
    }
    else
    {
      formControlModelList.add(tab);
      parseGrandchildren(conf.query("Eingabefelder"), -1, true);
      parseGrandchildren(conf.query("Buttons"), -1, false);
    }
  }
  
  /**
   * Parst die Kinder der Kinder von grandma als Steuerelemente und fügt der
   * {@link #formControlModelList} entsprechende FormControlModels hinzu.
   * @param idx falls >= 0 werden die Steuerelemente am entsprechenden Index der
   *        Liste in die Formularbeschreibung eingefügt, ansonsten ans Ende angehängt.
   * @param killLastGlue falls true wird das letzte Steuerelement entfernt, wenn es
   *        ein glue ist.
   * @return die Anzahl der erzeugten Steuerelemente.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private int parseGrandchildren(ConfigThingy grandma, int idx, boolean killLastGlue)
  {
    if (idx < 0) idx = formControlModelList.size();
    
    boolean lastIsGlue = false;
    FormControlModel model = null;
    int count = 0;
    Iterator grandmaIter = grandma.iterator();
    while (grandmaIter.hasNext())
    {
      Iterator iter = ((ConfigThingy)grandmaIter.next()).iterator();
      while (iter.hasNext())
      {
        model = new FormControlModel((ConfigThingy)iter.next());
        lastIsGlue = model.isGlue();
        ++count;
        formControlModelList.add(model, idx++);
      }
    }
    if (killLastGlue && lastIsGlue)
    {
      formControlModelList.remove(model);
      --count;
    }
    return count;
  }
  
  /**
   * Scannt das Dokument doc durch und erzeugt {@link FormControlModel}s für alle
   * Formularfelder, die noch kein umschließendes WollMux-Bookmark haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void scan(XTextDocument doc)
  {
    try{
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      try{
        String tit = ((String)UNO.getProperty(info,"Title")).trim();
        if (formTitle == GENERATED_FORM_TITLE && tit.length() > 0)
          formTitle = tit;
      }catch(Exception x){}
      DocumentTree tree = new DocumentTree(doc);
      Visitor visitor = new DocumentTree.Visitor(){
        private Map insertions = new HashMap();
        private StringBuilder text = new StringBuilder();
        private StringBuilder fixupText = new StringBuilder();
        private FormControlModel fixupCheckbox = null;
        
        private void fixup()
        {
          if (fixupCheckbox != null && fixupCheckbox.getLabel() == NO_LABEL)
          {
            fixupCheckbox.setLabel(makeLabelFromStartOf(fixupText, 2*GENERATED_LABEL_MAXLENGTH));
            fixupCheckbox = null;
          }
          fixupText.setLength(0);
        }
        
        public boolean container(Container container, int count)
        {
          fixup();
          
          if (container.getType() != DocumentTree.PARAGRAPH_TYPE) text.setLength(0);
          
          return true;
        }
        
        public boolean textRange(TextRange textRange)
        {
          String str = textRange.getString(); 
          text.append(str);
          fixupText.append(str);
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
          fixup();
          
          if (insertions.isEmpty())
          {
            FormControlModel model = registerFormControl(control, text);
            if (model != null && model.getType() == FormControlModel.CHECKBOX_TYPE)
              fixupCheckbox = model;
          }
          
          return true;
        }
      };
      visitor.visit(tree);
    } 
    catch(Exception x) {Logger.error("Fehler während des Scan-Vorgangs",x);}
  }
  
  /**
   * Fügt der {@link #formControlModelList} ein neues {@link FormControlModel} hinzu für
   * das {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl} control, 
   * wobei
   * text der Text sein sollte, der im Dokument vor control steht. Dieser Text wird zur
   * Generierung des Labels herangezogen. Es wird ebenfalls der 
   * {@link #insertionModelList} ein entsprechendes {@link InsertionModel} hinzugefügt.
   * Zusätzlich wird immer ein entsprechendes Bookmark um das Control herumgelegt, das
   * die Einfügestelle markiert. 
   * 
   * @return null, falls es sich bei dem Control nur um eine reine Einfügestelle
   *         handelt. In diesem Fall wird nur der {@link #insertionModelList}
   *         ein Element hinzugefügt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerFormControl(FormControl control, StringBuilder text)
  {
    String label;
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1).trim();
      if (label.length() == 0) label = INSERTION_ONLY; 
      id = m.group(2).trim();
    }
    else
    {
      label = makeLabelFromEndOf(text, GENERATED_LABEL_MAXLENGTH);
      id = descriptor;
    }
    
    id = makeControlId(label, id);
    
    FormControlModel model = null;
    
    if (label != INSERTION_ONLY)
    {
      switch (control.getType())
      {
        case DocumentTree.CHECKBOX_CONTROL: model = registerCheckbox(control, label, id); break;
        case DocumentTree.DROPDOWN_CONTROL: model = registerDropdown((DropdownFormControl)control, label, id); break;
        case DocumentTree.INPUT_CONTROL:    model = registerInput(control, label, id); break;
        default: Logger.error("Unbekannter Typ Formular-Steuerelement"); return null;
      }
    }
    
    String bookmarkName = insertFormValue(id);
    if (label == INSERTION_ONLY)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      }
    }
    
    bookmarkName = control.surroundWithBookmark(bookmarkName);

    try{
      InsertionModel imodel = new InsertionModel(bookmarkName, UNO.XBookmarksSupplier(doc));
      insertionModelList.add(imodel);
    }catch(Exception x)
    {
      Logger.error("Es wurde ein fehlerhaftes Bookmark generiert: \""+bookmarkName+"\"");
    }
    
    return model;
  }

  /**
   * Bastelt aus dem Ende des Textes text ein Label das maximal maxlen Zeichen lang ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromEndOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(str.length() - len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }
  
  /**
   * Bastelt aus dem Start des Textes text ein Label, das maximal maxlen Zeichen lang ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromStartOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(0, len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }
  
  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine Checkbox
   * hinzu und liefert es zurück.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerCheckbox(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    label = NO_LABEL; //immer fixUp-Text von hinter der Checkbox benutzen, weil meist bessere Ergebnisse
    model = FormControlModel.createCheckbox(label, id);
    if (control.getString().equalsIgnoreCase("true"))
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add("true");
      model.setAutofill(autofill);
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine Auswahlliste
   * hinzu und liefert es zurück.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerDropdown(DropdownFormControl control, String label, String id)
  {
    FormControlModel model = null;
    String[] items = control.getItems();
    boolean editable = false;
    for (int i = 0; i < items.length; ++i)
    {
      if (items[i].equalsIgnoreCase("<<Freitext>>")) 
      {
        String[] newItems = new String[items.length - 1];
        System.arraycopy(items, 0, newItems, 0, i);
        System.arraycopy(items, i + 1, newItems, i, items.length - i - 1);
        items = newItems;
        editable = true;
        break;
      }
    }
    model = FormControlModel.createComboBox(label, id, items);
    model.setEditable(editable);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(autofill);
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für ein Eingabefeld
   * hinzu und liefert es zurück.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerInput(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createTextfield(label, id);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(autofill);
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * Macht aus str einen passenden Bezeichner für ein Steuerelement. Falls 
   * label == {@link #INSERTION_ONLY}, so
   * muss der Bezeichner nicht eindeutig sein (dies ist der Marker für eine reine
   * Einfügestelle, für die kein Steuerelement erzeugt werden muss).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeControlId(String label, String str)
  {
    if (label == INSERTION_ONLY)
    {
      boolean glob = str.startsWith(GLOBAL_PREFIX);
      if (glob) str = str.substring(GLOBAL_PREFIX.length());
      str = str.replaceAll("[^a-zA-Z_0-9]","");
      if (str.length() == 0) str = "Einfuegung";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      if (glob) str = GLOBAL_PREFIX + str;
      return str;
    }
    else
    {
      str = str.replaceAll("[^a-zA-Z_0-9]","");
      if (str.length() == 0) str = "Steuerelement";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return formControlModelList.makeUniqueId(str);
    }
  }

  private static class NoWrapEditorKit extends DefaultEditorKit
  {
    private static final long serialVersionUID = -2741454443147376514L;
    private ViewFactory vf = null;

    public ViewFactory getViewFactory()
    {
      if (vf == null) vf=new NoWrapFactory();
      return vf;
    };

    private class NoWrapFactory implements ViewFactory
    {
      public View create(Element e)
      {
        return new PlainView(e);
      }
   
    };
  };

  
  /**
   * Öffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des Fensters
   * wird die geänderte Formularbeschreibung neu geparst, falls sie syntaktisch korrekt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editFormDescriptor()
  {
    editorFrame = new JFrame("Formularbeschreibung bearbeiten");
    editorFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    
    final JEditorPane editor=new JEditorPane("text/plain","");
    editor.setEditorKit(new NoWrapEditorKit());
    editor.setText(exportFormDescriptor().stringRepresentation());
    editor.setFont(new Font("Monospaced",Font.PLAIN,editor.getFont().getSize()+2));
    JScrollPane scrollPane = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(scrollPane, BorderLayout.CENTER);
    editor.setCaretPosition(0);
    editorFrame.getContentPane().add(contentPanel);
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
            editorFrame = null;
            formDescriptor.fromConfigThingy(conf);
            initModelsAndViews();
          }
        });
    
    
    editorFrame.pack();
    editorFrame.setVisible(true);
    fixFrameSize(editorFrame);
  }
  
  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD'insertValue' DB_SPALTE '"+id+"')";
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
    if (editorFrame != null) editorFrame.dispose();
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }
  
  /**
   * Schliesst den FM4000 und alle zugehörigen Fenster.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{abort();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  }
  
  /**
   * Bringt den FormularMax 4000 in den Vordergrund.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void toFront()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{myFrame.toFront();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  }
  
  /**
   * Workaround für Problem unter Windows, dass das Layout bei myFrame.pack() die 
   * Taskleiste nicht berücksichtigt (das Fenster also dahinter verschwindet), zumindest
   * solange nicht bis man die Taskleiste mal in ihrer Größe verändert hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFrameSize()
  {
    myFrame.pack();
    fixFrameSize(myFrame);
  }

  /**
   * Sorgt dafür, dass die Ausdehnung von frame nicht die maximal erlaubten
   * Fensterdimensionen überschreitet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void fixFrameSize(JFrame frame)
  {
    Rectangle maxWindowBounds;
    
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    String lafName = UIManager.getSystemLookAndFeelClassName(); 
    if (!lafName.contains("plaf.windows."))
      maxWindowBounds.height-=32; //Sicherheitsabzug für KDE Taskleiste
    
    Rectangle frameBounds = frame.getBounds();
    if (frameBounds.x < maxWindowBounds.x)
    {
      frameBounds.width -= (maxWindowBounds.x - frameBounds.x);
      frameBounds.x = maxWindowBounds.x;
    }
    if (frameBounds.y < maxWindowBounds.y)
    {
      frameBounds.height -= (maxWindowBounds.y - frameBounds.y);
      frameBounds.y = maxWindowBounds.y;
    }
    if (frameBounds.width > maxWindowBounds.width)
      frameBounds.width = maxWindowBounds.width;
    if (frameBounds.height > maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.height;
    frame.setBounds(frameBounds);
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
    new FormularMax4000(doc,null);
  }

}
