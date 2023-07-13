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
package org.libreoffice.lots.mailmerge.ifthenelse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.mailmerge.ifthenelse.IfThenElseBaseModel;

public class IfThenElseBaseModelTest
{

  @Test
  public void testIfThenElseBaseModel()
  {
    IfThenElseBaseModel model = new IfThenElseBaseModel()
    {
    };
    assertEquals("", model.getValue());
    assertEquals(model, model.getById(model.getId()));
    assertNull(model.getById("unknown"));
    assertNull(model.getParent());
  }

  @Test
  public void testCreateWithText() throws IOException, SyntaxErrorException
  {
    IfThenElseBaseModel model = new IfThenElseBaseModel()
    {
    };
    model.setValue("foo");
    ConfigThingy result = new ConfigThingy("CAT", "\"foo\"");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateWithEmptyText() throws IOException, SyntaxErrorException
  {
    IfThenElseBaseModel model = new IfThenElseBaseModel()
    {
    };
    ConfigThingy result = new ConfigThingy("CAT", "\"\"");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateWithField() throws IOException, SyntaxErrorException
  {
    IfThenElseBaseModel model = new IfThenElseBaseModel()
    {
    };
    model.setValue("{{foo}}");
    ConfigThingy result = new ConfigThingy("CAT", "VALUE \"foo\"");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateWithFieldAndText() throws IOException, SyntaxErrorException
  {
    IfThenElseBaseModel model = new IfThenElseBaseModel()
    {
    };
    model.setValue("foo {{bar}} baz");
    ConfigThingy result = new ConfigThingy("CAT", "\"foo \" VALUE \"bar\" \" baz\"");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }
}
