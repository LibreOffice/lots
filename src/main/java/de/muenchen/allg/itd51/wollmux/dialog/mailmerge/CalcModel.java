package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.XSpreadsheetDocument;

public class CalcModel implements DatasourceModel
{
  private String windowTitle;
  private String calcUrl;
  private XSpreadsheetDocument spreadSheetDocument;
  private String[] spreadSheetTableTitles;

  public CalcModel(String windowTitle, String calcUrl, String[] spreadSheetTableTitles,
      XSpreadsheetDocument spreadSheetDocument)
  {
    this.windowTitle = windowTitle;
    this.calcUrl = calcUrl;
    this.spreadSheetTableTitles = spreadSheetTableTitles;
    this.spreadSheetDocument = spreadSheetDocument;
  }

  public String getWindowTitle()
  {
    return this.windowTitle;
  }

  public String getDatasourceName()
  {
    return getWindowTitle();
  }

  public List<String> getTableNames()
  {
    return Arrays.asList(getSpreadSheetTableTitles());
  }

  public String getCalcUrl()
  {
    return this.calcUrl;
  }

  public String[] getSpreadSheetTableTitles()
  {
    return this.spreadSheetTableTitles;
  }

  public XSpreadsheetDocument getSpreadSheetDocument()
  {
    return this.spreadSheetDocument;
  }
}
