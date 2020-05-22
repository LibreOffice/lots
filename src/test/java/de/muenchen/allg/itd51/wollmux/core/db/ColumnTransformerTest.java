package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockQueryResults;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.StringLiteralFunction;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

class ColumnTransformerTest
{

  @Test
  void testColumnTransformer() throws Exception
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
  void testColumnTransformerValues() throws Exception
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
      public String getString(Values parameters)
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
