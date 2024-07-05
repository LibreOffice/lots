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
package org.libreoffice.lots.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.func.Function;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.StringLiteralFunction;

public class FunctionLibraryTest
{

  @Test
  public void testFunctionLibrary()
  {
    FunctionLibrary baseLib = new FunctionLibrary();
    Function f1 = new StringLiteralFunction("test1");
    Function f2 = new StringLiteralFunction("test2");
    assertThrows(NullPointerException.class, () -> baseLib.add(null, f1));
    assertThrows(NullPointerException.class, () -> baseLib.add("test1", null));
    baseLib.add("test1", f1);
    assertEquals(f1, baseLib.get("test1"));
    assertTrue(baseLib.hasFunction("test1"));
    assertFalse(baseLib.hasFunction("test2"));
    assertNull(baseLib.get("test2"));
    assertEquals(1, baseLib.getFunctionNames().size());

    FunctionLibrary lib = new FunctionLibrary(baseLib, true);
    lib.add("test2", f2);
    assertEquals(f1, lib.get("test1"));
    assertTrue(lib.hasFunction("test1"));
    assertEquals(2, lib.getFunctionNames().size());

    Iterator<Function> iter = lib.iterator();
    assertTrue(iter.hasNext());
    assertEquals(f2, iter.next());
    assertFalse(iter.hasNext());
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());

    assertTrue(lib.remove("test2"));
    assertTrue(baseLib.remove("test1"));
  }

}
