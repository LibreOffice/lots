package de.muenchen.allg.itd51.wollmux.slv.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.dispatch.WollMuxDispatch;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;
import de.muenchen.allg.itd51.wollmux.slv.events.OnMarkBlock;

/**
 * Dispatch for marking a block as print block.
 */
public class MarkBlockDispatch extends WollMuxDispatch
{
  /**
   * The command of this dispatch.
   */
  public static final String COMMAND = "wollmux:MarkBlock";

  MarkBlockDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL arg0, PropertyValue[] arg1)
  {
    WollMuxEvent event = new OnMarkBlock(DocumentManager.getTextDocumentController(frame),
        getMethodArgument());
    event.emit();
  }

}
