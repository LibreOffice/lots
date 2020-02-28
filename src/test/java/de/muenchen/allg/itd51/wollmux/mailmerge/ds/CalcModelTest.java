package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Table;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OfficeTest;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractCloseListener;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;

public class CalcModelTest extends OfficeTest
{
  private URL file = getClass().getResource("calcModel.ods");
  private XSpreadsheetDocument xDoc;
  private DatasourceModel model;
  private XCloseListener closeListener = new AbstractCloseListener()
  {
    @Override
    public void notifyClosing(EventObject arg0)
    {
      model.dispose();
    }
  };

  @BeforeEach
  public void setUp() throws Exception
  {
    xDoc = UNO.XSpreadsheetDocument(UNO.loadComponentFromURL(file.toString(), false, false, true));
    model = new CalcModel(xDoc);
    model.addCloseListener(closeListener);
    model.activateTable("Tabelle1");
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.XCloseable(xDoc).close(false);
  }

  @Test
  public void testContent() throws Exception
  {
    assertEquals("calcModel", model.getName(), "different model name");
    assertEquals("Tabelle1", model.getActivatedTable(), "different activated table");

    List<String> tables = model.getTableNames();
    assertTrue(tables.contains("Tabelle1"), "model doesn't contain table 'Tabelle1'");
    assertTrue(tables.contains("Tabelle2"), "model doesn't contain table 'Tabelle2'");

    Set<String> columns = model.getColumnNames();
    assertTrue(columns.contains("Anrede"), "Data doesn't contain column 'Anrede'");
    assertTrue(columns.contains("SGVorname"), "Data doesn't contain column 'SGVorname'");
    assertTrue(columns.contains("SGNachname"), "Data doesn't contain column 'SGNachname'");

    assertEquals(4, model.getNumberOfRecords(), "Different number of records");
    Map<String, String> record = model.getRecord(1);
    assertEquals("Frau", record.get("Anrede"), "Wrong data in record");
    assertEquals("Maria", record.get("SGVorname"), "Wrong data in record");
    assertEquals("B", record.get("SGNachname"), "Wrong data in record");

    assertEquals("calc", model.getSettings().get("TYPE").toString(), "different type in settings");
    assertTrue(model.getSettings().get("URL").toString().contains(file.getFile()),
        "different URL in settings");
    assertEquals("Tabelle1", model.getSettings().get("TABLE").toString(),
        "different table in settings");
  }

  @Test
  public void changeTable() throws Exception
  {
    model.activateTable("Tabelle2");
    assertEquals("Tabelle2", model.getActivatedTable(), "different activated table");
    Table<Integer, String, String> data = model.getData();
    assertEquals("1", data.get(1, "Test"), "wrong data");
    // second record is hidden, so it doesn't count
    assertEquals("3.5", data.get(2, "Test"), "wrong data");

    assertTrue(model.getColumnNames().contains("Column with 2 rows"), "column with line break");
    assertEquals("A", data.get(1, "Column with 2 rows"));
  }

  @Test
  public void modifyTable() throws Exception
  {
    Map<String, FieldSubstitution> mapping = new HashMap<>(2);
    FieldSubstitution first = new FieldSubstitution();
    first.addFixedText("Test");
    mapping.put("Test1", first);
    FieldSubstitution second = new FieldSubstitution();
    second.addField("SGVorname");
    second.addFixedText(" ");
    second.addField("SGNachname");
    mapping.put("Test2", second);
    model.addColumns(mapping);
    Map<String, String> data = model.getRecord(1);
    assertEquals("Test", data.get("Test1"), "wrong data");
    assertEquals("Maria B", data.get("Test2"), "wrong data");

    XSpreadsheet sheet = UNO.XSpreadsheet(xDoc.getSheets().getByName("Tabelle1"));
    UNO.XTextRange(sheet.getCellByPosition(3, 1)).setString("Musterfrau");
    data = model.getRecord(1);
    assertEquals("Musterfrau", data.get("SGNachname"), "wrong data");
    assertEquals("Maria Musterfrau", data.get("Test2"), "wrong data");

    UNO.XModifiable(xDoc).setModified(false);
  }

}
