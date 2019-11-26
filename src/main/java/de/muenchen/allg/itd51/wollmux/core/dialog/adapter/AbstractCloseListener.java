package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.lang.EventObject;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;

public abstract class AbstractCloseListener implements XCloseListener
{

  @Override
  public void disposing(EventObject arg0)
  {
    // default implementation
  }

  @Override
  public void notifyClosing(EventObject arg0)
  {
    // default implementation
  }

  @Override
  public void queryClosing(EventObject arg0, boolean arg1) throws CloseVetoException
  {
    // default implementation
  }

}
