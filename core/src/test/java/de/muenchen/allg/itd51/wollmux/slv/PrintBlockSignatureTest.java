/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.util.L;

public class PrintBlockSignatureTest
{

  @Test
  public void testPrintBlockSignature()
  {
    PrintBlockSignature signature = PrintBlockSignature.valueOfIgnoreCase("allversions");
    assertEquals("AllVersions", signature.getName());
    assertEquals("SLV_AllVersions", signature.getGroupName());
    assertEquals(L.m("wird immer gedruckt"), signature.getMessage());
    assertThrows(IllegalArgumentException.class, () -> PrintBlockSignature.valueOfIgnoreCase("foo"));
  }
}
