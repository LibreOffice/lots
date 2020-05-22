package de.muenchen.allg.itd51.wollmux.core.db.mock;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.NoBackingStoreException;

public class MockDataset implements DJDataset
{
  private String key;
  private Map<String, String> data;

  public MockDataset(String key, String column, String value)
  {
    this.key = key;
    data = new HashMap<>();
    data.put(column, value);
  }

  public MockDataset(String key, Map<String, String> data)
  {
    this.key = key;
    this.data = data;
  }

  public MockDataset()
  {
    this("ds", "column", "value");
  }

  @Override
  public String get(String columnName) throws ColumnNotFoundException
  {
    if (data.containsKey(columnName))
    {
      return data.get(columnName);
    }
    throw new ColumnNotFoundException();
  }

  @Override
  public String getKey()
  {
    return key;
  }

  @Override
  public void set(String columnName, String newValue) throws ColumnNotFoundException
  {
  }

  @Override
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    return false;
  }

  @Override
  public boolean hasBackingStore()
  {
    return false;
  }

  @Override
  public boolean isFromLOS()
  {
    return false;
  }

  @Override
  public boolean isSelectedDataset()
  {
    return false;
  }

  @Override
  public void select()
  {
  }

  @Override
  public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
  {
  }

  @Override
  public DJDataset copy()
  {
    return null;
  }

  @Override
  public void remove()
  {
  }

}
