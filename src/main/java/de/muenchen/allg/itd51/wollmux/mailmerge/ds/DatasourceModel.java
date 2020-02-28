package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.List;

public interface DatasourceModel
{
  public String getDatasourceName();

  public List<String> getTableNames();
}
