package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XCloseable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.HashableComponent;
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
import de.muenchen.allg.itd51.wollmux.dialog.MailmergeWizardController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource.CalcModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource.SOURCE_TYPE;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.Info;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class SeriendruckSidebar implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SeriendruckSidebar.class);
  private XWindow parentWindow;
  private XWindow window;
  private SimpleDialogLayout layout;
  private XToolkit toolkit;
  private MailMergeDatasource mailMergeDatasource;

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      layout.draw();
    }
  };

  private XControlModel unoControlContainerModel = null;
  private XControl dialogControl = null;

  public SeriendruckSidebar(XComponentContext context, XWindow parentWindow)
  {
    this.parentWindow = parentWindow;
    this.parentWindow.addWindowListener(this.windowAdapter);

    this.mailMergeDatasource = new MailMergeDatasource();

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
      window.setEnable(true);
      window.setVisible(true);

      this.parentWindow.setVisible(true);

      layout = new SimpleDialogLayout(window);
      layout.setMarginTop(20);
      layout.setMarginLeft(10);

      layout.addControlsToList(addPreviewControls());
      layout.addControlsToList(addDatasourcesControls());
      layout.addControlsToList(addSerienbriefFeld());
      layout.addControlsToList(addSpezialfeld());
      layout.addControlsToList(addPrintControls());

      window.setVisible(true);
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
    UNO.XComboBox(cbSerienbrieffeld.getXControl()).addItemListener(mailMergeItemListener);

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
    comboBox.addItems(new String[] { "Gender", "Wenn...Dann...Sonst", "Datensatznummer",
        "Serienbriefnummer", "Feld bearbeiten..." }, (short) 0);
    comboBox.addItemListener(specialItemListener);

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
    UNO.XButton(previewBtn.getXControl()).addActionListener(previewActionListener);

    ControlProperties jumpToFirstBtn = new ControlProperties(ControlType.BUTTON, "btnJumpToFirst");
    jumpToFirstBtn.setControlPercentSize(10, 30);
    jumpToFirstBtn.setLabel("<");
    jumpToFirstBtn.setEnabled(false);
    UNO.XButton(jumpToFirstBtn.getXControl()).addActionListener(jumpToFirstActionListener);

    ControlProperties printCountField = new ControlProperties(ControlType.NUMERIC_FIELD,
        "currentDocument");
    printCountField.setControlPercentSize(40, 30);
    printCountField.setBorder((short) 2);
    printCountField.setBorderColor(666666);
    printCountField.setSpinEnabled(Boolean.TRUE);
    printCountField.setValue(1);
    printCountField.setDecimalAccuracy((short) 0);
    printCountField.setEnabled(false);
    XSpinField spinfield = UNO.XSpinField(printCountField.getXControl());
    UNO.XTextComponent(spinfield).addTextListener(documentCountFieldListener);

    ControlProperties jumpToLastBtn = new ControlProperties(ControlType.BUTTON, "btnJumpToLast");
    jumpToLastBtn.setControlPercentSize(10, 30);
    jumpToLastBtn.setLabel(">");
    jumpToLastBtn.setEnabled(false);
    UNO.XButton(jumpToLastBtn.getXControl()).addActionListener(jumpToLastActionListener);

    navigationControls.add(previewBtn);
    navigationControls.add(jumpToFirstBtn);
    navigationControls.add(printCountField);
    navigationControls.add(jumpToLastBtn);

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

    ControlProperties currentDatasources = new ControlProperties(ControlType.LIST_BOX,
        "currentDatasources");
    currentDatasources.setControlPercentSize(100, 100);
    List<CalcModel> win = mailMergeDatasource.getOpenCalcWindows();

    if (win.isEmpty())
    {
      UNO.XWindow(currentDatasources.getXControl()).setVisible(false);
    } else
    {
      List<String> titles = new ArrayList<>();
      win.parallelStream().forEach(item -> {
        for (String spreadSheetTitle : item.getSpreadSheetTableTitles())
        {
          titles.add(item.getWindowTitle() + " - " + spreadSheetTitle);
        }
       
        XCloseable close = UNO.XCloseable(item.getSpreadSheetDocument());
        close.addCloseListener(documentCloseListener);
      });

      UNO.XListBox(currentDatasources.getXControl())
          .addItems(titles.toArray(new String[titles.size()]), (short) 0);
    }
    UNO.XListBox(currentDatasources.getXControl()).addItemListener(currentDatasourcesListener);

    ControlProperties mailmerge = new ControlProperties(ControlType.LIST_BOX, "mailmerge");
    mailmerge.setControlPercentSize(100, 30);
    UNO.XListBox(mailmerge.getXControl()).addItemListener(mailMergeItemListener);
    UNO.XWindow(mailmerge.getXControl()).setVisible(false);

    ControlProperties special = new ControlProperties(ControlType.LIST_BOX, "special");
    special.setControlPercentSize(100, 30);
    UNO.XListBox(special.getXControl()).addItemListener(specialItemListener);
    UNO.XWindow(special.getXControl()).setVisible(false);

    datasourceControls.add(labelSelectSource);
    datasourceControls.add(hLine);
    datasourceControls.add(file);
    datasourceControls.add(newCalcTable);
    datasourceControls.add(db);
    datasourceControls.add(currentDatasources);
    datasourceControls.add(mailmerge);
    datasourceControls.add(special);

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
      mailMergeDatasource.updatePreviewFields((int) UNO.XNumericField(arg0.Source).getValue());
    }
  };

  private AbstractItemListener currentDatasourcesListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      XListBox listbox = UNO.XListBox(arg0.Source);

      mailMergeDatasource.setDatasource(SOURCE_TYPE.CALC);
      mailMergeDatasource.setTable(listbox.getSelectedItem());

      // Spezialfeld-ComboBox aktualisieren
      XControl cbSerienbrieffeld = layout.getControl("cbSerienbrieffeld");
      List<String> columnNames = mailMergeDatasource.getColumnNames();
      XComboBox serienbriefFeld = UNO.XComboBox(cbSerienbrieffeld);
      serienbriefFeld.removeItems((short) 0, serienbriefFeld.getItemCount());
      serienbriefFeld.addItems(columnNames.toArray(new String[columnNames.size()]), (short) 0);
    }
  };

  private AbstractItemListener specialItemListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      XListBox listBox = UNO.XListBox(event.Source);

      LOGGER.debug("special {}", event.Selected);
      if (event.Selected == 0)
        return;

      switch (event.Selected)
      {
      case 0:
        break;
      case 1:
        // ConfigThingy für leere Gender-Funktion zusammenbauen.
        ConfigThingy genderConf = GenderDialog
            .generateGenderTrafoConf(mailMergeDatasource.getColumnNames().get(0), "", "", "");
        mailMergeDatasource.insertFieldFromTrafoDialog(mailMergeDatasource.getColumnNames(),
            listBox.getSelectedItem(), genderConf);
        break;
      case 2:
        // ConfigThingy für leere WennDannSonst-Funktion zusammenbauen. Aufbau:
        // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
        ConfigThingy ifConf = new ConfigThingy("IF");
        ConfigThingy strCmpConf = ifConf.add("STRCMP");
        strCmpConf.add("VALUE").add(mailMergeDatasource.getColumnNames().get(0));
        strCmpConf.add("");
        ifConf.add("THEN").add("");
        ifConf.add("ELSE").add("");
        // TODO: //insertFieldFromTrafoDialog(mailMergeDatasource.getColumnNames(),
        // listBox.getItem((short) 2), ifConf);
        break;
      case 3:
        getDocumentController()
            .insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_DATENSATZNUMMER);
        break;
      case 4:
        getDocumentController()
            .insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_SERIENBRIEFNUMMER);
        break;
      case 5:
        getDocumentController().insertNextDatasetFieldAtCursorPosition();
        break;
      case 6:
        // editFieldDialog.show(L.m("Spezialfeld bearbeiten"), myFrame);
        break;
      default:
        break;
      }

      listBox.selectItemPos((short) 0, true);
    }
  };

  private AbstractItemListener mailMergeItemListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      LOGGER.debug("mailmerge {}", event.Selected);
      if (event.Selected == 0)
        return;

      String name = UNO.XComboBox(event.Source).getItem((short) event.Selected);
      getDocumentController().insertMailMergeFieldAtCursorPosition(name);
    }
  };

  private AbstractActionListener printActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      if (!mailMergeDatasource.hasDatasource())
        return;

      MailMergeParams mailMergeParams = new MailMergeParams();
      MailmergeWizardController controller = new MailmergeWizardController(mailMergeParams);
      controller.createWizard();
    }
  };

  private AbstractActionListener dbActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      // mailMergeDatasource.selectOOoDatasourceAsDatasource();
    }
  };

  private AbstractActionListener newCalcTableActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      CalcModel calcModel = mailMergeDatasource.openAndselectNewCalcTableAsDatasource();

      XControl currentDatasources = layout.getControl("currentDatasources");
      UNO.XWindow(currentDatasources).setVisible(true);
      XListBox currentDatasourcesListBox = UNO.XListBox(currentDatasources);

      for (String spreadSheetTitle : calcModel.getSpreadSheetTableTitles())
      {
        currentDatasourcesListBox.addItem(calcModel.getWindowTitle() + " - " + spreadSheetTitle, 
            (short) (currentDatasourcesListBox.getItemCount() + 1));
        
        XCloseable close = UNO.XCloseable(calcModel.getSpreadSheetDocument());
        close.addCloseListener(documentCloseListener);
      }
    }
  };
  
  private AbstractCloseListener documentCloseListener = new AbstractCloseListener()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      XSpreadsheetDocument doc = UNO.XSpreadsheetDocument(arg0.Source);

      String title = UNO.getPropertyByPropertyValues(UNO.XModel(doc).getArgs(), "Title");
      
      if (title == null) {
        LOGGER.debug("SeriendruckSidebar: documentCloseListener: title is NULL."
            + " Could not close Window successfully.");
        return;
      }
      
      XListBox currentDatasources = UNO.XListBox(layout.getControl("currentDatasources"));
      
      int index = 0;
      for (String item : currentDatasources.getItems()) {
        for (String sheet : doc.getSheets().getElementNames())
        {
          if (item.contains(sheet) && item.contains(title)) 
          {
            currentDatasources.removeItems((short) index, (short) 1);
            break;
          }
        }
        index++;
      }
    }
  };

  private AbstractActionListener fileActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      UNO.XWindow(layout.getControl("currentDatasources")).setVisible(true);
      mailMergeDatasource.setDatasource(SOURCE_TYPE.CALC);
      XListBox listbox = UNO.XListBox(layout.getControl("currentDatasources"));
      List<CalcModel> result = mailMergeDatasource.selectFileAsDatasource();

      result.parallelStream().forEach(model -> {
        for (String sheatName : model.getSpreadSheetTableTitles())
        {
          listbox.addItem(model.getWindowTitle() + " - " + sheatName,
              (short) (listbox.getItemCount() + 1));
        }
        
        XCloseable close = UNO.XCloseable(model.getSpreadSheetDocument());
        close.addCloseListener(documentCloseListener);
      });
    }
  };

  private AbstractActionListener jumpToFirstActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XTextComponent currentDatasourceCountText = UNO
          .XTextComponent(layout.getControl("currentDocument"));
      currentDatasourceCountText.setText("1");

      mailMergeDatasource.updatePreviewFields(1);
    }
  };

  private AbstractActionListener jumpToLastActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      int datasetCount = mailMergeDatasource.getNumberOfDatasets();

      XTextComponent currentDatasourceCountText = UNO
          .XTextComponent(layout.getControl("currentDocument"));
      currentDatasourceCountText.setText(String.valueOf(datasetCount));

      mailMergeDatasource.updatePreviewFields(datasetCount);
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
          getDocumentController().setFormFieldsPreviewMode(false);
          UNO.XWindow(layout.getControl("btnJumpToLast")).setEnable(false);
          UNO.XWindow(layout.getControl("btnJumpToFirst")).setEnable(false);
          UNO.XWindow(layout.getControl("currentDocument")).setEnable(false);
        } else if (toggleState == 1)
        {
          if (!mailMergeDatasource.hasDatasource())
            return;

          getDocumentController().collectNonWollMuxFormFields();
          getDocumentController().setFormFieldsPreviewMode(true);
          mailMergeDatasource.updatePreviewFields(1);
          UNO.XTextComponent(layout.getControl("currentDocument")).setText("1");

          UNO.XWindow(layout.getControl("btnJumpToLast")).setEnable(true);
          UNO.XWindow(layout.getControl("btnJumpToFirst")).setEnable(true);
          UNO.XWindow(layout.getControl("currentDocument")).setEnable(true);
        }
      } catch (UnknownPropertyException | WrappedTargetException e)
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
    return 300; // crash
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

  private TextDocumentController getDocumentController()
  {
    Map<HashableComponent, Info> documentInfo = DocumentManager.getDocumentManager()
        .getTextDocumentList();

    // TODO: controller des aktuell verwendeten dokumentes holen
    Info info = null;
    for (Info docInfo : documentInfo.values())
    {
      if (docInfo.getTextDocumentController() != null)
        info = docInfo;
    }

    return info.getTextDocumentController();
  }

}