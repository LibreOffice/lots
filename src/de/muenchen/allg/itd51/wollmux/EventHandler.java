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
   * Diese Method ist für die Ausführung eines einzelnen (normalen) Events
   * zuständig. Nach der Bearbeitung startet der EventProcessor unmittelbar die
   * Bearbeitung des nächsten Events.
   * 
   * @param event
   */
  public static void processEvent(Event event)
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
      }

      // ON_NEW:
      if (event.getEvent() == Event.ON_NEW)
      {
        UnoService source = new UnoService(event.getSource());
        if (source.xTextDocument() != null)
        {
          new WMCommandInterpreter(source.xTextDocument()).interpret();
        }
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
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * GUIEvents sind grundsätzlich WollMux-modal. D.h. die Event-Verarbeitung
   * durch den EventProcessor-Thread wird nach der Ausführung von
   * processGUIEvent() solange gestoppt, bis das actionPerformed-Event beim
   * EventProcessor eintrifft.
   * 
   * @param event
   */
  public static void processGUIEvent(Event event)
  {
    Logger.debug("Bearbeiten des GUI-Events: " + event);

    try
    {
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
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

  }
}
