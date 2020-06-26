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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.ByJavaPropertyFinder;
import de.muenchen.allg.itd51.wollmux.db.ByOOoUserProfileFinder;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;

/**
 * Event for initializing WollMux.
 */
public class OnInitialize extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnInitialize.class);

  @Override
  protected void doit()
  {
    DatasourceJoiner dsj = DatasourceJoinerFactory.getDatasourceJoiner();

    if (dsj.getLOS().size() == 0)
    {
      // try to find a suitable sender
      int found = searchDefaultSender(dsj);

      // show dialog if no suitable sender was found
      if (found != 1)
      {
        new OnShowDialogAbsenderAuswaehlen().emit();
      }
      else
      {
        new OnPALChangedNotify().emit();
      }
    }
  }

  /**
   * Search for a sender by evaluating the configuration or using the information of the LibreOffice
   * profile. If one sender is found, further search strategies aren't evaluated.
   *
   * @param dsj
   *          {@link DatasourceJoiner} for searching senders.
   * @return Number of senders found.
   */
  @SuppressWarnings("squid:S2629")
  private int searchDefaultSender(DatasourceJoiner dsj)
  {

    QueryResults results = null;
    try
    {
      // search by strategy defined in configuration
      ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
      ConfigThingy strat = wmConf.query("PersoenlicheAbsenderlisteInitialisierung")
          .query("Suchstrategie").getLastChild();
      for (ConfigThingy element : strat)
      {
        if (element.getName().equals("BY_JAVA_PROPERTY"))
        {
          results = new ByJavaPropertyFinder(dsj).find(element);
        } else if (element.getName().equals("BY_OOO_USER_PROFILE"))
        {
          results = new ByOOoUserProfileFinder(dsj).find(element);
        } else
        {
          LOGGER.error("Ungültiger Schlüssel in Suchstrategie: {}", element.stringRepresentation());
        }
      }
    } catch (NodeNotFoundException e)
    {
      // search with OOoUserProfile:
      LOGGER.info("Couldn't find any search strategy");
      LOGGER.debug("", e);
      List<Pair<String, String>> query = new ArrayList<>();
      query.add(new ImmutablePair<>("Vorname", "${givenname}"));
      query.add(new ImmutablePair<>("Nachname", "${sn}"));

      results = new ByOOoUserProfileFinder(dsj).find(query);
    }

    return dsj.addToPAL(results);
  }
}