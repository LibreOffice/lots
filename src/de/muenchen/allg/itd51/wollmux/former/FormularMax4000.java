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
* 19.10.2006 | BNK | Quelltexteditor nicht mehr in einem eigenen Frame
* 20.10.2006 | BNK | Rückschreiben ins Dokument erfolgt jetzt automatisch.
* 26.10.2006 | BNK | Magische gender: Syntax unterstützt. 
* 30.10.2006 | BNK | Menüstruktur geändert; Datei/Speichern (unter...) hinzugefügt
* 05.02.2007 | BNK | [R5214]Formularmerkmale entfernen hat fast leere Formularnotiz übriggelassen
* 11.04.2007 | BNK | [R6176]Nicht-WM-Bookmarks killen
*                  | Nicht-WM-Bookmarks killen Funktion derzeit auskommentiert wegen Zerstörung von Referenzen
* 10.07.2007 | BNK | [P1403]abort() verbessert, damit FM4000 gemuellentsorgt werden kann                 
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XInterface;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Container;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.TextRange;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Visitor;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

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
   *                        Wird als "Name" die Spezialsyntax "<<gender:ID>>" verwendet, so
   *                        wird eine reine Einfügestelle erzeugt, die mit einer Gender-TRAFO
   *                        versehen wird, die abhängig vom Formularfeld ID einen der Werte
   *                        des Dropdowns auswählt, und zwar bei "Herr" oder "Herrn" den ersten
   *                        Eintrag, bei "Frau" den zweiten Eintrag und bei allem sonstigen
   *                        den dritten Eintrag. Hat das Dropdown nur 2 Einträge, so wird im
   *                        sonstigen Fall das Feld ID untransformiert übernommen. Falls vorhanden
   *                        werden ein bis 2 Spaces am Ende eines Eintrages der Dropdown-Liste 
   *                        entfernt. Dies ermöglicht es, das selbe Wort mehrfach in die
   *                        Liste aufzunehmen.
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
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen, dass
   * ein insertFormValue mit Gender-TRAFO erzeugt werden soll.
   */
  private static final String GENDER_PREFIX = "gender:";
  
  /**
   * Der {@link IDManager}-Namensraum für die IDs von {@link FormControlModel}s.
   */
  public static final Integer NAMESPACE_FORMCONTROLMODEL = new Integer(0);
  
  

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
   * Oberster Container der FM4000 GUI-Elemente. Wird direkt in die ContentPane von myFrame
   * gesteckt.
   */
  private JSplitPane mainContentPanel;
  
  /**
   * Oberster Container für den Quelltexteditor. Wird direkt in die ContentPane von myFrame
   * gesteckt. 
   */
  private JPanel editorContentPanel;
  
  /**
   * Der Übercontainer für die linke Hälfte des FM4000.
   */
  private LeftPanel leftPanel;
  
  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;
  
  /**
   * Das TextDocumentModel, zu dem das Dokument doc gehört.
   */
  private TextDocumentModel doc;
  
  /**
   * Verwaltet die IDs von Objekten.
   * @see #NAMESPACE_FORMCONTROLMODEL
   */
  private IDManager idManager = new IDManager();
  
  /**
   * Verwaltet die FormControlModels dieses Formulars.
   */
  private FormControlModelList formControlModelList;
  
  /**
   * Verwaltet die {@link InsertionModel}s dieses Formulars.
   */
  private InsertionModelList insertionModelList;
  
  /**
   * Verwaltet die {@link GroupModel}s dieses Formulars.
   */
  private GroupModelList groupModelList;
  
  /**
   * Funktionsbibliothek, die globale Funktionen zur Verfügung stellt.
   */
  private FunctionLibrary functionLibrary;
  
  /**
   * Verantwortlich für das Übersetzen von TRAFO, PLAUSI und AUTOFILL in
   * {@link FunctionSelection}s.
   */
  private FunctionSelectionProvider functionSelectionProvider;
  
  /**
   * Verantwortlich für das Übersetzen von Gruppennamen in
   * {@link FunctionSelection}s anhand des Sichtbarkeit-Abschnitts.
   */
  private FunctionSelectionProvider visibilityFunctionSelectionProvider;
  
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
   * Wird auf myFrame registriert, damit zum Schließen des Fensters abort() aufgerufen wird.
   */
  private MyWindowListener oehrchen;

  /**
   * Die Haupt-Menüleiste des FM4000.
   */
  private JMenuBar mainMenuBar;
  
  /**
   * Die Menüleiste, die angezeigt wird wenn der Quelltexteditor offen ist.
   */
  private JMenuBar editorMenuBar;

  /**
   * Der Quelltexteditor.
   */
  private JEditorPane editor;
  
  /**
   * Die Namen aller Druckfunktionen, die zur Auswahl stehen.
   */
  private Vector printFunctionNames;
  
  /**
   * Wird bei jeder Änderung von Formularaspekten gestartet, um nach einer Verzögerung die
   * Änderungen in das Dokument zu übertragen.
   */
  private Timer writeChangesTimer;

  /**
   * Der XSelectionSupplier des Dokuments. 
   */
  private XSelectionSupplier selectionSupplier;

  /**
   * Wird auf {@link #selectionSupplier} registriert, um Änderungen der Cursorselektion zu beobachten.
   */
  private MyXSelectionChangedListener myXSelectionChangedListener;

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
   * Wird bei jeder Änderung einer internen Datenstruktur aufgerufen, die ein Updaten des
   * Dokuments erforderlich macht um persistent zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void documentNeedsUpdating()
  {
    writeChangesTimer.restart();
  }
  
  /**
   * Liefert den {@link IDManager}, der für Objekte im Formular verwendet wird.
   * @see #NAMESPACE_FORMCONTROLMODEL
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public IDManager getIDManager() {return idManager;}
  
  /**
   * Startet eine Instanz des FormularMax 4000 für das Dokument des TextDocumentModels model.
   * @param abortListener (falls nicht null) wird aufgerufen, nachdem der FormularMax 4000 geschlossen wurde.
   * @param funcLib Funktionsbibliothek, die globale Funktionen zur Verfügung stellt.
   * @param printFuncLib Funktionsbibliothek, die Druckfunktionen zur Verfügung stellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000(TextDocumentModel model, ActionListener abortListener, FunctionLibrary funcLib, PrintFunctionLibrary printFuncLib)
  {
    this.doc = model;
    this.abortListener = abortListener;
    this.functionLibrary = funcLib;
    this.printFunctionNames = new Vector(printFuncLib.getFunctionNames());
    
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
    
    
    formControlModelList = new FormControlModelList(this);
    insertionModelList = new InsertionModelList(this);
    groupModelList = new GroupModelList(this);
    
    //  Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    
    leftPanel = new LeftPanel(insertionModelList, formControlModelList, this);
    RightPanel rightPanel = new RightPanel(insertionModelList, formControlModelList, functionLibrary, this);
    
    mainContentPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel.JComponent(), rightPanel.JComponent());
    myFrame.getContentPane().add(mainContentPanel);
    
    mainMenuBar = new JMenuBar();
    //========================= Datei ============================
    JMenu menu = new JMenu("Datei");
    
    JMenuItem menuItem = new JMenuItem("Speichern");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        save(doc);
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Speichern unter...");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        saveAs(doc);
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Beenden");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }});
    menu.add(menuItem);
    
    mainMenuBar.add(menu);
//  ========================= Formular ============================
    menu = new JMenu("Formular");
    
    menuItem = new JMenuItem("Formularfelder aus Dokument einlesen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        scan(doc.doc);
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
    
    menuItem = new JMenuItem("Druckfunktion setzen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        setPrintFunction();
        setFrameSize();
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("WollMux-Formularmerkmale aus Dokument entfernen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        deForm(doc); 
      }});
    menu.add(menuItem);
    
    /* Das Entfernen von Bookmarks kann Referenzfelder (Felder die Kopien anderer
     * Teile des Dokuments enthalten) zerstören, da diese dann ins Leere greifen.
     * Solange dies nicht erkannt wird, muss die Funktion deaktiviert sein.
     * 
     */
    if (new Integer(3).equals(new Integer(0)))
    {
    menuItem = new JMenuItem("Ladezeit des Dokuments optimieren");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        removeNonWMBookmarks(doc); 
      }});
    menu.add(menuItem);
    }
    
    menuItem = new JMenuItem("Formularbeschreibung editieren");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }});
    menu.add(menuItem);

    
    mainMenuBar.add(menu);
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
    
    
    mainMenuBar.add(menu);

    myFrame.setJMenuBar(mainMenuBar);

    writeChangesTimer = new Timer(500, new ActionListener()
    { public void actionPerformed(ActionEvent e)
      {
        updateDocument(doc);
    }});
    writeChangesTimer.setCoalesce(true);
    writeChangesTimer.setRepeats(false);
    
    initEditor();

    selectionSupplier = UNO.XSelectionSupplier(doc.doc.getCurrentController());
    myXSelectionChangedListener = new MyXSelectionChangedListener();
    selectionSupplier.addSelectionChangeListener(myXSelectionChangedListener);
    
    initModelsAndViews(doc.getFormDescription());
    
    writeChangesTimer.stop();
    
    setFrameSize();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Wertet formDescription sowie die Bookmarks von {@link #doc} aus und initialisiert 
   * alle internen
   * Strukturen entsprechend. Dies aktualisiert auch die entsprechenden Views.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void initModelsAndViews(ConfigThingy formDescription)
  {
    formControlModelList.clear();
    parseGlobalFormInfo(formDescription);
    
    ConfigThingy fensterAbschnitte = formDescription.query("Formular").query("Fenster");
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
      FormControlModel separatorTab = FormControlModel.createTab(id, id, this);
      formControlModelList.add(separatorTab,0);
    }
    
    insertionModelList.clear();
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc.doc); 
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try{
        String bookmark = bookmarks[i];
        if (InsertionModel.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel(bookmark, bmSupp, functionSelectionProvider, this));
      }catch(Exception x)
      {
        Logger.error(x);
      }
    }

    groupModelList.clear();
    ConfigThingy visibilityConf = formDescription.query("Formular").query("Sichtbarkeit");
    Iterator sichtbarkeitsAbschnittIterator = visibilityConf.iterator();
    while (sichtbarkeitsAbschnittIterator.hasNext())
    {
      ConfigThingy sichtbarkeitsAbschnitt = (ConfigThingy)sichtbarkeitsAbschnittIterator.next();
      Iterator sichtbarkeitsFunktionIterator = sichtbarkeitsAbschnitt.iterator();
      while (sichtbarkeitsFunktionIterator.hasNext())
      {
        ConfigThingy sichtbarkeitsFunktion = (ConfigThingy)sichtbarkeitsFunktionIterator.next();
        String groupName = sichtbarkeitsFunktion.getName(); 
        FunctionSelection funcSel = visibilityFunctionSelectionProvider.getFunctionSelection(groupName);
        groupModelList.add(new GroupModel(groupName, funcSel, this));
      }
    }
    
    setFrameSize();
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
    {
      formTitle = newTitle;
      documentNeedsUpdating();
    }
  }
  
  /**
   * Speichert die aktuelle Formularbeschreibung im Dokument und aktualisiert Bookmarks etc.
   * 
   * @return die aktualisierte Formularbeschreibung
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private ConfigThingy updateDocument(TextDocumentModel doc)
  {
    Logger.debug("Übertrage Formularbeschreibung ins Dokument");
    Map mapFunctionNameToConfigThingy = new HashMap();
    insertionModelList.updateDocument(mapFunctionNameToConfigThingy);
    ConfigThingy conf = buildFormDescriptor(mapFunctionNameToConfigThingy);
    if (!formTitle.equals(GENERATED_FORM_TITLE) 
    || formControlModelList.size() > 1   // nur 1 Element wird ignoriert, weil nur ein leerer Reiter  
    || !groupModelList.isEmpty())
    {
      doc.setFormDescription(new ConfigThingy(conf));
    }
    return conf;
  }
  
  /**
   * Ruft {@link #updateDocument(TextDocumentModel)} auf, falls noch Änderungen anstehen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void flushChanges()
  {
    if (writeChangesTimer.isRunning())
    {
      Logger.debug("Schreibe wartende Änderungen ins Dokument");
      writeChangesTimer.stop();
      try{
        updateDocument(doc);
      }catch(Exception x){ Logger.error(x);};
    }
  }

  /**
   * Liefert ein ConfigThingy zurück, das den aktuellen Zustand der Formularbeschreibung
   * repräsentiert. Zum Exportieren der Formularbeschreibung sollte {@link #updateDocument(XTextDocument)}
   * verwendet werden.
   * @param mapFunctionNameToConfigThingy bildet einen Funktionsnamen auf ein ConfigThingy ab, 
   *        dessen Wurzel der Funktionsname ist und dessen Inhalt eine Funktionsdefinition ist.
   *        Diese Funktionen ergeben den Funktionen-Abschnitt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  private ConfigThingy buildFormDescriptor(Map mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    ConfigThingy form = conf.add("Formular");
    form.add("TITLE").add(formTitle);
    form.addChild(formControlModelList.export());
    form.addChild(groupModelList.export());
    if (!mapFunctionNameToConfigThingy.isEmpty())
    {
      ConfigThingy funcs = form.add("Funktionen");
      Iterator iter = mapFunctionNameToConfigThingy.values().iterator();
      while (iter.hasNext())
      {
        funcs.addChild((ConfigThingy)iter.next());
      }
    }
    return conf;
  }
  
  /**
   * Extrahiert aus conf die globalen Eingenschaften des Formulars wie z,B, den Formulartitel
   * oder die Funktionen des Funktionen-Abschnitts.
   * @param conf der WM-Knoten der über einer beliebigen Anzahl von Formular-Knoten sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void parseGlobalFormInfo(ConfigThingy conf)
  {
    ConfigThingy tempConf = conf.query("Formular").query("TITLE");
    if (tempConf.count() > 0) formTitle = tempConf.toString();
    tempConf = conf.query("Formular").query("Funktionen");
    if (tempConf.count() >= 1)
    {
      try{tempConf = tempConf.getFirstChild();}catch(Exception x){}
    }
    else
    {
      tempConf = new ConfigThingy("Funktionen");
    }
    functionSelectionProvider = new FunctionSelectionProvider(functionLibrary, tempConf, getIDManager(), NAMESPACE_FORMCONTROLMODEL);
    
    tempConf = conf.query("Formular").query("Sichtbarkeit");
    if (tempConf.count() >= 1)
    {
      try{tempConf = tempConf.getFirstChild();}catch(Exception x){}
    }
    else
    {
      tempConf = new ConfigThingy("Sichtbarkeit");
    }
    visibilityFunctionSelectionProvider = new FunctionSelectionProvider(null, tempConf, getIDManager(), NAMESPACE_FORMCONTROLMODEL);
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
      documentNeedsUpdating();
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
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
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
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
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
    
    FormControlModel tab = FormControlModel.createTab(label, id, this);
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
    
    documentNeedsUpdating();
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
        model = new FormControlModel((ConfigThingy)iter.next(), functionSelectionProvider, this);
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
    
    documentNeedsUpdating();
    
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
      Visitor visitor = new ScanVisitor(); 
      visitor.visit(tree);
    } 
    catch(Exception x) {Logger.error("Fehler während des Scan-Vorgangs",x);}
    
    documentNeedsUpdating();
  }
  
  private class ScanVisitor extends DocumentTree.Visitor
  {
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
    
    boolean doGenderTrafo = false;
    
    String bookmarkName = insertFormValue(id);
    if (label == INSERTION_ONLY)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      } else if (id.startsWith(GENDER_PREFIX))
      {
        id = id.substring(GENDER_PREFIX.length());
        bookmarkName = insertFormValue(id);
        if (control.getType() == DocumentTree.DROPDOWN_CONTROL)
          doGenderTrafo = true;
      }
    }
    
    bookmarkName = control.surroundWithBookmark(bookmarkName);

    try{
      InsertionModel imodel = new InsertionModel(bookmarkName, UNO.XBookmarksSupplier(doc.doc), functionSelectionProvider, this);
      if (doGenderTrafo)
        addGenderTrafo(imodel, (DropdownFormControl)control);
      insertionModelList.add(imodel);
    }catch(Exception x)
    {
      Logger.error("Es wurde ein fehlerhaftes Bookmark generiert: \""+bookmarkName+"\"", x);
    }
    
    return model;
  }

  /**
   * Verpasst model eine Gender-TRAFO, die ihre Herr/Frau/Anders-Texte aus den Items von
   * control bezieht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addGenderTrafo(InsertionModel model, DropdownFormControl control)
  {
    String[] items = control.getItems();
    FunctionSelection genderTrafo = functionSelectionProvider.getFunctionSelection("Gender");
    String[] params = genderTrafo.getParameterNames();
    
    for (int i = 0; i < 3 && i < items.length; ++i)
    {
      String item = items[i];
      //bis zu 2 Leerzeichen am Ende löschen, um mehrere gleiche Einträge zu erlauben.
      if (item.endsWith(" ")) item = item.substring(0, item.length() - 1);
      if (item.endsWith(" ")) item = item.substring(0, item.length() - 1);
      genderTrafo.setParameterValue(params[i], ParamValue.literal(item));
    }
    
    model.setTrafo(genderTrafo);
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
    model = FormControlModel.createCheckbox(label, id, this);
    if (control.getString().equalsIgnoreCase("true"))
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add("true");
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
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
    model = FormControlModel.createComboBox(label, id, items, this);
    model.setEditable(editable);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
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
    model = FormControlModel.createTextfield(label, id, this);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
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
      String prefix = "";
      if (str.startsWith(GLOBAL_PREFIX))
      {
        prefix = GLOBAL_PREFIX;
        str = str.substring(GLOBAL_PREFIX.length());
      }
      else if (str.startsWith(GENDER_PREFIX))
      {
        prefix = GENDER_PREFIX;
        str = str.substring(GENDER_PREFIX.length());
      }
      str = str.replaceAll("[^a-zA-Z_0-9]","");
      if (str.length() == 0) str = "Einfuegung";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return prefix + str;
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
   * Initialisiert die GUI für den Quelltexteditor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initEditor()
  {
    JMenu menu;
    JMenuItem menuItem;
    editorMenuBar = new JMenuBar();
    //========================= Datei ============================
    menu = new JMenu("Datei");
    
    menuItem = new JMenuItem("Speichern");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy conf = new ConfigThingy("", null, new StringReader(editor.getText()));
          myFrame.setJMenuBar(mainMenuBar);
          myFrame.getContentPane().remove(editorContentPanel);
          myFrame.getContentPane().add(mainContentPanel);
          initModelsAndViews(conf);
          documentNeedsUpdating();
        }
        catch (Exception e1)
        {
          JOptionPane.showMessageDialog(myFrame, e1.getMessage(), "Fehler beim Parsen der Formularbeschreibung", JOptionPane.WARNING_MESSAGE);
        }
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        myFrame.setJMenuBar(mainMenuBar);
        myFrame.getContentPane().remove(editorContentPanel);
        myFrame.getContentPane().add(mainContentPanel);
        setFrameSize();
      }});
    menu.add(menuItem);
    
        
    editorMenuBar.add(menu);

    editor = new JEditorPane("text/plain","");
    editor.setEditorKit(new NoWrapEditorKit());
    
    editor.setFont(new Font("Monospaced",Font.PLAIN,editor.getFont().getSize()+2));
    JScrollPane scrollPane = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    editorContentPanel = new JPanel(new BorderLayout());
    editorContentPanel.add(scrollPane, BorderLayout.CENTER);
  }

  
  /**
   * Öffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des Fensters
   * wird die geänderte Formularbeschreibung neu geparst, falls sie syntaktisch korrekt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editFormDescriptor()
  {
    editor.setCaretPosition(0);
    editor.setText(updateDocument(doc).stringRepresentation());
    myFrame.getContentPane().remove(mainContentPanel);
    myFrame.getContentPane().add(editorContentPanel);
    myFrame.setJMenuBar(editorMenuBar);
    setFrameSize();
  }
  
  private void setPrintFunction()
  {
    final JEditorPane printFunctionConfigEditor = new JEditorPane("text/plain","");
    printFunctionConfigEditor.setEditorKit(new NoWrapEditorKit());
    
    printFunctionConfigEditor.setFont(new Font("Monospaced",Font.PLAIN,printFunctionConfigEditor.getFont().getSize()+2));
    JScrollPane scrollPane = new JScrollPane(printFunctionConfigEditor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    JPanel printFunctionEditorContentPanel = new JPanel(new BorderLayout());
    printFunctionEditorContentPanel.add(scrollPane, BorderLayout.CENTER);
    
    final JComboBox printFunctionComboBox = new JComboBox(printFunctionNames);
    printFunctionComboBox.setEditable(true);
    printFunctionComboBox.setSelectedItem(doc.getPrintFunctionName());
    printFunctionEditorContentPanel.add(printFunctionComboBox, BorderLayout.NORTH);
  
    final JDialog dialog = new JDialog(myFrame, true);
    
    ActionListener setFunc = new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        doc.setPrintFunctionConfig(printFunctionConfigEditor.getText());
        doc.setPrintFunction(printFunctionComboBox.getSelectedItem().toString());
        dialog.dispose();
      }
    };
    
    JButton okay = new JButton("Setzen");
    okay.addActionListener(setFunc);
    
    JButton abort = new JButton("Abbrechen");
    abort.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }});

    Box buttons = Box.createHorizontalBox();
    buttons.add(abort);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(okay);
    printFunctionEditorContentPanel.add(buttons, BorderLayout.SOUTH);
    
    printFunctionConfigEditor.setCaretPosition(0);
    printFunctionConfigEditor.setText(doc.getPrintFunctionConfig());
    
    
    dialog.setTitle("Druckfunktion setzen");
    dialog.add(printFunctionEditorContentPanel);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    if (frameWidth < 384)
      frameWidth = 384;
    if (frameHeight < 384)
      frameHeight = 384;
    
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    dialog.setBounds(x, y, frameWidth, frameHeight);
    dialog.setVisible(true);
  }
  
  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD 'insertValue' DB_SPALTE '"+id+"')";
  }
  
  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD 'insertFormValue' ID '"+id+"')";
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument
   * doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeNonWMBookmarks(TextDocumentModel doc)
  {
    doc.removeNonWMBookmarks();
  }
  
  /**
   * Entfernt die WollMux-Formularmerkmale aus dem Dokument.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void deForm(TextDocumentModel doc)
  {
    doc.deForm();
    initModelsAndViews(new ConfigThingy(""));
  }
  
  /**
   * Ruft die Datei/Speichern Funktion von OpenOffice.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void save(TextDocumentModel doc)
  {
    flushChanges();
    UNO.dispatch(doc.doc, ".uno:Save");    
  }
  
  /**
   * Ruft die Datei/Speichern unter... Funktion von OpenOffice.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void saveAs(TextDocumentModel doc)
  {
    flushChanges();
    UNO.dispatch(doc.doc, ".uno:SaveAs");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    flushChanges();
    
    /*
     * Wegen folgendem Java Bug (WONTFIX) 
     *   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304
     * sind die folgenden 3 Zeilen nötig, damit der FormularMax4000 gc'ed werden
     * kann. Die Befehle sorgen dafür, dass kein globales Objekt (wie z.B.
     * der Keyboard-Fokus-Manager) indirekt über den JFrame den FM4000 kennt.  
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);
    
    myFrame.dispose();
    myFrame = null;
    try{
      selectionSupplier.removeSelectionChangeListener(myXSelectionChangedListener);
    } catch(Exception x){}
    
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

  /**
   * Nimmt eine Menge von XTextRange Objekten, sucht alle umschlossenen Bookmarks und broadcastet
   * eine entsprechende Nachricht, damit sich die entsprechenden Objekte selektieren. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void selectionChanged(XIndexAccess access)
  {
    Set bookmarkNames = null; //wird lazy initialisiert
    
    int count = access.getCount();
    for (int i = 0; i < count; ++i)
    {
      XEnumerationAccess enuAccess = null;
      try{
        enuAccess = UNO.XEnumerationAccess(access.getByIndex(i));
      } catch(Exception x)
      {
        Logger.error(x);
      }
      try{
        if (enuAccess != null)
        {
          XEnumeration paraEnu = enuAccess.createEnumeration();
          while (paraEnu.hasMoreElements())
          {
            Object nextEle = paraEnu.nextElement();
            if (nextEle == null) throw new NullPointerException("nextElement() == null obwohl hasMoreElements()==true");
            XEnumerationAccess xs = UNO.XEnumerationAccess(nextEle);
            if (xs == null) throw new NullPointerException("Paragraph unterstützt nicht XEnumerationAccess?!?");
            XEnumeration textportionEnu = xs.createEnumeration();
            while (textportionEnu.hasMoreElements())
            {
              Object textportion = textportionEnu.nextElement();
              if ("Bookmark".equals(UNO.getProperty(textportion, "TextPortionType")))
              {
                XNamed bookmark = null; 
                try{
                  //boolean isStart = ((Boolean)UNO.getProperty(textportion, "IsStart")).booleanValue();
                  bookmark = UNO.XNamed(UNO.getProperty(textportion, "Bookmark"));
                } catch(Exception x){ continue;}
                
                String name = bookmark.getName();
                if (bookmarkNames == null) bookmarkNames = new HashSet();
                bookmarkNames.add(name);
              }
            }
          }
        }
      } catch(Exception x)
      {
        Logger.error(x);
      }
    }
    
    if (bookmarkNames != null && !bookmarkNames.isEmpty())
      broadcast(new BroadcastObjectSelectionByBookmarks(bookmarkNames));
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
  
  
  private class MyXSelectionChangedListener implements XSelectionChangeListener
  {
    public void selectionChanged(EventObject arg0)
    {
      try
      {
        Object selection = AnyConverter.toObject(XInterface.class, selectionSupplier.getSelection());
        final XIndexAccess access = UNO.XIndexAccess(selection);
        if (access == null) return;
        try{
          javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try{FormularMax4000.this.selectionChanged(access);}catch(Exception x){};
            }
          });
        }
        catch(Exception x) {}
      }
      catch (IllegalArgumentException e)
      {
        Logger.error("Kann Selection nicht in Objekt umwandeln", e);
      }
    }

    public void disposing(EventObject arg0) {}
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
    WollMuxFiles.setupWollMuxDir();
    Logger.init(System.err, Logger.DEBUG);
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    Map context = new HashMap();
    DialogLibrary dialogLib = WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, context);
    new FormularMax4000(new TextDocumentModel(doc),null, WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(), dialogLib, context, null), WollMuxFiles.parsePrintFunctions(WollMuxFiles.getWollmuxConf()));
  }

}
