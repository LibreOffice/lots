package de.muenchen.allg.itd51.wollmux.form.sidebar;

import de.muenchen.allg.itd51.wollmux.ComponentRegistration;

/**
 * Factory for {@link FormFactory}.
 */
public class FormSidebarRegistration implements ComponentRegistration
{

  @Override
  public Class<?> getComponent()
  {
    return FormFactory.class;
  }

  @Override
  public String getName()
  {
    return FormFactory.class.getName();
  }

  @Override
  public String[] getServiceNames()
  {
    return new String[] { FormFactory.SERVICE_NAME };
  }

}
