package de.muenchen.allg.itd51.wollmux.dispatch;

import java.util.Arrays;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dispatch for opening files as template or document.
 */
public class OpenTemplateDispatch extends WollMuxDispatch
{
  /**
   * Command for templates.
   */
  public static final String COMMAND_TEMPLATE = "wollmux:OpenTemplate";

  /**
   * Command for documents.
   */
  public static final String COMMAND_DOCUMENT = "wollmux:OpenDocument";

  /**
   * If true, file is opened as template and not processed by WollMux, otherwise WollMux processes
   * the file.
   */
  private boolean asTemplate;

  OpenTemplateDispatch(XDispatch origDisp, URL origUrl, XFrame frame, boolean asTemplate)
  {
    super(origDisp, origUrl, frame);
    this.asTemplate = asTemplate;
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    String[] fragIds = getMethodArgument().split("&");
    WollMuxEventHandler.getInstance().handleOpenDocument(Arrays.asList(fragIds), asTemplate);
  }

  @Override
  public boolean status()
  {
    return true;
  }

}
