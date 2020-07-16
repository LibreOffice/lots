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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

public class LocalOverrideStorageDummyImpl implements LocalOverrideStorage
{
  DJDataset dummyDataset;
  List<String> schema = new ArrayList<>();

  public LocalOverrideStorageDummyImpl()
  {
    dummyDataset = new DJDataset()
    {

      @Override
      public String getKey()
      {
        return "<key>";
      }

      @Override
      public String get(String columnName) throws ColumnNotFoundException
      {
        return columnName;
      }

      @Override
      public void set(String columnName, String newValue)
          throws ColumnNotFoundException
      {
        throw new UnsupportedOperationException();
      }

      @Override
      public void select()
      {
        throw new UnsupportedOperationException();
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isSelectedDataset()
      {
        return true;
      }

      @Override
      public boolean isFromLOS()
      {
        return true;
      }

      @Override
      public boolean hasLocalOverride(String columnName)
          throws ColumnNotFoundException
      {
        return false;
      }

      @Override
      public boolean hasBackingStore()
      {
        return false;
      }

      @Override
      public void discardLocalOverride(String columnName)
          throws ColumnNotFoundException, NoBackingStoreException
      {
        // nothing to do
      }

      @Override
      public DJDataset copy()
      {
        return dummyDataset;
      }
    };
  }

  @Override
  public void selectDataset(String selectKey, int sameKeyIndex)
  {
    // nothing to do
  }

  @Override
  public DJDataset newDataset()
  {
    return dummyDataset;
  }

  @Override
  public DJDataset copyNonLOSDataset(Dataset ds)
  {
    return dummyDataset;
  }

  @Override
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return dummyDataset;
  }

  @Override
  public int getSelectedDatasetSameKeyIndex() throws DatasetNotFoundException
  {
    return 0;
  }

  @Override
  public List<Dataset> refreshFromDatabase(Datasource database)
  {
    return new ArrayList<>();
  }

  @Override
  public List<String> getSchema()
  {
    if ( schema.isEmpty()){
      schema.add(DatasourceJoiner.NOCONFIG);
    }
    return schema;
  }

  @Override
  public void dumpData(ConfigThingy conf)
  {
    // nothing to do
  }

  @Override
  public void setSchema(List<String> schema)
  {
    this.schema = schema;
  }

  @Override
  public int size()
  {
    return 1;
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    List<Dataset> list = new ArrayList<>();
    list.add(dummyDataset);
    return list.iterator();
  }

  @Override
  public boolean isEmpty()
  {
    return false;
  }
}
