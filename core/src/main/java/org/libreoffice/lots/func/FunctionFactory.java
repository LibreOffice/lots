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
package org.libreoffice.lots.func;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.dialog.Dialog;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates functions from ConfigThingys.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FunctionFactory.class);

  /**
   * A function that always returns true.
   */
  private static final Function myAlwaysTrueFunction = new AlwaysTrueFunction();

  private FunctionFactory()
  {
    // hide public ctor
  }

  /**
   * Returns a function that always returns true.
   */
  public static Function alwaysTrueFunction()
  {
    return myAlwaysTrueFunction;
  }

  /**
   * Constructs a Function object from the GRANDSONS of conf. Does conf have no grandchildren, so
   * will be delivered as zero. If conf has exactly one grandson, it becomes a function
   * delivered corresponding to this grandson. If conf has more than one grandson, it will
   * provided a function that evaluates all grandchildren as booleans and
   * AND-linked.
   *
   * @param funcLib
   *          the function library based on its references to functions
   *          are to be resolved.
   * @param dialogLib
   *          resolved the dialog library based on its references to dialogs
   *          should be.
   * @param context
   *          Some basic functions (especially DIALOG) hold context-dependent values.
   *          The context map is used to distinguish between the different instances,
   *          in which the various instances are stored. becomes zero here
   *          is passed, then a ConfigurationErrorException is thrown if
   *          conf contains a function that needs a context.
   * @throws ConfigurationErrorException
   *           if conf is not a correct function description or the function
   *           requires a context but null was passed.
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
   * Constructs a Function object from the CHILDREN of conf. If conf has no children, like this
   * will be delivered as zero. If conf has exactly one child, a function is returned
   * corresponding to this child. If conf has more than one child, it becomes a function
   * supplied, which evaluates all children as Booleans and ANDs them.
   *
   * @param funcLib
   *          the function library based on its references to functions
   *          are to be resolved.
   * @param dialogLib
   *          resolved the dialog library based on its references to dialogs
   *          should be.
   * @param context
   *          Some basic functions (especially DIALOG) hold context-dependent values.
   *          The context map is used to distinguish between the different instances,
   *          in which the various instances are stored. becomes zero here
   *          is passed, then a ConfigurationErrorException is thrown if
   *          conf contains a function that needs a context.
   * @throws ConfigurationErrorException
   *           if conf is not a correct function description or the function
   *           requires a context but null was passed.
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
   * Returns a Function object to conf, where conf itself is a legal
   * Must be a node of the function description (e.g., "AND" or "MATCH").
   *
   * @param funcLib
   *          the function library based on its references to functions
   *          are to be resolved.
   * @param dialogLib
   *          resolved the dialog library based on its references to dialogs
   *          should be.
   * @param context
   *          Some basic functions (especially DIALOG) hold context-dependent values.
   *          The context map is used to distinguish between the different instances,
   *          in which the various instances are stored. becomes zero here
   *          is passed, then a ConfigurationErrorException is thrown if
   *          conf contains a function that needs a context.
   * @throws ConfigurationErrorException
   *           if conf is not a correct function description or the function
   *           requires a context but null was passed.
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
        "\"{0}\" is not a supported basic function. ", name)
        + outputErrorPosition(conf));
    }
  }

  /**
   * Returns "Text at the error location: " + the first 100 characters of the
   * String representation of conf
   */
  private static String outputErrorPosition(ConfigThingy conf)
  {
    String str = conf.stringRepresentation();
    int end = 100;
    if (str.length() < end) end = str.length();
    return L.m("Text at error position: {0}", str.substring(0, end));
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
        L.m("For functions of type \"BIND\" after \"FUNCTION\" a function name or a function must follow."));
    if (funcConf.count() > 1)
      throw new ConfigurationErrorException(L.m("For functions of type \"BIND\", "
          + "\"FUNCTION\" must not be followed by a list, but only by a function name or a function."));

    funcConf = funcConf.iterator().next(); // <function name>|<function> - ...

    if (funcConf.count() == 0) // i.e. only a <function name> was specified
    {
      String funcName = funcConf.toString();

      func = funcLib.get(funcName);
      if (func == null)
        throw new ConfigurationErrorException(L.m(
          "Function \"{0}\" is used before it was even defined", funcName));
    }
    else // i.e. an entire function was specified
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
        "Function of type \"DIALOG\" requires exacly 2 parameters, not {0}",
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
      /* Can't be because count() tested */
      dialogName = null;
      dataName = null;
    }

    Dialog dialog = dialogLib.get(dialogName);
    if (dialog == null)
      throw new ConfigurationErrorException(L.m(
        "Dialog \"{0}\" is not defined, but it is used by the DIALOG-Function",
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
    { // above it was checked that there is exactly one node that is not ELSE or
      // THEN is
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
        "Function of type \"REPLACE\" requires exactly 3 parameters, not {0}",
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
      throw new ConfigurationErrorException(L.m("Error in regex \"{0}\"", regex), x);
    }
    return new ReplaceFunction(strFun, p, repFun);
  }

  private static Function parseSPLIT(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException(L.m(
        "Function of type \"SPLIT\" requires exactly 3 parameters, not {0}",
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
        "Index argument of {0} must not be negative.",
        conf.getName()));

    String regex = reFun.getResult(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Error in regex \"{0}\"", regex), x);
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
          L.m("Function of type \"MATCH\" requires exacly 2 parameters, not {0}", conf.count()), x);
    }

    String regex = reFun.getResult(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Error in regex \"{0}\"", regex), x);
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
          "Function of type \"VALUE\" requires exacly 2 parameters, not {0}",
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
            "BY-specification of {0} has to contain exacly one function or one string",
            conf.getName()));
        }

        if (byFun != null)
        {
          throw new ConfigurationErrorException(L.m(
            "{0}-Function may only have one BY-specification", conf.getName()));
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
            "MIN-specification of {0} has to be \"<NonNegativeInteger>\"",
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
            "MAX-Angabe von {0} muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));
        }

        maxScale = num;
      }
      else
      {
        if (dividendFun != null)
        {
          throw new ConfigurationErrorException(L.m(
              "For {0}-function more than one unqualified function was specified. "
                  + "Note that the divisor must be enclosed with BY(...).",
              conf.getName()));
        }
        dividendFun = parse(funConf, funcLib, dialogLib, context);
      }
    }

    if (dividendFun == null)
    {
      throw new ConfigurationErrorException(L.m(
        "For the {0}-function exactly one unqualified function must be specified",
        conf.getName()));
    }

    if (maxScale < 0)
    {
      if (byFun == null)
      {
        // Division by 1 doesn't break anything
        maxScale = 1024; // Integer.MAX_SIZE should actually be here, but
        // Even when dividing by 1, the BigDecimal class reserves memory according to the maximum arity
      }
      else
      {
        throw new ConfigurationErrorException(
          L.m(
            "{0} requires specifying MAX \"<NonNegativeInteger>\", if specified with BY divisor",
            conf.getName()));
      }
    }

    if (maxScale < minScale)
    {
      throw new ConfigurationErrorException(L.m(
        "In case of {0} MIN must be smaller than or equal to MAX", conf.getName()));
    }

    return new DivideFunction(dividendFun, byFun, minScale, maxScale);
  }

  /**
   * Parses the "Functions" sections from conf and returns an appropriate one
   * Function Library.
   *
   * @param context
   *          the context in which the function definitions are to be evaluated
   *          (especially DIALOG functions). DANGER! Here are values
   *          saved, it's not just a key.
   *
   * @param baselib
   *          if non-null this is linked as a fallback to functions
   *          deliver that cannot be found elsewhere.
   */
  public static FunctionLibrary parseFunctions(ConfigThingy conf,
      DialogLibrary dialogLib, Map<Object, Object> context, FunctionLibrary baselib)
  {
    return parseFunctions(new FunctionLibrary(baselib), conf, "Functions",
      dialogLib, context);
  }

  /**
   * Parses and inserts the contents of conf,query(section) as function definitions
   * added funcs.
   *
   * @param context
   *          the context in which the function definitions are to be evaluated
   *          (especially DIALOG functions). DANGER! Here are values
   *          saved, it's not just a key.
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
          LOGGER.error("Error parsing the function \"{}\" in section \"{}\"", name, section, e);
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
          LOGGER.error("Error during parsing of the column replacement function for the result column \"{}\"",
              name, e);
        }
      }
    }
    return trafos;
  }
}
