package de.muenchen.allg.itd51.wollmux.sidebar;

import java.awt.SystemColor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XItemList;
import com.sun.star.awt.XMenu;
import com.sun.star.awt.XPopupMenu;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.MethodConcept;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XIntrospection;
import com.sun.star.beans.XIntrospectionAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.reflection.InvocationTargetException;
import com.sun.star.reflection.XIdlMethod;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XPALProvider;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractMenuListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractMouseListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.SearchBox;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBar;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBarConfig;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIButton;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIControl;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIElementAction;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIElementCreateListener;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIMenu;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIMenuItem;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UISearchbox;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UISenderbox;
import de.muenchen.uno.UnoReflect;

/**
 * Erzeugt das Fenster, dass in der WollMux-Sidebar angezeigt wird. Das Fenster
 * enthält einen Baum zur Auswahl von Vorlagen und darunter eine Reihe von
 * Buttons für häufig benutzte Funktionen.
 *
 */
public class WollMuxSidebarContent extends ComponentBase implements XToolPanel,
    XSidebarPanel, UIElementCreateListener
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WollMuxSidebarContent.class);

  private static WollMuxBarConfig config;

  private XComponentContext context;

  private XWindow parentWindow;

  private XWindow window;

  private SimpleLayoutManager layout;

  private XWindowPeer windowPeer;

  private XToolkit toolkit;

  private XMutableTreeDataModel dataModel;

  private Map<String, XMutableTreeNode> menus;
  private Map<String, UIElementAction> actions;
  private Map<String, UIElementAction> searchActions;

  private XTreeControl tree;

  private AbstractMouseListener xMouseListener = new AbstractMouseListener()
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
          UIElementAction action = actions.get(node.getDataValue());
          if (action != null)
          {
            action.performAction();
          }
        }
      } catch (Exception ex)
      {
        LOGGER.error("", ex);
      }
    }
  };

  private UIFactory uiFactory;
  
  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      layout.layout();
    }
  };

  public WollMuxSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;

    menus = new HashMap<>();
    actions = new HashMap<>();
    searchActions = new HashMap<>();

    this.parentWindow.addWindowListener(this.windowAdapter);
    layout = new SimpleLayoutManager(this.parentWindow);

    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    boolean allowUserConfig = true;
    try
    {
      allowUserConfig =
        conf.query(WollMuxBar.ALLOW_USER_CONFIG, 1).getLastChild().toString().equalsIgnoreCase(
          "true");
    }
    catch (NodeNotFoundException e)
    {}

    readWollMuxBarConf(allowUserConfig, conf);

    XMultiComponentFactory xMCF =
      UnoRuntime.queryInterface(XMultiComponentFactory.class,
        context.getServiceManager());
    XWindowPeer parentWindowPeer =
      UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    if (parentWindowPeer != null)
    {
      toolkit = parentWindowPeer.getToolkit();
      windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
      windowPeer.setBackground(0xffffffff);
      window = UnoRuntime.queryInterface(XWindow.class, windowPeer);

      if (window != null)
      {

        try
        {
          dataModel = GuiFactory.createTreeModel(xMCF, context);

          XMutableTreeNode root = dataModel.createNode("Vorlagen", false);
          dataModel.setRoot(root);

          XControl treeCtrl =
            GuiFactory.createTree(xMCF, context, toolkit, windowPeer, dataModel);
          tree = UnoRuntime.queryInterface(XTreeControl.class, treeCtrl);
          layout.add(treeCtrl);

          XWindow treeWnd = UnoRuntime.queryInterface(XWindow.class, treeCtrl);
          treeWnd.addMouseListener(xMouseListener);

          XControl line =
              GuiFactory.createControl(xMCF, context, toolkit, windowPeer,
                "com.sun.star.awt.UnoControlFixedLine", null, null, new Rectangle(0, 0, 10, 4));

          layout.add(line);

          uiFactory = new UIFactory(config);
          uiFactory.addElementCreateListener(this);

          ConfigThingy menubar = conf.query("Menueleiste");
          ConfigThingy menuConf = conf.query("Menues");

          if (menubar.count() > 0)
          {
            uiFactory.createUIElements(new UIElementContext(), null,
              menubar.getLastChild(), false);

            for (ConfigThingy menuDef : menuConf.getLastChild())
            {
              uiFactory.createUIElements(new UIElementContext(), menuDef,
                menuDef.getLastChild(), true);
            }
          }

          ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
          uiFactory.createUIElements(new UIElementContext(), menuConf,
            bkl.getLastChild(), false);

          tree.expandNode(root);
          window.setVisible(true);
        }
        catch (Exception ex)
        {
          LOGGER.error("", ex);
        }
      }
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
    // a real extension, you should
    // implement this correctly :)
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
    return 100;
  }

  private static void readWollMuxBarConf(boolean allowUserConfig,
      ConfigThingy wollmuxConf)
  {
    ConfigThingy wollmuxbarConf = null;
    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);

    if (wollmuxbarConfFile.exists())
    {
      if (allowUserConfig)
      {
        try
        {
          wollmuxbarConf =
            new ConfigThingy("wollmuxbarConf", wollmuxbarConfFile.toURI().toURL());
        }
        catch (Exception x)
        {
          LOGGER.error(
            L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
        }
      }
      else
      {
        LOGGER.debug(L.m(
          "Die Verwendung der Konfigurationsdatei '%1' ist deaktiviert. Sie wird nicht ausgewertet!",
          wollmuxbarConfFile.toString()));
      }
    }

    if (wollmuxbarConf == null) wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    ConfigThingy combinedConf = new ConfigThingy("combinedConf");
    combinedConf.addChild(wollmuxConf);
    combinedConf.addChild(wollmuxbarConf);

    try
    {
      LOGGER.debug(L.m("WollMuxBar gestartet"));

      if (combinedConf.query("Symbolleisten").count() == 0)
      {
        LOGGER.error(WollMuxBar.WOLLMUX_CONFIG_ERROR_MESSAGE);
        InfoDialog.showInfoModal(L.m("Fehlerhafte Konfiguration"), WollMuxBar.WOLLMUX_CONFIG_ERROR_MESSAGE);
      }
      else
      {
        config =
          new WollMuxBarConfig(0, wollmuxConf, wollmuxbarConf, allowUserConfig);
      }
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  @Override
  public void createControl(UIControl<?> element)
  {
    if (element == null)
    {
      LOGGER.debug("Unbekanntes Element.");
      return;
    }

    try
    {
      if (element.getClass().equals(UIButton.class))
      {
        final UIButton uiButton = (UIButton) element;
        XControl button =
          GuiFactory.createButton(UNO.xMCF, context, toolkit, windowPeer,
            uiButton.getLabel(), null, new Rectangle(0, 0, 100, 32));

        XButton xbutton = UnoRuntime.queryInterface(XButton.class, button);
        AbstractActionListener xButtonAction = event -> uiButton.getAction().performAction();
        xbutton.addActionListener(xButtonAction);
        layout.add(button);
      }
      else if (element.getClass().equals(UISenderbox.class))
      {
        createSenderbox((UISenderbox) element);
      }
      else if (element instanceof UISearchbox)
      {
        createSearchbox((UISearchbox) element);
      }
      else if (element.getClass().equals(UIMenu.class))
      {
        UIMenu menu = (UIMenu) element;

        XMutableTreeNode node = dataModel.createNode(menu.getLabel(), false);
        if (menu.getParent() == null)
        {
          ((XMutableTreeNode) dataModel.getRoot()).appendChild(node);
        }
        else
        {
          menus.get(menu.getParent()).appendChild(node);
        }
        menus.put(menu.getId(), node);
      }
      else if (element.getClass().equals(UIMenuItem.class))
      {
        UIMenuItem menuItem = (UIMenuItem) element;
        if (menus.get(menuItem.getParent()) != null)
        {
          XMutableTreeNode node = dataModel.createNode(menuItem.getLabel(), false);
          menus.get(menuItem.getParent()).appendChild(node);

          UUID uuid = UUID.randomUUID();
          actions.put(uuid.toString(), menuItem.getAction());
          node.setDataValue(uuid.toString());
        }
      }
    }
    catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  private void createSearchbox(UISearchbox element)
      throws com.sun.star.uno.Exception, UnknownPropertyException,
      PropertyVetoException, WrappedTargetException
  {
    String label = L.m("Suchen...");

    XControl searchBox =
      GuiFactory.createCombobox(UNO.xMCF, context, toolkit, windowPeer, label,
        new Rectangle(0, 0, 100, 32));

    final XWindow wnd = UnoRuntime.queryInterface(XWindow.class, searchBox);
    XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, searchBox);

    XControlModel model = searchBox.getModel();
    XPropertySet props =
        UnoRuntime.queryInterface(XPropertySet.class, model);
    props.setPropertyValue("TextColor", new Integer(SystemColor.textInactiveText.getRGB() & ~0xFF000000));

    searchBox.setModel(model);

    AbstractTextListener tfListener = event -> {
      XControl ctrl = UnoRuntime.queryInterface(XControl.class, event.Source);
      XTextComponent tfComponent = UnoRuntime.queryInterface(XTextComponent.class, event.Source);
      XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, event.Source);
      String text = tfComponent.getText();

      XControlModel tfModel = ctrl.getModel();
      XItemList items = UnoRuntime.queryInterface(XItemList.class, tfModel);

      if (text.length() > 0)
      {
        String[] words = text.split("\\s+");
        try
        {
          cmb.removeItems((short) 0, cmb.getItemCount());
          searchActions.clear();

          ConfigThingy menues = WollMuxFiles.getWollmuxConf().get("Menues");
          ConfigThingy labels = menues.queryAll("LABEL", 4, true);

          int n = 0;
          for (ConfigThingy l : labels)
          {
            ConfigThingy type = l.query("TYPE");
            if (type.count() != 0 && (type.getLastChild().toString().equals("button") || type.getLastChild().toString().equals("menu")))
            {
              ConfigThingy action = l.query("ACTION");
              if (action.count() != 0)
              {
                if (SearchBox.buttonMatches(l, words))
                {
                  UIMenuItem item = (UIMenuItem) uiFactory.createUIMenuElement(null, l, "");
                  items.insertItemText(n, item.getLabel());
                  UUID uuid = UUID.randomUUID();
                  searchActions.put(uuid.toString(), item.getAction());
                  items.setItemData(n, uuid.toString());
                  n++;
                }
              }
            }
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };
    tf.addTextListener(tfListener);

    AbstractFocusListener wndListener = new AbstractFocusListener()
    {
      @Override
      public void focusLost(FocusEvent event)
      {
        try
        {
          XControl searchBox =
            UnoRuntime.queryInterface(XControl.class, event.Source);
          XTextComponent tf =
            UnoRuntime.queryInterface(XTextComponent.class, searchBox);

          wnd.setPosSize(0, 0, 0, 32, PosSize.HEIGHT);

          if (tf.getText().isEmpty())
          {
            tf.setText(L.m("Suchen..."));
            XControlModel model = searchBox.getModel();
            XPropertySet props =
              UnoRuntime.queryInterface(XPropertySet.class, model);
            props.setPropertyValue("TextColor", new Integer(
              SystemColor.textInactiveText.getRGB() & ~0xFF000000));
          }

          windowPeer.invalidate((short)(InvalidateStyle.UPDATE | InvalidateStyle.TRANSPARENT));
          layout.layout();
        }
        catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }

      @Override
      public void focusGained(FocusEvent event)
      {
        try
        {
          XControl searchBox =
            UnoRuntime.queryInterface(XControl.class, event.Source);
          XTextComponent tf = UnoRuntime.queryInterface(XTextComponent.class, searchBox);
          XControlModel model = searchBox.getModel();
          XPropertySet props =
            UnoRuntime.queryInterface(XPropertySet.class, model);

          wnd.setPosSize(0, 0, 0, 320, PosSize.HEIGHT);

          int color = (Integer) props.getPropertyValue("TextColor");
          if (color == (SystemColor.textInactiveText.getRGB() & ~0xFF000000))
          {
            props.setPropertyValue("TextColor", new Integer(SystemColor.textText.getRGB() & ~0xFF000000));
            tf.setText("");
          }
          else
          {
            tf.setSelection(new Selection(0, tf.getText().length()));
          }

          windowPeer.invalidate((short)(InvalidateStyle.UPDATE | InvalidateStyle.TRANSPARENT));
          layout.layout();
        }
        catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    };
    wnd.addFocusListener(wndListener);

    XComboBox cmb = UnoRuntime.queryInterface(XComboBox.class, searchBox);
    AbstractItemListener cmbItemListener = event -> {
      try
      {
        XControl ctrl = UnoRuntime.queryInterface(XControl.class, event.Source);
        XItemList items =
          UnoRuntime.queryInterface(XItemList.class, ctrl.getModel());
        String uuid = (String) items.getItemData(event.Selected);
        UIElementAction action = searchActions.get(uuid);
        if (action != null)
        {
          action.performAction();
        }
      }
      catch (IndexOutOfBoundsException e)
      {
        LOGGER.error("", e);
      }
    };
    cmb.addItemListener(cmbItemListener);

    layout.add(searchBox);
  }

  private void createSenderbox(UISenderbox uiSenderbox)
      throws com.sun.star.uno.Exception, NoSuchMethodException,
      InvocationTargetException
  {
    if (!PersoenlicheAbsenderliste.getInstance().getCurrentSender().isEmpty())
    {
      uiSenderbox.setLabel(PersoenlicheAbsenderliste.getInstance().getCurrentSender().split("§§%=%§§")[0]);
    }

    XControl button =
        GuiFactory.createSenderbox(UNO.xMCF, context, toolkit, windowPeer,
          uiSenderbox.getLabel(), null, new Rectangle(0, 0, 100, 32));
    final XButton xbutton = UnoRuntime.queryInterface(XButton.class, button);

    String[] palEntries = PersoenlicheAbsenderliste.getInstance().getPALEntries();

    final XPopupMenu menu = UnoRuntime.queryInterface(XPopupMenu.class, UNO.xMCF.createInstanceWithContext("com.sun.star.awt.PopupMenu", context));
    XMenu xMenu = UnoRuntime.queryInterface(XMenu.class, menu);

    XIntrospection intro = UnoRuntime.queryInterface(XIntrospection.class, UNO.xMSF.createInstance("com.sun.star.beans.Introspection"));
    XIntrospectionAccess access = intro.inspect(xMenu);
    final XIdlMethod m_execute = access.getMethod("execute", MethodConcept.ALL);

    xMenu.addMenuListener(new AbstractMenuListener()
    {
      @Override
      public void itemSelected(MenuEvent event)
      {
        XMenu menu = UnoRuntime.queryInterface(XMenu.class, event.Source);
        try
        {
          String name = (String) UnoReflect.with(menu).method("getCommand").withArgs(new Short(event.MenuId)).invoke();
          short pos = (Short) UnoReflect.with(menu).method("getItemPos").withArgs(new Short(event.MenuId)).invoke();
          WollMuxEventHandler.getInstance().handleSetSender(name, pos);
        }
        catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    });


    short n = 0;
    for (String entry : palEntries)
    {
      menu.insertItem((short)(n+1), entry.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0], (short) 0, (short) (n+1));
      UnoReflect.with(menu).method("setCommand").withArgs(new Short((short)(n+1)), entry).invoke();
      n++;
    }

    PersoenlicheAbsenderliste.getInstance().addPALChangeEventListener(new XPALChangeEventListener()
    {
      @Override
      public void disposing(EventObject arg0)
      {}

      @Override
      public void updateContent(EventObject eventObject)
      {
        XPALProvider palProv =
            UnoRuntime.queryInterface(XPALProvider.class,
            eventObject.Source);
          if (palProv != null)
          {
            try
            {
              String[] entries = palProv.getPALEntries();
              String current = palProv.getCurrentSender();

              xbutton.setLabel(current.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0]);
              UnoReflect.with(menu).method("clear").invoke();

              short n = 0;
              for (String entry : entries)
              {
                menu.insertItem((short)(n+1), entry.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0], (short) 0, (short) (n+1));
                UnoReflect.with(menu).method("setCommand").withArgs(new Short((short)(n+1)), entry).invoke();
                n++;
              }
            }
            catch (Exception x)
            {
              LOGGER.error("", x);
            }
          }
      }
    });

    AbstractActionListener xButtonAction = event -> {
      try
      {
        final XWindow wndButton = UnoRuntime.queryInterface(XWindow.class, event.Source);
        Rectangle posSize = wndButton.getPosSize();
        // short n = menu.execute(windowPeer, new Rectangle(posSize.X, posSize.Height, 0, 0),
        // (short)0);
        m_execute.invoke(menu, new Object[][] { new Object[] { windowPeer,
            new Rectangle(posSize.X, posSize.Y + posSize.Height, 0, 0), new Short((short) 0) } });
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    };
    
    xbutton.addActionListener(xButtonAction);

    layout.add(button);
  }
}
