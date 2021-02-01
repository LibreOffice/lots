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

package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PrintSettingsTest
{

  @Test
  public void testPrintSettingsRangeStart()
  {
    PrintSettings settings = new PrintSettings(8);
    settings.setRangeStart(11);
    assertEquals(8, settings.getRangeStart(), "RangeStart should be 8");
    settings.setRangeStart(0);
    assertEquals(1, settings.getRangeStart(), "RangeStart should be 1");
    settings.setRangeStart(2);
    assertEquals(2, settings.getRangeStart(), "RangeStart should be 2");
    settings.setRangeEnd(5);
    assertEquals(5, settings.getRangeEnd(), "RangeEnd should be 5");
    settings.setRangeStart(6);
    assertEquals(5, settings.getRangeStart(), "RangeStart should be 5");
    assertEquals(6, settings.getRangeEnd(), "RangeEnd should be 6");
  }

  @Test
  public void testPrintSettingsRangeEnd()
  {
    PrintSettings settings = new PrintSettings(10);
    assertEquals(10, settings.getRangeEnd(), "RangeEnd should be 10");
    settings.setRangeEnd(11);
    assertEquals(10, settings.getRangeEnd(), "RangeEnd should be 10");
    settings.setRangeEnd(8);
    assertEquals(8, settings.getRangeEnd(), "RangeEnd should be 8");
    settings.setRangeStart(5);
    assertEquals(5, settings.getRangeStart(), "RangeStart should be 5");
    settings.setRangeEnd(4);
    assertEquals(5, settings.getRangeEnd(), "RangeEnd should be 5");
    assertEquals(4, settings.getRangeStart(), "RangeStart should be 4");
  }

  @Test
  public void testPrintSettingsRecords()
  {
    PrintSettings settings = new PrintSettings(8);
    settings.addRecord(0);
    assertTrue(settings.getRecords().isEmpty(), "0 can't be in the list of records");
    settings.addRecord(1);
    assertTrue(settings.getRecords().contains(1), "1 should be in the list");
    settings.addRecord(1);
    assertEquals(1, settings.getRecords().size(), "No duplicate values");
    settings.addRecord(2);
    assertTrue(settings.getRecords().contains(2), "2 should be in the list");
  }
}
