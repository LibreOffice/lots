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
package org.libreoffice.lots.form.sidebar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.FocusChangeReason;
import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindow2;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractFocusListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractSidebarPanel;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTabPageContainerListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractWindowListener;
import org.libreoffice.ext.unohelper.ui.layout.ControlLayout;
import org.libreoffice.ext.unohelper.ui.layout.FixedVerticalLayout;
import org.libreoffice.ext.unohelper.ui.layout.HorizontalLayout;
import org.libreoffice.ext.unohelper.ui.layout.Layout;
import org.libreoffice.ext.unohelper.ui.layout.TabLayout;
import org.libreoffice.ext.unohelper.ui.layout.VerticalLayout;
import org.libreoffice.ext.unohelper.ui.GuiFactory;
import org.libreoffice.ext.unohelper.ui.HTMLElement;
import org.libreoffice.ext.unohelper.util.UnoConfiguration;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.form.config.FormConfig;
import org.libreoffice.lots.form.config.TabConfig;
import org.libreoffice.lots.form.model.Control;
import org.libreoffice.lots.form.model.FormModel;
import org.libreoffice.lots.ui.UIElementConfig;
import org.libreoffice.lots.ui.UIElementType;

/**
 * form UI in sidebar.
 */
public class FormSidebarPanel extends AbstractSidebarPanel implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FormSidebarPanel.class);
  private static final int BUTTONS_PER_ROW = 2;
  private XWindow parentWindow;
  private XControlContainer controlContainer;
  private Layout vLayout;
  private XComponentContext context;
  private XMultiComponentFactory xMCF;
  private Map<Control, Short> buttons = new HashMap<>();
  private XTabPageContainer tabControlContainer;
  private FormSidebarController formSidebarController;
  private Map<String, Pair<XControl, XControl>> controls = new HashMap<>();
  private boolean tabChanged = true;

  // --- Lazy tab initialization state ---

  /**
   * Tracks which tab IDs (1-based) have had their controls fully created.
   */
  private final Set<Short> initializedTabs = new HashSet<>();

  /**
   * All tab configs, stored for lazy initialization of non-active tabs.
   */
  private List<TabConfig> allTabConfigs;

  /**
   * The form model, kept for lazy initialization callbacks.
   */
  private FormModel lazyFormModel;

  /**
   * The form config, kept for lazy initialization callbacks.
   */
  private FormConfig lazyFormConfig;

  /**
   * Per-tab VerticalLayouts (pre-created but initially empty for non-active tabs).
   * Index is (tabId - 1).
   */
  private List<Layout> tabContentLayouts = new ArrayList<>();

  /**
   * Creates a new form panel.
   *
   * @param context
   *          the sidebar context.
   * @param parentWindow
   *          parent sidebar window.
   * @param resourceUrl
   *          resource description.
   * @param formSidebarController
   *          {@link FormSidebarController} the form sidebar controller.
   */
  public FormSidebarPanel(XComponentContext context, XWindow parentWindow, String resourceUrl,
      FormSidebarController formSidebarController)
  {
    super(resourceUrl);
    this.context = context;
    this.panel = this;
    this.xMCF = UNO.XMultiComponentFactory(context.getServiceManager());

    vLayout = new VerticalLayout();

    this.parentWindow = parentWindow;
    controlContainer = UNO
        .XControlContainer(GuiFactory.createControlContainer(xMCF, context, new Rectangle(0, 0, 0, 0), null));
    XWindowPeer parentWindowPeer = UNO.XWindowPeer(parentWindow);
    XToolkit parentToolkit = parentWindowPeer.getToolkit();
    UNO.XControl(controlContainer).createPeer(parentToolkit, parentWindowPeer);

    this.formSidebarController = formSidebarController;

    parentWindow.addWindowListener(new AbstractWindowListener()
    {
      @Override
      public void windowResized(WindowEvent e)
      {
        paint();
      }
    });
  }

  /**
   * Paint the tabs and their content.
   */
  public void paint()
  {
    if (tabControlContainer != null)
    {
      short activeTab = tabControlContainer.getActiveTabPageID();
      for (Map.Entry<Control, Short> entry : buttons.entrySet())
      {
        // Guard: the button's control may not exist yet if the tab is lazily uninitialized.
        Pair<XControl, XControl> ctrlPair = controls.get(entry.getKey().getId());
        if (ctrlPair == null)
        {
          continue;
        }
        XWindow window = UNO.XWindow(ctrlPair.getRight());
        if (window != null)
        {
          window.setVisible(entry.getValue() == activeTab && entry.getKey().isVisible());
        }
      }
    }
    vLayout.layout(parentWindow.getPosSize());
  }

  /**
   * Creates the tab control with its content controls.
   *
   * <p>Only the first (default) tab's controls are created eagerly.
   * Controls for all other tabs are created lazily on first activation,
   * which significantly reduces startup time for forms with many tabs.</p>
   *
   * @param config
   *          The UI configuration of the form.
   * @param model
   *          {@link FormModel} FormModel from @{link {@link TextDocumentController}.
   */
  public void createTabControl(FormConfig config, FormModel model)
  {
    if (config != null && model != null)
    {
      // Store references for lazy initialization of non-active tabs.
      this.lazyFormConfig = config;
      this.lazyFormModel = model;
      this.allTabConfigs = config.getTabs();

      XControl tabControl = GuiFactory.createTabPageContainer(xMCF, context);
      controlContainer.addControl("tabControl", tabControl);
      tabControlContainer = UNO.XTabPageContainer(tabControl);

      // Lazy-init listener: when the user switches to a tab, initialize it on demand.
      AbstractTabPageContainerListener listener = event -> {
        short newTabId = tabControlContainer.getActiveTabPageID();
        if (!initializedTabs.contains(newTabId))
        {
          initTabControls(newTabId);
          formSidebarController.applyValuesToTab(newTabId, allTabConfigs.get(newTabId - 1));
        }
        this.paint();
      };
      tabControlContainer.addTabPageContainerListener(listener);

      tabControlContainer = UNO.XTabPageContainer(tabControl);
      Layout buttonLayout = new VerticalLayout(20, 5, 0, 0, 5);

      short tabId = 1;
      List<TabConfig> tabs = allTabConfigs;

      // Phase 1: Create all tab headers and empty content layouts (cheap).
      for (int i = 0; i < tabs.size(); i++)
      {
        TabConfig tab = tabs.get(i);
        HTMLElement element = new HTMLElement(tab.getTitle());
        GuiFactory.createTab(this.xMCF, this.context, UNO.XTabPageContainerModel(tabControl.getModel()),
            element.getText(), (short) (i + 1), 1000);

        // Pre-create an empty layout for each tab; it will be populated lazily.
        tabContentLayouts.add(new VerticalLayout(5, 5, 0, 15, 6));
      }

      // Phase 2: Eagerly initialize only the first tab (index 0, tabId 1).
      if (!tabs.isEmpty())
      {
        initTabControls((short) 1);
      }

      // Phase 3: Create button layouts for all tabs (buttons are always visible at the bottom
      // regardless of which tab is shown, so they must be eager).
      tabId = 1;
      for (int i = 0; i < tabs.size(); i++)
      {
        addButtonsToLayout(tabs.get(i), model, controlContainer, buttonLayout, tabId);
        tabId++;
      }

      Layout tabLayout = new TabLayout(UNO.XTabPageContainer(tabControl), xMCF, context);
      for (Layout l : tabContentLayouts)
      {
        tabLayout.addLayout(l, 1);
      }
      vLayout = new FixedVerticalLayout(tabLayout);
      vLayout.addLayout(buttonLayout, 1);

      tabControlContainer.setActiveTabPageID((short) 1);
    } else
    {
      XControl label = GuiFactory.createLabel(this.xMCF, this.context, "Das Dokument ist kein Formular.",
          new Rectangle(0, 0, 50, 20), null);
      controlContainer.addControl("label", label);
      vLayout.addControl(label);
    }

    paint();
  }

  /**
   * Lazily initializes (creates native UNO controls for) a tab page that has not yet been
   * populated.
   *
   * <p>This is the core of the lazy loading optimization. Each tab's controls are only created
   * (and their native OS peers instantiated) when that tab is first activated by the user.
   * For startup this means only tab 1's controls are created, saving potentially dozens of
   * expensive {@code calcAdjustedSize} round-trips per skipped tab.</p>
   *
   * @param tabId
   *          The 1-based tab identifier to initialize.
   */
  private void initTabControls(short tabId)
  {
    if (initializedTabs.contains(tabId))
    {
      return;
    }
    initializedTabs.add(tabId);

    int index = tabId - 1;
    if (index < 0 || index >= allTabConfigs.size())
    {
      LOGGER.warn("initTabControls: invalid tabId {}", tabId);
      return;
    }

    TabConfig tab = allTabConfigs.get(index);
    XTabPage xTabPage = tabControlContainer.getTabPageByID(tabId);
    XControlContainer tabPageControlContainer = UNO.XControlContainer(xTabPage);

    // Retrieve the pre-created empty layout for this tab.
    Layout controlsVLayout = tabContentLayouts.get(index);

    // Add backward tab-switcher (invisible focus trap) if not the first tab.
    if (index > 0)
    {
      addTabSwitcher("backward", allTabConfigs.get(index - 1), s -> {
        previousTab();
        s.reduce((f, se) -> se).ifPresent(XWindow::setFocus);
      }, tabPageControlContainer, controlsVLayout);
    }

    // Create all controls for this tab.
    setControls(tab, tabPageControlContainer, controlsVLayout);

    // Add forward tab-switcher if not the last tab.
    if (index < allTabConfigs.size() - 1)
    {
      addTabSwitcher("forward", allTabConfigs.get(index + 1), s -> {
        nextTab();
        s.findFirst().ifPresent(XWindow::setFocus);
      }, tabPageControlContainer, controlsVLayout);
    }

    LOGGER.debug("initTabControls: lazily initialized tab {} ('{}')", tabId, tab.getTitle());
  }

  /**
   * Add controls from {@link TabConfig} description to {link Layout}.
   *
   * @param tab
   *          {@link TabConfig} Tab from {@link FormModel}.
   * @param tabPageControlContainer
   *          {@link XControlContainer} ControlContainer in which the controls will be inserted.
   * @param layout
   *          The {@link Layout} in which newly created Layouts by this method will be inserted.
   * @return The generated Layout.
   */
  private void setControls(TabConfig tab, XControlContainer tabPageControlContainer, Layout layout)
  {
    tab.getControls().forEach(control -> {
      Layout controlLayout = createXControlByType(control, tabPageControlContainer);

      if (controlLayout == null)
      {
        LOGGER.trace("layout with control id '{}' is null.", control.getId());
      } else
      {
        layout.addLayout(controlLayout, 1);
      }
    });
  }

  /**
   * Add a control that performs the supplied action if it gets the focus. The action gets a
   * {@link Stream} of the controls which are visible on the given tab.
   *
   * @param id
   *          The ID of the control.
   * @param tab
   *          The tab which has the controls for the action.
   * @param action
   *          The action.
   * @param tabPageControlContainer
   *          {@link XControlContainer} ControlContainer in which the control will be inserted.
   * @param layout
   *          The {@link Layout} in which in which the control will be inserted.
   */
  private void addTabSwitcher(String id, TabConfig tab, Consumer<Stream<XWindow2>> action,
      XControlContainer tabPageControlContainer, Layout layout)
  {
    XControl tabSwithcer = GuiFactory.createTextfield(xMCF, context, id + tab.getId(), new Rectangle(0, 0, 0, 0), null,
        null);
    UNO.XWindow(tabSwithcer).addFocusListener(new AbstractFocusListener()
    {
      @Override
      public void focusGained(FocusEvent event)
      {
        if ((event.FocusFlags & FocusChangeReason.TAB) == 1)
        {
          Stream<XWindow2> controlConfigs = tab.getControls().stream()
              .filter(c -> c.getType() == UIElementType.BUTTON || c.getType() == UIElementType.CHECKBOX
                  || c.getType() == UIElementType.COMBOBOX || c.getType() == UIElementType.LISTBOX
                  || c.getType() == UIElementType.TEXTAREA || c.getType() == UIElementType.TEXTFIELD)
              .map(c -> controls.get(c.getId())).filter(Objects::nonNull).map(p -> UNO.XWindow2(p.getRight()))
              .filter(Objects::nonNull).filter(XWindow2::isVisible);
          action.accept(controlConfigs);
        }
      }
    });
    layout.addLayout(new ControlLayout(UNO.XWindow(tabSwithcer), 0)
    {
      @Override
      public boolean isVisible()
      {
        return false;
      }
    }, 1);
    tabPageControlContainer.addControl(id, tabSwithcer);
  }

  /**
   * Inserts Buttons from form descriptions 'Buttons'-Section to given layout.
   *
   * @param tab
   *          Tab from {@link FormModel}
   * @param tabPageControlContainer
   *          The ControlContainer where controls will be inserted.
   * @param tabPageButtonVLayout
   *          layout instance.
   * @param tabId
   *          The ID of the tab.
   */
  private void addButtonsToLayout(TabConfig tab, FormModel model, XControlContainer tabPageControlContainer,
      Layout layout, short tabId)
  {
    if (!tab.getButtons().isEmpty())
    {
      Layout tabButtonLayout = null;
      for (int i = 0; i < tab.getButtons().size(); i++)
      {
        if (i % (BUTTONS_PER_ROW + 1) == 0)
        {
          tabButtonLayout = new HorizontalLayout(0, 0, 0, 0, 5);
          layout.addLayout(tabButtonLayout, 1);
        }

        UIElementConfig control = tab.getButtons().get(i);
        Layout controlLayout = createXControlByType(control, tabPageControlContainer);
        if (controlLayout == null)
        {
          LOGGER.trace("layout with control id '{}' is null.", control.getId());
        } else
        {
          buttons.put(model.getControl(control.getId()), tabId);
          tabButtonLayout.addLayout(controlLayout, 1);
        }
      }
    }
  }

  /**
   * Creates LO's UI controls by type of {@link UIElementConfig}. And puts them in a Layout.
   *
   * @param control
   *          control model.
   * @param page
   *          {@link XTabPage} as XControl
   * @return Layout with one or more Controls.
   */
  private Layout createXControlByType(UIElementConfig control, XControlContainer pageContainer)
  {
    Layout layout = new HorizontalLayout(0, 0, 5, 10, 5);
    XControl xLabel = null;
    XControl xControl = null;
    SortedMap<String, Object> props = new TreeMap<>();

    String controlLabel = control.getLabel();
    HTMLElement htmlElement = new HTMLElement(controlLabel);

    switch (control.getType())
    {
    case TEXTFIELD:
      xLabel = createLabel(control, htmlElement);
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.READ_ONLY, control.isReadonly());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      xControl = GuiFactory.createTextfield(xMCF, context, "", new Rectangle(0, 0, 100, 20), props,
          formSidebarController::textChanged);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case BUTTON:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.LABEL, htmlElement.getText());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.ENABLED, !control.isReadonly());
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      xControl = GuiFactory.createButton(xMCF, context, htmlElement.getText(), formSidebarController::buttonPressed,
          new Rectangle(0, 0, 150, 20), props);
      UNO.XButton(xControl).setActionCommand(control.getAction());
      break;

    case LABEL:
      xLabel = createLabel(control, htmlElement);
      break;

    case COMBOBOX:
      xLabel = createLabel(control, htmlElement);
      if (control.isEditable())
      {
        xControl = createComboBox(control);
      } else
      {
        xControl = createListBox(control);
      }
      break;

    case CHECKBOX:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.LABEL, htmlElement.getText());
      props.put(UnoProperty.STATE, (short) 0);
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.ENABLED, !control.isReadonly());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      xControl = GuiFactory.createCheckBox(xMCF, context, formSidebarController::checkBoxChanged,
          new Rectangle(0, 0, 100, 20), props);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case TEXTAREA:
      layout = new VerticalLayout();
      xLabel = createLabel(control, htmlElement);
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.READ_ONLY, control.isReadonly());
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      xControl = GuiFactory.createTextfield(xMCF, context, "",
          new Rectangle(0, 0, 100, control.getLines() * 20), props, formSidebarController::textChanged);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      break;

    case LISTBOX:
      xLabel = createLabel(control, htmlElement);
      xControl = createListBox(control);
      break;

    case SEPARATOR:
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      xControl = GuiFactory.createLine(xMCF, context, new Rectangle(0, 0, 100, 5), props);
      break;

    default:
      return null;
    }

    controls.put(control.getId(), Pair.of(xLabel, xControl));
    if (xLabel != null)
    {
      layout.addControl(xLabel);
      pageContainer.addControl(control.getId() + "Label", xLabel);
    }
    if (xControl != null)
    {
      layout.addControl(xControl);
      pageContainer.addControl(control.getId(), xControl);
    }
    return layout;
  }

  private XControl createLabel(UIElementConfig control, HTMLElement htmlElement)
  {
    if (htmlElement != null)
    {
      SortedMap<String, Object> props = new TreeMap<>();
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.TEXT_COLOR, htmlElement.getRGBColor());
      props.put(UnoProperty.MULTILINE, true);
      props.put(UnoProperty.HELP_TEXT, control.getTip());
      props.put(UnoProperty.FONT_DESCRIPTOR, htmlElement.getFontDescriptor());
      props.put("TabIndex", -1);
      if (!htmlElement.getHref().isEmpty())
      {
        props.put(UnoProperty.URL, htmlElement.getHref());
        return GuiFactory.createHyperLinkLabel(xMCF, context, htmlElement.getText(), new Rectangle(0, 0, 100, 20),
            props);
      } else
      {
        return GuiFactory.createLabel(xMCF, context, htmlElement.getText(), new Rectangle(0, 0, 100, 20), props);
      }
    } else
    {
      LOGGER.trace("HTMLElement is NULL.");
    }

    return null;
  }

  private XControl createListBox(UIElementConfig control)
  {
    SortedMap<String, Object> propsListBox = new TreeMap<>();
    propsListBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsListBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsListBox.put(UnoProperty.DROPDOWN, true);
    propsListBox.put(UnoProperty.HELP_TEXT, control.getTip());
    XControl xControl = GuiFactory.createListBox(xMCF, context, formSidebarController::listBoxChanged,
        new Rectangle(0, 0, 100, 20), propsListBox);
    UNO.XListBox(xControl).setDropDownLineCount((short) 10);
    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XListBox(xControl).addItems(cmbValues, (short) 0);
    }
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());

    return xControl;
  }

  private XControl createComboBox(UIElementConfig control)
  {
    SortedMap<String, Object> propsComboBox = new TreeMap<>();
    propsComboBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsComboBox.put(UnoProperty.DROPDOWN, true);
    propsComboBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsComboBox.put(UnoProperty.BORDER, (short) 2);
    propsComboBox.put(UnoProperty.HELP_TEXT, control.getTip());
    XControl xControl = GuiFactory.createCombobox(xMCF, context, "", formSidebarController::comboBoxChanged,
        formSidebarController::textChanged, new Rectangle(0, 0, 100, 20), propsComboBox);
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());

    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XComboBox(xControl).addItems(cmbValues, (short) 0);
    }

    return xControl;
  }

  /**
   * Set Text Property on {@link XControlModel}.
   *
   * @param id
   *          {@link XControlModel} Model from {@link XControl}
   * @param text
   *          Text Value.
   */
  public void setText(String id, String text)
  {
    Pair<XControl, XControl> controlPair = controls.get(id);

    if (controlPair == null)
    {
      LOGGER.debug("controlPair ist null. id: {}", id);
      return;
    }

    XControl control = controls.get(id).getRight();

    if (control == null)
    {
      LOGGER.debug("control ist null. id: {}", id);
      return;
    }

    if (UNO.XTextComponent(control) != null)
    {
      UNO.XTextComponent(control).setText(text);
    } else if (UNO.XListBox(control) != null)
    {
      UNO.XListBox(control).selectItem(text, true);
    } else if (UNO.XCheckBox(control) != null)
    {
      UNO.XCheckBox(control).setState((short) (Boolean.parseBoolean(text) ? 1 : 0));
    } else
    {
      LOGGER.debug("Unknown control type");
    }
  }

  /**
   * Returns true if the control with the given ID has already been created (i.e. its tab has been
   * initialized). Used by the controller to skip value/color updates for not-yet-existing controls.
   *
   * @param id
   *          The control ID.
   * @return True if the control exists in the UI.
   */
  public boolean isControlInitialized(String id)
  {
    return controls.containsKey(id);
  }

  @Override
  public LayoutSize getHeightForWidth(int width)
  {
    return new LayoutSize(-1, -1, -1);
  }

  @Override
  public int getMinimalWidth()
  {
    int width = 0;
    try
    {
      int maxWidth = (int) UnoConfiguration.getConfiguration("org.openoffice.Office.UI.Sidebar/General",
          "MaximumWidth") - 60;
      width = vLayout.getMinimalWidth(maxWidth);
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
    return width;
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    return UNO.XAccessible(getWindow());
  }

  @Override
  public XWindow getWindow()
  {
    return UNO.XWindow(controlContainer);
  }

  @Override
  public void dispose()
  {
    formSidebarController.unregisterListener();
  }

  /**
   * Activate the previous tab.
   */
  public void previousTab()
  {
    if (tabChanged)
    {
      tabChanged = false;
      short currentabPageId = tabControlContainer.getActiveTabPageID();
      short prev = (short) (currentabPageId - 1);
      while (prev > 0
          && !UNO.XTabPageModel(UNO.XControl(tabControlContainer.getTabPageByID(prev)).getModel()).getEnabled())
      {
        prev--;
      }

      if (prev > 0)
      {
        tabControlContainer.setActiveTabPageID(prev);
      } else
      {
        tabControlContainer.setActiveTabPageID(tabControlContainer.getTabPageCount());
      }
      tabChanged = true;
    }
  }

  /**
   * Activate the next tab.
   */
  public void nextTab()
  {
    if (tabChanged)
    {
      tabChanged = false;
      short currentabPageId = tabControlContainer.getActiveTabPageID();
      short next = (short) (currentabPageId + 1);

      if (next > tabControlContainer.getTabPageCount())
      {
        tabControlContainer.setActiveTabPageID((short) 1);
      } else
      {
        tabControlContainer.setActiveTabPageID(next);
      }
      tabChanged = true;
    }
  }

  /**
   * Hide or show a control and its label.
   *
   * @param id
   *          The ID of the control.
   * @param visible
   *          True if the control should be visible, false otherwise.
   */
  public void setVisible(String id, boolean visible)
  {
    Pair<XControl, XControl> control = controls.get(id);
    if (control != null && control.getLeft() != null)
    {
      UNO.XWindow(control.getLeft()).setVisible(visible);
    }
    if (control != null && control.getRight() != null)
    {
      UNO.XWindow(control.getRight()).setVisible(visible);
    }
  }

  /**
   * Change the background color of a control.
   *
   * @param id
   *          The ID of the control.
   * @param okay
   *          If true the control gets its default background color, otherwise the provided color.
   * @param color
   *          The background color to set if okay is false.
   */
  public void setBackgroundColor(String id, boolean okay, int color, boolean init)
  {
    Pair<XControl, XControl> pair = controls.get(id);
    if (pair == null)
    {
      LOGGER.debug("control pair ist null. id: {}", id);
      return;
    }
    XControl control = pair.getRight();
    XControl controlleft = pair.getLeft();

    if (control == null)
    {
      LOGGER.debug("control ist null. id: {}", id);
      return;
    }

    boolean isListbox = (UNO.XListBox(control) != null);
    try
    {
      if(isListbox)
        UnoProperty.setProperty(control.getModel(), "NativeWidgetLook", Boolean.FALSE);
      if (okay)
      {
        UnoProperty.setPropertyToDefault(control.getModel(), UnoProperty.BACKGROUND_COLOR);
      } else
      {
        UnoProperty.setProperty(control.getModel(), UnoProperty.BACKGROUND_COLOR, color);
        if(!init)
          UNO.XWindow(controlleft).setFocus();
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }
}
