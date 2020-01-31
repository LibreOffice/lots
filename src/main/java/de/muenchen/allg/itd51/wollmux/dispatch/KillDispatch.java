package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;

/**
 * Dispatch for killing WollMux and LibreOffice.
 */
public class KillDispatch extends WollMuxDispatch
{

  public static final String COMMAND = "wollmux:Kill";

  KillDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnKill().emit();
  }

  @Override
  public boolean status()
  {
    return true;
  }

}
