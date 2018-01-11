/*
 * Dateiname: MailMerge.java
 * Projekt  : WollMux
 * Funktion : Druckfunktionen für den Seriendruck.
 * 
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 05.01.2007 | BNK | Erstellung
 * 15.01.2007 | BNK | Fortschrittsindikator
 * 29.01.2007 | BNK | "Keine Beschreibung vorhanden" durch Datensatznummer ersetzt.
 * 09.03.2007 | BNK | [P1257]Auch Datenquellen unterstützen, die keine Schlüssel haben.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.print;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.sdb.CommandType;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

public class MailMerge
{
  /**
   * Anzahl Millisekunden, die maximal gewartet wird, bis alle Datensätze für den
   * Serienbrief aus der Datenbank gelesen wurden.
   */
  private static final int DATABASE_TIMEOUT = 20000;

  /**
   * Druckt das zu pmod gehörende Dokument für alle Datensätze (offerSelection==true)
   * oder die Datensätze, die der Benutzer in einem Dialog auswählt (offerSelection
   * == false) aus der aktuell über Bearbeiten/Datenbank austauschen eingestellten
   * Tabelle. Für die Anzeige der Datensätze im Dialog wird die Spalte
   * "WollMuxDescription" verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist
   * und "1", "ja" oder "true" enthält, so ist der entsprechende Datensatz in der
   * Auswahlliste bereits vorselektiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMerge(XPrintModel pmod, boolean offerSelection)
  { // TESTED
    XTextDocument doc = pmod.getTextDocument();
    XPropertySet settings = null;
    try
    {
      settings =
        UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.document.Settings"));
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann DocumentSettings nicht auslesen"), x);
      return;
    }

    String datasource =
      (String) UNO.getProperty(settings, "CurrentDatabaseDataSource");
    String table = (String) UNO.getProperty(settings, "CurrentDatabaseCommand");
    Integer type = (Integer) UNO.getProperty(settings, "CurrentDatabaseCommandType");

    Logger.debug("Ausgewählte Datenquelle: \"" + datasource
      + "\"  Tabelle/Kommando: \"" + table + "\"  Typ: \"" + type + "\"");

    mailMerge(pmod, datasource, table, type, offerSelection);
  }

  /**
   * Falls offerSelection == false wird das zu pmod gehörende Dokument für jeden
   * Datensatz aus Tabelle table in Datenquelle datasource einmal ausgedruckt. Falls
   * offerSelection == true, wird dem Benutzer ein Dialog präsentiert, in dem er die
   * "WollMuxDescription"-Spalten aller Datensätze angezeigt bekommt und die
   * auszudruckenden Datensätze auswählen kann. Dabei sind alle Datensätze, die eine
   * Spalte "WollMuxSelected" haben, die den Wert "true", "ja" oder "1" enthält
   * bereits vorselektiert.
   * 
   * @param type
   *          muss {@link CommandType#TABLE} sein.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void mailMerge(XPrintModel pmod, String datasource, String table,
      Integer type, boolean offerSelection)
  {
    /*
     * Kann nur mit Tabellennamen umgehen, nicht mit beliebigen Statements. Falls
     * eine andere Art von Kommando eingestellt ist, wird der SuperMailMerge
     * gestartet, damit der Benutzer eine Tabelle auswählt.
     */
    if (datasource == null || datasource.length() == 0 || table == null
      || table.length() == 0 || type == null || type.intValue() != CommandType.TABLE)
    {
      superMailMerge(pmod);
      return;
    }

    ConfigThingy conf = new ConfigThingy("Datenquelle");
    conf.add("NAME").add("Knuddel");
    conf.add("TABLE").add(table);
    conf.add("SOURCE").add(datasource);
    Datasource ds;
    try
    {
      ds =
        new OOoDatasource(new HashMap<String, Datasource>(), conf, new URL(
          "file:///"), true);
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }

    Set<String> schema = ds.getSchema();
    QueryResults data;
    try
    {
      data = ds.getContents(DATABASE_TIMEOUT);
    }
    catch (TimeoutException e)
    {
      Logger.error(
        L.m("Konnte Daten für Serienbrief nicht aus der Datenquelle auslesen"), e);
      return;
    }

    mailMerge(pmod, offerSelection, schema, data);
  }

  /**
   * Falls offerSelection == false wird das zu pmod gehörende Dokument für jeden
   * Datensatz aus data einmal ausgedruckt. Falls offerSelection == true, wird dem
   * Benutzer ein Dialog präsentiert, in dem er die "WollMuxDescription"-Spalten
   * aller Datensätze angezeigt bekommt und die auszudruckenden Datensätze auswählen
   * kann. Dabei sind alle Datensätze, die eine Spalte "WollMuxSelected" haben, die
   * den Wert "true", "ja" oder "1" enthält bereits vorselektiert.
   * 
   * @param schema
   *          muss die Namen aller Spalten für den MailMerge enthalten.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void mailMerge(XPrintModel pmod, boolean offerSelection,
      Set<String> schema, QueryResults data)
  {
    Vector<ListElement> list = new Vector<ListElement>();
    int index = 1;
    for (Dataset dataset : data)
    {
      list.add(new ListElement(dataset, L.m("Datensatz ") + index));
      ++index;
    }

    if (offerSelection)
    {
      if (!selectFromListDialog(list)) return;
    }

    boolean modified = pmod.getDocumentModified(); // Modified-Zustand merken, um
    // ihn nachher wiederherzustellen
    pmod.collectNonWollMuxFormFields(); // falls der Benutzer manuell welche
    // hinzugefuegt hat

    MailMergeProgressWindow progress = new MailMergeProgressWindow(list.size());

    Iterator<ListElement> iter = list.iterator();
    while (iter.hasNext())
    {
      progress.makeProgress();
      ListElement ele = iter.next();
      if (offerSelection && !ele.isSelected()) continue;
      Iterator<String> colIter = schema.iterator();
      while (colIter.hasNext())
      {
        String column = colIter.next();
        String value = null;
        try
        {
          value = ele.getDataset().get(column);
        }
        catch (Exception e)
        {
          Logger.error(
            L.m("Spalte \"%1\" fehlt unerklärlicherweise => Abbruch des Drucks",
              column), e);
          return;
        }

        if (value != null) pmod.setFormValue(column, value);
      }
      pmod.printWithProps();
    }

    progress.close();

    pmod.setDocumentModified(modified);
  }

  private static class MailMergeProgressWindow
  {
    private JFrame myFrame;

    private JLabel countLabel;

    private int count = 0;

    private int maxcount;

    MailMergeProgressWindow(final int maxcount)
    {
      this.maxcount = maxcount;
      try
      {
        SwingUtilities.invokeAndWait(new Runnable()
        {
          @Override
          public void run()
          {
            myFrame = new JFrame(L.m("Seriendruck"));
            Common.setWollMuxIcon(myFrame);
            Box vbox = Box.createVerticalBox();
            myFrame.getContentPane().add(vbox);
            Box hbox = Box.createHorizontalBox();
            vbox.add(hbox);
            hbox.add(Box.createHorizontalStrut(5));
            hbox.add(new JLabel(L.m("Verarbeite Dokument")));
            hbox.add(Box.createHorizontalStrut(5));
            countLabel = new JLabel("   -");
            hbox.add(countLabel);
            hbox.add(new JLabel(" / " + maxcount + "    "));
            hbox.add(Box.createHorizontalStrut(5));
            myFrame.setAlwaysOnTop(true);
            myFrame.pack();
            int frameWidth = myFrame.getWidth();
            int frameHeight = myFrame.getHeight();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - frameWidth / 2;
            int y = screenSize.height / 2 - frameHeight / 2;
            myFrame.setLocation(x, y);
            myFrame.setVisible(true);
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      ;
    }

    public void makeProgress()
    {
      try
      {
        SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            ++count;
            countLabel.setText("" + count);
            if (maxcount > 0)
              myFrame.setTitle("" + Math.round(100 * (double) count / maxcount)
                + "%");
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      ;
    }

    public void close()
    {
      try
      {
        SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            myFrame.dispose();
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      ;
    }
  }

  /**
   * Wrapper für ein Dataset, um es einerseits in eine JList packen zu können,
   * andererseits auch dafür, den Zustand ausgewählt oder nicht speichern zu können.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class ListElement
  {
    private Dataset ds;

    private boolean selected = false;

    private String description;

    /**
     * Initialisiert dieses ListElement mit dem Dataset ds, wobei falls vorhanden die
     * Spalten "WollMuxDescription" und "WollMuxSelected" ausgewertet werden, um den
     * toString() respektive isSelected() Wert zu bestimmen. Falls keine
     * WollMuxDescription vorhanden ist, so wird description verwendet.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public ListElement(Dataset ds, String description)
    {
      this.ds = ds;
      this.description = description;
      try
      {
        String des = ds.get("WollMuxDescription");
        if (des != null && des.length() > 0) this.description = des;
      }
      catch (Exception x)
      {}
      try
      {
        String sel = ds.get("WollMuxSelected");
        if (sel != null
          && (sel.equalsIgnoreCase("true") || sel.equals("1") || sel.equalsIgnoreCase("ja")))
          selected = true;
      }
      catch (Exception x)
      {}
    }

    public void setSelected(boolean selected)
    {
      this.selected = selected;
    }

    public boolean isSelected()
    {
      return selected;
    }

    public Dataset getDataset()
    {
      return ds;
    }

    @Override
    public String toString()
    {
      return description;
    }
  }

  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}
   * s) auswählen lässt. ACHTUNG! Diese Methode kehrt erst zurück nachdem der
   * Benutzer den Dialog geschlossen hat.
   * 
   * @return true, gdw der Benutzer mit Okay bestätigt hat.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static boolean selectFromListDialog(final Vector<ListElement> list)
  {
    final boolean[] result = new boolean[] {
      false, false };
    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            createSelectFromListDialog(list, result);
          }
          catch (Exception x)
          {
            Logger.error(x);
            synchronized (result)
            {
              result[0] = true;
              result.notifyAll();
            }
          }
          ;
        }
      });

      synchronized (result)
      {
        while (!result[0])
          result.wait();
      }
      return result[1];

    }
    catch (Exception x)
    {
      Logger.error(x);
      return false;
    }
  }

  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}
   * s) auswählen lässt. ACHTUNG! Diese Methode darf nur im Event Dispatching Thread
   * aufgerufen werden.
   * 
   * @param result
   *          ein 2-elementiges Array auf das nur synchronisiert zugegriffen wird.
   *          Das erste Element wird auf false gesetzt, sobald der Dialog geschlossen
   *          wird. Das zweite Element wird in diesem Fall auf true gesetzt, wenn der
   *          Benutzer mir Okay bestätigt hat. Bei sonstigen Arten, den Dialog zu
   *          beenden bleibt das zweite Element unangetastet, sollte also mit false
   *          vorbelegt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void createSelectFromListDialog(final Vector<ListElement> list,
      final boolean[] result)
  {
    final JFrame myFrame = new JFrame(L.m("Gewünschte Ausdrucke wählen"));
    myFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myFrame.addWindowListener(new WindowListener()
    {
      @Override
      public void windowOpened(WindowEvent e)
      {}

      @Override
      public void windowClosing(WindowEvent e)
      {}

      @Override
      public void windowClosed(WindowEvent e)
      {
        synchronized (result)
        {
          result[0] = true;
          result.notifyAll();
        }
      }

      @Override
      public void windowIconified(WindowEvent e)
      {}

      @Override
      public void windowDeiconified(WindowEvent e)
      {}

      @Override
      public void windowActivated(WindowEvent e)
      {}

      @Override
      public void windowDeactivated(WindowEvent e)
      {}
    });
    myFrame.setAlwaysOnTop(true);
    JPanel myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.setContentPane(myPanel);

    final JList<MailMerge.ListElement> myList = new JList<MailMerge.ListElement>(list);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (int i = 0; i < list.size(); ++i)
    {
      ListElement ele = list.get(i);
      if (ele.isSelected()) myList.addSelectionInterval(i, i);
    }

    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane, BorderLayout.CENTER);

    Box top = Box.createVerticalBox();
    top.add(new JLabel(
      L.m("Bitte wählen Sie, welche Ausdrucke Sie bekommen möchten")));
    top.add(Box.createVerticalStrut(5));
    myPanel.add(top, BorderLayout.NORTH);

    Box bottomV = Box.createVerticalBox();
    bottomV.add(Box.createVerticalStrut(5));
    Box bottom = Box.createHorizontalBox();
    bottomV.add(bottom);
    myPanel.add(bottomV, BorderLayout.SOUTH);

    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myFrame.dispose();
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Alle"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myList.setSelectionInterval(0, list.size() - 1);
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalStrut(5));

    button = new JButton(L.m("Keinen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myList.clearSelection();
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Start"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        for (int i = 0; i < list.size(); ++i)
          (list.get(i)).setSelected(false);
        int[] sel = myList.getSelectedIndices();
        for (int i = 0; i < sel.length; ++i)
        {
          (list.get(sel[i])).setSelected(true);
        }
        synchronized (result)
        {
          result[1] = true;
        }
        myFrame.dispose();
      }
    });
    bottom.add(button);

    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }

  private static class CalcCellQueryResults implements QueryResults
  {
    /**
     * Bildet einen Spaltennamen auf den Index in dem zu dem Datensatz gehörenden
     * String[]-Array ab.
     */
    private Map<String, Integer> mapColumnNameToIndex;

    private List<Dataset> datasets = new ArrayList<Dataset>();

    @Override
    public int size()
    {
      return datasets.size();
    }

    @Override
    public Iterator<Dataset> iterator()
    {
      return datasets.iterator();
    }

    @Override
    public boolean isEmpty()
    {
      return datasets.isEmpty();
    }

    public void setColumnNameToIndexMap(Map<String, Integer> mapColumnNameToIndex)
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

      @Override
      public String get(String columnName) throws ColumnNotFoundException
      {
        Number idx = mapColumnNameToIndex.get(columnName);
        if (idx == null)
          throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!",
            columnName));
        return data[idx.intValue()];
      }

      @Override
      public String getKey()
      {
        return "key";
      }

    }

  }

  /**
   * Liefert die sichtbaren Zellen des Arbeitsblattes mit Namen sheetName aus dem
   * Calc Dokument, dessen Fenstertitel windowTitle ist. Die erste Zeile der
   * Calc-Tabelle wird herangezogen als Spaltennamen. Diese Spaltennamen werden zu
   * schema hinzugefügt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static QueryResults getVisibleCalcData(String windowTitle,
      String sheetName, Set<String> schema)
  {
    CalcCellQueryResults results = new CalcCellQueryResults();

    try
    {
      XSpreadsheetDocument doc = null;
      XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
      while (xenu.hasMoreElements())
      {
        doc = UNO.XSpreadsheetDocument(xenu.nextElement());
        if (doc != null)
        {
          String title =
            (String) UNO.getProperty(
              UNO.XModel(doc).getCurrentController().getFrame(), "Title");
          if (windowTitle.equals(title)) break;
        }
      }

      if (doc != null)
      {
        XCellRangesQuery sheet =
          UNO.XCellRangesQuery(doc.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
          SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
          XSheetCellRanges visibleCellRanges = sheet.queryVisibleCells();
          XSheetCellRanges nonEmptyCellRanges =
            sheet.queryContentCells((short) (com.sun.star.sheet.CellFlags.VALUE
              | com.sun.star.sheet.CellFlags.DATETIME
              | com.sun.star.sheet.CellFlags.STRING | com.sun.star.sheet.CellFlags.FORMULA));
          CellRangeAddress[] nonEmptyCellRangeAddresses =
            nonEmptyCellRanges.getRangeAddresses();
          for (int i = 0; i < nonEmptyCellRangeAddresses.length; ++i)
          {
            XSheetCellRanges ranges =
              UNO.XCellRangesQuery(visibleCellRanges).queryIntersection(
                nonEmptyCellRangeAddresses[i]);
            CellRangeAddress[] rangeAddresses = ranges.getRangeAddresses();
            for (int k = 0; k < rangeAddresses.length; ++k)
            {
              CellRangeAddress addr = rangeAddresses[k];
              for (int x = addr.StartColumn; x <= addr.EndColumn; ++x)
                columnIndexes.add(Integer.valueOf(x));

              for (int y = addr.StartRow; y <= addr.EndRow; ++y)
                rowIndexes.add(Integer.valueOf(y));
            }
          }

          if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
          {
            XCellRange sheetCellRange = UNO.XCellRange(sheet);

            /*
             * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte
             * als Tabellenspaltennamen interpretieren. Ein Mapping in
             * mapColumnNameToIndex wird erzeugt, wobei NICHT auf den Index in der
             * Calc-Tabelle gemappt wird, sondern auf den Index im später für jeden
             * Datensatz existierenden String[]-Array.
             */
            int ymin = rowIndexes.first().intValue();
            Map<String, Integer> mapColumnNameToIndex =
              new HashMap<String, Integer>();
            int idx = 0;
            Iterator<Integer> iter = columnIndexes.iterator();
            while (iter.hasNext())
            {
              int x = iter.next().intValue();
              String columnName =
                UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
              if (columnName.length() > 0)
              {
                mapColumnNameToIndex.put(columnName, Integer.valueOf(idx));
                schema.add(columnName);
                ++idx;
              }
              else
                iter.remove(); // Spalten mit leerem Spaltennamen werden nicht
              // benötigt.
            }

            results.setColumnNameToIndexMap(mapColumnNameToIndex);

            /*
             * Datensätze erzeugen
             */
            Iterator<Integer> rowIndexIter = rowIndexes.iterator();
            rowIndexIter.next(); // erste Zeile enthält die Tabellennamen, keinen
            // Datensatz
            while (rowIndexIter.hasNext())
            {
              int y = rowIndexIter.next().intValue();
              String[] data = new String[columnIndexes.size()];
              Iterator<Integer> columnIndexIter = columnIndexes.iterator();
              idx = 0;
              while (columnIndexIter.hasNext())
              {
                int x = columnIndexIter.next().intValue();
                String value =
                  UNO.XTextRange(sheetCellRange.getCellByPosition(x, y)).getString();
                data[idx++] = value;
              }

              results.addDataset(data);
            }
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }

    return results;
  }

  /**
   * Startet den ultimativen Seriendruck für pmod.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    SuperMailMerge.superMailMerge(pmod);
  }

  /**
   * Klasse, die den ultimativen Seriendruck realisiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class SuperMailMerge
  {
    /**
     * Liste von {@link Runnable}-Objekten, die sequentiell abgearbeitet werden im
     * Nicht-Event-Dispatching-Thread.
     */
    private List<Runnable> todo = new LinkedList<Runnable>();

    /**
     * Wird dies auf false gesetzt, so beendet sich {@link #run()}.
     */
    private boolean running = true;

    /**
     * Die Menge der Namen aller OOo-Datenquellen.
     */
    private Set<String> datasourceNames = new TreeSet<String>();

    /**
     * Die Menge aller Titel von offenen Calc-Dokument-Fenstern.
     */
    private Set<String> calcDocumentTitles = new TreeSet<String>();

    /**
     * Die ComboBox in der der Benutzer die OOo-Datenquelle bzw, das Calc-Dokument
     * für den MailMerge auswählen kann.
     */
    private JComboBox<String> datasourceSelector;

    /**
     * Das XPrintModel für diesen MailMerge.
     */
    private XPrintModel pmod;

    /**
     * Die ComboBox in der der Benutzer die Tabelle für den MailMerge auswählen kann.
     */
    private JComboBox<String> tableSelector;

    /**
     * Der Name der aktuell ausgewählten Datenquelle (bzw, der Titel des ausgewählten
     * Calc-Dokuments). ACHTUNG! Diese Variable wird initial vom Nicht-EDT befüllt,
     * dann aber nur noch im Event Dispatching Thread verwendet bis zu dem Zeitpunkt
     * wo die Datenquellenauswahl beendet ist und der Druck durch den nicht-EDT
     * Thread angeleiert wird.
     */
    private String selectedDatasource = "";

    /**
     * Der Name der aktuell ausgewählten Tabelle. ACHTUNG! Diese Variable wird
     * initial vom Nicht-EDT befüllt, dann aber nur noch im Event Dispatching Thread
     * verwendet bis zu dem Zeitpunkt wo die Datenquellenauswahl beendet ist und der
     * Druck durch den nicht-EDT Thread angeleiert wird.
     */
    private String selectedTable = "";

    /**
     * Startet den ultimativen MailMerge. ACHTUNG! Diese Methode kehrt erst zurück,
     * wenn der Ausdruck abgeschlossen oder abgebrochen wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static void superMailMerge(XPrintModel pmod)
    {
      SuperMailMerge merge = new SuperMailMerge(pmod);
      merge.run();
    }

    private SuperMailMerge(XPrintModel pmod)
    { // TESTED
      this.pmod = pmod;

      /*
       * Namen aller OOo-Datenquellen bestimmen.
       */
      String[] datasourceNamesA = UNO.XNameAccess(UNO.dbContext).getElementNames();
      for (int i = 0; i < datasourceNamesA.length; ++i)
        datasourceNames.add(datasourceNamesA[i]);

      /*
       * Titel aller offenen Calc-Fenster bestimmen.
       */
      XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
      while (xenu.hasMoreElements())
      {
        try
        {
          XSpreadsheetDocument doc = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (doc != null)
          {
            String title =
              (String) UNO.getProperty(
                UNO.XModel(doc).getCurrentController().getFrame(), "Title");
            if (title != null) calcDocumentTitles.add(title);
          }
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }

      /*
       * Aktuell über Bearbeiten/Datenbank austauschen gewählte Datenquelle/Tabelle
       * bestimmen, falls gesetzt.
       */
      XTextDocument doc = pmod.getTextDocument();
      XPropertySet settings = null;
      try
      {
        settings =
          UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.document.Settings"));
      }
      catch (Exception x)
      {
        Logger.error(L.m("Kann DocumentSettings nicht auslesen"), x);
        return;
      }
      String datasource =
        (String) UNO.getProperty(settings, "CurrentDatabaseDataSource");
      String table = (String) UNO.getProperty(settings, "CurrentDatabaseCommand");
      Integer type =
        (Integer) UNO.getProperty(settings, "CurrentDatabaseCommandType");
      if (datasource != null && datasourceNames.contains(datasource)
        && table != null && table.length() > 0 && type != null
        && type.intValue() == CommandType.TABLE)
      {
        selectedDatasource = datasource;
        selectedTable = table;
      }

      /*
       * Erzeugen der GUI auf die todo-Liste setzen.
       */
      todo.add(new Runnable()
      {
        @Override
        public void run()
        {
          inEDT("createGUI");
        }
      });
    }

    /**
     * Arbeitet die {@link #todo}-Liste ab, solange {@link #running}==true.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void run()
    {
      try
      {
        while (running)
        {
          Runnable r;
          synchronized (todo)
          {
            while (todo.isEmpty())
              todo.wait();
            r = todo.remove(0);
          }
          r.run();
        }
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    /**
     * Erstellt die GUI für die Auswahl der Datenquelle/Tabelle für den
     * SuperMailMerge. Darf nur im EDT aufgerufen werden.
     * 
     * Diese Methode wird indirekt per Reflection aufgerufen (daher keine
     * "unused"-Warnung)
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    @SuppressWarnings("unused")
    public void createGUI()
    {
      final JFrame myFrame = new JFrame(L.m("Seriendruck"));
      myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      myFrame.addWindowListener(new WindowListener()
      {
        @Override
        public void windowOpened(WindowEvent e)
        {}

        @Override
        public void windowClosing(WindowEvent e)
        {
          stopRunning();
          myFrame.dispose();
        }

        @Override
        public void windowClosed(WindowEvent e)
        {}

        @Override
        public void windowIconified(WindowEvent e)
        {}

        @Override
        public void windowDeiconified(WindowEvent e)
        {}

        @Override
        public void windowActivated(WindowEvent e)
        {}

        @Override
        public void windowDeactivated(WindowEvent e)
        {}
      });

      myFrame.setAlwaysOnTop(true);
      Box vbox = Box.createVerticalBox();
      JPanel myPanel = new JPanel();
      // myPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      myFrame.add(myPanel);
      myPanel.add(vbox);

      /*
       * Datenquellen-Auswahl-ComboBox bauen
       */
      Box hbox = Box.createHorizontalBox();
      vbox.add(hbox);
      hbox.add(new JLabel(L.m("Datenquelle")));
      datasourceSelector = new JComboBox<String>();
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(datasourceSelector);
      int selected = 0;
      int idx = 0;
      Iterator<String> iter = calcDocumentTitles.iterator();
      while (iter.hasNext())
      {
        datasourceSelector.addItem(iter.next());
        ++idx;
      }
      iter = datasourceNames.iterator();
      while (iter.hasNext())
      {
        String dsName = iter.next();
        if (dsName.equals(selectedDatasource)) selected = idx;
        datasourceSelector.addItem(dsName);
        ++idx;
      }

      if (idx > 0)
      {
        datasourceSelector.setSelectedIndex(selected);
        String newDatasource = (String) datasourceSelector.getSelectedItem();
        if (newDatasource != null) selectedDatasource = newDatasource;
      }

      /*
       * Auf Änderungen der Datenquellen-Auswahl-Combobox reagieren.
       */
      datasourceSelector.addItemListener(new ItemListener()
      {
        @Override
        public void itemStateChanged(ItemEvent e)
        {
          String newDatasource = (String) datasourceSelector.getSelectedItem();
          String newTable = (String) tableSelector.getSelectedItem();
          if (newDatasource != null && !newDatasource.equals(selectedDatasource))
          {
            selectedDatasource = newDatasource;
            selectedTable = newTable;
            addTodo("updateTableSelector", new String[] {
              selectedDatasource, selectedTable });
          }
        }
      });

      /*
       * Tabellenauswahl-ComboBox bauen.
       */
      hbox = Box.createHorizontalBox();
      vbox.add(Box.createVerticalStrut(5));
      vbox.add(hbox);
      hbox.add(new JLabel(L.m("Tabelle")));
      hbox.add(Box.createHorizontalStrut(5));
      tableSelector = new JComboBox<String>();
      hbox.add(tableSelector);

      /*
       * Buttons hinzufügen.
       */

      hbox = Box.createHorizontalBox();
      vbox.add(Box.createVerticalStrut(5));
      vbox.add(hbox);
      JButton button = new JButton(L.m("Abbrechen"));
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          stopRunning();
          myFrame.dispose();
        }
      });
      hbox.add(button);

      button = new JButton(L.m("Start"));
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          selectedTable = (String) tableSelector.getSelectedItem();
          selectedDatasource = (String) datasourceSelector.getSelectedItem();
          if (selectedTable != null && selectedDatasource != null)
          {
            clearTodo();
            addTodo("print", Boolean.FALSE);
            myFrame.dispose();
          }
        }
      });
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(button);

      button = new JButton(L.m("Einzelauswahl"));
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          selectedTable = (String) tableSelector.getSelectedItem();
          selectedDatasource = (String) datasourceSelector.getSelectedItem();
          if (selectedTable != null && selectedDatasource != null)
          {
            clearTodo();
            addTodo("print", Boolean.TRUE);
            myFrame.dispose();
          }
        }
      });
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(button);

      addTodo("updateTableSelector", new String[] {
        selectedDatasource, selectedTable });

      myFrame.pack();
      int frameWidth = myFrame.getWidth();
      int frameHeight = myFrame.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width / 2 - frameWidth / 2;
      int y = screenSize.height / 2 - frameHeight / 2;
      myFrame.setLocation(x, y);
      myFrame.setVisible(true);
      myFrame.requestFocus();
    }

    /**
     * Wird im Nicht-EDT aufgerufen und bestimmt die Tabellen der neu ausgewählten
     * Datenquelle und lässt dann im EDT die {@link #tableSelector}-ComboBox updaten.
     * 
     * Diese Methode wird indirekt über Reflection aufgerufen (daher keine
     * "unused"-Warnung)
     * 
     * @param datasourceAndTableName
     *          das erste Element ist der Name der neu ausgewählten Datenquelle bzw.
     *          des Calc-Dokuments. Das zweite Element ist der Name der vorher
     *          ausgewählten Tabelle (oder null). Letzterer wird benötigt, da falls
     *          die neue Datenquelle eine Tabelle gleichen Namens besitzt, diese als
     *          aktuelle Auswahl der ComboBox eingestellt werden soll.
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    @SuppressWarnings("unused")
    public void updateTableSelector(String[] datasourceAndTableName)
    {
      String datasourceName = datasourceAndTableName[0];
      final String tableName = datasourceAndTableName[1]; // ACHTUNG!! Darf null
      // sein!
      String[] tableNames = null;
      if (calcDocumentTitles.contains(datasourceName))
      {
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while (xenu.hasMoreElements())
        {
          try
          {
            XSpreadsheetDocument doc = UNO.XSpreadsheetDocument(xenu.nextElement());
            if (doc != null)
            {
              String title =
                (String) UNO.getProperty(
                  UNO.XModel(doc).getCurrentController().getFrame(), "Title");
              if (datasourceName.equals(title))
              {
                tableNames = UNO.XNameAccess(doc.getSheets()).getElementNames();
                break;
              }
            }
          }
          catch (Exception x)
          {
            Logger.error(x);
            return;
          }
        }
      }
      else if (datasourceNames.contains(datasourceName))
      {
        try
        {
          XDataSource ds =
            UNO.XDataSource(UNO.dbContext.getRegisteredObject(datasourceName));
          long lgto = DATABASE_TIMEOUT / 1000;
          if (lgto < 1) lgto = 1;
          ds.setLoginTimeout((int) lgto);
          XConnection conn = ds.getConnection("", "");
          XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
          tableNames = tables.getElementNames();
        }
        catch (Exception x)
        {
          Logger.error(x);
          return;
        }
      }
      else
        return; // kann passieren, falls weder Datenquellen noch Calc-Dokumente
      // vorhanden.

      if (tableNames == null || tableNames.length == 0)
        tableNames = new String[] { "n/a" };

      final String[] tNames = tableNames;
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            tableSelector.removeAllItems();
            int selected = 0;
            for (int i = 0; i < tNames.length; ++i)
            {
              if (tNames[i].equals(tableName)) selected = i;
              tableSelector.addItem(tNames[i]);
            }
            tableSelector.setSelectedIndex(selected);
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    @SuppressWarnings("unused") // wird per reflection aufgerufen
    public void print(Boolean offerselection)
    {
      if (calcDocumentTitles.contains(selectedDatasource))
      {
        Set<String> schema = new HashSet<String>();
        QueryResults data =
          getVisibleCalcData(selectedDatasource, selectedTable, schema);
        mailMerge(pmod, offerselection.booleanValue(), schema, data);
      }
      else
        mailMerge(pmod, selectedDatasource, selectedTable,
          Integer.valueOf(CommandType.TABLE), offerselection.booleanValue());
    }

    /**
     * Fügt den Aufruf der public-Methode method zur {@link #todo}-Liste hinzu.
     * 
     * @param method
     *          der Name einer public-Methode.
     * @param param
     *          Parameter, der der Methode übergeben werden soll, oder null falls die
     *          Methode keine Parameter erwartet.
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void addTodo(String method, Object param)
    {
      try
      {
        Class<?>[] paramTypes = null;
        Object[] params = null;
        if (param != null)
        {
          paramTypes = new Class[] { param.getClass() };
          params = new Object[] { param };
        }
        final Object[] finalParams = params;
        final Method m = this.getClass().getMethod(method, paramTypes);
        final SuperMailMerge self = this;
        synchronized (todo)
        {
          todo.add(new Runnable()
          {
            @Override
            public void run()
            {
              try
              {
                m.invoke(self, finalParams);
              }
              catch (Exception x)
              {
                Logger.error(x);
              }
              ;
            }
          });
          todo.notifyAll();
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    /**
     * Leert die {@link #todo}-Liste.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void clearTodo()
    {
      synchronized (todo)
      {
        todo.clear();
      }
    }

    /**
     * Löscht die {@link #todo}-Liste und fügt ihr dann einen Befehl zum Setzen von
     * {@link #running} auf false hinzu.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void stopRunning()
    {
      synchronized (todo)
      {
        todo.clear();
        todo.add(new Runnable()
        {
          @Override
          public void run()
          {
            running = false;
          }
        });
        todo.notifyAll();
      }
    }

    /**
     * Führt die public-Methode "method" im EDT aus (ansynchron).
     * 
     * @param method
     *          der Name einer public-Methode
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void inEDT(String method)
    {
      try
      {
        final Method m = this.getClass().getMethod(method, (Class[]) null);
        final SuperMailMerge self = this;
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              m.invoke(self, (Object[]) null);
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

  }
}
