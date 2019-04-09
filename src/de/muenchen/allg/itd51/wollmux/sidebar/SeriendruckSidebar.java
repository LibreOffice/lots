package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;
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

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {

    @Override
    public void windowShown(EventObject event)
    {
      getDocumentController().ifPresent(controller -> {
        mailMerge = DocumentManager.getDocumentManager()
            .getCurrentMailMergeNew(controller.getModel().doc);
        if (mailMerge == null)
        {
          mailMerge = new MailMergeNew(controller, actionEvent -> {
            if (actionEvent.getSource() instanceof MailMergeNew)
              WollMuxEventHandler.getInstance().handleMailMergeNewReturned(controller);
          });
          DocumentManager.getDocumentManager().setCurrentMailMergeNew(controller.getModel().doc,
              mailMerge);
        }
        updateControls();
      });
    }

    @Override
    public void windowResized(WindowEvent e)
    {
      layout.draw();
    }
  };

  private XControlModel unoControlContainerModel = null;
  private XControl dialogControl = null;

  public SeriendruckSidebar(XWindow parentWindow)
  {
    this.parentWindow = parentWindow;
    this.parentWindow.addWindowListener(this.windowAdapter);

    getDocumentController().ifPresent(controller -> controller.setFormFieldsPreviewMode(false));

    Object cont = UNO.createUNOService("com.sun.star.awt.UnoControlContainer");
    dialogControl = UnoRuntime.queryInterface(XControl.class, cont);

    // Instanzierung eines ControlContainers für das aktuelle Fenster
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
          getDocumentController()
              .ifPresent(controller -> controller.insertMailMergeFieldAtCursorPosition(name));
          UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
        }

        getDocumentController().ifPresent(TextDocumentController::collectNonWollMuxFormFields);
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

          GenderDialog genderDialog = new GenderDialog(params);
          break;
        case 2:
          // ConfigThingy für leere WennDannSonst-Funktion zusammenbauen. Aufbau:
          // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
          ConfigThingy ifConf = new ConfigThingy("IF");
          ConfigThingy strCmpConf = ifConf.add("STRCMP");
          strCmpConf.add("VALUE").add(mailMerge.getDs().getColumnNames().get(0));
          strCmpConf.add("");
          ifConf.add("THEN").add("");
          ifConf.add("ELSE").add("");
          
          TrafoDialogParameters paramsIfThenElse = new TrafoDialogParameters();
          paramsIfThenElse.conf = new ConfigThingy("IF");
          paramsIfThenElse.conf.addChild(ifConf);
          paramsIfThenElse.isValid = true;
          paramsIfThenElse.fieldNames = mailMerge.getDs().getColumnNames();
          
          new IfThenElseDialog(paramsIfThenElse);
          break;
        case 3:
          getDocumentController().ifPresent(controller -> controller
              .insertMailMergeFieldAtCursorPosition(MailMergeNew.TAG_DATENSATZNUMMER));
          break;
        case 4:
          getDocumentController().ifPresent(controller -> controller
              .insertMailMergeFieldAtCursorPosition(MailMergeNew.TAG_SERIENBRIEFNUMMER));
          break;
        case 5:
          getDocumentController()
              .ifPresent(TextDocumentController::insertNextDatasetFieldAtCursorPosition);
          break;
        case 6:
          boolean hasUnmappedFields = mailMerge.getDs()
              .checkUnmappedFields(mailMerge.getDs().getColumnNames());

          if (hasUnmappedFields)
          {
            AdjustFields adjustFieldsDialog = new AdjustFields();
            getDocumentController().ifPresent(controller -> {
              adjustFieldsDialog.showAdjustFieldsDialog(controller, mailMerge.getDs());
            });
          }
          break;
        default:
          break;
        }

        getDocumentController().ifPresent(TextDocumentController::collectNonWollMuxFormFields);
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
    jumpToFirstBtn.setLabel("<");
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
    UNO.XNumericField(printCount).setMin(0);

    ControlProperties jumpToLastBtnProps = new ControlProperties(ControlType.BUTTON, "btnJumpToLast");
    jumpToLastBtnProps.setControlPercentSize(10, 30);
    jumpToLastBtnProps.setLabel(">");
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

    return new ControlModel(Orientation.VERTICAL, Align.NONE, datasourceControls, Optional.empty());
  }

  private ControlModel addPrintControls()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties printBtn = new ControlProperties(ControlType.BUTTON, "btnPrint");
    printBtn.setControlPercentSize(100, 30);
    printBtn.setLabel("Drucken");
    UNO.XButton(printBtn.getXControl()).addActionListener(printActionListener);

    bottomControls.add(printBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls, Optional.empty());
  }

  private AbstractTextListener documentCountFieldListener = new AbstractTextListener()
  {
    @Override
    public void textChanged(TextEvent arg0)
    {
      mailMerge.getDs().updatePreviewFields((int) UNO.XNumericField(arg0.Source).getValue());
    }
  };

  private void updateControls()
  {
    // Spezialfeld-ComboBox aktualisieren
    mailMergeField.setMailMergeDatasource(mailMerge.getDs());
    XNumericField currentDatasourceCountText = UNO.XNumericField(printCount);
    currentDatasourceCountText
        .setMax(mailMerge.getDs().hasDatasource() ? mailMerge.getDs().getNumberOfDatasets() : 0);

    boolean active = mailMerge.getDs() != null && mailMerge.getDs().hasDatasource();
    XWindow preview = UNO.XWindow(layout.getControl("btnPreview"));
    preview.setEnable(active);
    XWindow print = UNO.XWindow(layout.getControl("btnPrint"));
    print.setEnable(active);
    XWindow mailmergeField = UNO.XWindow(layout.getControl("cbSerienbrieffeld"));
    mailmergeField.setEnable(active);
  }

  private void updateVisibleTables(CalcModel model, boolean add)
  {
    List<String> items = new ArrayList<>(Arrays.asList(currentDatasources.getItems()));
    List<String> titles = new ArrayList<>();
    for (String spreadSheetTitle : model.getSpreadSheetTableTitles())
    {
      titles.add(model.getWindowTitle() + " - " + spreadSheetTitle);
    }
    if (add)
    {
      items.addAll(titles);
    } else
    {
      items.removeAll(titles);
    }
    currentDatasources.removeItems((short) 0, currentDatasources.getItemCount());
    currentDatasources.addItems(items.toArray(new String[] {}), (short) 0);
  }

  private AbstractItemListener currentDatasourcesListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      XListBox listbox = UNO.XListBox(arg0.Source);

      mailMerge.getDs().setDatasource(listbox.getSelectedItem());
      updateControls();
    }
  };

  private AbstractActionListener printActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      if (!mailMerge.getDs().hasDatasource())
        return;

      getDocumentController().ifPresent(controller -> {

        MailMergeController c = new MailMergeController(controller, mailMerge.getDs());
        MailmergeWizardController mwController = new MailmergeWizardController(c,
            controller.getModel().doc);
        mwController.startWizard();
        controller.collectNonWollMuxFormFields();
        controller.setFormFieldsPreviewMode(true);
      });
    }
  };
  
  private AbstractNotifier palListener = new AbstractNotifier()
  {
    @Override
    public void notify(String message)
    {
      XListBox currentDatasources = UNO.XListBox(layout.getControl("currentDatasources"));

      List<String> dbTableNames = mailMerge.getDs().getDbTableNames(message);
      mailMerge.getDs().addCachedDbConnection(new DBModel(message, dbTableNames));
      
      currentDatasources.addItems(dbTableNames.toArray(new String[dbTableNames.size()]), 
          (short) (currentDatasources.getItemCount() + 1));
    }
  };

  private AbstractActionListener dbActionListener = new AbstractActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      DBDatasourceDialog oooDSDialog = new DBDatasourceDialog(palListener);
    }
  };

  private AbstractActionListener newCalcTableActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      CalcModel calcModel = mailMerge.getDs().openAndselectNewCalcTableAsDatasource();
      UNO.XCloseable(calcModel.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
      updateVisibleTables(calcModel, true);
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
      updateVisibleTables(model, false);
    }
  };

  private AbstractActionListener fileActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      CalcModel model = mailMerge.getDs().selectFileAsDatasource();
      if (model != null)
      {
        UNO.XCloseable(model.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
        updateVisibleTables(model, true);
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
          getDocumentController().ifPresent(controller -> controller.setFormFieldsPreviewMode(false));
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

          getDocumentController().ifPresent(controller -> {
            controller.collectNonWollMuxFormFields();
            controller.setFormFieldsPreviewMode(true);
          });
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

  private Optional<TextDocumentController> getDocumentController()
  {
    XTextDocument doc = UNO.getCurrentTextDocument();
    if (doc == null)
      return Optional.ofNullable(null);
    return Optional.ofNullable(DocumentManager.getTextDocumentController(doc));
  }

}