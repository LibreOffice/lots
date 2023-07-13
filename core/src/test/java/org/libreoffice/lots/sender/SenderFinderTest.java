/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.db.mock.MockDatasource;
import org.libreoffice.lots.sender.Sender;
import org.libreoffice.lots.sender.SenderException;
import org.libreoffice.lots.sender.SenderFinder;
import org.libreoffice.lots.sender.SenderService;
import org.libreoffice.lots.sender.mock.MockCache;

public class SenderFinderTest
{
  private static SenderService senderServcie = null;

  @BeforeAll
  public static void setupTest() throws SenderException
  {
    senderServcie = new SenderService(new MockDatasource(), null, new MockCache(), "");
  }

  @Test
  public void testFindWithValueEvaluate()
      throws IOException, SyntaxErrorException, InterruptedException, ExecutionException
  {
    SenderFinder dataFinder = new SenderFinder(senderServcie)
    {

      @Override
      protected String getValueForKey(String key)
      {
        return "value";
      }
    };
    List<Sender> results = dataFinder.find(new ConfigThingy("", "column \"${test}\"")).get();
    assertEquals(1, results.size());
  }

  @Test
  public void testFindWithNullKey() throws IOException, SyntaxErrorException, InterruptedException, ExecutionException
  {
    SenderFinder dataFinder = new SenderFinder(senderServcie)
    {

      @Override
      protected String getValueForKey(String key)
      {
        return "";
      }
    };
    List<Sender> results = dataFinder.find(List.of(ImmutablePair.of(null, ""))).get();
    assertTrue(results.isEmpty());
  }

  @Test
  public void testFindWithEmptyKey() throws IOException, SyntaxErrorException, InterruptedException, ExecutionException
  {
    SenderFinder dataFinder = new SenderFinder(senderServcie)
    {

      @Override
      protected String getValueForKey(String key)
      {
        return "";
      }
    };
    List<Sender> results = dataFinder.find(List.of(ImmutablePair.of("", ""))).get();
    assertTrue(results.isEmpty());
  }

  @Test
  public void testFindWithUnknownKey()
      throws IOException, SyntaxErrorException, InterruptedException, ExecutionException
  {
    SenderFinder dataFinder = new SenderFinder(senderServcie)
    {

      @Override
      protected String getValueForKey(String key) throws SenderException
      {
        throw new SenderException("unknown key");
      }
    };
    List<Sender> results = dataFinder.find(List.of(ImmutablePair.of("column", "\"${test}\""))).get();
    assertTrue(results.isEmpty());
  }
}
