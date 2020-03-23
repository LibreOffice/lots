package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

/**
 * Dispatch for WollMux mailmerge.
 */
public class SeriendruckDispatch extends WollMuxDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = "wollmux:Seriendruck";

  SeriendruckDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    // nothing to do
  }

  @Override
  public boolean status()
  {
    return true;
  }

  @Override
  public boolean isGlobal()
  {
    return true;
  }
}
