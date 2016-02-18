package de.muenchen.allg.itd51.wollmux.sidebar;

import com.sun.star.awt.MenuEvent;
import com.sun.star.awt.XMenuListener;
import com.sun.star.lang.EventObject;

public abstract class AbstractMenuListener implements XMenuListener
{

  @Override
  public void disposing(EventObject event)
  {}

  @Override
  public void activate(MenuEvent event)
  {}

  @Override
  public void deactivate(MenuEvent event)
  {}

  @Override
  public void highlight(MenuEvent event)
  {}

  @Override
  public void select(MenuEvent event)
  {}
}
