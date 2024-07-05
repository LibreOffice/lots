/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.mailmerge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheetDocument;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.mailmerge.ConnectionModel;
import org.libreoffice.lots.mailmerge.ConnectionModelListener;
import org.libreoffice.lots.mailmerge.NoTableSelectedException;
import org.libreoffice.lots.mailmerge.ds.DatasourceModel;
import org.libreoffice.lots.test.OfficeTest;

public class ConnectionModelTest extends OfficeTest
{
  private URL file = getClass().getResource("mailMergeModel.ods");
  private XSpreadsheetDocument xDoc;
  private Optional<DatasourceModel> model;

  @BeforeEach
  public void setUp() throws Exception
  {
    xDoc = UNO.XSpreadsheetDocument(loadComponent(file.toString(), false, true));
    model = ConnectionModel.addAndSelectDatasource(xDoc, Optional.empty());
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.XCloseable(xDoc).close(false);
  }

  @Test
  public void testInit() throws NoTableSelectedException
  {
    assertEquals("mailMergeModel - Tabelle1", ConnectionModel.buildConnectionName(model),
        "wrong connection");
    assertTrue(ConnectionModel.getConnections().contains("mailMergeModel - Tabelle2"),
        "connection not present");
  }

  @Test
  public void testListener() throws NoTableSelectedException
  {
    ConnectionModel.addListener(new ConnectionModelListener()
    {

      @Override
      public void connectionsChanged()
      {
        try
        {
          assertEquals("mailMergeModel - Tabelle2", ConnectionModel.buildConnectionName(model),
              "wrong connection");
        } catch (NoTableSelectedException ex)
        {
          assertTrue(false, "couldn't change datasource");
        }
      }
    });
    model = ConnectionModel.selectDatasource("mailMergeModel - Tabelle2");
  }

}
