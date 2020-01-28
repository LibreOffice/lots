package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

/**
 * Dispatcher for all WollMux dispatches.
 */
public class WollMuxDispatcher extends Dispatcher
{
  /**
   * Register all WollMux dispatches.
   */
  public WollMuxDispatcher()
  {
    super(SaveDispatch.COMMAND_SAVE, SaveDispatch.COMMAND_SAVE_AS,
        UpdateInputFieldsDispatch.COMMAND, PrintDispatch.COMMAND, PrintDispatch.COMMAND_DEFAULT,
        PrintPageDispatch.COMMAND, TextbausteinDispatch.COMMAND,
        TextbausteinDispatch.COMMAND_REFERENCE, PlatzhalterAnspringenDispatch.COMMAND,
        FormularMaxDispatch.COMMAND, SeriendruckDispatch.COMMAND, FunctionDialogDispatch.COMAND,
        DumpInfoDispatch.COMMAND, AboutDispatch.COMMAND, OpenTemplateDispatch.COMMAND_TEMPLATE,
        OpenTemplateDispatch.COMMAND_DOCUMENT, KillDispatch.COMMAND,
        AbsenderAuswaehlenDispatch.COMMAND, PALVerwaltenDispatch.COMMAND);
  }

  @Override
  public XDispatch create(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    switch (getDispatchMethodName(origUrl))
    {
    case SaveDispatch.COMMAND_SAVE:
    case SaveDispatch.COMMAND_SAVE_AS:
      return new SaveDispatch(origDisp, origUrl, frame);
    case UpdateInputFieldsDispatch.COMMAND:
      return new UpdateInputFieldsDispatch(origDisp, origUrl, frame);
    case PrintDispatch.COMMAND:
    case PrintDispatch.COMMAND_DEFAULT:
      return new PrintDispatch(origDisp, origUrl, frame);
    case PrintPageDispatch.COMMAND:
      return new PrintPageDispatch(origDisp, origUrl, frame);
    case TextbausteinDispatch.COMMAND:
      return new TextbausteinDispatch(origDisp, origUrl, frame, true);
    case TextbausteinDispatch.COMMAND_REFERENCE:
      return new TextbausteinDispatch(origDisp, origUrl, frame, false);
    case PlatzhalterAnspringenDispatch.COMMAND:
      return new PlatzhalterAnspringenDispatch(origDisp, origUrl, frame);
    case FormularMaxDispatch.COMMAND:
      return new FormularMaxDispatch(origDisp, origUrl, frame);
    case SeriendruckDispatch.COMMAND:
      return new SeriendruckDispatch(origDisp, origUrl, frame);
    case FunctionDialogDispatch.COMAND:
      return new FunctionDialogDispatch(origDisp, origUrl, frame);
    case DumpInfoDispatch.COMMAND:
      return new DumpInfoDispatch(origDisp, origUrl, frame);
    case AboutDispatch.COMMAND:
      return new AboutDispatch(origDisp, origUrl, frame);
    case OpenTemplateDispatch.COMMAND_TEMPLATE:
      return new OpenTemplateDispatch(origDisp, origUrl, frame, true);
    case OpenTemplateDispatch.COMMAND_DOCUMENT:
      return new OpenTemplateDispatch(origDisp, origUrl, frame, false);
    case KillDispatch.COMMAND:
      return new KillDispatch(origDisp, origUrl, frame);
    case AbsenderAuswaehlenDispatch.COMMAND:
      return new AbsenderAuswaehlenDispatch(origDisp, origUrl, frame);
    case PALVerwaltenDispatch.COMMAND:
      return new PALVerwaltenDispatch(origDisp, origUrl, frame);
    default:
      return null;
    }
  }

}
