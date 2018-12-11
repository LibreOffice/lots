package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.uno.RuntimeException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Dient als Basisklasse für konkrete Event-Implementierungen.
 */
public abstract class BasicEvent implements WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory
		      .getLogger(BasicEvent.class);
  private boolean[] lock = new boolean[] { true };

  /**
   * Diese Method ist für die Ausführung des Events zuständig. Nach der Bearbeitung
   * entscheidet der Rückgabewert ob unmittelbar die Bearbeitung des nächsten
   * Events gestartet werden soll oder ob das GUI blockiert werden soll bis das
   * nächste actionPerformed-Event beim EventProcessor eintrifft.
   */
  @Override
  public void process()
  {
    LOGGER.debug("Process WollMuxEvent {}", this);
    try
    {
      doit();
    }
    catch (WollMuxFehlerException e)
    {
      // hier wäre ein showNoConfigInfo möglich - ist aber nicht eindeutig auf no config zurückzuführen
      errorMessage(e);
    }
    // Notnagel für alle Runtime-Exceptions.
    catch (Throwable t)
    {
      LOGGER.error("", t);
    }
  }

  /**
   * Logged die übergebene Fehlermeldung nach Logger.error() und erzeugt ein
   * Dialogfenster mit der Fehlernachricht.
   */
  protected void errorMessage(Throwable t)
  {
    LOGGER.error("", t);
    String msg = "";
    if (t.getMessage() != null) msg += t.getMessage();
    Throwable c = t.getCause();
    /*
     * Bei RuntimeExceptions keine Benutzersichtbare Meldung, weil
     *
     * 1. der Benutzer damit eh nix anfangen kann
     *
     * 2. dies typischerweise passiert, wenn der Benutzer das Dokument geschlossen
     * hat, bevor der WollMux fertig war. In diesem Fall will er nicht mit einer
     * Meldung belästigt werden.
     */
    if (c instanceof RuntimeException) return;

    if (c != null)
    {
      msg += "\n\n" + c;
    }
    ModalDialogs.showInfoModal(L.m("WollMux-Fehler"), msg);
  }

  /**
   * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in der die
   * eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle auftretenden
   * Exceptions selbst behandeln, Fehler die jedoch benutzersichtbar in einem
   * Dialog angezeigt werden sollen, können über eine WollMuxFehlerException nach
   * oben weitergereicht werden.
   */
  protected abstract void doit() throws WollMuxFehlerException;

  /**
   * Diese Methode kann am Ende einer doit()-Methode aufgerufen werden und versucht
   * die Absturzwahrscheinlichkeit von OOo/WollMux zu senken in dem es den
   * GarbageCollector der JavaVM triggert freien Speicher freizugeben. Durch
   * derartige Aufräumaktionen insbesondere nach der Bearbeitung von Events, die
   * viel mit Dokumenten/Cursorn/Uno-Objekten interagieren, wird die Stabilität des
   * WollMux spürbar gesteigert.
   *
   * In der Vergangenheit gab es z.B. sporadische, nicht immer reproduzierbare
   * Abstürze von OOo, die vermutlich in einem fehlerhaften Speichermanagement in
   * der schwer zu durchschauenden Kette JVM->UNO-Proxies->OOo begründet waren.
   */
  protected void stabilize()
  {
    System.gc();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName();
  }

  /**
   * Setzt den Enable-Status aller OOo-Fenster, die der desktop aktuell liefert auf
   * enabled. Über den Status kann gesteuert werden, ob das Fenster
   * Benutzerinteraktionen wie z.B. Mausklicks auf Menüpunkte oder Tastendrücke
   * verarbeitet. Die Verarbeitung findet nicht statt, wenn enabled==false gesetzt
   * ist, ansonsten schon.
   *
   * @param enabled
   */
  static void enableAllOOoWindows(boolean enabled)
  {
    try
    {
      XFrames frames = UNO.XFramesSupplier(UNO.desktop).getFrames();
      for (int i = 0; i < frames.getCount(); i++)
      {
        try
        {
          XFrame frame = UNO.XFrame(frames.getByIndex(i));
          XWindow contWin = frame.getContainerWindow();
          if (contWin != null) contWin.setEnable(enabled);
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Setzt einen lock, der in Verbindung mit setUnlock und der
   * waitForUnlock-Methode verwendet werden kann, um quasi Modalität für nicht
   * modale Dialoge zu realisieren und setzt alle OOo-Fenster auf enabled==false.
   * setLock() sollte stets vor dem Aufruf des nicht modalen Dialogs erfolgen, nach
   * dem Aufruf des nicht modalen Dialogs folgt der Aufruf der
   * waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei der Beendigung
   * ein ActionEvent, das dafür sorgt, dass setUnlock aufgerufen wird.
   */
  protected void setLock()
  {
    enableAllOOoWindows(false);
    synchronized (lock)
    {
      lock[0] = true;
    }
  }

  /**
   * Macht einen mit setLock() gesetzten Lock rückgängig, und setzt alle
   * OOo-Fenster auf enabled==true und bricht damit eine evtl. wartende
   * waitForUnlock()-Methode ab.
   */
  protected void setUnlock()
  {
    synchronized (lock)
    {
      lock[0] = false;
      lock.notifyAll();
    }
    enableAllOOoWindows(true);
  }

  /**
   * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der Methode
   * setUnlock() aufgehoben wird. So kann die quasi Modalität nicht modale Dialoge
   * zu realisiert werden. setLock() sollte stets vor dem Aufruf des nicht modalen
   * Dialogs erfolgen, nach dem Aufruf des nicht modalen Dialogs folgt der Aufruf
   * der waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei der
   * Beendigung ein ActionEvent, das dafür sorgt, dass setUnlock aufgerufen wird.
   */
  protected void waitForUnlock()
  {
    try
    {
      synchronized (lock)
      {
        while (lock[0])
          lock.wait();
      }
    }
    catch (InterruptedException e)
    {}
  }

  /**
   * Dieser ActionListener kann nicht modalen Dialogen übergeben werden und sorgt
   * in Verbindung mit den Methoden setLock() und waitForUnlock() dafür, dass quasi
   * modale Dialoge realisiert werden können.
   */
  protected UnlockActionListener unlockActionListener = new UnlockActionListener();

  protected class UnlockActionListener implements ActionListener
  {
    public ActionEvent actionEvent = null;

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      actionEvent = arg0;
      setUnlock();
    }
  }
}