package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dispatch for handling a function dialog.
 */
public class FunctionDialogDispatch extends WollMuxDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMAND = "wollmux:FunctionDialog";

  FunctionDialogDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    WollMuxEventHandler.getInstance().handleFunctionDialog(
        DocumentManager.getTextDocumentController(frame), getMethodArgument());
  }

  @Override
  public boolean status()
  {
    return true;
  }

}
