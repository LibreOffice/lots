/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.sidebar;

import java.awt.Desktop;
import java.awt.SystemColor;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.InvalidateStyle;
import com.sun.star.awt.MenuEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.Selection;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XItemList;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XMenu;
import com.sun.star.awt.XPopupMenu;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.ExpandVetoException;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractFocusListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractMenuListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractMouseListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnDumpInfo;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.interfaces.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.sender.SenderService;
import org.libreoffice.ext.unohelper.ui.layout.Layout;
import org.libreoffice.ext.unohelper.ui.layout.VerticalLayout;
import de.muenchen.allg.itd51.wollmux.util.L;
import org.libreoffice.ext.unohelper.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.ui.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.ui.UIElementType;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;

/**
 * Create the window of the WollMuxBar. It contains the tree to access the templates and several
 * buttons for frequently used actions.
 */
public class WollMuxSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel
{

  private static final String ABSENDER_AUSWAEHLEN_ACTION = "absenderAuswaehlen";

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxSidebarContent.class);

  /**
   * A configuration option.
   */
  public static final String ALLOW_USER_CONFIG = "ALLOW_USER_CONFIG";

  /**
   * Filename used to write the configuration.
   */
  public static final String WOLLMUXBAR_CONF = "wollmuxbar.conf";

  /**
   * Error message if no configuration can be found.
   */
  public static final String WOLLMUX_CONFIG_ERROR_MESSAGE = L
      .m("No section \"Symbolleisten\" could be read from your WollMux configuration.\n"
          + "Therefore, the WollMux toolbar cannot be started. "
          + "Please check if in your wollmux.conf the %include for the configuration "
          + "of the WollMuxBar (e.g. wollmuxbar_standard.conf) is present "
          + "and check with the wollmux.log if possibly an error occurred "
          + "while processing a %include.");

  /**
   * The component in which the sidebar is visible.
   */
  private XComponentContext context;

  /**
   * The component factory.
   */
  private XMultiComponentFactory xMCF;

  /**
   * The parent window.
   */
  private XWindow parentWindow;

  /**
   * The container of the controls.
   */
  private XControlContainer controlContainer;

  /**
   * The layout of the controls.
   */
  private Layout layout;

  /**
   * The window peer.
   */
  private XWindowPeer windowPeer;

  /**
   * The tree control.
   */
  private XTreeControl tree;

  /**
   * The tree model.
   */
  private XMutableTreeDataModel dataModel;

  /**
   * Mapping from menu name to tree node.
   */
  private Map<String, XMutableTreeNode> menus;

  /**
   * Mapping from menu entry to mouse click action.
   */
  private Map<String, Runnable> actions;

  /**
   * Mapping from search list entry to mouse click action.
   */
  private Map<String, Runnable> searchActions;

  private SenderService senderService;

  /**
   * Create the sidebar.
   *
   * @param context
   *          The context of the sidebar.
   * @param parentWindow
   *          The parent window of the sidebar.
   */
  public WollMuxSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;
    senderService = SenderService.getInstance();

    menus = new HashMap<>();
    actions = new HashMap<>();
    searchActions = new HashMap<>();

    AbstractWindowListener windowAdapter = new AbstractWindowListener()
    {
      @Override
      public void windowResized(WindowEvent e)
      {
        layout.layout(parentWindow.getPosSize());
      }
    };
    this.parentWindow.addWindowListener(windowAdapter);
    layout = new VerticalLayout(5, 5, 5, 5, 5);

    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    boolean allowUserConfig = true;
    try
    {
      allowUserConfig = conf.query(ALLOW_USER_CONFIG, 1).getLastChild().toString().equalsIgnoreCase("true");
    } catch (NodeNotFoundException e)
    {
      LOGGER.trace("", e);
    }

    xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
    XWindowPeer parentWindowPeer = UNO.XWindowPeer(parentWindow);

    if (parentWindowPeer == null)
    {
      return;
    }

    XToolkit parentToolkit = parentWindowPeer.getToolkit();
    controlContainer = UNO
        .XControlContainer(GuiFactory.createControlContainer(xMCF, context, new Rectangle(0, 0, 0, 0), null));
    UNO.XControl(controlContainer).createPeer(parentToolkit, parentWindowPeer);
    windowPeer = UNO.XControl(controlContainer).getPeer();

    try
    {

      if (WollMuxSingleton.getInstance().isNoConfig())
      {
        String text = L.m("No WollMux Configuration found.\n"
            + "Please setup the WollMux Configuration\n"
            + "as described on the WollMux website");
        XControl txt = GuiFactory.createLabel(xMCF, context, text, new Rectangle(5, 15, 10, 80), null);
        controlContainer.addControl("txt", txt);
        layout.addControl(txt);

        XControl button = GuiFactory.createButton(UNO.xMCF, context, "Visit wollmux.org", null,
            new Rectangle(0, 0, 100, 32), null);

        XButton xbutton = UNO.XButton(button);
        AbstractActionListener xButtonAction = event -> openInBrowser("https://wollmux.org");
        xbutton.addActionListener(xButtonAction);
        controlContainer.addControl("btn", button);
        layout.addControl(button);
      } else
      {
        readWollMuxBarConf(allowUserConfig, conf);
        createWollMuxBar(context, xMCF, conf);
      }
    } catch (Exception ex)
    {
      LOGGER.error("", ex);
    }
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    // TODO: the following is wrong, since it doesn't respect i_rParentAccessible. In
    // a real extension, you should implement this correctly :)
    return UNO.XAccessible(getWindow());
  }

  @Override
  public XWindow getWindow()
  {
    if (controlContainer == null)
    {
      throw new DisposedException("", this);
    }
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
    return 300;
  }

  /**
   * Create the WollMux Bar.
   *
   * @param context
   *          The context of the sidebar.
   * @param xMCF
   *          The component factory.
   * @param conf
   *          The configuration.
   * @throws NodeNotFoundException
   *           A menu entry for an unknown menu should be created.
   * @throws ExpandVetoException
   *           The node can't be expanded.
   */
  private void createWollMuxBar(XComponentContext context, XMultiComponentFactory xMCF,
      ConfigThingy conf) throws NodeNotFoundException, ExpandVetoException
  {
    dataModel = GuiFactory.createTreeModel(xMCF, context);

    XMutableTreeNode root = dataModel.createNode("Vorlagen", false);
    dataModel.setRoot(root);

    XControl treeCtrl = GuiFactory.createTree(xMCF, context, dataModel);
    tree = UNO.XTreeControl(treeCtrl);
    controlContainer.addControl("treeCtrl", treeCtrl);
    layout.addControl(treeCtrl, 6);

    XWindow treeWnd = UNO.XWindow(treeCtrl);
    AbstractMouseListener xMouseListener = new AbstractMouseListener()
    {
      @Override
      public void mousePressed(MouseEvent event)
      {
        try
        {
          XMutableTreeNode node = UnoRuntime.queryInterface(XMutableTreeNode.class,
              tree.getClosestNodeForLocation(event.X, event.Y));

          if (node != null)
          {
            tree.clearSelection();
            tree.addSelection(node);
            Runnable action = actions.get(node.getDataValue());
            if (action != null)
            {
              action.run();
            }
          }
        } catch (Exception ex)
        {
          LOGGER.error("", ex);
        }
      }
    };
    treeWnd.addMouseListener(xMouseListener);

    XControl line = GuiFactory.createLine(xMCF, context, new Rectangle(0, 0, 10, 4), null);
    controlContainer.addControl("line", line);
    layout.addControl(line, 1);

    ConfigThingy menubar = conf.query("Menueleiste");
    ConfigThingy menuConf = conf.query("Menues");

    if (menubar.count() > 0)
    {
      createUIElements(null, menubar.getLastChild(), false);

      for (ConfigThingy menuDef : menuConf.getLastChild())
      {
        createUIElements(menuDef, menuDef.getLastChild(), true);
      }
    }

    ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
    createUIElements(menuConf, bkl.getLastChild(), false);

    tree.expandNode(root);
  }

  /**
   * Read the configuration of the sidebar.
   *
   * @param allowUserConfig
   *          If true the file {@link #WOLLMUXBAR_CONF} is used. Otherwise the configuration should
   *          be in main.conf.
   * @param wollmuxConf
   *          The main configuration.
   */
  private void readWollMuxBarConf(boolean allowUserConfig, ConfigThingy wollmuxConf)
  {
    ConfigThingy wollmuxbarConf = null;
    File wollmuxbarConfFile = new File(WollMuxFiles.getWollMuxDir(), WOLLMUXBAR_CONF);

    if (wollmuxbarConfFile.exists())
    {
      if (allowUserConfig)
      {
        try
        {
          wollmuxbarConf = new ConfigThingy("wollmuxbarConf", wollmuxbarConfFile.toURI().toURL());
        } catch (Exception x)
        {
          LOGGER.error("Error while reading \"{}\"", wollmuxbarConfFile.toString(), x);
        }
      } else
      {
        LOGGER.debug("Die Verwendung der Konfigurationsdatei '{}' ist deaktiviert. Sie wird nicht ausgewertet!",
            wollmuxbarConfFile);
      }
    }

    if (wollmuxbarConf == null)
      wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    ConfigThingy combinedConf = new ConfigThingy("combinedConf");
    combinedConf.addChild(wollmuxConf);
    combinedConf.addChild(wollmuxbarConf);

    try
    {
      LOGGER.debug("WollMuxBar gestartet");

      if (combinedConf.query("Symbolleisten").count() == 0)
      {
        LOGGER.error(WOLLMUX_CONFIG_ERROR_MESSAGE);
        InfoDialog.showInfoModal(L.m("Incorrect configuration"), WOLLMUX_CONFIG_ERROR_MESSAGE);
      }
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Create a control.
   *
   * @param element
   *          The configuration of the element.
   * @param isMenu
   *          If false {@link UIElementType#MENUITEM} and {@link UIElementType#BUTTON} create
   *          buttons otherwise menu entries.
   * @param parentEntry
   *          The parent menu entry.
   */
  private void createControl(UIElementConfig element, boolean isMenu, String parentEntry)
  {
    if (element == null)
    {
      LOGGER.debug("Unbekanntes Element.");
      return;
    }

    if (!isMenu && (element.getType() == UIElementType.MENUITEM || element.getType() == UIElementType.BUTTON))
    {
      XControl button = GuiFactory.createButton(UNO.xMCF, context, element.getLabel(), null,
          new Rectangle(0, 0, 100, 32), null);

      XButton xbutton = UNO.XButton(button);
      AbstractActionListener xButtonAction = event -> processUiElementEvent(element);
      xbutton.addActionListener(xButtonAction);
      controlContainer.addControl(element.getId(), button);
      layout.addControl(button);
    } else if (element.getType() == UIElementType.SENDERBOX)
    {
      createSenderbox(element);
    } else if (element.getType() == UIElementType.SEARCHBOX)
    {
      createSearchbox(element);
    } else if (element.getType() == UIElementType.MENU)
    {
      XMutableTreeNode node = dataModel.createNode(element.getLabel(), false);
      if (parentEntry == null)
      {
        ((XMutableTreeNode) dataModel.getRoot()).appendChild(node);
      } else
      {
        menus.get(parentEntry).appendChild(node);
      }
      menus.put(element.getMenu(), node);
    } else if (isMenu && menus.get(parentEntry) != null
        && (element.getType() == UIElementType.MENUITEM || element.getType() == UIElementType.BUTTON))
    {
      XMutableTreeNode node = dataModel.createNode(element.getLabel(), false);
      menus.get(parentEntry).appendChild(node);

      UUID uuid = UUID.randomUUID();
      actions.put(uuid.toString(), () -> processUiElementEvent(element));
      node.setDataValue(uuid.toString());
    }
  }

  /**
   * Check if an UI element is a button and the label contains the given text (ignore case).
   *
   * @param button
   *          Configuration of an UI element.
   * @param words
   *          Words which must be in the label of the UI element.
   * @return True if the element is a button and the label contains the text.
   */
  private static boolean buttonMatches(ConfigThingy button, String[] words)
  {
    if (words == null || words.length == 0)
      return false;
    String type = button.getString("TYPE", "");
    if (!type.equals("button"))
    {
      return false;
    }

    String label = button.getString("LABEL", "");
    if (label.isEmpty())
    {
      return false;
    }

    for (String word : words)
    {
      if (!label.toLowerCase().contains(word.toLowerCase()))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Create the search control.
   *
   * @param element
   *          The configuration of the control.
   */
  @SuppressWarnings("java:S3776")
  private void createSearchbox(UIElementConfig element)
  {
    String label = L.m("Search...");

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("TextColor", SystemColor.textInactiveText.getRGB() & ~0xFF000000);
    props.put("Autocomplete", false);
    props.put("HideInactiveSelection", true);
    XTextComponent searchBox = UNO.XTextComponent(
        GuiFactory.createTextfield(UNO.xMCF, context, label, new Rectangle(0, 0, 100, 32), props, null));

    SortedMap<String, Object> propsResult = new TreeMap<>();
    propsResult.put("Enabled", false);
    propsResult.put("TextColor", SystemColor.textText.getRGB() & ~0xFF000000);
    AbstractItemListener resultListener = event -> {
      try
      {
        XControl ctrl = UNO.XControl(event.Source);
        XItemList items = UNO.XItemList(ctrl.getModel());
        String uuid = (String) items.getItemData(event.Selected);
        Runnable action = searchActions.get(uuid);
        if (action != null)
        {
          action.run();
        }
      } catch (IndexOutOfBoundsException e)
      {
        LOGGER.error("", e);
      }
    };
    XListBox resultBox = UNO.XListBox(
        GuiFactory.createListBox(UNO.xMCF, context, resultListener, new Rectangle(0, 0, 100, 0), propsResult));
    UNO.XWindow(resultBox).setVisible(false);

    final XWindow wnd = UNO.XWindow(searchBox);

    AbstractTextListener tfListener = event -> {
      String text = searchBox.getText();
      XItemList items = UNO.XItemList(UNO.XControl(resultBox).getModel());

      if (text.length() > 0)
      {
        String[] words = text.split("\\s+");
        try
        {
          resultBox.removeItems((short) 0, resultBox.getItemCount());
          searchActions.clear();

          ConfigThingy menues = WollMuxFiles.getWollmuxConf().get("Menues");
          ConfigThingy labels = menues.queryAll("LABEL", 4, true);

          List<UIElementConfig> newItems = new ArrayList<>(labels.count());
          for (ConfigThingy l : labels)
          {
            UIElementConfig conf = new UIElementConfig(l);
            if ((conf.getType() == UIElementType.BUTTON || conf.getType() == UIElementType.MENUITEM)
                && conf.getAction() != null && buttonMatches(l, words))
            {
              newItems.add(conf);
            }
          }

          newItems.sort((item1, item2) -> item1.getLabel().compareTo(item2.getLabel()));
          for (short n = 0; n < newItems.size(); n++)
          {
            items.insertItemText(n, newItems.get(n).getLabel());
            UIElementConfig item = newItems.get(n);
            UUID uuid = UUID.randomUUID();
            searchActions.put(uuid.toString(), () -> processUiElementEvent(item));
            items.setItemData(n, uuid.toString());
          }
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    };
    searchBox.addTextListener(tfListener);

    AbstractFocusListener wndListener = new AbstractFocusListener()
    {
      @Override
      public void focusLost(FocusEvent event)
      {
        try
        {
          XTextComponent searchBox = UNO.XTextComponent(event.Source);

          activate(false);

          if (searchBox.getText().isEmpty())
          {
            searchBox.setText(L.m("Search..."));
            XControlModel model = UNO.XControl(searchBox).getModel();
            UnoProperty.setProperty(model, UnoProperty.TEXT_COLOR, SystemColor.textInactiveText.getRGB() & ~0xFF000000);
          }

          windowPeer.invalidate((short) (InvalidateStyle.UPDATE | InvalidateStyle.TRANSPARENT));
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }

      @Override
      public void focusGained(FocusEvent event)
      {
        try
        {
          XTextComponent tf = UNO.XTextComponent(event.Source);
          XControlModel model = UNO.XControl(searchBox).getModel();

          activate(true);

          int color = (Integer) UnoProperty.getProperty(model, UnoProperty.TEXT_COLOR);
          if (color == (SystemColor.textInactiveText.getRGB() & ~0xFF000000))
          {
            UnoProperty.setProperty(model, UnoProperty.TEXT_COLOR, SystemColor.textText.getRGB() & ~0xFF000000);
            tf.setText("");
          } else
          {
            tf.setSelection(new Selection(0, tf.getText().length()));
          }

          windowPeer.invalidate((short) (InvalidateStyle.UPDATE | InvalidateStyle.TRANSPARENT));
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }

      private void activate(boolean active)
      {
        UNO.XWindow(resultBox).setVisible(active);
        UNO.XWindow(resultBox).setEnable(active);
        UNO.XWindow(resultBox).setPosSize(0, 0, 0, active ? 200 : 0, PosSize.HEIGHT);
      }
    };
    wnd.addFocusListener(wndListener);

    controlContainer.addControl(element.getId(), UNO.XControl(searchBox));
    controlContainer.addControl("resultBox", UNO.XControl(resultBox));
    layout.addControl(UNO.XControl(searchBox));
    layout.addControl(UNO.XControl(resultBox));
  }

  /**
   * Create the control to change the sender.
   *
   * @param uiSenderbox
   *          The configuration of the control.
   * @throws com.sun.star.uno.Exception
   */
  private void createSenderbox(UIElementConfig uiSenderbox)
  {
    String label = uiSenderbox.getLabel();
    if (!senderService.getCurrentSender().isEmpty())
    {
      label = senderService.getCurrentSender().split("§§%=%§§")[0];
    }

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("FocusOnClick", false);
    XControl button = GuiFactory.createButton(UNO.xMCF, context, label, null, new Rectangle(0, 0, 100, 32), props);
    final XButton xbutton = UNO.XButton(button);

    String[] palEntries = senderService.getPALEntries();

    final XPopupMenu menu = UNO
        .XPopupMenu(UnoComponent.createComponentWithContext("com.sun.star.awt.PopupMenu", xMCF, context));

    menu.addMenuListener(new AbstractMenuListener()
    {
      @Override
      public void itemSelected(MenuEvent event)
      {
        XMenu menu = UNO.XMenu(event.Source);
        try
        {
          if (ABSENDER_AUSWAEHLEN_ACTION.equals(menu.getCommand(event.MenuId)))
          {
            senderService.showManageSenderListDialog();
          } else
          {
            String name = menu.getCommand(event.MenuId);
            short pos = menu.getItemPos(event.MenuId);
            SenderService.getInstance().selectSender(name, pos);
          }
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    });

    short n = 0;
    for (String entry : palEntries)
    {
      menu.insertItem((short) (n + 1), entry.split(SenderService.SENDER_KEY_SEPARATOR)[0],
          (short) 0, (short) (n + 1));
      menu.setCommand((short) (n + 1), entry);
      n++;
    }
    menu.insertItem((short) (n + 1), "Absenderliste verwalten", (short) 0, (short) (n + 1));
    menu.setCommand((short) (n + 1), ABSENDER_AUSWAEHLEN_ACTION);

    senderService.addPALChangeEventListener(new XPALChangeEventListener()
    {
      @Override
      public void disposing(EventObject arg0)
      {
        // nothing to do
      }

      @Override
      public void updateContent(EventObject eventObject)
      {

        String[] entries = senderService.getPALEntries();
        String current = senderService.getCurrentSender();

        xbutton.setLabel(current.split(SenderService.SENDER_KEY_SEPARATOR)[0]);
        menu.clear();

        short n = 0;
        for (String entry : entries)
        {
          menu.insertItem((short) (n + 1), entry.split(SenderService.SENDER_KEY_SEPARATOR)[0],
              (short) 0, (short) (n + 1));
          menu.setCommand((short) (n + 1), entry);
          n++;
        }
        menu.insertItem((short) (n + 1), "Absenderliste verwalten", (short) 0, (short) (n + 1));
        menu.setCommand((short) (n + 1), ABSENDER_AUSWAEHLEN_ACTION);
      }
    });

    AbstractActionListener xButtonAction = event -> {
      final XWindow wndButton = UNO.XWindow(event.Source);
      Rectangle posSize = wndButton.getPosSize();
      menu.execute(windowPeer, new Rectangle(posSize.X, posSize.Y + posSize.Height, 0, 0), (short) 0);
    };

    xbutton.addActionListener(xButtonAction);
    controlContainer.addControl(uiSenderbox.getId(), UNO.XControl(xbutton));
    layout.addControl(button);
  }

  /**
   * Create menu entries.
   *
   * @param menuConf
   *          The configuration of the parent entry.
   * @param elementParent
   *          List of menu entries or buttons.
   * @param isMenu
   *          If false {@link UIElementType#MENUITEM} and {@link UIElementType#BUTTON} create
   *          buttons otherwise menu entries.
   */
  private void createUIElements(ConfigThingy menuConf, ConfigThingy elementParent,
      boolean isMenu)
  {
    for (ConfigThingy uiElementDesc : elementParent)
    {
      UIElementConfig config = new UIElementConfig(uiElementDesc);

      if (!config.isSidebar())
      {
        continue;
      }

      if (isMenu)
      {
        createControl(config, isMenu, menuConf.getName());
      } else
      {
        createControl(config, isMenu, null);
      }
    }
  }

  /**
   * Perform actions when a button oder menu item is clicked.
   *
   * @param config
   *          The configuration of the UI element.
   */
  private void processUiElementEvent(UIElementConfig config)
  {
    String action = config.getAction();
    if (action.equals(ABSENDER_AUSWAEHLEN_ACTION))
    {
      senderService.showManageSenderListDialog();
    } else if (action.equals("openDocument"))
    {
      String fragId = config.getFragId();
      if (fragId.isEmpty())
      {
        LOGGER.error("ACTION \"{}\" erfordert mindestens ein Attribut FRAG_ID", action);
      } else
      {
        new OnOpenDocument(Arrays.asList(fragId), false).emit();
      }
    } else if (action.equals("openTemplate"))
    {
      String fragId = config.getFragId();
      if (fragId.isEmpty())
      {
        LOGGER.error("ACTION \"{}\" erfordert mindestens ein Attribut FRAG_ID", action);
      } else
      {
        new OnOpenDocument(Arrays.asList(fragId), true).emit();
      }
    } else if (action.equals("open"))
    {
      InfoDialog.showInfoModal(L.m("Multi-forms no longer supported"),
          L.m("Multi-forms are no longer supported. You need to open and fill each form individually."));
    } else if (action.equals("openExt"))
    {
      executeOpenExt(config.getExt(), config.getUrl());
    } else if (action.equals("dumpInfo"))
    {
      new OnDumpInfo().emit();
    } else if (action.equals("kill"))
    {
      new OnKill().emit();
    } else if (action.equals("about"))
    {
      new OnAbout().emit();
    }
  }

  /**
   * Open an external application.
   *
   * @param ext
   *          The ID of the application.
   * @param url
   *          The URL to start the application with.
   */
  private void executeOpenExt(String ext, String url)
  {
    try
    {
      final OpenExt openExt = OpenExt.getInstance(ext, url);
      openExt.storeIfNecessary();

      Runnable launch = () -> openExt.launch((Exception x) -> {
        LOGGER.error("", x);
        showError(x.getMessage());
      });

      launch.run();
    } catch (IOException x)
    {
      LOGGER.error("", x);
      showError(L.m("Error during download of file:\n{0}", x.getMessage()));
    } catch (Exception x)
    {
      LOGGER.error("", x);
      showError(x.getMessage());
    }
  }

  private void openInBrowser(String url)
  {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      try
      {
        Desktop.getDesktop().browse(new URI(url));
      } catch (IOException | URISyntaxException e)
      {
        LOGGER.error("", e);
      }
    } else {
      LOGGER.error("Opening URL {} in Browser failed.", url);
    }
  }

  /**
   * Show an error dialog.
   *
   * @param errorMsg
   *          The content of the dialog.
   */
  private void showError(String errorMsg)
  {
    InfoDialog.showInfoModal(L.m("Incorrect configuration"),
        L.m("{0}\nPlease contact your system administrator.", errorMsg));
  }
}
