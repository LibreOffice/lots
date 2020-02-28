package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModelListener;

public class MockDatasourceModel implements DatasourceModel
{
  @Override
  public int getNumberOfRecords() throws NoTableSelectedException
  {
    return 5;
  }

  @Override
  public Map<String, String> getRecord(int recordId) throws NoTableSelectedException
  {
    Map<String, String> map = new HashMap<>();
    map.put("Id", "" + recordId);
    return map;
  }

  @Override
  public void addCloseListener(XCloseListener arg0)
  {
  }

  @Override
  public void removeCloseListener(XCloseListener arg0)
  {
  }

  @Override
  public void addDatasourceListener(DatasourceModelListener listener)
  {
  }

  @Override
  public void removeDatasourceListener(DatasourceModelListener listener)
  {
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public List<String> getTableNames()
  {
    return null;
  }

  @Override
  public void activateTable(String tableName) throws NoTableSelectedException
  {
  }

  @Override
  public String getActivatedTable() throws NoTableSelectedException
  {
    return null;
  }

  @Override
  public Set<String> getColumnNames() throws NoTableSelectedException
  {
    return null;
  }

  @Override
  public boolean supportsAddColumns()
  {
    return false;
  }

  @Override
  public void addColumns(Map<String, FieldSubstitution> mapIdToSubstitution)
      throws NoTableSelectedException
  {
  }

  @Override
  public Table<Integer, String, String> getData() throws NoTableSelectedException
  {
    return null;
  }

  @Override
  public void toFront()
  {
  }

  @Override
  public ConfigThingy getSettings() throws NoTableSelectedException
  {
    return null;
  }

  @Override
  public void dispose()
  {
  }
}
