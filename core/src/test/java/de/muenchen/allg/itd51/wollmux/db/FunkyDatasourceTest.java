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

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

public class FunkyDatasourceTest
{

  @Test
  public void testFunkyDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column"), List.of(new MockDataset("ds", Map.of("column", "value")),
            new MockDataset("ds2", Map.of("column", "value2")))));
    Datasource ds = new FunkyDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"funky\" SOURCE \"mock\" Spaltenumsetzung(column2(CAT(\"neu_\" VALUE(\"column\"))))"));
    assertEquals("funky", ds.getName());
    assertEquals(List.of("column2", "column"), ds.getSchema());

    QueryResults results = ds.getContents();
    assertEquals(2, results.size());
    results = ds.getDatasetsByKey(List.of("ds"));
    assertEquals(1, results.size());

    results = ds.find(List.of(new QueryPart("column", "value2")));
    assertEquals(1, results.size());
    assertEquals("neu_value2", results.iterator().next().get("column2"));
  }

  @Test
  public void testInvalidFunkyDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column"), List.of(new MockDataset("ds", Map.of("column", "value")),
            new MockDataset("ds2", Map.of("column", "value2")))));
    assertThrows(ConfigurationErrorException.class, () -> new FunkyDatasource(nameToDatasource, new ConfigThingy("",
        "SOURCE \"mock\" Spaltenumsetzung(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
    assertThrows(ConfigurationErrorException.class, () -> new FunkyDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"funky\" Spaltenumsetzung(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
    assertThrows(ConfigurationErrorException.class, () -> new FunkyDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"funky\" SOURCE \"unknown\" Spaltenumsetzung(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
  }

}
