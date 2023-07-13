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
package org.libreoffice.lots.db.mock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.QueryResults;

public class MockQueryResults implements QueryResults
{
  List<Dataset> results;

  public MockQueryResults()
  {
    this(new MockDataset());
  }

  public MockQueryResults(Dataset... datasets)
  {
    results = Arrays.asList(datasets);
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    return results.iterator();
  }

  @Override
  public int size()
  {
    return results.size();
  }

  @Override
  public boolean isEmpty()
  {
    return results.isEmpty();
  }
}
