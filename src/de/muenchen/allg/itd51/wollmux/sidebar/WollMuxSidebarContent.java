package de.muenchen.allg.itd51.wollmux.sidebar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.MenuEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XMenu;
import com.sun.star.awt.XMouseListener;
import com.sun.star.awt.XPopupMenu;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowListener;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.MethodConcept;
import com.sun.star.beans.XIntrospection;
import com.sun.star.beans.XIntrospectionAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.reflection.XIdlMethod;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XPALProvider;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementContext;
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

/**
 * Erzeugt das Fenster, dass in der WollMux-Sidebar angezeigt wird. Das Fenster
 * enthält einen Baum zur Auswahl von Vorlagen und darunter eine Reihe von
 * Buttons für häufig benutzte Funktionen. 
 * 
 */
public class WollMuxSidebarContent extends ComponentBase implements XToolPanel,
    XSidebarPanel, XWindowListener, UIElementCreateListener
{
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

  private XTreeControl tree;

  private XMouseListener xMouseListener;

  public WollMuxSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;

    menus = new HashMap<String, XMutableTreeNode>();
    actions = new HashMap<String, UIElementAction>();

    this.parentWindow.addWindowListener(this);
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

    readWollMuxBarConf(false, allowUserConfig, false, conf);

    XMultiComponentFactory xMCF =
      UnoRuntime.queryInterface(XMultiComponentFactory.class,
        context.getServiceManager());
    XWindowPeer parentWindowPeer =
      UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    if (parentWindowPeer != null)
    {
      Rectangle windowSize = parentWindow.getPosSize();
      toolkit = parentWindowPeer.getToolkit();
      windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
      windowPeer.setBackground(0xffffffff);
      window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
      XControlContainer cc =
        UnoRuntime.queryInterface(XControlContainer.class, window);

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

          XPropertySet props =
            UnoRuntime.queryInterface(XPropertySet.class, treeCtrl.getModel());
          props.setPropertyValue("DataModel", dataModel);

          XWindow treeWnd = UnoRuntime.queryInterface(XWindow.class, treeCtrl);
          xMouseListener = new TreeMouseListener();
          treeWnd.addMouseListener(xMouseListener);
          
          XControl line =
              GuiFactory.createControl(xMCF, context, toolkit, windowPeer,
                "com.sun.star.awt.UnoControlFixedLine", null, null, new Rectangle(0, 0, 10, 4));
          
          layout.add(line);

          UIFactory factory = new UIFactory(config);
          factory.addElementCreateListener(this);

          ConfigThingy menubar = conf.query("Menueleiste");
          ConfigThingy menuConf = conf.query("Menues");

          if (menubar.count() > 0)
          {
            factory.createUIElements(new UIElementContext(), null,
              menubar.getLastChild(), false);

            for (ConfigThingy menuDef : menuConf.getLastChild())
            {
              factory.createUIElements(new UIElementContext(), menuDef,
                menuDef.getLastChild(), true);
            }
          }

          ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
          factory.createUIElements(new UIElementContext(), menuConf,
            bkl.getLastChild(), false);

          tree.expandNode(root);
          window.setVisible(true);
        }
        catch (Exception ex)
        {
          Logger.error(ex);
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
    return new LayoutSize(500, -1, 500);
  }

  @Override
  public int getMinimalWidth()
  {
    return 100;
  }

  @Override
  public void disposing(EventObject arg0)
  {}

  @Override
  public void windowHidden(EventObject arg0)
  {}

  @Override
  public void windowMoved(WindowEvent arg0)
  {}

  @Override
  public void windowResized(WindowEvent e)
  {
    layout.layout();
  }

  @Override
  public void windowShown(EventObject arg0)
  {}

  private static void readWollMuxBarConf(boolean menumanager,
      boolean allowUserConfig, boolean allowMenuManager, ConfigThingy wollmuxConf)
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
          Logger.error(
            L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
        }
      }
      else
      {
        Logger.debug(L.m(
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
      Logger.debug(L.m("WollMuxBar gestartet"));

      if (combinedConf.query("Symbolleisten").count() == 0)
      {
        Logger.error(WollMuxBar.WOLLMUX_CONFIG_ERROR_MESSAGE);
        JOptionPane.showMessageDialog(null, WollMuxBar.WOLLMUX_CONFIG_ERROR_MESSAGE,
          L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
      }
      else
      {
        config =
          new WollMuxBarConfig(0, wollmuxConf, wollmuxbarConf, allowUserConfig);
      }

      if (menumanager)
      {}

    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  @Override
  public void createControl(UIControl<?> element)
  {
    if (element == null)
    {
      Logger.debug("Unbekanntes Element.");
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
        xbutton.addActionListener(new XActionListener()
        {
          @Override
          public void disposing(EventObject arg0)
          {}
  
          @Override
          public void actionPerformed(ActionEvent arg0)
          {
            uiButton.getAction().performAction();
          }
        });
        layout.add(button);
      }
      else if (element.getClass().equals(UISenderbox.class))
      {
        final UISenderbox uiSenderbox = (UISenderbox) element;
        if (!PersoenlicheAbsenderliste.getInstance().getCurrentSender().isEmpty())
        {
          uiSenderbox.setLabel(PersoenlicheAbsenderliste.getInstance().getCurrentSender().split("§§%=%§§")[0]);
        }
        
        String[] palEntries = PersoenlicheAbsenderliste.getInstance().getPALEntries();
        
        final XPopupMenu menu = UnoRuntime.queryInterface(XPopupMenu.class, UNO.xMCF.createInstanceWithContext("com.sun.star.awt.PopupMenu", context));
        XMenu xMenu = UnoRuntime.queryInterface(XMenu.class, menu);

        XIntrospection intro = UnoRuntime.queryInterface(XIntrospection.class, UNO.xMSF.createInstance("com.sun.star.beans.Introspection"));
        XIntrospectionAccess access = intro.inspect(xMenu);
        XIdlMethod m_setcommand = access.getMethod("setCommand", MethodConcept.ALL);
        final XIdlMethod m_execute = access.getMethod("execute", MethodConcept.ALL);
        final XIdlMethod m_getcommand = access.getMethod("getCommand", MethodConcept.ALL);
        
        xMenu.addMenuListener(new AbstractMenuListener()
        {
          @Override
          public void select(MenuEvent event)
          {
            XMenu menu = UnoRuntime.queryInterface(XMenu.class, event.Source);
            try
            {
              String name = (String)m_getcommand.invoke(menu, new Object[][]{ new Object[]{ new Short(event.MenuId) } });
              WollMuxEventHandler.handleSetSender(name, event.MenuId - 1);
            }
            catch (Exception e)
            {
              Logger.error(e);
            }
          }
        });
        
        short n = 0;
        for (String entry : palEntries)
        {
          menu.insertItem((short)(n+1), entry.split("§§%=%§§")[0], (short) 0, (short) (n+1));
          m_setcommand.invoke(xMenu, new Object[][] { new Object[] { new Short((short)(n+1)), entry } });
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
                  String[] entriesCopy = new String[entries.length];
                  System.arraycopy(entries, 0, entriesCopy, 0, entries.length);
                  //handleSenderboxUpdate(entriesCopy, current);
                }
                catch (Exception x)
                {
                  Logger.error(x);
                }
              }
          }
        });
        
        XControl button =
          GuiFactory.createSenderbox(UNO.xMCF, context, toolkit, windowPeer,
            uiSenderbox.getLabel(), null, new Rectangle(0, 0, 100, 32));
        
        XButton xbutton = UnoRuntime.queryInterface(XButton.class, button);
        xbutton.addActionListener(new XActionListener() {
          @Override
          public void disposing(EventObject arg0)
          {}

          @Override
          public void actionPerformed(ActionEvent event)
          {
            try
            {
              final XWindow wndButton = UnoRuntime.queryInterface(XWindow.class, event.Source);
              Rectangle posSize = wndButton.getPosSize();
              //short n = menu.execute(windowPeer, new Rectangle(posSize.X, posSize.Height, 0, 0), (short)0);
              m_execute.invoke(menu, new Object[][] { new Object[] { windowPeer, new Rectangle(posSize.X, posSize.Y + posSize.Height, 0, 0), new Short((short)0) } });
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
          }
        });
        
        layout.add(button);
      }
      else if (element instanceof UISearchbox)
      {
        UISearchbox uiSearchbox = (UISearchbox) element;
        String label = L.m("Suchen...");
        XControl button =
          GuiFactory.createTextfield(UNO.xMCF, context, toolkit, windowPeer, label,
            new Rectangle(0, 0, 100, 32));
        layout.add(button);
      }
      else if (element.getClass().equals(UIMenu.class))
      {
        UIMenu menu = (UIMenu) element;
  
        XMutableTreeNode node = dataModel.createNode(menu.getLabel(), false);
        ((XMutableTreeNode) dataModel.getRoot()).appendChild(node);
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
      Logger.error(e);
    }
  }

  private final class TreeMouseListener implements XMouseListener
  {
    @Override
    public void disposing(EventObject arg0)
    {
    }
  
    @Override
    public void mouseReleased(MouseEvent arg0)
    {
    }
  
    @Override
    public void mousePressed(MouseEvent event)
    {
      try {
        XMutableTreeNode node = UnoRuntime.queryInterface(XMutableTreeNode.class, tree.getClosestNodeForLocation(event.X, event.Y));
        
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
      }
      catch (Exception ex)
      {
        Logger.error(ex);
      }
    }
  
    @Override
    public void mouseExited(MouseEvent arg0)
    {
    }
  
    @Override
    public void mouseEntered(MouseEvent arg0)
    {
    }
  }
}
