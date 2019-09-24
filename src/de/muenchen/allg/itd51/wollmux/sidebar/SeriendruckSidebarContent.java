package de.muenchen.allg.itd51.wollmux.sidebar;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XNumericField;
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
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractCloseListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
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
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.Layout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.VerticalLayout;

/**
 * Erzeugt das Fenster, dass in der WollMux-Sidebar angezeigt wird. Das Fenster enthält einen Baum
 * zur Auswahl von Vorlagen und darunter eine Reihe von Buttons für häufig benutzte Funktionen.
 *
 */
public class SeriendruckSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SeriendruckSidebarContent.class);

  private XComponentContext context;

  private XWindow parentWindow;

  private XWindow window;

  private Layout layout;

  private XWindowPeer windowPeer;

  private XToolkit toolkit;

  private XMultiComponentFactory xMCF;

  private XControl preview;
  private XControl jumpToLast;
  private XControl jumpToFirst;
  private XControl printCount;
  private XControl editTable;
  private XControl changeAll;
  private XControl addColumns;
  private XControl print;
  private XListBox currentDatasources;
  private XComboBox mailmergeBox;
  private XComboBox specialBox;
  private MailMergeField mailMergeField;

  private MailMergeNew mailMerge;
  private TextDocumentController textDocumentController;

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      layout.layout(parentWindow.getPosSize());
    }
  };

  public SeriendruckSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;

    this.parentWindow.addWindowListener(this.windowAdapter);
    layout = new VerticalLayout(5, 15);

    xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
    XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UnoRuntime.queryInterface(XWindow.class, windowPeer);

    addPreviewControls();
    addDatasourcesControls();
    addTableControls();
    addSerienbriefFeld();
    addPrintControls();

    window.setVisible(true);
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    if (window == null)
    {
      throw new DisposedException("", this);
    }
    // TODO: the following is wrong, since it doesn't respect i_rParentAccessible. In
    // a real extension, you should implement this correctly :)
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

  @Override
  public LayoutSize getHeightForWidth(int width)
  {
    int height = layout.getHeight();
    return new LayoutSize(height, height, height);
  }

  @Override
  public int getMinimalWidth()
  {
    return 300;
  }

  private void addPreviewControls()
  {
    Layout hLayout = new HorizontalLayout();
    SortedMap<String, Object> props = new TreeMap<>();
    props.put("Toggle", true);
    preview = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Vorschau",
        previewActionListener, new Rectangle(0, 0, 100, 32), props);
    UNO.XWindow(preview).setEnable(false);
    hLayout.addControl(preview, 4);

    jumpToFirst = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "<<",
        jumpToFirstActionListener, new Rectangle(0, 0, 30, 32), null);
    UNO.XWindow(jumpToFirst).setEnable(false);
    hLayout.addControl(jumpToFirst);

    printCount = GuiFactory.createSpinField(xMCF, context, toolkit, windowPeer, 1,
        documentCountFieldListener, new Rectangle(0, 0, 70, 32), null);
    XNumericField nf = UNO.XNumericField(printCount);
    nf.setMin(1);
    nf.setDecimalDigits((short) 0);
    UNO.XWindow(printCount).setEnable(false);
    hLayout.addControl(printCount, 4);

    jumpToLast = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, ">>",
        jumpToLastActionListener, new Rectangle(0, 0, 30, 32), null);
    UNO.XWindow(jumpToLast).setEnable(false);
    hLayout.addControl(jumpToLast);

    layout.addLayout(hLayout, 1);
  }

  private void addDatasourcesControls()
  {
    Layout vLayout = new VerticalLayout();
    vLayout
        .addControl(GuiFactory.createLabel(xMCF, context, toolkit, windowPeer, "Datenquelle wählen",
        new Rectangle(0, 0, 100, 20), null));

    vLayout.addControl(GuiFactory.createHLine(xMCF, context, toolkit, windowPeer,
        new Rectangle(0, 0, 100, 1), null));

    vLayout.addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Datei...",
        fileActionListener, new Rectangle(0, 0, 100, 32), null));

    vLayout
        .addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Neue Calc-Tabelle",
        newCalcTableActionListener, new Rectangle(0, 0, 100, 32), null));

    vLayout.addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Datenbank",
        dbActionListener, new Rectangle(0, 0, 100, 32), null));

    XControl ctrl = GuiFactory.createListBox(xMCF, context, toolkit, windowPeer,
        currentDatasourcesListener, new Rectangle(0, 0, 100, 200), null);
    currentDatasources = UNO.XListBox(ctrl);
    vLayout.addControl(ctrl, 6);
    layout.addLayout(vLayout, 1);

    for (CalcModel calcModel : MailMergeDatasource.getOpenCalcWindows())
    {
      updateVisibleTables(calcModel.getWindowTitle(), calcModel.getSpreadSheetTableTitles(), true,
          false);
    }
  }

  private void addTableControls()
  {
    Layout vLayout = new VerticalLayout();
    editTable = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Tabelle bearbeiten",
        editTableActionListener, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(editTable).setEnable(false);
    vLayout.addControl(editTable);

    addColumns = GuiFactory.createButton(xMCF, context, toolkit, windowPeer,
        "Tabellenspalten ergänzen", addTableColumnsActionListener, new Rectangle(0, 0, 100, 32),
        null);
    UNO.XWindow(addColumns).setEnable(false);
    vLayout.addControl(addColumns);

    changeAll = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Alle Felder anpassen",
        changeFieldsActionListener, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(changeAll).setEnable(false);
    vLayout.addControl(changeAll);
    layout.addLayout(vLayout, 1);
  }

  private void addSerienbriefFeld()
  {
    Layout vLayout = new VerticalLayout();
    Layout hLayout = new HorizontalLayout();

    XControl ctrl = GuiFactory.createLabel(xMCF, context, toolkit, windowPeer, "Serienbrieffeld",
        new Rectangle(0, 0, 100, 32), null);
    hLayout.addControl(ctrl, 4);

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("Dropdown", true);
    mailmergeBox = UNO.XComboBox(GuiFactory.createCombobox(xMCF, context, toolkit, windowPeer, "",
        new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(mailmergeBox).setEnable(false);
    mailMergeField = new MailMergeField(mailmergeBox);
    mailmergeBox.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected != 0)
        {
          String name = UNO.XComboBox(event.Source).getItem((short) event.Selected);
          textDocumentController.insertMailMergeFieldAtCursorPosition(name);
          UNO.XTextComponent(mailmergeBox).setText(mailmergeBox.getItem((short) 0));
        }

        textDocumentController.collectNonWollMuxFormFields();
        mailMerge.getDs().updatePreviewFields(mailMerge.getDs().getPreviewDatasetNumber());
      }
    });
    hLayout.addControl(UNO.XControl(mailmergeBox), 6);
    vLayout.addLayout(hLayout, 1);

    Layout hLayout2 = new HorizontalLayout();
    ctrl = GuiFactory.createLabel(xMCF, context, toolkit, windowPeer, "Spezialfeld",
        new Rectangle(0, 0, 100, 32), null);
    hLayout2.addControl(ctrl, 4);

    props = new TreeMap<>();
    props.put("Dropdown", true);
    specialBox = UNO.XComboBox(GuiFactory.createCombobox(xMCF, context, toolkit, windowPeer, "",
        new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(specialBox).setEnable(false);
    SpecialField.addItems(specialBox);
    specialBox.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        switch (event.Selected)
        {
        case 0:
          break;

        case 1:
          new GenderDialog(mailMerge.getDs().getColumnNames(), textDocumentController);
          break;

        case 2:
          new IfThenElseDialog(mailMerge.getDs().getColumnNames(), textDocumentController);
          break;

        case 3:
          textDocumentController
              .insertMailMergeFieldAtCursorPosition(MailMergeController.TAG_DATENSATZNUMMER);
          break;

        case 4:
          textDocumentController
              .insertMailMergeFieldAtCursorPosition(MailMergeController.TAG_SERIENBRIEFNUMMER);
          break;

        case 5:
          textDocumentController.insertNextDatasetFieldAtCursorPosition();
          break;

        default:
          break;
        }

        textDocumentController.collectNonWollMuxFormFields();
        mailMerge.getDs().updatePreviewFields(mailMerge.getDs().getPreviewDatasetNumber());
        UNO.XTextComponent(specialBox).setText(specialBox.getItem((short) 0));
      }
    });
    hLayout2.addControl(UNO.XControl(specialBox), 6);
    vLayout.addLayout(hLayout2, 1);
    layout.addLayout(vLayout, 1);
  }

  private void addPrintControls()
  {
    print = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Drucken",
        printActionListener, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(print).setEnable(false);
    layout.addControl(print);
  }

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

  private void setMailMergeOnDocument()
  {
    textDocumentController = DocumentManager
        .getTextDocumentController(UNO.getCurrentTextDocument());

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
        {
          for (CalcModel model : MailMergeDatasource.getOpenCalcWindows())
          {
            UNO.XCloseable(model.getSpreadSheetDocument())
                .removeCloseListener(documentCloseListener);
          }
          WollMuxEventHandler.getInstance().handleMailMergeNewReturned(textDocumentController);
        }
      });

      DocumentManager.getDocumentManager()
          .setCurrentMailMergeNew(textDocumentController.getModel().doc, mailMerge);
    }
  }

  private ActionListener adjustFieldsFinishListener = e -> {
    boolean hasUnmappedFields = mailMerge.getDs()
        .checkUnmappedFields(mailMerge.getDs().getColumnNames());
    XWindow2 btnChangeAll = UNO.XWindow2(changeAll);
    btnChangeAll.setEnable(btnChangeAll.isVisible() && hasUnmappedFields);
    XWindow2 btnAddColumns = UNO.XWindow2(addColumns);
    btnAddColumns.setEnable(btnAddColumns.isVisible() && hasUnmappedFields);
  };

  private AbstractActionListener editTableActionListener = e -> mailMerge.getDs().toFront();

  private AbstractActionListener addTableColumnsActionListener = e -> AdjustFields
      .showAddMissingColumnsDialog(textDocumentController, mailMerge.getDs(),
          adjustFieldsFinishListener);

  private AbstractActionListener changeFieldsActionListener = e -> AdjustFields
      .showAdjustFieldsDialog(textDocumentController, mailMerge.getDs(),
          adjustFieldsFinishListener);

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

  private AbstractActionListener printActionListener = e -> {
    if (!mailMerge.getDs().hasDatasource())
      return;

    MailMergeController c = new MailMergeController(textDocumentController, mailMerge.getDs());
    MailmergeWizardController mwController = new MailmergeWizardController(c,
        textDocumentController.getModel().doc);
    mwController.startWizard();
    textDocumentController.collectNonWollMuxFormFields();
    textDocumentController.setFormFieldsPreviewMode(true);
  };

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
      UNO.XNumericField(printCount)
          .setMax(mailMerge.getDs().hasDatasource() ? mailMerge.getDs().getNumberOfDatasets() : 0);

      boolean active = mailMerge.getDs() != null && mailMerge.getDs().hasDatasource();
      UNO.XWindow(preview).setEnable(active);
      UNO.XWindow(print).setEnable(active);
      UNO.XWindow(mailmergeBox).setEnable(active);
      UNO.XWindow(specialBox).setEnable(active);
      UNO.XWindow(editTable).setEnable(active);

      boolean hasUnmappedFields = mailMerge.getDs().hasDatasource()
          && mailMerge.getDs().checkUnmappedFields(mailMerge.getDs().getColumnNames());
      UNO.XWindow(addColumns)
          .setEnable(active && hasUnmappedFields && mailMerge.getDs().supportsAddColumns());
      UNO.XWindow(changeAll).setEnable(active && hasUnmappedFields);
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

  private AbstractActionListener dbActionListener = e -> {
    setMailMergeOnDocument();
    new DBDatasourceDialog(dbDatasourceListener);
  };

  private AbstractActionListener newCalcTableActionListener = e -> {
    setMailMergeOnDocument();
    CalcModel calcModel = mailMerge.getDs().openAndselectNewCalcTableAsDatasource();
    UNO.XCloseable(calcModel.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
    updateVisibleTables(calcModel.getWindowTitle(), calcModel.getSpreadSheetTableTitles(), true,
        true);
  };

  private AbstractActionListener fileActionListener = e -> {
    setMailMergeOnDocument();
    CalcModel model = mailMerge.getDs().selectFileAsDatasource();
    if (model != null)
    {
      UNO.XCloseable(model.getSpreadSheetDocument()).addCloseListener(documentCloseListener);
      updateVisibleTables(model.getWindowTitle(), model.getSpreadSheetTableTitles(), true, true);
    }
  };

  private AbstractTextListener documentCountFieldListener = e -> mailMerge.getDs()
      .updatePreviewFields((int) UNO.XNumericField(e.Source).getValue());

  private AbstractActionListener jumpToFirstActionListener = e -> {
    XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
    currentDatasourceCountText.setText("1");
    mailMerge.getDs().updatePreviewFields(1);
  };

  private AbstractActionListener jumpToLastActionListener = e -> {
    int datasetCount = mailMerge.getDs().getNumberOfDatasets();

    XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
    currentDatasourceCountText.setText(String.valueOf(datasetCount));

    mailMerge.getDs().updatePreviewFields(datasetCount);
  };

  private AbstractActionListener previewActionListener = e -> {
    XPropertySet propertySet = UNO.XPropertySet(preview.getModel());

    try
    {
      short toggleState = (short) propertySet.getPropertyValue("State");

      if (toggleState == 0)
      {
        textDocumentController.setFormFieldsPreviewMode(false);
        UNO.XWindow(jumpToLast).setEnable(false);
        UNO.XWindow(jumpToFirst).setEnable(false);
        UNO.XWindow(printCount).setEnable(false);
        UNO.XButton(preview).setLabel("Vorschau");
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
        UNO.XButton(preview).setLabel("<<Feldnamen>>");
      }
    } catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
        | PropertyVetoException ex)
    {
      LOGGER.error("", ex);
    }
  };
}
