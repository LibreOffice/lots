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
 * 27.03.2005 | LUT | neues Kommando openDocument                 
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.dialog.DatensatzBearbeiten;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;

/**
 * Der EventHandler stellt die statische Methode processEvent() zur Verfügung,
 * die die Abbarbeitung aller Events aus dem EvenProcessor übernimmt. Der
 * EventHandler ist der zentrale Einstiegspunkt, für die Implementierung aller
 * WollMux-Funktionen.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class EventHandler
{
  /**
   * Dieses Feld stellt ein Zwischenspeicher für Fragment-Urls dar, der
   * Dokument-Instanzen auf Fragment-URL-Listen mapped. Es wird dazu benutzt, im
   * Fall eines openTemplate-Befehls die urls der übergebenen frag_id-Liste
   * temporär zu speichern. Das Event on_new/on_load holt sich die temporär
   * gespeicherten Argumente aus der hashMap und übergibt sie dem
   * WMCommandInterpreter.
   */
  private static HashMap docFragUrlsBuffer = new HashMap();

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
        return on_opendocument(event, true);
      }

      if (event.getEvent() == Event.ON_OPENDOCUMENT)
      {
        return on_opendocument(event, false);
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

      if (event.getEvent() == Event.ON_SET_SENDER)
      {
        return on_set_sender(event);
      }

      if (event.getEvent() == Event.ON_SELECTION_CHANGED)
      {
        return on_selection_changed();
      }

      if (event.getEvent() == Event.ON_ADD_PAL_CHANGE_EVENT_LISTENER)
      {
        return on_add_pal_change_event_listener(event);
      }

      if (event.getEvent() == Event.ON_REMOVE_PAL_CHANGE_EVENT_LISTENER)
      {
        return on_remove_pal_change_event_listener(event);
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
    // Hier wurden früher als Workaround alle registrierten SenderBoxen
    // geupdated. Das ist jetzt nicht mehr notwendig.
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_selection_changed()
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    // registrierte PALChangeListener updaten
    Iterator i = mux.palChangeListenerIterator();
    while (i.hasNext())
    {
      Logger.debug2("on_selection_changed: Update SenderBox");
      EventObject eventObject = new EventObject();
      eventObject.Source = WollMuxSingleton.getInstance();
      try
      {
        ((XPALChangeEventListener) i.next()).updateContent(eventObject);
      }
      catch (java.lang.Exception x)
      {
        i.remove();
      }
    }

    // Cache und LOS auf Platte speichern.
    try
    {
      mux.getDatasourceJoiner().saveCacheAndLOS(WollMuxFiles.getLosCacheFile());
    }
    catch (IOException e)
    {
      Logger.error(e);
    }

    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_persoenliche_absenderliste()
      throws NodeNotFoundException, ConfigurationErrorException
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    ConfigThingy PALconf = mux.getWollmuxConf().query(
        "PersoenlicheAbsenderliste").getLastChild();
    ConfigThingy ADBconf = mux.getWollmuxConf()
        .query("AbsenderdatenBearbeiten").getLastChild();
    new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, mux
        .getDatasourceJoiner(), mux.getEventProcessor());
    return EventProcessor.waitForGUIReturn;
  }

  private static boolean on_datensatz_bearbeiten()
      throws NodeNotFoundException, ConfigurationErrorException,
      DatasetNotFoundException
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    ConfigThingy ADBconf = mux.getWollmuxConf()
        .query("AbsenderdatenBearbeiten").getLastChild();
    new DatensatzBearbeiten(ADBconf, mux.getDatasourceJoiner()
        .getSelectedDataset(), mux.getEventProcessor());
    return EventProcessor.waitForGUIReturn;
  }

  private static boolean on_absender_auswaehlen() throws NodeNotFoundException,
      ConfigurationErrorException
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    ConfigThingy whoAmIconf = mux.getWollmuxConf().query("AbsenderAuswaehlen")
        .getLastChild();
    ConfigThingy PALconf = mux.getWollmuxConf().query(
        "PersoenlicheAbsenderliste").getLastChild();
    ConfigThingy ADBconf = mux.getWollmuxConf()
        .query("AbsenderdatenBearbeiten").getLastChild();
    new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, mux
        .getDatasourceJoiner(), mux.getEventProcessor());
    return EventProcessor.waitForGUIReturn;
  }

  private static boolean on_opendocument(Event event, boolean asTemplate)
      throws Exception, NodeNotFoundException, TextFragmentNotDefinedException,
      EndlessLoopException, IOException, MalformedURLException
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    UnoService desktop = UnoService.createWithContext(
        "com.sun.star.frame.Desktop",
        mux.getXComponentContext());
    Vector args = event.getArgs();

    // das erste Argument ist das unmittelbar zu landende Textfragment und
    // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
    // werden nach argsUrlStr aufgelöst.
    String loadUrlStr = "";
    String[] argsUrlStr = new String[args.size() - 1];
    String errorExt = "";

    Iterator i = args.iterator();
    int count = 0;
    while (i.hasNext())
    {
      String frag_id = (String) i.next();

      // einheitlicher Fehlerzusatz:
      errorExt = "\n\nDer Fehler trat beim Auflösen des Textfragments mit der ID \""
                 + frag_id
                 + "\" auf.";

      // Fragment-URL holen und aufbereiten:
      String urlStr = mux.getTextFragmentList().getURLByID(frag_id);
      URL url;
      try
      {
        url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
      }
      catch (MalformedURLException e)
      {
        throw new MalformedURLException("Die URL \""
                                        + urlStr
                                        + "\" ist ungültig: "
                                        + e.getMessage()
                                        + errorExt);
      }

      // URL durch den URL-Transformer jagen
      UnoService trans = UnoService.createWithContext(
          "com.sun.star.util.URLTransformer",
          mux.getXComponentContext());
      com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
      unoURL[0].Complete = url.toExternalForm();
      trans.xURLTransformer().parseStrict(unoURL);
      urlStr = unoURL[0].Complete;

      // Workaround für Fehler in insertDocumentFromURL: Prüfen ob URL aufgelöst
      // werden kann, da sonst der insertDocumentFromURL einfriert.
      try
      {
        url = new URL(urlStr);
      }
      catch (MalformedURLException e)
      {
        // darf nicht auftreten, da url bereits oben geprüft wurde...
        Logger.error(e);
      }
      if (url.openConnection().getContentLength() <= 0)
      {
        throw new IOException(
            "Die URL \""
                + url.toExternalForm()
                + "\" kann nicht aufgelöst werden!\n\n"
                + "Bitte stellen Sie sicher, dass das verwendete Textfragment existiert und unbeschädigt ist."
                + errorExt);
      }

      // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
      // später öffnen) aufnehmen
      if (count == 0)
        loadUrlStr = urlStr;
      else
        argsUrlStr[count - 1] = urlStr;
      count++;
    }

    // open document as Template (or as document):
    try
    {
      UnoService doc = new UnoService(desktop.xComponentLoader()
          .loadComponentFromURL(
              loadUrlStr,
              "_blank",
              FrameSearchFlag.CREATE,
              new UnoProps("AsTemplate", new Boolean(asTemplate)).getProps()));
      docFragUrlsBuffer.put(doc.xInterface(), argsUrlStr);
    }
    catch (java.lang.Exception x)
    {
      throw new com.sun.star.io.IOException(
          "Die Vorlage mit der URL \""
              + loadUrlStr
              + "\" kann nicht geöffnet werden.\n\n"
              + "Bitte stellen Sie sicher, dass das verwendete Textfragment existiert und unbeschädigt ist."
              + errorExt, x);
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_load(Event event) throws EndlessLoopException,
      WMCommandsFailedException
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    UnoService source = new UnoService(event.getSource());
    if (source.supportsService("com.sun.star.text.TextDocument"))
    {
      // beim on_opendocument erzeugte frag_id-liste aus puffer holen.
      String[] frag_urls = new String[] {};
      if (docFragUrlsBuffer.containsKey(source.xInterface()))
        frag_urls = (String[]) docFragUrlsBuffer.remove(source.xInterface());

      // Interpretation von WM-Kommandos
      new WMCommandInterpreter(source.xTextDocument(), mux, frag_urls)
          .interpret();
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_frame_changed(Event event)
      throws EndlessLoopException, WMCommandsFailedException
  {
    // Hier wurden früher die In OOo-eingebetteten Menüs und Symbolleiten
    // geupdated. Das ist jetzt nicht mehr notwendig.
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_set_sender(Event event)
  {
    // prüfen der Event-Argumente
    if (event.getArgs().size() == 2
        && event.getArgs().get(0) instanceof String
        && event.getArgs().get(1) instanceof Integer)
    {
      String sender = (String) event.getArgs().get(0);
      Integer idx = (Integer) event.getArgs().get(1);

      DJDatasetListElement[] pal = WollMuxSingleton.getInstance()
          .getSortedPALEntries();

      // nur den neuen Absender setzen, wenn index und sender übereinstimmen,
      // d.h.
      // die Absenderliste der entfernten WollMuxBar konsistent war.
      if (idx.intValue() >= 0
          && idx.intValue() < pal.length
          && pal[idx.intValue()].toString().equals(sender))
      {
        pal[idx.intValue()].getDataset().select();
      }
      else
      {
        Logger.error("Setzen des Senders \""
                     + sender
                     + "\" schlug fehl, da der index \""
                     + idx
                     + "\" nicht mit der PAL übereinstimmt (Inkosistenzen?)");
      }
    }

    WollMuxSingleton.getInstance().getEventProcessor().addEvent(
        new Event(Event.ON_SELECTION_CHANGED));
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_initialize()
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    DatasourceJoiner dsj = mux.getDatasourceJoiner();

    // falls es noch keine Datensätze im LOS gibt.
    if (dsj.getLOS().size() == 0)
    {

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
      if (!vorname.equals("") && !nachname.equals("")) try
      {
        r = dsj.find("Vorname", vorname, "Nachname", nachname);
      }
      catch (TimeoutException e)
      {
        Logger.error(e);
      }

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
      mux.getEventProcessor().addEvent(new Event(Event.ON_ABSENDER_AUSWAEHLEN));
    }
    else
    {
      String message = "Die folgenden Datensätze konnten nicht "
                       + "aus der Datenbank aktualisiert werden. "
                       + "Wenn dieses Problem nicht temporärer "
                       + "Natur ist, sollten sie diese Datensätze aus "
                       + "ihrer Absenderliste löschen und neu hinzufügen:\n\n";
      List l = dsj.getStatus().lostDatasets;
      if (l.size() > 0)
      {
        Iterator i = l.iterator();
        while (i.hasNext())
        {
          Dataset ds = (Dataset) i.next();
          try
          {
            message += ds.get("Nachname") + ", ";
            message += ds.get("Vorname") + " (";
            message += ds.get("Rolle") + ")\n";
          }
          catch (ColumnNotFoundException x)
          {
            Logger.error(x);
          }
        }
        showInfoModal("WollMux-Info", message);
      }
    }
    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_add_pal_change_event_listener(Event e)
  {
    XPALChangeEventListener l = (XPALChangeEventListener) UnoRuntime
        .queryInterface(XPALChangeEventListener.class, e.getSource());
    WollMuxSingleton.getInstance().addPALChangeEventListener(l);

    WollMuxSingleton.getInstance().getEventProcessor().addEvent(
        new Event(Event.ON_SELECTION_CHANGED));

    return EventProcessor.processTheNextEvent;
  }

  private static boolean on_remove_pal_change_event_listener(Event e)
  {
    XPALChangeEventListener l = (XPALChangeEventListener) UnoRuntime
        .queryInterface(XPALChangeEventListener.class, e.getSource());
    WollMuxSingleton.getInstance().removePALChangeEventListener(l);
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
      XComponentContext m_xCmpCtx = WollMuxSingleton.getInstance()
          .getXComponentContext();

      // hole aktuelles Window:
      UnoService desktop = UnoService.createWithContext(
          "com.sun.star.frame.Desktop",
          m_xCmpCtx);
      // TODO: nicht den currentFrame zur gewinnung des xParent verwenden, da
      // mit der WollMuxBar die Vorraussetzung entfällt, dass IMMER bereits ein
      // bestehendes Dokument / ein bestehender Frame vorhanden ist. Besser ist
      // es, ein neues parent-Window zu erzeugen. Wie geht das?
      XFrame xFrame = desktop.xDesktop().getCurrentFrame();
      XWindow xParent = null;
      if (xFrame != null) xParent = xFrame.getContainerWindow();

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
          WollMuxSingleton.getInstance().getXComponentContext());

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
