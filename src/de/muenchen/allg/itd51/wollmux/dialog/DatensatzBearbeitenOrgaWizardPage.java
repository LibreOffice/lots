package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;

public class DatensatzBearbeitenOrgaWizardPage extends AbstractXWizardPage
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenOrgaWizardPage.class);
  private final short pageId;
  private XWindow window;

  public DatensatzBearbeitenOrgaWizardPage(XWindow parentWindow, short pageId)
  {
    this.pageId = pageId;

    String resultUrl = null;
    resultUrl = UNO.convertFilePathToURL("/home/aero/Downloads/DatensatzBearbeitenOrga.xdl");

    XWindowPeer peer = UNO.XWindowPeer(parentWindow);
    XContainerWindowProvider provider;
    try
    {
      provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
      window = provider.createContainerWindow(resultUrl, "", peer, null);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public void dispose()
  {
    window.dispose();
  }

  @Override
  public void activatePage()
  {
    window.setVisible(true);
  }

  @Override
  public boolean commitPage(short arg0)
  {
    window.setVisible(false);

    return true;
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
