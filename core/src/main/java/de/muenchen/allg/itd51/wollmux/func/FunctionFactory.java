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
package de.muenchen.allg.itd51.wollmux.func;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Erzeugt Functions aus ConfigThingys.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FunctionFactory.class);

  /**
   * Eine Funktion, die immer true liefert.
   */
  private static final Function myAlwaysTrueFunction = new AlwaysTrueFunction();

  private FunctionFactory()
  {
    // hide public ctor
  }

  /**
   * Liefert eine Funktion, die immer true liefert.
   */
  public static Function alwaysTrueFunction()
  {
    return myAlwaysTrueFunction;
  }

  /**
   * Erzeugt ein Function-Objekt aus den ENKELN von conf. Hat conf keine Enkel, so
   * wird null geliefert. Hat conf genau einen Enkel, so wird eine Funktion
   * geliefert, die diesem Enkel entspricht. Hat conf mehr als einen Enkel, so wird
   * eine Funktion geliefert, die alle Enkel als Booleans auswertet und
   * UND-verknüpft.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parseGrandchildren(ConfigThingy conf,
      FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
  {
    List<Function> andFunction = new ArrayList<>();
    for (ConfigThingy children : conf)
    {
      for (ConfigThingy func : children)
      {
        Function cons = parse(func, funcLib, dialogLib, context);

        andFunction.add(cons);
      }
    }

    if (andFunction.isEmpty())
    {
      return null;
    }
    if (andFunction.size() == 1)
    {
      return andFunction.get(0);
    }

    return new AndFunction(andFunction);
  }

  /**
   * Erzeugt ein Function-Objekt aus den KINDERN von conf. Hat conf keine Kinder, so
   * wird null geliefert. Hat conf genau ein Kind, so wird eine Funktion geliefert,
   * die diesem Kind entspricht. Hat conf mehr als ein Kind, so wird eine Funktion
   * geliefert, die alle Kinder als Booleans auswertet und UND-verknüpft.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parseChildren(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    List<Function> andFunction = new ArrayList<>();
    for (ConfigThingy func : conf)
    {
      Function cons = parse(func, funcLib, dialogLib, context);
      andFunction.add(cons);
    }

    if (andFunction.isEmpty())
    {
      return null;
    }
    if (andFunction.size() == 1)
    {
      return andFunction.get(0);
    }

    return new AndFunction(andFunction);
  }

  /**
   * Liefert ein Function Objekt zu conf, wobei conf selbst schon ein erlaubter
   * Knoten der Funktionsbeschreibung (z,B, "AND" oder "MATCH") sein muss.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parse(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    String name = conf.getName();

    if (conf.count() == 0) return new StringLiteralFunction(name);

    if (name.equals("AND"))
    {
      return new AndFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("NOT"))
    {
      return new NotFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("OR"))
    {
      return new OrFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("VALUE"))
    {
      return parseVALUE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("MATCH"))
    {
      return parseMATCH(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("REPLACE"))
    {
      return parseREPLACE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SPLIT"))
    {
      return parseSPLIT(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("IF"))
    {
      return parseIF(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("EXTERN"))
    {
      return new ExternalFunctionFunction(conf);
    }
    else if (name.equals("DIALOG"))
    {
      return parseDIALOG(conf, dialogLib, context);
    }
    else if (name.equals("BIND"))
    {
      return parseBIND(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SELECT"))
    {
      return new SelectFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("CAT") || name.equals("THEN") || name.equals("ELSE"))
    {
      return new CatFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("LENGTH"))
    {
      return new LengthFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("FORMAT") || name.equals("DIVIDE"))
    {
      return parseDIVIDE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("MINUS"))
    {
      return new MinusFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SUM"))
    {
      return new SumFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("DIFF"))
    {
      return new DiffFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("PRODUCT"))
    {
      return new ProductFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ABS"))
    {
      return new AbsFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SIGN"))
    {
      return new SignFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("LT"))
    {
      return new NumberCompareFunction(0, 1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("LE"))
    {
      return new NumberCompareFunction(1, 1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("GT"))
    {
      return new NumberCompareFunction(0, -1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("GE"))
    {
      return new NumberCompareFunction(-1, -1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("NUMCMP"))
    {
      return new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        conf, funcLib, dialogLib, context);
    }
    else if (name.equals("STRCMP"))
    {
      return new StrCmpFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ISERROR"))
    {
      return new IsErrorFunction(true, conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ISERRORSTRING"))
    {
      return new IsErrorFunction(false, conf, funcLib, dialogLib, context);
    }

    if (name.length() == 0)
    {
      throw new ConfigurationErrorException(
        L.m("Opening bracket without preceding function name found. ")
          + outputErrorPosition(conf));
    }
    else
    {
      throw new ConfigurationErrorException(L.m(
        "\"%1\" is not a supported basic function. ", name)
        + outputErrorPosition(conf));
    }
  }

  /**
   * Liefert "Text an der Fehlerstelle: " + die ersten 100 Zeichen der
   * Stringdarstellung von conf
   */
  private static String outputErrorPosition(ConfigThingy conf)
  {
    String str = conf.stringRepresentation();
    int end = 100;
    if (str.length() < end) end = str.length();
    return L.m("Text at error position: %1", str.substring(0, end));
  }

  private static Function parseBIND(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    ConfigThingy funcConf = conf.query("FUNCTION"); // funcConf = <query results> -
    // FUNCTION - ...
    if (funcConf.count() != 1)
      throw new ConfigurationErrorException(
        L.m("Function of type \"BIND\" requires exactly one sub element FUNCTION"));

    Function func;
    funcConf = funcConf.iterator().next(); // funcConf = FUNCTION - ...
    if (funcConf.count() == 0)
      throw new ConfigurationErrorException(
        L.m("For functions of type \"BIND\" after \"FUNCTION\" a function name or a function must follow."));//TODO
    if (funcConf.count() > 1)
      throw new ConfigurationErrorException(L.m("For functions of type \"BIND\" after \"FUNCTION\" "
          + "no list but a function name or function must follow."));//TODO

    funcConf = funcConf.iterator().next(); // <Funktionsname>|<Funktion> - ...

    if (funcConf.count() == 0) // d.h. es wurde nur ein <Funktionsname> angegeben
    {
      String funcName = funcConf.toString();

      func = funcLib.get(funcName);
      if (func == null)
        throw new ConfigurationErrorException(L.m(
          "Function \"%1\" is used before it was even defined", funcName));
    }
    else // d.h. es wurde eine ganze Funktion angegeben
    {
      func = parse(funcConf, funcLib, dialogLib, context);
    }

    return new BindFunction(func, conf, funcLib, dialogLib, context);
  }

  private static Function parseDIALOG(ConfigThingy conf, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    if (conf.count() != 2)
      throw new ConfigurationErrorException(L.m(
        "Function of type \"DIALOG\" requires exacly 2 parameters, not %1",
        conf.count()));

    String dialogName;
    String dataName;

    try
    {
      dialogName = conf.getFirstChild().toString();
      dataName = conf.getLastChild().toString();
    }
    catch (NodeNotFoundException x)
    {
      /* Kann nicht sein, weil count() getestet */
      dialogName = null;
      dataName = null;
    }

    Dialog dialog = dialogLib.get(dialogName);
    if (dialog == null)
      throw new ConfigurationErrorException(L.m(
        "Dialog \"%1\" is not defined, but it is used by the DIALOG-Function",
        dialogName));

    if (context == null)
      throw new ConfigurationErrorException(
        L.m("DIALOG-Function is context dependent and therefore cannot be used here."));

    return new DialogFunction(dialogName, dialog, dataName, context);
  }

  private static Function parseIF(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    ConfigThingy thenConf = conf.query("THEN");
    ConfigThingy elseConf = conf.query("ELSE");
    if (thenConf.count() > 1 || elseConf.count() > 1)
      throw new ConfigurationErrorException(
        L.m("Within the IF statement there must not be more than one THEN and one ELSE"));

    if (conf.count() - thenConf.count() - elseConf.count() != 1)
      throw new ConfigurationErrorException(
        L.m("IF must contain exactly one condition."));

    if (thenConf.count() == 0)
    {
      thenConf = new ConfigThingy("dummy");
      thenConf.add("THEN").add("");
    }

    if (elseConf.count() == 0)
    {
      elseConf = new ConfigThingy("dummy");
      elseConf.add("ELSE").add("");
    }

    Iterator<ConfigThingy> iter = conf.iterator();
    ConfigThingy condition;
    do
    { // oben wurde überprüft, dass es genau einen Knoten gibt, der nicht ELSE oder
      // THEN ist
      condition = iter.next();
    } while (condition.getName().equals("THEN")
      || condition.getName().equals("ELSE"));

    Function ifFun = parse(condition, funcLib, dialogLib, context);
    Function thenFun = parseChildren(thenConf, funcLib, dialogLib, context);
    Function elseFun = parseChildren(elseConf, funcLib, dialogLib, context);

    return new IfFunction(ifFun, thenFun, elseFun);
  }

  private static Function parseREPLACE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException(L.m(
        "Function of type \"REPLACE\" requires exactly 3 parameters, not %1",
        conf.count()));

    Function strFun;
    Function reFun;
    Function repFun;

    Iterator<ConfigThingy> iter = conf.iterator();
    strFun = parse(iter.next(), funcLib, dialogLib, context);
    reFun = parse(iter.next(), funcLib, dialogLib, context);
    repFun = parse(iter.next(), funcLib, dialogLib, context);

    String regex = reFun.getResult(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Error in regex \"%1\"", regex), x);
    }
    return new ReplaceFunction(strFun, p, repFun);
  }

  private static Function parseSPLIT(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException(L.m(
        "Function of type \"SPLIT\" requires exactly 3 parameters, not %1",
        conf.count()));

    Function strFun;
    Function reFun;
    int idx;

    Iterator<ConfigThingy> iter = conf.iterator();
    strFun = parse(iter.next(), funcLib, dialogLib, context);
    reFun = parse(iter.next(), funcLib, dialogLib, context);

    idx = -1;
    try
    {
      ConfigThingy idxConf = iter.next();
      if (idxConf.count() == 0)
      {
        idx = Integer.parseInt(idxConf.toString());
      }
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }
    if (idx < 0)
      throw new ConfigurationErrorException(L.m(
        "Index argument of %1 must be \"<NonNegativeInteger>\"",//TODO
        conf.getName()));

    String regex = reFun.getResult(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Error in regex \"%1\"", regex), x);
    }
    return new SplitFunction(strFun, p, idx);
  }

  private static Function parseMATCH(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function strFun;
    Function reFun;

    // TODO doesn't work as described. Exception isn't thrown by 1 or more than 2 parameters.
    try
    {
      strFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
      reFun = parse(conf.getLastChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
          L.m("Function of type \"MATCH\" requires exacly 2 parameters, not %1", conf.count()), x);
    }

    String regex = reFun.getResult(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Error in regex \"%1\"", regex), x);
    }
    return new MatchFunction(strFun, p);
  }

  private static Function parseVALUE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function valueNameFun;
    // TODO doesn't work as described. Exception isn't thrown by 0 or more than 1 parameters.
    try
    {
      valueNameFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(L.m(
          "Function of type \"VALUE\" requires exacly 2 parameters, not %1",
          conf.count()), e);
    }

    return new ValueFunction(valueNameFun.getResult(new Values.None()));
  }

  private static Function parseDIVIDE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function dividendFun = null;
    Function byFun = null;
    int minScale = 0;
    int maxScale = -1;

    for (ConfigThingy funConf : conf)
    {
      String name = funConf.getName();
      if (name.equals("BY"))
      {
        if (funConf.count() != 1)
        {
          throw new ConfigurationErrorException(L.m(
            "BY-specification of %1 has to contain exacly one function or one string",
            conf.getName()));
        }

        if (byFun != null)
        {
          throw new ConfigurationErrorException(L.m(
            "%1-Function may only have one BY-specification", conf.getName()));
        }

        byFun = parseChildren(funConf, funcLib, dialogLib, context);
      }
      else if (name.equals("MIN"))
      {
        int num = -1;
        try
        {
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }

        if (num < 0)
        {
          throw new ConfigurationErrorException(L.m(
            "MIN-specification of %1 has to be \"<NonNegativeInteger>\"",
            conf.getName()));
        }

        minScale = num;

      }
      else if (name.equals("MAX"))
      {
        int num = -1;
        try
        {
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }

        if (num < 0)
        {
          throw new ConfigurationErrorException(L.m(
            "MAX-Angabe von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));
        }

        maxScale = num;
      }
      else
      {
        if (dividendFun != null)
        {
          throw new ConfigurationErrorException(L.m(
              "For %1-function more than one unqualified function was specified. "
                  + "Note that the divisor must be enclosed with BY(...).",
              conf.getName()));
        }
        dividendFun = parse(funConf, funcLib, dialogLib, context);
      }
    }

    if (dividendFun == null)
    {
      throw new ConfigurationErrorException(L.m(
        "For the %1-function exactly one unqualified function must be specified",
        conf.getName()));
    }

    if (maxScale < 0)
    {
      if (byFun == null)
      {
        // Division durch 1 nichts kaputt macht
        maxScale = 1024; // eigentlich sollte hier Integer.MAX_SIZE stehen, aber
        // auch bei Division durch 1 reserviert die BigDecimal-Klasse Speicher entsprechend der maximalen Stelligkeit
      }
      else
      {
        throw new ConfigurationErrorException(
          L.m(
            "%1 requires specifying MAX \"<NonNegativeInteger>\", if specified with BY divisor",
            conf.getName()));
      }
    }

    if (maxScale < minScale)
    {
      throw new ConfigurationErrorException(L.m(
        "In case of %1 MIN must be smaller than or equal to MAX", conf.getName()));
    }

    return new DivideFunction(dividendFun, byFun, minScale, maxScale);
  }

  /**
   * Parst die "Funktionen" Abschnitte aus conf und liefert eine entsprechende
   * FunctionLibrary.
   * 
   * @param context
   *          der Kontext in dem die Funktionsdefinitionen ausgewertet werden sollen
   *          (insbesondere DIALOG-Funktionen). ACHTUNG! Hier werden Werte
   *          gespeichert, es ist nicht nur ein Schlüssel.
   * 
   * @param baselib
   *          falls nicht-null wird diese als Fallback verlinkt, um Funktionen zu
   *          liefern, die anderweitig nicht gefunden werden.
   */
  public static FunctionLibrary parseFunctions(ConfigThingy conf,
      DialogLibrary dialogLib, Map<Object, Object> context, FunctionLibrary baselib)
  {
    return parseFunctions(new FunctionLibrary(baselib), conf, "Funktionen",
      dialogLib, context);
  }

  /**
   * Parst die Inhalte von conf,query(section) als Funktionsdefinitionen und fügt sie
   * funcs hinzu.
   * 
   * @param context
   *          der Kontext in dem die Funktionsdefinitionen ausgewertet werden sollen
   *          (insbesondere DIALOG-Funktionen). ACHTUNG! Hier werden Werte
   *          gespeichert, es ist nicht nur ein Schlüssel.
   * 
   * @return funcs
   */
  public static FunctionLibrary parseFunctions(FunctionLibrary funcs,
      ConfigThingy conf, String section, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    conf = conf.query(section);
    Iterator<ConfigThingy> parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      Iterator<ConfigThingy> iter = parentIter.next().iterator();
      while (iter.hasNext())
      {
        ConfigThingy funcConf = iter.next();
        String name = funcConf.getName();
        try
        {
          Function func =
            parseChildren(funcConf, funcs, dialogLib, context);
          funcs.add(name, func);
        }
        catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m(
            "Error parsing the function \"%1\" in section \"%2\"", name,//TODO
            section), e);
        }
      }
    }

    return funcs;
  }

  public static Map<String, Function> parseTrafos(ConfigThingy trafoConf, String nodeName, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Map<String, Function> trafos = new HashMap<>();
    Iterator<ConfigThingy> suIter = trafoConf.query(nodeName, 1).iterator();
    while (suIter.hasNext())
    {
      ConfigThingy spaltenumsetzung = suIter.next();

      for (ConfigThingy transConf : spaltenumsetzung)
      {
        String name = transConf.getName();
        try
        {
          Function func = FunctionFactory.parseChildren(transConf, funcLib, dialogLib, context);
          if (func == null)
            throw new ConfigurationErrorException(
                L.m("Empty functions definition is not allowed. Instead use the empty string \"\""));
          trafos.put(name, func);
        } catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m("Error during parsing of the column replacement function for the result column \"%1\"", name), e);
        }
      }
    }
    return trafos;
  }
}
