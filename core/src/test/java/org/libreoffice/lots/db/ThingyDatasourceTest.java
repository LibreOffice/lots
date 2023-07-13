/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.Datasource;
import org.libreoffice.lots.db.QueryPart;
import org.libreoffice.lots.db.ThingyDatasource;

public class ThingyDatasourceTest
{
  URL file = getClass().getResource("thingyDatasource.conf");

  @Test
  public void testThingyDatasource() throws Exception
  {
    Datasource ds = new ThingyDatasource(null,
        new ConfigThingy("", "NAME \"conf\" URL \"" + file + "\" Schluessel(\"column\" \"column2\")"), null);
    Dataset data = ds.find(List.of(new QueryPart("column", "value1"))).iterator().next();
    assertTrue(data.getKey().contains("value1") && data.getKey().contains("value2"));
    assertEquals("value2", data.get("column2"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unknown"));
  }

}
