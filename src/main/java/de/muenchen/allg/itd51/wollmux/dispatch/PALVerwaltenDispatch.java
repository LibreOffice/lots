package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dispatch, which shows the sender list.
 */
public class PALVerwaltenDispatch extends WollMuxDispatch
{

  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = "wollmux:PALVerwalten";

  PALVerwaltenDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    WollMuxEventHandler.getInstance().handleShowDialogPersoenlicheAbsenderliste();
  }

  @Override
  public boolean status()
  {
    return true;
  }

}
