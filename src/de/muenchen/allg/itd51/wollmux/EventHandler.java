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
 * 01.12.2005 | BNK | +on_unload() das die Toolbar neu erzeugt (böser Hack zum 
 *                  | Beheben des Seitenansicht-Toolbar-Verschwindibus-Problems)
 *                  | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu können beim Lesen des Logfiles
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.dialog.DatensatzBearbeiten;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;

/**
 * Der EventHandler stellt die statische Methode processEvent() zur Verfügung,
 * die die Abbarbeitung eines einzelnen Events aus dem EvenProcessor übernehmen
 * soll. Der EventHandler ist der zentrale Einstiegspunkt, für die
 * Implementierung aller WollMux-Funktionen.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
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
    int code = 0;
    try
    {
      code = event.getSource().hashCode();
    }
    catch (java.lang.Exception x)
    {
    }

    Logger.debug("Bearbeiten des Events: " + event + " for #" + code);
    try
    {
      if (event.getEvent() == Event.ON_LOAD)
      {
        return on_load(event);
      }

      if (event.getEvent() == Event.ON_FRAME_CHANGED)
      {
        return on_frame_changed(event);
      }

      if (event.getEvent() == Event.ON_NEW)
      {
        return on_load(event);
      }

      if (event.getEvent() == Event.ON_FOCUS)
      {
        return on_focus(event);
      }

      if (event.getEvent() == Event.ON_OPENTEMPLATE)
      {
        return on_opentemplate(event);
      }

      if (event.getEvent() == Event.ON_ABSENDER_AUSWAEHLEN)
      {
        return on_absender_auswaehlen();
      }

      if (event.getEvent() == Event.ON_DATENSATZ_BEARBEITEN)
      {
        return on_datensatz_bearbeiten();
      }

      if (event.getEvent() == Event.ON_PERSOENLICHE_ABSENDERLISTE)
      {
        return on_persoenliche_absenderliste();
      }

      if (event.getEvent() == Event.ON_INITIALIZE)
      {
        return on_initialize();
      }

      if (event.getEvent() == Event.ON_DIALOG_BACK)
      {
        return on_selection_changed();
      }

      if (event.getEvent() == Event.ON_DIALOG_ABORT)
      {
        return on_selection_changed();
      }

      if (event.getEvent() == Event.ON_SELECTION_CHANGED)
      {
        return on_selection_changed();
      }

    }
    catch (Throwable e)
    {
      Logger.error(e);
      String msg = e.getClass().getName() + ":\n\n";
      if (e.getMessage() != null) msg += e.getMessage();
      showInfoModal("WollMux-Fehler:", msg);
    }
    return EventProcessor.processTheNextEvent;
  }

  /*****************************************************************************
   * Einzelne Eventhandler
   ****************************************************************************/

  private static boolean on_focus(Event event)
  {
    // Alle registrierten SenderBoxen updaten:
    UnoService source = new UnoService(event.getSource());
    if (source.supportsService("com.sun.star.text.TextDocument"))
    {
      Iterator i = WollMux.senderBoxesIterator();
      while (i.hasNext())
      {
        Logger.debug2("Update SenderBox");
        ((XSenderBox) i.next()).updateContentForFrame(source.xModel()
            .getCurrentController().getFrame());
      }
    }

    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_selection_changed() throws IOException
  {
    // Die SenderBox des aktuellen Frame updaten:
    try
    {
      UnoService desktop = UnoService.createWithContext(
          "com.sun.star.frame.Desktop",
          WollMux.getXComponentContext());
      XFrame frame = desktop.xDesktop().getCurrentFrame();
      Iterator i = WollMux.senderBoxesIterator();
      while (i.hasNext())
      {
        Logger.debug2("Update SenderBox");
        ((XSenderBox) i.next()).updateContentForFrame(frame);
      }
    }
    catch (Exception x)
    {
    }

    // Der Cache und der LOS auf Platte speichern.
    WollMux.getDatasourceJoiner().saveCacheAndLOS(WollMux.getLosCacheFile());

    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_persoenliche_absenderliste()
      throws NodeNotFoundException, ConfigurationErrorException
  {
    ConfigThingy PALconf = WollMux.getWollmuxConf().query(
        "PersoenlicheAbsenderliste").getLastChild();
    ConfigThingy ADBconf = WollMux.getWollmuxConf().query(
        "AbsenderdatenBearbeiten").getLastChild();
    new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, WollMux
        .getDatasourceJoiner(), EventProcessor.create());
    return EventProcessor.waitForGUIReturn;
  }

  private static boolean on_datensatz_bearbeiten()
      throws NodeNotFoundException, ConfigurationErrorException,
      DatasetNotFoundException
  {
    ConfigThingy ADBconf = WollMux.getWollmuxConf().query(
        "AbsenderdatenBearbeiten").getLastChild();
    new DatensatzBearbeiten(ADBconf, WollMux.getDatasourceJoiner()
        .getSelectedDataset(), EventProcessor.create());
    return EventProcessor.waitForGUIReturn;
  }

  private static boolean on_absender_auswaehlen() throws NodeNotFoundException,
      ConfigurationErrorException
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

  private static boolean on_opentemplate(Event event) throws Exception,
      NodeNotFoundException, TextFragmentNotDefinedException,
      EndlessLoopException, IOException, MalformedURLException
  {
    UnoService desktop = UnoService.createWithContext(
        "com.sun.star.frame.Desktop",
        WollMux.getXComponentContext());
    String frag_id = event.getArgument();

    // einheitlicher Fehlerzusatz:
    String errorExt = "\n\nDer Fehler trat beim Auflösen des Textfragments mit der ID \""
                      + frag_id
                      + "\" auf.";

    // Fragment-URL holen und aufbereiten:
    String urlStr = WollMux.getTextFragmentList().getURLByID(frag_id);
    URL url;
    try
    {
      url = new URL(WollMux.getDEFAULT_CONTEXT(), urlStr);
    }
    catch (MalformedURLException e)
    {
      throw new MalformedURLException("Die URL \""
                                      + urlStr
                                      + "\" dieser Vorlage ist ungültig: "
                                      + e.getMessage()
                                      + errorExt);
    }
    UnoService trans = UnoService.createWithContext(
        "com.sun.star.util.URLTransformer",
        WollMux.getXComponentContext());
    com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
    unoURL[0].Complete = url.toExternalForm();
    trans.xURLTransformer().parseStrict(unoURL);
    urlStr = unoURL[0].Complete;

    // open document as Template:
    try
    {
      desktop.xComponentLoader().loadComponentFromURL(
          urlStr,
          "_blank",
          0,
          new UnoProps("AsTemplate", Boolean.TRUE).getProps());
    }
    catch (java.lang.Exception x)
    {
      throw new com.sun.star.io.IOException(
          "Die Vorlage mit der URL \""
              + urlStr
              + "\" konnte nicht geöffnet werden.\n\n"
              + "Bitte stellen Sie sicher, dass die Vorlage existiert und unbeschädigt ist."
              + errorExt, x);
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_load(Event event) throws EndlessLoopException,
      WMCommandsFailedException
  {
    UnoService source = new UnoService(event.getSource());
    if (source.supportsService("com.sun.star.text.TextDocument"))
    {
      // auf Events des Frame hören:
      XFrame frame = source.xModel().getCurrentController().getFrame();
      frame.addFrameActionListener(EventProcessor.create());

      // OOOUI (Menues + Toolbars) aktualisieren
      EventProcessor.create().addEvent(
          new Event(Event.ON_FRAME_CHANGED, null, frame));

      // Interpretation von WM-Kommandos
      new WMCommandInterpreter(source.xTextDocument()).interpret();
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_frame_changed(Event event)
      throws EndlessLoopException, WMCommandsFailedException
  {
    UnoService source = new UnoService(event.getSource());
    if (source.xFrame() != null)
    {
      OOoUserInterface.generateToolbarEntries(WollMux.getWollmuxConf(), WollMux
          .getXComponentContext(), source.xFrame());
      OOoUserInterface.generateMenues(WollMux.getWollmuxConf(), WollMux
          .getXComponentContext(), source.xFrame());
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_initialize() throws NodeNotFoundException,
      TimeoutException, ConfigurationErrorException,
      UnsupportedOperationException, java.lang.IllegalArgumentException,
      ColumnNotFoundException
  {
    DatasourceJoiner dsj = WollMux.getDatasourceJoiner();

    // nichts machen, wenn es ist bereits Datensätze im LOS gibt.
    if (dsj.getLOS().size() != 0) return EventProcessor.processTheNextEvent;

    // Die initialen Daten aus den OOo UserProfileData holen:
    String vorname = getUserProfileData("givenname");
    String nachname = getUserProfileData("sn");
    Logger.debug2("Initialize mit Vorname=\""
                  + vorname
                  + "\" und Nachname=\""
                  + nachname
                  + "\"");

    // im DatasourceJoiner nach dem Benutzer suchen:
    QueryResults r = null;
    if (!vorname.equals("") && !nachname.equals(""))
      r = dsj.find("Vorname", vorname, "Nachname", nachname);

    // Auswertung der Suchergebnisse:
    if (r != null)
    {
      // alle matches werden in die PAL kopiert:
      Iterator i = r.iterator();
      while (i.hasNext())
      {
        ((DJDataset) i.next()).copy();
      }
    }

    // Absender Auswählen Dialog starten:
    EventProcessor.create().addEvent(new Event(Event.ON_ABSENDER_AUSWAEHLEN));
    return EventProcessor.processTheNextEvent;
  }

  /**
   * Diese Methode erzeugt einen modalen UNO-Dialog zur Anzeige von
   * Fehlermeldungen bei der Bearbeitung eines Events.
   * 
   * @param sTitle
   * @param sMessage
   */
  private static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage)
  {
    try
    {
      XComponentContext m_xCmpCtx = WollMux.getXComponentContext();

      // hole aktuelles Window:
      UnoService desktop = UnoService.createWithContext(
          "com.sun.star.frame.Desktop",
          m_xCmpCtx);
      XWindow xParent = desktop.xDesktop().getCurrentFrame()
          .getContainerWindow();

      // get access to the office toolkit environment
      com.sun.star.awt.XToolkit xKit = (com.sun.star.awt.XToolkit) UnoRuntime
          .queryInterface(com.sun.star.awt.XToolkit.class, m_xCmpCtx
              .getServiceManager().createInstanceWithContext(
                  "com.sun.star.awt.Toolkit",
                  m_xCmpCtx));

      // describe the info box ini it's parameters
      com.sun.star.awt.WindowDescriptor aDescriptor = new com.sun.star.awt.WindowDescriptor();
      aDescriptor.WindowServiceName = "infobox";
      aDescriptor.Bounds = new com.sun.star.awt.Rectangle(0, 0, 300, 200);
      aDescriptor.WindowAttributes = com.sun.star.awt.WindowAttribute.BORDER
                                     | com.sun.star.awt.WindowAttribute.MOVEABLE
                                     | com.sun.star.awt.WindowAttribute.CLOSEABLE;
      aDescriptor.Type = com.sun.star.awt.WindowClass.MODALTOP;
      aDescriptor.ParentIndex = 1;
      aDescriptor.Parent = (com.sun.star.awt.XWindowPeer) UnoRuntime
          .queryInterface(com.sun.star.awt.XWindowPeer.class, xParent);

      // create the info box window
      com.sun.star.awt.XWindowPeer xPeer = xKit.createWindow(aDescriptor);
      com.sun.star.awt.XMessageBox xInfoBox = (com.sun.star.awt.XMessageBox) UnoRuntime
          .queryInterface(com.sun.star.awt.XMessageBox.class, xPeer);
      if (xInfoBox == null) return;

      // fill it with all given informations and show it
      xInfoBox.setCaptionText("" + sTitle + "");
      xInfoBox.setMessageText("" + sMessage + "");
      xInfoBox.execute();
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  private static String getUserProfileData(String key)
  {
    try
    {
      UnoService confProvider = UnoService.createWithContext(
          "com.sun.star.configuration.ConfigurationProvider",
          WollMux.getXComponentContext());

      UnoService confView = confProvider.create(
          "com.sun.star.configuration.ConfigurationAccess",
          new UnoProps("nodepath", "/org.openoffice.UserProfile/Data")
              .getProps());
      return confView.xNameAccess().getByName(key).toString();
    }
    catch (Exception e)
    {
      return "";
    }
  }
}
