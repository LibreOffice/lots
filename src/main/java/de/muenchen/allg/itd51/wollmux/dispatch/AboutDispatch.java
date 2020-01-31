package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;

/**
 * Dispatch, which shows the about dialog.
 */
public class AboutDispatch extends WollMuxDispatch
{

  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = "wollmux:About";

  AboutDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnAbout().emit();
  }

  @Override
  public boolean status()
  {
    return true;
  }

}
