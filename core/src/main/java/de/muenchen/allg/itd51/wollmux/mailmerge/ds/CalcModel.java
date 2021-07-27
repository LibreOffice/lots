/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.math.DoubleMath;
import com.sun.star.awt.XTopWindow;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XModifyListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.mailmerge.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoProperty;

/**
 * A {@link DatasourceModel} using a calc file as source.
 */
public class CalcModel implements DatasourceModel
{
  private static final Logger LOGGER = LoggerFactory.getLogger(CalcModel.class);

  /**
   * The name of this database. It's the window title without Office.
   */
  private String datasourceName;

  /**
   * The underlying calc file.
   */
  private XSpreadsheetDocument spreadSheetDocument;

  /**
   * The data of one sheet.
   */
  HashBasedTable<Integer, String, String> data = HashBasedTable.create();

  /**
   * The name of the sheet.
   */
  private String sheetName;

  /**
   * Set of columns, which contain data.
   */
  private SortedSet<Integer> columnIndexes = new TreeSet<>();

  /**
   * Set or rows, which contain data.
   */
  private SortedSet<Integer> rowIndexes = new TreeSet<>();

  /**
   * Mapping from column name to column indexes.
   */
  private Map<String, String> mapColumnNameToCalcColumnName = new HashMap<>();

  /**
   * Listener on this model.
   */
  private Set<DatasourceModelListener> listener = new HashSet<>();

  /**
   * Listener for changes in the calc file.
   */
  private XModifyListener modifyListener = new XModifyListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // nothing to do
    }

    @Override
    public void modified(EventObject event)
    {
      try
      {
        readTable();
        listener.forEach(DatasourceModelListener::datasourceChanged);
      } catch (NoTableSelectedException ex)
      {
        LOGGER.debug("", ex);
      }
    }
  };

  /**
   * Create a new {@link DatasourceModel} based on a calc file.
   *
   * @param spreadSheetDocument
   *          The calc document.
   */
  public CalcModel(XSpreadsheetDocument spreadSheetDocument)
  {
    String title = UnoProperty.getPropertyByPropertyValues(UNO.XModel(spreadSheetDocument).getArgs(),
        UnoProperty.TITLE);
    this.datasourceName = UNO.stripOpenOfficeFromWindowName(title);
    this.spreadSheetDocument = spreadSheetDocument;
    UNO.XModifiable(spreadSheetDocument).addModifyListener(modifyListener);
  }

  @Override
  public void addDatasourceListener(DatasourceModelListener listener)
  {
    this.listener.add(listener);
  }

  @Override
  public void removeDatasourceListener(DatasourceModelListener listener)
  {
    this.listener.remove(listener);
  }

  @Override
  public void dispose()
  {
    UNO.XModifiable(spreadSheetDocument).removeModifyListener(modifyListener);
  }

  @Override
  public String getActivatedTable()
  {
    return sheetName;
  }

  @Override
  public void activateTable(String tableName) throws NoTableSelectedException
  {
    this.sheetName = tableName;
    readTable();
    LOGGER.debug("Tabelle {} wurde ausgewählt", sheetName);
  }

  @Override
  public String getName()
  {
    return this.datasourceName;
  }

  @Override
  public List<String> getTableNames()
  {
    XSpreadsheets sheets = spreadSheetDocument.getSheets();
    if (sheets == null)
    {
      return Collections.emptyList();
    }
    return Arrays.asList(sheets.getElementNames());
  }

  @Override
  public Set<String> getColumnNames() throws NoTableSelectedException
  {
    if (spreadSheetDocument == null)
    {
      return Collections.emptySet();
    }
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    return mapColumnNameToCalcColumnName.keySet();
  }

  @Override
  public boolean supportsAddColumns()
  {
    return true;
  }

  @Override
  public void toFront()
  {
    XModel documentModel = UNO.XModel(spreadSheetDocument);
    if (documentModel != null)
    {
      XTopWindow win = UNO
          .XTopWindow(documentModel.getCurrentController().getFrame().getContainerWindow());
      win.toFront();
      try
      {
        UNO.XSelectionSupplier(documentModel.getCurrentController())
            .select(spreadSheetDocument.getSheets().getByName(sheetName));
      } catch (IllegalArgumentException | NoSuchElementException | WrappedTargetException e)
      {
        LOGGER.debug("Couldn't select sheet {}", sheetName);
      }
    }
  }

  @Override
  public Table<Integer, String, String> getData() throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    return data;
  }

  @Override
  public int getNumberOfRecords() throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    return data.rowKeySet().size();
  }

  @Override
  public Map<String, String> getRecord(int rowIndex) throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    return data.row(rowIndex);
  }

  @Override
  public ConfigThingy getSettings() throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    ConfigThingy dq = new ConfigThingy("Datenquelle");
    String url = UNO.XModel(spreadSheetDocument).getURL();
    if (!url.isEmpty() && !sheetName.isEmpty())
    {
      ConfigThingy arg = new ConfigThingy("TYPE");
      arg.addChild(new ConfigThingy("calc"));
      dq.addChild(arg);
      arg = new ConfigThingy("URL");
      arg.addChild(new ConfigThingy(url));
      dq.addChild(arg);
      arg = new ConfigThingy("TABLE");
      arg.addChild(new ConfigThingy(sheetName));
      dq.addChild(arg);
    }
    return dq;
  }

  @Override
  public void addColumns(Map<String, FieldSubstitution> mapIdToSubstitution)
      throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }
    XCellRangesQuery sheet;
    try
    {
      sheet = UNO.XCellRangesQuery(spreadSheetDocument.getSheets().getByName(sheetName));
    } catch (Exception x)
    {
      return;
    }

    // add column after last column
    int newColumnX = 0;
    if (!columnIndexes.isEmpty())
    {
      newColumnX = columnIndexes.last() + 1;
    }

    // Placeholder for the row number in the formula. It's hopefully never used
    // by users.
    String rowNumPlaceholder = "Ø©¿";
    for (Map.Entry<String, FieldSubstitution> ent : mapIdToSubstitution.entrySet())
    {
      String fieldId = ent.getKey();
      FieldSubstitution subst = ent.getValue();
      String formulaStr = buildFormula(rowNumPlaceholder, subst);
      if (formulaStr == null)
      {
        continue;
      }

      try
      {
        XCellRange sheetCellRange = UNO.XCellRange(sheet);
        int ymin = 0;
        if (!rowIndexes.isEmpty())
        {
          ymin = rowIndexes.first();

          // only write rows which already have data.
          for (int y : rowIndexes)
          {
            UNO.XCell(sheetCellRange.getCellByPosition(newColumnX, y))
                .setFormula(formulaStr.replace(rowNumPlaceholder, "" + (y + 1)));
          }
        }
        UNO.XTextRange(sheetCellRange.getCellByPosition(newColumnX, ymin)).setString(fieldId);

        ++newColumnX;

      } catch (Exception x)
      {
        LOGGER.error(L.m("Kann Spalte \"%1\" nicht hinzufügen", fieldId), x);
      }
    }
  }

  @Override
  public void addCloseListener(XCloseListener listener)
  {
    UNO.XCloseable(spreadSheetDocument).addCloseListener(listener);
  }

  @Override
  public void removeCloseListener(XCloseListener listener)
  {
    UNO.XCloseable(spreadSheetDocument).removeCloseListener(listener);
  }

  /**
   * Map an index to a column name (e.g. A (=1) or AB (=28)).
   *
   * @param col
   *          A column index.
   * @return The column name.
   */
  private String getCalcColumnNameForColumnIndex(int col)
  {
    StringBuilder buffy = new StringBuilder();
    do
    {
      --col;
      buffy.insert(0, (char) ('A' + (col % 26)));
    } while ((col = col / 26) > 0);
    return buffy.toString();
  }

  /**
   * Read the data from the currently selected sheet.
   *
   * @throws NoTableSelectedException
   *           No sheet is selected.
   */
  private void readTable() throws NoTableSelectedException
  {
    if (sheetName == null)
    {
      throw new NoTableSelectedException();
    }

    data.clear();
    mapColumnNameToCalcColumnName.clear();
    if (spreadSheetDocument != null)
    {
      try
      {
        XCellRangesQuery sheet = UNO
            .XCellRangesQuery(spreadSheetDocument.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          XSheetCellRanges visibleCellRanges = sheet.queryVisibleCells();
          XSheetCellRanges nonEmptyCellRanges = sheet.queryContentCells(
              (short) (com.sun.star.sheet.CellFlags.VALUE | com.sun.star.sheet.CellFlags.DATETIME
                  | com.sun.star.sheet.CellFlags.STRING | com.sun.star.sheet.CellFlags.FORMULA));
          CellRangeAddress[] nonEmptyCellRangeAddresses = nonEmptyCellRanges.getRangeAddresses();
          columnIndexes = new TreeSet<>(Arrays.stream(nonEmptyCellRangeAddresses)
              .flatMap(nonEmptyCells -> Arrays.stream(UNO.XCellRangesQuery(visibleCellRanges)
                  .queryIntersection(nonEmptyCells).getRangeAddresses()))
              .flatMap(addr -> IntStream.rangeClosed(addr.StartColumn, addr.EndColumn).boxed())
              .distinct().sorted().collect(Collectors.toSet()));
          rowIndexes = new TreeSet<>(Arrays.stream(nonEmptyCellRangeAddresses)
              .flatMap(nonEmptyCells -> Arrays.stream(UNO.XCellRangesQuery(visibleCellRanges)
                  .queryIntersection(nonEmptyCells).getRangeAddresses()))
              .flatMap(addr -> IntStream.rangeClosed(addr.StartRow, addr.EndRow).boxed()).distinct()
              .sorted().collect(Collectors.toSet()));
          if (!rowIndexes.isEmpty() && !columnIndexes.isEmpty())
          {
            int startRow = rowIndexes.first();
            int endRow = rowIndexes.last();
            int startColumn = columnIndexes.first();
            int endColumn = columnIndexes.last();
            XCellRange range = UNO.XCellRange(sheet).getCellRangeByPosition(startColumn, startRow,
                endColumn, endRow);            
            Object[][] cellData = UNO.XCellRangeData(range).getDataArray();
            readRowData(cellData, range);
          }
        }
      } catch (Exception e)
      {
        LOGGER.debug("", e);
      }
    }
  }

  /**
   * Read the data of row and put it into the data table. If row doesn't contain data nothing is
   * done.
   *
   * @param cellData
   *          The data of the whole sheet..
   * @throws IndexOutOfBoundsException 
   */
  private void readRowData(Object[][] cellData, XCellRange range) throws IndexOutOfBoundsException
  {
    List<Integer> rows = new ArrayList<>(rowIndexes);
    for (int record = 0; record < rowIndexes.size(); record++)
    {
      int row = rows.get(record);
      for (int j = 0; j < cellData[0].length; j++)
      {
        if (columnIndexes.contains(j + columnIndexes.first()))
        {
          String column = UNO.XTextRange(range.getCellByPosition(j, rows.get(0))).getString();
          column = CharMatcher.breakingWhitespace().replaceFrom(column, " ");
          // first row contains the header
          if (record == 0)
          {
            mapColumnNameToCalcColumnName.put(column, getCalcColumnNameForColumnIndex(j + 1));
          } else
          {
            String value = UNO.XTextRange(range.getCellByPosition(j, row)).getString();
            data.put(record, column, value);
          }
        }
      }
    }
  }

  /**
   * Compute a formula of the field substitutions.
   *
   * @param rowNumPlaceholder
   *          The placeholder for row numbers.
   * @param subst
   *          List of substitutions.
   * @return A formula as string.
   */
  private String buildFormula(String rowNumPlaceholder, FieldSubstitution subst)
  {
    StringBuilder formula = new StringBuilder();
    for (FieldSubstitution.SubstElement substEle : subst)
    {
      if (formula.length() == 0)
        formula.append("=CONCATENATE(");
      else
        formula.append(';');
      if (substEle.isFixedText())
      {
        formula.append('"');
        formula.append(substEle.getValue().replace("\"", "\"\""));
        formula.append('"');
      } else if (substEle.isField())
      {
        String calcColumnName = mapColumnNameToCalcColumnName.get(substEle.getValue());
        if (calcColumnName != null)
        {
          formula.append(calcColumnName);
          formula.append(rowNumPlaceholder);
        } else
        {
          formula.append("\"<");
          formula.append(substEle.getValue().replace("\"", "\"\""));
          formula.append(rowNumPlaceholder);
          formula.append(">\"");
        }
      } else
      {
        formula.append("\"UNKNOWN\"");
      }
    }

    if (formula.length() == 0)
    {
      return null;
    }

    formula.append(')');
    return formula.toString();
  }
}
