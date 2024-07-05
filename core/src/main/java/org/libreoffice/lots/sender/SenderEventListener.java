/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.sender;

import java.util.List;

import org.libreoffice.lots.WollMuxSingleton;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.event.WollMuxEventHandler;
import org.libreoffice.lots.event.WollMuxEventListener;
import org.libreoffice.lots.event.handlers.OnInitialize;
import org.libreoffice.lots.util.L;

import com.google.common.eventbus.Subscribe;

/**
 * An event listener for all unspecified events.
 */
public class SenderEventListener implements WollMuxEventListener
{

  /**
   * Execute {@link OnInitialize} events.
   *
   * The listener is unregistered after showing
   * some info dialogs to prevent showing dialogs multiple times
   * on new instances.
   *
   * @param event
   *          An event.
   */
  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    SenderService service = SenderService.getInstance();

    if (WollMuxSingleton.getInstance().isNoConfig())
    {
      return;
    }

    if (service.getPALEntries().length == 0)
    {
      long found = service.searchDefaultSender();
      if (found != 1)
      {
        InfoDialog.showInfoModal(L.m("Select sender"),
            "More than one sender was found. The first one was selected.\n"
            + "Please make sure it is the right one (Use the dropdown in the WollMux sidebar).");
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
        String message = L.m("The following data sets could not be updated from the database:\n\n"
            + "{0}\nIf this problem persists, you might want to delete these data sets from "
            + "your sender list and add them again!", names);
        InfoDialog.showInfoModal(L.m("WollMux Info"), message);
      }
    }

    WollMuxEventHandler.getInstance().unregisterListener(this);
  }

}
