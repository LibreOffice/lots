/*
 * Dateiname: EventProcessor.java
 * Projekt  : WollMux
 * Funktion : Zentraler EventListener, der die synchronisierte Verarbeitung steuert.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.10.2005 | LUT | Erstellung
 * 01.12.2005 | BNK | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu können beim Lesen des Logfiles
 *                  | +ON_UNLOAD
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.star.document.EventObject;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.FrameAction;
import com.sun.star.frame.FrameActionEvent;
import com.sun.star.frame.XFrameActionListener;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UnoService;

/**
 * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
 * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
 * hineingepackt und von einem einzigen eventProcessingThread sequentiell
 * abgearbeitet.
 * 
 * @author lut
 */
public class EventProcessor implements XEventListener, XModifyListener,
    XFrameActionListener, ActionListener
{
  private List eventQueue = new LinkedList();

  private static EventProcessor singletonInstance;

  private static Thread eventProcessorThread;

  /**
   * Synchroniserter Booleanwert mit dem aktuellen processNextEvent-Status.
   */
  private static boolean processNextEvent[] = new boolean[1];

  public static final boolean processTheNextEvent = true;

  public static final boolean waitForGUIReturn = false;

  public static EventProcessor getInstance()
  {
    if (singletonInstance == null) singletonInstance = new EventProcessor();
    return singletonInstance;
  }

  private EventProcessor()
  {
    // starte den eventProcessorThread
    eventProcessorThread = new Thread(new Runnable()
    {
      public void run()
      {
        Logger.debug("Starte EventProcessor-Thread");
        try
        {
          while (true)
          {
            Event event;
            synchronized (eventQueue)
            {
              while (eventQueue.isEmpty())
                eventQueue.wait();
              event = (Event) eventQueue.remove(0);
            }
            if (event.getEvent() != Event.UNKNOWN)
            {
              synchronized (processNextEvent)
              {
                processNextEvent[0] = EventHandler.processEvent(event);
                while (processNextEvent[0] == waitForGUIReturn)
                  processNextEvent.wait();
              }
            }
          }
        }
        catch (InterruptedException e)
        {
          Logger.error("EventProcessor-Thread wurde unterbrochen:");
          Logger.error(e);
        }
        Logger.debug("Beende EventProcessor-Thread");
      }
    });
    eventProcessorThread.start();
  }

  /**
   * Diese Methode fügt ein Event an die eventQueue an und weckt den
   * EventProcessor-Thread.
   * 
   * @param event
   */
  public void addEvent(Event event)
  {
    synchronized (eventQueue)
    {
      eventQueue.add(event);
      eventQueue.notifyAll();
    }
  }

  /**
   * Wird vom GlobalEventBroadcaster aufgerufen.
   * 
   * @see com.sun.star.document.XEventListener#notifyEvent(com.sun.star.document.EventObject)
   */
  public void notifyEvent(com.sun.star.document.EventObject docEvent)
  {
    int code = 0;
    try
    {
      code = docEvent.Source.hashCode();
    }
    catch (Exception x)
    {
    }
    Logger.debug2("Incoming documentEvent for #"
                  + code
                  + ": "
                  + docEvent.EventName);
    UnoService source = new UnoService(docEvent.Source);

    // Im Falle von OnLoad oder OnNew den EventProcessor als Eventhandler
    // des neuen Dokuments registrieren und den Document-Status auf alive
    // setzen:
    if (docEvent.EventName.compareToIgnoreCase("OnLoad") == 0
        || docEvent.EventName.compareToIgnoreCase("OnNew") == 0)
      if (source.supportsService("com.sun.star.text.TextDocument"))
      {
        source.xComponent().addEventListener(this);
      }

    // Bekannte Event-Typen rausziehen:
    if (docEvent.EventName.compareToIgnoreCase("OnLoad") == 0)
      addEvent(new Event(Event.ON_LOAD, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnNew") == 0)
      addEvent(new Event(Event.ON_NEW, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnFocus") == 0)
      addEvent(new Event(Event.ON_FOCUS, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnCloseApp") == 0) {
      // muss synchron behandelt werden:
      // PALChange-Listener informieren:
      Iterator i = WollMuxSingleton.getInstance().palChangeListenerIterator();
      while(i.hasNext()) {
        ((XPALChangeEventListener) i.next()).disposing(new EventObject());
      }
    }
}

  /**
   * Wird von einzelnen Uno-Komponenten bei Änderungen aufgerufen.
   * 
   * @see com.sun.star.util.XModifyListener#modified(com.sun.star.lang.EventObject)
   */
  public void modified(com.sun.star.lang.EventObject modifyEvent)
  {
    addEvent(new Event(Event.ON_MODIFIED, "", modifyEvent.Source));
  }

  /**
   * Wird beim Beenden AWT/SWING-GUIs aufgerufen.
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent actionEvent)
  {
    // add back- und abort events
    if (actionEvent.getActionCommand().equals("back"))
    {
      addEvent(new Event(Event.ON_DIALOG_BACK, (String) actionEvent.getSource()));
    }
    if (actionEvent.getActionCommand().equals("abort"))
    {
      addEvent(new Event(Event.ON_DIALOG_ABORT, (String) actionEvent
          .getSource()));
    }
    synchronized (processNextEvent)
    {
      processNextEvent[0] = processTheNextEvent;
      processNextEvent.notifyAll();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
   */
  public void disposing(com.sun.star.lang.EventObject source)
  {
    Logger.debug2("disposing(): Removing events for #"
                  + source.hashCode()
                  + " from queue");

    // eventQueue durchscannen und Events mit nicht mehr erfüllten
    // Sourcen-Referenzen löschen.
    synchronized (eventQueue)
    {
      Iterator i = eventQueue.iterator();
      while (i.hasNext())
      {
        Event event = (Event) i.next();
        if (UnoRuntime.areSame(source.Source, event.getSource()))
        {
          Logger.debug2("Removing " + event);
          i.remove();
        }
      }
    }
  }

  public void frameAction(FrameActionEvent event)
  {
    FrameAction action = event.Action;
    String actionStr = "unknown";
    if (action == FrameAction.COMPONENT_ATTACHED)
    {
      actionStr = "COMPONENT_ATTACHED";
    }
    if (action == FrameAction.COMPONENT_DETACHING)
    {
      actionStr = "COMPONENT_DETACHING";
    }
    if (action == FrameAction.COMPONENT_REATTACHED)
    {
      actionStr = "COMPONENT_REATTACHED";
    }
    if (action == FrameAction.FRAME_ACTIVATED)
    {
      actionStr = "FRAME_ACTIVATED";
    }
    if (action == FrameAction.FRAME_DEACTIVATING)
    {
      actionStr = "FRAME_DEACTIVATING";
    }
    if (action == FrameAction.CONTEXT_CHANGED)
    {
      actionStr = "CONTEXT_CHANGED";
    }
    if (action == FrameAction.FRAME_UI_ACTIVATED)
    {
      actionStr = "FRAME_UI_ACTIVATED";
    }
    if (action == FrameAction.FRAME_UI_DEACTIVATING)
    {
      actionStr = "FRAME_UI_DEACTIVATING";
    }
    Logger.debug2("Incoming FrameActionEvent: " + actionStr);

    // Bekannte Event-Typen rausziehen:
    if (action == FrameAction.COMPONENT_REATTACHED)
      addEvent(new Event(Event.ON_FRAME_CHANGED, "", event.Source));
  }

}
