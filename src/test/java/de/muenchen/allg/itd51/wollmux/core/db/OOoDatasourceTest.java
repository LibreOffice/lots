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
package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

class OOoDatasourceTest extends OfficeTest
{

  @Test
  void testOOoDatasource() throws Exception
  {
    Datasource ds = new OOoDatasource(null,
        new ConfigThingy("", "NAME \"ooo\" SOURCE \"Bibliography\" TABLE \"biblio\" Schluessel (\"Identifier\")"));
    assertEquals("ooo", ds.getName());
    assertFalse(ds.getSchema().isEmpty());

    QueryResults results = ds.getContents();
    assertEquals(20, results.size());
    results = ds.getDatasetsByKey(List.of("Identifier#ARJ00#"));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("Identifier#ARJ00#", data.getKey());
    assertEquals("99", data.get("Pages"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unknown"));

    results = ds.find(List.of(new QueryPart("Author", "Gris, Myriam")));
    assertEquals(5, results.size());
  }

}
