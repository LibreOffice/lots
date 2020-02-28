package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;

/**
 * Factory for creating the sidebar.
 */
public class MailMergeFactory extends WeakBase implements XUIElementFactory, XServiceInfo
{

  @SuppressWarnings("java:S115")
  public static final String SERVICE_NAME = "de.muenchen.allg.itd51.wollmux.mailmerge.sidebar."
      + "SeriendruckSidebarFactory";

  private XComponentContext context;

  /**
   * Create the factory.
   *
   * @param context
   *          The context of the factory.
   */
  public MailMergeFactory(XComponentContext context)
  {
    this.context = context;
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException
  {
    if (!resourceUrl
        .startsWith("private:resource/toolpanel/SeriendruckSidebarFactory/SeriendruckSidebar"))
    {
      throw new NoSuchElementException(resourceUrl, this);
    }

    XWindow parentWindow = null;
    XModel model = null;
    for (int i = 0; i < arguments.length; i++)
    {
      if (arguments[i].Name.equals("ParentWindow"))
      {
        parentWindow = UNO.XWindow(arguments[i].Value);
      } else if (arguments[i].Name.equals("Controller"))
      {
        model = UNO.XController(arguments[i].Value).getModel();
      }
    }

    return new MailMergePanel(context, parentWindow, model, resourceUrl);
  }

  @Override
  public String getImplementationName()
  {
    return MailMergeFactory.class.getName();
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
