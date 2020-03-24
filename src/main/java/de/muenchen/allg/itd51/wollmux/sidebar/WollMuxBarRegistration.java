package de.muenchen.allg.itd51.wollmux.sidebar;

import de.muenchen.allg.itd51.wollmux.ComponentRegistration;

/**
 * Factory for {@link WollMuxSidebarFactory}.
 */
public class WollMuxBarRegistration implements ComponentRegistration
{

  @Override
  public Class<?> getComponent()
  {
    return WollMuxSidebarFactory.class;
  }

  @Override
  public String getName()
  {
    return WollMuxSidebarFactory.class.getName();
  }

  @Override
  public String[] getServiceNames()
  {
    return new String[] { WollMuxSidebarFactory.SERVICE_NAME };
  }

}
