package de.muenchen.allg.itd51.wollmux.comp;

import de.muenchen.allg.itd51.wollmux.ComponentRegistration;

/**
 * Factory for {@link WollMux}.
 */
public class WollMuxRegistration implements ComponentRegistration
{

  @Override
  public Class<?> getComponent()
  {
    return WollMux.class;
  }

  @Override
  public String getName()
  {
    return WollMux.class.getName();
  }

  @Override
  public String[] getServiceNames()
  {
    return WollMux.SERVICENAMES;
  }

}
