package de.muenchen.allg.itd51.wollmux.core.db.mock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;

public class MockQueryResults implements QueryResults
{
  List<Dataset> results;

  public MockQueryResults()
  {
    this(new MockDataset());
  }

  public MockQueryResults(Dataset... datasets)
  {
    results = Arrays.asList(datasets);
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    return results.iterator();
  }

  @Override
  public int size()
  {
    return results.size();
  }

  @Override
  public boolean isEmpty()
  {
    return results.isEmpty();
  }
}
