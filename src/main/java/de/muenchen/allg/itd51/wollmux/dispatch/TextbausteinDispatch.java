package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextbausteinEinfuegen;

/**
 * Dispatch for inserting boilerplates.
 */
public class TextbausteinDispatch extends WollMuxDispatch
{
  /**
   * Command for inserting a boilerplate.
   */
  public static final String COMMAND = "wollmux:TextbausteinEinfuegen";

  /**
   * Command for inserting a reference to a boilerplate
   */
  public static final String COMMAND_REFERENCE = "wollmux:TextbausteinVerweisEinfuegen";

  /**
   * If true, the boilerplate reference is immediately replaced by the text.
   */
  private boolean reprocess;

  TextbausteinDispatch(XDispatch origDisp, URL origUrl, XFrame frame, boolean reprocess)
  {
    super(origDisp, origUrl, frame);
    this.reprocess = reprocess;
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnTextbausteinEinfuegen(DocumentManager.getTextDocumentController(frame), reprocess).emit();
  }

}
