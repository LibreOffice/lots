/*
 * Dateiname: EventDispatcherThread.java
 * Projekt  : WollMux
 * Funktion : Wird aufgrund eines Ereignisses vom EventListener gestartet. Ist das 
 *            Ereignis für WollMux-relevant, so wird der entsprechende Code zur
 *            Bearbeitung des Ereignisses gestartet.
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
 */package de.muenchen.allg.itd51.wollmux;

import java.net.MalformedURLException;
import java.net.URL;

import com.sun.star.document.EventObject;

import de.muenchen.allg.afid.UnoService;

public class EventDispatcher
{

  public static void dispatchNotifyEvent(String prefix, EventObject event)
  {
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
                  + ": Ein Event wurde empfangen: "
                  + event.EventName
                  + ", Quelle: "
                  + new UnoService(event.Source).getImplementationName()
                  + "("
                  + title
                  + ")");
    
    // Event OnLoad bearbeiten:
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

  public static void dispatchModifiedEvent(String prefix,
      com.sun.star.lang.EventObject event)
  {
    Logger.debug2(prefix
                  + ": Die folgende Quelle wurde verändert: "
                  + new UnoService(event.Source).getImplementationName()
                  + "("
                  + ")");
  }
}
