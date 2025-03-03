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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.mailmerge.ifthenelse.ElseModel;
import org.libreoffice.lots.mailmerge.ifthenelse.IfModel;
import org.libreoffice.lots.mailmerge.ifthenelse.TestType;
import org.libreoffice.lots.mailmerge.ifthenelse.ThenModel;

public class IfModelTest
{

  @Test
  public void testIfModel()
  {
    IfModel model = new IfModel(new ThenModel(), new ElseModel());
    assertEquals(model, model.getThenModel().getParent());
    assertEquals(model, model.getElseModel().getParent());
    assertEquals("", model.getField());
    assertFalse(model.isNot());
    assertEquals(TestType.STRCMP, model.getComparator());
    model.setField("foo");
    model.setComparator(TestType.NUMCMP);
    model.setValue("1");
    assertEquals("WENN foo numerisch = 1", model.toString());
    model.setNot(true);
    model.setComparator(null);
    assertEquals("WENN foo nicht genau = 1", model.toString());
  }

  @Test
  public void testGetById()
  {
    ThenModel then = new ThenModel();
    ElseModel other = new ElseModel();
    IfModel model = new IfModel(then, other);
    assertEquals(model, model.getById(model.getId()));
    assertEquals(then, model.getById(then.getId()));
    assertEquals(other, model.getById(other.getId()));
    assertNull(model.getById("unknown"));
  }

  @Test
  public void testCreateDefault() throws IOException, SyntaxErrorException
  {
    IfModel model = new IfModel(new ThenModel(), new ElseModel());
    ConfigThingy result = new ConfigThingy("IF", "STRCMP(VALUE \"\" \"\") THEN (CAT \"\") ELSE (CAT \"\")");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateNullValues() throws IOException, SyntaxErrorException
  {
    IfModel model = new IfModel(new ThenModel(), new ElseModel());
    model.setValue(null);
    model.setField(null);
    ConfigThingy result = new ConfigThingy("IF", "STRCMP(VALUE \"\" \"\") THEN (CAT \"\") ELSE (CAT \"\")");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateNull() throws IOException, SyntaxErrorException
  {
    IfModel model = new IfModel(new ThenModel(), new ElseModel());
    model.setValue(null);
    model.setField(null);
    ConfigThingy result = new ConfigThingy("IF", "STRCMP(VALUE \"\" \"\") THEN (CAT \"\") ELSE (CAT \"\")");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testCreateWithNot() throws IOException, SyntaxErrorException
  {
    IfModel model = new IfModel(new ThenModel(), new ElseModel());
    model.setNot(true);
    ConfigThingy result = new ConfigThingy("IF", "NOT (STRCMP(VALUE \"\" \"\")) THEN (CAT \"\") ELSE (CAT \"\")");
    assertEquals(ConfigThingy.treeDump(result, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testIfModelFromConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("IF",
        "STRCMP(VALUE \"foo\" \"\") THEN (CAT (VALUE \"bar\")) ELSE (CAT (VALUE \"baz\"))");
    IfModel model = new IfModel(conf);
    assertEquals(ConfigThingy.treeDump(conf, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testIfModelFromConfigNoThenAndElse() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("IF", "NOT (STRCMP(VALUE \"\" \"\")) CAT \"\" CAT \"\"");
    IfModel model = new IfModel(conf);
    assertEquals("", model.getThenModel().getValue());
    assertEquals("", model.getElseModel().getValue());
  }

  @Test
  public void testIfModelFromConfigInvalidIf() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("IF", "NOT (STRCMP(VALUE \"\" \"\"))");
    IfModel model = new IfModel(conf);
    assertNotNull(model.getThenModel());
    assertNotNull(model.getElseModel());
  }

  @Test
  public void testIfModelFromConfigINested() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("IF");
    conf.addChild(new ConfigThingy("NOT", "STRCMP(VALUE \"\" \"\")"));
    conf.addChild(new ConfigThingy("THEN",
        "IF (STRCMP(VALUE \"foo\" \"\") THEN (CAT (VALUE \"bar\")) ELSE (CAT (VALUE \"baz\")))"));
    conf.addChild(new ConfigThingy("ELSE",
        "IF (STRCMP(VALUE \"foo\" \"\") THEN (CAT (VALUE \"bar\")) ELSE (CAT (VALUE \"baz\")))"));
    IfModel model = new IfModel(conf);
    assertEquals(IfModel.class, model.getThenModel().getClass());
    assertEquals(IfModel.class, model.getElseModel().getClass());
    assertEquals(ConfigThingy.treeDump(conf, ""), ConfigThingy.treeDump(model.create(), ""));
  }

  @Test
  public void testIfModelFromConfigInvalidComparator() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("IF",
        "BLA(VALUE \"foo\" \"\") THEN (CAT (VALUE \"bar\")) ELSE (CAT (VALUE \"baz\"))");
    IfModel model = new IfModel(conf);
    assertNotNull(model.getThenModel());
    assertNotNull(model.getElseModel());
  }
}
