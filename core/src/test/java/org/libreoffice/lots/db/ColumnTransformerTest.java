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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.db.ColumnTransformer;
import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.QueryResults;
import org.libreoffice.lots.db.mock.MockDataset;
import org.libreoffice.lots.db.mock.MockQueryResults;
import org.libreoffice.lots.func.Function;
import org.libreoffice.lots.func.StringLiteralFunction;
import org.libreoffice.lots.func.Values;

public class ColumnTransformerTest
{

  @Test
  public void testColumnTransformer() throws Exception
  {
    ColumnTransformer transformer = new ColumnTransformer();
    assertTrue(transformer.getSchema().isEmpty());

    Map<String, Function> map = new HashMap<>();
    map.put("trafo", new StringLiteralFunction("transformed"));
    transformer = new ColumnTransformer(map);
    assertTrue(transformer.hasPseudoColumn("trafo"));

    assertEquals("transformed", transformer.get("trafo", new MockDataset()));
    assertEquals("value", transformer.get("column", new MockDataset()));

    Dataset dsTransformed = transformer.transform(new MockDataset());
    assertEquals("transformed", dsTransformed.get("trafo"));
    assertEquals("ds", dsTransformed.getKey());

    QueryResults results = new MockQueryResults();
    QueryResults transformedResults = transformer.transform(results);
    assertFalse(transformedResults.isEmpty());
    assertEquals(1, transformedResults.size());
    Iterator<Dataset> iter = transformedResults.iterator();
    assertTrue(iter.hasNext());
    assertEquals("ds", iter.next().getKey());
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());
  }

  @Test
  public void testColumnTransformerValues() throws Exception
  {
    Map<String, Function> map = new HashMap<>();
    map.put("trafo", new Function()
    {
      @Override
      public String[] parameters()
      {
        return null;
      }

      @Override
      public String getResult(Values parameters)
      {
        assertFalse(parameters.hasValue("unknwon"));
        assertTrue(parameters.hasValue("column"));

        assertFalse(parameters.getBoolean("unknown"));

        assertEquals("", parameters.getString("unknown"));
        return parameters.getString("column");
      }

      @Override
      public void getFunctionDialogReferences(Collection<String> set)
      {
      }

      @Override
      public boolean getBoolean(Values parameters)
      {
        return false;
      }
    });
    ColumnTransformer transformer = new ColumnTransformer(map);
    assertEquals("value", transformer.get("trafo", new MockDataset()));
  }
}
