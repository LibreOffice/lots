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
package de.muenchen.allg.itd51.wollmux.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SimpleDatasetTest
{

  @Test
  public void testSimpleDataset() throws Exception
  {
    Map<String, String> data = new HashMap<>();
    data.put("column", "value");
    Dataset dataset = new SimpleDataset("Test", data);

    assertEquals("Test", dataset.getKey());
    assertEquals("value", dataset.get("column"));
    assertThrows(ColumnNotFoundException.class, () -> dataset.get("column2"));

    Dataset dataset2 = new SimpleDataset(List.of("column"), dataset);
    assertEquals("Test", dataset2.getKey());
    assertEquals("value", dataset2.get("column"));
  }

}
