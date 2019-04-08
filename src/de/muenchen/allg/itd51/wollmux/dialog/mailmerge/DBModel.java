package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.ArrayList;
import java.util.List;

public class DBModel
{
  public List<String> tableNames = new ArrayList<>();
  public String datasourceName;
  
  public DBModel(String datasourceName, List<String> tableNames) {
    this.datasourceName = datasourceName;
    this.tableNames = tableNames;
  }
  
  public String getDatasourceName() {
    return this.datasourceName;
  }
  
  public List<String> getTableNames() {
    return this.tableNames;
  }
  
  public void setTableNames(List<String> tableNames) {
    this.tableNames = tableNames;
  }
}
