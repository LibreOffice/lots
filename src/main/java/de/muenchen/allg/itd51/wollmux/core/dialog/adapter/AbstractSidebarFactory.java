package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.XComponentContext;

/**
 * Base implementation for sidebar factories.
 */
public abstract class AbstractSidebarFactory extends WeakBase
    implements XUIElementFactory, XServiceInfo
{

  /**
   * The context of the factory.
   */
  protected final XComponentContext context;

  @SuppressWarnings("java:S116")
  private final String __serviceName;

  /**
   * Create a new factory.
   * 
   * @param serviceName
   *          The service this factory can create.
   * @param context
   *          The context of the factory.
   */
  public AbstractSidebarFactory(String serviceName, XComponentContext context)
  {
    this.__serviceName = serviceName;
    this.context = context;
  }

  @Override
  public String getImplementationName()
  {
    return this.getClass().getName();
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
    {
      if (supportedServiceName.equals(serviceName))
      {
	return true;
      }
    }
    return false;
  }
}
