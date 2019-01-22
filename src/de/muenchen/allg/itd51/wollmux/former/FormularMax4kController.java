package de.muenchen.allg.itd51.wollmux.former;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.table.XCell;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextTable;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XInterface;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.document.ScanVisitor;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionTester;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel4InputUser;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel4InsertXValue;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModel;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModelList;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

public class FormularMax4kController
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormularMax4kController.class);

  private FormularMax4kView view;

  /**
   * Das TextDocumentModel, zu dem das Dokument doc gehört.
   */
  private TextDocumentController documentController;

  /**
   * Verwaltet die IDs von Objekten.
   * 
   * @see #NAMESPACE_FORMCONTROLMODEL
   */
  private IDManager idManager;

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
   * Verwaltet die {@link SectionModel}s dieses Formulars.
   */
  private SectionModelList sectionModelList;

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
   * Verantwortlich für das Übersetzen von Gruppennamen in {@link FunctionSelection}s
   * anhand des Sichtbarkeit-Abschnitts.
   */
  private FunctionSelectionProvider visibilityFunctionSelectionProvider;

  /**
   * Der globale Broadcast-Kanal wird für Nachrichten verwendet, die verschiedene
   * permanente Objekte erreichen müssen, die aber von (transienten) Objekten
   * ausgehen, die mit diesen globalen Objekten wegen des Ausuferns der Verbindungen
   * nicht in einer Beziehung stehen sollen. Diese Liste enthält alle
   * {@link BroadcastListener}, die auf dem globalen Broadcast-Kanal horchen. Dies
   * dürfen nur permanente Objekte sein, d.h. Objekte deren Lebensdauer nicht vor
   * Beenden des FM4000 endet.
   */
  private List<BroadcastListener> broadcastListeners =
    new ArrayList<BroadcastListener>();

  /**
   * Wird bei jeder Änderung von Formularaspekten gestartet, um nach einer
   * Verzögerung die Änderungen in das Dokument zu übertragen.
   */
  private Timer writeChangesTimer;

  /**
   * Der Standard-Formulartitel, solange kein anderer gesetzt wird.
   */
  private static final String GENERATED_FORM_TITLE =
    L.m("Generiert durch FormularMax 4000");

  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der FM4000 geschlossen
   * wurde.
   */
  private ActionListener abortListener = null;

  /**
   * Die Namen aller Druckfunktionen, die zur Auswahl stehen.
   */
  private PrintFunctionLibrary printFunctionLibrary;

  /**
   * Default-Name für ein neues Tab.
   */
  public static final String STANDARD_TAB_NAME = L.m("Tab");

  /**
   * Der {@link IDManager}-Namensraum für die IDs von {@link FormControlModel}s.
   */
  public static final Integer NAMESPACE_FORMCONTROLMODEL = Integer.valueOf(0);

  /**
   * Der {@link IDManager}-Namensraum für die DB_SPALTE-Angaben von
   * {@link InsertionModel}s.
   */
  public static final Integer NAMESPACE_DB_SPALTE = Integer.valueOf(1);

  /**
   * Der {@link IDManager}-Namensraum für die Namen von {@link GroupModel}s.
   */
  public static final Integer NAMESPACE_GROUPS = Integer.valueOf(2);

  /**
   * Wert von PLAUSI_MARKER_COLOR oder null wenn nicht gesetzt.
   */
  private String plausiMarkerColor = null;
  
  /**
   * Wird auf {@link #selectionSupplier} registriert, um Änderungen der
   * Cursorselektion zu beobachten.
   */
  private MyXSelectionChangedListener myXSelectionChangedListener;

  /**
   * Der XSelectionSupplier des Dokuments.
   */
  private XSelectionSupplier selectionSupplier;

  /**
   * Speichert die Funktionsdialoge-Abschnitte des Formulars. Der FM4000 macht
   * derzeit nichts besonderes mit ihnen, sondern schreibt sie einfach nur ins
   * Dokument zurück.
   */
  private ConfigThingy funktionsDialogeAbschnitteConf;

  /**
   * GUI zum interaktiven Zusammenbauen und Testen von Funktionen.
   */
  private FunctionTester functionTester = null;

  /**
   * Zahl von Formularsteuerelementen in einem Formular, ab der es in Zusammenhang
   * mit einer maximaler Heap Size der JVM, die kleiner ist als
   * {@link #LOWEST_ALLOWED_HEAP_SIZE}, zu Speicherplatzproblemen kommen kann.
   * 
   * Der Wert 5000 wurde vollkommen willkürlich gewählt und ist wahrscheinlich zu
   * hoch. Wir warten auf den ersten Bugreport mit einem {@link OutOfMemoryError} und
   * legen den Wert dann anhand des realen Falles neu fest.
   */
  private static final int CRITICAL_NUMBER_OF_FORMCONTROLS = 5000;

  /**
   * Mindestgröße der maximalen Heap Size der JVM (in Bytes). Sollte die maximale
   * Heap Size der JVM kleiner als dieser Wert sein, kann es bei Formularen mit mehr
   * Formularsteuerelementen als {@link #CRITICAL_NUMBER_OF_FORMCONTROLS} zu
   * Speicherplatzproblemen kommen.
   */
  private static final long LOWEST_ALLOWED_HEAP_SIZE = 70000000;

  public FormularMax4kController(TextDocumentController documentController,
      ActionListener abortListener, FunctionLibrary funcLib,
      PrintFunctionLibrary printFuncLib)
  {
    this.documentController = documentController;
    this.abortListener = abortListener;
    this.functionLibrary = funcLib;
    this.printFunctionLibrary = printFuncLib;

    formControlModelList = new FormControlModelList(this);
    insertionModelList = new InsertionModelList(this);
    groupModelList = new GroupModelList(this);
    sectionModelList = new SectionModelList(this);

    writeChangesTimer = new Timer(500, new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        updateDocument(FormularMax4kController.this.documentController);
      }
    });
    writeChangesTimer.setCoalesce(true);
    writeChangesTimer.setRepeats(false);

    initModelsAndViews(documentController.getFormDescription());
    
    if (!testMemoryRequirements())
    {
      return;
    }
    
    selectionSupplier = getSelectionSupplier();
    if(selectionSupplier != null)
    {
      myXSelectionChangedListener = new MyXSelectionChangedListener();
      selectionSupplier.addSelectionChangeListener(myXSelectionChangedListener); 
    }
}

  public void setAbortListener(ActionListener abortListener)
  {
    this.abortListener = abortListener;
  }

  public void run()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            view = new FormularMax4kView("FormularMax 4000", FormularMax4kController.this);

            writeChangesTimer.stop();

            view.setFrameSize();
            view.setVisible(true);
          }
          catch (Exception x)
          {
            LOGGER.error("", x);
          }
        }
      });
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Liefert den {@link IDManager}, der für Objekte im Formular verwendet wird.
   *
   * @see #NAMESPACE_FORMCONTROLMODEL
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public IDManager getIDManager()
  {
    return idManager;
  }

  public TextDocumentController getDocumentController()
  {
    return documentController;
  }

  public FunctionSelectionProvider getFunctionSelectionProvider()
  {
    return functionSelectionProvider;
  }

  public InsertionModelList getInsertionModelList()
  {
    return insertionModelList;
  }

  public FormControlModelList getFormControlModelList()
  {
    return formControlModelList;
  }

  public void setFormTitle(String newTitle)
  {
    if (newTitle != null)
    {
      formTitle = newTitle;
      documentNeedsUpdating();
    }
  }

  public Object getFormTitle()
  {
    return formTitle;
  }

  /**
   * Wird von {@link FormControlModel#setItems(String[])} auf model aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void comboBoxItemsHaveChanged(FormControlModel model)
  {
    insertionModelList.fixComboboxInsertions(model);
  }

  /**
   * Wird bei jeder Änderung einer internen Datenstruktur aufgerufen, die ein Updaten
   * des Dokuments erforderlich macht um persistent zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void documentNeedsUpdating()
  {
    writeChangesTimer.restart();
  }

  /**
   * listener wird über globale {@link Broadcast}s informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void addBroadcastListener(BroadcastListener listener)
  {
    if (!broadcastListeners.contains(listener))
    {
      broadcastListeners.add(listener);
    }
  }

  /**
   * Sendet die Nachricht b an alle Listener, die auf dem globalen Broadcast-Kanal
   * registriert sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void broadcast(Broadcast b)
  {
    for (BroadcastListener bl : broadcastListeners)
    {
      b.sendTo(bl);
    }
  }

  public LeftPanel createLeftPanel()
  {
    return new LeftPanel(insertionModelList, formControlModelList, groupModelList,
      sectionModelList, this, documentController.getModel().doc);
  }

  public RightPanel createRightPanel()
  {
    return new RightPanel(insertionModelList, formControlModelList, groupModelList,
      sectionModelList, functionLibrary, this);
  }

  public XSelectionSupplier getSelectionSupplier()
  {
    return UNO.XSelectionSupplier(documentController.getModel().doc.getCurrentController());
  }

  public void setPrintFunction()
  {
    PrintFunctionDialog pfd =
      new PrintFunctionDialog(view, true, documentController, printFunctionLibrary);
    pfd.setVisible(true);
  }

  public void setFilenameGeneratorFunction()
  {
    FilenameGeneratorFunctionDialog fgfd =
      new FilenameGeneratorFunctionDialog(view, true, documentController, idManager);
    fgfd.setVisible(true);
  }

  /**
   * Entfernt die WollMux-Formularmerkmale aus dem Dokument.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void deForm()
  {
    documentController.deForm();
    initModelsAndViews(new ConfigThingy(""));
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void removeNonWMBookmarks()
  {
    documentController.removeNonWMBookmarks();
  }

  /**
   * Öffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des
   * Fensters wird die geänderte Formularbeschreibung neu geparst, falls sie
   * syntaktisch korrekt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void editFormDescriptor()
  {
    view.setVisible(false);
    ConfigEditor editor = new ConfigEditor("Config Editor", this);
    
    editor.setVisible(true);
    editor.setText(updateDocument(documentController).stringRepresentation());
    editor.addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosed(WindowEvent e) {
        view.setVisible(true);
      }
    });
  }

  public void showFunctionTester()
  {
    if (functionTester == null)
    {
      functionTester = new FunctionTester(functionLibrary, new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          functionTester = null;
        }
      }, idManager, NAMESPACE_FORMCONTROLMODEL);
    }
    else
    {
      functionTester.toFront();
    }
  }
  
  /**
   * Bringt den FormularMax 4000 in den Vordergrund.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void toFront()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            view.toFront();
          }
          catch (Exception x)
          {}
          ;
        }
      });
    }
    catch (Exception x)
    {}
  }


  /**
   * Fügt am Anfang der Liste einen Tab ein, dessen Konfiguration aus tabConf kommt
   * (Wurzelknoten wird ignoriert, darunter sollten TITLE, Eingabefelder etc, liegen)
   * falls tabConf != null, ansonsten aus einem ConfigThingy was an der URL
   * tabConfUrl gespeichert ist (hier sind TITLE, Eingabefelder, etc, auf oberster
   * Ebene).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void insertStandardTab(ConfigThingy tabConf, URL tabConfUrl)
  {
    try
    {
      if (tabConf == null)
      {
        tabConf = new ConfigThingy("Empfaengerauswahl", tabConfUrl);
      }
      parseTab(tabConf, 0);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Hängt die Standardbuttons aus conf (Wurzelknoten "Buttons", darunter direkt die
   * Button-Spezifikationen) oder (falls conf==null) aus dem ConfigThingy das an
   * confUrl gespeichert ist (kein umschließender Abschnitt, sondern direkt die
   * Button-Beschreibungen) das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void insertStandardButtons(ConfigThingy conf, URL confUrl, int index)
  {
    try
    {
      if (conf == null)
      {
        conf = new ConfigThingy("Buttons", confUrl);
      }

      // damit ich parseGrandchildren() verwenden kann muss ich noch einen
      // Großelternknoten hinzufügen.
      conf = conf.query("Buttons", 0, 0);

      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Nimmt eine Menge von XTextRange Objekten, sucht alle umschlossenen Bookmarks und
   * broadcastet eine entsprechende Nachricht, damit sich die entsprechenden Objekte
   * selektieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void selectionChanged(XIndexAccess access)
  {
    Set<String> names = new HashSet<String>();

    int count = access.getCount();
    for (int i = 0; i < count; ++i)
    {
      XEnumerationAccess enuAccess = null;
      try
      {
        XTextRange range = UNO.XTextRange(access.getByIndex(i));
        enuAccess = UNO.XEnumerationAccess(range);
        handleParagraphEnumeration(names, enuAccess,
          UNO.XTextRangeCompare(range.getText()), range, false);
      }
      catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    if (!names.isEmpty()) broadcast(new BroadcastObjectSelectionByBookmarks(names));
  }

  public void mergeCheckBoxesIntoComboBox(ComboboxMergeDescriptor desc)
  {
    if (desc != null)
    {
      insertionModelList.mergeCheckboxesIntoCombobox(desc);
    }
  }
  
  /**
   * Ruft die Datei/Speichern Funktion von Office.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void save()
  {
    flushChanges();
    UNO.dispatch(documentController.getModel().doc, ".uno:Save");
  }
  
  /**
   * Ruft die Datei/Speichern unter... Funktion von Office.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveAs()
  {
    flushChanges();
    UNO.dispatch(documentController.getModel().doc, ".uno:SaveAs");
  }
  
  public void sendAsEmail() {
	  flushChanges();
	  UNO.dispatch(documentController.getModel().doc, ".uno:SendMail");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void abort()
  {
    flushChanges();

    if (functionTester != null) functionTester.abort();

    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
    
    view.close();
    
    try
    {
      selectionSupplier.removeSelectionChangeListener(myXSelectionChangedListener);
    }
    catch (Exception x)
    {}
  }

  
  /**
   * Ruft {@link #updateDocument(TextDocumentModel)} auf, falls noch Änderungen
   * anstehen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void flushChanges()
  {
    if (writeChangesTimer.isRunning())
    {
      LOGGER.debug(L.m("Schreibe wartende Änderungen ins Dokument"));
      writeChangesTimer.stop();
      try
      {
        updateDocument(documentController);
      }
      catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }
  }

  /**
   * Speichert die aktuelle Formularbeschreibung im Dokument und aktualisiert
   * Bookmarks etc.
   *
   * @return die aktualisierte Formularbeschreibung
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private ConfigThingy updateDocument(TextDocumentController documentController)
  {
    LOGGER.debug(L.m("Übertrage Formularbeschreibung ins Dokument"));
    Map<String, ConfigThingy> mapFunctionNameToConfigThingy =
      new HashMap<String, ConfigThingy>();
    insertionModelList.updateDocument(mapFunctionNameToConfigThingy);
    sectionModelList.updateDocument();
    ConfigThingy conf = buildFormDescriptor(mapFunctionNameToConfigThingy);
    documentController.setFormDescription(new ConfigThingy(conf));
    return conf;
  }

  /**
   * Liefert ein ConfigThingy zurück, das den aktuellen Zustand der
   * Formularbeschreibung repräsentiert. Zum Exportieren der Formularbeschreibung
   * sollte {@link #updateDocument(XTextDocument)} verwendet werden.
   * 
   * @param mapFunctionNameToConfigThingy
   *          bildet einen Funktionsnamen auf ein ConfigThingy ab, dessen Wurzel der
   *          Funktionsname ist und dessen Inhalt eine Funktionsdefinition ist. Diese
   *          Funktionen ergeben den Funktionen-Abschnitt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private ConfigThingy buildFormDescriptor(
      Map<String, ConfigThingy> mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    ConfigThingy form = conf.add("Formular");
    form.add("TITLE").add(formTitle);
    if (plausiMarkerColor != null)
      form.add("PLAUSI_MARKER_COLOR").add(plausiMarkerColor);
    form.addChild(formControlModelList.export());
    form.addChild(groupModelList.export());
    if (funktionsDialogeAbschnitteConf.count() > 0)
    {
      for (ConfigThingy funktionsDialogeAbschnitt : funktionsDialogeAbschnitteConf)
      {
        form.addChild(funktionsDialogeAbschnitt);
      }
    }
    if (!mapFunctionNameToConfigThingy.isEmpty())
    {
      ConfigThingy funcs = form.add("Funktionen");
      Iterator<ConfigThingy> iter =
        mapFunctionNameToConfigThingy.values().iterator();
      while (iter.hasNext())
      {
        funcs.addChild(iter.next());
      }
    }
    return conf;
  }

  /**
   * Wertet formDescription sowie die Bookmarks von {@link #doc} aus und
   * initialisiert alle internen Strukturen entsprechend. Dies aktualisiert auch die
   * entsprechenden Views.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void initModelsAndViews(ConfigThingy formDescription)
  {
    idManager = new IDManager();
    formControlModelList.clear();
    parseGlobalFormInfo(formDescription);

    ConfigThingy fensterAbschnitte =
      formDescription.query("Formular").query("Fenster");
    Iterator<ConfigThingy> fensterAbschnittIterator = fensterAbschnitte.iterator();
    while (fensterAbschnittIterator.hasNext())
    {
      ConfigThingy fensterAbschnitt = fensterAbschnittIterator.next();
      Iterator<ConfigThingy> tabIter = fensterAbschnitt.iterator();
      while (tabIter.hasNext())
      {
        ConfigThingy tab = tabIter.next();
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
      formControlModelList.add(separatorTab, 0);
    }

    insertionModelList.clear();

    /*
     * Collect insertions via WollMux bookmarks
     */
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(documentController.getModel().doc);
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try
      {
        String bookmark = bookmarks[i];
        if (DocumentCommands.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel4InsertXValue(bookmark, bmSupp,
            functionSelectionProvider, this));
      }
      catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    /*
     * Collect insertions via InputUser textfields
     */
    XTextFieldsSupplier tfSupp = UNO.XTextFieldsSupplier(documentController.getModel().doc);
    XEnumeration enu = tfSupp.getTextFields().createEnumeration();
    while (enu.hasMoreElements())
    {
      try
      {
        Object tf = enu.nextElement();
        XServiceInfo info = UNO.XServiceInfo(tf);
        if (info.supportsService("com.sun.star.text.TextField.InputUser"))
        {
          Matcher m =
            TextDocumentModel.INPUT_USER_FUNCTION.matcher(UNO.getProperty(tf,
              "Content").toString());

          if (m.matches())
            insertionModelList.add(new InsertionModel4InputUser(tf, documentController.getModel().doc,
              functionSelectionProvider, this));
        }
      }
      catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    groupModelList.clear();
    ConfigThingy visibilityConf =
      formDescription.query("Formular").query("Sichtbarkeit");
    Iterator<ConfigThingy> sichtbarkeitsAbschnittIterator =
      visibilityConf.iterator();
    while (sichtbarkeitsAbschnittIterator.hasNext())
    {
      ConfigThingy sichtbarkeitsAbschnitt = sichtbarkeitsAbschnittIterator.next();
      Iterator<ConfigThingy> sichtbarkeitsFunktionIterator =
        sichtbarkeitsAbschnitt.iterator();
      while (sichtbarkeitsFunktionIterator.hasNext())
      {
        ConfigThingy sichtbarkeitsFunktion = sichtbarkeitsFunktionIterator.next();
        String groupName = sichtbarkeitsFunktion.getName();
        try
        {
          IDManager.ID groupId =
            getIDManager().getActiveID(NAMESPACE_GROUPS, groupName);
          FunctionSelection funcSel =
            visibilityFunctionSelectionProvider.getFunctionSelection(groupName);
          groupModelList.add(new GroupModel(groupId, funcSel, this));
        }
        catch (DuplicateIDException x)
        {
          /*
           * Kein Problem. Wir haben die entsprechende Sichtbarkeitsgruppe schon
           * angelegt. Die Initialisierung des visibilityFunctionSelectionProviders
           * sorgt dafür, dass bei mehrfachen Deklarationen der selben
           * Sichtbarkeitsgruppe die letzte gewinnt. Der obige
           * getFunctionSelection()-Aufruf liefert nur noch die letzte Definition.
           */
        }
      }
    }

    sectionModelList.clear();
    XTextSectionsSupplier tsSupp = UNO.XTextSectionsSupplier(documentController.getModel().doc);
    XNameAccess textSections = tsSupp.getTextSections();
    String[] sectionNames = textSections.getElementNames();
    for (String sectionName : sectionNames)
    {
      sectionModelList.add(new SectionModel(sectionName, tsSupp, this));
    }
  }

  /**
   * Extrahiert aus conf die globalen Eingenschaften des Formulars wie z,B, den
   * Formulartitel oder die Funktionen des Funktionen-Abschnitts.
   * 
   * @param conf
   *          der WM-Knoten der über einer beliebigen Anzahl von Formular-Knoten
   *          sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void parseGlobalFormInfo(ConfigThingy conf)
  {
    ConfigThingy tempConf = conf.query("Formular").query("TITLE");

    if (tempConf.count() > 0)
    {
      formTitle = tempConf.toString();
    }
    tempConf = conf.query("Formular").query("PLAUSI_MARKER_COLOR");

    if (tempConf.count() > 0)
    {
      plausiMarkerColor = tempConf.toString();
    }

    funktionsDialogeAbschnitteConf =
      conf.query("Formular").query("Funktionsdialoge", 2);
    tempConf = conf.query("Formular").query("Funktionen");

    if (tempConf.count() >= 1)
    {
      try
      {
        tempConf = tempConf.getFirstChild();
      }
      catch (Exception x)
      {}
    }
    else
    {
      tempConf = new ConfigThingy("Funktionen");
    }
    functionSelectionProvider =
      new FunctionSelectionProvider(functionLibrary, tempConf, getIDManager(),
        NAMESPACE_FORMCONTROLMODEL);

    tempConf = conf.query("Formular").query("Sichtbarkeit");
    if (tempConf.count() >= 1)
    {
      try
      {
        tempConf = tempConf.getFirstChild();
      }
      catch (Exception x)
      {}
    }
    else
    {
      tempConf = new ConfigThingy("Sichtbarkeit");
    }
    visibilityFunctionSelectionProvider =
      new FunctionSelectionProvider(null, tempConf, getIDManager(),
        NAMESPACE_FORMCONTROLMODEL);
  }

  /**
   * Parst das Tab conf und fügt entsprechende FormControlModels der
   * {@link #formControlModelList} hinzu.
   * 
   * @param conf
   *          der Knoten direkt über "Eingabefelder" und "Buttons".
   * @param idx
   *          falls >= 0 werden die Steuerelemente am entsprechenden Index der Liste
   *          in die Formularbeschreibung eingefügt, ansonsten ans Ende angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void parseTab(ConfigThingy conf, int index)
  {
    int idx = index;
    String id = conf.getName();
    String label = id;
    String action = FormControlModel.NO_ACTION;
    String tooltip = "";
    char hotkey = 0;

    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("TITLE"))
      {
        label = str;
      }
      else if (name.equals("CLOSEACTION"))
      {
        action = str;
      }
      else if (name.equals("TIP"))
      {
        tooltip = str;
      }
      else if (name.equals("HOTKEY"))
      {
        hotkey = (str.length() > 0) ? str.charAt(0) : 0;
      }
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
   * 
   * @param idx
   *          falls >= 0 werden die Steuerelemente am entsprechenden Index der Liste
   *          in die Formularbeschreibung eingefügt, ansonsten ans Ende angehängt.
   * @param killLastGlue
   *          falls true wird das letzte Steuerelement entfernt, wenn es ein glue
   *          ist.
   * @return die Anzahl der erzeugten Steuerelemente.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private int parseGrandchildren(ConfigThingy grandma, int index,
      boolean killLastGlue)
  {
    int idx = index;
    if (idx < 0)
    {
      idx = formControlModelList.size();
    }

    boolean lastIsGlue = false;
    FormControlModel model = null;
    int count = 0;
    Iterator<ConfigThingy> grandmaIter = grandma.iterator();
    while (grandmaIter.hasNext())
    {
      Iterator<ConfigThingy> iter = grandmaIter.next().iterator();
      while (iter.hasNext())
      {
        model = new FormControlModel(iter.next(), functionSelectionProvider, this);
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
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void scan()
  {
    try
    {
      XDocumentProperties info =
        UNO.XDocumentPropertiesSupplier(documentController.getModel().doc).getDocumentProperties();
      try
      {
        String title = ((String) UNO.getProperty(info, "Title")).trim();
        if (formTitle == GENERATED_FORM_TITLE && title.length() > 0)
        {
          formTitle = title;
        }
      }
      catch (Exception x)
      {}
      DocumentTreeVisitor visitor = new ScanVisitor(this);
      visitor.visit(documentController.getModel().doc);
    }
    catch (Exception x)
    {
      LOGGER.error(L.m("Fehler während des Scan-Vorgangs"), x);
    }

    documentNeedsUpdating();
  }

  /**
   * Falls enuAccess != null, wird die entsprechende XEnumeration genommen und ihre
   * Elemente als Paragraphen bzw TextTables interpretiert, deren Inhalt enumeriert
   * wird, um daraus alle enthaltenen Bookmarks UND InputUser Felder zu bestimmen und
   * ihre Namen zu names hinzuzufügen.
   * 
   * @param doCompare
   *          if true, then text portions will be ignored if they lie outside of
   *          range (as tested with compare). Text portions inside of tables are
   *          always checked, regardless of doCompare.
   * 
   * @throws NoSuchElementException
   * @throws WrappedTargetException
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void handleParagraphEnumeration(Set<String> names,
      XEnumerationAccess enuAccess, XTextRangeCompare compare, XTextRange range,
      boolean doCompare) throws NoSuchElementException, WrappedTargetException
  {
    if (enuAccess != null)
    {
      XEnumeration paraEnu = enuAccess.createEnumeration();
      while (paraEnu.hasMoreElements())
      {
        Object nextEle = paraEnu.nextElement();
        if (nextEle == null)
          throw new NullPointerException(
            L.m("nextElement() == null obwohl hasMoreElements()==true"));

        XEnumerationAccess xs = UNO.XEnumerationAccess(nextEle);
        if (xs != null)
          handleParagraph(names, xs, compare, range, doCompare);
        else
        {// unterstützt nicht XEnumerationAccess, ist wohl SwXTextTable
          XTextTable table = UNO.XTextTable(nextEle);
          if (table != null) handleTextTable(names, table, compare, range);
        }
      }
    }
  }

  /**
   * Enumeriert über die Zellen von table und ruft für jede
   * {@link #handleParagraph(Set, XEnumerationAccess, XTextRangeCompare, XTextRange, boolean)}
   * auf, wobei für doCompare immer true übergeben wird.
   * 
   * @throws NoSuchElementException
   * @throws WrappedTargetException
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void handleTextTable(Set<String> names, XTextTable table,
      XTextRangeCompare compare, XTextRange range) throws NoSuchElementException,
      WrappedTargetException
  {
    String[] cellNames = table.getCellNames();
    for (int i = 0; i < cellNames.length; ++i)
    {
      XCell cell = table.getCellByName(cellNames[i]);
      handleParagraphEnumeration(names, UNO.XEnumerationAccess(cell), compare,
        range, true);
    }
  }

  /**
   * Enumeriert über die TextPortions des Paragraphen para und sammelt alle Bookmarks
   * und InputUser-Felder darin auf und fügt ihre Namen zu names hinzu.
   * 
   * @param doCompare
   *          if true, then text portions will be ignored if they lie outside of
   *          range (as tested with compare). Text portions inside of tables are
   *          always checked, regardless of doCompare.
   * 
   * @throws NoSuchElementException
   * @throws WrappedTargetException
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void handleParagraph(Set<String> names, XEnumerationAccess para,
      XTextRangeCompare compare, XTextRange range, boolean doCompare)
      throws NoSuchElementException, WrappedTargetException
  {
    XEnumeration textportionEnu = para.createEnumeration();
    while (textportionEnu.hasMoreElements())
    {
      Object textportion = textportionEnu.nextElement();
      String type = (String) Utils.getProperty(textportion, "TextPortionType");
      if ("Bookmark".equals(type)) // String constant first b/c type may be null
      {
        if (isInvalidRange(compare, range, textportion, doCompare)) continue;
        XNamed bookmark = null;
        try
        {
          // boolean isStart = ((Boolean)UNO.getProperty(textportion,
          // "IsStart")).booleanValue();
          bookmark = UNO.XNamed(UNO.getProperty(textportion, "Bookmark"));
          names.add(bookmark.getName());
        }
        catch (Exception x)
        {
          LOGGER.error("", x);
        }
      }
      else if ("TextField".equals(type)) // String const first b/c type may be null
      {
        XDependentTextField textField = null;
        try
        {
          textField =
            UNO.XDependentTextField(UNO.getProperty(textportion, "TextField"));
          XServiceInfo info = UNO.XServiceInfo(textField);
          if (info.supportsService("com.sun.star.text.TextField.InputUser"))
          {
            if (isInvalidRange(compare, range, textportion, doCompare)) continue;
            names.add((String) UNO.getProperty(textField, "Content"));
          }
        }
        catch (Exception x)
        {
          LOGGER.error("", x);
        }
      }
    }
  }

  /**
   * Returns true iff (doCompare == false OR range2 is null or not an XTextRange OR
   * range2 lies inside of range (tested with compare)).
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  private boolean isInvalidRange(XTextRangeCompare compare, XTextRange range,
      Object range2, boolean doCompare)
  {
    XTextRange compareRange = UNO.XTextRange(range2);
    if (doCompare && compareRange != null)
    {
      try
      {
        if (compare.compareRegionStarts(range, compareRange) < 0) return true;
        if (compare.compareRegionEnds(range, compareRange) > 0) return true;
      }
      catch (Exception x)
      {
        return true;
        /*
         * Do not Logger.error(x); because the most likely cause for an exception is
         * that range2 does not belong to the text object compare, which happens in
         * tables, because when enumerating over a range inside of a table the
         * enumeration hits a lot of unrelated cells (OOo bug).
         */
      }
    }
    return false;
  }
  
  private boolean testMemoryRequirements()
  {
    // Zuerst überprüfen wir, ob das Formular eine kritische Anzahl an FormControls
    // sowie eine niedrige Einstellung für die Java Heap Size hat, die zu
    // OutOfMemoryErrors führen könnte. Wenn ja, wird eine entsprechende Meldung
    // ausgegeben, dass der Benutzer seine Java-Einstellungen ändern soll und
    // der FormularMax wird nicht gestartet.
    int formControlCount = documentController.getFormDescription().query("TYPE", 6, 6).count();
    long maxMemory = Runtime.getRuntime().maxMemory();
    if (formControlCount > CRITICAL_NUMBER_OF_FORMCONTROLS
      && maxMemory < LOWEST_ALLOWED_HEAP_SIZE)
    {
      LOGGER.info(L.m(
        "Starten von FormularMax beim Bearbeiten von Dokument '%1' abgebrochen, da maximale Java Heap Size = %2 bytes und Anzahl FormControls = %3",
        documentController.getFrameController().getTitle(), maxMemory, formControlCount));
      JOptionPane.showMessageDialog(
        view,
        L.m("Der FormularMax 4000 kann nicht ausgeführt werden, da der Java-Laufzeitumgebung zu wenig Hauptspeicher zur Verfügung steht.\n"
          + "Bitte ändern Sie in Office Ihre Java-Einstellungen. Sie finden diese unter \"Extras->Optionen->LibreOffice->Erweitert\".\n"
          + "Dort wählen Sie in der Liste Ihre aktuelle Java-Laufzeitumgebung aus, klicken auf den Button \"Parameter\",\n"
          + "tragen den neuen Parameter \"-Xmx256m\" ein (Groß-/Kleinschreibung beachten!) und klicken auf \"Zuweisen\".\n"
          + "Danach ist ein Neustart von Office nötig."),
        L.m("Java Heap Size zu gering"), JOptionPane.ERROR_MESSAGE);
      DocumentManager.getDocumentManager().setCurrentFormularMax4000(documentController.getModel().doc, null);
      return false;
    }
    
    return true;
  }

  private class MyXSelectionChangedListener implements XSelectionChangeListener
  {
    @Override
    public void selectionChanged(EventObject arg0)
    {
      try
      {
        Object selection =
          AnyConverter.toObject(XInterface.class, selectionSupplier.getSelection());
        final XIndexAccess access = UNO.XIndexAccess(selection);
        if (access == null) return;
        try
        {
          javax.swing.SwingUtilities.invokeLater(new Runnable()
          {
            @Override
            public void run()
            {
              try
              {
                FormularMax4kController.this.selectionChanged(access);
              }
              catch (Exception x)
              {}
            }
          });
        }
        catch (Exception x)
        {}
      }
      catch (IllegalArgumentException e)
      {
        LOGGER.error(L.m("Kann Selection nicht in Objekt umwandeln"), e);
      }
    }

    @Override
    public void disposing(EventObject arg0)
    {}
  }
}