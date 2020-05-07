package de.muenchen.allg.itd51.wollmux.form.sidebar;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractSidebarPanel;
import de.muenchen.allg.dialog.adapter.AbstractTabPageContainerListener;
import de.muenchen.allg.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.Control;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.Tab;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.form.control.HTMLElement;
import de.muenchen.allg.itd51.wollmux.form.control.HTMLParserCallback;
import de.muenchen.allg.itd51.wollmux.sidebar.GuiFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.ControlLayout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.Layout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.VerticalLayout;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;

/**
 * form UI in sidebar.
 */
public class FormSidebarPanel extends AbstractSidebarPanel implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FormSidebarPanel.class);
  private XToolkit toolkit;
  private XWindowPeer windowPeer;
  private XWindow window;
  private Layout vLayout;
  private XComponentContext context;
  private XMultiComponentFactory xMCF;
  private Map<Short, Layout> tabPageLayouts = new HashMap<>();
  private XTabPageContainer tabControlContainer;
  private FormSidebarController formSidebarController;
  private Map<String, Pair<XControl, XControl>> controls = new HashMap<>();

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

    vLayout = new VerticalLayout(0, 0);

    XWindowPeer parentWindowPeer = UNO.XWindowPeer(parentWindow);
    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UNO.XWindow(windowPeer);

    window.setVisible(true);

    this.formSidebarController = formSidebarController;

    window.addWindowListener(new AbstractWindowListener()
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
  private void paint()
  {
    Rectangle rect = window.getPosSize();
    LOGGER.debug("width {}, height {}", rect.Width, rect.Height);
    if (tabControlContainer != null)
    {
      short activeTab = tabControlContainer.getActiveTabPageID();
      Rectangle tabRect = UNO.XWindow(tabControlContainer.getTabPageByID(activeTab)).getPosSize();
      Rectangle newRect = new Rectangle(tabRect.X, rect.Y, Integer.max(0, rect.Width - tabRect.X - 18), rect.Height);

      tabPageLayouts.get(activeTab).layout(newRect);
      newRect.Width += 15;
      vLayout.layout(newRect);
      UNO.XWindow(tabControlContainer.getTabPageByID(activeTab)).setPosSize(tabRect.X, tabRect.Y,
          Integer.max(0, rect.Width - tabRect.X - 13), rect.Height, PosSize.POSSIZE);
      Rectangle r = UNO.XWindow(tabControlContainer).getPosSize();
      UNO.XWindow(tabControlContainer).setPosSize(r.X, r.Y, Integer.max(0, rect.Width - tabRect.X - 3), rect.Height,
          PosSize.POSSIZE);
    } else
    {
      vLayout.layout(rect);
    }
  }

  /**
   * Creates the tab control with its content controls.
   * 
   * @param model
   *          {@link FormModel} FormModel from @{link {@link TextDocumentController}.
   */
  public void createTabControl(FormModel model)
  {
    if (model != null)
    {
      XControl tabControl = GuiFactory.createTabPageContainer(xMCF, context, toolkit, windowPeer);
      tabControlContainer = UNO.XTabPageContainer(tabControl);
      AbstractTabPageContainerListener listener = event -> formSidebarController.requestLayout();
      tabControlContainer.addTabPageContainerListener(listener);

      tabControlContainer = UNO.XTabPageContainer(tabControl);
      vLayout.addControl(tabControl);

      short tabId = 1;
      for (Map.Entry<String, Tab> entry : model.getTabs().entrySet())
      {
        Tab tab = entry.getValue();
        HTMLElement htmlElement = parseHtmlLabel(tab.getTitle());
        GuiFactory.createTab(this.xMCF, this.context, UNO.XTabPageContainerModel(tabControl.getModel()), htmlElement
            .getText(),
            tabId);
        XTabPage xTabPage = tabControlContainer.getTabPageByID(tabId);

        XControlContainer tabPageControlContainer = UNO.XControlContainer(xTabPage);

        Layout controlsVLayout = new VerticalLayout(5, 6);
        setControls(tab, tabPageControlContainer, controlsVLayout);
        addButtonsToLayout(tab, tabPageControlContainer, controlsVLayout);

        tabPageLayouts.put(tabId, controlsVLayout);

        tabId++;
      }
      tabControlContainer.setActiveTabPageID((short) 1);
    } else
    {
      XControl label = GuiFactory.createLabel(this.xMCF, this.context, this.toolkit, this.windowPeer,
          "Das Dokument ist kein Formular.", new Rectangle(0, 0, 50, 20), null);
      vLayout.addControl(label);
      formSidebarController.requestLayout();
    }
  }

  /**
   * Add controls from {@link Tab} description to {link Layout}.
   * 
   * @param tab
   *          {@link Tab} Tab from {@link FormModel}.
   * @param tabPageControlContainer
   *          {@link XControlContainer} ControlContainer in which the controls will be inserted.
   * @param layout
   *          The {@link Layout} in which newly created Layouts by this method will be inserted.
   * @return The generated Layout.
   */
  private void setControls(Tab tab, XControlContainer tabPageControlContainer, Layout layout)
  {
    tab.getControls().forEach(control -> {
      Layout controlLayout = createXControlByType(control, UNO.XControl(tabPageControlContainer));

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
   * Inserts Buttons from form descriptions 'Buttons'-Section to given layout.
   * 
   * @param tab
   *          Tab from {@link FormModel}
   * @param tabPageControlContainer
   *          The ControlContainer where controls will be inserted.
   * @param tabPageButtonVLayout
   *          layout instance.
   */
  private void addButtonsToLayout(Tab tab, XControlContainer tabPageControlContainer, Layout layout)
  {
    if (!tab.getButtons().isEmpty())
    {
      XControl page = UNO.XControl(tabPageControlContainer);
      Layout hButtonLayout = new HorizontalLayout(20, 5);

      tab.getButtons().forEach(control -> {
        Layout controlLayout = createXControlByType(control, page);

        if (controlLayout == null)
        {
          LOGGER.info("layout with control id '{}' is null.", control.getId());
        } else
        {
          hButtonLayout.addLayout(controlLayout, 1);
        }
      });

      layout.addLayout(hButtonLayout, 1);
    }
  }


  private HTMLElement parseHtmlLabel(String label)
  {
    HTMLElement htmlElement = new HTMLElement();

    if (label.contains("<html>"))
    {
      List<HTMLElement> htmlElements = parseHtml(label);

      if (!htmlElements.isEmpty())
      {
        return htmlElements.get(0);
      }
    }

    htmlElement.setText(label);

    return htmlElement;
  }
  /**
   * Creates LO's UI controls by type of {@link ControlModel}. In general this method returns a
   * ControlLayout which contains a single {@link XControl}. Type TEXTFIELD returns an
   * {@link HorizontalLayout} due no label support for LO's editfield.
   * 
   * @param control
   *          control model.
   * @param page
   *          {@link XTabPage} as XControl
   * @return Layout with one or more Controls.
   */
  private Layout createXControlByType(Control control, XControl page)
  {
    Layout layout = null;

    HTMLElement htmlElement = parseHtmlLabel(control.getLabel());

    switch (control.getType())
    {
    case TEXTFIELD:
      layout = new HorizontalLayout();

      XControl xLabel = createLabelForControl(htmlElement.getText(), page, layout);

      SortedMap<String, Object> props = new TreeMap<>();
      props.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      props.put(UnoProperty.READ_ONLY, control.isReadonly());
      props.put(UnoProperty.HELP_TEXT, control.getTip());

      XControl xTextField = GuiFactory.createTextfield(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
          control.getValue(), new Rectangle(0, 0, 100, 20), props, formSidebarController::textChanged);
      UNO.XWindow(xTextField).addFocusListener(formSidebarController.getFocusListener());

      layout.addControl(xTextField);
      controls.put(control.getId(), Pair.of(xLabel, xTextField));

      break;
    case BUTTON:
      SortedMap<String, Object> propsButton = new TreeMap<>();

      propsButton.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      propsButton.put(UnoProperty.LABEL, control.getLabel());
      propsButton.put(UnoProperty.HELP_TEXT, control.getTip());
      propsButton.put(UnoProperty.ENABLED, !control.isReadonly());

      XControl xControl = GuiFactory.createButton(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
          htmlElement.getText(), formSidebarController::buttonPressed, new Rectangle(0, 0, 150, 20), propsButton);
      layout = new ControlLayout(xControl);
      UNO.XButton(xControl).setActionCommand(control.getAction());
      controls.put(control.getId(), Pair.of(null, xControl));
      break;

    case LABEL:

      if (htmlElement != null && !htmlElement.getHref().isEmpty())
      {
        SortedMap<String, Object> propsHyperlinkLabel = new TreeMap<>();

        propsHyperlinkLabel.put(UnoProperty.DEFAULT_CONTROL, control.getId());
        propsHyperlinkLabel.put(UnoProperty.TEXT_COLOR, Math.abs(htmlElement.getRGBColor()) & ~0xFF000000);
        propsHyperlinkLabel.put(UnoProperty.URL, htmlElement.getHref());
        propsHyperlinkLabel.put(UnoProperty.MULTILINE, true);
        propsHyperlinkLabel.put(UnoProperty.HELP_TEXT, control.getTip());

        xControl = GuiFactory.createHyperLinkLabel(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
            new Rectangle(0, 0, 100, 20), propsHyperlinkLabel);
        controls.put(control.getId(), Pair.of(xControl, null));
        layout = new ControlLayout(xControl);
      } else
      {
        SortedMap<String, Object> propsLabel = new TreeMap<>();
        propsLabel.put(UnoProperty.DEFAULT_CONTROL, control.getId());
        propsLabel.put(UnoProperty.MULTILINE, true);
        propsLabel.put(UnoProperty.HELP_TEXT, control.getTip());
        propsLabel.put(UnoProperty.TEXT_COLOR, Math.abs(htmlElement.getRGBColor()) & ~0xFF000000);

        if (htmlElement.getFontDescriptor() != null)
          propsLabel.put("FontDescriptor", htmlElement.getFontDescriptor());

        xControl = GuiFactory.createLabel(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
            htmlElement.getText(), new Rectangle(0, 0, 100, 20), propsLabel);
        controls.put(control.getId(), Pair.of(xControl, null));
        layout = new ControlLayout(xControl);
      }

      break;

    case COMBOBOX:
      if (control.isEditable())
      {
        layout = createComboBox(control, htmlElement.getText(), page);
      } else
      {
        layout = createListBox(control, htmlElement.getText(), page);
      }
      break;
    case CHECKBOX:
      SortedMap<String, Object> propsCheckBox = new TreeMap<>();

      propsCheckBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());

      propsCheckBox.put(UnoProperty.HELP_TEXT, control.getTip());
      propsCheckBox.put(UnoProperty.LABEL, htmlElement.getText());
      propsCheckBox.put(UnoProperty.STATE, (short) 0);
      propsCheckBox.put(UnoProperty.ENABLED, !control.isReadonly());
      propsCheckBox.put(UnoProperty.MULTILINE, true);
      propsCheckBox.put(UnoProperty.HELP_TEXT, control.getTip());

      xControl = GuiFactory.createCheckBox(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
          formSidebarController::checkBoxChanged, new Rectangle(0, 0, 100, 20), propsCheckBox);
      layout = new ControlLayout(xControl);
      UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
      controls.put(control.getId(), Pair.of(null, xControl));
      break;

    case TEXTAREA:
      layout = new VerticalLayout();

      XControl xLabelTextArea = createLabelForControl(htmlElement.getText(), page, layout);

      SortedMap<String, Object> propsTextArea = new TreeMap<>();

      propsTextArea.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      propsTextArea.put(UnoProperty.MULTILINE, true);
      propsTextArea.put(UnoProperty.READ_ONLY, control.isReadonly());
      propsTextArea.put(UnoProperty.HELP_TEXT, control.getTip());

      XControl xControlTextField = GuiFactory.createTextfield(xMCF, context, page.getPeer().getToolkit(),
          page.getPeer(), control.getValue(), new Rectangle(0, 0, 100, control.getLines() * 20), propsTextArea,
          formSidebarController::textChanged);
      UNO.XWindow(xControlTextField).addFocusListener(formSidebarController.getFocusListener());

      layout.addControl(xControlTextField);
      controls.put(control.getId(), Pair.of(xLabelTextArea, xControlTextField));

      break;

    case LISTBOX:
      layout = createListBox(control, htmlElement.getText(), page);
      break;
    case SEPARATOR:
      SortedMap<String, Object> propsSeparator = new TreeMap<>();
      propsSeparator.put(UnoProperty.DEFAULT_CONTROL, control.getId());
      XControl line = GuiFactory.createHLine(xMCF, context, page.getPeer().getToolkit(),
          page.getPeer(),
          new Rectangle(0, 0, 100, 5),
          propsSeparator);
      layout = new ControlLayout(line);
      controls.put(control.getId(), Pair.of(null, line));
      break;

    default:
      break;
    }

    return layout;
  }

  private XControl createLabelForControl(String label, XControl page, Layout layout)
  {
    XControl xLabel = null;
    if (!label.isEmpty())
    {
      xLabel = GuiFactory.createLabel(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
          label,
          new Rectangle(0, 0, 50, 20), null);
      layout.addControl(xLabel);
    }
    return xLabel;
  }

  private Layout createListBox(Control control, String label, XControl page)
  {
    Layout layout = new HorizontalLayout();
    XControl xLabel = createLabelForControl(label, page, layout);

    SortedMap<String, Object> propsListBox = new TreeMap<>();
    propsListBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsListBox.put(UnoProperty.LABEL, control.getLabel());
    propsListBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsListBox.put(UnoProperty.DROPDOWN, true);
    propsListBox.put(UnoProperty.HELP_TEXT, control.getTip());

    XControl xControl = GuiFactory.createListBox(xMCF, context, page.getPeer().getToolkit(), page.getPeer(),
        formSidebarController::listBoxChanged, new Rectangle(0, 0, 100, 20), propsListBox);
    UNO.XListBox(xControl).setDropDownLineCount((short) 10);
    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XListBox(xControl).addItems(cmbValues, (short) 0);
    }
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());

    layout.addControl(xControl);
    controls.put(control.getId(), Pair.of(xLabel, xControl));
    return layout;
  }

  private Layout createComboBox(Control control, String label, XControl page)
  {
    Layout layout = new HorizontalLayout();
    XControl xLabel = createLabelForControl(label, page, layout);

    SortedMap<String, Object> propsComboBox = new TreeMap<>();
    propsComboBox.put(UnoProperty.DEFAULT_CONTROL, control.getId());
    propsComboBox.put(UnoProperty.DROPDOWN, true);
    propsComboBox.put(UnoProperty.READ_ONLY, control.isReadonly());
    propsComboBox.put(UnoProperty.BORDER, (short) 2);
    propsComboBox.put(UnoProperty.HELP_TEXT, control.getTip());
    XControl xControl = GuiFactory.createCombobox(xMCF, context, page.getPeer().getToolkit(), page.getPeer(), "",
        formSidebarController::comboBoxChanged, new Rectangle(0, 0, 100, 20), propsComboBox);
    UNO.XWindow(xControl).addFocusListener(formSidebarController.getFocusListener());
    if (!control.getOptions().isEmpty())
    {
      String[] cmbValues = new String[control.getOptions().size()];
      control.getOptions().toArray(cmbValues);
      UNO.XComboBox(xControl).addItems(cmbValues, (short) 0);
    }

    layout.addControl(xControl);
    controls.put(control.getId(), Pair.of(xLabel, xControl));
    return layout;
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
    XControl control = controls.get(id).getRight();
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

  @Override
  public LayoutSize getHeightForWidth(int width)
  {
    int height = vLayout.getHeightForWidth(width);
    if (tabControlContainer != null)
    {
      short activeId = tabControlContainer.getActiveTabPageID();
      Rectangle r = UNO.XWindow(tabControlContainer.getTabPageByID(activeId)).getPosSize();
      Layout currentLayout = tabPageLayouts.get(activeId);
      height = currentLayout.getHeightForWidth(width) + r.Y + 5;
    }
    return new LayoutSize(height, height, height);
  }

  @Override
  public int getMinimalWidth()
  {
    int width = 0;
    try
    {
      int maxWidth = (int) UnoConfiguration.getConfiguration("org.openoffice.Office.UI.Sidebar/General",
          "MaximumWidth") - 60;
      for (Map.Entry<Short, Layout> e : tabPageLayouts.entrySet())
      {
        width = Integer.max(width, e.getValue().getMinimalWidth(maxWidth));
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return width;
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
    return window;
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
    short currentabPageId = tabControlContainer.getActiveTabPageID();
    short prev = currentabPageId -= 1;

    if (prev > -1)
    {
      tabControlContainer.setActiveTabPageID(prev);
    }
  }

  /**
   * Activate the next tab.
   */
  public void nextTab()
  {
    short currentabPageId = tabControlContainer.getActiveTabPageID();
    short next = currentabPageId += 1;

    if (next > tabControlContainer.getTabPageCount())
    {
      tabControlContainer.setActiveTabPageID((short) 0);
    } else
    {
      tabControlContainer.setActiveTabPageID(next);
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
  public void setBackgroundColor(String id, boolean okay, int color)
  {
    XControl control = controls.get(id).getRight();
    try
    {
      if (okay)
      {
        UnoProperty.setPropertyToDefault(control.getModel(), UnoProperty.BACKGROUND_COLOR);
      } else
      {
        UnoProperty.setProperty(control.getModel(), UnoProperty.BACKGROUND_COLOR, color);
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Parses an html string with java swing's {@link HTMLEditorKit} to {@link HTMLElement}-Model.
   *
   * @param html
   *          HTML string.
   * @return List of HTML-Elements.
   */
  private List<HTMLElement> parseHtml(String html)
  {
    Reader stringReader = new StringReader(html);
    HTMLEditorKit.Parser parser = new ParserDelegator();
    HTMLParserCallback callback = new HTMLParserCallback();

    try
    {
      parser.parse(stringReader, callback, true);
    } catch (IOException e)
    {
      LOGGER.trace("", e);
    } finally
    {
      try
      {
        stringReader.close();
      } catch (IOException e)
      {
        LOGGER.trace("", e);
      }
    }

    return callback.getHtmlElement();
  }

  /**
   * Converts <br>
   * to \r\n for LO's text field
   *
   * @param html
   *          HTML string.
   * @return cleaned html.
   */
  private String convertLineBreaks(String html)
  {
    return html.replace("<br>", "\r\n");
  }

}