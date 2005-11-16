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
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.oooui.MenuList;

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

  /**
   * Synchroniserter Booleanwert mit dem aktuellen processNextEvent-Status.
   */
  private static boolean processNextEvent[] = new boolean[1];

  public static final boolean processTheNextEvent = true;

  public static final boolean waitForGUIReturn = false;

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
   * Wird vom GlobalEventBroadcaster aufgerufen.
   * 
   * @see com.sun.star.document.XEventListener#notifyEvent(com.sun.star.document.EventObject)
   */
  public void notifyEvent(com.sun.star.document.EventObject docEvent)
  {
    Logger.debug2("Incoming documentEvent: " + docEvent.EventName);
    UnoService source = new UnoService(docEvent.Source);

    // Bekannte Event-Typen rausziehen:
    if (docEvent.EventName.compareToIgnoreCase("OnLoad") == 0)
      addEvent(new Event(Event.ON_LOAD, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnNew") == 0)
      addEvent(new Event(Event.ON_NEW, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnFocus") == 0)
      addEvent(new Event(Event.ON_FOCUS, "", docEvent.Source));

    if (docEvent.EventName.compareToIgnoreCase("OnPrepareUnload") == 0)
    {
      // auf das Event OnPrepareUnload muss synchron reagiert werden:
      if (source.xTextDocument() != null)
      {
        Logger.debug2("Making Toolbar persistent.");
        XFrame frame = source.xModel().getCurrentController().getFrame();
        MenuList.generateToolbarEntries(WollMux.getWollmuxConf(), WollMux
            .getXComponentContext(), frame);
        MenuList.store(WollMux.getXComponentContext(), frame);
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
