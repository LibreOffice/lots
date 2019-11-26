/*
 * Dateiname: SyncActionListener.java
 * Projekt  : WollMux
 * Funktion : Vereinfacht die Synchronisation verschiedener Threads mittels 
 *            ActionListener
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 24.09.2009 | LUT | Erstellung als SyncActionListener.java
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Diese Klasse vereinfacht die Synchronisation verschiedener Threads über einen
 * ActionListener. Die Anwendung erfolgt in der Regel in folgenden Schritten:
 * 
 * SyncActionListener s = new SyncActionListener();
 * aufrufEinerMethodeDieEinenActionListenerErwartet(..., s); EventObject result =
 * s.synchronize();
 * 
 * Es ist sicher gestellt, dass s.synchronize() erst zurück kehrt, wenn der
 * ActionListener benachrichtigt wurde. Dabei wird das EventObject zurück gegeben,
 * mit dem {@link #actionPerformed(ActionEvent)} des Listeners aufgerufen wurde.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class SyncActionListener implements ActionListener
{
  /**
   * Das lock-Flag über das die Synchronisierung erfolgt.
   */
  private boolean[] lock = new boolean[] { true };

  /**
   * Enthält nach erfolgter Syncronisierung das zurückgegebene ActionEvent
   */
  private ActionEvent result = null;

  /**
   * Kehrt erst zurück, wenn {@link #actionPerformed(ActionEvent)} des Listeners
   * aufgerufen wurde und liefert das dabei übermittelte EventObject zurück.
   */
  public ActionEvent synchronize()
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
    {
      Thread.currentThread().interrupt();
      // nothing to do
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent arg0)
  {
    result = arg0;
    synchronized (lock)
    {
      lock[0] = false;
      lock.notifyAll();
    }
  }
}
