/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Table;
import com.sun.star.lang.EventObject;
import com.sun.star.sdb.XOfficeDatabaseDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XCloseListener;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractCloseListener;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

public class DBModelTest extends OfficeTest
{
  private URL file = getClass().getResource("dbModel.odb");
  private String dbName = "DBModelTest";
  private XOfficeDatabaseDocument xDoc;
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
    xDoc = UnoRuntime.queryInterface(XOfficeDatabaseDocument.class, loadComponent(file.toString(), false, true));
    UNO.dbContext.registerObject(dbName, xDoc.getDataSource());
    model = new DBModel(xDoc);
    model.addCloseListener(closeListener);
    model.activateTable("Tabelle1");
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.dbContext.revokeObject(dbName);
    UNO.XCloseable(xDoc).close(false);
  }

  @Test
  public void testContent() throws Exception
  {
    assertEquals("DBModelTest", model.getName(), "different model name");
    assertEquals("Tabelle1", model.getActivatedTable(), "different activated table");

    List<String> tables = model.getTableNames();
    assertTrue(tables.contains("Tabelle1"), "model doesn't contain table 'Tabelle1'");
    assertTrue(tables.contains("Tabelle2"), "model doesn't contain table 'Tabelle2'");

    Set<String> columns = model.getColumnNames();
    assertTrue(columns.contains("Anrede"), "Data doesn't contain column 'Anrede'");
    assertTrue(columns.contains("SGVorname"), "Data doesn't contain column 'SGVorname'");
    assertTrue(columns.contains("SGNachname"), "Data doesn't contain column 'SGNachname'");

    assertEquals(4, model.getNumberOfRecords(), "Different number of records");
    Map<String, String> record = model.getRecord(4);
    assertEquals("Frau", record.get("Anrede"), "Wrong data in record");
    assertEquals("Maria", record.get("SGVorname"), "Wrong data in record");
    assertEquals("B", record.get("SGNachname"), "Wrong data in record");

    assertEquals("ooo", model.getSettings().get("TYPE").toString(), "different type in settings");
    assertEquals(dbName, model.getSettings().get("SOURCE").toString(),
        "different source in settings");
    assertEquals("Tabelle1", model.getSettings().get("TABLE").toString(),
        "different table in settings");
  }

  @Test
  public void changeTable() throws Exception
  {
    model.activateTable("Tabelle2");
    assertEquals("Tabelle2", model.getActivatedTable(), "different activated table");
    Table<Integer, String, String> data = model.getData();
    assertEquals("1.0", data.get(1, "Test"), "wrong data");
  }
}
