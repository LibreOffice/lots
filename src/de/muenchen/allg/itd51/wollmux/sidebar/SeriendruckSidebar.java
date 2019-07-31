package de.muenchen.allg.itd51.wollmux.sidebar;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindow2;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractCloseListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.dialog.AbstractNotifier;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.AdjustFields;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.CalcModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.DBDatasourceDialog;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.DBModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeField;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailmergeWizardController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.SpecialField;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.IfThenElseDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class SeriendruckSidebar implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SeriendruckSidebar.class);
  private XWindow parentWindow;
  private XWindow window;
  private SimpleDialogLayout layout;
  private XToolkit toolkit;
  private MailMergeField mailMergeField;
  private XListBox currentDatasources;
  private XButton jumpToLast;
  private XButton jumpToFirst;
  private XSpinField printCount;
  private MailMergeNew mailMerge;
  private TextDocumentController textDocumentController;
  private XControlModel unoControlContainerModel = null;
  private XControl dialogControl = null;

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      layout.draw();
    }
  };

  public SeriendruckSidebar(XWindow parentWindow)
  {
    this.parentWindow = parentWindow;
    this.parentWindow.addWindowListener(this.windowAdapter);

    Object cont = UNO.createUNOService("com.sun.star.awt.UnoControlContainer");
    dialogControl = UnoRuntime.queryInterface(XControl.class, cont);

    // Instanziierung eines ControlContainers für das aktuelle Fenster
    Object unoControlContainerModelO = UNO
        .createUNOService("com.sun.star.awt.UnoControlContainerModel");
    unoControlContainerModel = UnoRuntime.queryInterface(XControlModel.class,
        unoControlContainerModelO);
    dialogControl.setModel(unoControlContainerModel);

    XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, this.parentWindow);

    if (parentWindowPeer != null)
    {
      dialogControl.createPeer(toolkit, parentWindowPeer);
      window = UNO.XWindow(dialogControl);

      layout = new SimpleDialogLayout(window);
      layout.setMarginTop(20);
      layout.setMarginLeft(10);

      layout.addControlsToList(addPreviewControls());
      layout.addControlsToList(addDatasourcesControls());
      layout.addControlsToList(addSerienbriefFeld());
      layout.addControlsToList(addSpezialfeld());
      layout.addControlsToList(addPrintControls());
      layout.addControlsToList(addTableControls());

      window.setVisible(true);
      window.setEnable(true);
    }

    layout.draw();
  }

  private ControlModel addSerienbriefFeld()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties serienbrieffeldLabel = new ControlProperties(ControlType.LABEL,
        "serienbrieffeldLabel");
    serienbrieffeldLabel.setControlPercentSize(30, 30);
    serienbrieffeldLabel.setLabel("Serienbrieffeld");

    ControlProperties cbSerienbrieffeld = new ControlProperties(ControlType.COMBOBOX,
        "cbSerienbrieffeld");
    cbSerienbrieffeld.setControlPercentSize(70, 30);
    cbSerienbrieffeld.setComboBoxDropDown(true);
    cbSerienbrieffeld.setEnabled(false);
    XComboBox comboBox = UNO.XComboBox(cbSerienbrieffeld.getXControl());
    mailMergeField = new MailMergeField(comboBox);
    comboBox.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected != 0)
        {
          String name = UNO.XComboBox(event.Source).getItem((short) event.Selected);
          textDocumentController.insertMailMergeFieldAtCursorPosition(name);
          UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
        }

        textDocumentController.collectNonWollMuxFormFields();
        mailMerge.getDs().updatePreviewFields(mailMerge.getDs().getPreviewDatasetNumber());
      }
    });

    bottomControls.add(serienbrieffeldLabel);
    bottomControls.add(cbSerienbrieffeld);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls, Optional.empty());
  }

  private ControlModel addSpezialfeld()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties spezialfeldLabel = new ControlProperties(ControlType.LABEL,
        "spezialfeldLabel");
    spezialfeldLabel.setControlPercentSize(30, 30);
    spezialfeldLabel.setLabel("Spezialfeld");

    ControlProperties cbSpezialfeld = new ControlProperties(ControlType.COMBOBOX, "cbSpezialfeld");
    cbSpezialfeld.setControlPercentSize(70, 30);
    cbSpezialfeld.setComboBoxDropDown(true);
    cbSpezialfeld.setEnabled(false);
    XComboBox comboBox = UNO.XComboBox(cbSpezialfeld.getXControl());
    SpecialField.addItems(comboBox);
    comboBox.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        switch (event.Selected)
        {
        case 0:
          break;

        case 1:
          // ConfigThingy für leere Gender-Funktion zusammenbauen.
          ConfigThingy genderConf = GenderDialog
              .generateGenderTrafoConf(mailMerge.getDs().getColumnNames().get(0), "", "", "");

          TrafoDialogParameters params = new TrafoDialogParameters();
          params.conf = new ConfigThingy("Func");
          params.conf.addChild(genderConf);
          params.isValid = true;
          params.fieldNames = mailMerge.getDs().getColumnNames();

          new GenderDialog(params, textDocumentController);
          break;

        case 2:          
          TrafoDialogParameters paramsIfThenElse = new TrafoDialogParameters();
          paramsIfThenElse.isValid = true;
          paramsIfThenElse.fieldNames = mailMerge.getDs().getColumnNames();
          new IfThenElseDialog(paramsIfThenElse, textDocumentController);
          break;

        case 3:
          textDocumentController.insertMailMergeFieldAtCursorPosition(MailMergeController.TAG_DATENSATZNUMMER);
          break;

        case 4:
          textDocumentController.insertMailMergeFieldAtCursorPosition(MailMergeController.TAG_SERIENBRIEFNUMMER);
          break;

        case 5:
          textDocumentController.insertNextDatasetFieldAtCursorPosition();
          break;

        default:
          break;
        }

        textDocumentController.collectNonWollMuxFormFields();
        mailMerge.getDs().updatePreviewFields(mailMerge.getDs().getPreviewDatasetNumber());
        UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
      }
    });

    bottomControls.add(spezialfeldLabel);
    bottomControls.add(cbSpezialfeld);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls, Optional.empty());
  }

  private ControlModel addPreviewControls()
  {
    List<ControlProperties> navigationControls = new ArrayList<>();

    ControlProperties previewBtn = new ControlProperties(ControlType.BUTTON, "btnPreview");
    previewBtn.setControlPercentSize(50, 30);
    previewBtn.enableToggleButton(true);
    previewBtn.setLabel("Vorschau");
    previewBtn.setEnabled(false);
    UNO.XButton(previewBtn.getXControl()).addActionListener(previewActionListener);

    ControlProperties jumpToFirstBtn = new ControlProperties(ControlType.BUTTON, "btnJumpToFirst");
    jumpToFirstBtn.setControlPercentSize(10, 30);
    jumpToFirstBtn.setLabel("<<");
    jumpToFirstBtn.setEnabled(false);
    jumpToFirst = UNO.XButton(jumpToFirstBtn.getXControl());
    jumpToFirst.addActionListener(jumpToFirstActionListener);

    ControlProperties printCountFieldProps = new ControlProperties(ControlType.NUMERIC_FIELD,
        "currentDocument");
    printCountFieldProps.setControlPercentSize(40, 30);
    printCountFieldProps.setBorder((short) 2);
    printCountFieldProps.setBorderColor(666666);
    printCountFieldProps.setSpinEnabled(Boolean.TRUE);
    printCountFieldProps.setValue(1);
    printCountFieldProps.setDecimalAccuracy((short) 0);
    printCountFieldProps.setEnabled(false);
    printCount = UNO.XSpinField(printCountFieldProps.getXControl());
    UNO.XTextComponent(printCount).addTextListener(documentCountFieldListener);
    UNO.XNumericField(printCount).setMin(1);

    ControlProperties jumpToLastBtnProps = new ControlProperties(ControlType.BUTTON, "btnJumpToLast");
    jumpToLastBtnProps.setControlPercentSize(10, 30);
    jumpToLastBtnProps.setLabel(">>");
    jumpToLastBtnProps.setEnabled(false);
    jumpToLast = UNO.XButton(jumpToLastBtnProps.getXControl());
    jumpToLast.addActionListener(jumpToLastActionListener);

    navigationControls.add(previewBtn);
    navigationControls.add(jumpToFirstBtn);
    navigationControls.add(printCountFieldProps);
    navigationControls.add(jumpToLastBtnProps);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, navigationControls,
        Optional.empty());
  }

  private ControlModel addDatasourcesControls()
  {
    List<ControlProperties> datasourceControls = new ArrayList<>();

    ControlProperties labelSelectSource = new ControlProperties(ControlType.LABEL,
        "lblSelectSource");
    labelSelectSource.setControlPercentSize(100, 30);
    labelSelectSource.setLabel("Datenquelle wählen");

    ControlProperties hLine = new ControlProperties(ControlType.LINE, "hLine");
    hLine.setControlPercentSize(100, 10);

    ControlProperties file = new ControlProperties(ControlType.BUTTON, "btnFile");
    file.setControlPercentSize(100, 30);
    file.setLabel("Datei..");
    UNO.XButton(file.getXControl()).addActionListener(fileActionListener);

    ControlProperties newCalcTable = new ControlProperties(ControlType.BUTTON, "btnNewCalcTable");
    newCalcTable.setControlPercentSize(100, 30);
    newCalcTable.setLabel("Neue Calc-Tabelle");
    UNO.XButton(newCalcTable.getXControl()).addActionListener(newCalcTableActionListener);

    ControlProperties db = new ControlProperties(ControlType.BUTTON, "btnDB");
    db.setControlPercentSize(100, 30);
    db.setLabel("Datenbank");
    UNO.XButton(db.getXControl()).addActionListener(dbActionListener);

    ControlProperties currentDatasourceProps = new ControlProperties(ControlType.LIST_BOX,
        "currentDatasources");
    currentDatasourceProps.setControlPercentSize(100, 100);
    currentDatasources = UNO.XListBox(currentDatasourceProps.getXControl());
    currentDatasources.addItemListener(currentDatasourcesListener);

    datasourceControls.add(labelSelectSource);
    datasourceControls.add(hLine);
    datasourceControls.add(file);
    datasourceControls.add(newCalcTable);
    datasourceControls.add(db);
    datasourceControls.add(currentDatasourceProps);

    for (CalcModel calcModel : MailMergeDatasource.getOpenCalcWindows())
    {
      updateVisibleTables(calcModel.getWindowTitle(), calcModel.getSpreadSheetTableTitles(), true,
          false);
    }

    return new ControlModel(Orientation.VERTICAL, Align.NONE, datasourceControls, Optional.empty());
  }

  private ControlModel addPrintControls()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties printBtn = new ControlProperties(ControlType.BUTTON, "btnPrint");
    printBtn.setControlPercentSize(100, 30);
    printBtn.setLabel("Drucken");
    UNO.XButton(printBtn.getXControl()).addActionListener(printActionListener);
    UNO.XWindow(printBtn.getXControl()).setEnable(false);

    bottomControls.add(printBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls, Optional.empty());
  }

  private ControlModel addTableControls()
  {
    List<ControlProperties> tableControls = new ArrayList<>();

    ControlProperties editBtn = new ControlProperties(ControlType.BUTTON, "btnEditTable");
    editBtn.setControlPercentSize(100, 30);
    editBtn.setLabel("Tabelle bearbeiten");
    UNO.XButton(editBtn.getXControl()).addActionListener(editTableActionListener);
    UNO.XWindow(editBtn.getXControl()).setEnable(false);
    tableControls.add(editBtn);

    ControlProperties addColumnsBtn = new ControlProperties(ControlType.BUTTON, "btnAddColumns");
    addColumnsBtn.setControlPercentSize(100, 30);
    addColumnsBtn.setLabel("Tabellenspalten ergänzen");
    UNO.XButton(addColumnsBtn.getXControl()).addActionListener(addTableColumnsActionListener);
    UNO.XWindow(addColumnsBtn.getXControl()).setEnable(false);
    tableControls.add(addColumnsBtn);

    ControlProperties changeAllBtn = new ControlProperties(ControlType.BUTTON, "btnChangeAll");
    changeAllBtn.setControlPercentSize(100, 30);
    changeAllBtn.setLabel("Alle Felder anpassen");
    UNO.XButton(changeAllBtn.getXControl()).addActionListener(changeFieldsActionListener);
    UNO.XWindow(changeAllBtn.getXControl()).setEnable(false);
    tableControls.add(changeAllBtn);

    return new ControlModel(Orientation.VERTICAL, Align.NONE, tableControls, Optional.empty());
  }

  private AbstractTextListener documentCountFieldListener = new AbstractTextListener()
  {
    @Override
    public void textChanged(TextEvent arg0)
    {
      mailMerge.getDs().updatePreviewFields((int) UNO.XNumericField(arg0.Source).getValue());
    }
  };

  private void updateVisibleTables(String title, String[] tabs, boolean add, boolean select)
  {
    Set<String> items = new LinkedHashSet<>(Arrays.asList(currentDatasources.getItems()));
    List<String> titles = new ArrayList<>();
    for (String spreadSheetTitle : tabs)
    {
      titles.add(title + " - " + spreadSheetTitle);
    }
    if (add)
    {
      items.addAll(titles);
    } else
    {
      items.removeAll(titles);
      String item = currentDatasources.getSelectedItem();
      currentDatasources.selectItem(item, !titles.contains(item));
    }
    currentDatasources.removeItems((short) 0, currentDatasources.getItemCount());
    currentDatasources.addItems(items.toArray(new String[] {}), (short) 0);
    if (select)
    {
      currentDatasources.selectItem(titles.get(0), true);
    }
  }

  private AbstractItemListener currentDatasourcesListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      if (mailMerge == null)
      {
        setMailMergeOnDocument();
      }
      mailMerge.getDs().setDatasource(currentDatasources.getSelectedItem());
      updateControls();
    }

    private void updateControls()
    {
      mailMergeField.setMailMergeDatasource(mailMerge.getDs());
      UNO.XNumericField(printCount).setMax(mailMerge.getDs().hasDatasource() ? mailMerge.getDs().getNumberOfDatasets() : 0);

      boolean active = mailMerge.getDs() != null && mailMerge.getDs().hasDatasource();
      UNO.XWindow(layout.getControl("btnPreview")).setEnable(active);
      UNO.XWindow(layout.getControl("btnPrint")).setEnable(active);
      UNO.XWindow(layout.getControl("cbSerienbrieffeld")).setEnable(active);
      UNO.XWindow(layout.getControl("cbSpezialfeld")).setEnable(active);
      UNO.XWindow(layout.getControl("btnPrint")).setEnable(active);
      UNO.XWindow(layout.getControl("btnEditTable")).setEnable(active);

      boolean hasUnmappedFields = mailMerge.getDs().hasDatasource()
          && mailMerge.getDs().checkUnmappedFields(mailMerge.getDs().getColumnNames());
      UNO.XWindow(layout.getControl("btnAddColumns"))
          .setEnable(active && hasUnmappedFields && mailMerge.getDs().supportsAddColumns());
      UNO.XWindow(layout.getControl("btnChangeAll")).setEnable(active && hasUnmappedFields);
    }
  };

  private AbstractActionListener printActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      if (!mailMerge.getDs().hasDatasource())
        return;

        MailMergeController c = new MailMergeController(textDocumentController, mailMerge.getDs());
        MailmergeWizardController mwController = new MailmergeWizardController(c,
            textDocumentController.getModel().doc);
        mwController.startWizard();
        textDocumentController.collectNonWollMuxFormFields();
        textDocumentController.setFormFieldsPreviewMode(true);
    }
  };

  private AbstractNotifier dbDatasourceListener = new AbstractNotifier()
  {
    @Override
    public void notify(String message)
    {
      setMailMergeOnDocument();

      List<String> dbTableNames = mailMerge.getDs().getDbTableNames(message);
      DBModel model = new DBModel(message, dbTableNames);
      mailMerge.getDs().addCachedDbConnection(model);
      updateVisibleTables(model.getDatasourceName(), model.getTableNames().toArray(new String[] {}),
          true, true);
    }
  };

  private AbstractActionListener dbActionListener = new AbstractActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      setMailMergeOnDocument();
      DBDatasourceDialog oooDSDialog = new DBDatasourceDialog(dbDatasourceListener);
    }
  };

  private AbstractActionListener newCalcTableActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      setMailMergeOnDocument();
      CalcModel calcModel = mailMerge.getDs().openAndselectNewCalcTableAsDatasource();
      UNO.XCloseable(calcModel.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
      updateVisibleTables(calcModel.getWindowTitle(), calcModel.getSpreadSheetTableTitles(), true,
          true);
    }
  };

  private AbstractCloseListener documentCloseListener = new AbstractCloseListener()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      XSpreadsheetDocument doc = UNO.XSpreadsheetDocument(arg0.Source);

      String title = UNO.getPropertyByPropertyValues(UNO.XModel(doc).getArgs(), "Title");

      if (title == null)
      {
        LOGGER.debug("SeriendruckSidebar: documentCloseListener: title is NULL."
            + " Could not close Window successfully.");
        return;
      }
      CalcModel model = new CalcModel(title, "", doc.getSheets().getElementNames(), doc);
      updateVisibleTables(model.getWindowTitle(), model.getSpreadSheetTableTitles(), false, false);
    }
  };

  private AbstractActionListener fileActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      setMailMergeOnDocument();
      CalcModel model = mailMerge.getDs().selectFileAsDatasource();
      if (model != null)
      {
        UNO.XCloseable(model.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
        updateVisibleTables(model.getWindowTitle(), model.getSpreadSheetTableTitles(), true, true);
      }
    }
  };

  private AbstractActionListener jumpToFirstActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
      currentDatasourceCountText.setText("1");
      mailMerge.getDs().updatePreviewFields(1);
    }
  };

  private AbstractActionListener jumpToLastActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      int datasetCount = mailMerge.getDs().getNumberOfDatasets();

      XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
      currentDatasourceCountText.setText(String.valueOf(datasetCount));

      mailMerge.getDs().updatePreviewFields(datasetCount);
    }
  };

  private AbstractActionListener editTableActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      mailMerge.getDs().toFront();
    }
  };

  private ActionListener adjustFieldsFinishListener = e -> {
    boolean hasUnmappedFields = mailMerge.getDs()
        .checkUnmappedFields(mailMerge.getDs().getColumnNames());
    XWindow2 btnChangeAll = UNO.XWindow2(layout.getControl("btnChangeAll"));
    btnChangeAll.setEnable(btnChangeAll.isVisible() && hasUnmappedFields);
    XWindow2 btnAddColumns = UNO.XWindow2(layout.getControl("btnAddColumns"));
    btnAddColumns.setEnable(btnAddColumns.isVisible() && hasUnmappedFields);
  };

  private AbstractActionListener addTableColumnsActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      AdjustFields.showAddMissingColumnsDialog(textDocumentController, mailMerge.getDs(),
          adjustFieldsFinishListener);
    }
  };

  private AbstractActionListener changeFieldsActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      AdjustFields.showAdjustFieldsDialog(textDocumentController, mailMerge.getDs(),
          adjustFieldsFinishListener);
    }
  };

  private AbstractActionListener previewActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XControl previewBtn = UNO.XControl(arg0.Source);

      XPropertySet propertySet = UNO.XPropertySet(previewBtn.getModel());

      try
      {
        short toggleState = (short) propertySet.getPropertyValue("State");

        if (toggleState == 0)
        {
          textDocumentController.setFormFieldsPreviewMode(false);
          UNO.XWindow(jumpToLast).setEnable(false);
          UNO.XWindow(jumpToFirst).setEnable(false);
          UNO.XWindow(printCount).setEnable(false);
        } else if (toggleState == 1)
        {
          if (!mailMerge.getDs().hasDatasource() && mailMerge.getDs().getNumberOfDatasets() <= 0)
          {
            propertySet.setPropertyValue("State", 0);
            return;
          }


          textDocumentController.collectNonWollMuxFormFields();
          textDocumentController.setFormFieldsPreviewMode(true);
          mailMerge.getDs().updatePreviewFields(mailMerge.getDs().getPreviewDatasetNumber());

          UNO.XWindow(jumpToLast).setEnable(true);
          UNO.XWindow(jumpToFirst).setEnable(true);
          UNO.XWindow(printCount).setEnable(true);
        }
      } catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
          | PropertyVetoException e)
      {
        LOGGER.error("", e);
      }
    }
  };

  private void setMailMergeOnDocument() {
    textDocumentController = DocumentManager.getTextDocumentController(UNO.getCurrentTextDocument());

    if (textDocumentController == null)
    {
      LOGGER.debug("SeriendruckSidebar: setMailMergeOnDocument(): textDocumentController is NULL.");
      return;
    }

    textDocumentController.setFormFieldsPreviewMode(false);

    mailMerge = DocumentManager.getDocumentManager()
        .getCurrentMailMergeNew(textDocumentController.getModel().doc);

    if (mailMerge == null)
    {
      mailMerge = new MailMergeNew(textDocumentController, actionEvent -> {
        if (actionEvent.getSource() instanceof MailMergeNew)
          WollMuxEventHandler.getInstance().handleMailMergeNewReturned(textDocumentController);
      });

      DocumentManager.getDocumentManager().setCurrentMailMergeNew(textDocumentController.getModel().doc,
          mailMerge);
    }
  }

  @Override
  public LayoutSize getHeightForWidth(int arg0)
  {
    return new LayoutSize(800, 800, 800);
  }

  @Override
  public int getMinimalWidth()
  {
    return 300;
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    return UnoRuntime.queryInterface(XAccessible.class, getWindow());
  }

  @Override
  public XWindow getWindow()
  {
    if (window == null)
    {
      throw new DisposedException("", this);
    }
    return window;
  }
}