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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.EventObject;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorage;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;

/**
 * Event for notification that the personal sender list has changed.
 *
 * It also updates the cache and {@link LocalOverrideStorage}.
 */
public class OnPALChangedNotify extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnPALChangedNotify.class);

  @Override
  protected void doit()
  {
    LOGGER.trace("OnPALChangedNotify: Update XPALChangeEventListener");
    for (XPALChangeEventListener listener : PersoenlicheAbsenderliste.getInstance())
    {
      listener.updateContent(new EventObject());
    }

    ConfigThingy cache = DatasourceJoinerFactory.getDatasourceJoiner()
        .saveCacheAndLOS(WollMuxFiles.getLosCacheFile());
    try
    {
      WollMuxFiles.writeConfToFile(WollMuxFiles.getLosCacheFile(), cache);
    } catch (IOException e)
    {
      LOGGER.error("Cache konnte nicht gespeichert werden.", e);
    }
  }
}