package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XWindowListener;
import com.sun.star.lang.EventObject;

public abstract class AbstractWindowListener implements XWindowListener
{

  @Override
  public void disposing(EventObject event)
  {
    // default implementation
  }

  @Override
  public void windowHidden(EventObject event)
  {
    // default implementation
  }

  @Override
  public void windowMoved(WindowEvent event)
  {
    // default implementation
  }

  @Override
  public void windowResized(WindowEvent event)
  {
    // default implementation
  }

  @Override
  public void windowShown(EventObject event)
  {
    // default implementation
  }

}
