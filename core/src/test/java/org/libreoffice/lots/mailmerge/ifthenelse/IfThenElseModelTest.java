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
package org.libreoffice.lots.mailmerge.ifthenelse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.mailmerge.ifthenelse.ElseModel;
import org.libreoffice.lots.mailmerge.ifthenelse.IfModel;
import org.libreoffice.lots.mailmerge.ifthenelse.IfThenElseModel;
import org.libreoffice.lots.mailmerge.ifthenelse.ThenModel;

public class IfThenElseModelTest
{

  @Test
  public void testNullConfig()
  {
    IfThenElseModel model = new IfThenElseModel(null);
    assertNotNull(model.getFunction());
  }

  @Test
  public void testEmptyConfig()
  {
    IfThenElseModel model = new IfThenElseModel(new ConfigThingy(""));
    assertNotNull(model.getFunction());
  }

  @Test
  public void testConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy result = new ConfigThingy("Func",
        "IF (NOT (STRCMP(VALUE \"foo\" \"bar\")) THEN (CAT \"then\") ELSE (CAT \"else\"))");
    IfThenElseModel model = new IfThenElseModel(result);
    assertNotNull(model.getFunction());
    assertEquals("Func", model.getName());
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testGetById() throws IOException, SyntaxErrorException
  {
    ConfigThingy result = new ConfigThingy("Func",
        "IF (NOT (STRCMP(VALUE \"foo\" \"bar\")) THEN (CAT \"then\") ELSE (CAT \"else\"))");
    IfThenElseModel model = new IfThenElseModel(result);
    assertEquals(model.getFunction(), model.getById(model.getFunction().getId()));
  }

  @Test
  public void testCreateNewCondition()
  {
    IfThenElseModel model = new IfThenElseModel(null);
    assertNull(model.createNewCondition(model.getFunction().getId()));
    assertNull(model.createNewCondition("unknown"));
    assertNotNull(model.createNewCondition(model.getFunction().getThenModel().getId()));
    assertEquals(IfModel.class, model.getFunction().getThenModel().getClass());
    assertNotNull(model.createNewCondition(model.getFunction().getElseModel().getId()));
    assertEquals(IfModel.class, model.getFunction().getElseModel().getClass());
  }

  @Test
  public void testDeleteCondition()
  {
    IfThenElseModel model = new IfThenElseModel(null);
    assertNull(model.deleteCondition(model.getFunction().getId()));
    assertNull(model.deleteCondition("unknown"));
    model.createNewCondition(model.getFunction().getThenModel().getId());
    model.createNewCondition(model.getFunction().getElseModel().getId());
    assertNotNull(model.deleteCondition(model.getFunction().getThenModel().getId()));
    assertEquals(ThenModel.class, model.getFunction().getThenModel().getClass());
    assertNotNull(model.deleteCondition(((IfModel) model.getFunction().getElseModel()).getThenModel().getId()));
    assertEquals(ElseModel.class, model.getFunction().getElseModel().getClass());
  }
}
