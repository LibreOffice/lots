package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveAs;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;

/**
 * Dispatch for saving files.
 */
public class SaveDispatch extends WollMuxNotifyingDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMMAND_SAVE = ".uno:Save";

  /**
   * Command of this dispatch.
   */
  public static final String COMMAND_SAVE_AS = ".uno:SaveAs";

  SaveDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatchWithNotification(URL url, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    this.listener = listener;
    this.props = props;
    emitEvent();
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    this.props = props;
    emitEvent();
  }

  private void emitEvent()
  {
    if (!DocumentManager.getTextDocumentController(frame).getModel().hasURL())
    {
      WollMuxEvent event = new OnSaveAs(DocumentManager.getTextDocumentController(frame), this);
      if (isSynchronMode(props))
      {
        event.process();
      } else
      {
        event.emit();
      }
    } else
    {
      dispatchOriginal();
    }
  }

}
