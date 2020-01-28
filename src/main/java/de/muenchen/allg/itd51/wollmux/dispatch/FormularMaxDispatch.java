package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;


/**
 * Dispatch, which shows the GUI for creating or modifying a form.
 */
public class FormularMaxDispatch extends WollMuxDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = "wollmux:FormularMax4000";

  FormularMaxDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    WollMuxEventHandler.getInstance().handleFormularMax4000Show(DocumentManager.getTextDocumentController(frame));
  }

}
