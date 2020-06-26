/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.form.binding.InvalidBindingStateException;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Add listener for changes in the personal sender list.
 */
public class OnAddPALChangeEventListener extends WollMuxEvent
{
  private XPALChangeEventListener listener;

  private Integer wollmuxConfHashCode;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener to register.
   * @param wollmuxConfHashCode
   *          HashCode of the configuration used by the caller.
   */
  public OnAddPALChangeEventListener(XPALChangeEventListener listener,
      Integer wollmuxConfHashCode)
  {
    this.listener = listener;
    this.wollmuxConfHashCode = wollmuxConfHashCode;
  }

  @Override
  protected void doit()
  {
    PersoenlicheAbsenderliste.getInstance().addPALChangeEventListener(listener);

    // Check if calling and my configuration are the same
    if (wollmuxConfHashCode != null)
    {
      int myWmConfHash = WollMuxFiles.getWollmuxConf().stringRepresentation().hashCode();
      if (myWmConfHash != wollmuxConfHashCode.intValue())
      {
        errorMessage(new InvalidBindingStateException(L.m(
            "Die Konfiguration des WollMux muss neu eingelesen werden.\n\nBitte beenden Sie den WollMux und Office und schießen Sie alle laufenden 'soffice.bin'-Prozesse über den Taskmanager ab.")));
      }
    }

  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
