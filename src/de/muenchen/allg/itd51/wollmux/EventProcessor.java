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

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;

/**
 * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
 * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
 * hineingepackt und von einem einzigen eventProcessingThread sequentiell
 * abgearbeitet.
 * 
 * @author lut
 */
public class EventProcessor implements XEventListener, XModifyListener,
    ActionListener
{
  private List eventQueue = new LinkedList();

  private static EventProcessor singletonInstance;

  private static Thread eventProcessorThread;

  private static boolean processGUIEvent[] = new boolean[1];

  public static EventProcessor create()
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
              if (event.isGUIEvent())
              {
                // GUI-Event verarbeiten und anschliessend warten, bis
                // actionPerformed() aufgerufen wird.
                synchronized (processGUIEvent)
                {
                  processGUIEvent[0] = false;
                  EventHandler.processGUIEvent(event);
                  while (processGUIEvent[0] == false)
                    processGUIEvent.wait();
                }
              }
              else
              {
                // Event normal ausführen.
                EventHandler.processEvent(event);
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
   * Wird vom GlobalEventBroadcaster aufgerufen.
   * 
   * @see com.sun.star.document.XEventListener#notifyEvent(com.sun.star.document.EventObject)
   */
  public void notifyEvent(com.sun.star.document.EventObject docEvent)
  {
    Logger.debug2("Incoming: " + new Event(docEvent));
    addEvent(new Event(docEvent));
  }

  /**
   * Wird von einzelnen Uno-Komponenten bei Änderungen aufgerufen.
   * 
   * @see com.sun.star.util.XModifyListener#modified(com.sun.star.lang.EventObject)
   */
  public void modified(com.sun.star.lang.EventObject modifyEvent)
  {
    Logger.debug2("Incoming: " + new Event(modifyEvent));
    addEvent(new Event(modifyEvent));
  }

  /**
   * Wird beim Beenden AWT/SWING-GUIs aufgerufen.
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent actionEvent)
  {
    //TODO: actionEvent.getActionCommand(); "back" auswerten
    // (String)actionEvent.getSource() ist der Name des Dialogs
    {
      synchronized (processGUIEvent)
      {
        processGUIEvent[0] = true;
        processGUIEvent.notifyAll();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
   */
  public void disposing(com.sun.star.lang.EventObject source)
  {
    // eventQueue durchscannen und Events mit nicht mehr erfüllten
    // Sourcen-Referenzen löschen.
    synchronized (eventQueue)
    {
      Iterator i = eventQueue.iterator();
      while (i.hasNext())
      {
        Event event = (Event) i.next();
        if (UnoRuntime.areSame(source.Source, event.getSource())) i.remove();
      }
    }
    // EventListener deregistrieren.
    XComponent xCompo = UNO.XComponent(source.Source);
    if (xCompo != null) xCompo.removeEventListener(this);
    // TODO: Wann hören wir auf?
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
}
