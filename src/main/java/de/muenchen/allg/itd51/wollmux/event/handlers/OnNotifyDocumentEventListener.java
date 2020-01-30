package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XDeck;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.sidebar.SidebarHelper;

/**
 * Event for notifying listeners registered on an object.
 */
public class OnNotifyDocumentEventListener extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnNotifyDocumentEventListener.class);
  private String eventName;

  private Object source;

  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          If listener == null all registered listeners are notified, otherwise only the provided
   *          listener is notified.
   * @param eventName
   *          Name of the notification event.
   * @param source
   *          The source of the notification event.
   */
  public OnNotifyDocumentEventListener(XEventListener listener,
      String eventName,
      Object source)
  {
    this.listener = listener;
    this.eventName = eventName;
    this.source = source;
  }

  @Override
  protected void doit()
  {
    final com.sun.star.document.EventObject eventObject = new com.sun.star.document.EventObject();
    eventObject.Source = source;
    eventObject.EventName = eventName;

    for (XEventListener docListener : DocumentManager.getDocumentManager()
        .getDocumentEventListener())
    {
      if (this.listener == null || this.listener == docListener)
      {
        LOGGER.trace("notifying XEventListener (event '{}')", eventName);
        new Thread(() -> docListener.notifyEvent(eventObject)).start();
      }
    }

    XComponent compo = UNO.XComponent(source);
    if (compo != null && eventName.equals(WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED))
    {
      DocumentManager.getDocumentManager().setProcessingFinished(compo);
      XTextDocument xTextDoc = UNO.XTextDocument(source);
      if (xTextDoc != null)
      {
        TextDocumentController documentController = DocumentManager
            .getTextDocumentController(xTextDoc);

        if (documentController.getModel().isFormDocument())
        {
          XDeck formGuiDeck = SidebarHelper.getDeckByName(SidebarHelper.WM_FORM_GUI,
              UNO.getCurrentTextDocument().getCurrentController());

          if (formGuiDeck != null)
          {
            formGuiDeck.activate(true);
          }
        }
      }
    }
  }
}