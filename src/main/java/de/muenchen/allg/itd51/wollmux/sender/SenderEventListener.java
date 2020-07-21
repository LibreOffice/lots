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
package de.muenchen.allg.itd51.wollmux.sender;

import java.util.List;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * An event listener for all unspecified events.
 */
public class SenderEventListener implements WollMuxEventListener
{

  /**
   * Execute {@link OnInitialize} events.
   *
   * @param event
   *          An event.
   */
  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    SenderService service = SenderService.getInstance();
    if (service.getPALEntries().length == 0)
    {
      long found = service.searchDefaultSender();
      if (found != 1)
      {
        SenderService.getInstance().showSelectSenderDialog();
      }
    } else
    {
      // show sender which can't be updated from database
      StringBuilder names = new StringBuilder();
      List<String> lost = service.getLostDatasetDisplayStrings();
      if (!lost.isEmpty())
      {
        for (String l : lost)
        {
          names.append("- " + l + "\n");
        }
        String message = L.m("Die folgenden Datensätze konnten nicht " + "aus der Datenbank aktualisiert werden:\n\n"
            + "%1\n" + "Wenn dieses Problem nicht temporärer " + "Natur ist, sollten Sie diese Datensätze aus "
            + "ihrer Absenderliste löschen und neu hinzufügen!", names);
        InfoDialog.showInfoModal(L.m("WollMux-Info"), message);
      }
    }
  }

}
