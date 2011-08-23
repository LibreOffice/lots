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
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XNamingService;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SimulationResults;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;

public class OOoBasedMailMerge
{
  private static final String COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION = "WM:SP:";

  private static final String COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION = "WM:MP:";

  private static final String OOoMAIL_MERGE_TMP_DIR = "OOoBasedMailMerge_tmpDir";

  private static final String OOoMAIL_MERGE_DATA_SOURCE =
    "OOoBasedMailMerge_OOoDataSource";

  private static final String TEMP_WOLLMUX_MAILMERGE_PREFIX = "WollMuxMailMerge";

  private static final String DATASOURCE_ODB_FILENAME = "datasource.odb";

  private static final String TABLE_NAME = "data";

  /**
   * TODO: Beschreiben... PrintFunction, die das jeweils nächste Element der
   * Seriendruckdaten nimmt und die Seriendruckfelder im Dokument entsprechend setzt.
   * Siehe {@link MailMergeNew#mailMergeNewSetFormValue(XPrintModel)}.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  public static void oooMailMergeToSingleFile(final XPrintModel pmod)
  {
    File tmpDir = null;
    try
    {
      tmpDir = (File) pmod.getPropertyValue(OOoMAIL_MERGE_TMP_DIR);
    }
    catch (Exception e)
    {}
    if (tmpDir == null)
    {
      tmpDir = createMailMergeTempdir();
      try
      {
        pmod.setPropertyValue(OOoMAIL_MERGE_TMP_DIR, tmpDir);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    OOoDataSource ds = null;
    try
    {
      ds = (OOoDataSource) pmod.getPropertyValue(OOoMAIL_MERGE_DATA_SOURCE);
    }
    catch (Exception e)
    {}
    if (ds == null)
    {
      ds = new CsvBasedOOoDataSource(tmpDir);
      try
      {
        pmod.setPropertyValue(OOoMAIL_MERGE_DATA_SOURCE, ds);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    SimulationResults sim = null;
    try
    {
      sim =
        (SimulationResults) pmod.getPropertyValue(MailMergeNew.PROP_SIMULATION_RESULTS);
    }
    catch (Exception e)
    {}

    if (sim != null && ds != null)
    {
      HashMap<String, String> data =
        new HashMap<String, String>(sim.getFormFieldValues());
      for (FormField f : sim.getFormFields())
      {
        String content = sim.getFormFieldContent(f);
        String trafo = f.getTrafoName();
        if (trafo == null) continue;
        String name =
          (f.singleParameterTrafo() ? COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION
                                   : COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION)
            + f.getTrafoName();
        data.put(name, content);
      }
      try
      {
        ds.getDataSourceWriter().addDataset(data);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    try
    {
      if (!Boolean.TRUE.equals(pmod.getPropertyValue(MailMergeNew.PROP_LAST_PRINT)))
        return;
    }
    catch (Exception e)
    {
      return;
    }

    if (ds != null) try
    {
      ds.getDataSourceWriter().flushAndClose();
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    XDocumentDataSource dataSource = ds.createXDocumentDatasource();

    String dbName = registerTempDatasouce(dataSource);

    File inputFile =
      createAndAdjustInputFile(tmpDir, pmod.getTextDocument(), dbName);

    Logger.debug(L.m("Temporäre Datenquelle: %1", dbName));

    try
    {
      runMailMerge(dbName, tmpDir, inputFile);
    }
    catch (IllegalArgumentException e)
    {
      // FIXME: warum mache ich hier nix?
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    removeTempDatasource(dbName, tmpDir);
    inputFile.delete();

    // Output-File als Template öffnen und aufräumen
    File outputFile = new File(tmpDir, "output0.odt");
    try
    {
      String unoURL = UNO.getParsedUNOUrl(outputFile.toURI().toString()).Complete;
      Logger.debug(L.m("Öffne erzeugtes Gesamtdokument %1", unoURL));
      UNO.loadComponentFromURL(unoURL, true, false);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    outputFile.delete();
    tmpDir.delete();
  }

  public static interface OOoDataSource
  {
    /**
     * TODO: comment OOoDataSource.createDatasource
     * 
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public XDocumentDataSource createXDocumentDatasource();

    /**
     * TODO: comment OOoDataSource.getDataSourceWriter
     * 
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public DataSourceWriter getDataSourceWriter();
  }

  /**
   * TODO: comment OOoBasedMailMerge
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class CsvBasedOOoDataSource implements OOoDataSource
  {
    File parentDir;

    CSVDataSourceWriter dsw;

    /**
     * @param parentDir
     */
    public CsvBasedOOoDataSource(File parentDir)
    {
      this.parentDir = parentDir;
      this.dsw = new CSVDataSourceWriter(parentDir);
    }

    @Override
    public DataSourceWriter getDataSourceWriter()
    {
      return dsw;
    }

    /**
     * TODO: comment OOoBasedMailMerge.createDatasource
     * 
     * @param tmpDir
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
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
  }

  /**
   * TODO: comment OOoBasedMailMerge
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static interface DataSourceWriter
  {
    public void addDataset(HashMap<String, String> ds) throws Exception;

    public void flushAndClose() throws Exception;
  }

  /**
   * TODO: comment OOoBasedMailMerge
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class CSVDataSourceWriter implements DataSourceWriter
  {
    File csvFile = null;

    ArrayList<HashMap<String, String>> datasets;

    HashSet<String> columns;

    ArrayList<String> headers = null;

    /**
     * @param parentDir
     */
    public CSVDataSourceWriter(File parentDir)
    {
      csvFile = new File(parentDir, TABLE_NAME + ".csv");
      datasets = new ArrayList<HashMap<String, String>>();
      columns = new HashSet<String>();
    }

    @Override
    public void addDataset(HashMap<String, String> ds) throws Exception
    {
      datasets.add(ds);
      columns.addAll(ds.keySet());
    }

    @Override
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
     * TODO: comment CSVDataSourceWriter.line
     * 
     * @param list
     * @return
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
     * TODO: comment CSVDataSourceWriter.literal
     * 
     * @param col
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private String literal(String col)
    {
      String esc = col.replaceAll("\"", "\"\"");
      return "\"" + esc + "\"";
    }

    /**
     * TODO: comment CSVDataSourceWriter.getHeaders
     * 
     * @return
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
     * TODO: comment CSVDataSourceWriter.getCSVFile
     * 
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public File getCSVFile()
    {
      return csvFile;
    }
  }

  /**
   * TODO: comment OOoBasedMailMerge.main
   * 
   * @param args
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

      runMailMerge(dbName, tmpDir, inputFile);

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

  /**
   * TODO: comment OOoBasedMailMerge.createAndAdjustInputFile
   * 
   * @param tmpDir
   * @param origDoc
   * @param dbName
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static File createAndAdjustInputFile(File tmpDir, XTextDocument origDoc,
      String dbName)
  {
    if (origDoc == null) return null;
    File inputFile = new File(tmpDir, "input.odt");
    String url = UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete;
    XStorable xStorable = UNO.XStorable(UNO.desktop.getCurrentComponent());
    if (xStorable != null)
    {
      try
      {
        xStorable.storeToURL(url, new PropertyValue[] {});
      }
      catch (IOException e)
      {
        return null;
      }
    }
    else
    {
      return null;
    }

    XComponent tmpDoc = null;
    try
    {
      tmpDoc = UNO.loadComponentFromURL(url, false, false, true);
    }
    catch (Exception e)
    {
      return null;
    }

    // insertFormValue-Bookmark anpassen
    if (UNO.XBookmarksSupplier(tmpDoc) != null)
    {
      XNameAccess xna = UNO.XBookmarksSupplier(tmpDoc).getBookmarks();
      for (String name : xna.getElementNames())
      {
        String docCmd = TextDocumentModel.getDocumentCommandByBookmarkName(name);
        if (docCmd != null)
        {
          String functionName =
            TextDocumentModel.getFunctionNameForDocumentCommand(docCmd);
          String id = TextDocumentModel.getFormIDForDocumentCommand(docCmd);
          try
          {
            XDependentTextField dbField = null;
            if (functionName != null)
              dbField =
                createDatabaseField(UNO.XMultiServiceFactory(tmpDoc), dbName,
                  TABLE_NAME, COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION + functionName);
            else if (id != null)
              dbField =
                createDatabaseField(UNO.XMultiServiceFactory(tmpDoc), dbName,
                  TABLE_NAME, id);

            if (dbField != null)
            {
              XTextContent bookmark = UNO.XTextContent(xna.getByName(name));
              bookmark.getAnchor().getText().insertTextContent(bookmark.getAnchor(),
                dbField, true);
            }
          }
          catch (Exception e)
          {
            Logger.error(e);
          }
        }
      }
    }

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

          String functionName =
            TextDocumentModel.getFunctionNameForUserFieldName(content);
          if (functionName != null)
          {
            try
            {
              XDependentTextField dbField =
                createDatabaseField(UNO.XMultiServiceFactory(tmpDoc), dbName,
                  TABLE_NAME, COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION + functionName);
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
   * TODO: comment OOoBasedMailMerge.createDatabaseField
   * 
   * @param factory
   * @param dbName
   * @param tableName
   * @param columnName
   * @return
   * @throws Exception
   * @throws IllegalArgumentException
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
   * TODO: comment OOoBasedMailMerge.removeTempDatasource
   * 
   * @param dbName
   * @param tmpDir
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
   * TODO: comment OOoBasedMailMerge.registerTempDatasouce
   * 
   * @param dataSource
   * @return
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
   * TODO: comment OOoBasedMailMerge.runMailMerge
   * 
   * @param dbName
   * @param tmpDir
   * @param inputFile
   * @throws Exception
   * @throws IllegalArgumentException
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void runMailMerge(String dbName, File tmpDir, File inputFile)
      throws Exception, IllegalArgumentException
  {
    XJob mailMerge =
      UnoRuntime.queryInterface(XJob.class, UNO.xMCF.createInstanceWithContext(
        "com.sun.star.text.MailMerge", UNO.defaultContext));

    // Register MailMergeEventListener
    XMailMergeBroadcaster xmmb =
      UnoRuntime.queryInterface(XMailMergeBroadcaster.class, mailMerge);
    xmmb.addMailMergeEventListener(new XMailMergeListener()
    {
      int count = 0;

      final long start = System.currentTimeMillis();

      public void notifyMailMergeEvent(MailMergeEvent arg0)
      {
        count++;
        Logger.debug2(L.m("OOo-MailMerger: verarbeite Datensatz %1 (%2 ms)", count,
          (System.currentTimeMillis() - start)));
      }
    });

    ArrayList<NamedValue> mmProps = new ArrayList<NamedValue>();
    mmProps.add(new NamedValue("DataSourceName", dbName));
    mmProps.add(new NamedValue("CommandType", CommandType.TABLE));
    mmProps.add(new NamedValue("Command", TABLE_NAME));
    mmProps.add(new NamedValue("DocumentURL",
      UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete));
    mmProps.add(new NamedValue("SaveAsSingleFile", Boolean.TRUE));
    mmProps.add(new NamedValue("OutputType", MailMergeType.FILE));
    mmProps.add(new NamedValue("FileNameFromColumn", Boolean.FALSE));
    mmProps.add(new NamedValue("FileNamePrefix", "output"));
    mmProps.add(new NamedValue("OutputURL",
      UNO.getParsedUNOUrl(tmpDir.toURI().toString()).Complete));
    Logger.debug(L.m("Starting Mail Merge in tmpDir %1", tmpDir));
    mailMerge.execute(mmProps.toArray(new NamedValue[mmProps.size()]));
    Logger.debug(L.m("Finished Mail Merge"));
  }

  /**
   * TODO: comment OOoBasedMailMerge.createMailMergeTempdir
   * 
   * @return
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
}
