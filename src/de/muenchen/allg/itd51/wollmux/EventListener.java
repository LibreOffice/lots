/*
 * Dateiname: EventDispatcher.java
 * Projekt  : WollMux
 * Funktion : Zentraler EventListener: Nimmt Events von OOo entgegen und sorgt für
 *            eine synchronisierte Verarbeitung durch den EventDispatcher.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 20.10.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 */

package de.muenchen.allg.itd51.wollmux;

import java.util.Stack;

import com.sun.star.document.EventObject;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.Exception;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Die Klasse EventListener stellt den zentralen EventListener für
 * OOo-Ereignisse dar. Der EventListener nimmt Ereignisse entgegen und sorgt für
 * eine synchronisierte Verarbeitung durch den EventDispatcherThread.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class EventListener implements XEventListener, XModifyListener
{
  private Stack eventStack;

  private String prefix;

  private boolean doExit;

  /**
   * Diese statische Methode registriert eine Instanz des EventListeners im
   * GlobalEventBroadcaster.
   * 
   * @throws Exception
   */
  public static void registerGlobalEventListener() throws Exception
  {
    UnoService eventBroadcaster = UnoService.createWithContext(
        "com.sun.star.frame.GlobalEventBroadcaster",
        WollMux.getXComponentContext());
    eventBroadcaster.xEventBroadcaster().addEventListener(
        new EventListener("global", true));
  }

  /**
   * Der Konstruktor erzeugt einen neuen EventListener mit einem Prefix (z.B.
   * "global"), der Angibt, in welchem Gültigkeitsbereich der EventListener
   * läuft.
   * 
   * @param prefix
   *          Der prefix gibt an, in welchem Gültigkeitsbereich sich der
   *          EventListener befindet.
   * @param doExit
   *          Ist der Wert doExit auf true gesetzt, dann sorgt der Aufruf der
   *          disposing-Methode durch den EventBroadcaster für eine Beendigung
   *          der aktuellen JVM. Diese Einstellung macht nur im globalen
   *          Gültigkeitsbereich Sinn.
   */
  public EventListener(String prefix, boolean doExit)
  {
    this.eventStack = new Stack();
    this.prefix = prefix;
    this.doExit = doExit;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.document.XEventListener#notifyEvent(com.sun.star.document.EventObject)
   */
  public void notifyEvent(EventObject event)
  {
    Thread t = new DispatcherThread(prefix, this);
    addToStack(event);
    t.start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.util.XModifyListener#modified(com.sun.star.lang.EventObject)
   */
  public void modified(com.sun.star.lang.EventObject event)
  {
    System.out.println(prefix
                       + ": Die folgende Quelle wurde verändert: "
                       + new UnoService(event.Source).getImplementationName()
                       + "("
                       + ")");
  }

  private synchronized void addToStack(EventObject event)
  {
    eventStack.add(event);
  }

  private synchronized Object getFromStack()
  {
    return (EventObject) eventStack.pop();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XEventListener#disposing(com.sun.star.lang.EventObject)
   */
  public void disposing(com.sun.star.lang.EventObject source)
  {
    XComponent xCompo = UNO.XComponent(source.Source);
    if (xCompo != null) xCompo.removeEventListener(this);
    if (doExit) System.exit(0);
  }

  /**
   * Der DispatcherThread wird vom notifyEvent gestartet. Für jedes Event wird
   * ein Thread gestartet, in dem die eigentliche Auswertung des Ereignisses
   * durch den EventDispatcher vorgenommen wird.
   */
  private class DispatcherThread extends Thread
  {
    private EventListener listener;

    private String prefix;

    public DispatcherThread(String prefix, EventListener listener)
    {
      this.listener = listener;
      this.prefix = prefix;
    }

    public void run()
    {
      Object o = listener.getFromStack();
      if (o instanceof EventObject)
        EventDispatcher.dispatchNotifyEvent(prefix, (EventObject) o);
      else if (o instanceof com.sun.star.lang.EventObject)
        EventDispatcher.dispatchModifiedEvent(
            prefix,
            (com.sun.star.lang.EventObject) o);
    }
  }

}
