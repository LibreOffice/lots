/*
 * Dateiname: WollMuxEventHandler.java
 * Projekt  : WollMux
 * Funktion : Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.10.2005 | LUT | Erstellung als EventHandler.java
 * 01.12.2005 | BNK | +on_unload() das die Toolbar neu erzeugt (böser Hack zum 
 *                  | Beheben des Seitenansicht-Toolbar-Verschwindibus-Problems)
 *                  | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu können beim Lesen des Logfiles
 * 27.03.2005 | LUT | neues Kommando openDocument
 * 21.04.2006 | LUT | +ConfigurationErrorException statt NodeNotFoundException bei
 *                    fehlendem URL-Attribut in Textfragmenten
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 *                    + Überarbeitung vieler Fehlermeldungen
 *                    + Zeilenumbrüche in showInfoModal, damit keine unlesbaren
 *                      Fehlermeldungen mehr ausgegeben werden.
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
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
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
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;

/**
 * Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{

  /**
   * Interface für die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausführung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public boolean process();

    /**
     * Gibt an, ob das Event eine Referenz auf das Objekt o, welches auch ein
     * UNO-Service sein kann, enthält.
     */
    public boolean requires(Object o);
  }

  private static class WollMuxFehlerException extends java.lang.Exception
  {
    private static final long serialVersionUID = 3618646713098791791L;

    public WollMuxFehlerException(String msg)
    {
      super(msg);
    }

    public WollMuxFehlerException(String msg, java.lang.Exception e)
    {
      super(msg, e);
    }

  }

  /**
   * Dient als Basisklasse für konkrete Event-Implementierungen.
   */
  private static class BasicEvent implements WollMuxEvent
  {

    /**
     * Diese Method ist für die Ausführung des Events zuständig. Nach der
     * Bearbeitung entscheidet der Rückgabewert ob unmittelbar die Bearbeitung
     * des nächsten Events gestartet werden soll oder ob das GUI blockiert
     * werden soll bis das nächste actionPerformed-Event beim EventProcessor
     * eintrifft.
     * 
     * @return einer der Werte <code>EventProcessor.processNextEvent</code>
     *         oder <code>EventProcessor.waitForGUIReturn</code>.
     */
    public boolean process()
    {
      Logger.debug("Process WollMuxEvent " + this.getClass().getSimpleName());
      try
      {
        return doit();
      }
      catch (WollMuxFehlerException e)
      {
        errorMessage(e);
      }
      // doit() darf zwar ausser der WollMuxFehler Exception keine weiteren
      // Exceptions werfen, hier aber der Notnagel für alle nicht in doit
      // abgefangenen Runtime-Exceptions.
      catch (Throwable t)
      {
        Logger.error(t);
      }
      return EventProcessor.processTheNextEvent;
    }

    /**
     * Logged die übergebene Fehlermeldung nach Logger.error() und erzeugt ein
     * Dialogfenster mit der Fehlernachricht.
     */
    private void errorMessage(Throwable t)
    {
      Logger.error(t);
      String msg = "";
      if (t.getMessage() != null) msg += t.getMessage();
      Throwable c = t.getCause();
      if (c != null)
      {
        msg += "\n\n" + c;
      }
      showInfoModal("WollMux-Fehler", msg);
    }

    /**
     * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in
     * der die eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle
     * auftretenden Exceptions selbst behandeln, Fehler die jedoch
     * benutzersichtbar in einem Dialog angezeigt werden sollen, können über
     * eine WollMuxFehlerException nach oben weitergereicht werden.
     * 
     * @return EventProcessor.processTheNextEvent oder
     *         EventProcessor.waitForGUIReturn
     */
    protected boolean doit() throws WollMuxFehlerException
    {
      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return false;
    }

  }

  private static void handle(WollMuxEvent event)
  {
    WollMuxSingleton.getInstance().getEventProcessor().addEvent(event);
  }

  public static void handleShowDialogAbsenderAuswaehlen()
  {
    handle(new OnShowDialogAbsenderAuswaehlen());
  }

  private static class OnShowDialogAbsenderAuswaehlen extends BasicEvent
  {
    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      ConfigThingy conf = mux.getWollmuxConf();

      // Konfiguration auslesen:
      ConfigThingy whoAmIconf;
      ConfigThingy PALconf;
      ConfigThingy ADBconf;
      try
      {
        whoAmIconf = requireLastSection(conf, "AbsenderAuswaehlen");
        PALconf = requireLastSection(conf, "PersoenlicheAbsenderliste");
        ADBconf = requireLastSection(conf, "AbsenderdatenBearbeiten");
      }
      catch (ConfigurationErrorException e)
      {
        throw new WollMuxFehlerException(
            "Der Dialog konnte nicht gestartet werden!", e);
      }

      // Dialog starten:
      try
      {
        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, mux
            .getDatasourceJoiner(), mux.getEventProcessor());
      }
      catch (java.lang.Exception e)
      {
        throw new WollMuxFehlerException(
            "Der Dialog konnte nicht gestartet werden!", e);
      }
      return EventProcessor.waitForGUIReturn;
    }
  }

  public static void handleShowDialogPersoenlicheAbsenderliste()
  {
    handle(new OnShowDialogPersoenlicheAbsenderlisteVerwalten());
  }

  private static class OnShowDialogPersoenlicheAbsenderlisteVerwalten extends
      BasicEvent
  {
    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      ConfigThingy conf = mux.getWollmuxConf();

      // Konfiguration auslesen:
      ConfigThingy PALconf;
      ConfigThingy ADBconf;
      try
      {
        PALconf = requireLastSection(conf, "PersoenlicheAbsenderliste");
        ADBconf = requireLastSection(conf, "AbsenderdatenBearbeiten");
      }
      catch (ConfigurationErrorException e)
      {
        throw new WollMuxFehlerException(
            "Der Dialog konnte nicht gestartet werden!", e);
      }

      // Dialog starten:
      try
      {
        new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, mux
            .getDatasourceJoiner(), mux.getEventProcessor());
      }
      catch (java.lang.Exception e)
      {
        throw new WollMuxFehlerException(
            "Der Dialog konnte nicht gestartet werden!", e);
      }

      return EventProcessor.waitForGUIReturn;
    }
  }

  public static void handleProcessTextDocument(XTextDocument xTextDoc)
  {
    handle(new OnProcessTextDocument(xTextDoc));
  }

  // früher ON_LOAD und ON_NEW
  private static class OnProcessTextDocument extends BasicEvent
  {
    XTextDocument xTextDoc;

    /**
     * Dieses Feld stellt ein Zwischenspeicher für Fragment-Urls dar, der
     * Dokument-Instanzen auf Fragment-URL-Listen mapped. Es wird dazu benutzt,
     * im Fall eines openTemplate-Befehls die urls der übergebenen frag_id-Liste
     * temporär zu speichern. Das Event on_new/on_load holt sich die temporär
     * gespeicherten Argumente aus der hashMap und übergibt sie dem
     * WMCommandInterpreter.
     */
    private static HashMap docFragUrlsBuffer = new HashMap();

    public OnProcessTextDocument(XTextDocument xTextDoc)
    {
      this.xTextDoc = xTextDoc;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      UnoService doc = new UnoService(xTextDoc);
      if (doc.supportsService("com.sun.star.text.TextDocument"))
      {
        // Konfigurationsabschnitt Textdokument verarbeiten:
        ConfigThingy tds = new ConfigThingy("Textdokument");
        try
        {
          tds = mux.getWollmuxConf().query("Fenster").getLastChild().query(
              "Textdokument").getLastChild();
          // Einstellungen setzen:
          setWindowViewSettings(doc, tds);
        }
        catch (NodeNotFoundException e)
        {
        }

        // Beim on_opendocument erzeugte frag_id-liste aus puffer holen.
        String[] fragUrls = new String[] {};
        if (docFragUrlsBuffer.containsKey(doc.xInterface()))
          fragUrls = (String[]) docFragUrlsBuffer.remove(doc.xInterface());

        // Mögliche Aktionen für das neu geöffnete Dokument:
        boolean processNormalCommands = false;
        boolean processFormCommands = false;

        // Bestimmung des Dokumenttyps (openAsTemplate?):
        if (doc.xTextDocument() != null)
          processNormalCommands = (doc.xTextDocument().getURL() == null || doc
              .xTextDocument().getURL().equals(""));

        // Auswerten der Special-Bookmarks "WM(CMD 'setType' TYPE '...')"
        if (doc.xBookmarksSupplier() != null)
        {
          XNameAccess bookmarks = doc.xBookmarksSupplier().getBookmarks();
          if (bookmarks.hasByName(DocumentCommand.SETTYPE_normalTemplate))
          {
            processNormalCommands = true;
            processFormCommands = false;

            // Bookmark löschen
            removeBookmark(doc, DocumentCommand.SETTYPE_normalTemplate);
          }
          else if (bookmarks
              .hasByName(DocumentCommand.SETTYPE_templateTemplate))
          {
            processNormalCommands = false;
            processFormCommands = false;

            // Bookmark löschen
            removeBookmark(doc, DocumentCommand.SETTYPE_templateTemplate);
          }
          else if (bookmarks.hasByName(DocumentCommand.SETTYPE_formDocument))
          {
            processNormalCommands = false;
            processFormCommands = true;

            // Das Bookmark wird NICHT aus dem Dokument gelöscht, da ein
            // formDocument immer ein formDocument bleiben soll.
          }
        }

        // Ausführung der Dokumentkommandos
        if (processNormalCommands || processFormCommands)
        {
          DocumentCommandInterpreter dci = new DocumentCommandInterpreter(doc
              .xTextDocument(), mux);

          try
          {
            if (processNormalCommands) dci.executeTemplateCommands(fragUrls);

            if (processFormCommands || dci.isFormular())
              dci.executeFormCommands();
          }
          catch (java.lang.Exception e)
          {
            throw new WollMuxFehlerException(
                "Fehler bei der Dokumentbearbeitung.", e);
          }
        }
      }
      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(xTextDoc, o);
    }

    /**
     * Die Methode löscht das Bookmark name aus dem Dokument doc und setzt den
     * document-modified-Status anschließend auf false, weil nur wirkliche
     * Benutzerinteraktion zur Speichern-Abfrage beim Schließen führen sollte.
     * 
     * @param doc
     * @param name
     */
    private static void removeBookmark(UnoService doc, String name)
    {
      try
      {
        if (doc.xBookmarksSupplier() != null)
        {
          Bookmark b = new Bookmark(name, doc.xBookmarksSupplier());
          b.remove();
        }
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }

      // So tun als ob das Dokument (durch das Löschen des Bookmarks) nicht
      // verändert worden wäre:
      if (doc.xModifiable() != null) try
      {
        doc.xModifiable().setModified(false);
      }
      catch (PropertyVetoException e)
      {
        Logger.error(e);
      }
    }

    /**
     * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und
     * ZOOM aus dem übergebenen Konfigurations-Abschnitt settings und setzt die
     * Fenstereinstellungen der Komponente compo entsprechend um. Bei den
     * Pärchen X/Y bzw. SIZE/WIDTH müssen jeweils beide Komponenten im
     * Konfigurationsabschnitt angegeben sein.
     * 
     * @param compo
     *          Die Komponente, deren Fenstereinstellungen gesetzt werden sollen
     * @param settings
     *          der Konfigurationsabschnitt, der X, Y, WIDHT, HEIGHT und ZOOM
     *          als direkte Kinder enthält.
     */
    private static void setWindowViewSettings(UnoService compo,
        ConfigThingy settings)
    {
      // Fenster holen (zum setzen der Fensterposition und des Zooms)
      UnoService window = new UnoService(null);
      XFrame frame = null;
      if (compo.xModel() != null)
        frame = compo.xModel().getCurrentController().getFrame();
      UnoService controller = new UnoService(compo.xModel()
          .getCurrentController());
      if (frame != null)
      {
        window = new UnoService(frame.getContainerWindow());
      }

      // Insets bestimmen (Rahmenmaße des Windows)
      int insetLeft = 0, insetTop = 0, insetRight = 0, insetButtom = 0;
      if (window.xDevice() != null)
      {
        DeviceInfo di = window.xDevice().getInfo();
        insetButtom = di.BottomInset;
        insetTop = di.TopInset;
        insetRight = di.RightInset;
        insetLeft = di.LeftInset;
      }

      // Position setzen:
      try
      {
        int xPos = new Integer(settings.get("X").toString()).intValue();
        int yPos = new Integer(settings.get("Y").toString()).intValue();
        if (window.xWindow() != null)
        {
          window.xWindow().setPosSize(
              xPos + insetLeft,
              yPos + insetTop,
              0,
              0,
              PosSize.POS);
        }
      }
      catch (java.lang.Exception e)
      {
      }
      // Dimensions setzen:
      try
      {
        int width = new Integer(settings.get("WIDTH").toString()).intValue();
        int height = new Integer(settings.get("HEIGHT").toString()).intValue();
        if (window.xWindow() != null)
          window.xWindow().setPosSize(
              0,
              0,
              width - insetLeft - insetRight,
              height - insetTop - insetButtom,
              PosSize.SIZE);
      }
      catch (java.lang.Exception e)
      {
      }
      // Zoom setzen:
      try
      {
        Short zoom = new Short(settings.get("ZOOM").toString());
        XPropertySet viewSettings = null;
        if (controller.xViewSettingsSupplier() != null)
          viewSettings = controller.xViewSettingsSupplier().getViewSettings();
        if (viewSettings != null)
          viewSettings.setPropertyValue("ZoomValue", zoom);
      }
      catch (java.lang.Exception e)
      {
      }
    }
  }

  public static void handleOpenDocument(Vector fragIDs, boolean asTemplate)
  {
    handle(new OnOpenDocument(fragIDs, asTemplate));
  }

  // früher ON_OPENDOCUMENT und ON_OPENTEMPLATE
  private static class OnOpenDocument extends BasicEvent
  {
    private boolean asTemplate;

    private Vector fragIDs;

    private OnOpenDocument(Vector fragIDs, boolean asTemplate)
    {
      this.fragIDs = fragIDs;
      this.asTemplate = asTemplate;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      UnoService desktop = new UnoService(null);
      try
      {
        desktop = UnoService.createWithContext(
            "com.sun.star.frame.Desktop",
            mux.getXComponentContext());
      }
      catch (Exception e)
      {
        Logger.error(e);
      }

      // das erste Argument ist das unmittelbar zu landende Textfragment und
      // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
      // werden nach argsUrlStr aufgelöst.
      String loadUrlStr = "";
      String[] fragUrls = new String[fragIDs.size() - 1];

      Iterator iter = fragIDs.iterator();
      for (int i = 0; iter.hasNext(); ++i)
      {
        String frag_id = (String) iter.next();

        // Fragment-URL holen und aufbereiten:
        String urlStr;
        try
        {
          urlStr = mux.getTextFragmentList().getURLByID(frag_id);
        }
        catch (java.lang.Exception e)
        {
          throw new WollMuxFehlerException(
              "Die URL zum Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht bestimmt werden.", e);
        }
        URL url;
        try
        {
          url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
          urlStr = url.toExternalForm();
        }
        catch (MalformedURLException e)
        {
          throw new WollMuxFehlerException(
              "Die URL '"
                  + urlStr
                  + "' des Textfragments mit der FRAG_ID '"
                  + frag_id
                  + "' ist ungültig.", e);
        }

        // URL durch den URL-Transformer jagen
        try
        {
          UnoService trans = UnoService.createWithContext(
              "com.sun.star.util.URLTransformer",
              mux.getXComponentContext());
          com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
          unoURL[0].Complete = urlStr;
          trans.xURLTransformer().parseStrict(unoURL);
          urlStr = unoURL[0].Complete;
        }
        catch (Exception e)
        {
          Logger.error(e);
        }

        // Workaround für Fehler in insertDocumentFromURL: Prüfen ob URL
        // aufgelöst werden kann, da sonst der insertDocumentFromURL einfriert.
        try
        {
          url = new URL(urlStr);
        }
        catch (MalformedURLException e)
        {
          // darf nicht auftreten, da url bereits oben geprüft wurde...
          Logger.error(e);
        }
        URLConnection con = null;
        try
        {
          con = url.openConnection();
        }
        catch (IOException e)
        {
          // Ich hab noch nicht rausgefunden, wann diese Exception geworfen
          // wird! auf jeden Fall NICHT, wenn die URL nicht aufgelöst werden
          // kann!!
          Logger.error(e);
        }
        if (con != null && con.getContentLength() <= 0)
        {
          throw new WollMuxFehlerException(
              "Die URL '"
                  + url.toExternalForm()
                  + "' des Textfragments mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht aufgelöst werden!\n\n"
                  + "Bitte stellen Sie sicher, dass das verwendete Textfragment existiert und unbeschädigt ist!");
        }

        // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
        // später öffnen) aufnehmen
        if (i == 0)
          loadUrlStr = urlStr;
        else
          fragUrls[i - 1] = urlStr;
      }

      // open document as Template (or as document):
      if (desktop.xComponentLoader() != null)
      {
        try
        {
          UnoService doc = new UnoService(desktop.xComponentLoader()
              .loadComponentFromURL(
                  loadUrlStr,
                  "_blank",
                  FrameSearchFlag.CREATE,
                  new UnoProps("AsTemplate", new Boolean(asTemplate))
                      .getProps()));
          OnProcessTextDocument.docFragUrlsBuffer.put(
              doc.xInterface(),
              fragUrls);
        }
        catch (java.lang.Exception x)
        {
          throw new WollMuxFehlerException("Die Vorlage mit der URL '"
                                           + loadUrlStr
                                           + "' kann nicht geöffnet werden.", x);
        }
      }

      return EventProcessor.processTheNextEvent;
    }
  }

  public static void handlePALChangedNotify()
  {
    handle(new OnPALChangedNotify());
  }

  // return "ON_SELECTION_CHANGED";
  private static class OnPALChangedNotify extends BasicEvent
  {
    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // registrierte PALChangeListener updaten
      Iterator i = mux.palChangeListenerIterator();
      while (i.hasNext())
      {
        Logger.debug2("OnPALChangedNotify: Update XPALChangeEventListener");
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
        mux.getDatasourceJoiner().saveCacheAndLOS(
            WollMuxFiles.getLosCacheFile());
      }
      catch (IOException e)
      {
        Logger.error(e);
      }

      return EventProcessor.processTheNextEvent;
    }
  }

  public static void handleSetSender(String senderName, int idx)
  {
    handle(new OnSetSender(senderName, idx));
  }

  // return "ON_SET_SENDER";
  private static class OnSetSender extends BasicEvent
  {
    private String senderName;

    private int idx;

    public OnSetSender(String senderName, int idx)
    {
      this.senderName = senderName;
      this.idx = idx;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      DJDatasetListElement[] pal = WollMuxSingleton.getInstance()
          .getSortedPALEntries();

      // nur den neuen Absender setzen, wenn index und sender übereinstimmen,
      // d.h.
      // die Absenderliste der entfernten WollMuxBar konsistent war.
      if (idx >= 0
          && idx < pal.length
          && pal[idx].toString().equals(senderName))
      {
        pal[idx].getDataset().select();
      }
      else
      {
        Logger.error("Setzen des Senders '"
                     + senderName
                     + "' schlug fehl, da der index '"
                     + idx
                     + "' nicht mit der PAL übereinstimmt (Inkosistenzen?)");
      }
      WollMuxEventHandler.handlePALChangedNotify();
      return EventProcessor.processTheNextEvent;
    }
  }

  public static void handleInitialize()
  {
    handle(new OnInitialize());
  }

  // return "ON_INITIALIZE";
  private static class OnInitialize extends BasicEvent
  {
    protected boolean doit() throws WollMuxFehlerException
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
        WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
      }
      else
      {
        // Liste der nicht zuordnenbaren Datensätze erstellen und ausgeben:
        String names = "";
        List l = dsj.getStatus().lostDatasets;
        if (l.size() > 0)
        {
          Iterator i = l.iterator();
          while (i.hasNext())
          {
            Dataset ds = (Dataset) i.next();
            try
            {
              names += "- " + ds.get("Nachname") + ", ";
              names += ds.get("Vorname") + " (";
              names += ds.get("Rolle") + ")\n";
            }
            catch (ColumnNotFoundException x)
            {
              Logger.error(x);
            }
          }
          String message = "Die folgenden Datensätze konnten nicht "
                           + "aus der Datenbank aktualisiert werden:\n\n"
                           + names
                           + "\nWenn dieses Problem nicht temporärer "
                           + "Natur ist, sollten sie diese Datensätze aus "
                           + "ihrer Absenderliste löschen und neu hinzufügen!";
          showInfoModal("WollMux-Info", message);
        }
      }
      return EventProcessor.processTheNextEvent;
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

  public static void handleAddPALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnAddPALChangeEventListener(listener));
  }

  // return "ON_ADD_PAL_CHANGE_EVENT_LISTENER";
  private static class OnAddPALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    public OnAddPALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton.getInstance().addPALChangeEventListener(listener);

      WollMuxEventHandler.handlePALChangedNotify();

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(listener, o);
    }
  }

  public static void handleRemovePALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnRemovePALChangeEventListener(listener));
  }

  // return "ON_REMOVE_PAL_CHANGE_EVENT_LISTENER";
  private static class OnRemovePALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    public OnRemovePALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton.getInstance().removePALChangeEventListener(listener);
      return EventProcessor.processTheNextEvent;
    }
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

      // wenn ein Frame vorhanden ist, wird dieser als Parent für die Erzeugung
      // einer Infobox über das Toolkit verwendet, ansonsten wird ein
      // swing-Dialog gestartet.
      XFrame xFrame = desktop.xDesktop().getCurrentFrame();
      if (xFrame != null)
      {
        XWindow xParent = xFrame.getContainerWindow();

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
      else
      {
        // zeige eine swing-infoBox an, falls kein OOo Parent vorhanden ist.

        // zu lange Strings umbrechen:
        final int MAXCHARS = 50;
        String formattedMessage = "";
        String[] lines = sMessage.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
          String[] words = lines[i].split(" ");
          int chars = 0;
          for (int j = 0; j < words.length; j++)
          {
            String word = words[j];
            if (chars > 0 && chars + word.length() > MAXCHARS)
            {
              formattedMessage += "\n";
              chars = 0;
            }
            formattedMessage += word + " ";
            chars += word.length() + 1;
          }
          if (i != lines.length - 1) formattedMessage += "\n";
        }

        // infobox ausgeben:
        Common.setLookAndFeel();
        javax.swing.JOptionPane.showMessageDialog(
            null,
            formattedMessage,
            sTitle,
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  private static ConfigThingy requireLastSection(ConfigThingy cf,
      String sectionName) throws ConfigurationErrorException
  {
    try
    {
      return cf.query(sectionName).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(
          "Der Schlüssel '"
              + sectionName
              + "' fehlt in der Konfigurationsdatei.", e);
    }
  }
}
