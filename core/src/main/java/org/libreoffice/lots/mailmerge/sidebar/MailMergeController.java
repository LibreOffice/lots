/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.mailmerge.sidebar;

import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.document.TextDocumentModel.ReferencedFieldID;
import org.libreoffice.lots.document.commands.DocumentCommandInterpreter;
import org.libreoffice.lots.event.WollMuxEventHandler;
import org.libreoffice.lots.event.handlers.OnSetFormValue;
import org.libreoffice.lots.event.handlers.OnTextDocumentControllerInitialized;
import org.libreoffice.lots.form.model.FormModelException;
import org.libreoffice.lots.mailmerge.ConnectionModel;
import org.libreoffice.lots.mailmerge.FieldSubstitution;
import org.libreoffice.lots.mailmerge.NoTableSelectedException;
import org.libreoffice.lots.mailmerge.ds.DBDatasourceDialog;
import org.libreoffice.lots.mailmerge.ds.DatasourceModel;
import org.libreoffice.lots.mailmerge.ds.DatasourceModelListener;
import org.libreoffice.lots.mailmerge.gender.GenderDialog;
import org.libreoffice.lots.mailmerge.gender.GenderTrafoModel;
import org.libreoffice.lots.mailmerge.ifthenelse.IfThenElseDialog;
import org.libreoffice.lots.mailmerge.ifthenelse.IfThenElseModel;
import org.libreoffice.lots.mailmerge.print.SetFormValue;
import org.libreoffice.lots.mailmerge.printsettings.MailmergeWizardController;
import org.libreoffice.lots.util.L;

/**
 * Controller of the sidebar.
 */
public class MailMergeController implements PreviewModelListener, DatasourceModelListener
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeController.class);

  /**
   * The selected data source. Do not change directly, use {@link #setDatasource(Optional)}.
   */
  private Optional<DatasourceModel> datasourceModel;

  /**
   * Model for preview mode.
   */
  private PreviewModel previewModel;

  /**
   * The controller of the document.
   */
  private TextDocumentController textDocumentController;

  /**
   * The sidebar panel.
   */
  private MailMergeGUI gui;

  /**
   * The event listener on {@link WollMuxEventHandler} has been unregistered.
   */
  private boolean isUnregistered;

  /**
   * Create a new controller and the gui of the sidebar.
   *
   * @param resourceUrl
   *          The resource description
   * @param context
   *          The context of the sidebar.
   * @param parentWindow
   *          The parent window, which contains the sidebar.
   * @param model
   *          The model of the document to which the sidebar belongs.
   */
  public MailMergeController(String resourceUrl, XComponentContext context,
      XWindow parentWindow, XModel model)
  {
    datasourceModel = Optional.empty();
    previewModel = new PreviewModel();
    previewModel.setDatasourceModel(datasourceModel);
    previewModel.addListener(this);
    gui = new MailMergeGUI(resourceUrl, this, context, parentWindow);
    gui.createGUI();
    XTextDocument doc = UNO.XTextDocument(model);
    if (DocumentManager.hasTextDocumentController(doc))
    {
      isUnregistered = true;
      onTextDocumentControllerInitialized(
          new OnTextDocumentControllerInitialized(DocumentManager.getTextDocumentController(doc)));
    } else
    {
      isUnregistered = false;
      WollMuxEventHandler.getInstance().registerListener(this);
    }
  }

  public XUIElement getGUI()
  {
    return gui;
  }

  /**
   * Sets {@link TextDocumentController} once it is available.
   *
   * @param event
   *          Event with the instance of {@link TextDocumentController}.
   */
  @Subscribe
  public void onTextDocumentControllerInitialized(OnTextDocumentControllerInitialized event)
  {
    if (textDocumentController == null)
    {
      textDocumentController = event.getTextDocumentController();
      if (textDocumentController != null)
      {
        LOGGER.debug("initialized");
        openDatasourceFromLastStoredSettings(
            textDocumentController.getModel().getMailmergeConfig());
        textDocumentController.setFormFieldsPreviewMode(false);
        unregisterListener();
      }
    }
  }

  @Override
  public void previewChanged()
  {
    boolean isPreview = previewModel.isPreview();
    gui.updatePreview(isPreview, previewModel.getPreviewNumber());
    textDocumentController.collectNonWollMuxFormFields();
    textDocumentController.setFormFieldsPreviewMode(isPreview);
    updatePreviewFields();
  }

  @Override
  public void datasourceChanged()
  {
    datasourceModel.ifPresent(ds -> {
      try
      {
        boolean hasUnmappedFields = textDocumentController.getModel()
            .getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(ds.getColumnNames())).length > 0;
        gui.activateControls(ds, ds.getNumberOfRecords(), hasUnmappedFields,
            ds.supportsAddColumns());
        textDocumentController.collectNonWollMuxFormFields();
        updatePreviewFields();

        ConfigThingy seriendruck = new ConfigThingy("Seriendruck");
        ConfigThingy dq = ds.getSettings();
        if (dq.count() > 0)
        {
          seriendruck.addChild(dq);
        }
        textDocumentController.setMailmergeConfig(seriendruck);
      } catch (NoTableSelectedException ex)
      {
        gui.deactivateControls(ds);
      }
    });
  }

  /**
   * Close this controller.
   */
  public void dispose()
  {
    unregisterListener();
  }

  /**
   * Each time this method is called the preview status is changed from activated to deactivated or
   * vice versa.
   *
   * @param event
   *          The event of the button.
   */
  public void togglePreview(ActionEvent event)
  {
    try
    {
      short toggleState = (short) UnoProperty.getProperty(UNO.XControl(event.Source).getModel(), UnoProperty.STATE);
      previewModel.setPreview(toggleState == 1);
    } catch (UnoHelperException | IllegalArgumentException | NoTableSelectedException ex)
    {
      LOGGER.error("", ex);
    }
  }

  /**
   * Show the first record of the data source.
   *
   * @param event
   *          The event of the button.
   */
  public void jumpToFirstRecord(ActionEvent event)
  {
    previewModel.setPreviewNumber(1);
  }

  /**
   * Show a record of the data source.
   *
   * @param event
   *          An event of the numeric field providing the record id.
   */
  public void updateCurrentRecord(TextEvent event)
  {
    previewModel.setPreviewNumber((int) UNO.XNumericField(event.Source).getValue());
  }

  /**
   * Show the last record of the data source.
   *
   * @param event
   *          The event of the button.
   */
  public void jumpToLastRecord(ActionEvent event)
  {
    try
    {
      previewModel.gotoLastDataset();
    } catch (NoTableSelectedException ex)
    {
      LOGGER.error("", ex);
    }
  }

  /**
   * Change to a new data source.
   *
   * @param event
   *          An event of the list box providing the name of the data source.
   */
  public void changeDatasource(ItemEvent event)
  {
    try
    {
      setDatasource(ConnectionModel.selectDatasource(UNO.XListBox(event.Source).getSelectedItem()));
    } catch (NoTableSelectedException e1)
    {
      LOGGER.debug("", e1);
    }
  }

  /**
   * Shows the dialog for selecting a database data source. The selected database is set as data
   * source.
   *
   * @param event
   *          The button event.
   */
  public void openDatasourceDialog(ActionEvent event)
  {
    new DBDatasourceDialog(dbName -> {
      try
      {
        setDatasource(ConnectionModel.addAndSelectDatasource(loadDataSource(dbName), Optional.empty()));
        gui.selectDatasource(ConnectionModel.buildConnectionName(datasourceModel));
      } catch (NoTableSelectedException e)
      {
        LOGGER.debug("", e);
      }
    });
  }

  /**
   * Open a new calc file and select it as data source.
   *
   * @param event
   *          The button event.
   */
  public void openAndselectNewCalcTableAsDatasource(ActionEvent event)
  {
    LOGGER.debug("Öffne neues Calc-Dokument als Datenquelle für Seriendruck");
    try
    {
      XSpreadsheetDocument spread = UNO
          .XSpreadsheetDocument(UNO.loadComponentFromURL("private:factory/scalc", true, true));

      setDatasource(ConnectionModel.addAndSelectDatasource(spread, Optional.empty()));
      gui.selectDatasource(ConnectionModel.buildConnectionName(datasourceModel));
    } catch (UnoHelperException | NoTableSelectedException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Open a calc file and select it as data source.
   *
   * @param event
   *          The button event.
   */
  public void selectFileAsDatasource(ActionEvent event)
  {
    XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext,
        TemplateDescription.FILEOPEN_SIMPLE);
    String filterName = "Tabellendokument";
    picker.appendFilter(filterName, "*.ods");
    picker.appendFilter("Alle Dateien", "*");
    picker.setCurrentFilter(filterName);
    picker.setMultiSelectionMode(false);

    short res = picker.execute();
    if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
    {
      String[] files = picker.getFiles();
      try
      {
        LOGGER.debug("Öffne {} als Datenquelle für Seriendruck", files[0]);
        XSpreadsheetDocument doc = getCalcDocByFile(files[0]);

        if (doc != null)
        {
          DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
              textDocumentController, WollMuxFiles.isDebugMode());

          try
          {
            dci.scanInsertFormValueCommands();

            textDocumentController.getFormModel();

            Map<String,String> formFieldValues = textDocumentController.getFormFieldValues();

            for (Map.Entry<String, String> entry: formFieldValues.entrySet())
            {
              textDocumentController.updateDocumentFormFields(entry.getKey());
            }
          } catch (FormModelException e)
          {
            LOGGER.error("", e);
          }

          setDatasource(ConnectionModel.addAndSelectDatasource(doc, Optional.empty()));
          gui.selectDatasource(ConnectionModel.buildConnectionName(datasourceModel));
        }
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Show the data source so it can be modified by the user.
   *
   * @param event
   *          The button event.
   */
  public void editDatasource(ActionEvent event)
  {
    datasourceModel.ifPresent(DatasourceModel::toFront);
  }

  /**
   * Show the dialog for adding new columns to a data source.
   *
   * @param event
   *          The button event.
   */
  public void showAddColumnsDialog(ActionEvent event)
  {
    datasourceModel.ifPresent(datasource -> {
      ActionListener addTableColumnsFinishListener = e -> {
        datasourceModel.ifPresent(ds -> {
          @SuppressWarnings("unchecked")
          Map<String, FieldSubstitution> mapIdToSubstitution = (HashMap<String, FieldSubstitution>) e
              .getSource();
          try
          {
            ds.addColumns(mapIdToSubstitution);
          } catch (NoTableSelectedException e1)
          {
            InfoDialog.showInfoModal("", e1.getMessage());
          }
        });
        updateDatasourceControls();
      };
      try
      {
        ReferencedFieldID[] fieldIDs = textDocumentController.getModel()
            .getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(datasource.getColumnNames()));
        List<String> columns = new ArrayList<>(datasource.getColumnNames());

        AdjustFields.showFieldMappingDialog("Tabellenspalten ergänzen", L.m("Column"),
            L.m("Assignment (initial)"), L.m("Add columns"), fieldIDs, columns, true,
            addTableColumnsFinishListener);
      } catch (NoTableSelectedException ex)
      {
        InfoDialog.showInfoModal("", ex.getMessage());
      }
    });
  }

  /**
   * Show a dialog for modifying the fields in the document.
   *
   * @param event
   *          The button event.
   */
  public void showModifyDocDialog(ActionEvent event)
  {
    datasourceModel.ifPresent(ds -> {
      ActionListener adjustFieldsFinishListener = e -> {
	@SuppressWarnings("unchecked")
	Map<String, FieldSubstitution> mapIdToSubstitution = (HashMap<String, FieldSubstitution>) e
	    .getSource();
	adjustFields(mapIdToSubstitution);
	updateDatasourceControls();
      };
      try
      {
        ReferencedFieldID[] fieldIDs = textDocumentController.getModel()
            .getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(ds.getColumnNames()));
        List<String> columns = new ArrayList<>(ds.getColumnNames());
        AdjustFields.showFieldMappingDialog("Adjust fields", L.m("Old field"),
            L.m("New assignment"), L.m("Adjust fields"), fieldIDs, columns, false,
            adjustFieldsFinishListener);
      } catch (NoTableSelectedException ex)
      {
        InfoDialog.showInfoModal("", ex.getMessage());
      }
    });
  }

  /**
   * Add a new field to the document.
   *
   * @param event
   *          An event of a combo box providing the name of column to be used in the field.
   */
  public void addMailMergeField(ItemEvent event)
  {
    if (event.Selected != 0)
    {
      String name = UNO.XComboBox(event.Source).getItem((short) event.Selected);
      textDocumentController.insertMailMergeFieldAtCursorPosition(name);
      UNO.XTextComponent(event.Source).setText(UNO.XComboBox(event.Source).getItem((short) 0));
    }

    textDocumentController.collectNonWollMuxFormFields();
    updatePreviewFields();
  }

  /**
   * Add a new special field to the document.
   *
   * @param event
   *          An event of a combo box providing the type of field.
   */
  public void addSpecialField(ItemEvent event)
  {
    switch (event.Selected)
    {
    case 0:
      break;

    case 1:
      addGenderField();
      break;

    case 2:
      addIfThenElseField();
      break;

    case 3:
      textDocumentController.insertMailMergeFieldAtCursorPosition(SetFormValue.TAG_RECORD_ID);
      break;

    case 4:
      textDocumentController.insertMailMergeFieldAtCursorPosition(SetFormValue.TAG_MAILMERGE_ID);
      break;

    case 5:
      textDocumentController.insertNextDatasetFieldAtCursorPosition();
      break;

    default:
      break;
    }

    textDocumentController.collectNonWollMuxFormFields();
    updatePreviewFields();
    UNO.XTextComponent(event.Source).setText(UNO.XComboBox(event.Source).getItem((short) 0));
  }

  private void addIfThenElseField()
  {
    datasourceModel.ifPresent(ds -> {
      try
      {
        ConfigThingy currentTrafo = textDocumentController.getModel().getFormFieldTrafoFromSelection();
        IfThenElseModel model = new IfThenElseModel(currentTrafo);
        short result = new IfThenElseDialog(new ArrayList<>(ds.getColumnNames()), model).execute();
        if (result == ExecutableDialogResults.OK)
        {
          ConfigThingy resultConf = model.create();
          if (model.getName() == null)
          {
            textDocumentController.replaceSelectionWithTrafoField(resultConf, "Wenn...Dann...Sonst");
          } else {
            textDocumentController.setTrafo(model.getName(), resultConf);
          }
        }
      } catch (NoTableSelectedException | NodeNotFoundException ex)
      {
        LOGGER.debug("", ex);
      }
    });
  }

  private void addGenderField()
  {
    datasourceModel.ifPresent(ds -> {
      ConfigThingy currentTrafo = textDocumentController.getModel().getFormFieldTrafoFromSelection();
      try
      {
        GenderTrafoModel model = new GenderTrafoModel(currentTrafo);
        short result = GenderDialog.startDialog(new ArrayList<>(ds.getColumnNames()), model);
        if (result == ExecutableDialogResults.OK)
        {
          ConfigThingy conf = model.generateGenderTrafoConf();
          if (model.getFunctionName() == null)
          {
            textDocumentController.replaceSelectionWithTrafoField(conf, "Gender");
          } else
          {
            textDocumentController.setTrafo(model.getFunctionName(), conf);
          }
        }
      } catch (NoTableSelectedException | NodeNotFoundException ex)
      {
        LOGGER.debug("", ex);
      }
    });
  }

  /**
   * Show the print settings dialog.
   *
   * @param event
   *          The button event.
   */
  public void print(ActionEvent event)
  {
    datasourceModel.ifPresent(ds -> {
      try
      {
        MailmergeWizardController mwController = new MailmergeWizardController(ds,
            textDocumentController);
        textDocumentController.collectNonWollMuxFormFields();
        mwController.startWizard();
      } catch (NoTableSelectedException ex)
      {
        LOGGER.debug("", ex);
        InfoDialog.showInfoModal(L.m("Printing could not be started."), ex.getMessage());
      }
    });
  }

  /**
   * Unregister the listener on the WollMux Event Bus.
   */
  private void unregisterListener()
  {
    if (!isUnregistered)
    {
      WollMuxEventHandler.getInstance().unregisterListener(this);
      isUnregistered = true;
    }
  }

  /**
   * Change the data source.
   *
   * @param model
   *          The new data source.
   */
  private void setDatasource(Optional<DatasourceModel> model)
  {
    datasourceModel.ifPresent(ds -> ds.removeDatasourceListener(this));
    datasourceModel = model;
    previewModel.setDatasourceModel(datasourceModel);
    datasourceModel.ifPresent(ds -> ds.addDatasourceListener(this));
    datasourceChanged();
  }

  public void setPreviewMode(boolean isPreview)
  {
    textDocumentController.setFormFieldsPreviewMode(isPreview);
  }
  /**
   * Set the fields in the document to the content of the data source.
   */
  private void updatePreviewFields()
  {
    if (previewModel.isPreview())
    {
      try
      {
        for (Map.Entry<String, String> entry : previewModel.getCurrentRecord().entrySet())
        {
          new OnSetFormValue(textDocumentController.getModel().doc, entry.getKey(),
              entry.getValue(), null).emit();
        }
        String previewDatasetNumberStr = "" + previewModel.getPreviewNumber();
        new OnSetFormValue(textDocumentController.getModel().doc, SetFormValue.TAG_RECORD_ID,
            previewDatasetNumberStr, null).emit();
        new OnSetFormValue(textDocumentController.getModel().doc, SetFormValue.TAG_MAILMERGE_ID,
            previewDatasetNumberStr, null).emit();
      } catch (NoTableSelectedException ex)
      {
        LOGGER.debug("", ex);
      }
    }
  }

  /**
   * Update the controls to modify a data source.
   */
  private void updateDatasourceControls()
  {
    datasourceModel.ifPresentOrElse(ds -> {
      try
      {
        boolean hasUnmappedFields = textDocumentController.getModel()
            .getReferencedFieldIDsThatAreNotInSchema(ds.getColumnNames()).length > 0;
        gui.updateFieldControls(hasUnmappedFields);
      } catch (NoTableSelectedException ex)
      {
        LOGGER.debug("", ex);
      }
    }, () -> gui.updateFieldControls(false));
  }

  /**
   * Open the data source stored in the meta data.
   *
   * @param mmconf
   *          The meta data configuration.
   */
  private void openDatasourceFromLastStoredSettings(ConfigThingy mmconf)
  {
    ConfigThingy datenquelle = new ConfigThingy("");
    String type = "unknown";
    try
    {
      ConfigThingy query = mmconf.query("DataSource");
      if (query.count() > 0)
      {
        datenquelle = query.getLastChild();
        type = datenquelle.getString("TYPE", null);

        if ("calc".equalsIgnoreCase(type))
        {
          openCalcFromSettings(datenquelle);
        } else if ("ooo".equalsIgnoreCase(type))
        {
          openBaseFromSettings(datenquelle);
        } else if (type != null)
        {
          LOGGER.error("Ignoriere Datenquelle mit unbekanntem Typ '{}'", type);
        }
      }
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("Missing argument for datasource of type \"{}\":", type, e);
    } catch (NoTableSelectedException ex)
    {
      LOGGER.error("Die Tabelle existiert nicht", ex);
    }
  }

  /**
   * Open the base data source stored in the meta data.
   *
   * @param datenquelle
   *          The meta data configuration.
   *
   * @throws NodeNotFoundException
   *           Invalid configuration.
   * @throws NoTableSelectedException
   *           The table doesn't exist.
   */
  private void openBaseFromSettings(ConfigThingy datenquelle)
      throws NodeNotFoundException, NoTableSelectedException
  {
    String source = datenquelle.get("SOURCE").toString();
    String table = datenquelle.get("TABLE").toString();
    if (ConnectionModel.hasConnection(source, table))
    {
      setDatasource(
          ConnectionModel.selectDatasource(ConnectionModel.buildConnectionName(source, table)));
    } else
    {
      XOfficeDatabaseDocument ds = loadDataSource(source);
      if (ds != null)
      {
        setDatasource(ConnectionModel.addAndSelectDatasource(ds, Optional.ofNullable(table)));
      }
    }
    gui.selectDatasource(ConnectionModel.buildConnectionName(datasourceModel));
  }

  /**
   * Open the calc data source stored in the meta data.
   *
   * @param datenquelle
   *          The meta data configuration.
   *
   * @throws NodeNotFoundException
   *           Invalid configuration.
   * @throws NoTableSelectedException
   *           The table doesn't exist.
   */
  private void openCalcFromSettings(ConfigThingy datenquelle)
      throws NodeNotFoundException, NoTableSelectedException
  {
    String url = datenquelle.get("URL").toString();
    String model = "";
    try
    {
      String[] splittedURL = Paths.get(new URL(url).toURI()).toFile().getName().split("\\.");
      model = splittedURL[splittedURL.length - 2];
    } catch (MalformedURLException | URISyntaxException e)
    {
      LOGGER.debug("", e);
    }
    String table = datenquelle.get("TABLE").toString();
    if (ConnectionModel.hasConnection(model, table))
    {
      String connectionName = ConnectionModel.buildConnectionName(model, table);
      setDatasource(ConnectionModel.selectDatasource(connectionName));
    } else
    {
      XSpreadsheetDocument doc = getCalcDocByFile(url);
      if (doc != null)
      {
        setDatasource(ConnectionModel.addAndSelectDatasource(doc, Optional.ofNullable(table)));
      }
    }
    gui.selectDatasource(ConnectionModel.buildConnectionName(datasourceModel));
  }

  /**
   * Open a LibreOffice data base.
   *
   * @param dbName
   *          The name of the data base.
   * @return The data base.
   */
  private XOfficeDatabaseDocument loadDataSource(String dbName)
  {
    try
    {
      XDataSource ds = UNO.XDataSource(UNO.dbContext.getRegisteredObject(dbName));
      XDocumentDataSource dds = UNO.XDocumentDataSource(ds);
      XOfficeDatabaseDocument dbdoc = dds.getDatabaseDocument();
      String url = UNO.XModel(dbdoc).getURL();

      for (XComponent comp : UnoCollection.getCollection(UNO.desktop.getComponents(),
          XComponent.class))
      {
        XModel dbModel = UNO.XModel(comp);
        if (dbModel.getURL().equals(url))
        {
          return dbdoc;
        }
      }
      return UnoRuntime.queryInterface(XOfficeDatabaseDocument.class,
          UNO.loadComponentFromURL(url, false, false, true));
    } catch (com.sun.star.uno.Exception | UnoHelperException e)
    {
      LOGGER.error("", e);
      return null;
    }
  }

  /**
   * Open a calc file.
   *
   * @param file
   *          The file name.
   * @return The document.
   */
  private XSpreadsheetDocument getCalcDocByFile(String file)
  {
    try
    {
      return UNO.XSpreadsheetDocument(UNO.loadComponentFromURL(file, false, true));
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
      return null;
    }
  }

  /**
   * Adjust all the fields given by the map.
   *
   * @param mapIdToSubstitution
   *          Mapping from a field id to their substitution.
   */
  private void adjustFields(Map<String, FieldSubstitution> mapIdToSubstitution)
  {
    for (Map.Entry<String, FieldSubstitution> ent : mapIdToSubstitution
        .entrySet())
    {
      String fieldId = ent.getKey();
      FieldSubstitution subst = ent.getValue();
      subst.apply(textDocumentController, fieldId);

      // update data structure
      textDocumentController.updateDocumentCommands();
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
          textDocumentController);
      dci.scanGlobalDocumentCommands();
      // does also collectNonWollMuxFormFields()
      dci.scanInsertFormValueCommands();

      // reset value
      textDocumentController.setFormFieldValue(fieldId, null);

      // update view
      for (FieldSubstitution.SubstElement ele : subst)
      {
	if (ele.isField())
	{
	  textDocumentController.updateDocumentFormFields(ele.getValue());
	}
      }
    }
  }
}
