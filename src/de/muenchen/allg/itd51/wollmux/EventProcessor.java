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

import java.util.LinkedList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.WollMuxEventHandler.WollMuxEvent;

/**
 * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
 * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
 * hineingepackt und von einem einzigen eventProcessingThread sequentiell
 * abgearbeitet.
 * 
 * @author lut
 */
public class EventProcessor
{  
  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu
   * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf false
   * gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public static void setAcceptEvents(boolean accept)
  {
    getInstance()._setAcceptEvents(accept);
  }
  
  /**
   * TODO: comment
   */
  public static void processTheNextEvent()
  {
    getInstance()._processTheNextEvent();
  }

  /**
   * Diese Methode fügt ein Event an die eventQueue an wenn der WollMux
   * erfolgreich initialisiert wurde und damit events akzeptieren darf.
   * Anschliessend weckt sie den EventProcessor-Thread.
   * 
   * @param event
   */
  public static void addEvent(WollMuxEventHandler.WollMuxEvent event) {
    getInstance()._addEvent(event);
  }
  
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

  /**
   * Returnwert eines WollMuxEventHandlers, der aussagt, dass der EventProcessor
   * mit der Ausführung der nächsten Events starten darf.
   */
  public static final boolean processTheNextEvent = true;

  /**
   * Returnwert eines WollMuxEventHandlers, der aussagt, dass der EventProcessor
   * so lange keine weiteren Events ausführt, bis die asynchrone Methode
   * processTheNextEvent() aufgerufen wird.
   */
  public static final boolean wait = false;

  private static EventProcessor getInstance()
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
  private void _setAcceptEvents(boolean accept)
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
              while (processNextEvent[0] == wait)
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
  private void _addEvent(WollMuxEventHandler.WollMuxEvent event)
  {
    if (acceptEvents) synchronized (eventQueue)
    {
      eventQueue.add(event);
      eventQueue.notifyAll();
    }
  }

  /**
   * TODO: ...Veranlasst den EventProcessor Wird beim Beenden von AWT/SWING-GUIs
   * aufgerufen.
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  private void _processTheNextEvent()
  {
    // Dialog bedingte Sperrung der Eventqueue aufheben und Eventbearbeitung
    // normal fortsetzen lassen:
    synchronized (processNextEvent)
    {
      processNextEvent[0] = processTheNextEvent;
      processNextEvent.notifyAll();
    }
  }
}
