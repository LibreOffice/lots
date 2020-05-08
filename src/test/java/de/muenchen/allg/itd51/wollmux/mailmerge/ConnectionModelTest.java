package de.muenchen.allg.itd51.wollmux.mailmerge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

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
