/*
 * Dateiname: OOoBasedMailMerge.java
 * Projekt  : WollMux
 * Funktion : Seriendruck über den OOo MailMergeService
 * 
 * Copyright (c) 2011 Landeshauptstadt München
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
 * 15.06.2011 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.sun.star.beans.NamedValue;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.sdb.CommandType;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.task.XJob;
import com.sun.star.text.MailMergeEvent;
import com.sun.star.text.MailMergeType;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XMailMergeBroadcaster;
import com.sun.star.text.XMailMergeListener;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XNamingService;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.PersistentData;
import de.muenchen.allg.itd51.wollmux.PersistentDataContainer;
import de.muenchen.allg.itd51.wollmux.PrintModels;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.SimulationResults;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormFieldType;
import de.muenchen.allg.itd51.wollmux.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

public class OOoBasedMailMerge
{
  private static final String SEP = ":";

  private static final String COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION = "WM:SP";

  private static final String COLUMN_PREFIX_CHECKBOX_FUNCTION = "WM:CB";

  private static final String COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION = "WM:MP";

  private static final String TEMP_WOLLMUX_MAILMERGE_PREFIX = "WollMuxMailMerge";

  private static final String DATASOURCE_ODB_FILENAME = "datasource.odb";

  private static final String TABLE_NAME = "data";

  private static final char OPENSYMBOL_CHECKED = 0xE4C4;

  private static final char OPENSYMBOL_UNCHECKED = 0xE470;

  /**
   * Druckfunktion für den Seriendruck in ein Gesamtdokument mit Hilfe des
   * OpenOffice.org-Seriendrucks.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public static void oooMailMerge(final XPrintModel pmod, OutputType type)
  {
    PrintModels.setStage(pmod, L.m("Seriendruck vorbereiten"));

    File tmpDir = createMailMergeTempdir();

    // Datenquelle mit über mailMergeNewSetFormValue simulierten Daten erstellen
    OOoDataSource ds = new CsvBasedOOoDataSource(tmpDir);
    try
    {
      MailMergeNew.mailMergeNewSetFormValue(pmod, ds);
      if (pmod.isCanceled()) return;
      ds.getDataSourceWriter().flushAndClose();
    }
    catch (Exception e)
    {
      Logger.error(
        L.m("OOo-Based-MailMerge: kann Simulationsdatenquelle nicht erzeugen!"), e);
      return;
    }
    if (ds.getSize() == 0)
    {
      WollMuxSingleton.showInfoModal(
        L.m("WollMux-Seriendruck"),
        L.m("Der Seriendruck wurde abgebrochen, da Ihr Druckauftrag keine Datensätze enthält."));
      pmod.cancel();
      return;
    }

    XDocumentDataSource dataSource = ds.createXDocumentDatasource();

    String dbName = registerTempDatasouce(dataSource);

    File inputFile =
      createAndAdjustInputFile(tmpDir, pmod.getTextDocument(), dbName);

    Logger.debug(L.m("Temporäre Datenquelle: %1", dbName));
    if (pmod.isCanceled()) return;

    Thread t = null;
    try
    {
      PrintModels.setStage(pmod, L.m("Gesamtdokument erzeugen"));
      ProgressUpdater updater = new ProgressUpdater(pmod, ds.getSize());
      t = runMailMerge(dbName, tmpDir, inputFile, updater, type);
    }
    catch (Exception e)
    {
      Logger.error(L.m("Fehler beim Starten des OOo-Seriendrucks"), e);
    }

    // Warte auf Ende des MailMerge-Threads unter berücksichtigung von
    // pmod.isCanceled()
    while (t != null && t.isAlive())
      try
      {
        t.join(1000);
        if (pmod.isCanceled()) break;
      }
      catch (InterruptedException e)
      {}
    if (pmod.isCanceled() && t.isAlive())
    {
      t.interrupt();
      Logger.debug(L.m("Der OOo-Seriendruck wurde abgebrochen"));
      return;
    }

    removeTempDatasource(dbName, tmpDir);
    ds.remove();
    inputFile.delete();
    if (pmod.isCanceled()) return;

    if (type == OutputType.toFile)
    {
      // Output-File als Template öffnen und aufräumen
      File outputFile = new File(tmpDir, "output0.odt");
      if (outputFile.exists())
        try
        {
          String unoURL =
            UNO.getParsedUNOUrl(outputFile.toURI().toString()).Complete;
          Logger.debug(L.m("Öffne erzeugtes Gesamtdokument %1", unoURL));
          UNO.loadComponentFromURL(unoURL, true, false);
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      else
      {
        WollMuxSingleton.showInfoModal(L.m("WollMux-Seriendruck"),
          L.m("Leider konnte kein Gesamtdokument erstellt werden."));
        pmod.cancel();
      }
      outputFile.delete();
    }

    tmpDir.delete();
  }

  /**
   * Übernimmt das Aktualisieren der Fortschrittsanzeige im XPrintModel pmod.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class ProgressUpdater
  {
    private XPrintModel pmod;

    private int currentCount;

    public ProgressUpdater(XPrintModel pmod, int maxDatasets)
    {
      this.pmod = pmod;
      this.currentCount = 0;
      pmod.setPrintProgressMaxValue((short) maxDatasets);
      pmod.setPrintProgressValue((short) 0);
    }

    public void incrementProgress()
    {
      pmod.setPrintProgressValue((short) ++currentCount);
    }
  }

  /**
   * Repräsentiert eine (noch nicht registrierte) Datenquelle für OpenOffice.org.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static abstract class OOoDataSource implements SimulationResultsProcessor
  {
    /**
     * Liefert das für die Registrierung der OOo-Datenquelle benötigte
     * {@link XDocumentDataSource}-Objekt zurück.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    abstract public XDocumentDataSource createXDocumentDatasource();

    /**
     * Liefert einen {@link DataSourceWriter} zurück, über den Datensätze in die
     * Datenquelle geschrieben werden können.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    abstract public DataSourceWriter getDataSourceWriter();

    /**
     * Liefert die Anzahl der Datensätze der Datenquelle zurück.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    abstract public int getSize();

    /**
     * Entfernt die Datenquelle
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    abstract public void remove();

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.SimulationResults.SimulationResultsProcessor
     * #processSimulationResults(de.muenchen.allg.itd51.wollmux.SimulationResults)
     */
    public void processSimulationResults(SimulationResults simRes)
    {
      if (simRes == null) return;

      HashMap<String, String> data =
        new HashMap<String, String>(simRes.getFormFieldValues());
      for (FormField field : simRes.getFormFields())
      {
        String columnName = getSpecialColumnNameForFormField(field);
        if (columnName == null) continue;
        String content = simRes.getFormFieldContent(field);

        // Checkboxen müssen über bestimmte Zeichen der Schriftart OpenSymbol
        // angenähert werden.
        if (field.getType() == FormFieldType.CheckBoxFormField)
          if (content.equalsIgnoreCase("TRUE"))
            content = "" + OPENSYMBOL_CHECKED;
          else
            content = "" + OPENSYMBOL_UNCHECKED;

        data.put(columnName, content);
      }
      try
      {
        getDataSourceWriter().addDataset(data);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Implementierung einer {@link OOoDataSource}, die als Backend ein CSV-Datei
   * verwendet.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class CsvBasedOOoDataSource extends OOoDataSource
  {
    File parentDir;

    CSVDataSourceWriter dsw;

    /**
     * Erzeugt eine {@link OOoDataSource}, die als Backend eine CSV-Datei verwendet
     * und die dafür notwendige Datei (eine .csv-Datei) im Verzeichnis parentDir
     * ablegt.
     */
    public CsvBasedOOoDataSource(File parentDir)
    {
      this.parentDir = parentDir;
      this.dsw = new CSVDataSourceWriter(parentDir);
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#
     * getDataSourceWriter()
     */
    public DataSourceWriter getDataSourceWriter()
    {
      return dsw;
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#
     * createXDocumentDatasource()
     */
    public XDocumentDataSource createXDocumentDatasource()
    {
      XSingleServiceFactory dbContext =
        UNO.XSingleServiceFactory(UNO.createUNOService("com.sun.star.sdb.DatabaseContext"));
      XDocumentDataSource dataSource = null;
      if (dbContext != null) try
      {
        dataSource = UNO.XDocumentDataSource(dbContext.createInstance());
      }
      catch (Exception e)
      {
        Logger.error(e);
      }

      if (dataSource != null)
      {
        String dirURL = UNO.getParsedUNOUrl(parentDir.toURI().toString()).Complete;
        UNO.setProperty(dataSource, "URL", "sdbc:flat:" + dirURL);

        UnoProps p = new UnoProps();
        p.setPropertyValue("Extension", "csv");
        p.setPropertyValue("CharSet", "UTF-8");
        p.setPropertyValue("FixedLength", false);
        p.setPropertyValue("HeaderLine", true);
        p.setPropertyValue("FieldDelimiter", ",");
        p.setPropertyValue("StringDelimiter", "\"");
        p.setPropertyValue("DecimalDelimiter", ".");
        p.setPropertyValue("ThousandDelimiter", "");
        UNO.setProperty(dataSource, "Info", p.getProps());

        XStorable xStorable = UNO.XStorable(dataSource.getDatabaseDocument());
        XModel model = UNO.XModel(xStorable);
        URL url = null;
        File tmpFile = new File(parentDir, DATASOURCE_ODB_FILENAME);
        url = UNO.getParsedUNOUrl(tmpFile.toURI().toString());
        if (url != null && xStorable != null && model != null) try
        {
          xStorable.storeAsURL(url.Complete, model.getArgs());
        }
        catch (IOException e)
        {
          Logger.error(e);
        }
      }
      return dataSource;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#getSize()
     */
    public int getSize()
    {
      return dsw.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#remove()
     */
    public void remove()
    {
      dsw.getCSVFile().delete();
    }
  }

  /**
   * Beschreibt einen DataSourceWriter mit dem die Daten des Seriendrucks in eine
   * Datenquelle geschrieben werden können. Eine konkrete Ableitungen ist der
   * {@link CSVDataSourceWriter}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static interface DataSourceWriter
  {
    /**
     * Fügt der zu erzeugenden Datenquelle einen neuen Datensatz hinzu durch
     * Schlüssel/Wert-Paare in einer HashMap definiert ist.
     * 
     * @throws Exception
     *           falls etwas beim Hinzufügen schief geht.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void addDataset(HashMap<String, String> ds) throws Exception;

    /**
     * Nachdem mit {@link #addDataset(HashMap)} alle Datensätze hinzugefügt wurden
     * schließt der Aufruf dieser Methode die Erzeugung der Datenquelle ab. Nach dem
     * Aufruf von {@link #flushAndClose()} ist die Erzeugung abgeschlossen und es
     * darf kein weiterer Aufruf von {@link #addDataset(HashMap)} erfolgen (bzw.
     * dieser ist dann ohne Wirkung).
     * 
     * @throws Exception
     *           falls etwas beim Finalisieren schief geht.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void flushAndClose() throws Exception;

    /**
     * Liefert die Anzahl der (bisher) mit {@link #addDataset(HashMap)} hinzugefügten
     * Datensätze zurück.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public int getSize();
  }

  /**
   * Implementiert einen DataSourceWriter, der Daten in eine CSV-Datei data.csv in
   * einem frei wählbaren Zielverzeichnis schreibt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class CSVDataSourceWriter implements DataSourceWriter
  {
    /**
     * Enthält die zu erzeugende bzw. erzeugte csv-Datei.
     */
    File csvFile = null;

    /**
     * Sammelt alle über {@link #addDataset(HashMap)} gesetzten Datensätze
     */
    ArrayList<HashMap<String, String>> datasets;

    /**
     * Sammelt die Namen aller über {@link #addDataset(HashMap)} gesetzten Spalten.
     */
    HashSet<String> columns;

    /**
     * Enthält nach einem Aufruf von {@link #getHeaders()} die sortierten Headers.
     */
    ArrayList<String> headers = null;

    /**
     * Erzeugt einen CSVDataSourceWriter, der die zu erzeugende csv-Datei in
     * parentDir ablegt.
     */
    public CSVDataSourceWriter(File parentDir)
    {
      csvFile = new File(parentDir, TABLE_NAME + ".csv");
      datasets = new ArrayList<HashMap<String, String>>();
      columns = new HashSet<String>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#getSize
     * ()
     */
    public int getSize()
    {
      return datasets.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#addDataset
     * (java.util.HashMap)
     */
    public void addDataset(HashMap<String, String> ds) throws Exception
    {
      datasets.add(ds);
      columns.addAll(ds.keySet());
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#
     * flushAndClose()
     */
    public void flushAndClose() throws Exception
    {
      PrintWriter p = new PrintWriter(csvFile);
      p.print(line(getHeaders()));
      for (HashMap<String, String> ds : datasets)
      {
        ArrayList<String> entries = new ArrayList<String>();
        for (String key : getHeaders())
        {
          String val = ds.get(key);
          if (val == null) val = "";
          entries.add(val);
        }
        p.print(line(entries));
      }
      p.close();
    }

    /**
     * Erzeugt die zu dem durch list repräsentierten Datensatz zugehörige
     * vollständige Textzeile für die csv-Datei.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private String line(List<String> list)
    {
      StringBuffer buf = new StringBuffer();
      for (String el : list)
      {
        if (buf.length() != 0) buf.append(",");
        buf.append(literal(el));
      }
      buf.append("\n");
      return buf.toString();
    }

    /**
     * Erzeugt ein für die csv-Datei gültiges literal aus dem Wert value und
     * übernimmt insbesondere das Escaping der Anführungszeichen.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private String literal(String value)
    {
      String esc = value.replaceAll("\"", "\"\"");
      return "\"" + esc + "\"";
    }

    /**
     * Liefert eine alphabetisch sortierte Liste alle Spaltennamen zurück, die jemals
     * über {@link #addDataset(HashMap)} benutzt bzw. gesetzt wurden.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private ArrayList<String> getHeaders()
    {
      if (headers != null) return headers;
      headers = new ArrayList<String>(columns);
      Collections.sort(headers);
      return headers;
    }

    /**
     * Liefert das File-Objekt der csv-Datei zurück, in die geschrieben wird/wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public File getCSVFile()
    {
      return csvFile;
    }
  }

  /**
   * Erzeugt das aus origDoc abgeleitete, für den OOo-Seriendruck heranzuziehende
   * Input-Dokument im Verzeichnis tmpDir und nimmt alle notwendigen Anpassungen vor,
   * damit der Seriendruck über die temporäre Datenbank dbName korrekt und möglichst
   * performant funktioniert, und liefert dieses zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static File createAndAdjustInputFile(File tmpDir, XTextDocument origDoc,
      String dbName)
  {
    // Aktuelles Dokument speichern als neues input-Dokument
    if (origDoc == null) return null;
    File inputFile = new File(tmpDir, "input.odt");
    String url = UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete;
    XStorable xStorable = UNO.XStorable(origDoc);
    if (xStorable != null)
    {
      try
      {
        xStorable.storeToURL(url, new PropertyValue[] {});
      }
      catch (IOException e)
      {
        Logger.error(
          L.m("Kann temporäres Eingabedokument für den OOo-Seriendruck nicht erzeugen"),
          e);
        return null;
      }
    }
    else
    {
      return null;
    }

    // Neues input-Dokument öffnen. Achtung: Normalerweise würde der
    // loadComponentFromURL den WollMux veranlassen, das Dokument zu interpretieren
    // (und damit zu verarbeiten). Da das bei diesem temporären Dokument nicht
    // erwünscht ist, erkennt der WollMux in
    // d.m.a.i.wollmux.event.GlobalEventListener.isTempMailMergeDocument(XModel
    // compo) über den Pfad der Datei dass es sich um ein temporäres Dokument handelt
    // und dieses nicht bearbeitet werden soll.
    XComponent tmpDoc = null;
    try
    {
      tmpDoc = UNO.loadComponentFromURL(url, false, false, true);
    }
    catch (Exception e)
    {
      return null;
    }

    // neues input-Dokument bearbeiten/anpassen
    addDatabaseFieldsForInsertFormValueBookmarks(UNO.XTextDocument(tmpDoc), dbName);
    adjustDatabaseAndInputUserFields(tmpDoc, dbName);
    removeAllBookmarks(tmpDoc);
    removeHiddenSections(tmpDoc);
    SachleitendeVerfuegung.deMuxSLVStyles(UNO.XTextDocument(tmpDoc));
    removeWollMuxMetadata(UNO.XTextDocument(tmpDoc));

    // neues input-Dokument speichern und schließen
    if (UNO.XStorable(tmpDoc) != null)
    {
      try
      {
        UNO.XStorable(tmpDoc).store();
      }
      catch (IOException e)
      {
        inputFile = null;
      }
    }
    else
    {
      inputFile = null;
    }

    boolean closed = false;
    if (UNO.XCloseable(tmpDoc) != null) do
    {
      try
      {
        UNO.XCloseable(tmpDoc).close(true);
        closed = true;
      }
      catch (CloseVetoException e)
      {
        try
        {
          Thread.sleep(2000);
        }
        catch (InterruptedException e1)
        {}
      }
    } while (closed == false);

    return inputFile;
  }

  /**
   * Entfernt alle Metadaten des WollMux aus dem Dokument doc die nicht reine
   * Infodaten des WollMux sind (wie z.B. WollMuxVersion, OOoVersion) um
   * sicherzustellen, dass der WollMux das Gesamtdokument nicht interpretiert.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static void removeWollMuxMetadata(XTextDocument doc)
  {
    if (doc == null) return;
    PersistentDataContainer c = PersistentData.createPersistentDataContainer(doc);
    for (DataID dataId : DataID.values())
      if (!dataId.isInfodata()) c.removeData(dataId);
    c.flush();
  }

  /**
   * Hebt alle unsichtbaren TextSections (Bereiche) in Dokument tmpDoc auf, wobei bei
   * auch der Inhalt entfernt wird. Das Entfernen der unsichtbaren Bereiche dient zur
   * Verbesserung der Performance, das Löschen der Bereichsinhalte ist notwendig,
   * damit das erzeugte Gesamtdokument korrekt dargestellt wird (hier habe ich wilde
   * Textverschiebungen beobachtet, die so vermieden werden sollen).
   * 
   * Bereiche sind auch ein möglicher Auslöser von allen möglichen falsch gesetzten
   * Seitenumbrüchen (siehe z.B. Issue:
   * http://openoffice.org/bugzilla/show_bug.cgi?id=73229)
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void removeHiddenSections(XComponent tmpDoc)
  {
    XTextSectionsSupplier tss = UNO.XTextSectionsSupplier(tmpDoc);
    if (tss == null) return;

    for (String name : tss.getTextSections().getElementNames())
    {
      try
      {
        XTextSection section =
          UNO.XTextSection(tss.getTextSections().getByName(name));
        if (Boolean.FALSE.equals(UNO.getProperty(section, "IsVisible")))
        {
          // Inhalt der Section löschen und Section aufheben:
          section.getAnchor().setString("");
          section.getAnchor().getText().removeTextContent(section);
        }
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Aufgrund eines Bugs in OOo führen Bookmarks zu einer Verlangsamung des
   * Seriendruck in der Komplexität O(n^2) und werden hier in dieser Methode alle aus
   * dem Dokument tmpDoc gelöscht. Bookmarks sollten im Ergebnisdokument sowieso
   * nicht mehr benötigt werden und sind damit aus meiner Sicht überflüssig.
   * 
   * Sollte irgendjemand irgendwann zu der Meinung kommen, dass die Bookmarks im
   * Dokument bleiben müssen, so müssen zumindest die Bookmarks von
   * WollMux-Dokumentkommandos gelöscht werden, damit sie nicht noch einmal durch den
   * WollMux bearbeitet werden.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void removeAllBookmarks(XComponent tmpDoc)
  {
    if (UNO.XBookmarksSupplier(tmpDoc) != null)
    {
      XNameAccess xna = UNO.XBookmarksSupplier(tmpDoc).getBookmarks();
      for (String name : xna.getElementNames())
      {
        XTextContent bookmark = null;
        try
        {
          bookmark = UNO.XTextContent(xna.getByName(name));
        }
        catch (NoSuchElementException e)
        {
          continue;
        }
        catch (Exception e)
        {
          Logger.error(e);
        }

        if (bookmark != null) try
        {
          bookmark.getAnchor().getText().removeTextContent(bookmark);
        }
        catch (NoSuchElementException e1)
        {
          Logger.error(e1);
        }
      }
    }
  }

  /**
   * Fügt dem Dokument doc für alle enthaltenen insertFormValue-Bookmarks zugehörige
   * OOo-Seriendruckfelder mit Verweis auf die Datenbank dbName hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static void addDatabaseFieldsForInsertFormValueBookmarks(
      XTextDocument doc, String dbName)
  {
    DocumentCommands cmds = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    cmds.update();
    HashMap<String, FormField> bookmarkNameToFormField =
      new HashMap<String, FormField>();
    for (DocumentCommand cmd : cmds)
    {
      if (cmd instanceof InsertFormValue)
      {
        InsertFormValue ifvCmd = (InsertFormValue) cmd;
        FormField field =
          FormFieldFactory.createFormField(doc, ifvCmd, bookmarkNameToFormField);
        if (field == null) continue;
        field.setCommand(ifvCmd);

        String columnName = getSpecialColumnNameForFormField(field);
        if (columnName == null) columnName = ifvCmd.getID();
        try
        {
          XDependentTextField dbField =
            createDatabaseField(UNO.XMultiServiceFactory(doc), dbName, TABLE_NAME,
              columnName);
          if (dbField == null) continue;

          ifvCmd.insertTextContentIntoBookmark(dbField, true);

          // Checkboxen müssen über bestimmte Zeichen der Schriftart OpenSymbol
          // angenähert werden.
          if (field.getType() == FormFieldType.CheckBoxFormField)
            UNO.setProperty(ifvCmd.getTextCursor(), "CharFontName", "OpenSymbol");
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
    }
  }

  /**
   * Liefert zum Formularfeld field unter Berücksichtigung des Feld-Typs und evtl.
   * gesetzter Trafos eine eindeutige Bezeichnung für die Datenbankspalte in die der
   * Wert des Formularfeldes geschrieben ist bzw. aus der der Wert des Formularfeldes
   * wieder ausgelesen werden kann oder null, wenn das Formularfeld über einen
   * primitiven Spaltennamen (der nur aus einer in den setValues gesetzten IDs
   * besteht) gefüllt werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static String getSpecialColumnNameForFormField(FormField field)
  {
    String trafo = field.getTrafoName();
    String id = field.getId();

    if (field.getType() == FormFieldType.CheckBoxFormField && id != null
      && trafo != null)
      return COLUMN_PREFIX_CHECKBOX_FUNCTION + SEP + id + SEP + trafo;

    else if (field.getType() == FormFieldType.CheckBoxFormField && id != null
      && trafo == null)
      return COLUMN_PREFIX_CHECKBOX_FUNCTION + SEP + id;

    else if (field.singleParameterTrafo() && id != null && trafo != null)
      return COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION + SEP + id + SEP + trafo;

    else if (!field.singleParameterTrafo() && trafo != null)
      return COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION + SEP + trafo;

    return null;
  }

  /**
   * Passt bereits enthaltene OOo-Seriendruckfelder und Nächster-Datensatz-Felder in
   * tmpDoc so an, dass sie über die Datenbank dbName befüllt werden und ersetzt
   * InputUser-Felder durch entsprechende OOo-Seriendruckfelder.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static void adjustDatabaseAndInputUserFields(XComponent tmpDoc,
      String dbName)
  {
    if (UNO.XTextFieldsSupplier(tmpDoc) != null)
    {
      XEnumeration xenum =
        UNO.XTextFieldsSupplier(tmpDoc).getTextFields().createEnumeration();
      while (xenum.hasMoreElements())
      {
        XDependentTextField tf = null;
        try
        {
          tf = UNO.XDependentTextField(xenum.nextElement());
        }
        catch (Exception e)
        {
          continue;
        }

        // Database-Felder anpassen auf temporäre Datenquelle/Tabelle
        if (UNO.supportsService(tf, "com.sun.star.text.TextField.Database"))
        {
          XPropertySet master = tf.getTextFieldMaster();
          UNO.setProperty(master, "DataBaseName", dbName);
          UNO.setProperty(master, "DataTableName", TABLE_NAME);
        }

        // "Nächster Datensatz"-Felder anpassen auf temporäre Datenquelle/Tabelle
        if (UNO.supportsService(tf, "com.sun.star.text.TextField.DatabaseNextSet"))
        {
          UNO.setProperty(tf, "DataBaseName", dbName);
          UNO.setProperty(tf, "DataTableName", TABLE_NAME);
        }

        // InputUser-Felder ersetzen durch entsprechende Database-Felder
        else if (UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
        {
          String content = "";
          try
          {
            content = AnyConverter.toString(UNO.getProperty(tf, "Content"));
          }
          catch (IllegalArgumentException e)
          {}

          String trafo = TextDocumentModel.getFunctionNameForUserFieldName(content);
          if (trafo != null)
          {
            try
            {
              XDependentTextField dbField =
                createDatabaseField(UNO.XMultiServiceFactory(tmpDoc), dbName,
                  TABLE_NAME, COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION + SEP + trafo);
              tf.getAnchor().getText().insertTextContent(tf.getAnchor(), dbField,
                true);
            }
            catch (Exception e)
            {
              Logger.error(e);
            }
          }
        }
      }
    }
  }

  /**
   * Erzeugt über die Factory factory ein neues OOo-Seriendruckfeld, das auf die
   * Datenbank dbName, die Tabelle tableName und die Spalte columnName verweist und
   * liefert dieses zurück.
   * 
   * @throws Exception
   *           Wenn die Factory das Feld nicht erzeugen kann.
   * @throws IllegalArgumentException
   *           Wenn irgendetwas mit den Attributen dbName, tableName oder columnName
   *           nicht stimmt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static XDependentTextField createDatabaseField(
      XMultiServiceFactory factory, String dbName, String tableName,
      String columnName) throws Exception, IllegalArgumentException
  {
    XDependentTextField dbField =
      UNO.XDependentTextField(factory.createInstance("com.sun.star.text.TextField.Database"));
    XPropertySet m =
      UNO.XPropertySet(factory.createInstance("com.sun.star.text.FieldMaster.Database"));
    UNO.setProperty(m, "DataBaseName", dbName);
    UNO.setProperty(m, "DataTableName", tableName);
    UNO.setProperty(m, "DataColumnName", columnName);
    dbField.attachTextFieldMaster(m);
    return dbField;
  }

  /**
   * Deregistriert die Datenbank dbName aus der Liste der Datenbanken (wie z.B. über
   * Extras->Optionen->Base/Datenbanken einsehbar) und löscht das zugehörige in
   * tmpDir enthaltene .odb-File von der Platte.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void removeTempDatasource(String dbName, File tmpDir)
  {
    XSingleServiceFactory dbContext =
      UNO.XSingleServiceFactory(UNO.createUNOService("com.sun.star.sdb.DatabaseContext"));
    XNamingService naming = UNO.XNamingService(dbContext);
    if (naming != null) try
    {
      naming.revokeObject(dbName);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    new File(tmpDir, DATASOURCE_ODB_FILENAME).delete();
  }

  /**
   * Registriert die {@link XDocumentDataSource} dataSource mit einem neuen
   * Zufallsnamen in OOo (so, dass sie z.B. in der Liste der Datenbanken unter
   * Tools->Extras->Optionen->Base/Datenbanken auftaucht) und gibt den Zufallsnamen
   * zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String registerTempDatasouce(XDocumentDataSource dataSource)
  {
    // neuen Zufallsnamen für Datenquelle bestimmen
    XSingleServiceFactory dbContext =
      UNO.XSingleServiceFactory(UNO.createUNOService("com.sun.star.sdb.DatabaseContext"));
    String name = null;
    XNameAccess nameAccess = UNO.XNameAccess(dbContext);
    if (nameAccess != null) do
    {
      name = TEMP_WOLLMUX_MAILMERGE_PREFIX + new Random().nextInt(100000);
    } while (nameAccess.hasByName(name));

    // Datenquelle registrieren
    if (name != null && UNO.XNamingService(dbContext) != null) try
    {
      UNO.XNamingService(dbContext).registerObject(name, dataSource);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    return name;
  }

  /**
   * Steuert den Ausgabetyp beim OOo-Seriendruck.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static enum OutputType {
    toFile,
    toPrinter;
  }

  /**
   * Startet die Ausführung des Seriendrucks in ein Gesamtdokument mit dem
   * c.s.s.text.MailMergeService in einem eigenen Thread und liefert diesen zurück.
   * 
   * @param dbName
   *          Name der Datenbank, die für den Seriendruck verwendet werden soll.
   * @param outputDir
   *          Directory in dem das Ergebnisdokument abgelegt werden soll.
   * @param inputFile
   *          Hauptdokument, das für den Seriendruck herangezogen wird.
   * @param progress
   *          Ein ProgressUpdater, der über den Bearbeitungsfortschritt informiert
   *          wird.
   * @throws Exception
   *           falls der MailMergeService nicht erzeugt werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static Thread runMailMerge(String dbName, final File outputDir,
      File inputFile, final ProgressUpdater progress, OutputType type)
      throws Exception
  {
    final XJob mailMerge =
      (XJob) UnoRuntime.queryInterface(XJob.class,
        UNO.xMCF.createInstanceWithContext("com.sun.star.text.MailMerge",
          UNO.defaultContext));

    // Register MailMergeEventListener
    XMailMergeBroadcaster xmmb =
      (XMailMergeBroadcaster) UnoRuntime.queryInterface(XMailMergeBroadcaster.class,
        mailMerge);
    xmmb.addMailMergeEventListener(new XMailMergeListener()
    {
      int count = 0;

      final long start = System.currentTimeMillis();

      public void notifyMailMergeEvent(MailMergeEvent arg0)
      {
        if (progress != null) progress.incrementProgress();
        count++;
        Logger.debug2(L.m("OOo-MailMerger: verarbeite Datensatz %1 (%2 ms)", count,
          (System.currentTimeMillis() - start)));
      }
    });

    final ArrayList<NamedValue> mmProps = new ArrayList<NamedValue>();
    mmProps.add(new NamedValue("DataSourceName", dbName));
    mmProps.add(new NamedValue("CommandType", CommandType.TABLE));
    mmProps.add(new NamedValue("Command", TABLE_NAME));
    mmProps.add(new NamedValue("DocumentURL",
      UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete));
    mmProps.add(new NamedValue("OutputURL",
      UNO.getParsedUNOUrl(outputDir.toURI().toString()).Complete));
    if (type == OutputType.toFile)
    {
      mmProps.add(new NamedValue("SaveAsSingleFile", Boolean.TRUE));
      mmProps.add(new NamedValue("OutputType", MailMergeType.FILE));
      mmProps.add(new NamedValue("FileNameFromColumn", Boolean.FALSE));
      mmProps.add(new NamedValue("FileNamePrefix", "output"));
    }
    else if (type == OutputType.toPrinter)
    {
      mmProps.add(new NamedValue("OutputType", MailMergeType.PRINTER));
      mmProps.add(new NamedValue("SinglePrintJobs", Boolean.FALSE));
    }
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          Logger.debug(L.m("Starting OOo-MailMerge in Verzeichnis %1", outputDir));
          mailMerge.execute(mmProps.toArray(new NamedValue[mmProps.size()]));
          Logger.debug(L.m("Finished Mail Merge"));
        }
        catch (Exception e)
        {
          Logger.debug(L.m("OOo-MailMergeService fehlgeschlagen: %1", e.getMessage()));
        }
      }
    });
    t.start();
    return t;
  }

  /**
   * Erzeugt ein neues temporäres Directory mit dem Aufbau
   * "<TEMP_WOLLMUX_MAILMERGE_PREFIX>xxx" (wobei xxx eine garantiert 3-stellige Zahl
   * ist), in dem sämtliche (temporäre) Dateien für den Seriendruck abgelegt werden
   * und liefert dieses zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static File createMailMergeTempdir()
  {
    File sysTmp = new File(System.getProperty("java.io.tmpdir"));
    File tmpDir;
    do
    {
      // +100 um eine 3-stellige Zufallszahl zu garantieren
      tmpDir =
        new File(sysTmp, TEMP_WOLLMUX_MAILMERGE_PREFIX
          + (new Random().nextInt(899) + 100));
    } while (!tmpDir.mkdir());
    return tmpDir;
  }

  /**
   * Testmethode
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void main(String[] args)
  {
    try
    {
      UNO.init();

      File tmpDir = createMailMergeTempdir();

      OOoDataSource ds = new CsvBasedOOoDataSource(tmpDir);
      XDocumentDataSource dataSource = ds.createXDocumentDatasource();

      String dbName = registerTempDatasouce(dataSource);

      File inputFile =
        createAndAdjustInputFile(tmpDir,
          UNO.XTextDocument(UNO.desktop.getCurrentComponent()), dbName);

      System.out.println("Temporäre Datenquelle: " + dbName);

      runMailMerge(dbName, tmpDir, inputFile, null, OutputType.toFile);

      removeTempDatasource(dbName, tmpDir);

      inputFile.delete();

      // Output-File als Template öffnen und aufräumen
      File outputFile = new File(tmpDir, "output0.odt");
      UNO.loadComponentFromURL(
        UNO.getParsedUNOUrl(outputFile.toURI().toString()).Complete, true, false);
      outputFile.delete();
      tmpDir.delete();

    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
}
