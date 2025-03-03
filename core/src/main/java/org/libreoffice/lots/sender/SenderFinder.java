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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.libreoffice.lots.config.ConfigThingy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search for sender. The query can have variables like {@code ${variable}} which get replaced
 * according to the strategy of the DataFinder.
 */
abstract class SenderFinder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SenderFinder.class);

  private SenderService senderList;

  /**
   * Create a finder.
   *
   * @param senderList
   *          The sender list to scan.
   */
  public SenderFinder(SenderService senderList)
  {
    this.senderList = senderList;
  }

  /**
   * Build the query parameter from the {@link ConfigThingy} and search for senders. Keys are used as
   * column names. Values have to be matched by the sender. All parameters have to match.
   *
   * @param conf
   *          The configuration. of the search.
   * @return Future of the list with senders.
   */
  public CompletableFuture<List<Sender>> find(ConfigThingy conf)
  {
    List<Pair<String, String>> query = new ArrayList<>();

    for (ConfigThingy element : conf)
    {
      query.add(new ImmutablePair<>(element.getName(), element.toString()));
    }
    return find(query);
  }

  /**
   * Search for senders.
   *
   * @param query
   *          The query parameter. The key of the pair is the column and the value has to match the
   *          sender.
   *
   * @return Future of the list with senders.
   */
  public CompletableFuture<List<Sender>> find(List<Pair<String, String>> query)
  {
    Map<String, String> evaluatedQuery = new HashMap<>(query.size());
    for (Pair<String, String> pair : query)
    {
      LOGGER.trace("{}.find({}, {})", this.getClass().getSimpleName(), pair.getKey(), pair.getValue() + ")");

      if (pair.getKey() == null || replaceVariables(pair.getKey()).isEmpty())
      {
        return CompletableFuture.completedFuture(Collections.emptyList());
      }
      evaluatedQuery.put(replaceVariables(pair.getKey()), replaceVariables(pair.getValue()));
    }

    return senderList.find(evaluatedQuery);
  }

  private String replaceVariables(String exp)
  {
    final Pattern pattern = Pattern.compile("\\$\\{([^\\}]*)\\}");
    while (true)
    {
      Matcher m = pattern.matcher(exp);
      if (!m.find())
        break;
      String key = m.group(1);
      String value;
      try
      {
        value = getValueForKey(key);
      } catch (SenderException e)
      {
        LOGGER.error(e.getMessage(), e);
        value = "";
      }
      value = replaceVariableBoundaries(value);
      exp = m.replaceFirst(value);
    }
    return exp;
  }

  private String replaceVariableBoundaries(String value)
  {
    value = value.replaceAll("\\$\\{", "<");
    value = value.replaceAll("\\}", ">");
    return value;
  }

  /**
   * Get the value of a variable like {@code ${variable}}.
   *
   * @param key
   *          The key of the variable.
   * @return The value of the variable.
   * @throws SenderException
   *           The variable doesn't exist.
   */
  protected abstract String getValueForKey(String key) throws SenderException;

}
