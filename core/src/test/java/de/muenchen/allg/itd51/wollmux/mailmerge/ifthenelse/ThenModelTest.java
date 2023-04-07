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
package de.muenchen.allg.itd51.wollmux.mailmerge.ifthenelse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;

public class ThenModelTest
{

  @Test
  public void testThenModel()
  {
    ThenModel model = new ThenModel();
    model.setValue("foo");
    assertEquals("DANN foo", model.toString());
  }

  @Test
  public void testThenModelFormConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("THEN", "CAT (\"foo\")");
    ThenModel model = new ThenModel(conf);
    assertEquals("foo", model.getValue());
  }

  @Test
  public void testThenModelFormConfigWithValue() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("THEN", "CAT (VALUE \"foo\")");
    ThenModel model = new ThenModel(conf);
    assertEquals("{{foo}}", model.getValue());
  }

  @Test
  public void testThenModelFormWrongFunctionConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("THEN", "CAT ()");
    ThenModel model = new ThenModel(conf);
    assertEquals("", model.getValue());
  }

  @Test
  public void testThenModelFormEmptyConfig() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("THEN", "ELSE (\"foo\")");
    ThenModel model = new ThenModel(conf);
    assertEquals("", model.getValue());
  }

  @Test
  public void testThenModelForm2Config() throws IOException, SyntaxErrorException
  {
    ConfigThingy conf = new ConfigThingy("THEN", "\"foo\" \"bar\"");
    ThenModel model = new ThenModel(conf);
    assertEquals("", model.getValue());
  }
}
