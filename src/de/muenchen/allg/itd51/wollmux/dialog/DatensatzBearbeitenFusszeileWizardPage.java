package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XEventListener;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;

public class DatensatzBearbeitenFusszeileWizardPage implements XWizardPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DatensatzBearbeitenFusszeileWizardPage.class);
  private final short pageId;
  private final XWindow window;

  public DatensatzBearbeitenFusszeileWizardPage(XWindow parentWindow, short pageId) throws Exception
  {
    this.pageId = pageId;

    String resultUrl = null;
    try
    {
      resultUrl = UNO.convertFilePathToURL("/home/bjr/Downloads/DatensatzBearbeitenFusszeile.xdl");
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }

    XWindowPeer peer = UNO.XWindowPeer(parentWindow);
    XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
        UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
            UNO.defaultContext));
    window = provider.createContainerWindow(resultUrl, "", peer, null);
  }

  @Override
  public void addEventListener(XEventListener arg0)
  {
    // unused
  }

  @Override
  public void dispose()
  {
    window.dispose();
  }

  @Override
  public void removeEventListener(XEventListener arg0)
  {
    // unused
  }

  @Override
  public void activatePage()
  {
    window.setVisible(true);
  }

  @Override
  public boolean canAdvance()
  {
    return true;
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