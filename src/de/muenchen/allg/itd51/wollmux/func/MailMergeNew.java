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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.dialogs.XFilePicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.UnavailableException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.QueryResultsList;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{
  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";


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
   * Wird auf true gesetzt, wenn der Benutzer beim Seriendruck auswählt, dass er
   * die Ausgabe in einem neuen Dokument haben möchte.
   */
  private boolean printIntoDocument = true;
  
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
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;
  
  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   * @param mod das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public MailMergeNew(TextDocumentModel mod, ActionListener abortListener)
  {
    this.mod = mod;
    this.ds = new MailMergeDatasource(mod);
    this.abortListener = abortListener;
    
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
    
    //FIXME: Ausgrauen, wenn kein Datenquelle ausgewählt
    button = new JButton("Serienbrieffeld");
    final JButton mailmergeFieldButton = button;
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showInsertFieldPopup(mailmergeFieldButton, 0, mailmergeFieldButton.getSize().height);
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
    
    final String VORSCHAU = "   Vorschau   ";
    button = new JButton(VORSCHAU);
    previewMode = false;
    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        if (!ds.hasDatasource()) return;
        if (previewMode)
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText(VORSCHAU);
          previewMode = false;
          //TODO showFieldNames();
        }
        else
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText("<Feldname>");
          previewMode = true;
          //TODO updatePreviewFields();
        }
      }
        });
    hbox.add(DimAdjust.fixedSize(button));
    
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
    previewDatasetNumberTextfield.setMaximumSize(new Dimension(Integer.MAX_VALUE,button.getPreferredSize().height));
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

    //FIXME: Ausgrauen, wenn keine Datenquelle gewählt ist.
    button = new JButton("Drucken");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource())
          showMailmergeTypeSelectionDialog();
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
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = frameHeight*3;//screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
    
    if (!ds.hasDatasource()) ds.showDatasourceSelectionDialog(myFrame);
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
    hbox.add(Box.createHorizontalStrut(5));
    
    Vector types = new Vector();
    types.add("in neues Dokument schreiben");
    types.add("auf dem Drucker ausgeben");
    final JComboBox typeBox = new JComboBox(types);
    typeBox.addItemListener(new ItemListener(){
      public void itemStateChanged(ItemEvent e)
      {
        printIntoDocument = (typeBox.getSelectedIndex() == 0);
      }});
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
        dialog.dispose();
        doMailMerge();
      }
    });
    hbox.add(button);

    vbox.add(hbox);
    
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    dialog.setLocation(x,y);
    dialog.setResizable(false);
    dialog.setVisible(true);
  }

  /**
   * Erzeugt ein neues JPopupMenu mit Einträgen für alle Namen aus 
   * {@link #ds},getColumnNames()
   * und zeigt es an neben invoker an der relativen Position x,y. 
   * @param invoker zu welcher Komponente gehört das Popup
   * @param x Koordinate des Popups im Koordinatenraum von invoker.
   * @param y Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void showInsertFieldPopup(JComponent invoker, int x, int y)
  {
    List columnNames = ds.getColumnNames();
    if (columnNames.isEmpty()) return;

    Collections.sort(columnNames);
    
    JPopupMenu menu = new JPopupMenu();
    
    JMenuItem button;
    Iterator iter = columnNames.iterator();
    while (iter.hasNext())
    {
      final String name = (String)iter.next();
      button = new JMenuItem(name);
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mod.insertMailMergeFieldAtCursorPosition(name);
        }
      });
      menu.add(button);
    }
        
    menu.show(invoker, x, y);
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
   * Führt den Seriendruck durch.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void doMailMerge()
  {
    //TODO Fortschrittsanzeiger
    mod.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();
    XPrintModel pmod = mod.createPrintModel(true);
    try{
      pmod.setPropertyValue("MailMergeNew_Schema", data.getSchema());
      pmod.setPropertyValue(PROP_QUERYRESULTS, data);
    }catch(Exception x)
    {
      Logger.error(x);
      return;
    }
    pmod.usePrintFunction("MailMergeNewSetFormValue");
    if (printIntoDocument) pmod.usePrintFunction("Gesamtdokument");
    pmod.printWithProps();
  }
  
  
  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten
   * nimmt und die Seriendruckfelder im Dokument entsprechend setzt.
   * Herangezogen werden die Properties {@link #PROP_QUERYRESULTS}
   * (ein Objekt vom Typ {@link QueryResults}) und 
   * "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält.
   * Dies funktioniert natürlich nur dann, wenn pmod kein Proxy ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    QueryResults data = (QueryResults)pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection schema = (Collection)pmod.getPropertyValue("MailMergeNew_Schema");
    
    Iterator iter = data.iterator();
    
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      Iterator schemaIter = schema.iterator();
      while (schemaIter.hasNext())
      {
        String spalte = (String)schemaIter.next();
        pmod.setFormValue(spalte, ds.get(spalte));
      }
      pmod.printWithProps();
    }
  }
  
  
  /**
   * Liefert die sichtbaren Zellen des Arbeitsblattes mit Namen sheetName aus dem Calc 
   * Dokument doc. Die erste sichtbare Zeile der Calc-Tabelle wird herangezogen
   * als Spaltennamen. Diese Spaltennamen werden zu schema hinzugefügt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static QueryResults getVisibleCalcData(XSpreadsheetDocument doc, String sheetName, Set schema)
  {
    CalcCellQueryResults results = new CalcCellQueryResults();
    
    try{
      if (doc != null)
      {
        XCellRangesQuery sheet = UNO.XCellRangesQuery(doc.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          SortedSet columnIndexes = new TreeSet();
          SortedSet rowIndexes = new TreeSet();
          getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);
          
          if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
          {
            XCellRange sheetCellRange = UNO.XCellRange(sheet);
            
            /*
             * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
             * Tabellenspaltennamen interpretieren. Ein Mapping in
             * mapColumnNameToIndex wird erzeugt, wobei NICHT auf den Index in
             * der Calc-Tabelle gemappt wird, sondern auf den Index im später für jeden
             * Datensatz existierenden String[]-Array.
             */
            int ymin = ((Number)rowIndexes.first()).intValue();
            Map mapColumnNameToIndex = new HashMap();
            int idx = 0;
            Iterator iter = columnIndexes.iterator();
            while (iter.hasNext())
            {
              int x = ((Number)iter.next()).intValue();
              String columnName = UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
              if (columnName.length() > 0)
              {
                mapColumnNameToIndex.put(columnName, new Integer(idx));
                schema.add(columnName);
                ++idx;  
              }
              else 
                iter.remove(); //Spalten mit leerem Spaltennamen werden nicht benötigt.
            }
            
            results.setColumnNameToIndexMap(mapColumnNameToIndex);
            
            /*
             * Datensätze erzeugen
             */
            Iterator rowIndexIter = rowIndexes.iterator();
            rowIndexIter.next(); //erste Zeile enthält die Tabellennamen, keinen Datensatz
            while (rowIndexIter.hasNext())
            {
              int y = ((Number)rowIndexIter.next()).intValue();
              String[] data = new String[columnIndexes.size()];
              Iterator columnIndexIter = columnIndexes.iterator();
              idx = 0;
              while (columnIndexIter.hasNext())
              {
                int x = ((Number)columnIndexIter.next()).intValue();
                String value = UNO.XTextRange(sheetCellRange.getCellByPosition(x, y)).getString();
                data[idx++] = value;
              }
              
              results.addDataset(data);
            }
          }
        }
      }
    }catch(Exception x)
    {
      Logger.error(x);
    }
    
    return results;
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
     * Wenn nach dieser Zeit in ms nicht alle Daten des Seriendruckauftrags
     * ausgelesen werden konnten, dann wird der Druckauftrag nicht ausgeführt
     * (und muss eventuell über die Von Bis Auswahl in mehrere Aufträge
     * zerteilt werden). 
     */
    private static final long MAILMERGE_GETCONTENTS_TIMEOUT = 60000;
    
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
     * Quelle der Serienbriefdaten ausgewählt wurde. Ist niemals null, kann
     * aber der leere String sein oder ein Name, der gar nicht in der
     * entsprechenden Datenquelle existiert.
     */
    private String tableName = "";
    
    /**
     * Wenn als aktuelle Datenquelle ein Calc-Dokument ausgewählt ist, dann
     * wird dieser Listener darauf registriert um Änderungen des Speicherorts,
     * so wie das Schließen des Dokuments zu überwachen.
     */
    private MyCalcListener myCalcListener = new MyCalcListener();
    
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
     */
    public MailMergeDatasource(TextDocumentModel mod)
    {
      this.mod = mod;
    }

    /**
     * Liefert die Titel der Spalten der aktuell ausgewählten Tabelle.
     * Ist derzeit keine Tabelle ausgewählt oder enthält die ausgewählte
     * Tabelle keine benannten Spalten, so wird ein leerer Vector geliefert.
     * @return
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public List getColumnNames()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getColumnNames(getCalcDoc(), tableName);
          case SOURCE_DB: return getDbColumnNames(oooDatasourceName, tableName);
          default: return new Vector();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector();
      }
    }
    
    
    /**
     * Liefert die Spaltennamen der Tabelle tableName aus der
     * OOo-Datenquelle oooDatasourceName.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private List getDbColumnNames(String oooDatasourceName, String tableName)
    {
      return new Vector(); //FIXME: getDbColumnNames()
    }

    /**
     * Liefert die Inhalte (als Strings) der nicht-leeren Zellen der ersten
     * sichtbaren Zeile von Tabellenblatt tableName in Calc-Dokument calcDoc.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private List getColumnNames(XSpreadsheetDocument calcDoc, String tableName)
    {
      List columnNames = new Vector();
      if (calcDoc == null) return columnNames;
      try{
        XCellRangesQuery sheet = UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
        SortedSet columnIndexes = new TreeSet();
        SortedSet rowIndexes = new TreeSet();
        getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);
        
        if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
        {
          XCellRange sheetCellRange = UNO.XCellRange(sheet);
          
          /*
           * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
           * Tabellenspaltennamen interpretieren. 
           */
          int ymin = ((Number)rowIndexes.first()).intValue();
          Iterator iter = columnIndexes.iterator();
          while (iter.hasNext())
          {
            int x = ((Number)iter.next()).intValue();
            String columnName = UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
            if (columnName.length() > 0)
            {
              columnNames.add(columnName);
            }
          }
        }        
      }catch(Exception x)
      {
        Logger.error("Kann Spaltennamen nicht bestimmen",x);
      }
      return columnNames;  
    }
    
    /**
     * Liefert den Inhalt der aktuell ausgewählten Serienbriefdatenquelle (leer, wenn
     * keine ausgewählt).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public QueryResultsWithSchema getData()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getData(getCalcDoc(), tableName);
          case SOURCE_DB: return getDbData(oooDatasourceName, tableName);
          default: return new QueryResultsWithSchema();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new QueryResultsWithSchema();
      }
    }

    /**
     * Liefert den Inhalt der Tabelle tableName aus der OOo Datenquelle 
     * mit Namen oooDatasourceName.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private QueryResultsWithSchema getDbData(String oooDatasourceName, String tableName) throws Exception, ConfigurationErrorException
    {
      ConfigThingy conf = new ConfigThingy("Datenquelle");
      conf.add("NAME").add("Knuddel");
      conf.add("TABLE").add(tableName);
      conf.add("SOURCE").add(oooDatasourceName);
      Datasource ds;
      ds = new OOoDatasource(new HashMap(),conf,new URL("file:///"), true);
      
      Set schema = ds.getSchema();
      QueryResults res = ds.getContents(MAILMERGE_GETCONTENTS_TIMEOUT);
      return new QueryResultsWithSchema(res, schema);
    }

    /**
     * Liefert die sichtbaren Zellen aus der Tabelle tableName des Dokuments
     * calcDoc als QueryResultsWithSchema zurück.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private QueryResultsWithSchema getData(XSpreadsheetDocument calcDoc, String tableName)
    {
      Set schema = new HashSet();
      QueryResults res = getVisibleCalcData(calcDoc, tableName, schema);
      return new QueryResultsWithSchema(res, schema);
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
    public void showDatasourceSelectionDialog(final JFrame parent)
    {
      final JDialog datasourceSelector = new JDialog(parent, "Wo sind Ihre Serienbriefdaten ?", true);
      
      Box vbox = Box.createVerticalBox();
      datasourceSelector.add(vbox);
      
      JLabel label = new JLabel("Wo sind Ihre Serienbriefdaten ?");
      vbox.add(label);
      
      JButton button;
      button = createDatasourceSelectorCalcWindowButton();
      if (button != null) 
      {
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            datasourceSelector.dispose();
            selectOpenCalcWindowAsDatasource(parent);
          }});
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      button = new JButton("Datei...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
          selectFileAsDatasource(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      button = new JButton("Neue Calc-Tabelle...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
          openAndselectNewCalcTableAsDatasource(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      button = new JButton("Datenbank...");
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          //TODO selectOOoDatasourceAsDatasource();
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
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

      if (tableName.length() > 0)
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
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
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
    
    /**
     * Präsentiert dem Benutzer einen Dialog, in dem er aus allen offenen Calc-Fenstern
     * eines als Datenquelle auswählen kann. Falls es nur ein
     * offenes Calc-Fenster gibt, wird dieses automatisch gewählt.
     * 
     * @param parent der JFrame zu dem der die Dialoge gehören sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void selectOpenCalcWindowAsDatasource(final JFrame parent)
    {
      List[] calcWindows = getOpenCalcWindows();
      List names = calcWindows[0];
      
      if (names.isEmpty()) return;
      
      if (names.size() == 1)
      {
        getCalcDoc((XSpreadsheetDocument)calcWindows[1].get(0));
        selectTable(parent);
        return;
      }
      
      final JDialog calcWinSelector = new JDialog(parent, "Welche Tabelle möchten Sie verwenden ?", true);
      
      Box vbox = Box.createVerticalBox();
      calcWinSelector.add(vbox);
      
      JLabel label = new JLabel("Welches Calc-Dokument möchten Sie verwenden ?");
      vbox.add(label);
      
      for (int i = 0; i < names.size(); ++i)
      {
        final String name = (String)names.get(i);
        final XSpreadsheetDocument spread = (XSpreadsheetDocument)calcWindows[1].get(i);
        JButton button;
        button = new JButton(name);
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            calcWinSelector.dispose();
            getCalcDoc(spread);
            selectTable(parent);
          }
        });
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      calcWinSelector.pack();
      int frameWidth = calcWinSelector.getWidth();
      int frameHeight = calcWinSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      calcWinSelector.setLocation(x,y);
      calcWinSelector.setResizable(false);
      calcWinSelector.setVisible(true);

    }
    
    /**
     * Öffnet ein neues Calc-Dokument und setzt es als Seriendruckdatenquelle.
     * 
     * @param parent der JFrame zu dem der die Dialoge gehören sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void openAndselectNewCalcTableAsDatasource(JFrame parent)
    {
      try
      {
        Logger.debug("Öffne neues Calc-Dokument als Datenquelle für Seriendruck");
        XSpreadsheetDocument spread = UNO.XSpreadsheetDocument(UNO.loadComponentFromURL("private:factory/scalc", true, true));
        XSpreadsheets sheets = spread.getSheets();
        String[] sheetNames = sheets.getElementNames();

        for (int i = 1; i < sheetNames.length; ++i)
          sheets.removeByName(sheetNames[i]);
        
        getCalcDoc(spread);
        selectTable(parent);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
    
    /**
     * Öffnet einen FilePicker und falls der Benutzer dort eine Tabelle auswählt, wird diese
     * geöffnet und als Datenquelle verwendet.
     * 
     * @param parent der JFrame zu dem der die Dialoge gehören sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void selectFileAsDatasource(JFrame parent)
    {
      XFilePicker picker = UNO.XFilePicker(UNO.createUNOService("com.sun.star.ui.dialogs.FilePicker"));
      short res = picker.execute();
      if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
      {
        String[] files = picker.getFiles();
        if (files.length == 0) return;
        try
        {
          Logger.debug("Öffne "+files[0]+" als Datenquelle für Seriendruck");
          try{
            getCalcDoc(files[0]);
          }catch(UnavailableException x)
          {
            return;
          }
          selectTable(parent);
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
    }
    
    /**
     * Bringt einen Dialog, mit dem der Benutzer in der aktuell ausgewählten
     * Datenquelle eine Tabelle auswählen kann. Falls die Datenquelle genau eine
     * nicht-leere Tabelle hat, so wird diese ohne Dialog automatisch ausgewählt.
     * Falls der Benutzer den Dialog abbricht, so wird die erste Tabelle gewählt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * @parent Das Hauptfenster, zu dem dieser Dialog gehört.
     * TESTED
     */
    private void selectTable(JFrame parent)
    {
      List names = getNamesOfNonEmptyTables();
      List allNames = getTableNames();
      if (allNames.isEmpty())
      {
        tableName = "";
        return;
      }
      if (names.isEmpty()) names = allNames;
      
      tableName = (String)names.get(0); //Falls der Benutzer den Dialog abbricht ohne Auswahl
      
      if (names.size() == 1) return; //Falls es nur eine Tabelle gibt, Dialog unnötig.
      
      final JDialog tableSelector = new JDialog(parent, "Welche Tabelle möchten Sie verwenden ?", true);
      
      Box vbox = Box.createVerticalBox();
      tableSelector.add(vbox);
      
      JLabel label = new JLabel("Welche Tabelle möchten Sie verwenden ?");
      vbox.add(label);
      
      Iterator iter = names.iterator();
      while (iter.hasNext())
      {
        final String name = (String)iter.next();
        JButton button;
        button = new JButton(name);
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            tableSelector.dispose();
            tableName = name;
          }
        });
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      tableSelector.pack();
      int frameWidth = tableSelector.getWidth();
      int frameHeight = tableSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      tableSelector.setLocation(x,y);
      tableSelector.setResizable(false);
      tableSelector.setVisible(true);
    }

    /**
     * Registriert {@link #myCalcListener} auf calcDoc, falls calcDoc != null.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void setListeners(XSpreadsheetDocument calcDoc)
    {
      //FIXME: Das Ändern des Names eines Sheets muss überwacht werden damit tableName angepasst wird.
      if (calcDoc == null) return;
      try{
        UNO.XCloseBroadcaster(calcDoc).addCloseListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error("Kann CloseListener nicht auf Calc-Dokument registrieren",x);
      }
      try{
        UNO.XEventBroadcaster(calcDoc).addEventListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error("Kann EventListener nicht auf Calc-Dokument registrieren",x);
      }
    }

    /**
     * Falls calcDoc != null wird versucht, {@link #myCalcListener} davon zu deregistrieren.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void removeListeners(XSpreadsheetDocument calcDoc)
    {
      if (calcDoc == null) return;
      
      try{
        UNO.XCloseBroadcaster(calcDoc).removeCloseListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error("Konnte alten XCloseListener nicht deregistrieren",x);
      }
      try{
        UNO.XEventBroadcaster(calcDoc).removeEventListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error("Konnte alten XEventListener nicht deregistrieren",x);
      }
      
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
      List[] calcWindows = getOpenCalcWindows();
      
      if (calcWindows[0].isEmpty()) return null;
      if (calcWindows[0].size() > 1) return new JButton("Offenes Calc-Fenster...");
      
      //Es gibt offenbar genau ein offenes Calc-Fenster
      //das XSpreadsheetDocument dazu ist in calcSheet zu finden
      List nonEmptyTableNames = getNamesOfNonEmptyTables((XSpreadsheetDocument)calcWindows[1].get(0));
      
      String str = calcWindows[0].get(0).toString();
      if (nonEmptyTableNames.size() == 1) str = str + "." + nonEmptyTableNames.get(0);
      
      return new JButton(str);
    }

    /**
     * Liefert die Titel und zugehörigen XSpreadsheetDocuments aller offenen Calc-Fenster.
     * @return ein Array mit 2 Elementen. Das erste ist eine Liste aller Titel von Calc-Fenstern,
     *         wobei jeder Titel bereits mit {@link #stripOpenOfficeFromWindowName(String)}
     *         bearbeitet wurde. Das zweite Element ist eine Liste von
     *         XSpreadsheetDocuments, wobei jeder Eintrag zum Fenstertitel mit dem selben
     *         Index in der ersten Liste gehört.
     *         Im Fehlerfalle sind beide Listen leer.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List[] getOpenCalcWindows()
    {
      List[] calcWindows = new List[]{new Vector(), new Vector()};
      try{
        XSpreadsheetDocument spread = null;
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while(xenu.hasMoreElements())
        {
          spread = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (spread != null)
          {
            String title = (String)UNO.getProperty(UNO.XModel(spread).getCurrentController().getFrame(),"Title");
            calcWindows[0].add(stripOpenOfficeFromWindowName(title));
            calcWindows[1].add(spread);
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return calcWindows;
    }

    /**
     * Falls aktuell eine Calc-Tabelle als Datenquelle ausgewählt ist, so
     * wird versucht, diese zurückzuliefern. Falls nötig wird die Datei
     * anhand von {@link #calcUrl} neu geöffnet. Falls es aus irgendeinem
     * Grund nicht möglich ist, diese zurückzuliefern, wird eine
     * {@link de.muenchen.allg.itd51.wollmux.UnavailableException} geworfen.
     * ACHTUNG! Das zurückgelieferte Objekt könnte bereits disposed sein!
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private XSpreadsheetDocument getCalcDoc() throws UnavailableException
    {
      if (sourceType != SOURCE_CALC) throw new UnavailableException("Keine Calc-Tabelle ausgewählt");
      if (calcDoc != null) return calcDoc;
      return getCalcDoc(calcUrl);
    }
    
    /**
     * Falls url bereits offen ist oder geöffnet werden kann und ein
     * Tabellendokument ist, so wird der {@link #sourceType} auf 
     * {@link #SOURCE_CALC} gestellt und die Calc-Tabelle als neue
     * Datenquelle ausgewählt.
     * @return das Tabellendokument
     * @throws UnavailableException falls ein Fehler auftritt oder die
     *         url kein Tabellendokument beschreibt. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private XSpreadsheetDocument getCalcDoc(String url) throws UnavailableException
    {
      /**
       * Falls schon ein offenes Fenster mit der entsprechenden URL
       * existiert, liefere dieses zurück und setze {@link #calcDoc}.
       */
      XSpreadsheetDocument newCalcDoc = null;
      try{
        XSpreadsheetDocument spread;
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while(xenu.hasMoreElements())
        {
          spread = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (spread != null && url.equals(UNO.XModel(spread).getURL()))
          {
            newCalcDoc = spread;
            break;
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      
      /**
       * Ansonsten versuchen wir das Dokument zu öffnen.
       */
      if (newCalcDoc == null)
      {
        try{
          Object ss = UNO.loadComponentFromURL(url, false, true); //FIXME: Dragndrop-Problem
          newCalcDoc = UNO.XSpreadsheetDocument(ss);
          if (newCalcDoc == null) throw new UnavailableException("URL \""+url+"\" ist kein Tabellendokument");
        }catch(Exception x) 
        {
          throw new UnavailableException(x);
        }
      }

      getCalcDoc(newCalcDoc);
      return calcDoc;
    }

    /**
     * Setzt newCalcDoc als Datenquelle für den Seriendruck.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void getCalcDoc(XSpreadsheetDocument newCalcDoc)
    {
      try{
        calcUrl = UNO.XModel(newCalcDoc).getURL();
      }catch(Exception x) //typischerweise DisposedException  
      { 
        return;
      }
      if (calcUrl.length() == 0) calcUrl = null;
      sourceType = SOURCE_CALC;
      removeListeners(calcDoc); //falls altes calcDoc vorhanden, dort deregistrieren.
      calcDoc = newCalcDoc;
      setListeners(calcDoc);
//    TODO Änderung muss ins Dokument übertragen werden, damit beim nächsten Mal richtige Datenquelle geöffnet werden kann
    }
    
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellenblätter der aktuell
     * ausgewählten Datenquelle. Wenn keine Datenquelle ausgewählt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List getNamesOfNonEmptyTables()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getNamesOfNonEmptyTables(getCalcDoc());
          case SOURCE_DB: return getNamesOfNonEmptyDbTables();
          default: return new Vector();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector();
      }
    }

    
    /**
     * Liefert die Namen aller Tabellen der aktuell
     * ausgewählten Datenquelle. Wenn keine Datenquelle ausgewählt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List getTableNames()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getTableNames(getCalcDoc());
          case SOURCE_DB: return getDbTableNames();
          default: return new Vector();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector();
      }
    }
    
    /**
     * Liefert die Namen aller Tabellen der aktuell
     * ausgewählten OOo-Datenquelle. Wenn keine OOo-Datenquelle ausgewählt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List getDbTableNames()
    {
      return new Vector();
    }
    
    /**
     * Liefert die Namen aller Tabellenblätter von calcDoc.
     * Falls calcDoc == null, wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List getTableNames(XSpreadsheetDocument calcDoc)
    {
      List nonEmptyTableNames = new Vector();
      if (calcDoc != null) 
      try{
        XSpreadsheets sheets = calcDoc.getSheets();
        String[] tableNames = sheets.getElementNames();
        nonEmptyTableNames.addAll(Arrays.asList(tableNames));
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return nonEmptyTableNames;
    }
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellen der aktuell
     * ausgewählten OOo-Datenquelle. Wenn keine OOo-Datenquelle ausgewählt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List getNamesOfNonEmptyDbTables()
    {
      return new Vector();
    }
    
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellenblätter von calcDoc.
     * Falls calcDoc == null wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED*/
    private List getNamesOfNonEmptyTables(XSpreadsheetDocument calcDoc)
    {
      List nonEmptyTableNames = new Vector();
      if (calcDoc != null) 
      try{
        XSpreadsheets sheets = calcDoc.getSheets();
        String[] tableNames = sheets.getElementNames();
        SortedSet columns = new TreeSet();
        SortedSet rows = new TreeSet();
        for (int i = 0; i < tableNames.length; ++i)
        {
          try{
            XCellRangesQuery sheet = UNO.XCellRangesQuery(sheets.getByName(tableNames[i]));
            columns.clear();
            rows.clear();
            getVisibleNonemptyRowsAndColumns(sheet, columns, rows);
            if (columns.size() > 0 && rows.size() > 0)
            {
              nonEmptyTableNames.add(tableNames[i]);
            }
          }catch(Exception x)
          {
            Logger.error(x);
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return nonEmptyTableNames;
    }
    
    private class MyCalcListener implements XCloseListener, XEventListener
    {

      public void queryClosing(EventObject arg0, boolean arg1) throws CloseVetoException
      {
      }

      public void notifyClosing(EventObject arg0)
      {
        Logger.debug("Calc-Datenquelle wurde unerwartet geschlossen");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            calcDoc = null;
          }
        });
      }

      public void disposing(EventObject arg0)
      {
        Logger.debug("Calc-Datenquelle wurde disposed()");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            calcDoc = null;
          }
        });
      }
    
      public void notifyEvent(com.sun.star.document.EventObject event)
      {  
        if (event.EventName.equals("OnSaveAsDone") && UnoRuntime.areSame(event.Source, calcDoc))
        {
          javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              calcUrl = UNO.XModel(calcDoc).getURL();
              Logger.debug("Speicherort der Tabelle hat sich geändert: \""+calcUrl+"\"");
              //TODO Änderung muss ins Dokument übertragen werden, damit beim nächsten Mal richtige Datenquelle geöffnet werden kann
            }
          });
        }
      }
    }

    /**
     * Gibt Ressourcen frei und deregistriert Listener.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void dispose()
    {
      removeListeners(calcDoc);
    }
  }
  
  private static class QueryResultsWithSchema implements QueryResults
  {
    protected QueryResults results;
    protected Set schema;
    
    /**
     * Constructs an empty QueryResultsWithSchema.
     */
    public QueryResultsWithSchema()
    {
      results = new QueryResultsList(new ArrayList());
      schema = new HashSet();
    }
    
    /**
     * Erzeugt ein neues QueryResultsWithSchema, das den Inhalt von res und das Schema
     * schema zusammenfasst. ACHTUNG! res und schema werden als Referenzen übernommen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public QueryResultsWithSchema(QueryResults res, Set schema)
    {
      this.schema = schema;
      this.results = res;
    }

    public int size()
    {
      return results.size();
    }

    public Iterator iterator()
    {
      return results.iterator();
    }

    public boolean isEmpty()
    {
      return results.isEmpty();
    }
    
    public Set getSchema() { return new HashSet(schema);}
    
  }
  
  private static class CalcCellQueryResults implements QueryResults
  {
    /**
     * Bildet einen Spaltennamen auf den Index in dem zu dem Datensatz gehörenden
     * String[]-Array ab.
     */
    private Map mapColumnNameToIndex;
   
    private List datasets = new ArrayList();
    
    public int size()
    {
      return datasets.size();
    }

    public Iterator iterator()
    {
      return datasets.iterator();
    }

    public boolean isEmpty()
    {
      return datasets.isEmpty();
    }

    public void setColumnNameToIndexMap(Map mapColumnNameToIndex)
    {
      this.mapColumnNameToIndex = mapColumnNameToIndex;
    }

    public void addDataset(String[] data)
    {
      datasets.add(new MyDataset(data));
    }
    
    private class MyDataset implements Dataset
    {
      private String[] data;
      public MyDataset(String[] data)
      {
        this.data = data;
      }

      public String get(String columnName) throws ColumnNotFoundException
      {
        Number idx = (Number)mapColumnNameToIndex.get(columnName);
        if (idx == null) throw new ColumnNotFoundException("Spalte "+columnName+" existiert nicht!");
        return data[idx.intValue()];
      }

      public String getKey()
      {
        return "key";
      }
      
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
    
    ds.dispose();
    
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }
 
  public static void main(String[] args) throws Exception
  {
     UNO.init();
     Logger.init(Logger.ALL);
     XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
     if (doc == null) 
     {
       System.err.println("Vordergrunddokument ist kein XTextDocument!");
       System.exit(1);
     }
     
     MailMergeNew mm = new MailMergeNew(new TextDocumentModel(doc), null);
     
     while(mm.myFrame == null) Thread.sleep(1000);
     while(mm.myFrame != null) Thread.sleep(1000);
     System.exit(0);
  }
}
