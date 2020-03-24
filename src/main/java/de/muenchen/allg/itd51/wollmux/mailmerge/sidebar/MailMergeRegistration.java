package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import de.muenchen.allg.itd51.wollmux.ComponentRegistration;

/**
 * Factory for {@link MailMergeFactory}.
 */
public class MailMergeRegistration implements ComponentRegistration
{

  @Override
  public Class<?> getComponent()
  {
    return MailMergeFactory.class;
  }

  @Override
  public String getName()
  {
    return MailMergeFactory.class.getName();
  }

  @Override
  public String[] getServiceNames()
  {
    return new String[] { MailMergeFactory.SERVICE_NAME };
  }

}
