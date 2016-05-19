/*
 * Dateiname: CoupledWindowController.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse steuert die Ankopplung von Fenstern an ein XTopWindow-Hauptfenster von OOo.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 22.01.2008 | LUT | Erstellung als CoupledWindowController
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XTopWindowListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Diese Klasse steuert die logische Ankopplung von eigentlich unabhängigen Fenstern
 * an ein XTopWindow-Hauptfenster von OOo. Die angekoppelten Fenster werden nur
 * sichtbar, wenn das Hauptfenster den Fokus erhält. Verliert das Hauptfenster oder
 * ein angeoppeltes Fenster den Fokus an ein Fenster, das nicht vom
 * CoupledWindowController überwacht wird, so werden alle angekoppelten Fenster
 * unsichtbar gestellt.
 * 
 * ACHTUNG: Die Windowmanager unter Windows und auf dem Basisclient verhalten sich
 * unterschiedlich und es werden unterschiedliche Ereignisse für gleiche Aktionen
 * generiert: z.B. erhält die Seriendruckleiste (das erste Beispiel eines
 * angekoppelten Fensters) auf dem Basisclient immer den Fokus, wenn
 * setCoupledWindowsVisible(true) aufgerufen wurde. Unter Windows wird das Fenster
 * zwar kurz aktiv, bekommt aber nicht den Fokus. Auch Situationen wie das Schließen
 * eines AWT-Fensters, das ein Parent-Window gesetzt hat, führen zu unterschiedlichen
 * Events auf dem Basisclient und unter Windows. Es ist auch nicht gewährleistet,
 * dass die Ereignisse Deaktivierung/Aktivierung beim Fensterwechsel immer in der
 * selben Reihenfolge eintreffen. Diese Klasse enthält einen Stand, der nach langer
 * Arbeit und Probiererei nun endlich in den meisten Fällen funktioniert. Wenn hier
 * etwas geändert werden muss, dann unbedingt unter Windows und auf dem Basisclient
 * ausführlich testen!!!
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class CoupledWindowController
{

  /**
   * Enthält alle mit addCoupledWindow registrierten angekoppelten Fenster. Dieses
   * Feld ist als ArrayList angelegt, damit gewährleistet ist, dass die Fenster immer
   * in der Reihenfolge der Registrierung sichtbar/unsichtbar geschalten werden (also
   * nicht in willkürlicher Reihenfolge, die ein HashSet mit sich bringen würde).
   */
  private ArrayList<CoupledWindow> coupledWindows = new ArrayList<CoupledWindow>();

  /**
   * Enthält eine Liste mit WeakReference-Objekten, die auf Unterfenster von
   * coupledWindows zeigen, die die verschiedenen CoupledWindowListener aufsammeln.
   */
  private ArrayList<WeakReference<Window>> collectedChildWindows =
    new ArrayList<WeakReference<Window>>();

  /**
   * Enthält den WindowStateWatcher, mit dem der Fensterstatus überwacht und die
   * notwendigen Aktionen angestossen werden.
   */
  private WindowStateWatcher windowState = new WindowStateWatcher();

  /**
   * Der WindowStateWatcher überwacht den Fensterstatus der angekoppelten Fenster und
   * stößt ggf. die notwendigen Aktionen an. Der WindowStateWatcher enthält die
   * Listener, mit denen das Hauptfenster und die angeoppelten Fenster überwacht
   * werden und weiss immer bescheid, welches (registrierte) Fenster aktuell den
   * Fokus besitzt. Er veranlasst entsprechende Aktionen, wenn das Hauptfenster den
   * Fokus bekommt oder die Anwendung den Fokus an ein fremdes Fenster abgibt.
   */
  private class WindowStateWatcher
  {
    /**
     * Wenn ein Fenster deaktiviert und nach DEACTIVATION_TIMEOUT Millisekunden kein
     * neues Fenster aktiviert wurde, dann werden alle angekoppelten Fenster auf
     * unsichtbar gestellt.
     */
    private static final int DEACTIVATION_TIMEOUT = 200;

    /**
     * Enthält das aktuell aktive Fenster oder null, wenn kein Fenster aktiv ist.
     */
    private XTopWindowOrAWTWindow[] activeWindow =
      new XTopWindowOrAWTWindow[] { null };

    /**
     * Enthält eine eindeutige Nummer, die dem zuletzt gestarteten Warte-Thread für
     * ein TimeoutEvent übergeben wurde.
     */
    private int lastValidTimeoutEvent = 0;

    /**
     * Nachdem alle angekoppelten Fenster unsichtbar gestellt wurden, wird dieses
     * Flag auf true gesetzt und gibt an, dass die Reaktivierung ausschließlich durch
     * des Hauptfenster veranlasst werden darf.
     */
    private boolean acceptMainWindowOnly = true;

    /**
     * Behandelt ein timeoutEvent, das beim Fokusverlust an ein fremdes Fenster
     * erzeugt wird. Ein Timeout-Event hat eine eindeutige Nummer, die mit
     * lastValideTimeoutEvent übereinstimmen muss, damit das Event gültig ist (dient
     * dazu, damit ein bereits laufender Warte-Thread nicht abgewürgt werden muss,
     * wenn bereits der nächste - dann gültige - Warte-Thread angestossen wurde). Ist
     * kein aktives Fenster vorhanden, so werden alle angekoppelten Fenster
     * unsichtbar gemacht.
     * 
     * @param nr
     *          eindeutige Nummer des timeout-Events, die mit lastValidTimeoutEvent
     *          übereinstimmen muss, damit das Event ausgeführt wird.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void timeoutEvent(int nr)
    {
      synchronized (activeWindow)
      {
        if (nr != lastValidTimeoutEvent)
        {
          Logger.debug2(L.m("ignoriere ungültiges timeout event"));
          return;
        }

        if (activeWindow[0] == null)
        {
          Logger.debug2(L.m("Timeout und kein aktives Fenster - stelle angekoppelte Fenster unsichtbar"));
          setCoupledWindowsVisible(false);
          acceptMainWindowOnly = true;
        }
        else
        {
          Logger.debug2(L.m("Timeout aber keine Aktion da Fenster #%1 aktiv.",
            Integer.valueOf(activeWindow[0].hashCode())));
        }
      }
    }

    /**
     * Registriert die Aktivierung eines Fensters, das durch den Schlüssel key
     * eindeutig beschrieben wird.
     * 
     * @param key
     *          der Schlüssel des derzeit aktiven Fensters, mit dem das Fenster
     *          später deaktiviert werden kann.
     * @param isMainWindow
     *          Das Hauptfenster hat eine Sonderrolle, da nur die Aktivierung des
     *          Hauptfenster das acceptMainWindow-Flag zurücksetzen darf. Über diesen
     *          Parameter kann angegeben werden, ob das Fenster als Hauptfenster
     *          interpretiert gewertet werden soll.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void activationEvent(XTopWindowOrAWTWindow key, boolean isMainWindow)
    {
      if (key == null) return;

      if (acceptMainWindowOnly && !isMainWindow)
      {
        Logger.debug2(L.m("Aktivierung ignoriert da Fenster #%1 kein Hauptfenster.",
          Integer.valueOf(key.hashCode())));
        return;
      }

      synchronized (activeWindow)
      {
        XTopWindowOrAWTWindow lastActiveWindow = activeWindow[0];
        activeWindow[0] = key;
        if (lastActiveWindow == null)
        {
          setCoupledWindowsVisible(true);
          acceptMainWindowOnly = false;
        }

        Logger.debug2(L.m("Aktivierung von Fenster #%1",
          Integer.valueOf(activeWindow[0].hashCode())));
      }
    }

    /**
     * Registriert die Deaktivierung eines Fensters, das durch den Schlüssel key
     * eindeutig beschrieben ist. Key muss dabei identisch mit dem Schlüssel sein,
     * mit dem das Fenster aktiviert wurde, sonst wird auch nichts unternommen. Eine
     * bestimmte Zeit nach dem Deaktivieren des Fensters wird ein Timeout-Event
     * abgesetzt.
     * 
     * @param key
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void deactivationEvent(XTopWindowOrAWTWindow key)
    {
      if (key == null) return;
      synchronized (activeWindow)
      {
        if (key.equals(activeWindow[0]))
        {
          Logger.debug2(L.m("Deaktivierung von Fenster #%1",
            Integer.valueOf(key.hashCode())));
          activeWindow[0] = null;
          startWaitForTimeout();
        }
        else
        {
          Logger.debug2(L.m("Deaktierung ignoriert, da Fenster #%1 nicht aktiv.",
            Integer.valueOf(key.hashCode())));
        }
      }
    }

    /**
     * Erzeugt nach einer bestimmten Zeit ein Timeout-Event über das die
     * angekoppelten Fenster unsichtbar geschalten werden, wenn bei Ausführung des
     * Timeout-Events kein Fenster aktiv ist.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void startWaitForTimeout()
    {
      final int nr = ++lastValidTimeoutEvent;
      Thread t = new Thread()
      {
        public void run()
        {
          try
          {
            Thread.sleep(DEACTIVATION_TIMEOUT);
          }
          catch (InterruptedException e)
          {}
          timeoutEvent(nr);
        }
      };
      t.setDaemon(true);
      t.start();
    }

    /**
     * Enthält den WindowListener der das Hauptfenster überwacht.
     */
    private final XTopWindowListener topWindowListener = new XTopWindowListener()
    {
      public void windowDeactivated(EventObject arg0)
      {
        windowState.deactivationEvent(new XTopWindowOrAWTWindow(
          UNO.XTopWindow(arg0.Source)));
      }

      public void windowActivated(EventObject arg0)
      {
        windowState.activationEvent(new XTopWindowOrAWTWindow(
          UNO.XTopWindow(arg0.Source)), true);
      }

      public void windowNormalized(EventObject arg0)
      {
      // nicht relevant
      }

      public void windowMinimized(EventObject arg0)
      {
        synchronized (activeWindow)
        {
          activeWindow[0] = null;
          acceptMainWindowOnly = true;
        }
        setCoupledWindowsVisible(false);
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
     * Enthält den WindowListener mit dem angekoppelte Fenster überwacht werden
     */
    private final WindowListener coupledWindowListener = new WindowListener()
    {
      public void windowActivated(WindowEvent e)
      {
        windowState.activationEvent(
          new XTopWindowOrAWTWindow((Window) e.getSource()), false);
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
        // Wird der Fokus an ein Unterfenster eines registrierten Fensters
        // abgegeben, so wird dieses Fenster aufgesammelt und ein
        // coupledWindowListener registriert, über den Statusänderungen dieses
        // Fensters mitverfolgt werden können. Die Zugehörigkeit des
        // Unterfensters zum Parent wird über eine Rückwärtssuche in der
        // Owner-Hierarchie des OppositeWindows festgestellt.
        Window w = e.getOppositeWindow();
        while (w != null)
        {
          for (Iterator<CoupledWindow> iter = coupledWindows.iterator(); iter.hasNext();)
          {
            CoupledWindow win = iter.next();
            if (win.isSameWindow(w))
            {
              collectChildWindow(e.getOppositeWindow());
              return;
            }
          }
          w = w.getOwner();
        }

        // Deaktivierungsevent weiterreichen
        windowState.deactivationEvent(new XTopWindowOrAWTWindow(
          (Window) e.getSource()));
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

    /**
     * Fügt eine WeakReference auf das Unterfenster childWindow zu
     * collectedChildWindows hinzu und registriert einen coupledWindowListener auf
     * childWindow, aber nur dann, wenn das Fenster nicht bereits registriert ist.
     * Bei der Prüfung, ob das Fenster bereits registriert ist, werden alle
     * WeakReference-Objekte aus der Liste collectedChildWindows gelöscht, deren
     * referenzierte Objekte nicht mehr existent sind.
     * 
     * @param childWindow
     *          das aufzusammelnde Kindfenster
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void collectChildWindow(Window childWindow)
    {
      for (Iterator<WeakReference<Window>> iter = collectedChildWindows.iterator(); iter.hasNext();)
      {
        WeakReference<Window> ref = iter.next();
        Window win = ref.get();
        if (win != null)
        {
          if (win.equals(childWindow)) return;
        }
        else
        {
          iter.remove();
        }
      }

      Logger.debug(L.m("Registriere Kindfenster #%1",
        Integer.valueOf(childWindow.hashCode())));
      collectedChildWindows.add(new WeakReference<Window>(childWindow));
      childWindow.addWindowListener(coupledWindowListener);
    }
  }

  /**
   * Koppelt das AWT-Window window an das Hauptfenster an. Die Methode muss
   * aufgerufen werden, solange das Fenster window unsichtbar und nicht aktiv ist
   * (also z.B. vor dem Aufruf von window.setVisible(true)).
   * 
   * @param window
   *          das Fenster, das an das Hauptfenster angekoppelt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void addCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toAdd = new CoupledAWTWindow(window);
    Logger.debug2("addCoupledWindow #" + toAdd.hashCode());
    toAdd.addWindowListener(windowState.coupledWindowListener);
    coupledWindows.add(toAdd);
  }

  /**
   * Löst die Bindung eines angekoppelten Fensters window an das Hauptfenster.
   * 
   * @param window
   *          das Fenster, dessen Bindung zum Hauptfenster gelöst werden soll. Ist
   *          das Fenster nicht angekoppelt, dann passiert nichts.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void removeCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toRemove = new CoupledAWTWindow(window);
    Logger.debug2("removeCoupledWindow #" + toRemove.hashCode());
    for (Iterator<CoupledWindow> iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow w = iter.next();
      if (w.equals(toRemove))
      {
        iter.remove();
        toRemove.removeWindowListener(windowState.coupledWindowListener);
      }
    }
    windowState.deactivationEvent(new XTopWindowOrAWTWindow(window));
  }

  /**
   * Diese Methode macht alle angekoppelten Fenster sichtbar oder unsichtbar.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void setCoupledWindowsVisible(boolean visible)
  {
    for (Iterator<CoupledWindow> iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow win = iter.next();
      win.setVisible(visible);
    }
  }

  /**
   * Registriert ein Hauptfenster in diesem CoupledWindowController und sollte immer
   * vor der Benutzung des Controllers aufgerufen werden.
   * 
   * @param w
   *          Das XTopWindow welches das entsprechende Hauptfenster ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.addTopWindowListener(windowState.topWindowListener);
    windowState.activationEvent(new XTopWindowOrAWTWindow(w), true);
  }

  /**
   * Deregistriert ein Hauptfenster und sollte aufgerufen werden, wenn der Controller
   * nicht mehr benötigt wird und aufgeräumt werden kann.
   * 
   * @param w
   *          Das XTopWindow welches früher als Hauptfenster diente.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void unsetTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.removeTopWindowListener(windowState.topWindowListener);
    windowState.deactivationEvent(new XTopWindowOrAWTWindow(w));
  }

  /**
   * Gibt an, ob in diesem CoupledWindowController angekoppelte Fenster registriert
   * wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean hasCoupledWindows()
  {
    return coupledWindows.size() > 0;
  }

  /**
   * Beschreibt ein beliebiges ankoppelbares Fenster.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private interface CoupledWindow
  {
    /**
     * Setzt das angekoppelte Fenster auf sichtbar oder unsichtbar und kann dabei
     * auch den Fokus erhalten (Das ist eine unschöner Nebeneffekt der AWT-Methode
     * setVisible(...), der hier berücksichtigt ist). Ändert visible nicht den
     * Sichtbarkeitsstatus des Fenster, so hat diese Methode keine Auswirkung.
     * 
     * @param visible
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void setVisible(boolean visible);

    /**
     * Registriert auf dem angekoppelten Fenster einen WindowListener über den das
     * Hauptfenster mitkriegen kann, dass ein angekoppeltes Fenster den Fokus an ein
     * fremdes, nicht angekoppeles, Fenster verloren hat und somit auch alle
     * angekoppelten Fenster unsichtbar gestellt werden sollen.
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

    /**
     * Liefert true gdw this gecoupled ist mit w.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public boolean isSameWindow(Window w);
  }

  /**
   * Diese Klasse repräsentiert ein CoupledWindow-Objekt, dem ein
   * java.awt.Window-Objekt zugrundeliegt und implementiert die Methoden hashCode()
   * und equals() damit das Objekt sinnvoll verglichen und in einer HashMap verwaltet
   * werden kann.
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
            {}
          }
        });
      }
      catch (Exception x)
      {}
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
            {}
          }
        });
      }
      catch (Exception x)
      {}
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
            {}
          }
        });
      }
      catch (Exception x)
      {}
    }

    public int hashCode()
    {
      return window.hashCode();
    }

    public boolean isSameWindow(Window w)
    {
      return this.window.equals(w);
    }

    public boolean equals(Object o)
    {
      try
      {
        CoupledAWTWindow w = (CoupledAWTWindow) o;
        return (w != null && window.equals(w.window));
      }
      catch (ClassCastException x)
      {}

      return false;
    }
  }

  private static class XTopWindowOrAWTWindow
  {
    private Object window;

    private int hash;

    public XTopWindowOrAWTWindow(Window win)
    {
      if (win == null)
      {
        Logger.error(L.m("Großes Problem"));
        throw new NullPointerException();
      }
      this.window = win;
      this.hash = win.hashCode();
    }

    public XTopWindowOrAWTWindow(XTopWindow win)
    {
      this.window = UNO.XInterface(win);
      if (window == null)
      {
        Logger.error(L.m("Großes Problem"));
        throw new NullPointerException();
      }
      this.hash = UnoRuntime.generateOid(win).hashCode();
    }

    public int hashCode()
    {
      return hash;
    }

    public boolean equals(Object o)
    {
      if (o == null) return false;
      try
      {
        XTopWindowOrAWTWindow win2 = (XTopWindowOrAWTWindow) o;
        if (window instanceof XInterface)
          return UnoRuntime.areSame(window, win2.window);
        else
          return window.equals(win2.window);
      }
      catch (ClassCastException x)
      {
        return false;
      }
    }
  }
}
