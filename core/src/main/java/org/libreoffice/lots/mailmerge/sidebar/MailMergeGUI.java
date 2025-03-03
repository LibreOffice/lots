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

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.Selection;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractSidebarPanel;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractWindowListener;
import org.libreoffice.ext.unohelper.ui.layout.HorizontalLayout;
import org.libreoffice.ext.unohelper.ui.layout.Layout;
import org.libreoffice.ext.unohelper.ui.layout.VerticalLayout;
import org.libreoffice.ext.unohelper.ui.GuiFactory;
import org.libreoffice.ext.unohelper.util.UnoConfiguration;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.mailmerge.ConnectionModel;
import org.libreoffice.lots.mailmerge.ConnectionModelListener;
import org.libreoffice.lots.mailmerge.ds.DatasourceModel;
import org.libreoffice.lots.mailmerge.ui.MailMergeField;
import org.libreoffice.lots.mailmerge.ui.SpecialField;
import org.libreoffice.lots.util.L;
import org.libreoffice.lots.util.Utils;

/**
 * The content of the sidebar.
 */
public class MailMergeGUI extends AbstractSidebarPanel implements XToolPanel, XSidebarPanel, ConnectionModelListener
{

  private MailMergeController controller;
  private Layout layout;
  private XComponentContext context;
  private XWindow window;
  private XControlContainer controlContainer;
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
   * @param resourceUrl
   *          The resource description
   * @param controller
   *          The controller of the panel.
   * @param context
   *          The context.
   * @param parentWindow
   *          The parent window.
   */
  public MailMergeGUI(String resourceUrl, MailMergeController controller, XComponentContext context,
      XWindow parentWindow)
  {
    super(resourceUrl);
    panel = this;
    this.controller = controller;
    this.context = context;
    layout = new VerticalLayout(5, 5, 5, 5, 15);
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
        controller.setPreviewMode(false);
        ConnectionModel.addOpenCalcWindows();
      }

      @Override
      public void windowHidden(EventObject event)
      {
        //
      }
    };
    parentWindow.addWindowListener(windowAdapter);
    xMCF = UNO.XMultiComponentFactory(context.getServiceManager());
    window = parentWindow;
    XWindowPeer parentWindowPeer = UNO.XWindowPeer(parentWindow);
    XToolkit parentToolkit = parentWindowPeer.getToolkit();
    controlContainer = UNO
        .XControlContainer(GuiFactory.createControlContainer(xMCF, context, new Rectangle(0, 0, 0, 0), null));
    UNO.XControl(controlContainer).createPeer(parentToolkit, parentWindowPeer);
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    // TODO: the following is wrong, since it doesn't respect
    // i_rParentAccessible. In
    // a real extension, you should implement this correctly :)
    return UNO.XAccessible(getWindow());
  }

  @Override
  public XWindow getWindow()
  {
    return UNO.XWindow(controlContainer);
  }

  @Override
  public LayoutSize getHeightForWidth(int width)
  {
    int height = layout.getHeightForWidth(width);
    return new LayoutSize(height, height, height);
  }

  @Override
  public int getMinimalWidth()
  {
    try
    {
      int maxWidth = (int) UnoConfiguration.getConfiguration("org.openoffice.Office.UI.Sidebar/General",
          "MaximumWidth");
      return layout.getMinimalWidth(maxWidth);
    } catch (UnoHelperException e)
    {
      return 300;
    }
  }

  @Override
  public void dispose()
  {
    ConnectionModel.removeListener(this);
    controller.dispose();
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
  public void activateControls(DatasourceModel ds, int max, boolean hasUnmappedFields, boolean canAddColumns)
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
    Utils.setProperty(preview.getModel(), UnoProperty.STATE, (short) (enabled ? 1 : 0));
    XTextComponent currentDatasourceCountText = UNO.XTextComponent(printCount);
    String maxText = String.valueOf(max);
    currentDatasourceCountText.setText(maxText);
    currentDatasourceCountText.setSelection(new Selection(maxText.length(), maxText.length()));
    UNO.XWindow(jumpToLast).setEnable(enabled);
    UNO.XWindow(jumpToFirst).setEnable(enabled);
    UNO.XWindow(printCount).setEnable(enabled);
    UNO.XButton(preview).setLabel(enabled ? L.m("<<Field names>>") : L.m("Preview"));
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
    preview = GuiFactory.createButton(xMCF, context, L.m("Preview"), controller::togglePreview,
        new Rectangle(0, 0, 100, 32), props);
    UNO.XWindow(preview).setEnable(false);
    controlContainer.addControl("preview", preview);
    hLayout.addControl(preview, 4);

    jumpToFirst = GuiFactory.createButton(xMCF, context, "<<", controller::jumpToFirstRecord,
        new Rectangle(0, 0, 30, 32), null);
    UNO.XWindow(jumpToFirst).setEnable(false);
    controlContainer.addControl("jumpToFirst", jumpToFirst);
    hLayout.addControl(jumpToFirst);

    printCount = GuiFactory.createSpinField(xMCF, context, 1, controller::updateCurrentRecord,
        new Rectangle(0, 0, 70, 32), null);
    XNumericField nf = UNO.XNumericField(printCount);
    nf.setMin(1);
    nf.setDecimalDigits((short) 0);
    UNO.XWindow(printCount).setEnable(false);
    controlContainer.addControl("printCount", printCount);
    hLayout.addControl(printCount, 4);

    jumpToLast = GuiFactory.createButton(xMCF, context, ">>", controller::jumpToLastRecord, new Rectangle(0, 0, 30, 32),
        null);
    UNO.XWindow(jumpToLast).setEnable(false);
    controlContainer.addControl("jumpToLast", jumpToLast);
    hLayout.addControl(jumpToLast);

    layout.addLayout(hLayout, 1);
  }

  /**
   * Create the controls for changing the data source.
   */
  private void addDatasourcesControls()
  {
    Layout vLayout = new VerticalLayout();
    XControl chooseLabel = GuiFactory.createLabel(xMCF, context, L.m("Choose data source"),
        new Rectangle(0, 0, 100, 20), null);
    controlContainer.addControl("chooseLabel", chooseLabel);
    vLayout.addControl(chooseLabel);

    XControl dsLine = GuiFactory.createLine(xMCF, context, new Rectangle(0, 0, 100, 1), null);
    controlContainer.addControl("dsLine", dsLine);
    vLayout.addControl(dsLine);

    XControl file = GuiFactory.createButton(xMCF, context, L.m("File ..."), controller::selectFileAsDatasource,
        new Rectangle(0, 0, 100, 32), null);
    controlContainer.addControl("file", file);
    vLayout.addControl(file);

    XControl newCalc = GuiFactory.createButton(xMCF, context, L.m("New Calc Sheet"),
        controller::openAndselectNewCalcTableAsDatasource, new Rectangle(0, 0, 100, 32), null);
    controlContainer.addControl("dsList", newCalc);
    vLayout.addControl(newCalc);

    XControl database = GuiFactory.createButton(xMCF, context, L.m("Database ..."), controller::openDatasourceDialog,
        new Rectangle(0, 0, 100, 32), null);
    controlContainer.addControl("database", database);
    vLayout.addControl(database);

    XControl dsList = GuiFactory.createListBox(xMCF, context, controller::changeDatasource,
        new Rectangle(0, 0, 100, 200), null);
    currentDatasources = UNO.XListBox(dsList);
    controlContainer.addControl("dsList", dsList);
    vLayout.addControl(dsList, 6);
    layout.addLayout(vLayout, 1);
  }

  /**
   * Create the controls to modify a data source.
   */
  private void addTableControls()
  {
    Layout vLayout = new VerticalLayout();

    editTable = GuiFactory.createButton(xMCF, context, L.m("Edit sheet"), controller::editDatasource,
        new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(editTable).setEnable(false);
    controlContainer.addControl("editTable", editTable);
    vLayout.addControl(editTable);

    addColumns = GuiFactory.createButton(xMCF, context, L.m("Complement table columns"), controller::showAddColumnsDialog,
        new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(addColumns).setEnable(false);
    controlContainer.addControl("addColumns", addColumns);
    vLayout.addControl(addColumns);

    changeAll = GuiFactory.createButton(xMCF, context, L.m("Adjust all fields"), controller::showModifyDocDialog,
        new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(changeAll).setEnable(false);
    controlContainer.addControl("changeAll", changeAll);
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

    XControl mailMergeLabel = GuiFactory.createLabel(xMCF, context, L.m("Mail merge field"),
        new Rectangle(0, 0, 100, 32), null);
    controlContainer.addControl("mailMergeLabel", mailMergeLabel);
    hLayout.addControl(mailMergeLabel, 4);

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("Dropdown", true);
    mailmergeBox = UNO.XComboBox(GuiFactory.createCombobox(xMCF, context, "", controller::addMailMergeField,
        null, new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(mailmergeBox).setEnable(false);
    mailMergeField = new MailMergeField(mailmergeBox);
    controlContainer.addControl("mailmergeBox", UNO.XControl(mailmergeBox));
    hLayout.addControl(UNO.XControl(mailmergeBox), 6);
    vLayout.addLayout(hLayout, 1);

    Layout hLayout2 = new HorizontalLayout();
    XControl specialLabel = GuiFactory.createLabel(xMCF, context, L.m("Special field"),
        new Rectangle(0, 0, 100, 32), null);
    controlContainer.addControl("specialLabel", specialLabel);
    hLayout2.addControl(specialLabel, 4);

    props = new TreeMap<>();
    props.put("Dropdown", true);
    specialBox = UNO.XComboBox(
        GuiFactory.createCombobox(xMCF, context, "", controller::addSpecialField, null, new Rectangle(0, 0, 80, 32), props));
    UNO.XWindow(specialBox).setEnable(false);
    SpecialField.addItems(specialBox);
    controlContainer.addControl("specialBox", UNO.XControl(specialBox));
    hLayout2.addControl(UNO.XControl(specialBox), 6);

    vLayout.addLayout(hLayout2, 1);
    layout.addLayout(vLayout, 1);
  }

  /**
   * Create the control for printing.
   */
  private void addPrintControls()
  {
    print = GuiFactory.createButton(xMCF, context, L.m("Print ..."), controller::print, new Rectangle(0, 0, 100, 32), null);
    UNO.XWindow(print).setEnable(false);
    controlContainer.addControl("print", print);
    layout.addControl(print);
  }
}
