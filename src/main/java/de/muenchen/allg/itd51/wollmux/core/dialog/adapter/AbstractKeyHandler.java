package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.XKeyHandler;
import com.sun.star.lang.EventObject;

public abstract class AbstractKeyHandler implements XKeyHandler
{

  @Override
  public void disposing(EventObject arg0)
  {
    // default implementation
    
  }

  @Override
  public boolean keyPressed(KeyEvent arg0)
  {
    // default implementation
    return false;
  }

  @Override
  public boolean keyReleased(KeyEvent arg0)
  {
    // default implementation
    return false;
  }

}
