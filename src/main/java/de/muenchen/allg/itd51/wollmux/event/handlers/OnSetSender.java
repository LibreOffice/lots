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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for selecting a new sender.
 */
public class OnSetSender extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnSetSender.class);

  private String senderName;

  private int idx;

  /**
   * Create this event.
   *
   * @param senderName
   *          The name of the new sender.
   * @param idx
   *          The id in the personal sender list of the new sender.
   */
  public OnSetSender(String senderName, int idx)
  {
    this.senderName = senderName;
    this.idx = idx;
  }

  @Override
  protected void doit()
  {
    String[] pal = PersoenlicheAbsenderliste.getInstance().getPALEntries();

    if (idx >= 0 && idx < pal.length && pal[idx].equals(senderName))
    {
      List<DJDataset> palDatasets = PersoenlicheAbsenderliste
          .getInstance().getSortedPALEntries();
      palDatasets.get(idx).select();
    } else
    {
      LOGGER.error(L.m(
          "Setzen des Senders '%1' schlug fehl, da der index '%2' nicht mit der PAL übereinstimmt (Inkonsistenzen?)",
          senderName, idx));
    }

    new OnPALChangedNotify().emit();
  }
}
