package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.FocusEvent;
import com.sun.star.awt.XFocusListener;
import com.sun.star.lang.EventObject;

public abstract class AbstractFocusListener implements XFocusListener
{

  @Override
  public void disposing(EventObject event)
  {
    // default implementation
  }

  @Override
  public void focusGained(FocusEvent event)
  {
    // default implementation
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    // default implementation
  }

}
