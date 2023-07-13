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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.db.Datasource;
import org.libreoffice.lots.db.EmptyDatasource;

public class EmptyDatasourceTest
{

  @Test
  public void testEmptyDatasource()
  {
    List<String> schema = List.of("column");
    Datasource empty = new EmptyDatasource(schema, "empty");
    assertEquals(schema, empty.getSchema());
    assertEquals("empty", empty.getName());
    assertTrue(empty.getDatasetsByKey(List.of("test")).isEmpty());
    assertTrue(empty.getContents().isEmpty());
    assertTrue(empty.find(Collections.emptyList()).isEmpty());
  }

}
