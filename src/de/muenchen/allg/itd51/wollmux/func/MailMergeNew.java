/*
* Dateiname: MailMergeNew.java
* Projekt  : WollMux
* Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import com.sun.star.container.XEnumeration;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{

  /**
   * Das {@link TextDocumentModel} zu dem Dokument an dem diese Toolbar hängt.
   */
  private TextDocumentModel mod;
  
  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;
  
  /**
   * true gdw wir uns im Vorschau-Modus befinden.
   */
  private boolean previewMode;
  
  /**
   * Die Nummer des zu previewenden Datensatzes.
   * ACHTUNG! Kann aufgrund von Veränderung der Daten im Hintergrund größer sein
   * als die Anzahl der Datensätze. Darauf muss geachtet werden.
   */
  private int previewDatasetNumber = 1;
  
  /**
   * Das Textfield in dem Benutzer direkt eine Datensatznummer für die Vorschau
   * eingeben können.
   */
  private JTextField previewDatasetNumberTextfield;
  
  /**
   * Das Toolbar-Fenster.
   */
  private JFrame myFrame;
  
  /**
   * Der WindowListener, der an {@link #myFrame} hängt.
   */
  private MyWindowListener oehrchen;
  
  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   * @param mod das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public MailMergeNew(TextDocumentModel mod)
  {
    this.mod = mod;
    this.ds = new MailMergeDatasource(mod);
    
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
    myFrame = new JFrame("Seriendruck (WollMux)");
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);
    
    Box hbox = Box.createHorizontalBox();
    myFrame.add(hbox);
    JButton button;
    button = new JButton("Datenquelle");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        ds.showDatasourceSelectionDialog(myFrame);
      }
    });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    button = new JButton("Serienbrieffeld");
    final JButton mailmergeFieldButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource()) 
          showInsertFieldPopup(mailmergeFieldButton, 0, mailmergeFieldButton.getSize().height);
        else
          ds.showDatasourceSelectionDialog(myFrame);
      }
        });
    hbox.add(button);
    
    button = new JButton("Spezialfeld");
    final JButton specialFieldButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        showInsertSpecialFieldPopup(specialFieldButton, 0, specialFieldButton.getSize().height);
      }
        });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    button = new JButton("Vorschau");
    previewMode = false;
    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        if (!ds.hasDatasource()) return;
        if (previewMode)
        {
          previewButton.setText("<<Feldname>>");
          //TODO showFieldNames();
        }
        else
        {
          previewButton.setText("Vorschau");
          //TODO updatePreviewFields();
        }
      }
        });
    hbox.add(button);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton("|<");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = 1;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton("<");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        --previewDatasetNumber;
        if (previewDatasetNumber < 1) previewDatasetNumber = 1;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    previewDatasetNumberTextfield = new JTextField("1",3);
    previewDatasetNumberTextfield.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        String tfValue = previewDatasetNumberTextfield.getText();
        try{
          int newValue = Integer.parseInt(tfValue);
          previewDatasetNumber = newValue;
        }catch(Exception x)
        {
          previewDatasetNumberTextfield.setText(""+previewDatasetNumber);
        }
        //TODO updatePreviewFields();
      }
        });
    hbox.add(previewDatasetNumberTextfield);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton(">");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        ++previewDatasetNumber;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton(">|");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = Integer.MAX_VALUE;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    button = new JButton("Drucken");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource())
          showMailmergeTypeSelectionDialog();
        else
          ds.showDatasourceSelectionDialog(myFrame);
      }
        });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    final JPopupMenu tabelleMenu = new JPopupMenu();
    JMenuItem item = new JMenuItem("Tabelle bearbeiten");
    item.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        //TODO Tabelle bearbeiten Button
      }
        });
    tabelleMenu.add(item);
    
    //  FIXME: Button muss ausgegraut sein, wenn aktuelle Cursor-Selektion
    // keine Seriendruckfelder enthält, die in der Tabelle noch nicht als
    // Spalten enthalten sind.
    item = new JMenuItem("Tabellenspalten ergänzen");
    item.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        //TODO Tabellenspalten ergänzen Button
      }
        });
    tabelleMenu.add(item);

//  FIXME: Button darf nur angezeigt werden, wenn tatsächlich eine Calc-Tabelle
    //ausgewählt ist.
    button = new JButton("Tabelle");
    final JButton tabelleButton = button;
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        tabelleMenu.show(tabelleButton, 0, tabelleButton.getSize().height);
      }
    });
    hbox.add(button);
    
    myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    myFrame.setResizable(false);
    myFrame.setVisible(true);
  }
  
  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues Dokument)
   * anwirft.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void showMailmergeTypeSelectionDialog()
  {
    final JDialog dialog = new JDialog(myFrame, "Seriendruck", true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    Box vbox = Box.createVerticalBox();
    dialog.add(vbox);
    
    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel("Serienbriefe");
    hbox.add(label);
    
    Vector types = new Vector();
    types.add("in neues Dokument schreiben");
    types.add("auf dem Drucker ausgeben");
    JComboBox typeBox = new JComboBox(types);
    hbox.add(typeBox);
    
    //FIXME: darf nur sichtbar sein, wenn in typeBox "auf dem Drucker ausgeben" gewählt ist
    JButton button = new JButton("Drucker einrichten");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO Drucker einrichten Button
      }
    });
    hbox.add(button);
    
    vbox.add(hbox);
    
    hbox = Box.createHorizontalBox();
    Border border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Folgende Datensätze verwenden");
    hbox.setBorder(border);
    
    ButtonGroup radioGroup = new ButtonGroup();
    JRadioButton rbutton;
    rbutton = new JRadioButton("Alle", true);
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    rbutton = new JRadioButton("Von", false);
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    JTextField start = new JTextField("     "); //TODO Handler, der Eingabe validiert (nur Zahl erlaubt) und evtl. das end Textfield anpasst (insbes. wenn dort noch nichts drinsteht). Hierzu sind bereits Zugriffe auf die Datenquelle erforderlich. Auch der Von-Radiobutton muss angewählt werden.
    hbox.add(start);
    label = new JLabel("Bis");
    hbox.add(label);
    JTextField end = new JTextField("     "); //TODO Handler wie bei start TextField
    hbox.add(end);
    rbutton = new JRadioButton(""); //TODO Anwahl muss selben Effekt haben wie das Drücken des "Einzelauswahl" Buttons
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    
    button = new JButton("Einzelauswahl...");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
//      TODO implementieren. Muss auch den davorstehenden Radio-Button selektieren.
      }
    });
    hbox.add(button);
    
    vbox.add(hbox);
    
    hbox = Box.createHorizontalBox();
    button = new JButton("Abbrechen");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });
    hbox.add(button);

    hbox.add(Box.createHorizontalGlue());
    
    button = new JButton("Los geht's!");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO Seriendruck durchführen
      }
    });
    hbox.add(button);

    vbox.add(hbox);
    
    dialog.pack();
    dialog.setVisible(true);
  }

  /**
   * Liefert von Tabellenblatt sheet die Indizes aller Zeilen und Spalten, in denen
   * mindestens eine sichtbare nicht-leere Zelle existiert.
   * @param sheet das zu scannende Tabellenblatt
   * @param columnIndexes diesem Set werden die Spaltenindizes hinzugefügt
   * @param rowIndexes diesem Set werden die Zeilenindizes hinzugefügt
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void getVisibleNonemptyRowsAndColumns(XCellRangesQuery sheet, SortedSet columnIndexes, SortedSet rowIndexes)
  {
    XSheetCellRanges visibleCellRanges = sheet.queryVisibleCells();
    XSheetCellRanges nonEmptyCellRanges = sheet
        .queryContentCells((short) ( com.sun.star.sheet.CellFlags.VALUE
                                   | com.sun.star.sheet.CellFlags.DATETIME
                                   | com.sun.star.sheet.CellFlags.STRING 
                                   | com.sun.star.sheet.CellFlags.FORMULA));
    CellRangeAddress[] nonEmptyCellRangeAddresses = nonEmptyCellRanges.getRangeAddresses();
    for (int i = 0; i < nonEmptyCellRangeAddresses.length; ++i)
    {
      XSheetCellRanges ranges = UNO.XCellRangesQuery(visibleCellRanges).queryIntersection(nonEmptyCellRangeAddresses[i]);
      CellRangeAddress[] rangeAddresses = ranges.getRangeAddresses();
      for (int k = 0; k < rangeAddresses.length; ++k)
      {
        CellRangeAddress addr = rangeAddresses[k];
        for (int x = addr.StartColumn; x <= addr.EndColumn; ++x)
          columnIndexes.add(new Integer(x));
        
        for (int y = addr.StartRow; y <= addr.EndRow; ++y)
          rowIndexes.add(new Integer(y));
      }
    }
  }
  
  
  /**
   * Erzeugt ein neues JPopupMenu mit Einträgen für alle Namen aus 
   * {@link #ds},getColumnNames()
   * und zeigt es an neben invoker an der relativen Position x,y. 
   * @param invoker zu welcher Komponente gehört das Popup
   * @param x Koordinate des Popups im Koordinatenraum von invoker.
   * @param y Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void showInsertFieldPopup(JComponent invoker, int x, int y)
  {
    List columnNames = ds.getColumnNames();
    if (columnNames.isEmpty()) return;
  }

  /**
   * Erzeugt ein JPopupMenu, das Einträge für das Einfügen von Spezialfeldern
   * enthält und zeigt es an neben invoker an der relativen
   * Position x,y.
   * @param invoker zu welcher Komponente gehört das Popup
   * @param x Koordinate des Popups im Koordinatenraum von invoker.
   * @param y Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void showInsertSpecialFieldPopup(JComponent invoker, int x, int y)
  {
    JPopupMenu menu = new JPopupMenu();
    
    JMenuItem button;
    button = new JMenuItem("Gender");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
   // TODO    insertGenderField();
      }
    });
    menu.add(button);
    
    button = new JMenuItem("Wenn...Dann...Sonst...");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
      //TODO  insertIfThenElseField();
      }
    });
    menu.add(button);
    
    button = new JMenuItem("Datensatznummer");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO insertDatasetIndex();
      }
    });
    menu.add(button);
    
    button = new JMenuItem("Serienbriefnummer");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO insertMailMergeIndex();
      }
    });
    menu.add(button);
    
    //FIXME: ausgegraut, wenn nicht genau ein Spezialfeld selektiert.
    button = new JMenuItem("Feld bearbeiten...");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO editSpecialField();
      }
    });
    menu.add(button);
    
    menu.show(invoker, x, y);
  }
  
  /**
   * Stellt eine OOo-Datenquelle oder ein offenes Calc-Dokument über ein gemeinsames
   * Interface zur Verfügung. Ist auch zuständig dafür, das Calc-Dokument falls nötig
   * wieder zu öffnen und Änderungen seines Fenstertitels und/oder seiner
   * Speicherstelle zu überwachen. Stellt auch
   * Dialoge zur Verfügung zur Auswahl der Datenquelle.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MailMergeDatasource
  {
    /**
     * Wert für {@link #sourceType}, der anzeigt, dass keine Datenquelle ausgewählt ist.
     */
    private static final int SOURCE_NONE = 0;
    
    /**
     * Wert für {@link #sourceType}, der anzeigt, dass eine Calc-Tabelle als Datenquelle 
     * ausgewählt ist.
     */
    private static final int SOURCE_CALC = 1;
    
    /**
     * Wert für {@link #sourceType}, der anzeigt, dass eine OOo Datenquelle
     * als Datenquelle ausgewählt ist.
     */
    private static final int SOURCE_DB = 2;
    
    /**
     * Zeigt an, was derzeit als Datenquelle ausgewählt ist.
     */
    private int sourceType = SOURCE_NONE;
    
    /**
     * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument derzeit
     * offen ist, dann ist diese Variable != null. Falls das Dokument nicht offen ist,
     * so ist seine URL in {@link #calcUrl} zu finden. Die Kombination 
     * calcDoc == null && calcUrl == null && sourceType == SOURCE_CALC ist
     * unzulässig.
     */
    private XSpreadsheetDocument calcDoc = null;
    
    /**
     * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument bereits
     * einmal gespeichert wurde, findet sich hier die URL des Dokuments, ansonsten
     * ist der Wert null. Falls das
     * Dokument nur als UnbenanntX im Speicher existiert, so ist eine
     * Referenz auf das Dokument in {@link #calcDoc} zu finden.
     * Die Kombination 
     * calcDoc == null && calcUrl == null && sourceType == SOURCE_CALC ist
     * unzulässig.
     */
    private String calcUrl = null;
    
    /**
     * Falls {@link #sourceType} == {@link #SOURCE_DB}, so ist hier der Name der
     * ausgewählten OOo-Datenquelle gespeichert, ansonsten null.
     */
    private String oooDatasourceName = null;
    
    /**
     * Speichert den Namen der Tabelle bzw, des Tabellenblattes, die als
     * Quelle der Serienbriefdaten ausgewählt wurde.
     */
    private String tableName = null;
    
    /**
     * Wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgewählten
     * Datenquelle.
     */
    private TextDocumentModel mod;
    
    /**
     * Erzeugt eine neue Datenquelle.
     * @param mod wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgewählten
     *        Datenquelle.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public MailMergeDatasource(TextDocumentModel mod)
    {
      this.mod = mod;
//    FIXME: Mit dem Problem umgehen, dass das verknüpfte Calc-Dokument jederzeit
      //geschlossen werden kann. Vielleicht kann/soll man das Schliessen dieses
      //Calc-Dokuments verhindern solange es die ausgewählte Datenquelle ist?
      //Oder es wird bei Bedarf wieder geöffnet? Da wir vermutlich ohnehin einen
      //XStorageChangeListener registrieren müssen auf das Calc-Dokument, können
      //wir auch gleich noch einen CloseListener registrieren, der zumindest merkt, dass
      //die Datei geschlossen wurde.
      //Vielleicht sollte man das Schließen auch nur dann verhindern, wenn das
      //Dokument ungespeichert ist?
    }

    /**
     * Liefert die Titel der Spalten der aktuell ausgewählten Tabelle.
     * Ist derzeit keine Tabelle ausgewählt oder enthält die ausgewählte
     * Tabelle keine benannten Spalten, so wird ein leerer Vector geliefert.
     * @return
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public Vector getColumnNames()
    {
      return new Vector(); //FIXME: getColumnNames()
    }

    /**
     * Liefert true, wenn derzeit eine Datenquelle ausgewählt ist.
     * @return
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public boolean hasDatasource()
    {
      return sourceType != SOURCE_NONE;
    }

    /**
     * Lässt den Benutzer über einen Dialog die Datenquelle auswählen.
     * @param parent der JFrame, zu dem dieser Dialog gehören soll.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void showDatasourceSelectionDialog(JFrame parent)
    {
      final JDialog datasourceSelector = new JDialog(parent, "Wo sind Ihre Serienbriefdaten ?", true);
      
      Box vbox = Box.createVerticalBox();
      datasourceSelector.add(vbox);
      
      JLabel label = new JLabel("Wo sind Ihre Serienbriefdaten ?");
      vbox.add(label);
      
      JButton button;
      button = new JButton("Datei...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          //TODO selectFileAsDatasource();
        }
      });
      vbox.add(button);
      
      button = createDatasourceSelectorCalcWindowButton();
      if (button != null) 
      {
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            //TODO selectOpenCalcWindowAsDatasource
          }});
        vbox.add(button);
      }
      
      button = new JButton("Neue Calc-Tabelle...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          //TODO openAndselectNewCalcTableAsDatasource();
        }
      });
      vbox.add(button);
      
      button = new JButton("Datenbank...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          //TODO selectOOoDatasourceAsDatasource();
        }
      });
      vbox.add(button);
      
      label = new JLabel("Aktuell ausgewählte Tabelle");
      vbox.add(label);
      String str = "<keine>";
      if (sourceType == SOURCE_CALC)
      { //TODO Testen
        if (calcDoc != null)
        {
          String title = (String)UNO.getProperty(UNO.XModel(calcDoc).getCurrentController().getFrame(),"Title");
          if (title == null) title = "?????";
          str = stripOpenOfficeFromWindowName(title);
        }
        else
        {
          str = calcUrl;
        }
      } else if (sourceType == SOURCE_DB)
      {
        str = oooDatasourceName;
      }

      if (tableName != null && tableName.length() > 0)
        str = str + "." + tableName;
      
      label = new JLabel(str);
      vbox.add(label);
      
      button = new JButton("Abbrechen");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
        }
      });
      vbox.add(button);
      
      datasourceSelector.pack();
      int frameWidth = datasourceSelector.getWidth();
      int frameHeight = datasourceSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      datasourceSelector.setLocation(x,y);
      datasourceSelector.setResizable(false);
      datasourceSelector.setVisible(true);
    }

    private static String stripOpenOfficeFromWindowName(String str)
    {
      int idx = str.indexOf(" - OpenOffice");
      if (idx > 0) str = str.substring(0, idx);
      return str;
    }
    
    /**
     * Erzeugt einen Button zur Auswahl der Datenquelle aus den aktuell offenen Calc-Fenstern,
     * dessen Beschriftung davon abhängt, was zur Auswahl steht oder liefert null, wenn nichts
     * zur Auswahl steht.
     * Falls es keine offenen Calc-Fenster gibt, wird null geliefert.
     * Falls es genau ein offenes Calc-Fenster gibt und dieses genau ein 
     * nicht-leeres Tabellenblatt hat,
     * so zeigt der Button die Beschriftung "<Fenstername>.<Tabellenname>".
     * Falls es genau ein offenes Calc-Fenster gibt und dieses mehr als ein nicht-leeres
     * oder kein nicht-leeres Tabellenblatt hat, so zeigt der Button die Beschriftung 
     * "<Fenstername>".
     * Falls es mehrere offene Calc-Fenster gibt, so zeigt der Button die Beschriftung
     * "Offenes Calc-Fenster...".
     * @return JButton oder null.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private JButton createDatasourceSelectorCalcWindowButton()
    {
      List calcWindowTitles = new Vector();
      XSpreadsheetDocument calcSheet = null;
      try{
        XSpreadsheetDocument spread = null;
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while(xenu.hasMoreElements())
        {
          spread = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (spread != null)
          {
            calcSheet = spread;
            String title = (String)UNO.getProperty(UNO.XModel(spread).getCurrentController().getFrame(),"Title");
            calcWindowTitles.add(title);
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      
      if (calcWindowTitles.isEmpty()) return null;
      if (calcWindowTitles.size() > 1) return new JButton("Offenes Calc-Fenster...");
      
      //Es gibt offenbar genau ein offenes Calc-Fenster
      //das XSpreadsheetDocument dazu ist in calcSheet zu finden
      XSpreadsheets sheets = calcSheet.getSheets();
      String[] tableNames = sheets.getElementNames();
      SortedSet columns = new TreeSet();
      SortedSet rows = new TreeSet();
      String tableName = null;
      int count = 0;
      for (int i = 0; i < tableNames.length; ++i)
      {
        try{
          XCellRangesQuery sheet = UNO.XCellRangesQuery(sheets.getByName(tableNames[i]));
          columns.clear();
          rows.clear();
          getVisibleNonemptyRowsAndColumns(sheet, columns, rows);
          if (columns.size() > 0 && rows.size() > 0)
          {
            if (++count > 1) break;
            tableName = tableNames[i];
          }
        }catch(Exception x)
        {
          Logger.error(x);
        }
      }
      
      String str = stripOpenOfficeFromWindowName(calcWindowTitles.get(0).toString());
      if (count == 1) str = str + "." + tableName;
      
      return new JButton(str);
    }
  }
  
    
  
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {abort(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
    
  }
  
  private void abort()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX) 
     *   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304
     * sind die folgenden 3 Zeilen nötig, damit der MailMerge gc'ed werden
     * kann. Die Befehle sorgen dafür, dass kein globales Objekt (wie z.B.
     * der Keyboard-Fokus-Manager) indirekt über den JFrame den MailMerge kennt.  
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);
    
    myFrame.dispose();
    myFrame = null;
    
  }
  
  public static void main(String[] args) throws Exception
  {
     UNO.init();
     
     XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
     if (doc == null) 
     {
       System.err.println("Vordergrunddokument ist kein XTextDocument!");
       System.exit(1);
     }
     
     MailMergeNew mm = new MailMergeNew(new TextDocumentModel(doc));
     
     while(mm.myFrame == null) Thread.sleep(1000);
     while(mm.myFrame != null) Thread.sleep(1000);
     System.exit(0);
  }
}
