/*
 * TODO Dateiname: Bookmark.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repräsentiert ein Bookmark in OOo und bietet Methoden
 *            für den vereinfachten Zugriff und die Manipulation von Bookmarks an.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 17.05.2006 | LUT | Dokumentation ergänzt
 * 07.08.2006 | BNK | +Bookmark(XNamed bookmark, XTextDocument doc)
 * 29.09.2006 | BNK | rename() gibt nun im Fehlerfall das BROKEN-String-Objekt zurück
 * 29.09.2006 | BNK | Unnötige renames vermeiden, um OOo nicht zu stressen
 * 29.09.2006 | BNK | Auch im optimierten Fall wo kein rename stattfindet auf BROKEN testen
 * 20.10.2006 | BNK | rename() Debug-Meldung nicht mehr ausgeben, wenn No Op Optimierung triggert.
 * 31.10.2006 | BNK | +select() zum Setzen des ViewCursors
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XTopWindowListener;
import com.sun.star.lang.EventObject;

/**
 * TODO: comment
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class CoupledWindowController
{

  /**
   * Enthält alle mit addCoupledWindow registrierten an dieses Dokument
   * gekoppelten Fenster. Dieses Feld ist als ArrayList angelegt, damit
   * gewährleistet ist, dass die Fenster immer in der Reihenfolge der
   * Registrierung sichtbar/unsichtbar geschalten werden (also nicht in
   * willkürlicher Reihenfolge, die ein HashSet mit sich bringen würde).
   */
  private ArrayList coupledWindows = new ArrayList();

  /**
   * TODO: dok
   */
  private WindowStateWatcher windowState = new WindowStateWatcher();

  /**
   * TODO: comment TextDocumentModel
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private class WindowStateWatcher
  {
    private HashSet activeWindows = new HashSet();

    private int lastValidTimeoutEvent = 0;

    public void timeoutEvent(int nr)
    {
      synchronized (activeWindows)
      {
        if (nr != lastValidTimeoutEvent)
        {
          Logger.debug2("ignoriere ungültiges timeout event");
          return;
        }
        Logger.debug2(activeWindows.size()
                      + " aktive Fenster nach Timeout-Event");

        if (activeWindows.size() == 0)
        {
          setCoupledWindowsVisible(false);
        }
      }
    }

    public void activationEvent(int hashCode)
    {
      synchronized (activeWindows)
      {
        // Eigentlich kann zu einem Zeitpunkt immer nur ein Fenster aktiv sein.
        // Unter Windows wird diese Anforderung aber nicht immer eingehalten.
        // Gerade beim Umschalten zwischen zwei Fenstern kann es durch zeitliche
        // Schwankungen dazu kommen, dass das Activation-Event des neuen
        // Fensters vor der Deaktivierung des alten Fensters erfolgt. Durch den
        // Einsatz des HashSets und durch activeWindows.clear() kann das
        // gewuenschte Verhalten hier aber nachgebildet werden.
        activeWindows.clear();
        activeWindows.add(new Integer(hashCode));
        setCoupledWindowsVisible(true);

        Logger.debug2(activeWindows.size()
                      + " aktive Fenster nach Aktivierung von #"
                      + hashCode);
      }
    }

    public void deactivationEvent(int hashCode)
    {
      synchronized (activeWindows)
      {
        activeWindows.remove(new Integer(hashCode));
        startWaitForTimeout();

        Logger.debug2(activeWindows.size()
                      + " aktive Fenster nach Deaktivierung von #"
                      + hashCode);
      }
    }

    private void startWaitForTimeout()
    {
      final int nr = ++lastValidTimeoutEvent;
      Thread t = new Thread()
      {
        public void run()
        {
          try
          {
            Thread.sleep(200);
          }
          catch (InterruptedException e)
          {
          }
          timeoutEvent(nr);
        }
      };
      t.setDaemon(true);
      t.start();
    }

    /**
     * Enthält den WindowListener, über den festgestellt wird, ob das zu dieser
     * Serienbriefleiste gehörende Textdokument derzeit aktiv ist (also den
     * Fokus besitzt) oder nicht.
     */
    private final XTopWindowListener topWindowListener = new XTopWindowListener()
    {
      public void windowDeactivated(EventObject arg0)
      {
        windowState.deactivationEvent(arg0.Source.hashCode());
      }

      public void windowActivated(EventObject arg0)
      {
        windowState.activationEvent(arg0.Source.hashCode());
      }

      public void windowNormalized(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowMinimized(EventObject arg0)
      {
        setCoupledWindowsVisible(false);
        synchronized (activeWindows)
        {
          activeWindows.clear();
        }
      }

      public void windowClosed(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowClosing(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowOpened(EventObject arg0)
      {
        // nicht relevant
      }

      public void disposing(EventObject arg0)
      {
        // nicht relevant
      }
    };

    /**
     * TODO: dok
     */
    private final WindowListener coupledWindowListener = new WindowListener()
    {
      public void windowActivated(WindowEvent e)
      {
        windowState.activationEvent(e.getSource().hashCode());
      }

      public void windowClosed(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowClosing(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowDeactivated(WindowEvent e)
      {
        windowState.deactivationEvent(e.getSource().hashCode());
      }

      public void windowDeiconified(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowIconified(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowOpened(WindowEvent e)
      {
        // nicht relevant
      }
    };
  }

  /**
   * TODO: comment TextDocumentModel.addCoupledWindow
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TODO: TESTEN
   */
  public void addCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toAdd = new CoupledAWTWindow(window);
    Logger.debug2("addCoupledWindow + #" + toAdd.hashCode());
    toAdd.addWindowListener(windowState.coupledWindowListener);
    coupledWindows.add(toAdd);
  }

  /**
   * TODO: comment TextDocumentModel.removeCoupledWindow
   * 
   * muss aufgerufen werden, nachdem das Fenster auf unsichtbar gestellt wurde
   * (sonst kommt der Zähler durcheinander, der die aktiven Fenster überwacht).
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TODO: TESTEN
   */
  public void removeCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toRemove = new CoupledAWTWindow(window);
    Logger.debug2("removeCoupledWindow + #" + toRemove.hashCode());
    for (Iterator iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow w = (CoupledWindow) iter.next();
      if (w.equals(toRemove))
      {
        iter.remove();
        toRemove.removeWindowListener(windowState.coupledWindowListener);
      }
    }
    windowState.deactivationEvent(window.hashCode());
  }

  /**
   * TODO: comment TextDocumentModel.setCoupledWindowsVisisble
   * 
   * Diese Methode muss aufgerufen werden, wenn das Fenster noch nicht sichtbar
   * ist.
   * 
   * @param visible
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void setCoupledWindowsVisible(boolean visible)
  {
    for (Iterator iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow win = (CoupledWindow) iter.next();
      win.setVisible(visible);
    }
  }

  /**
   * TODO: comment CoupledWindowController.setTopWindow
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.addTopWindowListener(windowState.topWindowListener);
    windowState.activationEvent(w.hashCode());
  }

  /**
   * TODO: comment CoupledWindowController.unsetTopWindow
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void unsetTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.removeTopWindowListener(windowState.topWindowListener);
    windowState.deactivationEvent(w.hashCode());
  }

  /**
   * TODO: comment CoupledWindowController.hasCoupledWindows
   * 
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean hasCoupledWindows()
  {
    return coupledWindows.size() > 0;
  }

  /**
   * TODO: comment TextDocumentModel
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public interface CoupledWindow
  {
    /**
     * Setzt das angekoppelte Fenster auf sichtbar oder unsichtbar und kann
     * dabei auch den Fokus erhalten (Das ist eine unschöner Nebeneffekt der
     * AWT-Methode setVisible(...), der hier berücksichtigt ist). Ändert visible
     * nicht den Sichtbarkeitsstatus des Fenster, so hat diese Methode keine
     * Auswirkung.
     * 
     * @param visible
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void setVisible(boolean visible);

    /**
     * Registriert auf dem angekoppelten Fenster einen WindowListener über den
     * das Hauptfenster mitkriegen kann, dass ein angekoppeltes Fenster den
     * Fokus an ein fremdes, nicht angekoppeles, Fenster verloren hat und somit
     * auch alle angekoppelten Fenster unsichtbar gestellt werden sollen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @param listener
     */
    public void addWindowListener(WindowListener listener);

    /**
     * Entfernt einen mit addFocusListener(...) registrierten WindowListener vom
     * angkoppelten Fenster.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @param listener
     */
    public void removeWindowListener(WindowListener listener);
  }

  /**
   * Diese Klasse repräsentiert ein CoupledWindow-Objekt, dem ein
   * java.awt.Window-Objekt zugrundeliegt und implementiert die Methoden
   * hashCode() und equals() damit das Objekt sinnvoll verwaltet werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class CoupledAWTWindow implements CoupledWindow
  {
    private Window window;

    public CoupledAWTWindow(Window window)
    {
      this.window = window;
    }

    public void setVisible(final boolean visible)
    {
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            try
            {
              if (window.isVisible() != visible) window.setVisible(visible);
            }
            catch (Exception x)
            {
            }
          }
        });
      }
      catch (Exception x)
      {
      }
    }

    public void addWindowListener(final WindowListener l)
    {
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            try
            {
              window.addWindowListener(l);
            }
            catch (Exception x)
            {
            }
          }
        });
      }
      catch (Exception x)
      {
      }
    }

    public void removeWindowListener(final WindowListener l)
    {
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            try
            {
              window.removeWindowListener(l);
            }
            catch (Exception x)
            {
            }
          }
        });
      }
      catch (Exception x)
      {
      }
    }

    public int hashCode()
    {
      return window.hashCode();
    }

    public boolean equals(Object o)
    {
      if (o instanceof CoupledAWTWindow)
      {
        CoupledAWTWindow w = (CoupledAWTWindow) o;
        return window.equals(w.window);
      }
      else
        return false;
    }
  }
}
