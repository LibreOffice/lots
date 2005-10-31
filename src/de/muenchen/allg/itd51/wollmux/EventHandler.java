/*
 * Dateiname: EventHandler.java
 * Projekt  : WollMux
 * Funktion : Ist zuständig für die Bearbeitung eines Events.
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

import java.net.URL;

import com.sun.star.beans.PropertyValue;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;

/**
 * TODO: Dokumentieren von EventHandler
 * 
 * @author lut
 * 
 */
public class EventHandler
{
  /**
   * Diese Method ist für die Ausführung eines einzelnen Events zuständig. Nach
   * der Bearbeitung entscheidet der Rückgabewert ob unmittelbar die Bearbeitung
   * des nächsten Events gestartet werden soll oder ob das GUI blockiert werden
   * soll bis das nächste actionPerformed-Event beim EventProcessor eintrifft.
   * 
   * @param event
   *          Das auszuführende Ereignis
   * @return einer der Werte <code>EventProcessor.processNextEvent</code> oder
   *         <code>EventProcessor.waitForGUIReturn</code>.
   */
  public static boolean processEvent(Event event)
  {
    Logger.debug("Bearbeiten des Events: " + event);
    try
    {
      // ON_LOAD:
      if (event.getEvent() == Event.ON_LOAD)
      {
        UnoService source = new UnoService(event.getSource());
        if (source.xTextDocument() != null)
        {
          new WMCommandInterpreter(source.xTextDocument()).interpret();
        }
        return EventProcessor.processTheNextEvent;
      }

      // ON_NEW:
      if (event.getEvent() == Event.ON_NEW)
      {
        UnoService source = new UnoService(event.getSource());
        if (source.xTextDocument() != null)
        {
          new WMCommandInterpreter(source.xTextDocument()).interpret();
        }
        return EventProcessor.processTheNextEvent;
      }

      // ON_OPENFRAG:
      if (event.getEvent() == Event.ON_OPENFRAG)
      {
        UnoService desktop = UnoService.createWithContext(
            "com.sun.star.frame.Desktop",
            WollMux.getXComponentContext());
        String frag_id = event.getArgument();

        // Fragment-URL holen und aufbereiten:
        URL url = new URL(WollMux.getDEFAULT_CONTEXT(), WollMux
            .getTextFragmentList().getURLByID(frag_id));
        UnoService trans = UnoService.createWithContext(
            "com.sun.star.util.URLTransformer",
            WollMux.getXComponentContext());
        com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
        unoURL[0].Complete = url.toExternalForm();
        trans.xURLTransformer().parseStrict(unoURL);
        String urlStr = unoURL[0].Complete;

        // open document as Template:
        PropertyValue[] props = new PropertyValue[] { new PropertyValue() };
        props[0].Name = "AsTemplate";
        props[0].Value = Boolean.TRUE;
        desktop.xComponentLoader().loadComponentFromURL(
            urlStr,
            "_blank",
            0,
            props);
        return EventProcessor.processTheNextEvent;
      }

      // ON_ABSENDERDATEN_BEARBEITEN:
      if (event.getEvent() == Event.ON_ABSENDERDATEN_BEARBEITEN)
      {
        ConfigThingy whoAmIconf = WollMux.getWollmuxConf().query(
            "AbsenderAuswaehlen").getLastChild();
        ConfigThingy PALconf = WollMux.getWollmuxConf().query(
            "PersoenlicheAbsenderliste").getLastChild();
        ConfigThingy ADBconf = WollMux.getWollmuxConf().query(
            "AbsenderdatenBearbeiten").getLastChild();
        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, WollMux
            .getDatasourceJoiner(), EventProcessor.create());
        return EventProcessor.waitForGUIReturn;
      }

      // ON_DIALOG_BACK:
      if (event.getEvent() == Event.ON_DIALOG_BACK)
      {
        // hier kann auf das Back-Event reagiert werden. In Event.getArgument()
        // steht der Name des aufrufenden Dialogs.
        return EventProcessor.processTheNextEvent;
      }

      // ON_DIALOG_ABORT:
      if (event.getEvent() == Event.ON_DIALOG_ABORT)
      {
        // hier kann auf das Abort-Event reagiert werden. In Event.getArgument()
        // steht der Name des aufrufenden Dialogs.
        return EventProcessor.processTheNextEvent;
      }

    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
    return EventProcessor.processTheNextEvent;
  }
}
