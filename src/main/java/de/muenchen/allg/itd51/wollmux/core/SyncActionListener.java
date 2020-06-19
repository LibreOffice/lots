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
