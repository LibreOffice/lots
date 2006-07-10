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
 * 21.04.2006 | LUT | +acceptEvents-Flag zum deaktivieren der Event-Entgegennahme.                 
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
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
import com.sun.star.lang.EventObject;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.WollMuxEventHandler.WollMuxEvent;

/**
 * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
 * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
 * hineingepackt und von einem einzigen eventProcessingThread sequentiell
 * abgearbeitet.
 * 
 * @author lut
 */
public class EventProcessor implements XEventListener, ActionListener,
    XCloseListener
{
  /**
   * Gibt an, ob der EventProcessor überhaupt events entgegennimmt. Ist
   * acceptEvents=false, werden alle Events ignoriert.
   */
  private boolean acceptEvents = false;

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

  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu
   * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf false
   * gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public void setAcceptEvents(boolean accept)
  {
    acceptEvents = accept;
    if (accept)
      Logger.debug("EventProcessor: akzeptiere neue Events.");
    else
      Logger.debug("EventProcessor: blockiere Entgegennahme von Events!");
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
            WollMuxEvent event;
            synchronized (eventQueue)
            {
              while (eventQueue.isEmpty())
                eventQueue.wait();
              event = (WollMuxEvent) eventQueue.remove(0);
            }
            synchronized (processNextEvent)
            {
              processNextEvent[0] = event.process();
              while (processNextEvent[0] == waitForGUIReturn)
                processNextEvent.wait();
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
   * Diese Methode fügt ein Event an die eventQueue an wenn der WollMux
   * erfolgreich initialisiert wurde und damit events akzeptieren darf.
   * Anschliessend weckt sie den EventProcessor-Thread.
   * 
   * @param event
   */
  public void addEvent(WollMuxEventHandler.WollMuxEvent event)
  {
    if (acceptEvents) synchronized (eventQueue)
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

    // Bekannte Event-Typen rausziehen:
    if (source.xTextDocument() != null)
    {
      if (docEvent.EventName.compareToIgnoreCase("OnLoad") == 0)
      {
        WollMuxEventHandler.handleProcessTextDocument(source.xTextDocument());
      }
      if (docEvent.EventName.compareToIgnoreCase("OnNew") == 0)
      {
        WollMuxEventHandler.handleProcessTextDocument(source.xTextDocument());
      }
    }
  }

  /**
   * Wird beim Beenden AWT/SWING-GUIs aufgerufen.
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent actionEvent)
  {
    // add back- und abort events
    String cmd = actionEvent.getActionCommand();
    if (cmd.equals("back") || cmd.equals("abort"))
    {
      // Alle bisherigen Dialoge nehmen potentiell Änderungen an der
      // Persönlichen Absenderliste vor. Daher wird hier ein PALChangedNotify
      // rausgegeben. TODO: Event OnDialogReturned stattdessen einführen und nur
      // bei PAL-Dialogen den PALChangeNotify durchführen.
      WollMuxEventHandler.handlePALChangedNotify();

      synchronized (processNextEvent)
      {
        processNextEvent[0] = processTheNextEvent;
        processNextEvent.notifyAll();
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
        WollMuxEvent event = (WollMuxEvent) i.next();
        if (event.requires(source.Source))
        {
          Logger.debug2("Removing " + event);
          i.remove();
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see com.sun.star.util.XCloseListener#queryClosing(com.sun.star.lang.EventObject, boolean)
   */
  public void queryClosing(EventObject event, boolean getsOwnership)
      throws CloseVetoException
  {
    if (getsOwnership && UNO.XTextDocument(event.Source) != null)
    {
      // sich selbst deregistrieren:
      if (UNO.XCloseable(event.Source) != null) try
      {
        UNO.XCloseable(event.Source).removeCloseListener(this);
      }
      catch (java.lang.Exception e)
      {
      }

      WollMuxEventHandler.handleRestoreOriginalWindowPosSize(UNO
          .XTextDocument(event.Source));

      // wer eine closeVetoException wirft muss im Falle von getsOwnership==true
      // auch dafür sorgen, dass das Dokument geschlossen wird.
//      WollMuxEventHandler.handleCloseTextDocument(UNO
          //.XTextDocument(event.Source));

      throw new CloseVetoException();
    }
  }

  /* (non-Javadoc)
   * @see com.sun.star.util.XCloseListener#notifyClosing(com.sun.star.lang.EventObject)
   */
  public void notifyClosing(EventObject event)
  {
    // sich selbst deregistrieren:
    if (UNO.XCloseable(event.Source) != null) try
    {
      UNO.XCloseable(event.Source).removeCloseListener(this);
    }
    catch (java.lang.Exception e)
    {
    }
  }
}
