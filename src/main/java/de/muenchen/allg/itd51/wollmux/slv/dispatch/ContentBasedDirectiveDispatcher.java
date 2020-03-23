package de.muenchen.allg.itd51.wollmux.slv.dispatch;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.dispatch.Dispatcher;
import de.muenchen.allg.itd51.wollmux.dispatch.WollMuxDispatch;

/**
 * Dispatcher for content based directive commands.
 */
public class ContentBasedDirectiveDispatcher extends Dispatcher
{
  /**
   * Register dispatches for content based directive commands.
   */
  public ContentBasedDirectiveDispatcher()
  {
    super(DirectiveDispatch.COMMAND, CopyDispatch.COMMAND, RecipientDispatch.COMMAND,
        MarkBlockDispatch.COMMAND);
  }

  @Override
  public WollMuxDispatch create(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    switch (getDispatchMethodName(origUrl))
    {
    case DirectiveDispatch.COMMAND:
      return new DirectiveDispatch(origDisp, origUrl, frame);
    case CopyDispatch.COMMAND:
      return new CopyDispatch(origDisp, origUrl, frame);
    case RecipientDispatch.COMMAND:
      return new RecipientDispatch(origDisp, origUrl, frame);
    case MarkBlockDispatch.COMMAND:
      return new MarkBlockDispatch(origDisp, origUrl, frame);
    default:
      return null;
    }
  }

}
