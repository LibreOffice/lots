/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class simplifies the synchronization of different threads over one
 * ActionListener. The application usually takes place in the following steps:
 *
 * SyncActionListener s = new SyncActionListener();
 * calling a method that expects an action listener (..., s); EventObject result =
 * s.synchronize();
 *
 * It is ensured that s.synchronize() does not return until the
 * ActionListener was notified. The EventObject is returned,
 * was called with the listener's {@link #actionPerformed(ActionEvent)} .
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class SyncActionListener implements ActionListener
{
  /**
   * The lock flag over which synchronization is done.
   */
  private boolean[] lock = new boolean[] { true };

  /**
   * Contains the returned ActionEvent after synchronization has taken place
   */
  private ActionEvent result = null;

  /**
   * Returns only when the listener's {@link #actionPerformed(ActionEvent)}
   * was called and returns the EventObject that was transmitted.
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
