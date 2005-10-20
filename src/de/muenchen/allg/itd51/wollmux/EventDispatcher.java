/*
 * Dateiname: EventDispatcher.java
 * Projekt  : WollMux
 * Funktion : Zentraler EventDispatcher: Nimmt Events von OOo entgegen und startet die
 *            Verarbeitung der für den WollMux-relevanten Ereignisse.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import com.sun.star.document.EventObject;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.Exception;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

public class EventDispatcher
{

  public static void registerGlobalEventListener() throws Exception
  {
    UnoService eventBroadcaster = UnoService.createWithContext(
        "com.sun.star.frame.GlobalEventBroadcaster",
        WollMux.getXComponentContext());
    eventBroadcaster.xEventBroadcaster().addEventListener(
        new ReportingEventListener("global", true));
  }

  private static class ReportingEventListener implements XEventListener,
      XModifyListener
  {
    private Stack eventStack;

    private String prefix;

    private boolean doExit;

    public ReportingEventListener(String prefix, boolean doExit)
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
      Thread t = new EventExecutor(prefix, this);
      addToStack(event);
      t.start();
    }

    private synchronized void addToStack(EventObject event)
    {
      eventStack.add(event);
    }

    public synchronized EventObject getFromStack()
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

  }

  private static class EventExecutor extends Thread
  {
    private ReportingEventListener listener;

    private String prefix;

    EventExecutor(String prefix, ReportingEventListener listener)
    {
      this.listener = listener;
      this.prefix = prefix;
    }

    public void run()
    {
      EventObject event = listener.getFromStack();

      UnoService source = new UnoService(event.Source);
      UnoService controller = new UnoService(source.xModel()
          .getCurrentController());

      String title = "";
      try
      {
        title = (String) controller.getPropertyValue("Title").getObject();
      }
      catch (java.lang.Exception x)
      {
      }
      Logger.debug2(prefix
                    + ": Juchu! Ein Event wurde empfangen: "
                    + event.EventName
                    + ", Quelle: "
                    + new UnoService(event.Source).getImplementationName()
                    + "("
                    + title
                    + ")");
      if (event.EventName.equals("OnLoad"))
      {
        if (source.xTextDocument() != null)
        {
          try
          {
            URL url = new URL(source.xModel().getURL());
            Logger.debug("Verarbeite Dokument mit der URL "
                         + url.toExternalForm());
            new WMCommandInterpreter(source.xTextDocument(), url).interpret();
          }
          catch (MalformedURLException e)
          {
            Logger.error(e);
          }
        }
      }
    }

  }
}
