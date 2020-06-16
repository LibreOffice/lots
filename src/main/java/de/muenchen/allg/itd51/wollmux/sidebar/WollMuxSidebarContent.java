package de.muenchen.allg.itd51.wollmux.sidebar;

import java.awt.SystemColor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.beans.MethodConcept;
import com.sun.star.beans.XIntrospection;
import com.sun.star.beans.XIntrospectionAccess;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.reflection.XIdlMethod;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractMenuListener;
import de.muenchen.allg.dialog.adapter.AbstractMouseListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementType;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnDumpInfo;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetSender;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnShowDialogAbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.ui.layout.Layout;
import de.muenchen.allg.itd51.wollmux.ui.layout.VerticalLayout;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;

/**
 * Erzeugt das Fenster, dass in der WollMux-Sidebar angezeigt wird. Das Fenster enthält einen Baum
 * zur Auswahl von Vorlagen und darunter eine Reihe von Buttons für häufig benutzte Funktionen.
 *
 */
public class WollMuxSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxSidebarContent.class);

  public static final String ALLOW_USER_CONFIG = "ALLOW_USER_CONFIG";

  /**
   * Name der Datei in der die WollMuxBar ihre Konfiguration schreibt.
   */
  public static final String WOLLMUXBAR_CONF = "wollmuxbar.conf";

  public static final Set<String> SUPPORTED_ACTIONS = new HashSet<>();
  static
  {
    SUPPORTED_ACTIONS.add("openTemplate");
    SUPPORTED_ACTIONS.add("absenderAuswaehlen");
    SUPPORTED_ACTIONS.add("openDocument");
    SUPPORTED_ACTIONS.add("openExt");
    SUPPORTED_ACTIONS.add("open");
    SUPPORTED_ACTIONS.add("dumpInfo");
    SUPPORTED_ACTIONS.add("abort");
    SUPPORTED_ACTIONS.add("kill");
    SUPPORTED_ACTIONS.add("about");
    SUPPORTED_ACTIONS.add("options");
  }

  public static final String WOLLMUX_CONFIG_ERROR_MESSAGE = L
      .m("Aus Ihrer WollMux-Konfiguration konnte kein Abschnitt \"Symbolleisten\" gelesen werden. "
          + "Die WollMux-Leiste kann daher nicht gestartet werden. Bitte überprüfen Sie, ob in Ihrer wollmux.conf "
          + "der %include für die Konfiguration der WollMuxBar (z.B. wollmuxbar_standard.conf) vorhanden ist und "
          + "überprüfen Sie anhand der wollmux.log ob evtl. beim Verarbeiten eines %includes ein Fehler "
          + "aufgetreten ist.");

  private XComponentContext context;

  private XWindow parentWindow;

  private XControlContainer controlContainer;

  private Layout layout;

  private XWindowPeer windowPeer;

  private XMutableTreeDataModel dataModel;

  private Map<String, XMutableTreeNode> menus;
  private Map<String, Runnable> actions;
  private Map<String, Runnable> searchActions;

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

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      layout.layout(parentWindow.getPosSize());
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
    layout = new VerticalLayout(5, 5, 5, 5, 5);

    DatasourceJoiner dj = DatasourceJoinerFactory.getDatasourceJoiner();

    if (dj.getLOS().size() > 0)
    {
      // Liste der nicht zuordnenbaren Datensätze erstellen und ausgeben:
      StringBuilder names = new StringBuilder();
      List<String> lost = DatasourceJoinerFactory.getLostDatasetDisplayStrings();
      if (!lost.isEmpty())
      {
        for (String l : lost)
        {
          names.append("- " + l + "\n");
        }
        String message = L.m("Die folgenden Datensätze konnten nicht " + "aus der Datenbank aktualisiert werden:\n\n"
            + "%1\n" + "Wenn dieses Problem nicht temporärer "
            + "Natur ist, sollten Sie diese Datensätze aus "
            + "ihrer Absenderliste löschen und neu hinzufügen!", names);
        InfoDialog.showInfoModal(this.parentWindow, L.m("WollMux-Info"), message);
      }
    }

    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    boolean allowUserConfig = true;
    try
    {
      allowUserConfig = conf.query(ALLOW_USER_CONFIG, 1).getLastChild().toString().equalsIgnoreCase("true");
    } catch (NodeNotFoundException e)
    {
      LOGGER.trace("", e);
    }

    XMultiComponentFactory xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
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
        String text = L.m("WollMux läuft ohne wollmux.conf !\n"
            + "Aus diesem Grund ist leider nicht der komplette Funktionsumfang verfügbar.");
        XControl txt = GuiFactory.createLabel(xMCF, context, text, new Rectangle(5, 15, 10, 80), null);
        controlContainer.addControl("txt", txt);
        layout.addControl(txt);
      } else
      {
        readWollMuxBarConf(allowUserConfig, conf);

        dataModel = GuiFactory.createTreeModel(xMCF, context);

        XMutableTreeNode root = dataModel.createNode("Vorlagen", false);
        dataModel.setRoot(root);

        XControl treeCtrl = GuiFactory.createTree(xMCF, context, dataModel);
        tree = UNO.XTreeControl(treeCtrl);
        controlContainer.addControl("treeCtrl", treeCtrl);
        layout.addControl(treeCtrl, 6);

        XWindow treeWnd = UNO.XWindow(treeCtrl);
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
    } catch (Exception ex)
    {
      LOGGER.error("", ex);
    }
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    // TODO: the following is wrong, since it doesn't respect i_rParentAccessible. In
    // a real extension, you should
    // implement this correctly :)
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
    try
    {
      int maxWidth = (int) UnoConfiguration.getConfiguration("org.openoffice.Office.UI.Sidebar/General",
          "MaximumWidth");
      return layout.getMinimalWidth(maxWidth);
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return 300;
    }
  }

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
          LOGGER.error(L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
        }
      } else
      {
        LOGGER.debug(L.m(
            "Die Verwendung der Konfigurationsdatei '%1' ist deaktiviert. Sie wird nicht ausgewertet!",
            wollmuxbarConfFile.toString()));
      }
    }

    if (wollmuxbarConf == null)
      wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    ConfigThingy combinedConf = new ConfigThingy("combinedConf");
    combinedConf.addChild(wollmuxConf);
    combinedConf.addChild(wollmuxbarConf);

    try
    {
      LOGGER.debug(L.m("WollMuxBar gestartet"));

      if (combinedConf.query("Symbolleisten").count() == 0)
      {
        LOGGER.error(WOLLMUX_CONFIG_ERROR_MESSAGE);
        InfoDialog.showInfoModal(L.m("Fehlerhafte Konfiguration"), WOLLMUX_CONFIG_ERROR_MESSAGE);
      }
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  private void createControl(UIElementConfig element, boolean isMenu, String parentEntry)
  {
    if (element == null)
    {
      LOGGER.debug("Unbekanntes Element.");
      return;
    }

    try
    {
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
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Liefert true gdw. das durch button beschriebene Element ein button ist (TYPE-Attribut muss
   * "button" sein) und alle in words enthaltenen strings ohne Beachtung der Groß-/Kleinschreibung
   * im Wert des LABEL-Attributs (das natürlich vorhanden sein muss) vorkommen.
   *
   * @param button
   *          Den ConfigThingy-Knoten, der ein UI-Element beschreibt, wie z.B. "(TYPE 'button' LABEL
   *          'Hallo' ...)"
   * @param words
   *          Diese Wörter müssen ALLE im LABEL vorkommen (ohne Beachtung der
   *          Groß-/Kleinschreibung).
   * @return If the element is a button.
   */
  public static boolean buttonMatches(ConfigThingy button, String[] words)
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

  private void createSearchbox(UIElementConfig element)
  {
    String label = L.m("Suchen...");

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
      XItemList items = UnoRuntime.queryInterface(XItemList.class, UNO.XControl(resultBox).getModel());

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
            searchBox.setText(L.m("Suchen..."));
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

  private void createSenderbox(UIElementConfig uiSenderbox) throws com.sun.star.uno.Exception
  {
    String label = uiSenderbox.getLabel();
    if (!PersoenlicheAbsenderliste.getInstance().getCurrentSender().isEmpty())
    {
      label = PersoenlicheAbsenderliste.getInstance().getCurrentSender().split("§§%=%§§")[0];
    }

    SortedMap<String, Object> props = new TreeMap<>();
    props.put("FocusOnClick", false);
    XControl button = GuiFactory.createButton(UNO.xMCF, context, label, null, new Rectangle(0, 0, 100, 32), props);
    final XButton xbutton = UNO.XButton(button);

    String[] palEntries = PersoenlicheAbsenderliste.getInstance().getPALEntries();

    final XPopupMenu menu = UNO.XPopupMenu(UNO.xMCF.createInstanceWithContext("com.sun.star.awt.PopupMenu", context));
    XMenu xMenu = UNO.XMenu(menu);

    XIntrospection intro = UNO.XIntrospection(UNO.xMSF.createInstance("com.sun.star.beans.Introspection"));
    XIntrospectionAccess access = intro.inspect(xMenu);
    final XIdlMethod executeMethod = access.getMethod("execute", MethodConcept.ALL);

    xMenu.addMenuListener(new AbstractMenuListener()
    {
      @Override
      public void itemSelected(MenuEvent event)
      {
        XMenu menu = UNO.XMenu(event.Source);
        try
        {
          String name = menu.getCommand(event.MenuId);
          short pos = menu.getItemPos(event.MenuId);
          new OnSetSender(name, pos).emit();
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    });

    short n = 0;
    for (String entry : palEntries)
    {
      menu.insertItem((short) (n + 1), entry.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0], (short) 0,
          (short) (n + 1));
      menu.setCommand((short) (n + 1), entry);
      n++;
    }

    PersoenlicheAbsenderliste.getInstance().addPALChangeEventListener(new XPALChangeEventListener()
    {
      @Override
      public void disposing(EventObject arg0)
      {
        // nothing to do
      }

      @Override
      public void updateContent(EventObject eventObject)
      {

        String[] entries = PersoenlicheAbsenderliste.getInstance().getPALEntries();
        String current = PersoenlicheAbsenderliste.getInstance().getCurrentSender();

        xbutton.setLabel(current.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0]);
        menu.clear();

        short n = 0;
        for (String entry : entries)
        {
          menu.insertItem((short) (n + 1), entry.split(PersoenlicheAbsenderliste.SENDER_KEY_SEPARATOR)[0],
              (short) 0,
              (short) (n + 1));
          menu.setCommand((short) (n + 1), entry);
          n++;
        }
      }
    });

    AbstractActionListener xButtonAction = event -> {
      try
      {
        final XWindow wndButton = UNO.XWindow(event.Source);
        Rectangle posSize = wndButton.getPosSize();
        executeMethod.invoke(menu, new Object[][] {
            new Object[] { windowPeer, new Rectangle(posSize.X, posSize.Y + posSize.Height, 0, 0), (short) 0 } });
      } catch (Exception e)
      {
        LOGGER.trace("", e);
      }
    };

    xbutton.addActionListener(xButtonAction);
    controlContainer.addControl(uiSenderbox.getId(), UNO.XControl(xbutton));
    layout.addControl(button);
  }

  /**
   * Die Funktion wird aufgerufen, um einen Bereich der Konfiguration zu parsen und
   * {@link UIControl}s zu erzeugen. Für jedes erzeugte Element werden alle Listener aufgerufen.
   *
   * @param menuConf
   * @param elementParent
   *          Die Konfiguration, die geparst werden soll. Muss eine Liste von Menüs oder Buttons
   *          enthalten.
   * @param isMenu
   *          Wenn true, werden button und menuitem in {@link UIMenuItem} umgewandelt, sonst in
   *          {@link UIButton}.
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

  private void processUiElementEvent(UIElementConfig config)
  {
    String action = config.getAction();
    if (action.equals("absenderAuswaehlen"))
    {
      new OnShowDialogAbsenderAuswaehlen().emit();
    } else if (action.equals("openDocument"))
    {
      String fragId = config.getFragId();
      if (fragId.isEmpty())
      {
        LOGGER.error(L.m("ACTION \"%1\" erfordert mindestens ein Attribut FRAG_ID", action));
      } else
      {
        new OnOpenDocument(Arrays.asList(fragId), false).emit();
      }
    } else if (action.equals("openTemplate"))
    {
      String fragId = config.getFragId();
      if (fragId.isEmpty())
      {
        LOGGER.error(L.m("ACTION \"%1\" erfordert mindestens ein Attribut FRAG_ID", action));
      } else
      {
        new OnOpenDocument(Arrays.asList(fragId), true).emit();
      }
    } else if (action.equals("open"))
    {
      InfoDialog.showInfoModal("Multiformulare werden nicht mehr unterstützt",
          "Multiformulare werden nicht mehr unterstützt. " + "Bitte kontaktieren Sie Ihren Administrator. "
              + "Sie müssen jedes Formular einzeln öffnen und ausfüllen.");
    } else if (action.equals("openExt"))
    {
      executeOpenExt(config.getExt(), config.getUrl());
    } else if (action.equals("dumpInfo"))
    {
      new OnDumpInfo().emit();
    } else if (action.equals("abort"))
    {
      // abort();
    } else if (action.equals("kill"))
    {
      new OnKill().emit();
      // abort();
    } else if (action.equals("about"))
    {
      new OnAbout().emit();
    } else if (action.equals("options"))
    {
      // options();
    }
  }

  /**
   * Führt die gleichnamige ACTION aus.
   *
   * TESTED
   */
  private void executeOpenExt(String ext, String url)
  {
    try
    {
      final OpenExt openExt = OpenExt.getInstance(ext, url);

      try
      {
        openExt.storeIfNecessary();
      } catch (IOException x)
      {
        LOGGER.error("", x);
        showError(L.m("Fehler beim Download der Datei:\n%1", x.getMessage()));
        return;
      }

      Runnable launch = () -> openExt.launch((Exception x) -> {
        LOGGER.error("", x);
        showError(x.getMessage());
      });

      launch.run();
    } catch (Exception x)
    {
      LOGGER.error("", x);
      showError(x.getMessage());
    }
  }

  private void showError(String errorMsg)
  {
    InfoDialog.showInfoModal(L.m("Fehlerhafte Konfiguration"),
        L.m("%1\nVerständigen Sie Ihre Systemadministration.", errorMsg));
  }
}
