package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;

/**
 * Dispatch, for printing.
 */
public class PrintDispatch extends WollMuxDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = ".uno:Print";

  /**
   * Command of this dispatch.
   */
  public static final String COMMAND_DEFAULT = ".uno:PrintDefault";

  /**
   * Command for modifying printer setup.
   */
  public static final String COMMAND_PRINTER_SETUP = ".uno:PrinterSetup";

  PrintDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnPrint(DocumentManager.getTextDocumentController(frame), origDisp, origUrl, props).emit();
  }

}
