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
package de.muenchen.allg.itd51.wollmux.mailmerge.gender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;

public class GenderTrafoModelTest
{

  @Test
  public void testNullConfig()
  {
    GenderTrafoModel model = new GenderTrafoModel(null);
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertNull(model.getFunctionName());
  }

  @Test
  public void testEmptyConfig()
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertNull(model.getFunctionName());
  }

  @Test
  public void testNoBindFuntion() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name", "FOO \"BAR\""));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertNull(model.getFunctionName());
  }

  @Test
  public void testNoFunctionArgument() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name", "BIND(FOO \"BAR\")"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertNull(model.getFunctionName());
  }

  @Test
  public void testNotAGenderFunction() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name", "BIND(FUNCTION \"BAR\")"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertNull(model.getFunctionName());
  }

  @Test
  public void testWithoutSetFunctions() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name", "BIND(FUNCTION \"Gender\")"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertEquals("name", model.getFunctionName());
  }

  @Test
  public void testTooManyArgumentsInSetFunction() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(
        new ConfigThingy("name", "BIND(FUNCTION \"Gender\" SET(\"Anrede\" \"field\" \"field2\"))"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertEquals("name", model.getFunctionName());
  }

  @Test
  public void testNotAValueFunctionForFieldSet() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(
        new ConfigThingy("name", "BIND(FUNCTION \"Gender\" SET(\"Anrede\" CAT(\"field\" \"field2\")))"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertEquals("name", model.getFunctionName());
  }

  @Test
  public void testNoStringFunctionsForSetFunctions() throws IOException, SyntaxErrorException
  {
    GenderTrafoModel model = new GenderTrafoModel(new ConfigThingy("name",
        "BIND(FUNCTION \"Gender\" SET(\"Anrede\" VALUE(\"field\" \"field2\")) SET(\"Falls_Anrede_HerrN\" VALUE(\"male\")) "
            + "SET(\"Falls_Anrede_Frau\" VALUE(\"female\")) SET(\"Falls_sonstige_Anrede\" VALUE(\"other\")))"));
    assertEquals("", model.getMale());
    assertEquals("", model.getFemale());
    assertEquals("", model.getOther());
    assertEquals("", model.getField());
    assertEquals("name", model.getFunctionName());
  }

  @Test
  public void testValidModel() throws IOException, SyntaxErrorException
  {
    ConfigThingy trafo = new ConfigThingy("Func",
        "BIND(FUNCTION \"Gender\" SET(\"Anrede\" VALUE(\"field\")) SET(\"Falls_Anrede_HerrN\" \"male\") "
            + "SET(\"Falls_Anrede_Frau\" \"female\") SET(\"Falls_sonstige_Anrede\" \"other\"))");
    GenderTrafoModel model = new GenderTrafoModel(trafo);
    assertEquals("male", model.getMale());
    assertEquals("female", model.getFemale());
    assertEquals("other", model.getOther());
    assertEquals("field", model.getField());
    assertEquals("Func", model.getFunctionName());
    assertEquals(ConfigThingy.treeDump(trafo, ""), ConfigThingy.treeDump(model.generateGenderTrafoConf(), ""));
  }
}
