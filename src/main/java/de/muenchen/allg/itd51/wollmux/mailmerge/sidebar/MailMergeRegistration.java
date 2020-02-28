package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import de.muenchen.allg.itd51.wollmux.ComponentRegistration;

/**
 * Factory for {@link SeriendruckSidebarFactory}.
 */
public class MailMergeRegistration implements ComponentRegistration
{

  @Override
  public Class<?> getComponent()
  {
    return SeriendruckSidebarFactory.class;
  }

  @Override
  public String getName()
  {
    return SeriendruckSidebarFactory.class.getName();
  }

  @Override
  public String[] getServiceNames()
  {
    return new String[] { SeriendruckSidebarFactory.__serviceName };
  }

}
