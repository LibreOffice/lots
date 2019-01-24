/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 04.03.2008 | BNK | Herausfaktorisiert aus MailMergeNew
 * 23.10.2013 | JGM | FilePicker durch Swing FileChooser ersetzt (Loest Probleme mit LO 4)
 * 20.11.2013 | UKT | Swing FileChooser wieder durch LO FilePicker ersetzt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sdbc.XRow;
import com.sun.star.sdbc.XRowSet;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.ui.dialogs.XFilePicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.QueryResultsWithSchema;
import de.muenchen.allg.itd51.wollmux.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Stellt eine OOo-Datenquelle oder ein offenes Calc-Dokument über ein gemeinsames
 * Interface zur Verfügung. Ist auch zuständig dafür, das Calc-Dokument falls nötig
 * wieder zu öffnen und Änderungen seines Fenstertitels und/oder seiner
 * Speicherstelle zu überwachen. Stellt auch Dialoge zur Verfügung zur Auswahl der
 * Datenquelle.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeDatasource
{
  /**
   * Wert für {@link #sourceType}, der anzeigt, dass keine Datenquelle ausgewählt
   * ist.
   */
  private static final int SOURCE_NONE = 0;

  /**
   * Wert für {@link #sourceType}, der anzeigt, dass eine Calc-Tabelle als
   * Datenquelle ausgewählt ist.
   */
  private static final int SOURCE_CALC = 1;

  /**
   * Wert für {@link #sourceType}, der anzeigt, dass eine OOo Datenquelle als
   * Datenquelle ausgewählt ist.
   */
  private static final int SOURCE_DB = 2;

  /**
   * Wenn nach dieser Zeit in ms nicht alle Daten des Seriendruckauftrags ausgelesen
   * werden konnten, dann wird der Druckauftrag nicht ausgeführt (und muss eventuell
   * über die Von Bis Auswahl in mehrere Aufträge zerteilt werden).
   */
  private static final long MAILMERGE_GETCONTENTS_TIMEOUT = 60000;

  /**
   * Timeout für den Login bei einer OOo-Datenquelle.
   */
  private static final long MAILMERGE_LOGIN_TIMEOUT = 5000;

  /**
   * Ein String der möglichst nie in einem vom Benutzer eingegebenen Felder-Anpassen
   * Feld auftauchen sollte. Wird als Platzhalter für das Einfügen der Zeilennummer
   * in den Formel-String verwendet. Darf keine Zeichen enthalten, die in regulären
   * Ausdrücken Bedeutung haben.
   */
  private static final String ROW_NUM_PLACEHOLDER = "Ø©¿";

  /**
   * Zeigt an, was derzeit als Datenquelle ausgewählt ist.
   */
  private int sourceType = SOURCE_NONE;

  /**
   * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument derzeit
   * offen ist, dann ist diese Variable != null. Falls das Dokument nicht offen ist,
   * so ist seine URL in {@link #calcUrl} zu finden. Die Kombination calcDoc == null
   * && calcUrl == null && sourceType == SOURCE_CALC ist unzulässig.
   */
  private XSpreadsheetDocument calcDoc = null;

  /**
   * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument bereits
   * einmal gespeichert wurde, findet sich hier die URL des Dokuments, ansonsten ist
   * der Wert null. Falls das Dokument nur als UnbenanntX im Speicher existiert, so
   * ist eine Referenz auf das Dokument in {@link #calcDoc} zu finden. Die
   * Kombination calcDoc == null && calcUrl == null && sourceType == SOURCE_CALC ist
   * unzulässig.
   */
  private String calcUrl = null;

  /**
   * Falls {@link #sourceType} == {@link #SOURCE_DB}, so ist dies der Name der
   * ausgewählten OOo-Datenquelle, ansonsten null.
   */
  private String oooDatasourceName = null;

  /**
   * Falls {@link #sourceType} == {@link #SOURCE_DB} und die Datenquelle bereits
   * initialisiert wurde (durch {@link #getOOoDatasource()}), so ist dies eine
   * {@link Datasource} zum Zugriff auf die ausgewählte OOo-Datenquelle, ansonsten
   * null.
   */
  private Datasource oooDatasource = null;

  /**
   * Speichert den Namen der Tabelle bzw, des Tabellenblattes, die als Quelle der
   * Serienbriefdaten ausgewählt wurde. Ist niemals null, kann aber der leere String
   * sein oder ein Name, der gar nicht in der entsprechenden Datenquelle existiert.
   */
  private String tableName = "";

  /**
   * Wenn als aktuelle Datenquelle ein Calc-Dokument ausgewählt ist, dann wird dieser
   * Listener darauf registriert um Änderungen des Speicherorts, so wie das Schließen
   * des Dokuments zu überwachen.
   */
  private MyCalcListener myCalcListener = new MyCalcListener();

  /**
   * Wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgewählten
   * Datenquelle.
   */
  private TextDocumentController documentController;

  /**
   * Erzeugt eine neue Datenquelle.
   * 
   * @param documentController
   *          wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgewählten
   *          Datenquelle.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public MailMergeDatasource(TextDocumentController documentController)
  {
    this.documentController = documentController;
    openDatasourceFromLastStoredSettings();
  }

  /**
   * Liefert die Titel der Spalten der aktuell ausgewählten Tabelle. Ist derzeit
   * keine Tabelle ausgewählt oder enthält die ausgewählte Tabelle keine benannten
   * Spalten, so wird ein leerer Vector geliefert. Die Reihenfolge der Spalten
   * entspricht der Reihenfolge der Werte, wie sie von
   * {@link #getValuesForDataset(int)} geliefert werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public List<String> getColumnNames()
  {
    try
    {
      switch (sourceType)
      {
        case SOURCE_CALC:
          return getColumnNames(getCalcDoc(), tableName);
        case SOURCE_DB:
          return getDbColumnNames(getOOoDatasource());
        default:
          return new Vector<String>();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return new Vector<String>();
    }
  }

  /**
   * Liefert die sichtbaren Inhalte (als Strings) der Zellen aus der rowIndex-ten
   * sichtbaren nicht-leeren Zeile (wobei die erste solche Zeile, diejenige die die
   * Namen for {@link #getColumnNames()} liefert, den Index 0 hat) aus der aktuell
   * ausgewählten Tabelle. Falls sich die Daten zwischen den Aufrufen der beiden
   * Methoden nicht geändert haben, passen die zurückgelieferten Daten in Anzahl und
   * Reihenfolge genau zu der von {@link #getColumnNames()} gelieferten Liste.
   * 
   * Falls rowIndex zu groß ist, wird ein Vektor mit leeren Strings zurückgeliefert.
   * Im Fehlerfall wird ein leerer Vektor zurückgeliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public List<String> getValuesForDataset(int rowIndex)
  {
    try
    {
      switch (sourceType)
      {
        case SOURCE_CALC:
          return getValuesForDataset(getCalcDoc(), tableName, rowIndex);
        case SOURCE_DB:
          return getDbValuesForDataset(getOOoDatasource(), rowIndex);
        default:
          return new Vector<String>();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return new Vector<String>();
    }
  }

  /**
   * Liefert die Anzahl der Datensätze der aktuell ausgewählten Tabelle. Ist derzeit
   * keine Tabelle ausgewählt oder enthält die ausgewählte Tabelle keine benannten
   * Spalten, so wird 0 geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public int getNumberOfDatasets()
  {
    try
    {
      switch (sourceType)
      {
        case SOURCE_CALC:
          return getNumberOfDatasets(getCalcDoc(), tableName);
        case SOURCE_DB:
          return getDbNumberOfDatasets();
        default:
          return 0;
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return 0;
    }
  }

  /**
   * Liefert true gdw die Funktion {@link #addColumns(Map)} verfügbar ist für diese
   * Datenquelle.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public boolean supportsAddColumns()
  {
    switch (sourceType)
    {
      case SOURCE_CALC:
        return true;
      case SOURCE_DB:
        return false;
      default:
        return false;
    }
  }

  /**
   * Liefert den Inhalt der aktuell ausgewählten Serienbriefdatenquelle (leer, wenn
   * keine ausgewählt).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public QueryResultsWithSchema getData()
  {
    try
    {
      switch (sourceType)
      {
        case SOURCE_CALC:
          return getData(getCalcDoc(), tableName);
        case SOURCE_DB:
          return getDbData(getOOoDatasource());
        default:
          return new QueryResultsWithSchema();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return new QueryResultsWithSchema();
    }
  }

  /**
   * Liefert true, wenn derzeit eine Datenquelle ausgewählt ist.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasDatasource()
  {
    return sourceType != SOURCE_NONE;
  }

  /**
   * Lässt den Benutzer über einen Dialog die Datenquelle auswählen.
   * 
   * @param parent
   *          der JFrame, zu dem dieser Dialog gehören soll.
   * @param callback
   *          wird (im EDT) getriggert, nachdem eine neue Datenquelle ausgewählt
   *          wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void showDatasourceSelectionDialog(final JFrame parent,
      final Runnable callback)
  {
    //set JDialog to Modeless type so that it remains visible when changing focus between opened 
    //calc and writer document. Drawback: when this Dialog is open, the "Seriendruck" bar is 
    //active too.
    final JDialog datasourceSelector =
      new JDialog(parent, L.m("Serienbriefdaten auswählen"), false);

    Box vbox = Box.createVerticalBox();
    datasourceSelector.add(vbox);

    JLabel label = new JLabel(L.m("Wo sind Ihre Serienbriefdaten ?"));
    vbox.add(label);

    JButton button;
    button = createDatasourceSelectorCalcWindowButton();
    if (button != null)
    {
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
          selectOpenCalcWindowAsDatasource(parent, callback);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
    }

    button = new JButton(L.m("Datei..."));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        datasourceSelector.dispose();
        selectFileAsDatasource(parent, callback);
      }
    });
    vbox.add(DimAdjust.maxWidthUnlimited(button));

    button = new JButton(L.m("Neue Calc-Tabelle..."));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        datasourceSelector.dispose();
        openAndselectNewCalcTableAsDatasource(parent, callback);
      }
    });
    vbox.add(DimAdjust.maxWidthUnlimited(button));

    button = new JButton(L.m("Datenbank..."));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        datasourceSelector.dispose();
        selectOOoDatasourceAsDatasource(parent, callback);
      }
    });
    vbox.add(DimAdjust.maxWidthUnlimited(button));

    label = new JLabel(L.m("Aktuell ausgewählte Tabelle"));
    vbox.add(label);
    String str = L.m("<keine>");
    if (sourceType == SOURCE_CALC)
    {
      if (calcDoc != null)
      {
        String title =
          (String) UNO.getProperty(
            UNO.XModel(calcDoc).getCurrentController().getFrame(), "Title");
        if (title == null) title = "?????";
        str = stripOpenOfficeFromWindowName(title);
      }
      else
      {
        str = calcUrl;
      }
    }
    else if (sourceType == SOURCE_DB)
    {
      str = oooDatasourceName;
    }

    if (tableName.length() > 0) str = str + "." + tableName;

    label = new JLabel(str);
    vbox.add(label);

    button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      @Override
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
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    datasourceSelector.setLocation(x, y);
    datasourceSelector.setResizable(false);
    datasourceSelector.setVisible(true);
  }

  /**
   * Versucht die Datenquelle in den Vordergrund zu holen und wird vom Button
   * "Tabelle bearbeiten" aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD-5.1)
   * 
   *         TESTED
   */
  public void toFront()
  {
    Object document = null;
    if (sourceType == SOURCE_CALC)
    {
      document = calcDoc;
    }
    else if (sourceType == SOURCE_DB)
    {
      try
      {
        XDocumentDataSource ds =
          UNO.XDocumentDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        XOfficeDatabaseDocument dbdoc = ds.getDatabaseDocument();
        String url = UNO.XModel(dbdoc).getURL();

        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while (xenu.hasMoreElements())
        {
          try
          {
            XModel model = UNO.XModel(xenu.nextElement());
            if (model.getURL().equals(url))
            {
              document = model;
              break;
            }
          }
          catch (Exception x)
          {}
        }

        if (document == null)
          document = UNO.loadComponentFromURL(url, false, false);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    try
    {
      XModel documentModel = UNO.XModel(document);
      if (documentModel != null)
      {
        XTopWindow win =
          UNO.XTopWindow(documentModel.getCurrentController().getFrame().getContainerWindow());
        win.toFront();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
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

  /**
   * Öffnet die Datenquelle die durch einen früheren Aufruf von
   * storeDatasourceSettings() im Dokument hinterlegt wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void openDatasourceFromLastStoredSettings()
  {
    ConfigThingy mmconf = documentController.getModel().getMailmergeConfig();
    ConfigThingy datenquelle = new ConfigThingy("");
    try
    {
      datenquelle = mmconf.query("Datenquelle").getLastChild();
    }
    catch (NodeNotFoundException e)
    {}

    String type = null;
    try
    {
      type = datenquelle.get("TYPE").toString();
    }
    catch (NodeNotFoundException e)
    {}

    if ("calc".equalsIgnoreCase(type))
    {
      try
      {
        String url = datenquelle.get("URL").toString();
        String table = datenquelle.get("TABLE").toString();
        try
        {
          Object d = getCalcDoc(url);
          if (d != null) setTable(table);
        }
        catch (UnavailableException e)
        {
          Logger.debug(e);
        }
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(L.m("Fehlendes Argument für Datenquelle vom Typ '%1':", type),
          e);
      }
    }
    else if ("ooo".equalsIgnoreCase(type))
    {
      try
      {
        String source = datenquelle.get("SOURCE").toString();
        String table = datenquelle.get("TABLE").toString();
        getOOoDatasource(source);
        setTable(table);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(L.m("Fehlendes Argument für Datenquelle vom Typ '%1':", type),
          e);
      }
    }
    else if (type != null)
    {
      Logger.error(L.m("Ignoriere Datenquelle mit unbekanntem Typ '%1'", type));
    }
  }

  /**
   * Speichert die aktuellen Einstellungen zu dieser Datenquelle im zugehörigen
   * Dokument persistent ab, damit die Datenquelle beim nächsten mal wieder
   * automatisch geöffnet/verbunden werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void storeDatasourceSettings()
  {
    // ConfigThingy für Einstellungen der Datenquelle erstellen:
    ConfigThingy dq = new ConfigThingy("Datenquelle");
    ConfigThingy arg;
    switch (sourceType)
    {
      case SOURCE_CALC:
        if (calcUrl == null || tableName.length() == 0) break;
        arg = new ConfigThingy("TYPE");
        arg.addChild(new ConfigThingy("calc"));
        dq.addChild(arg);
        arg = new ConfigThingy("URL");
        arg.addChild(new ConfigThingy(calcUrl));
        dq.addChild(arg);
        arg = new ConfigThingy("TABLE");
        arg.addChild(new ConfigThingy(tableName));
        dq.addChild(arg);
        break;
      case SOURCE_DB:
        if (oooDatasourceName == null || tableName.length() == 0) break;
        arg = new ConfigThingy("TYPE");
        arg.addChild(new ConfigThingy("ooo"));
        dq.addChild(arg);
        arg = new ConfigThingy("SOURCE");
        arg.addChild(new ConfigThingy(oooDatasourceName));
        dq.addChild(arg);
        arg = new ConfigThingy("TABLE");
        arg.addChild(new ConfigThingy(tableName));
        dq.addChild(arg);
        break;
    }

    ConfigThingy seriendruck = new ConfigThingy("Seriendruck");
    if (dq.count() > 0) seriendruck.addChild(dq);
    documentController.setMailmergeConfig(seriendruck);
  }

  /**
   * Liefert die Anzahl Datensätze aus OOo-Datenquelle oooDatasourceName, Tabelle
   * tableName.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   * 
   *         TESTED
   */
  private int getDbNumberOfDatasets()
  {
    if (sourceType != SOURCE_DB) return 0;

    XRowSet results = null;
    XConnection conn = null;
    try
    {
      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        long lgto = MAILMERGE_LOGIN_TIMEOUT / 1000;
        if (lgto < 1) lgto = 1;
        ds.setLoginTimeout((int) lgto);
        conn = ds.getConnection("", "");
      }
      catch (Exception x)
      {
        throw new TimeoutException(L.m(
          "Kann keine Verbindung zur Datenquelle \"%1\" herstellen",
          oooDatasourceName));
      }

      Object rowSet = UNO.createUNOService("com.sun.star.sdb.RowSet");
      results = UNO.XRowSet(rowSet);

      XPropertySet xProp = UNO.XPropertySet(results);

      xProp.setPropertyValue("ActiveConnection", conn);

      /*
       * EscapeProcessing == false bedeutet, dass OOo die Query nicht selbst anfassen
       * darf, sondern direkt an die Datenbank weiterleiten soll. Wird dies verwendet
       * ist das Ergebnis (derzeit) immer read-only, da OOo keine Updates von
       * Statements durchführen kann, die es nicht geparst hat. Siehe Kommentar zu
       * http://qa.openoffice.org/issues/show_bug.cgi?id=78522 Entspricht dem Button
       * SQL mit grünem Haken (SQL-Kommando direkt ausführen) im Base-Abfrageentwurf.
       */
      xProp.setPropertyValue("EscapeProcessing", Boolean.FALSE);

      xProp.setPropertyValue("CommandType",
        Integer.valueOf(com.sun.star.sdb.CommandType.COMMAND));

      xProp.setPropertyValue("Command", "SELECT COUNT(*) FROM "
        + sqlIdentifier(tableName) + ";");

      results.execute();

      results.first();
      XRow row = UNO.XRow(results);

      int num = row.getInt(1);
      return num;
    }
    catch (Exception x)
    {
      Logger.error(x);
      return 0;
    }
    finally
    {
      if (results != null) UNO.XComponent(results).dispose();
      if (conn != null) try
      {
        conn.close();
      }
      catch (Exception e)
      {}
    }
  }

  /**
   * Liefert str zurück, als Identifier-Name vorbereitet für das Einfügen in
   * SQL-Statements.
   * 
   * @param str
   *          beginnt und endet immer mit einem Doublequote.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String sqlIdentifier(String str)
  {
    return "\"" + str.replaceAll("\"", "\"\"") + "\"";
  }

  /**
   * Liefert die Anzahl Zeilen in Tabelle tableName von Calc-Dokument calcDoc in
   * denen mindestens eine sichtbare nicht-leere Zelle ist, wobei die erste sichtbare
   * Zeile nicht gezählt wird, weil diese die Spaltennamen beschreibt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   * 
   *         TESTED
   */
  private int getNumberOfDatasets(XSpreadsheetDocument calcDoc, String tableName)
  {
    if (calcDoc != null)
    {
      try
      {
        XCellRangesQuery sheet =
          UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
        SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
        SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
        getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);

        if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
        {
          if (rowIndexes.size() > 1) return rowIndexes.size() - 1;
        }
      }
      catch (Exception x)
      {
        Logger.error(L.m("Kann Anzahl Datensätze nicht bestimmen"), x);
      }
    }

    return 0;
  }

  /**
   * Liefert die Spaltennamen der Tabelle tableName aus der OOo-Datenquelle
   * oooDatasourceName in alphabetischer Reihenfolge. Die Reihenfolge entspricht der
   * von {@link #getDbValuesForDataset(Datasource, int)}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<String> getDbColumnNames(Datasource oooDatasource)
  {
    List<String> columnNames = new Vector<String>();
    columnNames.addAll(oooDatasource.getSchema());
    Collections.sort(columnNames);
    return columnNames;
  }

  /**
   * Liefert die Inhalte (als Strings) der nicht-leeren Zellen der ersten sichtbaren
   * Zeile von Tabellenblatt tableName in Calc-Dokument calcDoc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private List<String> getColumnNames(XSpreadsheetDocument calcDoc, String tableName)
  {
    List<String> columnNames = new Vector<String>();
    if (calcDoc == null) return columnNames;
    try
    {
      XCellRangesQuery sheet =
        UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
      SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
      SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
      getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);

      if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
      {
        XCellRange sheetCellRange = UNO.XCellRange(sheet);

        /*
         * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
         * Tabellenspaltennamen interpretieren.
         */
        int ymin = rowIndexes.first().intValue();
        Iterator<Integer> iter = columnIndexes.iterator();
        while (iter.hasNext())
        {
          int x = iter.next().intValue();
          String columnName =
            UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
          if (columnName.length() > 0)
          {
            columnNames.add(columnName);
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann Spaltennamen nicht bestimmen"), x);
    }
    return columnNames;
  }

  /**
   * Liefert die Daten des rowIndex-ten Datensatzes der Datenquelle oooDatasource
   * (wobei der erste Datensatz die Nummer 1 hat!!!) Falls sich die Daten zwischen
   * den Aufrufen der beiden Methoden nicht geändert haben, passen die
   * zurückgelieferten Daten in Anzahl und Reihenfolge genau zu der von
   * {@link #getDbColumnNames(Datasource)} gelieferten Liste.
   * 
   * Falls rowIndex zu groß ist, wird ein Vektor mit leeren Strings zurückgeliefert.
   * Im Fehlerfall wird ein leerer Vektor zurückgeliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   *         TESTED
   */
  private List<String> getDbValuesForDataset(Datasource oooDatasource, int rowIndex)
  {
    /*
     * Die folgende Implementierung ist nicht schön. Sie hat die folgenden Probleme
     * 
     * o Liest jedes Mal erneut die ganze Tabelle aus => langsam, übermäßige Garbage
     * Produktion
     * 
     * o Kann kein Ergebnis zurückliefern, wenn das Auslesen der gesamten Tabelle
     * nicht innerhalb des Timeouts möglich ist => Vorschau broken bei langsamen
     * Datenquellen
     * 
     * Der große Vorteil dieser Implementierung ist ihre Einfachheit.
     */

    List<String> list = getDbColumnNames(oooDatasource);
    try
    {
      if (rowIndex < 1)
        throw new IllegalArgumentException(L.m("Illegale Datensatznummer: %1",
          rowIndex));
      QueryResults res = oooDatasource.getContents(MAILMERGE_GETCONTENTS_TIMEOUT);
      for (Dataset ds : res)
      {
        if (--rowIndex == 0)
        {
          // Avoid needless obj creation by overwriting col names with return values
          for (int i = 0; i < list.size(); ++i)
          {
            String str;
            try
            {
              str = ds.get(list.get(i));
            }
            catch (ColumnNotFoundException x)
            {
              str = "";
            }
            list.set(i, str);
          }
          return list;
        }
      }

      // Avoid needless object creation by overwriting col names with return values
      for (int i = 0; i < list.size(); ++i)
        list.set(i, "");
      return list;
    }
    catch (Exception x)
    {
      Logger.error(x);
      return new Vector<String>();
    }
  }

  /**
   * Liefert die sichtbaren Inhalte (als Strings) der Zellen aus der rowIndex-ten
   * sichtbaren nicht-leeren Zeile (wobei die erste solche Zeile, diejenige die die
   * Namen for {@link #getColumnNames()} liefert, den Index 0 hat) von Tabellenblatt
   * tableName in Calc-Dokument calcDoc. Falls sich die Daten zwischen den Aufrufen
   * der beiden Methoden nicht geändert haben, passen die zurückgelieferten Daten in
   * Anzahl und Reihenfolge genau zu der von {@link #getColumnNames()} gelieferten
   * Liste.
   * 
   * Falls rowIndex zu groß ist, wird ein Vektor mit leeren Strings zurückgeliefert.
   * Im Fehlerfall wird ein leerer Vektor zurückgeliefert.
   * 
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private List<String> getValuesForDataset(XSpreadsheetDocument calcDoc,
      String tableName, int rowIndex)
  {
    List<String> columnValues = new Vector<String>();
    if (calcDoc == null) return columnValues;
    try
    {
      XCellRangesQuery sheet =
        UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
      SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
      SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
      getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);

      if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
      {
        XCellRange sheetCellRange = UNO.XCellRange(sheet);

        /*
         * Den Zeilenindex in der Tabelle von der rowIndex-ten sichtbaren Zeile
         * bestimmen.
         */
        int yTargetRow = -1;
        int count = rowIndex;
        for (int index : rowIndexes)
        {
          if (--count < 0)
          {
            yTargetRow = index;
            break;
          }
        }

        /*
         * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
         * Tabellenspaltennamen interpretieren (dies ist nötig, damit die
         * zurückgelieferten Werte zu denen von getColumnNames() passen). Wurde ein
         * Tabellenspaltenname identifiziert, so lies den zugehörigen Wert aus der
         * rowIndex-ten Zeile aus.
         */
        int ymin = rowIndexes.first().intValue();
        Iterator<Integer> iter = columnIndexes.iterator();
        while (iter.hasNext())
        {
          int x = iter.next().intValue();
          String columnName =
            UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
          if (columnName.length() > 0)
          {
            if (yTargetRow >= 0)
            {
              String columnValue =
                UNO.XTextRange(sheetCellRange.getCellByPosition(x, yTargetRow)).getString();
              columnValues.add(columnValue);
            }
            else
              columnValues.add("");
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann Spaltenwerte nicht bestimmen"), x);
    }
    return columnValues;
  }

  /**
   * Liefert den Inhalt der Tabelle tableName aus der OOo Datenquelle mit Namen
   * oooDatasourceName.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   *         TESTED
   */
  private QueryResultsWithSchema getDbData(Datasource oooDatasource)
      throws Exception
  {
    Set<String> schema = oooDatasource.getSchema();
    QueryResults res = oooDatasource.getContents(MAILMERGE_GETCONTENTS_TIMEOUT);
    return new QueryResultsWithSchema(res, schema);
  }

  /**
   * Liefert die sichtbaren Zellen aus der Tabelle tableName des Dokuments calcDoc
   * als QueryResultsWithSchema zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private QueryResultsWithSchema getData(XSpreadsheetDocument calcDoc,
      String tableName)
  {
    Set<String> schema = new HashSet<String>();
    QueryResults res = getVisibleCalcData(calcDoc, tableName, schema);
    return new QueryResultsWithSchema(res, schema);
  }

  /**
   * Präsentiert dem Benutzer einen Dialog, in dem er aus allen registrierten
   * Datenbanken eine als Datenquelle auswählen kann. Falls es nur eine registrierte
   * Datenbank gibt, wird diese automatisch gewählt.
   * 
   * @param parent
   *          der JFrame zu dem der die Dialoge gehören sollen.
   * @param callback
   *          wird (im EDT) getriggert, nachdem eine neue OOo Datenquelle als
   *          Datenquelle gesetzt wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void selectOOoDatasourceAsDatasource(final JFrame parent,
      final Runnable callback)
  {
    List<String> names = getRegisteredDatabaseNames();

    if (names.isEmpty()) return;

    if (names.size() == 1)
    {
      getOOoDatasource(names.get(0));
      SwingUtilities.invokeLater(callback);
      selectTable(parent);
      return;
    }

    final JDialog dbSelector = new JDialog(parent, L.m("Datenbank auswählen"), true);

    Box vbox = Box.createVerticalBox();
    dbSelector.add(vbox);

    JLabel label = new JLabel(L.m("Welche Datenbank möchten Sie verwenden ?"));
    vbox.add(label);

    for (int i = 0; i < names.size(); ++i)
    {
      final String name = names.get(i);
      JButton button;
      button = new JButton(name);
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          dbSelector.dispose();
          getOOoDatasource(name);
          SwingUtilities.invokeLater(callback);
          selectTable(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
    }

    dbSelector.pack();
    int frameWidth = dbSelector.getWidth();
    int frameHeight = dbSelector.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dbSelector.setLocation(x, y);
    dbSelector.setResizable(false);
    dbSelector.setVisible(true);
  }

  /**
   * Returns the names of all datasources registered in OOo.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private List<String> getRegisteredDatabaseNames()
  {
    List<String> datasourceNames = new Vector<String>();
    try
    {
      String[] datasourceNamesA = UNO.XNameAccess(UNO.dbContext).getElementNames();
      for (int i = 0; i < datasourceNamesA.length; ++i)
        datasourceNames.add(datasourceNamesA[i]);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    return datasourceNames;
  }

  /**
   * Präsentiert dem Benutzer einen Dialog, in dem er aus allen offenen Calc-Fenstern
   * eines als Datenquelle auswählen kann. Falls es nur ein offenes Calc-Fenster
   * gibt, wird dieses automatisch gewählt.
   * 
   * @param parent
   *          der JFrame zu dem der die Dialoge gehören sollen.
   * @param callback
   *          wird getriggert (im EDT), nachdem eine neue Calc-Tabelle als
   *          Datenquelle gesetzt wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void selectOpenCalcWindowAsDatasource(final JFrame parent,
      final Runnable callback)
  {
    MailMergeDatasource.OpenCalcWindows win = getOpenCalcWindows();
    List<String> names = win.titles;

    if (names.isEmpty()) return;

    if (names.size() == 1)
    {
      getCalcDoc(win.docs.get(0));
      SwingUtilities.invokeLater(callback);
      selectTable(parent);
      return;
    }

    final JDialog calcWinSelector =
      new JDialog(parent, L.m("Tabelle auswählen"), true);

    Box vbox = Box.createVerticalBox();
    calcWinSelector.add(vbox);

    JLabel label = new JLabel(L.m("Welches Calc-Dokument möchten Sie verwenden ?"));
    vbox.add(label);

    for (int i = 0; i < names.size(); ++i)
    {
      final String name = names.get(i);
      final XSpreadsheetDocument spread = win.docs.get(i);
      JButton button;
      button = new JButton(name);
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          calcWinSelector.dispose();
          getCalcDoc(spread);
          SwingUtilities.invokeLater(callback);
          selectTable(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
    }

    calcWinSelector.pack();
    int frameWidth = calcWinSelector.getWidth();
    int frameHeight = calcWinSelector.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    calcWinSelector.setLocation(x, y);
    calcWinSelector.setResizable(false);
    calcWinSelector.setVisible(true);
  }

  /**
   * Öffnet ein neues Calc-Dokument und setzt es als Seriendruckdatenquelle.
   * 
   * @param parent
   *          der JFrame zu dem der die Dialoge gehören sollen.
   * @param wird
   *          (im EDT) ausgeführt, nachdem das Calc-Dokument als Datenquelle gesetzt
   *          wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void openAndselectNewCalcTableAsDatasource(JFrame parent,
      final Runnable callback)
  {
    try
    {
      Logger.debug(L.m("Öffne neues Calc-Dokument als Datenquelle für Seriendruck"));
      XSpreadsheetDocument spread =
        UNO.XSpreadsheetDocument(UNO.loadComponentFromURL("private:factory/scalc",
          true, true));
      XSpreadsheets sheets = spread.getSheets();
      String[] sheetNames = sheets.getElementNames();

      // Lösche alle bis auf das erste Tabellenblatt ohne Änderung des
      // Modified-Status.
      XModifiable xmo = UNO.XModifiable(spread);
      boolean modified = (xmo != null) ? xmo.isModified() : false;
      for (int i = 1; i < sheetNames.length; ++i)
        sheets.removeByName(sheetNames[i]);
      if (xmo != null) xmo.setModified(modified);

      getCalcDoc(spread);
      SwingUtilities.invokeLater(callback);
      selectTable(parent);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Öffnet einen FilePicker und falls der Benutzer dort eine Tabelle auswählt, wird
   * diese geöffnet und als Datenquelle verwendet.
   * 
   * @param parent
   *          der JFrame zu dem der die Dialoge gehören sollen.
   * @param callback
   *          wird getriggert (im EDT), wenn eine neue Calc-Datei als Datenquelle
   *          gesetzt wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void selectFileAsDatasource(JFrame parent, final Runnable callback)
  {
    XFilePicker picker = UNO.XFilePicker(UNO.createUNOService("com.sun.star.ui.dialogs.FilePicker"));
    short res = picker.execute();
    if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
    {
      String[] files = picker.getFiles();
      if (files.length == 0) return;
      try
      {
        Logger.debug(L.m("Öffne %1 als Datenquelle für Seriendruck", files[0]));
        try
        {
          getCalcDoc(files[0]);
          SwingUtilities.invokeLater(callback);
        }
        catch (UnavailableException x)
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
   * nicht-leere Tabelle hat, so wird diese ohne Dialog automatisch ausgewählt. Falls
   * der Benutzer den Dialog abbricht, so wird die erste Tabelle gewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @parent Das Hauptfenster, zu dem dieser Dialog gehört. TESTED
   */
  private void selectTable(JFrame parent)
  {
    List<String> names = getTableNames();
    if (names.isEmpty())
    {
      setTable("");
      return;
    }

    setTable(names.get(0)); // Falls der Benutzer den Dialog abbricht ohne Auswahl

    if (names.size() == 1) return; // Falls es nur eine Tabelle gibt, Dialog
    // unnötig.

    final JDialog tableSelector =
      new JDialog(parent, L.m("Welche Tabelle möchten Sie verwenden ?"), true);

    Box vbox = Box.createVerticalBox();
    tableSelector.add(vbox);

    JLabel label = new JLabel(L.m("Welche Tabelle möchten Sie verwenden ?"));
    vbox.add(label);

    Iterator<String> iter = names.iterator();
    while (iter.hasNext())
    {
      final String name = iter.next();
      JButton button;
      button = new JButton(name);
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          tableSelector.dispose();
          setTable(name);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
    }

    tableSelector.pack();
    int frameWidth = tableSelector.getWidth();
    int frameHeight = tableSelector.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    tableSelector.setLocation(x, y);
    tableSelector.setResizable(false);
    tableSelector.setVisible(true);
  }

  /**
   * Setzt die zu verwendende Tabelle auf den Namen name und speichert die
   * Einstellungen persistent im zugehörigen Dokument ab, damit sie bei der nächsten
   * Bearbeitung des Dokuments wieder verfügbar sind.
   * 
   * @param name
   *          Name der Tabelle die aktuell eingestellt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void setTable(String name)
  {
    if (name == null)
      tableName = "";
    else
      tableName = name;
    oooDatasource = null;
    storeDatasourceSettings();
  }

  /**
   * Registriert {@link #myCalcListener} auf calcDoc, falls calcDoc != null.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListeners(XSpreadsheetDocument calcDoc)
  {
    if (calcDoc == null) return;
    try
    {
      UNO.XCloseBroadcaster(calcDoc).addCloseListener(myCalcListener);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann CloseListener nicht auf Calc-Dokument registrieren"), x);
    }
    try
    {
      UNO.XEventBroadcaster(calcDoc).addEventListener(myCalcListener);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann EventListener nicht auf Calc-Dokument registrieren"), x);
    }
  }

  /**
   * Falls calcDoc != null wird versucht, {@link #myCalcListener} davon zu
   * deregistrieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeListeners(XSpreadsheetDocument calcDoc)
  {
    if (calcDoc == null) return;

    try
    {
      UNO.XCloseBroadcaster(calcDoc).removeCloseListener(myCalcListener);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Konnte alten XCloseListener nicht deregistrieren"), x);
    }
    try
    {
      UNO.XEventBroadcaster(calcDoc).removeEventListener(myCalcListener);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Konnte alten XEventListener nicht deregistrieren"), x);
    }

  }

  // FIXME: Refaktorisieren an sinnvolle Stelle, am besten in UNOHelper
  public static String stripOpenOfficeFromWindowName(String str)
  {
    /*
     * Sonderfall für OpenOffice
     */
    int idx = str.indexOf(" - OpenOffice");
    /* Fallback für andere Office-Varianten */
    if (idx < 0) idx = str.lastIndexOf(" -");
    if (idx > 0) str = str.substring(0, idx);
    return str;
  }

  /**
   * Erzeugt einen Button zur Auswahl der Datenquelle aus den aktuell offenen
   * Calc-Fenstern, dessen Beschriftung davon abhängt, was zur Auswahl steht oder
   * liefert null, wenn nichts zur Auswahl steht. Falls es keine offenen Calc-Fenster
   * gibt, wird null geliefert. Falls es genau ein offenes Calc-Fenster gibt und
   * dieses genau ein nicht-leeres Tabellenblatt hat, so zeigt der Button die
   * Beschriftung "<Fenstername>.<Tabellenname>". Falls es genau ein offenes
   * Calc-Fenster gibt und dieses mehr als ein nicht-leeres oder kein nicht-leeres
   * Tabellenblatt hat, so zeigt der Button die Beschriftung "<Fenstername>". Falls
   * es mehrere offene Calc-Fenster gibt, so zeigt der Button die Beschriftung
   * "Offenes Calc-Fenster...".
   * 
   * @return JButton oder null.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JButton createDatasourceSelectorCalcWindowButton()
  {
    MailMergeDatasource.OpenCalcWindows win = getOpenCalcWindows();

    if (win.titles.isEmpty()) return null;
    if (win.titles.size() > 1) return new JButton(L.m("Offenes Calc-Fenster..."));

    // Es gibt offenbar genau ein offenes Calc-Fenster
    // das XSpreadsheetDocument dazu ist in calcSheet zu finden
    List<String> nonEmptyTableNames = getRelevantTableNames(win.docs.get(0));

    String str = win.titles.get(0);
    if (nonEmptyTableNames.size() == 1) str = str + "." + nonEmptyTableNames.get(0);

    return new JButton(str);
  }

  private static class OpenCalcWindows
  {
    public List<String> titles;

    public List<XSpreadsheetDocument> docs;
  }

  /**
   * Liefert die Titel und zugehörigen XSpreadsheetDocuments aller offenen
   * Calc-Fenster.
   * 
   * @return ein Objekt mit 2 Elementen. Das erste ist eine Liste aller Titel von
   *         Calc-Fenstern, wobei jeder Titel bereits mit
   *         {@link #stripOpenOfficeFromWindowName(String)} bearbeitet wurde. Das
   *         zweite Element ist eine Liste von XSpreadsheetDocuments, wobei jeder
   *         Eintrag zum Fenstertitel mit dem selben Index in der ersten Liste
   *         gehört. Im Fehlerfalle sind beide Listen leer.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private MailMergeDatasource.OpenCalcWindows getOpenCalcWindows()
  {
    MailMergeDatasource.OpenCalcWindows win = new OpenCalcWindows();
    win.titles = new Vector<String>();
    win.docs = new Vector<XSpreadsheetDocument>();
    try
    {
      XSpreadsheetDocument spread = null;
      XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
      while (xenu.hasMoreElements())
      {
        spread = UNO.XSpreadsheetDocument(xenu.nextElement());
        if (spread != null)
        {
          XFrame frame = UNO.XModel(spread).getCurrentController().getFrame();
          if (!Boolean.TRUE.equals(UNO.getProperty(frame, "IsHidden")))
          {
            String title = (String) UNO.getProperty(frame, "Title");
            win.titles.add(stripOpenOfficeFromWindowName(title));
            win.docs.add(spread);
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    return win;
  }

  /**
   * Falls aktuell eine Calc-Tabelle als Datenquelle ausgewählt ist, so wird
   * versucht, diese zurückzuliefern. Falls nötig wird die Datei anhand von
   * {@link #calcUrl} neu geöffnet. Falls es aus irgendeinem Grund nicht möglich ist,
   * diese zurückzuliefern, wird eine
   * {@link de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException} geworfen. ACHTUNG!
   * Das zurückgelieferte Objekt könnte bereits disposed sein!
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private XSpreadsheetDocument getCalcDoc() throws UnavailableException
  {
    if (sourceType != SOURCE_CALC)
      throw new UnavailableException(L.m("Keine Calc-Tabelle ausgewählt"));
    if (calcDoc != null) return calcDoc;
    return getCalcDoc(calcUrl);
  }

  /**
   * Falls url bereits offen ist oder geöffnet werden kann und ein Tabellendokument
   * ist, so wird der {@link #sourceType} auf {@link #SOURCE_CALC} gestellt und die
   * Calc-Tabelle als neue Datenquelle ausgewählt.
   * 
   * @return das Tabellendokument
   * @throws UnavailableException
   *           falls ein Fehler auftritt oder die url kein Tabellendokument
   *           beschreibt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private XSpreadsheetDocument getCalcDoc(String url) throws UnavailableException
  {
    /**
     * Falls schon ein offenes Fenster mit der entsprechenden URL existiert, liefere
     * dieses zurück und setze {@link #calcDoc}.
     */
    XSpreadsheetDocument newCalcDoc = null;
    try
    {
      XSpreadsheetDocument spread;
      XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
      while (xenu.hasMoreElements())
      {
        spread = UNO.XSpreadsheetDocument(xenu.nextElement());
        if (spread != null && url.equals(UNO.XModel(spread).getURL()))
        {
          newCalcDoc = spread;
          break;
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }

    /**
     * Ansonsten versuchen wir das Dokument zu öffnen.
     */
    if (newCalcDoc == null)
    {
      try
      {
        Object ss = UNO.loadComponentFromURL(url, false, true);
        newCalcDoc = UNO.XSpreadsheetDocument(ss);
        if (newCalcDoc == null)
          throw new UnavailableException(L.m("URL \"%1\" ist kein Tabellendokument",
            url));
      }
      catch (Exception x)
      {
        throw new UnavailableException(x);
      }
    }

    getCalcDoc(newCalcDoc);
    return calcDoc;
  }

  /**
   * Setzt newCalcDoc als Datenquelle für den Seriendruck.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void getCalcDoc(XSpreadsheetDocument newCalcDoc)
  {
    try
    {
      calcUrl = UNO.XModel(newCalcDoc).getURL();
    }
    catch (Exception x) // typischerweise DisposedException
    {
      return;
    }
    if (calcUrl.length() == 0) calcUrl = null;
    sourceType = SOURCE_CALC;
    oooDatasourceName = null;
    oooDatasource = null;
    removeListeners(calcDoc); // falls altes calcDoc vorhanden, dort
    // deregistrieren.
    calcDoc = newCalcDoc;
    setListeners(calcDoc);
    storeDatasourceSettings();
  }

  /**
   * Falls aktuell eine OOo-Datenquelle als Datenquelle ausgewählt ist, so wird diese
   * zurückgeliefert. Falls es aus irgendeinem Grund nicht möglich ist, diese
   * zurückzuliefern, wird eine
   * {@link de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException} geworfen.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  private Datasource getOOoDatasource() throws UnavailableException
  {
    if (sourceType != SOURCE_DB)
      throw new UnavailableException(L.m("Keine OOo-Datenquelle ausgewählt"));
    if (oooDatasource != null) return oooDatasource;

    ConfigThingy conf = new ConfigThingy("Datenquelle");
    conf.add("NAME").add("Knuddel");
    conf.add("TABLE").add(tableName);
    conf.add("SOURCE").add(oooDatasourceName);
    try
    {
      oooDatasource =
        new OOoDatasource(new HashMap<String, Datasource>(), conf, new URL(
          "file:///"), true);
    }
    catch (Exception x)
    {
      throw new UnavailableException(x);
    }

    return oooDatasource;
  }

  /**
   * Setzt die registrierte Datenquelle mit Namen newDsName als neue Datenquelle für
   * den Seriendruck.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void getOOoDatasource(String newDsName)
  {
    try
    {
      UNO.XNameAccess(UNO.dbContext).getByName(newDsName);
    }
    catch (DisposedException x)
    {
      return;
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }
    if (newDsName.length() == 0) return;

    sourceType = SOURCE_DB;
    removeListeners(calcDoc); // falls altes calcDoc vorhanden, dort deregistrieren.
    calcDoc = null;

    oooDatasourceName = newDsName;
    oooDatasource = null;
    storeDatasourceSettings();
  }

  /**
   * Liefert den Spaltennamen (z,B, "A") zu Spalte col. Dabei ist Spalte 1 = "A".
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private static String getCalcColumnNameForColumnIndex(int col)
  {
    StringBuilder buffy = new StringBuilder();
    do
    {
      --col;
      buffy.insert(0, (char) ('A' + (col % 26)));
    } while ((col = col / 26) > 0);
    return buffy.toString();
  }

  /**
   * Falls unterstützt (siehe {@link #supportsAddColumns()}), wird für jedes Mapping
   * fieldId->subst eine neue Spalte in die Datenbank eingefügt mit Titel fieldId,
   * deren Inhalt durch subst beschrieben wird. Referenzen auf andere Felder
   * innerhalb von subst werden interpretiert als Referenzen auf die Tabellenspalten
   * mit entsprechenden Titeln.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  public void addColumns(Map<String, FieldSubstitution> mapIdToSubstitution)
  {
    if (sourceType != SOURCE_CALC) return;

    XSpreadsheetDocument calcDoc;
    XCellRangesQuery sheet;
    try
    {
      calcDoc = getCalcDoc();
      sheet = UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
    }
    catch (Exception x)
    {
      return;
    }

    /*
     * Indizes sichtbarer Zellen sowie Mapping von Spaltennamen (basierend auf erster
     * sichtbarer Zeile) auf Calc-Spaltennamen (z.B. "A") erstellen.
     */
    Map<String, String> mapColumnNameToCalcColumnName =
      new HashMap<String, String>();
    SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
    SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
    try
    {
      getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);

      if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
      {
        XCellRange sheetCellRange = UNO.XCellRange(sheet);

        /*
         * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
         * Tabellenspaltennamen interpretieren.
         */
        int ymin = rowIndexes.first().intValue();
        Iterator<Integer> iter = columnIndexes.iterator();
        while (iter.hasNext())
        {
          int x = iter.next().intValue();
          String columnName =
            UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
          if (columnName.length() > 0)
          {
            mapColumnNameToCalcColumnName.put(columnName,
              getCalcColumnNameForColumnIndex(x + 1));
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(L.m("Fehler beim Zugriff auf Calc-Dokument"), x);
      return;
    }

    // Erste neue Spalte hinter die letzte Spalte
    int newColumnX = columnIndexes.last() + 1;

    for (Map.Entry<String, FieldSubstitution> ent : mapIdToSubstitution.entrySet())
    {
      String fieldId = ent.getKey();
      FieldSubstitution subst = ent.getValue();
      StringBuilder formula = new StringBuilder();
      for (FieldSubstitution.SubstElement substEle : subst)
      {
        if (formula.length() == 0)
          formula.append("=CONCATENATE(");
        else
          formula.append(';');
        if (substEle.isFixedText())
        {
          formula.append('"');
          formula.append(substEle.getValue().replaceAll("\"", "\"\""));
          formula.append('"');
        }
        else if (substEle.isField())
        {
          String calcColumnName =
            mapColumnNameToCalcColumnName.get(substEle.getValue());
          if (calcColumnName != null)
          {
            formula.append(calcColumnName);
            formula.append(ROW_NUM_PLACEHOLDER);
          }
          else
          {
            formula.append("\"<");
            formula.append(substEle.getValue().replaceAll("\"", "\"\""));
            formula.append(ROW_NUM_PLACEHOLDER);
            formula.append(">\"");
          }
        }
        else
          formula.append("\"UNKNOWN\"");
      }

      // Wenn Formel leer ist, brauchen wir nichts zu tun
      if (formula.length() == 0) continue;

      formula.append(')');
      String formulaStr = formula.toString();

      try
      {
        XCellRange sheetCellRange = UNO.XCellRange(sheet);

        int ymin = rowIndexes.first();
        int ymax = rowIndexes.last();
        UNO.XTextRange(sheetCellRange.getCellByPosition(newColumnX, ymin)).setString(
          fieldId);

        /*
         * KEINE zusätzlichen Zeilen mit der Formel belegen, weil ansonsten bei
         * Eingabe eines fixen Textes, auch wenn es nur ein Leerzeichen zwischen
         * <Vorname> und <Nachname> ist, dazu führt, dass 1007 zusätzliche Datensätze
         * erkannt werden.
         */
        for (int y = ymin + 1; y <= ymax + 0; ++y)
        {
          UNO.XCell(sheetCellRange.getCellByPosition(newColumnX, y)).setFormula(
            formulaStr.replaceAll(ROW_NUM_PLACEHOLDER, "" + (y + 1)));
        }

        ++newColumnX;

      }
      catch (Exception x)
      {
        Logger.error(L.m("Kann Spalte \"%1\" nicht hinzufügen", fieldId), x);
      }
    }
  }

  /**
   * Liefert die Namen aller relevanten Tabellen der aktuell ausgewählten
   * Datenquelle. Wenn keine Datenquelle ausgewählt ist, oder es keine Tabellen darin
   * gibt, so wird eine leere Liste geliefert. Eine Tabelle in einem Calc-Dokument
   * ist nur relevant, wenn sie nicht leer ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<String> getTableNames()
  {
    try
    {
      switch (sourceType)
      {
        case SOURCE_CALC:
          return getRelevantTableNames(getCalcDoc());
        case SOURCE_DB:
          return getDbTableNames();
        default:
          return new Vector<String>();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return new Vector<String>();
    }
  }

  /**
   * Liefert die Namen aller Tabellen der aktuell ausgewählten OOo-Datenquelle. Wenn
   * keine OOo-Datenquelle ausgewählt ist, oder es keine nicht-leere Tabelle gibt, so
   * wird eine leere Liste geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   *         TESTED
   */
  private List<String> getDbTableNames()
  {
    List<String> tableNames = new Vector<String>();
    if (sourceType == SOURCE_DB && oooDatasourceName != null)
    {
      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(oooDatasourceName));
        long lgto = MAILMERGE_LOGIN_TIMEOUT / 1000;
        if (lgto < 1) lgto = 1;
        ds.setLoginTimeout((int) lgto);
        XConnection conn = ds.getConnection("", "");
        XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
        for (String name : tables.getElementNames())
          tableNames.add(name);
        XNameAccess queries = UNO.XQueriesSupplier(conn).getQueries();
        for (String name : queries.getElementNames())
          tableNames.add(name);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
    return tableNames;
  }

  /**
   * Liefert die Namen aller nicht-leeren Tabellenblätter von calcDoc. Falls calcDoc
   * == null wird eine leere Liste geliefert. Falls alle Tabellen leer sind, wird
   * trotzdem eine Liste mit dem Namen der ersten Tabelle zurückgeliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private List<String> getRelevantTableNames(XSpreadsheetDocument calcDoc)
  {
    List<String> nonEmptyTableNames = new Vector<String>();
    if (calcDoc != null)
      try
      {
        XSpreadsheets sheets = calcDoc.getSheets();
        String[] tableNames = sheets.getElementNames();
        SortedSet<Integer> columns = new TreeSet<Integer>();
        SortedSet<Integer> rows = new TreeSet<Integer>();
        for (int i = 0; i < tableNames.length; ++i)
        {
          try
          {
            XCellRangesQuery sheet =
              UNO.XCellRangesQuery(sheets.getByName(tableNames[i]));
            columns.clear();
            rows.clear();
            getVisibleNonemptyRowsAndColumns(sheet, columns, rows);
            if (columns.size() > 0 && rows.size() > 0)
            {
              nonEmptyTableNames.add(tableNames[i]);
            }
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }

        if (nonEmptyTableNames.isEmpty() && tableNames.length > 0)
          nonEmptyTableNames.add(tableNames[0]);

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    return nonEmptyTableNames;
  }

  private class MyCalcListener implements XCloseListener, XEventListener
  {

    @Override
    public void queryClosing(EventObject arg0, boolean arg1)
        throws CloseVetoException
    {}

    @Override
    public void notifyClosing(EventObject arg0)
    {
      Logger.debug(L.m("Calc-Datenquelle wurde unerwartet geschlossen"));
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          calcDoc = null;
        }
      });
    }

    @Override
    public void disposing(EventObject arg0)
    {
      Logger.debug(L.m("Calc-Datenquelle wurde disposed()"));
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          calcDoc = null;
        }
      });
    }

    @Override
    public void notifyEvent(com.sun.star.document.EventObject event)
    {
      if (event.EventName.equals("OnSaveAsDone")
        && UnoRuntime.areSame(UNO.XInterface(event.Source), calcDoc))
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            calcUrl = UNO.XModel(calcDoc).getURL();
            Logger.debug(L.m("Speicherort der Tabelle hat sich geändert: \"%1\"",
              calcUrl));
            storeDatasourceSettings();
          }
        });
      }
    }
  }

  /**
   * Liefert die sichtbaren Zellen des Arbeitsblattes mit Namen sheetName aus dem
   * Calc Dokument doc. Die erste sichtbare Zeile der Calc-Tabelle wird herangezogen
   * als Spaltennamen. Diese Spaltennamen werden zu schema hinzugefügt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static QueryResults getVisibleCalcData(XSpreadsheetDocument doc,
      String sheetName, Set<String> schema)
  {
    MailMergeDatasource.CalcCellQueryResults results = new CalcCellQueryResults();

    try
    {
      if (doc != null)
      {
        XCellRangesQuery sheet =
          UNO.XCellRangesQuery(doc.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
          SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
          getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);

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
   * Liefert von Tabellenblatt sheet die Indizes aller Zeilen und Spalten, in denen
   * mindestens eine sichtbare nicht-leere Zelle existiert.
   * 
   * @param sheet
   *          das zu scannende Tabellenblatt
   * @param columnIndexes
   *          diesem Set werden die Spaltenindizes hinzugefügt
   * @param rowIndexes
   *          diesem Set werden die Zeilenindizes hinzugefügt
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void getVisibleNonemptyRowsAndColumns(XCellRangesQuery sheet,
      SortedSet<Integer> columnIndexes, SortedSet<Integer> rowIndexes)
  {
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
}
