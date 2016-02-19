/*
 * Dateiname: WollMuxBarEventHandler.java
 * Projekt  : WollMux
 * Funktion : Dient der thread-safen Kommunikation der WollMuxBar mit dem WollMux im OOo.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 18.04.2006 | BNK | Erstellung
 * 21.04.2006 | BNK | Vernünftige Meldung wenn keine Verbindung zum OOo WOllMux hergestellt werden konnte
 * 24.04.2006 | BNK | kleinere Aufräumarbeiten. Code Review.
 * 24.04.2006 | BNK | [R1390]Popup-Fenster, wenn Verbindung zu OOo WollMux nicht hergestellt
 *                  | werden konnte.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.XEnumeration;
import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XPALProvider;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Dient der thread-safen Kommunikation der WollMuxBar mit dem WollMux im OOo.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBarEventHandler
{
  /**
   * Die Event-Queue.
   */
  private List<Event> eventQueue = new LinkedList<Event>();

  /**
   * Die WollMuxBar, für die Events behandelt werden.
   */
  private WollMuxBar wollmuxbar;

  /**
   * Der WollMux-Service, mit dem die WollMuxBar Informationen austauscht. Der
   * WollMux-Service sollte nicht über dieses Feld, sondern ausschließlich über die
   * Methode getRemoteWollMux bezogen werden, da diese mit einem möglichen Schließen
   * von OOo während die WollMuxBar läuft klarkommt.
   * 
   * ACHTUNG! Diese Variable ist absichtlich ein Object, kein XWollMux, um den
   * queryInterface-Aufruf zu erzwingen, damit ein Disposed Zustand erkannt wird.
   */
  private Object remoteWollMux; // soll Object sein, nicht XWollMux!!

  /**
   * Falls nicht null, so ist dies der Desktop auf dem eventuell ein
   * TerminateListener registriert ist.
   */
  private XDesktop desktop;

  /**
   * Falls nicht null, so ist dieser TerminateListener auf {@link #desktop}
   * registriert.
   */
  private XTerminateListener terminateListener;

  /**
   * Dieses Objekt wird beim WollMux als {@link XPALChangeEventListener} registriert.
   */
  private XPALChangeEventListener myPALChangeEventListener;

  /**
   * Der Thread, der die Events abarbeitet.
   */
  private Thread myThread;

  /**
   * Startet die Event-Verarbeitung.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBarEventHandler(WollMuxBar wollmuxbar)
  {
    myPALChangeEventListener = new MyPALChangeEventListener();
    this.wollmuxbar = wollmuxbar;

    myThread = new EventProcessor();
    /*
     * Weil wir den Terminate-Event ausführen müssen, um uns korrekt vom WollMux
     * abzumelden dürfen wir kein Daemon sein.
     */
    myThread.setDaemon(false);
  }

  /**
   * Startet den Event Handler Thread.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void start()
  {
    myThread.start();
  }

  /**
   * Wartet, bis der Event-bearbeitende Thread sich beendet hat. ACHTUNG! Der Thread
   * beendet sich erst, wenn ein handleTerminate() abgesetzt wurde. Wird kein
   * handleTerminate() abgesetzt, dann wartet diese Methode endlos.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void waitForThreadTermination()
  {
    try
    {
      myThread.join();
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Startet OOo falls noetig und stellt Kontakt mit dem WollMux her, um über den
   * aktuellen Senderbox-Inhalt informiert zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectWithWollMux()
  {
    handle(new WollMuxConnectionEvent());
  }

  /**
   * Erzeugt eine wollmuxUrl und übergibt sie dem WollMux zur Bearbeitung.
   * 
   * @param dispatchCmd
   *          das Kommando, das der WollMux ausführen soll. (z.B. "openTemplate")
   * @param arg
   *          ein optionales Argument (z.B. "{fragid}"). Ist das Argument null oder
   *          der Leerstring, so wird es nicht mit übergeben.
   */
  public void handleWollMuxUrl(String dispatchCmd, String arg)
  {
    handle(new WollMuxUrlEvent(dispatchCmd, arg));
  }

  /**
   * Lässt die Senderboxes sich updaten.
   * 
   * @param entries
   *          die Einträge der PAL
   * @param current
   *          der ausgewählte Eintrag
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleSenderboxUpdate(String[] entries, String current)
  {
    handle(new SenderboxUpdateEvent(entries, current));
  }

  /**
   * Teilt dem WollMux mit, dass PAL Eintrag entry gewählt wurde, der der index-te
   * Eintrag der PAL (gezählt ab 0) ist. Es werden sowohl der Eintrag als auch der
   * Index übergeben, damit der WollMux auf Konsistenz prüfen kann. Schließlich ist
   * es möglich, dass in der Zwischenzeit konkurrierende Änderungen der Senderbox
   * stattgefunden haben.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleSelectPALEntry(String entry, int index)
  {
    handle(new SelectPALEntryEvent(entry, index));
  }

  /**
   * Lässt den EventHandler sich ordnungsgemäß deinitialisieren und seine Verbindung
   * zum entfernten WollMux lösen, sowie seinen Bearbeitungsthread beenden. Achtung!
   * Es sollte die Methode waitForThreadTermination() verwendet werden, bevor mit
   * System.exit() die JVM beendet wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleTerminate()
  {
    handle(new TerminateEvent());
  }

  /**
   * Versucht, eine WollMux-Verbindung aufzubauen falls im Moment keine besteht und
   * führt dann run aus. ACHTUNG! run wird auch ausgeführt, wenn das Herstellen der
   * Verbindung fehlschlägt.
   */
  public void handleDoWithConnection(Runnable run)
  {
    handle(new DoWithConnectionEvent(run));
  }

  /**
   * Schiebt Event e in die Queue.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void handle(Event e)
  {
    if (WollMuxFiles.isDebugMode())
      Logger.debug2(L.m("Füge %1 zur Event-Queue hinzu",
        e.getClass().getSimpleName()));
    synchronized (eventQueue)
    {
      eventQueue.add(e);
      eventQueue.notifyAll();
    }
  }

  /**
   * Interface für die Events, die dieser EventHandler abarbeitet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private interface Event
  {
    public void process();
  }

  private class WollMuxUrlEvent implements Event
  {
    private String url;

    public WollMuxUrlEvent(String dispatchCmd, String arg)
    {
      try
      {
        if (arg != null && !arg.equals(""))
          arg = "#" + URLEncoder.encode(arg, ConfigThingy.CHARSET);
        else
          arg = "";
      }
      catch (UnsupportedEncodingException e)
      {
        Logger.error(e);
      }

      url = dispatchCmd + arg;
    }

    @Override
    public void process()
    {
      XDispatchProvider dispProv = null;
      dispProv =
        UnoRuntime.queryInterface(XDispatchProvider.class,
          getRemoteWollMux(true));
      if (dispProv != null)
      {
        com.sun.star.util.URL dispatchUrl = new com.sun.star.util.URL();
        dispatchUrl.Complete = url;
        XDispatch disp =
          dispProv.queryDispatch(dispatchUrl, "_self",
            com.sun.star.frame.FrameSearchFlag.SELF);
        if (disp != null) disp.dispatch(dispatchUrl, new PropertyValue[] {});
      }
    }
  }

  private class SenderboxUpdateEvent implements Event
  {
    private String[] palEntries;

    private String selectedEntry;

    public SenderboxUpdateEvent(String[] entries, String current)
    {
      palEntries = entries;
      selectedEntry = current;
    }

    @Override
    public void process()
    {
      // GUI-Funktionen im Event-Dispatching Thread ausführen wg. Thread-Safety.
      try
      {
        final WollMuxBar wmbar = wollmuxbar;
        final String[] entries = palEntries;
        final String selected = selectedEntry;
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            if (wmbar != null)
            {
              wmbar.updateSenderboxes(entries, selected);
            }
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
  }

  private class SelectPALEntryEvent implements Event
  {
    private String entry;

    private int index;

    public SelectPALEntryEvent(String entry, int index)
    {
      this.entry = entry;
      this.index = index;
    }

    @Override
    public void process()
    {
      XWollMux mux = getRemoteWollMux(true);
      if (mux != null) mux.setCurrentSender(entry, (short) index);
    }
  }

  private class TerminateEvent implements Event
  {
    @Override
    public void process()
    {
      XWollMux mux = getRemoteWollMux(false);
      if (mux != null) mux.removePALChangeEventListener(myPALChangeEventListener);
      if (desktop != null && terminateListener != null)
        desktop.removeTerminateListener(terminateListener);
    }
  }

  private class WollMuxConnectionEvent implements Event
  {
    @Override
    public void process()
    {
      getRemoteWollMux(true);
    }
  }

  private class DoWithConnectionEvent implements Event
  {
    private Runnable run;

    public DoWithConnectionEvent(Runnable run)
    {
      this.run = run;
    }

    @Override
    public void process()
    {
      getRemoteWollMux(true);
      run.run();
    }
  }

  private class MyPALChangeEventListener implements XPALChangeEventListener
  {
    @Override
    public void updateContent(EventObject eventObject)
    {
      XPALProvider palProv =
        UnoRuntime.queryInterface(XPALProvider.class,
        eventObject.Source);
      if (palProv != null)
      {
        try
        {
          String[] entries = palProv.getPALEntries();
          String current = palProv.getCurrentSender();
          String[] entriesCopy = new String[entries.length];
          System.arraycopy(entries, 0, entriesCopy, 0, entries.length);
          handleSenderboxUpdate(entriesCopy, current);
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
    }

    /**
     * Wird zur Zeit (2006-04-19) nicht verwendet.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    @Override
    public void disposing(EventObject arg0)
    {}
  }

  /**
   * Der Thread, der die Events verarbeitet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class EventProcessor extends Thread
  {
    @Override
    public void run()
    {
      while (true)
      {
        try
        {
          Event e;
          synchronized (eventQueue)
          {
            while (eventQueue.isEmpty())
            {
              eventQueue.wait();
            }
            e = eventQueue.remove(0);
          }

          processEvent(e);
          if (e instanceof TerminateEvent)
          {
            Logger.debug(L.m("WollMuxBarEventHandler terminating"));
            return;
          }
        }
        catch (InterruptedException e)
        {
          Logger.error(e);
        }
      }
    }

    private void processEvent(Event e)
    {
      try
      {
        e.process();
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Diese Methode liefert eine Instanz auf den entfernten WollMux zurück, wobei der
   * connect-Parameter steuert, ob falls notwendig eine neue UNO-Verbindung aufgebaut
   * wird.
   * 
   * @param connect
   *          Die Methode versucht immer zuerst, eine bestehende Verbindung mit OOo
   *          zu verwenden, um das WollMux-Objekt zu bekommen. Der Parameter connect
   *          steuert das Verhalten, falls entweder bisher keine Verbindung mit OOo
   *          hergestellt wurde oder die Verbindung abgerissen ist. Falls connect ==
   *          false, so wird in diesen Fällen null zurückgeliefert ohne dass versucht
   *          wird, eine neue Verbindung aufzubauen. Falls connect == true, so wird
   *          versucht, eine neue Verbindung aufzubauen.
   * 
   * @return Instanz eines gültigen WollMux. Konnte oder sollte keine Verbindung
   *         hergestellt werden, so wird null zurückgeliefert. TESTED
   */
  private XWollMux getRemoteWollMux(boolean connect)
  {
    if (remoteWollMux != null)
    {
      try
      {
        XWollMux mux =
          UnoRuntime.queryInterface(XWollMux.class, remoteWollMux);
        if (mux == null) throw new DisposedException();
        return mux;
      }
      catch (DisposedException e)
      {
        remoteWollMux = null;
        desktop = null;
        terminateListener = null;
      }
    }

    if (connect)
    {
      XMultiServiceFactory factory = null;
      XComponentContext ctx = null;
      try
      {
        ctx = Bootstrap.bootstrap();
        factory =
          UnoRuntime.queryInterface(
            XMultiServiceFactory.class, ctx.getServiceManager());
      }
      catch (Exception x)
      {
        // whoops, that failed - can we get an urp connection to a
        // running OOo/LibO at the usual port (8100)?
        try
        {
          ctx = Bootstrap.createInitialComponentContext(null);
          // create a connector, so that it can contact the office
          XUnoUrlResolver urlResolver = UnoUrlResolver.create(ctx);
          Object initialObject = urlResolver.resolve("uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager");
          factory = UnoRuntime.queryInterface(
            XMultiServiceFactory.class, initialObject);
        }
        catch (Exception y)
        {
          Logger.error(L.m("Konnte keine Verbindung zu OpenOffice/LibreOffice herstellen"));
          wollmuxbar.connectionFailedWarning();

          return null;
        }
      }

      /*
       * Die UNO-Funktionalitäten ebenfalls initialisieren.
       */
      try
      {
        UNO.init(ctx.getServiceManager());

        /*
         * Erst bei bestehender Verbindung kann die Versionsnummer von OOo bestimmt
         * werden. Nach diesem Aufruf hier funktionieren die entsprechenden Aufrufe
         * der Workarounds Funktionen.
         */
        Workarounds.getOOoVersion();
      }
      catch (Exception e)
      {
        Logger.error(e);
        // Aber wir machen trotzdem weiter
      }

      try
      {
        remoteWollMux =
          factory.createInstance("de.muenchen.allg.itd51.wollmux.WollMux");
      }
      catch (Exception x)
      {}

      if (remoteWollMux == null)
      {
        Logger.error(L.m("Konnte keine Verbindung zum WollMux-Modul in OpenOffice herstellen"));
        if (WollMuxFiles.externalWollMuxEnabled())
          remoteWollMux = new WollMux(ctx);
        else
        {
          wollmuxbar.connectionFailedWarning();
          return null;
        }
      }

      XWollMux mux =
        UnoRuntime.queryInterface(XWollMux.class, remoteWollMux);
      try
      {
        int wmConfHashCode =
          WollMuxFiles.getWollmuxConf().stringRepresentation().hashCode();
        mux.addPALChangeEventListenerWithConsistencyCheck(myPALChangeEventListener,
          wmConfHashCode);

        installQuickstarter(factory);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      return mux;
    }

    return null;
  }

  private void installQuickstarter(XMultiServiceFactory factory) throws Exception
  {
    desktop = null;
    terminateListener = null;
    if (!wollmuxbar.isQuickstarterEnabled()) return;
    desktop =
      UnoRuntime.queryInterface(XDesktop.class,
        factory.createInstance("com.sun.star.frame.Desktop"));
    terminateListener = new XTerminateListener()
    {
      // Was any of the open documents changed?
      private boolean docChanged = false;
      
      @Override
      public void notifyTermination(EventObject arg0)
      {
        Logger.debug("notifyTermination");
      }

      @Override
      public void queryTermination(EventObject arg0) throws TerminationVetoException
      {
        Logger.debug("queryTermination");
        docChanged = false;
        /*
         * Because of issue
         * 
         * http://www.openoffice.org/issues/show_bug.cgi?id=65942
         * 
         * OOo does not allow the user to close the last window if termination is
         * vetoed. So we close all windows for him.
         */
        try
        {
          XEnumeration xenu = desktop.getComponents().createEnumeration();
          while (xenu.hasMoreElements())
          {
            Object compo = xenu.nextElement();
            XModifiable lMod = UNO.XModifiable(compo);
            if ((lMod != null) && lMod.isModified())
            {
              // Geaendert ==> Nicht schließen
              docChanged = true;
              continue;
            }
            try
            { /*
             * First see if the component itself offers a close function
             */
              UNO.XCloseable(compo).close(false);
            }
            catch (Exception x)
            {
              Logger.debug("Konnte Komponente nicht schließen. Versuche es über den Frame nochmal");
              try
              {
                /*
                 * Okay, so the component can't be closed. Maybe it has a frame that
                 * we can close.
                 */
                UNO.XCloseable(UNO.XController(compo).getFrame()).close(true);
              }
              catch (Exception y)
              {
                Logger.error("Konnte Komponente weder direkt noch über den Frame schließen");
              }
            }
          }
        }
        catch (Throwable x)
        {
          x.printStackTrace();
        }
        if (!docChanged)
        {
          // Wenn eine der Komponenten geändert wurde, dann kein Veto
          // sonst bleibt das Fenster einfach offen, ohne eine Info an den Anwender.
          throw new TerminationVetoException();
        }
      }

      @Override
      public void disposing(EventObject arg0)
      {
        Logger.debug("disposing");
      }
    };

    desktop.addTerminateListener(terminateListener);
  }

}
