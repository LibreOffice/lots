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
package de.muenchen.allg.itd51.wollmux.func;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

public class FunctionFactoryTest
{
  private static FunctionLibrary funcLib = new FunctionLibrary();
  private static DialogLibrary dialogLib = new DialogLibrary();
  private static HashMap<Object, Object> context = new HashMap<>();

  @BeforeAll
  public static void setup()
  {
    funcLib.add("func", new StringLiteralFunction("test"));
    dialogLib.add("dialog", new Dialog()
    {

      @Override
      public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib)
      {
        dialogEndListener.actionPerformed(new ActionEvent(this, 1, "success"));
      }

      @Override
      public Dialog instanceFor(Map<Object, Object> context)
      {
        return this;
      }

      @Override
      public Collection<String> getSchema()
      {
        return List.of("value1");
      }

      @Override
      public Object getData(String id)
      {
        if ("value1".equals(id))
        {
          return "v1";
        }
        return null;
      }
    });
  }

  @Test
  public void testParse() throws Exception
  {
    Function f = FunctionFactory.parse(new ConfigThingy("test", ""), funcLib, dialogLib, context);
    assertTrue(f instanceof StringLiteralFunction);

    f = FunctionFactory.parse(new ConfigThingy("AND", "\"true\""), funcLib, dialogLib, context);
    assertTrue(f instanceof AndFunction);

    f = FunctionFactory.parse(new ConfigThingy("NOT", "\"true\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NotFunction);

    f = FunctionFactory.parse(new ConfigThingy("OR", "\"true\""), funcLib, dialogLib, context);
    assertTrue(f instanceof OrFunction);

    f = FunctionFactory.parse(new ConfigThingy("VALUE", "\"true\""), funcLib, dialogLib, context);
    assertTrue(f instanceof ValueFunction);

    f = FunctionFactory.parse(new ConfigThingy("MATCH", "\"abs\" \"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof MatchFunction);

    f = FunctionFactory.parse(new ConfigThingy("REPLACE", "\"abc\" \"abc\" \"def\""), funcLib, dialogLib, context);
    assertTrue(f instanceof ReplaceFunction);

    f = FunctionFactory.parse(new ConfigThingy("SPLIT", "\"abc\" \"b\" \"0\""), funcLib, dialogLib, context);
    assertTrue(f instanceof SplitFunction);

    f = FunctionFactory.parse(new ConfigThingy("IF", "\"true\""), funcLib, dialogLib, context);
    assertTrue(f instanceof IfFunction);

    f = FunctionFactory.parse(
        new ConfigThingy("EXTERN",
            "URL \"java:de.muenchen.allg.itd51.wollmux.func.FunctionFactoryTest.extMethod\""),
        funcLib, dialogLib, context);
    assertTrue(f instanceof ExternalFunctionFunction);

    f = FunctionFactory.parse(new ConfigThingy("DIALOG", "\"dialog\" \"value1\""), funcLib, dialogLib, context);
    assertTrue(f instanceof DialogFunction);

    f = FunctionFactory.parse(new ConfigThingy("BIND", "FUNCTION \"func\""), funcLib, dialogLib, context);
    assertTrue(f instanceof BindFunction);

    f = FunctionFactory.parse(new ConfigThingy("SELECT", "\"test\""), funcLib, dialogLib, context);
    assertTrue(f instanceof SelectFunction);

    f = FunctionFactory.parse(new ConfigThingy("CAT", "\"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof CatFunction);

    f = FunctionFactory.parse(new ConfigThingy("THEN", "\"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof CatFunction);

    f = FunctionFactory.parse(new ConfigThingy("ELSE", "\"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof CatFunction);

    f = FunctionFactory.parse(new ConfigThingy("LENGTH", "\"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof LengthFunction);

    f = FunctionFactory.parse(new ConfigThingy("FORMAT", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof DivideFunction);

    f = FunctionFactory.parse(new ConfigThingy("DIVIDE", "\"5\" BY \"1\" MAX \"1\" MIN \"1\""), funcLib, dialogLib,
        context);
    assertTrue(f instanceof DivideFunction);

    f = FunctionFactory.parse(new ConfigThingy("MINUS", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof MinusFunction);

    f = FunctionFactory.parse(new ConfigThingy("SUM", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof SumFunction);

    f = FunctionFactory.parse(new ConfigThingy("DIFF", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof DiffFunction);

    f = FunctionFactory.parse(new ConfigThingy("PRODUCT", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof ProductFunction);

    f = FunctionFactory.parse(new ConfigThingy("ABS", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof AbsFunction);

    f = FunctionFactory.parse(new ConfigThingy("SIGN", "\"5\""), funcLib, dialogLib, context);
    assertTrue(f instanceof SignFunction);

    f = FunctionFactory.parse(new ConfigThingy("LT", "\"5\" \"2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NumberCompareFunction);

    f = FunctionFactory.parse(new ConfigThingy("LE", "\"5\" \"2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NumberCompareFunction);

    f = FunctionFactory.parse(new ConfigThingy("GT", "\"5\" \"2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NumberCompareFunction);

    f = FunctionFactory.parse(new ConfigThingy("GE", "\"5\" \"2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NumberCompareFunction);

    f = FunctionFactory.parse(new ConfigThingy("NUMCMP", "\"5\" \"2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof NumberCompareFunction);

    f = FunctionFactory.parse(new ConfigThingy("STRCMP", "\"abc\" \"def\""), funcLib, dialogLib, context);
    assertTrue(f instanceof StrCmpFunction);

    f = FunctionFactory.parse(new ConfigThingy("ISERROR", "\"test\""), funcLib, dialogLib, context);
    assertTrue(f instanceof IsErrorFunction);

    f = FunctionFactory.parse(new ConfigThingy("ISERRORSTRING", "\"test\""), funcLib, dialogLib, context);
    assertTrue(f instanceof IsErrorFunction);

    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("", "\"true\""), funcLib, dialogLib, context));

    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("unknown", "\"true\""), funcLib, dialogLib, context));
  }

  @Test
  public void testParseChildren() throws Exception
  {
    Function f = FunctionFactory.parseChildren(new ConfigThingy("", ""), funcLib, dialogLib, context);
    assertNull(f);

    f = FunctionFactory.parseChildren(new ConfigThingy("", "\"test\", \"test2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof AndFunction);
  }

  @Test
  public void testParseGrandChildren() throws Exception
  {
    Function f = FunctionFactory.parseGrandchildren(new ConfigThingy("", ""), funcLib, dialogLib, context);
    assertNull(f);

    f = FunctionFactory.parseGrandchildren(new ConfigThingy("", "bla (\"test\")"), funcLib, dialogLib, context);
    assertTrue(f instanceof StringLiteralFunction);

    f = FunctionFactory.parseGrandchildren(new ConfigThingy("", "bla (\"test\", \"test2\")"), funcLib, dialogLib,
        context);
    assertTrue(f instanceof AndFunction);
  }

  @Test
  public void testParseFunctions() throws Exception
  {
    FunctionLibrary lib = FunctionFactory.parseFunctions(new ConfigThingy("Functions", "Funktionen( func1 \"abc\")"),
        dialogLib, context, funcLib);
    assertTrue(lib.hasFunction("func1"));
    lib = FunctionFactory.parseFunctions(new ConfigThingy("Functions", "Funktionen( func1 (unknown \"abc\"))"),
        dialogLib, context, funcLib);
    assertFalse(lib.hasFunction("func1"));
  }

  @Test
  public void testParseTrafos() throws Exception
  {
    Map<String, Function> trafos = FunctionFactory.parseTrafos(new ConfigThingy("Trafos", "TRAFOS (trafo \"abc\")"),
        "TRAFOS", funcLib, dialogLib, context);
    assertTrue(trafos.containsKey("trafo"));

    trafos = FunctionFactory.parseTrafos(new ConfigThingy("Trafos", "TRAFOS \"abc\""), "TRAFOS", funcLib, dialogLib,
        context);
    assertTrue(trafos.isEmpty());
  }

  @Test
  public void testParseIf() throws Exception
  {
    Function f = FunctionFactory.parse(new ConfigThingy("IF", "\"true\" THEN \"then\" ELSE \"else\""), funcLib,
        dialogLib, context);
    assertTrue(f instanceof IfFunction);
    f = FunctionFactory.parse(new ConfigThingy("IF", "THEN \"then\" ELSE \"else\" \"true\""),
        funcLib,
        dialogLib, context);
    assertTrue(f instanceof IfFunction);

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("IF", "\"false\" THEN \"then\" THEN \"then\""), funcLib, dialogLib, context));

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("IF", "\"true\" ELSE \"else\" ELSE \"else\""), funcLib, dialogLib, context));

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("IF", "THEN \"else\" ELSE \"else\""), funcLib, dialogLib, context));
  }

  @Test
  public void testParseReplace() throws Exception
  {
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("REPLACE", "\"abc\" \"abc\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("REPLACE", "\"abc\" \"[\" \"def\""), funcLib, dialogLib, context));
  }

  @Test
  public void testParseSplit() throws Exception
  {
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("SPLIT", "\"abc\" \"abc\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("SPLIT", "\"abc\" \"abc\" \"-1\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("SPLIT", "\"abc\" \"abc\" (\"1\")"), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("SPLIT", "\"abc\" \"[\" \"0\""), funcLib, dialogLib, context));
  }

  @Test
  public void testParseMatch() throws Exception
  {
    Function f = FunctionFactory.parse(new ConfigThingy("MATCH", "\"abc\""), funcLib, dialogLib, context);
    assertTrue(f instanceof MatchFunction);
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("MATCH", "\"abc\" \"[\""), funcLib, dialogLib, context));

  }

  @Test
  public void testParseValue() throws Exception
  {
    Function f = FunctionFactory.parse(new ConfigThingy("VALUE", "\"value1\" \"value2\""), funcLib, dialogLib, context);
    assertTrue(f instanceof ValueFunction);
  }

  @Test
  public void testParseBind() throws Exception
  {
    Function f = FunctionFactory.parse(new ConfigThingy("BIND", "FUNCTION (SUM(\"1\" \"1\"))"), funcLib,
        dialogLib, context);
    assertTrue(f instanceof BindFunction);
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("BIND", "\"abc\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("BIND", "FUNCTION()"), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("BIND", "FUNCTION(\"abc\" \"def\")"), funcLib, dialogLib,
            context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("BIND", "FUNCTION \"abc\""), funcLib, dialogLib, context));
  }

  @Test
  public void testParseDialog() throws Exception
  {
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIALOG", "\"abc\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIALOG", "\"abc\" \"abc\""), funcLib, dialogLib, context));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIALOG", "\"dialog\" \"abc\""), funcLib, dialogLib, null));
  }

  @Test
  public void testParseDivide() throws Exception
  {
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIVIDE", "\"10\" BY(\"1\", \"2\")"), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" BY \"2\""), funcLib, dialogLib, null));

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MAX \"-1\""), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MAX \"abc\""), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MAX (abc(\"1\"))"), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\""), funcLib, dialogLib, null));

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MIN \"-1\""), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MIN \"abc\""), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MIN (abc(\"1\"))"), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" BY \"1\" MAX \"1\" MIN (\"2\")"), funcLib, dialogLib, null));

    assertThrows(ConfigurationErrorException.class, () -> FunctionFactory
        .parse(new ConfigThingy("DIVIDE", "\"10\" \"5\" BY \"1\" MAX \"2\""), funcLib, dialogLib, null));
    assertThrows(ConfigurationErrorException.class,
        () -> FunctionFactory.parse(new ConfigThingy("DIVIDE", "BY \"1\" MAX \"2\""), funcLib, dialogLib, null));
  }

  @Test
  public void testAlwaysTrueFunction()
  {
    assertTrue(FunctionFactory.alwaysTrueFunction() instanceof AlwaysTrueFunction);
  }

  public static String extMethod()
  {
    return "extMethod";
  }

}
