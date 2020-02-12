package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.awt.tab.XTabPageContainerModel;
import com.sun.star.awt.tab.XTabPageModel;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.form.model.Tab;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentControllerInitialized;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.Layout;
import de.muenchen.allg.itd51.wollmux.sidebar.layout.VerticalLayout;

public class FormularGuiSidebarContent implements XToolPanel, XSidebarPanel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FormularGuiSidebarContent.class);

  private XComponentContext context;
  private XWindow parentWindow;
  private XWindowPeer parentWindowPeer;
  private XToolkit toolkit;
  private XWindowPeer windowPeer;
  private XWindow window;
  private TextDocumentController documentController;
  private Layout vLayout;
  private Object tabControlContainer;
  private XTabPageContainerModel xTabPageContainerModel;

  private AbstractWindowListener windowAdapter = new AbstractWindowListener()
  {
    @Override
    public void windowResized(WindowEvent e)
    {
      vLayout.layout(parentWindow.getPosSize());
    }
  };

  public FormularGuiSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;
    this.parentWindow.addWindowListener(this.windowAdapter);
    vLayout = new VerticalLayout();

    parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);
    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UnoRuntime.queryInterface(XWindow.class, windowPeer);

    Object cont = UNO.createUNOService("com.sun.star.awt.UnoControlContainer");
    XControl dialogControl = UnoRuntime.queryInterface(XControl.class, cont);

    // Instanzierung eines ControlContainers für das aktuelle Fenster
    Object unoControlContainerModelO = UNO
        .createUNOService("com.sun.star.awt.UnoControlContainerModel");
    XControlModel unoControlContainerModel = UnoRuntime.queryInterface(XControlModel.class,
        unoControlContainerModelO);
    dialogControl.setModel(unoControlContainerModel);

    if (windowPeer != null)
    {
      dialogControl.createPeer(toolkit, parentWindowPeer);
      window = UNO.XWindow(dialogControl);

      // TabControl muss zuerst in den Container eingefügt werden, bevor die TabPages hinzugefügt
      // werden können
      Object tabPageContainerModel = UNO
          .createUNOService("com.sun.star.awt.tab.UnoControlTabPageContainerModel");
      xTabPageContainerModel = UnoRuntime.queryInterface(XTabPageContainerModel.class,
          tabPageContainerModel);

      tabControlContainer = UNO.createUNOService("com.sun.star.awt.tab.UnoControlTabPageContainer");
      XControl xControl = UnoRuntime.queryInterface(XControl.class, tabControlContainer);

      XControlModel xControlModel = UnoRuntime.queryInterface(XControlModel.class,
          tabPageContainerModel);
      xControl.setModel(xControlModel);
      Object toolkitAWT = UNO.createUNOService("com.sun.star.awt.Toolkit");
      XToolkit xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkitAWT);
      xControl.createPeer(xToolkit, dialogControl.getPeer());

      vLayout.addControl(xControl);

      window.setVisible(true);
      window.setEnable(true);
    }

    init();
    WollMuxEventHandler.getInstance().registerListener(this);
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

    documentController = controller;

    init();
  }

  private short tabPagesCount = 0;

  private void init()
  {
    if (documentController == null)
    {
      LOGGER.debug("{} documentController is NULL.", this.getClass().getSimpleName());
      return;
    }

    WollMuxEventHandler.getInstance().unregisterListener(this);

    FormModel model = null;
    try
    {
      model = documentController.getFormModel();
    } catch (FormModelException e)
    {
      LOGGER.error("", e);
      return;
    }

    if (model == null)
    {
      LOGGER.debug("{} FormModel is NULL. No form description found.",
          this.getClass().getSimpleName());
      return;
    }

    window.setVisible(true);

    Map<Short, List<XControl>> buttonsToAdd = new HashMap<>();

    for (Map.Entry<String, Tab> entry : model.getTabs().entrySet())
    {
      Tab tab = entry.getValue();
      XTabPage xTabPage = createTab(xTabPageContainerModel, tab.getTitle());
      XControlContainer tabPageControlContainer = UnoRuntime.queryInterface(XControlContainer.class,
          UNO.XControl(xTabPage));

      tab.getButtons().forEach(button -> {
        List<XControl> buttonList = new ArrayList<>();
        // XControl btn = GuiFactory.createButton(UNO.xMCF, context, toolkit,
        // windowPeer, button.getLabel(),
        // btnActionListener, new Rectangle(0, 0, 50, 50), null);

        XButton btn = createButton(0, 0, 100, button.getLabel());

        // "On-The-Fly" funktioniert nicht.
        // tabPageControlContainer.addControl(generateRandomId(), UNO.XControl(btn));

        buttonList.add(UNO.XControl(btn));
        buttonsToAdd.put(tabPagesCount, buttonList);
        tabPagesCount++;
      });
    }

    XTabPageContainer tabPageContainer = UnoRuntime.queryInterface(XTabPageContainer.class,
        tabControlContainer);
    UNO.XWindow(tabControlContainer).setPosSize(0, 0, 200, 400, PosSize.POSSIZE);
    UNO.XWindow(tabPageContainer).setPosSize(0, 0, 400, 400, PosSize.POSSIZE);

    for (Map.Entry<Short, List<XControl>> entry : buttonsToAdd.entrySet())
    {
      Layout vLayout = new VerticalLayout();

      XTabPage tabPageByIndex = tabPageContainer.getTabPage((short) entry.getKey());
      UNO.XWindow(tabPageByIndex).setPosSize(0, 0, 400, 400, PosSize.POSSIZE);

      XControlContainer cc = UnoRuntime.queryInterface(XControlContainer.class, tabPageByIndex);
      UNO.XWindow(cc).setPosSize(0, 0, 400, 400, PosSize.POSSIZE);

      for (XControl btn : entry.getValue())
      {
        cc.addControl(generateRandomId(), btn);
      }

      // nicht weiter beachten
      // add btn's to layout
      // for (XControl btn : entry.getValue())
      // {
      // // cc.addControl(generateRandomId(), btn);
      // vLayout.addControl(btn);
      // }
      //
      // // compute layout
      // vLayout.layout(UNO.XWindow(cc).getPosSize());
      //
      // // add xcontrols to control container
      // for (Map.Entry<Layout, Integer> layoutEntry : vLayout.getLayouts().entrySet())
      // {
      // cc.addControl(generateRandomId(), layoutEntry.getKey().getControl());
      // }
    }
  }

  private String generateRandomId()
  {
    int leftLimit = 97; // letter 'a'
    int rightLimit = 122; // letter 'z'
    int targetStringLength = 8;
    Random random = new Random();

    return random.ints(leftLimit, rightLimit + 1).limit(targetStringLength)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  private XActionListener btnActionListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // TODO Auto-generated method stub

    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      // TODO Auto-generated method stub

    }
  };

  private int count = 0;

  private XTabPage createTab(XTabPageContainerModel model, String tabTitle)
  {
    XTabPageModel tabPageModel = model.createTabPage((short) (count + 1)); // 0 is not valid;
    tabPageModel.setTitle(tabTitle);
    tabPageModel.setEnabled(true);

    XTabPage xTabPage = null;
    try
    {
      Object tabPageService = UNO.createUNOService("com.sun.star.awt.tab.UnoControlTabPage");
      xTabPage = UnoRuntime.queryInterface(XTabPage.class, tabPageService);
      UNO.XControl(xTabPage).setModel(UnoRuntime.queryInterface(XControlModel.class, tabPageModel));
      model.insertByIndex(count, tabPageModel); // first index must be zero.
    } catch (IllegalArgumentException | IndexOutOfBoundsException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }

    count++;

    return xTabPage;
  }

  private XButton createButton(int posX, int posY, int width, String label)
  {
    XButton xButton = null;
    try
    {
      String sName = "btn" + generateRandomId();
      Object oButtonModel = UNO.xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
      XMultiPropertySet xButtonMPSet = UnoRuntime.queryInterface(XMultiPropertySet.class,
          oButtonModel);

      xButtonMPSet.setPropertyValues(
          new String[] { "Height", "Label", "Name", "PositionX", "PositionY",
              "Width" },
          new Object[] { Integer.valueOf(14), label, sName, Integer.valueOf(posX),
              Integer.valueOf(posY), Integer.valueOf(width) });

      Object buttonService = UNO.createUNOService("com.sun.star.awt.UnoControlButton");
      xButton = UnoRuntime.queryInterface(XButton.class, buttonService);
      UNO.XControl(xButton).setModel(UnoRuntime.queryInterface(XControlModel.class, oButtonModel));
    } catch (com.sun.star.uno.Exception ex)
    {
      //
    }

    return xButton;
  }

  @Override
  public LayoutSize getHeightForWidth(int arg0)
  {
    return new LayoutSize(400, 400, 400);
  }

  @Override
  public int getMinimalWidth()
  {
    return 300;
  }

  @Override
  public XAccessible createAccessible(XAccessible arg0)
  {
    if (window == null)
    {
      throw new DisposedException("", this);
    }

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

}