/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.PrintException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.NamedValue;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
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
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XNamingService;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.URL;
import com.sun.star.util.XCancellable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory.FormFieldType;
import de.muenchen.allg.itd51.wollmux.document.PersistentDataContainer;
import de.muenchen.allg.itd51.wollmux.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.document.SimulationResults;
import de.muenchen.allg.itd51.wollmux.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Performs a mail merge with LibreOffice.
 */
public class OOoBasedMailMerge implements AutoCloseable
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OOoBasedMailMerge.class);

  private static final String SEP = ":";

  private static final String COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION = "WM:SP";

  private static final String COLUMN_PREFIX_CHECKBOX_FUNCTION = "WM:CB";

  private static final String COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION = "WM:MP";

  public static final String COLUMN_PREFIX_TEXTSECTION = "WM:SE_";

  private static final String TEMP_WOLLMUX_MAILMERGE_PREFIX = "WollMuxMailMerge";

  public static final String DATASOURCE_ODB_FILENAME = "datasource.odb";

  private static final String TABLE_NAME = "data";

  private XPrintModel pmod;
  private CSVDataSourceWriter ds;
  private File tmpDir;
  boolean loadPrintSettings = false;
  private File inputFile;
  private String dbName;
  private short type;

  /**
   * Create a mail merge based on LibreOffice.
   *
   * @param pmod
   *          The print model.
   * @param type
   *          One of {@link MailMergeType}.
   * @throws PrintException
   *           Some preconditions for the mail merge can't be ensured.
   */
  public OOoBasedMailMerge(final XPrintModel pmod, final short type) throws PrintException
  {
    this.pmod = pmod;
    this.type = type;
    PrintModels.setStage(pmod, L.m("Seriendruck vorbereiten"));

    createMailMergeTempdir();
    prepareDatasource();
    modifyLoadPrinterSetting(true);
    registerTempDatasouce();
    LOGGER.debug("Temporäre Datenquelle: {}", dbName);

    createAndAdjustInputFile();
  }

  @Override
  public void close() throws Exception
  {
    modifyLoadPrinterSetting(loadPrintSettings);
    unregisterTempDatasource();
    FileUtils.deleteDirectory(tmpDir);
  }

  /**
   * Start the mail merge.
   * 
   * @throws PrintException
   *           Something went wrong.
   */
  public void doMailMerge() throws PrintException
  {
    if (pmod.isCanceled())
    {
      return;
    }
    PrintModels.setStage(pmod, L.m("Gesamtdokument erzeugen"));

    try
    {
      final XJob mailMerge = createMailMergeJob();

      final ArrayList<NamedValue> mmProps = new ArrayList<>();
      mmProps.add(new NamedValue(UnoProperty.DATA_SOURCE_NAME, dbName));
      mmProps.add(new NamedValue(UnoProperty.COMMAND_TYPE, CommandType.TABLE));
      mmProps.add(new NamedValue(UnoProperty.COMMAND, TABLE_NAME));
      mmProps.add(new NamedValue(
          UnoProperty.DOCUMENT_URL,
          UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete));
      mmProps.add(new NamedValue(
          UnoProperty.OUTPUT_URL,
          UNO.getParsedUNOUrl(tmpDir.toURI().toString()).Complete));
      mmProps.add(new NamedValue(UnoProperty.OUTPUT_TYPE, type));
      if (type == MailMergeType.FILE)
      {
        mmProps.add(new NamedValue(UnoProperty.SAVE_AS_SINGLE_FILE, Boolean.TRUE));
        mmProps.add(new NamedValue(UnoProperty.FILE_NAME_FROM_COLUMN, Boolean.FALSE));
        mmProps.add(new NamedValue(UnoProperty.FILE_NAME_PREFIX, "output"));
      } else if (type == MailMergeType.SHELL)
      {
        mmProps.add(new NamedValue(UnoProperty.SAVE_AS_SINGLE_FILE, Boolean.TRUE));
        mmProps.add(new NamedValue(UnoProperty.FILE_NAME_FROM_COLUMN, Boolean.FALSE));
      } else if (type == MailMergeType.PRINTER)
      {
        mmProps.add(new NamedValue(UnoProperty.SINGLE_PRINT_JOBS, Boolean.FALSE));
      }

      LOGGER.debug("Starting OOo-MailMerge in Verzeichnis {}", tmpDir);

      Object result = mailMerge.execute(mmProps.toArray(new NamedValue[mmProps.size()]));

      // continue if not canceled or returned with error
      if (!pmod.isCanceled())
      {

        if (type == MailMergeType.FILE)
        {
          handleFileResult(new File(tmpDir, "output0.odt"));
        } else if (type == MailMergeType.SHELL)
        {
          pmod.setPropertyValue(PrintFunction.PRINT_RESULT, result);
          pmod.printWithProps();
        }
      }
    } catch (Exception e)
    {
      throw new PrintException("OOo-MailMergeService fehlgeschlagen:", e);
    }
  }

  // open file as template and delete afterwards
  private void handleFileResult(File outputFile)
  {
    if (outputFile.exists())
    {
      try
      {
        String unoURL = UNO.getParsedUNOUrl(outputFile.toURI().toString()).Complete;
        LOGGER.debug("Öffne erzeugtes Gesamtdokument {}", unoURL);
        UNO.loadComponentFromURL(unoURL, true, false);
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    } else
    {
      InfoDialog.showInfoModal(L.m("WollMux-Seriendruck"),
          L.m("Leider konnte kein Gesamtdokument erstellt werden."));
      pmod.cancel();
    }
    try
    {
      Files.delete(outputFile.toPath());
    } catch (java.io.IOException ex)
    {
      LOGGER.error("Couldn't delete file", ex);
    }
  }

  private void prepareDatasource() throws PrintException
  {
    try
    {
      // Datenquelle mit über mailMergeNewSetFormValue simulierten Daten
      // erstellen
      ds = new CSVDataSourceWriter();
      SetFormValue.mailMergeNewSetFormValue(pmod, ds);
      ds.flushAndClose();
    } catch (java.io.IOException ex)
    {
      throw new PrintException(
          L.m("OOo-Based-MailMerge: kann Simulationsdatenquelle nicht erzeugen!"), ex);
    }
    if (ds.getSize() == 0)
    {
      throw new PrintException(
          "Der Seriendruck wurde abgebrochen, da Ihr Druckauftrag keine Datensätze enthält.");
    }
  }

  /**
   * Modifies the configuration option for loading print properties when files are opened.
   *
   * @param load
   *          True if the properties should be loaded, false otherwise. @return The value before the
   *          modification.
   */
  private void modifyLoadPrinterSetting(boolean load)
  {
    try
    {
      loadPrintSettings = AnyConverter.toBoolean(
          UnoConfiguration.getConfiguration("/org.openoffice.Office.Common/Save/Document", UnoProperty.LOAD_PRINTER));
      UnoConfiguration.setConfiguration("/org.openoffice.Office.Common/Save/Document",
          new UnoProps(UnoProperty.LOAD_PRINTER, load));
    } catch (UnoHelperException e1)
    {
      LOGGER.warn("Die Option 'Laden von Druckeinstellungen mit dem Dokument' konnte nicht gesetzt werden.\n"
          + "Seriendrucke auf einem Drucker haben eventuell falsche Optionen gesetzt.");
      LOGGER.debug("", e1);
    }
  }

  /**
   * Create and prepare the input document for the mail merge form the print model.
   * 
   * @throws PrintException
   *           The input document can't be created or prepared.
   */
  private void createAndAdjustInputFile() throws PrintException
  {
    inputFile = new File(tmpDir, "input.odt");
    String url = UNO.getParsedUNOUrl(inputFile.toURI().toString()).Complete;
    XStorable xStorable = UNO.XStorable(pmod.getTextDocument());
    if (xStorable != null)
    {
      try
      {
        xStorable.storeToURL(url, new PropertyValue[] {});
      } catch (IOException e)
      {
        throw new PrintException("Temporäres Dokument konnte nicht angelegt werden", e);
      }

      // Workaround for #16487
      try
      {
        Thread.sleep(1000);
      } catch (InterruptedException e2)
      {
        LOGGER.error("", e2);
        Thread.currentThread().interrupt();
      }

      try
      {
        /*
         * Open new document. It shouldn't be handled by WollMux because it's a temporary document.
         * Check LibreOfficeEventHandler.
         */
        XTextDocument tmpDoc = UNO.XTextDocument(UNO.loadComponentFromURL(url, false, false, true));
        if (UNO.XStorable(tmpDoc) == null)
        {
          UNO.XCloseable(tmpDoc).close(true);
          throw new PrintException("Probleme mit dem temporären Dokument");
        }

        // prepare input document
        addDatabaseFieldsForInsertFormValueBookmarks(tmpDoc);
        updateTextSections(tmpDoc);
        adjustDatabaseAndInputUserFields(tmpDoc);
        /*
         * Bookmarks make LO mail merge slow. So we delete all of the.
         *
         * If at some time we need bookmarks at least WollMux document commands have to be removed
         * so that they are not processed twice.
         */
        removeAllBookmarks(tmpDoc);
        ContentBasedDirectiveModel.createModel(UNO.XTextDocument(tmpDoc)).renameTextStyles();
        removeWollMuxMetadata(UNO.XTextDocument(tmpDoc));

        UNO.XStorable(tmpDoc).store();
        UNO.XCloseable(tmpDoc).close(true);
      } catch (UnoHelperException | CloseVetoException | IOException e)
      {
        throw new PrintException("Probleme mit dem temporären Dokument", e);
      }
    }
  }

  /**
   * Prepare text sections in the document for the mail merge.
   * 
   * @param doc
   *          The document.
   */
  private void updateTextSections(XTextDocument doc)
  {
    XTextSectionsSupplier tssupp = UNO.XTextSectionsSupplier(doc);
    UnoDictionary<XTextSection> textSections = UnoDictionary.create(tssupp.getTextSections(), XTextSection.class);

    Pattern groupPattern = Pattern.compile(".* GROUPS(?:\\s\"(.*)\"|\\((.*)\\)\n?)");

    for (Entry<String, XTextSection> section : textSections.entrySet())
    {
      Matcher matcher = groupPattern.matcher(section.getKey());
      if (matcher.matches())
      {
        String res = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(2);
        String groups = res.replaceAll("\"", "");
        String[] groupNames = groups.split("\\s*,\\s*");

        try
        {
          XTextRange range = section.getValue().getAnchor();
          UnoProperty.setPropertyToDefault(range, UnoProperty.CHAR_HIDDEN);

          List<String> conditions = new ArrayList<>();
          for (String groupName : groupNames)
          {
            conditions
                .add(String.format("([%s] != \"true\")", COLUMN_PREFIX_TEXTSECTION + groupName));
          }

          String condition = StringUtils.join(conditions, " or ");
          UnoProperty.setProperty(section.getValue(), UnoProperty.IS_VISIBLE, false);
          UnoProperty.setProperty(section.getValue(), UnoProperty.CONDITION, condition);
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }
  }

  /**
   * Remove all non informational meta data of wollmux from the document.
   * 
   * @param doc
   *          The document.
   */
  private void removeWollMuxMetadata(XTextDocument doc)
  {
    if (doc == null)
      return;
    PersistentDataContainer c = DocumentManager.createPersistentDataContainer(doc);
    for (DataID dataId : DataID.values())
      if (!dataId.isInfodata())
        c.removeData(dataId);
    c.flush();
  }

  /**
   * Remove all bookmarks from the document.
   * 
   * @param tmpDoc
   *          The document.
   */
  private void removeAllBookmarks(XTextDocument tmpDoc)
  {
    if (UNO.XBookmarksSupplier(tmpDoc) != null)
    {
      UnoDictionary<XTextContent> bookmarks = UnoDictionary
          .create(UNO.XBookmarksSupplier(tmpDoc).getBookmarks(), XTextContent.class);
      for (XTextContent bookmark : bookmarks.values())
      {
        try
        {
          if (bookmark != null)
          {
            bookmark.getAnchor().getText().removeTextContent(bookmark);
          }
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }
  }

  /**
   * Replace all insertFormValue-Bookmarks with mail merge fields.
   * 
   * @param doc
   *          The document which contains the bookmarks.
   */
  private void addDatabaseFieldsForInsertFormValueBookmarks(XTextDocument doc)
  {
    DocumentCommands cmds = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    cmds.update();
    HashMap<String, FormField> bookmarkNameToFormField = new HashMap<>();
    for (DocumentCommand cmd : cmds)
    {
      if (cmd instanceof InsertFormValue)
      {
        InsertFormValue ifvCmd = (InsertFormValue) cmd;
        FormField field = FormFieldFactory.createFormField(doc, ifvCmd, bookmarkNameToFormField);
        if (field == null)
          continue;

        String columnName = getSpecialColumnNameForFormField(field);
        if (columnName == null)
          columnName = ifvCmd.getID();
        try
        {
          XDependentTextField dbField = createDatabaseField(UNO.XMultiServiceFactory(doc),
              TABLE_NAME, columnName);

          ifvCmd.insertTextContentIntoBookmark(dbField, true);

          // approximate checkboxes with chars of font 'OpenSymbol'
          if (field.getType() == FormFieldType.CHECKBOX_FORM_FIELD)
            UnoProperty.setProperty(ifvCmd.getTextCursor(), UnoProperty.CHAR_FONT_NAME, "OpenSymbol");
        } catch (PrintException | UnoHelperException e)
        {
          LOGGER.error("", e);
        }
      }
    }
  }

  /**
   * Create a column name for a {@link FormField}.
   * 
   * @param field
   *          The {@link FormField}.
   * @return A column name created from the type and possible Trafos of the {@link FormField}. Can
   *         be null if it the field has no Id or Trafo.
   */
  private String getSpecialColumnNameForFormField(FormField field)
  {
    String trafo = field.getTrafoName();
    String id = field.getId();

    if (field.getType() == FormFieldType.CHECKBOX_FORM_FIELD && id != null && trafo != null)
      return COLUMN_PREFIX_CHECKBOX_FUNCTION + SEP + id + SEP + trafo;

    else if (field.getType() == FormFieldType.CHECKBOX_FORM_FIELD && id != null && trafo == null)
      return COLUMN_PREFIX_CHECKBOX_FUNCTION + SEP + id;

    else if (field.singleParameterTrafo() && id != null && trafo != null)
      return COLUMN_PREFIX_SINGLE_PARAMETER_FUNCTION + SEP + id + SEP + trafo;

    else if (!field.singleParameterTrafo() && trafo != null)
      return COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION + SEP + trafo;

    return null;
  }

  /**
   * Adjust mail merge fields and InputUserFields to use the new database {@link #dbName}.
   * 
   * @param tmpDoc
   *          The document which contains the fields.
   */
  private void adjustDatabaseAndInputUserFields(XTextDocument tmpDoc)
  {
    if (UNO.XTextFieldsSupplier(tmpDoc) != null)
    {
      UnoCollection<XTextField> textFields = UnoCollection
          .getCollection(UNO.XTextFieldsSupplier(tmpDoc).getTextFields(), XTextField.class);
      for (XTextField field : textFields)
      {
        XDependentTextField tf = null;
        try
        {
          tf = UNO.XDependentTextField(field);
        } catch (Exception e)
        {
          continue;
        }

        // update database fields
        if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_DATABASE))
        {
          XPropertySet master = tf.getTextFieldMaster();
          Utils.setProperty(master, UnoProperty.DATA_BASE_NAME, dbName);
          Utils.setProperty(master, UnoProperty.DATA_TABLE_NAME, TABLE_NAME);
        }

        // update next record fields
        if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_DATABASE_NEXT_SET))
        {
          Utils.setProperty(tf, UnoProperty.DATA_BASE_NAME, dbName);
          Utils.setProperty(tf, UnoProperty.DATA_TABLE_NAME, TABLE_NAME);
        }

        // update user intpu fields
        if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_INPUT_USER))
        {
          adjustInputUserFields(tmpDoc, tf);
        }
      }
    }
  }

  /**
   * Adjust a InputUserField in the document.
   * 
   * @param tmpDoc
   *          The document.
   * @param tf
   *          The field.
   */
  private void adjustInputUserFields(XTextDocument tmpDoc, XDependentTextField tf)
  {
    String content = "";
    try
    {
      content = AnyConverter.toString(Utils.getProperty(tf, UnoProperty.CONTENT));
    } catch (IllegalArgumentException e)
    {
      // we assume that there's no content
    }

    String trafo = TextDocumentModel.getFunctionNameForUserFieldName(content);
    if (trafo != null)
    {
      try
      {
        XDependentTextField dbField = createDatabaseField(UNO.XMultiServiceFactory(tmpDoc),
            TABLE_NAME, COLUMN_PREFIX_MULTI_PARAMETER_FUNCTION + SEP + trafo);
        tf.getAnchor().getText().insertTextContent(tf.getAnchor(), dbField, true);
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Count the number of "Next record" fields in the document.
   * 
   * @return The number of fields.
   */
  private int countNextSets()
  {
    int numberOfNextSets = 1;
    if (UNO.XTextFieldsSupplier(pmod.getTextDocument()) != null)
    {
      UnoCollection<XTextField> textFields = UnoCollection
          .getCollection(UNO.XTextFieldsSupplier(pmod.getTextDocument()).getTextFields(), XTextField.class);
      for (XTextField field : textFields)
      {
        XDependentTextField tf = null;
        try
        {
          tf = UNO.XDependentTextField(field);
        } catch (Exception e)
        {
          continue;
        }

        if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_DATABASE_NEXT_SET))
        {
          numberOfNextSets++;
        }
      }
    }
    return numberOfNextSets;
  }

  /**
   * Create a new mail merge field using the database {@link #dbName}
   * 
   * @param factory
   *          The factory to create the field.
   * @param tableName
   *          The table of the database.
   * @param columnName
   *          The column in the table to use.
   * @return A mail merge field.
   * @throws PrintException
   *           The Field can't be created.
   */
  private XDependentTextField createDatabaseField(XMultiServiceFactory factory, String tableName,
      String columnName) throws PrintException
  {
    try
    {
      XDependentTextField dbField = UNO
          .XDependentTextField(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_DATABASE, factory));
      XPropertySet m = UNO.XPropertySet(UnoService.createService(UnoService.CSS_TEXT_FIELD_MASTER_DATABASE, factory));
      UnoProperty.setProperty(m, UnoProperty.DATA_BASE_NAME, dbName);
      UnoProperty.setProperty(m, UnoProperty.DATA_TABLE_NAME, tableName);
      UnoProperty.setProperty(m, UnoProperty.DATA_COLUMN_NAME, columnName);
      dbField.attachTextFieldMaster(m);
      return dbField;
    } catch (UnoHelperException ex)
    {
      throw new PrintException(ex);
    }
  }

  /**
   * Unregister the database {@link #dbName} and delete the corresponding file.
   */
  private void unregisterTempDatasource()
  {
    XSingleServiceFactory dbContext = UNO.XSingleServiceFactory(UNO.dbContext);
    XNamingService naming = UNO.XNamingService(dbContext);
    if (naming != null)
    {
      try
      {
        naming.revokeObject(dbName);
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Register a database with a new random name.
   * 
   * @throws PrintException
   *           The database can't be registered.
   */
  private void registerTempDatasouce() throws PrintException
  {
    XDocumentDataSource dataSource = ds.createXDocumentDatasource();
    // neuen Zufallsnamen für Datenquelle bestimmen
    UnoDictionary<Object> names = UnoDictionary.create(UNO.dbContext, Object.class);
    do
    {
      dbName = TEMP_WOLLMUX_MAILMERGE_PREFIX + new Random().nextInt(100000);
    } while (names.containsKey(dbName));

    try
    {
      UNO.dbContext.registerObject(dbName, dataSource);
    } catch (Exception e)
    {
      throw new PrintException("Die Datenbank konnte nicht registriert werden.", e);
    }
  }

  /**
   * Create the mail merge job.
   * 
   * @return The job.
   * @throws PrintException
   *           The job can't be created.
   */
  private XJob createMailMergeJob() throws PrintException
  {
    try
    {
      final XJob mailMerge = UnoRuntime.queryInterface(XJob.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.text.MailMerge", UNO.defaultContext));
      int maxDatasets = (int) Math.ceil((double) ds.getSize() / countNextSets());
      pmod.setPrintProgressMaxValue((short) maxDatasets);
      pmod.setPrintProgressValue((short) 0);

      XCancellable mailMergeCancellable = UNO.XCancellable(mailMerge);

      // Register MailMergeEventListener
      XMailMergeBroadcaster xmmb = UnoRuntime.queryInterface(XMailMergeBroadcaster.class,
          mailMerge);
      xmmb.addMailMergeEventListener(new XMailMergeListener()
      {
        int count = 0;

        final long start = System.currentTimeMillis();

        @Override
        public void notifyMailMergeEvent(MailMergeEvent event)
        {
          if (mailMergeCancellable != null && pmod.isCanceled())
          {
            mailMergeCancellable.cancel();
          }

          pmod.setPrintProgressValue((short) ++count);
          LOGGER.trace(L.m("OOo-MailMerger: verarbeite Datensatz %1 (%2 ms)", count,
              (System.currentTimeMillis() - start)));
          if (count >= maxDatasets && type == MailMergeType.PRINTER)
          {
            count = 0;
            pmod.setPrintMessage(L.m("Sende Druckauftrag - bitte warten..."));
          }
        }
      });
      return mailMerge;
    } catch (com.sun.star.uno.Exception ex)
    {
      throw new PrintException("Seriendruck konnte nicht ausgeführt werden", ex);
    }
  }

  /**
   * Creates a folder in the system temporary folder with name
   * '{@link #TEMP_WOLLMUX_MAILMERGE_PREFIX}xxx', where xxx is a 3 digit number.
   * 
   * @return The folder.
   */
  private void createMailMergeTempdir()
  {
    try
    {
      tmpDir = Files.createTempDirectory(TEMP_WOLLMUX_MAILMERGE_PREFIX).toFile();
    } catch (java.io.IOException e)
    {
      LOGGER.error("Can't create directory.", e);
      pmod.cancel();
      InfoDialog.showInfoModal(L.m("WollMux-Seriendruck"),
          "Es konnte kein Ordner für den Seriendruck erstellt werden.");
    }
  }

  /**
   * Class for writing records in a CSV-file.
   */
  private class CSVDataSourceWriter implements SimulationResultsProcessor
  {

    private static final char OPENSYMBOL_CHECKED = 0xE4C4;

    private static final char OPENSYMBOL_UNCHECKED = 0xE470;

    /**
     * The CSV-file.
     */
    File csvFile = new File(tmpDir, TABLE_NAME + ".csv");

    /**
     * Collection of all records.
     */
    ArrayList<Map<String, String>> records = new ArrayList<>();

    /**
     * Collection of all columns contained in {@link #records}.
     */
    HashSet<String> columns = new HashSet<>();

    /**
     * Get the number of available records.
     * 
     * @return Number of records.
     */
    public int getSize()
    {
      return records.size();
    }

    /**
     * Add a new record.
     * 
     * @param ds
     *          Key/Value pair defining a record.
     */
    public void addRecord(Map<String, String> ds)
    {
      records.add(ds);
      columns.addAll(ds.keySet());
    }

    /**
     * After all records where added with {@link #addRecord(Map)}. This method writes the file.
     * Later calls of {@link #addRecord(Map)} have no impact.
     * 
     * @throws java.io.IOException
     *           Data can't be written.
     */
    public void flushAndClose() throws java.io.IOException
    {
      try (FileOutputStream fos = new FileOutputStream(csvFile);
          PrintStream p = new PrintStream(fos, true, StandardCharsets.UTF_8))
      {
        List<String> headers = new ArrayList<>(columns);
        Collections.sort(headers);
        p.print(line(headers));
        for (Map<String, String> record : records)
        {
          ArrayList<String> entries = new ArrayList<>();
          for (String key : headers)
          {
            String val = record.get(key);
            if (val == null)
              val = "";
            entries.add(val);
          }
          p.print(line(entries));
        }
      }
    }

    @Override
    public void processSimulationResults(SimulationResults simRes)
    {
      if (simRes == null)
      {
        return;
      }

      HashMap<String, String> data = new HashMap<>(simRes.getFormFieldValues());
      for (FormField field : simRes.getFormFields())
      {
        String columnName = getSpecialColumnNameForFormField(field);
        if (columnName == null)
          continue;
        String content = simRes.getFormFieldContent(field);

        // checkboxes with chars of font 'OpenSymbol'
        if (field.getType() == FormFieldType.CHECKBOX_FORM_FIELD)
        {
          content = content.equalsIgnoreCase("TRUE") ? "" + OPENSYMBOL_CHECKED
              : "" + OPENSYMBOL_UNCHECKED;
        }

        data.put(columnName, content);
      }

      for (Map.Entry<String, Boolean> entry : simRes.getGroupsVisibilityState().entrySet())
      {
        OOoBasedMailMerge.LOGGER.info("{} --> {}", entry.getKey(), entry.getValue());
        data.put(OOoBasedMailMerge.COLUMN_PREFIX_TEXTSECTION + entry.getKey(),
            entry.getValue().toString());
      }

      try
      {
        addRecord(data);
      } catch (Exception e)
      {
        OOoBasedMailMerge.LOGGER.error("", e);
      }
    }

    /**
     * Create a {@link XDocumentDataSource}.
     * 
     * @return The {@link XDocumentDataSource}.
     * 
     * @throws PrintException
     *           Can't be created.
     */
    public XDocumentDataSource createXDocumentDatasource() throws PrintException
    {
      XSingleServiceFactory dbContext = UNO.XSingleServiceFactory(UNO.dbContext);
      XDocumentDataSource dataSource = null;
      if (dbContext != null)
      {
        try
        {
          dataSource = UNO.XDocumentDataSource(dbContext.createInstance());
        } catch (Exception e)
        {
          throw new PrintException("", e);
        }
      }

      if (dataSource != null)
      {
        String dirURL = UNO.getParsedUNOUrl(tmpDir.toURI().toString()).Complete;
        Utils.setProperty(dataSource, "URL", "sdbc:flat:" + dirURL);

        UnoProps p = new UnoProps();
        p.setPropertyValue(UnoProperty.EXTENSION, "csv");
        p.setPropertyValue(UnoProperty.CHAR_SET, "UTF-8");
        p.setPropertyValue(UnoProperty.FIXED_LENGTH, false);
        p.setPropertyValue(UnoProperty.HEADER_LINE, true);
        p.setPropertyValue(UnoProperty.FIELD_DELIMITER, ",");
        p.setPropertyValue(UnoProperty.STRING_DELIMITER, "\"");
        p.setPropertyValue(UnoProperty.DECIMAL_DELIMITER, ".");
        p.setPropertyValue(UnoProperty.THOUSAND_DELIMITER, "");
        Utils.setProperty(dataSource, UnoProperty.INFO, p.getProps());

        XStorable xStorable = UNO.XStorable(dataSource.getDatabaseDocument());
        XModel model = UNO.XModel(xStorable);
        URL url = null;
        File tmpFile = new File(tmpDir, OOoBasedMailMerge.DATASOURCE_ODB_FILENAME);
        url = UNO.getParsedUNOUrl(tmpFile.toURI().toString());
        if (url != null && xStorable != null && model != null)
        {
          try
          {
            xStorable.storeAsURL(url.Complete, model.getArgs());
          } catch (IOException e)
          {
            throw new PrintException("", e);
          }
        }
      }
      return dataSource;
    }

    /**
     * Create a line from a record.
     * 
     * @param list
     *          The record
     * @return String representing one line terminated by '\n'.
     */
    private String line(List<String> list)
    {
      StringBuilder buf = new StringBuilder();
      for (String el : list)
      {
        if (buf.length() != 0)
          buf.append(",");
        buf.append(escape(el));
      }
      buf.append("\n");
      return buf.toString();
    }

    /**
     * Escape Strings so that they can be used in a CSV-file.
     * 
     * @param value
     *          String to be escaped
     * @return Escaped String
     */
    private String escape(String value)
    {
      String esc = value.replaceAll("\"", "\"\"");
      return "\"" + esc + "\"";
    }
  }
}
