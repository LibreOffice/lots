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

import de.muenchen.allg.afid.OOoURL;
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
          URL url = new URL(source.xModel().getURL());
          Logger.debug("Verarbeite Dokument mit der URL "
                       + url.toExternalForm());
          new WMCommandInterpreter(source.xTextDocument(), url).interpret();
        }
      }

      // ON_OPENFRAG:
      if (event.getEvent() == Event.ON_OPENFRAG)
      {
        UnoService desktop = UnoService.createWithContext(
            "com.sun.star.frame.Desktop",
            WollMux.getXComponentContext());
        String frag_id = event.getArgument();
        String urlStr = WollMux.getTextFragmentList().getURLByID(frag_id);

        // open document as Template:
        PropertyValue[] props = new PropertyValue[] { new PropertyValue() };
        props[0].Name = "AsTemplate";
        props[0].Value = Boolean.TRUE;

        UnoService doc = new UnoService(desktop.xComponentLoader()
            .loadComponentFromURL(
                new OOoURL(urlStr, WollMux.getXComponentContext()).toString(),
                "_blank",
                0,
                props));

        new WMCommandInterpreter(doc.xTextDocument(), new URL(urlStr))
            .interpret();
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
        ConfigThingy whoAmIconf = WollMux.getWollmuxConf().get(
            "AbsenderAuswaehlen");
        ConfigThingy PALconf = WollMux.getWollmuxConf().get(
            "PersoenlicheAbsenderliste");
        ConfigThingy ADBconf = WollMux.getWollmuxConf().get(
            "AbsenderdatenBearbeiten");
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
