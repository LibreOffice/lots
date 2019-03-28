package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.ArrayList;
import java.util.List;
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
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
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
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XCloseable;

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
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource.CalcModel;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource.SOURCE_TYPE;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeField;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailmergeWizardController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.SpecialField;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
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
  private MailMergeNew mailMerge;

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {

    @Override
    public void windowShown(EventObject event)
    {
      mailMerge = DocumentManager.getDocumentManager()
          .getCurrentMailMergeNew(getDocumentController().getModel().doc);
      if (mailMerge == null)
      {
        mailMerge = new MailMergeNew(getDocumentController(), actionEvent -> {
          if (actionEvent.getSource() instanceof MailMergeNew)
            WollMuxEventHandler.getInstance().handleMailMergeNewReturned(getDocumentController());
        });
        DocumentManager.getDocumentManager()
            .setCurrentMailMergeNew(getDocumentController().getModel().doc, mailMerge);
      }
      updateControls();
    }

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

      window.setEnable(true);
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
          getDocumentController().insertMailMergeFieldAtCursorPosition(name);
          UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
        }
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
          // mailMergeDatasource.insertFieldFromTrafoDialog(mailMergeDatasource.getColumnNames(),
          // listBox.getText(), genderConf);

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
          // TODO: //insertFieldFromTrafoDialog(mailMergeDatasource.getColumnNames(),
          // listBox.getItem((short) 2), ifConf);
          break;
        case 3:
          getDocumentController().insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_DATENSATZNUMMER);
          break;
        case 4:
          getDocumentController().insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_SERIENBRIEFNUMMER);
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

        UNO.XListBox(comboBox).selectItemPos((short) 0, true);
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
    UNO.XNumericField(spinfield).setMin(0);

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
    UNO.XListBox(currentDatasources.getXControl()).addItemListener(currentDatasourcesListener);

    datasourceControls.add(labelSelectSource);
    datasourceControls.add(hLine);
    datasourceControls.add(file);
    datasourceControls.add(newCalcTable);
    datasourceControls.add(db);
    datasourceControls.add(currentDatasources);

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
    XNumericField currentDatasourceCountText = UNO
        .XNumericField(layout.getControl("currentDocument"));
    currentDatasourceCountText.setMax(
        mailMerge.getDs().hasDatasource() ? mailMerge.getDs().getNumberOfDatasets() : 0);
    XWindow preview = UNO.XWindow(layout.getControl("btnPreview"));
    preview.setEnable(mailMerge.getDs().hasDatasource());
    XWindow print = UNO.XWindow(layout.getControl("btnPrint"));
    print.setEnable(mailMerge.getDs().hasDatasource());
  }
  
  private void updateVisibleTables()
  {
    XListBox currentDatasources = UNO.XListBox(layout.getControl("currentDatasources"));
    currentDatasources.removeItems((short) 0, currentDatasources.getItemCount());
    List<CalcModel> win = mailMerge.getDs().getOpenCalcWindows();
    if (win.isEmpty())
    {
      UNO.XWindow(currentDatasources).setVisible(false);
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
      currentDatasources.addItems(titles.toArray(new String[titles.size()]), (short) 0);
    }
  }

  private AbstractItemListener currentDatasourcesListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      XListBox listbox = UNO.XListBox(arg0.Source);

      mailMerge.getDs().setDatasource(SOURCE_TYPE.CALC);
      mailMerge.getDs().setTable(listbox.getSelectedItem());
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

      MailMergeController c = new MailMergeController(getDocumentController(), mailMerge.getDs());
      MailmergeWizardController controller = new MailmergeWizardController(c,
          getDocumentController().getModel().doc);
      controller.startWizard();
    }
  };

  private AbstractActionListener dbActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      // mailMergeDatasource.selectOOoDatasourceAsDatasource();
      updateVisibleTables();
    }
  };

  private AbstractActionListener newCalcTableActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      CalcModel calcModel = mailMerge.getDs().openAndselectNewCalcTableAsDatasource();

      XControl currentDatasources = layout.getControl("currentDatasources");
      UNO.XWindow(currentDatasources).setVisible(true);
      updateVisibleTables();
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
      updateVisibleTables();
    }
  };

  private AbstractActionListener fileActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      UNO.XWindow(layout.getControl("currentDatasources")).setVisible(true);
      mailMerge.getDs().setDatasource(SOURCE_TYPE.CALC);
      XListBox listbox = UNO.XListBox(layout.getControl("currentDatasources"));
      short items = listbox.getItemCount();
      List<CalcModel> result = mailMerge.getDs().selectFileAsDatasource();

      result.parallelStream().forEach(model -> {
        for (String sheatName : model.getSpreadSheetTableTitles())
        {
          listbox.addItem(model.getWindowTitle() + " - " + sheatName,
              (short) (listbox.getItemCount() + 1));
        }

        XCloseable close = UNO.XCloseable(model.getSpreadSheetDocument());
        close.addCloseListener(documentCloseListener);
      });
      listbox.selectItemPos((short) (items + 1), true);
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

      mailMerge.getDs().updatePreviewFields(1);
    }
  };

  private AbstractActionListener jumpToLastActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      int datasetCount = mailMerge.getDs().getNumberOfDatasets();

      XTextComponent currentDatasourceCountText = UNO
          .XTextComponent(layout.getControl("currentDocument"));
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
          getDocumentController().setFormFieldsPreviewMode(false);
          UNO.XWindow(layout.getControl("btnJumpToLast")).setEnable(false);
          UNO.XWindow(layout.getControl("btnJumpToFirst")).setEnable(false);
          UNO.XWindow(layout.getControl("currentDocument")).setEnable(false);
        } else if (toggleState == 1)
        {
          if (!mailMerge.getDs().hasDatasource())
            return;

          getDocumentController().collectNonWollMuxFormFields();
          getDocumentController().setFormFieldsPreviewMode(true);
          mailMerge.getDs().updatePreviewFields(1);
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
    return DocumentManager.getTextDocumentController(UNO.getCurrentTextDocument());
  }

}