package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.dispatch.DispatchHelper;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class OnUpdateInputFields extends BasicEvent
{
  XTextDocument doc;
  DispatchHelper helper;

  public OnUpdateInputFields(XTextDocument doc, DispatchHelper helper)
  {
    this.doc = doc;
    this.helper = helper;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    WollMuxEventHandler.getInstance().handleAddDocumentEventListener(new XEventListener()
    {
      @Override
      public void disposing(EventObject arg0)
      {
        // nothing to do
      }
    
      @Override
      public void notifyEvent(com.sun.star.document.EventObject event)
      {
        if (UnoRuntime.areSame(doc, event.Source)
            && WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED.equals(event.EventName))
        {
          helper.dispatchFinished(true);
          WollMuxEventHandler.getInstance().handleRemoveDocumentEventListener(this);
        }
      }
    });
  }
}
