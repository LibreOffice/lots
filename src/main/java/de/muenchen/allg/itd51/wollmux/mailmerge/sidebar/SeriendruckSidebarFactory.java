package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class SeriendruckSidebarFactory extends WeakBase implements XUIElementFactory, XServiceInfo
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SeriendruckSidebarFactory.class);

  public static final String __serviceName = "de.muenchen.allg.itd51.wollmux.mailmerge.sidebar.SeriendruckSidebarFactory";

  private XComponentContext context;

  public SeriendruckSidebarFactory(XComponentContext context)
  {
    LOGGER.debug("SeriendruckSidebarFactory");
    this.context = context;
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException
  {
    LOGGER.debug("SeriendruckSidebarFactory:createUIElement");

    if (!resourceUrl
        .startsWith("private:resource/toolpanel/SeriendruckSidebarFactory/SeriendruckSidebar"))
    {
      throw new NoSuchElementException(resourceUrl, this);
    }

    XWindow parentWindow = null;
    for (int i = 0; i < arguments.length; i++)
    {
      if (arguments[i].Name.equals("ParentWindow"))
      {
        parentWindow = UnoRuntime.queryInterface(XWindow.class, arguments[i].Value);
        break;
      }
    }

    return new SeriendruckSidebarPanel(context, parentWindow, resourceUrl);
  }

  @Override
  public String getImplementationName()
  {
    return SeriendruckSidebarFactory.class.getName();
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
      if (supportedServiceName.equals(serviceName))
        return true;
    return false;
  }
}
