package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.Map;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTabController;
import com.sun.star.awt.XTabControllerModel;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XUnoControlContainer;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPageContainerModel;
import com.sun.star.awt.tab.XTabPageModel;
import com.sun.star.document.EventObject;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.form.model.Tab;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class FormularGuiSidebarContent implements XToolPanel, XSidebarPanel
{
  private XComponentContext context;
  private XWindow parentWindow;
  private XToolkit toolkit;
  private XWindowPeer windowPeer;
  private XWindow window;
  private XMultiComponentFactory xMCF;

  // FormController formController, ConfigThingy formFensterConf
  public FormularGuiSidebarContent(XComponentContext context, XWindow parentWindow)
  {
    this.context = context;
    this.parentWindow = parentWindow;

    xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, context.getServiceManager());
    XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

    toolkit = parentWindowPeer.getToolkit();
    windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
    windowPeer.setBackground(0xffffffff);
    window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
    // WollMuxEventHandler.getInstance().handleInitFormularGuiSidebar(this);
    WollMuxEventHandler.getInstance().handleAddDocumentEventListener(new XEventListener()
    {

      @Override
      public void disposing(com.sun.star.lang.EventObject arg0)
      {
        // TODO Auto-generated method stub

      }

      @Override
      public void notifyEvent(EventObject arg0)
      {
        init();
      }
    });
  }

  public void init()
  {
    FormModel model = null;
    try
    {
      model = DocumentManager.getDocumentManager()
          .getTextDocumentController(UNO.getCurrentTextDocument()).getFormModel();
    } catch (FormModelException e)
    {
      //
    }

    if (model == null)
      return;

    window.setVisible(true);
    XTabPageContainerModel tabContainerModel = initTabControl();

    for (Map.Entry<String, Tab> entry : model.getTabs().entrySet())
    {
      Tab tab = entry.getValue();
      createTab(tabContainerModel, tab.getTitle());
    }
  }

  private void createTab(XTabPageContainerModel model, String tabTitle)
  {
    XTabPageModel tabPageModel = model.createTabPage((short) 1);
    tabPageModel.setTitle(tabTitle);
    tabPageModel.setEnabled(true);
    try
    {
      model.insertByIndex(0, tabPageModel);
    } catch (IllegalArgumentException | IndexOutOfBoundsException | WrappedTargetException e)
    {
      //
    }
  }

  private XTabPageContainerModel initTabControl()
  {
    Object tabPagesContainerModel = UNO
        .createUNOService("com.sun.star.awt.tab.UnoControlTabPageContainerModel");
    XTabPageContainerModel xTabPageContainerModel = UnoRuntime
        .queryInterface(XTabPageContainerModel.class, tabPagesContainerModel);

    Object tabController = UNO.createUNOService("com.sun.star.awt.TabController");
    XTabController xTabController = UnoRuntime.queryInterface(XTabController.class, tabController);

    Object tabControllerModel = UNO.createUNOService("com.sun.star.awt.TabControllerModel");
    XTabControllerModel tabCModel = UnoRuntime.queryInterface(XTabControllerModel.class,
        tabControllerModel);
    xTabController.setModel(tabCModel);

    xTabController.setContainer(UnoRuntime.queryInterface(XControlContainer.class, this));

    XUnoControlContainer xcc = UnoRuntime.queryInterface(XUnoControlContainer.class, this);
    xcc.addTabController(xTabController);

    xTabController.activateFirst();

    return xTabPageContainerModel;
  }

  @Override
  public LayoutSize getHeightForWidth(int arg0)
  {
    // int height = layout.getHeight();
    return new LayoutSize(200, 200, 200);
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

}