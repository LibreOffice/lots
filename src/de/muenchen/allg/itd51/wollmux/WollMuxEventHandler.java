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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNamed;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.PrintFailedException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunction;

/**
 * Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{
  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu
   * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf false
   * gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public static void setAcceptEvents(boolean accept)
  {
    EventProcessor.getInstance().setAcceptEvents(accept);
  }

  /**
   * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
   * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
   * hineingepackt und von einem einzigen eventProcessingThread sequentiell
   * abgearbeitet.
   * 
   * @author lut
   */
  public static class EventProcessor
  {
    /**
     * Gibt an, ob der EventProcessor überhaupt events entgegennimmt. Ist
     * acceptEvents=false, werden alle Events ignoriert.
     */
    private boolean acceptEvents = false;

    private List eventQueue = new LinkedList();

    private static EventProcessor singletonInstance;

    private static Thread eventProcessorThread;

    private static EventProcessor getInstance()
    {
      if (singletonInstance == null) singletonInstance = new EventProcessor();
      return singletonInstance;
    }

    /**
     * Mit dieser Methode ist es möglich die Entgegennahme von Events zu
     * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf
     * false gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
     * 
     * @param accept
     */
    private void setAcceptEvents(boolean accept)
    {
      acceptEvents = accept;
      if (accept)
        Logger.debug("EventProcessor: akzeptiere neue Events.");
      else
        Logger.debug("EventProcessor: blockiere Entgegennahme von Events!");
    }

    private EventProcessor()
    {
      // starte den eventProcessorThread
      eventProcessorThread = new Thread(new Runnable()
      {
        public void run()
        {
          Logger.debug("Starte EventProcessor-Thread");
          try
          {
            while (true)
            {
              WollMuxEvent event;
              synchronized (eventQueue)
              {
                while (eventQueue.isEmpty())
                  eventQueue.wait();
                event = (WollMuxEvent) eventQueue.remove(0);
              }

              event.process();
            }
          }
          catch (InterruptedException e)
          {
            Logger.error("EventProcessor-Thread wurde unterbrochen:");
            Logger.error(e);
          }
          Logger.debug("Beende EventProcessor-Thread");
        }
      });
      eventProcessorThread.start();
    }

    /**
     * Diese Methode fügt ein Event an die eventQueue an wenn der WollMux
     * erfolgreich initialisiert wurde und damit events akzeptieren darf.
     * Anschliessend weckt sie den EventProcessor-Thread.
     * 
     * @param event
     */
    private void addEvent(WollMuxEventHandler.WollMuxEvent event)
    {
      if (acceptEvents) synchronized (eventQueue)
      {
        eventQueue.add(event);
        eventQueue.notifyAll();
      }
    }
  }

  /**
   * Interface für die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausführung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public void process();

    /**
     * Gibt an, ob das Event eine Referenz auf das Objekt o, welches auch ein
     * UNO-Service sein kann, enthält.
     */
    public boolean requires(Object o);
  }

  /**
   * Repräsentiert einen Fehler, der benutzersichtbar in einem Fehlerdialog
   * angezeigt wird.
   * 
   * @author christoph.lutz
   */
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

  private static class CantStartDialogException extends WollMuxFehlerException
  {
    private static final long serialVersionUID = -1130975078605219254L;

    public CantStartDialogException(java.lang.Exception e)
    {
      super("Der Dialog konnte nicht gestartet werden!\n\n"
            + "Bitte kontaktieren Sie Ihre Systemadministration.", e);
    }
  }

  /**
   * Dient als Basisklasse für konkrete Event-Implementierungen.
   */
  private static class BasicEvent implements WollMuxEvent
  {

    private boolean[] lock = new boolean[] { true };

    /**
     * Diese Method ist für die Ausführung des Events zuständig. Nach der
     * Bearbeitung entscheidet der Rückgabewert ob unmittelbar die Bearbeitung
     * des nächsten Events gestartet werden soll oder ob das GUI blockiert
     * werden soll bis das nächste actionPerformed-Event beim EventProcessor
     * eintrifft.
     */
    public void process()
    {
      Logger.debug("Process WollMuxEvent " + this.toString());
      try
      {
        doit();
      }
      catch (WollMuxFehlerException e)
      {
        errorMessage(e);
      }
      // Notnagel für alle Runtime-Exceptions.
      catch (Throwable t)
      {
        Logger.error(t);
      }
    }

    /**
     * Logged die übergebene Fehlermeldung nach Logger.error() und erzeugt ein
     * Dialogfenster mit der Fehlernachricht.
     */
    protected void errorMessage(Throwable t)
    {
      Logger.error(t);
      String msg = "";
      if (t.getMessage() != null) msg += t.getMessage();
      Throwable c = t.getCause();
      if (c != null)
      {
        msg += "\n\n" + c;
      }
      WollMuxSingleton.showInfoModal("WollMux-Fehler", msg);
    }

    /**
     * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in
     * der die eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle
     * auftretenden Exceptions selbst behandeln, Fehler die jedoch
     * benutzersichtbar in einem Dialog angezeigt werden sollen, können über
     * eine WollMuxFehlerException nach oben weitergereicht werden.
     */
    protected void doit() throws WollMuxFehlerException
    {
    };

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.WollMuxEventHandler.WollMuxEvent#requires(java.lang.Object)
     */
    public boolean requires(Object o)
    {
      return false;
    }

    public String toString()
    {
      return this.getClass().getSimpleName();
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um quasi Modalität für nicht
     * modale Dialoge zu realisieren. setLock() sollte stets vor dem Aufruf des
     * nicht modalen Dialogs erfolgen, nach dem Aufruf des nicht modalen Dialogs
     * folgt der Aufruf der waitForUnlock()-Methode. Der nicht modale Dialog
     * erzeugt bei der Beendigung ein ActionEvent, das dafür sorgt, dass
     * setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      synchronized (lock)
      {
        lock[0] = true;
      }
    }

    /**
     * Macht einen mit setLock() gesetzten Lock rückgängig und bricht damit eine
     * evtl. wartende waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der
     * Methode setUnlock() aufgehoben wird. So kann die quasi Modalität nicht
     * modale Dialoge zu realisiert werden. setLock() sollte stets vor dem
     * Aufruf des nicht modalen Dialogs erfolgen, nach dem Aufruf des nicht
     * modalen Dialogs folgt der Aufruf der waitForUnlock()-Methode. Der nicht
     * modale Dialog erzeugt bei der Beendigung ein ActionEvent, das dafür
     * sorgt, dass setUnlock aufgerufen wird.
     */
    protected void waitForUnlock()
    {
      try
      {
        synchronized (lock)
        {
          while (lock[0] == true)
            lock.wait();
        }
      }
      catch (InterruptedException e)
      {
      }
    }

    /**
     * Dieser ActionListener kann nicht modalen Dialogen übergeben werden und
     * sorgt in Verbindung mit den Methoden setLock() und waitForUnlock() dafür,
     * dass quasi modale Dialoge realisiert werden können.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        actionEvent = arg0;
        setUnlock();
      }
    }

    /**
     * Dieser DispatchResultListener kann der Methode
     * XNotifyableDispatch.dispatchWithNotification übergeben werden und sorgt
     * in Verbindung mit den Methoden setLock() und waitForUnlock() dafür, dass
     * ein Dispatch quasi synchron ausgeführt werden kann.
     */
    protected UnlockDispatchResultListener unlockDispatchResultListener = new UnlockDispatchResultListener();

    protected class UnlockDispatchResultListener implements
        XDispatchResultListener
    {
      public DispatchResultEvent resultEvent = null;

      public void disposing(EventObject arg0)
      {
        setUnlock();
      }

      public void dispatchFinished(DispatchResultEvent arg0)
      {
        resultEvent = arg0;
        setUnlock();
      }
    }
  }

  /**
   * Stellt das WollMuxEvent event in die EventQueue des EventProcessors.
   * 
   * @param event
   */
  private static void handle(WollMuxEvent event)
  {
    EventProcessor.getInstance().addEvent(event);
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog AbsenderAuswaehlen startet.
   */
  public static void handleShowDialogAbsenderAuswaehlen()
  {
    handle(new OnShowDialogAbsenderAuswaehlen());
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst und sorgt dafür, dass der Dialog
   * AbsenderAuswählen gestartet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnShowDialogAbsenderAuswaehlen extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
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
        throw new CantStartDialogException(e);
      }

      // Dialog modal starten:
      try
      {
        setLock();
        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, mux
            .getDatasourceJoiner(), unlockActionListener);
        waitForUnlock();
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }

      WollMuxEventHandler.handlePALChangedNotify();
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog
   * PersoenlichtAbsenderListe-Verwalten startet.
   */
  public static void handleShowDialogPersoenlicheAbsenderliste()
  {
    handle(new OnShowDialogPersoenlicheAbsenderlisteVerwalten());
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst und sorgt dafür, dass der Dialog
   * PersönlicheAbsendeliste-Verwalten gestartet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnShowDialogPersoenlicheAbsenderlisteVerwalten extends
      BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
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
        throw new CantStartDialogException(e);
      }

      // Dialog modal starten:
      try
      {
        setLock();
        new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, mux
            .getDatasourceJoiner(), unlockActionListener);
        waitForUnlock();
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }

      handlePALChangedNotify();
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Funktionsdialog dialogName aufruft
   * und die zurückgelieferten Werte in die entsprechenden FormField-Objekte des
   * Dokuments doc einträgt.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst.
   */
  public static void handleFunctionDialog(XTextDocument doc, String dialogName)
  {
    handle(new OnFunctionDialog(doc, dialogName));
  }

  private static class OnFunctionDialog extends BasicEvent
  {
    private XTextDocument doc;

    private String dialogName;

    private OnFunctionDialog(XTextDocument doc, String dialogName)
    {
      this.doc = doc;
      this.dialogName = dialogName;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // Dialog aus Funktionsdialog-Bibliothek holen:
      Dialog dialog = mux.getFunctionDialogs().get(dialogName);
      if (dialog == null)
        throw new WollMuxFehlerException("Funktionsdialog '"
                                         + dialogName
                                         + "' ist nicht definiert.");

      // Dialoginstanz erzeugen und modal anzeigen:
      Dialog dialogInst = null;
      try
      {
        dialogInst = dialog.instanceFor(new HashMap());

        setLock();
        dialogInst.show(unlockActionListener, mux.getGlobalFunctions(), mux
            .getFunctionDialogs());
        waitForUnlock();
      }
      catch (ConfigurationErrorException e)
      {
        throw new CantStartDialogException(e);
      }

      // Abbruch, wenn der Dialog nicht mit OK beendet wurde.
      String cmd = unlockActionListener.actionEvent.getActionCommand();
      if (!cmd.equalsIgnoreCase("select")) return;

      // Dem Dokument den Fokus geben, damit die Änderungen des Benutzers
      // transparent mit verfolgt werden können.
      try
      {
        UNO.XModel(doc).getCurrentController().getFrame().getContainerWindow()
            .setFocus();
      }
      catch (java.lang.Exception e)
      {
        // keine Gefährdung des Ablaufs falls das nicht klappt.
      }

      // Alle Werte die der Funktionsdialog sicher zurück liefert werden in
      // das Dokument übernommen.
      TextDocumentModel model = mux.getTextDocumentModel(doc);
      Collection schema = dialogInst.getSchema();
      Iterator iter = schema.iterator();
      while (iter.hasNext())
      {
        String id = (String) iter.next();
        String value = dialogInst.getData(id).toString();

        // Formularwerte ins Model uns ins Dokument übernehmen.
        model.setFormFieldValue(id, value);
        model.updateFormFields(id);
      }
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", '"
             + dialogName
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das einen modalen Dialog anzeigt, der
   * wichtige Versionsinformationen über den WollMux, die Konfiguration und die
   * WollMuxBar (nur falls wollmuxBarVersion nicht der Leersting ist) enthält.
   * Anmerkung: das WollMux-Modul hat keine Ahnung, welche WollMuxBar verwendet
   * wird. Daher ist es möglich, über den Parameter wollMuxBarVersion eine
   * Versionsnummer der WollMuxBar zu übergeben, die im Dialog angezeigt wird,
   * falls wollMuxBarVersion nicht der Leerstring ist.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgelöst, wenn die
   * WollMux-url "wollmux:about" aufgerufen wurde.
   */
  public static void handleAbout(String wollMuxBarVersion)
  {
    handle(new OnAbout(wollMuxBarVersion));
  }

  private static class OnAbout extends BasicEvent
  {
    private String wollMuxBarVersion;

    private final URL WM_URL = this.getClass().getClassLoader().getResource(
        "data/wollmux_klein.jpg");

    private OnAbout(String wollMuxBarVersion)
    {
      this.wollMuxBarVersion = wollMuxBarVersion;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      String str = "WollMux " + mux.getBuildInfo();
      if (wollMuxBarVersion != null && !wollMuxBarVersion.equals(""))
        str += "\nWollMux-Leiste " + wollMuxBarVersion;

      str += "\n\nWollMux-Konfiguration: " + mux.getConfVersionInfo();
      str += "\n" + WollMuxFiles.getDEFAULT_CONTEXT().toExternalForm();

      Common.setLookAndFeelOnce();

      ImageIcon icon = new ImageIcon(WM_URL);

      JOptionPane pane = new JOptionPane(str, JOptionPane.INFORMATION_MESSAGE,
          JOptionPane.DEFAULT_OPTION, icon);
      JDialog dialog = pane.createDialog(
          null,
          "Info über Vorlagen und Formulare (WollMux)");

      // Hintergrundfarbe aller Komponenten auf weiss setzen
      try
      {
        pane.setBackground(Color.WHITE);
        pane.getComponent(0).setBackground(Color.WHITE);
        pane.getComponent(1).setBackground(Color.WHITE);
      }
      catch (java.lang.Exception e)
      {
      }

      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true); // Wartet bis OK Dialog beendet!
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den FormularMax4000 aufruft für das
   * Dokument doc.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst.
   */
  public static void handleFormularMax4000Show(XTextDocument doc)
  {
    handle(new OnFormularMax4000Show(doc));
  }

  private static class OnFormularMax4000Show extends BasicEvent
  {
    private final XTextDocument doc;

    private OnFormularMax4000Show(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {

      if (doc == null) return;

      final TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      // Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
      FormularMax4000 max = model.getCurrentFormularMax4000();
      if (max != null)
      {
        max.toFront();
      }
      else
      {
        max = new FormularMax4000(model, new ActionListener()
        {
          public void actionPerformed(ActionEvent actionEvent)
          {
            if (actionEvent.getSource() instanceof FormularMax4000)
              WollMuxEventHandler.handleFormularMax4000Returned(model);
          }
        }, WollMuxSingleton.getInstance().getGlobalFunctions(),
            WollMuxSingleton.getInstance().getGlobalPrintFunctions());
        model.setCurrentFormularMax4000(max);
      }
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein
   * FormularMax4000 beendet wird und die entsprechenden internen Referenzen
   * gelöscht werden können.
   * 
   * Dieses Event wird vom EventProcessor geworfen, wenn der FormularMax
   * zurückkehrt.
   */
  public static void handleFormularMax4000Returned(TextDocumentModel model)
  {
    handle(new OnFormularMax4000Returned(model));
  }

  private static class OnFormularMax4000Returned extends BasicEvent
  {
    private TextDocumentModel model;

    private OnFormularMax4000Returned(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      model.setCurrentFormularMax4000(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + model.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das Auskunft darüber gibt, dass ein
   * TextDokument geschlossen wurde und damit auch das TextDocumentModel
   * disposed werden soll.
   * 
   * Dieses Event wird ausgelöst, wenn ein TextDokument geschlossen wird.
   */
  public static void handleTextDocumentClosed(XTextDocument doc)
  {
    handle(new OnTextDocumentClosed(doc));
  }

  private static class OnTextDocumentClosed extends BasicEvent
  {
    private XTextDocument doc;

    private OnTextDocumentClosed(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      if (mux.hasTextDocumentModel(doc))
      {
        TextDocumentModel model = mux.getTextDocumentModel(doc);
        model.dispose();
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die eigentliche Dokumentbearbeitung
   * eines TextDokuments startet.
   * 
   * @param xTextDoc
   *          Das XTextDocument, das durch den WollMux verarbeitet werden soll.
   */
  public static void handleProcessTextDocument(XTextDocument xTextDoc)
  {
    handle(new OnProcessTextDocument(xTextDoc));
  }

  /**
   * Dieses Event wird immer dann ausgelöst, wenn der GlobalEventBroadcaster von
   * OOo ein ON_NEW oder ein ON_LOAD-Event wirft. Das Event sorgt dafür, dass
   * die eigentliche Dokumentbearbeitung durch den WollMux angestossen wird.
   * 
   * @author christoph.lutz
   */
  private static class OnProcessTextDocument extends BasicEvent
  {
    XTextDocument xTextDoc;

    public OnProcessTextDocument(XTextDocument xTextDoc)
    {
      this.xTextDoc = xTextDoc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (xTextDoc == null) return;

      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      TextDocumentModel model = mux.getTextDocumentModel(xTextDoc);

      // Konfigurationsabschnitt Textdokument verarbeiten:
      ConfigThingy tds = new ConfigThingy("Textdokument");
      try
      {
        tds = mux.getWollmuxConf().query("Fenster").query("Textdokument")
            .getLastChild();
        model.setWindowViewSettings(tds);
      }
      catch (NodeNotFoundException e)
      {
      }

      // Mögliche Aktionen für das neu geöffnete Dokument:
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          mux);

      try
      {
        // Dokumentkommandos setType, setPrintFunction, all, draftOnly und
        // notInOriginal auswerten.
        dci.scanDocumentSettings();

        // Bei Vorlagen: Ausführung der Dokumentkommandos
        if (model.isTemplate()) dci.executeTemplateCommands();

        // Bei Formularen: Konfigurationsabschnitt Fenster/Formular verarbeiten
        if (model.isFormDocument())
        {
          try
          {
            model.setDocumentZoom(mux.getWollmuxConf().query("Fenster").query(
                "Formular").getLastChild().query("ZOOM"));
          }
          catch (java.lang.Exception e)
          {
          }
        }

        // Bei Formularen: Ausführung der Dokumentkommandos des Formularsystems
        if (model.isFormDocument()) dci.executeFormCommands();
      }
      catch (java.lang.Exception e)
      {
        throw new WollMuxFehlerException("Fehler bei der Dokumentbearbeitung.",
            e);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + xTextDoc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die Bearbeitung aller neu
   * hinzugekommener Dokumentkommandos übernimmt.
   * 
   * Dieses Event wird immer dann ausgelöst, wenn nach dem Öffnen eines
   * Dokuments neue Dokumentkommandos eingefügt wurden, die nun bearbeitet
   * werden sollen - z.B. wenn ein Textbaustein eingefügt wurde.
   * 
   * @param xTextDoc
   *          Das XTextDocument, das durch den WollMux verarbeitet werden soll.
   */
  public static void handleReprocessTextDocument(XTextDocument xTextDoc)
  {
    handle(new OnReprocessTextDocument(xTextDoc));
  }

  private static class OnReprocessTextDocument extends BasicEvent
  {
    XTextDocument xTextDoc;

    public OnReprocessTextDocument(XTextDocument xTextDoc)
    {
      this.xTextDoc = xTextDoc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (xTextDoc == null) return;

      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      TextDocumentModel model = mux.getTextDocumentModel(xTextDoc);

      // Dokument mit neuen Dokumentkommandos über den
      // DocumentCommandInterpreter bearbeiten:
      model.getDocumentCommandTree().update();
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          mux);
      try
      {
        dci.scanDocumentSettings();

        dci.executeTemplateCommands();
      }
      catch (java.lang.Exception e)
      {
        // Hier wird keine Exception erwartet, da Fehler (z.B. beim manuellen
        // Einfügen von Textbausteinen) bereits dort als Popup angezeigt werden
        // sollen, wo sie auftreten.
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + xTextDoc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass ein Dokument
   * geöffnet wird.
   * 
   * @param fragIDs
   *          Ein Vector mit fragIDs, wobei das erste Element die FRAG_ID des zu
   *          öffnenden Dokuments beinhalten muss. Weitere Elemente werden in
   *          eine Liste zusammengefasst und als Parameter für das
   *          Dokumentkommando insertContent verwendet.
   * @param asTemplate
   *          true, wenn das Dokument als "Unbenannt X" geöffnet werden soll
   *          (also im "Template-Modus") und false, wenn das Dokument zum
   *          Bearbeiten geöffnet werden soll.
   */
  public static void handleOpenDocument(Vector fragIDs, boolean asTemplate)
  {
    handle(new OnOpenDocument(fragIDs, asTemplate));
  }

  /**
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:openTemplate bzw. wollmux:openDocument empfängt
   * und sort dafür, dass das entsprechende Dokument geöffnet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnOpenDocument extends BasicEvent
  {
    private boolean asTemplate;

    private Vector fragIDs;

    private OnOpenDocument(Vector fragIDs, boolean asTemplate)
    {
      this.fragIDs = fragIDs;
      this.asTemplate = asTemplate;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // das erste Argument ist das unmittelbar zu landende Textfragment und
      // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
      // werden nach argsUrlStr aufgelöst.
      String loadUrlStr = "";
      String[] fragUrls = new String[fragIDs.size() - 1];
      String urlStr = "";

      Iterator iter = fragIDs.iterator();
      for (int i = 0; iter.hasNext(); ++i)
      {
        String frag_id = (String) iter.next();

        // Fragment-URL holen und aufbereiten:
        Vector urls = new Vector();

        urls = VisibleTextFragmentList.getURLsByID(frag_id);
        if (urls.size() == 0)
        {
          throw new WollMuxFehlerException(
              "Die URL zum Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht bestimmt werden:",
              new ConfigurationErrorException(
                  "Das Textfragment mit der FRAG_ID '"
                      + frag_id
                      + "' ist nicht definiert!"));
        }

        // Nur die erste funktionierende URL verwenden. Dazu werden alle URL zu
        // dieser FRAG_ID geprüft und in die Variablen loadUrlStr und fragUrls
        // übernommen.
        String errors = "";
        boolean found = false;
        Iterator iterUrls = urls.iterator();
        while (iterUrls.hasNext() && !found)
        {
          urlStr = (String) iterUrls.next();

          // URL erzeugen und prüfen, ob sie aufgelöst werden kann
          URL url;
          try
          {
            url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
            urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
            url = new URL(urlStr);
            WollMuxSingleton.checkURL(url);
          }
          catch (MalformedURLException e)
          {
            Logger.log(e);
            errors += "Die URL '"
                      + urlStr
                      + "' ist ungültig:\n"
                      + e.getLocalizedMessage()
                      + "\n\n";
            continue;
          }
          catch (IOException e)
          {
            Logger.log(e);
            errors += e.getLocalizedMessage() + "\n\n";
            continue;
          }

          found = true;
        }

        if (!found)
        {
          throw new WollMuxFehlerException(
              "Das Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht aufgelöst werden:\n\n"
                  + errors);
        }

        // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
        // später öffnen) aufnehmen
        if (i == 0)
        {
          loadUrlStr = urlStr;
        }
        else
        {
          fragUrls[i - 1] = urlStr;
        }
      }

      // open document as Template (or as document):
      try
      {
        XComponent doc = UNO.loadComponentFromURL(loadUrlStr, asTemplate, true);

        if (UNO.XTextDocument(doc) != null)
        {
          TextDocumentModel model = mux.getTextDocumentModel(UNO
              .XTextDocument(doc));
          model.setFragUrls(fragUrls);
        }
      }
      catch (java.lang.Exception x)
      {
        // sollte eigentlich nicht auftreten, da bereits oben geprüft.
        throw new WollMuxFehlerException("Die Vorlage mit der URL '"
                                         + loadUrlStr
                                         + "' kann nicht geöffnet werden.", x);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + ((asTemplate) ? "asTemplate" : "asDocument")
             + ", "
             + fragIDs
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass alle registrierten
   * XPALChangeEventListener geupdated werden.
   */
  public static void handlePALChangedNotify()
  {
    handle(new OnPALChangedNotify());
  }

  /**
   * Dieses Event wird immer dann erzeugt, wenn ein Dialog zur Bearbeitung der
   * PAL geschlossen wurde und immer dann wenn die PAL z.B. durch einen
   * wollmux:setSender-Befehl geändert hat. Das Event sorgt dafür, dass alle im
   * WollMuxSingleton registrierten XPALChangeListener geupdatet werden.
   * 
   * @author christoph.lutz
   */
  private static class OnPALChangedNotify extends BasicEvent
  {
    protected void doit()
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
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum setzen des aktuellen Absenders.
   * 
   * @param senderName
   *          Name des Senders in der Form "Nachname, Vorname (Rolle)" wie sie
   *          auch der PALProvider bereithält.
   * @param idx
   *          der zum Sender senderName passende index in der sortierten
   *          Senderliste - dient zur Konsistenz-Prüfung, damit kein Sender
   *          gesetzt wird, wenn die PAL der setzenden Komponente nicht mit der
   *          PAL des WollMux übereinstimmt.
   */
  public static void handleSetSender(String senderName, int idx)
  {
    handle(new OnSetSender(senderName, idx));
  }

  /**
   * Dieses Event wird ausgelöst, wenn im WollMux-Service die methode setSender
   * aufgerufen wird. Es sort dafür, dass ein neuer Absender gesetzt wird.
   * 
   * @author christoph.lutz
   */
  private static class OnSetSender extends BasicEvent
  {
    private String senderName;

    private int idx;

    public OnSetSender(String senderName, int idx)
    {
      this.senderName = senderName;
      this.idx = idx;
    }

    protected void doit()
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
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + senderName
             + ", "
             + idx
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die Druckfunktion zum TextDokument doc
   * auf functionName setzt.
   * 
   * Kann derzeit über den XWollMux-Service aufgerufen werden und steht damit
   * als Teil der Komfortdruckfunktionen auch in externen Routinen (z.B.
   * Basic-Makros, ...) zur Verfügung.
   * 
   * @param doc
   *          Das TextDokument, zu dem die Druckfunktion gesetzt werden soll.
   * @param functionName
   *          der Name der Druckfunktion. Der Name muss ein gültiger
   *          Funktionsbezeichner sein und in einem Abschnitt "Druckfunktionen"
   *          in der wollmux.conf definiert sein.
   */
  public static void handleSetPrintFunction(XTextDocument doc,
      String functionName)
  {
    handle(new OnSetPrintFunction(doc, functionName));
  }

  private static class OnSetPrintFunction extends BasicEvent
  {
    private String functionName;

    private XTextDocument doc;

    public OnSetPrintFunction(XTextDocument doc, String functionName)
    {
      this.functionName = functionName;
      this.doc = doc;
    }

    protected void doit()
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      model.setPrintFunction(functionName);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", '"
             + functionName
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle
   * Formularfelder Dokument auf den neuen Wert gesetzt werden. Bei
   * Formularfeldern mit TRAFO-Funktion wird die Transformation entsprechend
   * durchgeführt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI der Wert des Formularfeldes fieldID geändert wurde und sorgt
   * dafür, dass die Wertänderung auf alle betroffenen Formularfelder im
   * Dokument doc übertragen werden.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          Die ID der Formularfelder, deren Werte angepasst werden sollen.
   * @param newValue
   *          Der neue untransformierte Wert des Formularfeldes.
   * @param funcLib
   *          Die Funktionsbibliothek, die zur Gewinnung der Trafo-Funktion
   *          verwendet werden soll.
   */
  public static void handleFormValueChanged(TextDocumentModel doc,
      String fieldId, String newValue, FunctionLibrary funcLib)
  {
    handle(new OnFormValueChanged(doc, fieldId, newValue, funcLib));
  }

  private static class OnFormValueChanged extends BasicEvent
  {
    private String fieldId;

    private String newValue;

    private FunctionLibrary funcLib;

    private TextDocumentModel doc;

    public OnFormValueChanged(TextDocumentModel doc, String fieldId,
        String newValue, FunctionLibrary funcLib)
    {
      this.fieldId = fieldId;
      this.newValue = newValue;
      this.funcLib = funcLib;
      this.doc = doc;
    }

    protected void doit()
    {
      doc.setFormFieldValue(fieldId, newValue);
      doc.updateFormFields(fieldId, funcLib);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + fieldId
             + "', '"
             + newValue
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle
   * Sichtbarkeitselemente (Dokumentkommandos oder Bereiche mit Namensanhang
   * 'GROUPS ...') im übergebenen Dokument, die einer bestimmten Gruppe groupId
   * zugehören ein- oder ausgeblendet werden.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI bestimmte Text-Teile des übergebenen Dokuments ein- oder
   * ausgeblendet werden sollen.
   * 
   * @param model
   *          Das TextDocumentModel, welches die Sichtbarkeitselemente enthält.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   */
  public static void handleSetVisibleState(TextDocumentModel model,
      String groupId, boolean visible)
  {
    handle(new OnSetVisibleState(model, groupId, visible));
  }

  private static class OnSetVisibleState extends BasicEvent
  {
    private TextDocumentModel model;

    private String groupId;

    private boolean visible;

    public OnSetVisibleState(TextDocumentModel model, String groupId,
        boolean visible)
    {
      this.model = model;
      this.groupId = groupId;
      this.visible = visible;
    }

    protected void doit()
    {
      // invisibleGroups anpassen:
      HashSet invisibleGroups = model.getInvisibleGroups();
      if (visible)
        invisibleGroups.remove(groupId);
      else
        invisibleGroups.add(groupId);

      VisibilityElement firstChangedElement = null;

      // Sichtbarkeitselemente durchlaufen und alle ggf. updaten:
      Iterator iter = model.visibleElementsIterator();
      while (iter.hasNext())
      {
        VisibilityElement visibleElement = (VisibilityElement) iter.next();
        Set groups = visibleElement.getGroups();
        if (!groups.contains(groupId)) continue;

        // Visibility-Status neu bestimmen:
        boolean setVisible = true;
        Iterator i = groups.iterator();
        while (i.hasNext())
        {
          String groupId = (String) i.next();
          if (invisibleGroups.contains(groupId)) setVisible = false;
        }

        // Element merken, dessen Sichtbarkeitsstatus sich zuerst ändert und
        // den focus (ViewCursor) auf den Start des Bereichs setzen. Da das
        // Setzen eines ViewCursors in einen unsichtbaren Bereich nicht
        // funktioniert, wird die Methode focusRangeStart zwei mal aufgerufen,
        // je nach dem, ob der Bereich vor oder nach dem Setzen des neuen
        // Sichtbarkeitsstatus sichtbar ist.
        if (setVisible != visibleElement.isVisible()
            && firstChangedElement == null)
        {
          firstChangedElement = visibleElement;
          if (firstChangedElement.isVisible()) focusRangeStart(visibleElement);
        }

        // neuen Sichtbarkeitsstatus setzen:
        try
        {
          visibleElement.setVisible(setVisible);
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle Löschen von Dokumentinhalten
        }
      }

      // Den Cursor (nochmal) auf den Anfang des Ankers des Elements setzen,
      // dessen Sichtbarkeitsstatus sich zuerst geändert hat (siehe Begründung
      // oben).
      if (firstChangedElement != null && firstChangedElement.isVisible())
        focusRangeStart(firstChangedElement);
    }

    /**
     * Diese Methode setzt den ViewCursor auf den Anfang des Ankers des
     * Sichtbarkeitselements.
     * 
     * @param visibleElement
     *          Das Sichtbarkeitselement, auf dessen Anfang des Ankers der
     *          ViewCursor gesetzt werden soll.
     */
    private void focusRangeStart(VisibilityElement visibleElement)
    {
      try
      {
        model.getViewCursor().gotoRange(
            visibleElement.getAnchor().getStart(),
            false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "('"
             + groupId
             + "', "
             + visible
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das den ViewCursor des Dokuments auf das aktuell in der
   * Formular-GUI bearbeitete Formularfeld setzt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt dafür,
   * dass der View-Cursor des Dokuments das entsprechende FormField im Dokument
   * anspringt.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen
   *          mehrere Formularfelder diese ID, so wird bevorzugt das erste
   *          Formularfeld aus dem Vektor genommen, das keine Trafo enthält.
   *          Ansonsten wird das erste Formularfeld im Vektor verwendet.
   */
  public static void handleFocusFormField(TextDocumentModel doc, String fieldId)
  {
    handle(new OnFocusFormField(doc, fieldId));
  }

  private static class OnFocusFormField extends BasicEvent
  {
    private TextDocumentModel doc;

    private String fieldId;

    public OnFocusFormField(TextDocumentModel doc, String fieldId)
    {
      this.doc = doc;
      this.fieldId = fieldId;
    }

    protected void doit()
    {
      doc.focusFormField(fieldId);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.doc
             + ", '"
             + fieldId
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Position und Größe des übergebenen
   * Dokument-Fensters auf die vorgegebenen Werte setzt. AHTUNG: Die Maßangaben
   * beziehen sich auf die linke obere Ecke des Fensterinhalts OHNE die
   * Titelzeile und die Fensterdekoration des Rahmens. Um die linke obere Ecke
   * des gesamten Fensters richtig zu setzen, müssen die Größenangaben des
   * Randes der Fensterdekoration und die Höhe der Titelzeile VOR dem Aufruf der
   * Methode entsprechend eingerechnet werden.
   * 
   * @param model
   *          Das XModel-Interface des Dokuments dessen Position/Größe gesetzt
   *          werden soll.
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position
   *          in Pixel, gezählt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird
   *          die Titelzeile des Rahmens nicht beachtet und muss vorher
   *          entsprechend eingerechnet werden.
   */
  public static void handleSetWindowPosSize(TextDocumentModel model, int docX,
      int docY, int docWidth, int docHeight)
  {
    handle(new OnSetWindowPosSize(model, docX, docY, docWidth, docHeight));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI die
   * Position und die Ausmasse des Dokuments verändert. Ruft direkt
   * setWindowsPosSize der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowPosSize extends BasicEvent
  {
    private TextDocumentModel model;

    private int docX, docY, docWidth, docHeight;

    public OnSetWindowPosSize(TextDocumentModel model, int docX, int docY,
        int docWidth, int docHeight)
    {
      this.model = model;
      this.docX = docX;
      this.docY = docY;
      this.docWidth = docWidth;
      this.docHeight = docHeight;
    }

    protected void doit()
    {
      model.setWindowPosSize(docX, docY, docWidth, docHeight);
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(model.doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + docX
             + ", "
             + docY
             + ", "
             + docWidth
             + ", "
             + docHeight
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Anzeige des übergebenen Dokuments auf sichtbar
   * oder unsichtbar schaltet. Dabei wird direkt die entsprechende Funktion der
   * UNO-API verwendet.
   * 
   * @param model
   *          Das XModel interface des dokuments, welches sichtbar oder
   *          unsichtbar geschaltet werden soll.
   * @param visible
   *          true, wenn das Dokument sichtbar geschaltet werden soll und false,
   *          wenn das Dokument unsichtbar geschaltet werden soll.
   */
  public static void handleSetWindowVisible(TextDocumentModel model,
      boolean visible)
  {
    handle(new OnSetWindowVisible(model, visible));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI das
   * bearbeitete Dokument sichtbar/unsichtbar schalten möchte. Ruft direkt
   * setVisible der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowVisible extends BasicEvent
  {
    private TextDocumentModel model;

    boolean visible;

    public OnSetWindowVisible(TextDocumentModel model, boolean visible)
    {
      this.model = model;
      this.visible = visible;
    }

    protected void doit()
    {
      model.setWindowVisible(visible);
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(model.doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + visible + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument schließt.
   * 
   * @param doc
   *          Das zu schließende XTextDocument.
   */
  public static void handleCloseTextDocument(TextDocumentModel model)
  {
    handle(new OnCloseTextDocument(model));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die
   * Formular-GUI schließt und damit auch das zugehörige TextDokument
   * geschlossen werden soll.
   * 
   * @author christoph.lutz
   */
  private static class OnCloseTextDocument extends BasicEvent
  {
    private TextDocumentModel model;

    public OnCloseTextDocument(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit()
    {
      model.close();
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(model.doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + model.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das ggf. notwendige interaktive
   * Initialisierungen vornimmt. Derzeit wird vor allem die Konsitenz der
   * persönlichen Absenderliste geprüft und der AbsenderAuswählen Dialog
   * gestartet, falls die Liste leer ist.
   */
  public static void handleInitialize()
  {
    handle(new OnInitialize());
  }

  /**
   * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des
   * WollMux im WollMuxSingleton erzeugt und übernimmt alle benutzersichtbaren
   * (interaktiven) Initialisierungen wie z.B. das Darstellen des
   * AbsenderAuswählen-Dialogs, falls die PAL leer ist.
   * 
   * @author christoph.lutz
   */
  private static class OnInitialize extends BasicEvent
  {
    protected void doit()
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
                           + "Natur ist, sollten Sie diese Datensätze aus "
                           + "ihrer Absenderliste löschen und neu hinzufügen!";
          WollMuxSingleton.showInfoModal("WollMux-Info", message);
        }
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

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren des übergebenen
   * XPALChangeEventListeners.
   * 
   * @param listener
   */
  public static void handleAddPALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnAddPALChangeEventListener(listener));
  }

  /**
   * Dieses Event wird ausgelöst, wenn sich ein externer PALChangeEventListener
   * beim WollMux-Service registriert. Es sorgt dafür, dass der
   * PALChangeEventListener in die Liste der registrierten
   * PALChangeEventListener im WollMuxSingleton aufgenommen wird.
   * 
   * @author christoph.lutz
   */
  private static class OnAddPALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    public OnAddPALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected void doit()
    {
      WollMuxSingleton.getInstance().addPALChangeEventListener(listener);

      WollMuxEventHandler.handlePALChangedNotify();
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(listener, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den übergebenen XPALChangeEventListener
   * deregistriert.
   * 
   * @param listener
   *          der zu deregistrierende XPALChangeEventListener
   */
  public static void handleRemovePALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnRemovePALChangeEventListener(listener));
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgelöst wenn sich
   * ein externe XPALChangeEventListener beim WollMux deregistriert. Der zu
   * entfernende XPALChangeEventListerner wird anschließend im WollMuxSingleton
   * aus der Liste der registrierten XPALChangeEventListener genommen.
   * 
   * @author christoph.lutz
   */
  private static class OnRemovePALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    public OnRemovePALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected void doit()
    {
      WollMuxSingleton.getInstance().removePALChangeEventListener(listener);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent das signaisiert, dass der Drucken-Knopf in
   * OOo gedrückt wurde und eine evtl. definierte Komfortdruckfunktion
   * ausgeführt werden soll.
   * 
   * Das Event wird ausgelöst, wenn der registrierte WollMuxDispatchInterceptor
   * eines Dokuments eine entsprechende Nachricht bekommt.
   */
  public static void handlePrintButtonPressed(XTextDocument doc,
      String fallbackDispatchUrlStr)
  {
    handle(new OnPrintButtonPressed(doc, fallbackDispatchUrlStr));
  }

  private static class OnPrintButtonPressed extends BasicEvent
  {
    private XTextDocument doc;

    private String fallbackDispatchUrlStr;

    public OnPrintButtonPressed(XTextDocument doc, String fallbackDispatchUrlStr)
    {
      this.doc = doc;
      this.fallbackDispatchUrlStr = fallbackDispatchUrlStr;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (doc == null) return;

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      // Ziffern anpassung der Sachleitenden Verfügungen durlaufen lassen, um zu
      // erkennen, wenn Verfügungspunkte manuell aus dem Dokument gelöscht
      // wurden ohne die entsprechenden Knöpfe zum Einfügen/Entfernen von
      // Ziffern zu drücken.
      SachleitendeVerfuegung.ziffernAnpassen(model);

      PrintFunction printFunc = null;

      String printFunctionName = model.getPrintFunctionName();
      if (printFunctionName != null && !printFunctionName.equals(""))
      {
        printFunc = WollMuxSingleton.getInstance().getGlobalPrintFunctions()
            .get(printFunctionName);

        if (printFunc != null)
        {
          XPrintModel pmod = WollMuxSingleton.getInstance()
              .getTextDocumentModel(doc).getPrintModel();

          printFunc.invoke(pmod);
        }
        else
        {
          Logger.error("Druckfunktion '"
                       + printFunctionName
                       + "' nicht definiert.");
        }
      }

      if (printFunc == null)
      {
        // Fallback zum Dispatch über die URL von fallbackDispatchUrlStr
        com.sun.star.util.URL url = UNO.getParsedUNOUrl(fallbackDispatchUrlStr);
        XDispatch disp = WollMuxSingleton.getDispatchForModel(
            UNO.XModel(doc),
            url);
        if (disp != null) disp.dispatch(url, new PropertyValue[] {});
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das Dokument doc in
   * der Anzahl numberOfCopies ausgedruckt werden soll. Nach Beendigung des
   * Events soll der CallBack des übergebenen ActionsListeners aufgerufen
   * werden.
   * 
   * Das Event dient als Hilfe für die Komfortdruckfunktionen und wird vom
   * XPrintModel aufgerufen und mit diesem synchronisiert.
   */
  public static void handlePrint(XTextDocument doc, short numberOfCopies,
      ActionListener listener)
  {
    handle(new OnPrint(doc, numberOfCopies, listener));
  }

  private static class OnPrint extends BasicEvent
  {
    private XTextDocument doc;

    private short numberOfCopies;

    private ActionListener listener;

    public OnPrint(XTextDocument doc, short numberOfCopies,
        ActionListener listener)
    {
      this.doc = doc;
      this.numberOfCopies = numberOfCopies;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);
      try
      {
        model.print(numberOfCopies);
      }
      catch (PrintFailedException e)
      {
        errorMessage(e);
      }
      listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine weitere Ziffer
   * der Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende Ziffer
   * gelöscht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:ZifferEinfuegen"
   * dispatch erfolgte.
   */
  public static void handleButtonZifferEinfuegenPressed(XTextDocument doc)
  {
    handle(new OnZifferEinfuegen(doc));
  }

  private static class OnZifferEinfuegen extends BasicEvent
  {
    private XTextDocument doc;

    public OnZifferEinfuegen(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (doc == null) return;

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertVerfuegungspunkt(
            model,
            viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Abdruckzeile
   * der Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende
   * Abdruckzeile gelöscht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:Abdruck" dispatch
   * erfolgte.
   */
  public static void handleButtonAbdruckPressed(XTextDocument doc)
  {
    handle(new OnAbdruck(doc));
  }

  private static class OnAbdruck extends BasicEvent
  {
    private XTextDocument doc;

    public OnAbdruck(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (doc == null) return;

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertAbdruck(model, viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Zuleitungszeile
   * der Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende
   * Zuleitungszeile gelöscht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:Zuleitungszeile"
   * dispatch erfolgte.
   */
  public static void handleButtonZuleitungszeilePressed(XTextDocument doc)
  {
    handle(new OnButtonZuleitungszeilePressed(doc));
  }

  private static class OnButtonZuleitungszeilePressed extends BasicEvent
  {
    private XTextDocument doc;

    public OnButtonZuleitungszeilePressed(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertZuleitungszeile(
            model,
            viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das über den Bereich des viewCursors im
   * Dokument doc ein neues Bookmark mit dem Namen "WM(CMD'<blockname>')" legt,
   * wenn nicht bereits ein solches Bookmark im markierten Block definiert ist.
   * Ist bereits ein Bookmark mit diesem Namen vorhanden, so wird dieses
   * gelöscht.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:markBlock#<blockname>"
   * dispatch erfolgte.
   * 
   * @param doc
   *          Das Textdokument, in dem der Block eingefügt werden soll.
   * @param blockname
   *          Derzeit werden folgende Blocknamen akzeptiert "draftOnly",
   *          "notInOriginal" und "all". Alle anderen Blocknamen werden
   *          ignoriert und keine Aktion ausgeführt.
   */
  public static void handleMarkBlock(XTextDocument doc, String blockname)
  {
    handle(new OnMarkBlock(doc, blockname));
  }

  private static class OnMarkBlock extends BasicEvent
  {
    private XTextDocument doc;

    private String blockname;

    public OnMarkBlock(XTextDocument doc, String blockname)
    {
      this.doc = doc;
      this.blockname = blockname;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (doc == null
          || UNO.XBookmarksSupplier(doc) == null
          || blockname == null) return;

      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      TextDocumentModel model = mux.getTextDocumentModel(doc);

      XTextCursor range = model.getViewCursor();

      if (range == null) return;
      if (!(blockname.equalsIgnoreCase("draftOnly")
            || blockname.equalsIgnoreCase("notInOriginal") || blockname
          .equalsIgnoreCase("allVersions"))) return;

      String bookmarkName = "WM(CMD '" + blockname + "')";

      Set bmNames = getBookmarkNamesStartingWith(bookmarkName, range);
      if (bmNames.size() > 0)
      {
        // bereits bestehende Blöcke löschen
        Iterator iter = bmNames.iterator();
        while (iter.hasNext())
        {
          bookmarkName = iter.next().toString();
          try
          {
            new Bookmark(bookmarkName, UNO.XBookmarksSupplier(doc)).remove();
          }
          catch (NoSuchElementException e)
          {
          }
        }
        WollMuxSingleton.showInfoModal(
            blockname + "-Block entfernen",
            "Der ausgewählte Block enthielt bereits eine Markierung als '"
                + blockname
                + "'-Block. Die bestehende Markierung wurde aufgehoben!");
      }
      else
      {
        // neuen Block anlegen
        new Bookmark(bookmarkName, doc, range);
        WollMuxSingleton.showInfoModal(
            "Block einfügen",
            "Der ausgewählte Block wurde als '"
                + blockname
                + "'-Block markiert.");
      }

      // PrintBlöcke neu einlesen:
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          mux);
      dci.scanDocumentSettings();
    }

    /**
     * Liefert die Namen aller Bookmarks, die in im Bereich range existieren und
     * (case insesitive) mit dem Namen bookmarkName anfangen.
     * 
     * @param bookmarkName
     * @param range
     */
    private static HashSet getBookmarkNamesStartingWith(String bookmarkName,
        XTextRange range)
    {
      // Hier findet eine iteration des über den XEnumerationAccess des ranges
      // statt. Man könnte statt dessen auch über range-compare mit den bereits
      // bestehenden Blöcken aus TextDocumentModel.get<blockname>Blocks()
      // vergleichen...
      bookmarkName = bookmarkName.toLowerCase();
      HashSet found = new HashSet();
      HashSet started = new HashSet();
      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      if (UNO.XEnumerationAccess(cursor) != null)
      {
        XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();
        while (xenum.hasMoreElements())
        {
          XEnumeration parEnum = null;
          try
          {
            parEnum = UNO.XEnumerationAccess(xenum.nextElement())
                .createEnumeration();
          }
          catch (java.lang.Exception e)
          {
          }

          while (parEnum != null && parEnum.hasMoreElements())
          {
            XTextContent textPortion = null;
            try
            {
              textPortion = UNO.XTextContent(parEnum.nextElement());
            }
            catch (java.lang.Exception e)
            {
            }
            XNamed bookmark = UNO.XNamed(UNO.getProperty(
                textPortion,
                "Bookmark"));
            String name = (bookmark != null) ? bookmark.getName() : "";

            if (name.toLowerCase().startsWith(bookmarkName))
            {
              boolean isStart = ((Boolean) UNO.getProperty(
                  textPortion,
                  "IsStart")).booleanValue();
              if (isStart)
                started.add(name);
              else if (started.contains(name)) found.add(name);
            }
          }
        }
      }
      return found;
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", '"
             + blockname
             + "', viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass eine Datei
   * wollmux.dump erzeugt wird, die viele für die Fehlersuche relevanten
   * Informationen enthält wie z.B. Versionsinfo, Inhalt der wollmux.conf,
   * cache.conf, StringRepräsentation der Konfiguration im Speicher und eine
   * Kopie der Log-Datei.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht
   * für Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleDumpInfo()
  {
    handle(new OnDumpInfo());
  }

  private static class OnDumpInfo extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      WollMuxFiles.dumpInfo();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das gesamte Office
   * (und damit auch der WollMux) OHNE Sicherheitsabfragen(!) beendet werden
   * soll.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht
   * für Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleKill()
  {
    handle(new OnKill());
  }

  private static class OnKill extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      if (UNO.desktop != null)
      {
        UNO.desktop.terminate();
      }
      else
      {
        System.exit(0);
      }
    }

    public String toString()

    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass bei Sachleitenden
   * Verfügungen der Verfügungspunkt mit der Nummer verfPunkt ausgedruck wird,
   * wobei alle darauffolgenden Verfügungspunkte ausgeblendet sind.
   * 
   * Das Event wird üblicherweise von einer XPrintModel Implementierung (z.B.
   * TextDocument.PrintModel) geworfen, wenn aus der Druckfunktion die
   * zugehörige Methode aufgerufen wurde.
   * 
   * @param doc
   *          Das Dokument, welches die Verfügungspunkte enthält.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verfügungspunktes, wobei alle
   *          folgenden Verfügungspunkte ausgeblendet werden.
   * @param numberOfCopies
   *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt werden
   *          soll.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Blöcke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verfügungspunktes I
   *          ausgeblendet und alle notInOriginal-Blöcke ebenso. Andernfalls
   *          sind Ziffer und notInOriginal-Blöcke eingeblendet.
   * @param unlockActionListener
   *          Der unlockActionListener wird nach Beendigung des Druckens
   *          aufgerufen, wenn er != null.
   */
  public static void handlePrintVerfuegungspunkt(XTextDocument doc,
      short verfPunkt, short numberOfCopies, boolean isDraft,
      boolean isOriginal, ActionListener unlockActionListener)
  {
    handle(new OnPrintVerfuegungspunkt(doc, verfPunkt, numberOfCopies, isDraft,
        isOriginal, unlockActionListener));
  }

  private static class OnPrintVerfuegungspunkt extends BasicEvent
  {
    private XTextDocument doc;

    private short verfPunkt;

    private short numberOfCopies;

    private boolean isDraft;

    private boolean isOriginal;

    private ActionListener listener;

    public OnPrintVerfuegungspunkt(XTextDocument doc, short verfPunkt,
        short numberOfCopies, boolean isDraft, boolean isOriginal,
        ActionListener listener)
    {
      this.doc = doc;
      this.verfPunkt = verfPunkt;
      this.numberOfCopies = numberOfCopies;
      this.isDraft = isDraft;
      this.isOriginal = isOriginal;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      try
      {
        SachleitendeVerfuegung.printVerfuegungspunkt(
            model,
            verfPunkt,
            numberOfCopies,
            isDraft,
            isOriginal);
      }
      catch (PrintFailedException e)
      {
        errorMessage(e);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      if (listener != null) listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", verfPunkt="
             + verfPunkt
             + ", copies="
             + numberOfCopies
             + ", isDraft="
             + isDraft
             + ", isOriginal="
             + isOriginal
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass der PrintSetupDialog
   * von OOo angezeigt wird, über den der aktuelle Drucker ausgewählt und
   * geändert werden kann.
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn die gleichnamige Methode dort aufgerufen
   * wird.
   * 
   * @param doc
   *          Das Dokument für das die Druckereinstellung gelten sollen.
   * @param onlyOnce
   *          Gibt an, dass der Dialog nur beim ersten Aufruf (aus Sicht eines
   *          Dokuments) der Methode angezeigt wird. Wurde bereits vor dem
   *          Aufruf ein PrintSetup-Dialog gestartet, so öffnet sich der Dialog
   *          nicht und die Methode endet ohne Aktion.
   * @param unlockActionListener
   *          Der unlockActionListener wird immer aufgerufen, wenn sich doit()
   *          fertig ist.
   */
  public static void handleShowPrinterSetupDialog(XTextDocument doc,
      boolean onlyOnce, ActionListener unlockActionListener)
  {
    handle(new OnShowPrinterSetup(doc, onlyOnce, unlockActionListener));
  }

  private static class OnShowPrinterSetup extends BasicEvent
  {
    private XTextDocument doc;

    private boolean onlyOnce;

    private ActionListener listener;

    public OnShowPrinterSetup(XTextDocument doc, boolean onlyOnce,
        ActionListener listener)
    {
      this.doc = doc;
      this.onlyOnce = onlyOnce;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      // dialog nicht anzeigen, wenn er bereits einmal angezeigt wurde und
      // onlyOnce gesetzt ist.
      if (model.printSettingsDone && onlyOnce)
      {
        if (listener != null) listener.actionPerformed(null);
        return;
      }

      // Dialog anzeigen:
      try
      {
        com.sun.star.util.URL url = UNO
            .getParsedUNOUrl(DispatchInterceptor.DISP_UNO_PRINTER_SETUP);
        XNotifyingDispatch disp = UNO.XNotifyingDispatch(WollMuxSingleton
            .getDispatchForModel(UNO.XModel(doc), url));

        if (disp != null)
        {
          setLock();
          disp.dispatchWithNotification(
              url,
              new PropertyValue[] {},
              unlockDispatchResultListener);
          waitForUnlock();
        }
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      // Leider gibt der Dialog keine verwertbare Statusinformation zurück, die
      // beschreibt ob der Dialog mit "Abbrechen" oder mit "OK" beendet wurde.
      // Dann würde ich printSettingsDone nur dann auf true sezten, wenn der
      // Dialog mit OK beendet wurde...
      model.printSettingsDone = true;

      if (listener != null) listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", onlyOnce="
             + onlyOnce
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc
   * das Formularfeld mit der ID id auf den Wert value gesetzt wird. Ist das
   * Dokument ein Formulardokument (also mit einer angezeigten FormGUI), so wird
   * die Änderung über die FormGUI vorgenommen, die zugleich dafür sorgt, dass
   * von id abhängige Formularfelder mit angepasst werden. Besitzt das Dokument
   * keine Formularbeschreibung, so wird der Wert direkt gesetzt, ohne
   * Äbhängigkeiten zu beachten. Nach der erfolgreichen Ausführung aller
   * notwendigen Anpassungen wird der unlockActionListener benachrichtigt.
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn dort die Methode setFormValue aufgerufen
   * wird.
   * 
   * @param doc
   *          Das Dokument, in dem das Formularfeld mit der ID id neu gesetzt
   *          werden soll.
   * @param id
   *          Die ID des Formularfeldes, dessen Wert verändert werden soll. Ist
   *          die FormGUI aktiv, so werden auch alle von id abhängigen
   *          Formularwerte neu gesetzt.
   * @param value
   *          Der neue Wert des Formularfeldes id
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle
   *          notwendigen Anpassungen durchgeführt wurden.
   */
  public static void handleSetFormValueViaPrintModel(XTextDocument doc,
      String id, String value, ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueViaPrintModel(doc, id, value, unlockActionListener));
  }

  private static class OnSetFormValueViaPrintModel extends BasicEvent
  {
    private XTextDocument doc;

    private String id;

    private String value;

    private final ActionListener listener;

    public OnSetFormValueViaPrintModel(XTextDocument doc, String id,
        String value, ActionListener listener)
    {
      this.doc = doc;
      this.id = id;
      this.value = value;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      FormGUI formGUI = model.getFormGUI();
      if (formGUI != null)
      {
        // Werte über die FormGUI setzen lassen (damit sind auch automatisch
        // alle Abhängigkeiten richtig aufgelöst)
        formGUI.getController().setValue(id, value, new ActionListener()
        {
          public void actionPerformed(ActionEvent arg0)
          {
            handleSetFormValueViaPrintModelFinished(listener);
          }
        });
      }
      else
      {
        // Werte selber setzen:
        model.setFormFieldValue(id, value);
        model.updateFormFields(id);
        if (listener != null) listener.actionPerformed(null);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", id='"
             + id
             + "', value='"
             + value
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * TODO: doku anpassen! Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass
   * im Textdokument doc das Formularfeld mit der ID id auf den Wert value
   * gesetzt wird. Ist das Dokument ein Formulardokument (also mit einer
   * angezeigten FormGUI), so wird die Änderung über die FormGUI vorgenommen,
   * die zugleich dafür sorgt, dass von id abhängige Formularfelder mit
   * angepasst werden. Besitzt das Dokument keine Formularbeschreibung, so wird
   * der Wert direkt gesetzt, ohne Äbhängigkeiten zu beachten. Nach der
   * erfolgreichen Ausführung aller notwendigen Anpassungen wird der
   * unlockActionListener benachrichtigt.
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn dort die Methode
   * collectNonWollMuxFormFields aufgerufen wird.
   * 
   * @param model
   *          Das TextDokumentModel, in dem nach Nicht-WollMux Formularfeldern
   *          gesucht werden soll.
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle
   *          notwendigen Anpassungen durchgeführt wurden.
   */
  public static void handleCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentModel model, ActionListener listener)
  {
    handle(new OnCollectNonWollMuxFormFieldsViaPrintModel(model, listener));
  }

  private static class OnCollectNonWollMuxFormFieldsViaPrintModel extends
      BasicEvent
  {
    private TextDocumentModel model;

    private ActionListener listener;

    public OnCollectNonWollMuxFormFieldsViaPrintModel(TextDocumentModel model,
        ActionListener listener)
    {
      this.model = model;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      model.collectNonWollMuxFormFields();

      listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + model.doc.hashCode()
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Dieses WollMuxEvent ist das Gegenstück zu handleSetFormValueViaPrintModel
   * und wird dann erzeugt, wenn nach einer Änderung eines Formularwertes -
   * gesteuert durch die FormGUI - alle abhängigen Formularwerte angepasst
   * wurden. In diesem Fall ist die einzige Aufgabe dieses Events, den
   * unlockActionListener zu informieren, den handleSetFormValueViaPrintModel()
   * nicht selbst informieren konnte.
   * 
   * Das Event wird aus der Implementierung vom
   * OnSetFormValueViaPrintModel.doit() erzeugt, wenn Feldänderungen über die
   * FormGUI laufen.
   * 
   * @param unlockActionListener
   *          Der zu informierende unlockActionListener.
   */
  public static void handleSetFormValueViaPrintModelFinished(
      ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueViaPrintModelFinished(unlockActionListener));
  }

  private static class OnSetFormValueViaPrintModelFinished extends BasicEvent
  {
    private ActionListener listener;

    public OnSetFormValueViaPrintModelFinished(
        ActionListener unlockActionListener)
    {
      this.listener = unlockActionListener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (listener != null) listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das ein Textbaustein über
   * den Textbaustein-Bezeichner direkt ins Dokument eingefügt wird. Mit
   * reprocess wird übergeben, wann die Dokumentenkommandos ausgewertet werden
   * soll. Mir reprocess = true sofort.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen z.B über Druck eines
   * Tastenkuerzels oder Druck auf den Knopf der OOo-Symbolleiste ein
   * "wollmux:TextbausteinEinfuegen" dispatch erfolgte.
   */
  public static void handleTextbausteinEinfuegen(XTextDocument doc,
      boolean reprocess)
  {
    handle(new OnTextbausteinEinfuegen(doc, reprocess));
  }

  private static class OnTextbausteinEinfuegen extends BasicEvent
  {
    private XTextDocument doc;

    private boolean reprocess;

    public OnTextbausteinEinfuegen(XTextDocument doc, boolean reprocess)
    {
      this.doc = doc;
      this.reprocess = reprocess;

    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc).getViewCursor();
      boolean atLeastOne = TextModule.createInsertFragFromIdentifier(
          doc,
          viewCursor,
          reprocess);

      if (reprocess)
      {
        if (atLeastOne)
        {
          handleReprocessTextDocument(doc);
        }
        else
        {
          WollMuxSingleton.showInfoModal(
              "WollMux-Fehler",
              "An der Einfügestelle konnte kein Textbaustein gefunden werden.");
        }
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das der nächste
   * Platzhalter ausgehend vom Cursor angesprungen wird * Das Event wird von
   * WollMux.dispatch(...) geworfen z.B über Druck eines Tastenkuerzels oder
   * Druck auf den Knopf der OOo-Symbolleiste ein
   * "wollmux:PlatzhalterAnspringen" dispatch erfolgte.
   */
  public static void handleJumpToPlaceholder(XTextDocument doc)
  {
    handle(new OnJumpToPlaceholder(doc));
  }

  private static class OnJumpToPlaceholder extends BasicEvent
  {
    private XTextDocument doc;

    public OnJumpToPlaceholder(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc).getViewCursor();

      try
      {
        TextModule.jumpPlaceholders(doc, viewCursor);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das die nächste Marke
   * 'setJumpMark' angesprungen werden soll. Wird im
   * DocumentCommandInterpreter.DocumentExpander.fillPlaceholders aufgerufen
   * wenn nach dem Einfügen von Textbausteine keine Einfügestelle vorhanden ist
   * aber eine Marke 'setJumpMark'
   */
  public static void handleJumpToMark(XTextDocument doc, boolean msg)
  {
    handle(new OnJumpToMark(doc, msg));
  }

  private static class OnJumpToMark extends BasicEvent
  {
    private XTextDocument doc;

    private boolean msg;

    public OnJumpToMark(XTextDocument doc, boolean msg)
    {
      this.doc = doc;
      this.msg = msg;
    }

    protected void doit() throws WollMuxFehlerException
    {

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor == null) return;

      DocumentCommand cmd = model.getFirstJumpMark();

      if (cmd != null)
      {
        try
        {
          viewCursor.gotoRange(cmd.getTextRange(), false);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        cmd.markDone(true);
        model.getDocumentCommandTree().update();
      }
      else
      {
        if (msg)
        {
          WollMuxSingleton.showInfoModal(
              "WollMux",
              "Kein Platzhalter und keine Marke 'setJumpMark' vorhanden!");
        }
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", viewCursor)";
    }
  }

  // *******************************************************************************************

  /**
   * TODO dokumentieren
   */
  public static void handleOpenPath(String path)
  {
    handle(new OnOpenPath(path));
  }

  private static class OnOpenPath extends BasicEvent
  {
    private String path;

    public OnOpenPath(String path)
    {
      this.path = path;
    }

    protected void doit() throws WollMuxFehlerException
    {
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + path + "')";
    }
  }

  // *******************************************************************************************
  // Globale Helper-Methoden

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
