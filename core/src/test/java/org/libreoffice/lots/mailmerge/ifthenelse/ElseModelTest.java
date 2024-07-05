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

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.mailmerge.ifthenelse.ElseModel;

public class ElseModelTest
{

  @Test
  public void testElseModel()
  {
    ElseModel model = new ElseModel();
    model.setValue("foo");
    assertEquals("SONST foo", model.toString());
  }

  @Test
  public void testElseModelFormConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("ELSE", "CAT (\"foo\")");
    ElseModel model = new ElseModel(conf);
    assertEquals("foo", model.getValue());
  }

  @Test
  public void testElseModelFormConfigWithValue() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("ELSE", "CAT (VALUE \"foo\")");
    ElseModel model = new ElseModel(conf);
    assertEquals("{{foo}}", model.getValue());
  }

  @Test
  public void testElseModelFormWrongFunctionConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("ELSE", "CAT ()");
    ElseModel model = new ElseModel(conf);
    assertEquals("", model.getValue());
  }

  @Test
  public void testElseModelFormEmptyConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("ELSE", "THEN (\"foo\")");
    ElseModel model = new ElseModel(conf);
    assertEquals("", model.getValue());
  }

  @Test
  public void testElseModelForm2Config() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("ELSE", "\"foo\" \"bar\"");
    ElseModel model = new ElseModel(conf);
    assertEquals("", model.getValue());
  }
}
