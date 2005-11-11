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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

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
import de.muenchen.allg.itd51.wollmux.oooui.MenuList;

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
    Logger.debug("Bearbeiten des Events: " + event);
    try
    {
      if (event.getEvent() == Event.ON_LOAD)
      {
        return on_load(event);
      }

      if (event.getEvent() == Event.ON_NEW)
      {
        return on_load(event);
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
    catch (java.lang.Exception e)
    {
      Logger.error(e);
      showInfoModal("WollMux-Fehler:", e.toString());
    }
    return EventProcessor.processTheNextEvent;
  }

  /*****************************************************************************
   * Einzelne Eventhandler
   ****************************************************************************/

  private static boolean on_selection_changed() throws IOException
  {
    // Alle registrierten SenderBoxen updaten:
    Iterator i = WollMux.senderBoxesIterator();
    while (i.hasNext())
    {
      Logger.debug2("Update SenderBox");
      ((XSenderBox) i.next()).updateContent();
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
      MalformedURLException, NodeNotFoundException,
      TextFragmentNotDefinedException, EndlessLoopException,
      com.sun.star.io.IOException, IllegalArgumentException
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
    desktop.xComponentLoader().loadComponentFromURL(urlStr, "_blank", 0, props);
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_load(Event event) throws EndlessLoopException
  {
    UnoService source = new UnoService(event.getSource());
    if (source.xTextDocument() != null)
    {
      // WollMux-Menues aktualisieren
      XFrame frame = source.xModel().getCurrentController().getFrame();
      MenuList.generateMenues(WollMux.getWollmuxConf(), WollMux
          .getXComponentContext(), frame);
      MenuList.generateToolbarEntries(WollMux.getWollmuxConf(), WollMux
          .getXComponentContext(), frame);

      // Interpretation von WM-Kommandos
      new WMCommandInterpreter(source.xTextDocument()).interpret();
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

    // Wollmux-conf-Option CHECK_SENDER_ON_INIT auswerten:
    boolean ask = false;
    ConfigThingy askCT = WollMux.getWollmuxConf().query("CHECK_SENDER_ON_INIT");
    if (askCT.count() > 0)
    {
      String askStr = askCT.getLastChild().toString();
      if (askStr.compareToIgnoreCase("true") == 0) ask = true;
    }

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
    if (r == null || r.size() == 0)
    {
      // wenn kein Eintrag gefunden wurde, wird ein neuer Eintrag erzeugt.
      DJDataset ds = dsj.newDataset();
      ds.set("Vorname", PersoenlicheAbsenderlisteVerwalten.DEFAULT_VORNAME);
      ds.set("Nachname", PersoenlicheAbsenderlisteVerwalten.DEFAULT_NACHNAME);
      ds.set("Rolle", PersoenlicheAbsenderlisteVerwalten.DEFAULT_ROLLE);

      if (ask)
        EventProcessor.create().addEvent(
            new Event(Event.ON_DATENSATZ_BEARBEITEN));
      return EventProcessor.processTheNextEvent;
    }
    else if (r.size() == 1)
    {
      // wenn genau ein Match kam, wird dieser verwendet.
      DJDataset ds = (DJDataset) r.iterator().next();
      // TODO: obiger Cast sollt Dataset sein (oder die Doku ist falsch)
      ds.copy();

      if (ask)
        EventProcessor.create().addEvent(
            new Event(Event.ON_DATENSATZ_BEARBEITEN));
      return EventProcessor.processTheNextEvent;
    }
    else if (r.size() > 1)
    {
      // wenn mehrere Einträge da sind, werden alle in die PAL kopiert und
      // der AbsenderAuswaehlen Dialog gestartet:
      Iterator i = r.iterator();
      while (i.hasNext())
      {
        ((DJDataset) i.next()).copy();
        // TODO: obiger Cast sollt Dataset sein (oder die Doku ist falsch)
      }

      if (ask)
      {
        // Dialogkonfiguration holen:
        ConfigThingy whoAmIconf = WollMux.getWollmuxConf().query(
            "InitialenAbsenderAuswaehlen").getLastChild();
        ConfigThingy PALconf = WollMux.getWollmuxConf().query(
            "PersoenlicheAbsenderliste").getLastChild();
        ConfigThingy ADBconf = WollMux.getWollmuxConf().query(
            "AbsenderdatenBearbeiten").getLastChild();

        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, WollMux
            .getDatasourceJoiner(), EventProcessor.create());

        // danach soll der Datensatz-Bearbeiten Dialog starten.
        EventProcessor.create().addEvent(
            new Event(Event.ON_DATENSATZ_BEARBEITEN));
        return EventProcessor.waitForGUIReturn;
      }
      else
        return EventProcessor.processTheNextEvent;
    }
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
      PropertyValue[] args = new com.sun.star.beans.PropertyValue[] { new PropertyValue() };
      args[0].Name = "nodepath";
      args[0].Value = "/org.openoffice.UserProfile/Data";
      UnoService confProvider = UnoService.createWithContext(
          "com.sun.star.configuration.ConfigurationProvider",
          WollMux.getXComponentContext());
      UnoService confView = confProvider.create(
          "com.sun.star.configuration.ConfigurationAccess",
          args);
      return confView.xNameAccess().getByName(key).toString();
    }
    catch (Exception e)
    {
      return "";
    }
  }
}
