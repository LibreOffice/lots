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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.db.Datasource;
import org.libreoffice.lots.db.FunkyDatasource;
import org.libreoffice.lots.db.QueryPart;
import org.libreoffice.lots.db.QueryResults;
import org.libreoffice.lots.db.mock.MockDataset;
import org.libreoffice.lots.db.mock.MockDatasource;

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
        "NAME \"funky\" SOURCE \"mock\" ColumnTransformation(column2(CAT(\"neu_\" VALUE(\"column\"))))"));
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
        "SOURCE \"mock\" ColumnTransformation(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
    assertThrows(ConfigurationErrorException.class, () -> new FunkyDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"funky\" ColumnTransformation(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
    assertThrows(ConfigurationErrorException.class, () -> new FunkyDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"funky\" SOURCE \"unknown\" ColumnTransformation(column2(CAT(\"neu_\" VALUE(\"column\"))))")));
  }

}
