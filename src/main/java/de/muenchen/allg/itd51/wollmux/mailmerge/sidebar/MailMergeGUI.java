package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
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
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.mailmerge.ConnectionModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ConnectionModelListener;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.MailMergeField;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.SpecialField;
import de.muenchen.allg.itd51.wollmux.sidebar.GuiFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.Layout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.VerticalLayout;

/**
 * The content of the sidebar.
 */
public class MailMergeGUI extends ComponentBase
    implements XToolPanel, XSidebarPanel, ConnectionModelListener
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeGUI.class);

  private MailMergeController controller;
  private Layout layout;
  private XComponentContext context;
  private XWindow window;
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

  /**
   * Create a new panel.
   *
   * @param controller
   *          The controller of the panel.
   * @param context
   *          The context.
   * @param parentWindow
   *          The parent window.
   */
  public MailMergeGUI(MailMergeController controller, XComponentContext context,
      XWindow parentWindow)
  {
    this.controller = controller;
    this.context = context;
    layout = new VerticalLayout(5, 15);
    AbstractWindowListener windowAdapter = new AbstractWindowListener()
    {
      @Override
      public void windowResized(WindowEvent e)
      {
        layout.layout(parentWindow.getPosSize());
      }

      @Override
      public void windowShown(EventObject event)
      {
        ConnectionModel.addOpenCalcWindows();
        controller.showPreviewFiedls(false);
      }

      @Override
      public void windowHidden(EventObject event)
      {
        controller.showPreviewFiedls(true);
      }
    };
    parentWindow.addWindowListener(windowAdapter);
    xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
    XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    // TODO: the following is wrong, since it doesn't respect
    // i_rParentAccessible. In
    // a real extension, you should implement this correctly :)
    return UnoRuntime.queryInterface(XAccessible.class, getWindow());
  }

  @Override
  public XWindow getWindow()
  {
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
    ConnectionModel.removeListener(this);
    controller.dispose();
    super.dispose();
  }

  @Override
  public void connectionsChanged()
  {
    String selected = currentDatasources.getSelectedItem();
    currentDatasources.removeItems((short) 0, currentDatasources.getItemCount());
    List<String> items = ConnectionModel.getConnections();
    currentDatasources.addItems(items.toArray(new String[items.size()]), (short) 0);
    currentDatasources.selectItem(selected, true);
  }

  /**
   * Initialize the GUI.
   */
  public void createGUI()
  {
    addPreviewControls();
    addDatasourcesControls();
    addTableControls();
    addFieldControls();
    addPrintControls();
    connectionsChanged();
    ConnectionModel.addListener(this);
    window.setVisible(true);
  }

  /**
   * Activate the controls and initialize according to the data source.
   *
   * @param ds
   *          The data source.
   * @param max
   *          The number of records.
   * @param hasUnmappedFields
   *          Are there fields in the document which have no mapping to a column of the data source.
   * @param canAddColumns
   *          Can new columns be added to the data source.
   */
  public void activateControls(DatasourceModel ds, int max, boolean hasUnmappedFields,
      boolean canAddColumns)
  {
    mailMergeField.setMailMergeDatasource(ds);
    UNO.XNumericField(printCount).setMax(max);
    UNO.XWindow(preview).setEnable(true);
    UNO.XWindow(print).setEnable(true);
    UNO.XWindow(mailmergeBox).setEnable(true);
    UNO.XWindow(specialBox).setEnable(true);
    UNO.XWindow(editTable).setEnable(true);
    UNO.XWindow(changeAll).setEnable(hasUnmappedFields);
    UNO.XWindow(addColumns).setEnable(canAddColumns);
  }

  /**
   * Deactivate the controls.
   *
   * @param ds
   *          The data source.
   */
  public void deactivateControls(DatasourceModel ds)
  {
    mailMergeField.setMailMergeDatasource(ds);
    UNO.XNumericField(printCount).setMax(0);
    UNO.XWindow(preview).setEnable(false);
    UNO.XWindow(print).setEnable(false);
    UNO.XWindow(mailmergeBox).setEnable(false);
    UNO.XWindow(specialBox).setEnable(false);
    UNO.XWindow(editTable).setEnable(false);
    UNO.XWindow(changeAll).setEnable(false);
    UNO.XWindow(addColumns).setEnable(false);
  }

  /**
   * Update all controls belong to the preview controls.
   *
   * @param enabled
   *          If true, the controls are enabled.
   * @param max
   *          The maximum number of records.
   */
  public void updatePreview(boolean enabled, int max)
  {
    XPropertySet propertySet = UNO.XPropertySet(preview.getModel());
    try
    {
      propertySet.setPropertyValue("State", (short) (enabled ? 1 : 0));
    } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException
        | PropertyVetoException | WrappedTargetException e)
    {
      LOGGER.debug("", e);
    }
    XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
    currentDatasourceCountText.setText(String.valueOf(max));
    UNO.XWindow(jumpToLast).setEnable(enabled);
    UNO.XWindow(jumpToFirst).setEnable(enabled);
    UNO.XWindow(printCount).setEnable(enabled);
    UNO.XButton(preview).setLabel(enabled ? "<<Feldnamen>>" : "Vorschau");
  }

  /**
   * Update all controls belong to the field controls.
   *
   * @param enabled
   *          If true, the controls are enabled.
   */
  public void updateFieldControls(boolean enabled)
  {
    UNO.XWindow2(changeAll).setEnable(enabled && UNO.XWindow2(changeAll).isVisible());
    UNO.XWindow2(addColumns).setEnable(enabled && UNO.XWindow2(addColumns).isVisible());
  }

  /**
   * Update the control showing which data source is selected.
   *
   * @param ds
   *          The new data source.
   */
  public void selectDatasource(String ds)
  {
    currentDatasources.selectItem(ds, true);
  }

  /**
   * Create the controls for handling preview.
   */
  private void addPreviewControls()
  {
    Layout hLayout = new HorizontalLayout();
    SortedMap<String, Object> props = new TreeMap<>();
    props.put("Toggle", true);
    preview = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Vorschau",
        controller::togglePreview, new Rectangle(0, 0, 100, 32), props);
    UNO.XWindow(preview).setEnable(false);
    hLayout.addControl(preview, 4);

    jumpToFirst = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "<<",
        controller::jumpToFirstRecord, new Rectangle(0, 0, 30, 32), null);
    UNO.XWindow(jumpToFirst).setEnable(false);
    hLayout.addControl(jumpToFirst);

    printCount = GuiFactory.createSpinField(xMCF, context, toolkit, windowPeer, 1,
        controller::updateCurrentRecord, new Rectangle(0, 0, 70, 32), null);
    XNumericField nf = UNO.XNumericField(printCount);
    nf.setMin(1);
    nf.setDecimalDigits((short) 0);
    UNO.XWindow(printCount).setEnable(false);
    hLayout.addControl(printCount, 4);

    jumpToLast = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, ">>",
        controller::jumpToLastRecord, new Rectangle(0, 0, 30, 32), null);
    UNO.XWindow(jumpToLast).setEnable(false);
    hLayout.addControl(jumpToLast);

    layout.addLayout(hLayout, 1);
  }

  /**
   * Create the controls for changing the data source.
   */
  private void addDatasourcesControls()
  {
    Layout vLayout = new VerticalLayout();
    vLayout.addControl(GuiFactory.createLabel(xMCF, context, toolkit, windowPeer,
        "Datenquelle wählen", new Rectangle(0, 0, 100, 20), null));

    vLayout.addControl(GuiFactory.createHLine(xMCF, context, toolkit, windowPeer,
        new Rectangle(0, 0, 100, 1), null));

    vLayout.addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Datei...",
        controller::selectFileAsDatasource, new Rectangle(0, 0, 100, 32), null));

    vLayout
        .addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Neue Calc-Tabelle",
            controller::openAndselectNewCalcTableAsDatasource, new Rectangle(0, 0, 100, 32), null));

    vLayout.addControl(GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Datenbank",
        controller::openDatasourceDialog, new Rectangle(0, 0, 100, 32), null));

    XControl ctrl = GuiFactory.createListBox(xMCF, context, toolkit, windowPeer,
        controller::changeDatasource, new Rectangle(0, 0, 100, 200), null);
    currentDatasources = UNO.XListBox(ctrl);
    vLayout.addControl(ctrl, 6);
    layout.addLayout(vLayout, 1);
  }

  /**
   * Create the controls to modify a data source.
   */
  private void addTableControls()
  {
    Layout vLayout = new VerticalLayout();

    editTable = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Tabelle bearbeiten",
        controller::editDatasource, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(editTable).setEnable(false);
    vLayout.addControl(editTable);

    addColumns = GuiFactory.createButton(xMCF, context, toolkit, windowPeer,
        "Tabellenspalten ergänzen", controller::showAddColumnsDialog, new Rectangle(0, 0, 100, 32),
        null);
    UNO.XWindow(addColumns).setEnable(false);
    vLayout.addControl(addColumns);

    changeAll = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Alle Felder anpassen",
        controller::showModifyDocDialog, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(changeAll).setEnable(false);
    vLayout.addControl(changeAll);
    layout.addLayout(vLayout, 1);
  }

  /**
   * Create the controls to modify the document.
   */
  private void addFieldControls()
  {
    Layout vLayout = new VerticalLayout();
    Layout hLayout = new HorizontalLayout();

    XControl ctrl = GuiFactory.createLabel(xMCF, context, toolkit, windowPeer, "Serienbrieffeld",
        new Rectangle(0, 0, 100, 32), null);
    hLayout.addControl(ctrl, 4);

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("Dropdown", true);
    mailmergeBox = UNO.XComboBox(GuiFactory.createCombobox(xMCF, context, toolkit, windowPeer, "",
        controller::addMailMergeField, new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(mailmergeBox).setEnable(false);
    mailMergeField = new MailMergeField(mailmergeBox);
    hLayout.addControl(UNO.XControl(mailmergeBox), 6);
    vLayout.addLayout(hLayout, 1);

    Layout hLayout2 = new HorizontalLayout();
    ctrl = GuiFactory.createLabel(xMCF, context, toolkit, windowPeer, "Spezialfeld",
        new Rectangle(0, 0, 100, 32), null);
    hLayout2.addControl(ctrl, 4);

    props = new TreeMap<>();
    props.put("Dropdown", true);
    specialBox = UNO.XComboBox(GuiFactory.createCombobox(xMCF, context, toolkit, windowPeer, "",
        controller::addSpecialField, new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(specialBox).setEnable(false);
    SpecialField.addItems(specialBox);
    hLayout2.addControl(UNO.XControl(specialBox), 6);

    vLayout.addLayout(hLayout2, 1);
    layout.addLayout(vLayout, 1);
  }

  /**
   * Create the control for printing.
   */
  private void addPrintControls()
  {
    print = GuiFactory.createButton(xMCF, context, toolkit, windowPeer, "Drucken",
        controller::print, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(print).setEnable(false);
    layout.addControl(print);
  }
}
