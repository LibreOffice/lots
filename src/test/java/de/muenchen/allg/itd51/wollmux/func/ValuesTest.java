/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValuesTest
{

  @Test
  void none()
  {
    Values v = new Values.None();
    assertFalse(v.hasValue("test"));
    assertFalse(v.getBoolean("test"));
    assertEquals("", v.getString("test"));
  }

  @Test
  void simpleMap()
  {
    Values.SimpleMap v = new Values.SimpleMap();
    v.put("test", "a");
    assertTrue(v.hasValue("test"));
    assertFalse(v.getBoolean("test"));
    assertEquals("a", v.getString("test"));

    v.put("test", null);
    assertFalse(v.hasValue("test"));
    assertEquals("", v.getString("test"));
  }

}
