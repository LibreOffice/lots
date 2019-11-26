package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XEventListener;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;

public abstract class AbstractXWizardPage implements XWizardPage
{

  protected short pageId;
  protected XWindow window;

  public AbstractXWizardPage(short pageId, XWindow parentWindow, String dialogName) throws Exception
  {
    this.pageId = pageId;

    XWindowPeer peer = UNO.XWindowPeer(parentWindow);
    XContainerWindowProvider provider;

    provider = UnoRuntime.queryInterface(XContainerWindowProvider.class, UNO.xMCF
        .createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider", UNO.defaultContext));
    window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux." + dialogName + "?location=application", "", peer, null);
  }

  @Override
  public void addEventListener(XEventListener arg0)
  {
    // default implementation
  }

  @Override
  public void dispose()
  {
    window.dispose();
  }

  @Override
  public void removeEventListener(XEventListener arg0)
  {
    // default implementation
  }

  @Override
  public void activatePage()
  {
    window.setVisible(true);
  }

  @Override
  public boolean canAdvance()
  {
    return false;
  }

  @Override
  public boolean commitPage(short arg0)
  {
    return false;
  }

  @Override
  public short getPageId()
  {
    return pageId;
  }

  @Override
  public XWindow getWindow()
  {
    return window;
  }

}
