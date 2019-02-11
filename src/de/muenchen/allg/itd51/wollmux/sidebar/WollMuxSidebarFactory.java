package de.muenchen.allg.itd51.wollmux.sidebar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Dies ist die Implementation der Sidebar-Factory, die in Factories.xcu
 * definiert ist. Die Factory erzeugt ein {@link WollMuxSidebarPanel}.  
 *
 */
public class WollMuxSidebarFactory extends WeakBase implements XUIElementFactory,
    XServiceInfo
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WollMuxSidebarFactory.class);

  public static final String __serviceName =
    "de.muenchen.allg.itd51.wollmux.sidebar.WollMuxSidebarFactory";
  
  private XComponentContext context;

  public WollMuxSidebarFactory(XComponentContext context)
  {
    LOGGER.debug("WollMuxSidebarFactory:WollMuxSidebarFactory");
    this.context = context;
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException, IllegalArgumentException
  {
    LOGGER.debug("WollMuxSidebarFactory:createUIElement");

    if (!resourceUrl.startsWith("private:resource/toolpanel/WollMuxSidebarFactory/WollMuxSidebarPanel"))
    {
      throw new NoSuchElementException(resourceUrl, this);
    }

    XWindow parentWindow = null;
    for (int i = 0; i < arguments.length; i++)
    {
      if (arguments[i].Name.equals("ParentWindow"))
      {
        parentWindow =
          UnoRuntime.queryInterface(XWindow.class, arguments[i].Value);
        break;
      }
    }

    return new WollMuxSidebarPanel(context, parentWindow, resourceUrl);
  }

  @Override
  public String getImplementationName()
  {
    return WollMuxSidebarFactory.class.getName();
  }

  @Override
  public String[] getSupportedServiceNames()
  {
    return new String[] { __serviceName };
  }

  @Override
  public boolean supportsService(String serviceName)
  {
    for (final String supportedServiceName : getSupportedServiceNames())
      if (supportedServiceName.equals(serviceName)) return true;
    return false;
  }
}
