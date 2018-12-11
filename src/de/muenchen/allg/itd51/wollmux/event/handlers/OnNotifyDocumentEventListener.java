package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Über dieses Event werden alle registrierten DocumentEventListener (falls
 * listener==null) oder ein bestimmter registrierter DocumentEventListener (falls
 * listener != null) (XEventListener-Objekte) über Statusänderungen der
 * Dokumentbearbeitung informiert
 *
 * @param listener
 *          der zu benachrichtigende XEventListener. Falls null werden alle
 *          registrierten Listener benachrichtig. listener wird auf jeden Fall nur
 *          benachrichtigt, wenn er zur Zeit der Abarbeitung des Events noch
 *          registriert ist.
 * @param eventName
 *          Name des Events
 * @param source
 *          Das von der Statusänderung betroffene Dokument (üblicherweise eine
 *          XComponent)
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class OnNotifyDocumentEventListener extends BasicEvent
{
	private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnNotifyDocumentEventListener.class);
    private String eventName;

    private Object source;

    private XEventListener listener;

    public OnNotifyDocumentEventListener(XEventListener listener, String eventName,
        Object source)
    {
      this.listener = listener;
      this.eventName = eventName;
      this.source = source;
    }

    @Override
    protected void doit()
    {
      final com.sun.star.document.EventObject eventObject =
        new com.sun.star.document.EventObject();
      eventObject.Source = source;
      eventObject.EventName = eventName;

      Iterator<XEventListener> i =
          DocumentManager.getDocumentManager().documentEventListenerIterator();
      while (i.hasNext())
      {
        LOGGER.trace("notifying XEventListener (event '{}')", eventName);
        try
        {
          final XEventListener listener = i.next();
          if (this.listener == null || this.listener == listener) new Thread()
          {
            @Override
            public void run()
            {
              try
              {
                listener.notifyEvent(eventObject);
              }
              catch (java.lang.Exception x)
              {}
            }
          }.start();
        }
        catch (java.lang.Exception e)
        {
          i.remove();
        }
      }

      XComponent compo = UNO.XComponent(source);
      if (compo != null && eventName.equals(WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED))
      {
        DocumentManager.getDocumentManager().setProcessingFinished(
          compo);
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + eventName + "', "
        + ((source != null) ? "#" + source.hashCode() : "null") + ")";
    }
  }