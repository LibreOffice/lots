package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
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
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.AbstractNotifier;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.IfThenElseDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentControllerInitialized;
import de.muenchen.allg.itd51.wollmux.mailmerge.ConnectionModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ConnectionModelListener;
import de.muenchen.allg.itd51.wollmux.mailmerge.MailMergeController;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DBDatasourceDialog;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModelListener;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.MailmergeWizardController;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.MailMergeField;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.SpecialField;
import de.muenchen.allg.itd51.wollmux.sidebar.GuiFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.Layout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.VerticalLayout;

/**
 * Erzeugt das Fenster, dass in der WollMux-Sidebar angezeigt wird. Das Fenster enthält einen Baum
 * zur Auswahl von Vorlagen und darunter eine Reihe von Buttons für häufig benutzte Funktionen.
 *
 */
public class SeriendruckSidebarContent extends ComponentBase
    implements XToolPanel, XSidebarPanel, ConnectionModelListener,
    PreviewModelListener, DatasourceModelListener
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
  private boolean disposed = false;

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private Optional<DatasourceModel> datasourceModel;
  private PreviewModel previewModel;
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
    datasourceModel = Optional.empty();
    previewModel = new PreviewModel();
    previewModel.setDatasourceModel(datasourceModel);
    previewModel.addListener(this);

    this.parentWindow.addWindowListener(this.windowAdapter);
    layout = new VerticalLayout(5, 15);

    xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
    XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
    WollMuxEventHandler.getInstance().registerListener(this);
    init();
  }

  /**
   * Sets TextDocumentController once it is available.
   *
   * @param event
   *          OnTextDocumentControllerInitialized-Object with the instance of
   *          TextDocumentController.
   */
  @Subscribe
  public void onTextDocumentControllerInitialized(OnTextDocumentControllerInitialized event)
  {
    TextDocumentController controller = event.getTextDocumentController();

    if (controller == null)
    {
      LOGGER.error("{} notify(): documentController is NULL.", this.getClass().getSimpleName());
      return;
    }

    textDocumentController = controller;

    init();
  }

  /**
   * Initialize the UI and the connection to the current document.
   */
  public void init()
  {
    // An instance of DocumentManager.getTextDocumentController() may exist if another sidebar deck
    // was active when LO was started, see {@link OnTextDocumentControllerInitialized}.
    // If this deck was the active one at startup, this method should be notified by subscribed
    // {@link OnTextDocumentControllerInitialized} in the Constructor of this class which sets
    // textDocumentController instance once it exists.
    if (textDocumentController == null)
    {
      // We can't use this everytime. If this sidebar is the active one at startup,
      // UNO.getCurrentTextDocument() throws NULL due UNO.desktop.getCurrentComponent()
      // is not initialized. If NULL, we return and wait for an notification by
      // {@link OnTextDocumentControllerInitialized}.
      XTextDocument currentDoc = UNO.getCurrentTextDocument();

      if (currentDoc == null)
      {
        LOGGER.error("{} init(): Current Text Document is NULL.", this.getClass().getName());
        return;
      }

      textDocumentController = DocumentManager.getTextDocumentController(currentDoc);
    }

    if (textDocumentController != null)
    {
      openDatasourceFromLastStoredSettings(textDocumentController.getModel().getMailmergeConfig());
      addPreviewControls();
      addDatasourcesControls();
      addTableControls();
      addSerienbriefFeld();
      addPrintControls();
      ConnectionModel.addListener(this);

      textDocumentController.setFormFieldsPreviewMode(false);
      WollMuxEventHandler.getInstance().unregisterListener(this);
      window.setVisible(true);
    } else
    {
      LOGGER.error("SeriendruckSidebar: setMailMergeOnDocument(): textDocumentController is NULL.");
    }
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

  @Override
  public void dispose()
  {
    if (!disposed)
    {
      textDocumentController.setFormFieldsPreviewMode(true);
      ConnectionModel.removeListener(this);
      disposed = true;
    }
    super.dispose();
  }

  @Override
  public void connectionsChanged()
  {
    try
    {
      currentDatasources.removeItems((short) 0, currentDatasources.getItemCount());
      List<String> titles = ConnectionModel.getConnections();
      currentDatasources.addItems(titles.toArray(new String[] {}), (short) 0);
      currentDatasources.selectItem(
          ConnectionModel.buildConnectionName(datasourceModel), true);
    } catch (NoTableSelectedException ex)
    {
      LOGGER.debug("", ex);
    }
  }

  private void activateControls(DatasourceModel ds)
  {
    try
    {
      datasourceModel.ifPresent(mailMergeField::setMailMergeDatasource);

      UNO.XWindow(preview).setEnable(true);
      UNO.XWindow(print).setEnable(true);
      UNO.XWindow(mailmergeBox).setEnable(true);
      UNO.XWindow(specialBox).setEnable(true);
      UNO.XWindow(editTable).setEnable(true);

      UNO.XNumericField(printCount).setMax(ds.getNumberOfRecords());

      boolean hasUnmappedFields = textDocumentController.getModel()
          .getReferencedFieldIDsThatAreNotInSchema(
              new HashSet<>(ds.getColumnNames())).length > 0;
      UNO.XWindow(changeAll).setEnable(hasUnmappedFields);

      ConfigThingy seriendruck = new ConfigThingy("Seriendruck");
      ConfigThingy dq = ds.getSettings();
      if (dq.count() > 0)
      {
	seriendruck.addChild(dq);
      }
      textDocumentController.setMailmergeConfig(seriendruck);
    } catch (NoTableSelectedException ex)
    {
      deactivateControls();
    }
  }

  private void deactivateControls()
  {
    datasourceModel.ifPresent(mailMergeField::setMailMergeDatasource);
    UNO.XNumericField(printCount).setMax(0);
    UNO.XWindow(changeAll).setEnable(false);
    UNO.XWindow(preview).setEnable(false);
    UNO.XWindow(print).setEnable(false);
    UNO.XWindow(mailmergeBox).setEnable(false);
    UNO.XWindow(specialBox).setEnable(false);
    UNO.XWindow(editTable).setEnable(false);
  }

  @Override
  public void previewChanged()
  {
    boolean isPreview = previewModel.isPreview();

    XPropertySet propertySet = UNO.XPropertySet(preview.getModel());
    try
    {
      propertySet.setPropertyValue("State", (short) (isPreview ? 1 : 0));
    } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException
        | PropertyVetoException | WrappedTargetException e)
    {
      LOGGER.debug("", e);
    }
    if (isPreview)
    {
      XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
      currentDatasourceCountText.setText(String.valueOf(previewModel.getPreviewNumber()));

      textDocumentController.collectNonWollMuxFormFields();
      textDocumentController.setFormFieldsPreviewMode(true);

      UNO.XWindow(jumpToLast).setEnable(true);
      UNO.XWindow(jumpToFirst).setEnable(true);
      UNO.XWindow(printCount).setEnable(true);
      UNO.XButton(preview).setLabel("<<Feldnamen>>");

      updatePreviewFields();
    } else
    {
      textDocumentController.setFormFieldsPreviewMode(false);
      UNO.XWindow(jumpToLast).setEnable(false);
      UNO.XWindow(jumpToFirst).setEnable(false);
      UNO.XWindow(printCount).setEnable(false);
      UNO.XButton(preview).setLabel("Vorschau");
    }
  }

  @Override
  public void datasourceChanged()
  {
    datasourceModel.ifPresent(this::activateControls);
  }

  private void changeDatasource(Optional<DatasourceModel> model)
  {
    datasourceModel.ifPresent(ds -> ds.removeDatasourceListener(this));
    datasourceModel = model;
    previewModel.setDatasourceModel(datasourceModel);
    datasourceModel.ifPresent(ds -> ds.addDatasourceListener(this));
    datasourceModel.ifPresentOrElse(this::activateControls,
        this::deactivateControls);
  }

  private void updatePreviewFields()
  {
    try
    {
      for (Map.Entry<String, String> entry : previewModel.getCurrentRecord().entrySet())
      {
        new OnSetFormValue(textDocumentController.getModel().doc, entry.getKey(), entry.getValue(),
            null).emit();
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
        updatePreviewFields();
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
	  datasourceModel.ifPresent(ds -> {
	    try
	    {
	      new GenderDialog(new ArrayList<String>(ds.getColumnNames()),
	          textDocumentController);
	    } catch (NoTableSelectedException ex)
	    {
	      LOGGER.debug("", ex);
	    }
	  });
	  break;

	case 2:
	  datasourceModel.ifPresent(ds -> {
	    try
	    {
	      new IfThenElseDialog(new ArrayList<String>(ds.getColumnNames()),
	          textDocumentController);

	    } catch (NoTableSelectedException ex)
	    {
	      LOGGER.debug("", ex);
	    }
	  });
	  break;

	case 3:
	  textDocumentController
	      .insertMailMergeFieldAtCursorPosition(SetFormValue.TAG_RECORD_ID);
	  break;

	case 4:
	  textDocumentController.insertMailMergeFieldAtCursorPosition(
	      SetFormValue.TAG_MAILMERGE_ID);
	  break;

	case 5:
	  textDocumentController.insertNextDatasetFieldAtCursorPosition();
	  break;

	default:
	  break;
	}

	textDocumentController.collectNonWollMuxFormFields();
	updatePreviewFields();
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

  private void openDatasourceFromLastStoredSettings(ConfigThingy mmconf)
  {
    ConfigThingy datenquelle = new ConfigThingy("");
    String type = "unknown";
    try
    {
      ConfigThingy query = mmconf.query("Datenquelle");
      if (query.count() > 0)
      {
        datenquelle = query.getLastChild();
        type = datenquelle.getString("TYPE", null);

        if ("calc".equalsIgnoreCase(type))
        {
          String url = datenquelle.get("URL").toString();
          String table = datenquelle.get("TABLE").toString();
          XSpreadsheetDocument doc = getCalcDocByFile(url);
          if (doc != null)
          {
	    changeDatasource(ConnectionModel.addAndSelectDatasource(doc,
	        Optional.ofNullable(table)));
          }
        } else if ("ooo".equalsIgnoreCase(type))
        {
          String source = datenquelle.get("SOURCE").toString();
          String table = datenquelle.get("TABLE").toString();
          XOfficeDatabaseDocument ds = loadDataSource(source);
          if (ds != null)
          {
	    changeDatasource(ConnectionModel.addAndSelectDatasource(ds,
	        Optional.ofNullable(table)));
          }
        } else if (type != null)
        {
          LOGGER.error("Ignoriere Datenquelle mit unbekanntem Typ '{}'", type);
        }
      }
    } catch (NodeNotFoundException e)
    {
      LOGGER.error(L.m("Fehlendes Argument für Datenquelle vom Typ '%1':", type), e);
    } catch (NoTableSelectedException ex)
    {
      LOGGER.error("Die Tabelle existiert nicht", ex);
    }
  }

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
   * Öffnet ein neues Calc-Dokument und setzt es als Seriendruckdatenquelle.
   */
  private void openAndselectNewCalcTableAsDatasource()
  {
    LOGGER.debug(L.m("Öffne neues Calc-Dokument als Datenquelle für Seriendruck"));
    try
    {
      XSpreadsheetDocument spread = UNO
          .XSpreadsheetDocument(UNO.loadComponentFromURL("private:factory/scalc", true, true));

      changeDatasource(
          ConnectionModel.addAndSelectDatasource(spread, Optional.empty()));
    } catch (UnoHelperException | NoTableSelectedException e)
    {
      LOGGER.error("", e);
    }
  }

  private void selectFileAsDatasource()
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
        LOGGER.debug(L.m("Öffne %1 als Datenquelle für Seriendruck", files[0]));
        XSpreadsheetDocument doc = getCalcDocByFile(files[0]);

        if (doc != null)
        {
	  changeDatasource(
	      ConnectionModel.addAndSelectDatasource(doc, Optional.empty()));
        }
      } catch (Exception e)
      {
        LOGGER.error("", e);
      }
    }
  }

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
   * Update the controls to modify a data source.
   */
  private void updateDatasourceControls()
  {
    datasourceModel.ifPresentOrElse(ds -> {
      try
      {
	boolean hasUnmappedFields = textDocumentController.getModel()
	    .getReferencedFieldIDsThatAreNotInSchema(
	        ds.getColumnNames()).length > 0;
	UNO.XWindow2(changeAll).setEnable(
	    hasUnmappedFields && UNO.XWindow2(changeAll).isVisible());
	UNO.XWindow2(addColumns).setEnable(
	    hasUnmappedFields && UNO.XWindow2(addColumns).isVisible());
      } catch (NoTableSelectedException ex)
      {
	LOGGER.debug("", ex);
      }
    }, () -> {
      UNO.XWindow2(changeAll).setEnable(false);
      UNO.XWindow2(addColumns).setEnable(false);
    });
  }

  private AbstractActionListener editTableActionListener = e -> datasourceModel
      .ifPresent(DatasourceModel::toFront);

  private AbstractActionListener addTableColumnsActionListener = event -> datasourceModel
      .ifPresent(datasource -> {
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
              .getReferencedFieldIDsThatAreNotInSchema(
                  new HashSet<>(datasource.getColumnNames()));
          List<String> columns = new ArrayList<>(datasource.getColumnNames());

          AdjustFields.showFieldMappingDialog("Tabellenspalten ergänzen",
              L.m("Spalte"), L.m("Vorbelegung"), L.m("Spalten ergänzen"),
              fieldIDs, columns, true, addTableColumnsFinishListener);
        } catch (NoTableSelectedException ex)
        {
          InfoDialog.showInfoModal("", ex.getMessage());
        }
      });

  private AbstractActionListener changeFieldsActionListener = event -> datasourceModel
      .ifPresent(ds -> {
        ActionListener adjustFieldsFinishListener = e -> {
          @SuppressWarnings("unchecked")
          Map<String, FieldSubstitution> mapIdToSubstitution = (HashMap<String, FieldSubstitution>) e
              .getSource();
          textDocumentController.adjustFields(mapIdToSubstitution);
          updateDatasourceControls();
        };
        try
        {
          ReferencedFieldID[] fieldIDs = textDocumentController.getModel()
              .getReferencedFieldIDsThatAreNotInSchema(
                  new HashSet<>(ds.getColumnNames()));
          List<String> columns = new ArrayList<>(ds.getColumnNames());
          AdjustFields.showFieldMappingDialog("Felder anpassen",
              L.m("Altes Feld"), L.m("Neue Belegung"), L.m("Felder anpassen"),
              fieldIDs, columns, false, adjustFieldsFinishListener);
        } catch (NoTableSelectedException ex)
        {
          InfoDialog.showInfoModal("", ex.getMessage());
        }
      });

  private AbstractActionListener printActionListener = e -> datasourceModel
      .ifPresent(ds -> {
        try
        {
          MailMergeController c = new MailMergeController(textDocumentController, ds);
          MailmergeWizardController mwController = new MailmergeWizardController(c,
              textDocumentController);
          mwController.startWizard();
          textDocumentController.collectNonWollMuxFormFields();
          textDocumentController.setFormFieldsPreviewMode(true);
        } catch (NoTableSelectedException ex)
        {
          LOGGER.debug("", ex);
          InfoDialog.showInfoModal("Fehler beim Seriendruck", ex.getMessage());
        }
      });

  private AbstractItemListener currentDatasourcesListener = e -> {
    try
    {
      changeDatasource(ConnectionModel
          .selectDatasource(currentDatasources.getSelectedItem()));
    } catch (NoTableSelectedException e1)
    {
      LOGGER.debug("", e1);
    }
  };

  private AbstractActionListener dbActionListener = e -> new DBDatasourceDialog(
      new AbstractNotifier()
      {
        @Override
        public void notify(String dbName)
        {
          try
          {
	    changeDatasource(ConnectionModel.addAndSelectDatasource(
	        loadDataSource(dbName), Optional.empty()));
          } catch (NoTableSelectedException e)
          {
            LOGGER.debug("", e);
          }
        }
      });

  private AbstractActionListener newCalcTableActionListener = e -> openAndselectNewCalcTableAsDatasource();

  private AbstractActionListener fileActionListener = e -> selectFileAsDatasource();

  private AbstractTextListener documentCountFieldListener = e -> previewModel
      .setPreviewNumber((int) UNO.XNumericField(e.Source).getValue());

  private AbstractActionListener jumpToFirstActionListener = e -> previewModel.setPreviewNumber(1);

  private AbstractActionListener jumpToLastActionListener = e -> {
    try
    {
      previewModel.gotoLastDataset();
    } catch (NoTableSelectedException ex)
    {
      LOGGER.error("", ex);
    }
  };

  private AbstractActionListener previewActionListener = e -> {
    try
    {
      XPropertySet propertySet = UNO.XPropertySet(preview.getModel());
      short toggleState = (short) propertySet.getPropertyValue("State");
      previewModel.setPreview(toggleState == 1);
    } catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
        | NoTableSelectedException ex)
    {
      LOGGER.error("", ex);
    }
  };
}
