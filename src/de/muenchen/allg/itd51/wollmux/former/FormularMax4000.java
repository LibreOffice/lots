/*
 * Dateiname: FormularMax4000.java
 * Projekt  : WollMux
 * Funktion : Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 * 
 * Copyright (c) 2010 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
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
 * 19.07.2007 | BNK | [R5406]Views und Teile der Views können nach Benutzerwunsch ein- oder ausgeblendet werden
 *                  | Änderung der Menüstruktur (Einführung Ansicht und Bearbeiten Menü, Einfügen wandert nach Bearbeiten)
 *                  | JSplitPane besser initialisiert, um verschieben des Dividers zu verbessern.
 * 01.08.2007 | BNK | FunctionTester eingebaut      
 * 10.12.2007 | BNK | [R11302]intelligentere Behandlung von Leerzeichen am Ende von gender-Dropdown-Listen
 * 13.01.2010 | BED | [R67584]FormularMax öffnet sich bei großen Formularen und niedriger JVM Heap Size nicht mehr, sondern bringt entsprechende Meldung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
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
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Container;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.TextRange;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Visitor;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionTester;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel4InputUser;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel4InsertXValue;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModel;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModelList;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

/**
 * Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormularMax4000
{
  /**
   * Default-Name für ein neues Tab.
   */
  public static final String STANDARD_TAB_NAME = L.m("Tab");

  /**
   * Die Namen der Parameter, die die Gender-Trafo erwartet. ACHTUNG! Diese müssen
   * exakt mit den Parametern der Gender()-Funktion aus der WollMux-Konfig
   * übereinstimmen. Insbesondere dürfen sie nicht übersetzt werden, ohne dass die
   * Gender()-Funktion angepasst wird. Und falls die Gender()-Funktion geändert wird,
   * dann funktionieren existierende Formulare nicht mehr.
   */
  private static final String[] GENDER_TRAFO_PARAMS = new String[] {
    "Falls_Anrede_HerrN", "Falls_Anrede_Frau", "Falls_sonstige_Anrede", "Anrede" };

  /**
   * Regex für Test ob String mit Buchstabe oder Underscore beginnt. ACHTUNG! Das .*
   * am Ende ist notwendig, da String.matches() immer den ganzen String testet.
   */
  private static final String STARTS_WITH_LETTER_RE = "^[a-zA-Z_].*";

  /**
   * Der Standard-Formulartitel, solange kein anderer gesetzt wird.
   */
  private static final String GENERATED_FORM_TITLE =
    L.m("Generiert durch FormularMax 4000");

  /**
   * Maximale Anzahl Zeichen für ein automatisch generiertes Label.
   */
  private static final int GENERATED_LABEL_MAXLENGTH = 30;

  /**
   * URL des Quelltexts für den Standard-Empfängerauswahl-Tab.
   */
  private final URL EMPFAENGER_TAB_URL =
    this.getClass().getClassLoader().getResource(
      "data/empfaengerauswahl_controls.conf");

  /**
   * URL des Quelltexts für die Standardbuttons für einen mittleren Tab.
   */
  private final URL STANDARD_BUTTONS_MIDDLE_URL =
    this.getClass().getClassLoader().getResource("data/standardbuttons_mitte.conf");

  /**
   * URL des Quelltexts für die Standardbuttons für den letzten Tab.
   */
  private final URL STANDARD_BUTTONS_LAST_URL =
    this.getClass().getClassLoader().getResource("data/standardbuttons_letztes.conf");

  /**
   * Beim Import neuer Formularfelder oder Checkboxen schaut der FormularMax4000 nach
   * speziellen Hinweisen/Namen/Einträgen, die diesem Muster entsprechen. Diese
   * Zusatzinformationen werden herangezogen um Labels, IDs und andere Informationen
   * zu bestimmen.
   * 
   * >>>>Eingabefeld<<<<: Als "Hinweis" kann "Label<<ID>>" angegeben werden und wird
   * beim Import entsprechend berücksichtigt. Wird nur "<<ID>>" angegeben, so
   * markiert das Eingabefeld eine reine Einfügestelle (insertValue oder
   * insertContent) und beim Import wird dafür kein Formularsteuerelement erzeugt.
   * Wird ID ein "glob:" vorangestellt, so wird gleich ein insertValue-Bookmark
   * erstellt.
   * 
   * >>>>>Eingabeliste/Dropdown<<<<<: Als "Name" kann "Label<<ID>>" angegeben werden
   * und wird beim Import berücksichtigt. Als Spezialeintrag in der Liste kann
   * "<<Freitext>>" eingetragen werden und signalisiert dem FM4000, dass die ComboBox
   * im Formular auch die Freitexteingabe erlauben soll. Wie bei Eingabefeldern auch
   * ist die Angabe "<<ID>>" ohne Label möglich und signalisiert, dass es sich um
   * eine reine Einfügestelle handelt, die kein Formularelement erzeugen soll. Wird
   * als "Name" die Spezialsyntax "<<gender:ID>>" verwendet, so wird eine reine
   * Einfügestelle erzeugt, die mit einer Gender-TRAFO versehen wird, die abhängig
   * vom Formularfeld ID einen der Werte des Dropdowns auswählt, und zwar bei "Herr"
   * oder "Herrn" den ersten Eintrag, bei "Frau" den zweiten Eintrag und bei allem
   * sonstigen den dritten Eintrag. Hat das Dropdown nur 2 Einträge, so wird im
   * sonstigen Fall das Feld ID untransformiert übernommen. Falls vorhanden werden
   * bis zu N-1 Leerzeichen am Ende eines Eintrages der Dropdown-Liste entfernt,
   * wobei N die Anzahl der Einträge ist, die bis auf folgende Leerzeichen identisch
   * zu diesem Eintrag sind. Dies ermöglicht es, das selbe Wort mehrfach in die Liste
   * aufzunehmen.
   * 
   * >>>>>Checkbox<<<<<: Bei Checkboxen kann als "Hilfetext" "Label<<ID>>" angegeben
   * werden und wird beim Import entsprechend berücksichtigt.
   * 
   * Technischer Hinweis: Auf dieses Pattern getestet wird grundsätzlich der String,
   * der von {@link DocumentTree.FormControl#getDescriptor()} geliefert wird.
   * 
   */
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN =
    Pattern.compile("\\A(.*)<<(.*)>>\\z");

  /**
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertValue anstatt eines insertFormValue erzeugt werden soll.
   */
  private static final String GLOBAL_PREFIX = "glob:";

  /**
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertFormValue mit Gender-TRAFO erzeugt werden soll.
   */
  private static final String GENDER_PREFIX = "gender:";

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
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der FM4000 geschlossen
   * wurde.
   */
  private ActionListener abortListener = null;

  /**
   * Das Haupt-Fenster des FormularMax4000.
   */
  private JFrame myFrame;

  /**
   * Oberster Container der FM4000 GUI-Elemente. Wird direkt in die ContentPane von
   * myFrame gesteckt.
   */
  private JSplitPane mainContentPanel;

  /**
   * Die normale Größe des Dividers von {@link #mainContentPanel}.
   */
  private int defaultDividerSize;

  /**
   * Oberster Container für den Quelltexteditor. Wird direkt in die ContentPane von
   * myFrame gesteckt.
   */
  private JPanel editorContentPanel;

  /**
   * Der Übercontainer für die linke Hälfte des FM4000.
   */
  private LeftPanel leftPanel;

  /**
   * Der Übercontainer für die rechte Hälfte des FM4000.
   */
  private RightPanel rightPanel;

  /**
   * Ein JPanel mit minimaler und bevorzugter Größe 0, das für die rechte Seite des
   * FM4000 verwendet wird, wenn diese ausgeblendet sein soll.
   */
  private JPanel nonExistingRightPanel;

  /**
   * Beschreibt die aktuellen Sichtbarkeitseinstellungen des Benutzers.
   */
  private ViewVisibilityDescriptor viewVisibilityDescriptor =
    new ViewVisibilityDescriptor();

  /**
   * GUI zum interaktiven Zusammenbauen und Testen von Funktionen.
   */
  private FunctionTester functionTester = null;

  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;

  /**
   * Wert von PLAUSI_MARKER_COLOR oder null wenn nicht gesetzt.
   */
  private String plausiMarkerColor = null;

  /**
   * Das TextDocumentModel, zu dem das Dokument doc gehört.
   */
  private TextDocumentModel doc;

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
    new Vector<BroadcastListener>();

  /**
   * Wird auf myFrame registriert, damit zum Schließen des Fensters abort()
   * aufgerufen wird.
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
  private Vector<String> printFunctionNames;

  /**
   * Wird bei jeder Änderung von Formularaspekten gestartet, um nach einer
   * Verzögerung die Änderungen in das Dokument zu übertragen.
   */
  private Timer writeChangesTimer;

  /**
   * Der XSelectionSupplier des Dokuments.
   */
  private XSelectionSupplier selectionSupplier;

  /**
   * Wird auf {@link #selectionSupplier} registriert, um Änderungen der
   * Cursorselektion zu beobachten.
   */
  private MyXSelectionChangedListener myXSelectionChangedListener;

  /**
   * Speichert die Funktionsdialoge-Abschnitte des Formulars. Der FM4000 macht
   * derzeit nichts besonderes mit ihnen, sondern schreibt sie einfach nur ins
   * Dokument zurück.
   */
  private ConfigThingy funktionsDialogeAbschnitteConf;

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

  /**
   * Sendet die Nachricht b an alle Listener, die auf dem globalen Broadcast-Kanal
   * registriert sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void broadcast(Broadcast b)
  {
    Iterator<BroadcastListener> iter = broadcastListeners.iterator();
    while (iter.hasNext())
    {
      b.sendTo(iter.next());
    }
  }

  /**
   * listener wird über globale {@link Broadcast}s informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void addBroadcastListener(BroadcastListener listener)
  {
    if (!broadcastListeners.contains(listener)) broadcastListeners.add(listener);
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
   * Liefert den {@link IDManager}, der für Objekte im Formular verwendet wird.
   * 
   * @see #NAMESPACE_FORMCONTROLMODEL
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public IDManager getIDManager()
  {
    return idManager;
  }

  /**
   * Startet eine Instanz des FormularMax 4000 für das Dokument des
   * TextDocumentModels model.
   * 
   * @param abortListener
   *          (falls nicht null) wird aufgerufen, nachdem der FormularMax 4000
   *          geschlossen wurde.
   * @param funcLib
   *          Funktionsbibliothek, die globale Funktionen zur Verfügung stellt.
   * @param printFuncLib
   *          Funktionsbibliothek, die Druckfunktionen zur Verfügung stellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000(TextDocumentModel model, ActionListener abortListener,
      FunctionLibrary funcLib, PrintFunctionLibrary printFuncLib)
  {
    this.doc = model;
    this.abortListener = abortListener;
    this.functionLibrary = funcLib;
    this.printFunctionNames = new Vector<String>(printFuncLib.getFunctionNames());

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            createGUI();
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  private void createGUI()
  {
    Common.setLookAndFeelOnce();

    // Zuerst überprüfen wir, ob das Formular eine kritische Anzahl an FormControls
    // sowie eine niedrige Einstellung für die Java Heap Size hat, die zu
    // OutOfMemoryErrors führen könnte. Wenn ja, wird eine entsprechende Meldung
    // ausgegeben, dass der Benutzer seine Java-Einstellungen ändern soll und
    // der FormularMax wird nicht gestartet.
    int formControlCount = doc.getFormDescription().query("TYPE", 6, 6).count();
    long maxMemory = Runtime.getRuntime().maxMemory();
    if (formControlCount > CRITICAL_NUMBER_OF_FORMCONTROLS
      && maxMemory < LOWEST_ALLOWED_HEAP_SIZE)
    {
      Logger.log(L.m(
        "Starten von FormularMax beim Bearbeiten von Dokument '%1' abgebrochen, da maximale Java Heap Size = %2 bytes und Anzahl FormControls = %3",
        doc.getTitle(), maxMemory, formControlCount));
      JOptionPane.showMessageDialog(
        myFrame,
        L.m("Der FormularMax 4000 kann nicht ausgeführt werden, da der Java-Laufzeitumgebung zu wenig Hauptspeicher zur Verfügung steht.\n"
          + "Bitte ändern Sie in OpenOffice.org Ihre Java-Einstellungen. Sie finden diese unter \"Extras->Optionen->OpenOffice.org->Java\".\n"
          + "Dort wählen Sie in der Liste Ihre aktuelle Java-Laufzeitumgebung aus, klicken auf den Button \"Parameter\",\n"
          + "tragen den neuen Parameter \"-Xmx256m\" ein (Groß-/Kleinschreibung beachten!) und klicken auf \"Zuweisen\".\n"
          + "Danach ist ein Neustart von OpenOffice.org nötig."),
        L.m("Java Heap Size zu gering"), JOptionPane.ERROR_MESSAGE);
      doc.setCurrentFormularMax4000(null);
      return;
    }

    formControlModelList = new FormControlModelList(this);
    insertionModelList = new InsertionModelList(this);
    groupModelList = new GroupModelList(this);
    sectionModelList = new SectionModelList(this);

    // Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    // der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert
    // wird
    myFrame.addWindowListener(oehrchen);

    // WollMux-Icon für das Frame
    Common.setWollMuxIcon(myFrame);

    leftPanel =
      new LeftPanel(insertionModelList, formControlModelList, groupModelList,
        sectionModelList, this, doc.doc);
    rightPanel =
      new RightPanel(insertionModelList, formControlModelList, groupModelList,
        sectionModelList, functionLibrary, this);

    // damit sich Slider von JSplitPane vernünftig bewegen lässt.
    rightPanel.JComponent().setMinimumSize(new Dimension(100, 0));
    nonExistingRightPanel = new JPanel();
    nonExistingRightPanel.setMinimumSize(new Dimension(0, 0));
    nonExistingRightPanel.setPreferredSize(nonExistingRightPanel.getMinimumSize());
    nonExistingRightPanel.setMaximumSize(nonExistingRightPanel.getMinimumSize());
    mainContentPanel =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel.JComponent(),
        nonExistingRightPanel);
    mainContentPanel.setResizeWeight(1.0);
    defaultDividerSize = mainContentPanel.getDividerSize();
    mainContentPanel.setDividerSize(0);

    myFrame.getContentPane().add(mainContentPanel);

    mainMenuBar = new JMenuBar();
    // ========================= Datei ============================
    JMenu menu = new JMenu(L.m("Datei"));

    JMenuItem menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        save(doc);
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Speichern unter..."));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        saveAs(doc);
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Beenden"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Bearbeiten ============================
    menu = new JMenu(L.m("Bearbeiten"));

    // ========================= Bearbeiten/Einfügen ============================
    JMenu submenu = new JMenu(L.m("Standardelemente einfügen"));

    if (!createStandardelementeMenuNew(submenu))
      createStandardelementeMenuOld(submenu);

    menu.add(submenu);
    // =================== Bearbeiten (Fortsetzung) ============================

    menu.addSeparator();

    menuItem = new JMenuItem(L.m("Checkboxen zu ComboBox"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ComboboxMergeDescriptor desc = leftPanel.mergeCheckboxesIntoCombobox();
        if (desc != null) insertionModelList.mergeCheckboxesIntoCombobox(desc);
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Ansicht ============================
    menu = new JMenu(L.m("Ansicht"));

    menuItem = new JCheckBoxMenuItem("ID");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewId =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewId);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("LABEL");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewLabel =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewLabel);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TYPE");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewType =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewType);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem(L.m("Elementspezifische Felder"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewAdditional =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewAdditional);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TRAFO, PLAUSI, AUTOFILL, GROUPS");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (((AbstractButton) e.getSource()).isSelected())
        {
          mainContentPanel.setDividerSize(defaultDividerSize);
          mainContentPanel.setRightComponent(rightPanel.JComponent());
          mainContentPanel.setResizeWeight(0.6);
        }
        else
        {
          mainContentPanel.setDividerSize(0);
          mainContentPanel.setRightComponent(nonExistingRightPanel);
          mainContentPanel.setResizeWeight(1.0);
        }
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menu.addSeparator();
    menuItem = new JMenuItem(L.m("Funktionstester"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (functionTester == null)
        {
          functionTester = new FunctionTester(functionLibrary, new ActionListener()
          {
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
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Formular ============================
    menu = new JMenu(L.m("Formular"));

    menuItem = new JMenuItem(L.m("Formularfelder aus Vorlage einlesen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        scan(doc.doc);
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Formulartitel setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setFormTitle();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Druckfunktionen setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setPrintFunction();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Dateiname vorgeben"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setFilenameGeneratorFunction();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("WollMux-Formularmerkmale aus Vorlage entfernen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        deForm(doc);
      }
    });
    menu.add(menuItem);

    /*
     * Das Entfernen von Bookmarks kann Referenzfelder (Felder die Kopien anderer
     * Teile des Dokuments enthalten) zerstören, da diese dann ins Leere greifen.
     * Solange dies nicht erkannt wird, muss die Funktion deaktiviert sein.
     */
    if (Integer.valueOf(3).equals(Integer.valueOf(0)))
    {
      menuItem = new JMenuItem(L.m("Ladezeit des Dokuments optimieren"));
      menuItem.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          removeNonWMBookmarks(doc);
        }
      });
      menu.add(menuItem);
    }

    menuItem = new JMenuItem(L.m("Formularbeschreibung editieren"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);

    myFrame.setJMenuBar(mainMenuBar);

    writeChangesTimer = new Timer(500, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        updateDocument(doc);
      }
    });
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
   * Wertet den Konfigurationsabschnitt FormularMax4000/Standardelemente aus und fügt
   * submenu entsprechende Einträge hinzu.
   * 
   * @return false, falls der Konfigurationsabschnitt nicht existiert.
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private boolean createStandardelementeMenuNew(JMenu submenu)
  {
    boolean found = false;
    for (ConfigThingy fm4000conf : WollMuxSingleton.getInstance().getWollmuxConf().query(
      "FormularMax4000", 1))
    {
      for (ConfigThingy eleConf : fm4000conf.query("Standardelemente", 1))
      {
        found = true;
        for (ConfigThingy conf : eleConf)
        {
          try
          {
            String label = conf.get("LABEL", 1).toString();
            ConfigThingy tabConf = conf.query("Tab", 1);
            if (tabConf.count() > 1)
              throw new ConfigurationErrorException(
                L.m("Mehr als ein Tab-Abschnitt"));
            if (tabConf.count() == 1)
            {
              JMenuItem menuItem;
              menuItem = new JMenuItem(L.m(label));
              final ConfigThingy tabConfEntry =
                tabConf.getFirstChild().getFirstChild();
              menuItem.addActionListener(new ActionListener()
              {
                public void actionPerformed(ActionEvent e)
                {
                  insertStandardTab(tabConfEntry, null);
                  setFrameSize();
                }
              });
              submenu.add(menuItem);

            }
            else
            {
              ConfigThingy buttonsConf = conf.query("Buttons", 1);
              if (buttonsConf.count() > 1)
                throw new ConfigurationErrorException(
                  L.m("Mehr als ein Buttons-Abschnitt"));
              if (buttonsConf.count() == 0)
                throw new ConfigurationErrorException(
                  L.m("Weder Tab noch Buttons-Abschnitt"));

              final ConfigThingy buttonsConfEntry = buttonsConf.getFirstChild();

              JMenuItem menuItem = new JMenuItem(L.m(label));
              menuItem.addActionListener(new ActionListener()
              {
                public void actionPerformed(ActionEvent e)
                {
                  insertStandardButtons(buttonsConfEntry, null);
                  setFrameSize();
                }
              });
              submenu.add(menuItem);

            }
          }
          catch (Exception x)
          {
            Logger.error(
              L.m("Fehler beim Parsen des Abschnitts FormularMax4000/Standardelemente"),
              x);
          }
        }
      }
    }
    return found;
  }

  /**
   * Fügt submenu die alten im WollMux gespeicherten Standardelemente-Einträge hinzu.
   * Sollte nur verwendet werden, wenn der entsprechende Konfigurationsabschnitt in
   * der wollmux,conf fehlt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private void createStandardelementeMenuOld(JMenu submenu)
  {
    JMenuItem menuItem;
    menuItem = new JMenuItem(L.m("Empfängerauswahl-Tab"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardTab(null, EMPFAENGER_TAB_URL);
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zurück, Weiter->"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtons(null, STANDARD_BUTTONS_MIDDLE_URL);
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zurück, PDF, Drucken"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtons(null, STANDARD_BUTTONS_LAST_URL);
        setFrameSize();
      }
    });
    submenu.add(menuItem);
  }

  /**
   * Wertet formDescription sowie die Bookmarks von {@link #doc} aus und
   * initialisiert alle internen Strukturen entsprechend. Dies aktualisiert auch die
   * entsprechenden Views.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initModelsAndViews(ConfigThingy formDescription)
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
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc.doc);
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try
      {
        String bookmark = bookmarks[i];
        if (InsertionModel4InsertXValue.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel4InsertXValue(bookmark, bmSupp,
            functionSelectionProvider, this));
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    /*
     * Collect insertions via InputUser textfields
     */
    XTextFieldsSupplier tfSupp = UNO.XTextFieldsSupplier(doc.doc);
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
            InsertionModel4InputUser.INPUT_USER_FUNCTION.matcher(UNO.getProperty(tf,
              "Content").toString());

          if (m.matches())
            insertionModelList.add(new InsertionModel4InputUser(tf, doc.doc,
              functionSelectionProvider, this));
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
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
    XTextSectionsSupplier tsSupp = UNO.XTextSectionsSupplier(doc.doc);
    XNameAccess textSections = tsSupp.getTextSections();
    String[] sectionNames = textSections.getElementNames();
    for (String sectionName : sectionNames)
    {
      sectionModelList.add(new SectionModel(sectionName, tsSupp, this));
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
    String newTitle =
      JOptionPane.showInputDialog(myFrame, L.m("Bitte Formulartitel eingeben"),
        formTitle);
    if (newTitle != null)
    {
      formTitle = newTitle;
      documentNeedsUpdating();
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
  private ConfigThingy updateDocument(TextDocumentModel doc)
  {
    Logger.debug(L.m("Übertrage Formularbeschreibung ins Dokument"));
    Map<String, ConfigThingy> mapFunctionNameToConfigThingy =
      new HashMap<String, ConfigThingy>();
    insertionModelList.updateDocument(mapFunctionNameToConfigThingy);
    sectionModelList.updateDocument();
    ConfigThingy conf = buildFormDescriptor(mapFunctionNameToConfigThingy);
    doc.setFormDescription(new ConfigThingy(conf));
    return conf;
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
      Logger.debug(L.m("Schreibe wartende Änderungen ins Dokument"));
      writeChangesTimer.stop();
      try
      {
        updateDocument(doc);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      ;
    }
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
    if (tempConf.count() > 0) formTitle = tempConf.toString();
    tempConf = conf.query("Formular").query("PLAUSI_MARKER_COLOR");
    if (tempConf.count() > 0) plausiMarkerColor = tempConf.toString();
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
   * Fügt am Anfang der Liste einem Tab ein, dessen Konfiguration aus tabConf kommt
   * (Wurzelknoten wird ignoriert, darunter sollten TITLE, Eingabefelder etc, liegen)
   * falls tabConf != null, ansonsten aus einem ConfigThingy was an der URL
   * tabConfUrl gespeichert ist (hier sind TITLE, Eingabefelder, etc, auf oberster
   * Ebene).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardTab(ConfigThingy tabConf, URL tabConfUrl)
  {
    try
    {
      if (tabConf == null)
        tabConf = new ConfigThingy("Empfaengerauswahl", tabConfUrl);
      parseTab(tabConf, 0);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      Logger.error(x);
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
  private void insertStandardButtons(ConfigThingy conf, URL confUrl)
  {
    try
    {
      if (conf == null) conf = new ConfigThingy("Buttons", confUrl);

      // damit ich parseGrandchildren() verwenden kann muss ich noch einen
      // Großelternknoten hinzufügen.
      conf = conf.query("Buttons", 0, 0);

      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
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
  private void parseTab(ConfigThingy conf, int idx)
  {
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
        label = str;
      else if (name.equals("CLOSEACTION"))
        action = str;
      else if (name.equals("TIP"))
        tooltip = str;
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
  private int parseGrandchildren(ConfigThingy grandma, int idx, boolean killLastGlue)
  {
    if (idx < 0) idx = formControlModelList.size();

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
  private void scan(XTextDocument doc)
  {
    try
    {
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      try
      {
        String tit = ((String) UNO.getProperty(info, "Title")).trim();
        if (formTitle == GENERATED_FORM_TITLE && tit.length() > 0) formTitle = tit;
      }
      catch (Exception x)
      {}
      DocumentTree tree = new DocumentTree(doc);
      Visitor visitor = new ScanVisitor();
      visitor.visit(tree);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Fehler während des Scan-Vorgangs"), x);
    }

    documentNeedsUpdating();
  }

  private class ScanVisitor extends DocumentTree.Visitor
  {
    private Map<String, InsertionBookmark> insertions =
      new HashMap<String, InsertionBookmark>();

    private StringBuilder text = new StringBuilder();

    private StringBuilder fixupText = new StringBuilder();

    private FormControlModel fixupCheckbox = null;

    private void fixup()
    {
      if (fixupCheckbox != null && fixupCheckbox.getLabel().length() == 0)
      {
        fixupCheckbox.setLabel(makeLabelFromStartOf(fixupText,
          2 * GENERATED_LABEL_MAXLENGTH));
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
   * Fügt der {@link #formControlModelList} ein neues {@link FormControlModel} hinzu
   * für das {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * control, wobei text der Text sein sollte, der im Dokument vor control steht.
   * Dieser Text wird zur Generierung des Labels herangezogen. Es wird ebenfalls der
   * {@link #insertionModelList} ein entsprechendes {@link InsertionModel}
   * hinzugefügt. Zusätzlich wird immer ein entsprechendes Bookmark um das Control
   * herumgelegt, das die Einfügestelle markiert.
   * 
   * @return null, falls es sich bei dem Control nur um eine reine Einfügestelle
   *         handelt. In diesem Fall wird nur der {@link #insertionModelList} ein
   *         Element hinzugefügt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerFormControl(FormControl control,
      StringBuilder text)
  {
    boolean insertionOnlyNoLabel = false;
    String label = "";
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1).trim();
      if (label.length() == 0) insertionOnlyNoLabel = true;
      id = m.group(2).trim();
    }
    else
    {
      if (control.getType() == DocumentTree.CHECKBOX_CONTROL)
        label = ""; // immer fixUp-Text von hinter der Checkbox benutzen, weil
      // meist bessere Ergebnisse als Text von vorne
      else
        label = makeLabelFromEndOf(text, GENERATED_LABEL_MAXLENGTH);
      id = descriptor;
    }

    id = makeControlId(label, id, insertionOnlyNoLabel);

    FormControlModel model = null;

    if (!insertionOnlyNoLabel)
    {
      switch (control.getType())
      {
        case DocumentTree.CHECKBOX_CONTROL:
          model = registerCheckbox(control, label, id);
          break;
        case DocumentTree.DROPDOWN_CONTROL:
          model = registerDropdown((DropdownFormControl) control, label, id);
          break;
        case DocumentTree.INPUT_CONTROL:
          model = registerInput(control, label, id);
          break;
        default:
          Logger.error(L.m("Unbekannter Typ Formular-Steuerelement"));
          return null;
      }
    }

    boolean doGenderTrafo = false;

    String bookmarkName = insertFormValue(id);
    if (insertionOnlyNoLabel)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      }
      else if (id.startsWith(GENDER_PREFIX))
      {
        id = id.substring(GENDER_PREFIX.length());
        bookmarkName = insertFormValue(id);
        if (control.getType() == DocumentTree.DROPDOWN_CONTROL)
          doGenderTrafo = true;
      }
    }

    bookmarkName = control.surroundWithBookmark(bookmarkName);

    try
    {
      InsertionModel imodel =
        new InsertionModel4InsertXValue(bookmarkName,
          UNO.XBookmarksSupplier(doc.doc), functionSelectionProvider, this);
      if (doGenderTrafo) addGenderTrafo(imodel, (DropdownFormControl) control);
      insertionModelList.add(imodel);
    }
    catch (Exception x)
    {
      Logger.error(
        L.m("Es wurde ein fehlerhaftes Bookmark generiert: \"%1\"", bookmarkName), x);
    }

    return model;
  }

  /**
   * Verpasst model eine Gender-TRAFO, die ihre Herr/Frau/Anders-Texte aus den Items
   * von control bezieht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addGenderTrafo(InsertionModel model, DropdownFormControl control)
  {
    String[] items = control.getItems();
    FunctionSelection genderTrafo =
      functionSelectionProvider.getFunctionSelection("Gender");

    for (int i = 0; i < 3 && i < items.length; ++i)
    {
      String item = items[i];

      /*
       * Bestimme die maximal am Ende des Eintrags zu entfernende Anzahl Leerzeichen.
       * Dies ist die Anzahl an Einträgen, die bis auf folgende Leerzeichen identisch
       * sind MINUS 1.
       */
      String item1 = item;
      while (item1.endsWith(" "))
        item1 = item1.substring(0, item1.length() - 1);
      int n = 0;
      for (int j = 0; j < items.length; ++j)
      {
        String item2 = items[j];
        while (item2.endsWith(" "))
          item2 = item2.substring(0, item2.length() - 1);
        if (item1.equals(item2)) ++n;
      }

      // bis zu N-1 Leerzeichen am Ende löschen, um mehrere gleiche Einträge zu
      // erlauben.
      for (; n > 1 && item.endsWith(" "); --n)
        item = item.substring(0, item.length() - 1);
      genderTrafo.setParameterValue(GENDER_TRAFO_PARAMS[i], ParamValue.literal(item));
    }

    model.setTrafo(genderTrafo);
  }

  /**
   * Bastelt aus dem Ende des Textes text ein Label das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromEndOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(str.length() - len);
    if (label.length() < 2) label = "";
    return label;
  }

  /**
   * Bastelt aus dem Start des Textes text ein Label, das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromStartOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(0, len);
    if (label.length() < 2) label = "";
    return label;
  }

  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine
   * Checkbox hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerCheckbox(FormControl control, String label,
      String id)
  {
    FormControlModel model = null;
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
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine
   * Auswahlliste hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerDropdown(DropdownFormControl control,
      String label, String id)
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
    String preset = unicodeTrim(control.getString());
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
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für ein
   * Eingabefeld hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerInput(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createTextfield(label, id, this);
    String preset = unicodeTrim(control.getString());
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
   * Liefert str zurück minus führende und folgende Whitespace (wobei
   * Unicode-Leerzeichen) korrekt berücksichtigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private String unicodeTrim(String str)
  {
    if (str.length() == 0) return str;

    if (Character.isWhitespace(str.charAt(0))
      || Character.isWhitespace(str.charAt(str.length() - 1)))
    {
      int i = 0;
      while (i < str.length() && Character.isWhitespace(str.charAt(i)))
        ++i;
      int j = str.length() - 1;
      while (j >= 0 && Character.isWhitespace(str.charAt(j)))
        --j;
      if (i > j) return "";
      return str.substring(i, j + 1);
    }
    else
      return str;
  }

  /**
   * Macht aus str einen passenden Bezeichner für ein Steuerelement. Falls
   * insertionOnlyNoLabel == true, so muss der Bezeichner nicht eindeutig sein (dies
   * ist der Marker für eine reine Einfügestelle, für die kein Steuerelement erzeugt
   * werden muss).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeControlId(String label, String str, boolean insertionOnlyNoLabel)
  {
    if (insertionOnlyNoLabel)
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
      str = str.replaceAll("[^a-zA-Z_0-9]", "");
      if (str.length() == 0) str = "Einfuegung";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return prefix + str;
    }
    else
    {
      str = str.replaceAll("[^a-zA-Z_0-9]", "");
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
      if (vf == null) vf = new NoWrapFactory();
      return vf;
    };

    private static class NoWrapFactory implements ViewFactory, Serializable
    {
      private static final long serialVersionUID = -932935111327537530L;

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
    // ========================= Datei ============================
    menu = new JMenu(L.m("Datei"));

    menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy conf =
            new ConfigThingy("", null, new StringReader(editor.getText()));
          myFrame.setJMenuBar(mainMenuBar);
          myFrame.getContentPane().remove(editorContentPanel);
          myFrame.getContentPane().add(mainContentPanel);
          initModelsAndViews(conf);
          documentNeedsUpdating();
        }
        catch (Exception e1)
        {
          JOptionPane.showMessageDialog(myFrame, e1.getMessage(),
            L.m("Fehler beim Parsen der Formularbeschreibung"),
            JOptionPane.WARNING_MESSAGE);
        }
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        myFrame.setJMenuBar(mainMenuBar);
        myFrame.getContentPane().remove(editorContentPanel);
        myFrame.getContentPane().add(mainContentPanel);
        setFrameSize();
      }
    });
    menu.add(menuItem);

    editorMenuBar.add(menu);

    Workarounds.applyWorkaroundForOOoIssue102164();
    editor = new JEditorPane("text/plain", "");
    editor.setEditorKit(new NoWrapEditorKit());

    editor.setFont(new Font("Monospaced", Font.PLAIN, editor.getFont().getSize() + 2));
    JScrollPane scrollPane =
      new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    editorContentPanel = new JPanel(new BorderLayout());
    editorContentPanel.add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Öffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des
   * Fensters wird die geänderte Formularbeschreibung neu geparst, falls sie
   * syntaktisch korrekt ist.
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
    final JList printFunctionCurrentList =
      new JList(new Vector<String>(doc.getPrintFunctions()));
    JPanel printFunctionEditorContentPanel = new JPanel(new BorderLayout());
    printFunctionEditorContentPanel.add(printFunctionCurrentList,
      BorderLayout.CENTER);

    final JComboBox printFunctionComboBox = new JComboBox(printFunctionNames);
    printFunctionComboBox.setEditable(true);

    printFunctionEditorContentPanel.add(printFunctionComboBox, BorderLayout.NORTH);
    final JDialog dialog = new JDialog(myFrame, true);

    ActionListener removeFunc = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Object[] todel = printFunctionCurrentList.getSelectedValues();
        for (int i = 0; i < todel.length; i++)
          doc.removePrintFunction("" + todel[i]);
        printFunctionCurrentList.setListData(new Vector<String>(
          doc.getPrintFunctions()));
      }
    };

    ActionListener addFunc = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String newFunctionName = printFunctionComboBox.getSelectedItem().toString();
        doc.addPrintFunction(newFunctionName);
        printFunctionCurrentList.setListData(new Vector<String>(
          doc.getPrintFunctions()));
      }
    };

    JButton wegDamit = new JButton(L.m("Entfernen"));
    wegDamit.addActionListener(removeFunc);

    JButton machDazu = new JButton(L.m("Hinzufügen"));
    machDazu.addActionListener(addFunc);

    JButton ok = new JButton(L.m("OK"));
    ok.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });

    Box buttons = Box.createHorizontalBox();
    buttons.add(wegDamit);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(machDazu);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    printFunctionEditorContentPanel.add(buttons, BorderLayout.SOUTH);

    dialog.setTitle(L.m("Druckfunktion setzen"));
    dialog.add(printFunctionEditorContentPanel);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    if (frameHeight < 200) frameHeight = 200;

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setBounds(x, y, frameWidth, frameHeight);
    dialog.setVisible(true);
  }

  private void setFilenameGeneratorFunction()
  {
    AdjustorFunction func = parseAdjustorFunction(doc.getFilenameGeneratorFunc());
    String functionName = null;
    if (func != null) functionName = func.functionName;

    Box vbox = Box.createVerticalBox();
    vbox.setBorder(new EmptyBorder(8, 5, 10, 5));

    JTextField tf = new JTextField();
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(tf);
    final TextComponentTags tt = new TextComponentTags(tf);
    if (func != null) tt.setContent(TextComponentTags.CAT_VALUE_SYNTAX, func.CAT);
    Collection<ID> idsCol = idManager.getAllIDs(NAMESPACE_FORMCONTROLMODEL);
    List<String> ids = new ArrayList<String>();
    for (ID id : idsCol)
      ids.add(id.getID());
    JPotentiallyOverlongPopupMenuButton insertFieldButton =
      new JPotentiallyOverlongPopupMenuButton(L.m("ID"),
        TextComponentTags.makeInsertFieldActions(ids, tt));
    insertFieldButton.setFocusable(false);
    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Dateiname"), JLabel.LEFT));
    hbox.add(Box.createHorizontalGlue());
    hbox.add(insertFieldButton);
    vbox.add(hbox);
    vbox.add(tf);

    final List<String> adjustFuncs = new ArrayList<String>();
    adjustFuncs.add(L.m("-- keine --"));
    int sel = 0;
    for (String fName : doc.getFunctionLibrary().getFunctionNames())
    {
      Function f = doc.getFunctionLibrary().get(fName);
      if (f != null && f.parameters().length == 1
        && f.parameters()[0].equals("Filename"))
      {
        if (functionName != null && functionName.equals(fName))
          sel = adjustFuncs.size();
        adjustFuncs.add(fName);
      }
    }
    vbox.add(Box.createVerticalStrut(5));
    hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Nachträgliche Anpassung")));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);
    final JComboBox adjustFuncCombo = new JComboBox(adjustFuncs.toArray());
    if (sel > 0)
      adjustFuncCombo.setSelectedIndex(sel);
    else if (functionName != null)
    {
      adjustFuncCombo.addItem(functionName);
      adjustFuncCombo.addItemListener(new ItemListener()
      {
        public void itemStateChanged(ItemEvent e)
        {
          if (adjustFuncCombo.getSelectedIndex() == adjustFuncs.size())
          {
            adjustFuncCombo.setBackground(Color.red);
            adjustFuncCombo.setToolTipText(L.m("Achtung: Funktion nicht definiert!"));
          }
          else
          {
            adjustFuncCombo.setBackground(null);
            adjustFuncCombo.setToolTipText(null);
          }

        }
      });
      adjustFuncCombo.setSelectedIndex(adjustFuncs.size());
    }
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(adjustFuncCombo);
    vbox.add(adjustFuncCombo);

    final JDialog dialog = new JDialog(myFrame, true);

    JButton cancel = new JButton(L.m("Abbrechen"));
    cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });

    ActionListener submitActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy functionConf =
            createFilenameGeneratorFunctionConf(tt, adjustFuncCombo);
          doc.setFilenameGeneratorFunc(functionConf);
        }
        catch (Exception e1)
        {
          Logger.error(e1);
        }
        dialog.dispose();
      }
    };

    JButton ok = new JButton(L.m("OK"));
    ok.addActionListener(submitActionListener);
    tf.addActionListener(submitActionListener);

    Box buttons = Box.createHorizontalBox();
    buttons.add(cancel);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    vbox.add(Box.createVerticalGlue());
    vbox.add(buttons, BorderLayout.SOUTH);

    dialog.setTitle(L.m("Dateiname vorgeben"));
    dialog.add(vbox);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.pack();

    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    if (frameHeight < 200) frameHeight = 200;

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setBounds(x, y, frameWidth, frameHeight);
    dialog.setVisible(true);
  }

  private static class AdjustorFunction
  {
    final ConfigThingy CAT;

    final String functionName;

    AdjustorFunction(ConfigThingy cat, String functionName)
    {
      this.CAT = cat;
      this.functionName = functionName;
    }
  }

  private static AdjustorFunction parseAdjustorFunction(ConfigThingy func)
  {
    if (func == null) return null;
    if (func.getName().equals("CAT") && isCatFuncOk(func))
      return new AdjustorFunction(func, null);
    if (!func.getName().equals("BIND") || func.count() != 2) return null;
    Iterator<ConfigThingy> bindIter = func.iterator();
    ConfigThingy n = bindIter.next();
    if (n == null || !n.getName().equals("FUNCTION") || n.count() != 1) return null;
    String bindFunctionName = n.iterator().next().getName();
    n = bindIter.next();
    if (n == null || !n.getName().equals("SET") || n.count() != 2) return null;
    Iterator<ConfigThingy> setIter = n.iterator();
    n = setIter.next();
    if (n == null || !n.getName().equals("Filename") || n.count() != 0) return null;
    n = setIter.next();
    if (n == null || !n.getName().equals("CAT") || !isCatFuncOk(n)) return null;
    return new AdjustorFunction(n, bindFunctionName);
  }

  private static boolean isCatFuncOk(ConfigThingy catFunc)
  {
    for (ConfigThingy c : catFunc)
    {
      boolean invalid = true;
      if (c.count() == 0) invalid = false;
      if (c.getName().equals("VALUE") && c.count() == 1) invalid = false;
      if (invalid) return false;
    }
    return true;
  }

  private static ConfigThingy createFilenameGeneratorFunctionConf(
      TextComponentTags tt, JComboBox adjustFuncCombo)
  {
    if (tt.getJTextComponent().getText().trim().length() == 0) return null;
    ConfigThingy catFunc = tt.getContent(TextComponentTags.CAT_VALUE_SYNTAX);
    ConfigThingy bindFunc = null;
    if (adjustFuncCombo.getSelectedIndex() != 0)
    {
      bindFunc = new ConfigThingy("BIND");
      ConfigThingy function = new ConfigThingy("FUNCTION");
      function.add(adjustFuncCombo.getSelectedItem().toString());
      bindFunc.addChild(function);
      ConfigThingy set = new ConfigThingy("SET");
      set.add("Filename");
      set.addChild(catFunc);
      bindFunc.addChild(set);
      return bindFunc;
    }
    else
      return catFunc;
  }

  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD 'insertValue' DB_SPALTE '" + id + "')";
  }

  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD 'insertFormValue' ID '" + id + "')";
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeNonWMBookmarks(TextDocumentModel doc)
  {
    doc.removeNonWMBookmarks();
  }

  /**
   * Entfernt die WollMux-Formularmerkmale aus dem Dokument.
   * 
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
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit der FormularMax4000 gc'ed werden kann. Die Befehle
     * sorgen dafür, dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager)
     * indirekt über den JFrame den FM4000 kennt.
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    if (functionTester != null) functionTester.abort();

    try
    {
      selectionSupplier.removeSelectionChangeListener(myXSelectionChangedListener);
    }
    catch (Exception x)
    {}

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
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            abort();
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
        public void run()
        {
          try
          {
            myFrame.toFront();
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
   * Workaround für Problem unter Windows, dass das Layout bei myFrame.pack() die
   * Taskleiste nicht berücksichtigt (das Fenster also dahinter verschwindet),
   * zumindest solange nicht bis man die Taskleiste mal in ihrer Größe verändert hat.
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
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void fixFrameSize(JFrame frame)
  {
    Rectangle maxWindowBounds;

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    String lafName = UIManager.getSystemLookAndFeelClassName();
    if (!lafName.contains("plaf.windows.")) maxWindowBounds.height -= 32; // Sicherheitsabzug
    // für KDE
    // Taskleiste

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
   * Nimmt eine Menge von XTextRange Objekten, sucht alle umschlossenen Bookmarks und
   * broadcastet eine entsprechende Nachricht, damit sich die entsprechenden Objekte
   * selektieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void selectionChanged(XIndexAccess access)
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
        Logger.error(x);
      }
    }

    if (!names.isEmpty()) broadcast(new BroadcastObjectSelectionByBookmarks(names));
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
      String type = (String) UNO.getProperty(textportion, "TextPortionType");
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
          Logger.error(x);
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
          Logger.error(x);
        }
      }
    }
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    public void windowClosed(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}

  }

  private class MyXSelectionChangedListener implements XSelectionChangeListener
  {
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
            public void run()
            {
              try
              {
                FormularMax4000.this.selectionChanged(access);
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
      catch (IllegalArgumentException e)
      {
        Logger.error(L.m("Kann Selection nicht in Objekt umwandeln"), e);
      }
    }

    public void disposing(EventObject arg0)
    {}
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
    WollMuxSingleton.initialize(UNO.defaultContext);
    Logger.init(System.err, Logger.DEBUG);
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    Map<Object, Object> context = new HashMap<Object, Object>();
    DialogLibrary dialogLib =
      WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, context);
    new FormularMax4000(new TextDocumentModel(doc), null,
      WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(), dialogLib, context,
        null), WollMuxFiles.parsePrintFunctions(WollMuxFiles.getWollmuxConf()));
  }

}
